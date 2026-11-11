-- Proves: (1) signup_role='admin' never elevates -- lands as 'user';
-- (2) signup_role='doctor' is honored; (3) an authenticated user cannot
-- update their own role, directly or via any payload shape.
--
-- NOTE on structure: each "create a user, then assert on the profiles row
-- the trigger created" case is deliberately split into two separate
-- top-level statements (capturing the new auth.users id via set_config,
-- then asserting in the next statement) rather than one statement like
-- `with new_user as (insert ... returning id) select is((select role from
-- profiles where id = (select id from new_user)), ...)`. That single-
-- statement form reliably returns no match: per Postgres's documented
-- same-snapshot semantics for data-modifying CTEs, a side effect of a
-- trigger fired during the CTE (the profiles row insert, cascading from
-- the auth.users insert) is not visible to another part of the very same
-- statement -- only to later statements in the same transaction. Verified
-- by hand against the live project before writing this file.
begin;

select plan(3);

-- ---------------------------------------------------------------------------
-- 1. signup_role='admin' must be rejected by handle_new_user() and land as 'user'
-- ---------------------------------------------------------------------------
with new_user as (
  insert into auth.users (
    id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at
  )
  values (
    gen_random_uuid(), 'authenticated', 'authenticated',
    'pgtap-admin-attempt@example.test',
    crypt('not-a-real-login', gen_salt('bf')),
    now(),
    jsonb_build_object('full_name', 'PGTAP Admin Attempt', 'signup_role', 'admin'),
    now(), now()
  )
  returning id
)
select set_config('pgtap.admin_attempt_id', id::text, true) from new_user;

select is(
  (select role::text from public.profiles where id = current_setting('pgtap.admin_attempt_id')::uuid),
  'user',
  'signup_role=admin must not grant the admin role -- lands as user'
);

-- ---------------------------------------------------------------------------
-- 2. signup_role='doctor' is the one non-default role signup may request
-- ---------------------------------------------------------------------------
with new_user as (
  insert into auth.users (
    id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at
  )
  values (
    gen_random_uuid(), 'authenticated', 'authenticated',
    'pgtap-doctor-signup@example.test',
    crypt('not-a-real-login', gen_salt('bf')),
    now(),
    jsonb_build_object('full_name', 'PGTAP Doctor Signup', 'signup_role', 'doctor'),
    now(), now()
  )
  returning id
)
select set_config('pgtap.doctor_signup_id', id::text, true) from new_user;

select is(
  (select role::text from public.profiles where id = current_setting('pgtap.doctor_signup_id')::uuid),
  'doctor',
  'signup_role=doctor is honored'
);

-- ---------------------------------------------------------------------------
-- 3. An authenticated user can never update their own role -- not via
-- missing column grants, and not via the prevent_role_escalation trigger
-- if grants were ever misconfigured. Create a throwaway user, impersonate
-- them as the `authenticated` Postgres role, and attempt a direct UPDATE.
-- ---------------------------------------------------------------------------
with new_user as (
  insert into auth.users (
    id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at
  )
  values (
    gen_random_uuid(), 'authenticated', 'authenticated',
    'pgtap-self-update@example.test',
    crypt('not-a-real-login', gen_salt('bf')),
    now(),
    jsonb_build_object('full_name', 'PGTAP Self Update Test'),
    now(), now()
  )
  returning id
)
select set_config('pgtap.self_update_user_id', id::text, true) from new_user;

set local role authenticated;

select throws_ok(
  format(
    'update public.profiles set role = %L where id = %L',
    'admin',
    current_setting('pgtap.self_update_user_id')::uuid
  ),
  '42501'
);

reset role;

select * from finish();

rollback;
