-- The project's schema-level default privileges grant EXECUTE on every new
-- function to anon/authenticated automatically. Our per-function "grant to
-- authenticated" calls in 008 therefore left anon with EXECUTE too (visible
-- in pg_proc.proacl), even though each function checks auth.uid()/is_admin()
-- internally. This migration removes that implicit access and makes the
-- intended access list explicit, function by function. It also changes the
-- default for future functions so this cannot silently recur.

revoke execute on all functions in schema public from anon, authenticated;

-- Trigger-only functions: never called directly via RPC by anyone.
revoke execute on function public.handle_new_user() from public;
revoke execute on function public.set_updated_at() from public;
revoke execute on function public.validate_request_status_transition() from public;
revoke execute on function public.log_request_status_change() from public;
revoke execute on function public.log_request_created() from public;

-- Helper predicates.
grant execute on function public.is_admin() to authenticated;
grant execute on function public.is_approved_doctor(uuid) to anon, authenticated;

-- Public read-only directory / discovery RPCs.
grant execute on function public.list_approved_doctors(uuid) to anon, authenticated;
grant execute on function public.get_doctor_public_profile(uuid) to anon, authenticated;
grant execute on function public.get_available_slots(uuid, uuid, date) to authenticated;

-- Authenticated-only business RPCs.
grant execute on function public.submit_doctor_application(text, text, text, text, text, integer) to authenticated;
grant execute on function public.admin_set_doctor_status(uuid, public.doctor_status, text) to authenticated;
grant execute on function public.admin_assign_admin_role(uuid) to authenticated;
grant execute on function public.set_doctor_services(uuid[]) to authenticated;
grant execute on function public.set_weekly_availability(jsonb) to authenticated;
grant execute on function public.create_request(uuid, uuid, timestamptz, text) to authenticated;
grant execute on function public.doctor_respond_request(uuid, boolean, text) to authenticated;
grant execute on function public.doctor_confirm_request(uuid) to authenticated;
grant execute on function public.doctor_complete_request(uuid) to authenticated;
grant execute on function public.cancel_request(uuid, text) to authenticated;
grant execute on function public.mark_notification_read(uuid) to authenticated;

-- Prevent this from silently recurring: future functions in public must be
-- granted EXECUTE explicitly rather than inheriting it automatically.
alter default privileges in schema public revoke execute on functions from anon, authenticated;

-- Move btree_gist out of the public schema (flagged by the linter as
-- extension_in_public); it is only used internally by the exclusion
-- constraint and does not need to be public-schema-resident.
create schema if not exists extensions;
alter extension btree_gist set schema extensions;
