# RPC reference: doctor lifecycle and admin management

Every function below is `SECURITY DEFINER`, `SET search_path = ''`, and
checks its own authorization inside the function body (never relies on
RLS alone) -- consistent with `docs/security-principles.md`. Error codes
are documented in full, with exact meanings, in `docs/error-codes.md`;
this file focuses on caller roles, side effects, and the decisions behind
each function. Added in `20261112000010_doctor_profile_management.sql`
(doctor-side) and `20261112000011_admin_management.sql` (admin-side)
unless noted otherwise.

## Doctor-side

### `submit_doctor_profile(p_title, p_specialization, p_qualifications, p_bio, p_years, p_license_number, p_modes service_mode[], p_slot_duration) returns void`

**Caller:** any signed-in user whose `profiles.role = 'doctor'`. Rejects
with `AS022_NOT_A_DOCTOR_ACCOUNT` otherwise, `AS023_INVALID_DOCTOR_PROFILE_FIELDS`
for bad input.

**Behavior:**
- No existing `doctor_profiles` row -> inserts one with `status = 'pending'`.
- Existing row with `status = 'rejected'` -> updates fields, resets status
  to `pending`, clears `rejection_reason`/`reviewed_by`/`reviewed_at` (this
  is "resubmission").
- Existing row with `status = 'approved'` or `'suspended'`, and
  `license_number` or `specialization` differ from the current values ->
  updates fields, resets status to `pending` for re-review, clears
  `reviewed_by`/`reviewed_at`.
  - **Documented rule:** the prompt that specified this function tied the
    re-review trigger to "an approved doctor" specifically; it's applied to
    `suspended` doctors too here, since a suspended account changing its
    most sensitive credential fields without a fresh admin look seemed
    like the wrong default to leave open.
- Existing row, `approved`/`suspended`, and neither `license_number` nor
  `specialization` changed -> updates fields, status unchanged. Editing
  title/qualifications/bio/years/consultation_modes/slot_duration never
  forces a re-review.
- Existing row, still `pending` -> updates fields, status unchanged.

**Side effects:** every status-changing path writes one `audit_log` row
(`doctor_profile_submitted` / `doctor_profile_resubmitted` /
`doctor_profile_changed_pending_review`), `actor_id` = the doctor
themselves. A benign field edit that doesn't change status writes nothing.

**Not touched:** `submit_doctor_application` (the original, narrower
application-intake function, still used by the Doctor app) -- see that
migration's header comment for why this is a new function rather than a
replacement.

### `set_doctor_services(p_service_ids uuid[]) returns setof doctor_services`

Pre-existing (`20261111120008_rpc_functions.sql`), unchanged by this work
-- already matches this prompt's spec exactly (atomic replace, every
service must be `requires_doctor` and `is_active`). Caller: an approved
doctor (`is_approved_doctor(auth.uid())`).

### `set_doctor_availability(p_windows jsonb) returns void`

**Caller:** an approved doctor. `AS024_ONLY_APPROVED_DOCTORS` otherwise.

`p_windows` is a JSON array of `{day_of_week, start_time, end_time,
slot_duration_minutes}`. Validates every window (`AS025_INVALID_AVAILABILITY_WINDOW`
for a bad day/time/slot-duration, or a window shorter than its own slot
duration), rejects overlaps within the submitted batch before touching the
table (`AS026_AVAILABILITY_OVERLAP`, with the `doctor_weekly_availability_no_overlap`
EXCLUDE constraint as a fallback), then atomically deletes and re-inserts
the doctor's full weekly schedule.

**Not touched:** `set_weekly_availability` (same shape, different name,
still used by the Doctor app) -- see the migration header for why this is
additive.

### `set_accepting_requests(p_value boolean) returns void`

**Caller:** an approved doctor. `AS024_ONLY_APPROVED_DOCTORS` otherwise.
Sets `doctor_profiles.accepting_requests`.

## Admin-side

Every function below checks `is_admin()` first (`AS027_NOT_AUTHORIZED`
otherwise) and writes to `audit_log` via the internal `log_audit()` helper
(never directly exposed to any role).

### `admin_set_doctor_status(p_doctor_id, p_new_status doctor_status, p_reason text) returns doctor_profiles`

Pre-existing (`20261111120008_rpc_functions.sql`), upgraded in place --
same name, same parameters, so neither app nor website call sites needed
to change. See the migration header for the full "upgraded in place"
rationale and the suspension-cascade decision.

**Allowed transitions:** `pending->approved`, `pending->rejected`,
`approved->suspended`, `suspended->approved`. Anything else ->
`AS029_INVALID_DOCTOR_STATUS_TRANSITION`. Note `rejected->approved` is
*not* allowed -- a rejected doctor must resubmit via `submit_doctor_profile`
(moving themselves to `pending`) before an admin can approve them again.
`rejected` requires a non-empty `p_reason`.

**Side effects:**
- Row locked with `FOR UPDATE` before validating.
- `doctor_profiles` updated (status, `rejection_reason`, `reviewed_by`,
  `reviewed_at`; `p_reason` also appended to `admin_notes` when given
  alongside a non-rejection transition).
