-- Rate limiting for create_service_request, upgraded in place (same
-- name/signature -- no app change). Two simple, counting-based checks,
-- not a token-bucket/sliding-window table: a patient may not have more
-- than 10 active (requested/accepted/confirmed) requests at once, and may
-- not create more than 5 requests in the trailing 60 minutes. Both checks
-- run early (right after the suspension check), before any service/
-- doctor lookups, so a rate-limited caller gets a cheap rejection.
--
-- DESIGN NOTE (why counting queries, not a dedicated rate-limit table):
-- both limits are expressed directly against requests.patient_id, which
-- is already indexed (requests_patient_idx on (patient_id, created_at
-- desc)), so each check is a short index range scan, not a sequential
-- scan or a second table to keep consistent. A dedicated token-bucket
-- table would add write amplification (one extra row per request) and a
-- cleanup job for no real benefit at this scale; if this backend ever
-- needs per-IP or per-unauthenticated-endpoint limiting (this project has
-- neither -- every mutating RPC requires auth.uid()), that's a genuinely
-- different problem (no stable patient_id to key on) and would need an
-- edge-level solution (e.g. Supabase's own API rate limiting, or a
-- Redis-backed counter in an Edge Function in front of the RPC), not a
-- change to this function.

begin;

create or replace function public.create_service_request(
  p_service_id uuid,
  p_doctor_id uuid,
  p_slot_start timestamptz,
  p_mode public.service_mode,
  p_notes text,
  p_contact_phone text,
  p_address text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_caller_id uuid;
  v_caller_role public.user_role;
  v_caller_suspended boolean;
  v_active_count integer;
  v_recent_count integer;
  v_service public.services;
  v_category_active boolean;
  v_doctor public.doctor_profiles;
  v_doctor_suspended boolean;
  v_slot_end timestamptz;
  v_notes text;
  v_address text;
  v_phone text;
  v_request_id uuid;
  v_service_name text;
  v_slot_matches boolean;
  v_max_horizon_days integer;
  v_min_lead_minutes integer;
begin
  v_caller_id := auth.uid();
  if v_caller_id is null then
    raise exception 'AS000_NOT_AUTHENTICATED: You must be signed in to submit a request.' using errcode = 'P0001';
  end if;

  select role, is_suspended into v_caller_role, v_caller_suspended
  from public.profiles where id = v_caller_id;

  if v_caller_role is distinct from 'user' then
    raise exception 'AS004_ROLE_NOT_PATIENT: Only patient accounts can submit service requests.' using errcode = 'P0001';
  end if;

  if v_caller_suspended then
    raise exception 'AS005_ACCOUNT_SUSPENDED: Your account is suspended and cannot submit requests.' using errcode = 'P0001';
  end if;

  select count(*) into v_active_count
  from public.requests
  where patient_id = v_caller_id and status in ('requested', 'accepted', 'confirmed');
  if v_active_count >= 10 then
    raise exception 'AS034_TOO_MANY_ACTIVE_REQUESTS: You have too many active requests. Please wait for one to be resolved before creating another.'
      using errcode = 'P0001';
  end if;

  select count(*) into v_recent_count
  from public.requests
  where patient_id = v_caller_id and created_at > now() - interval '1 hour';
  if v_recent_count >= 5 then
    raise exception 'AS035_RATE_LIMITED: You have created too many requests recently. Please try again later.'
      using errcode = 'P0001';
  end if;

  select * into v_service from public.services where id = p_service_id;
  if v_service.id is null or not v_service.is_active then
    raise exception 'AS006_SERVICE_UNAVAILABLE: The selected service is not available.' using errcode = 'P0001';
  end if;

  select is_active into v_category_active
  from public.service_categories where id = v_service.category_id;
  if v_category_active is distinct from true then
    raise exception 'AS006_SERVICE_UNAVAILABLE: The selected service is not available.' using errcode = 'P0001';
  end if;

  v_notes := nullif(btrim(p_notes), '');
  v_address := nullif(btrim(p_address), '');
  v_phone := nullif(btrim(p_contact_phone), '');

  if v_notes is not null and char_length(v_notes) > 1000 then
    raise exception 'AS013_TEXT_TOO_LONG: Notes must be 1000 characters or fewer.' using errcode = 'P0001';
  end if;
  if v_address is not null and char_length(v_address) > 500 then
    raise exception 'AS013_TEXT_TOO_LONG: Address must be 500 characters or fewer.' using errcode = 'P0001';
  end if;
  if v_phone is not null and v_phone !~ '^\+?[0-9 ()-]{7,20}$' then
    raise exception 'AS012_INVALID_CONTACT_PHONE: Please provide a valid contact phone number.' using errcode = 'P0001';
  end if;

  if p_mode = 'home_visit' and v_address is null then
    raise exception 'AS011_ADDRESS_REQUIRED: An address is required for home visits.' using errcode = 'P0001';
  end if;

  select max_horizon_days, min_lead_minutes into v_max_horizon_days, v_min_lead_minutes
  from public.booking_settings();

  if v_service.requires_doctor then
    if p_doctor_id is null then
      raise exception 'AS007_DOCTOR_REQUIRED: Please choose a doctor for this service.' using errcode = 'P0001';
    end if;

    if v_phone is null then
      raise exception 'AS012_INVALID_CONTACT_PHONE: A contact phone number is required so the doctor can reach you.'
        using errcode = 'P0001';
    end if;

    select * into v_doctor from public.doctor_profiles where user_id = p_doctor_id;
    select is_suspended into v_doctor_suspended from public.profiles where id = p_doctor_id;

    if v_doctor.user_id is null
       or v_doctor.status <> 'approved'
       or not v_doctor.accepting_requests
       or coalesce(v_doctor_suspended, true)
    then
      raise exception 'AS002_DOCTOR_NOT_AVAILABLE: This doctor is not currently accepting requests.' using errcode = 'P0001';
    end if;

    if not exists (
      select 1 from public.doctor_services where doctor_id = p_doctor_id and service_id = p_service_id
    ) then
      raise exception 'AS008_DOCTOR_NOT_OFFERED: This doctor does not offer the selected service.' using errcode = 'P0001';
    end if;

    if not (p_mode = any(v_service.supported_modes)) or not (p_mode = any(v_doctor.consultation_modes)) then
      raise exception 'AS009_MODE_UNSUPPORTED: The selected consultation mode is not supported.' using errcode = 'P0001';
    end if;

    if p_slot_start is null then
      raise exception 'AS001_SLOT_UNAVAILABLE: Please choose a time slot.' using errcode = 'P0001';
    end if;

    select exists (
      select 1 from public.get_doctor_open_slots(p_doctor_id, (p_slot_start at time zone 'Asia/Kolkata')::date) s
      where s.slot_start = p_slot_start
    ) into v_slot_matches;

    if not v_slot_matches then
      raise exception 'AS001_SLOT_UNAVAILABLE: That slot is no longer available. Please choose another.' using errcode = 'P0001';
    end if;

    if exists (
      select 1 from public.requests
      where patient_id = v_caller_id
        and doctor_id = p_doctor_id
        and scheduled_start = p_slot_start
        and status in ('requested', 'accepted', 'confirmed')
    ) then
      raise exception 'AS010_DUPLICATE_REQUEST: You already have an active request with this doctor at that time.'
        using errcode = 'P0001';
    end if;

    v_slot_end := p_slot_start + make_interval(mins => v_service.duration_minutes);
  else
    if p_doctor_id is not null then
      raise exception 'AS014_DOCTOR_NOT_ALLOWED: This service does not require selecting a doctor.' using errcode = 'P0001';
    end if;

    if p_slot_start is null then
      raise exception 'AS001_SLOT_UNAVAILABLE: Please choose a time.' using errcode = 'P0001';
    end if;

    if p_slot_start < now() + make_interval(mins => v_min_lead_minutes) then
      raise exception 'AS003_INVALID_DATE: Please choose a time at least % minutes from now.', v_min_lead_minutes
        using errcode = 'P0001';
    end if;

    if (p_slot_start at time zone 'Asia/Kolkata')::date > (now() at time zone 'Asia/Kolkata')::date + v_max_horizon_days then
      raise exception 'AS003_INVALID_DATE: Please choose a time within the next % days.', v_max_horizon_days
        using errcode = 'P0001';
    end if;

    if not (p_mode = any(v_service.supported_modes)) then
      raise exception 'AS009_MODE_UNSUPPORTED: The selected consultation mode is not supported for this service.'
        using errcode = 'P0001';
    end if;

    v_slot_end := p_slot_start + make_interval(mins => v_service.duration_minutes);
  end if;

  begin
    insert into public.requests (
      patient_id, doctor_id, service_id, scheduled_start, scheduled_end,
      mode, status, patient_notes, contact_phone, address_text
    ) values (
      v_caller_id, p_doctor_id, p_service_id, p_slot_start, v_slot_end,
      p_mode, 'requested', v_notes, v_phone, v_address
    )
    returning id into v_request_id;
  exception
    when exclusion_violation then
      raise exception 'AS001_SLOT_UNAVAILABLE: That slot was just booked by someone else. Please choose another.'
        using errcode = 'P0001';
    when unique_violation then
      raise exception 'AS010_DUPLICATE_REQUEST: You already have an active request with this doctor at that time.'
        using errcode = 'P0001';
  end;

  select name into v_service_name from public.services where id = p_service_id;

  insert into public.notifications (user_id, type, title, body, related_request_id)
  values (
    v_caller_id,
    'request_created',
    'Request submitted',
    format('Your request for %s has been submitted.', coalesce(v_service_name, 'a service')),
    v_request_id
  );

  return v_request_id;
end;
$$;

commit;
