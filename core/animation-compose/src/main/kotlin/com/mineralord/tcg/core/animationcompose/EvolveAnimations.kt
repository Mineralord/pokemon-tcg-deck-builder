package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.AnimationDefinition
import com.mineralord.tcg.core.animation.AnimationId
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.EvolveVisual
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Ids ESTABLES de las variantes de evolución. Fuente única: el Studio y cualquier consumidor
 * referencian estas constantes (no literales sueltos) para conmutar entre variantes.
 */
object EvolveVariants {
    const val EVO_001 = "EVO_001"
    const val EVO_002 = "EVO_002"
    const val EVO_003 = "EVO_003"
    const val EVO_004 = "EVO_004"
    const val EVO_005 = "EVO_005"

    /** Todas las variantes registradas, en orden. */
    val all: List<String> = listOf(EVO_001, EVO_002, EVO_003, EVO_004, EVO_005)
}

/**
 * Recetas de las cinco variantes de evolución (I+D de la categoría "Evolución"). Cada una es un
 * [EvolveVisual] distinto + su duración: cinco evoluciones visualmente diferentes que comparten
 * ejecutor, renderer y pipeline. Diseñadas a partir de referencias AAA:
 * - EVO_001 Crystal Bloom  — Hearthstone (cristal/escarcha) + LoR: anillo frío, bombeo suave.
 * - EVO_002 Energy Spiral  — Genshin/Honkai gacha: giro marcado + ascenso, matiz violeta.
 * - EVO_003 Radiant Ascension — LoR champion level-up: gran ascenso + destello, oro.
 * - EVO_004 DNA Morph      — evolución del anime Pokémon: volteo/morfología, verde, sin anillo.
 * - EVO_005 Celestial Burst — Marvel Snap reveal + estallido del anime: bombeo fuerte + anillo amplio.
 */
private val EVOLVE_RECIPES: Map<String, Pair<Duration, EvolveVisual>> = mapOf(
    EvolveVariants.EVO_001 to (700.milliseconds to
        EvolveVisual(rise = 0.15f, spin = 0f, flip = 0f, pulse = 0.18f, flash = 0.25f, ring = 2.4f, hue = 185f)),
    EvolveVariants.EVO_002 to (850.milliseconds to
        EvolveVisual(rise = 0.5f, spin = 1.5f, flip = 0f, pulse = 0.22f, flash = 0.3f, ring = 1.6f, hue = 290f)),
    EvolveVariants.EVO_003 to (950.milliseconds to
        EvolveVisual(rise = 0.9f, spin = 0.1f, flip = 0f, pulse = 0.12f, flash = 0.7f, ring = 1.2f, hue = 45f)),
    EvolveVariants.EVO_004 to (600.milliseconds to
        EvolveVisual(rise = 0.2f, spin = 0f, flip = 1f, pulse = 0.3f, flash = 0.2f, ring = 0f, hue = 140f)),
    EvolveVariants.EVO_005 to (800.milliseconds to
        EvolveVisual(rise = 0.35f, spin = 0.25f, flip = 0f, pulse = 0.4f, flash = 0.85f, ring = 3f, hue = 210f)),
)

/** Receta por defecto si llega un id desconocido: evolución mínima inerte (no rompe el pipeline). */
private val DEFAULT_EVOLVE = 600.milliseconds to EvolveVisual()

/**
 * Contribuidor de recetas de evolución. Mapea [AnimationRequest.Evolved] a la
 * [AnimationDefinition] de su variante (un único [AnimationStep.Evolve] sobre la ranura del
 * Activo). Vive en el núcleo compartido: el juego y el Studio reproducen la MISMA animación.
 */
val EvolveAnimations = AnimationDefinitionContributor { builder ->
    builder.register(AnimationRequest.Evolved::class) { request ->
        val (duration, visual) = EVOLVE_RECIPES[request.variantId] ?: DEFAULT_EVOLVE
        AnimationDefinition(
            id = AnimationId("evolved:${request.playerId}:${request.variantId}:${request.pokemonId}"),
            name = "Evolved:${request.variantId}",
            steps = listOf(
                AnimationStep.Evolve(
                    slotId = activeSlotId(request.playerId).value,
                    duration = duration,
                    visual = visual,
                ),
            ),
        )
    }
}
