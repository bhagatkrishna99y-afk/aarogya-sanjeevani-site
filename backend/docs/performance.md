# Performance review

## Rate limiting

`create_service_request` (upgraded in place in
`20261112000013_rate_limiting.sql`) enforces two limits, checked
immediately after the role/suspension check and before any service/
doctor lookups, so a rate-limited caller gets a cheap rejection:

- **At most 10 active requests at once** per patient (`status IN
  ('requested', 'accepted', 'confirmed')`) -- `AS034_TOO_MANY_ACTIVE_REQUESTS`.
- **At most 5 requests created in the trailing 60 minutes** per patient
  (`created_at > now() - interval '1 hour'`) -- `AS035_RATE_LIMITED`.

**Design notes, for anyone adding the next rate limit to this backend:**

- Both limits are plain `SELECT count(*)` queries against
  `requests.patient_id`, which is already indexed
  (`requests_patient_idx` on `(patient_id, created_at DESC)` covers the
  rolling-window check directly; the active-count check filters by
  `status` on top of an index-scoped `patient_id` lookup, which is fine
  at the row counts one patient will ever have). No dedicated token-
  bucket/sliding-window table was introduced -- one would add write
  amplification (an extra row per request) and a cleanup job for no real
  benefit at this scale, when the thing being limited is already a row
  in an existing, already-indexed table.
- This pattern only works because the thing being limited
  (`create_service_request`) always has a stable, authenticated
  `patient_id` to key on -- every mutating RPC in this backend requires
  `auth.uid()`. It does **not** generalize to limiting unauthenticated or
  pre-auth traffic (login attempts, anon endpoints), which this backend
  has very little of (`get_doctor_open_slots`, `create_service_request`,
  and every other RPC besides the four intentionally-anon-callable
  browsing functions all require sign-in already). If that need arises,
  it's a different problem needing an edge-level solution (Supabase's own
  API rate limiting, or a Redis-backed counter in front of the RPC in an
  Edge Function) -- not another counting query in a plpgsql function with
  no stable key to count by.
- Counting queries like this read the current row count under whatever
  transaction isolation the connection is using (default `READ
  COMMITTED`), so two requests racing in the same instant could both pass
  the count check before either commits -- a patient could end up with 11
  active requests instead of 10 in that narrow window. This is accepted
  here: the limits are abuse-deterrence (stop a script from spamming
  hundreds of requests), not a hard invariant the way `requests_no_doctor_overlap`
  is (a real double-booking, enforced by a DB-level EXCLUDE constraint,
  not a count check) -- an off-by-one on a soft limit is a non-issue;
  silently double-booking a doctor is not, which is why that one gets a
  real constraint instead.

## Advisor sweep

Ran `get_advisors` (security and performance) before and after this
review. Baseline, before any changes in this pass:

- **Security**: no missing-RLS, no mutable-search_path, and no
  unindexed-foreign-key findings -- all three were already clean from
  earlier work (verified directly too, below). The remaining findings are
  all already-accepted, documented exceptions: the 3 `security_definer_view`
  entries (`doctors_public`, `doctor_services_public`,
  `doctor_patient_contacts` -- see `docs/security-principles.md`), every
  RPC showing up as "callable by authenticated" (that's the entire point
  of an RPC), the 4 intentionally-anon-callable public-browsing functions,
  and `auth_leaked_password_protection` (a project-level Auth setting, not
  a database change -- **no tool available to this session can toggle
  it**; enable it from the dashboard under Authentication → Policies →
  Password settings).
- **Performance**: `multiple_permissive_policies` (3 tables) and 7
  `unused_index` INFO findings.

### Fixed: multiple_permissive_policies

`profiles`, `doctor_profiles`, and `doctor_weekly_availability` each had
two separate permissive `SELECT` policies applying to the same role (an
"own row" policy and a separate `is_admin()` policy, evaluated and OR'd on
every query). `20261112000012_policy_consolidation.sql` merges each pair
into one policy with an OR'd condition -- same accessible rows for every
role, fewer policy evaluations per query. Re-running the advisor after
confirms the warning is gone.

