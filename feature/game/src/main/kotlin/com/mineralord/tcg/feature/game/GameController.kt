package com.mineralord.tcg.feature.game

import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.feature.game.anim.FxCue
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Superficie que consume [GameScreen], independiente de si la partida es LOCAL
 * (vs IA → [GameViewModel]) u ONLINE (host-autoritativo → OnlineGameViewModel).
 * Así la misma pantalla de combate sirve para ambos modos sin duplicar UI.
 */
interface GameController {
    /** Estado observable del combate (tablero, fases de ceremonia, log…). */
    val ui: StateFlow<GameUiState>

    /** Señales de animación (daño, embate, KO, premios, moneda). */
    val fx: SharedFlow<FxCue>

    // ---- Ceremonia inicial ----
    fun chooseCoin(heads: Boolean)
    fun chooseFirst(playerFirst: Boolean)
    fun onDealComplete()
    fun chooseActive(id: CardId)
    fun clearActive()
    fun toggleBench(id: CardId)
    fun confirmSetup()

    // ---- Juego ----
    fun onIntent(intent: GameIntent)
    fun onResolve(chosen: List<CardId>)

    /**
     * Juega un OBJETO que apunta a un Pokémon (p. ej. Poción) directamente sobre
     * [targetId], resolviendo en un solo gesto la decisión de objetivo que abriría.
     * Equivale a `PlayTrainer(cardId)` + `ResolveDecision([targetId])`, atómico y
     * válido en ambos modos (PvE y PvP).
     */
    fun playItemOn(cardId: CardId, targetId: CardId)

    // ---- Consultas de UI ----
    fun cardName(id: CardId): String
    fun card(id: CardId): Card?
}
