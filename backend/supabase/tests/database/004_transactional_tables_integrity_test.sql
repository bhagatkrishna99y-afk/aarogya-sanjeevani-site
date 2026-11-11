-- Proves: two overlapping ACCEPTED requests for one doctor are rejected;
-- two overlapping REQUESTED requests are allowed (deliberate relaxation --
-- see 20261112000006's header comment); request_status_history is
-- append-only; a home_visit request without an address is rejected.
begin;

-- 5 assertions: "two overlapping requested requests are allowed" needs two
-- separate lives_ok calls to prove both inserts succeed.
select plan(5);

-- ---------------------------------------------------------------------------
-- Fixtures: one doctor, two patients, one service.
-- ---------------------------------------------------------------------------
with new_user as (
  insert into auth.users (
    id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at
  )
  values (
    gen_random_uuid(), 'authenticated', 'authenticated',
    'pgtap-req-doctor@example.test',
    crypt('not-a-real-login', gen_salt('bf')),
    now(),
    jsonb_build_object('full_name', 'PGTAP Req Doctor'),
    now(), now()
  )
  returning id
)
select set_config('pgtap.doctor_id', id::text, true) from new_user;

insert into public.doctor_profiles (user_id, specialization, license_number)
values (current_setting('pgtap.doctor_id')::uuid, 'General Medicine', 'PGTAP-REQ-LIC');

with new_user as (
  insert into auth.users (
    id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at
  )
  values (
    gen_random_uuid(), 'authenticated', 'authenticated',
    'pgtap-req-patient1@example.test',
    crypt('not-a-real-login', gen_salt('bf')),
    now(),
    jsonb_build_object('full_name', 'PGTAP Req Patient One'),
    now(), now()
  )
  returning id
)
select set_config('pgtap.patient1_id', id::text, true) from new_user;

with new_user as (
  insert into auth.users (
    id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at
  )
  values (
    gen_random_uuid(), 'authenticated', 'authenticated',
    'pgtap-req-patient2@example.test',
    crypt('not-a-real-login', gen_salt('bf')),
    now(),
    jsonb_build_object('full_name', 'PGTAP Req Patient Two'),
    now(), now()
  )
  returning id
)
select set_config('pgtap.patient2_id', id::text, true) from new_user;

select set_config('pgtap.service_id', (select id::text from public.services limit 1), true);

-- ---------------------------------------------------------------------------
-- 1. Two overlapping ACCEPTED requests for the same doctor must fail
-- ---------------------------------------------------------------------------
insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status)
values (
  current_setting('pgtap.patient1_id')::uuid,
  current_setting('pgtap.doctor_id')::uuid,
  current_setting('pgtap.service_id')::uuid,
  '2026-12-01 09:00+00', '2026-12-01 09:30+00',
  'in_person', 'accepted'
);

select throws_ok(
  format(
    $sql$insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status)
         values (%L, %L, %L, '2026-12-01 09:15+00', '2026-12-01 09:45+00', 'in_person', 'accepted')$sql$,
    current_setting('pgtap.patient2_id')::uuid,
    current_setting('pgtap.doctor_id')::uuid,
    current_setting('pgtap.service_id')::uuid
  ),
  '23P01'
);

-- ---------------------------------------------------------------------------
-- 2. Two overlapping REQUESTED requests for the same doctor are allowed
-- ---------------------------------------------------------------------------
select lives_ok(
  format(
    $sql$insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status)
         values (%L, %L, %L, '2026-12-02 09:00+00', '2026-12-02 09:30+00', 'in_person', 'requested')$sql$,
    current_setting('pgtap.patient1_id')::uuid,
    current_setting('pgtap.doctor_id')::uuid,
    current_setting('pgtap.service_id')::uuid
  )
);

select lives_ok(
  format(
    $sql$insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status)
         values (%L, %L, %L, '2026-12-02 09:15+00', '2026-12-02 09:45+00', 'in_person', 'requested')$sql$,
    current_setting('pgtap.patient2_id')::uuid,
    current_setting('pgtap.doctor_id')::uuid,
    current_setting('pgtap.service_id')::uuid
  )
);

-- ---------------------------------------------------------------------------
-- 3. request_status_history is append-only
-- ---------------------------------------------------------------------------
select throws_ok(
  $sql$update public.request_status_history set note = 'tampered' where true$sql$,
  '0A000'
);

-- ---------------------------------------------------------------------------
-- 4. home_visit without an address is rejected
-- ---------------------------------------------------------------------------
select throws_ok(
  format(
    $sql$insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status)
         values (%L, null, %L, '2026-12-03 09:00+00', '2026-12-03 09:30+00', 'home_visit', 'requested')$sql$,
    current_setting('pgtap.patient1_id')::uuid,
    current_setting('pgtap.service_id')::uuid
  ),
  '23514'
);

select * from finish();

rollback;
