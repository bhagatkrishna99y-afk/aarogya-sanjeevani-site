-- Doctor-related tables: strict integrity hardening, additive + two
-- integrity-tightening FK retargets (safe: both tables are currently empty).
--
-- IMPORTANT -- `public.doctor_profiles`, `public.doctor_services` and
-- `public.doctor_weekly_availability` already exist live (migrations
-- 20261111120003/20261111120004/20261111120005), created before this spec
-- existed. Per the same instruction as prior additive migrations in this
-- series, nothing is renamed or dropped:
--   - `doctor_profiles.id` in the prompt maps to the existing primary key
--     `user_id` (already the convention documented in docs/conventions.md:
--     a table whose PK *is* a FK to another table's id is named for what it
--     refers to, not generically `id`).
--   - `years_of_experience` maps to the existing `years_experience` column.
--   - `doctor_availability` in the prompt maps to the existing
--     `doctor_weekly_availability` table.
--   - The prompt's per-profile `slot_duration_minutes` (restricted to
--     10/15/20/30/45/60) is a NEW column on doctor_profiles, distinct from
--     doctor_weekly_availability's existing per-window `slot_duration_minutes`
--     (unrestricted `> 0`, left as-is) -- the prompt's doctor_availability
--     table definition doesn't list slot_duration_minutes as one of its own
--     columns, consistent with this being a profile-level setting now.
--
-- FK retargeting: doctor_services.doctor_id and
-- doctor_weekly_availability.doctor_id currently reference profiles(id)
-- directly. Per this prompt's explicit "strict integrity" framing and table
-- definitions (both specify `references doctor_profiles(id)`), both are
-- retargeted to doctor_profiles(user_id) here -- which only accepts a
-- stricter subset of the same uuid domain (every doctor_profiles.user_id is
-- already a profiles.id), so this cannot break any existing data, and both
-- tables are currently empty (set_doctor_services/set_weekly_availability
-- already require an approved doctor_profiles row to exist first, so no
-- row has ever violated what the new FK requires).
--
-- Role trigger: "the linked profile must have role 'doctor'" is relaxed to
-- "role IN ('user', 'doctor')" (not a strict equality check) -- by explicit
-- decision, because the existing, already-shipped doctor onboarding flow
-- creates a `pending` doctor_profiles row via submit_doctor_application()
-- while the applicant's profile role is still 'user' (role only becomes
-- 'doctor' when an admin approves via admin_set_doctor_status). A strict
-- role = 'doctor' check would reject every new application at the point
-- it's created. The relaxed check still does real work: it rejects an
-- 'admin' profile ever getting a doctor_profiles row.
--
-- RLS/grants note: all three tables already have RLS enabled with
-- SELECT-only policies (own row, admin, or the narrow is_approved_doctor()
-- case for doctor_services) and anon/authenticated already hold SELECT-only
-- grants -- no insert/update/delete grants exist for any of these three,
-- consistent with "no policies yet" / lockdown-by-default in spirit (unlike
-- the catalog tables, these were RPC-gated from the start).
--
-- Slot-length-vs-window-length rule: "an availability window must be at
-- least slot_duration_minutes long" cannot be a table CHECK constraint on
-- doctor_weekly_availability alone, because the relevant slot_duration_minutes
-- now lives on doctor_profiles (a different table), and CHECK constraints
-- cannot reference another table's row. THE RULE, for whichever RPC
-- creates/updates availability windows (set_weekly_availability already
-- exists and should be updated to enforce this): before inserting a window,
-- look up the doctor's doctor_profiles.slot_duration_minutes and reject the
-- window if (end_minute - start_minute) < that value.

begin;

-- ---------------------------------------------------------------------------
-- doctor_profiles: new columns + role trigger
-- ---------------------------------------------------------------------------
alter table public.doctor_profiles add column if not exists title text;
alter table public.doctor_profiles add column if not exists qualifications text;
alter table public.doctor_profiles add column if not exists consultation_modes public.service_mode[]
  not null default array['in_person']::public.service_mode[];
alter table public.doctor_profiles add column if not exists slot_duration_minutes integer not null default 30;
alter table public.doctor_profiles add column if not exists accepting_requests boolean not null default true;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'doctor_profiles_bio_length') then
    alter table public.doctor_profiles
      add constraint doctor_profiles_bio_length check (bio is null or char_length(bio) <= 1000);
  end if;
end $$;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'doctor_profiles_years_experience_upper') then
    alter table public.doctor_profiles
      add constraint doctor_profiles_years_experience_upper check (years_experience <= 70);
  end if;
end $$;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'doctor_profiles_consultation_modes_not_empty') then
    alter table public.doctor_profiles
      add constraint doctor_profiles_consultation_modes_not_empty check (cardinality(consultation_modes) > 0);
  end if;
end $$;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'doctor_profiles_slot_duration_allowed') then
    alter table public.doctor_profiles
      add constraint doctor_profiles_slot_duration_allowed check (slot_duration_minutes in (10, 15, 20, 30, 45, 60));
  end if;
