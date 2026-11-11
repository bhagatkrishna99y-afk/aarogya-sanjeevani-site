-- pgTAP smoke test: the three extensions every later migration depends on
-- must be present. Run via `supabase test db` (see backend/README.md).
begin;

select plan(3);

select has_extension('pgcrypto', 'pgcrypto is installed (gen_random_uuid() for primary keys)');
select has_extension('btree_gist', 'btree_gist is installed (needed for EXCLUDE constraints, e.g. double-booking prevention)');
select has_extension('citext', 'citext is installed (case-insensitive email comparisons)');

select * from finish();

rollback;
