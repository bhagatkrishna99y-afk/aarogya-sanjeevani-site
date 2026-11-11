# Aarogya Sanjeevani — Backend (Supabase)

Postgres schema, RLS policies and SECURITY DEFINER RPCs for the patient/doctor/admin
platform. The backend is the single source of truth for availability, authorization
and request status transitions — no client re-implements this logic.

## Project

Linked Supabase project ref: `oleazbruvnlmuobqomge`.

## Layout

```
supabase/
  migrations/   -- applied in filename order
  seed.sql       -- sample service categories/services for local dev
  config.toml    -- Supabase CLI config
```

## Apply the schema

Install the Supabase CLI first (`npm i -g supabase`), then from `backend/`:

```bash
supabase login
supabase link --project-ref oleazbruvnlmuobqomge
supabase db push          # applies all migrations to the linked project
supabase db execute -f supabase/seed.sql   # optional: sample catalog data
```

For local development instead of the hosted project:

```bash
supabase start             # spins up local Postgres + Auth + Studio
supabase db reset          # applies migrations + seed.sql from scratch
```

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
  `rejection_reason`, `admin_notes` are sensitive and only ever readable by the
  owning doctor or an admin — patient-facing reads go through
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
4. **Catalog**: run `seed.sql`, then confirm `select * from services;` is
   readable even as `anon`.
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
