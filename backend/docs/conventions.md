# Backend conventions

These conventions apply to every migration added **from `20261112000001_enable_citext.sql` onward**.
The 14 migrations before it predate this document and are left as-is rather
than rewritten, per project decision — do not edit already-applied migration
files to "fix" them into compliance; add a new migration instead.

## Schema conventions

- **Table and column names**: `snake_case`, always singular-vs-plural by
  convention of "table = collection" (`profiles`, `requests`,
  `doctor_weekly_availability`) — no quoting required, ever.
- **Primary keys**: `id uuid primary key default gen_random_uuid()`, unless a
  table's primary key *is* a foreign key to another table's id (e.g. a
  profile-extension table keyed 1:1 on `auth.users.id`) — in that case name
  it for what it refers to (`user_id`, not `id`).
- **Timestamps**: every table gets `created_at timestamptz not null default
  now()` and `updated_at timestamptz not null default now()`. Maintain
  `updated_at` with a `BEFORE UPDATE` trigger (see `set_updated_at()` in
  `20261111120006_functions_triggers.sql`) — never leave it to the
  application layer to remember.
  - **Always `timestamptz`, never `timestamp`.** Postgres stores `timestamptz`
    as UTC internally regardless of session timezone, which is what you want
    for a server that talks to clients across timezones.
  - **Display-timezone assumption: Asia/Kolkata.** The database stores and
    reasons in UTC exclusively. Any "what day/time is this for a user"
    conversion happens in the client or at the edge (e.g. `at time zone
    'Asia/Kolkata'` in a reporting query), never baked into stored data.
    Don't add a timezone column "just in case" until a real multi-timezone
    requirement shows up.
- **"Array is not empty" checks**: use `cardinality(col) > 0`, never
  `array_length(col, 1) > 0`. `array_length()` returns `NULL` — not `0` — for
  a zero-length array (there is no "dimension 1" to report), and a `CHECK`
  constraint treats a `NULL` result as satisfied, so the array_length form
  silently lets an empty array through. Caught live while writing the pgTAP
  test for `services_supported_modes_not_empty`
  (`20261112000004_service_catalog_hardening.sql`): the test inserted `'{}'`
  expecting a rejection and got none, until the constraint was rewritten
  with `cardinality()`.
- **Case-insensitive text** (email, etc.): use the `citext` type
  (`extensions.citext` if `extensions` isn't on your `search_path` — check
  with `show search_path;` before assuming a bare `citext` resolves) instead
  of `text` + a lowercasing trigger or app-side `.toLowerCase()` discipline
  that will eventually be forgotten somewhere.
- **Foreign keys**: always specify `ON DELETE` explicitly — never leave it to
  the (also explicit, but easy to get wrong) default of `NO ACTION`.
  - `ON DELETE CASCADE` when the child row has no meaning without the parent
    (e.g. a profile's own sub-records).
  - `ON DELETE RESTRICT` when deleting the parent while children exist is
    almost certainly a mistake worth surfacing loudly (e.g. deleting a
    service category that still has services).
  - `ON DELETE SET NULL` only on a nullable column where "the reference is
    gone but this row still matters" is the correct business meaning.
  - Never silently pick the Postgres default — a bare `references
    other_table(id)` without `on delete ...` should be treated as a review
    comment waiting to happen.

## Naming: constraints, indexes, policies

- **Indexes**: `<table>_<column(s)>_idx` (`requests_patient_idx`), or
  `<table>_<purpose>_idx` for a partial/expression index
  (`notifications_unread_idx`).
- **Check/unique/exclusion constraints**: `<table>_<what_it_enforces>`
  (`requests_no_doctor_overlap`, `requests_time_order`) — the name should
  tell you what broke when the constraint violation error shows up in a log,
  without having to go look at the DDL.
- **Append-only tables** (audit/history tables nobody should ever edit or
  delete from, even at the RPC/owner level): attach the generic
  `public.reject_update_or_delete()` trigger function
  (`20261112000006_transactional_tables_strict_integrity.sql`) `BEFORE UPDATE
  OR DELETE` rather than writing a new one-off trigger function per table —
  it just raises with `errcode = '0A000'` unconditionally. Already used by
  `request_status_history` and `audit_log`; reuse it for the next one too.
- **Foreign key constraints**: left as Postgres's default
  `<table>_<column>_fkey` unless a table has more than one FK to the same
  target and the default names collide or confuse (rare; name explicitly
  when it happens).
- **RLS policies: name them like a sentence a reviewer can read without
  opening the policy body.** Prefer `<table>_<action>_<who/condition>`:
  `profiles_select_own_or_admin`, `services_admin_write`,
  `notifications_select_own`. A policy name that requires reading the `USING`
  clause to understand its scope is a policy that will be misapplied later.

## Migrations

- **Naming**: `<YYYYMMDDHHMMSS>_<clear_snake_case_description>.sql` —
  timestamp first (so `ls` / the CLI's own ordering is chronological), then
  a description specific enough that `git log` / `supabase migration list`
  output is self-explanatory without opening the file
  (`20260101000100_create_enums.sql`, not `20260101000100_update.sql`).
- **One concern per file.** Don't bundle "add table X" with "also fix
  unrelated policy Y" — makes `supabase db reset` output and `git blame`
  both harder to reason about, and makes a bad migration harder to isolate
  and re-do.
- **Idempotent where sensible**: `create table if not exists`, `create
  extension if not exists`, `drop policy if exists` before `create policy`,
  `create or replace function`. A migration that fails on a second run
  (e.g. against a database someone partially hand-patched) is a migration
  that will eventually cost someone an afternoon — the small extra typing
  cost is worth it. The exception is genuinely one-shot data migrations
  (backfills) — guard those with a `where not exists (...)` check instead of
  making them literally re-runnable, and say so in a comment.
- **Wrapped in a transaction**: every migration file starts with `begin;`
  and ends with `commit;`. The Supabase CLI does not add this for you — if a
  statement partway through a migration fails, you want the whole file to
  have done nothing, not left the schema half-migrated.
  - Exception: statements that cannot run inside a transaction block
    (`create index concurrently`, `alter type ... add value` before use in
    the same transaction on older Postgres). When you hit one of these, say
    so in a comment at the top of the file explaining why `begin`/`commit`
    is absent for that specific statement.
- **Never edit an already-applied migration file.** If migration 014 shipped
  something wrong, the fix is a new migration (015) that corrects it — see
  `20261111120011_fix_is_admin_anon_grant.sql`,
  `20261111120012_fix_get_available_slots_timezone.sql`, and
  `20261111120014_fix_doctor_own_admin_notes_leak.sql` for real examples of
  exactly this pattern from this project's own history.

## Seed data

- Lives under `supabase/seed/`, one file per logical group, numbered so
  `supabase db reset` applies them in a predictable order
  (`001_catalog.sql`, `002_...sql`). Referenced via `sql_paths` in
  `supabase/config.toml`'s `[db.seed]` section, not the single-file
  `seed.sql` default.
- Seed SQL must be idempotent (`on conflict ... do nothing`, `where not
  exists (...)`) — it runs every time someone does `supabase db reset`,
  including against a database that already has the seed data from a
  previous reset.
