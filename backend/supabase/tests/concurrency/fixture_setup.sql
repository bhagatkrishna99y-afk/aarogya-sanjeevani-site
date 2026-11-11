-- Fixture for the slot-race concurrency simulation (see
-- docs/concurrency-testing.md and scripts/run_concurrency_test.sh).
--
-- Unlike every pgTAP file in this project, this one is NOT wrapped in
-- begin;/rollback; -- the whole point is for these rows to persist across
-- two separate pgbench invocations (two different OS processes, so they
-- cannot share one open transaction). Run fixture_teardown.sql afterward
-- to remove everything this creates.
--
-- Run as a normal privileged connection (e.g. plain `psql "$DATABASE_URL"`),
-- not through PostgREST/RLS -- this is test setup, not something an app
-- ever does.

create table if not exists public._concurrency_fixture (
  doctor_id uuid not null,
  service_id uuid not null,
  category_id uuid not null,
  patient_ids uuid[] not null,
  slot_start timestamptz not null,
  slot_end timestamptz not null
);
truncate public._concurrency_fixture;

do $$
declare
  v_doctor_id uuid;
  v_category_id uuid;
  v_service_id uuid;
  v_patient_ids uuid[] := '{}';
  v_patient_id uuid;
  v_target_date date := current_date + 3;
  v_slot_start timestamptz;
  v_slot_end timestamptz;
  i int;
begin
  -- One approved, accepting-requests doctor.
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'concurrency-doctor@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Concurrency Doctor', 'signup_role', 'doctor'), now(), now())
  returning id into v_doctor_id;

  -- No doctor_profiles row exists until a real submit_doctor_profile()
  -- call -- inserted directly here since the fixture skips that flow.
  insert into public.doctor_profiles (
    user_id, title, specialization, qualifications, bio, years_experience,
    license_number, consultation_modes, slot_duration_minutes, status, accepting_requests, reviewed_at
  )
  values (v_doctor_id, 'Dr.', 'General Medicine', 'MBBS', 'Fixture doctor for concurrency testing.', 10,
    'CONC-LIC-1', array['in_person']::service_mode[], 30, 'approved', true, now());

  -- 20 patients.
  for i in 1..20 loop
    insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
    values (gen_random_uuid(), 'authenticated', 'authenticated', format('concurrency-patient-%s@example.test', i),
      crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', format('Concurrency Patient %s', i)), now(), now())
    returning id into v_patient_id;
    v_patient_ids := v_patient_ids || v_patient_id;
  end loop;

  -- Service + category.
  insert into public.service_categories (name, slug, is_active)
  values ('Concurrency Test Category', 'concurrency-test-category', true)
  returning id into v_category_id;

  insert into public.services (category_id, name, slug, requires_doctor, duration_minutes, is_active, supported_modes)
  values (v_category_id, 'Concurrency Test Consultation', 'concurrency-test-consultation', true, 30, true, array['in_person']::service_mode[])
  returning id into v_service_id;

  insert into public.doctor_services (doctor_id, service_id) values (v_doctor_id, v_service_id);

  -- Availability covering exactly one target day, one hour window
  -- (two 30-minute slots) so get_doctor_open_slots/create_service_request
  -- agree on the same slot boundaries the pgbench scripts will target.
  insert into public.doctor_weekly_availability (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes)
  values (v_doctor_id, extract(dow from v_target_date)::smallint, '10:00', '11:00', 30);

  v_slot_start := v_target_date + time '10:00';
  v_slot_end := v_slot_start + interval '30 minutes';

  insert into public._concurrency_fixture (doctor_id, service_id, category_id, patient_ids, slot_start, slot_end)
  values (v_doctor_id, v_service_id, v_category_id, v_patient_ids, v_slot_start, v_slot_end);

  raise notice 'Fixture ready: doctor_id=%, service_id=%, slot_start=%, 20 patients created.', v_doctor_id, v_service_id, v_slot_start;
end;
$$;

select * from public._concurrency_fixture;
