package com.mineralord.tcg.feature.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Target
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import com.mineralord.tcg.engine.rules.GameEngine
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.feature.game.board.BattleTheme
import com.mineralord.tcg.feature.game.board.BoardGeometry
import com.mineralord.tcg.feature.game.board.BattleLogOverlay
import com.mineralord.tcg.feature.game.board.CardDetailSheet
import com.mineralord.tcg.feature.game.board.CardHdViewer
import com.mineralord.tcg.feature.game.board.CountPile
import com.mineralord.tcg.feature.game.board.EndTurnHex
import com.mineralord.tcg.feature.game.board.FieldPokemon
import com.mineralord.tcg.feature.game.board.GameOverOverlay
import com.mineralord.tcg.feature.game.board.HandFan
import com.mineralord.tcg.feature.game.board.MatBackground
import com.mineralord.tcg.feature.game.board.PrizeBadge
import com.mineralord.tcg.feature.game.board.SheetAction
import com.mineralord.tcg.feature.game.board.TurnBanner
import com.mineralord.tcg.feature.game.anim.CoinFlipFx
import com.mineralord.tcg.feature.game.anim.FloatingDamage
import com.mineralord.tcg.feature.game.anim.FloatingNumber
import com.mineralord.tcg.feature.game.anim.FxCue
import com.mineralord.tcg.feature.game.anim.newFxId
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay

// Geometría del tablero centralizada en BoardGeometry (ver board/BoardGeometry.kt);
// el layout se posiciona por BoardGeometry.NBox (cajas normalizadas).

/** Selección actual del jugador (carta de mano o su Pokémon activo). */
private sealed interface Sel {
    data class Hand(val card: Card) : Sel
    data object Active : Sel
}

/**
 * Pantalla de combate con el look de Pokémon TCG Live: tapete diagonal,
 * activos enfrentados, banca curva, pilas laterales, riel de premios/temporizador,
 * botón hexagonal de fin de turno, mano en abanico y panel de carta.
 */
