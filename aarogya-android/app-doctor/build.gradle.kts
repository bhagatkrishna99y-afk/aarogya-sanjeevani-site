plugins {
    alias(libs.plugins.aarogya.android.application)
    alias(libs.plugins.aarogya.android.application.compose)
    alias(libs.plugins.aarogya.android.hilt)
    alias(libs.plugins.aarogya.android.lint)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.aarogya.sanjeevani.doctor"

    defaultConfig {
        applicationId = "com.aarogya.sanjeevani.doctor"
    }

    productFlavors {
        getByName("dev") {
            resValue("string", "app_name", "Aarogya Doctor (Dev)")
        }
        getByName("prod") {
            resValue("string", "app_name", "Aarogya Sanjeevani for Doctors")
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
    implementation(libs.timber)

    testImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
