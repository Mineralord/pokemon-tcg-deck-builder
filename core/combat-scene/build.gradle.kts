// core:combat-scene
//
// **La ÚNICA Combat Scene del proyecto (núcleo compartido).**
//
// Escena de combate VISUAL y reutilizable: renderiza tapete, zonas, cartas, HUD, botones, overlays
// y hospeda el `AnimationStage` + el motor canónico. NO conoce reglas, ni partidas, ni el Sandbox:
// se conduce por el contrato `CombatSceneController`. La usan ambos productos —el juego (con su
// Game Controller) y el Studio (con su Sandbox Controller)— para que exista una sola implementación
// visual del combate. Cualquier mejora visual aquí beneficia a los dos automáticamente.
//
// Regla de Oro / Arquitectura Dual: depende SÓLO de núcleo compartido (core:designsystem para la
// apariencia y core:animation-compose para el pipeline). Nunca de engine/reglas, data del juego,
// features del juego ni del Studio.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.mineralord.tcg.core.combatscene"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Apariencia (tokens/tema) y pipeline de animación: ambos núcleo compartido.
    api(project(":core:designsystem"))
    api(project(":core:animation-compose"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
