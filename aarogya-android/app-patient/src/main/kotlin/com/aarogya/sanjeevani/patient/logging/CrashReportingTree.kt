package com.aarogya.sanjeevani.patient.logging

import android.util.Log
import com.aarogya.sanjeevani.patient.FeatureFlags
import timber.log.Timber

/**
 * The one place this app would wire in a real crash/error-reporting
 * backend (Crashlytics or otherwise) -- planted in release builds
 * alongside (never instead of) [Timber.DebugTree] in debug, so every
 * call site already logs through [Timber] today and nothing else needs
 * to change the day a real backend is added here.
 *
 * Two deliberate safety rules, both enforced here rather than trusted to
 * every call site:
 * - **Disabled by default.** [log] is a no-op unless
 *   [FeatureFlags.CRASH_REPORTING_ENABLED] is on. Flipping that flag
 *   alone must never be enough to start sending anything anywhere --
 *   this class has no network/SDK call in it at all yet, so even
 *   enabled, today, it does nothing. The flag exists so turning this on
 *   later (once a real backend is plugged into [log]) is a one-line
 *   change, not a re-plant.
 * - **Never the free-text message.** [message] is exactly where a
 *   future call site could accidentally interpolate an email, phone
 *   number, or full name (`Timber.e("Upload failed for $email")`).
 *   Only [priority], [tag] and the [Throwable] (its type and stack
 *   trace, not arbitrary nested exception messages a caller composed)
 *   would ever leave the device through this tree -- so a report still
 *   tells a developer *where* and *what kind of failure*, never *who*.
 * - Below [Log.WARN], nothing is even worth this tree's attention --
 *   verbose/debug/info noise stays local-only (it already goes nowhere
 *   in a release build in the first place, since [Timber.DebugTree]
 *   isn't planted there, but this tree would otherwise still receive it).
 */
class CrashReportingTree : Timber.Tree() {
    // Widened from Timber.Tree's own `protected` so this class's
    // behavior is directly unit-testable (see CrashReportingTreeTest)
    // without having to plant it and reach for Timber's own API as a
    // side channel.
    public override fun isLoggable(
        tag: String?,
        priority: Int,
    ): Boolean = priority >= Log.WARN

    @Suppress("UnusedParameter") // priority/tag/t are exactly what a real backend's call would take
    public override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (!FeatureFlags.CRASH_REPORTING_ENABLED || t == null) return
        // No real backend wired in yet -- this is the one line a
        // `FirebaseCrashlytics.getInstance().recordException(t)` (or
        // equivalent) call would replace, once there is one.
    }
}
