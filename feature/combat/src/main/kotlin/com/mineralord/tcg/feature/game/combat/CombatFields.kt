package com.mineralord.tcg.feature.game.combat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PokemonInPlay

/**
 * CAPA DE TABLERO (BoardLayer): campos del jugador y del rival. Solo presentación
 * — recibe [GameState] y callbacks; no conoce ni el motor ni el transporte.
 */

/** Un Pokémon en juego (Activo o Banca): carta + PS + energías + estados. */
@Composable
fun PokemonSlot(
    pip: PokemonInPlay?,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
    selected: Boolean = false,
    highlighted: Boolean = false,
    onTap: (() -> Unit)? = null,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(CombatTheme.CardAspect)
                .then(if (onTap != null && pip != null) Modifier.clickable(onClick = onTap) else Modifier),
        ) {
            if (pip == null) {
                EmptySlot(Modifier.fillMaxSize())
            } else {
                CombatCard(
                    imageUrl = pip.card.artwork.small(true),
                    faceDown = faceDown,
                    selected = selected || highlighted,
                    contentDescription = pip.card.name.es,
                    card = if (faceDown) null else pip.card,
                    modifier = Modifier.fillMaxSize(),
                )
                if (!faceDown) {
                    if (pip.statuses.isNotEmpty()) {
                        StatusRow(pip.statuses, Modifier.align(Alignment.TopStart).padding(2.dp))
                    }
                    if (pip.attachedEnergy.isNotEmpty()) {
                        EnergyBadge(pip.attachedEnergy.size, Modifier.align(Alignment.TopEnd).padding(2.dp))
                    }
                }
            }
        }
        if (pip != null && !faceDown) {
            Spacer(Modifier.height(3.dp))
            HpBar(pip.remainingHp, pip.card.hp, Modifier.fillMaxWidth(0.86f))
        }
    }
}

/** Banca: 5 huecos uniformes (responsive por peso). */
@Composable
fun BenchStrip(
    bench: List<PokemonInPlay>,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
    selectedId: CardId? = null,
    highlightId: CardId? = null,
    onTap: ((CardId) -> Unit)? = null,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(CombatTheme.Gap),
        verticalAlignment = Alignment.Top,
    ) {
        for (i in 0 until 5) {
            val pip = bench.getOrNull(i)
            PokemonSlot(
                pip = pip,
                faceDown = faceDown,
                selected = pip != null && pip.card.id == selectedId,
                highlighted = pip != null && pip.card.id == highlightId,
                onTap = if (pip != null && onTap != null) ({ onTap(pip.card.id) }) else null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Campo del RIVAL: banca (detrás) + Activo (hacia el centro). */
@Composable
fun OpponentField(
    state: GameState,
    faceDown: Boolean,
    modifier: Modifier = Modifier,
    onInspect: (Card) -> Unit,
) {
    val opp = state.opponent
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(CombatTheme.Gap),
    ) {
        BenchStrip(
            opp.bench,
            faceDown = faceDown,
            onTap = { id -> opp.bench.firstOrNull { it.card.id == id }?.let { onInspect(it.card) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            PokemonSlot(
                pip = opp.active,
                faceDown = faceDown,
                onTap = { opp.active?.let { onInspect(it.card) } },
                modifier = Modifier.fillMaxWidth(0.30f),
            )
        }
    }
}

/** Campo del JUGADOR: Activo (hacia el centro) + banca (hacia la mano). */
@Composable
fun PlayerField(
    state: GameState,
    selectedActive: Boolean,
    promoteHint: Boolean,
    modifier: Modifier = Modifier,
    onActiveTap: () -> Unit,
    onBenchTap: (CardId) -> Unit,
) {
    val me = state.player
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(CombatTheme.Gap),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            PokemonSlot(
                pip = me.active,
                selected = selectedActive,
                onTap = onActiveTap,
                modifier = Modifier.fillMaxWidth(0.32f),
            )
        }
        BenchStrip(
            me.bench,
            // Al promover tras un KO, la banca guía visualmente (borde dorado).
            highlightId = if (promoteHint) me.bench.firstOrNull()?.card?.id else null,
            onTap = onBenchTap,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        )
    }
}
