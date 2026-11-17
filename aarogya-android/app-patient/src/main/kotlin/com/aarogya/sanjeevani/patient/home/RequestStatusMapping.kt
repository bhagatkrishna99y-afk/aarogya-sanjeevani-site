package com.aarogya.sanjeevani.patient.home

import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.designsystem.component.AsRequestStatus

/** [com.aarogya.sanjeevani.core.data.model.RequestStatus] (this app's
 *  real domain enum) to [AsRequestStatus] (the design system's own
 *  presentation-only vocabulary) -- see that type's doc comment for why
 *  :core:designsystem never imports :core:data's enum directly. */
fun RequestStatus.toAsRequestStatus(): AsRequestStatus =
    when (this) {
        RequestStatus.REQUESTED -> AsRequestStatus.Requested
        RequestStatus.ACCEPTED -> AsRequestStatus.Accepted
        RequestStatus.CONFIRMED -> AsRequestStatus.Confirmed
        RequestStatus.COMPLETED -> AsRequestStatus.Completed
        RequestStatus.REJECTED -> AsRequestStatus.Rejected
        RequestStatus.CANCELLED -> AsRequestStatus.Cancelled
    }
