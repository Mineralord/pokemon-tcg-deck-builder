package com.mineralord.tcg.feature.game

import android.app.Application
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.netplay.FxCueDto
import com.mineralord.tcg.data.netplay.GameIntentDto
import com.mineralord.tcg.data.netplay.GameReplay
import com.mineralord.tcg.data.netplay.MatchRole
import com.mineralord.tcg.data.netplay.MatchTransport
import com.mineralord.tcg.data.netplay.NetMessage
import com.mineralord.tcg.data.netplay.toDtoFor
import com.mineralord.tcg.data.netplay.toGameState
import com.mineralord.tcg.data.netplay.toDto
import com.mineralord.tcg.data.netplay.toIntent
import com.mineralord.tcg.data.profile.CurrencyKind
import com.mineralord.tcg.data.profile.EconomyRules
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.events.CombatLog
import com.mineralord.tcg.engine.events.GameEvent
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.withId
import com.mineralord.tcg.engine.rules.GameEngine
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.engine.rules.GameSetup
import com.mineralord.tcg.engine.rules.SeededRng
import com.mineralord.tcg.feature.game.anim.FxCue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Controlador de una partida ONLINE (2 humanos) sobre [MatchTransport], modelo
 * **host-autoritativo**: el HOST corre el [GameEngine] (única fuente de verdad) y
 * envía "fotos" ([NetMessage.Snapshot]) ya CENSURADAS a la vista del GUEST; el
 * GUEST no ejecuta motor: pinta lo que recibe y manda sus jugadas ([NetMessage.Intent]).
 *
 * Convención de lados en el estado real (host): HOST = [Side.PLAYER], GUEST = [Side.OPPONENT].
 * Al GUEST se le envía `toDtoFor(OPPONENT)` → se ve a sí mismo abajo, sin info oculta del rival.
 *
 * La LÓGICA DE JUEGO compartida (estado de UI, preparación, pipeline de intents, FX,
 * fin de partida) vive en [GameCore] —el MISMO que usa el modo PvE [GameViewModel]—,
 * por composición. Aquí solo queda lo específico del online: la mensajería de red y la
 * ceremonia networked (moneda justa entre ambos, reparto e intercambio de preparación).
 */
