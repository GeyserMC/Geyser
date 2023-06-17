architectury {
    common("forge", "fabric")
}

dependencies {
    api(projects.core)

    compileOnly(libs.mixin)
}

repositories {
    mavenLocal()

    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}