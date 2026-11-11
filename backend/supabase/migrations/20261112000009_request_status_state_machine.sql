-- A single controlled state machine for request status changes.
--
-- DECISION (consolidate in place, zero app changes): doctor_respond_request,
-- doctor_confirm_request, doctor_complete_request, and cancel_request
-- already exist and are already called by name, with these exact
-- parameter names, by both apps (RequestsRepository.kt in each). Checking
-- every call site first: all four discard the RPC's return value entirely
-- (`Unit` after the call) -- so their return *type* can change freely, and
-- their *names and parameters* are the only compatibility surface that
-- matters. Rather than ship a parallel, differently-named function (as the
-- additive-only choice was for the scheduling migration, where the new
-- function's signature genuinely couldn't preserve the old call sites),
-- this migration makes transition_request_status() the one real state
-- machine and turns all four existing RPCs into thin wrappers around it --
-- same names, same params, same "returns requests" shape, zero app
-- changes, and every existing caller transparently gets the fixes below.
--
-- Real bugs fixed by consolidating (found while reading the existing
-- bodies -- see git history for the exact definitions before this
-- migration):
--   - cancel_request wrote its reason into doctor_response_note; the
--     dedicated cancellation_reason column (added in 20261112000006) was
--     never written to. Same bug for rejection_reason in
--     doctor_respond_request. Fixed: the new function writes to the
--     correct column for each terminal state.
--   - doctor_confirm_request/doctor_complete_request only ever checked
--     doctor_id = auth.uid() -- an admin could never confirm or complete a
--     request. Fixed per this prompt's explicit "(or admin)".
--   - doctor_respond_request never checked the doctor was still approved,
--     and never caught exclusion_violation on accept (a raw 23P01 would
--     have reached the client). Both fixed.
--   - None of the four used SELECT ... FOR UPDATE before validating.
--   - doctor_respond_request required a reason to decline; this prompt's
--     spec says the reason is optional for rejected, so that requirement
--     is dropped (an explicit behavior change the prompt asked for, not a
--     guess).
--
-- DECISION (error code numbering): the scheduling migration
-- (20261112000008) already defined AS002_DOCTOR_NOT_AVAILABLE and
-- AS003_INVALID_DATE. This prompt's literal AS002_SLOT_ALREADY_BOOKED and
-- AS003_INVALID_TRANSITION would collide with those under different
-- meanings, breaking the "one meaning per code" contract docs/error-codes.md
-- already documents. Renumbered to continue the existing global sequence
-- (AS015 onward) instead -- see docs/error-codes.md for the full,
-- renumbered list and the reasoning restated there.
--
-- DECISION (log_request_status_change / notifications): the existing
-- AFTER UPDATE trigger already writes the request_status_history row and
-- the accepted/rejected/confirmed/completed/cancelled notifications on
-- every status UPDATE, regardless of entry point -- so
-- transition_request_status does NOT duplicate that; it only performs the
-- UPDATE (with the correct reason column set) and lets the trigger do its
-- job, same as accepted_at/confirmed_at/etc. (requests_set_status_timestamps)
-- and updated_at (set_updated_at), both also already automatic. The
-- trigger itself needed one real fix to match this prompt's explicit
-- requirement -- "rejected: notify patient (include the reason only if the
-- doctor provided one)" -- since it previously built a generic message
-- and read the (wrong, per above) doctor_response_note column. That one
-- fix is included below; the cancelled branch's notification logic
-- (patient-cancels notifies doctor, doctor-cancels notifies patient,
-- admin-cancels notifies nobody) is intentionally left exactly as it
-- already was -- this prompt only specifies the patient/doctor cases, so
-- the pre-existing admin-sends-nothing behavior is preserved rather than
-- extended on a guess.

begin;

-- ---------------------------------------------------------------------------
-- booking_settings: add the (default: disabled) patient cancellation
-- notice window, per "make it a constant" -- centralized with the other
-- scheduling constants rather than a second settings function.
-- ---------------------------------------------------------------------------
-- Adding an output column changes the function's row type, which Postgres
-- will not let CREATE OR REPLACE do -- drop first (functions hold no data,
-- so this is risk-free, unlike dropping a table).
drop function if exists public.booking_settings();

