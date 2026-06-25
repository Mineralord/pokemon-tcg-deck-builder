package com.mineralord.tcg.feature.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineralord.tcg.core.designsystem.DeckBox
import com.mineralord.tcg.core.designsystem.TcgColors

private val ScreenBg = Color(0xFF1B2430)
private val TileBg = Color(0xFF26303D)

private enum class DeckTab(val label: String) { RECIENTES("RECIENTES"), FAVORITAS("FAVORITAS"), TODAS("TODAS") }

/** Gestor de barajas (it.2: ver, activa/favorita, crear y editar). */
@Composable
fun DecksScreen(
    modifier: Modifier = Modifier,
    viewModel: DecksViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingDeckId by remember { mutableStateOf<String?>(null) }

    // Si hay edición activa, el editor toma toda la pantalla.
    editingDeckId?.let { id ->
        DeckEditorScreen(deckId = id, onBack = { editingDeckId = null }, modifier = modifier)
        return
    }

    var tab by remember { mutableStateOf(DeckTab.TODAS) }
    var onlyValid by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().background(ScreenBg)) {
        // Cabecera roja + búsqueda.
        Column(Modifier.fillMaxWidth().background(TcgColors.Red).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text("GESTOR DE BARAJAS · FORMATO ESTÁNDAR", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar baraja…", color = Color(0x99000000), fontSize = 13.sp) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)),
            )
        }

        // Pestañas + checkbox SOLO VÁLIDAS.
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF141B24)).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeckTab.entries.forEach { t ->
                val sel = t == tab
                Text(
                    t.label,
                    color = if (sel) TcgColors.Gold else Color(0xCCFFFFFF),
                    fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clickable { tab = t }.padding(vertical = 12.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onlyValid = !onlyValid }.padding(horizontal = 6.dp),
            ) {
                Box(
                    Modifier.size(16.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (onlyValid) TcgColors.Gold else Color(0x33FFFFFF))
                        .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (onlyValid) Text("✓", color = TcgColors.Ink, fontSize = 11.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.size(4.dp))
                Text("VÁLIDAS", color = Color(0xCCFFFFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.loading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                val decks = state.decks
                    .let { if (tab == DeckTab.FAVORITAS) it.filter { d -> d.isFavorite } else it }
                    .let { if (onlyValid) it.filter { d -> d.valid } else it }
                    .let { if (query.isBlank()) it else it.filter { d -> d.name.contains(query, ignoreCase = true) } }
                    .let { if (tab == DeckTab.RECIENTES) it.sortedByDescending { d -> d.updatedAt } else it }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (tab != DeckTab.FAVORITAS) {
                        item(key = "create") {
                            CreateDeckTile { viewModel.createDeck { id -> editingDeckId = id } }
                        }
                    }
                    items(decks.size, key = { decks[it].id }) { i ->
                        DeckCell(decks[i]) { selectedId = decks[i].id }
                    }
                }
            }
        }
    }

    val selected = state.decks.firstOrNull { it.id == selectedId }
    if (selected != null) {
        DeckDetailSheet(
            deck = selected,
            onDismiss = { selectedId = null },
            onSetActive = { viewModel.setActive(selected.id) },
            onToggleFavorite = { viewModel.toggleFavorite(selected.id) },
            onEdit = { selectedId = null; editingDeckId = selected.id },
            onDuplicate = { viewModel.duplicateDeck(selected.id); selectedId = null },
            onDelete = { viewModel.deleteDeck(selected.id); selectedId = null },
        )
    }
}

/** Tile "CREA UNA BARAJA" — crea una baraja vacía y abre el editor. */
@Composable
private fun CreateDeckTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(10.dp))
            .background(TileBg)
            .border(1.dp, Color(0xFF3A4757), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("＋", color = Color(0xAAFFFFFF), fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text("CREA UNA BARAJA", color = Color(0xCCFFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Celda de baraja: caja tintada + nombre + indicadores (activa / inválida). */
@Composable
private fun DeckCell(deck: DeckUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            DeckBox(type = deck.type, modifier = Modifier.fillMaxSize())
            if (deck.isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TcgColors.Gold)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) { Text("ACTIVA", color = TcgColors.Ink, fontSize = 8.sp, fontWeight = FontWeight.Black) }
            }
            if (!deck.valid) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center,
                ) { Text("!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (deck.isFavorite) Text("★ ", color = TcgColors.Gold, fontSize = 12.sp)
            Text(deck.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}
