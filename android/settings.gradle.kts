pluginManagement {
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

rootProject.name = "pokemon-tcg-live-clone"

// --- engine: Kotlin puro (JVM), sin dependencias de Android ---
include(":engine:model")
include(":engine:events")
include(":engine:effects")
include(":engine:rules")

// --- data: lógica de datos/gacha (Kotlin puro de momento) ---
include(":data:gacha")
include(":data:cards")

// --- core + feature + app: capa Android/Compose ---
include(":core:designsystem")
include(":feature:packs")
include(":app")
// include(":core:common")
// include(":core:designsystem")
// include(":core:ui")
// include(":data:cards")
// include(":data:gacha")
// include(":data:profile")
// include(":feature:packs")
// include(":feature:game")
// include(":app")
