plugins {
    id("com.modrinth.minotaur")
}

// Ensure that the readme is synched
tasks.modrinth.get().dependsOn(tasks.modrinthSyncBody)

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN") ?: "") // Even though this is the default value, apparently this prevents GitHub Actions caching the token?
    projectId.set("geyser")
    versionNumber.set(project.version as String + "-" + System.getenv("BUILD_NUMBER"))
    versionType.set("beta")
    changelog.set(System.getenv("CHANGELOG") ?: "")
    gameVersions.add(libs.minecraft.get().version as String)
    failSilently.set(true)

    syncBodyFrom.set(rootProject.file("README.md").readText())
}