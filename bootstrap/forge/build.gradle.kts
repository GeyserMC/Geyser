import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.common.util.RunConfig
import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace

plugins {
    id("eclipse")
    id("net.minecraftforge.gradle") version("5.1.+")
    id("org.spongepowered.mixin") version("0.7.+")
}

val modId = "geyser_forge"
val minecraftVersion = "1.19.4"

minecraft {
    mappings("official", minecraftVersion)

    runs(closureOf<NamedDomainObjectContainer<RunConfig>> {
        create("client") {
            workingDirectory(project.file("run"))
            ideaModule = "${rootProject.name}.${project.name}.main"
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            mods.create(modId).sources(sourceSets.main.get())
        }

        create("server") {
            configurations {
                listOf(project.configurations.shadow.get())
            }
            workingDirectory(project.file("run"))
            ideaModule = "${rootProject.name}.${project.name}.main"
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            mods.create(modId).sources(sourceSets.main.get())
        }

        create("data") {
            workingDirectory(project.file("run"))
            ideaModule = "${rootProject.name}.${project.name}.main"
            args("--mod", modId,
                "--all",
                "--output", file("src/generated/resources/").toString(),
                "--existing", file("src/main/resources/").toString())
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            mods.create(modId).sources(sourceSets.main.get())
        }
    })
}

mixin {
    add(sourceSets.main.get(), "geyser_forge.mixins.refmap.json")
    config("geyser_forge.mixins.json")
}

dependencies {
    minecraft("net.minecraftforge:forge:1.19.4-45.0.63")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

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

    maven("https://maven.minecraftforge.net/")
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

application {
    mainClass.set("org.geysermc.geyser.platform.forge.GeyserForgeMain")
}

tasks {
    jar {
        archiveClassifier.set("unshaded")
    }

    shadowJar {
        // Mirrors the example forge project, otherwise tons of dependencies are shaded that shouldn't be
        configurations = listOf(project.configurations.shadow.get())
        // The remapped shadowJar is the final desired Geyser-Forge.jar
        archiveBaseName.set("Geyser-Forge")
        // archiveVersion.set(project.version.toString())
        archiveClassifier.set("")

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

    reobf {
        shadowJar {
        }
    }
}

afterEvaluate {
    val shadowJar = tasks.named<ShadowJar>("shadowJar").get()
    val reobfJar = tasks.named<RenameJarInPlace>("reobfJar").get()
    val build = tasks.named<DefaultTask>("build").get()

    reobfJar.dependsOn(shadowJar)
    reobfJar.input.set(shadowJar.archiveFile)
    build.dependsOn(reobfJar)
}