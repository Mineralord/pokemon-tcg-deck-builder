package com.mineralord.tcg.feature.game.combat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.mineralord.tcg.feature.game.anim.FxCue
import com.mineralord.tcg.feature.game.board.typeColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.Ability
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.Target
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.feature.game.CoinFlipOverlay
import com.mineralord.tcg.feature.game.DealOverlay
import com.mineralord.tcg.feature.game.cardTargetsPokemon
import com.mineralord.tcg.feature.game.itemTargetChoose
import com.mineralord.tcg.feature.game.toolAttachScope
import com.mineralord.tcg.core.designsystem.CardDetailDialog
import com.mineralord.tcg.core.designsystem.actor.actorControlled
import com.mineralord.tcg.core.designsystem.motion.MotionContainer
import com.mineralord.tcg.core.designsystem.motion.MotionDialog
import com.mineralord.tcg.core.designsystem.motion.MotionEdge
import com.mineralord.tcg.core.designsystem.motion.MotionPanel
import com.mineralord.tcg.core.designsystem.motion.MotionTransitions
import com.mineralord.tcg.core.designsystem.motion.FlightOverlay
import com.mineralord.tcg.core.designsystem.motion.rememberFlightHost
import com.mineralord.tcg.core.designsystem.motion.motionAppear
import com.mineralord.tcg.core.designsystem.motion.motionElevate
import com.mineralord.tcg.core.designsystem.motion.motionPress
import com.mineralord.tcg.core.designsystem.EnergySphere
import com.mineralord.tcg.core.designsystem.typeColor
import com.mineralord.tcg.core.designsystem.tilt.resolveFinish
import com.mineralord.tcg.feature.game.board.BoardGeometry
import com.mineralord.tcg.feature.game.board.HandCat
import com.mineralord.tcg.feature.game.board.HandFan
import com.mineralord.tcg.feature.game.board.HandFilterBar
import com.mineralord.tcg.feature.game.board.handCatOf
import com.mineralord.tcg.feature.game.board.sortedHandCards

/**
 * Pantalla de combate canónica — replica 1:1 las MEDIDAS y POSICIONES de [BoardGeometry]
 * (mismas cajas que la pantalla clásica) pero es 100% DINÁMICA: el tapete se DIBUJA en
 * Compose ([CombatMat], arte propio, responsive) en vez del bitmap horneado, y cada zona
 * es un Composable colocado por su NBox. Sin assets de terceros.
 *
 * Contrato intacto: solo lee [CombatSceneController.ui] y emite por sus métodos. No toca motor,
 * reglas ni ViewModels. La pantalla es la MISMA para el juego y para el Studio; sólo cambia el
 * controlador que se le inyecta (el anfitrión provee el suyo; ya no hay `viewModel` por defecto).
 */
