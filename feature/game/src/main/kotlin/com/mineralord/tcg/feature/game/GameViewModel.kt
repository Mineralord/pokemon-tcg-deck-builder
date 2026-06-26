package com.mineralord.tcg.feature.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.cards.StarterDecks
import com.mineralord.tcg.data.cards.toDeck
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.events.CombatLog
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.rules.GameEngine
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.engine.rules.GameSetup
import com.mineralord.tcg.engine.rules.SeededRng
import com.mineralord.tcg.engine.rules.SmartAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estado observable del combate para la pantalla. */
data class GameUiState(
    val loading: Boolean = true,
    val state: GameState? = null,
    val log: List<String> = emptyList(),
    /** Mensaje transitorio (acción ilegal, etc.); se limpia en la próxima acción. */
    val message: String? = null,
    /** true mientras la IA está jugando su turno (bloquea la entrada). */
    val aiThinking: Boolean = false,
)

/**
 * Orquesta una partida del jugador (lado [Side.PLAYER]) contra la IA
 * ([SmartAgent], lado [Side.OPPONENT]). Construye los mazos desde el perfil
 * (baraja activa) y un mazo inicial rival, arranca con [GameSetup], aplica los
 * intents del jugador y deja que la IA juegue su turno automáticamente.
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val profileRepo = ProfileRepository(app)
    private lateinit var repo: CardRepository
    private lateinit var engine: GameEngine
    private lateinit var agent: SmartAgent
    private lateinit var combatLog: CombatLog

    private var state: GameState? = null
    private val log = mutableListOf<String>()

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repo = withContext(Dispatchers.Default) { CardRepository.load() }
            combatLog = CombatLog(
                spanish = true,
                cardName = { id -> repo[id]?.name?.es ?: id.raw },
                sideName = { s -> if (s == Side.PLAYER) "Tú" else "Rival" },
            )
            val (playerCards, oppCards) = withContext(Dispatchers.Default) { buildDecks() }
            engine = GameEngine(SeededRng(System.nanoTime()))
            agent = SmartAgent(engine)
            val initial = GameSetup.start(playerCards, oppCards, SeededRng(System.nanoTime()))
            state = initial
            log += "¡Comienza el combate! Es tu turno."
            emit()
        }
    }

    /** Baraja del jugador (activa del perfil) y un mazo inicial rival distinto. */
    private suspend fun buildDecks(): Pair<List<Card>, List<Card>> {
        val profile = profileRepo.profile.first()
        val playerDeck = profile.decks.firstOrNull { it.id == profile.activeDeckId }
            ?: profile.decks.firstOrNull()
            ?: StarterDecks.ALL.first().toDeck()
        val oppStarter = StarterDecks.ALL.firstOrNull { it.id != playerDeck.id }
            ?: StarterDecks.ALL.first()
        val playerCards = playerDeck.expandedCardIds().mapNotNull { repo[it] }
        val oppCards = oppStarter.toDeck().expandedCardIds().mapNotNull { repo[it] }
        return playerCards to oppCards
    }

    /** Nombre legible de una carta (para etiquetas de decisión, etc.). */
    fun cardName(id: CardId): String = repo[id]?.name?.es ?: id.raw

    /** Jugadas legales del lado en turno (vacío si hay decisión pendiente). */
    fun legalIntents(): List<GameIntent> =
        state?.let { engine.legalIntents(it) } ?: emptyList()

    /** Aplica un intent del jugador y, si procede, deja jugar a la IA. */
    fun onIntent(intent: GameIntent) {
        val current = state ?: return
        if (current.isOver) return
        viewModelScope.launch { applyAndAdvance(current, intent) }
    }

    /** Resuelve la decisión pendiente con las cartas elegidas. */
    fun onResolve(chosen: List<CardId>) = onIntent(GameIntent.ResolveDecision(chosen))

    private suspend fun applyAndAdvance(current: GameState, intent: GameIntent) {
        val res = engine.apply(current, intent)
        if (!res.accepted) {
            _ui.value = _ui.value.copy(message = res.rejection)
            return
        }
        state = res.state
        res.events.forEach { log += combatLog.render(it) }
        emit()

        // Turno de la IA (incluye resolver sus propias decisiones). Guarda de
        // progreso por si insistiera en algo ilegal.
        var guard = 0
        while (!(state?.isOver ?: true) && state?.activeSide == Side.OPPONENT && guard++ < 80) {
            _ui.value = _ui.value.copy(aiThinking = true)
            delay(450)
            val s = state ?: break
            val ai = agent.decide(s, Side.OPPONENT)
            val r = engine.apply(s, ai)
            if (!r.accepted) {
                val forced = engine.apply(s, GameIntent.EndTurn)
                state = forced.state
                forced.events.forEach { log += combatLog.render(it) }
                emit()
                continue
            }
            state = r.state
            r.events.forEach { log += combatLog.render(it) }
            emit()
        }
        _ui.value = _ui.value.copy(aiThinking = false)
    }

    private fun emit() {
        _ui.value = GameUiState(
            loading = false,
            state = state,
            log = log.toList(),
            message = null,
            aiThinking = _ui.value.aiThinking,
        )
    }
}
