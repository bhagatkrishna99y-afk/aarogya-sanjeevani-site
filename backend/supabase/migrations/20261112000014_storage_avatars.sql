-- Private storage bucket for doctor profile photos.
--
-- DECISION (how "signed URLs from an RPC" actually works here): Postgres
-- has no built-in primitive for producing a Storage-service-signed URL
-- token -- that's an HTTP-API-level operation performed by the separate
-- Storage microservice (`POST /storage/v1/object/sign/{bucket}/{path}`),
-- not something a plpgsql function can fabricate without calling out to
-- that service with a stored service-role secret (a materially riskier
-- design -- vault-stored secret + network extension -- for no real
-- benefit here). The actual signed URL is created client-side by the
-- normal Storage SDK call `createSignedUrl(path, expiresIn)`; what makes
-- that call succeed for a *public, approved* doctor's photo while the
-- bucket stays private is the RLS policy below
-- (avatars_select_approved_doctor), which is the real equivalent of "an
-- RPC that exposes signed URLs" in a pure-Postgres architecture. The
-- "which path do I sign" question is already answered by the existing
-- doctors_public view (20261112000005), which already exposes
-- `avatar_url` filtered to approved, non-suspended doctors only -- so no
-- new read RPC was needed for that half either.
--
-- Path convention: every object's key starts with the owning user's id as
-- the first path segment (e.g. `<user_id>/avatar.jpg`), matching the
-- prompt's "path prefix = their user id" requirement and what
-- storage.foldername() expects to compare against.
--
-- Size/mime-type limits are enforced by the Storage service itself from
-- the bucket's own file_size_limit/allowed_mime_types columns -- not
-- re-implemented in RLS, which has no clean way to inspect an upload's
-- byte size or declared content type before the row exists.

begin;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('doctor-avatars', 'doctor-avatars', false, 2097152, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do update
set file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists avatars_select_own on storage.objects;
create policy avatars_select_own on storage.objects
  for select to authenticated
  using (bucket_id = 'doctor-avatars' and (storage.foldername(name))[1] = (select auth.uid()::text));

drop policy if exists avatars_insert_own on storage.objects;
create policy avatars_insert_own on storage.objects
  for insert to authenticated
  with check (bucket_id = 'doctor-avatars' and (storage.foldername(name))[1] = (select auth.uid()::text));

drop policy if exists avatars_update_own on storage.objects;
create policy avatars_update_own on storage.objects
  for update to authenticated
  using (bucket_id = 'doctor-avatars' and (storage.foldername(name))[1] = (select auth.uid()::text))
  with check (bucket_id = 'doctor-avatars' and (storage.foldername(name))[1] = (select auth.uid()::text));

drop policy if exists avatars_delete_own on storage.objects;
create policy avatars_delete_own on storage.objects
  for delete to authenticated
  using (bucket_id = 'doctor-avatars' and (storage.foldername(name))[1] = (select auth.uid()::text));

-- Lets a signed-URL request succeed for anyone viewing an approved,
-- non-suspended doctor's own avatar object -- the bucket itself is never
-- marked public, so a plain/public URL still never resolves; only a
-- signed URL (which requires this SELECT permission to issue) works.
drop policy if exists avatars_select_approved_doctor on storage.objects;
create policy avatars_select_approved_doctor on storage.objects
  for select to anon, authenticated
  using (
    bucket_id = 'doctor-avatars'
    and exists (
      select 1
      from public.doctor_profiles dp
      join public.profiles p on p.id = dp.user_id
      where dp.user_id::text = (storage.foldername(storage.objects.name))[1]
        and dp.status = 'approved'
        and not p.is_suspended
    )
  );

commit;
