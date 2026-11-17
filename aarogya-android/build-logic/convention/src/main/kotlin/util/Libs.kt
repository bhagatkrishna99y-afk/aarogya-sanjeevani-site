import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * The generated type-safe `libs.foo` accessors only exist for scripts in
 * the *main* build; build-logic is a separate, included build, so its own
 * plugin code has to look the catalog up by name instead. Every
 * convention plugin reads versions/coordinates through this so there's
 * still exactly one source of truth (gradle/libs.versions.toml).
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
