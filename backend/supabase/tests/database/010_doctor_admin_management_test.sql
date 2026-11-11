-- Proves the doctor lifecycle + admin RPCs from 20261112000010/000011:
-- a doctor cannot approve themselves, a patient cannot call any admin
-- function, only admin can approve, suspending a doctor cascades
-- correctly (future requested -> rejected, future accepted -> flagged,
-- past requested untouched), the last admin cannot be demoted, and audit
-- rows are written. Also spot-checks submit_doctor_profile's resubmission/
-- re-review rules and set_doctor_availability's validation.
--
-- Same DO-block error-code-capture pattern as the two prior test files in
-- this series (every custom error shares SQLSTATE P0001).
begin;

select plan(47);

-- =============================================================================
-- Fixtures
-- =============================================================================
with c as (
  insert into public.service_categories (name, slug, is_active)
  values ('PGTAP Admin Category', 'pgtap-admin-category', true)
  returning id
)
select set_config('pgtap.category_id', id::text, true) from c;

with s as (
  insert into public.services (category_id, name, slug, requires_doctor, duration_minutes, is_active, supported_modes)
  values (current_setting('pgtap.category_id')::uuid, 'PGTAP Admin Service', 'pgtap-admin-service', true, 30, true, array['in_person']::service_mode[])
  returning id
)
select set_config('pgtap.service_id', id::text, true) from s;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-admgmt-patienta@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Admgmt Patient A'), now(), now())
  returning id
) select set_config('pgtap.patient_a', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-admgmt-doctora@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Admgmt Doctor A', 'signup_role', 'doctor'), now(), now())
  returning id
) select set_config('pgtap.doctor_a', id::text, true) from u;

insert into public.doctor_profiles (user_id, specialization, license_number, status, accepting_requests, consultation_modes)
values (current_setting('pgtap.doctor_a')::uuid, 'General Medicine', 'PGTAP-ADM-LIC-A', 'pending', true, array['in_person']::service_mode[]);

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-admgmt-doctorb@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Admgmt Doctor B', 'signup_role', 'doctor'), now(), now())
  returning id
) select set_config('pgtap.doctor_b', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-admgmt-approveddoc@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Admgmt Approved Doc', 'signup_role', 'doctor'), now(), now())
  returning id
) select set_config('pgtap.doctor_c', id::text, true) from u;

insert into public.doctor_profiles (user_id, specialization, license_number, status, accepting_requests, consultation_modes)
values (current_setting('pgtap.doctor_c')::uuid, 'Dermatology', 'PGTAP-ADM-LIC-C', 'approved', true, array['in_person']::service_mode[]);

-- Doctor D stays approved and untouched for the whole file -- doctor_a
-- gets suspended and doctor_c gets sent back to pending by earlier tests,
-- so set_doctor_availability/set_accepting_requests (which require an
-- actually-approved doctor) need their own clean fixture.
with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-admgmt-doctord@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Admgmt Doctor D', 'signup_role', 'doctor'), now(), now())
  returning id
) select set_config('pgtap.doctor_d', id::text, true) from u;

insert into public.doctor_profiles (user_id, specialization, license_number, status, accepting_requests, consultation_modes)
values (current_setting('pgtap.doctor_d')::uuid, 'Orthopedics', 'PGTAP-ADM-LIC-D', 'approved', true, array['in_person']::service_mode[]);

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-admgmt-admina@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Admgmt Admin A'), now(), now())
  returning id
) select set_config('pgtap.admin_a', id::text, true) from u;

update public.profiles set role = 'admin' where id = current_setting('pgtap.admin_a')::uuid;

-- The live database already has at least one real admin account (this
-- project's own setup), which would otherwise defeat the "admin_a is the
-- sole admin" premise the last-admin-demotion test below depends on.
-- Neutralizing every other admin here is safe -- this whole file runs
-- inside one transaction that always rolls back.
update public.profiles set role = 'user' where role = 'admin' and id <> current_setting('pgtap.admin_a')::uuid;

-- Dedicated fixture for the last-admin-demotion test, kept separate from
-- doctor_b so promoting/demoting it doesn't disturb doctor_b's role = 'doctor'
-- (needed later for the submit_doctor_profile resubmission test).
with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-admgmt-secondadmin@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Admgmt Second Admin'), now(), now())
  returning id
) select set_config('pgtap.second_admin_candidate', id::text, true) from u;

-- Error-code capture helper, reused across this file.
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

set local role authenticated;

