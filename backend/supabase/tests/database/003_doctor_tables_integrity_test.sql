-- Proves: overlapping active availability windows are rejected; an
-- end-before-start window is rejected; a non-user/non-doctor (admin)
-- profile cannot get a doctor_profiles row; and doctors_public never
-- exposes license_number.
begin;

select plan(4);

-- Fixture: one real user -> doctor_profiles row (status defaults to
-- 'pending', which the relaxed role check allows since role is still
-- 'user' at this point -- see 20261112000005's header comment).
with new_user as (
  insert into auth.users (
    id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at
  )
  values (
    gen_random_uuid(), 'authenticated', 'authenticated',
    'pgtap-doctor-fixture@example.test',
    crypt('not-a-real-login', gen_salt('bf')),
    now(),
    jsonb_build_object('full_name', 'PGTAP Doctor Fixture'),
    now(), now()
  )
  returning id
)
select set_config('pgtap.doctor_user_id', id::text, true) from new_user;

insert into public.doctor_profiles (user_id, specialization, license_number)
values (current_setting('pgtap.doctor_user_id')::uuid, 'General Medicine', 'PGTAP-LIC-001');

-- ---------------------------------------------------------------------------
-- 1. Overlapping active availability windows for the same doctor/day are rejected
-- ---------------------------------------------------------------------------
insert into public.doctor_weekly_availability (doctor_id, day_of_week, start_time, end_time)
values (current_setting('pgtap.doctor_user_id')::uuid, 1, '09:00', '12:00');

select throws_ok(
  format(
    $sql$insert into public.doctor_weekly_availability (doctor_id, day_of_week, start_time, end_time)
         values (%L, 1, '11:00', '14:00')$sql$,
    current_setting('pgtap.doctor_user_id')::uuid
  ),
  '23P01'
);

-- ---------------------------------------------------------------------------
-- 2. end_time <= start_time is rejected
-- ---------------------------------------------------------------------------
select throws_ok(
  format(
    $sql$insert into public.doctor_weekly_availability (doctor_id, day_of_week, start_time, end_time)
         values (%L, 2, '14:00', '13:00')$sql$,
    current_setting('pgtap.doctor_user_id')::uuid
  ),
  '23514'
);

-- ---------------------------------------------------------------------------
-- 3. A non-user/non-doctor (admin) profile cannot get a doctor_profiles row
-- ---------------------------------------------------------------------------
with admin_user as (
  insert into auth.users (
    id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at
  )
  values (
    gen_random_uuid(), 'authenticated', 'authenticated',
    'pgtap-admin-fixture@example.test',
    crypt('not-a-real-login', gen_salt('bf')),
    now(),
    jsonb_build_object('full_name', 'PGTAP Admin Fixture'),
    now(), now()
  )
  returning id
)
select set_config('pgtap.admin_user_id', id::text, true) from admin_user;

-- Running as the table owner (postgres), so this direct role change is
-- allowed by prevent_role_escalation (see 20261112000002) -- only a client
-- UPDATE via the `authenticated` Postgres role is blocked by that trigger.
update public.profiles set role = 'admin' where id = current_setting('pgtap.admin_user_id')::uuid;

select throws_ok(
  format(
    $sql$insert into public.doctor_profiles (user_id, specialization, license_number)
         values (%L, 'General Medicine', 'PGTAP-LIC-002')$sql$,
    current_setting('pgtap.admin_user_id')::uuid
  ),
  'P0001'
);

-- ---------------------------------------------------------------------------
-- 4. doctors_public never exposes license_number (or any other private column)
-- ---------------------------------------------------------------------------
select hasnt_column('public', 'doctors_public', 'license_number', 'doctors_public must never expose license_number');

select * from finish();

rollback;
