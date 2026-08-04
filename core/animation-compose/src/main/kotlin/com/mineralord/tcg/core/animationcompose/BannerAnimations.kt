package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.AnimationDefinition
import com.mineralord.tcg.core.animation.AnimationId
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.BannerVisual
import kotlin.time.Duration.Companion.milliseconds

/**
 * Recetas de los RÓTULOS de anuncio (familia "Banner"): un único [AnimationStep.Banner] cuya
 * identidad (dirección, tinte, texto) son DATOS. Cubre dos requests con el MISMO ejecutor/renderer:
 *
 * - [AnimationRequest.AttackStarted] → rótulo de ataque, bidireccional:
 *     · entrante (rival→mí): barre desde la derecha, tinte cálido/rojo — "ATAQUE ENTRANTE".
 *     · saliente (mí→rival): barre desde la izquierda, tinte frío/cian — "TU ATAQUE".
 * - [AnimationRequest.AbilityActivated] → rótulo de habilidad:
 *     · manual: dorado, "HABILIDAD".
 *     · pasiva/disparada: carmesí, "HABILIDAD PASIVA".
 *
 * Referencias AAA: Marvel Snap (reveal con placa + barrido de luz), Legends of Runeterra (kicker +
 * título), Hearthstone. Vive en el núcleo compartido: juego y Studio reproducen el MISMO rótulo.
 */
val BannerAnimations = AnimationDefinitionContributor { builder ->
    builder.register(AnimationRequest.AttackStarted::class) { request ->
        val visual = if (request.incoming) {
            BannerVisual(accentHue = 6f, secondaryHue = 32f, fromRight = true, emphasis = 0.8f, sheen = 0.7f)
        } else {
            BannerVisual(accentHue = 194f, secondaryHue = 165f, fromRight = false, emphasis = 0.6f, sheen = 0.6f)
        }
        val kicker = if (request.incoming) "Ataque entrante" else "Tu ataque"
        AnimationDefinition(
            id = AnimationId("banner:attack:${if (request.incoming) "in" else "out"}:${request.attackerId}"),
            name = "Banner:Attack:${if (request.incoming) "Incoming" else "Outgoing"}",
            steps = listOf(
                AnimationStep.Banner(kicker = kicker, title = request.attackName, duration = 1500.milliseconds, visual = visual),
            ),
        )
    }

    builder.register(AnimationRequest.AbilityActivated::class) { request ->
        val visual = if (request.manual) {
            BannerVisual(accentHue = 45f, secondaryHue = 52f, fromRight = false, emphasis = 0.7f, sheen = 0.85f)
        } else {
            BannerVisual(accentHue = 350f, secondaryHue = 20f, fromRight = false, emphasis = 0.45f, sheen = 0.5f)
        }
        val kicker = if (request.manual) "Habilidad" else "Habilidad pasiva"
        AnimationDefinition(
            id = AnimationId("banner:ability:${if (request.manual) "manual" else "passive"}:${request.sourcePokemonId}"),
            name = "Banner:Ability:${if (request.manual) "Manual" else "Passive"}",
            steps = listOf(
                // Texto principal = NOMBRE DEL POKÉMON (no el de la habilidad).
                AnimationStep.Banner(kicker = kicker, title = request.pokemonName, duration = 1500.milliseconds, visual = visual),
            ),
        )
    }
}