-- =============================================================================
-- A doctor cannot approve themselves.
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);
select is(
  pg_temp.try_as(format('select public.admin_set_doctor_status(%L::uuid, %L, null)', current_setting('pgtap.doctor_a'), 'approved')),
  'AS027_NOT_AUTHORIZED',
  'a doctor cannot approve themselves'
);

-- An unrelated doctor cannot approve another doctor either.
select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_b'), true);
select is(
  pg_temp.try_as(format('select public.admin_set_doctor_status(%L::uuid, %L, null)', current_setting('pgtap.doctor_a'), 'approved')),
  'AS027_NOT_AUTHORIZED',
  'an unrelated doctor cannot approve another doctor'
);

-- =============================================================================
-- A patient cannot call any admin function.
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);

select is(
  pg_temp.try_as(format('select public.admin_set_doctor_status(%L::uuid, %L, null)', current_setting('pgtap.doctor_a'), 'approved')),
  'AS027_NOT_AUTHORIZED',
  'a patient cannot call admin_set_doctor_status'
);
select is(
  pg_temp.try_as(format('select public.admin_set_user_role(%L::uuid, %L)', current_setting('pgtap.doctor_a'), 'admin')),
  'AS027_NOT_AUTHORIZED',
  'a patient cannot call admin_set_user_role'
);
select is(
  pg_temp.try_as(format('select public.admin_set_user_suspended(%L::uuid, true)', current_setting('pgtap.doctor_a'))),
  'AS027_NOT_AUTHORIZED',
  'a patient cannot call admin_set_user_suspended'
);
select is(
  pg_temp.try_as('select public.admin_upsert_service_category(null, ''Patient Attempt'', null, null, true, 0)'),
  'AS027_NOT_AUTHORIZED',
  'a patient cannot call admin_upsert_service_category'
);
select is(
  pg_temp.try_as('select public.admin_set_category_active(gen_random_uuid(), false)'),
  'AS027_NOT_AUTHORIZED',
  'a patient cannot call admin_set_category_active'
);
select is(
  pg_temp.try_as(format('select public.admin_upsert_service(null, %L::uuid, ''x'', null, null, true, 30, array[''in_person'']::service_mode[], true, 0)', current_setting('pgtap.category_id'))),
  'AS027_NOT_AUTHORIZED',
  'a patient cannot call admin_upsert_service'
);
select is(
  pg_temp.try_as(format('select public.admin_set_service_active(%L::uuid, false)', current_setting('pgtap.service_id'))),
  'AS027_NOT_AUTHORIZED',
  'a patient cannot call admin_set_service_active'
);
select is(
  pg_temp.try_as('select * from public.admin_list_doctors(null, 20, 0)'),
  'AS027_NOT_AUTHORIZED',
  'a patient cannot call admin_list_doctors'
);
select is(
  pg_temp.try_as('select * from public.admin_list_requests(null, null, null, null, null, 20, 0)'),
  'AS027_NOT_AUTHORIZED',
  'a patient cannot call admin_list_requests'
);

-- =============================================================================
-- Only admin can approve; audit row is written.
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.admin_a'), true);
select is(
  pg_temp.try_as(format('select public.admin_set_doctor_status(%L::uuid, %L, null)', current_setting('pgtap.doctor_a'), 'approved')),
  'OK',
  'only admin can approve a pending doctor'
);

reset role;
select is(
  (select status::text from public.doctor_profiles where user_id = current_setting('pgtap.doctor_a')::uuid),
  'approved', 'approving sets doctor_profiles.status to approved'
);
select is(
  (select role::text from public.profiles where id = current_setting('pgtap.doctor_a')::uuid),
  'doctor', 'approving sets profiles.role to doctor'
);
select ok(
  (select count(*)::int from public.audit_log
     where action = 'admin_set_doctor_status' and entity_id = current_setting('pgtap.doctor_a')::uuid) > 0,
  'an audit_log row is written for admin_set_doctor_status'
);
set local role authenticated;

-- =============================================================================
-- Suspending a doctor cascades correctly.
-- =============================================================================
reset role;
with r as (
  insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status, contact_phone)
  values (current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, current_setting('pgtap.service_id')::uuid,
    now() + interval '2 day', now() + interval '2 day 30 minute', 'in_person', 'requested', '+91 90000 00001')
  returning id
) select set_config('pgtap.future_requested', id::text, true) from r;

with r as (
  insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status, contact_phone)
  values (current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, current_setting('pgtap.service_id')::uuid,
    now() + interval '3 day', now() + interval '3 day 30 minute', 'in_person', 'accepted', '+91 90000 00002')
  returning id
) select set_config('pgtap.future_accepted', id::text, true) from r;

