package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.GlowVisual

/**
 * Valores canónicos del aura de habilidad (ÚNICA fuente de verdad). Los comparten el
 * contribuidor de animación ([AbilityGlowAnimations], aura por evento) y el indicador
 * PERSISTENTE del tablero ([PersistentAbilityGlow]) para que el juego muestre EXACTAMENTE
 * la misma aura que el Studio.
 *
 * - PASIVA: rojo, respiración lenta y estable, sin shimmer (estado ambiental).
 * - MANUAL: dorado, más brillante, con shimmer que recorre el borde (call-to-action).
 */
object AbilityGlowVisuals {
    val Passive = GlowVisual(hue = 4f, saturation = 0.9f, bloomRadius = 0.95f, edgeWidth = 0.06f, breathCycles = 2f, shimmer = 0f)
    val Manual = GlowVisual(hue = 45f, saturation = 0.9f, bloomRadius = 1.05f, edgeWidth = 0.07f, breathCycles = 3f, shimmer = 0.9f)

    fun forManual(manual: Boolean): GlowVisual = if (manual) Manual else Passive
}
