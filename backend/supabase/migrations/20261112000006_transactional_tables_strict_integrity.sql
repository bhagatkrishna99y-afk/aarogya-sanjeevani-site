-- Transactional tables: strict integrity hardening. Mostly additive, plus a
-- small number of deliberate, explicitly-requested behavior changes on the
-- existing live `requests`/`request_status_history`/`notifications` tables
-- (all three empty at migration time -- verified live before writing this),
-- and one brand-new table (`audit_log`).
--
-- Naming map (nothing renamed/dropped, per the policy established in this
-- migration series -- new spec names map onto existing names):
--   service_requests            -> requests
--   service_request_status_log  -> request_status_history
--   start_at / end_at           -> scheduled_start / scheduled_end
--   request_id (on notifications) -> related_request_id
--   message (on notifications)  -> body
--
-- Deliberate behavior changes (not purely additive -- called out explicitly):
--   1. requests.patient_id FK: ON DELETE CASCADE -> ON DELETE RESTRICT, and
--      requests.doctor_id FK retargeted from profiles(id) to
--      doctor_profiles(user_id) with ON DELETE RESTRICT, and requests.service_id
--      FK made explicitly ON DELETE RESTRICT (was unnamed NO ACTION) -- all
--      per this prompt's explicit table definition. Safe: `requests` is
--      currently empty.
--   2. The EXCLUDE constraint's WHERE clause is narrowed from "status IN
--      (requested, accepted, confirmed)" to "status IN (accepted, confirmed)"
--      -- per this prompt's explicit constraint definition AND its explicit
--      test requirement ("two overlapping requested requests are allowed"),
--      both independently confirming the same intent. This is a genuine
--      product behavior change from what's currently shipped (create_request
--      today rejects ANY overlapping request immediately, even a merely
--      "requested" one) -- multiple patients can now submit overlapping
--      requests for the same doctor/time, and only acceptance is exclusive.
--      KNOWN GAP, out of scope for this schema-only migration: when a doctor
--      accepts one of several overlapping "requested" siblings, the others
--      are not auto-rejected -- that needs a future RPC change
--      (doctor_respond_request should reject overlapping siblings when
--      accepting one). The new partial unique index below only closes the
--      narrower "same patient, same doctor, same start_at" duplicate case.
--   3. notifications.related_request_id FK: ON DELETE CASCADE -> ON DELETE
--      SET NULL, per this prompt's explicit table definition -- notifications
--      now outlive the request they reference, consistent with being a
--      user-facing log rather than data strictly owned by the request.
--   4. notifications.type converted from `text` to the `notification_type`
--      enum (added in 20261112000002 but never wired to this column until
--      now). This surfaced a real, previously-undetected bug: existing
--      trigger code emitted values that don't match the enum at all --
--      log_request_created() used 'request_received' (enum has
--      'request_created') and admin_set_doctor_status() used
--      'doctor_status_approved'/'doctor_status_rejected'/'doctor_status_suspended'
--      (enum has 'doctor_approved'/'doctor_rejected'/'doctor_suspended').
--      Both are fixed here as part of the same migration -- converting the
--      column without fixing them would have broken every doctor-application
--      and every new-request notification immediately.
--
-- RLS/grants note: requests, request_status_history and notifications
-- already have RLS enabled with SELECT-only policies, and
-- anon/authenticated already hold no insert/update/delete grants at all on
-- any of them -- already exactly "all writes go through RPC functions only."
-- audit_log is genuinely new: RLS is enabled with zero policies (per this
-- prompt's explicit "no policies yet"), and only SELECT is granted to
-- authenticated -- with zero policies, nobody (not even admin) can read a
-- row yet, which satisfies "not readable by anyone except admin" until a
-- future migration adds the is_admin()-gated policy.

begin;

-- ---------------------------------------------------------------------------
-- requests: new columns, generated requested_date, FK changes, relaxed
-- EXCLUDE, new partial unique index, new composite index, status-timestamp trigger
-- ---------------------------------------------------------------------------
alter table public.requests add column if not exists requested_date date
  generated always as ((scheduled_start at time zone 'Asia/Kolkata')::date) stored;

alter table public.requests add column if not exists mode public.service_mode not null default 'in_person';
alter table public.requests add column if not exists contact_phone text;
alter table public.requests add column if not exists address_text text;
alter table public.requests add column if not exists cancellation_reason text;
alter table public.requests add column if not exists rejection_reason text;
alter table public.requests add column if not exists accepted_at timestamptz;
alter table public.requests add column if not exists confirmed_at timestamptz;
alter table public.requests add column if not exists completed_at timestamptz;
alter table public.requests add column if not exists cancelled_at timestamptz;
alter table public.requests add column if not exists rejected_at timestamptz;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'requests_patient_notes_length') then
    alter table public.requests
      add constraint requests_patient_notes_length check (patient_notes is null or char_length(patient_notes) <= 1000);
  end if;
