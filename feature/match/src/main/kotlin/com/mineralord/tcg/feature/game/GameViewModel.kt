package com.mineralord.tcg.feature.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.cards.AiDeckFactory
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.cards.StarterDecks
import com.mineralord.tcg.data.cards.toDeck
import com.mineralord.tcg.data.profile.CurrencyKind
import com.mineralord.tcg.data.profile.EconomyRules
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.events.CombatLog
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.withId
import com.mineralord.tcg.engine.rules.GameEngine
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.engine.rules.GameSetup
import com.mineralord.tcg.engine.rules.SeededRng
import com.mineralord.tcg.engine.rules.SmartAgent
import com.mineralord.tcg.feature.game.anim.FxCue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Orquesta una partida del jugador (lado [Side.PLAYER]) contra la IA
 * ([SmartAgent], lado [Side.OPPONENT]). La lógica de juego COMPARTIDA (estado,
 * preparación, pipeline de intents, FX, fin de partida) vive en [GameCore]; aquí
 * solo queda lo específico del modo PvE: construir los mazos, la ceremonia local
 * (moneda/reparto/revelado) y dejar jugar a la IA. El modo online reutiliza el
 * mismo [GameCore] desde [OnlineGameController], sin duplicar esa lógica.
 */
private const val REVEAL_MS = 2000L

/** Modo de VERIFICACIÓN: arranca el tablero con la Banca llena (5) en ambos lados,
 *  saltándose moneda/reparto/preparación. Solo para inspeccionar la maquetación del
 *  panal 3+2. Poner en false para el flujo normal de partida. */
private const val DEBUG_FULL_BENCH = false

class GameViewModel(app: Application) : AndroidViewModel(app), GameController {

    private val profileRepo = ProfileRepository(app)
    private lateinit var engine: GameEngine
    private lateinit var agent: SmartAgent

    /** Núcleo compartido: dueño del estado de UI, la preparación y el pipeline de intents. */
    private val core = GameCore(viewModelScope)

    override val ui: StateFlow<GameUiState> = core.ui
    override val fx: SharedFlow<FxCue> = core.fx

    // Repartos guardados hasta que el jugador confirme su preparación.
    private var playerDealt: GameSetup.DealtSide? = null
    private var opponentDealt: GameSetup.DealtSide? = null
    /** Mulligans de cada lado (para la compensación oficial de robo del rival). */
    private var playerMulligans = 0
    private var opponentMulligans = 0
    /** Quién empieza (lo decide el ganador del volado). Se aplica en [confirmSetup]. */
    private var firstSide: Side = Side.PLAYER

    init {
        viewModelScope.launch {
            val repo = withContext(Dispatchers.Default) { CardRepository.load() }
            core.repo = repo
            core.combatLog = CombatLog(
                spanish = true,
                cardName = { id -> repo[id.printed]?.name?.es ?: id.printed.raw },
                sideName = { s -> if (s == Side.PLAYER) "Tú" else "Rival" },
            )
            // Tablero provisional PvE: ambos repartos guardados (rival auto-elegido).
            core.buildProvisional = provisional@{ activeId, benchIds ->
                val mine = playerDealt ?: return@provisional null
                val opp = opponentDealt ?: return@provisional null
                GameSetup.provisional(
                    player = mine,
                    playerActiveId = activeId,
                    playerBenchIds = benchIds,
                    opponent = GameSetup.autoChoose(opp),
                )
            }
            // Punto de extensión para recompensas de PvE (aún sin premio concreto).
            core.onGameFinished = { winner -> onGameFinished(winner) }

            val (playerCards, oppCards) = withContext(Dispatchers.Default) { buildDecks() }
            engine = GameEngine(SeededRng(System.nanoTime()))
            agent = SmartAgent(engine, PveConfig.difficulty)
            core.engine = engine

            // MODO DEPURACIÓN: arranca directo en el tablero con la Banca LLENA (5) en
            // ambos lados, para VERIFICAR la maquetación del panal 3+2 sin jugar la
            // preparación. Poner en false para el flujo normal (moneda → reparto → prep).
            if (DEBUG_FULL_BENCH) {
                core.state = GameSetup.debugFullBench(playerCards, oppCards, SeededRng(System.nanoTime()))
                core.log += "[DEBUG] Tablero con banca llena (5) para verificación."
                core.emit()
                return@launch
            }

            // Reparto interactivo: ambos lados roban 7. Si un lado no saca ningún
            // Básico, rebaraja (MULLIGAN) y se cuenta para la compensación oficial.
            val dealRng = SeededRng(System.nanoTime())
            val (dealt, pm) = GameSetup.dealCounting(playerCards, dealRng)
            val (oDealt, om) = GameSetup.dealCounting(oppCards, dealRng)
            playerDealt = dealt
            opponentDealt = oDealt
            playerMulligans = pm
            opponentMulligans = om
            // Primero el lanzamiento de moneda inicial; al resolverse pasa a la
            // preparación (elegir Activo/Banca). Los repartos ya quedaron guardados.
            core.coinFlip = CoinFlipUiState(CoinPhase.CHOOSING)
            core.log += "Lanzamiento de moneda inicial: elige cara o cruz."
            core.emit()
        }
    }

