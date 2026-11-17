plugins {
    alias(libs.plugins.aarogya.android.library)
    alias(libs.plugins.aarogya.android.library.compose)
    alias(libs.plugins.aarogya.android.lint)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.aarogya.sanjeevani.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(libs.androidx.compose.material.icons.extended)

    // BaseViewModel is part of this module's public API surface (ViewModel,
    // viewModelScope, StateFlow/Flow all appear in its signature), so these
    // are `api`, not `implementation` -- a consumer extending BaseViewModel
    // needs them on its own compile classpath too.
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.kotlinx.coroutines.core)
}
