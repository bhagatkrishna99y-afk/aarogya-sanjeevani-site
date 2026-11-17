# Patient App — Manual QA Checklist

Manual test cases for `app-patient`, organized by the areas named in the PRD quality
requirements. Each case lists **Preconditions**, **Steps**, and **Expected result**.
Run the full list once per release candidate, on at least one phone-class device
(~360dp wide) and one tablet-class device (~600dp+ wide), in both light and dark
system theme. Where a case is device/network-condition-specific, that's called out
in Preconditions rather than repeated per section.

Status legend for sign-off: `PASS` / `FAIL` / `BLOCKED` / `N/A`, plus the device,
Android version, and build (versionName + versionCode) tested.

---

## 1. Authentication

### 1.1 Sign up with a new email
- **Preconditions:** Fresh install or logged-out state. A real email address not yet registered in the target Supabase project.
- **Steps:** Welcome → Create account → enter full name, email, password, confirm password → check the consent box → Sign up.
- **Expected result:** If the project auto-confirms email, the app signs in and lands on Home. If confirmation is required, navigates to Check Inbox with the entered email shown, and no session is established yet.

### 1.2 Sign-up validation
- **Preconditions:** On the Sign Up screen.
- **Steps:** Leave full name blank, type a 1-character name, then an obviously-invalid email, then a password/confirm-password mismatch; tap Sign up each time without fixing the previous field.
- **Expected result:** Each field shows its own inline error only after it's been touched (blurred) or after a submit attempt; nothing submits while any field is invalid or consent is unchecked; no crash.

### 1.3 Sign up with an email already in use
- **Preconditions:** An email that already has an account.
- **Steps:** Sign up with that email.
- **Expected result:** A clear "email already in use" error is shown (not a generic failure); the form keeps everything the patient typed.

### 1.4 Log in with correct credentials
- **Preconditions:** An existing, confirmed patient account.
- **Steps:** Welcome → Log in → enter email/password → Log in.
- **Expected result:** Lands on Home, signed in as that patient.

### 1.5 Log in with wrong password
- **Steps:** Log in with a valid email and an incorrect password.
- **Expected result:** A clear "invalid credentials" message (snackbar); the email field keeps its value; the password field does not need to be re-typed from scratch unless the UX intentionally clears it; double-tapping Log in does not fire two sign-in attempts.

### 1.6 Log in with an unconfirmed account
- **Preconditions:** An account that signed up but never confirmed its email.
- **Steps:** Log in with that account's credentials.
- **Expected result:** A clear "please confirm your email" message, not a generic error.

### 1.7 Log in as a suspended account
- **Preconditions:** A patient account an admin has marked suspended.
- **Steps:** Log in with that account.
- **Expected result:** Signed back out automatically; a clear "account suspended" message; never lands on Home signed in.

### 1.8 Log in with the wrong role
- **Preconditions:** A doctor account (not a patient) on the same backend.
- **Steps:** Attempt to log in to the Patient app with that doctor's credentials.
- **Expected result:** Rejected with a clear message; never lands on Home signed in as that account.

### 1.9 Forgot password
- **Steps:** Welcome → Log in → Forgot password → enter a registered email → Send reset link.
- **Expected result:** Confirmation that a reset link was sent; re-entering the same email and resending doesn't error oddly; an unregistered email still shows a generic "sent" confirmation (never reveals whether an email is registered).

### 1.10 Double-tapping any auth submit button
- **Steps:** On Sign Up, Login, and Forgot Password, rapidly double-tap the submit button.
- **Expected result:** Only one network call fires; the button shows a loading state and is not clickable again until the first call resolves; no duplicate account/duplicate reset email.

---

## 2. Services

### 2.1 Browse the Services tab
- **Preconditions:** Signed in, online.
- **Steps:** Open the Services tab.
- **Expected result:** Category chips load (with a shimmer while loading), then the full service list for "All"; each service shows name and short description.

