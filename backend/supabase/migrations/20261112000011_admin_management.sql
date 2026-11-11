-- Admin-side management RPCs: doctor status (upgraded in place),
-- user role/suspension, service catalog CRUD, and read-convenience
-- listings. Every function checks is_admin() and, where it writes, logs
-- to audit_log via the log_audit() helper from 20261112000010.
--
-- DECISION (admin_set_doctor_status, upgraded in place): this function
-- already exists with exactly this name and signature
-- (20261111120008_rpc_functions.sql), and the website's DoctorsPage.tsx
-- already calls it with only p_doctor_id/p_new_status/p_reason, discarding
-- the return value. Checked the UI before tightening the allowed-transition
-- graph: the "Reactivate" button only ever fires suspended->approved
-- (rendered only when doctor.status === 'suspended'); rejected-status rows
-- render no action buttons at all. So removing the old rejected->approved
-- transition (the prompt's explicit "rejected->pending is done only by
-- doctor resubmission" -- admin cannot skip straight to approved from
-- rejected) is safe. Everything else about this function is additive:
-- row locking, audit_log, and the suspension cascade below.
--
-- DOCUMENTED RULE (suspension cascade): when a doctor is suspended,
--   - every one of their FUTURE (scheduled_start > now()) 'requested'
--     requests is force-rejected (direct UPDATE here, not a call through
--     transition_request_status -- that function's requested->rejected
--     transition is intentionally doctor-only, and this is the admin/
--     system acting on the doctor's behalf, a different authorization
--     context). The existing log_request_status_change trigger still
--     fires on this UPDATE, so the patient still gets a request_rejected
--     notification with the reason, and the history row is still written
--     -- no logic is duplicated here.
--   - every FUTURE 'accepted'/'confirmed' request is left alone
--     (not auto-cancelled -- an admin should decide whether the patient
--     gets reassigned or the visit proceeds under special arrangement,
--     not have the system silently cancel a confirmed appointment). The
--     patient is notified (a 'system' notification explaining a review is
--     underway) and an audit_log row is written per request
--     (action = 'accepted_request_flagged_for_review') so admins can find
--     them -- chosen over adding a new "flagged" column/admin queue table,
--     since that would be a schema change beyond what this prompt asked
--     for; audit_log already exists and is exactly for this kind of
--     "something needs a human look" trail. A dedicated review queue
--     would be a reasonable follow-up if this trail proves too hard to
--     work from in practice.
--
-- DECISION (admin_set_user_role, additive): admin_assign_admin_role
-- (20261111120008) only ever promotes to admin and is still called by the
-- website's "Make admin" button -- left untouched. admin_set_user_role is
-- a new, more general function (any role, with the last-admin-demotion
-- guard this prompt asks for) for future use.
--
-- Category/service upsert RPCs and the admin_list_*/admin_get_* read
-- helpers are all new; the admin website console currently reads/writes
-- these tables directly (already covered by admin-only RLS), so these
-- RPCs are an additive, audit-logged alternative path, not a replacement.

begin;

-- ---------------------------------------------------------------------------
-- admin_set_doctor_status (upgraded in place)
-- ---------------------------------------------------------------------------
create or replace function public.admin_set_doctor_status(
  p_doctor_id uuid,
  p_new_status public.doctor_status,
  p_reason text default null
)
returns public.doctor_profiles
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_current public.doctor_profiles;
  v_result public.doctor_profiles;
  v_notification_type public.notification_type;
  v_req record;
  v_service_name text;
