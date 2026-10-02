package com.mineralord.tcg.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Convierte este composable en una BARRERA de toques: absorbe los toques (taps) que caen en sus
 * zonas sin elementos interactivos, de modo que NO lleguen a capas situadas DEBAJO (pantallas u
 * overlays anteriores). Al abrir una nueva interfaz encima, solo esa interfaz responde.
 *
 * Implementado como un `clickable` SILENCIOSO (sin ripple ni acción): captura los taps pero NO
 * interfiere con el desplazamiento (scroll) ni con los arrastres de los hijos — esos son gestos de
 * arrastre que los hijos reclaman primero. Aplicar al contenedor raíz de la capa superior.
 */
fun Modifier.blockPassThroughTouches(): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    clickable(interactionSource = interaction, indication = null, onClick = {})
}
