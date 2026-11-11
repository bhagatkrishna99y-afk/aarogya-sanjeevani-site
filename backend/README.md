# Aarogya Sanjeevani — Backend (Supabase)

Postgres schema, RLS policies and SECURITY DEFINER RPCs for the patient/doctor/admin
platform. The backend is the single source of truth for availability, authorization
and request status transitions — no client re-implements this logic.

See also: [`docs/conventions.md`](docs/conventions.md) (schema and migration
conventions) and [`docs/security-principles.md`](docs/security-principles.md)
(the non-negotiable security rules this backend follows, with pointers to
where each one is implemented).

## Project

Linked Supabase project ref: `oleazbruvnlmuobqomge`.

## Layout

```
supabase/
  migrations/        -- schema changes, applied in filename order, never edited after being applied
  seed/              -- idempotent sample/dev data, applied in filename order (see config.toml [db.seed])
  tests/database/    -- pgTAP tests, run via `supabase test db`
  config.toml        -- Supabase CLI config
docs/
  conventions.md          -- naming, typing, migration conventions
  security-principles.md  -- RLS / SECURITY DEFINER / privilege model rules
.env.example          -- SUPABASE_URL + SUPABASE_ANON_KEY only; never the service key
```

## Setup

Install the Supabase CLI first (`npm i -g supabase`), then from `backend/`:

```bash
cp .env.example .env   # fill in your own values if not using the project above
supabase login
supabase link --project-ref oleazbruvnlmuobqomge
```

## Local development loop

```bash
supabase start      # spins up local Postgres + Auth + Studio in Docker (first run pulls images, so it's slow once)
supabase db reset    # drops the local DB, replays every migration in supabase/migrations, then every seed file in supabase/seed
```

Studio (local Postgres/Auth UI) is printed in `supabase start`'s output,
typically `http://localhost:54323`. Run `supabase db reset` after pulling any
new migration, and any time your local DB state feels suspect — it's cheap
and meant to be run often.

## Running tests

```bash
supabase test db
```

This runs every pgTAP file under `supabase/tests/database/` against a fresh
local database (it implies a `db reset` first). Add a new test file per
concern as the schema grows — see `supabase/tests/database/000_extensions_test.sql`
for the current minimal example. A test file should `begin; select plan(n);
... select * from finish(); rollback;` so it never leaves data behind.

## Creating a new migration

```bash
supabase migration new clear_snake_case_description
```

This creates `supabase/migrations/<timestamp>_clear_snake_case_description.sql`
with the correct timestamp already filled in — prefer this over creating the
file by hand so the ordering is never accidentally wrong. Then:

1. Write the migration following [`docs/conventions.md`](docs/conventions.md)
   — wrapped in `begin;`/`commit;`, idempotent where sensible, one concern
   per file.
2. `supabase db reset` locally to confirm it applies cleanly from scratch
   (not just on top of your current local state).
3. Add or update a pgTAP test under `supabase/tests/database/` if the change
   is security- or behavior-relevant (new RLS policy, new RPC, new
   constraint) and run `supabase test db`.
4. Commit the migration file. **Never edit a migration that has already been
   pushed/applied** — ship a follow-up migration instead, even to fix a typo.

## Linking and pushing to a hosted project

Already linked to `oleazbruvnlmuobqomge` per Setup above. To push local
migrations you've added to the hosted project:

```bash
supabase db push          # applies any migrations not yet recorded as run on the linked project
```

To point this repo at a *different* hosted project instead (e.g. a new
environment):

```bash
supabase link --project-ref <other-project-ref>
supabase db push
```

`supabase db push` only runs migrations the hosted project doesn't already
have a record of (tracked in its own `supabase_migrations.schema_migrations`
table) — it will not re-run or re-order anything. Seed data is **not** pushed
by `db push`; run it explicitly against the hosted project if needed:

```bash
supabase db execute -f supabase/seed/001_catalog.sql --linked
```

## CI

`.github/workflows/backend-db-tests.yml` (repo root) runs `supabase db reset`
then `supabase test db` on every push/PR that touches `backend/supabase/**`,
using the Supabase CLI's local Docker stack — no connection to the hosted
project, no secrets required.

## Recommended manual hardening

Supabase's security advisor flags **leaked password protection** as disabled
by default. Enable it from the dashboard: Authentication → Policies →
Password Security → "Check against HaveIBeenPwned.org". This can't be set
via SQL/migration, only from the dashboard or Management API.

## Bootstrapping the first admin

