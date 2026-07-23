// studio:assets
//
// **Asset Registry del Studio (APK 2): la fuente ÚNICA de verdad de los recursos del Studio.**
//
// Módulo Kotlin puro (JVM), sin Android ni Compose: aloja SÓLO el modelo de datos genérico de Assets
// y el registro/consulta. Nace registrando un único tipo (Animation), pero su forma está preparada
// para incorporar Audio, Partículas, Shaders, Modelos, UI, Fuentes, Temas… en el futuro sin rediseño
// (basta añadir un `AssetType` y un subtipo de `Asset`).
//
// Regla de Oro / Arquitectura Dual: es infraestructura EXCLUSIVA del Studio; el juego no la conoce.
// Depende únicamente del núcleo compartido puro `:core:animation` (para referenciar la
// `AnimationRequest` real que dispara el pipeline). Al ser JVM puro, es 100% testeable sin dispositivo.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Referencia al recurso real de los assets de animación: la request de dominio del pipeline.
    api(project(":core:animation"))

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
