import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

val terminalConsoleVersion = "1.2.0"
val jlineVersion = "3.21.0"

dependencies {
    api(projects.core)

    implementation(libs.terminalconsoleappender) {
        exclude("org.apache.logging.log4j")
        exclude("org.jline")
    }

    implementation(libs.bundles.jline)

    implementation(libs.bundles.log4j)
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
}
