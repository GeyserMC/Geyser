rootProject.name = "Geyser"

include(":bootstrap-velocity")
include(":bootstrap-sponge")
include(":bootstrap-parent")
include(":bootstrap-bungeecord")
include(":bootstrap-standalone")
include(":geyser-api")
include(":ap")
include(":common")
include(":base-api")
include(":api-parent")
include(":bootstrap-spigot")
include(":core")
project(":bootstrap-velocity").projectDir = file("bootstrap/velocity")
project(":bootstrap-sponge").projectDir = file("bootstrap/sponge")
project(":bootstrap-parent").projectDir = file("bootstrap")
project(":bootstrap-bungeecord").projectDir = file("bootstrap/bungeecord")
project(":bootstrap-standalone").projectDir = file("bootstrap/standalone")
project(":geyser-api").projectDir = file("api/geyser")
project(":base-api").projectDir = file("api/base")
project(":api-parent").projectDir = file("api")
project(":bootstrap-spigot").projectDir = file("bootstrap/spigot")

pluginManagement {
    plugins {
        id("com.github.johnrengelman.shadow") version "7.1.0"
        id("net.kyori.blossom") version "1.2.0"
    }
}
