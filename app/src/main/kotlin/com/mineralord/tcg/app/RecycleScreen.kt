package com.mineralord.tcg.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.profile.CraftingRules
import com.mineralord.tcg.data.profile.CurrencyKind
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// RECICLAJE DE CARTAS — Canon Fase 5 (Crafting), §3.7 Tabla Oficial de Conversión.
// Reciclar duplicados EXCEDENTES otorga Fichas (el único recurso de fabricación §2.5).
// Alcance aprobado: se reciclan las copias que sobran MANTENIENDO ≥1 (nunca la última copia,
// preserva colección/Museo). Valor por rareza = escalón económico I..IX (CraftingRules).
// Estética = misma que la colección de cartas (neumórfica clara de TCG Live).
// ─────────────────────────────────────────────────────────────────────────────

private val RcBgTop = Color(0xFFEDF1F8)
private val RcBgBottom = Color(0xFFDBE3EF)
private val RcPanel = Color(0xFFFFFFFF)
private val RcInk = Color(0xFF2B3346)
private val RcMuted = Color(0xFF8A93A6)
private val RcAccent = Color(0xFF35C4E8)       // cian TCG Live
private val RcFicha = Color(0xFF7C4DFF)        // identidad de las Fichas (morado)

/** Escalón económico I..IX del Canon (§3.2) para una rareza del motor. */
private fun tierOf(r: Rarity): Int = when (r) {
    Rarity.COMMON -> 1
    Rarity.UNCOMMON -> 2
    Rarity.RARE -> 3
    Rarity.RARE_HOLO -> 4
    Rarity.DOUBLE_RARE -> 5          // Pokémon ex (Premium Base)
    Rarity.ULTRA_RARE -> 6           // V / Full Art
    Rarity.ILLUSTRATION_RARE -> 7
    Rarity.SPECIAL_ILLUSTRATION_RARE -> 8
    Rarity.HYPER_RARE -> 9
    Rarity.PROMO -> 1
}

private fun rarityLabel(r: Rarity): String = when (r) {
    Rarity.COMMON -> "Común"
    Rarity.UNCOMMON -> "Poco común"
    Rarity.RARE -> "Rara"
    Rarity.RARE_HOLO -> "Rara Holo"
    Rarity.DOUBLE_RARE -> "Doble Rara (ex)"
    Rarity.ULTRA_RARE -> "Ultra Rara"
    Rarity.ILLUSTRATION_RARE -> "Illustration Rare"
    Rarity.SPECIAL_ILLUSTRATION_RARE -> "Special Illustration"
    Rarity.HYPER_RARE -> "Hyper Rara"
    Rarity.PROMO -> "Promo"
}

/** Una carta con copias de sobra, candidata a reciclaje. */
private data class Recyclable(
    val id: String,
    val name: String,
    val rarity: Rarity,
    val owned: Int,
    val image: String,
) {
    val extras: Int get() = owned - 1                       // conservamos ≥1
    val fichasPer: Int get() = CraftingRules.recycleFichas(tierOf(rarity))
}

@Composable
fun RecycleScreen(onExit: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ProfileRepository(context) }

    var catalog by remember { mutableStateOf<Map<String, Card>?>(null) }
    var owned by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var fichas by remember { mutableStateOf(0) }
    var confirming by remember { mutableStateOf(false) }

    // Cantidad seleccionada a reciclar por carta (id -> nº).
    val selection = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        val cards = withContext(Dispatchers.Default) { CardRepository.load() }
        catalog = withContext(Dispatchers.Default) { cards.all.associateBy { it.id.raw } }
        repo.profile.collectLatest {
            owned = it.owned
            fichas = it.balances[CurrencyKind.FICHAS] ?: 0
        }
    }

    val items by remember {
        derivedStateOf {
            val cat = catalog ?: return@derivedStateOf emptyList()
            owned.mapNotNull { (id, n) ->
                if (n <= 1) return@mapNotNull null                      // sin excedentes
                if (ProfileRepository.capFor(id) == ProfileRepository.UNLIMITED) return@mapNotNull null // energías
                val c = cat[id] ?: return@mapNotNull null
                Recyclable(id, c.name.es, c.rarity, n, c.artwork.smallEs ?: c.artwork.large(spanish = true))
            }.sortedByDescending { it.fichasPer * it.extras }
        }
    }

    val totalFichas by remember { derivedStateOf { selection.entries.sumOf { (id, q) -> q * (items.firstOrNull { it.id == id }?.fichasPer ?: 0) } } }
    val totalCards by remember { derivedStateOf { selection.values.sum() } }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RcBgTop, RcBgBottom))),
    ) {
        Column(Modifier.fillMaxSize()) {
            RecycleHeader(fichas = fichas, onExit = onExit)
            HelpNote()
            if (items.isEmpty()) {
                EmptyState()
            } else {
                SelectAllRow(
                    anySelected = totalCards > 0,
                    onAll = { items.forEach { selection[it.id] = it.extras } },
                    onNone = { selection.clear() },
                )
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items, key = { it.id }) { r ->
                        RecycleRow(
                            r = r,
                            qty = selection[r.id] ?: 0,
                            onQty = { q -> if (q <= 0) selection.remove(r.id) else selection[r.id] = q.coerceIn(0, r.extras) },
                        )
                    }
                }
            }
        }

        // Barra inferior de acción (aparece al seleccionar algo).
        AnimatedVisibility(
            visible = totalCards > 0,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            RecycleActionBar(totalCards = totalCards, totalFichas = totalFichas) { confirming = true }
        }
    }

    if (confirming) {
        ConfirmDialog(
            totalCards = totalCards,
            totalFichas = totalFichas,
            onCancel = { confirming = false },
            onConfirm = {
                confirming = false
                val snapshot = selection.toMap()
                scope.launch {
                    snapshot.forEach { (id, q) ->
                        val per = items.firstOrNull { it.id == id }?.fichasPer ?: 0
                        repo.recycleExtras(id, q, q * per)
                    }
                    selection.clear()
                }
            },
        )
    }
}

