-- Proves get_doctor_open_slots() and create_service_request() behave per
-- 20261112000008_scheduling_business_logic.sql: happy path, slot-taken
-- rejection, unapproved-doctor rejection (both as empty-slots and as a
-- create_service_request error), wrong-mode rejection, past-date rejection,
-- patient-role enforcement, and suspended-account enforcement.
--
-- Every custom error in this project uses `errcode = 'P0001'` uniformly
-- (see RequestsRepository/create_request precedent), so throws_ok(sql,
-- 'P0001') alone cannot distinguish *which* AS0xx code fired. Instead, each
-- rejection case below runs the call inside a DO block, catches the
-- exception, and captures `split_part(sqlerrm, ':', 1)` -- the stable code
-- prefix every message starts with -- via set_config, then asserts on that
-- captured code directly. This is a stronger, still-fixture-based check,
-- not a replacement for the documented two-session concurrency test (see
-- docs/concurrency-testing.md), which this file cannot exercise.
begin;

select plan(14);

-- =============================================================================
-- Fixtures (created as the unrestricted table owner, before any role switch)
-- =============================================================================

with c as (
  insert into public.service_categories (name, slug, is_active)
  values ('PGTAP Scheduling Category', 'pgtap-scheduling-category', true)
  returning id
)
select set_config('pgtap.category_id', id::text, true) from c;

with s as (
  insert into public.services (category_id, name, slug, requires_doctor, duration_minutes, is_active, supported_modes)
  values (
    current_setting('pgtap.category_id')::uuid,
    'PGTAP Scheduling Service', 'pgtap-scheduling-service',
    true, 30, true, array['in_person']::service_mode[]
  )
  returning id
)
select set_config('pgtap.service_id', id::text, true) from s;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-sched-patienta@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Sched Patient A'), now(), now())
  returning id
)
select set_config('pgtap.patient_a', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-sched-patientsuspended@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Sched Patient Suspended'), now(), now())
  returning id
)
select set_config('pgtap.patient_suspended', id::text, true) from u;

update public.profiles set is_suspended = true where id = current_setting('pgtap.patient_suspended')::uuid;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-sched-otherpatient@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Sched Other Patient'), now(), now())
  returning id
)
select set_config('pgtap.patient_other', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-sched-doctoraccount@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Sched Doctor Account', 'signup_role', 'doctor'), now(), now())
  returning id
)
select set_config('pgtap.doctor_role_account', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-sched-doctora@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Sched Doctor A', 'signup_role', 'doctor'), now(), now())
  returning id
)
select set_config('pgtap.doctor_a', id::text, true) from u;

insert into public.doctor_profiles (user_id, specialization, license_number, status, accepting_requests, consultation_modes)
values (current_setting('pgtap.doctor_a')::uuid, 'General Medicine', 'PGTAP-SCHED-LIC-A', 'approved', true, array['in_person']::service_mode[]);

insert into public.doctor_services (doctor_id, service_id)
values (current_setting('pgtap.doctor_a')::uuid, current_setting('pgtap.service_id')::uuid);

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-sched-doctorb@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Sched Doctor B', 'signup_role', 'doctor'), now(), now())
  returning id
)
select set_config('pgtap.doctor_b', id::text, true) from u;

-- Doctor B is 'pending' -- never approved -- for the unapproved-doctor cases.
insert into public.doctor_profiles (user_id, specialization, license_number, status, accepting_requests, consultation_modes)
values (current_setting('pgtap.doctor_b')::uuid, 'Dermatology', 'PGTAP-SCHED-LIC-B', 'pending', true, array['in_person']::service_mode[]);

insert into public.doctor_services (doctor_id, service_id)
values (current_setting('pgtap.doctor_b')::uuid, current_setting('pgtap.service_id')::uuid);

-- Two days out, so every slot in the window is trivially past the 60-minute
-- lead time regardless of what time the test happens to run.
select set_config('pgtap.target_date', (current_date + 2)::text, true);

insert into public.doctor_weekly_availability (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes, is_active)
values (
  current_setting('pgtap.doctor_a')::uuid,
  extract(dow from current_setting('pgtap.target_date')::date),
  '00:00', '23:30', 30, true
);

-- =============================================================================
-- Capture two open slots for doctor A on the target date, as an
-- authenticated caller (get_doctor_open_slots requires auth.uid()).
-- =============================================================================
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);

