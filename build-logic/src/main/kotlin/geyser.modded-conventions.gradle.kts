import org.gradle.kotlin.dsl.dependencies

plugins {
    id("geyser.platform-conventions")
    id("architectury-plugin")
    id("dev.architectury.loom")
}

architectury {
    minecraft = "1.19.4"
}

loom {
    silentMojangMappingsLicense()
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
        // The remapped shadowJar is the final desired mod jar
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
}

dependencies {
    minecraft("com.mojang:minecraft:1.19.4")
    mappings(loom.officialMojangMappings())
}