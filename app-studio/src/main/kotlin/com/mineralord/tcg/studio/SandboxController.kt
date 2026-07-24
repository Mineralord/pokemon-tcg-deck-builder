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
 * Siembra un tapete real (Bulbasaur Activo; Ivysaur y Venusaur en mano) construyendo un [GameState]
 * canónico con datos reales de [CardRepository]; no ejecuta reglas. Traduce las intenciones que emite
 * la pantalla (arrastres, etc.) a [BoardInteraction] NEUTRALES y las delega en [onInteraction] (el
 * sistema de herramientas Active Tool decide qué hacer). No conoce Evolution ni ninguna categoría; no
 * dibuja nada.
 *
 * @param onInteraction recibe cada interacción del tablero ya normalizada (la consume el ToolController).
 */
class SandboxController(
    private val onInteraction: (BoardInteraction) -> Unit,
) : CombatSceneController {

    private var repo: CardRepository? = null

    private val _ui = MutableStateFlow(GameUiState(loading = true))
    override val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _fx = MutableSharedFlow<FxCue>(extraBufferCapacity = 16)
    override val fx: SharedFlow<FxCue> = _fx.asSharedFlow()

    private var byId: Map<CardId, Card> = emptyMap()

    /** Construye el tapete inicial con cartas reales. Llamar fuera del hilo principal (carga JSON). */
    fun seed(repo: CardRepository) {
        this.repo = repo
        fun pokemon(number: Int): PokemonCard? = repo[CardId("sv3pt5-$number")] as? PokemonCard
        val bulbasaur = pokemon(1) ?: return   // datos no disponibles: se mantiene "cargando"
        val ivysaur = pokemon(2)
        val venusaur = pokemon(3)
        val charmander = pokemon(4)

        val me = PlayerState(
            side = Side.PLAYER,
            // turnsInPlay >= 1: la pantalla considera válido evolucionar sobre este Activo.
            active = PokemonInPlay(card = bulbasaur, turnsInPlay = 1),
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

    /** Id (raw) de la carta actualmente en el Activo del jugador, o null. Actor de las evoluciones. */
    fun activeCardId(): String? = _ui.value.state?.player?.active?.card?.id?.raw

    // ---- Consultas de UI ----
    override fun card(id: CardId): Card? = byId[id]
    override fun cardName(id: CardId): String = byId[id]?.name?.es ?: id.raw

    /** Restaura el tapete inicial para repetir pruebas (usando el repo ya cargado). */
    fun reset() {
        repo?.let { seed(it) }
    }

    // ---- Traduce la intención de la pantalla a una interacción NEUTRAL y la delega. ----
    override fun onIntent(intent: GameIntent) {
        val interaction = intent.toBoardInteraction()
        // 1) La carta soltada ATERRIZA y PERMANECE en su destino (deja de ser un ghost de drag y pasa
        //    a ser el actor/resultado de la secuencia). Estructurado por tipo de interacción para
        //    reutilizarse en futuras interacciones (no es lógica exclusiva de Evolution).
        applyToBoard(interaction)
        // 2) Se delega en el sistema de herramientas, que reproduce el asset activo SOBRE ese destino.
        onInteraction(interaction)
    }

    /**
     * Aplica el efecto visual de una interacción al estado del tablero (sandbox, sin reglas), para que
     * la carta arrastrada aterrice donde se soltó y la animación sea continuación del gesto. Cada
     * [InteractionKind] declara aquí su efecto; hoy sólo Evolution (aterrizaje de la evolución).
     */
    private fun applyToBoard(interaction: BoardInteraction) {
        when (interaction.kind) {
            InteractionKind.EvolveDrop -> landEvolution(interaction.sourceCardId, interaction.targetCardId)
            else -> Unit // otras interacciones aún no modifican el tablero del sandbox
        }
    }

    /** La evolución soltada sustituye al Pokémon Activo objetivo y queda en su lugar (apila la base). */
    private fun landEvolution(evolutionId: String?, ontoId: String?) {
        val state = _ui.value.state ?: return
        val me = state.player
        val active = me.active ?: return
        if (ontoId == null || active.card.id.raw != ontoId) return
        val evoCard = me.hand.firstOrNull { it.id.raw == evolutionId } as? PokemonCard ?: return

        val evolved = PokemonInPlay(
            card = evoCard,
            damage = active.damage,
            attachedEnergy = active.attachedEnergy,
            attachedTools = active.attachedTools,
            statuses = active.statuses,
            evolutionStack = active.evolutionStack + active.card,
            turnsInPlay = 1, // permite encadenar (p. ej. Venusaur sobre Ivysaur) para seguir probando.
        )
        val newState = state.copy(
            player = me.copy(active = evolved, hand = me.hand.filterNot { it.id.raw == evolutionId }),
        )
        byId = allCardsById(newState)
        _ui.value = _ui.value.copy(state = newState)
    }

    /** Mapeo genérico intención→interacción. Sin lógica de categoría: cada tool decide si la acepta. */
    private fun GameIntent.toBoardInteraction(): BoardInteraction = when (this) {
        is GameIntent.Evolve -> BoardInteraction(InteractionKind.EvolveDrop, evolution.raw, onto.raw)
        is GameIntent.PlayBasicToBench -> BoardInteraction(InteractionKind.PlayBasic, card.raw)
        is GameIntent.AttachEnergy -> BoardInteraction(InteractionKind.AttachEnergy, energy.raw, to.raw)
        is GameIntent.Retreat -> BoardInteraction(InteractionKind.Retreat, benchTarget.raw)
        is GameIntent.Attack -> BoardInteraction(InteractionKind.Attack)
        GameIntent.EndTurn -> BoardInteraction(InteractionKind.EndTurn)
        else -> BoardInteraction(InteractionKind.Other)
    }
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
