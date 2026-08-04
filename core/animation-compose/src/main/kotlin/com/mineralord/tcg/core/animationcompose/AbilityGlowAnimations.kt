package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.AnimationDefinition
import com.mineralord.tcg.core.animation.AnimationId
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.GlowVisual
import kotlin.time.Duration.Companion.milliseconds

/**
 * Recetas del AURA de habilidad (familia "Glow"): un único [AnimationStep.AbilityGlow] sobre la
 * ranura del Pokémon (banca o activo). El modo llega en [AnimationRequest.AbilityGlowRequested.manual]
 * y sólo cambia los DATOS del [GlowVisual] — un único ejecutor/renderer sirve ambos:
 *
 * - PASIVA (false): rojo, respiración lenta y estable, sin shimmer (estado ambiental).
 * - MANUAL (true): dorado, más brillante, con shimmer que recorre el borde (call-to-action,
 *   como el glow de carta jugable de Hearthstone).
 *
 * Vive en el núcleo compartido: juego y Studio reproducen la MISMA aura.
 */
val AbilityGlowAnimations = AnimationDefinitionContributor { builder ->
    builder.register(AnimationRequest.AbilityGlowRequested::class) { request ->
        val visual = if (request.manual) {
            GlowVisual(hue = 45f, saturation = 0.9f, bloomRadius = 1.05f, edgeWidth = 0.07f, breathCycles = 3f, shimmer = 0.9f)
        } else {
            GlowVisual(hue = 4f, saturation = 0.9f, bloomRadius = 0.95f, edgeWidth = 0.06f, breathCycles = 2f, shimmer = 0f)
        }
        AnimationDefinition(
            id = AnimationId("glow:${if (request.manual) "manual" else "passive"}:${request.sourcePokemonId}"),
            name = "AbilityGlow:${if (request.manual) "Manual" else "Passive"}",
            steps = listOf(
                AnimationStep.AbilityGlow(slotId = request.slotId, duration = 2600.milliseconds, visual = visual),
            ),
        )
    }
}
