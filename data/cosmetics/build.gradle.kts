// data:cosmetics — CATÁLOGO data-driven de cosméticos (contenido, no estado del jugador).
// Kotlin PURO JVM (como data:cards): lee recursos JSON del classpath, testeable en JVM y
// consumible por la app y el Studio. NO depende de Android, red ni del estado del jugador
// (data:profile depende de ESTE módulo, no al revés).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