@Composable
fun GameScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    vm: GameController = viewModel<GameViewModel>(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val state = ui.state

    var sel by remember { mutableStateOf<Sel?>(null) }
    // Carta de la mano tocada durante la preparación (para elegir Activo/Banca).
    var setupSel by remember { mutableStateOf<Card?>(null) }
    // Carta que se está INSPECCIONANDO en HD a pantalla completa (tocar cualquier
    // carta del tablero/mano fuera de la preparación). Solo lectura; sin acciones.
    var inspect by remember { mutableStateOf<Card?>(null) }
    // Arrastre de una carta de la mano a un Pokémon propio: energía → adjuntar, o
    // evolución → evolucionar. `dragCard` es la carta arrastrada, `dragPos` la posición
    // del dedo (coords de raíz) y `targetBounds` los límites de cada Pokémon propio.
    var dragCard by remember { mutableStateOf<Card?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    val targetBounds = remember { mutableStateMapOf<CardId, Rect>() }
    // Límites del panel gris central (zona de soltado de Entrenadores).
    var centerBounds by remember { mutableStateOf<Rect?>(null) }
    // Límites de los slots de Banca VACÍOS (por índice), para soltar ahí un Básico.
    val benchSlotBounds = remember { mutableStateMapOf<Int, Rect>() }
    // Límites del slot de ACTIVO propio (aunque esté vacío), para soltar ahí un Básico
    // durante la preparación (arrastrar de la mano al puesto activo).
    var activeSlotBounds by remember { mutableStateOf<Rect?>(null) }
    // Pokémon propio cuyo panel de acción (ataques + energías) está abierto.
    var actionSheet by remember { mutableStateOf<PokemonInPlay?>(null) }
    // Carta candidata de una decisión que se está viendo A DETALLE (para seleccionarla ahí).
    var decisionDetail by remember { mutableStateOf<Card?>(null) }
    // Selección acumulada de la decisión pendiente (se reinicia con cada interacción nueva).
    val decisionSelected = remember(state?.interaction) { mutableStateListOf<CardId>() }
    var showLog by remember { mutableStateOf(false) }
    // Emparejamientos energía→Pokémon de Banca durante una decisión de Generador
    // Eléctrico (AttachFromRevealed). Se reinicia por cada decisión nueva.
    val attachPairs = remember(state?.interaction) { mutableStateListOf<Pair<CardId, CardId>>() }
    // Cartas recién buscadas y llevadas a la mano (Cinio/Super Ball…): se REVELAN en
    // un overlay para que veas qué agarraste (fiel al "enséñalos" de la carta).
    var searchReveal by remember { mutableStateOf<List<Card>?>(null) }
    // Posición del dedo (coords de raíz) mientras se ARRASTRA el Activo propio hacia la
    // Banca para retirarse; null si no se está arrastrando.
    var activeDragPos by remember { mutableStateOf<Offset?>(null) }
    // En el detalle del Activo: true cuando se pulsó "RETIRARSE" y se está eligiendo a
    // qué Pokémon de la Banca promover.
    var retreatPick by remember { mutableStateOf(false) }

    // ---- Revelado inicial: el rival voltea sus Pokémon y entran los premios ----
    val revealing = ui.revealing
    // 0 = boca abajo (dorso), 1 = boca arriba (arte). Se mantiene en 1 en juego normal.
    val revealFlip = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(revealing) {
        if (revealing) {
            revealFlip.snapTo(0f)
            delay(350)
            revealFlip.animateTo(1f, androidx.compose.animation.core.tween(520))
        } else {
            revealFlip.snapTo(1f)
        }
    }

    // ---- Estado de animaciones (FX) ----
    val floats = remember { mutableStateListOf<FloatingNumber>() }
    var playerShake by remember { mutableIntStateOf(0) }
    var oppShake by remember { mutableIntStateOf(0) }
    var playerLunge by remember { mutableIntStateOf(0) }
    var oppLunge by remember { mutableIntStateOf(0) }
    var playerKo by remember { mutableIntStateOf(0) }
    var oppKo by remember { mutableIntStateOf(0) }
    var coin by remember { mutableStateOf<Pair<Long, Boolean>?>(null) }
    LaunchedEffect(Unit) {
        vm.fx.collect { cue ->
            when (cue) {
                is FxCue.Attack -> if (cue.side == Side.PLAYER) playerLunge++ else oppLunge++
                is FxCue.Damage -> {
                    floats.add(FloatingNumber(newFxId(), cue.side, cue.amount, cue.weakness, cue.resistance))
                    if (cue.side == Side.PLAYER) playerShake++ else oppShake++
                }
                is FxCue.Heal -> floats.add(FloatingNumber(newFxId(), cue.side, cue.amount, weakness = false, resistance = false, heal = true))
                is FxCue.Knockout -> {
                    if (cue.side == Side.PLAYER) { playerShake++; playerKo++ } else { oppShake++; oppKo++ }
                }
                is FxCue.Coin -> coin = newFxId() to cue.heads
                is FxCue.Prize -> {} // TODO: animación de premio que viaja al stack
            }
        }
    }

    // Reloj tipo ajedrez por jugador (cosmético, sin penalización).
    var pClock by remember { mutableStateOf(1500) }
    var oClock by remember { mutableStateOf(1500) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val s = vm.ui.value.state ?: continue
            if (s.isOver) continue
            if (s.activeSide == Side.PLAYER) pClock = (pClock - 1).coerceAtLeast(0)
            else oClock = (oClock - 1).coerceAtLeast(0)
        }
    }

    // Banner de turno.
    var bannerVisible by remember { mutableStateOf(false) }
    var bannerMine by remember { mutableStateOf(true) }
    LaunchedEffect(state?.turn, state?.activeSide, ui.revealing, ui.setup != null) {
        val s = state ?: return@LaunchedEffect
        if (s.isOver || ui.revealing || ui.setup != null) return@LaunchedEffect
        bannerMine = s.activeSide == Side.PLAYER
        bannerVisible = true
        delay(1300)
        bannerVisible = false
    }

    Box(modifier.fillMaxSize().background(TcgColors.Navy)) {
        MatBackground()

        // ---- Lanzamiento de moneda inicial (sobre el tablero vacío) ----
        val coinFlip = ui.coinFlip
        if (coinFlip != null) {
            CoinFlipOverlay(coinFlip, onCall = vm::chooseCoin, onChooseFirst = vm::chooseFirst)
            return@Box
        }

        // ---- Reparto animado de la mano inicial (mazo -> abanico) ----
        val dealing = ui.dealing
        if (dealing != null) {
            DealOverlay(cards = dealing.hand, onComplete = vm::onDealComplete)
            return@Box
        }

        // ---- Preparación EN EL TABLERO: elegir Activo/Banca (mi Activo boca arriba,
        //      rival boca abajo, banner "Pulsa Listo…" + botón LISTO, sin premios aún).
        //      Ya no es un overlay oscuro: el tablero se dibuja con el estado provisional. ----
        val setup = ui.setup
        val inSetup = setup != null

        if (state == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Preparando combate…", color = TcgColors.Parchment)
            }
            return@Box
        }

        val myTurn = isMyTurn(state, ui) && !revealing && !inSetup
        val awaitingMine = !inSetup && state.interaction?.side == Side.PLAYER

        // Pokémon propio VÁLIDO bajo el dedo mientras se arrastra una carta → se ilumina
        // (glow estilo TCG Live) para indicar dónde caerá al soltar. Solo se resalta si
        // la jugada es legal (energía a cualquiera; evolución a su pre-evolución).
        val myInPlay = listOfNotNull(state.player.active) + state.player.bench
        // Resuelve el Pokémon EN JUEGO bajo el dedo iterando el estado actual (ids
        // vigentes), no el mapa de bounds: así una entrada OBSOLETA de una pre-evolución
        // ya evolucionada (p. ej. Flaaffy tras pasar a Ampharos, misma casilla y rect)
        // no eclipsa al Pokémon actual e impide adjuntarle energía/objetos.
        fun dropTargetUnder(dc: Card): CardId? = myInPlay.firstOrNull { pip ->
            isValidDrop(dc, pip, state.turn) && targetBounds[pip.card.id]?.contains(dragPos) == true
        }?.card?.id
        // Durante Generador Eléctrico (AttachFromRevealed), el arrastre de una Energía
        // revelada solo es válido sobre los Pokémon de Banca ELEGIBLES (tipo Rayo).
        val revealDecision = (state.interaction?.decision as? PendingDecision.AttachFromRevealed)
            ?.takeIf { state.interaction?.side == Side.PLAYER }
        fun revealTargetUnder(): CardId? = myInPlay.firstOrNull { pip ->
            pip.card.id in (revealDecision?.benchCandidates ?: emptyList()) &&
                targetBounds[pip.card.id]?.contains(dragPos) == true
        }?.card?.id
        // Objeto DIRIGIDO (Poción…) arrastrado desde la mano: sus destinos válidos son los
        // Pokémon propios según el ámbito de su efecto (activo/banca/todos) y, si el efecto
        // solo cura (onlyDamaged), únicamente los que tienen daño. Al soltar sobre uno se
        // juega el Objeto y se aplica el efecto en ese Pokémon.
        val draggingItemChoose = (dragCard as? TrainerCard)?.let { itemTargetChoose(it) }
        fun itemTargetUnder(choose: com.mineralord.tcg.engine.model.EffectOp.ChooseTarget): CardId? {
            val cands = when (choose.from) {
                Target.OWN_ACTIVE -> listOfNotNull(state.player.active)
                Target.OWN_BENCH -> state.player.bench
                else -> myInPlay                       // OWN_ALL
            }.filter { !choose.onlyDamaged || it.damage > 0 }
            return cands.firstOrNull { targetBounds[it.card.id]?.contains(dragPos) == true }?.card?.id
        }
        // HERRAMIENTA arrastrada: destinos válidos = Pokémon propios SIN Herramienta ya
        // anclada (máx. 1 por Pokémon). Al soltar sobre uno se ancla ahí.
        val draggingToolScope = (dragCard as? TrainerCard)?.let { toolAttachScope(it) }
        fun toolTargetUnder(): CardId? = myInPlay.firstOrNull {
            it.attachedTools.isEmpty() && targetBounds[it.card.id]?.contains(dragPos) == true
        }?.card?.id
        val dropHoverTarget: CardId? = dragCard?.let {
            when {
                revealDecision != null -> revealTargetUnder()
                draggingItemChoose != null -> itemTargetUnder(draggingItemChoose)
                draggingToolScope != null -> toolTargetUnder()
                else -> dropTargetUnder(it)
            }
        }
        // Pokémon de Banca bajo el dedo mientras arrastro el Activo para RETIRARME.
        fun retreatTargetUnder(pos: Offset): CardId? =
            state.player.bench.firstOrNull { targetBounds[it.card.id]?.contains(pos) == true }?.card?.id
        val retreatHover: CardId? = activeDragPos?.let { retreatTargetUnder(it) }
        // Entrenador arrastrado sobre el panel gris central → se ilumina para indicar que
        // al soltar se jugará (efecto + descarte). Los que apuntan a un Pokémon (Objetos
        // dirigidos como Poción, y Herramientas) NO se juegan en el panel central: solo
        // se aplican soltándolos sobre el Pokémon objetivo, así que se excluyen aquí.
        val draggingTrainer = dragCard.let {
            it is TrainerCard && (it.kind is TrainerKind.Supporter || it.kind is TrainerKind.Item) &&
                !cardTargetsPokemon(it)
        }
        val centerHot = draggingTrainer && centerBounds?.contains(dragPos) == true
        // Básico arrastrado desde la mano → se resaltan los slots de Banca VACÍOS y, al
        // soltarlo sobre uno, se coloca ahí. Solo fuera de la preparación (drag activo).
        val draggingBasic = dragCard.let { it is PokemonCard && it.isBasic }
        val benchOccupied = state.player.bench.size
        fun benchDropIndex(): Int? = (benchOccupied until BoardGeometry.MeBenchSlots.size)
            .firstOrNull { benchSlotBounds[it]?.contains(dragPos) == true }
        val benchHotIndex: Int? = if (draggingBasic) benchDropIndex() else null
        // En PREPARACIÓN: arrastrar un Básico de la mano al slot de Activo (aunque esté
        // vacío) lo pone como Activo; sobre un slot de Banca vacío, lo añade a la Banca.
        val setupActiveHot = inSetup && draggingBasic && activeSlotBounds?.contains(dragPos) == true

        // ---- Tablero proporcional 1:1 (posicionado por BoardGeometry.NBox) ----
        // Cada zona se coloca y dimensiona con su caja normalizada, anclada a un área
        // de tablero bloqueada a 1080:2400 (letterbox). Los testTags exponen los bounds
        // reales a uiautomator (`tools/` los mide y compara contra refspec/board_start.json).
        // Edge-to-edge como TCG Live: el tablero llena TODA la pantalla (sin insets de
        // barras), de modo que el área de tablero == lienzo de referencia 1080x2400.
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

            val canRetreat = myTurn && state.interaction == null &&
                state.player.bench.isNotEmpty() &&
                (state.player.active?.let { it.attachedEnergyCount >= it.card.retreatCost.size } ?: false)

            Box(Modifier.size(boardW, boardH).align(Alignment.Center)) {
                // --- Zona de soltado de ENTRENADORES: panel gris central (captura sus
                //     límites para el hit-test; se ilumina cuando un Entrenador lo sobrevuela). ---
                Box(
                    Modifier
                        .place(BoardGeometry.CenterPanel, boardW, boardH, "center_panel")
                        .onGloballyPositioned { c -> centerBounds = c.boundsInRoot() }
                        .dropGlow(centerHot),
                )

                // --- Barra de turno (réplica 1:1 de TCG Live): el arco curvo bajo el
                //     Activo de quien tiene el turno se ENCIENDE en amarillo; el del rival
                //     queda apagado. Geometría medida en referencias_live (ref_0040 jugador,
                //     ref_0052 rival). Se dibuja ANTES que cartas/HUD (queda por debajo).
                //     Oculto en preparación y en el revelado inicial. ---
                if (!inSetup && !revealing && !state.isOver) {
                    TurnArc(mine = state.activeSide == Side.PLAYER)
                }

                // --- Chrome superior ---
                Box(Modifier.place(BoardGeometry.TopAvatar, boardW, boardH, "top_avatar")) {
                    OpponentNameplate("Rival")
                }
                // Engranaje horneado en el mat: hit-target transparente (salida por ahora).
                Box(Modifier.place(BoardGeometry.TopSettings, boardW, boardH, "top_settings").clickable { onExit() })

                // --- Mano del rival (boca abajo) ---
                Box(
                    Modifier.place(BoardGeometry.OppHand, boardW, boardH, "opp_hand"),
                    contentAlignment = Alignment.Center,
                ) { OpponentHandFan(count = state.opponent.hand.size, boardH = boardH) }

                // --- Premios / mazo / descarte rival (los premios entran en el revelado) ---
                if (!inSetup) {
                    Box(
                        Modifier
                            .place(BoardGeometry.OppPrizes, boardW, boardH, "opp_prizes")
                            .graphicsLayer { alpha = revealFlip.value },
                        contentAlignment = Alignment.Center,
                    ) { com.mineralord.tcg.feature.game.board.PrizeStack(state.opponent.prizesRemaining, mine = false) }
                }
                // Descarte (arriba del slot) y mazo (abajo, sobre la pestaña). El contador del
                // mazo se dibuja aparte bajo el mazo; el descarte no lleva contador (como el azul).
                Box(
                    Modifier.place(BoardGeometry.OppDiscard, boardW, boardH, "opp_discard"),
                    contentAlignment = Alignment.Center,
                ) { com.mineralord.tcg.feature.game.board.DeckPile(state.opponent.discard.size, mine = false, "Descarte", topCard = state.opponent.discard.lastOrNull(), showTab = false) }
                Box(
                    Modifier.place(BoardGeometry.OppDeck, boardW, boardH, "opp_deck"),
                    contentAlignment = Alignment.Center,
                ) { com.mineralord.tcg.feature.game.board.DeckPile(state.opponent.deck.size, mine = false, "Mazo", showTab = false) }
                Box(
                    Modifier.place(BoardGeometry.OppDeckTab, boardW, boardH, "opp_deck_tab"),
                    contentAlignment = Alignment.Center,
                ) { com.mineralord.tcg.feature.game.board.NumericTab(state.opponent.deck.size, mine = false) }

                // --- Banca rival (boca abajo en preparación; se voltea en el revelado) ---
                if (revealing || inSetup) {
                    RevealBench(if (inSetup) 0f else revealFlip.value, state.opponent.bench, BoardGeometry.OppBenchSlots, boardW, boardH)
                } else {
                    BenchRow(
                        state.opponent.bench, BoardGeometry.OppBenchSlots, boardW, boardH, "opp_bench",
                        onTap = { id -> inspect = state.opponent.bench.firstOrNull { it.card.id == id }?.card },
                    )
                }

                // --- Activo rival (se voltea durante el revelado) ---
                Box(
                    Modifier.place(BoardGeometry.OppActive, boardW, boardH, "opp_active"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (revealing || inSetup) {
                        RevealActive(if (inSetup) 0f else revealFlip.value, state.opponent.active, boardH, BoardGeometry.OppActive)
                    } else {
                        ActiveInBox(
                            pip = state.opponent.active, boardH = boardH, box = BoardGeometry.OppActive,
                            mine = false, shakeTrigger = oppShake, lungeTrigger = oppLunge, koTrigger = oppKo,
                            onTap = { inspect = state.opponent.active?.card },
                        )
                    }
                }

                // --- Estadio (junto al lente central) ---
                Box(
                    Modifier.place(BoardGeometry.Stadium, boardW, boardH, "stadium"),
                    contentAlignment = Alignment.Center,
                ) { com.mineralord.tcg.feature.game.board.StadiumSlot(stadium = state.stadium) }

                // --- Activo jugador ---
                Box(
                    Modifier
                        .place(BoardGeometry.MeActive, boardW, boardH, "me_active")
                        .onGloballyPositioned { c ->
                            val r = c.boundsInRoot()
                            activeSlotBounds = r
                            state.player.active?.card?.id?.let { targetBounds[it] = r }
                        }
                        .dropGlow(
                            setupActiveHot ||
                                (dropHoverTarget != null && dropHoverTarget == state.player.active?.card?.id),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    ActiveInBox(
                        pip = state.player.active, boardH = boardH, box = BoardGeometry.MeActive,
                        mine = true, shakeTrigger = playerShake, lungeTrigger = playerLunge, koTrigger = playerKo,
                        onTap = {
                            when {
                                inSetup -> vm.clearActive()
                                // En mi turno y sin decisión pendiente: panel de acción
                                // (ataques + energías). Si no, inspección HD de solo lectura.
                                myTurn && state.interaction == null -> { retreatPick = false; actionSheet = state.player.active }
                                else -> inspect = state.player.active?.card
                            }
                        },
                        canRetreat = canRetreat,
                        onActiveDragStart = { activeDragPos = it },
                        onActiveDrag = { activeDragPos = it },
                        onActiveDragEnd = {
                            val target = activeDragPos?.let { retreatTargetUnder(it) }
                            if (target != null && canRetreat) vm.onIntent(GameIntent.Retreat(target))
                            activeDragPos = null
                        },
                    )
                }

                // --- Premios / mazo / descarte jugador (los premios entran en el revelado) ---
                if (!inSetup) {
                    Box(
                        Modifier
                            .place(BoardGeometry.MePrizes, boardW, boardH, "me_prizes")
                            .graphicsLayer { alpha = revealFlip.value },
                        contentAlignment = Alignment.Center,
                    ) { com.mineralord.tcg.feature.game.board.PrizeStack(state.player.prizesRemaining, mine = true) }
                }
                // Mazo (arriba del slot, sin pestaña interna) y descarte (debajo). El contador
                // del mazo se dibuja aparte, centrado sobre el "0" horneado del mat.
                Box(
                    Modifier.place(BoardGeometry.MeDeck, boardW, boardH, "me_deck"),
                    contentAlignment = Alignment.Center,
                ) { com.mineralord.tcg.feature.game.board.DeckPile(state.player.deck.size, mine = true, "Mazo", showTab = false) }
                Box(
                    Modifier.place(BoardGeometry.MeDiscard, boardW, boardH, "me_discard"),
                    contentAlignment = Alignment.Center,
                ) { com.mineralord.tcg.feature.game.board.DeckPile(state.player.discard.size, mine = true, "Descarte", topCard = state.player.discard.lastOrNull(), showTab = false) }
                Box(
                    Modifier.place(BoardGeometry.MeDeckTab, boardW, boardH, "me_deck_tab"),
                    contentAlignment = Alignment.Center,
                ) { com.mineralord.tcg.feature.game.board.NumericTab(state.player.deck.size, mine = true) }

                // --- Banca jugador (en preparación, tocar un Básico lo retira a la mano) ---
                BenchRow(
                    state.player.bench, BoardGeometry.MeBenchSlots, boardW, boardH, "me_bench",
                    onTap = if (inSetup) { id -> vm.toggleBench(id) }
                    else { id -> inspect = state.player.bench.firstOrNull { it.card.id == id }?.card },
                    onBounds = { id, rect -> targetBounds[id] = rect },
                    highlightId = dropHoverTarget ?: retreatHover,
                )
                // Slots de Banca VACÍOS: capturan sus límites (para soltar un Básico) y se
                // iluminan mientras se arrastra un Básico sobre ellos. También en la
                // preparación, para poder ARRASTRAR un Básico de la mano a la Banca.
                run {
                    for (i in benchOccupied until BoardGeometry.MeBenchSlots.size) {
                        val slot = BoardGeometry.MeBenchSlots[i]
                        Box(
                            Modifier
                                .place(slot, boardW, boardH, "me_bench_empty_$i")
                                .onGloballyPositioned { c -> benchSlotBounds[i] = c.boundsInRoot() }
                                .dropGlow(benchHotIndex == i),
                        )
                    }
                }

                // --- Mano (tamaño de carta proporcional al tablero, 1:1 con la referencia) ---
                val handCardH = boardH * BoardGeometry.HandCardHFrac
                val handCardW = handCardH * BoardGeometry.CardAspect
                Box(Modifier.place(BoardGeometry.MeHand, boardW, boardH, "me_hand")) {
                    HandFan(
                        cards = state.player.hand,
                        enabled = inSetup || (myTurn && state.interaction == null),
                        cardW = handCardW,
                        cardH = handCardH,
                        onSelect = {
                            if (inSetup) {
                                // Solo los Básicos son colocables; el resto se ignora en preparación.
                                if (it is PokemonCard && it.isBasic) setupSel = it
                            } else {
                                inspect = it
                            }
                        },
                        selectedId = if (inSetup) setupSel?.id else (sel as? Sel.Hand)?.card?.id,
                        // En PREPARACIÓN: solo Básicos, arrastrables al Activo o a la Banca.
                        // En juego: energías, Básicos (a la Banca), evoluciones (sobre su
                        // pre-evolución), Entrenadores al panel central y Herramientas a un Pokémon.
                        canDrag = {
                            if (inSetup) {
                                it is PokemonCard && it.isBasic
                            } else {
                                it is EnergyCard ||
                                    it is PokemonCard ||
                                    (it is TrainerCard && (it.kind is TrainerKind.Supporter ||
                                        it.kind is TrainerKind.Item || it.kind is TrainerKind.Tool))
                            }
                        },
                        onCardDragStart = { card, pos -> dragCard = card; dragPos = pos },
                        onCardDrag = { pos -> dragPos = pos },
                        onCardDragEnd = {
                            val card = dragCard
                            if (inSetup) {
                                // Preparación: soltar un Básico sobre el Activo lo pone de
                                // Activo; sobre un slot de Banca vacío, lo añade a la Banca.
                                if (card is PokemonCard && card.isBasic) {
                                    when {
                                        activeSlotBounds?.contains(dragPos) == true -> vm.chooseActive(card.id)
                                        benchDropIndex() != null -> vm.toggleBench(card.id)
                                    }
                                }
                            } else {
                                // Objeto DIRIGIDO (Poción…) soltado sobre un Pokémon propio
                                // válido: se juega la carta y se aplica su efecto en ese
                                // Pokémon en un solo gesto (sin panel de elección aparte).
                                val itemTarget = draggingItemChoose?.let { itemTargetUnder(it) }
                                // HERRAMIENTA soltada sobre un Pokémon propio sin Herramienta.
                                val toolTarget = if (draggingToolScope != null) toolTargetUnder() else null
                                if (card is TrainerCard && itemTarget != null) {
                                    vm.playItemOn(card.id, itemTarget)
                                } else if (card is TrainerCard && toolTarget != null) {
                                    vm.onIntent(GameIntent.AttachTool(card.id, toolTarget))
                                } else {
                                    // Objetivo Pokémon VÁLIDO bajo el dedo (energía/evolución),
                                    // resuelto contra el estado vigente (ignora bounds obsoletos).
                                    val target = card?.let { dropTargetUnder(it) }
                                    val onCenter = centerBounds?.contains(dragPos) == true
                                    val intent = when (card) {
                                        is EnergyCard -> target?.let { GameIntent.AttachEnergy(card.id, it) }
                                        is PokemonCard ->
                                            if (card.isBasic) benchDropIndex()?.let { GameIntent.PlayBasicToBench(card.id) }
                                            else target?.let { GameIntent.Evolve(card.id, it) }
                                        // Los que apuntan a un Pokémon (Objeto dirigido /
                                        // Herramienta) NO se juegan por el panel central.
                                        is TrainerCard ->
                                            if (onCenter && !cardTargetsPokemon(card)) GameIntent.PlayTrainer(card.id) else null
                                        else -> null
                                    }
                                    intent?.let { vm.onIntent(it) }
                                }
                            }
                            dragCard = null
                        },
                        onCardDragCancel = { dragCard = null },
                    )
                }

                // --- Riel derecho: premios + fin de turno + registro ---
                Box(
                    Modifier.place(BoardGeometry.RailPrizeOpp, boardW, boardH, "rail_prize_opp"),
                    contentAlignment = Alignment.Center,
                ) { PrizeBadge(state.opponent.prizesRemaining, fmt(oClock), mine = false) }
                if (!inSetup) {
                    Box(
                        Modifier.place(BoardGeometry.RailEndTurn, boardW, boardH, "rail_endturn"),
                        contentAlignment = Alignment.Center,
                    ) { EndTurnHex(enabled = myTurn && state.interaction == null) { vm.onIntent(GameIntent.EndTurn) } }
                }
                Box(
                    Modifier.place(BoardGeometry.RailPrizeMe, boardW, boardH, "rail_prize_me"),
                    contentAlignment = Alignment.Center,
                ) { PrizeBadge(state.player.prizesRemaining, fmt(pClock), mine = true) }
                Box(
                    Modifier.place(BoardGeometry.RailLog, boardW, boardH, "rail_log"),
                    contentAlignment = Alignment.Center,
                ) { LogButton { showLog = true } }

                // --- Riel izquierdo: botones ⊘/💬 horneados en el mat -> hit-targets ---
                Box(Modifier.place(BoardGeometry.LeftEmote, boardW, boardH, "left_emote").clickable { /* emote: pendiente */ })
                Box(Modifier.place(BoardGeometry.LeftCancel, boardW, boardH, "left_cancel").clickable { sel = null; setupSel = null })

                // --- Banner de preparación "Pulsa Listo…" + botón LISTO (on-board) ---
                if (inSetup) {
                    Box(
                        Modifier.place(BoardGeometry.SetupBanner, boardW, boardH, "setup_banner"),
                        contentAlignment = Alignment.Center,
                    ) { SetupPrompt(canConfirm = setup!!.canConfirm, onReady = vm::confirmSetup) }
                }
            }
        }

        // ---- Banner de turno ----
        Box(Modifier.align(Alignment.Center)) {
            TurnBanner(
                text = if (bannerMine) "TU TURNO" else "TURNO DEL RIVAL",
                visible = bannerVisible,
                mine = bannerMine,
            )
        }

        // ---- Preparación: hoja de colocación del Básico tocado (Activo / Banca) ----
        if (inSetup) {
            setupSel?.let { card ->
                val benched = setup!!.benchIds.size >= GameEngine.BENCH_LIMIT
                Box(Modifier.align(Alignment.BottomCenter)) {
                    CardDetailSheet(
                        imageUrl = card.artwork.large(true),
                        title = card.name.es,
                        info = (card as? PokemonCard)?.let { "PS ${it.hp}" },
                        actions = listOf(
                            SheetAction(
                                label = "Poner como Activo",
                                sublabel = null,
                                accent = Color(0xFF2E7D32),
                                enabled = true,
                            ) { vm.chooseActive(card.id); setupSel = null },
                            SheetAction(
                                label = if (benched) "Banca llena" else "Poner en Banca",
                                sublabel = null,
                                accent = Color(0xFF1565C0),
                                enabled = !benched,
                            ) { vm.toggleBench(card.id); setupSel = null },
                        ),
                        onDismiss = { setupSel = null },
                    )
                }
            }
        }

        // ---- Fantasma de la carta que se está arrastrando (sigue el dedo) ----
        dragCard?.let { card ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val gh = maxHeight * BoardGeometry.HandCardHFrac
                val gw = gh * BoardGeometry.CardAspect
                val density = androidx.compose.ui.platform.LocalDensity.current
                val gwPx = with(density) { gw.toPx() }
                val ghPx = with(density) { gh.toPx() }
                AsyncImage(
                    model = card.artwork.small(true),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .offset { IntOffset((dragPos.x - gwPx / 2f).toInt(), (dragPos.y - ghPx / 2f).toInt()) }
                        .size(gw, gh)
                        .graphicsLayer { alpha = 0.92f }
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
        }

        // ---- Panel de decisión pendiente ----
        if (awaitingMine) {
            val decision = state.interaction!!.decision
            if (decision is PendingDecision.CoinFlip) {
                // La Dominguera y similares: TIRA la moneda y DESPUÉS ocurre el efecto.
                CoinTossOverlay(prompt = decision.prompt.es) { vm.onResolve(emptyList()) }
            } else
            Box(Modifier.align(Alignment.BottomCenter)) {
                if (decision is PendingDecision.AttachFromRevealed) {
                    // Bandeja con arrastre de Energía. El ORIGEN depende de la carta: del
                    // DESCARTE (Mela / Canto Apasionado, fromDiscard) o del MAZO (Generador
                    // Eléctrico). Antes se buscaba SIEMPRE en el mazo → en Mela la bandeja
                    // salía vacía (nada que arrastrar) y la carta no hacía nada.
                    val source = if (decision.fromDiscard) state.player.discard else state.player.deck
                    val revealedCards = decision.revealed.mapNotNull { id ->
                        source.firstOrNull { it.id == id }
                    }
                    val screenHdp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
                    val trayCardH = (screenHdp * BoardGeometry.BenchCardHFrac).dp
                    RevealAttachTray(
                        decision = decision,
                        revealedCards = revealedCards,
                        pairs = attachPairs,
                        benchName = { id -> vm.cardName(id) },
                        cardW = trayCardH * BoardGeometry.CardAspect,
                        cardH = trayCardH,
                        onEnergyDragStart = { card, pos -> dragCard = card; dragPos = pos },
                        onEnergyDrag = { pos -> dragPos = pos },
                        onEnergyDragEnd = {
                            val energy = dragCard
                            val target = revealTargetUnder()
                            if (energy != null && target != null &&
                                attachPairs.size < decision.maxAttach &&
                                attachPairs.none { it.first == energy.id }
                            ) {
                                attachPairs.add(energy.id to target)
                            }
                            dragCard = null
                        },
                        onUnassign = { energyId -> attachPairs.removeAll { it.first == energyId } },
                        onDone = {
                            // Envía los pares intercalados [energía, destino, …].
                            vm.onResolve(attachPairs.flatMap { listOf(it.first, it.second) })
                        },
                    )
                } else {
                    DecisionPanel(
                        decision, vm, selected = decisionSelected,
                        onTapCandidate = { decisionDetail = it },
                        onConfirm = { ids ->
                            searchRevealFor(decision, ids, vm)?.let { searchReveal = it }
                            vm.onResolve(ids)
                        },
                    )
                }
            }
        }

        // ---- Detalle de una carta CANDIDATA de la decisión: carta grande + botón para
        //      SELECCIONARLA ahí mismo (bug: antes había una lupa y no se elegía en detalle). ----
        if (awaitingMine) decisionDetail?.let { card ->
            val decision = state.interaction!!.decision
            val count = decisionCount(decision)
            val id = card.id
            val isSel = id in decisionSelected
            CardDetailOverlay(imageUrl = card.artwork.large(true), onDismiss = { decisionDetail = null }) {
                DetailButton(
                    label = when {
                        isSel -> "Quitar de la selección"
                        count <= 1 -> "Elegir esta carta"
                        else -> "Añadir a la selección (${decisionSelected.size}/$count)"
                    },
                    enabled = isSel || decisionSelected.size < count,
                    accent = if (isSel) TcgColors.RedDark else Color(0xFF2E7D32),
                ) {
                    when {
                        isSel -> { decisionSelected.remove(id); decisionDetail = null }
                        count <= 1 -> {
                            decisionDetail = null
                            searchRevealFor(decision, listOf(id), vm)?.let { searchReveal = it }
                            vm.onResolve(listOf(id))
                        }
                        decisionSelected.size < count -> { decisionSelected.add(id); decisionDetail = null }
                    }
                }
            }
        }

        // ---- Detalle del Activo propio: carta grande + energías en esferas + ATAQUES
        //      seleccionables ahí mismo (bug: no se podían usar ataques). ----
        actionSheet?.let { pip ->
            val canAttackNow = myTurn && state.interaction == null && state.turn > 1
            CardDetailOverlay(imageUrl = pip.card.artwork.large(true), onDismiss = { actionSheet = null; retreatPick = false }) {
                EnergyOrbs(pip)
                if (state.turn == 1) {
                    Text(
                        "Quien empieza no puede atacar en su primer turno.",
                        color = TcgColors.Parchment.copy(alpha = 0.7f), fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                if (pip.card.attacks.isEmpty()) {
                    Text("Este Pokémon no tiene ataques.", color = TcgColors.Parchment.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                pip.card.attacks.forEach { atk ->
                    val canPay = canAttackNow && pip.attachedEnergyCount >= atk.convertedCost
                    AttackRow(atk, canPay) {
                        actionSheet = null
                        vm.onIntent(GameIntent.Attack(atk.name.es))
                    }
                }

                // ---- RETIRADA: botón + elección del Pokémon de Banca que pasa a Activo.
                //      Habilitado en mi turno, sin decisión pendiente, con energía suficiente
                //      para el coste de retirada y al menos un Pokémon en Banca. ----
                val retreatCost = pip.card.retreatCost.size
                val canRetreatNow = myTurn && state.interaction == null &&
                    state.player.bench.isNotEmpty() && pip.attachedEnergyCount >= retreatCost
                if (canRetreatNow) {
                    if (!retreatPick) {
                        DetailButton(
                            label = if (retreatCost == 0) "RETIRARSE" else "RETIRARSE (descarta $retreatCost energía)",
                            enabled = true,
                            accent = Color(0xFF1565C0),
                        ) { retreatPick = true }
                    } else {
                        Text(
                            "Elige el Pokémon de Banca que pasará a Activo:",
                            color = TcgColors.Parchment, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.player.bench.forEach { b ->
                                AsyncImage(
                                    model = b.card.artwork.small(true),
                                    contentDescription = b.card.name.es,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .aspectRatio(BoardGeometry.CardAspect)
                                        .clip(RoundedCornerShape(7.dp))
                                        .border(1.dp, BattleTheme.Gold.copy(alpha = 0.6f), RoundedCornerShape(7.dp))
                                        .clickable {
                                            actionSheet = null; retreatPick = false
                                            vm.onIntent(GameIntent.Retreat(b.card.id))
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- Inspección de carta en HD (solo lectura; tocar para cerrar). Va por
        //      encima de los paneles inferiores para poder ojear cualquier carta. ----
        inspect?.let { card ->
            CardHdViewer(imageUrl = card.artwork.large(true)) { inspect = null }
        }

        // ---- Revelado de cartas BUSCADAS y llevadas a la mano (Cinio/Super Ball…):
        //      muestra qué agarraste, como el "enséñalos" de la carta. Toca para cerrar. ----
        searchReveal?.let { cards ->
            SearchRevealOverlay(cards) { searchReveal = null }
        }

        // ---- Mensaje de error transitorio ----
        ui.message?.let {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 60.dp)) {
                Text(
                    it,
                    color = TcgColors.Parchment,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TcgColors.RedDark)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        // ---- Números de daño flotantes (anclados a la zona del activo de cada lado) ----
        floats.forEach { fn ->
            val align = if (fn.side == Side.PLAYER) Alignment.Center else Alignment.Center
            val yOffset = if (fn.side == Side.PLAYER) 70.dp else (-70).dp
            Box(Modifier.align(align).offset(y = yOffset)) {
                FloatingDamage(fn) { id -> floats.removeAll { it.id == id } }
            }
        }

        // ---- Moneda ----
        coin?.let { (id, heads) ->
            Box(Modifier.align(Alignment.Center)) {
                CoinFlipFx(heads = heads, id = id) { coin = null }
            }
        }

        if (showLog) BattleLogOverlay(ui.log, onClose = { showLog = false })
        if (state.isOver) GameOverOverlay(won = state.winner == Side.PLAYER, onExit = onExit)
    }
}

// --------------------------------------------------------------------- piezas

/**
 * Coloca y dimensiona un elemento por su caja normalizada [NBox] dentro del área de
 * tablero (origen arriba-izquierda) y lo etiqueta para medición por uiautomator.
 */
/**
 * ¿Es legal soltar [dragged] (arrastrada desde la mano) sobre [target] en juego?
 * Energía: siempre. Evolución (Pokémon no Básico): el objetivo debe ser su
 * pre-evolución (coincidencia de nombre ES/EN) y llevar al menos un turno en juego.
 * Réplica de la validación del motor (GameEngine.evolve) para el resalte y el soltar.
 */
private fun isValidDrop(dragged: Card, target: PokemonInPlay, turn: Int): Boolean = when (dragged) {
    is EnergyCard -> true
    is PokemonCard -> !dragged.isBasic &&
        (dragged.evolvesFrom == target.card.name.en || dragged.evolvesFrom == target.card.name.es) &&
        target.turnsInPlay >= 1 &&
        turn > 1   // regla oficial: no evolucionar en el primer turno de la partida
    else -> false
}

/**
 * Resalte (glow) de un objetivo válido mientras se arrastra una carta sobre él,
 * al estilo de TCG Live: un aura cian-blanca que PULSA, dibujada ENCIMA del
 * contenido (si no, la imagen de la carta la taparía). Halo exterior suave +
 * borde nítido brillante.
 */
private fun Modifier.dropGlow(on: Boolean): Modifier = composed {
    if (!on) this
    else {
        val transition = rememberInfiniteTransition(label = "glow")
        val pulse by transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
            label = "pulse",
        )
        this.drawWithContent {
            drawContent()
            val r = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            // Halo exterior difuso (varias pasadas para simular el desenfoque).
            drawRoundRect(Color(0xFF57D9FF).copy(alpha = 0.22f * pulse), cornerRadius = r, style = Stroke(16.dp.toPx()))
            drawRoundRect(Color(0xFF8CE6FF).copy(alpha = 0.30f * pulse), cornerRadius = r, style = Stroke(9.dp.toPx()))
            // Borde nítido brillante.
            drawRoundRect(Color(0xFFD6F6FF).copy(alpha = 0.55f + 0.45f * pulse), cornerRadius = r, style = Stroke(3.5.dp.toPx()))
        }
    }
}

/**
 * Barra de turno de TCG Live: el arco dorado curvo que rodea al Activo de quien está
 * jugando se ENCIENDE (amarillo brillante); el del otro lado no se dibuja. Es un tubo
 * grueso con caperuza redonda, halo exterior difuso y una línea de brillo superior,
 * con un pulso muy suave para el efecto "prendido". Geometría (fracciones del tablero)
 * MEDIDA sobre las referencias reales:
 *   - Jugador (∩, domo bajo el Activo): bordes y≈0.5635, pico central y≈0.5365.
 *   - Rival    (∪, valle sobre su Activo): bordes y≈0.2594, valle central y≈0.2894.
 */
@Composable
private fun TurnArc(mine: Boolean) {
    val transition = rememberInfiniteTransition(label = "turnarc")
    val pulse by transition.animateFloat(
        initialValue = 0.72f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "arcpulse",
    )
    val edgeY = if (mine) 0.5635f else 0.2594f
    val ctrlY = if (mine) 0.5365f else 0.2894f
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(0f, edgeY * h)
            quadraticBezierTo(w * 0.5f, ctrlY * h, w, edgeY * h)
        }
        val gold = Color(0xFFF6C43C)
        // Halo exterior difuso (varias pasadas anchas y tenues).
        drawPath(path, gold.copy(alpha = 0.16f * pulse), style = Stroke(width = h * 0.028f, cap = StrokeCap.Round))
        drawPath(path, gold.copy(alpha = 0.28f * pulse), style = Stroke(width = h * 0.018f, cap = StrokeCap.Round))
        // Cuerpo del tubo dorado.
        drawPath(path, gold.copy(alpha = 0.92f), style = Stroke(width = h * 0.010f, cap = StrokeCap.Round))
        // Línea de brillo superior (highlight glossy).
        val hi = Path().apply {
            moveTo(0f, edgeY * h - h * 0.002f)
            quadraticBezierTo(w * 0.5f, ctrlY * h - h * 0.002f, w, edgeY * h - h * 0.002f)
        }
        drawPath(hi, Color(0xFFFFF0B4).copy(alpha = 0.85f * pulse), style = Stroke(width = h * 0.0035f, cap = StrokeCap.Round))
    }
}

/**
 * Overlay de lanzamiento INTERACTIVO de moneda (La Dominguera y similares): velo
 * oscuro + moneda dorada + botón "¡LANZAR!". Al pulsar, la moneda gira y se resuelve
 * la decisión (`onFlip`); el resultado real y el robo los aplica el motor, y la
 * animación del giro/cara la muestra el FX de moneda (CoinFlipFx). Fiel a TCG Live:
 * primero tiras la moneda, DESPUÉS ocurre el efecto.
 */
@Composable
private fun CoinTossOverlay(prompt: String, onFlip: () -> Unit) {
    var flipping by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                prompt.uppercase(),
                color = TcgColors.Gold, fontWeight = FontWeight.Black, fontSize = 18.sp,
                letterSpacing = 1.sp, textAlign = TextAlign.Center,
            )
            val transition = rememberInfiniteTransition(label = "cointoss")
            val spin by transition.animateFloat(
                0f, 360f, infiniteRepeatable(tween(500), RepeatMode.Restart), label = "spin",
            )
            Box(
                Modifier
                    .size(120.dp)
                    .graphicsLayer { if (flipping) rotationX = spin }
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFFF6D670), Color(0xFFB98A2A)),
                        ),
                    )
                    .border(4.dp, Color(0xFFFFF0B4), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("★", color = Color(0xFF7A5A10), fontSize = 40.sp)
            }
            if (!flipping) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BattleTheme.Gold)
                        .clickable { flipping = true; onFlip() }
                        .padding(horizontal = 34.dp, vertical = 14.dp),
                ) {
                    Text("¡LANZAR!", color = Color(0xFF3A2A08), fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            } else {
                Text("Lanzando…", color = TcgColors.Parchment.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
    }
}

private fun Modifier.place(b: BoardGeometry.NBox, boardW: Dp, boardH: Dp, tag: String): Modifier =
    this.offset(x = boardW * b.x, y = boardH * b.y)
        .size(width = boardW * b.w, height = boardH * b.h)
        // contentDescription "tt:<tag>" -> uiautomator lo expone como content-desc con
        // los bounds reales de la caja; el arnes (`tools/`) mide por ese prefijo.
        .semantics { contentDescription = "tt:$tag" }

/**
 * Nombre del rival (DINÁMICO) dibujado como texto blanco, igual que el tablero real.
 * El engranaje y los botones de emote van horneados en el mat; aquí solo el nombre,
 * que cambia según el rival (IA o jugador en netplay).
 */
@Composable
private fun OpponentNameplate(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        Text(
            name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

/**
 * Banca de un lado colocada por su caja normalizada. Las cartas conservan su forma
 * (alto de la caja × relación de carta) y se reparten centradas en la fila.
 */
@Composable
private fun BenchRow(
    bench: List<PokemonInPlay>,
    slots: List<BoardGeometry.NBox>,
    boardW: Dp,
    boardH: Dp,
    tag: String,
    onTap: ((CardId) -> Unit)? = null,
    onBounds: ((CardId, Rect) -> Unit)? = null,
    highlightId: CardId? = null,
) {
    // Banca en PANAL (3+2): cada carta se ancla a su slot medido 1:1 (ver
    // BoardGeometry.MeBenchSlots/OppBenchSlots). Las cartas se colocan por índice.
    bench.take(slots.size).forEachIndexed { i, pip ->
        val slot = slots[i]
        Box(
            Modifier
                .place(slot, boardW, boardH, "${tag}_$i")
                .onGloballyPositioned { c -> onBounds?.invoke(pip.card.id, c.boundsInRoot()) }
                .dropGlow(highlightId == pip.card.id),
        ) {
            FieldPokemon(
                pip = pip,
                width = boardW * slot.w,
                height = boardH * slot.h,
                onClick = onTap?.let { tap -> { tap(pip.card.id) } },
            )
        }
    }
}

/** Mano del rival: reversos de carta solapados en abanico (solo el conteo importa).
 *  Tamaño proporcional al tablero (1:1 con la referencia). */
@Composable
private fun OpponentHandFan(count: Int, boardH: Dp) {
    val cardH = boardH * BoardGeometry.OppHandCardHFrac
    val cardW = cardH * BoardGeometry.CardAspect
    Row(
        Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(-(cardW * 0.42f), Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count.coerceIn(0, 9)) {
            com.mineralord.tcg.feature.game.board.CardBack(
                width = cardW,
                height = cardH,
                mine = false,
            )
        }
    }
}

/**
 * Pokémon activo dibujado dentro de su caja normalizada (centrado), con sus FX.
 * La carta conserva su forma (alto de la caja × relación de carta).
 */
@Composable
private fun ActiveInBox(
    pip: PokemonInPlay?,
    boardH: Dp,
    box: BoardGeometry.NBox,
    mine: Boolean,
    shakeTrigger: Int,
    lungeTrigger: Int,
    koTrigger: Int,
    onTap: () -> Unit = {},
    canRetreat: Boolean = false,
    onActiveDragStart: ((Offset) -> Unit)? = null,
    onActiveDrag: ((Offset) -> Unit)? = null,
    onActiveDragEnd: (() -> Unit)? = null,
) {
    // La carta encaja dentro del slot preservando su forma (letterbox interno).
    // boardW = boardH * Aspect (tablero bloqueado a 1080:2400).
    val boxW = boardH * BoardGeometry.Aspect * box.w
    val boxH = boardH * box.h
    val boxAspect = BoardGeometry.Aspect * (box.w / box.h) // ancho/alto de la caja
    val cardW: Dp
    val cardH: Dp
    if (boxAspect <= BoardGeometry.CardAspect) {
        cardW = boxW; cardH = boxW / BoardGeometry.CardAspect
    } else {
        cardH = boxH; cardW = boxH * BoardGeometry.CardAspect
    }
    val shakeX = com.mineralord.tcg.feature.game.anim.rememberShake(shakeTrigger)
    val lungeY = com.mineralord.tcg.feature.game.anim.rememberLunge(lungeTrigger, mine)
    val ko = com.mineralord.tcg.feature.game.anim.rememberKnockout(koTrigger)
    // Arrastrar el Activo propio (mi turno, con energía suficiente) sobre un Pokémon de
    // la Banca = RETIRARSE promoviendo a ESE. Reporta la posición del dedo en coords de
    // raíz para que GameScreen resuelva el objetivo de Banca y lo resalte.
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragging by remember { mutableStateOf(false) }
    FieldPokemon(
        pip = pip,
        width = cardW,
        height = cardH,
        modifier = Modifier
            .offset(x = shakeX.dp, y = lungeY.dp)
            .graphicsLayer {
                alpha = ko.alpha
                scaleX = ko.scale * (if (dragging) 1.06f else 1f)
                scaleY = ko.scale * (if (dragging) 1.06f else 1f)
                rotationZ = ko.rotation
            }
            .onGloballyPositioned { coords = it }
            .then(
                if (mine && canRetreat && onActiveDragStart != null) {
                    Modifier.pointerInput(canRetreat) {
                        detectDragGestures(
                            onDragStart = { local ->
                                dragging = true
                                coords?.let { onActiveDragStart.invoke(it.localToRoot(local)) }
                            },
                            onDrag = { ch, _ ->
                                coords?.let { onActiveDrag?.invoke(it.localToRoot(ch.position)) }
                                ch.consume()
                            },
                            onDragEnd = { dragging = false; onActiveDragEnd?.invoke() },
                            onDragCancel = { dragging = false; onActiveDragEnd?.invoke() },
                        )
                    }
                } else Modifier,
            ),
        // Ambos activos son tocables: el mío para acciones/inspección, el rival para
        // inspección en HD (solo lectura).
        onClick = onTap,
    )
}

/**
 * Carta que se voltea sobre el eje Y (dorso → arte) según [flip] (0 = boca abajo,
 * 1 = boca arriba). Base del revelado inicial del rival.
 */
@Composable
private fun FlipCard(flip: Float, width: Dp, height: Dp, face: @Composable () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    Box(
        Modifier
            .size(width, height)
            .graphicsLayer {
                rotationY = flip * 180f
                cameraDistance = 14f * density
            },
        contentAlignment = Alignment.Center,
    ) {
        if (flip <= 0.5f) {
            com.mineralord.tcg.feature.game.board.CardBack(width = width, height = height, mine = false)
        } else {
            // Contrarrota la cara para que no quede espejada tras cruzar los 90°.
            Box(Modifier.graphicsLayer { rotationY = 180f }) { face() }
        }
    }
}

/** Activo del rival con volteo, encajado en su caja igual que [ActiveInBox]. */
@Composable
private fun RevealActive(flip: Float, pip: PokemonInPlay?, boardH: Dp, box: BoardGeometry.NBox) {
    val boxW = boardH * BoardGeometry.Aspect * box.w
    val boxH = boardH * box.h
    val boxAspect = BoardGeometry.Aspect * (box.w / box.h)
    val cardW: Dp
    val cardH: Dp
    if (boxAspect <= BoardGeometry.CardAspect) {
        cardW = boxW; cardH = boxW / BoardGeometry.CardAspect
    } else {
        cardH = boxH; cardW = boxH * BoardGeometry.CardAspect
    }
    FlipCard(flip, cardW, cardH) { FieldPokemon(pip = pip, width = cardW, height = cardH) }
}

/** Banca del rival con volteo, con la misma disposición que [BenchRow]. */
@Composable
private fun RevealBench(
    flip: Float,
    bench: List<PokemonInPlay>,
    slots: List<BoardGeometry.NBox>,
    boardW: Dp,
    boardH: Dp,
) {
    bench.take(slots.size).forEachIndexed { i, pip ->
        val slot = slots[i]
        val cardW = boardW * slot.w
        val cardH = boardH * slot.h
        Box(Modifier.place(slot, boardW, boardH, "opp_bench_$i")) {
            FlipCard(flip, cardW, cardH) { FieldPokemon(pip = pip, width = cardW, height = cardH) }
        }
    }
}

/** Botón flotante circular con un emoji/símbolo (emote, cancelar). */
@Composable
private fun FloatIconButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xCC14233D))
            .border(1.5.dp, BattleTheme.Gold.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 16.sp, color = TcgColors.Parchment)
    }
}

/**
 * Banner de preparación EN EL TABLERO (réplica de TCG Live, ref_0030): pastilla
 * blanca "Pulsa Listo para continuar cuando tengas todo preparado." + botón
 * amarillo **LISTO** (habilitado en cuanto hay Activo).
 */
@Composable
private fun SetupPrompt(canConfirm: Boolean, onReady: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xF2F5F1E8))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                "Pulsa Listo para continuar cuando tengas todo preparado.",
                color = Color(0xFF2A2A2A),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (canConfirm) BattleTheme.Gold else BattleTheme.Gold.copy(alpha = 0.4f))
                .clickable(enabled = canConfirm, onClick = onReady)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Text("LISTO", color = Color(0xFF3A2A08), fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LogButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF14233D))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("🔍", fontSize = 15.sp)
    }
}