    // -------------------------------------------------- lanzamiento de moneda

    /** El jugador elige cara (true) o cruz (false); anima el volado y resuelve. */
    override fun chooseCoin(heads: Boolean) {
        val cf = core.coinFlip ?: return
        if (cf.phase != CoinPhase.CHOOSING) return
        core.coinFlip = cf.copy(phase = CoinPhase.SPINNING, call = heads)
        core.emit()
        viewModelScope.launch {
            val result = kotlin.random.Random.nextBoolean()
            core.tryEmitFx(FxCue.Coin(Side.PLAYER, result))
            delay(1600) // giro de la moneda
            val won = result == heads
            if (won) {
                // Regla oficial: el ganador del volado decide quién empieza.
                core.coinFlip = CoinFlipUiState(
                    CoinPhase.CHOOSE_ORDER, call = heads, result = result, playerWon = true,
                    message = "Has ganado el lanzamiento. ¿Quién empieza?",
                )
                core.emit()
                // Espera a chooseFirst().
            } else {
                // El rival gana y decide empezar él (jugarás segundo).
                firstSide = Side.OPPONENT
                core.coinFlip = CoinFlipUiState(
                    CoinPhase.RESULT, call = heads, result = result, playerWon = false,
                    message = "El rival ha ganado y decide empezar. Jugarás segundo.",
                )
                core.emit()
                delay(2400)
                beginDeal()
            }
        }
    }

    /** El jugador (que ganó el volado) elige quién toma el primer turno. */
    override fun chooseFirst(playerFirst: Boolean) {
        val cf = core.coinFlip ?: return
        if (cf.phase != CoinPhase.CHOOSE_ORDER) return
        firstSide = if (playerFirst) Side.PLAYER else Side.OPPONENT
        core.coinFlip = cf.copy(
            phase = CoinPhase.RESULT,
            message = if (playerFirst) "Empiezas tú." else "Cedes el primer turno: empieza el rival.",
        )
        core.emit()
        viewModelScope.launch {
            delay(1600)
            beginDeal()
        }
    }

    // -------------------------------------------------------------- reparto (7)

    /**
     * Tras el volado, reparte la mano inicial: la UI anima las 7 cartas saliendo
     * del mazo hacia el abanico. El reparto (con mulligan) ya se resolvió en init.
     */
    private fun beginDeal() {
        val dealt = playerDealt ?: return
        core.coinFlip = null
        core.dealing = DealUiState(dealt.hand)
        core.log += "Robas tu mano inicial (7 cartas)."
        core.emit()
    }

    /** La UI avisa que terminó la animación del reparto; pasa a la preparación. */
    override fun onDealComplete() {
        if (core.dealing == null) return
        core.dealing = null
        beginSetup()
    }

    /** Tras el reparto, arranca la preparación interactiva desde el reparto guardado. */
    private fun beginSetup() {
        val dealt = playerDealt ?: return
        core.coinFlip = null
        core.dealing = null
        core.setup = SetupUiState(hand = dealt.hand, basics = GameSetup.basicsIn(dealt))
        core.rebuildProvisional()
        core.log += "Elige tu Pokémon Activo y coloca tu Banca."
        core.emit()
    }

    // ------------------------------------------------------------- preparación
    // Las mutaciones de preparación son idénticas en ambos modos → viven en [GameCore].

    override fun chooseActive(id: CardId) = core.chooseActive(id)

    override fun clearActive() = core.clearActive()

    override fun toggleBench(id: CardId) = core.toggleBench(id)

