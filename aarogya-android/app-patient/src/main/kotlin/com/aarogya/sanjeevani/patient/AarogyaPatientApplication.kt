package com.aarogya.sanjeevani.patient

import android.app.Application
import com.aarogya.sanjeevani.patient.logging.CrashReportingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AarogyaPatientApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
        installUncaughtExceptionLogger()
    }

    /**
     * Wraps whatever uncaught-exception handler is already installed
     * (the OS's own, by default) rather than replacing it -- this must
     * never be the thing standing between a crash and the system's own
     * crash dialog/process-death handling, which Android relies on to
     * actually restart the app cleanly next launch. All this adds is
     * one [Timber.e] call (routed through [CrashReportingTree] in a
     * release build) so a fatal crash has a chance to be reported
     * before the process dies, exactly like a non-fatal error already
     * would be -- it does not, and cannot safely, keep the app running
     * past an uncaught exception on top of what the previous handler
     * already does.
     */
    private fun installUncaughtExceptionLogger() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception on thread %s", thread.name)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
