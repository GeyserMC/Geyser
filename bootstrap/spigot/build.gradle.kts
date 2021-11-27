import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(project(":core"))
    implementation("org.geysermc.geyser.adapters:spigot-all:1.2-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.15.2-R0.1-SNAPSHOT")
    compileOnly("com.viaversion:viaversion:4.0.0")
}

tasks.withType<ShadowJar> {
    dependencies {
        exclude {
                e -> e.name.startsWith("io.netty")
                || e.name.startsWith("org.yaml")
                || e.name.startsWith("com.google")
        }
    }

    println(destinationDirectory.get())
    archiveFileName.set("Geyser-Spigot.jar")
    println(archiveFileName.get())
}

description = "bootstrap-spigot"