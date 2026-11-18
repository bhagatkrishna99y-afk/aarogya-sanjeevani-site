package com.aarogya.sanjeevani.patient

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Swaps in [HiltTestApplication] so any `@HiltAndroidTest` can satisfy a
 * real `hiltViewModel()` call -- the stock `AndroidJUnitRunner` launches
 * the app's own `@HiltAndroidApp` [Application], whose component is
 * sealed against production modules and can't be swapped per test.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
