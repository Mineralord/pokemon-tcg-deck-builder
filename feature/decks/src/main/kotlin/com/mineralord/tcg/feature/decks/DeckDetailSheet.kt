package com.mineralord.tcg.feature.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.mineralord.tcg.core.designsystem.DeckBox
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.Supertype

private val SheetBg = Color(0xFF202A37)

/** Panel inferior con el detalle de una baraja (rejilla completa + ⋮ + acciones). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailSheet(
    deck: DeckUi,
    onDismiss: () -> Unit,
    onSetActive: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    // Orden de presentación: Pokémon → Entrenador → Energía.
    val ordered = remember(deck) {
        deck.cards.sortedBy {
            when (it.supertype) { Supertype.POKEMON -> 0; Supertype.TRAINER -> 1; Supertype.ENERGY -> 2 }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg,
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (deck.isFavorite) "★" else "☆",
                    color = TcgColors.Gold, fontSize = 24.sp,
                    modifier = Modifier.clickable(onClick = onToggleFavorite).padding(end = 10.dp),
                )
                DeckBox(type = deck.type, modifier = Modifier.size(56.dp, 56.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(deck.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1)
                    Text("${deck.totalCards}/60", color = if (deck.totalCards == 60) Color(0xFF66BB6A) else Color(0xFFE53935), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (deck.valid) "✓ VÁLIDAS" else "⚠ ${deck.invalidReasons.firstOrNull() ?: "No válida"}",
                        color = if (deck.valid) Color(0xFF66BB6A) else Color(0xFFE53935), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Box {
                    Text("⋮", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp,
                        modifier = Modifier.clickable { menuOpen = true }.padding(horizontal = 6.dp))
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Editar") }, onClick = { menuOpen = false; onEdit() })
                        DropdownMenuItem(text = { Text("Duplicar") }, onClick = { menuOpen = false; onDuplicate() })
                        DropdownMenuItem(text = { Text("Borrar") }, onClick = { menuOpen = false; onDelete() })
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SheetButton(if (deck.isActive) "✓ BARAJA ACTIVA" else "USAR ESTA BARAJA", filled = deck.isActive, enabled = !deck.isActive, modifier = Modifier.weight(1f), onClick = onSetActive)
                SheetButton("EDITAR BARAJA", filled = false, enabled = true, modifier = Modifier.weight(1f), onClick = onEdit)
            }

            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(ordered.size) { i ->
                    val c = ordered[i]
                    DeckCardCell(name = c.name, imageEs = c.imageEs, badge = "${c.count}")
                }
            }
        }
    }
}

@Composable
private fun SheetButton(text: String, filled: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = when {
        filled -> TcgColors.Gold
        enabled -> Color(0xFF2E3A49)
        else -> Color(0xFF2E3A49).copy(alpha = 0.4f)
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(bg).clickable(enabled = enabled, onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (filled) TcgColors.Ink else Color.White.copy(alpha = if (enabled) 1f else 0.5f), fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}
