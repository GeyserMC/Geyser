architectury {
    common("neoforge", "fabric")
}

loom {
    mixin.defaultRefmapName.set("geyser-refmap.json")
}

afterEvaluate {
    // We don't need these
    tasks.named("remapModrinthJar").configure {
        enabled = false
    }
}

dependencies {
    api(projects.core)
    compileOnly(libs.mixin)

    // Only here to suppress "unknown enum constant EnvType.CLIENT" warnings. DO NOT USE!
    compileOnly(libs.fabric.loader)
}