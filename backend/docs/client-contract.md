# Client contract

For app developers (website and Android/Kotlin) integrating against this
Supabase project. For each screen-level need: which view/RPC to call, its
parameters, the shape of what comes back, and every error code it can
raise with the friendly message to show.

**Conventions that apply everywhere below:**

- All reads/writes go through the Supabase client SDK (PostgREST for
  tables/views, `.rpc()` for functions) using the **anon or authenticated
  publishable key** -- never the service role key in a client app.
- Every mutating RPC and most read RPCs require a signed-in user
  (`auth.uid()`); calling one while signed out raises
  `AS000_NOT_AUTHENTICATED`. The small number of RPCs callable while
  signed out (`list_approved_doctors`, `get_doctor_public_profile`,
  `is_admin`, `is_approved_doctor` -- all **legacy**, see the note at the
  bottom) and the public `SELECT` policies on `service_categories`,
  `services`, `doctors_public`, `doctor_services_public` are the only
  anonymous-accessible surface; everything else needs a session.
- Every custom error is raised as `RAISE EXCEPTION '<CODE>: message' USING
  ERRCODE = 'P0001'` -- **do not branch on SQLSTATE**, it is always
  `P0001`/`23505`/`23P01` etc. depending on what was caught, never a
  per-code SQLSTATE. Parse the `<CODE>` prefix (everything before the
  first `:` in the error message your SDK surfaces as
  `error.message`/`PostgrestException.message`) and map it to a localized
  string client-side using the tables below. The English text after the
  colon is a reasonable fallback, not a stable contract. Full registry:
  `docs/error-codes.md`.
- Timestamps are `timestamptz` -- always UTC on the wire; format them in
  the user's local timezone client-side. `requests.requested_date` is a
  generated `date` column in `Asia/Kolkata`, for date-grouping only.

---

## 1. Signup

No RPC -- signup is `supabase.auth.signUp()` with user metadata; a
`handle_new_user()` trigger on `auth.users` creates the matching
`public.profiles` row automatically.

| Field | Where | Effect |
|---|---|---|
| `full_name` | `options.data.full_name` | Copied into `profiles.full_name`. |
| `signup_role` | `options.data.signup_role` | If exactly `"doctor"`, `profiles.role` is set to `doctor` immediately at signup. Omit it (or any other value) for a normal patient account, which gets `role = 'user'`. There is no `"admin"` self-signup path -- admins are promoted via `admin_set_user_role` by an existing admin. |

A doctor account created this way has **no** `doctor_profiles` row yet --
the app must route a fresh doctor straight to the "submit your profile"
screen (§10) before anything else doctor-related will work.

---

## 2. Profile (read / update)

No RPC -- direct table access, RLS-scoped to the caller's own row (or any
row, for an admin).

- **Read**: `select * from profiles where id = auth.uid()` (or just
  `.single()` with no filter if your SDK call already scopes by the
  signed-in user).
  Response shape (`profiles`):
  ```ts
  { id: string; role: 'user' | 'doctor' | 'admin'; full_name: string | null;
    phone: string | null; email: string; avatar_url: string | null;
    is_suspended: boolean; created_at: string; updated_at: string }
  ```
- **Update**: `update profiles set full_name = ..., phone = ... where id =
  auth.uid()`. Only your own row is writable (`profiles_update_own`
  policy). `role` cannot be changed this way even on your own row -- a
  `prevent_role_escalation` trigger blocks it; role changes only happen
  through `admin_set_user_role` (§13).
- No custom error codes here -- a disallowed `role` change surfaces as a
  raw Postgres trigger error, and a write to someone else's row simply
  updates zero rows (RLS silently filters it, no exception).

---

## 3. Service categories & services (browsing)

No RPC -- direct table reads, publicly readable when active.

- **Categories**: `select * from service_categories where is_active =
  true order by sort_order`. (An admin's own client may omit the
  `is_active` filter to see inactive ones too -- `is_admin()` is OR'd into
  the policy.)
  Shape: `{ id, name, slug, description, icon, is_active, sort_order,
  created_at, updated_at }`.
