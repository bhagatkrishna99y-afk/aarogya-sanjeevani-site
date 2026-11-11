-- Helper predicates used throughout RLS policies and RPCs.
-- SECURITY DEFINER + empty search_path: the function body can only resolve
-- names we schema-qualify ourselves, so it cannot be hijacked by a caller
-- who has created same-named objects earlier in their search_path.

create function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.profiles where id = auth.uid() and role = 'admin'
  );
$$;

create function public.is_approved_doctor(p_doctor_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.doctor_profiles
    where user_id = p_doctor_id and status = 'approved'
  );
$$;

revoke all on function public.is_admin() from public;
revoke all on function public.is_approved_doctor(uuid) from public;
grant execute on function public.is_admin() to authenticated;
grant execute on function public.is_approved_doctor(uuid) to authenticated, anon;

-- Generic updated_at maintenance.
create function public.set_updated_at()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();

create trigger doctor_profiles_set_updated_at
  before update on public.doctor_profiles
  for each row execute function public.set_updated_at();

create trigger requests_set_updated_at
  before update on public.requests
  for each row execute function public.set_updated_at();

-- Creates the profiles row the moment a user signs up via Supabase Auth.
-- role is always 'user' here -- this is the only INSERT path into profiles,
-- so a client can never create itself as a doctor or admin.
create function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, full_name, phone)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1)),
    new.raw_user_meta_data ->> 'phone'
  );
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Defense in depth: even though only RPCs (running as the function owner)
-- can write to requests.status, this trigger rejects any update that is not
-- a legal transition, so a bug in a future RPC cannot corrupt history.
create function public.validate_request_status_transition()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if new.status = old.status then
    return new;
  end if;

  if (old.status, new.status) not in (
    ('requested', 'accepted'),
    ('requested', 'rejected'),
    ('requested', 'cancelled'),
    ('accepted', 'confirmed'),
    ('accepted', 'cancelled'),
    ('confirmed', 'completed'),
    ('confirmed', 'cancelled')
  ) then
    raise exception 'Invalid request status transition: % -> %', old.status, new.status
      using errcode = '22023';
  end if;

  return new;
end;
$$;

create trigger requests_validate_status_transition
  before update of status on public.requests
  for each row execute function public.validate_request_status_transition();

-- Automatically logs every status change and notifies the relevant parties,
-- so RPCs only ever need to update requests.status.
create function public.log_request_status_change()
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
  values (new.id, old.status, new.status, auth.uid(), new.doctor_response_note);

  if new.status = 'accepted' then
    insert into public.notifications (user_id, type, title, body, related_request_id)
    values (new.patient_id, 'request_accepted', 'Request accepted',
      format('Your request for %s was accepted by the doctor.', coalesce(v_service_name, 'your service')), new.id);
  elsif new.status = 'rejected' then
    insert into public.notifications (user_id, type, title, body, related_request_id)
    values (new.patient_id, 'request_rejected', 'Request rejected',
      format('Your request for %s was declined.', coalesce(v_service_name, 'your service')), new.id);
  elsif new.status = 'confirmed' then
    insert into public.notifications (user_id, type, title, body, related_request_id)
    values (new.patient_id, 'request_confirmed', 'Request confirmed',
      format('Your request for %s was confirmed.', coalesce(v_service_name, 'your service')), new.id);
  elsif new.status = 'completed' then
    insert into public.notifications (user_id, type, title, body, related_request_id)
    values (new.patient_id, 'request_completed', 'Visit completed',
      format('Your %s visit is marked completed.', coalesce(v_service_name, 'service')), new.id);
  elsif new.status = 'cancelled' then
    if auth.uid() = new.patient_id and new.doctor_id is not null then
      insert into public.notifications (user_id, type, title, body, related_request_id)
      values (new.doctor_id, 'request_cancelled', 'Request cancelled',
        format('A patient cancelled their %s request.', coalesce(v_service_name, 'service')), new.id);
    elsif new.doctor_id is not null and auth.uid() = new.doctor_id then
      insert into public.notifications (user_id, type, title, body, related_request_id)
      values (new.patient_id, 'request_cancelled', 'Request cancelled',
        format('Your %s request was cancelled by the doctor.', coalesce(v_service_name, 'service')), new.id);
    end if;
  end if;

  return new;
end;
$$;

create trigger requests_log_status_change
  after update of status on public.requests
  for each row execute function public.log_request_status_change();

-- New request: log the initial history row and notify the doctor (if one is assigned).
create function public.log_request_created()
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
    values (new.doctor_id, 'request_received', 'New service request',
      format('You have a new request for %s.', coalesce(v_service_name, 'a service')), new.id);
  end if;

  return new;
end;
$$;

create trigger requests_log_created
  after insert on public.requests
  for each row execute function public.log_request_created();
