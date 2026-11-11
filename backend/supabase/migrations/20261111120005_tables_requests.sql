-- Requests are the central booking entity. All status transitions happen
-- only through RPCs in 008_rpc_functions.sql -- direct client INSERT/UPDATE
-- grants are revoked in 007_rls.sql.

create table public.requests (
  id uuid primary key default gen_random_uuid(),
  patient_id uuid not null references public.profiles (id) on delete cascade,
  doctor_id uuid references public.profiles (id),
  service_id uuid not null references public.services (id),
  scheduled_start timestamptz not null,
  scheduled_end timestamptz not null,
  status public.request_status not null default 'requested',
  patient_notes text,
  doctor_response_note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint requests_time_order check (scheduled_end > scheduled_start)
);

create index requests_patient_idx on public.requests (patient_id, created_at desc);
create index requests_doctor_idx on public.requests (doctor_id, created_at desc);
create index requests_status_idx on public.requests (status);

-- Database-level double-booking prevention: no two "live" requests for the
-- same doctor may occupy overlapping time ranges. This holds regardless of
-- what any client believes is available, closing the race-condition window
-- that purely client-side slot checks cannot close.
alter table public.requests
  add constraint requests_no_doctor_overlap
  exclude using gist (
    doctor_id with =,
    tstzrange(scheduled_start, scheduled_end, '[)') with &&
  )
  where (doctor_id is not null and status in ('requested', 'accepted', 'confirmed'));

-- Full audit trail of every status transition, for both compliance and the
-- admin "monitor requests and status history" journey.
create table public.request_status_history (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.requests (id) on delete cascade,
  old_status public.request_status,
  new_status public.request_status not null,
  changed_by uuid references public.profiles (id),
  note text,
  changed_at timestamptz not null default now()
);

create index request_status_history_request_idx on public.request_status_history (request_id, changed_at);

-- In-app notifications only (no push/email/SMS in this release -- see product rules).
create table public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id) on delete cascade,
  type text not null,
  title text not null,
  body text not null,
  related_request_id uuid references public.requests (id) on delete cascade,
  read_at timestamptz,
  created_at timestamptz not null default now()
);

create index notifications_user_idx on public.notifications (user_id, created_at desc);
create index notifications_unread_idx on public.notifications (user_id) where read_at is null;
