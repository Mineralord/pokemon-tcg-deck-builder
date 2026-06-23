// Root build. Los plugins se declaran sin aplicar; cada módulo aplica lo que necesita.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}
