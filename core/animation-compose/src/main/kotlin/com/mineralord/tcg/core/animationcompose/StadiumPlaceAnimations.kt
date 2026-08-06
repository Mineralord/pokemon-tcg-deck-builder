package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.AnimationDefinition
import com.mineralord.tcg.core.animation.AnimationId
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animation.AnimationStep
import kotlin.time.Duration.Companion.milliseconds

/**
 * Id de la ranura del slot de Estadio (lateral, ÚNICO y compartido por ambos jugadores). El
 * call-site de la UI debe publicarla con `trackBounds(stadiumSlotId(), coords)`.
 */
fun stadiumSlotId(): SlotId = SlotId("stadium")

/** Duración de la colocación del Estadio: viaje + asentamiento con peso. */
private val STADIUM_PLACE_DURATION = 380.milliseconds

/**
 * Contribuidor de la receta de COLOCACIÓN de Estadio. Mapea [AnimationRequest.StadiumPlaced] a una
 * [AnimationDefinition] cuyo único paso es un [AnimationStep.StadiumPlace] de la mano del jugador al
 * slot de Estadio compartido. No conoce Compose ni el ejecutor: sólo describe la receta como datos.
 */
val StadiumPlaceAnimations = AnimationDefinitionContributor { builder ->
    builder.register(AnimationRequest.StadiumPlaced::class) { request ->
        AnimationDefinition(
            id = AnimationId("stadium-placed:${request.playerId}:${request.stadiumId}"),
            name = "StadiumPlaced",
            steps = listOf(
                AnimationStep.StadiumPlace(
                    fromSlotId = handSlotId(request.playerId).value,
                    toSlotId = stadiumSlotId().value,
                    duration = STADIUM_PLACE_DURATION,
                ),
            ),
        )
    }
}
