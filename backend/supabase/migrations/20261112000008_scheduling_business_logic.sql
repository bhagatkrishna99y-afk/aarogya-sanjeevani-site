-- Scheduling business logic: slot lookup, request creation, and two
-- read-convenience views. Purely additive -- per an explicit decision
-- (asked via AskUserQuestion because this is a real behavioral fork, not a
-- naming one): the existing get_available_slots(doctor,service,date) and
-- create_request(service,doctor,start,notes) RPCs, which the Patient app
-- already calls by name, are left completely untouched. This migration adds
-- new, independent RPCs (get_doctor_open_slots, create_service_request)
-- that are stricter and more complete (accepting_requests flag, suspended
-- checks, patient-role check, mode/contact/address support, duplicate
-- detection, sanitization, stable error codes, a patient notification the
-- old path never sent). The two old RPCs are not called from here and not
-- modified; migrating the Patient app onto the new ones is a separate task.
--
-- DECISION (availability timezone): doctor_weekly_availability.start_time/
-- end_time are plain `time` values with no timezone attached, and
-- set_weekly_availability() (20261111120008_rpc_functions.sql) stores
-- whatever a doctor types verbatim, with zero conversion -- i.e. a doctor
-- types their own local (Asia/Kolkata) business hours and that's what lands
-- in the column. The existing get_available_slots interprets those same
-- naive values as literal UTC clock time (`... at time zone 'UTC'`), which
-- appears to predate full correctness here; since this migration does not
-- touch that function, that's noted but out of scope. get_doctor_open_slots
-- below does what this prompt explicitly asks for and what the data model
-- implies is correct: naive local times are interpreted as Asia/Kolkata
-- wall-clock (`... at time zone 'Asia/Kolkata'`).
--
-- DECISION (slot-blocking statuses): the prompt is explicit --
-- "Exclude any slot that overlaps an existing request ... with status
-- accepted or confirmed" -- so, unlike get_available_slots (which also
-- blocks on 'requested'), get_doctor_open_slots only excludes
-- accepted/confirmed overlaps. This matches requests_no_doctor_overlap's
-- own EXCLUDE constraint scope exactly (20261112000006), so multiple
-- patients can still see and request the same open slot; the doctor
-- resolves the conflict by accepting one, which the EXCLUDE constraint and
-- this function will then correctly treat as blocking for everyone else.
--
-- DECISION (no-doctor services still need a scheduled_start/end):
-- requests.scheduled_start/scheduled_end are NOT NULL at the schema level,
-- so "the request is created without them" (doctor and slot) is read as
-- "without a doctor and without a doctor-availability-validated slot" --
-- p_slot_start is still required and becomes scheduled_start directly
-- (subject to the same min-lead-time/max-horizon checks as the doctor
-- path), with scheduled_end derived from the service's duration_minutes.
--
-- DECISION (settings as functions, not a table): booking_settings() below
-- is the "constant ... defined in a settings table or function" the prompt
-- asks for -- a function, not a new mutable table, since these values
-- aren't meant to be edited by any role today and a table would need its
-- own RLS/grant surface for no real benefit yet.

begin;

-- ---------------------------------------------------------------------------
-- Settings
-- ---------------------------------------------------------------------------
create or replace function public.booking_settings()
returns table (max_horizon_days integer, min_lead_minutes integer)
language plpgsql
security definer
set search_path = ''
as $$
begin
  if auth.uid() is null then
    raise exception 'AS000_NOT_AUTHENTICATED: You must be signed in.' using errcode = 'P0001';
  end if;

  return query select 60, 60;
end;
$$;

revoke all on function public.booking_settings() from public, anon;
grant execute on function public.booking_settings() to authenticated;

-- ---------------------------------------------------------------------------
-- get_doctor_open_slots
-- ---------------------------------------------------------------------------
create or replace function public.get_doctor_open_slots(p_doctor_id uuid, p_date date)
returns table (slot_start timestamptz, slot_end timestamptz)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_today date;
  v_max_horizon_days integer;
  v_min_lead_minutes integer;
  v_day_of_week smallint;
