package com.aarogya.sanjeevani.patient.navigation

import kotlinx.serialization.Serializable

/**
 * The entire patient navigation graph, as type-safe routes (Navigation
 * Compose 2.8+: a `@Serializable` type stands in for the old string
 * route, and its own properties stand in for nav args -- no string
 * route templates, no manual `NavType` bundles anywhere in this app).
 *
 * Shape, top to bottom:
 * - [Splash] resolves [com.aarogya.sanjeevani.core.data.auth.AuthState]
 *   once at launch and is never returned to.
 * - [AuthGraph] is signed-out territory ([Welcome]/[Login]/[SignUp]/
 *   [ForgotPassword]).
 * - [HomeGraph] is everything behind the bottom nav bar -- one nested
 *   graph per tab, each independently save/restore-able (see
 *   `PatientNavHost.kt`'s `navigateToTab`), so switching tabs never
 *   loses a tab's own back stack.
 *
 * [AuthGraph] and [HomeGraph] are never navigated *to* a destination
 * inside themselves directly -- they exist so the auth gate (see
 * `AuthGateEffect.kt`) and the bottom bar's "am I already home"
 * check can ask "is the current destination anywhere inside this
 * graph" with one `hasRoute` call instead of enumerating every leaf
 * destination.
 */
@Serializable
object Splash

@Serializable
object AuthGraph

@Serializable
object Welcome

@Serializable
object Login

@Serializable
object SignUp

@Serializable
object ForgotPassword

/** Reached from [SignUp] only when the project requires email
 *  confirmation (`SignUpOutcome.PendingEmailConfirmation`) -- there is
 *  no session yet, so the auth gate has no reason to navigate anywhere
 *  on its own; this is the one piece of post-sign-up navigation the
 *  screen itself still does (see `SignUpContract.kt`'s doc comment). */
@Serializable
data class CheckInbox(val email: String)

@Serializable
object HomeGraph

// ---- Home tab ---------------------------------------------------------
// Home -> Doctors (directory) -> Doctor Detail -> Availability -> (same
// RequestForm/Review/Success as the Services tab, registered a second
// time here for this tab's own back stack). Doctor Detail's own service
// id is unknown on this path until the patient picks one through
// ServicePickerBottomSheet -- once picked, this leg continues exactly
// like the Services tab's own does. See DoctorDetail/Availability below
// for why the *same* destinations appear under both tabs.

@Serializable
object HomeTabGraph

@Serializable
object HomeRoute

@Serializable
object DoctorsDirectory

// ---- Services tab -------------------------------------------------------
// ServicesList -> ServiceDetail -> DoctorListForService -> DoctorDetail
// -> Availability -> RequestForm -> Review -> Success.

@Serializable
object ServicesTabGraph

/** [categoryId] pre-selects a category when arriving from somewhere
 *  that already knows which one the user wants (Home's category row,
 *  e.g.) -- `null` (the default, and what the bottom bar/tab switch
 *  always uses) shows every category. */
@Serializable
data class ServicesList(val categoryId: String? = null)

@Serializable
data class ServiceDetail(val serviceId: String)

/** [serviceName] is carried from [ServiceDetail] rather than re-fetched
 *  here -- the pinned header (and the empty state's message, when zero
 *  doctors currently offer the service) both need a name to show even
 *  when the `doctor_services_public` read that would otherwise be the
 *  only source of it comes back with nothing. */
@Serializable
data class DoctorListForService(val serviceId: String, val serviceName: String)

/**
 * [doctorId]/[doctorName]/[slotStart]/[slotDurationMinutes] are all
 * `null` together on the one path that reaches this without going
 * through [Availability] first: a service that doesn't
 * [com.aarogya.sanjeevani.core.data.services.Service.requiresDoctor] --
 * Service Detail skips the doctor/availability pickers entirely and
 * sends the patient straight here, so this screen itself is where that
 * path's own mode/slot gets chosen (still a known gap -- see
 * `RequestFormScreen.kt`'s own doc comment). [serviceId]/[serviceName]
 * are always present either way. [Review]/[Success] carry no args of
 * their own -- both share this same screen's `RequestFormViewModel`
 * instance via `navController.getBackStackEntry<RequestForm>()`
 * (`PatientNavHost.kt`), rather than re-threading every field (plus
 * whatever the patient already typed) through nav args a second and
 * third time.
 */
@Serializable
data class RequestForm(
    val serviceId: String,
    val serviceName: String,
    val doctorId: String? = null,
    val doctorName: String? = null,
    val slotStart: String? = null,
    val slotDurationMinutes: Int? = null,
)

@Serializable
object Review

@Serializable
object Success

/**
 * Reached two ways -- [DoctorsDirectory] (Home tab, [serviceId]/
 * [serviceName] start `null` until `ServicePickerBottomSheet` picks
 * one) and [DoctorListForService] (Services tab, both already known) --
 * both continue to [Availability] and on to [RequestForm] once a
 * service id exists, so this and [Availability] are each declared
 * *twice*, once inside [HomeTabGraph] and once inside [ServicesTabGraph]
 * (see `PatientNavHost.kt`). That's deliberate, not duplication to clean
 * up: reaching the same doctor from two different tabs are two genuinely
 * different navigation contexts with two different back stacks, which
 * is exactly what bottom-nav tabs are supposed to give you.
 */
@Serializable
data class DoctorDetail(val doctorId: String, val serviceId: String? = null, val serviceName: String? = null)

/** Unlike [DoctorDetail], [serviceId]/[serviceName] are never null by
 *  the time this is reached -- either carried straight from
 *  [DoctorListForService], or supplied by `ServicePickerBottomSheet`
 *  on the directory path, in both cases before this route is ever
 *  constructed. */
@Serializable
data class Availability(
    val doctorId: String,
    val doctorName: String,
    val serviceId: String,
    val serviceName: String,
)

// ---- Requests tab -------------------------------------------------------

@Serializable
object RequestsTabGraph

@Serializable
object RequestsList

/** Deep-link stub target -- `aarogya://request/{requestId}` is wired on
 *  this destination (see `PatientNavHost.kt`) but no `<intent-filter>`
 *  exists in the manifest yet, so it's only reachable by internal/
 *  programmatic navigation (and, once the manifest entry exists, `adb
 *  shell am start -d "aarogya://request/<id>"`) for now -- see the task
 *  this was built under for why that's intentional at this stage. */
@Serializable
data class RequestDetail(val requestId: String)

// ---- Notifications tab ---------------------------------------------------

@Serializable
object NotificationsTabGraph

@Serializable
object NotificationsList

// ---- Profile tab ----------------------------------------------------------

@Serializable
object ProfileTabGraph

@Serializable
object ProfileRoute

@Serializable
object EditProfile

@Serializable
object DeleteAccountInfo