### 2.2 Filter by category
- **Steps:** Tap a category chip other than "All".
- **Expected result:** The list updates to only that category's services; the chip shows selected (not color-only — check with grayscale/TalkBack too); tapping the same chip again (or "All") clears the filter.

### 2.3 Open a service's detail
- **Steps:** Tap a service in the list.
- **Expected result:** Service Detail shows full description, duration, supported modes, and whether a doctor is required; a way to proceed to doctor discovery or availability depending on `requiresDoctor`.

### 2.4 Empty category
- **Preconditions:** A category with zero active services (seed data or backend state).
- **Steps:** Filter to that category.
- **Expected result:** A clear empty state ("No services match...") rather than a blank screen; no crash.

### 2.5 Services list survives a background/foreground cycle
- **Steps:** Open Services, background the app (Home button), wait a few seconds, foreground it again.
- **Expected result:** The list is still there (no flash back to a loading skeleton for data that hasn't changed); a silent background refresh never replaces good data with an error screen.

---

## 3. Doctors

### 3.1 Browse the Doctors Directory
- **Steps:** Navigate to Doctors Directory (from Home or a service that requires a doctor).
- **Expected result:** Doctor cards load with name, specialization, years of experience, accepting-requests status; pagination loads more as you scroll near the end.

### 3.2 Filter by specialization and consultation mode
- **Steps:** Apply a specialization filter, then a mode filter, then both together, then clear both.
- **Expected result:** The list updates correctly for each combination; clearing returns to the full list; no duplicate network calls fired on rapid filter changes.

### 3.3 Search doctors with no matches
- **Steps:** Filter to a combination with zero matching doctors.
- **Expected result:** A clear empty state, not a blank list or a spinner stuck forever.

### 3.4 Open a doctor's detail
- **Steps:** Tap a doctor card.
- **Expected result:** Doctor Detail shows full bio, qualifications, years of experience, and the services they offer; a way to view availability.

### 3.5 A doctor not accepting requests
- **Preconditions:** A doctor whose `acceptingRequests` is false.
- **Steps:** Open that doctor's detail and attempt to view availability.
- **Expected result:** Clearly communicated as not currently accepting requests (not just a quietly-disabled button with no explanation).

---

## 4. Availability

### 4.1 View a doctor's open slots
- **Preconditions:** A doctor accepting requests with open slots in the booking window.
- **Steps:** From Doctor Detail, choose a service, go to Availability.
- **Expected result:** A date strip shows selectable days; slots for the selected day load grouped by time-of-day period; at least one slot is selectable.

### 4.2 Select a slot and continue
- **Steps:** Tap an open slot, then Continue.
- **Expected result:** The tapped slot shows selected (check icon, not color alone); Continue becomes enabled only once a slot is chosen; proceeds to the request form.

### 4.3 Change the selected date
- **Steps:** Select a slot on one date, then switch to a different date in the strip.
- **Expected result:** The previous selection is cleared (no stale "selected" slot carried to a different day); Continue becomes disabled again until a new slot is picked on the new date.

### 4.4 A day with no open slots
- **Steps:** Navigate to a date with zero open slots for that doctor.
- **Expected result:** A clear empty state for that day (e.g. "No times are available on this date"), with the date strip still usable to pick another day.

### 4.5 Booking horizon limit
- **Preconditions:** Know the backend's `max_horizon_days` setting.
- **Steps:** Try to navigate the date strip past that many days out.
- **Expected result:** The strip does not offer dates beyond the allowed horizon.

### 4.6 A slot taken by someone else between viewing and submitting
- **Preconditions:** Two sessions (or a way to book the same slot from elsewhere) able to race for the same slot.
- **Steps:** Select a slot in Availability, then have the other session book that exact slot, then submit the request.
- **Expected result:** A clear "slot no longer available" message (AS001/AS019), returning to Availability with a refreshed slot list rather than crashing or silently double-booking.

---

## 5. Request creation

### 5.1 Fill and submit a request
- **Preconditions:** A selected slot from Availability.
- **Steps:** Fill in mode (if choosable), contact phone, address (for home visits), optional notes → Review → Submit request.
- **Expected result:** Review shows an accurate summary of everything entered; after submit, lands on a success screen with the new request's status (Requested) and a way to view it in the Requests tab.

### 5.2 Request form validation
- **Steps:** Try to continue/submit with no mode selected, an invalid phone number, a blank address on a home-visit request, and an over-length notes field.
- **Expected result:** Each is flagged with a specific, actionable message; submit is blocked until all are resolved; the already-valid fields are not reset by fixing one invalid field.

### 5.3 A request that requires an address only for home visits
- **Steps:** Switch mode between in-person/online/home-visit while filling the form.
- **Expected result:** The address field only appears/is only required when Home Visit is selected; switching away from Home Visit does not keep a stale address-required error on screen.

### 5.4 Double-tapping Submit
- **Steps:** On the Review screen, rapidly double-tap "Submit request".
- **Expected result:** Only one request is created; the button shows a loading state and disables itself while the call is in flight.

### 5.5 Submitting while offline
- **Preconditions:** Airplane mode on, mid-form (after selecting a slot).
- **Steps:** Fill the form and attempt to submit.
- **Expected result:** A clear network-error message with a way to retry; the form's own input is preserved; no crash.

### 5.6 Hitting a per-patient request limit
- **Preconditions:** A patient account already at the backend's open-request limit.
- **Steps:** Attempt to submit another request.
- **Expected result:** A clear "limit exceeded" message, not a generic failure.

---

## 6. Cancellation

### 6.1 Cancel a requested/accepted/confirmed request
- **Preconditions:** An active request in a cancellable status.
- **Steps:** Requests tab → open the request → Cancel → enter a reason (if required) → Confirm.
- **Expected result:** A confirmation dialog appears before anything happens; after confirming, the request's status updates to Cancelled and the timeline reflects it; cancelling does not require leaving and re-entering the screen to see the update.

### 6.2 Dismissing the cancel confirmation
- **Steps:** Open the cancel dialog, then dismiss/cancel it instead of confirming.
- **Expected result:** The request is untouched; no network call was made.

### 6.3 Cancelling a request that was just accepted by the doctor (race)
- **Preconditions:** A request in Requested status that a doctor accepts at the same moment the patient tries to cancel it with a stale "Requested" assumption.
- **Steps:** Open the request detail, let the status change happen on the backend (another session acting as the doctor), then confirm cancel.
- **Expected result:** A clear conflict message (AS016) is shown; the detail screen refreshes to the real current status rather than applying a cancellation that no longer makes sense; no crash.

### 6.4 Double-tapping Cancel's confirm button
- **Steps:** Rapidly double-tap the confirm button in the cancel dialog.
- **Expected result:** Only one cancellation request is sent; the dialog closes once, cleanly.

### 6.5 Attempting to cancel an already-completed/cancelled/rejected request
- **Steps:** Open a request already in a terminal status.
- **Expected result:** No cancel action is offered at all for a terminal-status request.

---

## 7. Notifications

### 7.1 Receiving a status-change notification
- **Preconditions:** A request whose status changes (accepted/rejected/confirmed/completed/cancelled by the doctor side).
- **Steps:** Trigger the status change, then open the Notifications tab.
- **Expected result:** A new notification appears describing the change; the bottom-bar/Home bell badge count increases.

### 7.2 Tapping a notification
- **Steps:** Tap an unread notification tied to a request.
- **Expected result:** It's marked read (badge count decreases) and navigates to that request's detail; if the related request was deleted/inaccessible, a friendly "not found" state instead of a crash.

### 7.3 Mark all as read
- **Preconditions:** Multiple unread notifications.
- **Steps:** Tap "Mark all as read".
- **Expected result:** All notifications show read; the badge count drops to zero; double-tapping the button doesn't cause any visible glitch or duplicate network calls.

### 7.4 Mark-all-read failure
- **Preconditions:** Simulated/forced network failure during mark-all-read (or airplane mode toggled mid-tap).
- **Steps:** Tap "Mark all as read" while offline.
- **Expected result:** The optimistic "all read" view rolls back to the real (still-unread) state, with an error message; it doesn't get stuck showing a wrong read state.

### 7.5 Notifications list pagination and refresh
- **Preconditions:** More than one page's worth of notifications.
- **Steps:** Scroll to load more; pull to refresh.
- **Expected result:** Loads additional pages correctly; pull-to-refresh updates the list without losing scroll position unexpectedly; a refresh failure on top of existing content keeps the existing content visible with an error message, not a blank/error screen.

### 7.6 Empty notifications
- **Preconditions:** A brand-new account with no notifications yet.
- **Steps:** Open the Notifications tab.
- **Expected result:** A clear empty state, not a blank screen or an infinite shimmer.

---

## 8. Profile

### 8.1 View profile
- **Steps:** Open the Profile tab.
- **Expected result:** Shows full name (or a prompt to add one), sign-in email (read-only), phone if set, member-since date, and avatar (initials fallback if no photo).

### 8.2 Edit profile — valid change
- **Steps:** Edit Profile → change full name and/or phone → Save.
- **Expected result:** Save is disabled until something actually changes; after saving, returns to Profile showing the new values; the change persists after leaving and returning to the screen.

### 8.3 Edit profile — validation
- **Steps:** Clear the full name entirely, then type a 1-character name, then a 101+ character name, then an invalid phone.
- **Expected result:** Each is flagged with a specific message; Save stays disabled until resolved.

### 8.4 Discard changes
- **Steps:** Change a field in Edit Profile, then tap back (hardware/gesture back or the top-bar back arrow).
- **Expected result:** A "discard changes?" confirmation appears; confirming discards the edit and returns to Profile unchanged; dismissing keeps you on the edit screen with the typed value intact.

### 8.5 Edit profile — save failure
- **Preconditions:** Airplane mode on, mid-edit.
- **Steps:** Change a field and tap Save.
- **Expected result:** A clear error message; the typed values are not lost; Save becomes available again (not stuck in a permanent loading state).

### 8.6 Avatar upload (only if `AVATAR_UPLOAD_ENABLED` is on for this build)
- **Steps:** Tap the avatar, pick a photo.
- **Expected result:** Shows an uploading indicator on the avatar badge, then the new photo; a too-large photo is compressed automatically; if even the lowest-quality compression doesn't fit, a clear "choose a smaller photo" message, not a silent failure.

### 8.7 Appearance setting
- **Steps:** Profile → Appearance → choose Light, Dark, and System in turn.
- **Expected result:** The whole app's theme changes immediately to match; the choice persists after fully closing and reopening the app.

### 8.8 Contact support / request account deletion
- **Steps:** Tap "Contact support", then separately tap "Request account deletion".
- **Expected result:** Each opens the device's email client with the right address pre-filled (support request vs. deletion request); the deletion screen explains that self-service deletion isn't available and never offers a fake "delete" button that does nothing real.

### 8.9 Log out
- **Steps:** Profile → Log out → confirm in the dialog.
- **Expected result:** A confirmation dialog appears first; confirming signs the patient out and returns to the Welcome/Login screen; dismissing the dialog keeps the session active.

---

## 9. Session restoration

### 9.1 Cold start while already signed in
- **Preconditions:** Signed in, then the app process is killed (not just backgrounded) by the OS or manually.
- **Steps:** Relaunch the app from the home screen/app drawer.
- **Expected result:** Lands directly on Home signed in, with no visible flash of the Welcome/Login screen first.

### 9.2 Cold start while signed out
- **Preconditions:** Signed out (or never signed in).
- **Steps:** Launch the app.
- **Expected result:** Lands on Welcome/Login; never shows any authenticated screen even momentarily.

### 9.3 Rotation / process death mid-form
- **Steps:** Start filling the Sign Up or Request form, rotate the device (or trigger "Don't keep activities" in Developer Options to force process death), then return to the app.
- **Steps (cont.):** Observe whether in-progress non-sensitive fields survive.
- **Expected result:** Fields intentionally persisted across process death (e.g. sign-up email, full name) survive; passwords never do (by design — confirm this is the case, not a bug).

### 9.4 Expired or invalidated session mid-use
- **Preconditions:** A way to force-expire or revoke the current session server-side.
- **Steps:** While signed in and on any screen, invalidate the session, then perform an action that hits the network (pull to refresh, navigate to a new screen).
- **Expected result:** The app detects the invalid session and returns to Welcome/Login in a reasonable time, rather than leaving the patient stuck on a screen that fails every retry with no way back to login. *(Known gap as of this writing — see the engineering audit; if still unresolved, record as a tracked known issue, not a new bug.)*

### 9.5 Tab state restoration
- **Steps:** Switch to the Requests tab, select the "Past" sub-tab, scroll down, then switch to another bottom-bar tab and back.
- **Expected result:** Returns to the same tab/scroll position rather than resetting to "Active" at the top.

---

## 10. Logout

### 10.1 Logout clears the session everywhere
- **Steps:** Log out, then try to use the device/browser back gesture to return to an authenticated screen.
- **Expected result:** Cannot navigate back into any authenticated screen after logout; relaunching the app also lands on Welcome/Login, not Home.

### 10.2 Logout resets app-wide state
- **Preconditions:** Unread notifications present before logging out.
- **Steps:** Note the unread badge count, log out, log back in as the same or a different patient.
- **Expected result:** The badge/notification state reflects the newly-signed-in account, not leftover state from the previous session.

### 10.3 Logout while a background poll is in flight
- **Preconditions:** The app has been in the foreground long enough for the notifications poll to be active.
- **Steps:** Log out right as a poll would fire (or shortly after).
- **Expected result:** No crash, no stray network call attributed to the now-signed-out session; polling stops.

---

## 11. Error handling

### 11.1 Airplane mode on any main list screen
- **Steps:** Enable airplane mode, then open/refresh Home, Services, Doctors Directory, Requests, Notifications in turn.
- **Expected result:** Each shows a clear "no connection" error state with a retry action; retrying after disabling airplane mode succeeds without needing to restart the app.

### 11.2 Throttled/slow network
- **Preconditions:** Network throttled to a very slow speed (Android Studio's network profiler, a proxy, or a real poor-signal environment).
- **Steps:** Navigate through the five main journeys.
- **Expected result:** Loading states (skeletons) show for as long as needed, never a frozen/unresponsive UI; no request appears to silently hang forever with no feedback.

### 11.3 Backend 500 / unexpected error
- **Preconditions:** A way to force a server error (a backend maintenance window, a deliberately broken RPC in a test environment, etc.).
- **Steps:** Trigger the error on any screen's load or submit action.
- **Expected result:** A generic but clear error message with a retry action where applicable; the app never crashes outright.

### 11.4 Empty datasets everywhere
- **Steps:** With a freshly-seeded or newly-created account that has no requests/notifications/avatar, visit every list screen.
- **Expected result:** Every one shows a purpose-built empty state (not a blank screen, not an error, not an infinite spinner).

### 11.5 A genuinely unexpected crash
- **Preconditions:** Hard to trigger deliberately; note any crash encountered during the rest of this checklist here.
- **Steps:** N/A — opportunistic.
- **Expected result:** If one occurs, the app does not get stuck in a crash loop on relaunch; file it as a bug with repro steps regardless of how it was found.

---

## 12. Light and dark UI

Run once in light, once in dark (device system setting), on both a ~360dp phone and
a ~600dp+ tablet (4 passes total). Use this section as a quick visual sweep, not a
replacement for exercising every screen functionally above.

### 12.1 Every main screen in both themes
- **Steps:** Visit Welcome, Login, Sign Up, Home, Services, Doctors Directory, Doctor Detail, Availability, Request Form/Review/Success, Requests List, Request Detail, Notifications, Profile, Edit Profile.
- **Expected result:** Text is legible against its background in both themes (no light-on-light or dark-on-dark text); status colors (StatusChip) remain distinguishable; no screen looks "half-themed" (a stray hardcoded white/black background clashing with the rest).

### 12.2 Switching theme live
- **Steps:** With the app open on a content-heavy screen, change the device's system theme (or the in-app Appearance setting) without restarting the app.
- **Expected result:** The screen re-themes immediately and correctly, with no visual glitches or stuck colors from the previous theme.

### 12.3 Tablet-width layout
- **Steps:** On a ~600dp+ wide device/emulator, open several screens including a long one (Services list, Requests list).
- **Expected result:** Content is capped to a readable width and centered rather than stretching edge-to-edge uncomfortably; nothing overlaps or gets clipped at this width.

### 12.4 Small-width layout
- **Steps:** On a ~360dp wide device, open the same set of screens, including ones with chip rows and multi-button rows (Doctors Directory filters, Availability's date strip and slot grid).
- **Expected result:** Nothing overflows the screen edge; horizontally-scrolling rows scroll smoothly; buttons/text don't get clipped or wrap awkwardly.

---

## 13. Accessibility

### 13.1 Accessibility Scanner pass
- **Preconditions:** Google's Accessibility Scanner app installed on the test device.
- **Steps:** Run a scan on Welcome/Login, Home, Doctors Directory → Availability, Request Form → Review → Success, and Profile.
- **Expected result:** No new issues beyond a documented, accepted baseline; any touch target under 48dp, low-contrast text, or missing label is filed as a bug.

### 13.2 TalkBack walkthrough — sign-up/login
- **Steps:** Enable TalkBack, swipe through Welcome, Sign Up, and Login end to end, including submitting the form.
- **Expected result:** Every control is announced with a clear, actionable label; field errors are announced when they appear; focus order follows the visual top-to-bottom order; the submit button's loading state is announced.

### 13.3 TalkBack walkthrough — booking a request
- **Steps:** With TalkBack on, go from Home through Doctors Directory, Doctor Detail, Availability (select a date and a slot), Request Form, Review, Submit.
- **Expected result:** Slot selection state is announced (selected/not selected), not conveyed by color alone; the date strip's days are announced with their full date, not just a number; loading states announce "Loading" once per screen, not silently or once per shimmer bar.

### 13.4 TalkBack walkthrough — Requests, Notifications, Profile
- **Steps:** With TalkBack on, open Requests (both tabs), Request Detail, cancel a request via the dialog, open Notifications, mark one read, open Profile, open Edit Profile.
- **Expected result:** Every screen's title is reachable via TalkBack's heading-navigation gesture; status chips announce their status as text ("Status: Accepted"), not just a color; the cancel/logout confirmation dialogs are announced as dialogs with both options reachable.

### 13.5 200% font scale
- **Preconditions:** Device system font size set to the maximum (or 200% if the device allows a precise value).
- **Steps:** Visit the same screens as 13.3/13.4.
- **Expected result:** Text reflows/wraps rather than getting clipped; buttons grow to fit their (now larger) label rather than clipping it; no critical information becomes unreadable or inaccessible at this scale.

### 13.6 Non-color-only status communication
- **Steps:** With the device in grayscale (Settings → Accessibility → Color correction/grayscale, or a simulated color-blindness mode), review every status chip (request status, doctor status) and every selectable chip (slot, category, filter).
- **Expected result:** Every status/selection is still distinguishable without color — via an icon, a check mark, strike-through text, or a text label — not by hue alone.

### 13.7 Minimum touch target size
- **Steps:** Spot-check every icon-only button (back arrow, notification bell, password-visibility toggle, avatar camera badge) with the Accessibility Scanner's "touch target size" check or a measurement overlay.
- **Expected result:** Every tappable control measures at least 48×48dp, even when the visible icon inside it is smaller.
