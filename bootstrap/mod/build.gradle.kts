plugins {
    id("geyser.modded-conventions")
}

//architectury {
//    common("neoforge", "fabric")
//}

afterEvaluate {
    // We don't need these
    tasks.named("mergeShadowAndJarJar").configure {
        enabled = false
    }
}

dependencies {
    api(projects.core)
    compileOnly(libs.mixin)
    compileOnly(libs.mixinextras)

    // Only here to suppress "unknown enum constant EnvType.CLIENT" warnings. DO NOT USE!
    compileOnly(libs.fabric.loader)
}
