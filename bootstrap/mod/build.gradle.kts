architectury {
    common("neoforge", "fabric")
}

loom {
    mixin.defaultRefmapName.set("geyser-refmap.json")
}

dependencies {
    api(projects.core)
    compileOnly(libs.mixin)

    // Only here to suppress "unknown enum constant EnvType.CLIENT" warnings. DO NOT USE!
    compileOnly(libs.fabric.loader)
}