No RPC can grant the `admin` role to prevent self-elevation. After the first
real user signs up through any client, promote them manually once, from the
SQL editor (Studio or `supabase db execute`) using the service role:

```sql
update public.profiles set role = 'admin' where id = '<their auth.users id>';
```

Every subsequent admin can be added by an existing admin via the
`admin_assign_admin_role(p_user_id uuid)` RPC — never self-service.

## Schema overview

- `profiles` — one row per `auth.users` row. `role` (`user`/`doctor`/`admin`) is
  never client-writable; it only changes via `admin_set_doctor_status` (user → doctor)
  or `admin_assign_admin_role` (→ admin).
- `doctor_profiles` — application + professional profile. `license_number`,
  `rejection_reason`, `admin_notes` are sensitive; `admin_notes` is admin-only
  (the owning doctor reads their own status via `get_my_doctor_application()`,
  which excludes it) — patient-facing reads go through
  `list_approved_doctors` / `get_doctor_public_profile`, which select a safe
  column list only.
- `service_categories`, `services` — public catalog, admin-managed.
- `doctor_services`, `doctor_weekly_availability` — doctor configuration,
  managed only via `set_doctor_services` / `set_weekly_availability`.
- `requests` — the booking entity. All transitions
  (`requested → accepted → confirmed → completed`, plus `rejected`/`cancelled`)
  happen only through RPCs; a trigger rejects any other transition as defense
  in depth, and an `EXCLUDE` constraint (`requests_no_doctor_overlap`) makes
  double-booking impossible at the database level, independent of app logic.
- `request_status_history` — full audit trail, written automatically by trigger.
- `notifications` — in-app only (no push/email/SMS in this release).

## Key RPCs

| Function | Caller | Purpose |
|---|---|---|
| `submit_doctor_application` | authenticated user | Register/resubmit a doctor application (always lands in `pending`) |
| `get_my_doctor_application` | authenticated user | Read your own application/status, excluding admin-only fields |
| `admin_set_doctor_status` | admin | Approve/reject/suspend/reactivate; only path that grants the `doctor` role |
| `admin_assign_admin_role` | admin | Grant admin role to another user |
| `list_approved_doctors` / `get_doctor_public_profile` | anyone | Safe, public doctor directory |
| `set_doctor_services` | approved doctor | Configure which services they offer |
| `set_weekly_availability` | approved doctor | Configure recurring weekly schedule |
| `get_available_slots` | authenticated user | Compute open slots for a doctor/service/date |
| `create_request` | authenticated user | Submit a booking request (DB constraint guards against races) |
| `doctor_respond_request` | assigned doctor | Accept or reject a `requested` request |
| `doctor_confirm_request` / `doctor_complete_request` | assigned doctor | Advance `accepted → confirmed → completed` |
| `cancel_request` | patient, assigned doctor, or admin | Cancel while still cancellable |
| `mark_notification_read` | owner | Mark one notification read |

## Verifying the backend

After `supabase db push` (or `db reset` locally):

1. **RLS is on everywhere**: in Studio → Table Editor, every table under
   `public` should show the RLS shield as enabled.
2. **Sign up two users** via Supabase Auth (e.g. through the Studio Auth tab or
   `supabase.auth.signUp` from any client) — confirm a `profiles` row appears
   with `role = 'user'` for each.
3. **Doctor flow**: call `submit_doctor_application(...)` as user A → confirm
   `doctor_profiles.status = 'pending'`. As an admin (after bootstrapping,
   above), call `admin_set_doctor_status(userA_id, 'approved')` → confirm
   `profiles.role` for user A becomes `'doctor'`.
4. **Catalog**: run the seed files under `supabase/seed/`, then confirm
   `select * from services;` is readable even as `anon`.
5. **Availability + booking**: as the approved doctor, call
   `set_doctor_services` and `set_weekly_availability`; as user B, call
   `get_available_slots(doctorId, serviceId, someDate)` and then
   `create_request(...)` with one of the returned slots. Try calling
   `create_request` twice with the same slot from two sessions — the second
   must fail with "That slot was just booked by someone else."
6. **Status transitions**: as the doctor, call `doctor_respond_request`,
   `doctor_confirm_request`, `doctor_complete_request` in order, and confirm
   `request_status_history` gains a row each time and the patient's
   `notifications` table gains an entry each time.
7. **Negative checks**: confirm a plain `update public.requests set status = ...`
   from a client (not an RPC) is rejected (no UPDATE grant), and that selecting
   `license_number` or `rejection_reason` as a different user returns no rows.
