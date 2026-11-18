# Patient App — End-to-End Test Report
**Date:** 2026-11-17
**Device:** Motorola Edge 40 (ZD222F5KWV), physical device, 1080x2400
**Build:** `app-patient` dev debug, versionName 1.0.0-rc1
**Backend:** hosted dev Supabase project (`oleazbruvnlmuobqomge.supabase.co`) — configured this
session (see Setup notes below); seeded with 2 patients, 2 approved doctors, 1 pending doctor,
1 suspended doctor, 1 admin.
**Tester:** Claude Code, driving the real device via `adb` (taps derived from live
`uiautomator` dumps, not guessed coordinates), with a continuous `logcat` capture running
throughout.

No app code was changed during this run, per instructions. This report only records findings —
nothing has been fixed yet.

---

## 1. Summary table

| Area | PASS | FAIL | BLOCKED | Notes |
|---|---|---|---|---|
| A. Authentication | 7 | 5 | 1 | Sign-up is a hard blocker |
| B. Home & services | 6 | 1 | 0 | No-doctor booking is a hard blocker |
| C. Doctors & availability | 5 | 1 | 0 | Cosmetic "Dr. Dr." bug only |
| D. Requests | 5 | 2 | 0 | Submit is a hard blocker; cancel works |
| E. Cross-app (doctor accepts/completes) | 0 | 0 | 1 | Entirely blocked — needs a creatable request |
| F. Notifications | 1 | 0 | 1 | Empty state only; no real notification existed to test against |
| G. Profile | 6 | 1 | 0 | Policy links point to a placeholder domain |
| H. Errors & resilience | 1 | 2 | 2 | Offline handling is good; retry and rotation both have real bugs |
| I. UI / accessibility / performance | 4 | 2 | 0 | Cold start passes; extreme font scale has clipping |
| J. Security | 3 | 0 | 0 | 1 item (deep links) is not applicable — no such feature exists yet |
| **Total** | **38** | **14** | **5** | |

No crashes occurred anywhere in this run — not during sign-up/submit failures, not offline, not
on rotation, not at 200% font scale. Every failure below is a wrong/incomplete/missing
**behavior**, never an app crash.

---

## 2. Failures — detail

Ordered by severity (Blocker → Major → Minor). Screenshots are in this same folder
(`docs/test-run-2026-11-17/`).

### BLOCKER — Creating a new account always fails (A2)
- **Steps:** Welcome → Create an account → fill a valid new name/email/password, check Terms →
  Create account.
- **Expected:** Account created, user signed in (or taken to an email-confirmation step).
- **Actual:** A generic "Something went wrong. Please try again." snackbar every time
  (reproduced twice identically). No exception or any app-tagged log line appears in `logcat`
  at the moment of failure — the error is swallowed before any logging happens.
- **Screenshots:** `A06-signup-ready-to-submit.png`, `A07-signup-submitted.png`,
  `A08-signup-retry-same-email.png`
- **Suspected cause:** unconfirmed — the Supabase dashboard was unreliable this session (a
  platform-wide incident banner, plus a lost browser tab group) so the server-side Auth/API
  logs could not be cross-checked. See the combined note below — this is almost certainly the
  same root cause as D2.

### BLOCKER — Submitting any new request always fails (D2)
- **Steps:** Pick any doctor/service combo with available slots → fill the request form →
  Review → Submit request.
- **Expected:** Request created, shown in My Requests as "Requested."
- **Actual:** Identical generic "Something went wrong. Please try again." snackbar (this time
  with a "Retry" link, which also doesn't help). No log output at the time of failure.
- **Screenshots:** `D02-request-review.png`, `D03-after-doubletap-submit.png`
- **Suspected cause, and why A2+D2 are almost certainly the same bug:** a pre-existing SEEDED
  request could be cancelled successfully end-to-end (status flipped to Cancelled, a new
  History entry appeared, confirmation toast shown — see `D06-cancel-dialog.png`,
  `D07-after-cancel.png`). Cancelling is an **UPDATE** to an existing row; it works. Sign-up
  and request-creation both **INSERT** a brand-new row; both fail identically and silently.
  This strongly points at something INSERT-specific on the backend — a Row Level Security
  INSERT policy, a server-side trigger/function that only fires on insert, a missing default
  value/sequence, or a check constraint — rather than a broad outage, a bad auth token, or a
  client-side networking problem (any of which would also have broken the UPDATE, and would
  also have broken the dozens of successful reads this session: login, browsing
  doctors/services/availability/profile/home data all worked perfectly).
- **Why this matters most:** every downstream flow that needs a request to exist — My Requests
  population from a fresh booking, the double-submit-creates-only-one-request guard, request
  rejection/completion, and the entire Section E cross-app doctor-accepts-the-request flow —
  could not be tested this run because of this one bug. Fixing it likely unblocks several other
  "BLOCKED" rows in the table above for free.

### BLOCKER — Services with no doctor required can never actually be booked (B4)
- **Steps:** Services → "Diagnostic Sample Collection at Home" → Continue (correctly skips
  doctor/slot selection since no doctor is needed) → lands on Request details.
- **Expected:** A bookable request form for this home-visit service.
- **Actual:** The form immediately shows "Choosing a date and time for this service isn't
  available yet." and the Continue button is permanently disabled (confirmed by tapping it
  directly — nothing happens). There is no date/time picker anywhere on the screen to resolve
  this.
