-- Proves transition_request_status() (20261112000009) enforces exactly the
-- allowed-transition graph and per-transition role rules, and that its
-- side effects (request_status_history row, notifications) fire exactly
-- once per transition via the pre-existing triggers. Also covers
-- mark_notification_read / mark_all_notifications_read /
-- get_unread_notification_count.
--
-- Error codes use the same DO-block-capture pattern as
-- 008_scheduling_business_logic_test.sql (every custom error shares
-- SQLSTATE P0001, so throws_ok alone can't distinguish which AS0xx code
-- fired -- see that file's header for the full rationale).
--
-- Only 'accepted'/'confirmed' rows for the same doctor must not overlap in
-- time (requests_no_doctor_overlap's EXCLUDE scope); 'requested',
-- 'completed', 'rejected' and 'cancelled' rows are exempt, so most fixture
-- timestamps below are arbitrary and only the accepted/confirmed ones are
-- deliberately spread across distinct days.
begin;

select plan(48);

-- =============================================================================
-- Fixtures
-- =============================================================================
with c as (
  insert into public.service_categories (name, slug, is_active)
  values ('PGTAP Status Category', 'pgtap-status-category', true)
  returning id
)
select set_config('pgtap.category_id', id::text, true) from c;

with s as (
  insert into public.services (category_id, name, slug, requires_doctor, duration_minutes, is_active, supported_modes)
  values (current_setting('pgtap.category_id')::uuid, 'PGTAP Status Service', 'pgtap-status-service', true, 30, true, array['in_person']::service_mode[])
  returning id
)
select set_config('pgtap.service_id', id::text, true) from s;

-- Users: two patients, two approved doctors, one pending (unapproved)
-- doctor, one admin.
with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-status-patienta@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Status Patient A'), now(), now())
  returning id
) select set_config('pgtap.patient_a', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-status-patientb@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Status Patient B'), now(), now())
  returning id
) select set_config('pgtap.patient_b', id::text, true) from u;

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-status-doctora@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Status Doctor A', 'signup_role', 'doctor'), now(), now())
  returning id
) select set_config('pgtap.doctor_a', id::text, true) from u;

insert into public.doctor_profiles (user_id, specialization, license_number, status, accepting_requests, consultation_modes)
values (current_setting('pgtap.doctor_a')::uuid, 'General Medicine', 'PGTAP-STATUS-LIC-A', 'approved', true, array['in_person']::service_mode[]);

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-status-doctorb@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Status Doctor B', 'signup_role', 'doctor'), now(), now())
  returning id
) select set_config('pgtap.doctor_b', id::text, true) from u;

insert into public.doctor_profiles (user_id, specialization, license_number, status, accepting_requests, consultation_modes)
values (current_setting('pgtap.doctor_b')::uuid, 'Dermatology', 'PGTAP-STATUS-LIC-B', 'approved', true, array['in_person']::service_mode[]);

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-status-doctorpending@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Status Doctor Pending', 'signup_role', 'doctor'), now(), now())
  returning id
) select set_config('pgtap.doctor_pending', id::text, true) from u;

insert into public.doctor_profiles (user_id, specialization, license_number, status, accepting_requests, consultation_modes)
values (current_setting('pgtap.doctor_pending')::uuid, 'Pediatrics', 'PGTAP-STATUS-LIC-P', 'pending', true, array['in_person']::service_mode[]);

with u as (
  insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at, raw_user_meta_data, created_at, updated_at)
  values (gen_random_uuid(), 'authenticated', 'authenticated', 'pgtap-status-admin@example.test',
    crypt('x', gen_salt('bf')), now(), jsonb_build_object('full_name', 'Status Admin'), now(), now())
  returning id
) select set_config('pgtap.admin_a', id::text, true) from u;

update public.profiles set role = 'admin' where id = current_setting('pgtap.admin_a')::uuid;

