# Play Store Listing — Aarogya Sanjeevani (Patient)

Draft copy for the Play Console listing. **Fill in the bracketed placeholders**
before submitting — none of them should ship as-is. The Data Safety section below
is written to match what this app *actually* collects as of this build; re-check it
against the code any time a new feature starts reading/writing a new kind of data
(especially if avatar upload or real crash reporting are ever turned on).

---

## Short description (max 80 characters)

```
Book doctor visits, track requests, and manage your care — all in one app.
```
(76 characters)

---

## Full description (max 4000 characters)

```
Aarogya Sanjeevani makes it simple to find a doctor, book a visit, and stay on
top of your care — all from your phone.

BROWSE SERVICES AND DOCTORS
See what services are available, filter doctors by specialization and
consultation mode (in person, home visit, or online), and read each doctor's
experience and qualifications before you book.

BOOK ON YOUR SCHEDULE
Pick an open time slot that works for you. Choose how you'd like to be seen —
in person, at home, or online — and add any notes your doctor should know
ahead of time.

TRACK EVERY REQUEST
Follow your request from submitted to accepted to confirmed, with a clear
timeline at every step. Need to change your mind? Cancel directly from the
app, right up until it's no longer possible to.

STAY IN THE LOOP
Get notified the moment your request's status changes, so you're never left
wondering whether your visit is confirmed.

YOUR PROFILE, YOUR WAY
Keep your contact details up to date, choose light or dark appearance, and
reach support in one tap if you ever need help.

Aarogya Sanjeevani is built for patients who want a straightforward way to
request care and stay informed — nothing more complicated than it needs to
be.

[ADD: one sentence naming the actual clinic/provider network this app serves,
once that's finalized — e.g. "Built for patients of [Clinic Name]."]
```
(1,116 characters — well under the 4000 limit; room to add provider-specific
details once finalized.)

---

## Privacy policy URL

```
[ADD: the deployed website's /privacy-policy URL — see BuildConfig.WEBSITE_BASE_URL;
no production domain is registered yet as of this build, see that field's own
doc comment in app-patient/build.gradle.kts. Required before this listing can
go live — Play Console will not accept a submission without one.]
```

---

## Data Safety form answers

Based on an audit of what this build's code actually reads, writes, and sends
(not what a future feature might add) — `core:data`'s repositories and DTOs, and
the dependency list in `app-patient/build.gradle.kts`/`gradle/libs.versions.toml`.

### Does your app collect or share any of the required user data types?
**Yes.**

### Data collected

| Category | Data type | Collected? | Shared? | Purpose | Required or optional |
|---|---|---|---|---|---|
| Personal info | Name | Yes | No | App functionality (account, display name) | Required |
| Personal info | Email address | Yes | No | App functionality (account, sign-in) | Required |
| Personal info | Phone number | Yes | No | App functionality (contact for a booked visit) | Optional |
| Personal info | Address | Yes | No | App functionality (home-visit requests only) | Optional, only if home-visit mode is chosen |
| Photos | Photos | Only if avatar upload is enabled for this build* | No | App functionality (profile picture) | Optional |
| App activity | App interactions | Yes | No | App functionality (which service/doctor/time slot was requested, request notes) | Required to use the booking feature |
| App info and performance | Crash logs, diagnostics | **Not currently collected** (see note below) | No | — | — |

\* As of this build, avatar upload is behind `FeatureFlags.AVATAR_UPLOAD_ENABLED`,
shipped **off**. If a future build turns it on, update this row to "Yes" before
that build is submitted.

**Crash logs / diagnostics note:** this build includes a crash-reporting hook
(`CrashReportingTree`) that is wired to a feature flag (`CRASH_REPORTING_ENABLED`),
shipped **off**, and has no real reporting backend connected even when the flag is
on — nothing is sent anywhere. If a real crash-reporting backend (Crashlytics or
otherwise) is ever connected, **this table must be updated first** — add an
"App info and performance" row, mark it collected and shared with that backend,
and describe what it excludes (this app's own `CrashReportingTree` never forwards
free-text log messages, only exception type/stack trace and a tag, specifically so
personal data never ends up in a crash report — but the Data Safety form still
needs to disclose the category once it's actually active).

### Is all of the user data collected by your app encrypted in transit?
**Yes.** The app's network security config (`network_security_config.xml`)
explicitly forbids cleartext traffic; every request goes over HTTPS to the
backend (Supabase).

### Do you provide a way for users to request that their data be deleted?
**Yes, but not self-service in-app.** The Profile screen's "Request account
deletion" opens a pre-filled email to support; there is no in-app deletion flow
in this build. Answer the Play Console's data-deletion question accordingly
(a web form or support-contact deletion path, not an in-app one) and link
whatever deletion-request process is documented for your support team —
`[ADD: that process's documentation link, if one exists separately from the
mailto flow]`.

### Third parties data is shared with
The app's backend, Supabase, processes all of the above as this app's own
service-provider infrastructure (not a separate third party using the data for
its own purposes). No advertising, analytics, or tracking SDK is included in
this app (confirmed: no `analytics`/`ads`/`firebase` dependency anywhere in
`gradle/libs.versions.toml` or either app module's `build.gradle.kts` as of
this build) — answer accordingly in the "third-party sharing" section (no data
shared with third parties for their own purposes).

### Permissions this app requests
- `android.permission.INTERNET` — required for all app functionality (talking to
  the backend).
- `android.permission.ACCESS_NETWORK_STATE` — used only to show the offline
  banner when there's no connection; never sent anywhere.

No camera, location, contacts, storage, microphone, or SMS permission is
requested by this app.