### Not fixed (deliberately): unused_index

`service_categories_is_active_idx`, `service_categories_sort_order_idx`,
`services_is_active_idx`, `services_sort_order_idx`, `requests_status_idx`,
`requests_service_idx`, `doctor_services_service_idx` all show as
"never used." This is a fresh project with almost no real query traffic
yet, not evidence these indexes are unnecessary -- each supports a WHERE/
ORDER BY clause the catalog-browsing and admin-listing queries actually
use (confirmed in the EXPLAIN runs below, several of these exact indexes
show up as chosen access paths once there's enough data for the planner
to prefer them over a sequential scan). Removing them now, before the
project has any real usage history, would be premature; revisit if they
still show zero scans after real traffic.

## Manual checks beyond the advisor

The prompt specifically calls out missing RLS, mutable search_path, and
unindexed foreign keys. Checked each directly, not just via the advisor:

- **RLS**: every table in `public` has `rls_enabled = true` (`list_tables`).
- **Mutable search_path**: `select proname from pg_proc ... where
  proconfig is null or search_path not set` returns zero rows -- every
  function in `public` pins `search_path`.
- **Unindexed foreign keys**: every single-column FK in `public` has an
  index whose leading column matches the FK column (checked via
  `pg_constraint` joined to `pg_index.indkey[0]`). None were missing.

## Missing index found and added

Reviewing the list-view queries (`patient_requests_view`,
`doctor_requests_view`, `admin_list_requests`) against the existing
indexes: `requests_doctor_status_start_idx (doctor_id, status,
scheduled_start)` already supports the doctor-side "my requests, filtered
by status, ordered by appointment time" pattern, but there was no
patient-side equivalent -- `requests_patient_idx` is `(patient_id,
created_at DESC)`, which doesn't help a query filtering by status and
ordering by `scheduled_start`. Added
`requests_patient_status_start_idx (patient_id, status, scheduled_start)`
in `20261112000015_missing_indexes.sql`.

## EXPLAIN runs

All run with `EXPLAIN (ANALYZE, BUFFERS)` against the seeded project. The
`patient_requests_view`/`doctor_requests_view` runs were executed as the
actual patient/doctor (`set local role authenticated` +
`request.jwt.claim.sub`), so they reflect the real RLS-filtered plan, not
an unrestricted superuser's.

### `get_doctor_open_slots` (the internal query, not the function-call wrapper)

A plain `EXPLAIN` on `SELECT * FROM get_doctor_open_slots(...)` only shows
a single opaque "Function Scan" node -- Postgres doesn't look inside a
plpgsql function body for planning. To see the real plan, the function's
core CTE was extracted and run directly:

```
Sort (actual time=0.306..0.308 rows=24)
  -> Nested Loop Anti Join (actual time=0.242..0.278 rows=24)
       -> Nested Loop (actual time=0.216..0.232 rows=24)
            -> Index Scan using doctor_weekly_availability_doctor_idx
                 Index Cond: (doctor_id = $1 AND day_of_week = $2)
            -> Function Scan on generate_series
       -> Materialize
            -> Seq Scan on requests r
                 Filter: (status = ANY ('{accepted,confirmed}') AND doctor_id = $1)
Execution Time: 0.515 ms
```

`doctor_weekly_availability_doctor_idx` is used exactly as intended for
the `(doctor_id, day_of_week)` lookup. The `requests` scan is a sequential
scan -- correct at this table's current size (a handful of rows); once
`requests` has real volume, `requests_doctor_status_start_idx` is already
in place to support this same `doctor_id + status IN (...)` filter and
the planner will switch to it automatically (cost-based, no code change
needed).

### `patient_requests_view`

