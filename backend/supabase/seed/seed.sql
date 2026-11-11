-- ============================================================================
-- DEVELOPMENT-ONLY seed data. Never run this against a production project.
-- Test account credentials are documented in supabase/seed/README.md, not
-- here, and are clearly marked development-only there.
--
-- Idempotent: every insert below is safe to re-run (ON CONFLICT guards,
-- or a lookup-first helper function for auth users). Supersedes the
-- earlier 001_catalog.sql, which covered 3 of what are now 5 categories
-- with 1 service each -- consolidated here into the full catalog this
-- project's prompt specifies, so there's exactly one seed file to reason
-- about. Runs after migrations during `supabase db reset`
-- (supabase/config.toml's [db.seed] picks up every supabase/seed/*.sql).
-- ============================================================================

begin;

-- ----------------------------------------------------------------------------
-- Helper: idempotent test-user creation via direct auth.users/auth.identities
-- rows (the "CLI seed" approach -- supabase db reset has no running GoTrue
-- admin API to call, so seed scripts create confirmed, password-login-ready
-- users by inserting the same rows GoTrue itself would write). Dropped at
-- the end of this file -- it's seed-only plumbing, not a permanent schema
-- object.
-- ----------------------------------------------------------------------------
create or replace function public.__seed_get_or_create_user(
  p_email text,
  p_full_name text,
  p_phone text,
  p_signup_role text,
  p_password text
) returns uuid
language plpgsql
as $$
declare
  v_user_id uuid;
begin
  select id into v_user_id from auth.users where email = p_email;
  if v_user_id is not null then
    return v_user_id;
  end if;

  v_user_id := gen_random_uuid();

  insert into auth.users (
    instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
    confirmation_token, email_change, email_change_token_new, recovery_token
  ) values (
    '00000000-0000-0000-0000-000000000000', v_user_id, 'authenticated', 'authenticated',
    p_email, crypt(p_password, gen_salt('bf')), now(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    jsonb_build_object('full_name', p_full_name, 'phone', p_phone, 'signup_role', p_signup_role),
    now(), now(), '', '', '', ''
  );

  insert into auth.identities (
    id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at
  ) values (
    gen_random_uuid(), v_user_id,
    jsonb_build_object('sub', v_user_id::text, 'email', p_email),
    'email', v_user_id::text, now(), now(), now()
  )
  on conflict (provider_id, provider) do nothing;

  return v_user_id;
end;
$$;

-- ----------------------------------------------------------------------------
-- Service catalog: 5 categories, 3 services each.
-- ----------------------------------------------------------------------------
insert into public.service_categories (name, description, icon, sort_order, is_active)
values
  ('General Healthcare', 'Everyday consultations for common health concerns.', 'stethoscope', 1, true),
  ('Specialist Consultation', 'Consultations with doctors in a specific specialty.', 'user-md', 2, true),
  ('Diagnostic Services', 'Lab tests and imaging -- no doctor selection needed.', 'flask', 3, true),
  ('Home Healthcare', 'Doctor and care visits delivered at the patient''s home.', 'home', 4, true),
  ('Nursing Services', 'Nursing and therapy care delivered at the patient''s home.', 'heart-pulse', 5, true)
on conflict (name) do nothing;

insert into public.services (
  category_id, name, description, short_description, requires_doctor,
  duration_minutes, supported_modes, is_active, sort_order
)
select c.id, v.name, v.description, v.short_description, v.requires_doctor,
       v.duration_minutes, v.supported_modes, true, v.sort_order
from (
  values
    -- General Healthcare
    ('General Healthcare', 'General Physician Consultation',
     'Discuss common symptoms, get a diagnosis, and receive a treatment plan from a general physician.',
     'Talk to a GP about any health concern.', true, 20,
     array['in_person','online']::service_mode[], 1),
    ('General Healthcare', 'Pediatric Consultation',
     'A consultation focused on infant, child, and adolescent health.',
     'Child health checkups and concerns.', true, 20,
     array['in_person','online']::service_mode[], 2),
    ('General Healthcare', 'Vaccination Administration',
     'Routine vaccine administration by trained clinical staff, no doctor selection required.',
     'Walk-in vaccination.', false, 15,
     array['in_person']::service_mode[], 3),

    -- Specialist Consultation
    ('Specialist Consultation', 'Dermatology Consultation',
     'Skin, hair, and nail concerns reviewed by a dermatologist.',
     'See a skin specialist.', true, 30,
     array['in_person','online']::service_mode[], 1),
    ('Specialist Consultation', 'Cardiology Consultation',
     'Heart health review and consultation with a cardiologist.',
     'See a heart specialist.', true, 30,
     array['in_person','online']::service_mode[], 2),
    ('Specialist Consultation', 'Orthopedic Consultation',
     'Joint, bone, and muscle concerns reviewed by an orthopedic specialist.',
     'See a bone & joint specialist.', true, 30,
     array['in_person']::service_mode[], 3),

    -- Diagnostic Services (no doctor selection)
    ('Diagnostic Services', 'Diagnostic Sample Collection at Home',
     'A phlebotomist visits to collect blood or other samples for lab testing -- no doctor required.',
     'At-home sample pickup for lab tests.', false, 15,
     array['home_visit']::service_mode[], 1),
    ('Diagnostic Services', 'Complete Blood Count (CBC)',
     'A standard walk-in blood panel; results are shared once ready.',
     'Walk-in blood test.', false, 15,
     array['in_person']::service_mode[], 2),
    ('Diagnostic Services', 'X-Ray Imaging',
     'Walk-in diagnostic X-ray imaging.',
     'Walk-in X-ray.', false, 20,
     array['in_person']::service_mode[], 3),

    -- Home Healthcare
    ('Home Healthcare', 'Doctor Home Visit Consultation',
     'A doctor visits the patient at home for a full consultation.',
     'A doctor comes to you.', true, 45,
     array['home_visit']::service_mode[], 1),
    ('Home Healthcare', 'Post-Surgery Home Care',
     'Short-term recovery support and monitoring at home after a surgical procedure.',
     'Recovery support at home.', false, 60,
     array['home_visit']::service_mode[], 2),
    ('Home Healthcare', 'Elderly Care Attendant (Daily)',
     'A daily care attendant visit for elderly patients needing routine assistance.',
     'Daily elderly care visit.', false, 60,
     array['home_visit']::service_mode[], 3),

    -- Nursing Services
    ('Nursing Services', 'Wound Dressing at Home',
     'A trained nurse changes and dresses wounds at the patient''s home.',
     'At-home wound care.', false, 30,
     array['home_visit']::service_mode[], 1),
    ('Nursing Services', 'IV Therapy / Injection at Home',
     'Administration of prescribed injections or IV fluids at home by a nurse.',
     'At-home injections/IV.', false, 20,
     array['home_visit']::service_mode[], 2),
    ('Nursing Services', 'Physiotherapy Session at Home',
     'A physiotherapy session delivered at the patient''s home.',
     'At-home physiotherapy.', false, 45,
     array['home_visit']::service_mode[], 3)
) as v (category_name, name, description, short_description, requires_doctor, duration_minutes, supported_modes, sort_order)
join public.service_categories c on c.name = v.category_name
where not exists (
  select 1 from public.services s where s.category_id = c.id and s.name = v.name
);

-- ----------------------------------------------------------------------------
-- Test users: 2 patients, 4 doctors (2 approved w/ services+availability,
-- 1 pending, 1 suspended), 1 admin. Passwords/emails: see
-- supabase/seed/README.md.
-- ----------------------------------------------------------------------------
select public.__seed_get_or_create_user('patient1@aarogya.test', 'Asha Mehta', '+91 90000 00001', 'user', 'DevPass123!');
select public.__seed_get_or_create_user('patient2@aarogya.test', 'Rohan Gupta', '+91 90000 00002', 'user', 'DevPass123!');

select public.__seed_get_or_create_user('doctor.approved1@aarogya.test', 'Dr. Priya Sharma', '+91 90000 00011', 'doctor', 'DevPass123!');
select public.__seed_get_or_create_user('doctor.approved2@aarogya.test', 'Dr. Vikram Rao', '+91 90000 00012', 'doctor', 'DevPass123!');
select public.__seed_get_or_create_user('doctor.pending@aarogya.test', 'Dr. Neha Joshi', '+91 90000 00013', 'doctor', 'DevPass123!');
select public.__seed_get_or_create_user('doctor.suspended@aarogya.test', 'Dr. Arjun Nair', '+91 90000 00014', 'doctor', 'DevPass123!');

select public.__seed_get_or_create_user('admin@aarogya.test', 'Admin User', '+91 90000 00099', 'user', 'DevPass123!');
update public.profiles set role = 'admin'
where id = (select id from auth.users where email = 'admin@aarogya.test');

-- Doctor profiles.
insert into public.doctor_profiles (
  user_id, title, specialization, qualifications, bio, years_experience,
  license_number, consultation_modes, slot_duration_minutes, status, accepting_requests
)
select u.id, 'Dr.', 'General Medicine', 'MBBS, MD (General Medicine)',
       'General physician with a focus on preventive care.', 12,
       'SEED-LIC-APPROVED-1', array['in_person','online']::service_mode[], 20, 'approved', true
from auth.users u where u.email = 'doctor.approved1@aarogya.test'
on conflict (user_id) do nothing;

insert into public.doctor_profiles (
  user_id, title, specialization, qualifications, bio, years_experience,
  license_number, consultation_modes, slot_duration_minutes, status, accepting_requests
)
select u.id, 'Dr.', 'Dermatology', 'MBBS, MD (Dermatology)',
       'Dermatologist specializing in skin and hair concerns.', 8,
       'SEED-LIC-APPROVED-2', array['in_person','online']::service_mode[], 30, 'approved', true
from auth.users u where u.email = 'doctor.approved2@aarogya.test'
on conflict (user_id) do nothing;

insert into public.doctor_profiles (
  user_id, title, specialization, qualifications, bio, years_experience,
  license_number, consultation_modes, slot_duration_minutes, status, accepting_requests
)
select u.id, 'Dr.', 'Cardiology', 'MBBS, DM (Cardiology)',
       'Cardiologist, application under review.', 6,
       'SEED-LIC-PENDING-1', array['in_person']::service_mode[], 30, 'pending', true
from auth.users u where u.email = 'doctor.pending@aarogya.test'
on conflict (user_id) do nothing;

insert into public.doctor_profiles (
  user_id, title, specialization, qualifications, bio, years_experience,
  license_number, consultation_modes, slot_duration_minutes, status, accepting_requests
)
select u.id, 'Dr.', 'Orthopedics', 'MBBS, MS (Orthopedics)',
       'Orthopedic specialist. Account currently suspended for seed/testing purposes.', 15,
       'SEED-LIC-SUSPENDED-1', array['in_person']::service_mode[], 30, 'suspended', true
from auth.users u where u.email = 'doctor.suspended@aarogya.test'
on conflict (user_id) do nothing;

-- Services offered by each approved doctor.
insert into public.doctor_services (doctor_id, service_id)
select u.id, s.id
from auth.users u
join public.services s on s.name in ('General Physician Consultation', 'Vaccination Administration')
where u.email = 'doctor.approved1@aarogya.test'
on conflict (doctor_id, service_id) do nothing;

insert into public.doctor_services (doctor_id, service_id)
select u.id, s.id
from auth.users u
join public.services s on s.name = 'Dermatology Consultation'
where u.email = 'doctor.approved2@aarogya.test'
on conflict (doctor_id, service_id) do nothing;

-- Weekly availability for each approved doctor (Mon-Fri, 09:00-17:00 for
-- doctor 1; Mon/Wed/Fri 10:00-16:00 for doctor 2).
insert into public.doctor_weekly_availability (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes)
select u.id, d.day, '09:00'::time, '17:00'::time, 20
from auth.users u
cross join (values (1), (2), (3), (4), (5)) as d(day)
where u.email = 'doctor.approved1@aarogya.test'
  and not exists (
    select 1 from public.doctor_weekly_availability w where w.doctor_id = u.id and w.day_of_week = d.day
  );

insert into public.doctor_weekly_availability (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes)
select u.id, d.day, '10:00'::time, '16:00'::time, 30
from auth.users u
cross join (values (1), (3), (5)) as d(day)
where u.email = 'doctor.approved2@aarogya.test'
  and not exists (
    select 1 from public.doctor_weekly_availability w where w.doctor_id = u.id and w.day_of_week = d.day
  );

-- A handful of sample requests across different statuses, so the Patient/
-- Doctor/admin apps have something to show immediately after seeding.
insert into public.requests (
  patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status, contact_phone
)
select
  (select id from auth.users where email = 'patient1@aarogya.test'),
  (select id from auth.users where email = 'doctor.approved1@aarogya.test'),
  (select id from public.services where name = 'General Physician Consultation'),
  date_trunc('day', now()) + interval '1 day' + interval '10 hour',
  date_trunc('day', now()) + interval '1 day' + interval '10 hour 20 minute',
  'in_person', 'requested', '+91 90000 00001'
where not exists (
  select 1 from public.requests
  where patient_id = (select id from auth.users where email = 'patient1@aarogya.test')
    and doctor_id = (select id from auth.users where email = 'doctor.approved1@aarogya.test')
    and scheduled_start = date_trunc('day', now()) + interval '1 day' + interval '10 hour'
);

insert into public.requests (
  patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status, contact_phone
)
select
  (select id from auth.users where email = 'patient2@aarogya.test'),
  (select id from auth.users where email = 'doctor.approved2@aarogya.test'),
  (select id from public.services where name = 'Dermatology Consultation'),
  date_trunc('day', now()) + interval '2 day' + interval '11 hour',
  date_trunc('day', now()) + interval '2 day' + interval '11 hour 30 minute',
  'online', 'accepted', '+91 90000 00002'
where not exists (
  select 1 from public.requests
  where patient_id = (select id from auth.users where email = 'patient2@aarogya.test')
    and doctor_id = (select id from auth.users where email = 'doctor.approved2@aarogya.test')
    and scheduled_start = date_trunc('day', now()) + interval '2 day' + interval '11 hour'
);

insert into public.requests (
  patient_id, doctor_id, service_id, scheduled_start, scheduled_end, mode, status, contact_phone
)
select
  (select id from auth.users where email = 'patient1@aarogya.test'),
  null,
  (select id from public.services where name = 'Complete Blood Count (CBC)'),
  date_trunc('day', now()) - interval '3 day' + interval '9 hour',
  date_trunc('day', now()) - interval '3 day' + interval '9 hour 15 minute',
  'in_person', 'completed', '+91 90000 00001'
where not exists (
  select 1 from public.requests
  where patient_id = (select id from auth.users where email = 'patient1@aarogya.test')
    and doctor_id is null
    and service_id = (select id from public.services where name = 'Complete Blood Count (CBC)')
    and scheduled_start = date_trunc('day', now()) - interval '3 day' + interval '9 hour'
);

drop function public.__seed_get_or_create_user(text, text, text, text, text);

commit;
