// feature:match — LA PARTIDA como concepto arquitectónico propio (network-free).
//
// Contiene la orquestación de una partida reutilizable por CUALQUIER anfitrión: PvE (GameViewModel +
// SmartAgent), y —vía GameCore/GameController— también el modo Online, replays y espectadores. NO conoce
// red ni Firebase (eso vive en feature:game / data:netplay). Por ser network-free, el Studio puede
// depender de este módulo sin violar la Arquitectura Dual.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.mineralord.tcg.feature.match"
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
    // La pantalla de combate compartida (contrato neutral CombatSceneController + view-state GameUiState).
    implementation(project(":feature:combat"))
    implementation(project(":core:designsystem"))
    // :engine:rules reexporta (api) model/events/effects.
    implementation(project(":engine:rules"))
    implementation(project(":data:cards"))
    implementation(project(":data:profile"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
}
