-- Performance advisor fix: multiple_permissive_policies.
--
-- Three tables had two separate permissive SELECT policies that both
-- apply to the same role, which Postgres must evaluate and OR together on
-- every query: profiles, doctor_profiles (each had an "own row" policy
-- and a separate "is_admin()" policy, both `to authenticated`), and
-- doctor_weekly_availability (an "approved doctor, public" policy `to
-- anon, authenticated` and an "own row or admin" policy `to authenticated`
-- -- the authenticated role was paying for both on every query). Each
-- pair is merged into a single policy per table with an OR'd condition,
-- preserving the exact same accessible rows for every role -- this is a
-- pure performance change, not a visibility change.
--
-- Verified by inspecting pg_policies before writing this: no other table
-- had this pattern.

begin;

drop policy if exists profiles_select_own on public.profiles;
drop policy if exists profiles_select_admin on public.profiles;
create policy profiles_select_own_or_admin on public.profiles
  for select to authenticated
  using (id = (select auth.uid()) or is_admin());

drop policy if exists doctor_profiles_select_own on public.doctor_profiles;
drop policy if exists doctor_profiles_select_admin on public.doctor_profiles;
create policy doctor_profiles_select_own_or_admin on public.doctor_profiles
  for select to authenticated
  using (user_id = (select auth.uid()) or is_admin());

drop policy if exists doctor_weekly_availability_select_approved_doctor on public.doctor_weekly_availability;
drop policy if exists doctor_weekly_availability_select_own_or_admin on public.doctor_weekly_availability;
create policy doctor_weekly_availability_select_public_own_or_admin on public.doctor_weekly_availability
  for select to anon, authenticated
  using (
    is_approved_doctor(doctor_id)
    or doctor_id = (select auth.uid())
    or is_admin()
  );

commit;
