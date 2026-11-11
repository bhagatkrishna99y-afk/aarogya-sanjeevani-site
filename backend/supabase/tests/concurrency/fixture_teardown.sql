-- Removes everything fixture_setup.sql created. Run after both pgbench
-- phases and the verification query in run_concurrency_test.sh.
--
-- request_status_history is append-only (see docs/data-model.md), so the
-- CASCADE delete from `requests` needs its trigger briefly disabled, the
-- same workaround used in docs/performance.md's synthetic-load cleanup.
do $$
declare
  v_doctor_id uuid;
begin
  select doctor_id into v_doctor_id from public._concurrency_fixture;
  if v_doctor_id is null then
    raise notice 'No concurrency fixture found -- nothing to tear down.';
    return;
  end if;

  alter table public.request_status_history disable trigger request_status_history_append_only;
  delete from public.notifications where related_request_id in (
    select id from public.requests where doctor_id = v_doctor_id
  ) or user_id = v_doctor_id or user_id = any (
    select patient_ids from public._concurrency_fixture
  );
  delete from public.requests where doctor_id = v_doctor_id;
  alter table public.request_status_history enable trigger request_status_history_append_only;

  delete from public.doctor_services where doctor_id = v_doctor_id;
  delete from public.doctor_weekly_availability where doctor_id = v_doctor_id;
  delete from public.doctor_profiles where user_id = v_doctor_id;
  delete from public.services where id = (select service_id from public._concurrency_fixture);
  delete from public.service_categories where id = (select category_id from public._concurrency_fixture);

  delete from auth.users where id = v_doctor_id or id = any (select patient_ids from public._concurrency_fixture);

  drop table public._concurrency_fixture;

  raise notice 'Concurrency fixture torn down.';
end;
$$;