@Composable
private fun RecycleHeader(fichas: Int, onExit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(RcPanel).clickable(onClick = onExit),
            contentAlignment = Alignment.Center,
        ) { Text("‹", color = RcInk, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Text("Reciclaje", color = RcInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(1.dp).weight(1f))
        FichaPill(fichas)
    }
}

@Composable
private fun FichaPill(fichas: Int) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(RcPanel).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FichaGlyph(18.dp)
        Spacer(Modifier.width(6.dp))
        Text("$fichas", color = RcInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Spacer(Modifier.width(4.dp))
        Text("Fichas", color = RcMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

/** Ficha estilizada (rombo morado con brillo) para identificar el recurso. */
@Composable
private fun FichaGlyph(size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(4.dp))
            .background(Brush.linearGradient(listOf(RcFicha, Color(0xFF5E35B1)))),
        contentAlignment = Alignment.Center,
    ) { Text("◈", color = Color(0xCCFFFFFF), fontSize = (size.value * 0.7f).sp) }
}

@Composable
private fun HelpNote() {
    Text(
        "Recicla los duplicados que te sobran por Fichas. Siempre conservas al menos 1 copia de cada carta.",
        color = RcMuted, fontSize = 12.5.sp, lineHeight = 16.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    )
}

@Composable
private fun SelectAllRow(anySelected: Boolean, onAll: () -> Unit, onNone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Duplicados reciclables", color = RcInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        val label = if (anySelected) "Quitar todo" else "Seleccionar todo"
        Text(
            label, color = RcAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { if (anySelected) onNone() else onAll() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun RecycleRow(r: Recyclable, qty: Int, onQty: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
            .background(RcPanel).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = r.image, contentDescription = r.name, contentScale = ContentScale.Fit,
            modifier = Modifier.width(52.dp).aspectRatio(0.72f).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.name, color = RcInk, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(rarityLabel(r.rarity), color = RcMuted, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tienes ${r.owned} · sobran ${r.extras}", color = RcMuted, fontSize = 11.5.sp)
                Spacer(Modifier.width(8.dp))
                FichaGlyph(12.dp)
                Spacer(Modifier.width(3.dp))
                Text("+${r.fichasPer} c/u", color = RcFicha, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Stepper(qty = qty, max = r.extras, onQty = onQty)
    }
}

@Composable
private fun Stepper(qty: Int, max: Int, onQty: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepBtn("−", enabled = qty > 0) { onQty(qty - 1) }
        Text(
            "$qty", color = RcInk, fontWeight = FontWeight.Black, fontSize = 16.sp,
            textAlign = TextAlign.Center, modifier = Modifier.width(30.dp),
        )
        StepBtn("+", enabled = qty < max) { onQty(qty + 1) }
    }
}

@Composable
private fun StepBtn(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) RcAccent else RcAccent.copy(alpha = 0.25f)
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(bg).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }
}

@Composable
private fun RecycleActionBar(totalCards: Int, totalFichas: Int, onRecycle: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(14.dp)) {
        Row(
            Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                .background(RcAccent).clickable(onClick = onRecycle).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Reciclar $totalCards", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            FichaGlyph(18.dp)
            Spacer(Modifier.width(6.dp))
            Text("+$totalFichas", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ConfirmDialog(totalCards: Int, totalFichas: Int, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x66000000)).clickable(onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(28.dp).clip(RoundedCornerShape(20.dp)).background(RcPanel).padding(22.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("¿Reciclar duplicados?", color = RcInk, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Vas a reciclar $totalCards duplicado(s) por $totalFichas Fichas. Conservarás al menos 1 copia de cada carta. Esta acción no se puede deshacer.",
                color = RcMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp)).border(1.5.dp, RcMuted, RoundedCornerShape(12.dp))
                        .clickable(onClick = onCancel).padding(horizontal = 20.dp, vertical = 10.dp),
                ) { Text("Cancelar", color = RcInk, fontWeight = FontWeight.Bold) }
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(RcAccent)
                        .clickable(onClick = onConfirm).padding(horizontal = 20.dp, vertical = 10.dp),
                ) { Text("Reciclar", color = Color.White, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FichaGlyph(46.dp)
            Spacer(Modifier.height(14.dp))
            Text("No tienes duplicados para reciclar", color = RcInk, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Cuando acumules más de una copia de una carta, podrás reciclar las que te sobren por Fichas.",
                color = RcMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp,
            )
        }
    }
}