- **Services in a category**: `select * from services where category_id =
  ? and is_active = true order by sort_order`.
  Shape: `{ id, category_id, name, slug, description, short_description,
  requires_doctor, duration_minutes, supported_modes: service_mode[],
  metadata: jsonb, is_active, sort_order, created_at, updated_at }`.
  `requires_doctor` determines whether the booking flow needs a doctor
  picker screen at all (§7-8 only apply when it's `true`).

---

## 4. Doctor listings (public)

Use the views, not the legacy RPCs (see bottom note).

- **All approved, non-suspended doctors**: `select * from doctors_public`.
  Shape: `{ doctor_id, full_name, title, specialization, qualifications,
  bio, years_of_experience, consultation_modes: service_mode[],
  accepting_requests, avatar_url }`. Never includes `license_number`,
  `admin_notes`, email, or phone.
- **Doctors offering a specific service**: `select * from
  doctor_services_public where service_id = ?`.
  Shape: `{ doctor_id, full_name, service_id, service_name, service_slug }`.
- **Avatar images**: `avatar_url` (when not null) is a path inside the
  private `doctor-avatars` storage bucket, not a public URL -- request a
  signed URL for it via the storage client
  (`supabase.storage.from('doctor-avatars').createSignedUrl(path, ttl)`);
  do not try to construct a public URL, the bucket is private by design.

---

## 5. Doctor detail screen

No dedicated "doctor detail" RPC exists -- compose it client-side from two
reads, both public:

1. `select * from doctors_public where doctor_id = ?` (profile fields, §4).
2. `select * from doctor_services_public where doctor_id = ?` (which
   services they offer).

---

## 6. Open slots for booking

```ts
supabase.rpc('get_doctor_open_slots', { p_doctor_id, p_date })
```
- `p_date`: a plain `date` (no time component) -- one calendar day at a
  time, in the clinic's local day (the function works entirely in
  `Asia/Kolkata` internally; pass the date the user picked on a calendar
  widget).
- Returns `{ slot_start: timestamptz, slot_end: timestamptz }[]` -- already
  filtered to exclude anything inside the minimum lead time, anything
  beyond the max booking horizon, and anything already `accepted`/
  `confirmed` for that doctor. An unapproved/not-accepting/suspended
  doctor simply yields `[]` -- **not** an error (see
  `docs/error-codes.md`'s "Why `get_doctor_open_slots` doesn't raise
  AS002" section) -- so an empty result should render as "no slots that
  day," not a generic error toast.
- Errors: `AS000_NOT_AUTHENTICATED` ("Please sign in to view available
  times."), `AS003_INVALID_DATE` ("Please choose a different date." --
  date is in the past or beyond the booking horizon).

---

## 7. Create a request (booking)

```ts
supabase.rpc('create_service_request', {
  p_service_id, p_doctor_id, p_slot_start, p_mode, p_notes, p_contact_phone, p_address
})
```
- `p_doctor_id`: required if the service's `requires_doctor` is `true`,
  must be `null` otherwise.
- `p_slot_start`: must be exactly one of the `slot_start` values
  `get_doctor_open_slots` just returned for that doctor/date -- don't let
  the user free-type a time.
- `p_mode`: one of `'in_person' | 'home_visit' | 'online'`; must be in the
  service's (and, if a doctor is involved, the doctor's)
  `consultation_modes`/`supported_modes`.
- `p_address`: required when `p_mode = 'home_visit'`, otherwise optional.
- `p_contact_phone`: required whenever a doctor is involved.
- Returns the new request's `uuid` directly (not a row) -- fetch the full
  row afterward via `patient_requests_view` (§9) if you need it displayed
  immediately.