begin
  if not public.is_admin() then
    raise exception 'AS027_NOT_AUTHORIZED: You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  select * into v_current from public.doctor_profiles where user_id = p_doctor_id for update;
  if v_current.user_id is null then
    raise exception 'AS028_DOCTOR_NOT_FOUND: Doctor application not found.' using errcode = 'P0001';
  end if;

  if (v_current.status, p_new_status) not in (
    ('pending', 'approved'),
    ('pending', 'rejected'),
    ('approved', 'suspended'),
    ('suspended', 'approved')
  ) then
    raise exception 'AS029_INVALID_DOCTOR_STATUS_TRANSITION: That status change is not allowed from the current state.'
      using errcode = 'P0001';
  end if;

  if p_new_status = 'rejected' and coalesce(trim(p_reason), '') = '' then
    raise exception 'AS029_INVALID_DOCTOR_STATUS_TRANSITION: A reason is required when rejecting a doctor application.'
      using errcode = 'P0001';
  end if;

  update public.doctor_profiles
  set status = p_new_status,
      rejection_reason = case when p_new_status = 'rejected' then p_reason else null end,
      admin_notes = case when p_reason is not null and p_new_status <> 'rejected'
                          then coalesce(admin_notes || E'\n', '') || p_reason
                          else admin_notes end,
      reviewed_by = auth.uid(),
      reviewed_at = now()
  where user_id = p_doctor_id
  returning * into v_result;

  if p_new_status = 'approved' then
    update public.profiles set role = 'doctor' where id = p_doctor_id;
  end if;

  perform public.log_audit(
    'admin_set_doctor_status', 'doctor_profiles', p_doctor_id,
    jsonb_build_object('status', v_current.status),
    jsonb_build_object('status', p_new_status, 'reason', p_reason)
  );

  v_notification_type := case p_new_status
    when 'approved' then 'doctor_approved'
    when 'rejected' then 'doctor_rejected'
    when 'suspended' then 'doctor_suspended'
    else 'doctor_approved'
  end;

  insert into public.notifications (user_id, type, title, body)
  values (
    p_doctor_id,
    v_notification_type,
    case p_new_status
      when 'approved' then 'Application approved'
      when 'rejected' then 'Application rejected'
      when 'suspended' then 'Account suspended'
      else 'Account reactivated'
    end,
    case p_new_status
      when 'approved' then 'Your doctor application has been approved. You can now configure your services and availability.'
      when 'rejected' then coalesce('Your application was declined: ' || p_reason, 'Your application was declined.')
      when 'suspended' then 'Your account has been suspended. Please contact support for details.'
      else 'Your account has been reactivated.'
    end
  );

  -- Suspension cascade -- see the documented rule above.
  if p_new_status = 'suspended' then
    for v_req in
      select r.id, r.patient_id, r.service_id
      from public.requests r
      where r.doctor_id = p_doctor_id
        and r.status = 'requested'
        and r.scheduled_start > now()
    loop
      select name into v_service_name from public.services where id = v_req.service_id;

      update public.requests
      set status = 'rejected', rejection_reason = 'The assigned doctor is no longer available.'
      where id = v_req.id;

      perform public.log_audit(
        'doctor_suspension_auto_rejected_request', 'requests', v_req.id,
        jsonb_build_object('status', 'requested'), jsonb_build_object('status', 'rejected', 'doctor_id', p_doctor_id)
      );
    end loop;

    for v_req in
      select r.id, r.patient_id, r.service_id
      from public.requests r
      where r.doctor_id = p_doctor_id
        and r.status in ('accepted', 'confirmed')
        and r.scheduled_start > now()
    loop
      select name into v_service_name from public.services where id = v_req.service_id;

      insert into public.notifications (user_id, type, title, body, related_request_id)
      values (
        v_req.patient_id, 'system', 'Your appointment needs a review',
        format('Your upcoming %s appointment is under review because the assigned doctor is no longer available. Our team will contact you.',
          coalesce(v_service_name, 'service')),
        v_req.id
      );

      perform public.log_audit(
        'accepted_request_flagged_for_review', 'requests', v_req.id,
        null, jsonb_build_object('doctor_id', p_doctor_id, 'patient_id', v_req.patient_id)
      );
    end loop;
  end if;

  return v_result;
end;
$$;

-- CREATE OR REPLACE preserves this function's existing grants (authenticated
-- already has execute from 20261111120009); no grant statements needed.

-- ---------------------------------------------------------------------------
-- admin_set_user_role
-- ---------------------------------------------------------------------------
create or replace function public.admin_set_user_role(p_user_id uuid, p_new_role public.user_role)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_current public.profiles;
  v_admin_count integer;
begin
  if not public.is_admin() then
    raise exception 'AS027_NOT_AUTHORIZED: You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  select * into v_current from public.profiles where id = p_user_id for update;
  if v_current.id is null then
    raise exception 'AS030_USER_NOT_FOUND: User not found.' using errcode = 'P0001';
  end if;

  if v_current.role = p_new_role then
    raise exception 'AS029_INVALID_DOCTOR_STATUS_TRANSITION: That user already has this role.' using errcode = 'P0001';
  end if;

  if v_current.role = 'admin' and p_new_role <> 'admin' then
    select count(*) into v_admin_count from public.profiles where role = 'admin';
    if v_admin_count <= 1 then
      raise exception 'AS031_CANNOT_DEMOTE_LAST_ADMIN: At least one admin account must remain.' using errcode = 'P0001';
    end if;
  end if;

  update public.profiles set role = p_new_role where id = p_user_id;

  perform public.log_audit(
    'admin_set_user_role', 'profiles', p_user_id,
    jsonb_build_object('role', v_current.role), jsonb_build_object('role', p_new_role)
  );
