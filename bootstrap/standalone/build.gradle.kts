import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(project(":core"))
    implementation("net.minecrell:terminalconsoleappender:1.2.0")
    implementation("org.jline:jline-terminal:3.20.0")
    implementation("org.jline:jline-terminal-jna:3.20.0")
    implementation("org.jline:jline-reader:3.20.0")
    implementation("org.apache.logging.log4j:log4j-api:2.13.1")
    implementation("org.apache.logging.log4j:log4j-core:2.13.2")
    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.13.1")
}

tasks.withType<ShadowJar> {
    println(destinationDirectory.get())
    archiveFileName.set("Geyser-Standalone.jar")
    println(archiveFileName.get())
}

description = "bootstrap-standalone"