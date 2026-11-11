-- Business-logic RPCs. All are SECURITY DEFINER with an empty search_path
-- and schema-qualified references. Each function performs its own
-- authorization check before touching data -- RLS/grants on the underlying
-- tables intentionally block direct writes, so these functions are the only
-- way those rows can change. Error messages are written to be shown directly
-- to end users (rule: never leak internal database errors).

-- ---------------------------------------------------------------------------
-- Doctor application (registration + resubmission after rejection)
-- ---------------------------------------------------------------------------
create function public.submit_doctor_application(
  p_full_name text,
  p_phone text,
  p_specialization text,
  p_license_number text,
  p_bio text,
  p_years_experience integer
)
returns public.doctor_profiles
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_existing public.doctor_profiles;
  v_result public.doctor_profiles;
begin
  if auth.uid() is null then
    raise exception 'You must be signed in to apply.' using errcode = 'P0001';
  end if;

  if coalesce(trim(p_full_name), '') = '' then
    raise exception 'Full name is required.' using errcode = 'P0001';
  end if;
  if coalesce(trim(p_specialization), '') = '' then
    raise exception 'Specialization is required.' using errcode = 'P0001';
  end if;
  if coalesce(trim(p_license_number), '') = '' then
    raise exception 'A valid license number is required.' using errcode = 'P0001';
  end if;
  if coalesce(p_years_experience, -1) < 0 then
    raise exception 'Years of experience must be zero or more.' using errcode = 'P0001';
  end if;

  select * into v_existing from public.doctor_profiles where user_id = auth.uid();

  if v_existing.user_id is not null and v_existing.status in ('pending', 'approved', 'suspended') then
    raise exception 'You already have an active or pending doctor application.' using errcode = 'P0001';
  end if;

  update public.profiles
  set full_name = p_full_name, phone = coalesce(p_phone, phone)
  where id = auth.uid();

  if v_existing.user_id is not null then
    update public.doctor_profiles
    set specialization = p_specialization,
        license_number = p_license_number,
        bio = p_bio,
        years_experience = p_years_experience,
        status = 'pending',
        rejection_reason = null,
        reviewed_by = null,
        reviewed_at = null
    where user_id = auth.uid()
    returning * into v_result;
  else
    insert into public.doctor_profiles (
      user_id, specialization, license_number, bio, years_experience, status
    ) values (
      auth.uid(), p_specialization, p_license_number, p_bio, p_years_experience, 'pending'
    )
    returning * into v_result;
  end if;

  return v_result;
end;
$$;

-- ---------------------------------------------------------------------------
-- Admin: doctor status lifecycle (the ONLY place role can become 'doctor')
-- ---------------------------------------------------------------------------
create function public.admin_set_doctor_status(
  p_doctor_id uuid,
  p_new_status public.doctor_status,
  p_reason text default null
)
returns public.doctor_profiles
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_current public.doctor_profiles;
  v_result public.doctor_profiles;
begin
  if not public.is_admin() then
    raise exception 'You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  select * into v_current from public.doctor_profiles where user_id = p_doctor_id;
  if v_current.user_id is null then
    raise exception 'Doctor application not found.' using errcode = 'P0001';
  end if;

  if (v_current.status, p_new_status) not in (
    ('pending', 'approved'),
    ('pending', 'rejected'),
    ('approved', 'suspended'),
    ('suspended', 'approved'),
    ('rejected', 'approved')
  ) then
    raise exception 'That status change is not allowed from the current state.' using errcode = 'P0001';
  end if;

  if p_new_status = 'rejected' and coalesce(trim(p_reason), '') = '' then
    raise exception 'A reason is required when rejecting a doctor application.' using errcode = 'P0001';
  end if;

  update public.doctor_profiles
  set status = p_new_status,
      rejection_reason = case when p_new_status = 'rejected' then p_reason else null end,
      admin_notes = case when p_reason is not null and p_new_status <> 'rejected'
                          then coalesce(admin_notes || E'\n', '') || p_reason
                          else admin_notes end,
      reviewed_by = auth.uid(),
      reviewed_at = now()
  where user_id = p_doctor_id
  returning * into v_result;

  if p_new_status = 'approved' then
    update public.profiles set role = 'doctor' where id = p_doctor_id;
  end if;

  insert into public.notifications (user_id, type, title, body)
  values (
    p_doctor_id,
    'doctor_status_' || p_new_status::text,
    case p_new_status
      when 'approved' then 'Application approved'
      when 'rejected' then 'Application rejected'
      when 'suspended' then 'Account suspended'
      else 'Account reactivated'
    end,
    case p_new_status
      when 'approved' then 'Your doctor application has been approved. You can now configure your services and availability.'
      when 'rejected' then coalesce('Your application was declined: ' || p_reason, 'Your application was declined.')
      when 'suspended' then 'Your account has been suspended. Please contact support for details.'
      else 'Your account has been reactivated.'
    end
  );

  return v_result;
