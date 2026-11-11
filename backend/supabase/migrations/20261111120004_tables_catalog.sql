-- Public service catalog and doctor-service / availability configuration.

create table public.service_categories (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  description text,
  icon text,
  is_active boolean not null default true,
  sort_order integer not null default 0,
  created_at timestamptz not null default now()
);

create table public.services (
  id uuid primary key default gen_random_uuid(),
  category_id uuid not null references public.service_categories (id) on delete restrict,
  name text not null,
  description text,
  requires_doctor boolean not null default true,
  duration_minutes integer not null check (duration_minutes > 0),
  is_active boolean not null default true,
  sort_order integer not null default 0,
  created_at timestamptz not null default now()
);

create index services_category_idx on public.services (category_id);

-- Which services an approved doctor offers. Managed only through the
-- set_doctor_services RPC (no direct client writes -- see 007_rls.sql).
create table public.doctor_services (
  doctor_id uuid not null references public.profiles (id) on delete cascade,
  service_id uuid not null references public.services (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (doctor_id, service_id)
);

-- Recurring weekly availability. Managed only through set_weekly_availability.
create table public.doctor_weekly_availability (
  id uuid primary key default gen_random_uuid(),
  doctor_id uuid not null references public.profiles (id) on delete cascade,
  day_of_week smallint not null check (day_of_week between 0 and 6),
  start_time time not null,
  end_time time not null,
  slot_duration_minutes integer not null default 30 check (slot_duration_minutes > 0),
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  constraint doctor_weekly_availability_time_order check (end_time > start_time)
);

create index doctor_weekly_availability_doctor_idx on public.doctor_weekly_availability (doctor_id, day_of_week);
