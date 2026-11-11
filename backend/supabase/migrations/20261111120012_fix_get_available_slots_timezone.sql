-- Fixes a type error found during QA: `date + time` produces a plain
-- timestamp, which doesn't match this function's timestamptz return type
-- and can't be compared against now() unambiguously. doctor_weekly_availability
-- has no timezone column, so wall-clock times are treated as UTC and
-- converted explicitly with `at time zone`.
create or replace function public.get_available_slots(
  p_doctor_id uuid,
  p_service_id uuid,
  p_date date
)
returns table (slot_start timestamptz, slot_end timestamptz)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_duration_minutes integer;
  v_day_of_week smallint;
begin
  if not public.is_approved_doctor(p_doctor_id) then
    raise exception 'This doctor is not currently accepting requests.' using errcode = 'P0001';
  end if;

  if not exists (select 1 from public.doctor_services where doctor_id = p_doctor_id and service_id = p_service_id) then
    raise exception 'This doctor does not offer the selected service.' using errcode = 'P0001';
  end if;

  select duration_minutes into v_duration_minutes from public.services where id = p_service_id and is_active;
  if v_duration_minutes is null then
    raise exception 'The selected service is not available.' using errcode = 'P0001';
  end if;

  if p_date < current_date then
    raise exception 'Please choose a date that is today or later.' using errcode = 'P0001';
  end if;

  v_day_of_week := extract(dow from p_date);

  return query
  with windows as (
    select start_time, end_time, slot_duration_minutes
    from public.doctor_weekly_availability
    where doctor_id = p_doctor_id and day_of_week = v_day_of_week and is_active
  ),
  candidates as (
    select
      w.end_time as window_end,
      (p_date + w.start_time) + (make_interval(mins => n * w.slot_duration_minutes)) as candidate_start_local
    from windows w,
      lateral generate_series(0, greatest(
        0,
        (extract(epoch from (w.end_time - w.start_time)) / 60 / w.slot_duration_minutes)::integer - 1
      )) as n
  )
  select
    (c.candidate_start_local at time zone 'UTC') as slot_start,
    (c.candidate_start_local at time zone 'UTC') + make_interval(mins => v_duration_minutes) as slot_end
  from candidates c
  where (c.candidate_start_local at time zone 'UTC') > now()
    and (c.candidate_start_local::time + make_interval(mins => v_duration_minutes)) <= c.window_end
    and not exists (
      select 1 from public.requests r
      where r.doctor_id = p_doctor_id
        and r.status in ('requested', 'accepted', 'confirmed')
        and tstzrange(r.scheduled_start, r.scheduled_end, '[)')
            && tstzrange(
                 (c.candidate_start_local at time zone 'UTC'),
                 (c.candidate_start_local at time zone 'UTC') + make_interval(mins => v_duration_minutes),
                 '[)'
               )
    )
  order by slot_start;
end;
$$;
