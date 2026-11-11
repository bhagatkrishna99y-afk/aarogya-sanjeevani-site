-- RLS verification matrix: impersonates patient A, patient B, doctor A
-- (approved), doctor B (pending, unapproved), doctor C (suspended), and an
-- admin, asserting exactly what each can and cannot read or write across
-- every table. Every negative Security Acceptance Criterion from the
-- prompt is covered explicitly (see the inline labels).
--
-- Row-count assertions use `is(count, 0, ...)` rather than expecting an
-- error: a denied SELECT under RLS returns zero rows silently, it does not
-- throw. Only grant-denied writes (no UPDATE/INSERT privilege at all) throw,
-- and those use throws_ok(sql, sqlstate) -- the two-argument form, the only
-- one verified to behave as expected in this project (see
-- 001_profiles_signup_and_role_protection_test.sql's header).
begin;

select plan(27);

-- =============================================================================
-- Fixtures (created as the unrestricted table owner, before any role switch)
-- =============================================================================

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-matrix-patienta@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Matrix Patient A'), now(), now())
  returning id
)
select set_config('pgtap.patient_a', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-matrix-patientb@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Matrix Patient B'), now(), now())
  returning id
)
select set_config('pgtap.patient_b', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-matrix-doctora@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Matrix Doctor A', 'signup_role', 'doctor'), now(), now())
  returning id
)
select set_config('pgtap.doctor_a', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-matrix-doctorb@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Matrix Doctor B'), now(), now())
  returning id
)
select set_config('pgtap.doctor_b', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-matrix-doctorc@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Matrix Doctor C'), now(), now())
  returning id
)
select set_config('pgtap.doctor_c', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-matrix-admin@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Matrix Admin'), now(), now())
  returning id
)
select set_config('pgtap.admin_a', id::text, true) from u;

update public.profiles set role = 'admin' where id = current_setting('pgtap.admin_a')::uuid;

insert into public.doctor_profiles (user_id, specialization, license_number, status)
values (current_setting('pgtap.doctor_a')::uuid, 'General Medicine', 'MATRIX-LIC-A', 'approved');

insert into public.doctor_profiles (user_id, specialization, license_number, status)
values (current_setting('pgtap.doctor_b')::uuid, 'Dermatology', 'MATRIX-LIC-B', 'pending');

insert into public.doctor_profiles (user_id, specialization, license_number, status)
values (current_setting('pgtap.doctor_c')::uuid, 'Pediatrics', 'MATRIX-LIC-C', 'suspended');

select set_config('pgtap.service_id', (select id::text from public.services where requires_doctor limit 1), true);

-- requestA: patient A <-> doctor A. requestB: patient B <-> doctor A.
-- Neither involves doctor B or doctor C.
with r as (
  insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status, contact_phone)
  values (
    current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, current_setting('pgtap.service_id')::uuid,
    '2026-12-10 09:00+00', '2026-12-10 09:30+00', 'in_person', 'accepted', '+91 90000 00001'
  )
  returning id
)
select set_config('pgtap.request_a', id::text, true) from r;

with r as (
  insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status, contact_phone)
  values (
    current_setting('pgtap.patient_b')::uuid, current_setting('pgtap.doctor_a')::uuid, current_setting('pgtap.service_id')::uuid,
    '2026-12-11 09:00+00', '2026-12-11 09:30+00', 'in_person', 'requested', '+91 90000 00002'
  )
  returning id
)
select set_config('pgtap.request_b', id::text, true) from r;

-- =============================================================================
-- profiles
-- =============================================================================
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);

select is(
  (select count(*)::int from public.profiles where id = current_setting('pgtap.patient_a')::uuid),
  1,
  'patient A can select their own profile'
);

-- Security Acceptance Criterion: patient cannot read another patient's data.
select is(
  (select count(*)::int from public.profiles where id = current_setting('pgtap.patient_b')::uuid),
  0,
  'patient A cannot select patient B''s profile'
);

-- Security Acceptance Criterion: user cannot elevate role.
select throws_ok(
  format('update public.profiles set role = %L where id = %L', 'admin', current_setting('pgtap.patient_a')::uuid),
  '42501'
);

-- =============================================================================
-- doctor_profiles
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);

select is(
  (select count(*)::int from public.doctor_profiles where user_id = current_setting('pgtap.doctor_a')::uuid),
  1,
  'doctor A can select their own doctor_profiles row'
);

select is(
  (select count(*)::int from public.doctor_profiles where user_id = current_setting('pgtap.doctor_b')::uuid),
  0,
  'doctor A cannot select doctor B''s doctor_profiles row'
);

-- Security Acceptance Criterion: doctor cannot approve themselves.
select throws_ok(
  format('update public.doctor_profiles set status = %L where user_id = %L', 'approved', current_setting('pgtap.doctor_a')::uuid),
  '42501'
);

-- =============================================================================
-- doctors_public view
-- =============================================================================
reset role;
set local role anon;

-- Security Acceptance Criterion: unapproved doctor not visible in the public view.
select is(
  (select count(*)::int from public.doctors_public where doctor_id = current_setting('pgtap.doctor_b')::uuid),
  0,
  'an unapproved (pending) doctor does not appear in doctors_public'
);

