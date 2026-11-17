import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.aarogya.android.application)
    alias(libs.plugins.aarogya.android.application.compose)
    alias(libs.plugins.aarogya.android.hilt)
    alias(libs.plugins.aarogya.android.lint)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.aarogya.sanjeevani.patient"

    defaultConfig {
        applicationId = "com.aarogya.sanjeevani.patient"
    }

    productFlavors {
        getByName("dev") {
            resValue("string", "app_name", "Aarogya Patient (Dev)")
            buildConfigField("String", "WEBSITE_BASE_URL", "\"${websiteBaseUrl("WEBSITE_BASE_URL_DEV")}\"")
            buildConfigField("String", "CONTACT_EMAIL", "\"${contactEmail("CONTACT_EMAIL_DEV")}\"")
        }
        getByName("prod") {
            resValue("string", "app_name", "Aarogya Sanjeevani")
            buildConfigField("String", "WEBSITE_BASE_URL", "\"${websiteBaseUrl("WEBSITE_BASE_URL_PROD")}\"")
            buildConfigField("String", "CONTACT_EMAIL", "\"${contactEmail("CONTACT_EMAIL_PROD")}\"")
        }
    }

    // JUnit Jupiter (pulled in transitively by a testing dependency) ships
    // its own META-INF/LICENSE*/NOTICE* in several of its own jars --
    // harmless duplicate legal text, but mergeDevDebugAndroidTestJavaResource
    // refuses to pick one on its own. Excluding the whole family rather than
    // each exact filename one at a time (LICENSE.md, then LICENSE-notice.md,
    // ...) -- standard practice for this well-known JUnit 5 packaging
    // collision. Latent until :app-patient:connectedDevDebugAndroidTest was
    // actually run for the first time this session.
    packaging {
        resources {
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
        }
    }
}

/**
 * Where the Welcome screen's Privacy Policy link (opened in a Custom
 * Tab) actually points -- this app-patient-only property, same
 * local.properties-first/`-P`-fallback shape as `AndroidApplicationConventionPlugin`'s
 * `secret()` (not reused directly: that helper is `internal` to
 * build-logic, and WEBSITE_BASE_URL/app-doctor has no reason to need
 * this at all, so it isn't added to that shared convention plugin).
 * No real production domain is registered yet (the website's own
 * sitemap still ships a literal "example.com" placeholder, replaced at
 * *its* build time by `VITE_SITE_URL`) -- failing fast here rather than
 * guessing one.
 */
fun websiteBaseUrl(key: String): String {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        FileInputStream(localPropertiesFile).use { localProperties.load(it) }
    }
    return localProperties.getProperty(key)
        ?: (findProperty(key) as String?)
        ?: error(
            "Missing \"$key\". Copy local.properties.example to local.properties " +
                "and fill in the deployed website's URL, or pass -P$key=... .",
        )
}

/** Where the Profile screen's "Contact support" (About) and "Request
 *  account deletion" `mailto:` links point -- same local.properties-
 *  first/`-P`-fallback shape as [websiteBaseUrl], for the same reason:
 *  an app-patient-only property with no reason for app-doctor or the
 *  shared convention plugin to know about it. */
fun contactEmail(key: String): String {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        FileInputStream(localPropertiesFile).use { localProperties.load(it) }
    }
    return localProperties.getProperty(key)
        ?: (findProperty(key) as String?)
        ?: error(
            "Missing \"$key\". Copy local.properties.example to local.properties " +
                "and fill in the support contact email, or pass -P$key=... .",
        )
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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
    implementation(libs.timber)

    testImplementation(project(":core:testing"))
    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.navigation.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
