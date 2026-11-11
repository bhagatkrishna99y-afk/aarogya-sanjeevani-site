-- End-to-end backend verification: three complete user journeys exercised
-- through the real public RPCs/views only (never a raw table write for
-- anything an app would actually do -- the only direct-table steps are
-- auth.users/auth.identities signup, which has no RPC equivalent, and one
-- deliberate scheduled_start backdate so "completed" can be reached
-- without the test waiting in real time for an appointment to pass).
--
-- Journey 1 (happy path, full lifecycle): patient signs up -> doctor
-- signs up -> doctor submits profile -> admin approves -> doctor sets
-- services + availability -> patient reads open slots -> patient creates
-- a request -> doctor accepts -> confirms -> completes -> patient reads
-- the timeline -> notifications exist for both sides.
-- Journey 2: a second request, cancelled by the patient while requested.
-- Journey 3: a third request, rejected by the doctor with a reason.
--
-- Same DO-block error/result-capture pattern as the rest of this test
-- suite (every custom error shares SQLSTATE P0001).
begin;

select plan(42);

-- =============================================================================
-- Shared setup: one patient, one doctor, one admin, one category/service.
-- =============================================================================
with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'e2e-patient@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'E2E Patient'), now(), now())
  returning id
) select set_config('e2e.patient', id::text, true) from u;

-- Doctor signup: signup_role in raw_user_meta_data is what handle_new_user()
-- reads to set profiles.role = 'doctor' immediately at signup (confirmed
-- in handle_new_user()'s own body) -- this is the exact mechanism the
-- Doctor app's signup screen relies on.
with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'e2e-doctor@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Dr. E2E', 'signup_role', 'doctor'), now(), now())
  returning id
) select set_config('e2e.doctor', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'e2e-admin@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'E2E Admin'), now(), now())
  returning id
) select set_config('e2e.admin', id::text, true) from u;
update public.profiles set role = 'admin' where id = current_setting('e2e.admin')::uuid;

with c as (
  insert into public.service_categories (name, slug, is_active)
  values ('E2E Category', 'e2e-category', true)
  returning id
) select set_config('e2e.category', id::text, true) from c;

with s as (
  insert into public.services (category_id, name, slug, requires_doctor, duration_minutes, is_active, supported_modes)
  values (current_setting('e2e.category')::uuid, 'E2E Consultation', 'e2e-consultation', true, 30, true, array['in_person']::service_mode[])
  returning id
) select set_config('e2e.service', id::text, true) from s;

create or replace function pg_temp.capture(p_sql text) returns text language plpgsql as $$
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
-- Journey 1: happy path, full lifecycle.
-- =============================================================================

-- Doctor submits their professional profile.
select set_config('request.jwt.claim.sub', current_setting('e2e.doctor'), true);
select is(
  pg_temp.capture('select public.submit_doctor_profile(''Dr.'', ''General Medicine'', ''MBBS'', ''Bio.'', 10, ''E2E-LIC-1'', array[''in_person'']::service_mode[], 30)'),
  'OK', '1. doctor submits their professional profile'
);
reset role;
select is(
  (select status::text from public.doctor_profiles where user_id = current_setting('e2e.doctor')::uuid),
  'pending', '1a. a fresh submission lands as pending'
);
set local role authenticated;

-- Admin approves.
select set_config('request.jwt.claim.sub', current_setting('e2e.admin'), true);
select is(
  pg_temp.capture(format('select public.admin_set_doctor_status(%L::uuid, %L, null)', current_setting('e2e.doctor'), 'approved')),
  'OK', '2. admin approves the doctor'
);
reset role;
select is(
  (select status::text from public.doctor_profiles where user_id = current_setting('e2e.doctor')::uuid),
  'approved', '2a. doctor_profiles.status is now approved'
);
select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('e2e.doctor')::uuid and type = 'doctor_approved') > 0,
  '2b. the doctor is notified of approval'
);
set local role authenticated;

-- Doctor configures services and availability.
select set_config('request.jwt.claim.sub', current_setting('e2e.doctor'), true);
select is(
  pg_temp.capture(format('select public.set_doctor_services(array[%L]::uuid[])', current_setting('e2e.service'))),
  'OK', '3. doctor sets their offered services'
);
reset role;
select ok(
  (select count(*)::int from public.doctor_services
     where doctor_id = current_setting('e2e.doctor')::uuid and service_id = current_setting('e2e.service')::uuid) = 1,
  '3a. the doctor_services row exists'
);
set local role authenticated;

select set_config('e2e.target_date', (current_date + 2)::text, true);
select is(
  pg_temp.capture(format(
    $sql$select public.set_doctor_availability(
      jsonb_build_array(jsonb_build_object(
        'day_of_week', %s, 'start_time', '00:00', 'end_time', '23:30', 'slot_duration_minutes', 30
      ))
    )$sql$,
    extract(dow from current_setting('e2e.target_date')::date)
  )),
  'OK', '4. doctor sets their weekly availability'
);
reset role;
select ok(
  (select count(*)::int from public.doctor_weekly_availability where doctor_id = current_setting('e2e.doctor')::uuid) = 1,
  '4a. the availability row exists'
);
set local role authenticated;

