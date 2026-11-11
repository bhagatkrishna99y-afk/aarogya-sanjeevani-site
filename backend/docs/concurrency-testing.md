# Testing concurrent booking of the same slot

`create_service_request()` is protected against two patients booking the
same doctor slot at the same instant by `requests_no_doctor_overlap`, a
GiST `EXCLUDE` constraint scoped to `accepted`/`confirmed` requests
(`20261112000006_transactional_tables_strict_integrity.sql`), and by
`requests_patient_doctor_start_unique`, a partial unique index that blocks
the same patient from double-booking the same doctor/time while any of
their own request is `requested`/`accepted`/`confirmed`. The function
catches both as `exclusion_violation`/`unique_violation` and re-raises them
as the stable `AS001_SLOT_UNAVAILABLE` / `AS010_DUPLICATE_REQUEST` codes
(see `docs/error-codes.md`).

A single pgTAP test file runs everything inside one session and one
transaction, so it cannot produce two statements racing to commit at the
same instant -- the scenario below needs two real, separate database
sessions. This is a manual (or CI-scripted-with-two-connections) procedure,
not something `supabase test db` runs today.

## Procedure

You need a doctor with at least one open slot and two different patient
accounts. The quickest way to get there locally is to run the fixture setup
from `supabase/tests/database/008_scheduling_business_logic_test.sql` up
through the "capture two open slots" step, **without** the surrounding
`begin;`/`rollback;` (so the fixture rows actually persist), note
`pgtap.doctor_a`'s `user_id`, `pgtap.service_id`, and `pgtap.slot1_start`,
then clean the fixture rows up by hand afterward.

1. Open two separate `psql` connections (or two Supabase Studio SQL editor
   tabs -- each tab is its own session) to the same database.
2. In **both** sessions, set the calling patient's identity and start a
   transaction, but do not commit yet:
   ```sql
   begin;
   set local role authenticated;
   select set_config('request.jwt.claim.sub', '<patient-a-or-b-uuid>', true);
   ```
   Use a different patient UUID in each session.
3. In **both** sessions, prepare (but do not yet execute) the same call:
   ```sql
   select public.create_service_request(
     '<service_id>', '<doctor_id>', '<slot1_start>'::timestamptz,
     'in_person', null, '+91 90000 00000', null
   );
   ```
4. Execute the `select` in session 1, then -- before committing session 1
   -- execute the identical `select` in session 2. Postgres will block
   session 2's `INSERT` on the GiST index while session 1's transaction is
   still open (since both target the same doctor/time range and the same
   partial index condition applies once either row reaches `accepted`;
   for two simultaneous `requested` rows for *different* patients the
   EXCLUDE constraint does **not** block them -- see the deliberate
   relaxation in `20261112000006`'s header comment -- so this step
   specifically demonstrates the duplicate/overlap path once one side is
   accepted, not a blanket "only one request can ever exist" rule).
5. Commit session 1 (`commit;`). Session 2 should then either complete
   successfully (if the two `requested` rows don't collide) or receive
   `AS001_SLOT_UNAVAILABLE` / `AS010_DUPLICATE_REQUEST` once it resumes,
   depending on which constraint the two concrete rows collide on.
6. To specifically exercise the `accepted`-vs-`accepted` collision (the
   case `requests_no_doctor_overlap` exists for): instead of step 3, have
   session 1 call `create_service_request` and commit it, then have an
   admin session call `doctor_respond_request(request_id, true, null)` to
   move it to `accepted` and commit that too. Then, in two fresh
   concurrent sessions, attempt to directly `UPDATE public.requests SET
   status = 'accepted' WHERE id = ...` two different `requested` rows for
   the same doctor/time range at the same instant (via
   `doctor_respond_request`, which doctors call to accept) -- the second
   session's commit will fail with SQLSTATE `23P01` (`exclusion_violation`),
   which `doctor_respond_request` should itself translate into a friendly
   error (out of scope for this migration; tracked separately).

## What "pass" looks like

- Exactly one of the two concurrent attempts succeeds and returns a request
  id; the other fails with a recognizable `AS0xx` code (not a raw
  `23P01`/`23505` SQLSTATE leaking to the client -- if it does, the
  `exception when exclusion_violation`/`when unique_violation` handlers in
  `create_service_request` need revisiting).
- No two `accepted`/`confirmed` requests for the same doctor ever end up
  with overlapping `tstzrange(scheduled_start, scheduled_end)` -- check with:
  ```sql
  select doctor_id, count(*)
  from public.requests
  where status in ('accepted', 'confirmed')
  group by doctor_id, tstzrange(scheduled_start, scheduled_end)
  having count(*) > 1;
  ```
  This should always return zero rows, concurrent test or not.
