package com.mineralord.tcg.feature.game.combat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.tilt.resolveFinish
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.PokemonCard

/**
 * CAPA DE DECISIONES: cuando el motor pausa un efecto esperando una elección del
 * jugador ([PendingDecision]), esta hoja inferior la resuelve → onResolve(chosen).
 * Cubre los 5 tipos de decisión del motor. Solo presentación + selección; el motor
 * valida y aplica.
 */
@Composable
fun DecisionPanel(
    decision: PendingDecision,
    cardOf: (CardId) -> Card?,
    onResolve: (List<CardId>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(Color(0xF21A2030))
            .border(1.dp, CombatTheme.Gold.copy(alpha = 0.5f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            decision.prompt.es,
            color = CombatTheme.Gold,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
        )
        when (decision) {
            is PendingDecision.CoinFlip -> {
                DButton("¡LANZAR MONEDA!", enabled = true, accent = CombatTheme.Gold) { onResolve(emptyList()) }
            }
            is PendingDecision.CoinFlipThenSearch -> {
                DButton("¡LANZAR MONEDA!", enabled = true, accent = CombatTheme.Gold) { onResolve(emptyList()) }
            }
            is PendingDecision.ChooseTargets ->
                SelectGrid(decision.candidates, decision.count, cardOf, allowNone = false, onResolve = onResolve)
            is PendingDecision.SearchCards ->
                SelectGrid(decision.candidates, decision.count, cardOf, allowNone = true, onResolve = onResolve)
            is PendingDecision.AttachFromRevealed ->
                AttachPairing(decision, cardOf, onResolve)
            is PendingDecision.PlaceCounters ->
                CounterSpread(decision, cardOf, onResolve)
            is PendingDecision.MoveEnergy -> {
                Text("Selección de energías no soportada aún aquí.", color = CombatTheme.Muted, fontSize = 11.sp)
                DButton("Continuar", enabled = true, accent = CombatTheme.Gold) { onResolve(emptyList()) }
            }
            is PendingDecision.ChooseEnergyType ->
                EnergyTypePicker(decision, onResolve)
            is PendingDecision.ChooseAttack ->
                AttackCopyPicker(decision, cardOf, onResolve)
        }
    }
}

/** Rejilla de candidatos: seleccionar hasta [count]; confirmar → onResolve. */
@Composable
private fun SelectGrid(
    candidates: List<CardId>,
    count: Int,
    cardOf: (CardId) -> Card?,
    allowNone: Boolean,
    onResolve: (List<CardId>) -> Unit,
) {
    val sel = remember(candidates) { mutableStateListOf<CardId>() }
    Column(
        Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        candidates.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { id ->
                    val selected = id in sel
                    CombatCard(
                        imageUrl = cardOf(id)?.artwork?.large(true),
                        selected = selected,
                        contentDescription = cardOf(id)?.name?.es,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(CombatTheme.CardAspect)
                            .clickable {
                                if (selected) sel.remove(id)
                                else if (sel.size < count) sel.add(id)
                            },
                    )
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DButton("Confirmar (${sel.size}/$count)", enabled = sel.isNotEmpty(), accent = CombatTheme.Good) {
            onResolve(sel.toList())
        }
        if (allowNone) {
            DButton("Ninguna", enabled = true, accent = CombatTheme.Muted) { onResolve(emptyList()) }
        }
    }
}

/**
 * Elegir 1 ataque del Activo rival para copiarlo (Mew ex — Hackeo Genómico). Se presenta con el MISMO
 * panel de ataque del combate ([AttackPanel]) — idéntico a TCG Live —; «USAR ATAQUE» resuelve con el
 * índice del ataque elegido. Respaldo con botones por nombre si no se resuelve la carta rival.
 */
@Composable
private fun AttackCopyPicker(
    decision: PendingDecision.ChooseAttack,
    cardOf: (CardId) -> Card?,
    onResolve: (List<CardId>) -> Unit,
) {
    val pokemon = cardOf(decision.fromPokemon) as? PokemonCard
    Column(
        Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (pokemon != null) {
            val type = pokemon.types.firstOrNull()
            val finish = resolveFinish(pokemon.rarity)
            pokemon.attacks.forEachIndexed { i, atk ->
                AttackPanel(
                    attack = atk,
                    type = type,
                    finish = finish,
                    enabled = true,
                    used = false,
                    description = atk.text.es,
                    onUse = { onResolve(listOf(PendingDecision.encodeAttackIndex(i))) },
                )
            }
        } else {
            decision.attackNames.forEachIndexed { i, name ->
                DButton(name, enabled = true, accent = CombatTheme.Gold) {
                    onResolve(listOf(PendingDecision.encodeAttackIndex(i)))
                }
            }
        }
    }
}

/** Elegir 1 tipo de Energía (Porygon — Conversión 4): un botón por tipo candidato. */
@Composable
private fun EnergyTypePicker(
    decision: PendingDecision.ChooseEnergyType,
    onResolve: (List<CardId>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        decision.candidates.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { type ->
                    Box(Modifier.weight(1f)) {
                        DButton(energyTypeLabelEs(type), enabled = true, accent = CombatTheme.Good) {
                            onResolve(listOf(PendingDecision.encodeType(type)))
                        }
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** Etiqueta en español del tipo de Energía para la UI. */
private fun energyTypeLabelEs(type: com.mineralord.tcg.engine.model.EnergyType): String = when (type) {
    com.mineralord.tcg.engine.model.EnergyType.GRASS -> "Planta"
    com.mineralord.tcg.engine.model.EnergyType.FIRE -> "Fuego"
    com.mineralord.tcg.engine.model.EnergyType.WATER -> "Agua"
    com.mineralord.tcg.engine.model.EnergyType.LIGHTNING -> "Rayo"
    com.mineralord.tcg.engine.model.EnergyType.PSYCHIC -> "Psíquico"
    com.mineralord.tcg.engine.model.EnergyType.FIGHTING -> "Lucha"
    com.mineralord.tcg.engine.model.EnergyType.DARKNESS -> "Oscuro"
    com.mineralord.tcg.engine.model.EnergyType.METAL -> "Metal"
    com.mineralord.tcg.engine.model.EnergyType.DRAGON -> "Dragón"
    com.mineralord.tcg.engine.model.EnergyType.FAIRY -> "Hada"
    com.mineralord.tcg.engine.model.EnergyType.COLORLESS -> "Incoloro"
}

/**
 * Reparto de contadores de daño (Embestida Hueca / Psicopoder): toca un Pokémon para
 * ponerle un contador (10 de daño); repite hasta colocar todos. `chosen` = lista con el
 * id repetido una vez por contador. Confirmar exige haber repartido los [count] contadores.
 */
@Composable
private fun CounterSpread(
    decision: PendingDecision.PlaceCounters,
    cardOf: (CardId) -> Card?,
    onResolve: (List<CardId>) -> Unit,
) {
    val placed = remember(decision) { mutableStateListOf<CardId>() }
    val total = decision.count
    Column(
        Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        decision.candidates.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { id ->
                    val n = placed.count { it == id }
                    Box(Modifier.weight(1f)) {
                        CombatCard(
                            imageUrl = cardOf(id)?.artwork?.large(true),
                            selected = n > 0,
                            contentDescription = cardOf(id)?.name?.es,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(CombatTheme.CardAspect)
                                .clickable {
                                    if (placed.size < total) placed.add(id)
                                    else { placed.remove(id) } // lleno: tocar quita 1 de ese objetivo
                                },
                        )
                        if (n > 0) {
                            Text(
                                "+${n * 10}",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CombatTheme.Foe)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DButton("Confirmar (${placed.size}/$total)", enabled = placed.size == total, accent = CombatTheme.Good) {
            onResolve(placed.toList())
        }
        DButton("Reiniciar", enabled = placed.isNotEmpty(), accent = CombatTheme.Muted) { placed.clear() }
    }
}

/** Empareja Energías reveladas con Pokémon de Banca (Generador Eléctrico / Mela). */
@Composable
private fun AttachPairing(
    decision: PendingDecision.AttachFromRevealed,
    cardOf: (CardId) -> Card?,
    onResolve: (List<CardId>) -> Unit,
) {
    val pairs = remember(decision) { mutableStateListOf<Pair<CardId, CardId>>() }
    var curEnergy by remember(decision) { mutableStateOf<CardId?>(null) }

    Text("Energías reveladas (toca una, luego un Pokémon):", color = CombatTheme.Muted, fontSize = 11.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        decision.energyCandidates.take(6).forEach { id ->
            val used = pairs.any { it.first == id }
            CombatCard(
                imageUrl = cardOf(id)?.artwork?.small(true),
                selected = curEnergy == id,
                dimmed = used,
                modifier = Modifier
                    .width(52.dp)
                    .aspectRatio(CombatTheme.CardAspect)
                    .clickable(enabled = !used) { curEnergy = id },
            )
        }
    }
    Text("Pokémon elegibles:", color = CombatTheme.Muted, fontSize = 11.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        decision.benchCandidates.take(6).forEach { id ->
            CombatCard(
                imageUrl = cardOf(id)?.artwork?.small(true),
                modifier = Modifier
                    .width(52.dp)
                    .aspectRatio(CombatTheme.CardAspect)
                    .clickable(enabled = curEnergy != null && pairs.size < decision.maxAttach) {
                        curEnergy?.let { e -> pairs.add(e to id); curEnergy = null }
                    },
            )
        }
    }
    Text("Uniones: ${pairs.size}/${decision.maxAttach}", color = CombatTheme.OnSurface, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DButton("Listo", enabled = true, accent = CombatTheme.Good) {
            onResolve(pairs.flatMap { listOf(it.first, it.second) })
        }
        DButton("Deshacer", enabled = pairs.isNotEmpty(), accent = CombatTheme.Muted) {
            if (pairs.isNotEmpty()) pairs.removeAt(pairs.size - 1)
        }
    }
}

@Composable
private fun DButton(label: String, enabled: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) accent else accent.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(label, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}
