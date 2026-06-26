package com.mineralord.tcg.engine.model

/**
 * Estado de juego — todo inmutable. El motor (`:engine:rules`) toma un
 * [GameState] y un intent, y devuelve un nuevo [GameState] + lista de eventos.
 * Nada aquí muta in-place: cada transición produce una copia (`copy`), lo que
 * hace el motor determinista, depurable y trivialmente serializable para
 * guardar/reproducir partidas.
 */

/** Identifica a uno de los dos lados de la mesa. */
enum class Side { PLAYER, OPPONENT }

/** Una carta de Pokémon puesta en juego, con su estado dinámico. */
data class PokemonInPlay(
    val card: PokemonCard,
    val damage: Int = 0,                      // daño acumulado (no HP restante)
    val attachedEnergy: List<EnergyCard> = emptyList(),
    val attachedTools: List<TrainerCard> = emptyList(),
    val statuses: Set<Status> = emptySet(),
    /** Pila de evolución debajo de esta carta (de más antigua a más reciente). */
    val evolutionStack: List<PokemonCard> = emptyList(),
    val turnsInPlay: Int = 0,
) {
    val remainingHp: Int get() = (card.hp - damage).coerceAtLeast(0)
    val isKnockedOut: Boolean get() = damage >= card.hp
    val attachedEnergyCount: Int get() = attachedEnergy.size
}

/** Estado completo de un jugador. */
data class PlayerState(
    val side: Side,
    val active: PokemonInPlay?,
    val bench: List<PokemonInPlay> = emptyList(),
    val hand: List<Card> = emptyList(),
    val deck: List<Card> = emptyList(),
    val discard: List<Card> = emptyList(),
    val lostZone: List<Card> = emptyList(),
    val prizes: List<Card> = emptyList(),
    val prizesRemaining: Int = prizes.size,
) {
    /** Todos los Pokémon en juego (activo + banca), sin nulls. */
    val allInPlay: List<PokemonInPlay>
        get() = listOfNotNull(active) + bench
}

/** Fases del turno (modeladas como tipo para una máquina de estados explícita). */
enum class Phase { SETUP, DRAW, MAIN, ATTACK, BETWEEN_TURNS, GAME_OVER }

/** Estado raíz de una partida. */
data class GameState(
    val player: PlayerState,
    val opponent: PlayerState,
    val turn: Int,
    val activeSide: Side,
    val phase: Phase,
    val stadium: TrainerCard? = null,         // estadio en campo (único, compartido)
    val winner: Side? = null,
    /**
     * Efecto en pausa esperando una elección (buscar/objetivo/mover energía).
     * Mientras no sea null, el motor solo acepta resolver la decisión.
     */
    val interaction: PendingInteraction? = null,
    /** Un Apoyo por turno: se pone a true al jugar uno; se resetea en fin de turno. */
    val supporterPlayedThisTurn: Boolean = false,
    /** Una energía por turno: se pone a true al unir una; se resetea en fin de turno. */
    val energyAttachedThisTurn: Boolean = false,
    /** Habilidades 1/turno ya usadas este turno (por id de Pokémon). */
    val abilitiesUsedThisTurn: Set<CardId> = emptySet(),
) {
    fun sideState(side: Side): PlayerState = if (side == Side.PLAYER) player else opponent
    val activePlayer: PlayerState get() = sideState(activeSide)
    val isOver: Boolean get() = phase == Phase.GAME_OVER || winner != null

    /** Hay una decisión pendiente que el jugador/IA debe resolver antes de seguir. */
    val awaitingDecision: Boolean get() = interaction != null
}