create function public.booking_settings()
returns table (max_horizon_days integer, min_lead_minutes integer, cancellation_notice_minutes integer)
language plpgsql
security definer
set search_path = ''
as $$
begin
  if auth.uid() is null then
    raise exception 'AS000_NOT_AUTHENTICATED: You must be signed in.' using errcode = 'P0001';
  end if;

  return query select 60, 60, 0;
end;
$$;

revoke all on function public.booking_settings() from public, anon;
grant execute on function public.booking_settings() to authenticated;

-- ---------------------------------------------------------------------------
-- log_request_status_change: same trigger, same notifications, except the
-- rejected branch now reads the correct column and includes the reason
-- when one was given; the history note reads cancellation_reason for
-- cancellations too (previously always doctor_response_note for both).
-- ---------------------------------------------------------------------------
create or replace function public.log_request_status_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_service_name text;
begin
  select name into v_service_name from public.services where id = new.service_id;

  insert into public.request_status_history (request_id, old_status, new_status, changed_by, note)
  values (
    new.id, old.status, new.status, auth.uid(),
    case new.status
      when 'rejected' then new.rejection_reason
      when 'cancelled' then new.cancellation_reason
      else null
    end
  );

  if new.status = 'accepted' then
    insert into public.notifications (user_id, type, title, body, related_request_id)
    values (new.patient_id, 'request_accepted', 'Request accepted',
      format('Your request for %s was accepted by the doctor.', coalesce(v_service_name, 'your service')), new.id);
  elsif new.status = 'rejected' then
    insert into public.notifications (user_id, type, title, body, related_request_id)
    values (
      new.patient_id, 'request_rejected', 'Request rejected',
      case when coalesce(trim(new.rejection_reason), '') <> '' then
        format('Your request for %s was declined: %s', coalesce(v_service_name, 'your service'), new.rejection_reason)
      else
        format('Your request for %s was declined.', coalesce(v_service_name, 'your service'))
      end,
      new.id
    );
  elsif new.status = 'confirmed' then
    insert into public.notifications (user_id, type, title, body, related_request_id)
    values (new.patient_id, 'request_confirmed', 'Request confirmed',
      format('Your request for %s was confirmed.', coalesce(v_service_name, 'your service')), new.id);
  elsif new.status = 'completed' then
    insert into public.notifications (user_id, type, title, body, related_request_id)
    values (new.patient_id, 'request_completed', 'Visit completed',
      format('Your %s visit is marked completed.', coalesce(v_service_name, 'service')), new.id);
  elsif new.status = 'cancelled' then
    if auth.uid() = new.patient_id and new.doctor_id is not null then
      insert into public.notifications (user_id, type, title, body, related_request_id)
      values (new.doctor_id, 'request_cancelled', 'Request cancelled',
        format('A patient cancelled their %s request.', coalesce(v_service_name, 'service')), new.id);
    elsif new.doctor_id is not null and auth.uid() = new.doctor_id then
      insert into public.notifications (user_id, type, title, body, related_request_id)
      values (new.patient_id, 'request_cancelled', 'Request cancelled',
        format('Your %s request was cancelled by the doctor.', coalesce(v_service_name, 'service')), new.id);
    end if;
  end if;

  return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- transition_request_status: the state machine.
