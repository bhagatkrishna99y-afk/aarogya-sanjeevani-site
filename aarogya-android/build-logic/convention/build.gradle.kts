plugins {
    `kotlin-dsl`
}

group = "com.aarogya.sanjeevani.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // These are the *plugin* artifacts (not the AGP/Kotlin runtime) --
    // declaring them here is what lets this module's own .kt files
    // `apply(plugin = "com.android.application")` etc. and reference
    // types like `ApplicationExtension`/`KotlinAndroidProjectExtension`.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
}

// Registers each convention plugin under the plugin id modules apply via
// `alias(libs.plugins.aarogya.*)` in the version catalog -- the catalog
// entry's id must match the `id(...)` here exactly.
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "aarogya.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "aarogya.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "aarogya.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "aarogya.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "aarogya.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidLint") {
            id = "aarogya.android.lint"
            implementationClass = "AndroidLintConventionPlugin"
        }
    }
}
