import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * The handful of settings every module -- application or library -- needs
 * identically: compile/min SDK, Java 17 source/target compatibility, and
 * the matching Kotlin JVM target. `CommonExtension` is the interface both
 * `ApplicationExtension` and `LibraryExtension` implement, so this one
 * function configures either.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        compileSdk = 35

        defaultConfig {
            minSdk = 26
            // Without this, AGP falls back to the ancient JUnit3-style
            // android.test.InstrumentationTestRunner, which never discovers
            // @RunWith(AndroidJUnit4::class)/@Test-annotated classes at all --
            // every connectedAndroidTest run silently executes zero tests
            // instead of failing loudly. Latent until a real androidTest
            // module actually tried to run this session.
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
