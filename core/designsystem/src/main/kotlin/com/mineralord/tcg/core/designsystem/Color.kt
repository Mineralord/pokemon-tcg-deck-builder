package com.mineralord.tcg.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Tokens de color del clon, derivados del análisis visual de las referencias
 * (§0 del blueprint). Aproximan la identidad de TCG Live: cabecera roja, fondo
 * crema, acentos dorados (botón JUGAR / arcos del tablero) y campo navy.
 *
 * Son valores semilla; se afinarán con muestreo de píxel fino sobre los frames.
 */
object TcgColors {
    val Red = Color(0xFFC8202C)          // cabecera / cinta superior
    val RedDark = Color(0xFF8E1620)
    val Cream = Color(0xFFF3EFE7)        // fondo inferior
    val Gold = Color(0xFFE4AB5E)         // botón JUGAR / arcos activos
    val GoldDark = Color(0xFFB8842F)
    val Navy = Color(0xFF04152B)         // campo de juego
    val NavyLight = Color(0xFF1E2F45)
    val Ink = Color(0xFF1A1A1A)          // texto principal
    val Parchment = Color(0xFFFFFFFF)

    // Acentos por tipo de energía (para badges de HP y costes).
    val TypeLightning = Color(0xFFF2C500)
    val TypeFire = Color(0xFFE8503A)
    val TypeWater = Color(0xFF3AA6E8)
    val TypeGrass = Color(0xFF4CAF50)
    val TypePsychic = Color(0xFF9B59B6)
    val TypeFighting = Color(0xFFC8702A)
    val TypeDarkness = Color(0xFF34495E)
    val TypeMetal = Color(0xFF95A5A6)
}
