package com.aarogyasanjeevani.patient.ui.navigation

/** All navigation destinations. Centralized here so argument names can't drift between screens. */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"

    const val HOME = "home"
    const val MY_REQUESTS = "my_requests"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"

    /**
     * The booking flow is one nested graph scoped to a single shared
     * BookingViewModel (via [androidx.hilt.navigation.compose.hiltViewModel]
     * with the graph's own back stack entry) so every step -- doctor list,
     * doctor detail, slot picking, review -- shares the same in-progress
     * booking state without re-fetching or passing it through every route.
     */
    const val BOOKING_GRAPH = "booking/{serviceId}"
    fun bookingGraph(serviceId: String) = "booking/$serviceId"
    const val ARG_SERVICE_ID = "serviceId"

    const val DOCTOR_LIST = "doctor_list"
    const val DOCTOR_DETAIL = "doctor_detail"
    const val BOOK_SLOT = "book_slot"
    const val REVIEW_REQUEST = "review_request"

    const val REQUEST_DETAIL = "request_detail/{requestId}"
    fun requestDetail(requestId: String) = "request_detail/$requestId"
    const val ARG_REQUEST_ID = "requestId"
}
