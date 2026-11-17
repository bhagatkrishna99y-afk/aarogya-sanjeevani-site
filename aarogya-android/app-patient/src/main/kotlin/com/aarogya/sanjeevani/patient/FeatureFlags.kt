package com.aarogya.sanjeevani.patient

/** A plain compile-time switch -- this app has no remote-config service,
 *  so "behind a flag" means exactly this: flip it here, rebuild, ship.
 *  Off by default -- avatar upload (image pick + client-side compress +
 *  upload to the private `doctor-avatars` bucket) is new, higher-risk
 *  surface for this release; the rest of the Profile screen works fully
 *  without it (initials-only avatar). */
object FeatureFlags {
    const val AVATAR_UPLOAD_ENABLED = false

    /** Off until a real crash-reporting backend (Crashlytics or
     *  otherwise) is actually wired in -- see [com.aarogya.sanjeevani.patient.logging.CrashReportingTree].
     *  Until then this only gates whether that tree's `log` override is
     *  a no-op; nothing is sent anywhere regardless. */
    const val CRASH_REPORTING_ENABLED = false
}
