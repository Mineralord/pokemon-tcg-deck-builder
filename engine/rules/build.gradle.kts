plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":engine:model"))
    api(project(":engine:events"))
    api(project(":engine:effects"))
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
