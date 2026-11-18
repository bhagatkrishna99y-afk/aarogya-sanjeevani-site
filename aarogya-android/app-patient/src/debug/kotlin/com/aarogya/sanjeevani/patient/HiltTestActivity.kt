package com.aarogya.sanjeevani.patient

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * `hiltViewModel()` requires the hosting Activity itself to implement
 * Hilt's generated component interfaces, which only `@AndroidEntryPoint`
 * provides -- a plain `androidx.activity.ComponentActivity` can never
 * satisfy it, regardless of the test's Application class. Lives in the
 * `debug` source set rather than `androidTest` deliberately: an
 * instrumented test's `createAndroidComposeRule<T>()` launches its
 * Activity inside the *target app's* own process, so a test-only
 * Activity declared in the separate androidTest APK can never resolve
 * there -- it has to ship as part of the debug app build itself (never
 * release, and never actually launched outside tests).
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
