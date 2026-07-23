package com.mineralord.tcg.studio

import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.Phase
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.feature.game.GameUiState
import com.mineralord.tcg.feature.game.anim.FxCue
import com.mineralord.tcg.feature.game.combat.CombatSceneController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * **Sandbox Controller: conduce MANUALMENTE la pantalla de combate REAL en el Studio.**
 *
 * Implementa el contrato neutral [CombatSceneController] —el mismo del que depende `CombatScreen`—
 * para que el Studio renderice EXACTAMENTE la misma pantalla que el juego. La única diferencia con el
 * juego es quién gobierna la UI: aquí un humano (sandbox), no las reglas del motor.
 *
 * En esta v1 siembra un tapete real (Bulbasaur Activo; Ivysaur y Venusaur en mano) construyendo un
 * [GameState] canónico con datos reales de [CardRepository]; no ejecuta reglas. Las intenciones que
 * emite la pantalla se aceptan sin validar (sandbox); su manipulación libre completa se ampliará en
 * Sprints siguientes. No dibuja nada: todo el render es de la pantalla compartida.
 */
class SandboxController : CombatSceneController {

    private val _ui = MutableStateFlow(GameUiState(loading = true))
    override val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _fx = MutableSharedFlow<FxCue>(extraBufferCapacity = 16)
    override val fx: SharedFlow<FxCue> = _fx.asSharedFlow()

    private var byId: Map<CardId, Card> = emptyMap()

    /** Construye el tapete inicial con cartas reales. Llamar fuera del hilo principal (carga JSON). */
    fun seed(repo: CardRepository) {
        fun pokemon(number: Int): PokemonCard? = repo[CardId("sv3pt5-$number")] as? PokemonCard
        val bulbasaur = pokemon(1) ?: return   // datos no disponibles: se mantiene "cargando"
        val ivysaur = pokemon(2)
        val venusaur = pokemon(3)
        val charmander = pokemon(4)

        val me = PlayerState(
            side = Side.PLAYER,
            active = PokemonInPlay(card = bulbasaur),
            hand = listOfNotNull(ivysaur, venusaur),
            deck = List(40) { bulbasaur as Card },
            prizes = List(6) { bulbasaur as Card },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT,
            active = charmander?.let { PokemonInPlay(card = it) },
            deck = List(40) { bulbasaur as Card },
            prizes = List(6) { bulbasaur as Card },
        )
        val state = GameState(
            player = me,
            opponent = opponent,
            turn = 1,
            activeSide = Side.PLAYER,
            phase = Phase.MAIN,
        )
        byId = allCardsById(state)
        _ui.value = GameUiState(loading = false, state = state)
    }

    private fun allCardsById(s: GameState): Map<CardId, Card> {
        val m = HashMap<CardId, Card>()
        listOf(s.player, s.opponent).forEach { ps ->
            (ps.deck + ps.hand + ps.discard + ps.prizes).forEach { m[it.id] = it }
            ps.bench.forEach { m[it.card.id] = it.card }
            ps.active?.let { m[it.card.id] = it.card }
        }
        return m
    }

    // ---- Consultas de UI ----
    override fun card(id: CardId): Card? = byId[id]
    override fun cardName(id: CardId): String = byId[id]?.name?.es ?: id.raw

    // ---- Sandbox: se acepta sin reglas (v1: sin cambios de estado). ----
    override fun onIntent(intent: GameIntent) { /* sandbox: manipulación libre en Sprints futuros */ }
    override fun onResolve(chosen: List<CardId>) { /* no hay decisiones del motor en el sandbox */ }
    override fun playItemOn(cardId: CardId, targetId: CardId) { /* no-op en v1 */ }

    // ---- Ceremonia inicial: el sandbox arranca ya en juego (sin moneda/reparto/preparación). ----
    override fun chooseCoin(heads: Boolean) {}
    override fun chooseFirst(playerFirst: Boolean) {}
    override fun onDealComplete() {}
    override fun chooseActive(id: CardId) {}
    override fun clearActive() {}
    override fun toggleBench(id: CardId) {}
    override fun confirmSetup() {}
}
