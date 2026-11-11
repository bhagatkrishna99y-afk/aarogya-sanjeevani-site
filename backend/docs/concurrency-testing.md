# Testing concurrent booking of the same slot

`transition_request_status()`'s `requested -> accepted` path is protected
against two overlapping requests for the same doctor both ending up
`accepted`/`confirmed` by `requests_no_doctor_overlap`, a GiST `EXCLUDE`
constraint scoped to `status IN ('accepted', 'confirmed')`
(`20261112000006_transactional_tables_strict_integrity.sql`). The function
catches the resulting `exclusion_violation` and re-raises it as the stable
`AS019_SLOT_ALREADY_BOOKED` (see `docs/error-codes.md`) -- the request
stays `requested` rather than erroring into an inconsistent state.

A pgTAP test runs everything inside one session and one transaction, so it
cannot make two statements race to commit at the same instant -- proving
this needs two real, separate database connections racing concurrently.
This doc has two parts: the automated script (the real deliverable, run it
against a database you control) and what this session itself could verify
without one (a sequential run of the exact same code path, which proves the
mechanism works but not that it survives genuine concurrent load).

## Automated concurrency simulation (run this)

`scripts/run_concurrency_test.sh`, backed by
`supabase/tests/concurrency/*.sql` and two `pgbench` scripts, simulates:

