# Error codes

RPCs in this backend raise errors as `RAISE EXCEPTION '<CODE>: <friendly message>' USING ERRCODE = 'P0001';`
-- every custom error shares the generic `P0001` SQLSTATE (consistent with
the rest of this project's RPCs; see `create_request`/`get_available_slots`
in `20261111120008_rpc_functions.sql`), so **apps must not branch on
SQLSTATE** for these. Instead, parse the stable `<CODE>` prefix out of the
error message (everything before the first `:`) and map that to a friendly,
localized string client-side. The text after the colon is a reasonable
English fallback, not a stable contract -- it may be reworded without
notice; the code itself will not change once shipped.

All codes below are raised by `get_doctor_open_slots()` and
`create_service_request()`, added in
`20261112000008_scheduling_business_logic.sql`.

| Code | Raised by | Meaning |
|---|---|---|
| `AS000_NOT_AUTHENTICATED` | `booking_settings`, `get_doctor_open_slots`, `create_service_request` | The caller has no `auth.uid()` -- not signed in. |
| `AS001_SLOT_UNAVAILABLE` | `create_service_request` | The chosen slot is missing, no longer open (not in `get_doctor_open_slots`'s current result), or was booked by someone else in the instant between validation and insert (caught from the `requests_no_doctor_overlap` EXCLUDE constraint). |
| `AS002_DOCTOR_NOT_AVAILABLE` | `create_service_request` | The target doctor is not `approved`, not `accepting_requests`, or their account is suspended. (`get_doctor_open_slots` treats the same condition as an empty result, not an error -- see below.) |
| `AS003_INVALID_DATE` | `get_doctor_open_slots`, `create_service_request` | The date/slot is in the past, inside the minimum lead time, or beyond the maximum booking horizon (`booking_settings()`). When raised from inside `create_service_request`, it may have propagated up from a nested `get_doctor_open_slots` call made to validate the chosen slot. |
| `AS004_ROLE_NOT_PATIENT` | `create_service_request` | The caller's `profiles.role` is not `user` -- only patient accounts may submit service requests. |
| `AS005_ACCOUNT_SUSPENDED` | `create_service_request` | The caller's `profiles.is_suspended` is true. |
| `AS006_SERVICE_UNAVAILABLE` | `create_service_request` | The service does not exist, is inactive, or its category is inactive. |
| `AS007_DOCTOR_REQUIRED` | `create_service_request` | The service has `requires_doctor = true` but no `p_doctor_id` was given. |
| `AS008_DOCTOR_NOT_OFFERED` | `create_service_request` | The target doctor does not have a `doctor_services` row for the requested service. |
| `AS009_MODE_UNSUPPORTED` | `create_service_request` | `p_mode` is not in the service's `supported_modes` (and, when a doctor is involved, not in the doctor's `consultation_modes` either). |
| `AS010_DUPLICATE_REQUEST` | `create_service_request` | The same patient already has a `requested`/`accepted`/`confirmed` request with the same doctor at the same `scheduled_start` (also enforced at the database level by `requests_patient_doctor_start_unique`, caught here as a `unique_violation` fallback). |
| `AS011_ADDRESS_REQUIRED` | `create_service_request` | `p_mode = 'home_visit'` but no address was given (also enforced by the `requests_home_visit_address` CHECK constraint). |
| `AS012_INVALID_CONTACT_PHONE` | `create_service_request` | The contact phone is missing when a doctor is involved, or doesn't match the same format used for `profiles.phone` (`^\+?[0-9 ()-]{7,20}$`). |
| `AS013_TEXT_TOO_LONG` | `create_service_request` | `p_notes` exceeds 1000 characters, or `p_address` exceeds 500, after trimming. |
| `AS014_DOCTOR_NOT_ALLOWED` | `create_service_request` | The service has `requires_doctor = false` but a `p_doctor_id` was given anyway. |

## Why `get_doctor_open_slots` doesn't raise `AS002`

`get_doctor_open_slots` is a browsing query: asking "what's open for this
doctor on this date" when the doctor happens to be unapproved, not
accepting requests, or suspended is not a caller mistake -- the correct
answer is simply "nothing," so it returns zero rows. `create_service_request`,
by contrast, is a write whose caller explicitly chose that doctor; the same
condition there is a hard rejection (`AS002_DOCTOR_NOT_AVAILABLE`), because
proceeding would create a request against a doctor who can't act on it.

## Concurrency

`AS001_SLOT_UNAVAILABLE` can also surface as the outcome of a genuine race
between two concurrent bookings for the same slot -- see
`docs/concurrency-testing.md` for how to verify that case, which a
single-session pgTAP test cannot exercise.
