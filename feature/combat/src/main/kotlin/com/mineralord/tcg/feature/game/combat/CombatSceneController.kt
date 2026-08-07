package com.mineralord.tcg.feature.game.combat

import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.rules.AbilityGlow
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.feature.game.GameUiState
import com.mineralord.tcg.feature.game.anim.FxCue
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * **Contrato NEUTRAL del que depende [CombatScreen]. Separa el RENDER del CONTROL.**
 *
 * La pantalla de combate es ÚNICA para todo el proyecto y no sabe quién la gobierna: sólo lee el
 * [ui] observable y emite intenciones por sus métodos. Dos implementaciones lo cumplen:
 *  - el **juego** (`GameViewModel` PvE / `OnlineGameController` PvP, vía `GameController`), dirigido
 *    por las reglas del motor;
 *  - el **Studio** (`SandboxController`), dirigido manualmente por el usuario.
 *
 * Es deliberadamente NEUTRAL (no se llama `GameController`) para que la pantalla no dependa de nada
 * específico del juego. Cualquier mejora visual en `CombatScreen` beneficia automáticamente a ambos.
 */
interface CombatSceneController {
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
     * Juega un OBJETO que apunta a un Pokémon (p. ej. Poción) directamente sobre [targetId],
     * resolviendo en un solo gesto la decisión de objetivo que abriría.
     */
    fun playItemOn(cardId: CardId, targetId: CardId)

    // ---- Consultas de UI ----
    fun cardName(id: CardId): String
    fun card(id: CardId): Card?

    /**
     * Intents legales AHORA para el jugador en turno. La UI lo usa para saber qué habilidades
     * manuales están disponibles. En controladores sin motor local devuelve lista vacía.
     */
    fun legalIntents(): List<GameIntent> = emptyList()

    /**
     * Aura de Habilidad del Pokémon [id] para el tablero (como TCG Live): DORADA si tiene una Habilidad
     * MANUAL disponible para activar ahora, ROJA si tiene una Habilidad PASIVA activa (siempre), o null.
     * Derivada del ESTADO; los controladores sin motor local devuelven null (sin aura).
     */
    fun abilityGlow(id: CardId): AbilityGlow? = null
}
