-- Missing index found while reviewing the list-view queries for
-- docs/performance.md: requests_doctor_status_start_idx (doctor_id,
-- status, scheduled_start) already supports the doctor-side "my
-- requests, filtered by status, ordered by appointment time" pattern
-- (doctor_requests_view, admin_list_requests filtered by doctor), but
-- there was no patient-side equivalent -- requests_patient_idx is
-- (patient_id, created_at DESC), which doesn't help a query filtering by
-- status and ordering by scheduled_start (patient_requests_view,
-- admin_list_requests filtered by patient/status). Added as its own
-- migration since it postdates 20261112000012 (already applied) -- never
-- editing an already-applied migration file, per
-- docs/conventions.md.
--
-- Verified via EXPLAIN against the function's internal query (see
-- docs/performance.md): the planner picks this index up directly for a
-- plain status-filtered scan. It does not get picked for
-- patient_requests_view's own OR'd (patient_id = X OR doctor_id = X OR
-- is_admin()) filter at the current (tiny, 2-distinct-patient) data
-- volume -- expected and self-correcting once patient cardinality is
-- realistic; also documented in docs/performance.md.

begin;

create index if not exists requests_patient_status_start_idx
  on public.requests (patient_id, status, scheduled_start);

commit;