end;
$$;

-- ---------------------------------------------------------------------------
-- Admin: grant admin role to an existing user. Never self-service.
-- ---------------------------------------------------------------------------
create function public.admin_assign_admin_role(p_user_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception 'You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  update public.profiles set role = 'admin' where id = p_user_id;

  if not found then
    raise exception 'User not found.' using errcode = 'P0001';
  end if;
end;
$$;

-- ---------------------------------------------------------------------------
-- Public doctor directory (safe columns only -- never license/admin fields)
-- ---------------------------------------------------------------------------
create function public.list_approved_doctors(p_service_id uuid default null)
returns table (
  doctor_id uuid,
  full_name text,
  specialization text,
  bio text,
  years_experience integer
)
language sql
stable
security definer
set search_path = ''
as $$
  select p.id, p.full_name, dp.specialization, dp.bio, dp.years_experience
  from public.doctor_profiles dp
  join public.profiles p on p.id = dp.user_id
  where dp.status = 'approved'
    and (
      p_service_id is null
      or exists (
        select 1 from public.doctor_services ds
        where ds.doctor_id = dp.user_id and ds.service_id = p_service_id
      )
    )
  order by p.full_name;
$$;

create function public.get_doctor_public_profile(p_doctor_id uuid)
returns table (
  doctor_id uuid,
  full_name text,
  specialization text,
  bio text,
  years_experience integer
)
language sql
stable
security definer
set search_path = ''
as $$
  select p.id, p.full_name, dp.specialization, dp.bio, dp.years_experience
  from public.doctor_profiles dp
  join public.profiles p on p.id = dp.user_id
  where dp.status = 'approved' and dp.user_id = p_doctor_id;
$$;

revoke all on function public.list_approved_doctors(uuid) from public;
revoke all on function public.get_doctor_public_profile(uuid) from public;
grant execute on function public.list_approved_doctors(uuid) to anon, authenticated;
grant execute on function public.get_doctor_public_profile(uuid) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- Doctor: configure offered services
-- ---------------------------------------------------------------------------
create function public.set_doctor_services(p_service_ids uuid[])
returns setof public.doctor_services
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_approved_doctor(auth.uid()) then
    raise exception 'Only approved doctors can configure services.' using errcode = 'P0001';
  end if;

  delete from public.doctor_services
  where doctor_id = auth.uid()
    and (p_service_ids is null or service_id <> all (p_service_ids));

  insert into public.doctor_services (doctor_id, service_id)
  select auth.uid(), s.id
  from public.services s
  where s.id = any (p_service_ids)
    and s.requires_doctor
    and s.is_active
  on conflict (doctor_id, service_id) do nothing;

  return query select * from public.doctor_services where doctor_id = auth.uid();
end;
$$;

-- ---------------------------------------------------------------------------
-- Doctor: configure recurring weekly availability
-- p_schedule: jsonb array of {day_of_week, start_time, end_time, slot_duration_minutes}
-- ---------------------------------------------------------------------------
create function public.set_weekly_availability(p_schedule jsonb)
returns setof public.doctor_weekly_availability
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_item jsonb;
  v_day smallint;
  v_start time;
  v_end time;
  v_slot_minutes integer;
begin
  if not public.is_approved_doctor(auth.uid()) then
    raise exception 'Only approved doctors can configure availability.' using errcode = 'P0001';
  end if;

  if jsonb_typeof(p_schedule) <> 'array' then
    raise exception 'Schedule must be a list of availability windows.' using errcode = 'P0001';
  end if;

  for v_item in select * from jsonb_array_elements(p_schedule)
  loop
    v_day := (v_item ->> 'day_of_week')::smallint;
    v_start := (v_item ->> 'start_time')::time;
    v_end := (v_item ->> 'end_time')::time;
    v_slot_minutes := coalesce((v_item ->> 'slot_duration_minutes')::integer, 30);

    if v_day is null or v_day < 0 or v_day > 6 then
      raise exception 'Each availability window needs a valid day of week.' using errcode = 'P0001';
    end if;
    if v_start is null or v_end is null or v_end <= v_start then
      raise exception 'Each availability window needs a valid start and end time.' using errcode = 'P0001';
    end if;
    if v_slot_minutes <= 0 then
      raise exception 'Slot duration must be greater than zero.' using errcode = 'P0001';
    end if;
  end loop;

  delete from public.doctor_weekly_availability where doctor_id = auth.uid();

  insert into public.doctor_weekly_availability (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes)
  select
    auth.uid(),
    (item ->> 'day_of_week')::smallint,
    (item ->> 'start_time')::time,
    (item ->> 'end_time')::time,
    coalesce((item ->> 'slot_duration_minutes')::integer, 30)
  from jsonb_array_elements(p_schedule) as item;

  return query select * from public.doctor_weekly_availability where doctor_id = auth.uid();
end;
$$;

-- ---------------------------------------------------------------------------
-- Patient: compute open slots for a doctor/service/date
-- ---------------------------------------------------------------------------
create function public.get_available_slots(
  p_doctor_id uuid,
  p_service_id uuid,
  p_date date
)
returns table (slot_start timestamptz, slot_end timestamptz)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_duration_minutes integer;
  v_day_of_week smallint;
begin
  if not public.is_approved_doctor(p_doctor_id) then
    raise exception 'This doctor is not currently accepting requests.' using errcode = 'P0001';
  end if;

  if not exists (select 1 from public.doctor_services where doctor_id = p_doctor_id and service_id = p_service_id) then
    raise exception 'This doctor does not offer the selected service.' using errcode = 'P0001';
  end if;

  select duration_minutes into v_duration_minutes from public.services where id = p_service_id and is_active;
  if v_duration_minutes is null then
    raise exception 'The selected service is not available.' using errcode = 'P0001';
  end if;

  if p_date < current_date then
    raise exception 'Please choose a date that is today or later.' using errcode = 'P0001';
  end if;

  v_day_of_week := extract(dow from p_date);

  return query
  with windows as (
    select start_time, end_time, slot_duration_minutes
    from public.doctor_weekly_availability
    where doctor_id = p_doctor_id and day_of_week = v_day_of_week and is_active
  ),
  candidates as (
    select
      w.end_time as window_end,
      (p_date + w.start_time) + (make_interval(mins => n * w.slot_duration_minutes)) as candidate_start
    from windows w,
      lateral generate_series(0, greatest(
        0,
        (extract(epoch from (w.end_time - w.start_time)) / 60 / w.slot_duration_minutes)::integer - 1
      )) as n
  )
  select
    c.candidate_start as slot_start,
    c.candidate_start + make_interval(mins => v_duration_minutes) as slot_end
  from candidates c
  where c.candidate_start > now()
    and (c.candidate_start::time + make_interval(mins => v_duration_minutes)) <= c.window_end
    and not exists (
      select 1 from public.requests r
      where r.doctor_id = p_doctor_id
        and r.status in ('requested', 'accepted', 'confirmed')
        and tstzrange(r.scheduled_start, r.scheduled_end, '[)')
            && tstzrange(c.candidate_start, c.candidate_start + make_interval(mins => v_duration_minutes), '[)')
    )
  order by slot_start;
end;
$$;

revoke all on function public.get_available_slots(uuid, uuid, date) from public;
grant execute on function public.get_available_slots(uuid, uuid, date) to authenticated;

-- ---------------------------------------------------------------------------
-- Patient: create a request
-- ---------------------------------------------------------------------------
create function public.create_request(
  p_service_id uuid,
  p_doctor_id uuid,
  p_scheduled_start timestamptz,
  p_patient_notes text default null
)
returns public.requests
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_service public.services;
  v_scheduled_end timestamptz;
  v_result public.requests;
begin
  if auth.uid() is null then
    raise exception 'You must be signed in to submit a request.' using errcode = 'P0001';
  end if;

  select * into v_service from public.services where id = p_service_id and is_active;
  if v_service.id is null then
    raise exception 'The selected service is not available.' using errcode = 'P0001';
  end if;

  if v_service.requires_doctor then
    if p_doctor_id is null then
      raise exception 'Please choose a doctor for this service.' using errcode = 'P0001';
    end if;
    if not public.is_approved_doctor(p_doctor_id) then
      raise exception 'This doctor is not currently accepting requests.' using errcode = 'P0001';
    end if;
    if not exists (select 1 from public.doctor_services where doctor_id = p_doctor_id and service_id = p_service_id) then
      raise exception 'This doctor does not offer the selected service.' using errcode = 'P0001';
    end if;
  elsif p_doctor_id is not null then
    raise exception 'This service does not require selecting a doctor.' using errcode = 'P0001';
  end if;

  if p_scheduled_start <= now() then
    raise exception 'Please choose a time in the future.' using errcode = 'P0001';
  end if;

  v_scheduled_end := p_scheduled_start + make_interval(mins => v_service.duration_minutes);

  begin
    insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, patient_notes)
    values (auth.uid(), p_doctor_id, p_service_id, p_scheduled_start, v_scheduled_end, p_patient_notes)
    returning * into v_result;
  exception
    when exclusion_violation then
      raise exception 'That slot was just booked by someone else. Please pick another slot.' using errcode = 'P0001';
  end;

  return v_result;
