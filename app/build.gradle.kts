import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
}

// Credenciales de firma RELEASE: viven en `keystore.properties` (NO versionado, ver .gitignore),
// nunca en gradle.properties (que sí va al repo). Si el archivo no existe, la build release queda
// sin firmar y se omite la signingConfig: así cualquiera puede compilar debug sin tus claves.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystoreProps.getProperty("storeFile") != null

android {
    namespace = "com.mineralord.tcg.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mineralord.tcg"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "0.2.12"
    }

    signingConfigs {
        // Fuerza firma v1 (JAR) además de v2 para que el APK sea instalable por
        // sideload (explorador de archivos) en cualquier teléfono. AGP desactiva
        // v1 por defecto cuando minSdk>=24, lo que provoca "error al analizar el
        // paquete" en algunos parsers/OEM.
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
        // Firma RELEASE con TU propio keystore (clave estable, "desarrollador conocido"): reduce el
        // aviso agresivo de Play Protect al instalar por sideload. Solo se registra si existe
        // keystore.properties; sus valores NO se versionan.
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Firma la APK que repartes a tus amigos con tu clave estable (si está configurada).
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
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
    implementation(project(":core:designsystem"))
    implementation(project(":feature:carddetail"))
    implementation(project(":feature:packs"))
    implementation(project(":feature:decks"))
    implementation(project(":feature:combat"))
    implementation(project(":feature:game"))
    implementation(project(":data:cards"))
    implementation(project(":data:cosmetics"))
    implementation(project(":data:gacha"))
    implementation(project(":data:profile"))
    implementation(project(":data:cloud"))
    implementation(project(":data:netplay"))
    implementation(project(":data:netfirestore"))
    implementation(project(":engine:model"))
    implementation(project(":engine:rules"))

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)
    implementation(libs.play.services.auth)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
