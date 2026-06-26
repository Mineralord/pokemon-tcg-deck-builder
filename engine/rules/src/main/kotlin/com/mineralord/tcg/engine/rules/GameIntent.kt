package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.CardId

/**
 * Acciones que un jugador (humano o IA) puede solicitar al motor durante su
 * turno. El motor las valida y, si son legales, produce un nuevo estado +
 * eventos. Una acción ilegal se rechaza vía [EngineResult] sin mutar nada.
 *
 * Mientras haya una decisión pendiente ([GameState.interaction] != null), el
 * motor solo acepta [ResolveDecision].
 */
sealed interface GameIntent {

    /** Pone un Pokémon Básico de la mano en la Banca. */
    data class PlayBasicToBench(val card: CardId) : GameIntent

    /** Evoluciona un Pokémon en juego con una carta de evolución de la mano. */
    data class Evolve(val evolution: CardId, val onto: CardId) : GameIntent

    /** Une una carta de energía de la mano a un Pokémon en juego. */
    data class AttachEnergy(val energy: CardId, val to: CardId) : GameIntent

    /** Retira el Activo a la Banca pagando el coste de retirada. */
    data class Retreat(val benchTarget: CardId) : GameIntent

    /** El Activo ataca; en el TCG, atacar termina el turno. */
    data class Attack(val attackName: String) : GameIntent

    /**
     * Juega un Entrenador de la mano (Apoyo o Objeto). Resuelve su efecto
     * autorado; si éste exige una elección, deja una decisión pendiente.
     */
    data class PlayTrainer(val card: CardId) : GameIntent

    /** Activa la habilidad [abilityName] de un Pokémon [pokemon] en juego. */
    data class UseAbility(val pokemon: CardId, val abilityName: String) : GameIntent

    /** Resuelve la decisión pendiente eligiendo las cartas [chosen]. */
    data class ResolveDecision(val chosen: List<CardId>) : GameIntent

    /** Termina el turno voluntariamente. */
    data object EndTurn : GameIntent
}
