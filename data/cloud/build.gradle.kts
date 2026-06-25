plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mineralord.tcg.data.cloud"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        // El Web Client ID de OAuth (tipo "Web application") se lee de
        // gradle.properties (clave GOOGLE_SERVER_CLIENT_ID). Es la "audiencia"
        // del ID token; sin él, el inicio de sesión con Google fallará.
        val serverClientId = (project.findProperty("GOOGLE_SERVER_CLIENT_ID") as String?) ?: ""
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$serverClientId\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(project(":data:profile"))
    implementation(project(":data:cards"))
    implementation(project(":data:gacha"))
    implementation(project(":engine:model"))

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)
    implementation(libs.play.services.auth)
}