@Composable
fun CombatScreen(
    onExit: () -> Unit,
    vm: CombatSceneController,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val state = ui.state

    // FX PREMIUM de ataque en el lente central (color+motivo por TIPO, ~1 s). SINCRONIZADO con
    // el IMPACTO de daño (FxCue.Damage): revienta sobre el Pokémon golpeado (el defensor =
    // cue.side) con el color/motivo del TIPO del ATACANTE (el activo del lado contrario).
    var lensFx by remember { mutableStateOf<LensFxSpec?>(null) }
    LaunchedEffect(Unit) {
        vm.fx.collect { cue ->
            if (cue is FxCue.Damage) {
                val st = vm.ui.value.state ?: return@collect
                // El defensor recibe el daño (cue.side); el atacante es el activo del OTRO lado.
                val attacker = if (cue.side == Side.PLAYER) st.opponent.active else st.player.active
                val type = attacker?.card?.types?.firstOrNull()
                lensFx = LensFxSpec(
                    id = System.nanoTime(),
                    color = typeColor(type),
                    motif = motifFor(type),
                    // fromPlayer = ataca el jugador → impacto arriba (activo rival golpeado).
                    fromPlayer = cue.side == Side.OPPONENT,
                )
            }
        }
    }

    var inspect by remember { mutableStateOf<Card?>(null) }
    var zonePanel by remember { mutableStateOf<Pair<String, List<Card>>?>(null) }
    // Registro de batalla (lo abre el botón de lista+lupa de la columna derecha).
    var showLog by remember { mutableStateOf(false) }
    // ---- VUELO REAL entre zonas (capa Motion) ----
    // Host de vuelos + rects de ancla por zona (coords de raíz) + última clasificación de
    // cartas para detectar movimientos por diffing de estado (no toca lógica ni reglas).
    val flightHost = rememberFlightHost()
    val anchorBounds = remember { mutableStateMapOf<CardZone, Rect>() }
    var prevZones by remember { mutableStateOf<Map<CardId, CardZone>?>(null) }
    // Reloj de partida por lado (banco de tiempo mm:ss) que descuenta en el lado ACTIVO,
    // como el temporizador de las pestañas de estado de TCG Live. Arranca en 25:00.
    var oppClock by remember { mutableStateOf(25 * 60) }
    var meClock by remember { mutableStateOf(25 * 60) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val u = vm.ui.value
            val s = u.state ?: continue
            if (s.winner != null || u.setup != null || u.revealing) continue
            if (s.activeSide == Side.PLAYER) meClock = (meClock - 1).coerceAtLeast(0)
            else oppClock = (oppClock - 1).coerceAtLeast(0)
        }
    }
    // Carta de la mano tocada que espera ELEGIR un objetivo (energía → Pokémon,
    // evolución → su pre-evolución). null = no hay jugada en curso.
    var pendingPlay by remember { mutableStateOf<Card?>(null) }
    // Barra inferior de la mano (Pokémon/Entrenador/Energía): al tocar un icono, la mano se
    // DESLIZA hasta donde empieza esa categoría (como TCG Live). `handFocus` = categoría pulsada;
    // `handFocusNonce` cambia en cada toque para re-desplazar aunque se repita la categoría.
    var handFocus by remember { mutableStateOf<HandCat?>(null) }
    var handFocusNonce by remember { mutableStateOf(0) }
    // ---- Arrastre para jugar cartas de la mano ----
    // `dragCard` = carta arrastrada; `dragPos` = posición del dedo (coords de raíz);
    // `targetBounds` = límites de cada Pokémon propio; `benchSlotBounds`/`activeSlotBounds`
    // = huecos para soltar un Básico; `centerBounds` = panel central (jugar Entrenadores).
    var dragCard by remember { mutableStateOf<Card?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    val targetBounds = remember { mutableStateMapOf<CardId, Rect>() }
    val benchSlotBounds = remember { mutableStateMapOf<Int, Rect>() }
    var activeSlotBounds by remember { mutableStateOf<Rect?>(null) }
    var centerBounds by remember { mutableStateOf<Rect?>(null) }
    // Retirada por arrastre: tras pulsar RETIRARSE en el detalle, se arrastra un Pokémon de
    // la Banca sobre el Activo para reemplazarlo (GameIntent.Retreat).
    var retreatMode by remember { mutableStateOf(false) }
    var benchDragCard by remember { mutableStateOf<Card?>(null) }
    var benchDragPos by remember { mutableStateOf(Offset.Zero) }

    Box(modifier.fillMaxSize().background(Color(0xFF06080D))) {

        // ---- Ceremonia inicial: reutiliza los overlays existentes ----
        val coinFlip = ui.coinFlip
        if (coinFlip != null) {
            CoinFlipOverlay(coinFlip, onCall = vm::chooseCoin, onChooseFirst = vm::chooseFirst)
            return@Box
        }
        val dealing = ui.dealing
        if (dealing != null) {
            DealOverlay(cards = dealing.hand, onComplete = vm::onDealComplete)
            return@Box
        }
        if (state == null) {
            CombatLoading()
            return@Box
        }

        val setup = ui.setup
        val inSetup = setup != null
        val revealing = ui.revealing
        val mustPromote = state.pendingPromotion.contains(Side.PLAYER)
        val myTurn = state.activeSide == Side.PLAYER &&
            !revealing && !inSetup && state.interaction == null && !mustPromote

        // ---- VUELO REAL: diff de zonas entre estados → una carta que cambia de zona VUELA
        // desde su ancla de origen a la de destino con trayectoria física. Se omite durante
        // la ceremonia inicial (reparto/preparación/revelado) para no encadenar decenas de vuelos.
        LaunchedEffect(state, inSetup, revealing) {
            val now = classifyZones(state)
            val prev = prevZones
            val quiet = !inSetup && !revealing && ui.dealing == null
            if (prev != null && quiet) {
                val cards = allCardsById(state)
                now.forEach { (id, zone) ->
                    val old = prev[id]
                    if (old != null && old != zone) {
                        val from = anchorBounds[old]
                        val to = anchorBounds[zone]
                        val card = cards[id]
                        if (from != null && to != null && card != null) {
                            val faceUp = zone.faceUp
                            flightHost.launch(from, to) {
                                CombatCard(
                                    imageUrl = if (faceUp) card.artwork.small(true) else null,
                                    faceDown = !faceUp,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
            prevZones = now
        }

        // ---- Tablero proporcional 1:1 (bloqueado a 1080:2400, centrado) ----
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val availW = maxWidth
            val availH = maxHeight
            val boardW: Dp
            val boardH: Dp
            if (availW <= availH * BoardGeometry.Aspect) {
                boardW = availW; boardH = availW / BoardGeometry.Aspect
            } else {
                boardH = availH; boardW = availH * BoardGeometry.Aspect
            }

            Box(Modifier.size(boardW, boardH).align(Alignment.Center)) {
                // Tapete ORIGINAL dibujado, alineado a las bandas de BoardGeometry.
                // El riel del lado activo se enciende como tubo de neón (indicador de turno);
                // oculto en preparación/revelado/fin de partida.
                val litSide = if (inSetup || revealing || state.winner != null) null else state.activeSide
                // Tipo del Pokémon Activo en foco (lado en turno): alimenta el ambiente reactivo
                // de la lente central. Solo lectura del estado; el ambiente es pura presentación.
                val activeType = if (litSide == null) null
                    else state.activePlayer.active?.card?.types?.firstOrNull()
                CombatMat(Modifier.matchParentSize(), litSide = litSide, activeType = activeType)

                // Anclas invisibles por zona: capturan el rect (coords de raíz) de cada zona
                // para que la capa de VUELO sepa de/hacia dónde viajan las cartas. Sin pointer
                // input → no interceptan toques; se dibujan bajo el resto de zonas.
                CardZone.values().forEach { z ->
                    Box(
                        Modifier.place(zoneNBox(z), boardW, boardH)
                            .onGloballyPositioned { anchorBounds[z] = it.boundsInRoot() },
                    )
                }

                val opp = state.opponent
                val me = state.player
                val faceDownOpp = inSetup || revealing

                // Objetivo (Pokémon propio) bajo el dedo según lo que se arrastra:
                //  - energía/evolución → dropTargetUnder (isPlayTarget)
                //  - Objeto dirigido (Poción) → itemTargetUnder (ámbito del efecto, solo dañados si aplica)
                //  - Herramienta → toolTargetUnder (Pokémon sin Herramienta)
                fun dropTargetUnder(card: Card): CardId? =
                    (listOfNotNull(me.active) + me.bench).firstOrNull { pip ->
                        isPlayTarget(card, pip) && targetBounds[pip.card.id]?.contains(dragPos) == true
                    }?.card?.id
                fun itemTargetUnder(choose: EffectOp.ChooseTarget): CardId? {
                    val cands = when (choose.from) {
                        Target.OWN_ACTIVE -> listOfNotNull(me.active)
                        Target.OWN_BENCH -> me.bench
                        else -> me.allInPlay
                    }.filter { !choose.onlyDamaged || it.damage > 0 }
                    return cands.firstOrNull { targetBounds[it.card.id]?.contains(dragPos) == true }?.card?.id
                }
                fun toolTargetUnder(): CardId? =
                    me.allInPlay.firstOrNull { it.attachedTools.isEmpty() && targetBounds[it.card.id]?.contains(dragPos) == true }?.card?.id

                val dragging = dragCard
                val dragItemChoose = (dragging as? TrainerCard)?.let { itemTargetChoose(it) }
                val dragToolScope = (dragging as? TrainerCard)?.let { toolAttachScope(it) }
                val hoverTargetId: CardId? = when {
                    dragging == null -> null
                    dragItemChoose != null -> itemTargetUnder(dragItemChoose)
                    dragToolScope != null -> toolTargetUnder()
                    else -> dropTargetUnder(dragging)
                }
                // El panel central solo se ilumina para Entrenadores que NO apuntan a un Pokémon.
                val centerHot = dragging is TrainerCard && !cardTargetsPokemon(dragging) &&
                    centerBounds?.contains(dragPos) == true
                val activeSetupHot = inSetup && dragging is PokemonCard && dragging.isBasic &&
                    activeSlotBounds?.contains(dragPos) == true

                // Panel central (soltar Entrenadores): captura límites + glow al sobrevolar.
                Box(
                    Modifier.place(BoardGeometry.CenterPanel, boardW, boardH)
                        .onGloballyPositioned { centerBounds = it.boundsInRoot() }
                        .then(if (centerHot) Modifier.border(2.dp, CombatTheme.Gold, RoundedCornerShape(20.dp)) else Modifier),
                )

                // ---------- RIVAL ----------
                // Mano rival (dorsos).
                Box(Modifier.place(BoardGeometry.OppHand, boardW, boardH)) {
                    OppHandBacks(opp.hand.size)
                }
                // Banca rival (por slots del panal).
                opp.bench.forEachIndexed { i, pip ->
                    BoardGeometry.OppBenchSlots.getOrNull(i)?.let { slot ->
                        Box(Modifier.place(slot, boardW, boardH)) {
                            FieldCard(pip, faceDown = faceDownOpp, onTap = { inspect = pip.card })
                        }
                    }
                }
                // Activo rival.
                Box(Modifier.place(BoardGeometry.OppActive, boardW, boardH)) {
                    FieldCard(opp.active, faceDown = faceDownOpp, onTap = { opp.active?.let { inspect = it.card } })
                }
                // Premios / mazo / descarte rival.
                Box(Modifier.place(BoardGeometry.OppPrizes, boardW, boardH)) {
                    PrizeFan(opp.prizesRemaining)
                }
                Box(
                    Modifier.place(BoardGeometry.OppDiscard, boardW, boardH)
                        .clickable(enabled = opp.discard.isNotEmpty()) { zonePanel = "Descarte del rival" to opp.discard },
                ) {
                    PileStack(opp.discard.size, faceDown = false, topCard = opp.discard.lastOrNull())
                }
                Box(Modifier.place(BoardGeometry.OppDeck, boardW, boardH)) {
                    PileStack(opp.deck.size, faceDown = true)
                }

                // ---------- ESTADIO ----------
                state.stadium?.let { stad ->
                    Box(Modifier.place(BoardGeometry.Stadium, boardW, boardH)) {
                        FieldCardImage(stad.artwork.small(true), stad.name.es) { inspect = stad }
                    }
                }

                // ---------- JUGADOR ----------
                Box(
                    Modifier.place(BoardGeometry.MeActive, boardW, boardH)
                        .onGloballyPositioned { c ->
                            val r = c.boundsInRoot(); activeSlotBounds = r
                            me.active?.card?.id?.let { targetBounds[it] = r }
                        },
                ) {
                    FieldCard(
                        me.active,
                        selected = false,
                        highlighted = activeSetupHot ||
                            (hoverTargetId != null && hoverTargetId == me.active?.card?.id) ||
                            (retreatMode && benchDragCard != null && activeSlotBounds?.contains(benchDragPos) == true),
                        onTap = {
                            when {
                                inSetup -> vm.clearActive()
                                me.active == null -> {}
                                // Tocar el Activo = VER A DETALLE (holo/3D). Atacar = botón ⚔.
                                else -> me.active?.let { inspect = it.card }
                            }
                        },
                    )
                }
                me.bench.forEachIndexed { i, pip ->
                    BoardGeometry.MeBenchSlots.getOrNull(i)?.let { slot ->
                        Box(
                            Modifier.place(slot, boardW, boardH)
                                .onGloballyPositioned { targetBounds[pip.card.id] = it.boundsInRoot() },
                        ) {
                            FieldCard(
                                pip,
                                highlighted = mustPromote || (hoverTargetId == pip.card.id),
                                onTap = {
                                    when {
                                        inSetup -> vm.toggleBench(pip.card.id)
                                        mustPromote -> vm.onIntent(GameIntent.PromoteActive(pip.card.id))
                                        // Tocar un Pokémon de Banca = VER A DETALLE (holo/3D).
                                        else -> inspect = pip.card
                                    }
                                },
                                // Retirada: arrastrar este Pokémon sobre el Activo lo reemplaza.
                                draggable = retreatMode,
                                onDragStart = { pos -> benchDragCard = pip.card; benchDragPos = pos },
                                onDrag = { pos -> benchDragPos = pos },
                                onDragEnd = {
                                    if (retreatMode && activeSlotBounds?.contains(benchDragPos) == true) {
                                        vm.onIntent(GameIntent.Retreat(pip.card.id))
                                    }
                                    benchDragCard = null; retreatMode = false
                                },
                            )
                        }
                    }
                }
                // Slots de Banca VACÍOS: capturan sus límites (para soltar un Básico) y se
                // iluminan mientras se arrastra un Básico sobre ellos.
                for (i in me.bench.size until BoardGeometry.MeBenchSlots.size) {
                    val slot = BoardGeometry.MeBenchSlots[i]
                    val hot = dragging is PokemonCard && dragging.isBasic &&
                        benchSlotBounds[i]?.contains(dragPos) == true
                    Box(
                        Modifier.place(slot, boardW, boardH)
                            .onGloballyPositioned { benchSlotBounds[i] = it.boundsInRoot() },
                    ) { EmptySlot(Modifier.fillMaxSize(), highlighted = hot) }
                }
                // Los PREMIOS propios NO se inspeccionan (información oculta para ti mismo).
                Box(Modifier.place(BoardGeometry.MePrizes, boardW, boardH)) {
                    PrizeFan(me.prizesRemaining)
                }
                Box(
                    Modifier.place(BoardGeometry.MeDiscard, boardW, boardH)
                        .clickable(enabled = me.discard.isNotEmpty()) { zonePanel = "Tu descarte" to me.discard },
                ) { PileStack(me.discard.size, faceDown = false, topCard = me.discard.lastOrNull()) }
                // El MAZO propio NO se inspecciona (información oculta).
                Box(Modifier.place(BoardGeometry.MeDeck, boardW, boardH)) {
                    PileStack(me.deck.size, faceDown = true)
                }

                // Mano del jugador — REUTILIZA el HandFan de TCG Live (abanico, agrupación de
                // copias idénticas con contador, orden por supertipo, scroll para ojear, y
                // dimensiones 1:1 = alto·aspecto de carta). Comportamiento idéntico a
                // la pantalla clásica; aquí se juega por TOQUE (canDrag por defecto = false).
                // Alto MEDIDO en ref_0034 (Combate 1): carta de mano ≈0.26W → alto ≈0.163H
                // (la "Ultra Ball" ocupa x≈0.44..0.70). Local al combate para no alterar la clásica.
                val handCardH = boardH * 0.163f
                val handCardW = handCardH * BoardGeometry.CardAspect
                Box(Modifier.place(BoardGeometry.MeHand, boardW, boardH)) {
                    HandFan(
                        cards = sortedHandCards(me.hand),
                        enabled = inSetup || (myTurn && state.interaction == null),
                        // La barra no atenúa: DESLIZA la mano a la categoría pulsada.
                        focusCategory = handFocus,
                        focusNonce = handFocusNonce,
                        cardW = handCardW,
                        cardH = handCardH,
                        selectedId = pendingPlay?.id,
                        onSelect = { card ->
                            when {
                                // En preparación, tocar un Básico lo coloca (Activo/Banca).
                                inSetup -> if (card is PokemonCard && card.isBasic) {
                                    if (setup?.activeId == null) vm.chooseActive(card.id) else vm.toggleBench(card.id)
                                }
                                // En juego, tocar una carta la MUESTRA A DETALLE (holo/3D).
                                // Para JUGARLA, arrástrala (energía→Pokémon, evolución, Básico→Banca, Entrenador→centro).
                                else -> inspect = card
                            }
                        },
                        // ---- Arrastre 1:1: energía→Pokémon, evolución→pre-evolución, Básico→
                        //      Banca/Activo(setup), Entrenador→panel central. ----
                        canDrag = { card ->
                            if (inSetup) card is PokemonCard && card.isBasic
                            else card is EnergyCard || card is PokemonCard || card is TrainerCard
                        },
                        onCardDragStart = { card, pos -> dragCard = card; dragPos = pos },
                        onCardDrag = { pos -> dragPos = pos },
                        onCardDragCancel = { dragCard = null },
                        onCardDragEnd = {
                            val card = dragCard
                            if (card != null) {
                                if (inSetup) {
                                    if (card is PokemonCard && card.isBasic) {
                                        when {
                                            activeSlotBounds?.contains(dragPos) == true -> vm.chooseActive(card.id)
                                            benchSlotBounds.values.any { it.contains(dragPos) } -> vm.toggleBench(card.id)
                                        }
                                    }
                                } else {
                                    when (card) {
                                        is EnergyCard ->
                                            dropTargetUnder(card)?.let { vm.onIntent(GameIntent.AttachEnergy(card.id, it)) }
                                        is PokemonCard ->
                                            if (card.isBasic) {
                                                if (benchSlotBounds.values.any { it.contains(dragPos) }) {
                                                    vm.onIntent(GameIntent.PlayBasicToBench(card.id))
                                                }
                                            } else {
                                                dropTargetUnder(card)?.let { vm.onIntent(GameIntent.Evolve(card.id, it)) }
                                            }
                                        is TrainerCard -> {
                                            val itemChoose = itemTargetChoose(card)
                                            val toolScope = toolAttachScope(card)
                                            when {
                                                // Objeto dirigido (Poción…) soltado sobre un Pokémon válido → un gesto.
                                                itemChoose != null ->
                                                    itemTargetUnder(itemChoose)?.let { vm.playItemOn(card.id, it) }
                                                // Herramienta soltada sobre un Pokémon sin Herramienta.
                                                toolScope != null ->
                                                    toolTargetUnder()?.let { vm.onIntent(GameIntent.AttachTool(card.id, it)) }
                                                // Entrenador normal → panel central.
                                                centerBounds?.contains(dragPos) == true ->
                                                    vm.onIntent(GameIntent.PlayTrainer(card.id))
                                            }
                                        }
                                    }
                                }
                            }
                            dragCard = null
                        },
                    )
                }
                // Barra de filtros por tipo (contador total + iconos Pokémon/Entrenador/Energía).
                // POSICIÓN 1:1 medida en ref_0034: pill blanco al FONDO, centrado, solapando la
                // parte baja del abanico (centro ≈ x0.50, y0.967; barra sólida y≈0.950..0.984).
                // El pill se auto-dimensiona; la caja solo lo centra. Se dibuja DESPUÉS del abanico
                // (queda por encima), como en TCG Live.
                if (me.hand.isNotEmpty()) {
                    Box(
                        Modifier.place(BoardGeometry.NBox(0.20f, 0.945f, 0.60f, 0.044f), boardW, boardH),
                        contentAlignment = Alignment.Center,
                    ) {
                        HandFilterBar(
                            cards = me.hand,
                            active = handFocus,
                            onToggle = { cat -> handFocus = cat; handFocusNonce++ },
                        )
                    }
                }

                // ---------- HUD / rieles ----------
                Box(Modifier.place(BoardGeometry.TopSettings, boardW, boardH)) {
                    IconChip("⏻", CombatTheme.Foe, onExit)
                }
                // Columna derecha 1:1 con TCG Live: pestañas de estado (rival arriba / jugador
                // abajo, espejo) con barra de turno + chevrones + temporizador + premios, y el
                // botón de registro de batalla debajo.
                Box(Modifier.place(BoardGeometry.RailStatusOpp, boardW, boardH)) {
                    PlayerStatusPanel(
                        prizes = opp.prizesRemaining,
                        prizeAccent = Color(0xFFCE3B39),
                        isOpponent = true,
                        active = litSide == Side.OPPONENT,
                        timerText = mmss(oppClock),
                    )
                }
                Box(Modifier.place(BoardGeometry.RailStatusMe, boardW, boardH)) {
                    PlayerStatusPanel(
                        prizes = me.prizesRemaining,
                        prizeAccent = Color(0xFF3977BE),
                        isOpponent = false,
                        active = litSide == Side.PLAYER,
                        timerText = mmss(meClock),
                    )
                }
                Box(Modifier.place(BoardGeometry.RailLog, boardW, boardH)) {
                    BattleLogButton(onClick = { showLog = true })
                }
                Box(Modifier.place(BoardGeometry.RailEndTurn, boardW, boardH), contentAlignment = Alignment.Center) {
                    val enabled = if (inSetup) (setup?.canConfirm == true) else myTurn
                    PillButton(
                        label = if (inSetup) "LISTO" else "FIN",
                        enabled = enabled,
                        accent = if (inSetup) CombatTheme.Good else CombatTheme.Gold,
                        onClick = { if (inSetup) vm.confirmSetup() else vm.onIntent(GameIntent.EndTurn) },
                    )
                }
                // (El ATAQUE y la RETIRADA viven ahora en el VISOR A DETALLE del Activo propio.)
                // Banner de preparación / promoción.
                val banner = when {
                    retreatMode -> "Arrastra un Pokémon de la Banca al Activo para retirarte · toca aquí para cancelar"
                    inSetup -> "Coloca tu Activo y tu Banca, luego pulsa LISTO"
                    mustPromote -> "Elige tu nuevo Pokémon Activo"
                    else -> null
                }
                banner?.let {
                    Box(
                        Modifier.place(BoardGeometry.SetupBanner, boardW, boardH)
                            .clickable(enabled = retreatMode) { retreatMode = false; benchDragCard = null },
                        contentAlignment = Alignment.Center,
                    ) {
                        BannerText(it)
                    }
                }
            }
        }

        // ---- Capa MODAL / OVERLAY (todo animado con el framework Motion) ----
        // Decisión pendiente del jugador: entra/sale deslizando desde abajo (retiene su
        // contenido durante la salida para no cortar la animación).
        val decisionInter = state.interaction?.takeIf { it.side == Side.PLAYER }
        var lastDecision by remember { mutableStateOf(decisionInter?.decision) }
        if (decisionInter != null) lastDecision = decisionInter.decision
        MotionPanel(
            visible = decisionInter != null,
            edge = MotionEdge.Bottom,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            lastDecision?.let { dec ->
                DecisionPanel(decision = dec, cardOf = { id -> vm.card(id) }, onResolve = { vm.onResolve(it) })
            }
        }

        // Inspector de zona (descartes): diálogo Motion (scrim fundido + contenido con pop).
        var lastZone by remember { mutableStateOf(zonePanel) }
        if (zonePanel != null) lastZone = zonePanel
        MotionDialog(visible = zonePanel != null, onDismiss = { zonePanel = null }) {
            lastZone?.let { (title, cards) ->
                ZoneInspectorBody(title, cards, onCardTap = { inspect = it }, onDismiss = { zonePanel = null })
            }
        }

        // Registro de batalla: diálogo Motion.
        MotionDialog(visible = showLog, onDismiss = { showLog = false }) {
            BattleLogBody(ui.log, onDismiss = { showLog = false })
        }

        // Visor a DETALLE (giroscopio + holo + pinch): conserva su propio scrim/gyro; se
        // envuelve con un fundido Motion para que aparezca/desaparezca con suavidad.
        var lastInspect by remember { mutableStateOf(inspect) }
        if (inspect != null) lastInspect = inspect
        MotionContainer(
            visible = inspect != null,
            enter = MotionTransitions.overlayEnter(),
            exit = MotionTransitions.overlayExit(),
        ) {
            lastInspect?.let { card ->
                val activePip = state.player.active
                val showActions = myTurn && activePip != null && card.id == activePip.card.id
                CardDetailDialog(
                    imageUrl = card.artwork.large(true),
                    contentDescription = card.name.es,
                    onDismiss = { inspect = null },
                    rarity = card.rarity,
                    cardNumber = card.id.printed.raw.substringAfterLast('-').toIntOrNull(),
                    // Sin esto el visor asumía set 151 → las cartas de otros sets no resolvían foil.
                    setCode = card.id.printed.raw.let { if (it.startsWith("energy")) "energy" else it.substringBeforeLast('-') },
                    bottomBar = if (showActions && activePip != null) {
                        {
                            // Intents legales AHORA → distinguen habilidades manuales disponibles.
                            val legal = vm.legalIntents()
                            ActiveActions(
                                pip = activePip,
                                canRetreat = state.player.bench.isNotEmpty() &&
                                    activePip.attachedEnergyCount >= activePip.card.retreatCost.size,
                                onAttack = { name -> inspect = null; vm.onIntent(GameIntent.Attack(name)) },
                                onRetreat = { inspect = null; retreatMode = true },
                                onUseAbility = { ability ->
                                    inspect = null
                                    vm.onIntent(GameIntent.UseAbility(activePip.card.id, ability.name.es))
                                },
                                canUseAbility = { ability ->
                                    GameIntent.UseAbility(activePip.card.id, ability.name.es) in legal
                                },
                                abilityUsed = { activePip.card.id in state.abilitiesUsedThisTurn },
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }

        // Fantasma del Pokémon de Banca arrastrado hacia el Activo (retirada).
        benchDragCard?.let { c ->
            Box(
                Modifier
                    .offset { IntOffset(benchDragPos.x.roundToInt() - 42, benchDragPos.y.roundToInt() - 59) }
                    .size(84.dp, 118.dp),
            ) {
                CombatCard(imageUrl = c.artwork.small(true), modifier = Modifier.fillMaxSize())
            }
        }

        // Fantasma de la carta arrastrada siguiendo el dedo (coords de raíz).
        dragCard?.let { c ->
            Box(
                Modifier
                    .offset { IntOffset(dragPos.x.roundToInt() - 42, dragPos.y.roundToInt() - 59) }
                    .size(84.dp, 118.dp),
            ) {
                CombatCard(imageUrl = c.artwork.small(true), modifier = Modifier.fillMaxSize())
            }
        }

        // Capa de VUELO entre zonas (por encima del tablero, bajo el fantasma de arrastre).
        FlightOverlay(flightHost)

        // Estallido de ataque en el lente (efímero, ~1 s, color/motivo por tipo).
        lensFx?.let { spec -> LensAttackFx(spec) { lensFx = null } }

        // Fin de partida: fundido Motion.
        MotionContainer(
            visible = state.isOver,
            enter = MotionTransitions.overlayEnter(),
            exit = MotionTransitions.overlayExit(),
        ) {
            GameOverPanel(won = state.winner == Side.PLAYER, onExit = onExit)
        }
    }
}

/** Formatea segundos como mm:ss para el temporizador de las pestañas de estado. */
private fun mmss(sec: Int): String = "%02d:%02d".format(sec / 60, sec % 60)

/**
 * Zonas del tablero usadas como ANCLAS de vuelo. [faceUp] indica si la carta se ve de frente
 * al ATERRIZAR en esa zona (mano propia, descartes, activos y bancas) o de dorso (mazos,
 * premios, mano rival — información oculta).
 */
private enum class CardZone(val faceUp: Boolean) {
    ME_DECK(false), ME_HAND(true), ME_DISCARD(true), ME_PRIZES(false), ME_BENCH(true), ME_ACTIVE(true),
    OPP_DECK(false), OPP_HAND(false), OPP_DISCARD(true), OPP_PRIZES(false), OPP_BENCH(true), OPP_ACTIVE(true),
}

/** Caja del TAMAÑO DE UNA CARTA centrada en el centro de [base] (para zonas que son franjas
 *  anchas — mano, banca, abanico de premios — cuyo NBox completo haría volar cartas enormes). */
private fun centeredCard(base: BoardGeometry.NBox, wf: Float, hf: Float): BoardGeometry.NBox =
    BoardGeometry.NBox(base.cx - wf / 2f, base.cy - hf / 2f, wf, hf)

/**
 * NBox de referencia (del tamaño de una carta) de cada zona para la capa de VUELO. Las zonas
 * que son franjas (mano, banca, premios) se reducen a una carta centrada; el resto ya son
 * cajas del tamaño de su carta.
 */
private fun zoneNBox(z: CardZone): BoardGeometry.NBox = when (z) {
    CardZone.ME_DECK -> BoardGeometry.MeDeck
    CardZone.ME_HAND -> centeredCard(BoardGeometry.MeHand, 0.26f, 0.163f)
    CardZone.ME_DISCARD -> BoardGeometry.MeDiscard
    CardZone.ME_PRIZES -> centeredCard(BoardGeometry.MePrizes, 0.144f, 0.090f)
    CardZone.ME_BENCH -> centeredCard(BoardGeometry.MeBench, 0.178f, 0.112f)
    CardZone.ME_ACTIVE -> BoardGeometry.MeActive
    CardZone.OPP_DECK -> BoardGeometry.OppDeck
    CardZone.OPP_HAND -> centeredCard(BoardGeometry.OppHand, 0.130f, 0.081f)
    CardZone.OPP_DISCARD -> BoardGeometry.OppDiscard
    CardZone.OPP_PRIZES -> centeredCard(BoardGeometry.OppPrizes, 0.144f, 0.090f)
    CardZone.OPP_BENCH -> centeredCard(BoardGeometry.OppBench, 0.150f, 0.094f)
    CardZone.OPP_ACTIVE -> BoardGeometry.OppActive
}

/** Clasifica CADA carta del estado a su zona (para diffing entre estados sucesivos). */
private fun classifyZones(s: GameState): Map<CardId, CardZone> {
    val m = HashMap<CardId, CardZone>()
    s.player.let { p ->
        p.deck.forEach { m[it.id] = CardZone.ME_DECK }
        p.hand.forEach { m[it.id] = CardZone.ME_HAND }
        p.discard.forEach { m[it.id] = CardZone.ME_DISCARD }
        p.prizes.forEach { m[it.id] = CardZone.ME_PRIZES }
        p.bench.forEach { m[it.card.id] = CardZone.ME_BENCH }
        p.active?.let { m[it.card.id] = CardZone.ME_ACTIVE }
    }
    s.opponent.let { o ->
        o.deck.forEach { m[it.id] = CardZone.OPP_DECK }
        o.hand.forEach { m[it.id] = CardZone.OPP_HAND }
        o.discard.forEach { m[it.id] = CardZone.OPP_DISCARD }
        o.prizes.forEach { m[it.id] = CardZone.OPP_PRIZES }
        o.bench.forEach { m[it.card.id] = CardZone.OPP_BENCH }
        o.active?.let { m[it.card.id] = CardZone.OPP_ACTIVE }
    }
    return m
}

/** Índice CardId → Card sobre todas las zonas del estado (para dibujar la carta que vuela). */
private fun allCardsById(s: GameState): Map<CardId, Card> {
    val m = HashMap<CardId, Card>()
    listOf(s.player, s.opponent).forEach { ps ->
        (ps.deck + ps.hand + ps.discard + ps.prizes).forEach { m[it.id] = it }
        ps.bench.forEach { m[it.card.id] = it.card }
        ps.active?.let { m[it.card.id] = it.card }
    }
    return m
}

/** Contador que transiciona con un deslizamiento vertical (sube al subir, baja al bajar). */
@Composable
private fun AnimatedCount(
    count: Int,
    color: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
) {
    androidx.compose.animation.AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            } else {
                (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            }
        },
        label = "AnimatedCount",
    ) { value ->
        Text("$value", color = color, fontWeight = FontWeight.Black, fontSize = fontSize)
    }
}

/** Cuerpo del registro de batalla (lo envuelve MotionDialog, que aporta scrim y cierre). */
@Composable
private fun BattleLogBody(log: List<String>, onDismiss: () -> Unit) {
    run {
        Column(
            Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(CombatTheme.PanelCorner))
                .background(CombatTheme.Surface)
                .border(1.dp, CombatTheme.Border, RoundedCornerShape(CombatTheme.PanelCorner))
                .clickable {}
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Registro de batalla", color = CombatTheme.Gold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text("✕", color = CombatTheme.Muted, fontSize = 20.sp, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (log.isEmpty()) {
                    Text("Sin eventos todavía.", color = CombatTheme.Muted, fontSize = 12.sp)
                }
                log.asReversed().forEach { line ->
                    Text(line, color = CombatTheme.OnSurface, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Coloca un Composable en su caja normalizada [BoardGeometry.NBox] (offset + size). */
private fun Modifier.place(b: BoardGeometry.NBox, boardW: Dp, boardH: Dp): Modifier =
    this.offset(x = boardW * b.x, y = boardH * b.y).size(width = boardW * b.w, height = boardH * b.h)

/** ¿Es [pip] un objetivo válido para la carta [card] en curso (energía/evolución)? */
private fun isPlayTarget(card: Card?, pip: PokemonInPlay): Boolean = when (card) {
    is EnergyCard -> true
    is PokemonCard -> card.evolvesFrom != null && pip.turnsInPlay >= 1 &&
        (card.evolvesFrom == pip.card.name.en || card.evolvesFrom == pip.card.name.es)
    else -> false
}

/** Completa una jugada dirigida sobre [target]: energía → adjuntar, evolución → evolucionar. */
private fun completePlay(card: Card, target: PokemonInPlay?, vm: CombatSceneController) {
    if (target == null) return
    when (card) {
        is EnergyCard -> vm.onIntent(GameIntent.AttachEnergy(card.id, target.card.id))
        is PokemonCard -> if (isPlayTarget(card, target)) vm.onIntent(GameIntent.Evolve(card.id, target.card.id))
        else -> {}
    }
}

/**
 * Inicia la jugada de una carta de la mano. Devuelve la carta si necesita ELEGIR un
 * objetivo (energía / evolución); null si se jugó directamente (Básico→Banca, Entrenador)
 * o no es jugable. Los Entrenadores dirigidos (Poción) se juegan con [GameIntent.PlayTrainer]:
 * el motor abre entonces una decisión de objetivo que resuelve el DecisionPanel.
 */
private fun startPlay(card: Card, state: GameState, vm: CombatSceneController): Card? = when (card) {
    is EnergyCard -> card
    is PokemonCard -> when {
        card.isBasic -> { if (state.player.bench.size < 5) vm.onIntent(GameIntent.PlayBasicToBench(card.id)); null }
        card.evolvesFrom != null -> card
        else -> null
    }
    is TrainerCard -> { vm.onIntent(GameIntent.PlayTrainer(card.id)); null }
}

// -------------------------------------------------------------- Zonas colocadas

/** Un Pokémon en su caja: la carta LLENA la caja + HP/energía/estados superpuestos. */
@Composable
private fun FieldCard(
    pip: PokemonInPlay?,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
    selected: Boolean = false,
    highlighted: Boolean = false,
    onTap: (() -> Unit)? = null,
    draggable: Boolean = false,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier
            .fillMaxSize()
            // Actor visual: por defecto identidad (el juego no provee controller → sin cambios). Una
            // animación puede tomar el control TEMPORAL de ESTA MISMA carta (sin placeholder ni copia).
            .then(if (pip != null) Modifier.actorControlled(pip.card.id.raw) else Modifier)
            .onGloballyPositioned { coords = it }
            .then(if (onTap != null && pip != null) Modifier.clickable(onClick = onTap) else Modifier)
            .then(
                if (draggable && pip != null) Modifier.pointerInput(pip.card.id) {
                    detectDragGestures(
                        onDragStart = { local -> coords?.let { onDragStart(it.localToRoot(local)) } },
                        onDrag = { change, _ -> change.consume(); coords?.let { onDrag(it.localToRoot(change.position)) } },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                    )
                } else Modifier,
            ),
    ) {
        if (pip == null) {
            EmptySlot(Modifier.fillMaxSize(), highlighted = highlighted)
            return@Box
        }
        CombatCard(
            imageUrl = pip.card.artwork.small(true),
            faceDown = faceDown,
            selected = selected || highlighted,
            contentDescription = pip.card.name.es,
            card = if (faceDown) null else pip.card,
            // Entrada al entrar en juego (aterrizaje) + elevación física al resaltar/seleccionar.
            modifier = Modifier
                .fillMaxSize()
                .motionAppear(pip.card.id)
                .motionElevate(selected || highlighted),
        )
        if (!faceDown) {
            HpPill(pip.remainingHp, pip.card.hp, Modifier.align(Alignment.TopEnd).padding(2.dp))
            if (pip.attachedEnergy.isNotEmpty()) {
                EnergyBadge(pip.attachedEnergy.size, Modifier.align(Alignment.BottomEnd).padding(2.dp))
            }
            if (pip.statuses.isNotEmpty()) {
                StatusRow(pip.statuses, Modifier.align(Alignment.BottomStart).padding(2.dp))
            }
        }
    }
}

/** Carta simple (estadio) que llena su caja. */
@Composable
private fun FieldCardImage(url: String?, cd: String?, onTap: () -> Unit) {
    CombatCard(url, contentDescription = cd, modifier = Modifier.fillMaxSize().clickable(onClick = onTap))
}

/** Píldora de PS pequeña sobre la carta. */
@Composable
private fun HpPill(remaining: Int, max: Int, modifier: Modifier = Modifier) {
    val frac = if (max <= 0) 0f else remaining.toFloat() / max
    val accent = when {
        frac > 0.5f -> CombatTheme.Good
        frac > 0.2f -> CombatTheme.Gold
        else -> CombatTheme.Foe
    }
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xF2101319))
            .border(1.dp, accent, RoundedCornerShape(50))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        AnimatedCount(remaining, color = Color.White, fontSize = 10.sp)
    }
}

/**
 * PREMIOS (estilo TCG Live): 6 dorsos VERTICALES de tamaño real (ancho de la caja → alto por
 * CardAspect, NUNCA deformados), solapados con la base abajo y asomando hacia arriba, llenando
 * el alto del slot. Posiciones FIJAS (el paso no depende del conteo): al retirar un premio, las
 * restantes NO se mueven. El frontal (i=0) queda encima.
 */
@Composable
private fun PrizeFan(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) {
        Box(modifier.fillMaxSize())
        return
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val cardW = maxWidth
        val cardH = maxHeight
        val pCardH = cardW / CombatTheme.CardAspect
        val total = 6
        val n = count.coerceIn(1, total)
        val step = ((cardH - pCardH) / (total - 1)).coerceAtLeast(pCardH * 0.08f)
        Box(Modifier.fillMaxSize()) {
            for (i in (n - 1) downTo 0) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = -step * i)
                        .width(cardW)
                        .height(pCardH)
                        .clip(RoundedCornerShape(CombatTheme.CardCorner)),
                ) { CardBack(Modifier.fillMaxSize()) }
            }
        }
    }
}

/** Pila (mazo/descarte/premios): dorso o carta superior + contador. */
@Composable
private fun PileStack(count: Int, modifier: Modifier = Modifier, faceDown: Boolean, topCard: Card? = null) {
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(CombatTheme.CardCorner))) {
            if (!faceDown && topCard != null) {
                AsyncImage(
                    model = topCard.artwork.small(true),
                    contentDescription = topCard.name.es,
                    contentScale = ContentScale.Crop,
                    // La carta que llega al descarte "aterriza" con la entrada Motion.
                    modifier = Modifier.fillMaxSize().motionAppear(topCard.id),
                )
            } else {
                CardBack(Modifier.fillMaxSize())
            }
        }
        if (count > 0) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                AnimatedCount(count, fontSize = 11.sp)
            }
        }
    }
}

