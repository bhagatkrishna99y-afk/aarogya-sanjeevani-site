-- Service catalog hardening, additive only.
--
-- IMPORTANT -- `public.service_categories` and `public.services` already
-- exist live (migration 20261111120004), created before this spec existed.
-- Per the same instruction as 20261112000002 (don't rename/drop/recreate
-- anything already live), this migration only adds what's missing:
--   - `display_order` in the prompt maps to the existing `sort_order` column
--     on both tables (same meaning, kept under its existing name).
--   - `default_duration_minutes` in the prompt maps to the existing
--     `services.duration_minutes` column; its check constraint is tightened
--     from `> 0` to `between 10 and 240` as requested (existing seed values
--     15/20/30 all satisfy the new range, so this is safe against current data).
--   - Everything else requested (slug, short_description, supported_modes,
--     metadata, updated_at, the new indexes) is genuinely new and added here.
--
-- RLS/grants note: both tables already have RLS enabled with real policies
-- (`*_select` restricting non-admins to `is_active` rows, `*_admin_insert/
-- update/delete` gating writes to `is_admin()`) from migration 20261111120007,
-- and `authenticated` already holds table-level INSERT/UPDATE/DELETE grants
-- used directly by the website's admin console -- not "RLS enabled with no
-- policies" / "SELECT only" as a from-scratch spec would assume. Per explicit
-- instruction, these are left exactly as they are: tightening the grants to
-- SELECT-only now, with no catalog-management RPC yet to replace the
-- console's direct writes, would break already-shipped admin functionality.
-- Revisit this once a catalog-management RPC exists.
--
-- Inactive-category rule: there is no way to enforce "a service cannot be
-- created under an inactive category" with a plain CHECK constraint --
-- CHECK constraints cannot reference another table's row, and category_id's
-- target category can be deactivated independently after a service already
-- exists under it anyway. This rule belongs in the admin RPC that creates/
-- moves services once one exists (none does yet -- see the grants note
-- above). THE RULE: an admin RPC that inserts or updates a service's
-- category_id must first check `exists (select 1 from service_categories
-- where id = p_category_id and is_active)` and raise a friendly error
-- otherwise. Until that RPC exists, this is enforced nowhere and is
-- documented here so it isn't forgotten when it's built.

begin;

-- ---------------------------------------------------------------------------
-- service_categories: slug + updated_at
-- ---------------------------------------------------------------------------
alter table public.service_categories add column if not exists slug text;
alter table public.service_categories add column if not exists updated_at timestamptz not null default now();

update public.service_categories
set slug = regexp_replace(trim(both '-' from regexp_replace(lower(name), '[^a-z0-9]+', '-', 'g')), '-+', '-', 'g')
where slug is null;

do $$ begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'service_categories' and column_name = 'slug' and is_nullable = 'YES'
  ) then
    alter table public.service_categories alter column slug set not null;
  end if;
end $$;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'service_categories_slug_key') then
    alter table public.service_categories add constraint service_categories_slug_key unique (slug);
  end if;
end $$;

drop trigger if exists service_categories_set_updated_at on public.service_categories;
create trigger service_categories_set_updated_at
  before update on public.service_categories
  for each row execute function public.set_updated_at();

create index if not exists service_categories_is_active_idx on public.service_categories (is_active);
create index if not exists service_categories_sort_order_idx on public.service_categories (sort_order);

-- ---------------------------------------------------------------------------
-- services: slug, short_description, supported_modes, metadata, updated_at
-- ---------------------------------------------------------------------------
alter table public.services add column if not exists slug text;
alter table public.services add column if not exists short_description text;
alter table public.services add column if not exists supported_modes public.service_mode[]
  not null default array['in_person']::public.service_mode[];
alter table public.services add column if not exists metadata jsonb not null default '{}'::jsonb;
alter table public.services add column if not exists updated_at timestamptz not null default now();

update public.services
set slug = regexp_replace(trim(both '-' from regexp_replace(lower(name), '[^a-z0-9]+', '-', 'g')), '-+', '-', 'g')
where slug is null;

do $$ begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'services' and column_name = 'slug' and is_nullable = 'YES'
  ) then
    alter table public.services alter column slug set not null;
  end if;
end $$;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'services_slug_key') then
    alter table public.services add constraint services_slug_key unique (slug);
  end if;
end $$;

-- cardinality(), not array_length(arr, 1): array_length() returns NULL (not
-- 0) for a zero-length array, which a CHECK constraint treats as satisfied --
-- confirmed live while writing the pgTAP test for this constraint, which
-- initially passed an empty array straight through.
do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'services_supported_modes_not_empty') then
    alter table public.services
      add constraint services_supported_modes_not_empty check (cardinality(supported_modes) > 0);
  end if;
end $$;

-- Tighten duration_minutes from "> 0" to "between 10 and 240" as requested.
do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'services_duration_minutes_range') then
    if exists (select 1 from pg_constraint where conname = 'services_duration_minutes_check') then
      alter table public.services drop constraint services_duration_minutes_check;
    end if;
    alter table public.services
      add constraint services_duration_minutes_range check (duration_minutes between 10 and 240);
  end if;
end $$;

drop trigger if exists services_set_updated_at on public.services;
create trigger services_set_updated_at
  before update on public.services
  for each row execute function public.set_updated_at();

create index if not exists services_is_active_idx on public.services (is_active);
create index if not exists services_sort_order_idx on public.services (sort_order);

-- ---------------------------------------------------------------------------
-- Auto-generate slug on insert when not supplied. Required because the
-- website's admin console (CatalogPage.tsx) inserts categories/services
-- directly (per the kept grants above) without a slug field -- without
-- this, every "Add category"/"Add service" call would now fail on the new
-- NOT NULL constraint. Collision-safe: appends -2, -3, ... if the slugified
-- name is already taken.
-- ---------------------------------------------------------------------------
create or replace function public.slugify(input text)
returns text
language sql
immutable
set search_path = ''
as $$
  select regexp_replace(trim(both '-' from regexp_replace(lower(input), '[^a-z0-9]+', '-', 'g')), '-+', '-', 'g');
$$;

create or replace function public.service_categories_generate_slug()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_base text;
  v_candidate text;
  v_suffix int := 1;
begin
  if new.slug is null or btrim(new.slug) = '' then
    v_base := public.slugify(new.name);
    v_candidate := v_base;
    while exists (select 1 from public.service_categories where slug = v_candidate) loop
      v_suffix := v_suffix + 1;
      v_candidate := v_base || '-' || v_suffix;
    end loop;
    new.slug := v_candidate;
  end if;
  return new;
end;
$$;

drop trigger if exists service_categories_generate_slug on public.service_categories;
create trigger service_categories_generate_slug
  before insert on public.service_categories
  for each row execute function public.service_categories_generate_slug();

create or replace function public.services_generate_slug()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_base text;
  v_candidate text;
  v_suffix int := 1;
begin
  if new.slug is null or btrim(new.slug) = '' then
    v_base := public.slugify(new.name);
    v_candidate := v_base;
    while exists (select 1 from public.services where slug = v_candidate) loop
      v_suffix := v_suffix + 1;
      v_candidate := v_base || '-' || v_suffix;
    end loop;
    new.slug := v_candidate;
  end if;
  return new;
end;
$$;

drop trigger if exists services_generate_slug on public.services;
create trigger services_generate_slug
  before insert on public.services
  for each row execute function public.services_generate_slug();

revoke all on function public.slugify(text) from public;
revoke all on function public.service_categories_generate_slug() from public;
revoke all on function public.services_generate_slug() from public;

commit;
