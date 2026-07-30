package com.mineralord.tcg.feature.game

import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.engine.events.CombatLog
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.rules.EngineResult
import com.mineralord.tcg.engine.rules.GameEngine
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.feature.game.anim.FxCue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * NÚCLEO COMPARTIDO de una partida (Fase 2 de la unificación). Es Kotlin puro (sin
 * Android) y es la ÚNICA fuente de:
 *  - el estado observable de UI ([ui]) y las señales de animación ([fx]),
 *  - las mutaciones de PREPARACIÓN (Activo/Banca), idénticas en PvE y PvP,
 *  - el PIPELINE de intents autoritativo ([applyLocal]/[commit]): aplica el motor,
 *    registra los eventos con [combatLog], reproduce FX y dispara [onGameFinished].
 *
 * Tanto [GameViewModel] (PvE) como [OnlineGameController] (PvP) lo poseen por
 * COMPOSICIÓN (no herencia: `GameViewModel` ya extiende `AndroidViewModel`) y delegan
 * en él. Cada modo solo conserva su diferencia real: PvE avanza la IA; el HOST de PvP
 * difunde el snapshot; el GUEST reenvía el intent. Así, un cambio en la lógica de
 * juego se hace UNA vez y aplica a ambos modos.
 *
 * Las dependencias que llegan tras la carga async (repo, combatLog, engine) y los
 * enganches específicos de modo ([buildProvisional], [onGameFinished]) se inyectan
 * como `var`/`lateinit` para poder construir el core en el field-initializer del
 * controlador (y así exponer [ui]/[fx] de inmediato).
 */
class GameCore(private val scope: CoroutineScope) {

    lateinit var repo: CardRepository
    lateinit var combatLog: CombatLog

    /** Motor autoritativo. `null` en el GUEST de PvP (no ejecuta motor: pinta snapshots). */
    var engine: GameEngine? = null

    /**
     * Construye el tablero PROVISIONAL durante la preparación desde las elecciones
     * actuales. Difiere por modo (PvE usa ambos repartos; el host de PvP un stand-in;
     * el guest su propia mano), por eso se inyecta. Devuelve null si aún no hay datos.
     */
    var buildProvisional: (activeId: CardId?, benchIds: List<CardId>) -> GameState? = { _, _ -> null }

    /** Se dispara UNA sola vez al terminar la partida. Punto de extensión para las
     *  recompensas (distintas por modo); no altera la lógica de juego compartida. */
    var onGameFinished: (winner: Side) -> Unit = {}

    // ---------------------------------------------------------------- estado de UI
    var state: GameState? = null
    var coinFlip: CoinFlipUiState? = null
    var dealing: DealUiState? = null
    var setup: SetupUiState? = null
    var revealing: Boolean = false
    var aiThinking: Boolean = false
    /** Mensaje transitorio (jugada ilegal, etc.): se publica una vez y se limpia. */
    var message: String? = null
    val log = mutableListOf<String>()

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _fx = MutableSharedFlow<FxCue>(extraBufferCapacity = 64)
    val fx: SharedFlow<FxCue> = _fx.asSharedFlow()

    /** Publica el estado actual a la UI y consume el [message] transitorio. */
    fun emit() {
        _ui.value = GameUiState(
            loading = false,
            state = state,
            coinFlip = coinFlip,
            dealing = dealing,
            setup = setup,
            revealing = revealing,
            log = log.toList(),
            message = message,
            aiThinking = aiThinking,
        )
        message = null
    }

    suspend fun emitFx(cue: FxCue) = _fx.emit(cue)
    fun tryEmitFx(cue: FxCue) { _fx.tryEmit(cue) }

    /** Reproduce las animaciones de una lista de eventos (mapeo/espaciado compartidos). */
    suspend fun playEventFx(events: List<com.mineralord.tcg.engine.events.GameEvent>) =
        playFx(events) { _fx.emit(it) }