with slots as (
  select slot_start, slot_end, row_number() over (order by slot_start) as rn
  from public.get_doctor_open_slots(current_setting('pgtap.doctor_a')::uuid, current_setting('pgtap.target_date')::date)
)
select
  set_config('pgtap.slot1_start', (select slot_start::text from slots where rn = 1), true),
  set_config('pgtap.slot1_end', (select slot_end::text from slots where rn = 1), true),
  set_config('pgtap.slot2_start', (select slot_start::text from slots where rn = 2), true),
  set_config('pgtap.slot2_end', (select slot_end::text from slots where rn = 2), true);

reset role;

select ok(
  current_setting('pgtap.slot1_start', true) is not null and current_setting('pgtap.slot2_start', true) is not null,
  'fixture produced at least two open slots for doctor A on the target date'
);

-- Pre-book slot 1 as 'accepted' (by a different patient), directly -- this
-- is the state create_service_request must respect, not produce.
insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status, contact_phone)
values (
  current_setting('pgtap.patient_other')::uuid,
  current_setting('pgtap.doctor_a')::uuid,
  current_setting('pgtap.service_id')::uuid,
  current_setting('pgtap.slot1_start')::timestamptz,
  current_setting('pgtap.slot1_end')::timestamptz,
  'in_person', 'accepted', '+91 90000 00000'
);

-- =============================================================================
-- Happy path: patient A books slot 2.
-- =============================================================================
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);

do $$
declare
  v_id uuid;
  v_code text;
begin
  begin
    v_id := public.create_service_request(
      current_setting('pgtap.service_id')::uuid,
      current_setting('pgtap.doctor_a')::uuid,
      current_setting('pgtap.slot2_start')::timestamptz,
      'in_person'::service_mode,
      'Happy path test notes.',
      '+91 90000 00099',
      null
    );
    v_code := 'OK';
  exception when others then
    v_code := split_part(sqlerrm, ':', 1);
    v_id := null;
  end;
  perform set_config('pgtap.happy_path_code', v_code, true);
  perform set_config('pgtap.happy_path_request_id', coalesce(v_id::text, ''), true);
end;
$$;

select is(
  current_setting('pgtap.happy_path_code'),
  'OK',
  'happy path: patient A can book an open slot with a valid doctor/mode/phone'
);

select is(
  (select status::text from public.requests where id = current_setting('pgtap.happy_path_request_id')::uuid),
  'requested',
  'the new request lands with status requested'
);

select is(
  (select scheduled_start from public.requests where id = current_setting('pgtap.happy_path_request_id')::uuid),
  current_setting('pgtap.slot2_start')::timestamptz,
  'the new request''s scheduled_start matches the chosen slot exactly'
);

select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('pgtap.patient_a')::uuid
       and type = 'request_created'
       and related_request_id = current_setting('pgtap.happy_path_request_id')::uuid) > 0,
  'create_service_request sends the patient a request_created notification'
);

select ok(
  (select count(*)::int from public.request_status_history
     where request_id = current_setting('pgtap.happy_path_request_id')::uuid
       and new_status = 'requested') > 0,
  'the existing requests_log_created trigger still writes the first status history row'
);

reset role;

-- =============================================================================
-- Rejection cases, all as patient A unless noted otherwise.
-- =============================================================================
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);

-- Slot 1 is now 'accepted' for someone else -- no longer an open slot.
do $$
declare
  v_code text;
begin
  begin
    perform public.create_service_request(
      current_setting('pgtap.service_id')::uuid,
      current_setting('pgtap.doctor_a')::uuid,
      current_setting('pgtap.slot1_start')::timestamptz,
      'in_person'::service_mode,
      null, '+91 90000 00098', null
    );
    v_code := 'NO_ERROR_RAISED';
  exception when others then
    v_code := split_part(sqlerrm, ':', 1);
  end;
  perform set_config('pgtap.case_slot_taken', v_code, true);
end;
$$;

select is(
  current_setting('pgtap.case_slot_taken'),
  'AS001_SLOT_UNAVAILABLE',
  'a slot already accepted for another patient is rejected'
);

-- Doctor B is pending, never approved.
do $$
declare
  v_code text;
begin
  begin
    perform public.create_service_request(
      current_setting('pgtap.service_id')::uuid,
      current_setting('pgtap.doctor_b')::uuid,
      (current_setting('pgtap.target_date')::date + time '10:00') at time zone 'Asia/Kolkata',
      'in_person'::service_mode,
      null, '+91 90000 00097', null
    );
    v_code := 'NO_ERROR_RAISED';
  exception when others then
    v_code := split_part(sqlerrm, ':', 1);
  end;
  perform set_config('pgtap.case_unapproved_doctor', v_code, true);
