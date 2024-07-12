import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

val Project.libs: LibrariesForLibs
    get() = rootProject.extensions.getByType()