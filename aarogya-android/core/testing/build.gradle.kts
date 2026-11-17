plugins {
    alias(libs.plugins.aarogya.android.library)
    alias(libs.plugins.aarogya.android.lint)
}

android {
    namespace = "com.aarogya.sanjeevani.core.testing"
}

dependencies {
    // :core:testing is the one module allowed to depend on other
    // :core:* modules purely to build fakes of their public types --
    // the rule that keeps this acyclic is that nothing else may depend
    // back on :core:testing from `main` (only app/feature *test* source
    // sets do). If :core:data or :core:network needed fakes-of-themselves
    // for their own tests, those belong in that module's own test
    // sources or a testFixtures source set, never here.
    api(project(":core:common"))
    api(project(":core:data"))

    api(libs.junit4)
    api(libs.mockk)
    api(libs.mockk.android)
    api(libs.turbine)
    api(libs.kotlinx.coroutines.test)
    api(libs.androidx.test.ext.junit)
    api(libs.androidx.test.espresso.core)
}