begin
  if auth.uid() is null then
    raise exception 'AS000_NOT_AUTHENTICATED: You must be signed in to view available slots.' using errcode = 'P0001';
  end if;

  select max_horizon_days, min_lead_minutes into v_max_horizon_days, v_min_lead_minutes
  from public.booking_settings();

  v_today := (now() at time zone 'Asia/Kolkata')::date;

  if p_date < v_today then
    raise exception 'AS003_INVALID_DATE: Please choose a date that is today or later.' using errcode = 'P0001';
  end if;

  if p_date > v_today + v_max_horizon_days then
    raise exception 'AS003_INVALID_DATE: Please choose a date within the next % days.', v_max_horizon_days
      using errcode = 'P0001';
  end if;

  -- An unavailable doctor is a valid (if empty) answer, not an error --
  -- this is a browsing query, not a write that must reject bad input.
  if not exists (
    select 1
    from public.doctor_profiles dp
    join public.profiles pr on pr.id = dp.user_id
    where dp.user_id = p_doctor_id
      and dp.status = 'approved'
      and dp.accepting_requests
      and not pr.is_suspended
  ) then
    return;
  end if;

  v_day_of_week := extract(dow from p_date);

  return query
  with windows as (
    select start_time, end_time, slot_duration_minutes
    from public.doctor_weekly_availability
    where doctor_id = p_doctor_id
      and day_of_week = v_day_of_week
      and is_active
  ),
  candidates as (
    select
      w.end_time as window_end,
      w.slot_duration_minutes,
      ((p_date + w.start_time) + make_interval(mins => n * w.slot_duration_minutes)) as candidate_start_local
    from windows w,
      lateral generate_series(0, greatest(
        0,
        (extract(epoch from (w.end_time - w.start_time)) / 60 / w.slot_duration_minutes)::integer - 1
      )) as n
  )
  select
    (c.candidate_start_local at time zone 'Asia/Kolkata') as slot_start,
    (c.candidate_start_local at time zone 'Asia/Kolkata') + make_interval(mins => c.slot_duration_minutes) as slot_end
  from candidates c
  where (c.candidate_start_local::time + make_interval(mins => c.slot_duration_minutes)) <= c.window_end
    and (c.candidate_start_local at time zone 'Asia/Kolkata') >= now() + make_interval(mins => v_min_lead_minutes)
    and not exists (
      select 1 from public.requests r
      where r.doctor_id = p_doctor_id
        and r.status in ('accepted', 'confirmed')
        and tstzrange(r.scheduled_start, r.scheduled_end, '[)')
            && tstzrange(
                 (c.candidate_start_local at time zone 'Asia/Kolkata'),
                 (c.candidate_start_local at time zone 'Asia/Kolkata') + make_interval(mins => c.slot_duration_minutes),
                 '[)'
               )
    )
  order by slot_start;
end;
$$;

revoke all on function public.get_doctor_open_slots(uuid, date) from public, anon;
grant execute on function public.get_doctor_open_slots(uuid, date) to authenticated;

