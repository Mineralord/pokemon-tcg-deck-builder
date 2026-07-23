package com.mineralord.tcg.core.animation

/**
 * Parámetros visuales PUROS de una animación de evolución. Son "perillas" numéricas
 * (sin tipos de Compose) que describen COMO se ve una variante de evolución; el
 * ejecutor/renderer del mundo Compose las interpreta a lo largo del progreso 0→1.
 *
 * Gracias a que la identidad de cada variante vive como DATOS aquí, cinco evoluciones
 * distintas son cinco [EvolveVisual] distintos dentro de la misma [AnimationDefinition]
 * y el mismo pipeline — sin código nuevo por variante (Open-Closed llevado a los datos).
 *
 * @param rise fracción de la altura de la carta que asciende en el pico.
 * @param spin vueltas completas de giro en el plano (rotationZ).
 * @param flip número de volteos horizontales (efecto "morfología"/carta que gira de canto).
 * @param pulse escala extra en el pico (bombeo).
 * @param flash alfa pico del destello blanco de completado.
 * @param ring radio máximo del anillo expansivo, en múltiplos del ancho de la carta.
 * @param hue matiz del anillo/glow en grados (0..360).
 */
data class EvolveVisual(
    val rise: Float = 0f,
    val spin: Float = 0f,
    val flip: Float = 0f,
    val pulse: Float = 0f,
    val flash: Float = 0f,
    val ring: Float = 0f,
    val hue: Float = 0f,
)
