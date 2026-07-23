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
include(":data:profile")
include(":data:cloud")
include(":data:netplay")
include(":data:netfirestore")

// --- core: sistema de animaciones (Kotlin puro JVM, independiente del engine y de la UI) ---
include(":core:animation")

// --- core: integración Compose del framework de animaciones (Android/Compose/render) ---
include(":core:animation-compose")

// --- core + feature + app: capa Android/Compose ---
include(":core:designsystem")
// --- feature:combat — la ÚNICA pantalla de combate (UI canónica), compartida por juego y Studio.
//     Sólo UI + modelos canónicos (engine/data:cards); NUNCA red/matchmaking/Firebase. ---
include(":feature:combat")
include(":feature:packs")
include(":feature:decks")
include(":feature:game")
include(":app")

// --- studio: cáscara del Pokémon TCG Studio (APK 2), sobre el mismo núcleo compartido ---
include(":studio:assets")
include(":studio:shell")
include(":app-studio")
// include(":core:common")
// include(":core:designsystem")
// include(":core:ui")
// include(":data:cards")
// include(":data:gacha")
// include(":data:profile")
// include(":feature:packs")
// include(":feature:game")
// include(":app")
