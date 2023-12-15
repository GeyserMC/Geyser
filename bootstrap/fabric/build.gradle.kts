import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("fabric-loom") version "1.0-SNAPSHOT"
    id("com.modrinth.minotaur") version "2.+"
}

dependencies {
    //to change the versions see the gradle.properties file
    minecraft(libs.fabric.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.loader)

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation(libs.fabric.api)

    // This should be in the libs TOML, but something about modImplementation AND include just doesn't work
    include(modImplementation("me.lucko", "fabric-permissions-api", "0.2-SNAPSHOT"))

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

loom {
    mixin.defaultRefmapName.set("geyser-fabric-refmap.json")
}

repositories {
    mavenLocal()
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

application {
    mainClass.set("org.geysermc.geyser.platform.fabric.GeyserFabricMain")
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
        // Mirrors the example fabric project, otherwise tons of dependencies are shaded that shouldn't be
        configurations = listOf(project.configurations.shadow.get())
        // The remapped shadowJar is the final desired Geyser-Fabric.jar
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("shaded")

        relocate("org.objectweb.asm", "org.geysermc.relocate.asm")
        relocate("org.yaml", "org.geysermc.relocate.yaml") // https://github.com/CardboardPowered/cardboard/issues/139
        relocate("com.fasterxml.jackson", "org.geysermc.relocate.jackson")
        relocate("net.kyori", "org.geysermc.relocate.kyori")

        dependencies {
            // Exclude everything EXCEPT some DNS stuff required for HAProxy
            exclude(dependency("io.netty:netty-transport-classes-epoll:.*"))
            exclude(dependency("io.netty:netty-transport-native-epoll:.*"))
            exclude(dependency("io.netty:netty-transport-native-unix-common:.*"))
            exclude(dependency("io.netty:netty-transport-classes-kqueue:.*"))
            exclude(dependency("io.netty:netty-transport-native-kqueue:.*"))
            exclude(dependency("io.netty:netty-handler:.*"))
            exclude(dependency("io.netty:netty-common:.*"))
            exclude(dependency("io.netty:netty-buffer:.*"))
            exclude(dependency("io.netty:netty-resolver:.*"))
            exclude(dependency("io.netty:netty-transport:.*"))
            exclude(dependency("io.netty:netty-codec:.*"))
            exclude(dependency("io.netty:netty-resolver-dns:.*"))
            exclude(dependency("io.netty:netty-resolver-dns-native-macos:.*"))
        }
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        archiveBaseName.set("Geyser-Fabric")
        archiveVersion.set("")
        archiveClassifier.set("")
    }

    register("remapModrinthJar", RemapJarTask::class) {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        archiveBaseName.set("geyser-fabric")
        archiveVersion.set(project.version.toString() + "+build."  + System.getenv("GITHUB_RUN_NUMBER"))
        archiveClassifier.set("")
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN")) // Even though this is the default value, apparently this prevents GitHub Actions caching the token?
    projectId.set("wKkoqHrH")
    versionNumber.set(project.version as String + "-" + System.getenv("GITHUB_RUN_NUMBER"))
    versionType.set("beta")
    changelog.set("A changelog can be found at https://github.com/GeyserMC/Geyser/commits")

    syncBodyFrom.set(rootProject.file("README.md").readText())

    uploadFile.set(tasks.getByPath("remapModrinthJar"))
    gameVersions.addAll("1.20.4")

    loaders.add("fabric")
    failSilently.set(true)

    dependencies {
        required.project("fabric-api")
    }
}