// ----------------------------------------------------------------- helpers UI

private fun isMyTurn(state: GameState, ui: GameUiState): Boolean =
    !state.isOver && state.activeSide == Side.PLAYER && !ui.aiThinking

private fun fmt(secs: Int): String = "%d:%02d".format(secs / 60, secs % 60)

private fun playLabel(card: Card): String = when (card) {
    is PokemonCard -> if (card.isBasic) "Poner en banca" else "Evolucionar"
    is EnergyCard -> "Unir energía al activo"
    is TrainerCard -> "Jugar entrenador"
}

private fun cardInfo(card: Card): String? = when (card) {
    is PokemonCard -> "PS ${card.hp}" + (card.evolvesFrom?.let { " · Evoluciona de $it" } ?: "")
    is TrainerCard -> card.text.es.take(80)
    is EnergyCard -> "Carta de energía"
}

private fun damageText(d: com.mineralord.tcg.engine.model.Damage): String = when (d) {
    is com.mineralord.tcg.engine.model.Damage.Fixed -> "${d.value} de daño"
    com.mineralord.tcg.engine.model.Damage.Variable -> "daño variable"
    com.mineralord.tcg.engine.model.Damage.None -> "sin daño"
}

/** Acción principal al tocar una carta de la mano. */
private fun handAction(state: GameState, card: Card): GameIntent? {
    val me = state.player
    return when (card) {
        is PokemonCard -> when {
            card.isBasic && me.bench.size < 5 -> GameIntent.PlayBasicToBench(card.id)
            card.evolvesFrom != null -> {
                val target = me.allInPlay.firstOrNull {
                    it.turnsInPlay >= 1 &&
                        (card.evolvesFrom == it.card.name.en || card.evolvesFrom == it.card.name.es)
                }
                target?.let { GameIntent.Evolve(card.id, it.card.id) }
            }
            else -> null
        }
        is EnergyCard -> me.active?.let { GameIntent.AttachEnergy(card.id, it.card.id) }
        is TrainerCard -> GameIntent.PlayTrainer(card.id)
    }
}