-- ---------------------------------------------------------------------------
create or replace function public.transition_request_status(
  p_request_id uuid,
  p_new_status public.request_status,
  p_reason text default null
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_caller_id uuid;
  v_request public.requests;
  v_old_status public.request_status;
  v_is_patient boolean;
  v_is_doctor boolean;
  v_is_admin boolean;
  v_doctor_ok boolean;
  v_notice_minutes integer;
begin
  v_caller_id := auth.uid();
  if v_caller_id is null then
    raise exception 'AS000_NOT_AUTHENTICATED: You must be signed in.' using errcode = 'P0001';
  end if;

  select * into v_request from public.requests where id = p_request_id for update;
  if v_request.id is null then
    raise exception 'AS015_REQUEST_NOT_FOUND: Request not found.' using errcode = 'P0001';
  end if;

  v_old_status := v_request.status;
  v_is_admin := public.is_admin();
  v_is_patient := v_request.patient_id = v_caller_id;
  v_is_doctor := v_request.doctor_id is not null and v_request.doctor_id = v_caller_id;

  if not (v_is_patient or v_is_doctor or v_is_admin) then
    raise exception 'AS017_NOT_AUTHORIZED_FOR_TRANSITION: You are not authorized to act on this request.'
      using errcode = 'P0001';
  end if;

  if p_new_status = v_old_status then
    raise exception 'AS016_INVALID_TRANSITION: This request is already %.', v_old_status using errcode = 'P0001';
  end if;

  if v_old_status = 'requested' and p_new_status = 'accepted' then
    if not v_is_doctor then
      raise exception 'AS017_NOT_AUTHORIZED_FOR_TRANSITION: Only the assigned doctor can accept this request.'
        using errcode = 'P0001';
    end if;

    select (
      dp.status = 'approved' and not coalesce(pr.is_suspended, true)
    ) into v_doctor_ok
    from public.doctor_profiles dp
    join public.profiles pr on pr.id = dp.user_id
    where dp.user_id = v_caller_id;

    if not coalesce(v_doctor_ok, false) then
      raise exception 'AS018_DOCTOR_NOT_APPROVED: You are no longer approved to respond to requests.'
        using errcode = 'P0001';
    end if;

  elsif v_old_status = 'requested' and p_new_status = 'rejected' then
    if not v_is_doctor then
      raise exception 'AS017_NOT_AUTHORIZED_FOR_TRANSITION: Only the assigned doctor can decline this request.'
        using errcode = 'P0001';
    end if;

  elsif v_old_status = 'accepted' and p_new_status = 'confirmed' then
    if not (v_is_doctor or v_is_admin) then
      raise exception 'AS017_NOT_AUTHORIZED_FOR_TRANSITION: Only the assigned doctor or an admin can confirm this request.'
        using errcode = 'P0001';
    end if;

  elsif v_old_status = 'confirmed' and p_new_status = 'completed' then
    if not (v_is_doctor or v_is_admin) then
      raise exception 'AS017_NOT_AUTHORIZED_FOR_TRANSITION: Only the assigned doctor or an admin can complete this request.'
        using errcode = 'P0001';
    end if;
    if now() < v_request.scheduled_start then
      raise exception 'AS020_TOO_EARLY_TO_COMPLETE: This request cannot be completed before its scheduled start time.'
        using errcode = 'P0001';
    end if;

  elsif v_old_status in ('requested', 'accepted', 'confirmed') and p_new_status = 'cancelled' then
    if v_is_patient then
      select coalesce(cancellation_notice_minutes, 0) into v_notice_minutes from public.booking_settings();
      if v_notice_minutes > 0 and (v_request.scheduled_start - now()) < make_interval(mins => v_notice_minutes) then
        raise exception 'AS021_CANCELLATION_NOTICE_WINDOW: This request can no longer be cancelled -- the minimum notice window has passed.'
          using errcode = 'P0001';
      end if;
    elsif v_is_doctor then
      if v_old_status = 'requested' then
        raise exception 'AS017_NOT_AUTHORIZED_FOR_TRANSITION: A doctor can only cancel an accepted or confirmed request -- decline a requested one instead.'
          using errcode = 'P0001';
      end if;
    elsif not v_is_admin then
      raise exception 'AS017_NOT_AUTHORIZED_FOR_TRANSITION: You are not authorized to cancel this request.'
        using errcode = 'P0001';
    end if;

  else
    raise exception 'AS016_INVALID_TRANSITION: % cannot move to %.', v_old_status, p_new_status using errcode = 'P0001';
  end if;

  begin
    if p_new_status = 'rejected' then
      update public.requests set status = p_new_status, rejection_reason = p_reason where id = p_request_id;
    elsif p_new_status = 'cancelled' then
      update public.requests set status = p_new_status, cancellation_reason = p_reason where id = p_request_id;
    else
      update public.requests set status = p_new_status where id = p_request_id;
    end if;
  exception
    when exclusion_violation then
      raise exception 'AS019_SLOT_ALREADY_BOOKED: That slot was just booked by someone else. This request stays requested.'
        using errcode = 'P0001';
  end;
end;
$$;

revoke all on function public.transition_request_status(uuid, public.request_status, text) from public, anon;
grant execute on function public.transition_request_status(uuid, public.request_status, text) to authenticated;

-- ---------------------------------------------------------------------------
-- Thin wrappers: same names, same parameters, same "returns requests"
-- shape every existing caller already gets -- all logic now lives in
-- transition_request_status.
-- ---------------------------------------------------------------------------
create or replace function public.doctor_respond_request(p_request_id uuid, p_accept boolean, p_reason text default null)
returns public.requests
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_result public.requests;
begin
  perform public.transition_request_status(
    p_request_id,
    (case when p_accept then 'accepted' else 'rejected' end)::public.request_status,
    p_reason
  );
  select * into v_result from public.requests where id = p_request_id;
  return v_result;
end;
$$;

create or replace function public.doctor_confirm_request(p_request_id uuid)
returns public.requests
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_result public.requests;
begin
  perform public.transition_request_status(p_request_id, 'confirmed'::public.request_status, null);
  select * into v_result from public.requests where id = p_request_id;
  return v_result;
end;
$$;

create or replace function public.doctor_complete_request(p_request_id uuid)
returns public.requests
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_result public.requests;
begin
  perform public.transition_request_status(p_request_id, 'completed'::public.request_status, null);
  select * into v_result from public.requests where id = p_request_id;
  return v_result;
end;
$$;

create or replace function public.cancel_request(p_request_id uuid, p_reason text default null)
returns public.requests
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_result public.requests;
begin
  perform public.transition_request_status(p_request_id, 'cancelled'::public.request_status, p_reason);
  select * into v_result from public.requests where id = p_request_id;
  return v_result;
end;
$$;

-- ---------------------------------------------------------------------------
-- Notifications: mark_notification_read's return type changes (row -> row
-- count) per this prompt's explicit spec. Checked first: both apps discard
-- its return value entirely, so this is safe -- but Postgres still
-- requires DROP before a return-type change, which also clears the
-- existing grants, reapplied below.
-- ---------------------------------------------------------------------------
drop function if exists public.mark_notification_read(uuid);

