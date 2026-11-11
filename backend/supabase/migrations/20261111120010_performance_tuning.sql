-- Addresses Supabase performance-linter findings: missing FK-covering
-- indexes, RLS policies re-evaluating auth.*() per row, and overlapping
-- permissive policies on the public catalog tables.

create index doctor_profiles_reviewed_by_idx on public.doctor_profiles (reviewed_by);
create index doctor_services_service_idx on public.doctor_services (service_id);
create index notifications_related_request_idx on public.notifications (related_request_id);
create index request_status_history_changed_by_idx on public.request_status_history (changed_by);
create index requests_service_idx on public.requests (service_id);

-- Wrap auth.*() calls in (select ...) so Postgres evaluates them once per
-- query instead of once per row (see "Auth RLS Initialization Plan").

drop policy profiles_select_own_or_admin on public.profiles;
create policy profiles_select_own_or_admin on public.profiles
  for select to authenticated
  using (
    id = (select auth.uid())
    or public.is_admin()
    or exists (
      select 1 from public.requests r
      where r.patient_id = profiles.id and r.doctor_id = (select auth.uid())
    )
  );

drop policy profiles_update_own on public.profiles;
create policy profiles_update_own on public.profiles
  for update to authenticated
  using (id = (select auth.uid()))
  with check (id = (select auth.uid()));

drop policy doctor_profiles_select_own_or_admin on public.doctor_profiles;
create policy doctor_profiles_select_own_or_admin on public.doctor_profiles
  for select to authenticated
  using (user_id = (select auth.uid()) or public.is_admin());

drop policy doctor_services_select on public.doctor_services;
create policy doctor_services_select on public.doctor_services
  for select to anon, authenticated
  using (
    doctor_id = (select auth.uid())
    or public.is_admin()
    or public.is_approved_doctor(doctor_id)
  );

drop policy doctor_weekly_availability_select on public.doctor_weekly_availability;
create policy doctor_weekly_availability_select on public.doctor_weekly_availability
  for select to authenticated
  using (doctor_id = (select auth.uid()) or public.is_admin());

drop policy requests_select_participant_or_admin on public.requests;
create policy requests_select_participant_or_admin on public.requests
  for select to authenticated
  using (
    patient_id = (select auth.uid())
    or doctor_id = (select auth.uid())
    or public.is_admin()
  );

drop policy request_status_history_select on public.request_status_history;
create policy request_status_history_select on public.request_status_history
  for select to authenticated
  using (
    public.is_admin()
    or exists (
      select 1 from public.requests r
      where r.id = request_status_history.request_id
        and (r.patient_id = (select auth.uid()) or r.doctor_id = (select auth.uid()))
    )
  );

drop policy notifications_select_own on public.notifications;
create policy notifications_select_own on public.notifications
  for select to authenticated
  using (user_id = (select auth.uid()));

-- Split "admin manages everything" into write-only policies so they no
-- longer overlap with the public SELECT policy on the same role/action.
drop policy service_categories_admin_write on public.service_categories;
create policy service_categories_admin_insert on public.service_categories
  for insert to authenticated
  with check (public.is_admin());
create policy service_categories_admin_update on public.service_categories
  for update to authenticated
  using (public.is_admin())
  with check (public.is_admin());
create policy service_categories_admin_delete on public.service_categories
  for delete to authenticated
  using (public.is_admin());

drop policy services_admin_write on public.services;
create policy services_admin_insert on public.services
  for insert to authenticated
  with check (public.is_admin());
create policy services_admin_update on public.services
  for update to authenticated
  using (public.is_admin())
  with check (public.is_admin());
create policy services_admin_delete on public.services
  for delete to authenticated
  using (public.is_admin());
