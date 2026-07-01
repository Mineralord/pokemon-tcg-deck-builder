package com.mineralord.tcg.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.feature.game.board.BattleTheme
import com.mineralord.tcg.feature.game.board.BattleLogOverlay
import com.mineralord.tcg.feature.game.board.CardDetailSheet
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

private val ActiveW = 86.dp
private val ActiveH = 120.dp
private val BenchW = 60.dp
private val BenchH = 84.dp

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
    vm: GameViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val state = ui.state

    var sel by remember { mutableStateOf<Sel?>(null) }
    var showLog by remember { mutableStateOf(false) }

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
    LaunchedEffect(state?.turn, state?.activeSide) {
        val s = state ?: return@LaunchedEffect
        if (s.isOver) return@LaunchedEffect
        bannerMine = s.activeSide == Side.PLAYER
        bannerVisible = true
        delay(1300)
        bannerVisible = false
    }

    Box(modifier.fillMaxSize().background(TcgColors.Navy)) {
        MatBackground()

        if (state == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Preparando combate…", color = TcgColors.Parchment)
            }
            return@Box
        }

        val myTurn = isMyTurn(state, ui)
        val awaitingMine = state.interaction?.side == Side.PLAYER

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(end = 40.dp), // reserva la franja del rail derecho (premios/timer)
        ) {
            TopBar(opponentName = "Rival", onExit = onExit, onSettings = {})

            // ---- Mano del rival (boca abajo, abanico arriba) ----
            OpponentHandFan(count = state.opponent.hand.size)

            // ---- Zona del rival: [Premios | Banca | Mazo/Descarte] ----
            ZoneRow(player = state.opponent, mine = false)

            // ---- Activo del rival (en el carril, junto al divisor) ----
            ActiveCard(
                pip = state.opponent.active,
                mine = false,
                shakeTrigger = oppShake,
                lungeTrigger = oppLunge,
                koTrigger = oppKo,
            )

            // ---- Centro (lente dorado del tapete) ----
            Spacer(Modifier.weight(1f))

            // ---- Activo del jugador (en el carril, junto al divisor) ----
            val canRetreat = myTurn && state.interaction == null &&
                state.player.bench.isNotEmpty() &&
                (state.player.active?.let { it.attachedEnergyCount >= it.card.retreatCost.size } ?: false)
            ActiveCard(
                pip = state.player.active,
                mine = true,
                shakeTrigger = playerShake,
                lungeTrigger = playerLunge,
                koTrigger = playerKo,
                onTap = { if (myTurn) sel = Sel.Active },
                canRetreat = canRetreat,
                onRetreat = {
                    state.player.bench.firstOrNull()?.let {
                        vm.onIntent(GameIntent.Retreat(it.card.id))
                    }
                },
            )

            // ---- Zona del jugador: [Premios | Banca | Mazo/Descarte] ----
            ZoneRow(player = state.player, mine = true)

            // ---- Mano ----
            HandFan(
                cards = state.player.hand,
                enabled = myTurn && state.interaction == null,
                onSelect = { sel = Sel.Hand(it) },
                selectedId = (sel as? Sel.Hand)?.card?.id,
            )
        }

        // ---- Riel derecho: premios + temporizador + registro + fin de turno ----
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrizeBadge(state.opponent.prizesRemaining, fmt(oClock))
            Spacer(Modifier.height(6.dp))
            LogButton { showLog = true }
            Spacer(Modifier.height(6.dp))
            PrizeBadge(state.player.prizesRemaining, fmt(pClock))
            Spacer(Modifier.height(8.dp))
            EndTurnHex(enabled = myTurn && state.interaction == null) { vm.onIntent(GameIntent.EndTurn) }
        }

        // ---- Banner de turno ----
        Box(Modifier.align(Alignment.Center)) {
            TurnBanner(
                text = if (bannerMine) "TU TURNO" else "TURNO DEL RIVAL",
                visible = bannerVisible,
                mine = bannerMine,
            )
        }

        // ---- Panel de carta (selección) ----
        if (sel != null && !awaitingMine && !state.isOver) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                CardDetailFor(state, sel!!, vm) { sel = null }
            }
        }

        // ---- Panel de decisión pendiente ----
        if (awaitingMine) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                DecisionPanel(state.interaction!!.decision, vm)
            }
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

