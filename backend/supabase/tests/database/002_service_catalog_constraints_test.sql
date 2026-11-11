-- Proves the services table's new constraints actually reject bad data:
-- empty supported_modes, a duplicate slug, and an out-of-range duration.
begin;

select plan(3);

select set_config(
  'pgtap.test_category_id',
  (select id::text from public.service_categories limit 1),
  true
);

-- ---------------------------------------------------------------------------
-- 1. supported_modes must not be empty
-- ---------------------------------------------------------------------------
select throws_ok(
  format(
    $sql$insert into public.services (category_id, name, slug, supported_modes, duration_minutes)
         values (%L, 'PGTAP Empty Modes', 'pgtap-empty-modes', '{}', 30)$sql$,
    current_setting('pgtap.test_category_id')::uuid
  ),
  '23514'
);

-- ---------------------------------------------------------------------------
-- 2. slug must be unique
-- ---------------------------------------------------------------------------
insert into public.services (category_id, name, slug, duration_minutes)
values (current_setting('pgtap.test_category_id')::uuid, 'PGTAP Slug Original', 'pgtap-duplicate-slug', 30);

select throws_ok(
  format(
    $sql$insert into public.services (category_id, name, slug, duration_minutes)
         values (%L, 'PGTAP Slug Duplicate', 'pgtap-duplicate-slug', 30)$sql$,
    current_setting('pgtap.test_category_id')::uuid
  ),
  '23505'
);

-- ---------------------------------------------------------------------------
-- 3. duration_minutes must be between 10 and 240
-- ---------------------------------------------------------------------------
select throws_ok(
  format(
    $sql$insert into public.services (category_id, name, slug, duration_minutes)
         values (%L, 'PGTAP Bad Duration', 'pgtap-bad-duration', 5)$sql$,
    current_setting('pgtap.test_category_id')::uuid
  ),
  '23514'
);

select * from finish();

rollback;
