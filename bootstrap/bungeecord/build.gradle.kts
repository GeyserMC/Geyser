import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(project(":core"))
    compileOnly("com.github.SpigotMC.BungeeCord:bungeecord-proxy:a7c6ede")
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
    archiveFileName.set("Geyser-BungeeCord.jar")
    println(archiveFileName.get())
}

description = "bootstrap-bungeecord"