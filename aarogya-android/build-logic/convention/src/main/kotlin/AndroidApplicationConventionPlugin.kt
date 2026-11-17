import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.FileInputStream
import java.util.Properties

/**
 * Applied by both app-patient and app-doctor. Owns everything that's
 * identical between the two apps: SDK levels, build types, the dev/prod
 * flavor dimension, R8 for release, and reading Supabase credentials out
 * of local.properties (falling back to Gradle/CLI properties) into
 * BuildConfig -- centralized here so that logic exists exactly once
 * rather than copy-pasted into both app build.gradle.kts files.
 *
 * What's deliberately left to each app's own build.gradle.kts: the base
 * applicationId, and each flavor's app_name resValue -- both are
 * genuinely per-app content, not shared behavior.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            val localProperties = loadLocalProperties()
            val signing = loadSigningConfig()

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)

                defaultConfig {
                    targetSdk = 35
                    versionCode = intProperty("aarogya.versionCode")
                    versionName = stringProperty("aarogya.versionName")
                }

                buildFeatures {
                    buildConfig = true
                }

                if (signing != null) {
                    signingConfigs {
                        create("release") {
                            storeFile = signing.storeFile
                            storePassword = signing.storePassword
                            keyAlias = signing.keyAlias
                            keyPassword = signing.keyPassword
                        }
                    }
                }

                buildTypes {
                    debug {
                        isMinifyEnabled = false
                        applicationIdSuffix = ".debug"
                    }
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        // Unsigned (today's default, if neither the CI env
                        // vars nor a local keystore.properties resolve to a
                        // real signing config) is still fine for a local
                        // assembleRelease/bundleRelease smoke test of R8 --
                        // just never for an artifact that leaves this
                        // machine. See loadSigningConfig's own doc comment
                        // for exactly what each path needs.
                        if (signing != null) signingConfig = signingConfigs.getByName("release")
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )
                    }
                }

                flavorDimensions += FLAVOR_DIMENSION
                productFlavors {
                    create("dev") {
                        dimension = FLAVOR_DIMENSION
                        applicationIdSuffix = ".dev"
                        buildConfigStringField(
                            "SUPABASE_URL",
                            secret("SUPABASE_URL_DEV", localProperties),
                        )
                        buildConfigStringField(
                            "SUPABASE_ANON_KEY",
                            secret("SUPABASE_ANON_KEY_DEV", localProperties),
                        )
                    }
                    create("prod") {
                        dimension = FLAVOR_DIMENSION
                        buildConfigStringField(
                            "SUPABASE_URL",
                            secret("SUPABASE_URL_PROD", localProperties),
                        )
                        buildConfigStringField(
                            "SUPABASE_ANON_KEY",
                            secret("SUPABASE_ANON_KEY_PROD", localProperties),
                        )
                    }
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
            }
        }
    }
}

internal const val FLAVOR_DIMENSION = "environment"

/** `BuildConfig.<name>` as a quoted String field -- `buildConfigField`'s
 *  value parameter is a literal Java/Kotlin expression, so the value
 *  itself needs its own embedded quotes. */
internal fun com.android.build.api.dsl.ApplicationProductFlavor.buildConfigStringField(
    name: String,
    value: String,
) {
    buildConfigField("String", name, "\"$value\"")
}

/**
 * [aarogya.versionCode]/[aarogya.versionName] in the root `gradle.properties`
 * (read automatically into every project's properties by Gradle itself,
 * unlike the Supabase credentials [secret] reads -- those are the ones
 * that need `local.properties`/`-P`-fallback logic, this one doesn't)
 * are the one versioning scheme both app-patient and app-doctor share,
 * bumped together here rather than per-module. Failing fast on a
 * missing/malformed value is deliberate: a build that silently fell back
 * to some default versionCode could get uploaded to the Play Console
 * with a code that doesn't actually increase from the last release,
 * which the Console rejects anyway, but only after the rest of the
 * build/signing work is already done.
 */
internal fun Project.intProperty(key: String): Int =
    (findProperty(key) as String?)?.toIntOrNull()
        ?: error("Missing or non-numeric \"$key\" in gradle.properties.")

internal fun Project.stringProperty(key: String): String =
    (findProperty(key) as String?)
        ?: error("Missing \"$key\" in gradle.properties.")

internal fun Project.loadLocalProperties(): Properties {
    val properties = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        FileInputStream(file).use { properties.load(it) }
    }
    return properties
}

internal data class SigningConfigValues(
    val storeFile: java.io.File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

/**
 * Two ways to supply a signing config, checked in order, either of
 * which is complete on its own -- never a mix of one value from each:
 *
 * 1. **Environment variables** (`SIGNING_STORE_FILE`/`SIGNING_STORE_PASSWORD`/
 *    `SIGNING_KEY_ALIAS`/`SIGNING_KEY_PASSWORD`) -- the CI path. GitHub
 *    Actions decodes the keystore from an encrypted secret to a temp
 *    file and exports its path as `SIGNING_STORE_FILE`, exactly like a
 *    human would set these by hand to sign locally without a
 *    `keystore.properties` file at all (see
 *    `.github/workflows/release.yml`).
 * 2. **`keystore.properties`** at the repo root (gitignored, same
 *    shape as `local.properties` -- see `keystore.properties.example`)
 *    -- for signing a release build on your own machine.
 *
 * Neither present: returns `null`, and the caller leaves the `release`
 * build type unsigned (today's default) rather than failing the build --
 * unlike [secret], a missing signing config doesn't mean this build is
 * useless, just that it isn't a build anyone should distribute.
 */
internal fun Project.loadSigningConfig(): SigningConfigValues? {
    val envStoreFile = System.getenv("SIGNING_STORE_FILE")
    val envStorePassword = System.getenv("SIGNING_STORE_PASSWORD")
    val envKeyAlias = System.getenv("SIGNING_KEY_ALIAS")
    val envKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")
    if (envStoreFile != null && envStorePassword != null && envKeyAlias != null && envKeyPassword != null) {
        return SigningConfigValues(
            storeFile = file(envStoreFile),
            storePassword = envStorePassword,
            keyAlias = envKeyAlias,
            keyPassword = envKeyPassword,
        )
    }

    val propertiesFile = rootProject.file("keystore.properties")
    if (!propertiesFile.exists()) return null
    val properties = Properties().apply { FileInputStream(propertiesFile).use { load(it) } }
    val storeFile = properties.getProperty("storeFile") ?: return null
    val storePassword = properties.getProperty("storePassword") ?: return null
    val keyAlias = properties.getProperty("keyAlias") ?: return null
    val keyPassword = properties.getProperty("keyPassword") ?: return null
    return SigningConfigValues(
        storeFile = rootProject.file(storeFile),
        storePassword = storePassword,
        keyAlias = keyAlias,
        keyPassword = keyPassword,
    )
}

/**
 * local.properties first (the documented, gitignored place for this --
 * see local.properties.example), then Gradle/CLI properties
 * (`-PSUPABASE_URL_DEV=...` or a `~/.gradle/gradle.properties` entry, the
 * "or Gradle properties" half of the requirement, useful for CI). If
 * neither is set, fail with a clear message at configuration time rather
 * than silently shipping a build that can never reach Supabase.
 */
internal fun Project.secret(key: String, localProperties: Properties): String =
    localProperties.getProperty(key)
        ?: (findProperty(key) as String?)
        ?: error(
            "Missing \"$key\". Copy local.properties.example to local.properties " +
                "and fill in your Supabase project's values, or pass -P$key=... .",
        )
