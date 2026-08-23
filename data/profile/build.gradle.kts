plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mineralord.tcg.data.profile"
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
}

dependencies {
    api(project(":engine:model"))
    // api: los tipos de cosméticos (CosmeticCategory, Cosmetic, …) forman parte de la API
    // pública de data:profile (PlayerProfile.equippedCosmetics, buyCosmetic), así que los
    // dependientes (data:cloud, app) los ven transitivamente.
    api(project(":data:cosmetics"))
    implementation(project(":data:cards"))
    implementation(project(":data:gacha"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
