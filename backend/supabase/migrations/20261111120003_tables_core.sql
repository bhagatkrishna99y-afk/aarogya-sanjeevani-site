-- profiles: one row per auth.users row. role defaults to 'user' and is never
-- writable by the client (see 007_rls.sql column grants) -- it only changes
-- through the admin_set_doctor_status / admin_assign_admin_role RPCs.
create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  role public.user_role not null default 'user',
  full_name text not null,
  phone text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table public.profiles is 'One row per authenticated user. role is server-controlled only.';

-- doctor_profiles: the doctor application / professional profile.
-- license_number, rejection_reason and admin_notes are sensitive and must
-- never be returned to patients -- enforced by keeping all patient-facing
-- doctor reads behind SECURITY DEFINER RPCs that select a safe column list,
-- never direct table grants.
create table public.doctor_profiles (
  user_id uuid primary key references public.profiles (id) on delete cascade,
  specialization text not null,
  bio text,
  license_number text not null,
  years_experience integer not null default 0 check (years_experience >= 0),
  status public.doctor_status not null default 'pending',
  rejection_reason text,
  admin_notes text,
  reviewed_by uuid references public.profiles (id),
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table public.doctor_profiles is 'Doctor application + professional profile. Sensitive columns are never exposed to patients.';

create index doctor_profiles_status_idx on public.doctor_profiles (status);
