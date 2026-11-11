-- Phase 2: N parallel sessions, all as the SAME doctor, each try to
-- accept a DIFFERENT one of the `requested` rows phase 1 created (client
-- :client_id picks the (client_id+1)'th such row, ordered by creation).
-- Run with -c N -j N -t 1, where N <= 20 (the wrapper script's
-- $ACCEPT_CLIENTS controls N; 2 reproduces the prompt's literal "the
-- doctor accepts two of them", a higher N exercises the same EXCLUDE
-- constraint under heavier concurrent load).
--
-- Expected result: exactly one client's transition_request_status call
-- commits (its request becomes `accepted`); every other client's call
-- raises AS019_SLOT_ALREADY_BOOKED (caught from the requests_no_doctor_overlap
-- EXCLUDE constraint's exclusion_violation inside transition_request_status
-- itself -- see docs/error-codes.md) and that client's own request stays
-- `requested`. pgbench will report these as failed transactions; that is
-- the test passing, not an infrastructure problem -- see
-- docs/concurrency-testing.md for how run_concurrency_test.sh tells the
-- two apart.
begin;
set local role authenticated;
select set_config(
  'request.jwt.claim.sub',
  (select doctor_id::text from public._concurrency_fixture),
  true
);
select public.transition_request_status(
  (
    select r.id from public.requests r, public._concurrency_fixture f
    where r.doctor_id = f.doctor_id and r.scheduled_start = f.slot_start and r.status = 'requested'
    order by r.created_at
    offset :client_id limit 1
  ),
  'accepted',
  null
);
commit;
