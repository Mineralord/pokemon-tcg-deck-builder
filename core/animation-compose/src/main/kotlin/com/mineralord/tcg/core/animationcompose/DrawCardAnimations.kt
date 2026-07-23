package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.AnimationDefinition
import com.mineralord.tcg.core.animation.AnimationId
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animation.AnimationStep
import kotlin.time.Duration.Companion.milliseconds

/**
 * Convención de ids de ranura del robo. El `AnimationStep` puro referencia ranuras por
 * `String`; aquí se construyen a partir de la request de dominio. Cuando exista el
 * sistema de mano, el destino se resolverá al hueco concreto de la carta; por ahora la
 * carta aterriza en la ranura lógica de la mano del jugador (una sola carta, animación
 * #001). Los call-sites de la UI deben publicar estas ranuras con `trackBounds`.
 */
fun deckSlotId(playerId: String): SlotId = SlotId("deck:$playerId")
fun handSlotId(playerId: String): SlotId = SlotId("hand:$playerId")

/** Duración del robo: dentro de la ventana 280–360 ms del estándar "Snap sobrio". */
private val DRAW_CARD_DURATION = 320.milliseconds

/**
 * Contribuidor de la receta del robo de carta. Mapea la request de dominio
 * [AnimationRequest.CardDrawn] a una [AnimationDefinition] cuyo único paso es un
 * [AnimationStep.DrawCard] del mazo a la mano del jugador.
 *
 * Registrar este contribuidor en el composition root (junto a los demás) hace que
 * `submit(CardDrawn(...))` reproduzca el robo de punta a punta. No conoce Compose ni el
 * ejecutor: sólo describe la receta como datos.
 */
val DrawCardAnimations = com.mineralord.tcg.core.animationcompose.AnimationDefinitionContributor { builder ->
    builder.register(AnimationRequest.CardDrawn::class) { request ->
        AnimationDefinition(
            id = AnimationId("card-drawn:${request.playerId}:${request.cardId}"),
            name = "CardDrawn",
            steps = listOf(
                AnimationStep.DrawCard(
                    fromSlotId = deckSlotId(request.playerId).value,
                    toSlotId = handSlotId(request.playerId).value,
                    duration = DRAW_CARD_DURATION,
                ),
            ),
        )
    }
}