-- Helper macro-by-convention: every request fixture below is inserted
-- directly (as the unrestricted owner) so its starting status is whatever
-- the test needs, independent of the state machine itself.
create or replace function pg_temp.mk_request(
  p_patient uuid, p_doctor uuid, p_status public.request_status,
  p_start timestamptz, p_minutes int default 30
) returns uuid language plpgsql as $$
declare
  v_id uuid;
begin
  insert into public.requests (patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status, contact_phone)
  values (
    p_patient, p_doctor, current_setting('pgtap.service_id')::uuid,
    p_start, p_start + make_interval(mins => p_minutes), 'in_person', p_status, '+91 90000 00000'
  )
  returning id into v_id;
  return v_id;
end;
$$;

-- requests_patient_doctor_start_unique is keyed on (patient_id, doctor_id,
-- scheduled_start) for requested/accepted/confirmed rows -- every fixture
-- below that shares patient A + doctor A and lands in one of those three
-- statuses needs its own distinct scheduled_start, hence the staggered
-- hour offsets on an otherwise-arbitrary "tomorrow".
select set_config('pgtap.req_a1', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'requested', now() + interval '1 day 0 hour')::text, true);
select set_config('pgtap.req_a2', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'requested', now() + interval '10 day')::text, true);
-- The pre-existing conflicting accepted booking for doctor A, same time as req_a2.
select pg_temp.mk_request(current_setting('pgtap.patient_b')::uuid, current_setting('pgtap.doctor_a')::uuid, 'accepted', now() + interval '10 day');

select set_config('pgtap.req_unapproved', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_pending')::uuid, 'requested', now() + interval '1 day')::text, true);
select set_config('pgtap.req_reject1', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'requested', now() + interval '1 day 1 hour')::text, true);

select set_config('pgtap.req_confirm1', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'accepted', now() + interval '3 day')::text, true);
select set_config('pgtap.req_confirm_admin', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'accepted', now() + interval '4 day')::text, true);

select set_config('pgtap.req_complete_future', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'confirmed', now() + interval '5 day')::text, true);
select set_config('pgtap.req_complete_past', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'confirmed', now() - interval '5 day')::text, true);
select set_config('pgtap.req_complete_admin_past', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'confirmed', now() - interval '6 day')::text, true);

select set_config('pgtap.req_cancel_by_patient', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'requested', now() + interval '1 day 2 hour')::text, true);
select set_config('pgtap.req_cancel_by_doctor_forbidden', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'requested', now() + interval '1 day 3 hour')::text, true);
select set_config('pgtap.req_cancel_by_admin_requested', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'requested', now() + interval '1 day 4 hour')::text, true);
select set_config('pgtap.req_cancel_unrelated', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'requested', now() + interval '1 day 5 hour')::text, true);

select set_config('pgtap.req_cancel_accepted_by_doctor', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'accepted', now() + interval '6 day')::text, true);
select set_config('pgtap.req_cancel_accepted_by_patient', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'accepted', now() + interval '7 day')::text, true);
select set_config('pgtap.req_cancel_confirmed_by_admin', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'confirmed', now() + interval '8 day')::text, true);

select set_config('pgtap.req_terminal_completed', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'completed', now() - interval '20 day')::text, true);
select set_config('pgtap.req_terminal_rejected', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'rejected', now() + interval '1 day')::text, true);
select set_config('pgtap.req_terminal_cancelled', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'cancelled', now() + interval '1 day')::text, true);
select set_config('pgtap.req_noop', pg_temp.mk_request(current_setting('pgtap.patient_a')::uuid, current_setting('pgtap.doctor_a')::uuid, 'requested', now() + interval '1 day 6 hour')::text, true);

-- Generic capture helper used by every negative test below: calls
-- transition_request_status as whoever `request.jwt.claim.sub` names,
-- catches the exception, and stashes the AS0xx code prefix.
create or replace function pg_temp.try_transition(p_request text, p_status public.request_status, p_reason text default null)
returns text language plpgsql as $$
declare
  v_code text;
begin
  begin
    perform public.transition_request_status(current_setting(p_request)::uuid, p_status, p_reason);
    v_code := 'OK';
  exception when others then
    v_code := split_part(sqlerrm, ':', 1);
  end;
  return v_code;
