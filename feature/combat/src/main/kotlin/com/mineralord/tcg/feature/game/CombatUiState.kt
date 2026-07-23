package com.mineralord.tcg.feature.game

import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PokemonCard

/**
 * Estado observable del combate para la pantalla (view-state canónico).
 *
 * Vive en `feature:combat` porque es el LENGUAJE de la pantalla de combate compartida: tanto el
 * juego (`GameViewModel`/`OnlineGameController`) como el Studio (`SandboxController`) lo producen y
 * `CombatScreen` lo consume. Se extrajo de `GameViewModel` al unificar la pantalla; su forma no cambia.
 */
data class GameUiState(
    val loading: Boolean = true,
    val state: GameState? = null,
    /** No null durante el lanzamiento de moneda inicial (antes de la preparación). */
    val coinFlip: CoinFlipUiState? = null,
    /** No null durante el reparto animado de la mano inicial (tras la moneda). */
    val dealing: DealUiState? = null,
    /** No null durante la preparación interactiva (elegir Activo/Banca). */
    val setup: SetupUiState? = null,
    /** true durante el revelado inicial: el rival voltea sus Pokémon y se reparten
     *  los premios, antes de ceder el control del primer turno. */
    val revealing: Boolean = false,
    val log: List<String> = emptyList(),
    /** Mensaje transitorio (acción ilegal, etc.); se limpia en la próxima acción. */
    val message: String? = null,
    /** true mientras la IA está jugando su turno (bloquea la entrada). */
    val aiThinking: Boolean = false,
)

/** Fase del lanzamiento de moneda inicial (réplica de TCG Live). CHOOSE_ORDER =
 *  el jugador ganó el volado y elige quién empieza (regla oficial: el ganador decide). */
enum class CoinPhase { CHOOSING, SPINNING, RESULT, CHOOSE_ORDER }

/**
 * Estado del reparto animado de la mano inicial: las 7 cartas que salen del mazo
 * hacia la mano, en orden. La UI las anima (mazo→abanico) y avisa al terminar.
 */
data class DealUiState(val hand: List<Card>)

/** Estado del volado de moneda: elección del jugador, resultado y quién empieza. */
data class CoinFlipUiState(
    val phase: CoinPhase,
    /** Elección del jugador: true = cara, false = cruz. */
    val call: Boolean? = null,
    /** Resultado del volado: true = cara, false = cruz. */
    val result: Boolean? = null,
    val playerWon: Boolean? = null,
    val message: String? = null,
)

/**
 * Estado de la preparación interactiva: la mano repartida del jugador, sus Básicos elegibles y las
 * elecciones actuales de Activo y Banca. Mientras exista, la pantalla muestra el overlay de setup.
 */
data class SetupUiState(
    val hand: List<Card>,
    val basics: List<PokemonCard>,
    val activeId: CardId? = null,
    val benchIds: List<CardId> = emptyList(),
) {
    /** ¿Se puede confirmar? Basta con haber elegido Activo. */
    val canConfirm: Boolean get() = activeId != null
}
