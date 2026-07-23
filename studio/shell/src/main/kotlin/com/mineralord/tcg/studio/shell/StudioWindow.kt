package com.mineralord.tcg.studio.shell

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * **Clase de tamaño del área de trabajo del Studio (adaptación de pantalla, permanente).**
 *
 * Codifica los *breakpoints de ancho oficiales de Material 3 / Window Size Classes* (compacto <600dp,
 * medio 600–839dp, expandido ≥840dp), pero medidos sobre el **workspace real** que el Shell tiene
 * disponible —no sobre la ventana física—. Esto lo hace correcto por construcción en multi-ventana,
 * pantalla dividida, plegables y ChromeOS, sin depender de la Activity ni de artefactos externos.
 *
 * El Shell deriva esta clase (ver `StudioShell` con `BoxWithConstraints`) y la usa para elegir un
 * único sistema adaptativo (Rail lateral vs. Rail inferior); **no existen layouts Portrait/Landscape
 * duplicados**. Los Labs nunca ven esta clase: reciben un área ya adaptada.
 */
@Immutable
enum class StudioWindowWidth {
    /** Teléfono en Portrait o ventana estrecha. Navegación en Rail inferior. */
    Compact,

    /** Teléfono grande en Landscape / tablet pequeña. Rail lateral. */
    Medium,

    /** Tablet / plegable abierto / ChromeOS. Rail lateral más ancho. */
    Expanded;

    companion object {
        // Umbrales oficiales de ancho de Window Size Classes (Material 3).
        val MediumThreshold: Dp = 600.dp
        val ExpandedThreshold: Dp = 840.dp

        /** Clasifica un ancho disponible según los umbrales oficiales. */
        fun fromWidth(width: Dp): StudioWindowWidth = when {
            width < MediumThreshold -> Compact
            width < ExpandedThreshold -> Medium
            else -> Expanded
        }
    }
}
