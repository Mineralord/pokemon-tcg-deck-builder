// app-studio — APK 2: Pokémon TCG Studio (herramienta de desarrollo).
//
// Cáscara delgada que ensambla el Studio sobre el MISMO núcleo compartido que el juego
// (Regla de Oro / Arquitectura Dual). Enlaza ÚNICAMENTE `studio:shell` (+ núcleo compartido
// transitivo). NO depende de features del juego (feature:*), ni de red/nube (data:netfirestore,
// data:cloud), ni de Firebase: por eso NO aplica el plugin google-services y no lleva secretos
// (invariantes 1 y 6 de la Arquitectura Dual). Ninguna app depende de la otra.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.mineralord.tcg.studio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mineralord.tcg.studio"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
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
    // Única feature del Studio: la cáscara/Shell. El núcleo compartido llega transitivamente.
    implementation(project(":studio:shell"))
    // Fuente única de verdad de los recursos del Studio (Asset Registry). Aporta el modelo de datos;
    // el catálogo concreto de animaciones se ensambla aquí, en el composition root (StudioAssets.kt).
    implementation(project(":studio:assets"))
    // La ÚNICA pantalla de combate (UI canónica compartida con el juego): el Studio EJECUTA la MISMA
    // CombatScreen, sólo cambia el controlador (SandboxController). feature:combat api-expone los
    // modelos canónicos (engine:rules→model, data:cards) y el núcleo visual (core:designsystem).
    // NO arrastra red/matchmaking/Firebase (eso vive en feature:game, del que el Studio NO depende).
    implementation(project(":feature:combat"))
    // LA PARTIDA (network-free): el Modo Partida del Match Builder hospeda el controlador REAL del juego
    // (GameViewModel: GameEngine + SmartAgent) reutilizando feature:match. NO arrastra red/Firebase
    // (eso vive en feature:game, del que el Studio SIGUE sin depender): Arquitectura Dual intacta.
    implementation(project(":feature:match"))
    // SIMULADOR DE APERTURA DE SOBRES: el Studio hospeda el MISMO flujo de sobres del juego
    // (feature:packs) a pantalla completa. feature:packs es network-free (gacha/cards/profile locales,
    // sin Firebase/red): compatible con la Arquitectura Dual, igual que feature:combat/match.
    implementation(project(":feature:packs"))
    // Motor de animaciones compartido (núcleo): el Studio HOSPEDA el mismo pipeline que el juego.
    // Es núcleo compartido (Compose puro, sin reglas de juego/red/Firebase): compatible con la
    // Arquitectura Dual y la Regla de Oro. `core:animation` llega transitivamente (api).
    implementation(project(":core:animation-compose"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)
}
