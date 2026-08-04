package com.mineralord.tcg.core.animation

/**
 * Parámetros visuales PUROS del aura de habilidad ([AnimationStep.AbilityGlow]). "Perillas"
 * numéricas sin tipos de Compose; el renderer las interpreta a lo largo del progreso 0→1.
 *
 * El carácter de cada modo vive como DATOS, no como ramas de código:
 * - PASIVA (estado ambiental): rojo, respiración lenta y estable, sin shimmer de borde.
 * - MANUAL (accionable/invitación): dorado, más brillante, con shimmer que recorre el borde
 *   (call-to-action, como el glow de carta jugable de Hearthstone).
 *
 * @param hue matiz del bloom/rim en grados (0..360). Rojo pasiva≈0, dorado manual≈45.
 * @param saturation saturación del color del aura (0f..1f).
 * @param bloomRadius radio máximo del bloom radial POR DEBAJO de la carta, en múltiplos del ancho.
 * @param edgeWidth grosor del rim-glow del borde, en múltiplos del ancho de la carta.
 * @param breathCycles ciclos de respiración (pulso de intensidad) durante toda la duración.
 * @param shimmer intensidad del reflejo que recorre el borde (0f = sin shimmer → pasiva; >0 → manual).
 */
data class GlowVisual(
    val hue: Float = 0f,
    val saturation: Float = 0.85f,
    val bloomRadius: Float = 0.9f,
    val edgeWidth: Float = 0.06f,
    val breathCycles: Float = 2f,
    val shimmer: Float = 0f,
)