-- Patient reads open slots.
select set_config('request.jwt.claim.sub', current_setting('e2e.patient'), true);
with slots as (
  select slot_start, slot_end, row_number() over (order by slot_start) as rn
  from public.get_doctor_open_slots(current_setting('e2e.doctor')::uuid, current_setting('e2e.target_date')::date)
)
select set_config('e2e.slot_start', (select slot_start::text from slots where rn = 1), true);
select ok(
  current_setting('e2e.slot_start', true) is not null,
  '5. patient reads at least one open slot for the doctor'
);

-- Patient creates a request using that exact slot.
do $$
declare
  v_id uuid;
begin
  v_id := public.create_service_request(
    current_setting('e2e.service')::uuid, current_setting('e2e.doctor')::uuid,
    current_setting('e2e.slot_start')::timestamptz, 'in_person'::service_mode,
    'Looking forward to it.', '+91 90000 00001', null
  );
  perform set_config('e2e.request_1', v_id::text, true);
end;
$$;
select ok(current_setting('e2e.request_1', true) is not null, '6. patient creates a request for that slot');
reset role;
select is(
  (select status::text from public.requests where id = current_setting('e2e.request_1')::uuid),
  'requested', '6a. the new request has status requested'
);
set local role authenticated;

-- Doctor accepts, confirms, completes.
select set_config('request.jwt.claim.sub', current_setting('e2e.doctor'), true);
select is(
  pg_temp.capture(format('select public.transition_request_status(%L::uuid, %L, null)', current_setting('e2e.request_1'), 'accepted')),
  'OK', '7. doctor accepts the request'
);
reset role;
select is(
  (select status::text from public.requests where id = current_setting('e2e.request_1')::uuid),
  'accepted', '7a. status is now accepted'
);
select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('e2e.patient')::uuid and type = 'request_accepted'
       and related_request_id = current_setting('e2e.request_1')::uuid) > 0,
  '7b. the patient is notified of acceptance'
);
set local role authenticated;

select is(
  pg_temp.capture(format('select public.transition_request_status(%L::uuid, %L, null)', current_setting('e2e.request_1'), 'confirmed')),
  'OK', '8. doctor confirms the request'
);
reset role;
select is(
  (select status::text from public.requests where id = current_setting('e2e.request_1')::uuid),
  'confirmed', '8a. status is now confirmed'
);
select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('e2e.patient')::uuid and type = 'request_confirmed'
       and related_request_id = current_setting('e2e.request_1')::uuid) > 0,
  '8b. the patient is notified of confirmation'
);

-- Backdate scheduled_start so "completed" doesn't need to wait in real
-- time for the appointment to actually pass -- a status column isn't
-- touched, so none of the status-transition triggers fire from this.
update public.requests set scheduled_start = now() - interval '1 hour' where id = current_setting('e2e.request_1')::uuid;
set local role authenticated;

select set_config('request.jwt.claim.sub', current_setting('e2e.doctor'), true);
select is(
  pg_temp.capture(format('select public.transition_request_status(%L::uuid, %L, null)', current_setting('e2e.request_1'), 'completed')),
  'OK', '9. doctor completes the request (after its start time)'
);
reset role;
select is(
  (select status::text from public.requests where id = current_setting('e2e.request_1')::uuid),
  'completed', '9a. status is now completed'
);
select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('e2e.patient')::uuid and type = 'request_completed'
       and related_request_id = current_setting('e2e.request_1')::uuid) > 0,
  '9b. the patient is notified of completion'
);

-- Patient reads their timeline: the view, plus the full status log.
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('e2e.patient'), true);
select is(
  (select status::text from public.patient_requests_view where id = current_setting('e2e.request_1')::uuid),
  'completed', '10. patient_requests_view reflects the final status'
);
select is(
  (select count(*)::int from public.request_status_history where request_id = current_setting('e2e.request_1')::uuid),
  4, '10a. the timeline has exactly 4 entries: requested, accepted, confirmed, completed'
);
select is(
  (select array_agg(new_status::text order by changed_at)
     from public.request_status_history where request_id = current_setting('e2e.request_1')::uuid),
  array['requested', 'accepted', 'confirmed', 'completed'],
  '10b. the timeline is in the exact expected order'
);

-- Notifications exist for both sides across the whole journey.
reset role;
select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('e2e.patient')::uuid
       and related_request_id = current_setting('e2e.request_1')::uuid) >= 4,
  '11. the patient received at least one notification per lifecycle step'
);
select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('e2e.doctor')::uuid
       and related_request_id = current_setting('e2e.request_1')::uuid) >= 1,
  '11a. the doctor received at least the new-request notification'
);

