plugins {
    java
    `java-library`
    `maven-publish`
}

allprojects{
    apply(plugin = "java")
    apply(plugin = "java-library")

    group = "org.geysermc"

    java.sourceCompatibility = JavaVersion.VERSION_16
    java.targetCompatibility = JavaVersion.VERSION_16

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

repositories {
    gradlePluginPortal()
}

subprojects {
    apply(plugin = "maven-publish")

    repositories {
        mavenLocal()

        maven("https://repository.apache.org/snapshots/")
        maven("https://jitpack.io")
        maven("https://repo.opencollab.dev/maven-releases/")
        maven("https://repo.opencollab.dev/maven-snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.maven.apache.org/maven2/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/public/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.velocitypowered.com/snapshots/")
        maven("https://repo.viaversion.com")
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.20")
        annotationProcessor("org.projectlombok:lombok:1.18.20")

        compileOnly("org.checkerframework:checker-qual:3.19.0")
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}