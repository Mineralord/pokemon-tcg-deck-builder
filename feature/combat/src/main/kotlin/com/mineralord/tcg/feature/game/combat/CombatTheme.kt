package com.mineralord.tcg.feature.game.combat

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * REBUILD de la pantalla de combate — identidad propia (minimalista premium),
 * NO reutiliza el fondo horneado ni la geometría 1:1 medida: todo es dinámico,
 * responsive y por Composables. Este objeto centraliza los tokens de diseño
 * (color, espaciado, radios) para mantener la coherencia y evolucionar el look
 * desde un único sitio.
 */
object CombatTheme {
    val BgTop = Color(0xFF161B26)
    val BgBottom = Color(0xFF0A0D13)
    val Surface = Color(0xFF1C2434)
    val Border = Color(0x1FFFFFFF)
    val OnSurface = Color(0xFFEAECF2)
    val Muted = Color(0xFF8B93A7)
    val Mine = Color(0xFF4DA3FF)
    val Foe = Color(0xFFE0555F)
    val Gold = Color(0xFFF0C24B)
    val Good = Color(0xFF54C08A)

    val Gap = 8.dp
    val CardCorner = 9.dp
    val PanelCorner = 16.dp

    /** Aspecto de carta (ancho/alto) — nunca se deforma el arte. */
    const val CardAspect = 106f / 148f

    fun background(): Brush = Brush.verticalGradient(listOf(BgTop, BgBottom))
}