- **Screenshot:** `B05-no-doctor-request-form.png`
- **Suspected cause:** looks like an unfinished feature — date/time selection for doctor-less
  services isn't implemented, and its placeholder/guard message is being shown directly to the
  end user instead of being hidden behind a finished implementation.

### MAJOR — Home screen content overlaps the system status bar (A5)
- **Steps:** Log in → land on Home.
- **Expected:** Content starts below the status bar.
- **Actual:** The "Good afternoon, [name]" greeting heading and the profile avatar circle
  render underneath/overlapping the status bar icons (clock, notification, battery%) — on
  every single Home load. Confirmed in both dark and light theme. Gets significantly worse at
  large font scale (200%), where the heading visually collides with the icons.
- **Screenshots:** `A10-login-success.png` (also visible in `home_top.png`,
  `I02-extreme-font.png`)
- **Suspected cause:** Home's top app bar is missing proper WindowInsets/status-bar padding
  that every other screen in the app correctly has (Profile, Services, Doctors, etc. all start
  cleanly below the status bar).

### MAJOR — Home's "Try again" retry doesn't actually recover (H1)
- **Steps:** Go offline (confirmed via `ping`/`dumpsys wifi`, not just toggling the
  airplane-mode setting, which this device doesn't fully honor for Wi-Fi) → cold-load Home →
  see the offline banner and per-section "Try again" links (this part is correct and a real
  **PASS** — see below) → restore real connectivity (confirmed via a successful `ping`) → tap
  "Try again" on a section.
- **Expected:** The section reloads successfully now that the network is back.
- **Actual:** "Try again" kept failing with the same "Something went wrong." message, repeatedly.
  Switching to the Services tab and back to Home, however, loaded everything correctly on the
  very next natural visit.
- **Screenshot:** `H01-airplane-mode-coldload.png`
- **Suspected cause:** the retry action likely isn't actually re-invoking the data load (or is
  checking a stale cached "offline" flag) rather than genuinely retrying the network call. A
  plain screen re-visit, which presumably re-triggers the ViewModel's own lifecycle-driven
  load, works fine.
- **The good news in the same test:** going genuinely offline is handled well — a clear
  "You're offline" banner, independent per-section error+retry affordances, and no crash.

### MAJOR — Rotating the screen discards unsaved Edit Profile changes (H6)
- **Steps:** Profile → Edit profile → change the Full name field (Save correctly enables) →
  rotate the device to landscape.