/**
 * Bandeja del Generador Eléctrico: muestra las 5 cartas reveladas del top del mazo.
 * Las Energía Rayo Básicas elegibles se ARRASTRAN a un Pokémon Rayo de la Banca
 * (reutiliza el sistema de arrastre/objetivo/glow del tablero). Hasta [maxAttach];
 * el botón "Listo" resuelve la decisión (baraja el resto).
 */
@Composable
private fun RevealAttachTray(
    decision: PendingDecision.AttachFromRevealed,
    revealedCards: List<Card>,
    pairs: List<Pair<CardId, CardId>>,
    benchName: (CardId) -> String,
    cardW: Dp,
    cardH: Dp,
    onEnergyDragStart: (Card, Offset) -> Unit,
    onEnergyDrag: (Offset) -> Unit,
    onEnergyDragEnd: () -> Unit,
    onUnassign: (CardId) -> Unit,
    onDone: () -> Unit,
) {
    val assigned = pairs.associate { it.first to it.second }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(TcgColors.RedDark)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "${decision.prompt.es}  (${pairs.size}/${decision.maxAttach})",
            color = TcgColors.Parchment, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        )
        Text(
            // El subtítulo se ADAPTA a la carta: del descarte (Mela / Canto Apasionado)
            // se une a CUALQUIER Pokémon tuyo; del mazo (Generador Eléctrico) a un
            // Pokémon Rayo elegible. Antes estaba fijo al texto del Generador y salía en
            // Mela ("Energía Rayo… de tu Banca"), que no corresponde a esa carta.
            if (decision.fromDiscard)
                "Arrastra la Energía del descarte a uno de tus Pokémon. Toca una asignada para quitarla."
            else
                "Arrastra las Energía Rayo a un Pokémon Rayo elegible. Toca una asignada para quitarla.",
            color = TcgColors.Parchment.copy(alpha = 0.7f), fontSize = 10.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            revealedCards.forEach { card ->
                val isEnergy = card.id in decision.energyCandidates
                val isAssigned = card.id in assigned
                val canDrag = isEnergy && !isAssigned && pairs.size < decision.maxAttach
                var coords by remember(card.id) { mutableStateOf<LayoutCoordinates?>(null) }
                Box(
                    Modifier
                        .size(cardW, cardH)
                        .clip(RoundedCornerShape(6.dp))
                        .graphicsLayer { alpha = if (isEnergy) 1f else 0.4f }
                        .onGloballyPositioned { coords = it }
                        .then(
                            if (canDrag) Modifier.pointerInput(card.id) {
                                detectVerticalDragGestures(
                                    onDragStart = { local -> coords?.let { onEnergyDragStart(card, it.localToRoot(local)) } },
                                    onVerticalDrag = { ch, _ -> coords?.let { onEnergyDrag(it.localToRoot(ch.position)) } },
                                    onDragEnd = { onEnergyDragEnd() },
                                    onDragCancel = { onEnergyDragEnd() },
                                )
                            } else if (isAssigned) Modifier.clickable { onUnassign(card.id) } else Modifier,
                        ),
                ) {
                    AsyncImage(
                        model = card.artwork.small(true),
                        contentDescription = card.name.es,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                    )
                    if (isAssigned) {
                        Box(
                            Modifier.fillMaxSize().background(Color(0xBB000000)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "✓\n${benchName(assigned.getValue(card.id))}",
                                color = TcgColors.Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TcgColors.Gold)
                .clickable(onClick = onDone)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (pairs.isEmpty()) "Listo (no unir nada)" else "Listo",
                color = Color(0xFF3A2A08), fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Si la decisión es una BÚSQUEDA que lleva cartas a la MANO, devuelve las cartas
 * elegidas [ids] (resueltas por el controlador) para revelarlas; si no, null.
 */
private fun searchRevealFor(decision: PendingDecision, ids: List<CardId>, vm: GameController): List<Card>? =
    (decision as? PendingDecision.SearchCards)
        ?.takeIf { it.destination == com.mineralord.tcg.engine.model.Zone.HAND }
        ?.let { ids.mapNotNull { id -> vm.card(id) }.takeIf { it.isNotEmpty() } }

/**
 * Overlay que REVELA las cartas recién buscadas (llevadas a la mano): sus artes en
 * fila sobre un velo oscuro, con un rótulo. Toca en cualquier parte para cerrar.
 * Réplica del "enséñalos" de TCG Live tras usar un buscador (Cinio/Super Ball…).
 */
@Composable
private fun SearchRevealOverlay(cards: List<Card>, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xE6000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (cards.size == 1) "AÑADISTE A TU MANO" else "AÑADISTE A TU MANO (${cards.size})",
                color = TcgColors.Gold, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                cards.take(4).forEach { card ->
                    AsyncImage(
                        model = card.artwork.large(true),
                        contentDescription = card.name.es,
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .width(140.dp)
                            .aspectRatio(BoardGeometry.CardAspect)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
            }
            Text(
                "Toca para cerrar",
                color = TcgColors.Parchment.copy(alpha = 0.6f), fontSize = 11.sp,
            )
        }
    }
}

/** Nº de cartas que puede elegir una decisión (1 si no aplica). */
private fun decisionCount(decision: PendingDecision): Int = when (decision) {
    is PendingDecision.ChooseTargets -> decision.count
    is PendingDecision.SearchCards -> decision.count
    is PendingDecision.MoveEnergy -> decision.count
    is PendingDecision.AttachFromRevealed -> decision.maxAttach
    is PendingDecision.CoinFlip -> 0
}

/**
 * Panel de decisión (buscar en mazo, elegir objetivo de Banca, mover energía): muestra
 * las cartas candidatas como IMÁGENES (no nombres). Tocar una carta la abre A DETALLE
 * (`onTapCandidate`) para verla grande y SELECCIONARLA ahí. Las ya seleccionadas se
 * marcan (marco dorado + ✓). El botón CONFIRMAR resuelve con lo seleccionado.
 */
@Composable
private fun DecisionPanel(
    decision: PendingDecision,
    vm: GameController,
    selected: List<CardId>,
    onTapCandidate: (Card) -> Unit,
    onConfirm: (List<CardId>) -> Unit,
) {
    val (prompt, candidates, count) = when (decision) {
        is PendingDecision.ChooseTargets -> Triple(decision.prompt.es, decision.candidates, decision.count)
        is PendingDecision.SearchCards -> Triple(decision.prompt.es, decision.candidates, decision.count)
        is PendingDecision.MoveEnergy ->
            Triple(decision.prompt.es, decision.fromCandidates + decision.toCandidates, decision.count)
        // AttachFromRevealed se dibuja en RevealAttachTray; CoinFlip en CoinTossOverlay.
        is PendingDecision.AttachFromRevealed -> Triple(decision.prompt.es, emptyList(), 0)
        is PendingDecision.CoinFlip -> Triple(decision.prompt.es, emptyList<CardId>(), 0)
    }
    val screenHdp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val cardH = (screenHdp * BoardGeometry.BenchCardHFrac * 1.35f).dp
    val cardW = cardH * BoardGeometry.CardAspect

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(TcgColors.RedDark)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(prompt, color = TcgColors.Parchment, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Toca una carta para verla a detalle y seleccionarla.",
            color = TcgColors.Parchment.copy(alpha = 0.7f), fontSize = 10.sp,
        )
        if (candidates.isEmpty()) {
            Text("No hay cartas disponibles.", color = TcgColors.Parchment.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(candidates) { id ->
                val card = vm.card(id)
                val isSel = id in selected
                Box(
                    Modifier
                        .size(cardW, cardH)
                        .clip(RoundedCornerShape(7.dp))
                        .border(
                            if (isSel) 3.dp else 1.dp,
                            if (isSel) TcgColors.Gold else BattleTheme.Gold.copy(alpha = 0.5f),
                            RoundedCornerShape(7.dp),
                        )
                        .clickable { card?.let(onTapCandidate) },
                ) {
                    if (card != null) {
                        AsyncImage(
                            model = card.artwork.small(true),
                            contentDescription = card.name.es,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)),
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(Color(0xFF14233D)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                vm.cardName(id), color = TcgColors.Parchment,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(2.dp),
                            )
                        }
                    }
                    if (isSel) {
                        Box(
                            Modifier.align(Alignment.BottomStart).padding(2.dp).size(18.dp)
                                .clip(CircleShape).background(TcgColors.Gold),
                            contentAlignment = Alignment.Center,
                        ) { Text("✓", color = Color(0xFF3A2A08), fontSize = 11.sp, fontWeight = FontWeight.Black) }
                    }
                }
            }
        }
        if (count > 1) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected.isNotEmpty()) TcgColors.Gold else TcgColors.GoldDark.copy(alpha = 0.4f))
                    .clickable(enabled = selected.isNotEmpty()) { onConfirm(selected.toList()) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Confirmar (${selected.size}/$count)", color = Color(0xFF3A2A08), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Overlay de CARTA A DETALLE a pantalla completa: la carta grande centrada sobre un
 * velo oscuro y, debajo, un [panel] de acciones (seleccionar, o lista de ataques con
 * energías). Tocar el fondo cierra; los controles del panel consumen su toque.
 */
@Composable
private fun CardDetailOverlay(
    imageUrl: String,
    onDismiss: () -> Unit,
    panel: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color(0xF2000000)).clickable(onClick = onDismiss),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .padding(top = 14.dp, bottom = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Carta GRANDE pegada al borde superior para dejar sitio abajo a las acciones.
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(BoardGeometry.CardAspect)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = panel,
            )
        }
    }
}

