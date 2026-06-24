package com.mineralord.tcg.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineralord.tcg.engine.model.Rarity

private val BinderBg = Color(0xFF1B2430)
private val SlotBg = Color(0xFF26303D)
private val PillBg = Color(0xFF3A4757)

/** Cartadex — binder del set 151 (réplica de la captura de colección). */
@Composable
fun CollectionScreen(
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(BinderBg)) {
        // Selector de set (pill).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PillBg)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(state.setName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }

        // Chip del set + progreso.
        SetChip(label = state.setLabel, owned = state.ownedInSet, total = state.totalInSet)

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.loading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.slots) { slot -> BinderSlot(slot) }
                }
            }
        }

        // Barra inferior "VISTA DEL SET MAESTRO".
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141B24))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF7E57C2), Color(0xFF5E35B1))))
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text("VISTA DEL SET MAESTRO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SetChip(label: String, owned: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2E5A66), Color(0xFF1E3A44))))
                .border(2.dp, Color(0xFF4FD1E0), RoundedCornerShape(12.dp))
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = Color(0xFFE53935), fontWeight = FontWeight.Black, fontSize = 30.sp)
                Text("$owned/$total", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth(0.8f).height(6.dp).clip(RoundedCornerShape(50)).background(Color(0x33000000)),
                ) {
                    val frac = if (total > 0) owned.toFloat() / total else 0f
                    Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(50)).background(Color(0xFF7E57C2)))
                }
            }
        }
    }
}

@Composable
private fun BinderSlot(slot: SlotUi) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (slot.owned) rarityColor(slot.rarity).copy(alpha = 0.22f) else SlotBg)
            .border(
                1.dp,
                if (slot.owned) rarityColor(slot.rarity) else Color(0xFF3A4757),
                RoundedCornerShape(8.dp),
            ),
    ) {
        if (slot.owned) {
            Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(4.dp)).background(rarityColor(slot.rarity).copy(alpha = 0.5f)))
                Text(slot.name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("Nº ${slot.number}" + if (slot.count > 1) "  ×${slot.count}" else "", color = Color(0xCCFFFFFF), fontSize = 8.sp)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text(
                    "%03d".format(slot.number),
                    color = Color(0x99FFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(12.dp)
                        .height(12.dp)
                        .background(Color(0x55FFFFFF), RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

private fun rarityColor(r: Rarity): Color = when (r) {
    Rarity.COMMON -> Color(0xFF90A4AE)
    Rarity.UNCOMMON -> Color(0xFF4FC3F7)
    Rarity.RARE -> Color(0xFF66BB6A)
    Rarity.RARE_HOLO -> Color(0xFF42A5F5)
    Rarity.DOUBLE_RARE -> Color(0xFFFFB300)
    Rarity.ULTRA_RARE -> Color(0xFFAB47BC)
    Rarity.ILLUSTRATION_RARE -> Color(0xFFFF7043)
    Rarity.SPECIAL_ILLUSTRATION_RARE -> Color(0xFFEC407A)
    Rarity.HYPER_RARE -> Color(0xFFFFD54F)
    Rarity.PROMO -> Color(0xFF78909C)
}