end;
$$;

-- ---------------------------------------------------------------------------
-- Doctor: accept or reject a pending request
-- ---------------------------------------------------------------------------
create function public.doctor_respond_request(
  p_request_id uuid,
  p_accept boolean,
  p_reason text default null
)
returns public.requests
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_request public.requests;
  v_result public.requests;
begin
  select * into v_request from public.requests where id = p_request_id;
  if v_request.id is null then
    raise exception 'Request not found.' using errcode = 'P0001';
  end if;
  if v_request.doctor_id is distinct from auth.uid() then
    raise exception 'You are not authorized to respond to this request.' using errcode = 'P0001';
  end if;
  if v_request.status <> 'requested' then
    raise exception 'This request has already been responded to.' using errcode = 'P0001';
  end if;
  if not p_accept and coalesce(trim(p_reason), '') = '' then
    raise exception 'A reason is required when declining a request.' using errcode = 'P0001';
  end if;

  update public.requests
  set status = case when p_accept then 'accepted' else 'rejected' end,
      doctor_response_note = p_reason
  where id = p_request_id
  returning * into v_result;

  return v_result;
end;
$$;

-- ---------------------------------------------------------------------------
-- Doctor: confirm an accepted request
-- ---------------------------------------------------------------------------
create function public.doctor_confirm_request(p_request_id uuid)
returns public.requests
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_request public.requests;
  v_result public.requests;
