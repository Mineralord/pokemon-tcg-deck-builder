package com.mineralord.tcg.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.rules.GameIntent

private val PanelBg = Color(0xFF14233D)
private val SlotBg = Color(0xFF1E2F45)

/**
 * Pantalla de combate (MVP funcional) contra la IA. Pinta el tablero a partir
 * del [GameState] y ofrece acciones por toque; el motor valida cada intent.
 */
@Composable
fun GameScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    vm: GameViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val state = ui.state

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TcgColors.Navy)
            .statusBarsPadding(),
    ) {
        // Barra superior.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹ Salir",
                color = TcgColors.Parchment,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.clickable(onClick = onExit),
            )
            Spacer(Modifier.width(16.dp))
            val turnLabel = when {
                state == null -> "Cargando…"
                state.isOver -> "Fin de la partida"
                ui.aiThinking || state.activeSide == Side.OPPONENT -> "Turno del rival…"
                else -> "Tu turno"
            }
            Text(turnLabel, color = TcgColors.Gold, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }

        if (state == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Preparando combate…", color = TcgColors.Parchment)
            }
            return
        }

        // Rival (arriba).
        BoardSide(player = state.opponent, mine = false, vm = vm, enabled = false)

        // Registro de combate (últimas líneas).
        LogPanel(ui.log, modifier = Modifier.weight(1f))

        // Jugador (abajo).
        BoardSide(player = state.player, mine = true, vm = vm, enabled = isMyTurn(state, ui))

        val awaitingMine = state.interaction?.side == Side.PLAYER
        when {
            state.isOver -> GameOverBanner(state.winner, onExit)
            awaitingMine -> DecisionPanel(state.interaction!!.decision, vm)
            else -> ActionBar(state, vm, enabled = isMyTurn(state, ui))
        }

        // Mano del jugador.
        HandRow(
            cards = state.player.hand,
            enabled = isMyTurn(state, ui) && state.interaction == null,
            onTap = { card -> handAction(state, card)?.let(vm::onIntent) },
        )

        ui.message?.let {
            Text(
                it,
                color = TcgColors.Red,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
        }
    }
}

// ----------------------------------------------------------------- helpers UI

private fun isMyTurn(state: GameState, ui: GameUiState): Boolean =
    !state.isOver && state.activeSide == Side.PLAYER && !ui.aiThinking

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
private fun BoardSide(player: PlayerState, mine: Boolean, vm: GameViewModel, enabled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (mine) PanelBg else SlotBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            "${if (mine) "Tú" else "Rival"} · Premios ${player.prizesRemaining} · " +
                "Mazo ${player.deck.size} · Mano ${player.hand.size} · Descarte ${player.discard.size}",
            color = TcgColors.Parchment,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PokemonView(player.active, big = true)
            Spacer(Modifier.width(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(player.bench) { PokemonView(it, big = false) }
            }
        }
    }
}

@Composable
private fun PokemonView(pip: PokemonInPlay?, big: Boolean) {
    val w = if (big) 76.dp else 52.dp
    val h = if (big) 106.dp else 72.dp
    Box(
        modifier = Modifier.size(w, h).clip(RoundedCornerShape(6.dp)).background(SlotBg),
        contentAlignment = Alignment.Center,
    ) {
        if (pip == null) {
            Text("—", color = TcgColors.Parchment.copy(alpha = 0.4f))
            return
        }
        AsyncImage(
            model = pip.card.artwork.small(true),
            contentDescription = pip.card.name.es,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
        )
        // HUD: daño + energías + estados.
        Column(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            val remaining = pip.card.hp - pip.damage
            Text(
                "$remaining/${pip.card.hp}",
                color = if (pip.damage > 0) TcgColors.Red else TcgColors.Parchment,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            val tags = buildString {
                if (pip.attachedEnergyCount > 0) append("⚡${pip.attachedEnergyCount} ")
                pip.statuses.forEach { append(it.name.first()) }
            }
            if (tags.isNotBlank()) {
                Text(tags, color = TcgColors.Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LogPanel(log: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TcgColors.Navy)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        log.takeLast(5).forEach {
            Text(it, color = TcgColors.Parchment.copy(alpha = 0.85f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun ActionBar(state: GameState, vm: GameViewModel, enabled: Boolean) {
    val active = state.player.active
    Column(
        modifier = Modifier.fillMaxWidth().background(PanelBg).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            val attacks = active?.card?.attacks?.filter { active.attachedEnergyCount >= it.convertedCost }.orEmpty()
            items(attacks) { atk ->
                ActionChip("⚔ ${atk.name.es}", enabled) { vm.onIntent(GameIntent.Attack(atk.name.es)) }
            }
            // Retirarse al primer banco si hay energía suficiente.
            if (active != null && state.player.bench.isNotEmpty() &&
                active.attachedEnergyCount >= active.card.retreatCost.size
            ) {
                item {
                    ActionChip("↩ Retirarse", enabled) {
                        vm.onIntent(GameIntent.Retreat(state.player.bench.first().card.id))
                    }
                }
            }
            // Habilidades activables.
            val abilities = state.player.allInPlay.flatMap { p -> p.card.abilities.map { p to it } }
            items(abilities) { (p, ab) ->
                ActionChip("✦ ${ab.name.es}", enabled) {
                    vm.onIntent(GameIntent.UseAbility(p.card.id, ab.name.es))
                }
            }
        }
        ActionChip("Terminar turno", enabled, full = true) { vm.onIntent(GameIntent.EndTurn) }
    }
}

@Composable
private fun ActionChip(label: String, enabled: Boolean, full: Boolean = false, onClick: () -> Unit) {
    val bg = if (enabled) TcgColors.Gold else TcgColors.GoldDark.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .then(if (full) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xFF3A2A08), fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
    val selected = remember(decision) { mutableStateListOf<com.mineralord.tcg.engine.model.CardId>() }

    Column(
        modifier = Modifier.fillMaxWidth().background(TcgColors.RedDark).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(prompt, color = TcgColors.Parchment, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(candidates) { id ->
                val isSel = id in selected
                ActionChip(
                    label = (if (isSel) "✓ " else "") + vm.cardName(id),
                    enabled = true,
                ) {
                    if (count <= 1) {
                        vm.onResolve(listOf(id))
                    } else {
                        if (isSel) selected.remove(id) else if (selected.size < count) selected.add(id)
                    }
                }
            }
        }
        if (count > 1) {
            ActionChip("Confirmar (${selected.size}/$count)", enabled = selected.isNotEmpty(), full = true) {
                vm.onResolve(selected.toList())
            }
        }
    }
}

@Composable
private fun HandRow(cards: List<Card>, enabled: Boolean, onTap: (Card) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(TcgColors.Navy).padding(vertical = 8.dp)) {
        Text("  Tu mano (${cards.size})", color = TcgColors.Parchment, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            items(cards) { card ->
                Box(
                    modifier = Modifier
                        .size(58.dp, 81.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SlotBg)
                        .clickable(enabled = enabled) { onTap(card) },
                ) {
                    AsyncImage(
                        model = card.artwork.small(true),
                        contentDescription = card.name.es,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun GameOverBanner(winner: Side?, onExit: () -> Unit) {
    val won = winner == Side.PLAYER
    Column(
        modifier = Modifier.fillMaxWidth().background(if (won) TcgColors.Gold else TcgColors.RedDark).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (won) "¡Ganaste! 🏆" else "Has perdido…",
            color = if (won) Color(0xFF3A2A08) else TcgColors.Parchment,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
        )
        ActionChip("Volver al inicio", enabled = true, full = true, onClick = onExit)
    }
}