-- ---------------------------------------------------------------------------
-- create_service_request
-- ---------------------------------------------------------------------------
create or replace function public.create_service_request(
  p_service_id uuid,
  p_doctor_id uuid,
  p_slot_start timestamptz,
  p_mode public.service_mode,
  p_notes text,
  p_contact_phone text,
  p_address text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_caller_id uuid;
  v_caller_role public.user_role;
  v_caller_suspended boolean;
  v_service public.services;
  v_category_active boolean;
  v_doctor public.doctor_profiles;
  v_doctor_suspended boolean;
  v_slot_end timestamptz;
  v_notes text;
  v_address text;
  v_phone text;
  v_request_id uuid;
  v_service_name text;
  v_slot_matches boolean;
  v_max_horizon_days integer;
  v_min_lead_minutes integer;
begin
  v_caller_id := auth.uid();
  if v_caller_id is null then
    raise exception 'AS000_NOT_AUTHENTICATED: You must be signed in to submit a request.' using errcode = 'P0001';
  end if;

  select role, is_suspended into v_caller_role, v_caller_suspended
  from public.profiles where id = v_caller_id;

  if v_caller_role is distinct from 'user' then
    raise exception 'AS004_ROLE_NOT_PATIENT: Only patient accounts can submit service requests.' using errcode = 'P0001';
  end if;

  if v_caller_suspended then
    raise exception 'AS005_ACCOUNT_SUSPENDED: Your account is suspended and cannot submit requests.' using errcode = 'P0001';
  end if;

  select * into v_service from public.services where id = p_service_id;
  if v_service.id is null or not v_service.is_active then
    raise exception 'AS006_SERVICE_UNAVAILABLE: The selected service is not available.' using errcode = 'P0001';
  end if;

  select is_active into v_category_active
  from public.service_categories where id = v_service.category_id;
  if v_category_active is distinct from true then
    raise exception 'AS006_SERVICE_UNAVAILABLE: The selected service is not available.' using errcode = 'P0001';
  end if;

  -- Sanitize and length-limit free text before anything else touches it.
  v_notes := nullif(btrim(p_notes), '');
  v_address := nullif(btrim(p_address), '');
  v_phone := nullif(btrim(p_contact_phone), '');

  if v_notes is not null and char_length(v_notes) > 1000 then
    raise exception 'AS013_TEXT_TOO_LONG: Notes must be 1000 characters or fewer.' using errcode = 'P0001';
  end if;
  if v_address is not null and char_length(v_address) > 500 then
    raise exception 'AS013_TEXT_TOO_LONG: Address must be 500 characters or fewer.' using errcode = 'P0001';
  end if;
  if v_phone is not null and v_phone !~ '^\+?[0-9 ()-]{7,20}$' then
    raise exception 'AS012_INVALID_CONTACT_PHONE: Please provide a valid contact phone number.' using errcode = 'P0001';
  end if;

  if p_mode = 'home_visit' and v_address is null then
    raise exception 'AS011_ADDRESS_REQUIRED: An address is required for home visits.' using errcode = 'P0001';
  end if;

  select max_horizon_days, min_lead_minutes into v_max_horizon_days, v_min_lead_minutes
  from public.booking_settings();

  if v_service.requires_doctor then
    if p_doctor_id is null then
      raise exception 'AS007_DOCTOR_REQUIRED: Please choose a doctor for this service.' using errcode = 'P0001';
    end if;

    if v_phone is null then
      raise exception 'AS012_INVALID_CONTACT_PHONE: A contact phone number is required so the doctor can reach you.'
        using errcode = 'P0001';
    end if;

    select * into v_doctor from public.doctor_profiles where user_id = p_doctor_id;
    select is_suspended into v_doctor_suspended from public.profiles where id = p_doctor_id;

    if v_doctor.user_id is null
       or v_doctor.status <> 'approved'
       or not v_doctor.accepting_requests
       or coalesce(v_doctor_suspended, true)
    then
      raise exception 'AS002_DOCTOR_NOT_AVAILABLE: This doctor is not currently accepting requests.' using errcode = 'P0001';
    end if;

    if not exists (
      select 1 from public.doctor_services where doctor_id = p_doctor_id and service_id = p_service_id
    ) then
      raise exception 'AS008_DOCTOR_NOT_OFFERED: This doctor does not offer the selected service.' using errcode = 'P0001';
    end if;

    if not (p_mode = any(v_service.supported_modes)) or not (p_mode = any(v_doctor.consultation_modes)) then
      raise exception 'AS009_MODE_UNSUPPORTED: The selected consultation mode is not supported.' using errcode = 'P0001';
    end if;

    if p_slot_start is null then
      raise exception 'AS001_SLOT_UNAVAILABLE: Please choose a time slot.' using errcode = 'P0001';
    end if;

    select exists (
      select 1 from public.get_doctor_open_slots(p_doctor_id, (p_slot_start at time zone 'Asia/Kolkata')::date) s
      where s.slot_start = p_slot_start
    ) into v_slot_matches;

    if not v_slot_matches then
      raise exception 'AS001_SLOT_UNAVAILABLE: That slot is no longer available. Please choose another.' using errcode = 'P0001';
    end if;

    if exists (
      select 1 from public.requests
      where patient_id = v_caller_id
        and doctor_id = p_doctor_id
        and scheduled_start = p_slot_start
        and status in ('requested', 'accepted', 'confirmed')
    ) then
      raise exception 'AS010_DUPLICATE_REQUEST: You already have an active request with this doctor at that time.'
        using errcode = 'P0001';
    end if;

    v_slot_end := p_slot_start + make_interval(mins => v_service.duration_minutes);
  else
    if p_doctor_id is not null then
      raise exception 'AS014_DOCTOR_NOT_ALLOWED: This service does not require selecting a doctor.' using errcode = 'P0001';
    end if;

    if p_slot_start is null then
      raise exception 'AS001_SLOT_UNAVAILABLE: Please choose a time.' using errcode = 'P0001';
    end if;

    if p_slot_start < now() + make_interval(mins => v_min_lead_minutes) then
      raise exception 'AS003_INVALID_DATE: Please choose a time at least % minutes from now.', v_min_lead_minutes
        using errcode = 'P0001';
    end if;

    if (p_slot_start at time zone 'Asia/Kolkata')::date > (now() at time zone 'Asia/Kolkata')::date + v_max_horizon_days then
      raise exception 'AS003_INVALID_DATE: Please choose a time within the next % days.', v_max_horizon_days
        using errcode = 'P0001';
    end if;

    if not (p_mode = any(v_service.supported_modes)) then
      raise exception 'AS009_MODE_UNSUPPORTED: The selected consultation mode is not supported for this service.'
        using errcode = 'P0001';
    end if;

    v_slot_end := p_slot_start + make_interval(mins => v_service.duration_minutes);
  end if;

  begin
    insert into public.requests (
      patient_id, doctor_id, service_id, scheduled_start, scheduled_end,
      mode, status, patient_notes, contact_phone, address_text
    ) values (
      v_caller_id, p_doctor_id, p_service_id, p_slot_start, v_slot_end,
      p_mode, 'requested', v_notes, v_phone, v_address
    )
    returning id into v_request_id;
  exception
    when exclusion_violation then
      raise exception 'AS001_SLOT_UNAVAILABLE: That slot was just booked by someone else. Please choose another.'
        using errcode = 'P0001';
    when unique_violation then
      raise exception 'AS010_DUPLICATE_REQUEST: You already have an active request with this doctor at that time.'
        using errcode = 'P0001';
  end;

  -- The requests_log_created trigger (20261111120006) already wrote the
  -- first request_status_history row and, when doctor_id is set, the
  -- doctor's "new request" notification. Only the patient's own
  -- confirmation notification is missing from that path -- add it here.
  select name into v_service_name from public.services where id = p_service_id;

  insert into public.notifications (user_id, type, title, body, related_request_id)
  values (
    v_caller_id,
    'request_created',
    'Request submitted',
    format('Your request for %s has been submitted.', coalesce(v_service_name, 'a service')),
    v_request_id
  );

  return v_request_id;
end;
$$;

revoke all on function public.create_service_request(
  uuid, uuid, timestamptz, public.service_mode, text, text, text
) from public, anon;
grant execute on function public.create_service_request(
  uuid, uuid, timestamptz, public.service_mode, text, text, text
) to authenticated;

-- ---------------------------------------------------------------------------
-- patient_requests_view: a patient's own requests, with service name and
-- the doctor's PUBLIC name/specialization (never license_number etc. --
-- sourced from doctors_public, the already-vetted column list).
-- security_invoker = true: relies entirely on requests' own RLS (patient
-- sees own rows, doctor sees assigned rows, admin sees all) plus
-- doctors_public's own safe, public filter -- no bypass needed.
-- ---------------------------------------------------------------------------
create or replace view public.patient_requests_view
with (security_invoker = true) as
select
  r.id,
  r.patient_id,
  r.doctor_id,
  r.service_id,
  s.name as service_name,
  dpub.full_name as doctor_full_name,
  dpub.specialization as doctor_specialization,
  r.scheduled_start,
  r.scheduled_end,
  r.mode,
  r.status,
  r.patient_notes,
  r.doctor_response_note,
  r.contact_phone,
  r.address_text,
  r.rejection_reason,
  r.cancellation_reason,
  r.created_at,
  r.updated_at
