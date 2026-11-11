# Security principles

These are the non-negotiable rules this backend follows. They are already
implemented in the existing migrations — this document explains *why*, so
the same decisions get made consistently as the schema grows, and names the
actual files that demonstrate each rule in practice.

## 1. RLS everywhere

Every table in `public` that holds user data has `alter table ... enable row
level security;` with explicit policies — never a table left world-readable
"temporarily," never a policy that defaults to permissive. See
`20261111120007_rls.sql` for the baseline, and
`20261111120010_performance_tuning.sql` /
`20261111120014_fix_doctor_own_admin_notes_leak.sql` for examples of
tightening policies after a gap was found.

A table with RLS enabled and *zero* policies denies all access by default —
that is the safe failure mode. If a table ever needs to be fully public-read
(e.g. the service catalog), say so with an explicit `using (true)` or
`using (is_active)` policy, not by leaving RLS off.

## 2. SECURITY DEFINER functions, always with `set search_path = ''`

Every RPC that needs to bypass RLS (because it performs a privileged
check-then-write, like approving a doctor or creating a booking) is
`security definer` **and** declares `set search_path = ''`, forcing every
reference inside the function body to be schema-qualified (`public.profiles`,
not `profiles`). Without the empty `search_path`, a caller who can create
objects earlier in their own search path (rare on Supabase, but not
impossible with certain roles/extensions) could shadow a table or function
the definer-context code relies on and hijack it. See
`20261111120008_rpc_functions.sql` — every function there follows this
pattern; a new RPC that doesn't is a bug.

## 3. Default privileges: revoke first, then grant only what's needed

`anon` and `authenticated` do not get broad default access to new tables or
functions. `20261111120009_fix_function_grants.sql` documents a real
incident from this project: Supabase's own schema-level default privileges
silently granted `EXECUTE` on every new function to `anon`/`authenticated`,
so a function created with `grant execute ... to authenticated` **also**
ended up executable by `anon` — found only by inspecting `pg_proc.proacl`
directly, not by reading the grant statements. The fix:

```sql
alter default privileges in schema public revoke execute on functions from anon, authenticated;
```

...so every future function must be granted explicitly, and a missing grant
fails loudly (permission denied) instead of silently succeeding for a role
that should never have had access.

That fix turned out to be incomplete in a way that bit again almost
immediately: it only revoked the default for `anon, authenticated`, not for
the separate `PUBLIC` pseudo-role that Postgres grants `EXECUTE` to on
*every* `CREATE FUNCTION` unless told otherwise. `prevent_role_escalation()`
(added in `20261112000002_core_enums_and_profiles.sql`, after the fix above)
silently inherited that implicit `PUBLIC` grant and was callable directly
via `/rest/v1/rpc/prevent_role_escalation` by anyone signed in or not — a
trigger-only function that should never be invokable directly. Caught by
running the advisor immediately after creating it, not by review.
`20261112000003_fix_prevent_role_escalation_public_grant.sql` fixes that one
function and closes the systemic gap:

```sql
alter default privileges in schema public revoke execute on functions from public;
```

The lesson generalizes: "revoke the default" is only as complete as the list
of grantees you remembered to name. When Postgres has more than one implicit
default in play (`PUBLIC` here is separate from, and broader than, `anon`/
`authenticated`), check `pg_proc.proacl` directly after creating a new
function — don't assume a grant statement you wrote is the only grant that
exists.

The same discipline applies to table grants: `revoke all on all tables in
schema public from anon, authenticated;` first, then grant column- or
row-scoped access back per table based on what that role actually needs —
never `grant all`.

## 4. Never trust a client-provided role or status value

`profiles.role` (`user` / `doctor` / `admin`) and `doctor_profiles.status`
(`pending` / `approved` / `rejected` / `suspended`) are never set by a value
the client sends directly:

- `role` starts as `user` for every signup (via the `handle_new_user()`
  trigger on `auth.users`, which is the *only* INSERT path into `profiles`),
  and only ever changes inside `admin_set_doctor_status()` (user → doctor,
  the moment an application is approved) or `admin_assign_admin_role()`
  (→ admin) — both of which check `is_admin()` themselves before touching
  anything.
- `doctor_profiles.status` always starts at `pending` regardless of what a
  resubmission payload might contain, and only an admin RPC can move it
  from there — never a direct client `UPDATE`.
- `requests.status` only changes through the specific RPC for that
  transition (`doctor_respond_request`, `doctor_confirm_request`, etc.), each
  of which re-validates the *current* status server-side before accepting a
  transition — a stale client never gets to "skip" a state by sending the
  target status directly. A `BEFORE UPDATE` trigger
  (`validate_request_status_transition()`) rejects any transition outside
  the known-good state machine as defense in depth, in case a future RPC
  bug tries to do something illegal.

The general rule this generalizes to: **any column whose value implies an
authorization decision is written only by a function that re-derives or
re-checks that authorization itself** — never trusted as input.

## Verifying these hold

- `supabase test db` should include (and grow over time) assertions like
  "anon cannot execute `admin_set_doctor_status`", not just schema-shape
  checks.
- After any RLS or grant change, check Supabase's advisor output
  (`get_advisors` / the dashboard's Advisors tab) for `security` findings —
  several of the fixes referenced above were caught exactly this way, not
  by code review alone.