end;
$$;

select is(
  current_setting('pgtap.case_unapproved_doctor'),
  'AS002_DOCTOR_NOT_AVAILABLE',
  'an unapproved (pending) doctor is rejected'
);

-- Service and doctor only support in_person; try online.
do $$
declare
  v_code text;
begin
  begin
    perform public.create_service_request(
      current_setting('pgtap.service_id')::uuid,
      current_setting('pgtap.doctor_a')::uuid,
      current_setting('pgtap.slot2_start')::timestamptz,
      'online'::service_mode,
      null, '+91 90000 00096', null
    );
    v_code := 'NO_ERROR_RAISED';
  exception when others then
    v_code := split_part(sqlerrm, ':', 1);
  end;
  perform set_config('pgtap.case_wrong_mode', v_code, true);
end;
$$;

select is(
  current_setting('pgtap.case_wrong_mode'),
  'AS009_MODE_UNSUPPORTED',
  'a consultation mode the service/doctor do not support is rejected'
);

-- A slot start in the past (yesterday).
do $$
declare
  v_code text;
begin
  begin
    perform public.create_service_request(
      current_setting('pgtap.service_id')::uuid,
      current_setting('pgtap.doctor_a')::uuid,
      now() - interval '1 day',
      'in_person'::service_mode,
      null, '+91 90000 00095', null
    );
    v_code := 'NO_ERROR_RAISED';
  exception when others then
    v_code := split_part(sqlerrm, ':', 1);
  end;
  perform set_config('pgtap.case_past_date', v_code, true);
end;
$$;

select is(
  current_setting('pgtap.case_past_date'),
  'AS003_INVALID_DATE',
  'a slot start in the past is rejected'
);

-- A doctor-role account cannot submit a service request.
select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_role_account'), true);

do $$
declare
  v_code text;
begin
  begin
    perform public.create_service_request(
      current_setting('pgtap.service_id')::uuid,
      current_setting('pgtap.doctor_a')::uuid,
      current_setting('pgtap.slot2_start')::timestamptz,
      'in_person'::service_mode,
      null, '+91 90000 00094', null
    );
    v_code := 'NO_ERROR_RAISED';
  exception when others then
    v_code := split_part(sqlerrm, ':', 1);
  end;
  perform set_config('pgtap.case_doctor_role', v_code, true);
end;
$$;

select is(
  current_setting('pgtap.case_doctor_role'),
  'AS004_ROLE_NOT_PATIENT',
  'a doctor-role account cannot submit a service request'
);

-- A suspended patient cannot submit a service request.
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_suspended'), true);

do $$
declare
  v_code text;
begin
  begin
    perform public.create_service_request(
      current_setting('pgtap.service_id')::uuid,
      current_setting('pgtap.doctor_a')::uuid,
      current_setting('pgtap.slot2_start')::timestamptz,
      'in_person'::service_mode,
      null, '+91 90000 00093', null
    );
    v_code := 'NO_ERROR_RAISED';
  exception when others then
    v_code := split_part(sqlerrm, ':', 1);
  end;
  perform set_config('pgtap.case_suspended', v_code, true);
end;
$$;

select is(
  current_setting('pgtap.case_suspended'),
  'AS005_ACCOUNT_SUSPENDED',
  'a suspended patient account cannot submit a service request'
);

-- =============================================================================
-- get_doctor_open_slots direct unit checks.
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);

select is(
  (select count(*)::int from public.get_doctor_open_slots(
     current_setting('pgtap.doctor_b')::uuid, current_setting('pgtap.target_date')::date
   )),
  0,
  'get_doctor_open_slots returns zero rows (not an error) for an unapproved doctor'
);

do $$
declare
  v_code text;
begin
  begin
    perform * from public.get_doctor_open_slots(current_setting('pgtap.doctor_a')::uuid, current_date - 1);
    v_code := 'NO_ERROR_RAISED';
  exception when others then
    v_code := split_part(sqlerrm, ':', 1);
  end;
  perform set_config('pgtap.case_slots_past_date', v_code, true);
end;
$$;

select is(
  current_setting('pgtap.case_slots_past_date'),
  'AS003_INVALID_DATE',
  'get_doctor_open_slots rejects a date in the past'
);

reset role;

select * from finish();

rollback;
