-- Doctor-side profile/service/availability management RPCs.
--
-- DECISION (submit_doctor_profile, additive): submit_doctor_application
-- (20261111120008) already exists, is called by the Doctor app by this
-- exact name, and has a materially different signature (collects
-- full_name/phone for profiles, nothing about title/qualifications/
-- consultation_modes/slot_duration_minutes). It also cannot do what this
-- prompt requires for an approved/suspended doctor (it hard-rejects any
-- resubmission once status is anything but a fresh row). Rather than
-- reshape that function's signature (breaking the Doctor app's current
-- call), submit_doctor_profile ships as a new, additive function with
-- exactly the signature this prompt asks for; submit_doctor_application
-- is untouched. Migrating the Doctor app's initial-application screen
-- onto the richer function is a separate task.
--
-- DOCUMENTED RULE (submit_doctor_profile re-review trigger): changing
-- license_number or specialization resets status to 'pending' for both
-- 'approved' AND 'suspended' doctors -- the prompt's wording ties the rule
-- to "an approved doctor" specifically, but a suspended doctor is already
-- in a flagged state, and silently letting them change their most
-- sensitive credential fields without a fresh admin look seemed like the
-- wrong default. Editing any other field (title, qualifications, bio,
-- years, consultation_modes, slot_duration_minutes) never changes status.
-- Every status flip this function makes is written to audit_log with
-- actor_id = the doctor themselves, so admins have a trail of doctor-
-- initiated changes alongside admin-initiated ones.
--
-- DECISION (set_doctor_availability, additive): same situation as
-- set_doctor_availability vs. the existing set_weekly_availability
-- (20261111120008) -- same shape (a single jsonb schedule), different
-- name the prompt asked for, Doctor app already calls the old name. New,
-- additive function with the fuller validation (overlap rejection within
-- the submitted batch, window-vs-slot-duration check, stable error
-- codes); set_weekly_availability untouched.
--
-- set_doctor_services already matches this prompt's spec exactly (same
-- name, same signature, already atomic, already requires_doctor + active
-- only) -- not touched in this migration.

begin;