-- =============================================================================
-- Journey 2: cancellation (patient cancels while still requested).
-- =============================================================================
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('e2e.patient'), true);

with slots as (
  select slot_start from public.get_doctor_open_slots(current_setting('e2e.doctor')::uuid, current_setting('e2e.target_date')::date)
  order by slot_start offset 1 limit 1
)
select set_config('e2e.slot2_start', (select slot_start::text from slots), true);
select ok(current_setting('e2e.slot2_start', true) is not null, '12. a second open slot is available');

do $$
declare
  v_id uuid;
begin
  v_id := public.create_service_request(
    current_setting('e2e.service')::uuid, current_setting('e2e.doctor')::uuid,
    current_setting('e2e.slot2_start')::timestamptz, 'in_person'::service_mode,
    null, '+91 90000 00002', null
  );
  perform set_config('e2e.request_2', v_id::text, true);
end;
$$;
select ok(current_setting('e2e.request_2', true) is not null, '13. patient creates a second request');

select is(
  pg_temp.capture(format(
    'select public.transition_request_status(%L::uuid, %L, %L)', current_setting('e2e.request_2'), 'cancelled', 'Changed my mind.'
  )),
  'OK', '14. patient cancels their own requested request'
);
reset role;
select is(
  (select status::text from public.requests where id = current_setting('e2e.request_2')::uuid),
  'cancelled', '14a. status is now cancelled'
);
select is(
  (select cancellation_reason from public.requests where id = current_setting('e2e.request_2')::uuid),
  'Changed my mind.', '14b. the cancellation reason is stored'
);
select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('e2e.doctor')::uuid and type = 'request_cancelled'
       and related_request_id = current_setting('e2e.request_2')::uuid) > 0,
  '14c. the doctor is notified of the cancellation'
);
select is(
  (select count(*)::int from public.request_status_history
     where request_id = current_setting('e2e.request_2')::uuid and new_status = 'cancelled'),
  1, '14d. exactly one cancellation entry is logged'
);

-- =============================================================================
-- Journey 3: rejection (doctor rejects with a reason).
-- =============================================================================
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('e2e.patient'), true);

with slots as (
  select slot_start from public.get_doctor_open_slots(current_setting('e2e.doctor')::uuid, current_setting('e2e.target_date')::date)
  order by slot_start offset 2 limit 1
)
select set_config('e2e.slot3_start', (select slot_start::text from slots), true);
select ok(current_setting('e2e.slot3_start', true) is not null, '15. a third open slot is available');

do $$
declare
  v_id uuid;
begin
  v_id := public.create_service_request(
    current_setting('e2e.service')::uuid, current_setting('e2e.doctor')::uuid,
    current_setting('e2e.slot3_start')::timestamptz, 'in_person'::service_mode,
    null, '+91 90000 00003', null
  );
  perform set_config('e2e.request_3', v_id::text, true);
end;
$$;
select ok(current_setting('e2e.request_3', true) is not null, '16. patient creates a third request');

select set_config('request.jwt.claim.sub', current_setting('e2e.doctor'), true);
select is(
  pg_temp.capture(format(
    'select public.transition_request_status(%L::uuid, %L, %L)', current_setting('e2e.request_3'), 'rejected', 'Fully booked that day.'
  )),
  'OK', '17. doctor rejects the request with a reason'
);
reset role;
select is(
  (select status::text from public.requests where id = current_setting('e2e.request_3')::uuid),
  'rejected', '17a. status is now rejected'
);
select is(
  (select rejection_reason from public.requests where id = current_setting('e2e.request_3')::uuid),
  'Fully booked that day.', '17b. the rejection reason is stored'
);
select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('e2e.patient')::uuid and type = 'request_rejected'
       and related_request_id = current_setting('e2e.request_3')::uuid
       and body like '%Fully booked that day.%') > 0,
  '17c. the patient is notified, with the reason included in the message'
);
select is(
  (select count(*)::int from public.request_status_history
     where request_id = current_setting('e2e.request_3')::uuid and new_status = 'rejected'),
  1, '17d. exactly one rejection entry is logged'
);

-- Final sanity: the slot the cancelled/rejected requests used is free
-- again (both statuses are outside the EXCLUDE constraint's scope, so
-- nothing blocks a fresh request at the same time -- prove it by
-- confirming the slot is still/again listed as open).
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('e2e.patient'), true);
select ok(
  (select count(*)::int from public.get_doctor_open_slots(current_setting('e2e.doctor')::uuid, current_setting('e2e.target_date')::date)
     where slot_start = current_setting('e2e.slot2_start')::timestamptz) = 1,
  '18. the cancelled request''s slot is open again'
);
select ok(
  (select count(*)::int from public.get_doctor_open_slots(current_setting('e2e.doctor')::uuid, current_setting('e2e.target_date')::date)
     where slot_start = current_setting('e2e.slot3_start')::timestamptz) = 1,
  '18a. the rejected request''s slot is open again'
);

reset role;

select * from finish();

rollback;
