package com.mineralord.tcg.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.CardDetailDialog
import com.mineralord.tcg.core.designsystem.HoloCardImage
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
    var detailUrl by remember { mutableStateOf<String?>(null) }
    var detailRarity by remember { mutableStateOf<com.mineralord.tcg.engine.model.Rarity?>(null) }
    var detailNumber by remember { mutableStateOf<Int?>(null) }
    var detailSet by remember { mutableStateOf<String?>(null) }
    var detailCount by remember { mutableStateOf(0) }
    var detailCap by remember { mutableStateOf(4) }
    var rarityFilter by remember { mutableStateOf<Rarity?>(null) } // PROVISIONAL

    Column(modifier = modifier.fillMaxSize().background(BinderBg)) {
        // Selector de set (pestañas): una por cada set de las barajas (151 + SV1–4 + Promos…).
        SetTabBar(tabs = state.tabs, selected = state.selectedTab, onSelect = viewModel::selectSet)

        // Chip del set + progreso.
        SetChip(name = state.setName, is151 = state.isSet151, owned = state.ownedInSet, total = state.totalInSet)

        // PROVISIONAL: filtro por rareza.
        RarityFilterBar(selected = rarityFilter, onSelect = { rarityFilter = it })

        val shownSlots = remember(state.slots, rarityFilter) {
            state.slots.filter { rarityFilter == null || it.rarity == rarityFilter }
        }

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
                    items(shownSlots) { slot -> BinderSlot(slot, onClick = { detailUrl = slot.imageLarge; detailRarity = slot.rarity; detailNumber = slot.number; detailSet = slot.setCode; detailCount = slot.count; detailCap = slot.cap }) }
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

    detailUrl?.let { url ->
        CardDetailDialog(imageUrl = url, contentDescription = null, onDismiss = { detailUrl = null }, rarity = detailRarity, cardNumber = detailNumber, setCode = detailSet, copiesOwned = detailCount, copiesCap = detailCap)
    }
}

/** Pestañas de set: fila desplazable de chips, una por cada binder disponible. */
@Composable
private fun SetTabBar(tabs: List<SetTab>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEachIndexed { i, tab ->
            val active = i == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) Color(0xFF2C79E0) else PillBg)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    tab.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

/** PROVISIONAL: barra de chips para filtrar el binder por rareza. */
@Composable
private fun RarityFilterBar(selected: Rarity?, onSelect: (Rarity?) -> Unit) {
    val options: List<Pair<Rarity?, String>> = listOf(
        null to "Todo",
        Rarity.COMMON to "C",
        Rarity.UNCOMMON to "U",
        Rarity.RARE to "R",
        Rarity.RARE_HOLO to "Holo",
        Rarity.DOUBLE_RARE to "ex",
        Rarity.ULTRA_RARE to "Ultra",
        Rarity.ILLUSTRATION_RARE to "IR",
        Rarity.SPECIAL_ILLUSTRATION_RARE to "SIR",
        Rarity.HYPER_RARE to "Oro",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (rarity, label) ->
            val active = selected == rarity
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Color(0xFF2C79E0) else PillBg)
                    .clickable { onSelect(rarity) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Black else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SetChip(name: String, is151: Boolean, owned: Int, total: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Solo el 151 tiene logo dedicado empaquetado; el resto muestra su nombre.
        if (is151) {
            Image(
                painter = painterResource(R.drawable.sv035_logo_169_es),
                contentDescription = "Escarlata y Púrpura 151",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.7f),
            )
        } else {
            Text(name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text("$owned/$total", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth(0.6f).height(6.dp).clip(RoundedCornerShape(50)).background(Color(0x33000000)),
        ) {
            val frac = if (total > 0) owned.toFloat() / total else 0f
            Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(50)).background(Color(0xFF7E57C2)))
        }
    }
}

@Composable
private fun BinderSlot(slot: SlotUi, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (slot.owned) rarityColor(slot.rarity).copy(alpha = 0.22f) else SlotBg)
            .border(
                1.dp,
                if (slot.owned) rarityColor(slot.rarity) else Color(0xFF3A4757),
                RoundedCornerShape(8.dp),
            ),
    ) {
        when {
            // Carta poseída: arte real + HOLO (foil real de malie, brillo ambiental sin
            // giroscopio). Aunque no haya arte en español, HoloCardImage baja el front REAL de
            // malie (que sí existe para promos/SV1…), así que también brillan. El slot ya tiene
            // el aspecto de carta (0.72).
            slot.owned -> HoloCardImage(
                imageUrl = slot.imageEs ?: slot.imageLarge,
                setCode = slot.setCode,
                cardNumber = slot.number,
                rarity = slot.rarity,
                contentDescription = slot.name,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            )
            // Hueco vacío (no poseída).
            else -> Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text(
                    "%03d".format(slot.number),
                    color = Color(0x99FFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }

        // Contador de copias (como en TCG Live): el jugador SIEMPRE ve cuántas tiene de cada carta.
        // "n/tope" (4 · 30 energías); dorado al completar el playset, gris translúcido si aún no.
        if (slot.owned) {
            val complete = slot.count >= slot.cap
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (complete) Color(0xFFFFD54F) else Color(0xCC10161F))
                    .border(1.dp, if (complete) Color(0xFFFFE9A6) else Color(0x55FFFFFF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    "${slot.count}/${slot.cap}",
                    color = if (complete) Color(0xFF1A1400) else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        // Punto rojo: la carta no tiene versión en español.
        if (!slot.hasSpanish) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(12.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xFFE53935))
                    .border(1.5.dp, Color.White, androidx.compose.foundation.shape.CircleShape),
            )
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