    /** Confirma la preparación: resuelve al rival, reparte premios y arranca el combate. */
    override fun confirmSetup() {
        val s = core.setup ?: return
        val activeId = s.activeId ?: return
        val mine = playerDealt ?: return
        val opp = opponentDealt ?: return
        val gs0 = GameSetup.finish(
            player = GameSetup.SideChoice(mine, activeId, s.benchIds),
            opponent = GameSetup.autoChoose(opp),
            firstSide = firstSide,
        )
        // Compensación oficial por MULLIGAN: el rival de quien rebarajó roba 1 carta
        // extra por cada mulligan que hizo el otro.
        var gs = GameSetup.drawExtra(gs0, Side.OPPONENT, playerMulligans)
        gs = GameSetup.drawExtra(gs, Side.PLAYER, opponentMulligans)
        core.setup = null
        core.state = gs
        if (playerMulligans > 0) core.log += "Hiciste $playerMulligans mulligan(s): el rival roba $playerMulligans carta(s) extra."
        if (opponentMulligans > 0) core.log += "El rival hizo $opponentMulligans mulligan(s): robas $opponentMulligans carta(s) extra."
        // Revelado inicial: el rival voltea sus Pokémon y se reparten los premios.
        // La UI reproduce el volteo/entrada; al terminar cedemos el primer turno.
        core.revealing = true
        core.log += "Ambos entrenadores revelan sus Pokémon."
        core.emit()
        viewModelScope.launch {
            delay(REVEAL_MS)
            core.revealing = false
            core.log += if (firstSide == Side.PLAYER) "¡Comienza el combate! Es tu turno."
                        else "¡Comienza el combate! Empieza el rival."
            core.emit()
            // Si empieza el rival, deja que la IA juegue su primer turno hasta cederte el control.
            if (core.state?.activeSide == Side.OPPONENT) advanceAi()
        }
    }

    /**
     * Baraja del jugador (activa del perfil) y un mazo rival GENERADO AL AZAR con lógica.
     * Para que ninguna partida PvE se repita, la IA no usa un mazo fijo: [AiDeckFactory]
     * arma una baraja legal de 60 con cualquier carta de efectos completos (mismo motor
     * que el editor). Semilla por tiempo → cada combate, un rival distinto.
     */
    private suspend fun buildDecks(): Pair<List<Card>, List<Card>> {
        val profile = profileRepo.profile.first()
        val playerDeck = profile.decks.firstOrNull { it.id == profile.activeDeckId }
            ?: profile.decks.firstOrNull()
            ?: StarterDecks.ALL.first().toDeck()
        val oppEntries = AiDeckFactory.buildRandomDeck(
            all = core.repo.all,
            rng = kotlin.random.Random(System.nanoTime()),
        )
        // Cada copia física recibe un id de INSTANCIA único, para que copias de la
        // misma carta impresa sean independientes en juego (energía/evolución/descarte).
        val playerCards = uniquify(playerDeck.expandedCardIds().mapNotNull { core.repo[it] })
        val oppCards = uniquify(AiDeckFactory.expand(oppEntries).mapNotNull { core.repo[it] })
        return playerCards to oppCards
    }

    /** Asigna a cada carta de la lista un id de instancia único (índice global). */
    private fun uniquify(cards: List<Card>): List<Card> =
        cards.mapIndexed { i, c -> c.withId(c.id.withInstance(i)) }

    /** Nombre legible de una carta (para etiquetas de decisión, etc.). */
    override fun cardName(id: CardId): String = cardNameOf(core.repo, id)

    /** Resuelve la carta por id en cualquier zona del estado (helper compartido). */
    override fun card(id: CardId): Card? = lookupCard(core.state, core.repo, id)

    /** Jugadas legales del lado en turno (vacío si hay decisión pendiente). */
    override fun legalIntents(): List<GameIntent> =
        core.state?.let { engine.legalIntents(it) } ?: emptyList()

    /** Aura de Habilidad (dorada manual / roja pasiva) del Pokémon, derivada del estado actual. */
    override fun abilityGlow(id: CardId): com.mineralord.tcg.engine.rules.AbilityGlow? =
        core.state?.let { engine.abilityGlow(it, id) }

    /** Aplica un intent del jugador y, si procede, deja jugar a la IA. */
    override fun onIntent(intent: GameIntent) {
        val current = core.state ?: return
        if (current.isOver) return
        viewModelScope.launch { applyAndAdvance(intent) }
    }

    /** Resuelve la decisión pendiente con las cartas elegidas. */
    override fun onResolve(chosen: List<CardId>) = onIntent(GameIntent.ResolveDecision(chosen))

