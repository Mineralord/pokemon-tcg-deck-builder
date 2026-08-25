package com.mineralord.tcg.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Paleta del "design system claro" de TCG Live, muestreada píxel a píxel del
 * video de referencia `BARAJAS PRIMERA VISTA.mp4` (1080×2400). Es la fuente de
 * verdad cromática para la nueva pantalla "Mis barajas" y las que la sigan.
 *
 * Los acentos por baraja (amarillo, rojo, teal…) NO viven aquí: los elige el
 * jugador y se derivan de [typeColor]. Esto solo fija el "chasis" neutro.
 */
object BarajasPalette {
    /** Fondo de pantalla: azul-gris muy claro con un leve degradado vertical. */
    val BgTop = Color(0xFFE7F0F6)
    val BgBottom = Color(0xFFE3EEF4)

    /** Superficies elevadas (tiles, píldoras, chip). Blanco puro con sombra suave. */
    val Surface = Color(0xFFFFFFFF)

    /** Celda "Crear nueva": hueca, un punto más oscura que las tiles. */
    val Hollow = Color(0xFFE4ECF6)
    val HollowBorder = Color(0xFFCFDBE8)

    /** Texto principal (título "Mis barajas"): casi negro. */
    val Ink = Color(0xFF24272A)

    /** Texto secundario atenuado (Editar, "?", "15/25", "Crear nueva"). */
    val Muted = Color(0xFF7C8794)

    /** Nombre de la baraja bajo el arte. */
    val DeckName = Color(0xFF687184)

    /** Borde tenue de píldoras/tiles. */
    val HairlineBorder = Color(0xFFD8E2EC)

    /** Iconos de la barra de navegación inferior. */
    val NavIcon = Color(0xFF2C4160)
    val NavIconInactive = Color(0xFFAFBBCB)

    /** Rojo de acción destructiva: badge "−" en modo edición y botón "Vale". */
    val DeleteRed = Color(0xFFF24139)

    /** Botón "Guardar" del modo edición: degradado cian. */
    val SaveGradient = listOf(Color(0xFF6CDCFD), Color(0xFF80D5F9))

    /** Velo tenue tras los diálogos modales (fondo claro atenuado). */
    val Scrim = Color(0xB3DDE7F1)

    /** Fila-cabecera oscura de menús contextuales (p. ej. "Crear nueva baraja"). */
    val MenuHeaderBg = Color(0xFF415268)

    /** Hilo divisor "arcoíris" bajo la cabecera (izq→der). */
    val DividerGradient = listOf(
        Color(0xFF50A9DB), // azul
        Color(0xFF2CBAD2), // cian
        Color(0xFF3DC5A1), // teal
        Color(0xFF78C662), // verde
        Color(0xFFC2C81F), // lima
    )
}