end;
$$;

set local role authenticated;

-- =============================================================================
-- requested -> accepted
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);
select is(pg_temp.try_transition('pgtap.req_a1', 'accepted'), 'AS017_NOT_AUTHORIZED_FOR_TRANSITION',
  'the patient cannot accept their own request');

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_b'), true);
select is(pg_temp.try_transition('pgtap.req_a1', 'accepted'), 'AS017_NOT_AUTHORIZED_FOR_TRANSITION',
  'doctor B cannot accept doctor A''s request');

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_pending'), true);
select is(pg_temp.try_transition('pgtap.req_unapproved', 'accepted'), 'AS018_DOCTOR_NOT_APPROVED',
  'a pending (not approved) doctor cannot accept their own assigned request');

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);
select is(pg_temp.try_transition('pgtap.req_a2', 'accepted'), 'AS019_SLOT_ALREADY_BOOKED',
  'accepting is rejected when a conflicting accepted request already exists for that doctor/time');

select is(
  (select status::text from public.requests where id = current_setting('pgtap.req_a2')::uuid),
  'requested',
  'the conflicting request stays requested after the failed accept attempt'
);

select is(pg_temp.try_transition('pgtap.req_a1', 'accepted'), 'OK',
  'happy path: the assigned, approved doctor can accept a requested request');

select is(
  (select status::text from public.requests where id = current_setting('pgtap.req_a1')::uuid),
  'accepted', 'accepting sets status to accepted'
);
select ok(
  (select accepted_at from public.requests where id = current_setting('pgtap.req_a1')::uuid) is not null,
  'accepting sets accepted_at'
);
select is(
  (select count(*)::int from public.request_status_history
     where request_id = current_setting('pgtap.req_a1')::uuid and new_status = 'accepted'),
  1, 'exactly one status history row is written for the accept'
);
-- notifications RLS is strictly user_id = auth.uid() with no
-- doctor/patient cross-visibility (unlike request_status_history's
-- broader participant policy) -- reset to the unrestricted owner to
-- verify a notification recipient other than whoever just acted.
reset role;
select is(
  (select count(*)::int from public.notifications
     where user_id = current_setting('pgtap.patient_a')::uuid and type = 'request_accepted'
       and related_request_id = current_setting('pgtap.req_a1')::uuid),
  1, 'exactly one request_accepted notification is sent to the patient'
);
set local role authenticated;

-- =============================================================================
-- requested -> rejected
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);
select is(pg_temp.try_transition('pgtap.req_reject1', 'rejected'), 'AS017_NOT_AUTHORIZED_FOR_TRANSITION',
  'the patient cannot reject their own request');

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_b'), true);
select is(pg_temp.try_transition('pgtap.req_reject1', 'rejected'), 'AS017_NOT_AUTHORIZED_FOR_TRANSITION',
  'doctor B cannot reject doctor A''s request');

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);
select is(pg_temp.try_transition('pgtap.req_reject1', 'rejected', 'Fully booked that day.'), 'OK',
  'happy path: the assigned doctor can reject a requested request, with a reason');

select is(
  (select rejection_reason from public.requests where id = current_setting('pgtap.req_reject1')::uuid),
  'Fully booked that day.', 'the rejection reason is stored in rejection_reason (not doctor_response_note)'
);
reset role;
select ok(
  (select count(*)::int from public.notifications
     where user_id = current_setting('pgtap.patient_a')::uuid and type = 'request_rejected'
       and related_request_id = current_setting('pgtap.req_reject1')::uuid
       and body like '%Fully booked that day.%') = 1,
  'the rejection notification includes the reason the doctor gave'
);
set local role authenticated;
select is(
  (select count(*)::int from public.request_status_history
     where request_id = current_setting('pgtap.req_reject1')::uuid and new_status = 'rejected'),
  1, 'exactly one status history row is written for the rejection'
);

