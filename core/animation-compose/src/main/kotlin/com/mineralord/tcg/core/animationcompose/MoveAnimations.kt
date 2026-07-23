package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.AnimationDefinition
import com.mineralord.tcg.core.animation.AnimationId
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animation.AnimationStep
import kotlin.time.Duration.Companion.milliseconds

/**
 * Convención de id de la ranura del Activo. Complementa a [deckSlotId]/[handSlotId] del robo.
 * La UI real publica esta ranura con `trackBounds`.
 */
fun activeSlotId(playerId: String): SlotId = SlotId("active:$playerId")

/** Duración del movimiento carta→zona (poner un Pokémon en juego). */
private val MOVE_DURATION = 260.milliseconds

/**
 * Contribuidor de la receta de "un Pokémon entra en juego" ([AnimationRequest.PokemonPlayed]):
 * mueve la carta de la mano al Activo mediante un único [AnimationStep.Move].
 *
 * Es la versión de PRODUCCIÓN de la receta que hasta ahora sólo existía en el test de integración
 * del pipeline. Vive aquí, en el núcleo compartido, para que **el juego y el Studio reproduzcan
 * exactamente la misma animación** (un único conjunto de `AnimationDefinition`). No conoce Compose
 * ni el ejecutor: sólo describe la receta como datos.
 */
val MoveAnimations = AnimationDefinitionContributor { builder ->
    builder.register(AnimationRequest.PokemonPlayed::class) { request ->
        AnimationDefinition(
            id = AnimationId("pokemon-played:${request.playerId}:${request.pokemonId}"),
            name = "PokemonPlayed",
            steps = listOf(
                AnimationStep.Move(
                    fromSlotId = handSlotId(request.playerId).value,
                    toSlotId = activeSlotId(request.playerId).value,
                    duration = MOVE_DURATION,
                ),
            ),
        )
    }
}