with r as (
  insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status, contact_phone)
  values (current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, current_setting('pgtap.service_id')::uuid,
    now() - interval '10 day', now() - interval '10 day' + interval '30 minute', 'in_person', 'requested', '+91 90000 00003')
  returning id
) select set_config('pgtap.past_requested', id::text, true) from r;

set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('pgtap.admin_a'), true);
select is(
  pg_temp.try_as(format('select public.admin_set_doctor_status(%L::uuid, %L, %L)', current_setting('pgtap.doctor_a'), 'suspended', 'Under investigation.')),
  'OK',
  'admin can suspend an approved doctor'
);

reset role;
select is(
  (select status::text from public.requests where id = current_setting('pgtap.future_requested')::uuid),
  'rejected', 'a future requested request is auto-rejected when the doctor is suspended'
);
select is(
  (select rejection_reason from public.requests where id = current_setting('pgtap.future_requested')::uuid),
  'The assigned doctor is no longer available.', 'the auto-rejection carries a clear reason'
);
select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('pgtap.patient_a')::uuid and type = 'request_rejected'
       and related_request_id = current_setting('pgtap.future_requested')::uuid) > 0,
  'the patient is notified their future requested request was rejected'
);
select is(
  (select status::text from public.requests where id = current_setting('pgtap.future_accepted')::uuid),
  'accepted', 'a future accepted request is left alone (not auto-cancelled), per the documented rule'
);
select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('pgtap.patient_a')::uuid and type = 'system'
       and related_request_id = current_setting('pgtap.future_accepted')::uuid) > 0,
  'the patient is notified their future accepted request needs review'
);
select ok(
  (select count(*)::int from public.audit_log
     where action = 'accepted_request_flagged_for_review' and entity_id = current_setting('pgtap.future_accepted')::uuid) > 0,
  'the future accepted request is flagged for review in audit_log'
);
select is(
  (select status::text from public.requests where id = current_setting('pgtap.past_requested')::uuid),
  'requested', 'a PAST requested request is untouched by the suspension cascade (future-only scope)'
);
select ok(
  (select count(*)::int from public.audit_log
     where action = 'doctor_suspension_auto_rejected_request' and entity_id = current_setting('pgtap.future_requested')::uuid) > 0,
  'the auto-rejection itself is also captured in audit_log'
);

-- =============================================================================
-- The last admin cannot be demoted; demoting with a second admin succeeds.
-- =============================================================================
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('pgtap.admin_a'), true);
select is(
  pg_temp.try_as(format('select public.admin_set_user_role(%L::uuid, %L)', current_setting('pgtap.admin_a'), 'user')),
  'AS031_CANNOT_DEMOTE_LAST_ADMIN',
  'the sole remaining admin cannot be demoted'
);

reset role;
update public.profiles set role = 'admin' where id = current_setting('pgtap.second_admin_candidate')::uuid;
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('pgtap.admin_a'), true);

select is(
  pg_temp.try_as(format('select public.admin_set_user_role(%L::uuid, %L)', current_setting('pgtap.second_admin_candidate'), 'user')),
  'OK',
  'demoting an admin succeeds when another admin remains'
);
reset role;
select is(
  (select role::text from public.profiles where id = current_setting('pgtap.second_admin_candidate')::uuid),
  'user', 'the demoted account''s role is updated'
);
select ok(
  (select count(*)::int from public.audit_log
     where action = 'admin_set_user_role' and entity_id = current_setting('pgtap.second_admin_candidate')::uuid) > 0,
  'an audit_log row is written for admin_set_user_role'
);
set local role authenticated;

-- =============================================================================
-- submit_doctor_profile
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);
select is(
  pg_temp.try_as('select public.submit_doctor_profile(null, ''Cardiology'', null, null, 5, ''LIC-X'', array[''in_person'']::service_mode[], 30)'),
  'AS022_NOT_A_DOCTOR_ACCOUNT',
  'a non-doctor-role account cannot submit a professional profile'
);

-- doctor_b is role='doctor' (from signup) but has no doctor_profiles row yet.
select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_b'), true);
select is(
  pg_temp.try_as('select public.submit_doctor_profile(''Dr.'', ''Cardiology'', ''MBBS'', null, 5, ''LIC-NEW-B'', array[''in_person'']::service_mode[], 30)'),
  'OK',
  'a first-time doctor-role account can submit a professional profile'
);
reset role;
select is(
  (select status::text from public.doctor_profiles where user_id = current_setting('pgtap.doctor_b')::uuid),
  'pending', 'a brand-new submission lands as pending'
);
select ok(
  (select count(*)::int from public.audit_log
     where action = 'doctor_profile_submitted' and entity_id = current_setting('pgtap.doctor_b')::uuid) > 0,
  'the initial submission is logged to audit_log'
);
set local role authenticated;