from public.requests r
join public.services s on s.id = r.service_id
left join public.doctors_public dpub on dpub.doctor_id = r.doctor_id;

revoke all on public.patient_requests_view from public, anon, authenticated;
grant select on public.patient_requests_view to authenticated;

-- ---------------------------------------------------------------------------
-- doctor_requests_view: a doctor's own assigned requests, with service name
-- and the patient's full name only. contact_phone comes from
-- doctor_request_contacts (already NULL-masked unless accepted/confirmed)
-- rather than requests.contact_phone directly, so the masking rule lives in
-- exactly one place. security_invoker = true: requests RLS already scopes
-- rows to the calling doctor (or admin); doctor_patient_contacts and
-- doctor_request_contacts are each independently scoped to
-- "auth.uid() = doctor_id" by their own definitions (20261112000007), so
-- this view only resolves rows for the signed-in doctor, not for admin --
-- an intentional scope for a doctor-facing convenience view.
-- ---------------------------------------------------------------------------
create or replace view public.doctor_requests_view
with (security_invoker = true) as
select
  r.id,
  r.patient_id,
  dpc.full_name as patient_full_name,
  r.service_id,
  s.name as service_name,
  r.doctor_id,
  r.scheduled_start,
  r.scheduled_end,
  r.mode,
  r.status,
  r.patient_notes,
  r.doctor_response_note,
  drc.contact_phone,
  r.address_text,
  r.created_at,
  r.updated_at
from public.requests r
join public.services s on s.id = r.service_id
left join public.doctor_patient_contacts dpc on dpc.patient_id = r.patient_id
left join public.doctor_request_contacts drc on drc.request_id = r.id
where r.doctor_id = (select auth.uid());

revoke all on public.doctor_requests_view from public, anon, authenticated;
grant select on public.doctor_requests_view to authenticated;

commit;
