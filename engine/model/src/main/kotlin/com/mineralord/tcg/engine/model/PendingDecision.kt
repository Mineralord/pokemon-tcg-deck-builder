package com.mineralord.tcg.engine.model

/**
 * Decisión que el motor NO puede resolver solo: requiere que el jugador (o la
 * IA) elija. El intérprete pausa el efecto, emite una [PendingDecision] (dentro
 * de [PendingInteraction]) y, al recibir la respuesta, continúa. Así se garantiza
 * que **toda** resolución es explícita y automatizable — nunca queda una acción
 * "a mano" sin definir.
 *
 * `candidates` ya viene filtrada/calculada por el intérprete, de modo que la IA
 * solo tiene que escoger entre opciones legales.
 *
 * Vive en `:engine:model` (y no en `:engine:effects`) para que [GameState] pueda
 * contener la interacción en curso sin invertir la dirección de dependencias.
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

/**
 * Efecto en pausa esperando una elección. Guarda la [decision] visible para
 * UI/IA, las [remainingOps] que faltan por ejecutar (la "continuación") y de
 * quién es el efecto ([side]/[sourceId], para resolver SELF/recoil y bindings).
 *
 * [endsTurnOnResolve] = true cuando la interacción proviene de un ataque: el
 * turno se cerrará automáticamente al vaciar la cadena de decisiones.
 */
data class PendingInteraction(
    val decision: PendingDecision,
    val remainingOps: List<EffectOp>,
    val side: Side,
    val sourceId: CardId?,
    val endsTurnOnResolve: Boolean = false,
)