- **Expected:** The typed edit survives the rotation (per the test plan's explicit requirement).
- **Actual:** The field snaps back to the original saved value, and Save disables again as if
  nothing had been typed.
- **Screenshot:** `H06-rotated-form.png`
- **Suspected cause:** the uncommitted field value is very likely missing
  `rememberSaveable`/proper ViewModel state hoisting, so it resets to the originally-loaded
  value on configuration change instead of surviving it. Only Edit Profile was tested this
  run; other forms (sign-up, request notes) look structurally similar and may share the bug —
  worth a quick follow-up check.

### MINOR/MAJOR (UX) — Doctor-account login is silently refused, no message (A6)
- **Steps:** On the Patient app's Login screen, sign in with a doctor's seeded credentials.
- **Expected:** Refused, with a friendly explanation.
- **Actual:** Refused correctly (the doctor is never let into the Patient app) — but with zero
  feedback. The login button spins, then the screen silently returns to Welcome with no toast,
  snackbar, or dialog at all. Confirmed on two separate attempts, including one screenshot
  taken immediately after the tap that caught the button's own loading spinner right before the
  silent bounce.
- The security property holds; only the "friendly message" half of the requirement is missing.

### MINOR — Doctor names show a doubled "Dr." prefix (C1)
- **Steps:** Doctors tab, or any doctor detail/availability screen.
- **Expected:** "Dr. Priya Sharma."
- **Actual:** "Dr. Dr. Priya Sharma" / "Dr. Dr. Vikram Rao" everywhere a doctor's name is shown.
  The seed data's display name is already "Dr. Priya Sharma" — the UI appears to prepend an
  extra, unconditional "Dr. " on top of that.
- **Screenshots:** `C01-doctor-directory.png`, `C03-doctor-detail.png`

### MINOR (accessibility) — Service-mode selection isn't exposed to TalkBack (D1)
- **Steps:** Request form → tap "In person" or "Online."
- **Expected:** The current selection is conveyed to assistive technology, not only visually.
- **Actual:** The chosen mode clearly highlights visually (filled background + checkmark), but
  a direct `uiautomator` accessibility-tree dump taken immediately afterward shows
  `selected="false"` on **both** options regardless of which is actually chosen. A TalkBack
  user gets no indication at all of which mode is currently selected.

### MINOR — Privacy Policy (and likely Terms) point to an unconfigured placeholder URL (G)
- **Steps:** Profile → Privacy Policy.
- **Expected:** Opens the real privacy policy.
- **Actual:** Opens correctly in a proper Chrome Custom Tab (that mechanism is a genuine
  **PASS**), but the URL itself is still `local-placeholder.example`, which doesn't resolve
  (`DNS_PROBE_FINISHED_NXDOMAIN`). Same unconfigured-placeholder pattern this session also
  found in the backend URL before today's setup. Terms very likely has the identical
  placeholder (not separately re-verified).
- **Screenshot:** `G08-privacy-policy.png`

### MINOR — At extreme font scale, a doctor's avatar initials clip and overlap their name (I2)
- **Steps:** Set system font scale to 200% → Home → Featured doctors.
- **Expected:** Readable at large text sizes, even if cramped.
- **Actual:** "DV" (Dr. Vikram Rao's avatar initials) renders as just "D", and the avatar circle
  overlaps the wrapped name text below it ("Dr. Vikram / Rao"). 200% is an extreme setting
  (most users needing large text use 130–150%, which held up fine), so this is lower priority
  than the other findings, but it's real and reproducible.
- **Screenshot:** `I02-extreme-font.png`

---

## 3. Crash stack traces

None. No crash occurred anywhere in this test run, across authentication failures, the two
Blocker-level backend failures, going genuinely offline, rotating mid-form, or running at 200%
font scale. Every failure found was a wrong/missing/incomplete *behavior*, not an app crash.

---

## 4. Cold start time and notable slow screens

- **Cold start: 1905ms** on a clean install, launched for the device's Owner profile with the
  screen awake — **under the 2000ms target.** (An earlier 3044ms reading was a measurement
  artifact, not a real result: the app had briefly ended up installed into the device's
  secondary "Vault Profile" instead of the Owner profile, and the screen was off during that
  attempt. Both were corrected before taking the measurement above.)
- No screen showed a sustained slow-loading or frozen-UI problem during normal use.
- `logcat` did show a handful of "Skipped N frames" / "Davey!" (~900ms) jank warnings, but every
  one of them was tied to a cold-start or app-resume moment (the very first frame right after
  the process was created or unfrozen), never to active scrolling. Scroll-jank on a genuinely
  long list could not be meaningfully tested this run — the seeded backend only has 2 doctors
  and a modest service catalog, none of which produce a long enough scrollable list.

---

## 5. Prioritized fix list (top 10)

1. **Backend INSERT failures block sign-up and request creation (A2 + D2)** — same root cause,
   fix once. Highest priority: blocks new-user acquisition entirely and blocks booking for
   every existing user. Needs a look at the backend's RLS INSERT policies / insert-time
   triggers or functions for `auth.users`→profile creation and for `patient_requests`.
2. **No-doctor-required services can never be booked (B4)** — missing date/time picker for
   that path; the whole category (home-visit diagnostics, etc.) is a dead end as shipped.
3. **Home screen overlaps the status bar (A5)** — visible on every single login, both themes,
   worse at large font. Simple, high-visibility fix (WindowInsets padding).
4. **Home's "Try again" doesn't actually retry (H1)** — undermines the (otherwise good)
   offline-handling story; users who go offline and come back have no working way to recover
   short of leaving and re-entering the screen.
5. **Rotation discards unsaved Edit Profile text (H6)** — real data loss, reproducible.
6. **Doctor-account login gives zero feedback (A6)** — the block itself is correct and
   important to keep; just needs the missing message.
7. **"Dr. Dr." duplicate prefix on every doctor name (C1)** — cosmetic but visible everywhere a
   doctor's name appears; trivial fix.
8. **Service-mode selection invisible to TalkBack (D1)** — accessibility gap, straightforward
   fix (expose `selected`/`role` semantics on the mode-choice rows).
9. **Privacy Policy/Terms point to a placeholder domain (G)** — config fix, not code; swap in
   the real URL(s) before release.
10. **Avatar initials clip at 200% font scale (I2)** — lowest priority of the ten; only shows at
    an extreme (if fully supported) accessibility setting.

**Not included above, but worth noting for planning:**
- The app currently has **no deep-linking capability at all** (no scheme/host registered in the
  manifest) — not a bug, but worth flagging if deep links are expected before release (J3).
- Several test cases (Section E's full cross-app flow, My Requests population from a *new*
  booking, the double-submit guard, rejected/completed request actions, notification
  unread/badge behavior, slow-network simulation, session-expiry handling, and process-death
  form recovery) could not be exercised this run — most of them are waiting directly on fix #1
  above, and should be re-tested together with it.

---

## 6. Session environment notes (for context, not bugs)
- This machine had no local Supabase/Docker stack and no hosted dev backend wired in at the
  start of this session; your existing "Aarogya sanjeevani" hosted Supabase project was found
  to already have all the right seed accounts and was wired into `local.properties` for this
  run (`SUPABASE_URL_DEV`/`SUPABASE_ANON_KEY_DEV` — values never printed).
- The Supabase dashboard had a platform-wide incident banner for part of this session, which
  made it unreliable for directly inspecting server-side Auth/API logs to pin down the A2/D2
  root cause further — worth a follow-up there once the dashboard is stable.

---

## 7. Resolution — fix session of 2026-11-17/18

All ten items from the prioritized fix list above were worked, one per commit on `master`
(none pushed — a push still needs your go-ahead). Status below reflects what was actually
verified, not assumed.

| # | Issue | Status | Evidence |
|---|---|---|---|
| 1 | Sign-up/request-creation generic error (A2+D2) | **FIXED** | Root cause: `kotlinx.serialization`'s shared `Json` instance was also used to encode RPC bodies with `explicitNulls=false`, dropping declared-but-null parameters and breaking PostgREST's function-signature match (`PGRST202`), reproduced directly via curl before fixing. Also mapped GoTrue's `email_address_invalid`/`over_email_send_rate_limit` to friendly messages. Phone-verified: new sign-up and request creation both succeed. Commit `3666ef1`. |
| 2 | No-doctor services can't be booked (B2) | **FIXED** | Added a date strip + Morning/Afternoon/Evening time-window picker for no-doctor services; backend already accepted a self-scheduled slot, no migration needed. Phone-verified end-to-end: booked "Diagnostic Sample Collection at Home", appeared correctly in My Requests. Commit `baef921`. |
| 3 | Home overlaps the status bar (A5) | **FIXED** | Home had no `Scaffold`/`TopAppBar` of its own, so it never got the status-bar padding every other screen gets for free. Added `windowInsetsPadding(WindowInsets.statusBars)`. Also found and fixed the same gap in the shared `OfflineBanner` (visible on every screen while offline). Phone-verified before/after on Home, confirmed no regression on Services/Notifications. Commits `f55ecd8`, `ec1ea9d`. |
| 4 | "Try again" doesn't recover after connectivity returns (H1) | **FIXED** | `HomeViewModel` now observes `ConnectivityObserver` and auto-retries any section still in `Error` on a false→true transition, instead of relying solely on a manually-timed tap. Phone-verified end-to-end with a real airplane-mode toggle: all three sections recovered with no tap, within seconds of the first successful ping. Commit `86be76e`. |
| 5 | Rotation discards unsaved Edit Profile changes (H6) | **FIXED** | Not a missing `SavedStateHandle` — the ViewModel already survives rotation. The real bug: `EditProfileRoute`'s one-time `Load` replays on every Activity recreation (rotation's default behavior, since `MainActivity` declares no `configChanges`), silently re-fetching and overwriting the typed edit. Added an idempotency guard. Audited every other form; none had the same gap. Phone-verified portrait→landscape→portrait with a typed name surviving both rotations. Commit `3587910`. |
| 6 | Doctor-account login refused with no feedback (A6) | **FIXED** | The block and the message-resolution logic were already correct — the bug was a race: `signInAsPatient`'s own internal sign-in briefly flips global `AuthState` to `Authenticated` before the role check reverses it, and the app-wide auth gate reacting to that resets the whole back stack, destroying the login screen's event channel before it can show the rejection message. Moved the message to a channel (`AuthRepository.signInRejections`) that survives the reset. Phone-verified with the seeded `doctor.approved1@aarogya.test` account: the correct snackbar ("This looks like a doctor or admin account...") now appears. Commit `54302ad`. |
| 7 | "Dr. Dr." doubled prefix (C1) | **FIXED — phone-verified** | `doctor_profiles.title` ("Dr.") and the account's `full_name` ("Dr. Priya Sharma") are independent columns, both populated; two render sites blindly concatenated them. Added one shared `doctorDisplayName()` helper with 7 passing unit tests covering every case named (Dr., Dr, Prof., no title, already-prefixed). Confirmed on-device: Home's featured-doctor cards and the Doctor Detail screen both show "Dr. Priya Sharma" / "Dr. Vikram Rao" correctly, once each. Commit `2a91995`. |
| 8 | Service-mode selection not exposed to TalkBack (D1) | **FIXED — `AsChip` regression test now confirmed passing** | `ModeCard` genuinely lacked selection semantics (report's own uiautomator dump showed `selected="false"` on both options always) — fixed with the same `selectable`+`Role.RadioButton`+`stateDescription` pattern already proven for #2's time-window chips. Investigated the other three named spots: the Appearance/theme dialog already uses the identical pattern (no fix needed); slot/category chips route through the shared `AsChip`→Material3 `FilterChip`, whose own doc comment claims built-in TalkBack support — wrote `AsChipA11yTest.kt` to check this directly. Commit `93207c6`. **Follow-up this session, in order:** (1) found that no module set `testInstrumentationRunner`, so every `connectedDevDebugAndroidTest` run all session had been silently discovering **zero** tests and reporting a false `BUILD SUCCESSFUL` — fixed with `androidx.test.runner.AndroidJUnitRunner` (commit `0130527`), benefiting every instrumented test in the project. (2) That alone wasn't enough for Hilt-backed screens (`hiltViewModel()` needs the host Activity itself to be `@AndroidEntryPoint`) — added a `HiltTestRunner`/`HiltTestActivity` pair in the `debug` source set (not `androidTest`, which runs in a separate process the target app's Activity can't be launched from) and wired up `hilt-android-testing`. (3) With the suite actually running for the first time, it surfaced and this session fixed several more pre-existing, never-before-run bugs: a race in `AuthGateEffect` navigating before `NavHost` had set its graph when `authState` starts already-authenticated; two tests asserting on ambiguous duplicate text instead of scoping to the clickable element; a hardcoded "View a service" placeholder that doesn't exist on the real screen; and `PatientJourneyE2ETest` hardcoding `mode = ONLINE`/`address = null` when the resolved service/doctor combination can legitimately need `home_visit` (which requires a non-null address). **Confirmed: the full 43-test instrumented suite now passes twice in a row**, `AsChipA11yTest` included. |
| 9 | Privacy Policy/Terms point to a placeholder domain (G) | **BLOCKED — corrected finding** | **Supersedes an earlier, wrong diagnosis in this same row.** The first pass only fetched `aarogyasanjeevani.com` over plain HTTP/DNS and saw an unrelated site at `/privacy-policy`/`/terms` (404s), and concluded the pages didn't exist yet. Direct inspection of the Vercel dashboard (the project this domain should point to) shows that's wrong: `PrivacyPolicyPage.tsx`/`TermsPage.tsx` already exist, both routes (`/privacy-policy`, `/terms`) are correctly registered in `website/src/app/router.tsx`, and the latest Production Deployment is "Ready". The real problem is purely DNS: in Vercel → Settings → Domains, both `aarogyasanjeevani.com` and `www.aarogyasanjeevani.com` show **"Invalid Configuration"** — the domain's actual DNS at your registrar doesn't have the records Vercel expects, so requests to it never reach Vercel at all; they land on whatever unrelated site that domain's current DNS happens to point to (which is the site the first pass actually saw). `local.properties` was still **not** changed — nothing on the app side needs fixing. |
| 10 | Avatar initials clip at 200% font scale (I2) | **FIXED — phone-verified** | Initials used a plain `.sp` size, which scales with the system font-size setting, while the avatar's own circular container is fixed in `.dp` and never does — at large scale the text outgrew the circle exactly as reported. Derived the font size from the avatar's own `Dp` via `Density.toSp()` instead, extracted into a small pure function with 2 unit tests (rendered pixel size identical at 1x/2x font scale; still scales with the avatar's own size). Confirmed on-device at 200% system font scale: the Profile screen's "N" initial sits cleanly centered in its circle, no clipping. Commit `2750d2b`. |

**Infrastructure fixes made along the way, not numbered issues themselves:**
- The `OfflineBanner` status-bar overlap above (folded into #3's follow-up commit `ec1ea9d`).
- `RequestFormFlowTest.kt` (an existing androidTest) stopped compiling after #2 added a required
  `clock` constructor param — latent until the instrumented suite was ever actually compiled this
  session. Fixed in commit `607f2fd`.
- `:app-patient:connectedDevDebugAndroidTest` couldn't even build: several JUnit Jupiter 5.8.2
  jars (transitive) each ship their own `META-INF/LICENSE*`/`NOTICE*`, which AGP's resource
  merger refuses to pick between. Added a packaging exclude. Commit `37ef2eb`.
- No module set `testInstrumentationRunner`, so every connected-device test run all session had
  been silently discovering **zero tests** and reporting a false `BUILD SUCCESSFUL` — this had
  been masking every other instrumented test in the project, not just `AsChipA11yTest`. Fixed by
  adding `androidx.test.runner.AndroidJUnitRunner` to the shared convention plugin.
- Hilt-backed screens (anything calling `hiltViewModel()`) still couldn't run under that stock
  runner, since the host Activity itself needs to be `@AndroidEntryPoint`. Added a
  `HiltTestRunner`/`HiltTestActivity` pair in the `debug` source set (deliberately not
  `androidTest`, which launches in a separate process that can't host the target app's own
  Activity) and wired up `hilt-android-testing`.
- A real race in `AuthGateEffect`: its `LaunchedEffect` could call `navController.navigate()`
  before `NavHost` had called `setGraph()` on the same controller, whenever `authState` was
  already settled (`Authenticated`/`Unauthenticated`) on the very first composition rather than
  starting at `Loading` first. Fixed by waiting on `navController.currentBackStackEntryFlow.first()`
  before acting — this is a genuine production bug (a fast-resolving cached session could in
  principle hit it), not just a test artifact.
- Several pre-existing androidTest bugs, never caught before because nothing had ever actually
  executed: two tests asserting on ambiguous duplicate text (a chip/section-header sharing the
  same label) instead of scoping to the clickable element; `TabStateRestorationTest` clicking a
  "View a service" placeholder that doesn't exist on the real, backend-driven screen; and
  `PatientJourneyE2ETest` hardcoding `mode = ONLINE`/`address = null`, which the backend correctly
  rejects whenever the resolved doctor/service combination is `home_visit`-only (that mode
  requires a non-null address).
- A pre-existing ktlint/detekt conflict in `app-doctor`'s `NetworkConfigModule.kt` (one wanted a
  single-line body, the other flagged the resulting line as too long) — resolved by wrapping the
  call's arguments instead of the whole body.

**Confirmed, fully resolved this session:**
- The full 43-test `app-patient` instrumented suite — including `AsChipA11yTest` (#8) and the
  booking/request E2E suite — passes, twice in a row, with the phone unlocked and awake
  throughout.
- Issues #7 and #10 are now phone-verified directly (not just unit-tested): doctor names render
  correctly ("Dr. Priya Sharma", not doubled) on both Home and Doctor Detail, and avatar initials
  stay inside their circle at 200% system font scale.
- All unit tests pass project-wide; `ktlintCheck` and `detekt` are both clean project-wide.
- Issue #9's real root cause (DNS, not missing pages) is understood and documented above — you're
  handling the DNS records yourself, so no app-side action is needed.

Nothing from this session has been pushed to GitHub or to the live Supabase project — all
commits are local on `master`, and only the dev Supabase project was ever touched.