@Composable
private fun TopBar(opponentName: String, onExit: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "‹",
            color = TcgColors.Parchment,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            modifier = Modifier.clickable(onClick = onExit).padding(end = 10.dp),
        )
        Box(Modifier.size(26.dp).clip(CircleShape).background(TcgColors.RedDark), contentAlignment = Alignment.Center) {
            Text(opponentName.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(opponentName, color = TcgColors.Parchment, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text("⚙", color = TcgColors.Parchment, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onSettings))
    }
}

/**
 * Fila de zona de un lado, como en TCG Live: [Premios | Banca | Mazo/Descarte].
 * El Pokémon activo NO va aquí: se dibuja aparte, en el carril central.
 */
@Composable
private fun ZoneRow(player: PlayerState, mine: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PrizesPanelTall(player.prizesRemaining, mine)
        BenchPanel(player, mine, Modifier.weight(1f))
        DeckDiscardColumn(player)
    }
}

/** Panel de premios (extremo izquierdo), con su racimo de hexágonos. */
@Composable
private fun PrizesPanelTall(remaining: Int, mine: Boolean) {
    Box(
        Modifier
            .width(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background((if (mine) BattleTheme.MineTop else BattleTheme.OppTop).copy(alpha = 0.5f))
            .border(1.5.dp, BattleTheme.Gold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        com.mineralord.tcg.feature.game.board.PrizeCluster(remaining, mine)
    }
}

/** Panel de banca (centro), con las cartas de banca (hasta 5, en filas de 3). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BenchPanel(player: PlayerState, mine: Boolean, modifier: Modifier = Modifier) {
    val bg = (if (mine) BattleTheme.MineTop else BattleTheme.OppTop).copy(alpha = 0.5f)
    Box(
        modifier
            .heightIn(min = BenchH + 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(2.dp, BattleTheme.Gold.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 3,
        ) {
            player.bench.forEach { pip ->
                FieldPokemon(pip = pip, width = BenchW, height = BenchH)
            }
        }
    }
}

/** Mazo + descarte como pilas (extremo derecho). */
@Composable
private fun DeckDiscardColumn(player: PlayerState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CountPile("Mazo", player.deck.size, BattleTheme.Gold.copy(alpha = 0.55f))
        CountPile("Descarte", player.discard.size, BattleTheme.CenterBand)
    }
}

/** Mano del rival: cartas boca abajo en abanico (solo el conteo importa). */
@Composable
private fun OpponentHandFan(count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy((-12).dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count.coerceIn(0, 9)) {
            Box(
                Modifier
                    .size(28.dp, 40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BattleTheme.OppBottom)
                    .border(1.dp, BattleTheme.Gold.copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
            )
        }
    }
}

/** Pokémon activo dibujado en el carril central (centrado), con sus FX. */
@Composable
private fun ActiveCard(
    pip: PokemonInPlay?,
    mine: Boolean,
    shakeTrigger: Int,
    lungeTrigger: Int,
    koTrigger: Int,
    onTap: () -> Unit = {},
    canRetreat: Boolean = false,
    onRetreat: () -> Unit = {},
) {
    val shakeX = com.mineralord.tcg.feature.game.anim.rememberShake(shakeTrigger)
    val lungeY = com.mineralord.tcg.feature.game.anim.rememberLunge(lungeTrigger, mine)
    val ko = com.mineralord.tcg.feature.game.anim.rememberKnockout(koTrigger)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val retreatThresholdPx = with(density) { 56.dp.toPx() }
    var dragY by remember { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxWidth().padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
        FieldPokemon(
            pip = pip,
            width = ActiveW,
            height = ActiveH,
            modifier = Modifier
                .offset(x = shakeX.dp, y = lungeY.dp)
                .offset { androidx.compose.ui.unit.IntOffset(0, dragY.toInt()) }
                .graphicsLayer {
                    alpha = ko.alpha
                    scaleX = ko.scale
                    scaleY = ko.scale
                    rotationZ = ko.rotation
                }
                .then(
                    if (mine && canRetreat) {
                        Modifier.pointerInput(canRetreat) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragY >= retreatThresholdPx) onRetreat()
                                    dragY = 0f
                                },
                                onDragCancel = { dragY = 0f },
                            ) { _, delta -> dragY = (dragY + delta).coerceIn(0f, retreatThresholdPx * 1.6f) }
                        }
                    } else Modifier,
                ),
            onClick = if (mine) onTap else null,
        )
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

