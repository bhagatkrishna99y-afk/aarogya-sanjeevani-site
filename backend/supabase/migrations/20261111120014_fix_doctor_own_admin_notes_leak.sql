-- Found while designing the Doctor app's "my application status" screen:
-- doctor_profiles_select_own_or_admin let a doctor SELECT their own row in
-- full, including admin_notes -- an internal, admin-only field (license
-- number and rejection_reason are meant for the doctor; admin_notes is not).
-- RLS filters rows, not columns, so a single "own row OR admin" policy can't
-- mask one column differently per caller. Column-level GRANTs can't do it
-- either, since both the doctor and the admin are the same Postgres
-- `authenticated` role. The fix: restrict the table's SELECT policy to
-- admins only, and give doctors a SECURITY DEFINER RPC that returns their
-- own row with admin_notes excluded -- the same pattern already used for
-- patient-facing doctor reads (list_approved_doctors / get_doctor_public_profile).

drop policy doctor_profiles_select_own_or_admin on public.doctor_profiles;

create policy doctor_profiles_select_admin_only on public.doctor_profiles
  for select to authenticated
  using (public.is_admin());

create function public.get_my_doctor_application()
returns table (
  user_id uuid,
  specialization text,
  bio text,
  license_number text,
  years_experience integer,
  status public.doctor_status,
  rejection_reason text,
  reviewed_at timestamptz,
  created_at timestamptz,
  updated_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
  select
    dp.user_id, dp.specialization, dp.bio, dp.license_number, dp.years_experience,
    dp.status, dp.rejection_reason, dp.reviewed_at, dp.created_at, dp.updated_at
  from public.doctor_profiles dp
  where dp.user_id = auth.uid();
$$;

revoke all on function public.get_my_doctor_application() from public;
grant execute on function public.get_my_doctor_application() to authenticated;
