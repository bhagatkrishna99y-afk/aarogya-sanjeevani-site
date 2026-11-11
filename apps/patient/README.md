# Aarogya Sanjeevani — Patient App

Native Android app (Kotlin, Jetpack Compose, Material 3, MVVM + Hilt) for the
patient journey: discover services, pick a doctor, choose a slot, submit a
request, track it, get notified, cancel when permitted.

## ⚠️ Not build-verified in this environment

This project was written without access to the Android SDK, an emulator, or
Gradle/Kotlin compiler in the authoring environment, so **it has not been
compiled or run**. Open it in Android Studio (Ladybug/Koala or newer) and let
it sync Gradle before trusting anything here — bring your own judgment to the
first build, especially around:

- Exact `io.github.jan-tennert.supabase` (supabase-kt) API surface for your
  resolved version (`Auth`/`Postgrest` plugin install blocks, `SessionManager`
  interface, `rpc()`/`update()` signatures) — these evolve between releases.
- `kotlinx-datetime` 0.6.x API calls (`LocalDateTime` constructor, `toInstant`,
  `todayIn`, `Instant.plus(Duration)`).
- Library version compatibility in `gradle/libs.versions.toml` — bump via
  Android Studio's suggested upgrades if anything conflicts.

The Gradle wrapper jar binary isn't included (couldn't be generated without
network access to Gradle's distribution service) — Android Studio will offer
to add it automatically on first open, or run `gradle wrapper` yourself once
system Gradle is available.

## Configuration

The Supabase URL and publishable key are baked into `BuildConfig` from
`app/build.gradle.kts` defaults (pointing at the `oleazbruvnlmuobqomge`
project already used by the backend and website). Override them without
editing the file by adding to `~/.gradle/gradle.properties` or a local
`gradle.properties`:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
```

## Architecture

- **MVVM + unidirectional data flow**: every screen has a `@HiltViewModel`
  exposing `StateFlow<UiState<T>>` (or a screen-specific state data class);
  Composables only read state and call ViewModel functions, never touch
  Supabase directly.
- **data/remote/dto** — wire-format DTOs matching Postgres column names
  exactly (snake_case via `@SerialName`), decoded by supabase-kt's Postgrest
  client. Timestamps are kept as ISO-8601 strings and parsed to
  `kotlinx.datetime.Instant` only in the repository layer.
- **data/repository** — one repository per bounded concern (`AuthRepository`,
  `CatalogRepository`, `DoctorRepository`, `RequestRepository`,
  `NotificationRepository`), each wrapping Supabase calls in [safeCall] so
  every failure becomes a friendly, user-safe message (never a raw DB error —
  our own RPCs already raise friendly text server-side; this is the client's
  matching discipline).
- **data/session/EncryptedSessionManager** — persists the Supabase session
  using `androidx.security.crypto.EncryptedSharedPreferences` (AES-256-GCM),
  not plain SharedPreferences.
- **The backend is the source of truth**: this app never computes slot
  availability or booking conflicts itself for doctor-bound services — it
  calls `get_available_slots`/`create_request` and surfaces whatever the
  server decides, including the friendly "that slot was just booked" error
  from the double-booking exclusion constraint. For services that don't
  require a doctor, there is no server-side schedule to query (the backend
  only models per-doctor availability), so the app offers a fixed set of
  half-hourly slots during standard hours — still subject to the server's
  "must be in the future" check.
- **ui/navigation/PatientNavGraph** — single `NavHost`; the booking flow
  (doctor list → doctor detail → slot picking → review/submit) is one nested
  graph sharing a single `BookingViewModel` instance (scoped to the graph's
  own back stack entry via `hiltViewModel(parentEntry)`), so in-progress
  booking state isn't re-fetched or threaded through every route's arguments.

## Verifying the app (once it builds)

1. **Sign up** a new patient — confirm a `profiles` row appears in Supabase
   with `role = 'user'`. If the project requires email confirmation (this
   one does by default), the app shows "check your email" instead of signing
   you in silently.
2. **Home** — confirm the live service catalog loads (seeded categories:
   General Consultation, Dermatology, Lab Tests).
3. Pick a **doctor-required** service with no approved doctors yet → confirm
   the empty state, not a crash.
4. Once a doctor is approved and has configured services/availability
   (via the Doctor app or SQL), pick that service → doctor → date → an open
   slot → add notes → submit. Confirm it appears under **My requests**.
5. From **My requests**, open the request, confirm status history renders,
   and confirm "Cancel request" is only offered while `isCancellable`.
6. Trigger a duplicate booking from two sessions targeting the same slot —
   confirm the second gets the friendly "just booked" message, not a crash
   or raw Postgres error.
7. **Notifications** — confirm entries appear after a doctor accepts/rejects/
   confirms/completes/cancels a request, and that tapping one marks it read
   (the unread dot disappears).
8. **Profile** — edit name/phone, confirm it persists; sign out, confirm you
   land back on Login and protected screens are no longer reachable.
