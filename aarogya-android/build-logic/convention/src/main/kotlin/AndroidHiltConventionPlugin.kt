import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Applied by every module that uses Hilt -- both apps (`@HiltAndroidApp`)
 * and any `:core:*` module that provides a `@Module`/`@InstallIn` (e.g.
 * :core:network's Supabase client, :core:data's repository bindings).
 * KSP generates Hilt's code everywhere it's applied; full component
 * aggregation only actually happens at the app module, same as any
 * other multi-module Hilt setup.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("com.google.dagger.hilt.android")
            }

            dependencies {
                add("implementation", libs.findLibrary("hilt-android").get())
                add("ksp", libs.findLibrary("hilt-android-compiler").get())
            }
        }
    }
}
