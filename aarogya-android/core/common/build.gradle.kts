plugins {
    alias(libs.plugins.aarogya.android.library)
    alias(libs.plugins.aarogya.android.hilt)
    alias(libs.plugins.aarogya.android.lint)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.aarogya.sanjeevani.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.coroutines.core)
    // Clock (ClockModule.kt) is part of this module's public API surface.
    api(libs.kotlinx.datetime)
    implementation(libs.androidx.datastore.preferences)

    // Not :core:testing -- that module depends on :core:common (for fakes
    // built on AppResult/AppError), so the dependency has to run only
    // one way to avoid a cycle. This module's own tests are simple
    // enough not to need :core:testing's fakes anyway.
    testImplementation(libs.turbine)
}
