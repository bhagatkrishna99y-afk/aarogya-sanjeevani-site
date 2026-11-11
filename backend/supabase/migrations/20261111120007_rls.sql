-- Row Level Security for every table holding user data, plus explicit
-- privilege grants. Default posture: revoke everything from anon/authenticated,
-- then grant back only what each role actually needs. Mutations to
-- doctor_profiles, doctor_services, doctor_weekly_availability and requests
-- go exclusively through SECURITY DEFINER RPCs (008_rpc_functions.sql), which
-- run as the table owner and therefore bypass both RLS and these grants --
-- that is the only privileged path, and it is never reachable without the
-- authorization checks each RPC performs first.

revoke all on all tables in schema public from anon, authenticated;

-- ---------------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------------
alter table public.profiles enable row level security;

create policy profiles_select_own_or_admin on public.profiles
  for select to authenticated
  using (
    id = auth.uid()
    or public.is_admin()
    or exists (
      select 1 from public.requests r
      where r.patient_id = profiles.id and r.doctor_id = auth.uid()
    )
  );

create policy profiles_update_own on public.profiles
  for update to authenticated
  using (id = auth.uid())
  with check (id = auth.uid());

grant select, update on public.profiles to authenticated;
-- role is never client-writable: self-service updates may only touch these columns.
revoke update on public.profiles from authenticated;
grant update (full_name, phone) on public.profiles to authenticated;

-- ---------------------------------------------------------------------------
-- doctor_profiles (no direct writes -- see RPCs)
-- ---------------------------------------------------------------------------
alter table public.doctor_profiles enable row level security;

create policy doctor_profiles_select_own_or_admin on public.doctor_profiles
  for select to authenticated
  using (user_id = auth.uid() or public.is_admin());

grant select on public.doctor_profiles to authenticated;

-- ---------------------------------------------------------------------------
-- service_categories / services (public catalog)
-- ---------------------------------------------------------------------------
alter table public.service_categories enable row level security;
alter table public.services enable row level security;

create policy service_categories_select on public.service_categories
  for select to anon, authenticated
  using (is_active or public.is_admin());

create policy service_categories_admin_write on public.service_categories
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

create policy services_select on public.services
  for select to anon, authenticated
  using (is_active or public.is_admin());

create policy services_admin_write on public.services
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

grant select on public.service_categories to anon, authenticated;
grant insert, update, delete on public.service_categories to authenticated;
grant select on public.services to anon, authenticated;
grant insert, update, delete on public.services to authenticated;

-- ---------------------------------------------------------------------------
-- doctor_services (no direct writes -- see set_doctor_services RPC)
-- ---------------------------------------------------------------------------
alter table public.doctor_services enable row level security;

create policy doctor_services_select on public.doctor_services
  for select to anon, authenticated
  using (
    doctor_id = auth.uid()
    or public.is_admin()
    or public.is_approved_doctor(doctor_id)
  );

grant select on public.doctor_services to anon, authenticated;

-- ---------------------------------------------------------------------------
-- doctor_weekly_availability (no direct writes -- see set_weekly_availability RPC)
-- ---------------------------------------------------------------------------
alter table public.doctor_weekly_availability enable row level security;

create policy doctor_weekly_availability_select on public.doctor_weekly_availability
  for select to authenticated
  using (doctor_id = auth.uid() or public.is_admin());

grant select on public.doctor_weekly_availability to authenticated;

-- ---------------------------------------------------------------------------
-- requests (no direct writes -- see RPCs)
-- ---------------------------------------------------------------------------
alter table public.requests enable row level security;

create policy requests_select_participant_or_admin on public.requests
  for select to authenticated
  using (
    patient_id = auth.uid()
    or doctor_id = auth.uid()
    or public.is_admin()
  );

grant select on public.requests to authenticated;

-- ---------------------------------------------------------------------------
-- request_status_history (read-only to participants; written by trigger)
-- ---------------------------------------------------------------------------
alter table public.request_status_history enable row level security;

create policy request_status_history_select on public.request_status_history
  for select to authenticated
  using (
    public.is_admin()
    or exists (
      select 1 from public.requests r
      where r.id = request_status_history.request_id
        and (r.patient_id = auth.uid() or r.doctor_id = auth.uid())
    )
  );

grant select on public.request_status_history to authenticated;

-- ---------------------------------------------------------------------------
-- notifications (read-only to owner; written by triggers/RPCs; mark-read via RPC)
-- ---------------------------------------------------------------------------
alter table public.notifications enable row level security;

create policy notifications_select_own on public.notifications
  for select to authenticated
  using (user_id = auth.uid());

grant select on public.notifications to authenticated;