end $$;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'requests_home_visit_address') then
    alter table public.requests
      add constraint requests_home_visit_address check (mode <> 'home_visit' or address_text is not null);
  end if;
end $$;

-- FK retargeting + ON DELETE changes (see header comment for why).
alter table public.requests drop constraint if exists requests_patient_id_fkey;
alter table public.requests
  add constraint requests_patient_id_fkey foreign key (patient_id) references public.profiles (id) on delete restrict;

alter table public.requests drop constraint if exists requests_doctor_id_fkey;
alter table public.requests
  add constraint requests_doctor_id_fkey foreign key (doctor_id) references public.doctor_profiles (user_id) on delete restrict;

alter table public.requests drop constraint if exists requests_service_id_fkey;
alter table public.requests
  add constraint requests_service_id_fkey foreign key (service_id) references public.services (id) on delete restrict;

-- Relax the EXCLUDE constraint to accepted/confirmed only (see header comment).
alter table public.requests drop constraint if exists requests_no_doctor_overlap;
alter table public.requests
  add constraint requests_no_doctor_overlap
  exclude using gist (
    doctor_id with =,
    tstzrange(scheduled_start, scheduled_end, '[)') with &&
  )
  where (doctor_id is not null and status in ('accepted', 'confirmed'));

-- Partial unique index: a patient cannot have two active requests for the
-- same doctor at the same start time (narrower than the EXCLUDE above, which
-- no longer covers 'requested').
create unique index if not exists requests_patient_doctor_start_unique
  on public.requests (patient_id, doctor_id, scheduled_start)
  where doctor_id is not null and status in ('requested', 'accepted', 'confirmed');

create index if not exists requests_doctor_status_start_idx
  on public.requests (doctor_id, status, scheduled_start);

-- Auto-populate the per-transition timestamp columns. Separate from the
-- existing log_request_status_change()/log_request_created() triggers
-- (left untouched) to avoid touching already-applied, tested trigger code.
create or replace function public.requests_set_status_timestamps()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if new.status = 'accepted' and old.status is distinct from 'accepted' then
    new.accepted_at := now();
  elsif new.status = 'confirmed' and old.status is distinct from 'confirmed' then
    new.confirmed_at := now();
  elsif new.status = 'completed' and old.status is distinct from 'completed' then
    new.completed_at := now();
  elsif new.status = 'cancelled' and old.status is distinct from 'cancelled' then
    new.cancelled_at := now();
  elsif new.status = 'rejected' and old.status is distinct from 'rejected' then
    new.rejected_at := now();
  end if;
  return new;
end;
$$;

drop trigger if exists requests_set_status_timestamps on public.requests;
create trigger requests_set_status_timestamps
  before update of status on public.requests
  for each row execute function public.requests_set_status_timestamps();

revoke all on function public.requests_set_status_timestamps() from public;

