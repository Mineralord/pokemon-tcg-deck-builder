package com.mineralord.tcg.app

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.HexagonShape
import com.mineralord.tcg.data.profile.Cosmetic
import com.mineralord.tcg.data.profile.CosmeticCatalog
import com.mineralord.tcg.data.profile.CosmeticCategory
import com.mineralord.tcg.data.profile.CosmeticRarity
import com.mineralord.tcg.data.profile.CurrencyKind
import com.mineralord.tcg.data.profile.PlayerProfile

// ============================ IDENTIDAD VISUAL AAA ============================
// Fondo "piano glass" oscuro (Marvel Snap): los objetos tienen precedencia; la UI es cristal.
private val StoreBg = Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF141A2E), Color(0xFF0A0E1A)))
private val Glass = Color(0x14FFFFFF)
private val GlassBorder = Color(0x24FFFFFF)
private val MonedasGold = Color(0xFFFFD54F)
private val CristalesBlue = Color(0xFF4FC3F7)

/** Color canónico de cada rareza cosmética (estándar de la industria; §9.3). */
private val CosmeticRarity.color: Color
    get() = when (this) {
        CosmeticRarity.COMUN -> Color(0xFFB0BEC5)
        CosmeticRarity.POCO_COMUN -> Color(0xFF4CAF50)
        CosmeticRarity.RARO -> Color(0xFF2C82C9)
        CosmeticRarity.EPICO -> Color(0xFFA335EE)
        CosmeticRarity.LEGENDARIO -> Color(0xFFFF9800)
        CosmeticRarity.MITICO -> Color(0xFFFF3D71)
    }

/** Las rarezas altas lucen un barrido de brillo animado (shimmer). */
private val CosmeticRarity.shimmers: Boolean get() = ordinal >= CosmeticRarity.LEGENDARIO.ordinal

// ============================ HUB DE LA TIENDA ============================

/**
 * Hub de la Tienda (Fase 3 §2.2): separa las categorías por sistema económico —
 * **Sobres** (Cristales) y **Cosméticos** (Monedas)— sin mezclarlas. Estética AAA
 * piano-glass; cada sección es un panel grande, táctil y con su moneda.
 */
@Composable
fun StoreHubScreen(
    balances: Map<CurrencyKind, Int>,
    onSobres: () -> Unit,
    onCosmeticos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StoreBg)
            .padding(20.dp),
    ) {
        Text("TIENDA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
        Text(
            "Amplía tu colección y personaliza tu identidad.",
            color = Color(0x99FFFFFF), fontSize = 13.sp,
        )
        Spacer(Modifier.height(20.dp))
        StoreSection(
            title = "Sobres",
            subtitle = "Cartas de expansión · abre y colecciona",
            colors = listOf(Color(0xFF1B3A6B), Color(0xFF0E2144)),
            accent = CristalesBlue,
            currencyLabel = "${balances[CurrencyKind.CRISTALES] ?: 0} 💎  Cristales",
            onClick = onSobres,
        )
        Spacer(Modifier.height(16.dp))
        StoreSection(
            title = "Cosméticos",
            subtitle = "Avatares, marcos, tapetes y más · personalización",
            colors = listOf(Color(0xFF4A2A6B), Color(0xFF241238)),
            accent = MonedasGold,
            currencyLabel = "${balances[CurrencyKind.MONEDAS] ?: 0} 🪙  Monedas",
            onClick = onCosmeticos,
        )
    }
}

@Composable
private fun StoreSection(
    title: String,
    subtitle: String,
    colors: List<Color>,
    accent: Color,
    currencyLabel: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Column(Modifier.align(Alignment.TopStart)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color(0xCCFFFFFF), fontSize = 12.sp)
        }
        Chip(currencyLabel, accent, Modifier.align(Alignment.BottomStart))
        Text(
            "›",
            color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun Chip(label: String, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x33000000))
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

// ============================ TIENDA DE COSMÉTICOS ============================

