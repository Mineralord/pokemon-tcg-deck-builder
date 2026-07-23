// studio:shell
//
// Cáscara del Pokémon TCG Studio (APK 2): el CONTENEDOR PERMANENTE (Boot + Shell, 11.1).
// No aloja ninguna herramienta todavía; solo el chasis de regiones (Toolbar · Rail · Host ·
// Status Bar) que hospedará los Labs futuros.
//
// Regla de Oro / Arquitectura Dual: depende ÚNICAMENTE del núcleo compartido (core:designsystem);
// nunca de features del juego, red ni Firebase. Toda apariencia sale del Design System congelado
// (cadena `Component → ComponentStyle → Visual Tokens → Theme`): cero tokens/componentes nuevos.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.mineralord.tcg.studio.shell"
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
    // Único enlace: el núcleo compartido de diseño (tokens + ComponentStyle del PDS).
    api(project(":core:designsystem"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
