import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

val terminalConsoleVersion = "1.2.0"
val jlineVersion = "3.21.0"

dependencies {
    api(projects.core)

    implementation(libs.terminalconsoleappender) {
        exclude("org.apache.logging.log4j", "log4j-core")
        exclude("org.jline", "jline-reader")
        exclude("org.jline", "jline-terminal")
        exclude("org.jline", "jline-terminal-jna")
    }

    implementation(libs.bundles.jline)

    implementation(libs.bundles.log4j)
}

application {
    mainClass.set("org.geysermc.geyser.platform.standalone.GeyserStandaloneBootstrap")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Geyser-Standalone")

    transform(Log4j2PluginsCacheFileTransformer())
}