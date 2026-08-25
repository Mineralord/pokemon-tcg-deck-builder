package com.mineralord.tcg.feature.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.data.cards.CardFilter
import com.mineralord.tcg.data.cards.Characteristic
import com.mineralord.tcg.engine.model.TrainerCategory

/**
 * #9 — Panel de filtros (bottom sheet) del selector de cartas. Buscador por nombre +
 * pastillas toggle agrupadas: *Otros* (características de Pokémon), *Carta de Entrenador*
 * (sub-tipos, con "Marcar todo") y *Expansiones* (con "Marcar todo"). Pie: **X** (cerrar),
 * **Restablecer** (limpia) y **Buscar** (aplica, cian). Trabaja sobre un borrador local
 * y sólo confirma al pulsar Buscar. [Video 3, 02:38–02:56]
 */
@Composable
internal fun DeckFilterSheet(
    initial: CardFilter,
    expansions: List<String>,
    onApply: (CardFilter) -> Unit,
    onClose: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }

    Box(
        modifier = Modifier.fillMaxSize().background(BarajasPalette.Scrim).clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(BarajasPalette.Surface)
                .clickable(enabled = false) {}       // consume toques del sheet
                .statusBarsPadding(),
        ) {
            Text(
                "Filtros",
                color = BarajasPalette.Ink,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            // Contenido desplazable.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Buscador por nombre.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(BarajasPalette.Hollow)
                        .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔍", fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (draft.nameQuery.isEmpty()) {
                            Text("Buscar por nombre…", color = BarajasPalette.Muted, fontSize = 13.sp)
                        }
                        BasicTextField(
                            value = draft.nameQuery,
                            onValueChange = { draft = draft.copy(nameQuery = it) },
                            singleLine = true,
                            textStyle = TextStyle(color = BarajasPalette.Ink, fontSize = 13.sp),
                            cursorBrush = SolidColor(BarajasPalette.NavIcon),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Otros (características de Pokémon).
                FilterSection("Otros") {
                    val chars = listOf(
                        Characteristic.HAS_ABILITY to "Con habilidad",
                        Characteristic.POKEMON_EX to "Pokémon ex",
                        Characteristic.BASIC to "Básico",
                        Characteristic.STAGE_1 to "Fase 1",
                        Characteristic.STAGE_2 to "Fase 2",
                    )
                    for ((c, label) in chars) {
                        FilterChip(label, c in draft.characteristics) {
                            draft = draft.copy(characteristics = draft.characteristics.toggle(c))
                        }
                    }
                }

                // Carta de Entrenador (sub-tipos) + Marcar todo.
                FilterSection("Carta de Entrenador") {
                    val kinds = listOf(
                        TrainerCategory.ITEM to "Objeto",
                        TrainerCategory.TOOL to "Herramienta",
                        TrainerCategory.SUPPORTER to "Partidario",
                        TrainerCategory.STADIUM to "Estadio",
                    )
                    val allKinds = kinds.map { it.first }.toSet()
                    FilterChip("Marcar todo", draft.trainerKinds.containsAll(allKinds)) {
                        draft = draft.copy(
                            trainerKinds = if (draft.trainerKinds.containsAll(allKinds)) emptySet() else allKinds,
                        )
                    }
                    for ((k, label) in kinds) {
                        FilterChip(label, k in draft.trainerKinds) {
                            draft = draft.copy(trainerKinds = draft.trainerKinds.toggle(k))
                        }
                    }
                }

                // Expansiones + Marcar todo.
                if (expansions.isNotEmpty()) {
                    FilterSection("Expansiones") {
                        val allExp = expansions.toSet()
                        FilterChip("Marcar todo", draft.expansions.containsAll(allExp)) {
                            draft = draft.copy(
                                expansions = if (draft.expansions.containsAll(allExp)) emptySet() else allExp,
                            )
                        }
                        for (code in expansions) {
                            FilterChip(code, code in draft.expansions) {
                                draft = draft.copy(expansions = draft.expansions.toggle(code))
                            }
                        }
                    }
                }
            }

            // Pie: X · Restablecer · Buscar.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BarajasPalette.Surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FooterIcon("✕", onClose)
                DialogButton(
                    text = "Restablecer",
                    textColor = BarajasPalette.Muted,
                    borderColor = BarajasPalette.HairlineBorder,
                    modifier = Modifier.weight(1f),
                    onClick = { draft = CardFilter() },
                )
                DialogButton(
                    text = "Buscar",
                    textColor = Color.White,
                    borderColor = Color.Transparent,
                    filled = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onApply(draft) },
                )
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = BarajasPalette.DeckName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BarajasPalette.NavIcon else BarajasPalette.Hollow)
            .border(
                1.dp,
                if (selected) BarajasPalette.NavIcon else BarajasPalette.HairlineBorder,
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.White else BarajasPalette.Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun FooterIcon(glyph: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(BarajasPalette.Hollow)
            .border(1.dp, BarajasPalette.HairlineBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, color = BarajasPalette.Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

private fun <T> Set<T>.toggle(item: T): Set<T> =
    if (item in this) this - item else this + item
