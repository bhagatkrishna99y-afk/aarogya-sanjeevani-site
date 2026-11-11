-- Phase 1: 20 parallel sessions each create a service request for the
-- SAME doctor and the SAME slot, as a DIFFERENT patient (client :client_id,
-- 0-indexed, is pgbench's own built-in per-client variable -- run with
-- -c 20 -j 20 -t 1 so each of the 20 clients executes this file exactly
-- once, concurrently).
--
-- Expected result: all 20 succeed. Two different patients holding a
-- `requested` row for the same doctor/time is explicitly NOT blocked by
-- requests_no_doctor_overlap (that EXCLUDE is scoped to accepted/confirmed
-- only -- see docs/data-model.md's "Key constraints" section) or by
-- requests_patient_doctor_start_unique (scoped per-patient). This phase
-- exists to produce the 20 competing `requested` rows that phase 2 races
-- to accept.
begin;
set local role authenticated;
select set_config(
  'request.jwt.claim.sub',
  (select (patient_ids)[:client_id + 1]::text from public._concurrency_fixture),
  true
);
select public.create_service_request(
  (select service_id from public._concurrency_fixture),
  (select doctor_id from public._concurrency_fixture),
  (select slot_start from public._concurrency_fixture),
  'in_person'::service_mode,
  null,
  '+91 90000 ' || lpad((:client_id + 1)::text, 5, '0'),
  null
);
commit;
