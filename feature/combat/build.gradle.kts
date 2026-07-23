// feature:combat
//
// **La ÚNICA pantalla de combate del proyecto (UI canónica).**
//
// Contiene la implementación visual EXACTA del combate (CombatScreen + tapete, zonas, cartas, HUD,
// overlays, FX, geometría) que usa Pokémon TCG Clone. Se extrajo de `feature:game` para que el mismo
// `CombatScreen` lo usen DOS controladores a través de un contrato NEUTRAL ([CombatSceneController]):
// el juego (GameViewModel/OnlineGameController, dirigido por reglas) y el Studio (SandboxController,
// manual). Una sola implementación visual; cualquier mejora aquí beneficia a ambos.
//
// Arquitectura Dual (relajada por decisión de producto SÓLO para la UI de combate): depende de los
// MODELOS CANÓNICOS del combate (engine:rules→model/events/effects, data:cards) y del núcleo visual
// (core:designsystem). **NUNCA** de red/matchmaking/autenticación/Firebase (data:netplay, data:cloud,
// data:netfirestore): esa orquestación se queda en `feature:game`.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.mineralord.tcg.feature.combat"
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
    // Modelos canónicos del combate (engine:rules reexporta model/events/effects) + datos de cartas.
    api(project(":engine:rules"))
    api(project(":data:cards"))
    // Núcleo visual compartido (tokens, Motion, FlightOverlay, CardDetailDialog…).
    api(project(":core:designsystem"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