    // ----------------------------------------------------------------- preparación
    // Mutaciones IDÉNTICAS en ambos modos (elegir Activo, alternar Banca).

    fun chooseActive(id: CardId) {
        val s = setup ?: return
        setup = s.copy(activeId = id, benchIds = s.benchIds - id)
        rebuildProvisional(); emit()
    }

    fun clearActive() {
        val s = setup ?: return
        setup = s.copy(activeId = null)
        rebuildProvisional(); emit()
    }

    fun toggleBench(id: CardId) {
        val s = setup ?: return
        if (id == s.activeId) return
        val next = when {
            id in s.benchIds -> s.benchIds - id
            s.benchIds.size >= GameEngine.BENCH_LIMIT -> s.benchIds
            else -> s.benchIds + id
        }
        setup = s.copy(benchIds = next)
        rebuildProvisional(); emit()
    }

    fun rebuildProvisional() {
        val s = setup ?: return
        buildProvisional(s.activeId, s.benchIds)?.let { state = it }
    }

    // ------------------------------------------------------------ pipeline de intents

    /**
     * Aplica [intent] con el motor local (autoritativo). Devuelve el [EngineResult] si
     * fue ACEPTADO —para que el modo haga su post-proceso (IA / broadcast)— o `null` si
     * el motor lo rechazó. Con [surfaceRejection] publica el motivo del rechazo (el host
     * NO lo hace para jugadas ilegales del guest). En caso aceptado registra eventos,
     * emite y evalúa el fin de partida vía [commit].
     */
    fun applyLocal(intent: GameIntent, surfaceRejection: Boolean = true): EngineResult? {
        val eng = engine ?: return null
        val cur = state ?: return null
        if (cur.isOver) return null
        val res = eng.apply(cur, intent)
        if (!res.accepted) {
            if (surfaceRejection) { message = res.rejection; emit() }
            return null
        }
        commit(res)
        return res
    }

    /**
     * Juega un OBJETO dirigido (Poción…) sobre [targetId] de forma ATÓMICA en el lado
     * autoritativo: aplica `PlayTrainer` y, si eso abrió una decisión de elegir objetivo
     * del jugador, la resuelve con [targetId]. Devuelve los [EngineResult] aplicados (para
     * que el modo haga su post-proceso: IA en PvE, broadcast en el host de PvP). Vacío si
     * el motor rechazó jugar la carta (o no hay motor: el GUEST no usa esta ruta).
     */
    fun applyItemOnTarget(cardId: CardId, targetId: CardId): List<EngineResult> {
        val played = applyLocal(GameIntent.PlayTrainer(cardId)) ?: return emptyList()
        val results = mutableListOf(played)
        val inter = state?.interaction
        if (inter?.decision is PendingDecision.ChooseTargets && inter.side == state?.activeSide) {
            applyLocal(GameIntent.ResolveDecision(listOf(targetId)))?.let { results += it }
        }
        return results
    }

    /**
     * Consolida un [EngineResult] ya calculado: actualiza el estado, registra sus
     * eventos con el [combatLog] compartido, publica a la UI y evalúa el fin de partida.
     * NO reproduce FX (el modo decide si esperarlos en secuencia —PvE— o lanzarlos en
     * paralelo —host de PvP—) para no cambiar el "feel" de cada uno.
     */
    fun commit(res: EngineResult) {
        state = res.state
        res.events.forEach { log += combatLog.render(it) }
        emit()
        checkGameOver()
    }

    private var finished = false

    /**
     * Dispara [onGameFinished] UNA sola vez cuando el estado actual está terminado. Lo
     * llama [commit] (ruta autoritativa PvE/host) y también el GUEST de PvP al recibir
     * un snapshot terminado, para que las recompensas NO dependan del modo.
     */
    fun checkGameOver() {
        val s = state ?: return
        if (s.isOver && !finished) {
            finished = true
            s.winner?.let { w -> scope.launch { onGameFinished(w) } }
        }
    }
}