end;
$$;

revoke all on function public.admin_set_user_role(uuid, public.user_role) from public, anon;
grant execute on function public.admin_set_user_role(uuid, public.user_role) to authenticated;

-- ---------------------------------------------------------------------------
-- admin_set_user_suspended
-- ---------------------------------------------------------------------------
create or replace function public.admin_set_user_suspended(p_user_id uuid, p_value boolean)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_current public.profiles;
begin
  if not public.is_admin() then
    raise exception 'AS027_NOT_AUTHORIZED: You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  select * into v_current from public.profiles where id = p_user_id for update;
  if v_current.id is null then
    raise exception 'AS030_USER_NOT_FOUND: User not found.' using errcode = 'P0001';
  end if;

  update public.profiles set is_suspended = p_value where id = p_user_id;

  perform public.log_audit(
    'admin_set_user_suspended', 'profiles', p_user_id,
    jsonb_build_object('is_suspended', v_current.is_suspended), jsonb_build_object('is_suspended', p_value)
  );
end;
$$;

revoke all on function public.admin_set_user_suspended(uuid, boolean) from public, anon;
grant execute on function public.admin_set_user_suspended(uuid, boolean) to authenticated;

-- ---------------------------------------------------------------------------
-- Service catalog CRUD
-- ---------------------------------------------------------------------------
create or replace function public.admin_upsert_service_category(
  p_id uuid default null,
  p_name text default null,
  p_description text default null,
  p_icon text default null,
  p_is_active boolean default true,
  p_sort_order integer default 0
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid;
  v_old public.service_categories;
begin
  if not public.is_admin() then
    raise exception 'AS027_NOT_AUTHORIZED: You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  if coalesce(trim(p_name), '') = '' then
    raise exception 'AS032_INVALID_CATALOG_FIELDS: A category name is required.' using errcode = 'P0001';
  end if;

  begin
    if p_id is null then
      insert into public.service_categories (name, description, icon, is_active, sort_order)
      values (p_name, p_description, p_icon, p_is_active, p_sort_order)
      returning id into v_id;

      perform public.log_audit('admin_upsert_service_category', 'service_categories', v_id, null, jsonb_build_object('name', p_name));
    else
      select * into v_old from public.service_categories where id = p_id;
      if v_old.id is null then
        raise exception 'AS033_CATALOG_ENTITY_NOT_FOUND: Service category not found.' using errcode = 'P0001';
      end if;

      update public.service_categories
      set name = p_name, description = p_description, icon = p_icon, is_active = p_is_active, sort_order = p_sort_order
      where id = p_id;

      v_id := p_id;
      perform public.log_audit(
        'admin_upsert_service_category', 'service_categories', v_id,
        jsonb_build_object('name', v_old.name, 'is_active', v_old.is_active),
        jsonb_build_object('name', p_name, 'is_active', p_is_active)
      );
    end if;
  exception
    when check_violation or unique_violation then
      raise exception 'AS032_INVALID_CATALOG_FIELDS: That category could not be saved -- check the name and try again.'
        using errcode = 'P0001';
  end;

  return v_id;
end;
$$;

revoke all on function public.admin_upsert_service_category(uuid, text, text, text, boolean, integer) from public, anon;
grant execute on function public.admin_upsert_service_category(uuid, text, text, text, boolean, integer) to authenticated;

create or replace function public.admin_set_category_active(p_category_id uuid, p_is_active boolean)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_old public.service_categories;
begin
  if not public.is_admin() then
    raise exception 'AS027_NOT_AUTHORIZED: You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  select * into v_old from public.service_categories where id = p_category_id for update;
  if v_old.id is null then
    raise exception 'AS033_CATALOG_ENTITY_NOT_FOUND: Service category not found.' using errcode = 'P0001';
  end if;

  update public.service_categories set is_active = p_is_active where id = p_category_id;

  perform public.log_audit(
    'admin_set_category_active', 'service_categories', p_category_id,
    jsonb_build_object('is_active', v_old.is_active), jsonb_build_object('is_active', p_is_active)
  );
end;
$$;

revoke all on function public.admin_set_category_active(uuid, boolean) from public, anon;
grant execute on function public.admin_set_category_active(uuid, boolean) to authenticated;

-- Disabling a service (admin_set_service_active below) only ever flips
-- is_active -- requests.service_id is ON DELETE RESTRICT and nothing here
-- deletes or touches existing requests, so history stays intact.
create or replace function public.admin_upsert_service(
  p_id uuid default null,
  p_category_id uuid default null,
  p_name text default null,
  p_description text default null,
  p_short_description text default null,
  p_requires_doctor boolean default true,
  p_duration_minutes integer default 30,
  p_supported_modes public.service_mode[] default array['in_person']::public.service_mode[],
  p_is_active boolean default true,
  p_sort_order integer default 0
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid;
  v_old public.services;
begin
  if not public.is_admin() then
    raise exception 'AS027_NOT_AUTHORIZED: You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  if coalesce(trim(p_name), '') = '' then
    raise exception 'AS032_INVALID_CATALOG_FIELDS: A service name is required.' using errcode = 'P0001';
  end if;
  if p_category_id is null or not exists (select 1 from public.service_categories where id = p_category_id) then
    raise exception 'AS032_INVALID_CATALOG_FIELDS: Choose a valid category.' using errcode = 'P0001';
  end if;
  if p_supported_modes is null or cardinality(p_supported_modes) = 0 then
    raise exception 'AS032_INVALID_CATALOG_FIELDS: Choose at least one supported mode.' using errcode = 'P0001';
  end if;

  begin
    if p_id is null then
      insert into public.services (
        category_id, name, description, short_description, requires_doctor,
        duration_minutes, supported_modes, is_active, sort_order
      ) values (
        p_category_id, p_name, p_description, p_short_description, p_requires_doctor,
        p_duration_minutes, p_supported_modes, p_is_active, p_sort_order
      )
      returning id into v_id;

      perform public.log_audit('admin_upsert_service', 'services', v_id, null, jsonb_build_object('name', p_name));
    else
      select * into v_old from public.services where id = p_id;
      if v_old.id is null then
        raise exception 'AS033_CATALOG_ENTITY_NOT_FOUND: Service not found.' using errcode = 'P0001';
      end if;

      update public.services
      set category_id = p_category_id,
          name = p_name,
          description = p_description,
          short_description = p_short_description,
          requires_doctor = p_requires_doctor,
          duration_minutes = p_duration_minutes,
          supported_modes = p_supported_modes,
          is_active = p_is_active,
          sort_order = p_sort_order
      where id = p_id;

      v_id := p_id;
      perform public.log_audit(
        'admin_upsert_service', 'services', v_id,
        jsonb_build_object('name', v_old.name, 'is_active', v_old.is_active),
        jsonb_build_object('name', p_name, 'is_active', p_is_active)
      );
    end if;
  exception
    when check_violation or unique_violation then
      raise exception 'AS032_INVALID_CATALOG_FIELDS: That service could not be saved -- check the fields and try again.'
        using errcode = 'P0001';
  end;

  return v_id;
end;
$$;

revoke all on function public.admin_upsert_service(
  uuid, uuid, text, text, text, boolean, integer, public.service_mode[], boolean, integer
) from public, anon;
grant execute on function public.admin_upsert_service(
  uuid, uuid, text, text, text, boolean, integer, public.service_mode[], boolean, integer
) to authenticated;

create or replace function public.admin_set_service_active(p_service_id uuid, p_is_active boolean)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_old public.services;
begin
  if not public.is_admin() then
    raise exception 'AS027_NOT_AUTHORIZED: You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  select * into v_old from public.services where id = p_service_id for update;
  if v_old.id is null then
    raise exception 'AS033_CATALOG_ENTITY_NOT_FOUND: Service not found.' using errcode = 'P0001';
  end if;

  update public.services set is_active = p_is_active where id = p_service_id;

  perform public.log_audit(
    'admin_set_service_active', 'services', p_service_id,
    jsonb_build_object('is_active', v_old.is_active), jsonb_build_object('is_active', p_is_active)
  );
end;
$$;

revoke all on function public.admin_set_service_active(uuid, boolean) from public, anon;
grant execute on function public.admin_set_service_active(uuid, boolean) to authenticated;

-- ---------------------------------------------------------------------------
-- Read-convenience listings. RLS already lets admin read these tables in
-- full; these add is_admin() as defense in depth plus pagination/joins.
-- ---------------------------------------------------------------------------
create or replace function public.admin_list_doctors(
  p_status public.doctor_status default null,
  p_limit integer default 20,
  p_offset integer default 0
)
returns table (
  user_id uuid,
  full_name text,
  email text,
  phone text,
  specialization text,
  license_number text,
  status public.doctor_status,
  years_experience integer,
  is_suspended boolean,
  created_at timestamptz,
  reviewed_at timestamptz
)
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception 'AS027_NOT_AUTHORIZED: You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  return query
  select dp.user_id, pr.full_name, pr.email::text, pr.phone, dp.specialization, dp.license_number,
         dp.status, dp.years_experience, pr.is_suspended, dp.created_at, dp.reviewed_at
  from public.doctor_profiles dp
  join public.profiles pr on pr.id = dp.user_id
  where p_status is null or dp.status = p_status
  order by dp.created_at desc
  limit greatest(coalesce(p_limit, 20), 0)
  offset greatest(coalesce(p_offset, 0), 0);
end;
$$;

revoke all on function public.admin_list_doctors(public.doctor_status, integer, integer) from public, anon;
grant execute on function public.admin_list_doctors(public.doctor_status, integer, integer) to authenticated;

create or replace function public.admin_list_requests(
  p_status public.request_status default null,
  p_doctor_id uuid default null,
  p_service_id uuid default null,
  p_date_from date default null,
  p_date_to date default null,
  p_limit integer default 20,
  p_offset integer default 0
)
returns table (
  id uuid,
  patient_id uuid,
  patient_full_name text,
  doctor_id uuid,
  doctor_full_name text,
  service_id uuid,
  service_name text,
  scheduled_start timestamptz,
  scheduled_end timestamptz,
  mode public.service_mode,
  status public.request_status,
  created_at timestamptz
)
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception 'AS027_NOT_AUTHORIZED: You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  return query
  select
    r.id, r.patient_id, pp.full_name as patient_full_name,
    r.doctor_id, dp.full_name as doctor_full_name,
    r.service_id, s.name as service_name,
    r.scheduled_start, r.scheduled_end, r.mode, r.status, r.created_at
  from public.requests r
  join public.profiles pp on pp.id = r.patient_id
  left join public.profiles dp on dp.id = r.doctor_id
  join public.services s on s.id = r.service_id
  where (p_status is null or r.status = p_status)
    and (p_doctor_id is null or r.doctor_id = p_doctor_id)
    and (p_service_id is null or r.service_id = p_service_id)
    and (p_date_from is null or r.scheduled_start >= p_date_from::timestamptz)
    and (p_date_to is null or r.scheduled_start < (p_date_to + 1)::timestamptz)
  order by r.scheduled_start desc
  limit greatest(coalesce(p_limit, 20), 0)
  offset greatest(coalesce(p_offset, 0), 0);
end;
$$;

revoke all on function public.admin_list_requests(
  public.request_status, uuid, uuid, date, date, integer, integer
) from public, anon;
grant execute on function public.admin_list_requests(
  public.request_status, uuid, uuid, date, date, integer, integer
) to authenticated;

create or replace function public.admin_get_request_detail(p_request_id uuid)
returns table (
  id uuid,
  patient_id uuid,
  patient_full_name text,
  patient_phone text,
  doctor_id uuid,
  doctor_full_name text,
  service_id uuid,
  service_name text,
  scheduled_start timestamptz,
  scheduled_end timestamptz,
  mode public.service_mode,
  status public.request_status,
  patient_notes text,
  doctor_response_note text,
  contact_phone text,
  address_text text,
  rejection_reason text,
  cancellation_reason text,
  created_at timestamptz,
  updated_at timestamptz,
  status_log jsonb
)
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception 'AS027_NOT_AUTHORIZED: You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  if not exists (select 1 from public.requests where id = p_request_id) then
    raise exception 'AS015_REQUEST_NOT_FOUND: Request not found.' using errcode = 'P0001';
  end if;

  return query
  select
    r.id, r.patient_id, pp.full_name, pp.phone,
    r.doctor_id, dp.full_name,
    r.service_id, s.name,
    r.scheduled_start, r.scheduled_end, r.mode, r.status,
    r.patient_notes, r.doctor_response_note, r.contact_phone, r.address_text,
    r.rejection_reason, r.cancellation_reason, r.created_at, r.updated_at,
    (
      select coalesce(jsonb_agg(jsonb_build_object(
        'old_status', h.old_status, 'new_status', h.new_status,
        'changed_by', h.changed_by, 'note', h.note, 'changed_at', h.changed_at
      ) order by h.changed_at), '[]'::jsonb)
      from public.request_status_history h
      where h.request_id = r.id
    ) as status_log
  from public.requests r
  join public.profiles pp on pp.id = r.patient_id
  left join public.profiles dp on dp.id = r.doctor_id
  join public.services s on s.id = r.service_id
  where r.id = p_request_id;
end;
$$;

revoke all on function public.admin_get_request_detail(uuid) from public, anon;
grant execute on function public.admin_get_request_detail(uuid) to authenticated;

commit;
