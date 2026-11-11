-- Found via the security advisor right after creating prevent_role_escalation()
-- in 20261112000002: it was callable directly via RPC by both anon and
-- authenticated (/rest/v1/rpc/prevent_role_escalation), even though it's a
-- trigger-only function that must never be invoked directly.
--
-- Root cause: 20261111120009_fix_function_grants.sql only revoked the
-- default EXECUTE grant from `anon, authenticated` --
--   alter default privileges in schema public revoke execute on functions from anon, authenticated;
-- -- but Postgres's real default (every CREATE FUNCTION implicitly grants
-- EXECUTE to the PUBLIC pseudo-role unless told otherwise) was never
-- addressed. That's a strictly broader gap than the one function it just
-- bit: EVERY function created after 20261111120009 has been silently
-- getting an implicit PUBLIC grant, independent of anything granted to
-- anon/authenticated specifically. Confirmed via pg_proc.proacl -- compare
-- handle_new_user/set_updated_at (created before 20261111120009, manually
-- revoked from public there) against prevent_role_escalation, is_doctor and
-- current_app_role (all created after, none manually revoked from public --
-- the latter two happen to be fine only because they were deliberately
-- granted to authenticated anyway).

begin;

revoke execute on function public.prevent_role_escalation() from public;

-- Close the gap for every function created from here on, trigger-only or not.
alter default privileges in schema public revoke execute on functions from public;

commit;