/**
 * Tienda de Cosméticos (Fase 3, Cap. 8–14): catálogo permanente por categoría, con
 * rareza cosmética (§9), compra con **Monedas** (§3.3) y equipado (uno por categoría).
 * Presentación AAA: marcos por rareza con glow, shimmer en las rarezas altas y ficha
 * de compra con previsualización grande.
 */
@Composable
fun CosmeticStoreScreen(
    profile: PlayerProfile,
    onBuy: (Cosmetic) -> Unit,
    onEquip: (Cosmetic) -> Unit,
    modifier: Modifier = Modifier,
) {
    var category by remember { mutableStateOf(CosmeticCategory.AVATAR) }
    var selected by remember { mutableStateOf<Cosmetic?>(null) }
    val monedas = profile.balances[CurrencyKind.MONEDAS] ?: 0

    Column(modifier = modifier.fillMaxSize().background(StoreBg)) {
        // Cabecera con saldo de Monedas.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Cosméticos", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Chip("$monedas 🪙", MonedasGold)
        }

        // Pestañas de categoría (scroll horizontal).
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(CosmeticCategory.entries.toList()) { c ->
                CategoryTab(c.displayName, active = c == category) { category = c }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Rejilla del catálogo de la categoría.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(CosmeticCatalog.byCategory(category), key = { it.id }) { cosmetic ->
                CosmeticCard(
                    cosmetic = cosmetic,
                    owned = profile.ownsCosmetic(cosmetic.id),
                    equipped = profile.equippedIn(cosmetic.category) == cosmetic.id,
                    onClick = { selected = cosmetic },
                )
            }
        }
    }

    selected?.let { c ->
        CosmeticDetailDialog(
            cosmetic = c,
            owned = profile.ownsCosmetic(c.id),
            equipped = profile.equippedIn(c.category) == c.id,
            canAfford = monedas >= c.priceMonedas,
            onBuy = { onBuy(c); selected = null },
            onEquip = { onEquip(c); selected = null },
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun CategoryTab(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) Color(0xFF2E5FA8) else Glass)
            .border(1.dp, if (active) CristalesBlue else GlassBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (active) Color.White else Color(0xB3FFFFFF),
            fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun CosmeticCard(
    cosmetic: Cosmetic,
    owned: Boolean,
    equipped: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Glass)
            .rarityFrame(cosmetic.rarity, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            CosmeticPreview(cosmetic, Modifier.fillMaxSize())
            if (equipped) {
                Chip("Equipado", cosmetic.rarity.color, Modifier.align(Alignment.TopEnd))
            } else if (owned) {
                Chip("En posesión", Color(0xFF66BB6A), Modifier.align(Alignment.TopEnd))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(cosmetic.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(cosmetic.rarity.displayName, color = cosmetic.rarity.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        when {
            equipped -> Text("✓ Equipado", color = Color(0xFF9CCC65), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            owned -> Text("Tocar para equipar", color = Color(0x99FFFFFF), fontSize = 11.sp)
            else -> Text("${cosmetic.priceMonedas} 🪙", color = MonedasGold, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** Ficha de compra/equipado con previsualización grande (§15.5: info del objeto). */
@Composable
private fun CosmeticDetailDialog(
    cosmetic: Cosmetic,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF10161F))
                .rarityFrame(cosmetic.rarity, RoundedCornerShape(22.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                CosmeticPreview(cosmetic, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(14.dp))
            Text(cosmetic.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(cosmetic.rarity.color))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${cosmetic.rarity.displayName} · ${cosmetic.category.displayName}",
                    color = cosmetic.rarity.color, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                )
            }
            if (cosmetic.prestige) {
                Spacer(Modifier.height(4.dp))
                Text("★ Cosmético de prestigio", color = MonedasGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            when {
                equipped -> ActionButton("Equipado", enabled = false, accent = Color(0xFF66BB6A), onClick = {})
                owned -> ActionButton("Equipar", enabled = true, accent = cosmetic.rarity.color, onClick = onEquip)
                else -> ActionButton(
                    "Comprar · ${cosmetic.priceMonedas} 🪙",
                    enabled = canAfford,
                    accent = MonedasGold,
                    onClick = onBuy,
                )
            }
            if (!owned && !canAfford) {
                Spacer(Modifier.height(8.dp))
                Text("Monedas insuficientes", color = Color(0xFFFF8A80), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, enabled: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) accent else accent.copy(alpha = 0.25f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) Color(0xFF10161F) else Color(0x80FFFFFF),
            fontWeight = FontWeight.Black, fontSize = 15.sp,
        )
    }
}

// ============================ PREVISUALIZACIÓN PROCEDURAL ============================

/**
 * Previsualización procedural (arte propio, sin copyright) según categoría y paleta del
 * cosmético. Da una lectura clara del objeto sin necesitar assets reales.
 */
@Composable
private fun CosmeticPreview(cosmetic: Cosmetic, modifier: Modifier = Modifier) {
    val colors = cosmetic.colors.map { Color(it) }.ifEmpty { listOf(Color.Gray, Color.DarkGray) }
    val brush = Brush.linearGradient(colors)
    // Tapetes y fundas usan su previsualización REAL (WYSIWYG): lo que ves en la ficha es lo
    // que verás en la partida.
    when (cosmetic.category) {
        CosmeticCategory.TAPETE -> {
            com.mineralord.tcg.feature.game.combat.MatThemePreview(
                cosmetic.id,
                modifier.padding(8.dp).clip(RoundedCornerShape(10.dp)),
            )
            return
        }
        CosmeticCategory.FUNDA -> {
            Box(modifier, contentAlignment = Alignment.Center) {
                com.mineralord.tcg.feature.game.combat.SleevePreview(
                    cosmetic.id,
                    Modifier.fillMaxSize(0.7f).aspectRatio(0.72f),
                )
            }
            return
        }
        else -> Unit
    }
    when (cosmetic.category) {
        CosmeticCategory.AVATAR -> Box(
            modifier.padding(10.dp).clip(HexagonShape()).background(brush)
                .border(3.dp, cosmetic.rarity.color, HexagonShape()),
        )
        CosmeticCategory.MARCO -> Box(
            modifier.padding(6.dp).clip(RoundedCornerShape(14.dp))
                .border(8.dp, brush, RoundedCornerShape(14.dp)),
        ) { Box(Modifier.fillMaxSize().padding(10.dp).clip(CircleShape).background(Color(0x22FFFFFF))) }
        CosmeticCategory.FONDO -> Box(modifier.padding(6.dp).clip(RoundedCornerShape(12.dp)).background(brush))
        CosmeticCategory.TAPETE -> Box(
            modifier.padding(vertical = 24.dp, horizontal = 6.dp).clip(RoundedCornerShape(10.dp))
                .background(brush).border(2.dp, cosmetic.rarity.color.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
        )
        CosmeticCategory.FUNDA -> Box(
            modifier, contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxSize(0.7f).aspectRatio(0.72f).clip(RoundedCornerShape(8.dp)).background(brush)
                    .border(2.dp, Color(0x55FFFFFF), RoundedCornerShape(8.dp)),
            )
        }
        CosmeticCategory.CAJA -> Box(
            modifier.padding(12.dp).clip(RoundedCornerShape(6.dp)).background(brush)
                .border(3.dp, Color(0x55000000), RoundedCornerShape(6.dp)),
        )
    }
}

// ============================ MARCO POR RAREZA (glow + shimmer) ============================

/**
 * Borde por rareza: color sólido + glow suave; en rarezas altas, un barrido de brillo
 * animado (shimmer) sobre el borde, al estilo de las tiendas AAA.
 */
private fun Modifier.rarityFrame(rarity: CosmeticRarity, shape: Shape): Modifier = composed {
    val base = this.border(1.5.dp, rarity.color.copy(alpha = 0.9f), shape)
    if (!rarity.shimmers) return@composed base
    val transition = rememberInfiniteTransition(label = "shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Restart), label = "shimmerPhase",
    )
    base.drawBehind {
        val w = size.width
        val x = w * phase
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, rarity.color.copy(alpha = 0.55f), Color.Transparent),
                startX = x - w * 0.25f, endX = x + w * 0.25f,
            ),
        )
    }
}