-- =============================================================================
-- accepted -> confirmed
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);
select is(pg_temp.try_transition('pgtap.req_confirm1', 'confirmed'), 'AS017_NOT_AUTHORIZED_FOR_TRANSITION',
  'the patient cannot confirm a request');

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_b'), true);
select is(pg_temp.try_transition('pgtap.req_confirm1', 'confirmed'), 'AS017_NOT_AUTHORIZED_FOR_TRANSITION',
  'an unrelated doctor cannot confirm doctor A''s accepted request');

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);
select is(pg_temp.try_transition('pgtap.req_confirm1', 'confirmed'), 'OK',
  'happy path: the assigned doctor can confirm an accepted request');
reset role;
select is(
  (select count(*)::int from public.notifications
     where user_id = current_setting('pgtap.patient_a')::uuid and type = 'request_confirmed'
       and related_request_id = current_setting('pgtap.req_confirm1')::uuid),
  1, 'exactly one request_confirmed notification is sent'
);
set local role authenticated;

select set_config('request.jwt.claim.sub', current_setting('pgtap.admin_a'), true);
select is(pg_temp.try_transition('pgtap.req_confirm_admin', 'confirmed'), 'OK',
  'happy path: an admin can confirm an accepted request');

-- =============================================================================
-- confirmed -> completed
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);
select is(pg_temp.try_transition('pgtap.req_complete_past', 'completed'), 'AS017_NOT_AUTHORIZED_FOR_TRANSITION',
  'the patient cannot complete a request');

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);
select is(pg_temp.try_transition('pgtap.req_complete_future', 'completed'), 'AS020_TOO_EARLY_TO_COMPLETE',
  'a confirmed request cannot be completed before its scheduled start time');

select is(pg_temp.try_transition('pgtap.req_complete_past', 'completed'), 'OK',
  'happy path: the assigned doctor can complete a confirmed request once its start time has passed');
select ok(
  (select completed_at from public.requests where id = current_setting('pgtap.req_complete_past')::uuid) is not null,
  'completing sets completed_at'
);
reset role;
select is(
  (select count(*)::int from public.notifications
     where user_id = current_setting('pgtap.patient_a')::uuid and type = 'request_completed'
       and related_request_id = current_setting('pgtap.req_complete_past')::uuid),
  1, 'exactly one request_completed notification is sent'
);
set local role authenticated;

select set_config('request.jwt.claim.sub', current_setting('pgtap.admin_a'), true);
select is(pg_temp.try_transition('pgtap.req_complete_admin_past', 'completed'), 'OK',
  'happy path: an admin can complete a confirmed request once its start time has passed');

-- =============================================================================
-- -> cancelled
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_b'), true);
select is(pg_temp.try_transition('pgtap.req_cancel_unrelated', 'cancelled'), 'AS017_NOT_AUTHORIZED_FOR_TRANSITION',
  'an unrelated patient cannot cancel someone else''s request');

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);
select is(pg_temp.try_transition('pgtap.req_cancel_by_doctor_forbidden', 'cancelled'), 'AS017_NOT_AUTHORIZED_FOR_TRANSITION',
  'the assigned doctor cannot cancel a still-requested request (must decline instead)');

select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);
select is(pg_temp.try_transition('pgtap.req_cancel_by_patient', 'cancelled', 'Changed my mind.'), 'OK',
  'happy path: the patient can cancel their own requested request');
select is(
  (select cancellation_reason from public.requests where id = current_setting('pgtap.req_cancel_by_patient')::uuid),
  'Changed my mind.', 'the cancellation reason is stored in cancellation_reason'
);
reset role;
select is(
  (select count(*)::int from public.notifications
     where user_id = current_setting('pgtap.doctor_a')::uuid and type = 'request_cancelled'
       and related_request_id = current_setting('pgtap.req_cancel_by_patient')::uuid),
  1, 'a patient-initiated cancellation notifies the doctor exactly once'
);
set local role authenticated;
select is(
  (select count(*)::int from public.request_status_history
     where request_id = current_setting('pgtap.req_cancel_by_patient')::uuid and new_status = 'cancelled'),
  1, 'exactly one status history row is written for the cancellation'
);

