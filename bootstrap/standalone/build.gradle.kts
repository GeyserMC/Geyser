import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

val terminalConsoleVersion = "1.2.0"
val jlineVersion = "3.10.0"

dependencies {
    api(projects.core)

    implementation("net.minecrell", "terminalconsoleappender", terminalConsoleVersion) {
        exclude("org.apache.logging.log4j", "log4j-core")
        exclude("org.jline", "jline-reader")
        exclude("org.jline", "jline-terminal")
        exclude("org.jline", "jline-terminal-jna")
    }

    implementation("org.jline", "jline-terminal", jlineVersion)
    implementation("org.jline", "jline-terminal-jna", jlineVersion)
    implementation("org.jline", "jline-reader", jlineVersion)

    implementation("org.apache.logging.log4j", "log4j-api", Versions.log4jVersion)
    implementation("org.apache.logging.log4j", "log4j-core", Versions.log4jVersion)
    implementation("org.apache.logging.log4j", "log4j-slf4j18-impl", Versions.log4jVersion)
}

application {
    mainClass.set("org.geysermc.geyser.platform.standalone.GeyserStandaloneBootstrap")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Geyser")

    transform(Log4j2PluginsCacheFileTransformer())
}