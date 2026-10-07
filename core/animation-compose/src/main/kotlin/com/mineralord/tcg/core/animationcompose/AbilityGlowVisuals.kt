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

    // Indicadores de JUGABILIDAD de la mano / Activo (misma aura, distinto color), estilo TCG Live:
    // - PLAYABLE: azul, respiración suave sin shimmer → "esta carta se puede usar ahora" (Objetos,
    //   Herramientas, Apoyos no gastados, Energía no adjuntada) y "el Activo puede atacar".
    // - EVOLVE: amarillo con leve shimmer (call-to-action) → "esta carta hace evolucionar algo en juego".
    val Playable = GlowVisual(hue = 208f, saturation = 0.85f, bloomRadius = 0.98f, edgeWidth = 0.06f, breathCycles = 2f, shimmer = 0f)
    val Evolve = GlowVisual(hue = 50f, saturation = 0.95f, bloomRadius = 1.0f, edgeWidth = 0.065f, breathCycles = 2.5f, shimmer = 0.6f)

    fun forManual(manual: Boolean): GlowVisual = if (manual) Manual else Passive
}