-- ---------------------------------------------------------------------------
-- log_audit: the one place that writes to audit_log, so every caller
-- (doctor-initiated changes here; admin-initiated changes in
-- 20261112000011) writes rows shaped the same way. Internal plumbing
-- only -- never granted to any role; a SECURITY DEFINER function calling
-- it runs as the owner regardless of grants, which is the only way it's
-- meant to be invoked.
-- ---------------------------------------------------------------------------
create or replace function public.log_audit(
  p_action text,
  p_entity_type text,
  p_entity_id uuid,
  p_old_data jsonb default null,
  p_new_data jsonb default null
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.audit_log (actor_id, action, entity_type, entity_id, old_data, new_data)
  values (auth.uid(), p_action, p_entity_type, p_entity_id, p_old_data, p_new_data);
end;
$$;

revoke all on function public.log_audit(text, text, uuid, jsonb, jsonb) from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- submit_doctor_profile
-- ---------------------------------------------------------------------------
create or replace function public.submit_doctor_profile(
  p_title text,
  p_specialization text,
  p_qualifications text,
  p_bio text,
  p_years integer,
  p_license_number text,
  p_modes public.service_mode[],
  p_slot_duration integer
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_caller_id uuid;
  v_caller_role public.user_role;
  v_existing public.doctor_profiles;
  v_needs_review boolean;
begin
  v_caller_id := auth.uid();
  if v_caller_id is null then
    raise exception 'AS000_NOT_AUTHENTICATED: You must be signed in.' using errcode = 'P0001';
  end if;

  select role into v_caller_role from public.profiles where id = v_caller_id;
  if v_caller_role is distinct from 'doctor' then
    raise exception 'AS022_NOT_A_DOCTOR_ACCOUNT: Only doctor accounts can submit a professional profile.'
      using errcode = 'P0001';
  end if;

  if coalesce(trim(p_specialization), '') = '' then
    raise exception 'AS023_INVALID_DOCTOR_PROFILE_FIELDS: Specialization is required.' using errcode = 'P0001';
  end if;
  if coalesce(trim(p_license_number), '') = '' then
    raise exception 'AS023_INVALID_DOCTOR_PROFILE_FIELDS: A valid license number is required.' using errcode = 'P0001';
  end if;
  if coalesce(p_years, -1) < 0 or p_years > 70 then
    raise exception 'AS023_INVALID_DOCTOR_PROFILE_FIELDS: Years of experience must be between 0 and 70.'
      using errcode = 'P0001';
  end if;
  if p_modes is null or cardinality(p_modes) = 0 then
    raise exception 'AS023_INVALID_DOCTOR_PROFILE_FIELDS: Choose at least one consultation mode.' using errcode = 'P0001';
  end if;
  if p_slot_duration is null or not (p_slot_duration = any (array[10, 15, 20, 30, 45, 60])) then
    raise exception 'AS023_INVALID_DOCTOR_PROFILE_FIELDS: Slot duration must be one of 10, 15, 20, 30, 45, or 60 minutes.'
      using errcode = 'P0001';
  end if;

  select * into v_existing from public.doctor_profiles where user_id = v_caller_id for update;

  if v_existing.user_id is null then
    insert into public.doctor_profiles (
      user_id, title, specialization, qualifications, bio, years_experience,
      license_number, consultation_modes, slot_duration_minutes, status
    ) values (
      v_caller_id, p_title, p_specialization, p_qualifications, p_bio, p_years,
      p_license_number, p_modes, p_slot_duration, 'pending'
    );

    perform public.log_audit(
      'doctor_profile_submitted', 'doctor_profiles', v_caller_id,
      null, jsonb_build_object('status', 'pending')
    );
    return;
  end if;

  v_needs_review := v_existing.status in ('approved', 'suspended')
    and (p_license_number is distinct from v_existing.license_number
         or p_specialization is distinct from v_existing.specialization);

  update public.doctor_profiles
  set title = p_title,
      specialization = p_specialization,
      qualifications = p_qualifications,
      bio = p_bio,
      years_experience = p_years,
      license_number = p_license_number,
      consultation_modes = p_modes,
      slot_duration_minutes = p_slot_duration,
      status = case
        when v_existing.status = 'rejected' then 'pending'
        when v_needs_review then 'pending'
        else v_existing.status
      end,
      rejection_reason = case when v_existing.status = 'rejected' then null else rejection_reason end,
      reviewed_by = case when v_existing.status = 'rejected' or v_needs_review then null else reviewed_by end,
      reviewed_at = case when v_existing.status = 'rejected' or v_needs_review then null else reviewed_at end
  where user_id = v_caller_id;

  if v_existing.status = 'rejected' then
    perform public.log_audit(
      'doctor_profile_resubmitted', 'doctor_profiles', v_caller_id,
      jsonb_build_object('status', 'rejected'), jsonb_build_object('status', 'pending')
    );
  elsif v_needs_review then
    perform public.log_audit(
      'doctor_profile_changed_pending_review', 'doctor_profiles', v_caller_id,
      jsonb_build_object('status', v_existing.status, 'license_number', v_existing.license_number, 'specialization', v_existing.specialization),
      jsonb_build_object('status', 'pending', 'license_number', p_license_number, 'specialization', p_specialization)
    );
  end if;
end;
$$;

revoke all on function public.submit_doctor_profile(
  text, text, text, text, integer, text, public.service_mode[], integer
) from public, anon;
grant execute on function public.submit_doctor_profile(
  text, text, text, text, integer, text, public.service_mode[], integer
) to authenticated;

-- ---------------------------------------------------------------------------
-- set_doctor_availability
-- ---------------------------------------------------------------------------
create or replace function public.set_doctor_availability(p_windows jsonb)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_item jsonb;
  v_other jsonb;
  v_day smallint;
  v_start time;
  v_end time;
  v_slot_minutes integer;
begin
  if not public.is_approved_doctor(auth.uid()) then
    raise exception 'AS024_ONLY_APPROVED_DOCTORS: Only approved doctors can configure availability.'
      using errcode = 'P0001';
  end if;

  if p_windows is null or jsonb_typeof(p_windows) <> 'array' then
    raise exception 'AS025_INVALID_AVAILABILITY_WINDOW: Windows must be a list.' using errcode = 'P0001';
  end if;

  for v_item in select * from jsonb_array_elements(p_windows)
  loop
    v_day := (v_item ->> 'day_of_week')::smallint;
    v_start := (v_item ->> 'start_time')::time;
    v_end := (v_item ->> 'end_time')::time;
    v_slot_minutes := (v_item ->> 'slot_duration_minutes')::integer;

    if v_day is null or v_day < 0 or v_day > 6 then
      raise exception 'AS025_INVALID_AVAILABILITY_WINDOW: Each window needs a day of week between 0 and 6.'
        using errcode = 'P0001';
    end if;
    if v_start is null or v_end is null or v_end <= v_start then
      raise exception 'AS025_INVALID_AVAILABILITY_WINDOW: Each window needs a valid start time before its end time.'
        using errcode = 'P0001';
    end if;
    if v_slot_minutes is null or not (v_slot_minutes = any (array[10, 15, 20, 30, 45, 60])) then
      raise exception 'AS025_INVALID_AVAILABILITY_WINDOW: Slot duration must be one of 10, 15, 20, 30, 45, or 60 minutes.'
        using errcode = 'P0001';
    end if;
    if (extract(epoch from (v_end - v_start)) / 60) < v_slot_minutes then
      raise exception 'AS025_INVALID_AVAILABILITY_WINDOW: A window must be at least as long as its own slot duration.'
        using errcode = 'P0001';
    end if;

    -- Reject overlaps within the submitted batch itself, same day, before
    -- ever touching the table -- the EXCLUDE constraint would also catch
    -- this (even intra-statement), but with a raw exclusion_violation
    -- rather than a code the app can map to a friendly message.
    for v_other in select * from jsonb_array_elements(p_windows)
    loop
      if v_other <> v_item
         and (v_other ->> 'day_of_week')::smallint = v_day
         and (v_other ->> 'start_time')::time < v_end
         and (v_other ->> 'end_time')::time > v_start
      then
        raise exception 'AS026_AVAILABILITY_OVERLAP: Two windows on the same day overlap.' using errcode = 'P0001';
      end if;
    end loop;
  end loop;

  delete from public.doctor_weekly_availability where doctor_id = auth.uid();

  begin
    insert into public.doctor_weekly_availability (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes)
    select
      auth.uid(),
      (item ->> 'day_of_week')::smallint,
      (item ->> 'start_time')::time,
      (item ->> 'end_time')::time,
      (item ->> 'slot_duration_minutes')::integer
    from jsonb_array_elements(p_windows) as item;
  exception
    when exclusion_violation then
      raise exception 'AS026_AVAILABILITY_OVERLAP: Two windows overlap.' using errcode = 'P0001';
  end;
end;
$$;

revoke all on function public.set_doctor_availability(jsonb) from public, anon;
grant execute on function public.set_doctor_availability(jsonb) to authenticated;

-- ---------------------------------------------------------------------------
-- set_accepting_requests
-- ---------------------------------------------------------------------------
create or replace function public.set_accepting_requests(p_value boolean)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_approved_doctor(auth.uid()) then
    raise exception 'AS024_ONLY_APPROVED_DOCTORS: Only approved doctors can change this setting.'
      using errcode = 'P0001';
  end if;

  update public.doctor_profiles set accepting_requests = p_value where user_id = auth.uid();
end;
$$;

revoke all on function public.set_accepting_requests(boolean) from public, anon;
grant execute on function public.set_accepting_requests(boolean) to authenticated;

commit;