begin
  select * into v_request from public.requests where id = p_request_id;
  if v_request.id is null then
    raise exception 'Request not found.' using errcode = 'P0001';
  end if;
  if v_request.doctor_id is distinct from auth.uid() then
    raise exception 'You are not authorized to confirm this request.' using errcode = 'P0001';
  end if;
  if v_request.status <> 'accepted' then
    raise exception 'Only accepted requests can be confirmed.' using errcode = 'P0001';
  end if;

  update public.requests set status = 'confirmed' where id = p_request_id returning * into v_result;
  return v_result;
end;
$$;

-- ---------------------------------------------------------------------------
-- Doctor: complete a confirmed request
-- ---------------------------------------------------------------------------
create function public.doctor_complete_request(p_request_id uuid)
returns public.requests
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_request public.requests;
  v_result public.requests;
begin
  select * into v_request from public.requests where id = p_request_id;
  if v_request.id is null then
    raise exception 'Request not found.' using errcode = 'P0001';
  end if;
  if v_request.doctor_id is distinct from auth.uid() then
    raise exception 'You are not authorized to complete this request.' using errcode = 'P0001';
  end if;
  if v_request.status <> 'confirmed' then
    raise exception 'Only confirmed requests can be completed.' using errcode = 'P0001';
  end if;

  update public.requests set status = 'completed' where id = p_request_id returning * into v_result;
  return v_result;
