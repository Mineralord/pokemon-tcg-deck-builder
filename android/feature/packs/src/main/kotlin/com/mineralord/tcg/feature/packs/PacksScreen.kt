package com.mineralord.tcg.feature.packs

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.Rarity

/** Pantalla del slice vertical: abre un sobre y revela 10 cartas. */
@Composable
fun PacksScreen(
    modifier: Modifier = Modifier,
    viewModel: PacksViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TcgColors.Cream)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "SOBRES DIARIOS",
            color = TcgColors.RedDark,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
        )
        Text(
            text = if (state.loading) "Cargando catálogo…"
            else "Restantes hoy: ${state.remainingToday}/${state.maxPerDay}  ·  ${state.totalCards} cartas en el pool",
            color = TcgColors.Ink,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        Button(
            onClick = viewModel::openPack,
            enabled = !state.loading && state.remainingToday > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = TcgColors.Gold,
                contentColor = TcgColors.Ink,
                disabledContainerColor = TcgColors.GoldDark.copy(alpha = 0.4f),
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("ABRIR SOBRE", fontWeight = FontWeight.Black, fontSize = 18.sp)
        }

        state.deniedMessage?.let {
            Text(it, color = TcgColors.RedDark, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                state.loading -> CircularProgressIndicator(color = TcgColors.Red)
                state.revealed.isEmpty() -> Text(
                    "Pulsa ABRIR SOBRE para revelar 10 cartas.",
                    color = TcgColors.Ink.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.revealed) { card -> RevealedCardItem(card) }
                }
            }
        }
    }
}

@Composable
private fun RevealedCardItem(card: RevealedCard) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TcgColors.Parchment),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(rarityColor(card.rarity)),
            )
            Text(
                text = card.name,
                color = TcgColors.Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = rarityLabel(card.rarity),
                color = rarityColor(card.rarity),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
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

private fun rarityLabel(r: Rarity): String = when (r) {
    Rarity.COMMON -> "Común"
    Rarity.UNCOMMON -> "Poco común"
    Rarity.RARE -> "Rara"
    Rarity.RARE_HOLO -> "Rara Holo"
    Rarity.DOUBLE_RARE -> "Doble Rara (ex)"
    Rarity.ULTRA_RARE -> "Ultra Rara"
    Rarity.ILLUSTRATION_RARE -> "Ilustración Rara"
    Rarity.SPECIAL_ILLUSTRATION_RARE -> "Ilustración Especial"
    Rarity.HYPER_RARE -> "Hyper Rara"
    Rarity.PROMO -> "Promo"
}
