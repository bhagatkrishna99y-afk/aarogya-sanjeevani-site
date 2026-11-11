-- Enables citext for case-insensitive text comparisons (e.g. email lookups).
-- Idempotent (IF NOT EXISTS) and wrapped in a transaction per migration
-- convention -- see docs/conventions.md.

begin;

create schema if not exists extensions;
create extension if not exists citext with schema extensions;

commit;
