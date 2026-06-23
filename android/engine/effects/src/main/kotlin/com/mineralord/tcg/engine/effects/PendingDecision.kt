package com.mineralord.tcg.engine.effects

import com.mineralord.tcg.engine.model.CardFilter
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Zone

/**
 * Decisión que el motor NO puede resolver solo: requiere que el jugador (o la
 * IA) elija. El intérprete pausa el efecto, emite una [PendingDecision], y al
 * recibir la respuesta continúa. Así se garantiza que **toda** resolución es
 * explícita y automatizable — nunca queda una acción "a mano" sin definir.
 *
 * `candidates` ya viene filtrada/calculada por el intérprete, de modo que la
 * IA solo tiene que escoger entre opciones legales.
 */
sealed interface PendingDecision {
    val side: Side
    val prompt: LocalizedText

    /** Elegir N Pokémon objetivo entre [candidates]. */
    data class ChooseTargets(
        override val side: Side,
        override val prompt: LocalizedText,
        val candidates: List<CardId>,
        val count: Int,
    ) : PendingDecision

    /** Buscar en una zona cartas que cumplan [filter] y llevarlas a [destination]. */
    data class SearchCards(
        override val side: Side,
        override val prompt: LocalizedText,
        val from: Zone,
        val filter: CardFilter,
        val destination: Zone,
        val count: Int,
        val candidates: List<CardId>,
    ) : PendingDecision

    /** Mover N energías de un Pokémon a otro. */
    data class MoveEnergy(
        override val side: Side,
        override val prompt: LocalizedText,
        val fromCandidates: List<CardId>,
        val toCandidates: List<CardId>,
        val count: Int,
    ) : PendingDecision
}
