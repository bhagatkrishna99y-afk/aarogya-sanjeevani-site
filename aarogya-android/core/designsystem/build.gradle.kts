plugins {
    alias(libs.plugins.aarogya.android.library)
    alias(libs.plugins.aarogya.android.library.compose)
    alias(libs.plugins.aarogya.android.lint)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.aarogya.sanjeevani.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    // AsAvatar's image loading only -- the network engine (coil-network-ktor)
    // and the app-wide ImageLoader stay the consuming app's responsibility,
    // same as app-patient/app-doctor already set up.
    implementation(libs.coil.compose)

    // The debug-only Design Gallery screen's host Activity -- nothing in
    // src/main needs this, so it's scoped to the debug build type only.
    debugImplementation(libs.androidx.activity.compose)
}
