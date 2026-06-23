package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.CardId

/**
 * Acciones que un jugador (humano o IA) puede solicitar al motor durante su
 * turno. El motor las valida y, si son legales, produce un nuevo estado +
 * eventos. Una acción ilegal se rechaza vía [EngineResult] sin mutar nada.
 *
 * Conjunto mínimo de Fase 1; se ampliará (jugar Entrenador, mover energía,
 * resolver elecciones pendientes, etc.) con el módulo `:engine:effects`.
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

    /** Termina el turno voluntariamente. */
    data object EndTurn : GameIntent
}