select set_config('request.jwt.claim.sub', current_setting('pgtap.admin_a'), true);
select is(pg_temp.try_transition('pgtap.req_cancel_by_admin_requested', 'cancelled'), 'OK',
  'happy path: an admin can cancel a requested request');

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);
select is(pg_temp.try_transition('pgtap.req_cancel_accepted_by_doctor', 'cancelled'), 'OK',
  'happy path: the assigned doctor can cancel an accepted request');
reset role;
select is(
  (select count(*)::int from public.notifications
     where user_id = current_setting('pgtap.patient_a')::uuid and type = 'request_cancelled'
       and related_request_id = current_setting('pgtap.req_cancel_accepted_by_doctor')::uuid),
  1, 'a doctor-initiated cancellation notifies the patient exactly once'
);
set local role authenticated;

select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);
select is(pg_temp.try_transition('pgtap.req_cancel_accepted_by_patient', 'cancelled'), 'OK',
  'happy path: the patient can cancel an accepted request');

select set_config('request.jwt.claim.sub', current_setting('pgtap.admin_a'), true);
select is(pg_temp.try_transition('pgtap.req_cancel_confirmed_by_admin', 'cancelled'), 'OK',
  'happy path: an admin can cancel a confirmed request');

-- =============================================================================
-- Terminal states and invalid transitions
-- =============================================================================
select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);
select is(pg_temp.try_transition('pgtap.req_terminal_completed', 'cancelled'), 'AS016_INVALID_TRANSITION',
  'cancelling a completed request is rejected');

select is(pg_temp.try_transition('pgtap.req_terminal_rejected', 'cancelled'), 'AS016_INVALID_TRANSITION',
  'any transition out of a rejected request is rejected');

select set_config('request.jwt.claim.sub', current_setting('pgtap.doctor_a'), true);
select is(pg_temp.try_transition('pgtap.req_terminal_cancelled', 'accepted'), 'AS016_INVALID_TRANSITION',
  'any transition out of a cancelled request is rejected');

select is(pg_temp.try_transition('pgtap.req_noop', 'requested'), 'AS016_INVALID_TRANSITION',
  'transitioning a request to the status it already has is rejected');

reset role;

-- =============================================================================
-- Notification RPCs
-- =============================================================================
with n as (
  insert into public.notifications (user_id, type, title, body)
  values (current_setting('pgtap.patient_b')::uuid, 'system', 'Test', 'Test notification for patient B')
  returning id
)
select set_config('pgtap.notif_b', id::text, true) from n;

set local role authenticated;

select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);
select is(
  public.mark_notification_read(current_setting('pgtap.notif_b')::uuid),
  0,
  'mark_notification_read returns 0 when the notification belongs to someone else'
);
-- Raw SELECT as patient A would be RLS-filtered to zero rows regardless of
-- the real value (vacuously "passing" either way) -- reset role for an
-- authoritative read.
reset role;
select ok(
  (select read_at from public.notifications where id = current_setting('pgtap.notif_b')::uuid) is null,
  'the other user''s notification remains unread after the cross-user attempt'
);
set local role authenticated;

select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_b'), true);
select is(
  public.mark_notification_read(current_setting('pgtap.notif_b')::uuid),
  1,
  'mark_notification_read returns 1 when the caller owns the notification'
);

select is(
  public.get_unread_notification_count(),
  (select count(*)::int from public.notifications where user_id = current_setting('pgtap.patient_b')::uuid and read_at is null),
  'get_unread_notification_count matches the caller''s actual unread count'
);

select set_config('request.jwt.claim.sub', current_setting('pgtap.patient_a'), true);
select set_config('pgtap.patient_a_unread_before', public.get_unread_notification_count()::text, true);

select is(
  public.mark_all_notifications_read(),
  current_setting('pgtap.patient_a_unread_before')::int,
  'mark_all_notifications_read returns exactly the caller''s prior unread count'
);
select is(
  public.get_unread_notification_count(),
  0,
  'patient A has zero unread notifications after mark_all_notifications_read'
);

reset role;

select * from finish();

rollback;
