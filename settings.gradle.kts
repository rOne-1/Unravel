pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Unravel"

include(
    ":app",
    ":core:dsa-engine",
    ":core:canvas-renderer",
    ":core:design-system",
    ":core:database",
    ":feature:skill-map",
    ":feature:sandbox",
    ":feature:race-mode"
)