/** Construye el panel de carta según la selección y dispara los intents. */
@Composable
private fun BoxScope.CardDetailFor(state: GameState, sel: Sel, vm: GameViewModel, onDismiss: () -> Unit) {
    when (sel) {
        is Sel.Hand -> {
            val card = sel.card
            val intent = handAction(state, card)
            val actions = listOfNotNull(
                intent?.let {
                    SheetAction(
                        label = playLabel(card),
                        sublabel = null,
                        accent = Color(0xFF2E7D32),
                        enabled = true,
                    ) { vm.onIntent(it); onDismiss() }
                },
            )
            CardDetailSheet(
                imageUrl = card.artwork.large(true),
                title = card.name.es,
                info = cardInfo(card),
                actions = actions,
                onDismiss = onDismiss,
            )
        }
        Sel.Active -> {
            val pip = state.player.active ?: run { onDismiss(); return }
            val card = pip.card
            val actions = buildList {
                card.attacks.forEach { atk ->
                    add(
                        SheetAction(
                            label = "⚔ ${atk.name.es}",
                            sublabel = "Coste ${atk.convertedCost} · " + damageText(atk.baseDamage),
                            accent = TcgColors.RedDark,
                            enabled = pip.attachedEnergyCount >= atk.convertedCost,
                        ) { vm.onIntent(GameIntent.Attack(atk.name.es)); onDismiss() }
                    )
                }
                card.abilities.forEach { ab ->
                    add(
                        SheetAction(
                            label = "✦ ${ab.name.es}",
                            sublabel = ab.text.es.take(60),
                            accent = Color(0xFF6A1B9A),
                            enabled = true,
                        ) { vm.onIntent(GameIntent.UseAbility(card.id, ab.name.es)); onDismiss() }
                    )
                }
                if (state.player.bench.isNotEmpty() && pip.attachedEnergyCount >= card.retreatCost.size) {
                    add(
                        SheetAction(
                            label = "↩ Retirarse",
                            sublabel = "Coste de retirada ${card.retreatCost.size}",
                            accent = Color(0xFF1565C0),
                            enabled = true,
                        ) { vm.onIntent(GameIntent.Retreat(state.player.bench.first().card.id)); onDismiss() }
                    )
                }
            }
            CardDetailSheet(
                imageUrl = card.artwork.large(true),
                title = card.name.es,
                info = "PS ${pip.remainingHp}/${card.hp} · ⚡${pip.attachedEnergyCount}",
                actions = actions,
                onDismiss = onDismiss,
            )
        }
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

@Composable
private fun DecisionPanel(decision: PendingDecision, vm: GameViewModel) {
    val (prompt, candidates, count) = when (decision) {
        is PendingDecision.ChooseTargets -> Triple(decision.prompt.es, decision.candidates, decision.count)
        is PendingDecision.SearchCards -> Triple(decision.prompt.es, decision.candidates, decision.count)
        is PendingDecision.MoveEnergy ->
            Triple(decision.prompt.es, decision.fromCandidates + decision.toCandidates, decision.count)
    }
    val selected = remember(decision) { mutableStateListOf<CardId>() }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(TcgColors.RedDark)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(prompt, color = TcgColors.Parchment, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(candidates) { id ->
                val isSel = id in selected
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) TcgColors.Gold else Color(0xFF14233D))
                        .clickable {
                            if (count <= 1) vm.onResolve(listOf(id))
                            else if (isSel) selected.remove(id)
                            else if (selected.size < count) selected.add(id)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        (if (isSel) "✓ " else "") + vm.cardName(id),
                        color = if (isSel) Color(0xFF3A2A08) else TcgColors.Parchment,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (count > 1) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected.isNotEmpty()) TcgColors.Gold else TcgColors.GoldDark.copy(alpha = 0.4f))
                    .clickable(enabled = selected.isNotEmpty()) { vm.onResolve(selected.toList()) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Confirmar (${selected.size}/$count)", color = Color(0xFF3A2A08), fontWeight = FontWeight.Bold)
            }
        }
    }
}
