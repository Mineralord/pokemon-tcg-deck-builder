// core:animation
//
// Módulo Kotlin puro (JVM) que aloja la ARQUITECTURA del sistema de animaciones.
// Deliberadamente NO depende de Android ni de Jetpack Compose: el AnimationDirector
// es un orquestador de dominio que sólo produce estado observable (StateFlow). La
// reproducción real de animaciones vivirá en la capa Compose (core:designsystem /
// feature:game), que observará este módulo. Así el director es 100% testeable en JVM
// y permanece independiente tanto del Game Engine como del framework de UI.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Único requisito: StateFlow/coroutines para exponer estado observable.
    api(libs.kotlinx.coroutines.core)

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