    /** Juega un Objeto dirigido (Poción…) sobre un Pokémon y luego deja jugar a la IA. */
    override fun playItemOn(cardId: CardId, targetId: CardId) {
        val current = core.state ?: return
        if (current.isOver) return
        viewModelScope.launch {
            val results = core.applyItemOnTarget(cardId, targetId)
            results.forEach { core.playEventFx(it.events) }
            advanceAi()
        }
    }

    private suspend fun applyAndAdvance(intent: GameIntent) {
        val res = core.applyLocal(intent) ?: return
        // En modo VERIFICACIÓN, permite arrastrar varias energías por turno (la regla
        // real es una por turno) para poder probar el arrastre sin ciclar turnos.
        if (DEBUG_FULL_BENCH && intent is GameIntent.AttachEnergy) {
            core.state = core.state?.copy(energyAttachedThisTurn = false)
            core.emit()
        }
        core.playEventFx(res.events)
        advanceAi()
    }

    /**
     * Deja jugar a la IA (lado [Side.OPPONENT]) hasta que ceda el turno al jugador
     * o termine la partida. Incluye resolver sus propias decisiones. Guarda de
     * progreso por si insistiera en algo ilegal. Reutilizable desde [applyAndAdvance]
     * y desde [confirmSetup] cuando el rival empieza (ganó/eligió el primer turno).
     */
    private suspend fun advanceAi() {
        var guard = 0
        while (!(core.state?.isOver ?: true) && guard++ < 200) {
            val s = core.state ?: break
            // La IA promueve su nuevo Activo tras un KO en CUALQUIER momento (incluido
            // durante el turno del jugador: recoil/veneno o un KO por Objeto rival).
            if (Side.OPPONENT in s.pendingPromotion) {
                val decided = engine.apply(s, agent.decide(s, Side.OPPONENT))
                // Salvaguarda: si la IA no eligió una promoción válida, promueve el primero de su
                // Banca. Nunca se debe quedar el rival sin promover (bloquearía la partida entera).
                val r = if (decided.accepted) decided
                    else s.opponent.bench.firstOrNull()
                        ?.let { engine.apply(s, GameIntent.PromoteActive(it.card.id)) }
                if (r != null && r.accepted) { core.commit(r); core.playEventFx(r.events); continue }
                break // sin Banca para promover (no debería ocurrir): cede en vez de congelar
            }
            // Si el JUGADOR debe promover, la IA cede el control: la UI espera su elección.
            if (Side.PLAYER in s.pendingPromotion) break
            if (s.activeSide != Side.OPPONENT) break
            core.aiThinking = true
            core.emit()
            delay(450)
            val cur = core.state ?: break
            val r = engine.apply(cur, agent.decide(cur, Side.OPPONENT))
            if (r.accepted) { core.commit(r); core.playEventFx(r.events); continue }

            // La jugada de la IA fue RECHAZADA. Recuperación en capas para NO congelar la partida
            // (nunca se commitea un resultado rechazado, que antes hacía girar el guard en vano):
            //  1) si el rival tiene una decisión pendiente, decláinala (elegir nada) para desatascar;
            //  2) si no, o si falla, termina su turno;
            //  3) si nada de eso se acepta, cede el control (mejor ceder que quedar bloqueado).
            val recovery = if (cur.interaction?.side == Side.OPPONENT)
                engine.apply(cur, GameIntent.ResolveDecision(emptyList())) else null
            val fixed = when {
                recovery?.accepted == true -> recovery
                else -> engine.apply(cur, GameIntent.EndTurn).takeIf { it.accepted }
            }
            if (fixed != null) { core.commit(fixed); core.playEventFx(fixed.events); continue }
            break
        }
        core.aiThinking = false
        core.emit()
    }

    /**
     * Recompensa de PvE (Fase 2 §6.8): terminar una partida SIEMPRE otorga recursos. La
     * victoria da la recompensa plena; la derrota, una recompensa de participación menor
     * (§4.3.1: la economía no debe ser una barrera para jugar).
     */
    private fun onGameFinished(winner: Side) {
        val won = winner == Side.PLAYER
        val cristales = if (won) EconomyRules.PVE_WIN_CRISTALES else EconomyRules.PVE_LOSS_CRISTALES
        val monedas = if (won) EconomyRules.PVE_WIN_MONEDAS else EconomyRules.PVE_LOSS_MONEDAS
        viewModelScope.launch {
            profileRepo.credit(CurrencyKind.CRISTALES, cristales)
            profileRepo.credit(CurrencyKind.MONEDAS, monedas)
        }
    }
}
