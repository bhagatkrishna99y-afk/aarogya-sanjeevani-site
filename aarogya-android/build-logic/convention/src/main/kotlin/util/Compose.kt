import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Compose compiler metrics/stability reports are genuinely useful when
 * chasing a recomposition bug, but they add real build time every single
 * build -- so they're opt-in via gradle.properties's
 * `aarogya.enableComposeCompilerReports` rather than always-on. Flip it
 * locally, don't flip it in gradle.properties for everyone.
 */
internal fun Project.configureComposeCompiler() {
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        val enableReports = providers
            .gradleProperty("aarogya.enableComposeCompilerReports")
            .map(String::toBoolean)
            .getOrElse(false)
        if (enableReports) {
            metricsDestination.set(layout.buildDirectory.dir("compose_metrics"))
            reportsDestination.set(layout.buildDirectory.dir("compose_reports"))
        }
    }
}

/** The Compose BOM plus the baseline UI/Material3/tooling deps every
 *  Compose-enabled module needs -- applied identically whether that
 *  module is an application or a library. */
internal fun Project.addComposeDependencies() {
    val bom = libs.findLibrary("androidx-compose-bom").get()
    dependencies {
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
        add("implementation", libs.findLibrary("androidx-compose-ui").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
        add("implementation", libs.findLibrary("androidx-compose-material3").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
    }
}
