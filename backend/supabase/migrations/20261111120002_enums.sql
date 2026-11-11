-- Core enumerations. Roles and statuses are modeled as types, not free text,
-- so invalid values are rejected by Postgres rather than by application code.

create type public.user_role as enum ('user', 'doctor', 'admin');

create type public.doctor_status as enum ('pending', 'approved', 'rejected', 'suspended');

create type public.request_status as enum (
  'requested',
  'accepted',
  'confirmed',
  'completed',
  'rejected',
  'cancelled'
);