/** Dorsos de la mano rival (hasta 7 visibles). */
@Composable
private fun OppHandBacks(count: Int) {
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy((-6).dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count.coerceAtMost(7)) {
            Box(
                Modifier
                    .fillMaxHeight(0.9f)
                    .aspectRatio(CombatTheme.CardAspect)
                    .clip(RoundedCornerShape(4.dp)),
            ) { CardBack(Modifier.fillMaxSize()) }
        }
    }
}

/** Mano del jugador: abanico dinámico (LazyRow con leve solape) dentro de su banda. */
@Composable
private fun HandRow(hand: List<Card>, onTap: (Card) -> Unit) {
    if (hand.isEmpty()) return
    LazyRow(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy((-14).dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        items(hand, key = { it.id.raw }) { card ->
            CombatCard(
                imageUrl = card.artwork.small(true),
                contentDescription = card.name.es,
                card = card,
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .aspectRatio(CombatTheme.CardAspect)
                    .padding(bottom = 4.dp)
                    .clickable { onTap(card) },
            )
        }
    }
}

// -------------------------------------------------------------- HUD átomos

@Composable
private fun PrizeChip(count: Int, accent: Color) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(9.dp))
            .background(CombatTheme.Surface.copy(alpha = 0.9f))
            .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("★ $count", color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

@Composable
private fun IconChip(symbol: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(CombatTheme.Surface.copy(alpha = 0.85f))
            .border(1.dp, CombatTheme.Border, RoundedCornerShape(10.dp))
            .motionPress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = accent, fontSize = 15.sp)
    }
}

