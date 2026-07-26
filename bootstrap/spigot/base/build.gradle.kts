plugins {
    id("geyser.platform-base-conventions")
}

// TODO isolation

dependencies {
    api(projects.core)
    api(libs.erosion.bukkit.common) {
        isTransitive = false
    }

    implementation(libs.erosion.bukkit.nms) {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
        }
    }

    implementation(variantOf(libs.adapters.spigot) {
        classifier("all") // otherwise the unshaded jar is used without the shaded NMS implementations
    })
    implementation(variantOf(libs.adapters.paper) {
        classifier("all") // otherwise the unshaded jar is used without the shaded NMS implementations
    })

    implementation(libs.cloud.paper)
    implementation(libs.commodore)

    compileOnly(libs.folia.api)

    compileOnlyApi(libs.viaversion)

    implementation(libs.floodgate.spigot)
    compileOnly("com.mojang", "authlib", "1.5.21")

    // For 1.16.5/1.17.1
    implementation(libs.gson.record.factory) {
        isTransitive = false
    }
}

platformRelocate("it.unimi.dsi.fastutil")
// Relocate net.kyori but exclude the component logger
platformRelocate("net.kyori", "net.kyori.adventure.text.logger.slf4j.ComponentLogger")
platformRelocate("org.objectweb.asm")
platformRelocate("me.lucko.commodore")
platformRelocate("org.incendo")
platformRelocate("io.leangen.geantyref") // provided by cloud and Configurate, should also be relocated
platformRelocate("org.yaml") // Broken as of 1.20
platformRelocate("marcono1234.gson")
platformRelocate("org.bstats")

provided(libs.viaversion)
provided("com.mojang", "authlib")

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {

    // Prevents Paper 1.20.5+ from remapping Geyser
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }

    archiveBaseName.set("Geyser-Spigot-Base")

    dependencies {
        exclude(dependency("com.google.*:.*"))

        // Needed because older Spigot builds do not provide the haproxy module
        exclude("io.netty", libs.netty.codec.haproxy)

        // Commodore includes Brigadier
        exclude(dependency("com.mojang:.*"))
    }
}
