-- service_categories_select, services_select and doctor_services_select are
-- readable by anon and reference public.is_admin() in their USING clause.
-- Postgres checks EXECUTE privilege on every function referenced in a policy
-- qual up front, regardless of whether OR later short-circuits around it at
-- runtime. Without this grant, anonymous reads of the public catalog fail
-- with "permission denied for function is_admin" -- found while smoke
-- testing the website's public Services page against the live project.
grant execute on function public.is_admin() to anon;