-- Reject doctor_b, then have them resubmit.
select set_config('request.jwt.claim.sub', current_setting('pgtap.admin_a'), true);
select is(
  pg_temp.try_as(format('select public.admin_set_doctor_status(%L::uuid, %L, %L)', current_setting('pgtap.doctor_b'), 'rejected', 'Incomplete documents.')),
  'OK',
  'admin can reject a pending doctor'
);

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_b'), true);
select is(
  pg_temp.try_as('select public.submit_doctor_profile(''Dr.'', ''Cardiology'', ''MBBS, MD'', null, 6, ''LIC-NEW-B'', array[''in_person'']::service_mode[], 30)'),
  'OK',
  'a rejected doctor can resubmit'
);
reset role;
select is(
  (select status::text from public.doctor_profiles where user_id = current_setting('pgtap.doctor_b')::uuid),
  'pending', 'resubmission moves status back to pending'
);
select is(
  (select rejection_reason from public.doctor_profiles where user_id = current_setting('pgtap.doctor_b')::uuid),
  null, 'resubmission clears the prior rejection_reason'
);
select ok(
  (select count(*)::int from public.audit_log
     where action = 'doctor_profile_resubmitted' and entity_id = current_setting('pgtap.doctor_b')::uuid) > 0,
  'the resubmission is logged to audit_log'
);
set local role authenticated;

-- An approved doctor changing their license_number is sent back to pending.
select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_c'), true);
select is(
  pg_temp.try_as('select public.submit_doctor_profile(''Dr.'', ''Dermatology'', ''MBBS'', ''Updated bio.'', 10, ''PGTAP-ADM-LIC-C-CHANGED'', array[''in_person'']::service_mode[], 30)'),
  'OK',
  'an approved doctor can edit their profile'
);
reset role;
select is(
  (select status::text from public.doctor_profiles where user_id = current_setting('pgtap.doctor_c')::uuid),
  'pending', 'changing license_number on an approved doctor resets status to pending for re-review'
);
select ok(
  (select count(*)::int from public.audit_log
     where action = 'doctor_profile_changed_pending_review' and entity_id = current_setting('pgtap.doctor_c')::uuid) > 0,
  'the forced re-review is logged to audit_log'
);
set local role authenticated;

-- =============================================================================
-- set_doctor_availability / set_accepting_requests
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_d'), true);

select is(
  pg_temp.try_as($sql$select public.set_doctor_availability(
    '[{"day_of_week":1,"start_time":"09:00","end_time":"12:00","slot_duration_minutes":30},
      {"day_of_week":1,"start_time":"11:00","end_time":"13:00","slot_duration_minutes":30}]'::jsonb
  )$sql$),
  'AS026_AVAILABILITY_OVERLAP',
  'overlapping windows in the same submitted batch are rejected'
);

select is(
  pg_temp.try_as($sql$select public.set_doctor_availability(
    '[{"day_of_week":2,"start_time":"09:00","end_time":"09:10","slot_duration_minutes":30}]'::jsonb
  )$sql$),
  'AS025_INVALID_AVAILABILITY_WINDOW',
  'a window shorter than its own slot duration is rejected'
);

select is(
  pg_temp.try_as($sql$select public.set_doctor_availability(
    '[{"day_of_week":3,"start_time":"09:00","end_time":"12:00","slot_duration_minutes":30}]'::jsonb
  )$sql$),
  'OK',
  'a valid availability window is accepted'
);

select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);
select is(
  pg_temp.try_as('select public.set_accepting_requests(false)'),
  'AS024_ONLY_APPROVED_DOCTORS',
  'a non-approved-doctor account cannot change set_accepting_requests'
);

-- Doctor C is 'pending' again (from the license-number change above), so
-- the same approved-only gate applies to them too now.
select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_c'), true);
select is(
  pg_temp.try_as('select public.set_accepting_requests(false)'),
  'AS024_ONLY_APPROVED_DOCTORS',
  'a doctor sent back to pending by their own profile edit also loses access to set_accepting_requests'
);

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_d'), true);
select is(
  pg_temp.try_as('select public.set_accepting_requests(false)'),
  'OK',
  'a still-approved doctor can change set_accepting_requests'
);
reset role;
select is(
  (select accepting_requests from public.doctor_profiles where user_id = current_setting('pgtap.doctor_d')::uuid),
  false, 'set_accepting_requests persists the new value'
);
set local role authenticated;

reset role;

select * from finish();

rollback;