@Composable
private fun BannerText(text: String) {
    Text(
        text,
        color = CombatTheme.OnSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE60F1420))
            .border(1.dp, CombatTheme.Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

// -------------------------------------------------------------- Overlays

/**
 * Acciones del Activo propio en el visor a detalle: filas de ATAQUE (panel achaflanado
 * naranja con coste de energía + nombre + descripción + daño, al estilo TCG Live pero con
 * arte propio) y fila de RETIRADA con su coste.
 */
@Composable
private fun ActiveActions(
    pip: PokemonInPlay,
    canRetreat: Boolean,
    onAttack: (String) -> Unit,
    onRetreat: () -> Unit,
    onUseAbility: (Ability) -> Unit,
    canUseAbility: (Ability) -> Boolean,
    abilityUsed: (Ability) -> Boolean,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xF20B0F18))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // TIPO del Pokémon y su ACABADO (holo) alimentan el nuevo sistema de paneles: el marco
        // plateado se dibuja, el interior toma el color del tipo y el holo del panel reutiliza
        // EXACTAMENTE el shader de la carta (mismo Finish). Ver ActionPanels.kt.
        val type = pip.card.types.firstOrNull()
        val finish = resolveFinish(pip.card.rarity)
        // Habilidades primero (como en la referencia): MANUAL si el motor la ofrece como intent
        // legal (o ya se usó este turno → panel manual con "USADO"); PASIVA en caso contrario.
        pip.card.abilities.forEach { ability ->
            val usable = canUseAbility(ability)
            val used = abilityUsed(ability)
            if (usable || used) {
                ManualAbilityPanel(ability, type, finish, enabled = usable, used = used, onUse = { onUseAbility(ability) })
            } else {
                PassiveAbilityPanel(ability, type, finish)
            }
        }
        pip.card.attacks.forEach { atk ->
            AttackPanel(
                attack = atk,
                type = type,
                finish = finish,
                enabled = true,
                used = false,
                // Texto de regla REAL impreso; en blanco si el ataque solo hace daño → sin descripción.
                description = atk.text.es,
                onUse = { onAttack(atk.name.es) },
            )
        }
        RetreatPanel(
            type = type,
            finish = finish,
            cost = pip.card.retreatCost,
            enabled = canRetreat,
            onRetreat = onRetreat,
        )
    }
}

