-- Proves create_service_request's two rate limits from
-- 20261112000013_rate_limiting.sql: at most 10 active requests per
-- patient at once, and at most 5 created per rolling hour. See
-- docs/performance.md's "Rate limiting" section for the design notes.
--
-- Same error-code-capture pattern as the other test files in this series.
-- A no-doctor, home_visit service is used throughout so these tests don't
-- need any doctor/availability fixtures at all -- just the patient and
-- the service.
begin;

select plan(8);

with c as (
  insert into public.service_categories (name, slug, is_active)
  values ('PGTAP Rate Category', 'pgtap-rate-category', true)
  returning id
)
select set_config('pgtap.category_id', id::text, true) from c;

with s as (
  insert into public.services (category_id, name, slug, requires_doctor, duration_minutes, is_active, supported_modes)
  values (current_setting('pgtap.category_id')::uuid, 'PGTAP Rate Service', 'pgtap-rate-service', false, 15, true, array['home_visit']::service_mode[])
  returning id
)
select set_config('pgtap.service_id', id::text, true) from s;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-rate-patientactive@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Rate Patient Active'), now(), now())
  returning id
) select set_config('pgtap.patient_active', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-rate-patientrate@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Rate Patient Hourly'), now(), now())
  returning id
) select set_config('pgtap.patient_rate', id::text, true) from u;

create or replace function pg_temp.try_as(p_sql text) returns text language plpgsql as $$
declare
  v_code text;
begin
  begin
    execute p_sql;
    v_code := 'OK';
  exception when others then
    v_code := split_part(sqlerrm, ':', 1);
  end;
  return v_code;
end;
$$;

-- Backdated created_at (older than the 1-hour rate window) so these 9
-- fixture rows count toward the active-request cap only, not the hourly
-- rate limit -- isolates the two checks from each other.
insert into public.requests (
  patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status, contact_phone, address_text, created_at
)
select
  current_setting('pgtap.patient_active')::uuid, null, current_setting('pgtap.service_id')::uuid,
  now() + (g || ' day')::interval, now() + (g || ' day')::interval + interval '15 minute',
  'home_visit', 'requested', '+91 90000 00001', '123 Test Street', now() - interval '3 hour'
from generate_series(1, 9) as g;

set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_active'), true);

select is(
  pg_temp.try_as(format(
    $sql$select public.create_service_request(%L::uuid, null, now() + interval '20 day', 'home_visit'::service_mode, null, '+91 90000 00002', '123 Test Street')$sql$,
    current_setting('pgtap.service_id')
  )),
  'OK',
  'the 10th active request (at the cap) still succeeds'
);

select is(
  pg_temp.try_as(format(
    $sql$select public.create_service_request(%L::uuid, null, now() + interval '21 day', 'home_visit'::service_mode, null, '+91 90000 00003', '123 Test Street')$sql$,
    current_setting('pgtap.service_id')
  )),
  'AS034_TOO_MANY_ACTIVE_REQUESTS',
  'the 11th active request is rejected -- too many active requests'
);

-- A fresh patient for the hourly rate limit, so it isn't entangled with
-- the active-count cap above (5 < 10, so only the rate limit can fire).
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_rate'), true);

select is(
  pg_temp.try_as(format(
    $sql$select public.create_service_request(%L::uuid, null, now() + interval '1 day', 'home_visit'::service_mode, null, '+91 90000 00004', '123 Test Street')$sql$,
    current_setting('pgtap.service_id')
  )), 'OK', 'request 1 of 5 in the hour succeeds'
);
select is(
  pg_temp.try_as(format(
    $sql$select public.create_service_request(%L::uuid, null, now() + interval '2 day', 'home_visit'::service_mode, null, '+91 90000 00005', '123 Test Street')$sql$,
    current_setting('pgtap.service_id')
  )), 'OK', 'request 2 of 5 in the hour succeeds'
);
select is(
  pg_temp.try_as(format(
    $sql$select public.create_service_request(%L::uuid, null, now() + interval '3 day', 'home_visit'::service_mode, null, '+91 90000 00006', '123 Test Street')$sql$,
    current_setting('pgtap.service_id')
  )), 'OK', 'request 3 of 5 in the hour succeeds'
);
select is(
  pg_temp.try_as(format(
    $sql$select public.create_service_request(%L::uuid, null, now() + interval '4 day', 'home_visit'::service_mode, null, '+91 90000 00007', '123 Test Street')$sql$,
    current_setting('pgtap.service_id')
  )), 'OK', 'request 4 of 5 in the hour succeeds'
);
select is(
  pg_temp.try_as(format(
    $sql$select public.create_service_request(%L::uuid, null, now() + interval '5 day', 'home_visit'::service_mode, null, '+91 90000 00008', '123 Test Street')$sql$,
    current_setting('pgtap.service_id')
  )), 'OK', 'request 5 of 5 in the hour succeeds'
);

select is(
  pg_temp.try_as(format(
    $sql$select public.create_service_request(%L::uuid, null, now() + interval '6 day', 'home_visit'::service_mode, null, '+91 90000 00009', '123 Test Street')$sql$,
    current_setting('pgtap.service_id')
  )), 'AS035_RATE_LIMITED', 'the 6th request within the hour is rate-limited'
);

reset role;

select * from finish();

rollback;