- Approving also sets `profiles.role = 'doctor'`.
- One `audit_log` row (`admin_set_doctor_status`).
- One notification to the doctor (`doctor_approved`/`doctor_rejected`/`doctor_suspended`).
- **Suspension cascade**, only when `p_new_status = 'suspended'`:
  - Every FUTURE (`scheduled_start > now()`) `requested` request for that
    doctor is force-rejected directly (not via `transition_request_status`,
    whose `requested->rejected` is intentionally doctor-only -- this is a
    different authorization context). The existing
    `log_request_status_change` trigger still fires, so the patient gets
    the normal `request_rejected` notification and the history row is
    still written. One `audit_log` row per request
    (`doctor_suspension_auto_rejected_request`).
  - Every FUTURE `accepted`/`confirmed` request is left alone (not
    cancelled -- an admin should decide how to handle an already-confirmed
    appointment, not have the system cancel it silently). The patient gets
    a `system` notification that their appointment is under review, and
    one `audit_log` row per request (`accepted_request_flagged_for_review`)
    so admins can find them. See the migration header for why this uses
    `audit_log` rather than a new "flagged" column/queue table.
  - Past requests, of any status, are untouched.

### `admin_set_user_role(p_user_id, p_new_role user_role) returns void`

New. **Guard:** refuses to change the role of the last remaining `admin`
account away from `admin` -- `AS031_CANNOT_DEMOTE_LAST_ADMIN`. Also
refuses a no-op (`AS029_INVALID_DOCTOR_STATUS_TRANSITION`, "already has
this role" -- the same code as the doctor-status case, since both mean
"that transition doesn't apply here"). Writes one `audit_log` row
(`admin_set_user_role`).

**Not touched:** `admin_assign_admin_role` (promotion-to-admin only, still
used by the website's "Make admin" button) -- this is a new, more general
function for any role change, additive per the same reasoning as the
doctor-side functions above.

### `admin_set_user_suspended(p_user_id, p_value boolean) returns void`

New. Sets `profiles.is_suspended`. No last-admin guard (the prompt scoped
that guard to role changes only) and no suspension cascade (that's
`admin_set_doctor_status`'s concern, for the separate `doctor_profiles.status`
concept -- account-level suspension and doctor-approval-status suspension
are deliberately different things in this schema). Writes one `audit_log`
row (`admin_set_user_suspended`).

### `admin_upsert_service_category(p_id, p_name, p_description, p_icon, p_is_active, p_sort_order) returns uuid`

New. `p_id = null` inserts; otherwise updates. `slug` is generated
automatically by the existing `service_categories_generate_slug` trigger.
`AS032_INVALID_CATALOG_FIELDS` for a missing name or a constraint
violation on the table; `AS033_CATALOG_ENTITY_NOT_FOUND` for an unknown
`p_id` on update. Writes one `audit_log` row.

### `admin_set_category_active(p_category_id, p_is_active) returns void`

New. Toggles `service_categories.is_active`. `AS033_CATALOG_ENTITY_NOT_FOUND`
if the category doesn't exist. Writes one `audit_log` row.

### `admin_upsert_service(p_id, p_category_id, p_name, p_description, p_short_description, p_requires_doctor, p_duration_minutes, p_supported_modes, p_is_active, p_sort_order) returns uuid`

New. Same insert/update shape as the category upsert. Validates the
category exists and at least one supported mode is given
(`AS032_INVALID_CATALOG_FIELDS`). Writes one `audit_log` row.

### `admin_set_service_active(p_service_id, p_is_active) returns void`

New. Toggles `services.is_active`. **Disabling a service never deletes or
otherwise touches existing requests** -- `requests.service_id` is `ON
DELETE RESTRICT` and this function only ever updates the one `is_active`
column, so request history stays exactly as it was. `AS033_CATALOG_ENTITY_NOT_FOUND`
if the service doesn't exist. Writes one `audit_log` row.

### `admin_list_doctors(p_status, p_limit, p_offset) returns table(...)`

New read-convenience wrapper (admin RLS already permits reading these
tables directly; this adds `is_admin()` as defense in depth plus
pagination and the join to `profiles`). Returns `user_id, full_name,
email, phone, specialization, license_number, status, years_experience,
is_suspended, created_at, reviewed_at`, optionally filtered by `p_status`,
newest first.

### `admin_list_requests(p_status, p_doctor_id, p_service_id, p_date_from, p_date_to, p_limit, p_offset) returns table(...)`

New read-convenience wrapper. Returns request fields plus the patient's
and doctor's names and the service name, filterable by any combination of
the parameters, ordered by `scheduled_start` descending.

### `admin_get_request_detail(p_request_id) returns table(...)`

New read-convenience wrapper returning one request's full detail (every
column, plus patient/doctor names and phone) with its complete
`request_status_history` bundled as a `status_log` JSONB array column, so
a caller gets the request and its full history in one round trip.
`AS015_REQUEST_NOT_FOUND` if the request doesn't exist (same code as
`transition_request_status` uses for the same condition).

## Summary of what's additive vs. upgraded-in-place

| Old function (unchanged) | New function | Relationship |
|---|---|---|
| `submit_doctor_application` | `submit_doctor_profile` | Additive -- different signature, different purpose (intake vs. full profile management) |
| `set_weekly_availability` | `set_doctor_availability` | Additive -- same shape, fuller validation, different name the prompt asked for |
| `admin_assign_admin_role` | `admin_set_user_role` | Additive -- promotion-only vs. any role change with a last-admin guard |
| `admin_set_doctor_status` | *(same name)* | **Upgraded in place** -- same name/params, zero app changes, tightened transitions + audit log + suspension cascade |
| `set_doctor_services` | *(same name)* | **Unchanged** -- already matched this prompt's spec exactly |

Every "additive" row above means the old function keeps working exactly
as the apps already use it; migrating a given screen onto the new
function is a separate, not-yet-done task.