@Composable
private fun AttackSheet(
    active: PokemonInPlay,
    onAttack: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(CombatTheme.Surface)
            .border(1.dp, CombatTheme.Border, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(active.card.name.es, color = CombatTheme.OnSurface, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Text("✕", color = CombatTheme.Muted, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onDismiss))
        }
        if (active.card.attacks.isEmpty()) {
            Text("Sin ataques.", color = CombatTheme.Muted, fontSize = 12.sp)
        }
        active.card.attacks.forEach { atk ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CombatTheme.Gold.copy(alpha = 0.9f))
                    .clickable { onAttack(atk.name.es) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(atk.name.es, color = Color(0xFF20232B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ZoneInspectorBody(title: String, cards: List<Card>, onCardTap: (Card) -> Unit, onDismiss: () -> Unit) {
    run {
        Column(
            Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(CombatTheme.PanelCorner))
                .background(CombatTheme.Surface)
                .border(1.dp, CombatTheme.Border, RoundedCornerShape(CombatTheme.PanelCorner))
                .clickable {}
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("$title · ${cards.size}", color = CombatTheme.Gold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text("✕", color = CombatTheme.Muted, fontSize = 20.sp, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { card ->
                            CombatCard(
                                imageUrl = card.artwork.large(true),
                                contentDescription = card.name.es,
                                card = card,
                                modifier = Modifier.weight(1f).aspectRatio(CombatTheme.CardAspect).clickable { onCardTap(card) },
                            )
                        }
                        repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardInspector(card: Card, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xE6000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = card.artwork.large(true),
            contentDescription = card.name.es,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(CombatTheme.CardAspect).clip(RoundedCornerShape(18.dp)),
        )
    }
}

@Composable
private fun GameOverPanel(won: Boolean, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                if (won) "¡VICTORIA!" else "DERROTA",
                color = if (won) CombatTheme.Gold else CombatTheme.Foe,
                fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 2.sp,
            )
            PillButton("Volver al inicio", enabled = true, accent = CombatTheme.Gold, onClick = onExit)
        }
    }
}

@Composable
private fun PillButton(label: String, enabled: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) accent else accent.copy(alpha = 0.3f))
            .motionPress(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, color = Color(0xFF20232B), fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

@Composable
private fun CombatLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Preparando combate…", color = CombatTheme.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}
