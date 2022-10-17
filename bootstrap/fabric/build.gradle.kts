plugins {
    id("fabric-loom") version "1.0-SNAPSHOT"
    id("maven-publish")
    id("com.github.johnrengelman.shadow")
    id("java")
}

java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

//archivesBaseName = project.archives_base_name
//version = project.mod_version
//group = project.maven_group

val minecraftVersion = project.property("minecraft_version") as String
val yarnVersion = project.property("yarn_mappings") as String
val loaderVersion = project.property("loader_version") as String
val fabricVersion = project.property("fabric_version") as String

dependencies {
    //to change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnVersion:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    // PSA: Some older mods, compiled on Loom 0.2.1, might have outdated Maven POMs.
    // You may need to force-disable transitiveness on them.

    api(projects.core)
    shadow(projects.core) {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "org.slf4j")
        exclude(group = "com.nukkitx.fastutil")
        exclude(group = "io.netty.incubator")
    }
}

repositories {
    mavenLocal()
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

tasks {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this task, sources will not be generated.
    sourcesJar {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    shadowJar {
        relocate("org.objectweb.asm", "org.geysermc.relocate.asm")
        relocate("org.yaml", "org.geysermc.relocate.yaml") // https://github.com/CardboardPowered/cardboard/issues/139
        relocate("com.fasterxml.jackson", "org.geysermc.relocate.jackson")
        relocate("net.kyori", "org.geysermc.relocate.kyori")
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        archiveBaseName.set("Geyser-Fabric")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}