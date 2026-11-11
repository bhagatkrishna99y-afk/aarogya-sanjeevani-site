-- Core enums + profiles hardening, additive only.
--
-- IMPORTANT -- this project already has a live `public.profiles` table and a
-- `user_role` enum (`user`/`doctor`/`admin`) from migrations 20261111120002
-- and 20261111120003, applied before this spec existed. Per explicit
-- instruction, this migration does NOT rename, drop or recreate either --
-- `user_role` is used wherever the spec says `app_role`. `doctor_status` and
-- `request_status` also already exist with the exact label sets requested
-- (request_status's internal enumsortorder differs from the order listed in
-- the prompt -- requested/accepted/confirmed/completed/rejected/cancelled vs
-- .../rejected/cancelled/completed -- left as-is since the label *set* is
-- identical and no code depends on enum ordinal order; reordering an enum
-- requires recreating the type, which is out of scope for an additive
-- migration). Only `service_mode` and `notification_type` are new.
--
-- RLS note: `public.profiles` already has RLS enabled with two real policies
-- (`profiles_select_own_or_admin`, `profiles_update_own`) from migration
-- 20261111120007 -- not "enabled with no policies" as a from-scratch spec
-- would assume. Removing them to match that assumption would break the
-- website and both Android apps, which depend on being able to read/update
-- profiles today. They are left untouched; RLS policy work for any *new*
-- tables stays out of scope here per instruction (that's prompt 1.6).

begin;

-- ---------------------------------------------------------------------------
-- New enums only (user_role / doctor_status / request_status already exist)
-- ---------------------------------------------------------------------------
do $$ begin
  if not exists (select 1 from pg_type where typname = 'service_mode') then
    create type public.service_mode as enum ('in_person', 'home_visit', 'online');
  end if;
end $$;

do $$ begin
  if not exists (select 1 from pg_type where typname = 'notification_type') then
    create type public.notification_type as enum (
      'request_created',
      'request_accepted',
      'request_rejected',
      'request_confirmed',
      'request_cancelled',
      'request_completed',
      'doctor_approved',
      'doctor_rejected',
      'doctor_suspended',
      'system'
    );
  end if;
end $$;

-- ---------------------------------------------------------------------------
-- profiles: additive columns (email, avatar_url, is_suspended)
-- ---------------------------------------------------------------------------
alter table public.profiles add column if not exists email citext;
alter table public.profiles add column if not exists avatar_url text;
alter table public.profiles add column if not exists is_suspended boolean not null default false;

-- Backfill email for any rows created before this migration.
update public.profiles p
set email = u.email
from auth.users u
where u.id = p.id and p.email is null;

-- Only enforce NOT NULL once every existing row has been backfilled.
do $$ begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'profiles' and column_name = 'email' and is_nullable = 'YES'
  ) then
    alter table public.profiles alter column email set not null;
  end if;
end $$;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'profiles_email_key') then
    alter table public.profiles add constraint profiles_email_key unique (email);
  end if;
end $$;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'profiles_full_name_length') then
    alter table public.profiles
      add constraint profiles_full_name_length check (char_length(full_name) between 2 and 100);
  end if;
end $$;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'profiles_phone_format') then
    -- Basic format guard, not full E.164 validation: optional leading +,
    -- digits/spaces/parens/hyphens, 7-20 chars total.
    alter table public.profiles
      add constraint profiles_phone_format check (phone is null or phone ~ '^\+?[0-9 ()-]{7,20}$');
  end if;
end $$;

-- ---------------------------------------------------------------------------
-- handle_new_user(): now also sets email, and derives role from signup_role
-- (never trusted directly -- only 'user'/'doctor' are accepted, anything
-- else including 'admin' silently lands as 'user'; this is the ONLY place
-- role is set on insert, and it never reads a client-supplied role column).
-- ---------------------------------------------------------------------------
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_role public.user_role;
begin
  v_role := case new.raw_user_meta_data ->> 'signup_role'
    when 'doctor' then 'doctor'::public.user_role
    else 'user'::public.user_role
  end;

  insert into public.profiles (id, full_name, email, phone, role)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1)),
    new.email,
    new.raw_user_meta_data ->> 'phone',
    v_role
  );
  return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- prevent_role_escalation(): blocks direct client writes to role/email/
-- is_suspended regardless of grants, as defense in depth. SECURITY DEFINER
-- RPCs (admin_set_doctor_status, admin_assign_admin_role, ...) execute this
-- trigger as their owner (`postgres`), so they are unaffected -- the
-- authorization check for *those* already happens inside each RPC via
-- is_admin() before the UPDATE runs. A direct client UPDATE always executes
-- as the `authenticated` (or `anon`) Postgres role, which this trigger
-- rejects outright.
-- ---------------------------------------------------------------------------
create or replace function public.prevent_role_escalation()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if (new.role is distinct from old.role)
     or (new.email is distinct from old.email)
     or (new.is_suspended is distinct from old.is_suspended)
  then
    if current_user not in ('postgres', 'service_role') then
      raise exception 'You are not authorized to change role, email or suspension status.'
        using errcode = '42501';
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists profiles_prevent_role_escalation on public.profiles;
create trigger profiles_prevent_role_escalation
  before update on public.profiles
  for each row execute function public.prevent_role_escalation();

-- ---------------------------------------------------------------------------
-- Helper functions (is_admin() already exists from 20261111120006; adding
-- the two that don't).
-- ---------------------------------------------------------------------------
create or replace function public.is_doctor()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.profiles where id = auth.uid() and role = 'doctor'
  );
$$;

create or replace function public.current_app_role()
returns public.user_role
language sql
stable
security definer
set search_path = ''
as $$
  select role from public.profiles where id = auth.uid();
$$;

revoke all on function public.is_doctor() from public;
revoke all on function public.current_app_role() from public;
grant execute on function public.is_doctor() to authenticated;
grant execute on function public.current_app_role() to authenticated;

-- ---------------------------------------------------------------------------
-- Grants: re-affirm explicitly (already true in practice; avatar_url is the
-- one actual change -- added to the editable-column set now that it exists).
-- ---------------------------------------------------------------------------
revoke all on public.profiles from anon;
grant select on public.profiles to authenticated;
revoke update on public.profiles from authenticated;
grant update (full_name, phone, avatar_url) on public.profiles to authenticated;

commit;