/** Botón de acción a todo el ancho (seleccionar carta en el detalle de decisión). */
@Composable
private fun DetailButton(label: String, enabled: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) accent else accent.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** Fila de un ataque: coste (esferas), nombre y daño; atenuada si no es pagable. */
@Composable
private fun AttackRow(atk: com.mineralord.tcg.engine.model.Attack, enabled: Boolean, onClick: () -> Unit) {
    val accent = if (enabled) Color(0xFF2E7D32) else Color(0xFF2E7D32).copy(alpha = 0.3f)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(accent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            atk.cost.forEach { t -> EnergyOrb(t) }
        }
        Text(atk.name.es, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
        val dmg = (atk.baseDamage as? com.mineralord.tcg.engine.model.Damage.Fixed)?.value
        if (dmg != null && dmg > 0) {
            Text("$dmg", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

/** Esferas de las energías adjuntas a un Pokémon (una por energía, coloreadas por tipo). */
@Composable
private fun EnergyOrbs(pip: PokemonInPlay) {
    if (pip.attachedEnergy.isEmpty()) {
        Text("Sin energías", color = TcgColors.Parchment.copy(alpha = 0.5f), fontSize = 9.sp)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        pip.attachedEnergy.forEach { e ->
            EnergyOrb((e as? com.mineralord.tcg.engine.model.BasicEnergy)?.type)
        }
    }
}

/** Una esfera de energía oficial (PNG) por su tipo. */
@Composable
private fun EnergyOrb(type: com.mineralord.tcg.engine.model.EnergyType?) {
    com.mineralord.tcg.core.designsystem.EnergySphere(type = type, size = 16.dp)
}
