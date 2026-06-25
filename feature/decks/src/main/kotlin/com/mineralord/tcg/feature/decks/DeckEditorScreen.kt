package com.mineralord.tcg.feature.decks

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineralord.tcg.core.designsystem.DeckBox
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.Supertype

private val ScreenBg = Color(0xFF1B2430)
private val CollectionBg = Color(0xFF141B24)

private enum class EditCat(val label: String) { TODO("BARAJA"), POKEMON("POKÉMON"), TRAINER("ENTRENADORES"), ENERGY("ENERGÍA") }

/** Editor de baraja: cabecera + pestañas de categoría + rejilla del mazo + colección. */
@Composable
fun DeckEditorScreen(
    deckId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeckEditorViewModel = viewModel(),
) {
    LaunchedEffect(deckId) { viewModel.start(deckId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler { onBack() }

    var cat by remember { mutableStateOf(EditCat.TODO) }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    if (!state.exists && !state.loading) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    Column(modifier = modifier.fillMaxSize().background(ScreenBg)) {
        // Cabecera roja: caja + nombre + ⋮
        Row(
            modifier = Modifier.fillMaxWidth().background(TcgColors.Red).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeckBox(type = state.type, modifier = Modifier.size(40.dp, 40.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(state.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1)
                Text(
                    "${state.total}/60 · " + if (state.valid) "✓ válida" else "⚠ ${state.reasons.firstOrNull() ?: "no válida"}",
                    color = if (state.valid) Color(0xFFB8F0B8) else Color(0xFFFFD7D7),
                    fontSize = 10.sp,
                )
            }
            Box {
                Text("⋮", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp,
                    modifier = Modifier.clickable { menuOpen = true }.padding(horizontal = 8.dp))
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Renombrar") }, onClick = { menuOpen = false; renaming = true })
                    DropdownMenuItem(text = { Text("Borrar baraja") }, onClick = { menuOpen = false; viewModel.deleteDeck(onBack) })
                }
            }
        }

        // Pestañas de categoría con conteos.
        Row(Modifier.fillMaxWidth().background(Color(0xFF0F1620))) {
            EditCat.entries.forEach { c ->
                val n = when (c) {
                    EditCat.TODO -> state.total
                    EditCat.POKEMON -> state.pokemonCount
                    EditCat.TRAINER -> state.trainerCount
                    EditCat.ENERGY -> state.energyCount
                }
                val sel = c == cat
                Column(
                    Modifier.weight(1f).clickable { cat = c }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(c.label, color = if (sel) TcgColors.Gold else Color(0xCCFFFFFF), fontWeight = FontWeight.Black, fontSize = 10.sp, textAlign = TextAlign.Center)
                    Text("$n", color = if (sel) TcgColors.Gold else Color(0x99FFFFFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Rejilla del mazo (filtrada por categoría). Tocar = quitar 1.
        val deckShown = state.deckCards.filter {
            when (cat) {
                EditCat.TODO -> true
                EditCat.POKEMON -> it.supertype == Supertype.POKEMON
                EditCat.TRAINER -> it.supertype == Supertype.TRAINER
                EditCat.ENERGY -> it.supertype == Supertype.ENERGY
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            items(deckShown.size, key = { deckShown[it].id }) { i ->
                val c = deckShown[i]
                DeckCardCell(
                    name = c.name, imageEs = c.imageEs, badge = "${c.inDeck}",
                    onClick = { viewModel.removeCard(c.id) },
                )
            }
        }

        // Panel inferior: COLECCIÓN DE CARTAS + FILTROS/ORDENAR.
        Column(modifier = Modifier.weight(1.25f).fillMaxWidth().background(CollectionBg)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("COLECCIÓN DE CARTAS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PanelButton("≡ FILTROS", Modifier.weight(1f)) { showFilters = true }
                PanelButton("⇅ ORDENAR", Modifier.weight(1f)) { showFilters = true }
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.collection.size, key = { state.collection[it].id }) { i ->
                    val c = state.collection[i]
                    DeckCardCell(
                        name = c.name, imageEs = c.imageEs,
                        badge = if (c.inDeck > 0) "${c.inDeck}" else null,
                        dim = !c.canAdd && c.inDeck == 0,
                        badgeColor = TcgColors.Red,
                        onClick = { if (c.canAdd) viewModel.addCard(c.id) },
                    )
                }
            }
        }
    }

    if (renaming) {
        var text by remember { mutableStateOf(state.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            confirmButton = { TextButton(onClick = { viewModel.rename(text); renaming = false }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancelar") } },
            title = { Text("Renombrar baraja") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        )
    }

    if (showFilters) {
        FilterSortSheet(
            initialFilter = viewModel.currentFilter,
            initialSort = viewModel.currentSort,
            expansions = state.availableExpansions,
            count = { viewModel.countMatching(it) },
            onApply = { f, s -> viewModel.setFilter(f); viewModel.setSort(s); showFilters = false },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun PanelButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2E3A49))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}
