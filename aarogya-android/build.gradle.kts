// Top-level build file -- intentionally thin. Real module configuration
// (Android/Kotlin/Compose/Hilt defaults) lives in the build-logic
// convention plugins, applied per-module via their own build.gradle.kts.
// These `alias(...) apply false` lines only make the plugin versions
// (from the version catalog) available to resolve in subprojects, without
// activating them on the root project itself.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover) apply false
}
