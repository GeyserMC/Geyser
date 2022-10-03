plugins {
    id("fabric-loom") version "0.12-SNAPSHOT"
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
    processResources {
//        inputs.property "version", project.version
//
//        filesMatching("fabric.mod.json") {
//            expand "version": project.version
//        }
    }

    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    compileJava {
        options.encoding = "UTF-8"
    }

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

    jar {
        from("LICENSE")
    }

    remapJar {
        dependsOn(shadowJar)
        inputs.file(shadowJar.get().archiveFile)
        archiveBaseName.set("Geyser-Fabric")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}