end $$;

-- "The linked profile must have role 'doctor'" per the prompt, relaxed to
-- role IN ('user', 'doctor') -- see header comment for why.
create or replace function public.doctor_profiles_check_profile_role()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_role public.user_role;
begin
  select role into v_role from public.profiles where id = new.user_id;
  if v_role is null then
    raise exception 'Profile not found.' using errcode = 'P0001';
  end if;
  if v_role not in ('user', 'doctor') then
    raise exception 'Only user or doctor profiles can have a doctor profile.' using errcode = 'P0001';
  end if;
  return new;
end;
$$;

drop trigger if exists doctor_profiles_check_profile_role on public.doctor_profiles;
create trigger doctor_profiles_check_profile_role
  before insert or update on public.doctor_profiles
  for each row execute function public.doctor_profiles_check_profile_role();

revoke all on function public.doctor_profiles_check_profile_role() from public;

-- ---------------------------------------------------------------------------
-- doctor_services: retarget FK to doctor_profiles(user_id)
-- ---------------------------------------------------------------------------
alter table public.doctor_services drop constraint if exists doctor_services_doctor_id_fkey;
alter table public.doctor_services
  add constraint doctor_services_doctor_id_fkey
  foreign key (doctor_id) references public.doctor_profiles (user_id) on delete cascade;

-- ---------------------------------------------------------------------------
-- doctor_weekly_availability: retarget FK, add updated_at, generated minute
-- columns, and the no-overlap EXCLUDE constraint.
-- ---------------------------------------------------------------------------
alter table public.doctor_weekly_availability drop constraint if exists doctor_weekly_availability_doctor_id_fkey;
alter table public.doctor_weekly_availability
  add constraint doctor_weekly_availability_doctor_id_fkey
  foreign key (doctor_id) references public.doctor_profiles (user_id) on delete cascade;

alter table public.doctor_weekly_availability add column if not exists updated_at timestamptz not null default now();

alter table public.doctor_weekly_availability
  add column if not exists start_minute integer
  generated always as (extract(hour from start_time)::int * 60 + extract(minute from start_time)::int) stored;
alter table public.doctor_weekly_availability
  add column if not exists end_minute integer
  generated always as (extract(hour from end_time)::int * 60 + extract(minute from end_time)::int) stored;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'doctor_weekly_availability_no_overlap') then
    alter table public.doctor_weekly_availability
      add constraint doctor_weekly_availability_no_overlap
      exclude using gist (
        doctor_id with =,
        day_of_week with =,
        int4range(start_minute, end_minute) with &&
      )
      where (is_active);
  end if;
end $$;

drop trigger if exists doctor_weekly_availability_set_updated_at on public.doctor_weekly_availability;
create trigger doctor_weekly_availability_set_updated_at
  before update on public.doctor_weekly_availability
  for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- Public views: safe columns only, never license_number/rejection_reason/
-- reviewed_by/email/phone. security_invoker = false is deliberate: these
-- tables' own RLS is admin-only, so an invoker-rights view would return zero
-- rows for anon/authenticated -- the view's own column list and WHERE
-- clause are the entire safety boundary here, not the underlying RLS.
-- ---------------------------------------------------------------------------
create or replace view public.doctors_public
with (security_invoker = false) as
select
  dp.user_id as doctor_id,
  p.full_name,
  dp.title,
  dp.specialization,
  dp.qualifications,
  dp.bio,
  dp.years_experience as years_of_experience,
  dp.consultation_modes,
  dp.accepting_requests,
  p.avatar_url
from public.doctor_profiles dp
join public.profiles p on p.id = dp.user_id
where dp.status = 'approved' and not p.is_suspended;

create or replace view public.doctor_services_public
with (security_invoker = false) as
select
  dp.user_id as doctor_id,
  p.full_name,
  s.id as service_id,
  s.name as service_name,
  s.slug as service_slug
from public.doctor_services ds
join public.doctor_profiles dp on dp.user_id = ds.doctor_id
join public.profiles p on p.id = dp.user_id
join public.services s on s.id = ds.service_id
where dp.status = 'approved' and not p.is_suspended and s.is_active;

revoke all on public.doctors_public from anon, authenticated;
revoke all on public.doctor_services_public from anon, authenticated;
grant select on public.doctors_public to anon, authenticated;
grant select on public.doctor_services_public to anon, authenticated;

-- ---------------------------------------------------------------------------
-- Indexes requested explicitly (several already exist from prior migrations
-- -- doctor_profiles_status_idx, doctor_services_service_idx,
-- doctor_weekly_availability_doctor_idx covering (doctor_id, day_of_week) --
-- `create index if not exists` is a no-op for those, new only where missing).
-- ---------------------------------------------------------------------------
create index if not exists doctor_profiles_status_idx on public.doctor_profiles (status);
create index if not exists doctor_services_service_idx on public.doctor_services (service_id);
create index if not exists doctor_weekly_availability_doctor_idx
  on public.doctor_weekly_availability (doctor_id, day_of_week);

commit;
