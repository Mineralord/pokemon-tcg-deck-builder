package com.mineralord.tcg.feature.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.core.designsystem.TypeEmblem
import com.mineralord.tcg.data.cards.CardFilter
import com.mineralord.tcg.data.cards.CardSort
import com.mineralord.tcg.data.cards.Characteristic
import com.mineralord.tcg.data.cards.SortMethod
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.Supertype

private val PanelBg = Color(0xFFF2EFE9)
private val CardBg = Color.White

private enum class FsTab { FILTROS, ORDENAR }

/**
 * Panel FILTROS / ORDENAR fiel a TCG Live (arte original): dos pestañas, secciones
 * tipo tarjeta con checkboxes y emblemas de tipo, botón rojo "VER N CARTAS" + ✗.
 * Trabaja sobre un borrador y aplica al pulsar "VER N CARTAS".
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSortSheet(
    initialFilter: CardFilter,
    initialSort: CardSort,
    expansions: List<String>,
    count: (CardFilter) -> Int,
    onApply: (CardFilter, CardSort) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableStateOf(FsTab.FILTROS) }
    var f by remember { mutableStateOf(initialFilter) }
    var s by remember { mutableStateOf(initialSort) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PanelBg,
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            // Cabecera roja + pestañas.
            Box(Modifier.fillMaxWidth().background(TcgColors.Red).padding(14.dp)) {
                Text("FILTROS PARA CARTAS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Row(Modifier.fillMaxWidth().background(Color(0xFF2A2A2A))) {
                FsTab.entries.forEach { t ->
                    val sel = t == tab
                    Text(
                        t.name,
                        color = if (sel) TcgColors.Gold else Color(0xCCFFFFFF),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f).clickable { tab = t }.padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp)) {
                if (tab == FsTab.FILTROS) {
                    // SUPERTIPO
                    Section("CATEGORÍA") {
                        Supertype.entries.forEach { st ->
                            CheckRow(
                                label = supertypeLabel(st),
                                checked = st in f.supertypes,
                                onToggle = { f = f.copy(supertypes = f.supertypes.toggle(st)) },
                            )
                        }
                    }
                    // TIPO DE ENERGÍA
                    EmblemSection(
                        title = "TIPO DE ENERGÍA",
                        selected = f.energyTypes,
                        onToggle = { f = f.copy(energyTypes = f.energyTypes.toggle(it)) },
                        onAll = { f = f.copy(energyTypes = if (f.energyTypes.size == EnergyType.entries.size) emptySet() else EnergyType.entries.toSet()) },
                    )
                    // DEBILIDAD
                    EmblemSection(
                        title = "DEBILIDAD",
                        selected = f.weaknesses,
                        onToggle = { f = f.copy(weaknesses = f.weaknesses.toggle(it)) },
                        onAll = { f = f.copy(weaknesses = if (f.weaknesses.size == EnergyType.entries.size) emptySet() else EnergyType.entries.toSet()) },
                    )
                    // CARACTERÍSTICAS
                    Section("CARACTERÍSTICAS") {
                        Characteristic.entries.forEach { c ->
                            CheckRow(
                                label = characteristicLabel(c),
                                checked = c in f.characteristics,
                                onToggle = { f = f.copy(characteristics = f.characteristics.toggle(c)) },
                            )
                        }
                    }
                    // EXPANSIÓN
                    if (expansions.isNotEmpty()) {
                        Section("EXPANSIÓN") {
                            expansions.forEach { code ->
                                CheckRow(
                                    label = code,
                                    checked = code in f.expansions,
                                    onToggle = { f = f.copy(expansions = f.expansions.toggle(code)) },
                                )
                            }
                        }
                    }
                } else {
                    Section("ORDEN") {
                        RadioRow("Básico → Ev. final", !s.descending) { s = s.copy(descending = false) }
                        RadioRow("Ev. final → Básico", s.descending) { s = s.copy(descending = true) }
                    }
                    Section("MÉTODO DE ORDENACIÓN") {
                        RadioRow("Cadena de evolución", s.method == SortMethod.EVOLUTION_CHAIN) { s = s.copy(method = SortMethod.EVOLUTION_CHAIN) }
                        RadioRow("Orden alfabético", s.method == SortMethod.ALPHABETICAL) { s = s.copy(method = SortMethod.ALPHABETICAL) }
                        RadioRow("Expansión", s.method == SortMethod.EXPANSION) { s = s.copy(method = SortMethod.EXPANSION) }
                        RadioRow("Tipo de energía", s.method == SortMethod.ENERGY_TYPE) { s = s.copy(method = SortMethod.ENERGY_TYPE) }
                    }
                }
            }

            // Botón VER N CARTAS + ✗
            Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TcgColors.Red)
                        .clickable { onApply(f, s) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("VER ${count(f)} CARTAS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2A2A2A)).clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) { Text("✕", color = Color.White, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(8.dp)).background(CardBg),
    ) {
        Text(title, color = TcgColors.Ink, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmblemSection(title: String, selected: Set<EnergyType>, onToggle: (EnergyType) -> Unit, onAll: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(8.dp)).background(CardBg).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TcgColors.Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text("SELECCIONAR TODO", color = Color(0xFF888888), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.clickable { onAll() })
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EnergyType.entries.forEach { t ->
                TypeEmblem(type = t, size = 38.dp, selected = t in selected, modifier = Modifier.clickable { onToggle(t) })
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TcgColors.Ink, fontSize = 14.sp)
        Box(
            Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                .background(if (checked) TcgColors.Red else Color(0xFFE0E0E0))
                .border(1.dp, Color(0xFFBBBBBB), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) { if (checked) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun RadioRow(label: String, checked: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onSelect() }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TcgColors.Ink, fontSize = 14.sp)
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .background(if (checked) TcgColors.Red else Color(0xFFE0E0E0))
                .border(1.dp, Color(0xFFBBBBBB), CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (checked) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black) }
    }
}

private fun <T> Set<T>.toggle(v: T): Set<T> = if (v in this) this - v else this + v

private fun supertypeLabel(s: Supertype) = when (s) {
    Supertype.POKEMON -> "Pokémon"
    Supertype.TRAINER -> "Entrenador"
    Supertype.ENERGY -> "Energía"
}

private fun characteristicLabel(c: Characteristic) = when (c) {
    Characteristic.HAS_ABILITY -> "Con Habilidad"
    Characteristic.POKEMON_EX -> "Pokémon ex"
    Characteristic.BASIC -> "Básico"
    Characteristic.STAGE_1 -> "Etapa 1"
    Characteristic.STAGE_2 -> "Etapa 2"
}