1. **20 parallel sessions** (20 different patients) each create a
   `requested` row for the **same doctor and the same slot**
   (`01_create_requests.pgbench.sql`, run with `pgbench -c 20 -j 20 -t 1`).
   All 20 are expected to succeed -- two different patients both holding a
   `requested` row for the same doctor/time is deliberately *not* blocked
   (see `docs/data-model.md`'s "Key constraints" section); this phase just
   produces the 20 competing requests phase 2 races to accept.
2. **`$ACCEPT_CLIENTS` parallel sessions, all as the doctor**, each try to
   accept a *different* one of those 20 requests at the same instant
   (`02_accept_race.pgbench.sql`). Default `$ACCEPT_CLIENTS=2`, reproducing
   "the doctor accepts two of them" literally; raise it (up to 20) to
   exercise the same constraint under heavier concurrent load.
3. A verification query confirms **exactly one** request for that slot
   ended up `accepted`, and that no doctor anywhere in the table has two
   overlapping `accepted`/`confirmed` requests.

### Running it

Requires `psql` and `pgbench` (the standard PostgreSQL client tools) and a
`DATABASE_URL` pointing at a real Postgres connection -- **use a local
`supabase start` stack, not the shared hosted project**: this opens up to
20 concurrent connections and creates/deletes real rows (cleaned up by the
script itself at the end), neither of which belongs on shared
infrastructure just to run a test.

```bash
DATABASE_URL="postgresql://postgres:postgres@localhost:54322/postgres" \
  ./scripts/run_concurrency_test.sh
# or, to race all 20 instead of just 2:
ACCEPT_CLIENTS=20 DATABASE_URL="postgresql://postgres:postgres@localhost:54322/postgres" \
  ./scripts/run_concurrency_test.sh
```

Expect `pgbench`'s phase-2 output to report failed transactions -- that
**is** the test passing: every client but one is supposed to have its
`transition_request_status` call raise an exception (`AS019`, or
occasionally `AS016_INVALID_TRANSITION` if two clients happened to pick the
very same row -- either way, no double-acceptance). The script's own
verification query, not pgbench's transaction count, is the actual
pass/fail signal; it prints `PASS`/`FAIL` and exits `0`/`1` accordingly.

### A note on where this script was and wasn't run

This script was written and reviewed in this session but **not executed
here** -- the sandboxed tool environment this backend was built in has
neither a `psql`/`pgbench` binary on `PATH` nor a `DATABASE_URL`/Postgres
connection string available as an environment variable, so a real 20-way
concurrent connection race cannot be driven from inside it. It needs to be
run by a human (or CI) with real `psql`/`pgbench` and a database
connection, per the instructions above. What *could* be verified from
inside this session is the next section.

## What this session verified instead: the mechanism itself, sequentially

Using the project's live database connection (not a raw `psql` connection,
but the same effect), two different patients each created a `requested`
row for the same approved doctor at the identical slot, then the doctor's
`transition_request_status` was called to accept both, one right after the
other (not concurrently -- there is no way to open two simultaneous
sessions through this session's available tools):

```
first (p1) accept attempt                                   -> OK
second (p2) accept attempt -- same doctor, same time range  -> AS019_SLOT_ALREADY_BOOKED
```

The first request became `accepted`; the second raised
`AS019_SLOT_ALREADY_BOOKED` and stayed `requested`. This confirms the exact
code path (`requests_no_doctor_overlap`'s `exclusion_violation`, caught and
re-raised by `transition_request_status`) behaves correctly for the
condition it exists to catch. It is **not** equivalent to the pgbench
script above: a sequential proof only shows the second call executes
*after* the first has already committed and released its lock, which is
the easy case for a database to get right. The genuinely hard case --
many transactions racing to commit at the same physical instant, with the
lock manager (not application code) deciding a winner -- is exactly what
`requests_no_doctor_overlap` being a real GiST `EXCLUDE` constraint
(enforced by Postgres itself at commit time, not a `SELECT ... check then
INSERT` race in `plpgsql`) is *for*; this sequential run doesn't exercise
that, which is why the script above is the actual deliverable and this is
just corroborating evidence of the same mechanism.

## Manual two-session walkthrough (fallback, if you don't want to install pgbench)

You need a doctor with at least one open slot and two patient accounts --
`supabase/tests/concurrency/fixture_setup.sql` creates exactly that (run it
once, then `fixture_teardown.sql` when done; both can be run with plain
`psql`, no `pgbench` required for this manual version).

1. Open two separate `psql` connections (or two Supabase Studio SQL editor
   tabs -- each tab is its own session) to the same database.
2. In **both** sessions, impersonate the doctor and start a transaction,
   but do not commit yet:
   ```sql
   begin;
   set local role authenticated;
   select set_config('request.jwt.claim.sub', '<doctor-uuid-from-_concurrency_fixture>', true);
   ```
3. In session 1, pick one of the fixture's `requested` request ids; in
   session 2, pick a *different* one (same doctor, same slot -- any two
   rows from `select id from requests where doctor_id = ... and status =
   'requested'` work). Prepare, in both sessions:
   ```sql
   select public.transition_request_status('<that session''s request id>'::uuid, 'accepted', null);
   ```
4. Execute the `select` in session 1, then -- before committing session 1
   -- execute session 2's `select`. Session 2 will block, waiting on the
   GiST index while session 1's transaction is still open.
5. Commit session 1 (`commit;`). Session 2 should then resume and raise
   `AS019_SLOT_ALREADY_BOOKED` (or occasionally `AS016_INVALID_TRANSITION`,
   if it happened to target the exact same row session 1 already moved);
   its own request stays `requested`. Roll it back (`rollback;`) rather
   than committing an error.

## What "pass" looks like

- Across every concurrent attempt (pgbench phase 2, or the two manual
  sessions above), exactly one succeeds and returns normally; every other
  one fails with a recognizable `AS0xx` code -- never a raw
  `23P01`/`23505` SQLSTATE leaking out (if it does, the `exception when
  exclusion_violation` handler in `transition_request_status` needs
  revisiting).
- No two `accepted`/`confirmed` requests for the same doctor ever end up
  with overlapping `tstzrange(scheduled_start, scheduled_end)`, checked
  with:
  ```sql
  select doctor_id, count(*)
  from public.requests
  where status in ('accepted', 'confirmed')
  group by doctor_id, tstzrange(scheduled_start, scheduled_end)
  having count(*) > 1;
  ```
  This should always return zero rows, concurrent test or not --
  `run_concurrency_test.sh` checks exactly this as part of its PASS/FAIL
  verdict.
