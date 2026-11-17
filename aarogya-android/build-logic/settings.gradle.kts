dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Shares the *same* version catalog the rest of the project uses, so
    // "bump a version in gradle/libs.versions.toml" is the one place that
    // actually needs editing -- this build's own convention plugins stay
    // in lockstep with the module build.gradle.kts files that apply them.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"

include(":convention")
