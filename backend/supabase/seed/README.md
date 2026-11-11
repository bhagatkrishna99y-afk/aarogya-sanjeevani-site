# Seed data

`seed.sql` runs automatically after migrations whenever you run
`supabase db reset` locally (configured in `supabase/config.toml`'s
`[db.seed]` section). It is idempotent -- safe to re-run against a
database that already has this seed data.

**This is development-only data. Never run `seed.sql` against a
production project**, and never reuse these credentials or this pattern
of inserting directly into `auth.users`/`auth.identities` outside a local
dev database -- that approach exists only because `supabase db reset` has
no running Auth service to call a real signup API against.

## Test accounts

All passwords: `DevPass123!`

| Role | Email | Notes |
|---|---|---|
| Patient | `patient1@aarogya.test` | Asha Mehta |
| Patient | `patient2@aarogya.test` | Rohan Gupta |
| Doctor (approved) | `doctor.approved1@aarogya.test` | Dr. Priya Sharma, General Medicine -- has services + weekly availability configured |
| Doctor (approved) | `doctor.approved2@aarogya.test` | Dr. Vikram Rao, Dermatology -- has services + weekly availability configured |
| Doctor (pending) | `doctor.pending@aarogya.test` | Dr. Neha Joshi, Cardiology -- awaiting admin approval, no services/availability yet |
| Doctor (suspended) | `doctor.suspended@aarogya.test` | Dr. Arjun Nair, Orthopedics -- suspended, for testing the suspended-account paths |
| Admin | `admin@aarogya.test` | Full admin console access |

## What's seeded

- 5 service categories, 3 services each (15 total) -- see `seed.sql` for
  the exact list, covering both doctor-required and no-doctor-required
  services (e.g. diagnostic sample collection at home never asks for a
  doctor).
- The 7 accounts above, created via direct `auth.users`/`auth.identities`
  inserts (the standard approach for CLI-seeded test accounts, since
  `supabase db reset` seeding has no live Auth service to sign up
  through) -- fully functional for email/password sign-in in the Patient
  and Doctor apps against a local stack.
- A handful of sample `requests` in different statuses (`requested`,
  `accepted`, `completed`) so the apps have something to show immediately
  after seeding.

## Re-running

`supabase db reset` wipes and rebuilds the local database (migrations +
seeds) from scratch, so re-running it is the normal way to get back to
this exact state. Running `seed.sql` again by itself (e.g. via `psql -f`)
is also safe -- every insert is guarded (`on conflict` or a
lookup-before-insert check), so it won't create duplicates or error on
rows that already exist.
