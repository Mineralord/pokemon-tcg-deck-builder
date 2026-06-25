package com.mineralord.tcg.engine.events

import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Status

/**
 * GameEvent — el stream de eventos tipados que emite el motor de reglas.
 *
 * Hallazgo del análisis de referencias (§0 del blueprint): la pantalla
 * "REGISTRO DEL COMBATE" demuestra que la app modela CADA acción como un evento
 * discreto. Un único `List<GameEvent>` ordenado sirve a tres consumidores:
 *   (a) el log de combate, localizable (ver :feature:game),
 *   (b) el disparador de animaciones (mapea evento -> FX nativo en Compose),
 *   (c) la IA / automatización determinista.
 *
 * Por eso vive en su propio módulo Kotlin puro: sin dependencias de Android,
 * serializable, y compartido por motor, UI y audio.
 */
sealed interface GameEvent {
    /** Lado que origina el evento (cuando aplica). */
    val side: Side?

    // --- Flujo de turno ---
    data class TurnStarted(override val side: Side, val turn: Int) : GameEvent
    data class TurnEnded(override val side: Side) : GameEvent

    // --- Cartas / mano / mazo ---
    data class CardsDrawn(override val side: Side, val count: Int) : GameEvent
    data class DeckShuffled(override val side: Side) : GameEvent

    // --- Despliegue ---
    data class PokemonPlayed(override val side: Side, val card: CardId, val toBench: Boolean) : GameEvent
    data class Evolved(override val side: Side, val from: CardId, val to: CardId) : GameEvent
    data class EnergyAttached(override val side: Side, val energy: CardId, val to: CardId) : GameEvent
    data class ToolAttached(override val side: Side, val tool: CardId, val to: CardId) : GameEvent
    data class TrainerPlayed(override val side: Side, val card: CardId) : GameEvent
    data class StadiumPlayed(override val side: Side, val card: CardId) : GameEvent

    // --- Combate ---
    data class Retreated(override val side: Side, val from: CardId, val to: CardId) : GameEvent
    data class Attacked(override val side: Side, val attacker: CardId, val attackName: String) : GameEvent
    data class DamageDealt(
        override val side: Side,
        val target: CardId,
        val amount: Int,
        val weaknessApplied: Boolean = false,
        val resistanceApplied: Boolean = false,
    ) : GameEvent
    data class Healed(override val side: Side, val target: CardId, val amount: Int) : GameEvent
    data class StatusApplied(override val side: Side, val target: CardId, val status: Status) : GameEvent
    data class StatusRemoved(override val side: Side, val target: CardId, val status: Status) : GameEvent

    // --- Premios / KO / fin ---
    data class KnockedOut(override val side: Side, val pokemon: CardId) : GameEvent
    data class PrizeTaken(override val side: Side, val count: Int) : GameEvent
    data class CoinFlipped(override val side: Side?, val heads: Boolean) : GameEvent
    data class GameWon(override val side: Side) : GameEvent
}
