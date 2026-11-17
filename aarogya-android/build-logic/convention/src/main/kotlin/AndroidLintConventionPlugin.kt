import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * Static analysis, applied uniformly to every module (see each module's
 * own build.gradle.kts). Builds upon detekt's and ktlint's own sensible
 * default rule sets (`buildUponDefaultConfig`) rather than a fully
 * hand-authored config -- but core:designsystem is this repo's first
 * Compose-heavy module, and detekt's defaults (written with plain Kotlin
 * in mind) misfire on a few genuinely idiomatic Compose patterns -- a
 * PascalCase `@Composable` function, a private top-level `@Preview`
 * function with no in-source caller, a richly-configurable component
 * with 7+ named parameters. `config/detekt/detekt.yml` layers just those
 * three standard, well-documented overrides on top of the defaults; see
 * that file's own comment for why each one is there.
 */
class AndroidLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("io.gitlab.arturbosch.detekt")
                apply("org.jlleitschuh.gradle.ktlint")
            }

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                allRules = false
                autoCorrect = false
                parallel = true
                config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
            }

            // detekt's own compiler frontend only recognizes JVM targets up to
            // 22 (as of detekt 1.23.7) and otherwise auto-detects whichever JDK
            // is running Gradle -- pinned explicitly here to match this
            // project's actual Kotlin jvmTarget (KotlinAndroid.kt) so detekt
            // still works on a newer local JDK (e.g. 24) that its own
            // --jvm-target flag doesn't accept yet.
            tasks.withType<Detekt>().configureEach {
                jvmTarget = "17"
            }

            extensions.configure<KtlintExtension> {
                android.set(true)
                ignoreFailures.set(false)
                filter {
                    exclude { entry -> entry.file.path.contains("generated") }
                }
            }
        }
    }
}
