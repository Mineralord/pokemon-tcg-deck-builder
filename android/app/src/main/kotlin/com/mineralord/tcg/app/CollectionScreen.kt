package com.mineralord.tcg.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.Rarity

/** Pantalla de Colección: muestra las cartas poseídas (incl. los mazos desbloqueados). */
@Composable
fun CollectionScreen(
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TcgColors.Cream)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("COLECCIÓN", color = TcgColors.RedDark, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Text(
            if (state.loading) "Cargando…" else "${state.distinct} distintas · ${state.total} cartas",
            color = TcgColors.Ink, fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                state.loading -> CircularProgressIndicator(color = TcgColors.Red)
                state.cards.isEmpty() -> Text(
                    "Aún no tienes cartas. Abre un sobre para empezar.",
                    color = TcgColors.Ink.copy(alpha = 0.6f),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.cards) { card -> OwnedCardItem(card) }
                }
            }
        }
    }
}

@Composable
private fun OwnedCardItem(card: OwnedCardUi) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TcgColors.Parchment),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(rarityColor(card.rarity)),
            )
            Text(
                card.name, color = TcgColors.Ink, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp),
            )
            Text("×${card.count} · ${card.setCode}", color = TcgColors.Ink.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

private fun rarityColor(r: Rarity): Color = when (r) {
    Rarity.COMMON -> Color(0xFF7A7A7A)
    Rarity.UNCOMMON -> Color(0xFF3AA6E8)
    Rarity.RARE -> Color(0xFF2E7D32)
    Rarity.RARE_HOLO -> Color(0xFF1565C0)
    Rarity.DOUBLE_RARE -> Color(0xFFB8842F)
    Rarity.ULTRA_RARE -> Color(0xFF8E24AA)
    Rarity.ILLUSTRATION_RARE -> Color(0xFFE65100)
    Rarity.SPECIAL_ILLUSTRATION_RARE -> Color(0xFFC2185B)
    Rarity.HYPER_RARE -> Color(0xFFD4AF37)
    Rarity.PROMO -> Color(0xFF455A64)
}
