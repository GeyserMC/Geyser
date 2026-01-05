plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()

    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.architectury.dev/")
}

dependencies {
    // This is for the LibsAccessor.kt hack
    // this is OK as long as the same version catalog is used in the main build and build-logic
    // see https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    // This is for applying plugins, and using the version from the libs.versions.toml
    // Unfortunately they still need to be applied by their string name in the convention scripts.
    implementation(libs.lombok)
    implementation(libs.indra)
    implementation(libs.shadow)
    implementation(libs.architectury.plugin)
    implementation(libs.architectury.loom)
    implementation(libs.minotaur)
    implementation(libs.loom.companion)
}