select is(
  (select count(*)::int from public.doctors_public where doctor_id = current_setting('pgtap.doctor_a')::uuid),
  1,
  'an approved doctor appears in doctors_public, even to anon'
);

-- =============================================================================
-- requests
-- =============================================================================
reset role;
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);

select is(
  (select count(*)::int from public.requests where id = current_setting('pgtap.request_a')::uuid),
  1,
  'patient A can select their own request'
);

select is(
  (select count(*)::int from public.requests where id = current_setting('pgtap.request_b')::uuid),
  0,
  'patient A cannot select patient B''s request'
);

-- Security Acceptance Criterion: patient cannot change request status.
select throws_ok(
  format('update public.requests set status = %L where id = %L', 'cancelled', current_setting('pgtap.request_a')::uuid),
  '42501'
);

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);

select is(
  (select count(*)::int from public.requests where id in (current_setting('pgtap.request_a')::uuid, current_setting('pgtap.request_b')::uuid)),
  2,
  'doctor A (assigned to both) can select both requests'
);

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_b'), true);

-- Security Acceptance Criterion: doctor cannot read unrelated requests.
select is(
  (select count(*)::int from public.requests where id in (current_setting('pgtap.request_a')::uuid, current_setting('pgtap.request_b')::uuid)),
  0,
  'doctor B (unrelated) cannot select either request'
);

select set_config('request.jwt.claim.sub', current_setting('pgtap.admin_a'), true);

select ok(
  (select count(*)::int from public.requests) >= 2,
  'admin can select all requests'
);

-- =============================================================================
-- request_status_history
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);

select ok(
  (select count(*)::int from public.request_status_history where request_id = current_setting('pgtap.request_a')::uuid) > 0,
  'patient A can select status history for their own request'
);

select is(
  (select count(*)::int from public.request_status_history where request_id = current_setting('pgtap.request_b')::uuid),
  0,
  'patient A cannot select status history for patient B''s request'
);

-- =============================================================================
-- notifications
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);

select ok(
  (select count(*)::int from public.notifications where user_id = current_setting('pgtap.doctor_a')::uuid) > 0,
  'doctor A can select their own notifications (new-request notification from the fixture inserts)'
);

select is(
  (select count(*)::int from public.notifications where user_id = current_setting('pgtap.patient_a')::uuid),
  0,
  'doctor A cannot select patient A''s notifications'
);

-- Negative check runs BEFORE the positive one below, on the same row, so it
-- observes a clean read_at IS NULL state rather than one already flipped by
-- the positive test.
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);

update public.notifications set read_at = now() where user_id = current_setting('pgtap.doctor_a')::uuid;

select is(
  (select count(*)::int from public.notifications n
     where n.user_id = current_setting('pgtap.doctor_a')::uuid and n.read_at is not null),
  0,
  'patient A''s update of doctor A''s notifications matches zero rows (RLS-filtered, not an error)'
);

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);

-- Doctor A has more than one notification in this fixture (one per request
-- that names them as the assigned doctor), so this scopes to a single row
-- by id rather than by user_id -- "exactly 1 row became read" must mean one
-- specific row, not "however many rows doctor A happens to have".
select set_config(
  'pgtap.notification_a',
  (select id::text from public.notifications where user_id = current_setting('pgtap.doctor_a')::uuid order by created_at limit 1),
  true
);

select lives_ok(
  format(
    $sql$update public.notifications set read_at = now() where id = %L$sql$,
    current_setting('pgtap.notification_a')::uuid
  )
);

select is(
  (select read_at is not null from public.notifications where id = current_setting('pgtap.notification_a')::uuid),
  true,
  'doctor A can mark their own notification read'
);

-- =============================================================================
-- audit_log
-- =============================================================================
select is(
  (select count(*)::int from public.audit_log),
  0,
  'patient A (non-admin) cannot select any audit_log rows'
);

select set_config('request.jwt.claim.sub', current_setting('pgtap.admin_a'), true);

select lives_ok(
  $sql$select count(*) from public.audit_log$sql$
);

-- =============================================================================
-- doctor_services / doctor_weekly_availability write policies
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);

select lives_ok(
  format(
    $sql$insert into public.doctor_services (doctor_id, service_id) values (%L, %L)$sql$,
    current_setting('pgtap.doctor_a')::uuid, current_setting('pgtap.service_id')::uuid
  )
);

select throws_ok(
  format(
    $sql$insert into public.doctor_services (doctor_id, service_id) values (%L, %L)$sql$,
    current_setting('pgtap.doctor_b')::uuid, current_setting('pgtap.service_id')::uuid
  ),
  '42501'
);

select lives_ok(
  format(
    $sql$insert into public.doctor_weekly_availability (doctor_id, day_of_week, start_time, end_time)
         values (%L, 3, '09:00', '12:00')$sql$,
    current_setting('pgtap.doctor_a')::uuid
  )
);

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_c'), true);

-- doctor C is 'suspended' -- the WITH CHECK's status <> 'suspended' clause
-- must reject this even though doctor C owns the row being inserted.
select throws_ok(
  format(
    $sql$insert into public.doctor_weekly_availability (doctor_id, day_of_week, start_time, end_time)
         values (%L, 3, '09:00', '12:00')$sql$,
    current_setting('pgtap.doctor_c')::uuid
  ),
  '42501'
);

reset role;

select * from finish();

rollback;