create function public.mark_notification_read(p_notification_id uuid)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_count integer;
begin
  if auth.uid() is null then
    raise exception 'AS000_NOT_AUTHENTICATED: You must be signed in.' using errcode = 'P0001';
  end if;

  update public.notifications
  set read_at = now()
  where id = p_notification_id and user_id = auth.uid() and read_at is null;

  get diagnostics v_count = row_count;
  return v_count;
end;
$$;

revoke all on function public.mark_notification_read(uuid) from public, anon;
grant execute on function public.mark_notification_read(uuid) to authenticated;

create or replace function public.mark_all_notifications_read()
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_count integer;
begin
  if auth.uid() is null then
    raise exception 'AS000_NOT_AUTHENTICATED: You must be signed in.' using errcode = 'P0001';
  end if;

  update public.notifications
  set read_at = now()
  where user_id = auth.uid() and read_at is null;

  get diagnostics v_count = row_count;
  return v_count;
end;
$$;

revoke all on function public.mark_all_notifications_read() from public, anon;
grant execute on function public.mark_all_notifications_read() to authenticated;

create or replace function public.get_unread_notification_count()
returns integer
language plpgsql
security definer
set search_path = ''
as $$
begin
  if auth.uid() is null then
    raise exception 'AS000_NOT_AUTHENTICATED: You must be signed in.' using errcode = 'P0001';
  end if;

  return (select count(*)::integer from public.notifications where user_id = auth.uid() and read_at is null);
end;
$$;

revoke all on function public.get_unread_notification_count() from public, anon;
grant execute on function public.get_unread_notification_count() to authenticated;

commit;