class OnlineGameController(
    app: Application,
    private val transport: MatchTransport,
    private val playerName: String,
    private val myDeckPrintedIds: List<String>,
    private val myDeckName: String = "Baraja",
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : GameController {

    private val role: MatchRole = transport.role
    private val isHost get() = role == MatchRole.HOST

    // Semilla ÚNICA compartida por el reparto y el motor: capturarla permite reconstruir
    // la partida entera de forma determinista (ver [GameReplay]).
    private val seed: Long = System.nanoTime()
    private val rng = SeededRng(seed)

    /** Log autoritativo (host) de intents ACEPTADOS, en orden. Base para replay/export (§A). */
    private val replayLog = mutableListOf<GameIntentDto>()

    /** Núcleo compartido con el modo PvE: dueño del estado, la preparación y el pipeline. */
    private val core = GameCore(scope)

    /** Perfil local para acreditar la recompensa de PvP casual (atómico). */
    private val profileRepo = ProfileRepository(app)

    override val ui: StateFlow<GameUiState> = core.ui
    override val fx: SharedFlow<FxCue> = core.fx

    // Ceremonia (HOST).
    private var hostDealt: GameSetup.DealtSide? = null
    private var guestDealt: GameSetup.DealtSide? = null
    private var hostMulligans = 0
    private var guestMulligans = 0
    private var firstSide: Side = Side.PLAYER
    // Llamadas de moneda de cada lado (true=cara). AMBOS eligen; el host lanza una
    // moneda compartida cuando tiene las dos y el ganador elige orden.
    private var hostCall: Boolean? = null
    private var guestCall: Boolean? = null
    private var hostChoice: GameSetup.SideChoice? = null
    private var guestChoice: GameSetup.SideChoice? = null
    private var guestDeckPrinted: List<String>? = null
    private var guestDeckName: String? = null

    // Ceremonia (GUEST): su mano repartida (cartas con id de instancia del host).
    private var myDealtHand: List<Card> = emptyList()

    private var snapshotSeq = 0

    /** Se pone a true al cerrar para detener el bucle de re-suscripción resiliente. */
    @Volatile
    private var closed = false

    init {
        scope.launch {
            val repo = withContext(Dispatchers.Default) { CardRepository.load() }
            core.repo = repo
            // Mismo registro legible (español) que el modo PvE, para no divergir.
            core.combatLog = CombatLog(
                spanish = true,
                cardName = { id -> repo[id.printed]?.name?.es ?: id.printed.raw },
                sideName = { s -> if (s == Side.PLAYER) "Tú" else "Rival" },
            )
            if (isHost) core.engine = GameEngine(rng)
            // Tablero provisional durante la preparación: mi mano + un rival stand-in
            // boca abajo (nunca se revela en preparación). Difiere host/guest en el reparto.
            core.buildProvisional = provisional@{ activeId, benchIds ->
                val myDealt = if (isHost) hostDealt else GameSetup.DealtSide(myDealtHand, emptyList())
                myDealt ?: return@provisional null
                val oppStandIn = runCatching { GameSetup.autoChoose(myDealt) }.getOrNull()
                    ?: return@provisional null
                GameSetup.provisional(
                    player = myDealt,
                    playerActiveId = activeId,
                    playerBenchIds = benchIds,
                    opponent = oppStandIn,
                )
            }
            // Punto de extensión para recompensas de PvP (aún sin premio concreto).
            core.onGameFinished = { winner -> onGameFinished(winner) }

            // Escucha del rival con re-suscripción RESILIENTE: si el flujo termina o
            // falla (p. ej. Firestore cerró el listener por un error transitorio),
            // re-colecta tras un backoff en vez de morir para siempre. El transporte
            // deduplica por `seq`, así que re-suscribirse NO repite la ceremonia; y al
            // reconectar el guest pide el snapshot actual por si perdió alguno.
            scope.launch {
                var backoffMs = 1_000L
                var firstAttempt = true
                while (isActive && !closed) {
                    if (!firstAttempt && !isHost) send(NetMessage.RequestSnapshot)
                    firstAttempt = false
                    runCatching { transport.incoming.collect { onMessage(it) } }
                    if (!isActive || closed) break
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(15_000L)
                }
            }
            // Preséntate con tu mazo (nombre + cartas).
            send(NetMessage.Hello(playerName, myDeckPrintedIds, myDeckName))
            core.log += "Tu baraja: «$myDeckName» (${myDeckPrintedIds.size} cartas)."
            core.log += if (isHost) "Esperando a que se una tu rival…" else "Conectando con el anfitrión…"
            core.emit()
        }
    }

    // ------------------------------------------------------------- mensajería

    private fun send(msg: NetMessage) {
        scope.launch { runCatching { transport.send(msg) } }
    }

    private fun onMessage(msg: NetMessage) {
        when (msg) {
            is NetMessage.Hello -> if (isHost) onGuestHello(msg) else onHostHello(msg)
            is NetMessage.CoinCall -> if (isHost) onGuestCoinCall(msg)
            is NetMessage.CoinResult -> if (!isHost) onCoinResult(msg)
            is NetMessage.ChooseOrder -> if (isHost) onGuestChooseOrder(msg)
            is NetMessage.Deal -> if (!isHost) onDealFromHost(msg)
            is NetMessage.Ceremony -> if (!isHost) onCeremony(msg)
            is NetMessage.SetupChoice -> if (isHost) onGuestSetupChoice(msg)
            is NetMessage.Snapshot -> if (!isHost) onSnapshot(msg)
            is NetMessage.Intent -> if (isHost) onGuestIntent(msg)
            is NetMessage.RequestSnapshot -> if (isHost) broadcast()
            is NetMessage.LogLine -> { core.log += msg.text; core.emit() }
            is NetMessage.Fx -> onFx(msg)
            is NetMessage.FxBatch -> if (!isHost) onFxBatch(msg)
            is NetMessage.Bye -> { core.log += "El rival abandonó (${msg.reason})."; core.emit() }
        }
    }

    // ------------------------------------------------------------- HOST: arranque

    /** Rehidrata un mazo de ids IMPRESOS a cartas con id de instancia único (base disjunta). */
    private fun buildDeck(printedIds: List<String>, base: Int): List<Card> =
        printedIds.mapNotNull { core.repo[CardId(it)] }
            .mapIndexed { i, c -> c.withId(c.id.withInstance(base + i)) }

    private fun onGuestHello(hello: NetMessage.Hello) {
        if (hostDealt != null) return // ya arrancó
        guestDeckPrinted = hello.deck
        guestDeckName = hello.deckName
        // BLINDAJE del mazo del rival: SIEMPRE se arma con el mazo que el invitado
        // envió en su Hello (nunca con el del host). Que ambos elijan la MISMA baraja
        // es válido (espejo) — no es error. Solo se protege el caso de que no llegue
        // baraja, para no arrancar con un rival vacío/erróneo.
        if (hello.deck.isEmpty()) {
            core.log += "⚠ ${hello.playerName} no envió su baraja; no se puede iniciar."
            core.emit()
            return
        }
        val hostCards = buildDeck(myDeckPrintedIds, base = 0)
        val guestCards = buildDeck(hello.deck, base = 100_000)
        val (hd, hm) = GameSetup.dealCounting(hostCards, rng)
        val (gd, gm) = GameSetup.dealCounting(guestCards, rng)
        hostDealt = hd; guestDealt = gd; hostMulligans = hm; guestMulligans = gm
        core.log += "¡${hello.playerName} se unió con «${hello.deckName}» (${hello.deck.size} cartas)! Ambos eligen cara o cruz."
        // Moneda JUSTA: AMBOS llaman cara/cruz; el host lanza UNA moneda compartida
        // cuando tiene las dos llamadas y el ganador del volado elige el orden.
        send(NetMessage.Ceremony("COIN"))
        core.coinFlip = CoinFlipUiState(CoinPhase.CHOOSING, message = "Elige cara o cruz.")
        core.emit()
    }

    /** GUEST: registra la baraja del anfitrión (informativo; no arma nada). */
    private fun onHostHello(hello: NetMessage.Hello) {
        guestDeckName = hello.deckName // el nombre del rival (host) para el log del guest
        core.log += "El anfitrión juega «${hello.deckName}» (${hello.deck.size} cartas)."
        core.emit()
    }

    // ------------------------------------------------------------- MONEDA

    override fun chooseCoin(heads: Boolean) {
        // AMBOS llaman cara/cruz. Tu llamada te pone a la espera (moneda girando); el
        // host lanza UNA moneda compartida cuando tiene las dos.
        val cf = core.coinFlip ?: return
        if (cf.phase != CoinPhase.CHOOSING) return
        core.coinFlip = cf.copy(phase = CoinPhase.SPINNING, call = heads, message = "Esperando al rival…")
        core.emit()
        if (isHost) {
            hostCall = heads
            maybeResolveCoin()
        } else {
            send(NetMessage.CoinCall(heads))
        }
    }

    /** HOST: el invitado llamó cara/cruz → registra su llamada e intenta resolver. */
    private fun onGuestCoinCall(msg: NetMessage.CoinCall) {
        guestCall = msg.heads
        maybeResolveCoin()
    }

    /**
     * HOST: con AMBAS llamadas, lanza UNA moneda compartida (50/50) y determina el
     * ganador: si las llamadas DIFIEREN gana quien acertó el resultado; si son IGUALES
     * (empate) se desempata 50/50. El ganador elige el orden; el otro espera.
     */
    private fun maybeResolveCoin() {
        val hc = hostCall ?: return
        val gc = guestCall ?: return
        scope.launch {
            val result = kotlin.random.Random.nextBoolean()
            core.tryEmitFx(FxCue.Coin(Side.PLAYER, result))
            val hostMatches = hc == result
            val guestMatches = gc == result
            val hostWon = when {
                hostMatches && !guestMatches -> true
                guestMatches && !hostMatches -> false
                else -> kotlin.random.Random.nextBoolean() // ambos igual: desempate justo
            }
            send(NetMessage.CoinResult(heads = result, winnerIsHost = hostWon))
            core.coinFlip = CoinFlipUiState(CoinPhase.SPINNING, result = result)
            core.emit()
            delay(1600)
            core.coinFlip = if (hostWon) {
                CoinFlipUiState(
                    CoinPhase.CHOOSE_ORDER, result = result, playerWon = true,
                    message = "Ganaste el lanzamiento. ¿Quién empieza?",
                )
            } else {
                CoinFlipUiState(
                    CoinPhase.RESULT, result = result, playerWon = false,
                    message = "Tu rival ganó y decide el orden.",
                )
            }
            core.emit() // si host ganó, espera chooseFirst(); si no, espera ChooseOrder del guest
        }
    }

    /** GUEST: recibe el resultado del volado del host. */
    private fun onCoinResult(msg: NetMessage.CoinResult) {
        scope.launch {
            core.coinFlip = CoinFlipUiState(CoinPhase.SPINNING, result = msg.heads)
            core.tryEmitFx(FxCue.Coin(Side.PLAYER, msg.heads))
            core.emit()
            delay(1600)
            val guestWon = !msg.winnerIsHost
            if (guestWon) {
                core.coinFlip = CoinFlipUiState(
                    CoinPhase.CHOOSE_ORDER, result = msg.heads, playerWon = true,
                    message = "Ganaste el lanzamiento. ¿Quién empieza?",
                )
            } else {
                core.coinFlip = CoinFlipUiState(
                    CoinPhase.RESULT, result = msg.heads, playerWon = false,
                    message = "El anfitrión ganó y decide el orden.",
                )
            }
            core.emit()
        }
    }

    override fun chooseFirst(playerFirst: Boolean) {
        val cf = core.coinFlip ?: return
        if (cf.phase != CoinPhase.CHOOSE_ORDER) return
        if (isHost) {
            // HOST ganó: playerFirst = empieza el host (PLAYER).
            firstSide = if (playerFirst) Side.PLAYER else Side.OPPONENT
            core.coinFlip = cf.copy(phase = CoinPhase.RESULT)
            core.emit()
            scope.launch { delay(1200); beginDealHost() }
        } else {
            // GUEST ganó: manda su decisión al host. playerFirst = empieza el guest.
            core.coinFlip = cf.copy(phase = CoinPhase.RESULT, message = if (playerFirst) "Empiezas tú." else "Cede el turno.")
            core.emit()
            send(NetMessage.ChooseOrder(winnerGoesFirst = playerFirst))
        }
    }

    /** HOST: el guest (ganador) eligió el orden. */
    private fun onGuestChooseOrder(msg: NetMessage.ChooseOrder) {
        // El guest es OPPONENT; si el ganador (guest) va primero → OPPONENT primero.
        firstSide = if (msg.winnerGoesFirst) Side.OPPONENT else Side.PLAYER
        scope.launch { delay(400); beginDealHost() }
    }

    // ------------------------------------------------------------- REPARTO

    private fun beginDealHost() {
        val hd = hostDealt ?: return
        val gd = guestDealt ?: return
        core.coinFlip = null
        // Avisa al guest e inícia su reparto con SU mano (ids de instancia del host).
        send(NetMessage.Deal(gd.hand.map { it.id.raw }))
        send(NetMessage.Ceremony("DEAL"))
        core.dealing = DealUiState(hd.hand)
        core.log += "Robas tu mano inicial (7 cartas)."
        core.emit()
    }

    /** GUEST: recibe su mano repartida y anima el reparto. */
    private fun onDealFromHost(msg: NetMessage.Deal) {
        myDealtHand = cardsFromIds(msg.hand)
        core.coinFlip = null
        core.dealing = DealUiState(myDealtHand)
        core.log += "Robas tu mano inicial (7 cartas)."
        core.emit()
    }

    override fun onDealComplete() {
        if (core.dealing == null) return
        core.dealing = null
        beginSetup()
    }

    // ------------------------------------------------------------- PREPARACIÓN

    private fun beginSetup() {
        core.setup = if (isHost) {
            val hd = hostDealt ?: return
            SetupUiState(hand = hd.hand, basics = GameSetup.basicsIn(hd))
        } else {
            SetupUiState(hand = myDealtHand, basics = myDealtHand.filterIsInstance<PokemonCard>().filter { it.isBasic })
        }
        core.rebuildProvisional()
        core.log += "Elige tu Pokémon Activo y coloca tu Banca."
        core.emit()
    }

    // Las mutaciones de preparación son idénticas en ambos modos → viven en [GameCore].
    override fun chooseActive(id: CardId) = core.chooseActive(id)

    override fun clearActive() = core.clearActive()

    override fun toggleBench(id: CardId) = core.toggleBench(id)

    override fun confirmSetup() {
        val s = core.setup ?: return
        val activeId = s.activeId ?: return
        if (isHost) {
            val hd = hostDealt ?: return
            hostChoice = GameSetup.SideChoice(hd, activeId, s.benchIds)
            core.setup = null
            core.log += "Listo. Esperando a tu rival…"
            core.emit()
            maybeFinishHost()
        } else {
            // El guest manda su elección; el host finaliza y devolverá el revelado.
            send(NetMessage.SetupChoice(activeId.raw, s.benchIds.map { it.raw }))
            core.setup = null
            core.log += "Listo. Esperando al anfitrión…"
            core.emit()
        }
    }

    /** HOST: recibió la preparación del guest. */
    private fun onGuestSetupChoice(msg: NetMessage.SetupChoice) {
        val gd = guestDealt ?: return
        guestChoice = GameSetup.SideChoice(gd, CardId(msg.activeId), msg.benchIds.map { CardId(it) })
        maybeFinishHost()
    }

    /** HOST: cuando ambos confirmaron, finaliza la preparación y arranca el combate. */
    private fun maybeFinishHost() {
        val hc = hostChoice ?: return
        val gc = guestChoice ?: return
        var gs = GameSetup.finish(player = hc, opponent = gc, firstSide = firstSide)
        // Compensación por mulligan (rival roba +1 por cada mulligan del otro).
        gs = GameSetup.drawExtra(gs, Side.OPPONENT, hostMulligans)
        gs = GameSetup.drawExtra(gs, Side.PLAYER, guestMulligans)
        core.setup = null
        core.state = gs
        core.revealing = true
        core.log += "Ambos entrenadores revelan sus Pokémon."
        send(NetMessage.Ceremony("REVEAL"))
        broadcast()
        core.emit()
        scope.launch {
            delay(REVEAL_MS)
            core.revealing = false
            core.log += if (firstSide == Side.PLAYER) "¡Comienza el combate! Es tu turno." else "¡Comienza el combate! Empieza el rival."
            send(NetMessage.Ceremony("PLAY"))
            broadcast(); core.emit()
        }
    }

    /** GUEST: transiciones de ceremonia anunciadas por el host. */
    private fun onCeremony(msg: NetMessage.Ceremony) {
        when (msg.phase) {
            "COIN" -> { core.coinFlip = CoinFlipUiState(CoinPhase.CHOOSING); core.emit() }
            "REVEAL" -> { core.revealing = true; core.emit() }
            "PLAY" -> {
                core.revealing = false
                core.log += "¡Comienza el combate!"
                core.emit()
            }
        }
    }

    // ------------------------------------------------------------- JUEGO

    override fun onIntent(intent: GameIntent) {
        if (isHost) {
            // El host aplica con el motor compartido (publica el rechazo si es ilegal).
            val res = core.applyLocal(intent) ?: return
            replayLog += intent.toDto()
            afterHostApply(res.events)
        } else {
            // GUEST: manda el intent al host autoritativo.
            send(NetMessage.Intent(intent.toDto()))
        }
    }

    override fun onResolve(chosen: List<CardId>) = onIntent(GameIntent.ResolveDecision(chosen))

    override fun playItemOn(cardId: CardId, targetId: CardId) {
        if (isHost) {
            // Atómico en el host (jugar Objeto + resolver objetivo); un solo broadcast/FX.
            val results = core.applyItemOnTarget(cardId, targetId)
            if (results.isNotEmpty()) {
                replayLog += GameIntent.PlayTrainer(cardId).toDto()
                if (results.size >= 2) replayLog += GameIntent.ResolveDecision(listOf(targetId)).toDto()
                afterHostApply(results.flatMap { it.events })
            }
        } else {
            // GUEST: manda los dos intents en orden; el host los aplica secuencialmente.
            send(NetMessage.Intent(GameIntent.PlayTrainer(cardId).toDto()))
            send(NetMessage.Intent(GameIntent.ResolveDecision(listOf(targetId)).toDto()))
        }
    }

    /** HOST: aplica una jugada recibida del guest (ignora en silencio las ilegales). */
    private fun onGuestIntent(msg: NetMessage.Intent) {
        val res = core.applyLocal(msg.intent.toIntent(), surfaceRejection = false) ?: return
        replayLog += msg.intent
        afterHostApply(res.events)
    }

    /**
     * HOST: tras consolidar el resultado en [GameCore] (estado + log + fin de partida),
     * difunde el snapshot censurado al guest y reproduce las animaciones de combate —las
     * MISMAS que PvE, vía [GameCore.playEventFx]—. La moneda además se propaga por cable
     * (el guest no ejecuta motor).
     */
    private fun afterHostApply(events: List<GameEvent>) {
        broadcast()
        // TODAS las animaciones de combate viajan al invitado (no solo la moneda): rótulos de ataque/
        // habilidad, daño, KO, premios, curación, estadio… en SU perspectiva (lados volteados). Así PvP
        // se ve EXACTAMENTE igual que PvE para ambos jugadores, sin distinción.
        val cues = events.mapNotNull { it.toFxCue() }
        if (cues.isNotEmpty()) send(NetMessage.FxBatch(cues.map { it.toGuestDto() }))
        scope.launch { core.playEventFx(events) }
    }

    /** GUEST: revive por cable un lote de animaciones del host, con el mismo espaciado que PvE/host. */
    private fun onFxBatch(msg: NetMessage.FxBatch) {
        val cues = msg.cues.map { it.toCue() }
        scope.launch { core.playCueFx(cues) }
    }

    /** GUEST/HOST: señal de animación puntual por cable (legado). La moneda de ceremonia usa otra vía. */
    private fun onFx(msg: NetMessage.Fx) {
        when (msg.kind) {
            "COIN" -> core.tryEmitFx(FxCue.Coin(Side.PLAYER, msg.amount == 1))
        }
    }

    /**
     * Mapea un [FxCue] del host a [FxCueDto] para el invitado, VOLTEANDO el lado a su perspectiva
     * (host = PLAYER; el invitado se ve a sí mismo como PLAYER). La moneda neutral (side null) no voltea.
     */
    private fun FxCue.toGuestDto(): FxCueDto = when (this) {
        is FxCue.Attack -> FxCueDto.Attack(side.other().name, attackName)
        is FxCue.AbilityUse -> FxCueDto.AbilityUse(side.other().name, pokemon.raw, manual)
        is FxCue.Damage -> FxCueDto.Damage(side.other().name, amount, weakness, resistance)
        is FxCue.Heal -> FxCueDto.Heal(side.other().name, amount)
        is FxCue.Knockout -> FxCueDto.Knockout(side.other().name)
        is FxCue.Prize -> FxCueDto.Prize(side.other().name, count)
        is FxCue.Coin -> FxCueDto.Coin(side?.other()?.name, heads)
        is FxCue.StadiumPlaced -> FxCueDto.StadiumPlaced(side.other().name, card.raw)
    }

    /** Reconstruye un [FxCue] desde su DTO recibido por red (ya en la perspectiva del invitado). */
    private fun FxCueDto.toCue(): FxCue = when (this) {
        is FxCueDto.Attack -> FxCue.Attack(Side.valueOf(side), attackName)
        is FxCueDto.AbilityUse -> FxCue.AbilityUse(Side.valueOf(side), CardId(pokemon), manual)
        is FxCueDto.Damage -> FxCue.Damage(Side.valueOf(side), amount, weakness, resistance)
        is FxCueDto.Heal -> FxCue.Heal(Side.valueOf(side), amount)
        is FxCueDto.Knockout -> FxCue.Knockout(Side.valueOf(side))
        is FxCueDto.Prize -> FxCue.Prize(Side.valueOf(side), count)
        is FxCueDto.Coin -> FxCue.Coin(side?.let { Side.valueOf(it) }, heads)
        is FxCueDto.StadiumPlaced -> FxCue.StadiumPlaced(Side.valueOf(side), CardId(card))
    }

    /** GUEST: recibe una foto del estado y la pinta (rehidratada en su perspectiva). */
    private fun onSnapshot(msg: NetMessage.Snapshot) {
        if (msg.seq < snapshotSeq) return
        snapshotSeq = msg.seq
        core.state = msg.state.toGameState(core.repo)
        core.setup = null; core.coinFlip = null; core.dealing = null
        core.emit()
        // El guest no ejecuta motor: dispara el fin de partida al ver un snapshot terminado
        // (mismo hook de recompensas que el host, para no distinguir entre modos).
        core.checkGameOver()
    }

    /** HOST: envía al guest su vista CENSURADA del estado. */
    private fun broadcast() {
        if (!isHost) return
        val s = core.state ?: return
        send(NetMessage.Snapshot(seq = ++snapshotSeq, state = s.toDtoFor(Side.OPPONENT)))
    }

    // ------------------------------------------------------------- consultas UI

    override fun cardName(id: CardId): String = cardNameOf(core.repo, id)

    override fun card(id: CardId): Card? = lookupCard(core.state, core.repo, id)

    /**
     * PARIDAD ONLINE: `legalIntents` es una función PURA del [GameState], y el invitado ya recibe
     * el snapshot COMPLETO del host (incluido `abilitiesUsedThisTurn`), reconstruido desde su
     * propia perspectiva (guest = [Side.PLAYER]). Por eso el invitado calcula la legalidad con un
     * motor de SOLO LECTURA (no autoritativo, no muta nada) y obtiene exactamente las mismas
     * habilidades manuales disponibles que el host. El `rng` es irrelevante para esta consulta.
     */
    private val queryEngine: GameEngine by lazy { core.engine ?: GameEngine(SeededRng(0)) }

    override fun legalIntents(): List<GameIntent> =
        core.state?.let { queryEngine.legalIntents(it) } ?: emptyList()

    /** Reconstruye cartas desde ids de instancia (del host) usando el repo local. */
    private fun cardsFromIds(ids: List<String>): List<Card> =
        ids.mapNotNull { raw -> val cid = CardId(raw); core.repo[cid.printed]?.withId(cid) }

    /**
     * Recompensa de PvP CASUAL (no clasificatorio): terminar una partida online SIEMPRE otorga
     * recursos de participación (Canon Fase 2 §4.3.1). El Ranked por temporada (Fase 10, Cap. VII)
     * es un sistema aparte y futuro; esto no lo suplanta. `winner == Side.PLAYER` = jugador local
     * (host y guest ven su lado como PLAYER).
     */
    private fun onGameFinished(winner: Side) {
        val won = winner == Side.PLAYER
        val cristales = if (won) EconomyRules.PVP_WIN_CRISTALES else EconomyRules.PVP_LOSS_CRISTALES
        val monedas = if (won) EconomyRules.PVP_WIN_MONEDAS else EconomyRules.PVP_LOSS_MONEDAS
        scope.launch {
            profileRepo.credit(CurrencyKind.CRISTALES, cristales)
            profileRepo.credit(CurrencyKind.MONEDAS, monedas)
        }
    }

    /**
     * HOST: exporta la partida como [GameReplay] serializable (semilla + preparación +
     * intents aceptados). Reconstruible de forma determinista con
     * [com.mineralord.tcg.data.netplay.reconstructFinal]. Devuelve `null` si aún no se
     * finalizó la preparación o si no somos el host (única fuente autoritativa del log).
     */
    fun exportReplay(): GameReplay? {
        if (!isHost) return null
        val hc = hostChoice ?: return null
        val gc = guestChoice ?: return null
        val gd = guestDeckPrinted ?: return null
        return GameReplay(
            seed = seed,
            hostDeck = myDeckPrintedIds,
            guestDeck = gd,
            firstSideIsHost = firstSide == Side.PLAYER,
            hostActiveId = hc.activeId.raw,
            hostBenchIds = hc.benchIds.map { it.raw },
            guestActiveId = gc.activeId.raw,
            guestBenchIds = gc.benchIds.map { it.raw },
            intents = replayLog.toList(),
        )
    }

    fun close() {
        closed = true
        scope.launch {
            runCatching { transport.send(NetMessage.Bye("cerró la app")) }
            runCatching { transport.close() }
            scope.cancel() // detiene el bucle de re-suscripción y libera el listener
        }
    }

    private companion object {
        const val REVEAL_MS = 2000L
    }
}