-- ---------------------------------------------------------------------------
-- request_status_history: append-only (reject UPDATE/DELETE)
-- ---------------------------------------------------------------------------
create or replace function public.reject_update_or_delete()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  raise exception 'This table is append-only: % is not permitted.', tg_op using errcode = '0A000';
end;
$$;

revoke all on function public.reject_update_or_delete() from public;

drop trigger if exists request_status_history_append_only on public.request_status_history;
create trigger request_status_history_append_only
  before update or delete on public.request_status_history
  for each row execute function public.reject_update_or_delete();

-- ---------------------------------------------------------------------------
-- notifications: is_read (generated), type -> enum (+ trigger value fixes),
-- FK change, new index
-- ---------------------------------------------------------------------------
alter table public.notifications add column if not exists is_read boolean
  generated always as (read_at is not null) stored;

alter table public.notifications alter column type type public.notification_type using type::public.notification_type;

alter table public.notifications drop constraint if exists notifications_related_request_id_fkey;
alter table public.notifications
  add constraint notifications_related_request_id_fkey
  foreign key (related_request_id) references public.requests (id) on delete set null;

create index if not exists notifications_user_unread_sorted_idx
  on public.notifications (user_id, is_read, created_at desc);

-- Fix: align emitted notification type values with the enum (see header comment).
create or replace function public.log_request_created()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_service_name text;
begin
  select name into v_service_name from public.services where id = new.service_id;

  insert into public.request_status_history (request_id, old_status, new_status, changed_by, note)
  values (new.id, null, new.status, auth.uid(), null);

  if new.doctor_id is not null then
    insert into public.notifications (user_id, type, title, body, related_request_id)
    values (new.doctor_id, 'request_created', 'New service request',
      format('You have a new request for %s.', coalesce(v_service_name, 'a service')), new.id);
  end if;

  return new;
end;
$$;

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
begin
  if not public.is_admin() then
    raise exception 'You are not authorized to perform this action.' using errcode = 'P0001';
  end if;

  select * into v_current from public.doctor_profiles where user_id = p_doctor_id;
  if v_current.user_id is null then
    raise exception 'Doctor application not found.' using errcode = 'P0001';
  end if;

  if (v_current.status, p_new_status) not in (
    ('pending', 'approved'),
    ('pending', 'rejected'),
    ('approved', 'suspended'),
    ('suspended', 'approved'),
    ('rejected', 'approved')
  ) then
    raise exception 'That status change is not allowed from the current state.' using errcode = 'P0001';
  end if;

  if p_new_status = 'rejected' and coalesce(trim(p_reason), '') = '' then
    raise exception 'A reason is required when rejecting a doctor application.' using errcode = 'P0001';
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

  v_notification_type := case p_new_status
    when 'approved' then 'doctor_approved'
    when 'rejected' then 'doctor_rejected'
    when 'suspended' then 'doctor_suspended'
    else 'doctor_approved' -- reactivation (suspended -> approved) also lands here
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

  return v_result;
end;
$$;

-- ---------------------------------------------------------------------------
-- audit_log: brand new. Append-only, RLS enabled with no policies yet.
-- ---------------------------------------------------------------------------
create table if not exists public.audit_log (
  id bigint generated always as identity primary key,
  actor_id uuid,
  action text not null,
  entity_type text not null,
  entity_id uuid,
  old_data jsonb,
  new_data jsonb,
  created_at timestamptz not null default now()
);

comment on table public.audit_log is
  'Append-only audit trail. No FK on actor_id/entity_id by design (entity_type varies; actor may be absent for system actions). Not readable by anyone except admin -- RLS is enabled with zero policies for now; an is_admin()-gated SELECT policy is future work.';

alter table public.audit_log enable row level security;

drop trigger if exists audit_log_append_only on public.audit_log;
create trigger audit_log_append_only
  before update or delete on public.audit_log
  for each row execute function public.reject_update_or_delete();

revoke all on public.audit_log from anon, authenticated;
grant select on public.audit_log to authenticated;

commit;
