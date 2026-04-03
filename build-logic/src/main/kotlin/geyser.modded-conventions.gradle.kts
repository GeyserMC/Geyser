@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.dependencies

plugins {
    id("geyser.platform-conventions")
    id("architectury-plugin")
    id("dev.architectury.loom-no-remap")
}

// These are provided by Minecraft/modded platforms already, no need to include them
provided("com.google.code.gson", "gson")
provided("com.google.guava", ".*")
provided("org.slf4j", "slf4j-api")
provided("com.nukkitx.fastutil", ".*")
provided("org.cloudburstmc.fastutil.maps", ".*")
provided("org.cloudburstmc.fastutil.sets", ".*")
provided("org.cloudburstmc.fastutil.commons", ".*")
provided("org.cloudburstmc.fastutil", ".*")
provided("org.checkerframework", "checker-qual")
provided("io.netty", "netty-transport-classes-epoll")
provided("io.netty", "netty-transport-native-epoll")
provided("io.netty", "netty-transport-native-unix-common")
provided("io.netty", "netty-transport-classes-kqueue")
provided("io.netty", "netty-transport-native-kqueue")
provided("io.netty", "netty-transport-native-io_uring")
provided("io.netty", "netty-transport-classes-io_uring")
provided("io.netty", "netty-handler")
provided("io.netty", "netty-common")
provided("io.netty", "netty-buffer")
provided("io.netty", "netty-resolver")
provided("io.netty", "netty-transport")
provided("io.netty", "netty-codec")
provided("io.netty", "netty-codec-base")
provided("org.ow2.asm", "asm")

// cloud-fabric/cloud-neoforge jij's all cloud depends already
provided("org.incendo", ".*")
provided("io.leangen.geantyref", "geantyref")

architectury {
    minecraft = libs.minecraft.get().version as String
}

loom {
    silentMojangMappingsLicense()
}

indra {
    javaVersions {
        target(25)
    }
}

configurations {
    create("includeTransitive").isTransitive = true
    create("shadowBundle") {
        isCanBeResolved = true
        isCanBeConsumed = false
        isTransitive = false
    }
}

tasks {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this task, sources will not be generated.
    sourcesJar {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    jar {
        archiveClassifier.set("raw")
    }

    shadowJar {
        dependsOn(jar)

        // Mirrors the example fabric project, otherwise tons of dependencies are shaded that shouldn't be
        configurations = listOf(project.configurations.getByName("shadowBundle"))
        archiveBaseName.set("${project.name}-shaded")
        mergeServiceFiles()
    }

    // This task combines the output of the "jar" task, which includes JiJ dependencies,
    // and the shadowJar for the final jar.
    // thanks bluemap
    // https://github.com/BlueMap-Minecraft/BlueMap/blob/cfe73115dc4d1bdd97bc659f41364da65a6a2179/implementations/fabric/build.gradle.kts#L93-L107
    register<Jar>("mergeShadowAndJarJar") {
        dependsOn( tasks.shadowJar, tasks.jar )
        // from sources / final name are configured in the respective projects
        archiveVersion.set("")
        archiveClassifier.set("")
    }

    tasks.register<Copy>("renameModrinthJar") {
        val sourceJar = tasks.named<Jar>("mergeShadowAndJarJar")
        dependsOn(sourceJar)

        from(sourceJar.flatMap { it.archiveFile })
        into(layout.buildDirectory.dir("libs"))

        rename { "${versionName(project)}.jar" }
    }

    build {
        dependsOn(tasks.getByName("mergeShadowAndJarJar"))
    }
}

afterEvaluate {
    val providedDependencies = providedDependencies[project.name]!!
    val shadedDependencies = configurations.getByName("shadowBundle").resolvedConfiguration.resolvedArtifacts.stream()
        .map { dependency -> "${dependency.moduleVersion.id.module}" }.toList()

    // Now: Include all transitive dependencies that aren't excluded
    configurations["includeTransitive"].resolvedConfiguration.resolvedArtifacts.forEach { dep ->
        val name = "${dep.moduleVersion.id.group}:${dep.moduleVersion.id.name}"
        if (!shadedDependencies.contains(name) and !providedDependencies.contains(name)
            and !providedDependencies.contains("${dep.moduleVersion.id.group}:.*")
        ) {
            println("Including dependency via JiJ: ${dep.id}")
            dependencies.add("include", dep.moduleVersion.id.toString())
        } else {
            println("Not including ${dep.id} for ${project.name}!")
        }
    }
}

dependencies {
    minecraft(libs.minecraft)
}
