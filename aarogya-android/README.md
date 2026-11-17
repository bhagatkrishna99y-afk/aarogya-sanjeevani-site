# aarogya-android

Multi-module Android client for Aarogya Sanjeevani: two apps (patient-facing
and doctor-facing) sharing a common core built on Kotlin, Jetpack Compose,
Hilt, Coroutines/Flow, kotlinx.serialization, and supabase-kt against the
same Supabase backend used by the project's website.

This is a separate, standalone Android project from anything under `apps/`
elsewhere in this repository.

## Modules

| Module | Purpose |
|---|---|
| `:app-patient` | Patient-facing application (`com.aarogya.sanjeevani.patient`) |
| `:app-doctor` | Doctor-facing application (`com.aarogya.sanjeevani.doctor`) |
| `:core:designsystem` | Compose theme (Material 3) and base UI components (buttons, cards) |
| `:core:common` | `AppResult`/`AppError`, dispatcher qualifiers, constants |
| `:core:network` | Supabase client setup, serialization, exception-to-`AppError` mapping |
| `:core:data` | Shared repositories and DTOs: auth, profile, notifications, services, doctors, requests |
| `:core:ui` | Shared Compose building blocks: loading/error/empty states, a screen scaffold |
| `:core:testing` | Test fakes (one per repository interface) and a `MainDispatcherRule` |
| `build-logic` | Convention plugins sharing Android/Kotlin/Compose/Hilt/lint config across modules |

`:feature:*` modules were deliberately skipped: with two apps and this
module count, screen code lives directly under each app module's own
package (e.g. `com.aarogya.sanjeevani.patient.feature.*` as screens are
added) rather than behind an extra layer of Gradle modules that would add
build/config overhead without yet having enough shared-between-apps screen
code to justify it. Revisit this once a screen needs sharing between both
apps.

## Prerequisites

- JDK 17
- Android Studio (latest stable) or the command line with an Android SDK
  installed and `ANDROID_HOME`/`sdk.dir` set
- A Supabase project (URL + anon key) -- the same backend the website talks
  to, or your own dev/staging copy of it

## Setup

1. Clone the repo and open the `aarogya-android/` folder as the project root
   in Android Studio (not the repo root).
2. Copy `local.properties.example` to `local.properties` and fill in:
   - `sdk.dir` (Android Studio usually writes this automatically on first
     sync)
   - `SUPABASE_URL_DEV` / `SUPABASE_ANON_KEY_DEV`
   - `SUPABASE_URL_PROD` / `SUPABASE_ANON_KEY_PROD`

   `local.properties` is gitignored -- never commit it. For CI or a
   command-line build without a `local.properties`, pass the same four keys
   as Gradle properties instead (e.g. `-PSUPABASE_URL_DEV=...`).
3. Sync and run. Each app has `dev`/`prod` flavors and `debug`/`release`
   build types; run `app-patient`'s or `app-doctor`'s `devDebug` variant for
   local development.

## Building from the command line

```bash
./gradlew :app-patient:assembleDevDebug
./gradlew :app-doctor:assembleDevDebug
```

## Quality checks

```bash
./gradlew ktlintCheck   # formatting
./gradlew detekt        # static analysis
./gradlew test          # unit tests (all modules)
```

`ktlintFormat` auto-fixes formatting issues `ktlintCheck` reports.

## Architecture notes

- **Convention plugins** (`build-logic/convention`) hold all shared Android/
  Kotlin/Compose/Hilt/lint configuration. Module `build.gradle.kts` files
  stay thin -- just `plugins { alias(...) }` plus module-specific bits
  (namespace, applicationId, flavor `app_name`, dependencies).
- **Secrets** flow from `local.properties` (or Gradle/CLI properties) into
  each app's own flavor-specific `BuildConfig.SUPABASE_URL`/
  `SUPABASE_ANON_KEY` -- see `AndroidApplicationConventionPlugin.kt`.
  `:core:network` itself has no flavors and no `BuildConfig` of its own; it
  takes a plain `SupabaseConfig` value that each app provides via its own
  small Hilt module (`<app>/di/NetworkConfigModule.kt`).
- **`:core:data`'s DTOs and RPC calls are grounded in the real backend
  schema** (the same Supabase schema the website's `database.types.ts` is
  generated from), not an invented one -- so these repositories work
  against the real backend as written.
- **`:core:testing`** is the one module allowed to depend on other
  `:core:*` modules (to build fakes of their types); no other `:core:*`
  module depends back on it from its `main` source set, to avoid a
  dependency cycle.
