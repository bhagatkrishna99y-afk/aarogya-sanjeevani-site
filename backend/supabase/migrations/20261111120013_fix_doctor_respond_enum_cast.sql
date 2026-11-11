-- Fixes a type error found during QA: a CASE expression over two string
-- literals resolves to `text` (not the column's enum type the way a bare
-- literal assignment would), so Postgres rejected the UPDATE with
-- "column \"status\" is of type public.request_status but expression is of
-- type text". Cast the CASE result explicitly.
create or replace function public.doctor_respond_request(
  p_request_id uuid,
  p_accept boolean,
  p_reason text default null
)
returns public.requests
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_request public.requests;
  v_result public.requests;
begin
  select * into v_request from public.requests where id = p_request_id;
  if v_request.id is null then
    raise exception 'Request not found.' using errcode = 'P0001';
  end if;
  if v_request.doctor_id is distinct from auth.uid() then
    raise exception 'You are not authorized to respond to this request.' using errcode = 'P0001';
  end if;
  if v_request.status <> 'requested' then
    raise exception 'This request has already been responded to.' using errcode = 'P0001';
  end if;
  if not p_accept and coalesce(trim(p_reason), '') = '' then
    raise exception 'A reason is required when declining a request.' using errcode = 'P0001';
  end if;

  update public.requests
  set status = (case when p_accept then 'accepted' else 'rejected' end)::public.request_status,
      doctor_response_note = p_reason
  where id = p_request_id
  returning * into v_result;

  return v_result;
end;
$$;