Run as patient 1, with ~2000 synthetic requests temporarily loaded to get
a plan reflective of real data volume (removed afterward -- see note
below). The planner chose to drive from `services` (15 rows) and probe
`requests` via `requests_service_idx`, rather than using the new
`requests_patient_status_start_idx` directly:

```
Sort (actual time=13.320..13.323 rows=20)
  -> Hash Left Join (actual time=0.117..14.418 rows=1002)
       -> Nested Loop (actual time=0.050..13.954 rows=1002)
            -> Seq Scan on services s (rows=15)
            -> Index Scan using requests_service_idx on requests r (loops=15)
                 Index Cond: (service_id = s.id)
                 Filter: (patient_id = $1 OR doctor_id = $2 OR is_admin())
       -> Hash
            -> Nested Loop (doctor_profiles/profiles, approved+not suspended)
Execution Time: 15.330 ms
```

**This is the test data's own artifact, not a real problem**: the
synthetic set had only 2 distinct patients across ~2000 rows, so
`patient_id = $1` matches roughly half the table -- correctly judged by
the planner as not selective enough to be worth an index scan. In
production, with many more distinct patients and comparatively few
requests each, `patient_id` becomes highly selective and Postgres's
cost-based planner will switch to `requests_patient_status_start_idx`
on its own; this needs no code change, just real data. Re-check this plan
once there's real patient volume rather than assuming the index isn't
earning its keep.

### `doctor_requests_view`

Run as the doctor with the synthetic data's ~2000 rows (all assigned to
one doctor, so `doctor_id` *is* highly selective here):

```
Nested Loop Left Join (actual time=3.347..5.663 rows=2001)
  -> Hash Right Join
       -> Index Scan using requests_doctor_idx on requests r_1
            Index Cond: (doctor_id = $1)
       -> Hash
            -> ... (services/profiles joins)
  -> Materialize (doctor_patient_contacts)
Execution Time: 6.852 ms
```

`requests_doctor_idx` is used directly and cheaply here -- the doctor-side
access pattern (one doctor, many requests) is exactly the shape these
indexes were built for, and it shows.

### `admin_list_requests` (internal query)

Same function-is-opaque issue as `get_doctor_open_slots`; the function's
own `SELECT` was run directly, filtered by `status = 'requested'`:

```
Limit (actual time=0.701..0.706 rows=20)
  -> Sort (actual time=0.700..0.702)
       -> Hash Join (requests x services)
            -> Hash Left Join (x profiles, doctor)
                 -> Nested Loop (x profiles, patient)
                      -> Seq Scan on profiles pp (rows=8)
                      -> Index Scan using requests_patient_status_start_idx on requests r
                           Index Cond: (patient_id = pp.id AND status = 'requested')
Execution Time: 0.862 ms
```

This confirms the new `requests_patient_status_start_idx` is picked up
cleanly for a straightforward status-filtered admin query (no OR'd
RLS-style filter complicating the picture the way the patient-facing view
above has) -- direct validation that the new index does what it was added
for.

### Note on the synthetic load used for these EXPLAINs

The project's real data (from `supabase/seed/seed.sql`) is only 3
requests -- too small for any EXPLAIN to show realistic index usage
(everything is a trivially-cheap sequential scan at that size). ~2000
synthetic request rows were temporarily inserted, `ANALYZE`d, used for
the runs above, then removed. Removing them required temporarily
disabling `request_status_history`'s append-only trigger (`ALTER TABLE
... DISABLE TRIGGER`, delete, `ENABLE TRIGGER`) -- a reminder that in
this schema, once a request has a status-history row (every request gets
one immediately, via `requests_log_created`), it can never be deleted
normally; that is by design (the append-only history is a deliberate
audit guarantee, see `docs/conventions.md`), not a bug. The project's
`requests`/`request_status_history`/`notifications` row counts were
verified back to their pre-synthetic-load values (3 / 3 / 2) afterward.