end;
$$;

-- ---------------------------------------------------------------------------
-- Patient or doctor: cancel a request while it is still cancellable
-- ---------------------------------------------------------------------------
create function public.cancel_request(p_request_id uuid, p_reason text default null)
returns public.requests
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_request public.requests;
  v_result public.requests;
begin
  select * into v_request from public.requests where id = p_request_id;
  if v_request.id is null then
    raise exception 'Request not found.' using errcode = 'P0001';
  end if;
  if v_request.patient_id <> auth.uid() and v_request.doctor_id is distinct from auth.uid() and not public.is_admin() then
    raise exception 'You are not authorized to cancel this request.' using errcode = 'P0001';
  end if;
  if v_request.status not in ('requested', 'accepted', 'confirmed') then
    raise exception 'This request can no longer be cancelled.' using errcode = 'P0001';
  end if;

  update public.requests
  set status = 'cancelled',
      doctor_response_note = coalesce(p_reason, doctor_response_note)
  where id = p_request_id
  returning * into v_result;

  return v_result;
end;
$$;

-- ---------------------------------------------------------------------------
-- Any user: mark their own notification as read
-- ---------------------------------------------------------------------------
create function public.mark_notification_read(p_notification_id uuid)
returns public.notifications
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_result public.notifications;
begin
  update public.notifications
  set read_at = now()
  where id = p_notification_id and user_id = auth.uid() and read_at is null
  returning * into v_result;

  if v_result.id is null then
    raise exception 'Notification not found.' using errcode = 'P0001';
  end if;

  return v_result;
end;
$$;

-- ---------------------------------------------------------------------------
-- Execute grants: every write-capable function is restricted to authenticated.
-- ---------------------------------------------------------------------------
revoke all on function public.submit_doctor_application(text, text, text, text, text, integer) from public;
revoke all on function public.admin_set_doctor_status(uuid, public.doctor_status, text) from public;
revoke all on function public.admin_assign_admin_role(uuid) from public;
revoke all on function public.set_doctor_services(uuid[]) from public;
revoke all on function public.set_weekly_availability(jsonb) from public;
revoke all on function public.create_request(uuid, uuid, timestamptz, text) from public;
revoke all on function public.doctor_respond_request(uuid, boolean, text) from public;
revoke all on function public.doctor_confirm_request(uuid) from public;
revoke all on function public.doctor_complete_request(uuid) from public;
revoke all on function public.cancel_request(uuid, text) from public;
revoke all on function public.mark_notification_read(uuid) from public;

grant execute on function public.submit_doctor_application(text, text, text, text, text, integer) to authenticated;
grant execute on function public.admin_set_doctor_status(uuid, public.doctor_status, text) to authenticated;
grant execute on function public.admin_assign_admin_role(uuid) to authenticated;
grant execute on function public.set_doctor_services(uuid[]) to authenticated;
grant execute on function public.set_weekly_availability(jsonb) to authenticated;
grant execute on function public.create_request(uuid, uuid, timestamptz, text) to authenticated;
grant execute on function public.doctor_respond_request(uuid, boolean, text) to authenticated;
grant execute on function public.doctor_confirm_request(uuid) to authenticated;
grant execute on function public.doctor_complete_request(uuid) to authenticated;
grant execute on function public.cancel_request(uuid, text) to authenticated;
grant execute on function public.mark_notification_read(uuid) to authenticated;