- Errors (show the friendly text, not the code): `AS000_NOT_AUTHENTICATED`
  ("Please sign in to book."), `AS001_SLOT_UNAVAILABLE` ("That time was
  just booked by someone else -- please pick another."),
  `AS002_DOCTOR_NOT_AVAILABLE` ("This doctor isn't taking new requests
  right now."), `AS003_INVALID_DATE` ("That date is no longer bookable."),
  `AS004_ROLE_NOT_PATIENT` ("Only patient accounts can book."),
  `AS005_ACCOUNT_SUSPENDED` ("Your account has been suspended -- contact
  support."), `AS006_SERVICE_UNAVAILABLE` ("This service isn't available
  right now."), `AS007_DOCTOR_REQUIRED` ("Please choose a doctor for this
  service."), `AS008_DOCTOR_NOT_OFFERED` ("This doctor doesn't offer that
  service."), `AS009_MODE_UNSUPPORTED` ("That consultation type isn't
  available for this booking."), `AS010_DUPLICATE_REQUEST` ("You already
  have a request with this doctor at that time."),
  `AS011_ADDRESS_REQUIRED` ("Please provide an address for a home
  visit."), `AS012_INVALID_CONTACT_PHONE` ("Please enter a valid contact
  phone number."), `AS013_TEXT_TOO_LONG` ("Please shorten your notes/
  address."), `AS014_DOCTOR_NOT_ALLOWED` ("This service doesn't involve a
  doctor -- remove the doctor selection."), `AS034_TOO_MANY_ACTIVE_REQUESTS`
  ("You have too many open requests -- please wait for one to be resolved
  before booking another."), `AS035_RATE_LIMITED` ("You're booking too
  quickly -- please wait a bit and try again.").

---

## 8. My requests (list)

No parameters -- RLS does the scoping, so the same `select *` works for
every role as long as you query the view matching the caller.

- **Patient**: `select * from patient_requests_view order by
  scheduled_start desc`.
  Shape: `{ id, patient_id, doctor_id, service_id, service_name,
  doctor_full_name, doctor_specialization, scheduled_start, scheduled_end,
  mode, status, patient_notes, doctor_response_note, contact_phone,
  address_text, rejection_reason, cancellation_reason, created_at,
  updated_at }`. (`doctor_response_note` is a legacy column, always
  expect it to be `null` on new requests -- rejection/cancellation reasons
  and the full history live in `rejection_reason`/`cancellation_reason`
  and the status log, §9.)
- **Doctor**: `select * from doctor_requests_view order by scheduled_start
  desc`.
  Shape: `{ id, patient_id, patient_full_name, service_id, service_name,
  doctor_id, scheduled_start, scheduled_end, mode, status, patient_notes,
  doctor_response_note, contact_phone, address_text, created_at,
  updated_at }`. `contact_phone` is masked to `null` unless the request is
  `accepted`/`confirmed` (see `doctor_request_contacts`'s status-gated
  view -- a doctor shouldn't see a patient's phone number before accepting
  them).
- **Admin**: use `admin_list_requests` (§13) instead -- it's paginated and
  filterable, where the two views above are not.

---

## 9. Request detail + status log

No single RPC for a patient/doctor's own request detail -- compose two
reads (both RLS-scoped, so only your own requests/history are visible):

1. One row from `patient_requests_view`/`doctor_requests_view` filtered by
   `id = ?` (the full current state, §8's shape).
2. `select * from request_status_history where request_id = ? order by
   changed_at` for the timeline.
   Shape per row: `{ id, request_id, old_status: request_status | null,
   new_status: request_status, changed_by: uuid | null, note: string |
   null, changed_at: timestamptz }`. `old_status` is `null` only for the
   very first ("created") entry.

For an **admin**, `admin_get_request_detail(p_request_id)` (§13) returns
both in one call, with the status log pre-aggregated into a `status_log`
jsonb array.

---

## 10. Changing a request's status

**The single entry point for every status change, for every role**:

```ts
supabase.rpc('transition_request_status', { p_request_id, p_new_status, p_reason })
```
- `p_new_status`: one of `'accepted' | 'rejected' | 'confirmed' |
  'completed' | 'cancelled'` -- never pass `'requested'` (that's only the
  initial state, set by `create_service_request`).
- `p_reason`: optional for `rejected`/`cancelled` (shown to the other
  party), ignored/should be omitted for `accepted`/`confirmed`/`completed`.
- Returns nothing (`void`) on success -- re-fetch the request (§9) to show
  the new state; a notification (§11) is also created for the other party
  automatically.
- Who can call which transition:
  | Transition | Caller |
  |---|---|
  | `requested -> accepted` | the assigned doctor only |
  | `requested -> rejected` | the assigned doctor only |
  | `accepted -> confirmed` | the assigned doctor or an admin |
  | `confirmed -> completed` | the assigned doctor or an admin, only after `scheduled_start` has passed |
  | `{requested,accepted,confirmed} -> cancelled` | the patient (any of the three source states), the assigned doctor (`accepted`/`confirmed` only -- a doctor declines a still-`requested` one via `rejected` instead), or an admin |
- Errors: `AS000_NOT_AUTHENTICATED` ("Please sign in."),
  `AS015_REQUEST_NOT_FOUND` ("That request no longer exists."),
  `AS016_INVALID_TRANSITION` ("This request can no longer be updated --
  please refresh." -- the status graph itself rejects this change, for
  anyone), `AS017_NOT_AUTHORIZED_FOR_TRANSITION` ("You're not allowed to
  do that." -- right status, wrong caller), `AS018_DOCTOR_NOT_APPROVED`
  ("You must be an approved doctor to accept requests."),
  `AS019_SLOT_ALREADY_BOOKED` ("That time is no longer available -- it was
  just booked." -- see `docs/concurrency-testing.md`),
  `AS020_TOO_EARLY_TO_COMPLETE` ("This appointment hasn't happened yet."),
  `AS021_CANCELLATION_NOTICE_WINDOW` ("It's too close to the appointment
  time to cancel." -- only possible if `booking_settings()`'s
  `cancellation_notice_minutes` is raised above its current default of 0).

---

## 11. Notifications

- **List**: `select * from notifications where user_id = auth.uid() order
  by created_at desc` (RLS already scopes this to your own, the
  `user_id` filter is just for clarity/index use).
  Shape: `{ id, user_id, type: notification_type, title, body,
  related_request_id: uuid | null, read_at: timestamptz | null,
  is_read: boolean, created_at }`. `type` is one of
  `request_created | request_accepted | request_rejected |
  request_confirmed | request_cancelled | request_completed |
  doctor_approved | doctor_rejected | doctor_suspended | system`.
- **Unread count** (for a badge): `supabase.rpc('get_unread_notification_count')`
  -- returns a plain `integer`.
- **Mark one read**: `supabase.rpc('mark_notification_read', { p_notification_id })`
  -- returns an `integer` row count (`0` if it didn't exist or wasn't
  yours, `1` on success -- not an error either way, just check the count
  if you need to distinguish).
- **Mark all read**: `supabase.rpc('mark_all_notifications_read')` -- returns
  the `integer` count of rows actually updated.
- Error: `AS000_NOT_AUTHENTICATED` on all four if called signed-out.

---

## 12. Doctor onboarding and self-management

| Need | Call | Notes |
|---|---|---|
| Submit/edit professional profile | `submit_doctor_profile(p_title, p_specialization, p_qualifications, p_bio, p_years, p_license_number, p_modes, p_slot_duration)` | Caller must have `profiles.role = 'doctor'`. First call creates the row as `pending`. Editing an `approved`/`suspended` profile is allowed, but changing `specialization` or `license_number` forces it back to `pending` for re-review (flag this in the UI before submit). Editing a `rejected` profile clears the rejection reason and resubmits as `pending`. `p_modes`: `service_mode[]`, at least one. `p_slot_duration`: one of `10/15/20/30/45/60`. Returns `void`. Errors: `AS000_NOT_AUTHENTICATED`, `AS022_NOT_A_DOCTOR_ACCOUNT` ("Only doctor accounts can submit a profile."), `AS023_INVALID_DOCTOR_PROFILE_FIELDS` ("Please check the profile fields."). |
| View my own application/profile | No RPC -- `select * from doctor_profiles where user_id = auth.uid()`. | Shape: see `docs/data-model.md`'s `DOCTOR_PROFILES` entity. `null` result means "haven't submitted yet" -- route to the submission form. |
| Choose offered services | `set_doctor_services(p_service_ids: uuid[])` | Full atomic replace, not incremental -- pass the complete new list every time. Only active, `requires_doctor = true` services are actually inserted -- any other id in the array is silently dropped, not an error, so validate the picker's choices client-side. Returns the new `doctor_services` rows. Requires an **approved** doctor. Error: the caller is not an approved doctor -- "Only approved doctors can configure services." **Note**: unlike every other function in this document, this particular error is *not* prefixed with an `AS0xx` code -- match on the literal text, or treat any error from this call with no `AS` prefix as "you must be an approved doctor." |
| Set weekly availability | `set_doctor_availability(p_windows: jsonb)` | `p_windows` is a JSON array of `{ day_of_week: 0-6, start_time: "HH:MM", end_time: "HH:MM", slot_duration_minutes }`. Full atomic replace. Errors: `AS024_ONLY_APPROVED_DOCTORS` ("You must be approved before setting availability."), `AS025_INVALID_AVAILABILITY_WINDOW` ("Please check your availability times."), `AS026_AVAILABILITY_OVERLAP` ("Two of your availability windows overlap."). |
| Toggle "accepting new requests" | `set_accepting_requests(p_value: boolean)` | Simple on/off switch surfaced as a profile toggle. Errors: `AS024_ONLY_APPROVED_DOCTORS`. |

---

## 13. Admin console

All require `is_admin()`; every call is also written to `audit_log`
(not client-visible, but worth knowing nothing here is silent).

| Need | Call | Notes |
|---|---|---|
| List doctors (queue/directory) | `admin_list_doctors(p_status?, p_limit=20, p_offset=0)` | `p_status` filters by `doctor_status`, omit for all. Returns `{ user_id, full_name, email, phone, specialization, license_number, status, years_experience, is_suspended, created_at, reviewed_at }[]`. |
| List requests | `admin_list_requests(p_status?, p_doctor_id?, p_service_id?, p_date_from?, p_date_to?, p_limit=20, p_offset=0)` | Returns `{ id, patient_id, patient_full_name, doctor_id, doctor_full_name, service_id, service_name, scheduled_start, scheduled_end, mode, status, created_at }[]`. |
| Request detail | `admin_get_request_detail(p_request_id)` | Returns one row: everything in `admin_list_requests`'s shape plus `patient_phone, doctor_response_note, contact_phone, address_text, rejection_reason, cancellation_reason, updated_at, status_log: jsonb[]` (the full `request_status_history`, pre-joined). |
| Approve/reject/suspend/reinstate a doctor | `admin_set_doctor_status(p_doctor_id, p_new_status, p_reason?)` | Allowed pairs only: `pending->approved`, `pending->rejected` (reason required), `approved->suspended`, `suspended->approved`. **Not** `rejected->approved` -- a rejected doctor must resubmit (§12) instead. Suspending cascades: the doctor's future `requested` requests are auto-rejected (with notice to the patient); future `accepted`/`confirmed` ones are flagged for manual admin review, not auto-cancelled. Returns the updated `doctor_profiles` row. Errors: `AS027_NOT_AUTHORIZED`, `AS028_DOCTOR_NOT_FOUND`, `AS029_INVALID_DOCTOR_STATUS_TRANSITION`. |
| Change a user's role | `admin_set_user_role(p_user_id, p_new_role)` | Errors: `AS027_NOT_AUTHORIZED`, `AS030_USER_NOT_FOUND`, `AS029_INVALID_DOCTOR_STATUS_TRANSITION` (target already has that role), `AS031_CANNOT_DEMOTE_LAST_ADMIN` ("You can't remove the last admin account."). |
| Suspend/reinstate any user account | `admin_set_user_suspended(p_user_id, p_value)` | Errors: `AS027_NOT_AUTHORIZED`, `AS030_USER_NOT_FOUND`. |
| Create/edit a category | `admin_upsert_service_category(p_id?, p_name?, p_description?, p_icon?, p_is_active=true, p_sort_order=0)` | Omit `p_id` to create, pass it to edit. Returns the row's `uuid`. Errors: `AS027_NOT_AUTHORIZED`, `AS032_INVALID_CATALOG_FIELDS`, `AS033_CATALOG_ENTITY_NOT_FOUND` (editing a nonexistent id). |
| Activate/deactivate a category | `admin_set_category_active(p_category_id, p_is_active)` | Deactivating never deletes anything underneath it. Errors: `AS027_NOT_AUTHORIZED`, `AS033_CATALOG_ENTITY_NOT_FOUND`. |
| Create/edit a service | `admin_upsert_service(p_id?, p_category_id?, p_name?, p_description?, p_short_description?, p_requires_doctor=true, p_duration_minutes=30, p_supported_modes=['in_person'], p_is_active=true, p_sort_order=0)` | Same create-vs-edit pattern as the category version. Errors: `AS027_NOT_AUTHORIZED`, `AS032_INVALID_CATALOG_FIELDS`, `AS033_CATALOG_ENTITY_NOT_FOUND`. |
| Activate/deactivate a service | `admin_set_service_active(p_service_id, p_is_active)` | Disabling never deletes existing requests for it. Errors: `AS027_NOT_AUTHORIZED`, `AS033_CATALOG_ENTITY_NOT_FOUND`. |

---

## Legacy functions -- do not call these from a new client

The schema still contains an earlier generation of functions that predate
`create_service_request`/`transition_request_status`/the `doctors_public`
view family and are superseded by what's documented above:
`create_request`, `get_available_slots`, `cancel_request`,
`doctor_respond_request`, `doctor_confirm_request`,
`doctor_complete_request`, `get_doctor_public_profile`,
`list_approved_doctors`, `get_my_doctor_application`,
`submit_doctor_application`, `set_weekly_availability`. They still work
(nothing has dropped them), but none of them know about the rate limits,
the richer `doctors_public`/`doctor_services_public` view shapes, the
single-state-machine notification/audit side effects, or the current
`AS0xx` error registry -- using them alongside the current API would
produce inconsistent behavior (e.g. a booking made through `create_request`
bypasses the `AS034`/`AS035` rate limits entirely). Both the website and
Android clients should only ever call the functions and views named in
§1-13 above.
