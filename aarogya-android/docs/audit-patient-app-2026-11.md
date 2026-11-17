# Patient App — PRD Quality Audit

**Date:** 2026-11-16/17
**Scope:** `app-patient` and the `core:*` modules it depends on.
**Method:** full static code audit against the 7 PRD quality categories below, with
fixes applied in place and verified via `compileDevDebugKotlin`, `ktlintCheck`,
`detekt`, `testDevDebugUnitTest`, `compileDevDebugAndroidTestKotlin`, and a real
R8-minified `assembleDevRelease` — all green at the time of writing.

**What this report is not:** a substitute for a human+device pass. Several PRD
requirements — the Accessibility Scanner, a live TalkBack walkthrough, visual
light/dark and 360dp/600dp+ inspection, airplane-mode/throttled-network/expired-
session/500s live testing, cold-start and jank measurement, and a manual
click-through of the signed release build — need a physical or emulated Android
device and profiling tools this environment doesn't have. Every such item is
called out explicitly below, under **"Needs a human+device pass"**, rather than
guessed at or fabricated.

---

## Summary of fixes applied this pass

| # | File | Fix |
|---|------|-----|
| 1 | `app-patient/.../home/DoctorsDirectoryScreen.kt` | Deleted — stale, fully unreferenced duplicate of `doctors/DoctorsDirectoryScreen.kt` |
| 2 | `app-patient/.../ui/common/PlaceholderScreen.kt` | Deleted — fully dead, zero references anywhere |
| 3 | `app-patient/.../profile/EditProfileScreen.kt` | Loading state rendered the real (empty-valued) form fields + a bare `CircularProgressIndicator` instead of a skeleton; replaced with a proper `EditProfileSkeleton` |
| 4 | `app-patient/.../notifications/NotificationsListContract.kt`, `NotificationsListViewModel.kt`, `NotificationsListScreen.kt` | `MarkAllReadClicked` had no in-flight guard (unlike every other write action) — a double-tap fired two concurrent `markAllRead()` calls with a real optimistic-rollback race; added `isMarkingAllRead` guard + disabled the button while in flight |
| 5 | `app-patient/.../auth/AuthGateViewModel.kt` | `signOut()` had no double-tap guard; added `isSigningOut` |
| 6 | `core/designsystem/.../component/AsTopBar.kt` | Screen title wasn't marked as a heading (unlike `SectionHeader`); added `Modifier.semantics { heading() }` |
| 7 | `core/designsystem/.../component/SkeletonShimmer.kt` + 13 screen files | `SkeletonShimmer`'s own doc comment says a loading group "should announce 'Loading' once," but no call site actually did — TalkBack was silent on every loading screen. Added a `loadingGroupSemantics()` helper and wired it into every shimmer-group container |
| 8 | `app-patient/.../profile/ProfileScreen.kt` | Avatar photo-badge was a second, non-interactive semantics node layered on the real clickable target, with no accessible label at all during upload; merged into one clickable region with `contentDescription`/`stateDescription` |
| 9 | `app-patient/.../doctors/SlotChip.kt`, `AvailabilityScreen.kt` | Local `SlotChip` shared a name with an unrelated, more capable `core:designsystem` `SlotChip` (which has "unavailable" struck-through handling this one doesn't) — renamed to `TimeSlotChip` to remove the misleading collision |
| 10 | `app-patient/.../PatientApp.kt`, `MainActivity.kt`, `gradle/libs.versions.toml`, `app-patient/build.gradle.kts` | No `WindowSizeClass` usage anywhere in the app; added `calculateWindowSizeClass` in `MainActivity`, threaded through `PatientAppRoot`/`PatientApp`, capping content to a 600dp reading width and centering it on `Expanded` (tablet) widths |
| 11 | `app-patient/.../AarogyaPatientApplication.kt` | No global uncaught-exception handling; added a wrapper (never a replacement) around the previous default handler that logs via Timber before delegating, so a fatal crash gets a chance to be reported |
| 12 | `app-patient/.../logging/CrashReportingTree.kt` (new) + `FeatureFlags.kt` | No Crashlytics-ready logging hook existed; added a `Timber.Tree` planted in release builds, gated behind `CRASH_REPORTING_ENABLED = false`, that only ever forwards WARN+/exceptions and explicitly never the free-text log message |
| 13 | `app-patient/src/main/AndroidManifest.xml`, `res/xml/network_security_config.xml` (new) | No explicit network security config — cleartext was only blocked by the `targetSdk=35` platform default, not an auditable, won't-silently-regress rule; added an explicit `cleartextTrafficPermitted="false"` base config |
| 14 | `app-patient/proguard-rules.pro` | Stale "nothing project-specific needed yet" comment from the original scaffold; added the official kotlinx.serialization keep rules, plus a real, build-verified fix for an R8 "missing class" failure from Tink (`androidx.security.crypto`'s transitive dependency referencing `error-prone`'s `@Immutable` annotation, which is compile-time-only) |
| 15 | `core/designsystem/.../component/DateStrip.kt` | `items(dates) { ... }` had no `key` — added `key = { it.toEpochDay() }` |

Plus new tests: `EditProfileScreenA11yTest`, `SuccessScreenA11yTest`, a heading-semantics
test added to `AvailabilityScreenTest`, and two new `NotificationsListViewModelTest`
cases for the mark-all-read guard.

---

## 1. Loading / Error / Empty states

**Rule checked:** every screen has all three states via the shared components
(`ErrorContent`/`CompactErrorContent`, `EmptyContent`/`CompactEmptyContent`,
`SkeletonShimmer`); no bare spinners where a skeleton fits; every error has a
retry action; forms preserve input on error.

**Method:** enumerated all 24 `*Screen.kt` files under `app-patient/src/main/kotlin`,
grepped each for the three shared-component families plus any bare
`CircularProgressIndicator`, and read the ones with unusual counts in full.

**Findings & fixes:**
- Two dead files deleted (`home/DoctorsDirectoryScreen.kt`, `ui/common/PlaceholderScreen.kt`) — see table above.
- `HomeScreen.kt` showed zero matches for all three component families; traced this to
  `HomeSections.kt` (`CategoriesSection`/`RequestsSection`/`DoctorsSection`), which
  turned out to already use `CompactErrorContent`/`CompactEmptyContent`/`SkeletonShimmer`
  correctly — a false alarm, no fix needed.
- `EditProfileScreen.kt` (fix #3 above): the loading state rendered the actual form —
  empty `fullName`/`phone` fields plus a spinner below — which both looks like blank
  input and leaves nothing for TalkBack to announce. Fixed with a dedicated skeleton.
- Auth screens (`WelcomeScreen`, `LoginScreen`, `SignUpScreen`, `ForgotPasswordScreen`,
  `CheckInboxScreen`) correctly show zero matches for the three shared components —
  they're pure submit forms with no server-loaded content, so inline field errors +
  a snackbar (`ShowError`/`ShowRejection` events, resolved via `toUserMessage(context)`
  only at the UI edge) is the right pattern, not a gap.
- Verified forms preserve input on error by reading `LoginViewModel`, `SignUpViewModel`,
  `ForgotPasswordViewModel`, and `RequestFormViewModel`: every `submit()` guards on an
  `isSubmitting` flag, and on `AppResult.Failure` only flips `isSubmitting` back to
  `false` — no field is ever cleared. Confirmed via
  `EditProfileViewModelTest."a failed save keeps everything the patient typed"`
  (`app-patient/src/test/.../profile/EditProfileViewModelTest.kt:174-189`), which
  already existed and passes.
- `DoctorsDirectoryScreen.kt`'s and `ServicesListScreen.kt`'s pagination "load more"
  footer spinners (`CircularProgressIndicator` at the bottom of an infinite-scroll
  list) are the standard, acceptable pattern for a "loading next page" indicator — not
  a "bare spinner where a skeleton fits" violation; a skeleton doesn't make sense for
  a one-row footer. Left as-is.

**Needs a human+device pass:** none specific to this category — this was fully auditable statically.

---

## 2. Duplicate submission

**Rule checked:** every write action (sign-up, login, create request, cancel, mark
read, save profile, and every other mutating call) is guarded against a double-tap.

**Method:** grepped every `ViewModel.kt` under `app-patient` for calls into a
`...Repository.create/update/delete/cancel/upload/sign.../mark...` method, and read
each one's guard.

**Findings & fixes:**
- `SignUpViewModel.submit()`, `LoginViewModel.submit()`, `ForgotPasswordViewModel.submit()`,
  `RequestFormViewModel`'s submit, `AvailabilityViewModel.continueClicked()`,
  `RequestDetailViewModel.cancelRequest()`, `EditProfileViewModel.save()`,
  `ProfileViewModel.uploadAvatar()` — all already guard correctly (`if (currentState.isX) return`
  before setting the in-flight flag).
- `NotificationsListViewModel.markOneRead()` — no explicit guard, but it's idempotent-safe
  by construction: `onNotificationClicked` only calls it `if (!notification.isRead)`, and
  the optimistic update flips `isRead` to `true` before the network call, so a second tap
  on the same notification is already a no-op.
- `NotificationsListViewModel.markAllRead()` — **real gap, fixed** (table entry #4): no
  guard at all, and the "Mark all as read" button had no disabled state. A double-tap
  fired two concurrent `markAllRead()` calls; the optimistic-rollback logic
  (`allNotifications = previousAll` on failure) captures `allNotifications` by closure
  at call time, so two overlapping calls where one fails and one succeeds could roll
  back a *correct* state. Added `isMarkingAllRead` to the UI state, guarded the function
  on it, and disabled the button while it's true.
- `AuthGateViewModel.signOut()` — **real gap, fixed** (table entry #5): no guard. The
  logout confirmation dialog closes on the same tap that calls it, making a real
  double-tap window narrow, but not impossible on a sluggish device. Added `isSigningOut`.
- `ProfileViewModel.onAction(AppearanceModeSelected)` — writes the selected theme mode
  to DataStore on every tap; not guarded, but deliberately not: it's a last-value-wins
  preference toggle, not a create/mutate-once action, so a guard would add complexity
  for no behavioral benefit.

**New tests:** `NotificationsListViewModelTest` gained
`` `a second mark-all-read tap while one is already in flight is a no-op` `` and an
`isMarkingAllRead` assertion on the existing rollback test
(`app-patient/src/test/.../notifications/NotificationsListViewModelTest.kt:88-113`).

**Caveat on test depth:** these guards are verified the same way every existing guard
in this codebase is verified — asserting the in-flight flag resets to `false` after
completion — not by a true concurrent-dispatch race test. The fakes used in this test
suite resolve synchronously under `UnconfinedTestDispatcher`, so a literal "fire twice
before the first resolves" test would need a controllable-suspension fake that doesn't
exist yet for any ViewModel in this suite, including the ones already shipped. This is
a pre-existing, suite-wide test-depth limit, not something introduced here — flagging
it as a reasonable follow-up rather than building new async test infrastructure under
this task's scope.

**Needs a human+device pass:** a literal double-tap on a real device (finger speed,
touch debouncing at the OS/view layer) could behave slightly differently than a
scripted test; worth a quick manual check on sign-up, cancel, and mark-all-read during
a device pass.

---

## 3. Accessibility

**Rule checked:** Accessibility Scanner + TalkBack through the five main journeys;
missing `contentDescription`s; focus order; heading semantics; state descriptions on
selectable chips; 48dp+ targets; 200% text scaling; non-color-only status.

**Method:** read every shared component in `core/designsystem/.../component/` (21
files) plus every screen's icon-button/clickable usage, since the design system is
the highest-leverage place to fix something once and have it apply everywhere.

**What was already right** (no fix needed, confirmed by reading the code):
- `AsButton` — 48dp min height, `stateDescription = "Loading"`, `disabled()` semantics,
  label never replaced by the spinner (so TalkBack never loses the action's name mid-load).
- `AsListItem` — 48dp min height, `Role.Button` when clickable.
- `AsBottomBar` — Material's own `NavigationBarItem` (48dp+, selected/unselected
  semantics) plus a separate, explicit badge content description ("N unread notifications").
- `StatusChip` — icon + label + explicit `"Status: $label"` content description; never
  color alone.
- `SlotChip` (core:designsystem) and `DateStrip` — explicit "unavailable"/full-date
  announcements, strike-through text (not color-only), `Role.Tab`/`selected` semantics.
- `AsTextField` — Material3 `isError` state plus an explicit
  `Modifier.semantics { error(errorText) }` so TalkBack announces *what's* wrong, not
  just *that* something is; autofill wired via `AutofillNode`/`LocalAutofillTree`.
- `AsAvatar` — `clearAndSetSemantics { contentDescription = name }` so an image +
  initials-fallback announces once, not as two separate nodes.
- `AvailabilityScreenTest.kt` already asserted `assertIsSelected`/`assertIsNotSelected`
  on slot chips — real semantics assertions, not just click-behavior checks.

**Real findings & fixes:**
- `AsTopBar` (table #6) — the one screen-title component with no heading semantics at
  all, while `SectionHeader` already had it. A TalkBack user could jump between
  sub-sections but never between screen titles. Fixed; added a test asserting it
  (`AvailabilityScreenTest."theScreenTitle_isMarkedAsAHeading"`,
  `EditProfileScreenA11yTest."theScreenTitle_isMarkedAsAHeading"`).
- `SkeletonShimmer` loading-group announcement (table #7) — the component's own doc
  comment documents the contract every call site was supposed to follow and none did.
  Fixed with a new `loadingGroupSemantics()` helper, wired into 13 files:
  `RequestFormScreen.kt`, `AvailabilityScreen.kt`, `DoctorDetailScreen.kt`,
  `DoctorsDirectoryScreen.kt`, `NotificationsListScreen.kt`, `ProfileScreen.kt`,
  `EditProfileScreen.kt`, `RequestDetailScreen.kt`, `RequestsListScreen.kt`,
  `DoctorListForServiceScreen.kt`, `ServiceDetailScreen.kt`, `ServicesListScreen.kt`,
  `HomeSections.kt`. New test:
  `EditProfileScreenA11yTest."whileLoading_announcesLoadingOnce_andNeverShowsTheEmptyForm"`.
- `ProfileScreen.kt`'s avatar photo-badge (table #8) — the camera-icon badge overlapping
  the avatar was its own separate, non-interactive semantics node (so TalkBack read a
  confusing "image: Change photo" with no action attached to it), and had *no*
  accessible label at all while uploading (`CircularProgressIndicator` carries no
  description by default). Restructured so the clickable region, the "Change photo"
  label, and the "Uploading" state all live on one merged node; also disabled the
  click target while an upload is in flight (a minor extra double-tap guard, see §2).
- `SlotChip` naming collision (table #9) — not itself a live bug (the real booking flow
  only ever receives already-bookable slots, so the "unavailable" branch was never
  reachable from the real screen either way), but a name collision between an
  unrelated, more-capable design-system component and a local helper is exactly the
  kind of thing that misleads whoever touches this code next. Renamed to `TimeSlotChip`.

**Reviewed, no fix applied (deliberate):**
- `AsAvatar`'s initials text uses a fixed `fontSize = (size.value / 2.2).sp` inside a
  fixed-size circular `Box`. `.sp` does scale with the system font-size setting, so at
  200% scale the initials text would grow inside a circle that doesn't — a cosmetic
  clipping risk, not a loss of information (the person's full name is always shown
  separately as real, scalable text nearby). Treated as an acceptable, Android-standard
  avatar-initials tradeoff rather than a defect worth added complexity to fix.

**Needs a human+device pass (explicitly, per the original scope commitment to this
task — not run, not simulated, not approximated):**
- Running the actual Accessibility Scanner app against each of the five main journeys.
- A live TalkBack walkthrough of sign-up/login, home, doctor discovery/booking,
  request tracking, and profile — focus order in particular can only be fully verified
  by actually swiping through a real screen reader session, not by reading semantics
  modifiers in isolation.
- Visual confirmation of touch-target sizing and text reflow at 200% system font scale
  on a real device (Settings → Accessibility → Font size).

---

## 4. Theming

**Rule checked:** every screen in light and dark, at 360dp and 600dp+ (tablet);
`WindowSizeClass` for adaptive layouts where it clearly helps.

**Method:** grepped for hardcoded `Color(...)`/`Color.Red` etc. outside
`core/designsystem/.../theme/`; read `Theme.kt`'s light/dark `ColorScheme` + extended
colors; checked every component for fixed widths large enough to overflow a 360dp
screen; grepped for any pre-existing `WindowSizeClass` usage.

**Findings & fixes:**
- Zero hardcoded colors anywhere outside the theme package — every screen/component
  reads `MaterialTheme.colorScheme.*` or `AsTheme.extendedColors.*`.
- `Theme.kt` defines a complete `lightColorScheme`/`darkColorScheme` pair (every
  Material3 token filled in, not just the commonly-used subset) plus a separate
  `LightExtendedColors`/`DarkExtendedColors` for this app's own status-color
  vocabulary; dynamic color is opt-in and off by default (a healthcare app's brand
  teal shouldn't silently become a wallpaper-derived hue). Every component file already
  ships paired `@Preview(... light)`/`@Preview(..., uiMode = UI_MODE_NIGHT_YES)`
  composables — a real static substitute for eyeballing every component in both themes,
  though not a replacement for seeing the full assembled screens.
- No fixed-width element anywhere wide enough to overflow a 360dp screen; every
  horizontally-scrolling row (`DateStrip`, chip rows, doctor-card rows) is a proper
  `LazyRow`, not a fixed-width `Row`.
- **Zero `WindowSizeClass` usage anywhere in the app** (table #10) — a real gap against
  the PRD's explicit ask. Added `calculateWindowSizeClass(this)` in `MainActivity.kt`,
  threaded through `PatientAppRoot(windowWidthSizeClass)` → `PatientApp`, which now caps
  `PatientNavHost`'s content at a 600dp reading width and centers it when the window is
  `Expanded` (tablet-width). This is deliberately the single, lowest-risk adaptive
  pattern applied once at the root rather than a full multi-pane master-detail rebuild
  of the nav graph — scoped to what the PRD calls "where it clearly helps," not a
  speculative redesign of every screen's layout.
- New Gradle dependency: `androidx.compose.material3:material3-window-size-class`
  (catalog alias `androidx-compose-material3-windowsizeclass` — `-window-size-class`
  itself isn't a legal catalog alias since it contains the reserved word `class`).

**Needs a human+device pass:** actually seeing every screen rendered at 360dp and
600dp+, and in light/dark, on a real device or a resizable emulator — the static checks
above rule out the common failure modes (hardcoded colors, fixed overflow-prone widths,
no width-adaptive mechanism at all) but can't replace looking at it.

---

## 5. Resilience

**Rule checked:** airplane mode, throttled network, expired session, backend 500s,
empty datasets — the app must never crash and must always show a recoverable state.
Global uncaught-exception handling. Crashlytics-ready logging, disabled by default,
no personal data.

**Method:** read `core/network/.../SafeApiCall.kt` and `NetworkErrorMapper.kt` (the
one chokepoint every repository call goes through), traced how each failure mode maps
to the `AppError` hierarchy, and checked the application-level exception/logging setup.

**What was already right:**
- `safeApiCall` catches `Throwable` deliberately and exhaustively (documented as such),
  with correct `CancellationException` passthrough (never swallows a real coroutine
  cancellation) and a deliberate exception for `TimeoutCancellationException`
  (reported as a network timeout, not a cancelled scope). Retry is opt-in and bounded
  (3 attempts, exponential backoff), and **only** for explicitly-marked idempotent
  reads — a write is never silently retried (a timed-out `create_service_request` retried
  automatically could double-book a slot).
- `NetworkErrorMapper.toAppError()` maps every realistic failure mode already:
  - **Airplane mode / no connection** → `IOException` → `AppError.Network`.
  - **Slow network / timeout** → `HttpRequestTimeoutException`/`ConnectTimeoutException`/
    `SocketTimeoutException`/`TimeoutCancellationException` → `AppError.Network(isTimeout = true)`.
  - **Backend 500s / unrecognized errors** → an unprefixed `RestException` or
    `SerializationException` → `AppError.Unknown` — shown via the standard
    `ErrorContent` + retry (§1), a recoverable state by design.
  - **Expired/invalid session** → `UnauthorizedRestException`, or the backend's own
    `AS000_NOT_AUTHENTICATED` business code → `AppError.Unauthorized`.
  - **Empty datasets** → covered exhaustively in §1 (every list screen's `EmptyContent`).
- `AuthRepositoryImpl.observeAuthState()` is driven directly by Supabase's own
  `client.auth.sessionStatus` flow, which the SDK keeps correct via its own proactive
  token-refresh scheduling — `AuthGateEffect` reacts to that flow going
  `Unauthenticated` and redirects to Welcome/Login on its own, independent of whatever
  screen the patient happens to be on.

**A real, narrow finding — documented, not code-fixed (explicit judgment call):**
No screen does anything beyond showing the generic `ErrorContent` + retry when it
receives `AppError.Unauthorized` specifically (confirmed: the only other reference to
it in `app-patient` is `EditProfileViewModel.kt:40`, mapping a missing profile to it,
not handling a real 401). In the overwhelmingly common case this is fine — Supabase's
own session-refresh logic should already have flipped `sessionStatus` to
`Unauthenticated` (triggering the app-wide redirect) by the time a call actually comes
back 401. But in the narrow case where a call fails with `AppError.Unauthorized`
*before* the reactive session flow catches up (or where the backend's own
`AS000_NOT_AUTHENTICATED` business check fires independently of token expiry), a
patient could be stuck tapping "Retry" on a screen that will keep failing identically
forever, with no way back to login short of killing the app.
**Recommended follow-up** (not implemented in this pass, given its cross-module
footprint — `core:network` would need to signal `core:data` without inverting their
dependency direction, which needs a deliberate small-scope task of its own, not a
same-day retrofit into an already-large audit): a lightweight event emitted from
`safeApiCall` whenever it resolves `AppError.Unauthorized`, observed by a
long-lived `AuthRepositoryImpl`-owned collector that forces a local sign-out —
turning every `Unauthorized` into the same app-wide redirect a real session expiry
already produces, rather than leaving it to each screen's own generic error state.
- Added (table #11): a global uncaught-exception handler in
  `AarogyaPatientApplication.installUncaughtExceptionLogger()` that **wraps, never
  replaces**, whatever handler is already installed — it logs via `Timber.e` (routed
  through the new `CrashReportingTree` in release) and then always delegates to the
  previous handler, so the OS's own crash dialog and clean-process-restart behavior is
  completely unchanged. This is deliberately *not* an attempt to keep the app running
  past an uncaught exception — that's not something that can be done safely on Android,
  and trying to would be worse than letting the process die cleanly.
- Added (table #12): `CrashReportingTree` (`app-patient/.../logging/CrashReportingTree.kt`),
  planted in release builds. **Disabled by default** via
  `FeatureFlags.CRASH_REPORTING_ENABLED = false` — and even if flipped on, there is no
  real backend wired in yet (the `log()` override is a documented, intentional no-op;
  see its own doc comment for the exact one-line change a real Crashlytics/equivalent
  integration would make there). Filters to `WARN`/`ERROR` only, and — the one rule this
  class exists to enforce — **never forwards the free-text log message**, only the
  `Throwable` itself and its tag, since the message string is exactly where a future
  `Timber.e("Upload failed for $email")`-shaped mistake would leak personal data.
  Covered by `CrashReportingTreeTest.kt` (priority filtering, null-throwable drop).

**Needs a human+device pass:** actually toggling airplane mode, throttling network
(Android Studio's network profiler or a proxy), forcing a session expiry (revoking a
token server-side or waiting one out), and triggering a real backend 500 mid-flow —
confirming the recoverable states above actually render as expected, live, on each of
the five main journeys.

---

## 6. Performance

**Rule checked:** stable immutable models; keys in lazy lists; `remember`/
`derivedStateOf` where appropriate; Coil caching; baseline profiles; cold-start time
(target < 2s) and jank on a mid-range device profile.

**Method:** grepped every `items(...)` call in a `LazyRow`/`LazyColumn` for a missing
`key`; checked the Compose compiler configuration for stability-related settings;
checked for a custom Coil `ImageLoader`; checked existing `remember`/`derivedStateOf`
usage for correctness; checked for any existing baseline-profile tooling.

**Findings & fixes:**
- `core/designsystem/.../component/DateStrip.kt:47` (table #15) — `items(dates) { ... }`
  had no `key`; added `key = { it.toEpochDay() }`. The other `items(COUNT) { ... }`
  calls without a key (shimmer placeholders in `DoctorsDirectoryScreen.kt`,
  `HomeSections.kt`, `ServicesListScreen.kt`) are correct as-is — count-based,
  interchangeable loading bars have no identity for a key to usefully track.
- **Stability**: this project is on Kotlin 2.1.0's bundled Compose compiler plugin
  (`org.jetbrains.kotlin.plugin.compose`), which has *strong skipping mode* on by
  default and un-overridden here (`build-logic/convention/.../Compose.kt` has no
  `stabilityConfigurationFile`/skipping override at all). Strong skipping makes even
  "unstable" parameters (plain `List<T>`, as used throughout every `UiState` in this
  app) still skippable via runtime equality, which meaningfully reduces the practical
  cost of not wrapping every list-shaped UI-state field in
  `kotlinx.collections.immutable.ImmutableList`. Given the data sizes involved
  (a doctor's slot list, a page of requests/notifications — tens of items, not
  thousands), migrating to `ImmutableList` everywhere would be premature optimization
  for this app's actual scale; not done.
- Every domain model read (`DoctorPublic`, `Notification`, `PatientRequest`, etc.) is
  a plain Kotlin `data class` — structural equality, which is exactly what strong
  skipping leverages.
- `remember`/`derivedStateOf` usage already correct everywhere it's used
  (`DateStrip.kt`, `AsAvatar.kt`, `PatientNavHost.kt` all key their `remember` blocks
  on the right inputs). No `derivedStateOf` usage exists anywhere in the app; none of
  the current UI state shapes have a clear case for one (no frequently-changing Compose
  state feeding an expensive, rarely-changing derived value) — not adding one
  speculatively.
- `AvailabilityScreen.kt`'s `slots.groupByPeriod(...)` call runs un-`remember`-ed inside
  `SlotsGrid`, recomputed on every recomposition (e.g., every time `selectedSlot`
  changes). Reviewed and left as-is: a single day's bookable-slot list is realistically
  under ~50 items, and grouping that many items by time-of-day period is computationally
  negligible — wrapping it in `remember` would add a parameter/key to track for no
  measurable benefit at this data scale.
- **Coil**: only `AsAvatar.kt` loads a remote image (`coil3.compose.AsyncImage`), with
  no custom `ImageLoader` anywhere — the app relies on Coil 3's default
  `SingletonImageLoader`, which already enables both an in-memory LRU cache (~25% of
  available app memory) and a disk cache (~2% of disk space) out of the box. Given this
  app's actual image-loading footprint (small avatar JPEGs, not a photo gallery), a
  hand-tuned custom `ImageLoader` would be unneeded complexity; the defaults are
  appropriate and already "on."
- **Baseline profiles**: none exist, and none were added in this pass. Generating a
  real one requires a `:baselineprofile` Gradle module applying
  `androidx.baselineprofile` + a `BaselineProfileRule`-driven instrumented test that
  walks the critical startup→first-meaningful-content path, run via Macrobenchmark on
  a real or virtual device — none of which this environment can execute (no device, no
  Macrobenchmark target). Scaffolding an unverifiable new module/plugin into the build
  was judged too risky to ship blind (new plugin resolution, AGP/Kotlin-version
  compatibility, settings.gradle.kts wiring — all unverifiable without actually running
  the generator), so this is a **recommendation, not a fix**:
  1. Add a `:baselineprofile` module (`androidx.baselineprofile` plugin).
  2. One instrumented test (`BaselineProfileGenerator`) using `BaselineProfileRule`,
     driving: cold start → Splash → auth gate → Home → Doctors Directory →
     Doctor Detail → Availability (the heaviest real screen graph this app has).
  3. `app-patient/build.gradle.kts`: apply the baseline-profile consumer plugin and add
     `baselineProfile(project(":baselineprofile"))`.
  4. `./gradlew :baselineprofile:generateBaselineProfile` on a connected device/emulator.

**Needs a human+device pass (explicitly, per scope — not estimated or guessed at):**
- Cold-start time, measured against the PRD's <2s target, via Macrobenchmark's
  `StartupTimingMetric` or `adb shell am start -W` on a mid-range device profile.
- Jank, via Macrobenchmark's `FrameTimingMetric` while scrolling the heaviest lists
  (Doctors Directory, Requests, Notifications) and through the booking flow.
- Generating the actual baseline profile per the steps above, once a device is available.

---

## 7. Security

**Rule checked:** no secrets in the repo or `BuildConfig` beyond the URL and anon key;
network security config forbids cleartext; release builds disable debugging/logging;
R8 shrinking works without breaking serialization (keep rules added, release build
verified manually).

**Method:** read every `buildConfigField`/`secret()` call site; grepped the whole repo
for hardcoded-secret-shaped string literals, stray keystores, and
`google-services.json`; read the manifest and build types; ran a real, R8-minified
`assembleDevRelease` to completion (twice — see below) rather than inferring from
config alone.

**Findings & fixes:**
- `BuildConfig` exposes exactly four fields app-wide: `SUPABASE_URL`,
  `SUPABASE_ANON_KEY` (both via `AndroidApplicationConventionPlugin.kt`'s shared
  `secret()` helper — `local.properties` first, `-P` CLI property fallback, hard
  failure at configuration time if neither is set, never a hardcoded default), plus
  app-patient-only `WEBSITE_BASE_URL`/`CONTACT_EMAIL` (same pattern, `websiteBaseUrl()`/
  `contactEmail()` in `app-patient/build.gradle.kts`). No other secret-shaped field
  exists anywhere in the build.
- `local.properties` is correctly `.gitignore`d (`.gitignore:2`); `local.properties.example`
  contains only placeholder values and correctly documents that the Supabase anon key
  is the public, RLS-constrained one (safe to ship in a built APK) but still shouldn't
  be committed, as a matter of hygiene/rotation.
- Repo-wide grep for hardcoded secret-shaped literals (`(api[_-]?key|secret|password|token)\s*=\s*"..."`),
  stray `.jks`/`.keystore` files, and `google-services.json` — all clean, nothing found.
- **Network security config** (table #13) — **real gap, fixed**. There was no
  `android:networkSecurityConfig` at all; cleartext traffic was only blocked by the
  `targetSdk=35` platform default (API 28+ defaults `usesCleartextTraffic` to `false`),
  an implicit rule one `minSdk`/`targetSdk` change away from silently reverting. Added
  an explicit `res/xml/network_security_config.xml` (`cleartextTrafficPermitted="false"`
  base config, no per-domain overrides needed since every real request goes to
  Supabase's own HTTPS endpoint) and wired it into
  `app-patient/src/main/AndroidManifest.xml`.
- **Release debuggability/logging**: `AndroidApplicationConventionPlugin.kt`'s `release`
  build type has no `isDebuggable` override (AGP's own default — `false` for release —
  applies), `isMinifyEnabled = true`, `isShrinkResources = true`. Logging is gated
  correctly: `Timber.DebugTree()` only plants in `BuildConfig.DEBUG` builds; release
  builds plant the new `CrashReportingTree`, which — independent of the `DEBUG` check —
  is itself disabled by default (§5) and forwards no personal data even when enabled.
- **R8 + serialization** (table #14) — this is where a real, build-verified bug was
  found and fixed, not just a config review:
  1. `proguard-rules.pro` previously contained only a stale comment from the project's
     original "blank scaffold" state ("nothing project-specific is needed yet"),
     unchanged since, despite the app having grown a full kotlinx.serialization-backed
     network layer in the meantime. Added the official, documented kotlinx.serialization
     keep rules (serializer-companion lookup, `@Serializable` member retention), plus
     explicit `-keep`/`-keepclassmembers` rules scoped to `core.network`/`core.data`'s
     own DTO packages as additional margin against R8's full-mode shrinking reaching a
     serializer some other way the generated rules don't anticipate.
  2. Running the actual release build (`./gradlew :app-patient:assembleDevRelease`)
     with those rules in place **failed** at `minifyDevReleaseWithR8`:
     `ERROR: R8: Missing class com.google.errorprone.annotations.Immutable (referenced
     from: com.google.crypto.tink.KeyTemplate and 4 other contexts)` — Google Tink
     (pulled in transitively by `androidx.security.crypto`, which backs
     `core/network/.../EncryptedSessionManager.kt`'s session storage) references an
     error-prone annotation that's compile-time-only and never present at runtime. Fixed
     with the exact rule R8 itself generated and suggested
     (`app-patient/build/outputs/mapping/devRelease/missing_rules.txt`):
     `-dontwarn com.google.errorprone.annotations.Immutable`.
  3. Re-ran the full release build after the fix: **`BUILD SUCCESSFUL`**, R8 minification,
     resource shrinking, and APK packaging all completed cleanly
     (`app-patient:assembleDevRelease`, ~5 minutes, no errors or missing-class warnings).
     This is the "verify a release build manually" requirement, done to the extent
     possible without a device: the build succeeds end-to-end with shrinking and
     obfuscation on; what's still outstanding is installing that APK on a device and
     clicking through the real flow (see below) — a release build that merely *builds*
     isn't proof that, say, a `@Serializable` field didn't silently get renamed in a way
     that still parses but loses data at runtime, which only an actual run can catch.
- `android:allowBackup="false"` was already set on the `<application>` tag — correctly
  preventing Android Auto Backup from exfiltrating session tokens/cached data.

**Needs a human+device pass:**
- Installing the signed release build on a real device and manually clicking through
  sign-up, login, booking, request tracking, and profile — the one thing a successful
  R8 build cannot prove on its own (see point 3 above).
- No signing config exists yet (`AndroidApplicationConventionPlugin.kt`'s own comment
  documents this as intentional for local `assembleRelease` smoke-testing only) — a
  real release signing key needs to be provisioned before this build type is used for
  an actual release artifact.

---

## Build verification log (this pass)

Every fix above was verified green, per-module, before moving to the next category:

- `compileDevDebugKotlin` / `compileDevDebugAndroidTestKotlin` — `app-patient`, `core:designsystem`
- `ktlintCheck` — `app-patient`, `core:designsystem`
- `detekt` — `app-patient`, `core:designsystem`
- `testDevDebugUnitTest` — `app-patient` (full suite, including the new/updated tests
  listed per-section above)
- `assembleDevRelease` — real R8-minified release build, `BUILD SUCCESSFUL`

No module was left red at any point this session; every fix in the table above was
compiled, linted, and test-verified before the next one started.
