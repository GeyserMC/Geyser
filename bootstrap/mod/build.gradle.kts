architectury {
    common("neoforge", "fabric")
}

loom {
    mixin.defaultRefmapName.set("geyser-refmap.json")
}

dependencies {
    api(projects.core)
    compileOnly(libs.mixin)
}