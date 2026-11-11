# Aarogya Sanjeevani — Doctor App

Native Android app (Kotlin, Jetpack Compose, Material 3, MVVM + Hilt) for the
doctor journey: register, complete your professional profile, wait for admin
approval, configure the services you offer and your weekly availability,
then accept/reject/confirm/complete requests as they come in.

## ⚠️ Not build-verified in this environment

Same caveat as the Patient app (see `apps/patient/README.md`): written without
an Android SDK, emulator, or Gradle/Kotlin compiler available in the authoring
environment, so **it has not been compiled or run**. Open it in Android
Studio and let it sync before trusting it — watch especially for:

- supabase-kt (`io.github.jan-tennert.supabase`) API drift in the `Auth`/
  `Postgrest` plugin setup, `SessionManager`, and `rpc()` calls.
- `kotlinx-datetime` 0.6.x calls (`LocalTime` construction/formatting).
- The `DayCard`/`onChange` extension-function-type pattern in
  `ui/availability/AvailabilityScreen.kt` — double-check it against whatever
  Compose/Kotlin version Android Studio resolves.

The Gradle wrapper jar binary isn't included; Android Studio will offer to
add it on first open.

## Configuration

Same as the Patient app — `SUPABASE_URL`/`SUPABASE_PUBLISHABLE_KEY` default
to the `oleazbruvnlmuobqomge` project via `BuildConfig`, overridable through
Gradle properties.

## Architecture

Mirrors the Patient app's structure (MVVM + Hilt, repository layer wrapping
every Supabase call in `safeCall` for friendly errors, `EncryptedSessionManager`
for session storage) with doctor-specific pieces:

- **ui/application/ApplicationGate** — the root gate after sign-in. Calls
  `get_my_doctor_application()` (a SECURITY DEFINER RPC that deliberately
  excludes `admin_notes` — see the backend migration
  `20261111120014_fix_doctor_own_admin_notes_leak.sql`, added while building
  this screen after noticing the original RLS policy let a doctor read their
  own `admin_notes`, which should stay admin-only) and routes to: the
  application form (no application yet, or rejected → resubmit), a pending/
  suspended status screen, or through to the main app once `approved`.
- **ui/services** — lets an approved doctor pick which active,
  doctor-required services they offer (`set_doctor_services`).
- **ui/availability** — a per-day (Sun–Sat) editor for start/end time and
  slot length, calling `set_weekly_availability` with the full week's
  schedule each save (the RPC replaces the doctor's whole schedule
  atomically, so there's no partial-update case to handle client-side).
- **ui/requests** — tabs by status (pending/accepted/confirmed/completed/
  past); actions call `doctor_respond_request`, `doctor_confirm_request`,
  `doctor_complete_request` — every transition is server-validated, so a
  stale or already-handled request surfaces the backend's friendly error
  rather than silently succeeding client-side.

## Verifying the app (once it builds)

1. Sign up, then land on "Complete your profile" — submit an application
   with a specialization, license number and years of experience. Confirm
   it shows "Application under review" afterward.
2. As an admin (website `/admin/doctors`), approve the application. Pull to
   refresh (or sign back in) in the Doctor app — confirm it now shows the
   main Requests/Services/Availability tabs.
3. **Services** — select a couple of services, save, confirm they persist
   after navigating away and back.
4. **Availability** — enable a few days with start/end times and a slot
   length, save. Confirm a patient can then see open slots for those days
   (via the Patient app or `get_available_slots` directly).
5. Have a patient submit a request. Confirm it shows under **Requests →
   Pending**, with Accept/Reject. Accept it, confirm it moves to
   **Accepted** with a Confirm action; confirm it, then mark it completed.
   Confirm the patient sees matching notifications at each step.
6. Reject a different request with a reason — confirm the patient sees
   that reason.
7. Have an admin suspend the doctor — confirm the next protected action
   (e.g. updating availability) is rejected server-side with a friendly
   message, since `is_approved_doctor()` now returns false.
