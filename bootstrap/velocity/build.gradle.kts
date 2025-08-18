plugins {
    id("geyser.platform-conventions")
    id("geyser.modrinth-uploading-conventions")
    alias(libs.plugins.runvelocity)
}

dependencies {
    annotationProcessor(libs.velocity.api)
    api(projects.core)

    compileOnly(libs.velocity.proxy)
    compileOnly(libs.netty.transport.native.io.uring)
    compileOnly(libs.netty.transport.native.kqueue)

    compileOnlyApi(libs.velocity.api)
    api(libs.cloud.velocity)
}

platformRelocate("com.fasterxml.jackson")
platformRelocate("it.unimi.dsi.fastutil")
platformRelocate("net.kyori.adventure.text.serializer.gson.legacyimpl")
platformRelocate("org.yaml")
platformRelocate("org.incendo")
platformRelocate("io.leangen.geantyref") // provided by cloud, should also be relocated
        
// These dependencies are already present on the platform
provided(libs.velocity.api)

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.velocity.GeyserVelocityMain"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Geyser-Velocity")

    dependencies {
        exclude(dependency("com.google.*:.*"))
        exclude(dependency("io.netty:.*"))
        exclude(dependency("org.slf4j:.*"))
        exclude(dependency("org.ow2.asm:.*"))
        // Exclude all Kyori dependencies
        exclude(dependency("net.kyori:.*:.*"))
    }
}

modrinth {
    uploadFile.set(tasks.getByPath("shadowJar"))
    loaders.addAll("velocity")
}

tasks {
    runVelocity {
        version(libs.versions.runvelocityversion.get())
    }
}
