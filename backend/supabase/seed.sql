-- Sample catalog data for local development and testing.
-- Safe to re-run: uses ON CONFLICT on the unique category name.

insert into public.service_categories (name, description, icon, sort_order)
values
  ('General Consultation', 'Talk to a doctor about general health concerns.', 'stethoscope', 1),
  ('Dermatology', 'Skin, hair and nail care consultations.', 'skin', 2),
  ('Lab Tests', 'Book a diagnostic lab test, no doctor required.', 'flask', 3)
on conflict (name) do nothing;

insert into public.services (category_id, name, description, requires_doctor, duration_minutes, sort_order)
select c.id, v.name, v.description, v.requires_doctor, v.duration_minutes, v.sort_order
from (
  values
    ('General Consultation', 'General Physician Consultation', 'A 20-minute consultation with a general physician.', true, 20, 1),
    ('Dermatology', 'Skin Consultation', 'A 30-minute consultation with a dermatologist.', true, 30, 1),
    ('Lab Tests', 'Complete Blood Count (CBC)', 'Walk-in blood test, results via the lab.', false, 15, 1)
) as v (category_name, name, description, requires_doctor, duration_minutes, sort_order)
join public.service_categories c on c.name = v.category_name
where not exists (
  select 1 from public.services s where s.category_id = c.id and s.name = v.name
);
