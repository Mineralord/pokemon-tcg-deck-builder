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
    // Motor de animaciones compartido (núcleo): el Studio HOSPEDA el mismo pipeline que el juego.
    // Es núcleo compartido (Compose puro, sin reglas de juego/red/Firebase): compatible con la
    // Arquitectura Dual y la Regla de Oro. `core:animation` llega transitivamente (api).
    implementation(project(":core:animation-compose"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)
}
