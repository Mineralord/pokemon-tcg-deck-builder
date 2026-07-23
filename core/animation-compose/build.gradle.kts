// core:animation-compose
//
// Capa de integración Compose del framework de animaciones. Implementa los seams que
// `core:animation` (Kotlin puro) dejó abiertos: render por capas, overlay, Canvas,
// graphicsLayer y el registro de coordenadas (frontera lógico↔pantalla).
//
// `core:animation` NO se toca: sigue siendo puro. La dependencia fluye SOLO en este
// sentido (compose → puro), nunca al revés.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.mineralord.tcg.core.animationcompose"
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
    api(project(":core:animation"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui) // geometry/unit + snapshot state en tests JVM
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
