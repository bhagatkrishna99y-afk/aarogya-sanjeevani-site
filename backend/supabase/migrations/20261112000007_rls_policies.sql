-- RLS policies for every table, per the security acceptance criteria in
-- this prompt. Most tables already had policies from earlier migrations in
-- this series (several explicitly deferred "policy work" here, to this
-- migration); this file reconciles the new spec against what's already
-- live rather than blindly dropping/recreating. Every deviation from the
-- prompt's literal wording is called out below with the reasoning, per
-- explicit decisions made before writing this file.
--
-- DECISION 1 (patient contact info): profiles RLS is narrowed to own-row
-- + admin only -- the previous doctor-sees-linked-patient clause is
-- removed. A doctor needing to identify or contact a patient now goes
-- through two narrow views instead of the raw profiles table:
--   - doctor_patient_contacts: id + full_name only, for any patient who has
--     ever had a request with this doctor (never email/phone).
--   - doctor_request_contacts: contact_phone, masked to NULL unless the
--     request's status is 'accepted' or 'confirmed' -- the doctor doesn't
--     get a patient's phone number just because they requested a slot, only
--     once the doctor has actually accepted it. Reads requests.contact_phone
--     (already added in 20261112000006), not profiles.phone.
--   This requires an app change (Doctor app's RequestsRepository currently
--   joins profiles(full_name, phone) directly) -- tracked separately.
--
-- DECISION 2 (write model for doctor_profiles/doctor_services/
-- doctor_weekly_availability): direct RLS write policies are ADDED
-- alongside the existing RPCs (submit_doctor_application,
-- set_doctor_services, set_weekly_availability), which remain in place
-- unchanged. Both paths are now valid. Direct-write WITH CHECK clauses
-- replicate the RPCs' key validation where it matters (service must be
-- requires_doctor + active; doctor must not be suspended) so the second
-- path isn't materially weaker than the first.
--
-- DECISION 3 (doctor_services/doctor_weekly_availability "manage own rows
-- while ... status"): per the prompt's own stated default, a doctor may
-- manage these while their doctor_profiles.status is anything EXCEPT
-- 'suspended' (pending doctors can draft services/availability ahead of
-- approval; suspended doctors cannot touch either).
--
-- DECISION 4 (doctor_profiles doctor-self-SELECT): admin_notes stays
-- excluded even from the doctor's own row, via a column-level grant that
-- simply never lists it. Re-adding a blanket "doctor selects own row" RLS
-- policy without this would re-expose admin_notes, which was deliberately
-- closed earlier by revoking it at the table level and routing doctor's
-- own-profile reads through get_my_doctor_application() instead. That RPC
-- remains the recommended read path; this migration additionally allows
-- direct SELECT of the doctor's own row (needed for the new direct-write
-- policies' RETURNING clauses to work), with admin_notes excluded from the
-- column grant entirely -- nobody reads it via plain table grants; a
-- future admin-specific need would get its own RPC.

begin;

-- ---------------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------------
drop policy if exists profiles_select_own_or_admin on public.profiles;
create policy profiles_select_own on public.profiles
  for select to authenticated
  using (id = (select auth.uid()));

create policy profiles_select_admin on public.profiles
  for select to authenticated
  using (is_admin());

-- profiles_update_own already exists (own row, column-granted
-- full_name/phone/avatar_url) -- unchanged, matches "update only own row,
-- only granted columns" exactly.

-- Narrow view: id + full_name only, never email/phone, for patients linked
-- to the calling doctor by any request. security_invoker = false is
-- deliberate: profiles RLS no longer lets a doctor see another user's row,
-- so this view must bypass it -- its own column list and WHERE clause are
-- the entire safety boundary (same pattern as doctors_public).
create or replace view public.doctor_patient_contacts
with (security_invoker = false) as
select distinct p.id as patient_id, p.full_name
from public.requests r
join public.profiles p on p.id = r.patient_id
where r.doctor_id = (select auth.uid());

revoke all on public.doctor_patient_contacts from anon, authenticated;
grant select on public.doctor_patient_contacts to authenticated;

-- Narrow view: a patient's contact phone for a request, visible to the
-- assigned doctor only once the request is accepted/confirmed -- reads
-- requests.contact_phone, never profiles.phone. security_invoker = true
-- here (the default we want to be explicit about): the existing
-- requests_select_participant_or_admin policy already lets a doctor read
-- their own assigned requests in full, so this view doesn't need to bypass
-- anything -- it only adds the status-based masking a plain column grant
-- can't express.
create or replace view public.doctor_request_contacts
with (security_invoker = true) as
select
  r.id as request_id,
  r.doctor_id,
  case when r.status in ('accepted', 'confirmed') then r.contact_phone else null end as contact_phone
from public.requests r
where r.doctor_id = (select auth.uid());

revoke all on public.doctor_request_contacts from anon, authenticated;
grant select on public.doctor_request_contacts to authenticated;

-- ---------------------------------------------------------------------------
-- service_categories / services: already exactly as specified from
-- 20261112000004 (anon+authenticated select is_active rows; admin full
-- select + insert/update/delete). No changes.
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- doctor_profiles
-- ---------------------------------------------------------------------------
drop policy if exists doctor_profiles_select_admin_only on public.doctor_profiles;

create policy doctor_profiles_select_own on public.doctor_profiles
  for select to authenticated
  using (user_id = (select auth.uid()));

create policy doctor_profiles_select_admin on public.doctor_profiles
  for select to authenticated
  using (is_admin());

-- Own-row UPDATE, restricted (via column grant below) to fields that don't
-- touch approval state. The role-check trigger (doctor_profiles_check_profile_role,
-- from 20261112000005) still applies to every UPDATE regardless.
create policy doctor_profiles_update_own on public.doctor_profiles
  for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

-- Own-row INSERT, only ever landing as 'pending' -- mirrors what
-- submit_doctor_application() already enforces; this is the second,
-- additional write path (Decision 2).
create policy doctor_profiles_insert_own_pending on public.doctor_profiles
  for insert to authenticated
  with check (user_id = (select auth.uid()) and status = 'pending');

grant select, insert, update on public.doctor_profiles to authenticated;
-- Column-level SELECT: everything except admin_notes (Decision 4).
revoke select on public.doctor_profiles from authenticated;
grant select (
  user_id, title, specialization, qualifications, bio, years_experience,
  license_number, consultation_modes, slot_duration_minutes, status,
  accepting_requests, rejection_reason, reviewed_by, reviewed_at,
  created_at, updated_at
) on public.doctor_profiles to authenticated;
-- Column-level INSERT/UPDATE: never status/reviewed_by/reviewed_at/
-- rejection_reason/admin_notes -- those change only via admin_set_doctor_status().
revoke insert on public.doctor_profiles from authenticated;
grant insert (
  user_id, title, specialization, qualifications, bio, years_experience,
  license_number, consultation_modes, slot_duration_minutes, accepting_requests
) on public.doctor_profiles to authenticated;
revoke update on public.doctor_profiles from authenticated;
grant update (
  title, specialization, qualifications, bio, years_experience,
  license_number, consultation_modes, slot_duration_minutes, accepting_requests
) on public.doctor_profiles to authenticated;

-- ---------------------------------------------------------------------------
-- doctor_services
-- ---------------------------------------------------------------------------
-- doctor_services_select already covers "own rows, admin, or anyone for an
-- approved doctor" -- already matches "patients can select for approved
-- doctors" exactly. No change.

-- Split by action (rather than one FOR ALL policy) so this doesn't add a
-- second, overlapping SELECT policy alongside doctor_services_select.
create policy doctor_services_insert_own on public.doctor_services
  for insert to authenticated
  with check (
    doctor_id = (select auth.uid())
    and exists (select 1 from public.doctor_profiles dp where dp.user_id = doctor_id and dp.status <> 'suspended')
    and exists (select 1 from public.services s where s.id = service_id and s.requires_doctor and s.is_active)
  );

create policy doctor_services_delete_own on public.doctor_services
  for delete to authenticated
  using (
    doctor_id = (select auth.uid())
    and exists (select 1 from public.doctor_profiles dp where dp.user_id = doctor_id and dp.status <> 'suspended')
  );

grant insert, delete on public.doctor_services to authenticated;

-- ---------------------------------------------------------------------------
-- doctor_weekly_availability
-- ---------------------------------------------------------------------------
drop policy if exists doctor_weekly_availability_select on public.doctor_weekly_availability;
create policy doctor_weekly_availability_select_own_or_admin on public.doctor_weekly_availability
  for select to authenticated
  using (doctor_id = (select auth.uid()) or is_admin());

create policy doctor_weekly_availability_select_approved_doctor on public.doctor_weekly_availability
  for select to anon, authenticated
  using (is_approved_doctor(doctor_id));

-- Split by action (rather than one FOR ALL policy) so this doesn't add a
-- second, overlapping SELECT policy alongside the two above.
create policy doctor_weekly_availability_insert_own on public.doctor_weekly_availability
  for insert to authenticated
  with check (
    doctor_id = (select auth.uid())
    and exists (select 1 from public.doctor_profiles dp where dp.user_id = doctor_id and dp.status <> 'suspended')
  );

create policy doctor_weekly_availability_update_own on public.doctor_weekly_availability
  for update to authenticated
  using (
    doctor_id = (select auth.uid())
    and exists (select 1 from public.doctor_profiles dp where dp.user_id = doctor_id and dp.status <> 'suspended')
  )
  with check (
    doctor_id = (select auth.uid())
    and exists (select 1 from public.doctor_profiles dp where dp.user_id = doctor_id and dp.status <> 'suspended')
  );

create policy doctor_weekly_availability_delete_own on public.doctor_weekly_availability
  for delete to authenticated
  using (
    doctor_id = (select auth.uid())
    and exists (select 1 from public.doctor_profiles dp where dp.user_id = doctor_id and dp.status <> 'suspended')
  );

grant insert, update, delete on public.doctor_weekly_availability to authenticated;
grant select on public.doctor_weekly_availability to anon;

-- ---------------------------------------------------------------------------
-- requests: requests_select_participant_or_admin already covers "patient
-- own, doctor own, admin all" exactly. No insert/update/delete grants
-- exist and none are added -- RPC only, unchanged.
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- request_status_history: request_status_history_select already covers
-- "patient/doctor for their own request, admin all" exactly. Append-only
-- trigger (20261112000006) already blocks all writes for every role,
-- including postgres via direct SQL outside an RPC -- "nobody can write
-- directly" except the trigger that logs it automatically. No change.
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- notifications
-- ---------------------------------------------------------------------------
-- notifications_select_own already covers "own notifications only". is_read
-- is a GENERATED column (from 20261112000006) and therefore cannot be
-- granted UPDATE on directly -- granting UPDATE on read_at alone achieves
-- the same end result, since is_read follows it automatically.
create policy notifications_update_own on public.notifications
  for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

grant update on public.notifications to authenticated;
revoke update on public.notifications from authenticated;
grant update (read_at) on public.notifications to authenticated;

-- ---------------------------------------------------------------------------
-- audit_log: admin select only.
-- ---------------------------------------------------------------------------
create policy audit_log_select_admin on public.audit_log
  for select to authenticated
  using (is_admin());

commit;
