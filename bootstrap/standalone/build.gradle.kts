import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    application
    id("geyser.platform-conventions")
}

dependencies {
    api(projects.core)

    implementation(libs.terminalconsoleappender) {
        exclude("org.apache.logging.log4j")
        exclude("org.jline")
    }

    implementation(libs.bundles.jline)
    implementation(libs.bundles.log4j)

    implementation(libs.gson.runtime)
}

application {
    mainClass.set("org.geysermc.geyser.platform.standalone.GeyserStandaloneBootstrap")
}

tasks.named<Jar>("jar") {
    manifest {
        // log4j provides multi-release java 9 code which resolves https://github.com/GeyserMC/Geyser/issues/3693
        attributes("Multi-Release" to true)
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Geyser-Standalone")

    transform(Log4j2PluginsCacheFileTransformer())
}

tasks.named<JavaExec>("run") {
    val dir = projectDir.resolve("run")
    dir.mkdirs()
    jvmArgs("-Dio.netty.leakDetection.level=PARANOID")
    workingDir = dir

    standardInput = System.`in`
}

// Manual task to print the supported Java & Bedrock game versions as JSON. 
// This is parsed by the CI to include in the Downloads API
// Invoke with ./gradlew :standalone:printMinecraftVersions
tasks.register<JavaExec>("printMinecraftVersions") {
    group = "ci"
    description = "Print supported Java & Bedrock versions"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.geysermc.geyser.platform.standalone.GeyserStandaloneBootstrap")

    args("--print-minecraft-versions")
}