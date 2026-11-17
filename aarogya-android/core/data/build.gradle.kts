plugins {
    alias(libs.plugins.aarogya.android.library)
    alias(libs.plugins.aarogya.android.hilt)
    alias(libs.plugins.aarogya.android.lint)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.aarogya.sanjeevani.core.data"
}

dependencies {
    api(project(":core:common"))
    api(project(":core:network"))

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest.kt)
    implementation(libs.supabase.auth.kt)
    implementation(libs.supabase.storage.kt)
    implementation(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)

    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(project(":core:testing"))
}
