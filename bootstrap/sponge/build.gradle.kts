import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(project(":core"))
    compileOnly("org.spongepowered:spongeapi:7.1.0")
}

tasks.withType<ShadowJar> {
    dependencies {
        exclude {
                e -> e.name.startsWith("org.ow2.asm")
                || e.name.startsWith("org.yaml")
                || e.name.startsWith("com.google.code.gson")
        }
    }

    println(destinationDirectory.get())
    archiveFileName.set("Geyser-Sponge.jar")
    println(archiveFileName.get())
}

description = "bootstrap-sponge"