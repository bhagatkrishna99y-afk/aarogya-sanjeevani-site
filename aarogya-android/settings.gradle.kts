pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "aarogya-android"

include(":app-patient")
include(":app-doctor")
include(":core:designsystem")
include(":core:common")
include(":core:network")
include(":core:data")
include(":core:ui")
include(":core:testing")
