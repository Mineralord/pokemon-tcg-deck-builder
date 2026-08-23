package com.mineralord.tcg.studio

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
import androidx.compose.runtime.LaunchedEffect
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
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.HexagonShape
import com.mineralord.tcg.data.cosmetics.Cosmetic
import com.mineralord.tcg.data.cosmetics.CosmeticAssetType
import com.mineralord.tcg.data.cosmetics.CosmeticCategory
import com.mineralord.tcg.data.cosmetics.CosmeticLicense
import com.mineralord.tcg.data.cosmetics.CosmeticRarity
import com.mineralord.tcg.data.cosmetics.CosmeticStatus
import com.mineralord.tcg.data.cosmetics.Cosmetics
import com.mineralord.tcg.feature.game.combat.CelebrationPreview
import com.mineralord.tcg.feature.game.combat.MatThemePreview
import com.mineralord.tcg.feature.game.combat.SleevePreview

// ============================ ESTÉTICA (piano glass, coherente con la Tienda) ============================
private val LabBg = Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF141A2E), Color(0xFF0A0E1A)))
private val Glass = Color(0x14FFFFFF)
private val GlassBorder = Color(0x24FFFFFF)
private val Accent = Color(0xFF35C4E8)

private val CosmeticRarity.color: Color
    get() = when (this) {
        CosmeticRarity.COMUN -> Color(0xFFB0BEC5)
        CosmeticRarity.POCO_COMUN -> Color(0xFF4CAF50)
        CosmeticRarity.RARO -> Color(0xFF2C82C9)
        CosmeticRarity.EPICO -> Color(0xFFA335EE)
        CosmeticRarity.LEGENDARIO -> Color(0xFFFF9800)
        CosmeticRarity.MITICO -> Color(0xFFFF3D71)
    }

/**
 * **Cosmetics Lab (Studio).** Herramienta interna de inspección del catálogo cosmético
 * data-driven completo (`Cosmetics.repo`): TODOS los cosméticos —procedurales y por asset—
 * con su nombre e id únicos, rareza, estado, licencia y procedencia. A pantalla completa,
 * organizado por categoría, con filtros y ficha de metadata. Es un tool de desarrollo: muestra
 * también los RESTRICTED (catalogados, sin binario), etiquetados como tales.
 */
@Composable
fun CosmeticsLabContent(
    onEnterFullscreen: (@Composable () -> Unit) -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val open = { onEnterFullscreen { CosmeticsLabScreen(onExit = onExitFullscreen) } }
    LaunchedEffect(Unit) { open() }
    Box(Modifier.fillMaxSize().background(LabBg), contentAlignment = Alignment.Center) {
        Box(
            Modifier.clip(RoundedCornerShape(12.dp)).background(Glass).border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                .clickable { open() }.padding(horizontal = 20.dp, vertical = 14.dp),
        ) { Text("⤢  Abrir Cosmetics Lab (pantalla completa)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black) }
    }
}

/** Filtro por tipo de asset. */
private enum class TypeFilter(val label: String) { TODOS("Todos"), PROCEDURAL("Procedural"), ASSET("Con asset") }

@Composable
private fun CosmeticsLabScreen(onExit: () -> Unit) {
    val all = remember { Cosmetics.repo.all }
    var category by remember { mutableStateOf<CosmeticCategory?>(null) } // null = todas
    var typeFilter by remember { mutableStateOf(TypeFilter.TODOS) }
    var detail by remember { mutableStateOf<Cosmetic?>(null) }

    val items = remember(category, typeFilter, all) {
        all
            .filter { category == null || it.category == category }
            .filter {
                when (typeFilter) {
                    TypeFilter.TODOS -> true
                    TypeFilter.PROCEDURAL -> it.assetType == CosmeticAssetType.PROCEDURAL
                    TypeFilter.ASSET -> it.assetType != CosmeticAssetType.PROCEDURAL
                }
            }
    }
    val procedurals = all.count { it.assetType == CosmeticAssetType.PROCEDURAL }

    Column(Modifier.fillMaxSize().background(LabBg)) {
        // Cabecera.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Cosmetics Lab", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text(
                    "${all.size} cosméticos · $procedurals procedurales · ${all.size - procedurals} con asset",
                    color = Color(0x99FFFFFF), fontSize = 12.sp,
                )
            }
            LabChip("✕ Salir", accent = Color(0xFFFF6E6E)) { onExit() }
        }

        // Filtro de tipo.
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeFilter.entries.forEach { t ->
                LabChip(t.label, active = t == typeFilter) { typeFilter = t }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Categorías (Todas + cada una).
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { LabChip("Todas", active = category == null) { category = null } }
            items(CosmeticCategory.entries.toList()) { c ->
                LabChip(c.displayName, active = c == category) { category = c }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Rejilla adaptable (escala a miles).
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { c -> LabCard(c) { detail = c } }
        }
    }

    detail?.let { CosmeticDetailDialog(it) { detail = null } }
}

@Composable
private fun LabCard(cosmetic: Cosmetic, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Glass)
            .border(1.5.dp, cosmetic.rarity.color.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            CosmeticThumb(cosmetic, Modifier.fillMaxSize())
            // Etiqueta PROCEDURAL vs ASSET (lo que el usuario pidió: indicar cuál es cuál).
            TypeTag(cosmetic.assetType, Modifier.align(Alignment.TopStart))
            if (cosmetic.status == CosmeticStatus.RESTRICTED) {
                MiniTag("RESTRICTED", Color(0xFFFF6E6E), Modifier.align(Alignment.TopEnd))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(cosmetic.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
        // ID único, en fuente monoespaciada.
        Text(cosmetic.id, color = Color(0x99FFFFFF), fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(cosmetic.rarity.displayName, color = cosmetic.rarity.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/** Previsualización: delega tapete/funda/efectos a feature:combat; el resto es procedural local. */
@Composable
private fun CosmeticThumb(cosmetic: Cosmetic, modifier: Modifier = Modifier) {
    // Cosméticos por ASSET (IMAGE): render fiel del binario (empaquetado en assets/, GREEN)
    // vía Coil desde android_asset, igual que el juego. Manda sobre el placeholder procedural.
    if (cosmetic.assetType == CosmeticAssetType.IMAGE && cosmetic.assetRef != null) {
        Box(modifier.padding(10.dp), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = "file:///android_asset/${cosmetic.assetRef}",
                contentDescription = cosmetic.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(0.82f),
            )
        }
        return
    }
    when (cosmetic.category) {
        CosmeticCategory.TAPETE -> MatThemePreview(cosmetic.id, modifier.clip(RoundedCornerShape(10.dp)))
        CosmeticCategory.FUNDA -> Box(modifier, contentAlignment = Alignment.Center) {
            SleevePreview(cosmetic.id, Modifier.fillMaxSize(0.7f).aspectRatio(0.72f))
        }
        CosmeticCategory.VICTORIA -> CelebrationPreview(cosmetic.id, victory = true, modifier.clip(RoundedCornerShape(10.dp)))
        CosmeticCategory.DERROTA -> CelebrationPreview(cosmetic.id, victory = false, modifier.clip(RoundedCornerShape(10.dp)))
        else -> GenericThumb(cosmetic, modifier)
    }
}

/** Renderizadores procedurales genéricos (avatar/marco/fondo/caja/moneda/insignia/emote/…). */
@Composable
private fun GenericThumb(cosmetic: Cosmetic, modifier: Modifier = Modifier) {
    val colors = cosmetic.colors.map { Color(it) }.ifEmpty { listOf(Color(0xFF9E9E9E), Color(0xFF616161)) }
    val brush = Brush.linearGradient(colors)
    when (cosmetic.category) {
        CosmeticCategory.AVATAR -> Box(modifier.padding(10.dp).clip(HexagonShape()).background(brush).border(3.dp, cosmetic.rarity.color, HexagonShape()))
        CosmeticCategory.MARCO -> Box(modifier.padding(6.dp).clip(RoundedCornerShape(14.dp)).border(8.dp, brush, RoundedCornerShape(14.dp))) {
            Box(Modifier.fillMaxSize().padding(10.dp).clip(CircleShape).background(Color(0x22FFFFFF)))
        }
        CosmeticCategory.FONDO -> Box(modifier.padding(6.dp).clip(RoundedCornerShape(12.dp)).background(brush))
        CosmeticCategory.CAJA -> Box(modifier.padding(12.dp).clip(RoundedCornerShape(6.dp)).background(brush).border(3.dp, Color(0x55000000), RoundedCornerShape(6.dp)))
        CosmeticCategory.MONEDA -> Box(modifier.padding(10.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize(0.82f).clip(CircleShape).background(brush).border(3.dp, Color(0x66FFFFFF), CircleShape))
        }
        CosmeticCategory.BADGE -> Box(modifier.padding(10.dp), contentAlignment = Alignment.Center) {
            val shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
            Box(Modifier.fillMaxSize(0.72f).clip(shape).background(brush).border(2.dp, cosmetic.rarity.color, shape))
        }
        else -> Box(modifier.padding(10.dp).clip(RoundedCornerShape(12.dp)).background(brush).border(2.dp, cosmetic.rarity.color, RoundedCornerShape(12.dp)))
    }
}

@Composable
private fun TypeTag(type: CosmeticAssetType, modifier: Modifier = Modifier) {
    val (label, color) = when (type) {
        CosmeticAssetType.PROCEDURAL -> "PROC" to Accent
        CosmeticAssetType.IMAGE -> "IMG" to Color(0xFF66BB6A)
        CosmeticAssetType.LOTTIE -> "LOTTIE" to Color(0xFFFFB300)
        CosmeticAssetType.AUDIO -> "AUDIO" to Color(0xFFAB47BC)
        CosmeticAssetType.MODEL3D -> "3D" to Color(0xFFFF7043)
    }
    MiniTag(label, color, modifier)
}

@Composable
private fun MiniTag(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(Color(0xAA000000)).border(1.dp, color, RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) { Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun LabChip(label: String, active: Boolean = false, accent: Color = Accent, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (active) accent.copy(alpha = 0.9f) else Glass)
            .border(1.dp, if (active) accent else GlassBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp),
    ) { Text(label, color = if (active) Color(0xFF06121A) else Color(0xCCFFFFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

/** Ficha de metadata completa del cosmético (todos los campos data-driven). */
@Composable
private fun CosmeticDetailDialog(cosmetic: Cosmetic, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF10161F))
                .border(1.dp, cosmetic.rarity.color.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) { CosmeticThumb(cosmetic, Modifier.fillMaxSize()) }
            Spacer(Modifier.height(12.dp))
            Text(cosmetic.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(cosmetic.id, color = Color(0x99FFFFFF), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TypeTag(cosmetic.assetType)
                MiniTag(cosmetic.status.name, if (cosmetic.status == CosmeticStatus.ACTIVE) Color(0xFF66BB6A) else Color(0xFFFFB300))
                MiniTag(cosmetic.license.name, if (cosmetic.license == CosmeticLicense.GREEN) Color(0xFF66BB6A) else Color(0xFFFF6E6E))
            }
            Spacer(Modifier.height(14.dp))
            MetaRow("Categoría", cosmetic.category.displayName)
            MetaRow("Rareza", cosmetic.rarity.displayName)
            MetaRow("Precio", "${cosmetic.priceMonedas} Monedas${if (cosmetic.prestige) " (prestigio)" else ""}")
            cosmetic.renderer?.let { MetaRow("Renderer", it) }
            cosmetic.assetRef?.let { MetaRow("Asset", it) }
            cosmetic.source?.let { MetaRow("Fuente", it) }
            cosmetic.author?.let { MetaRow("Autor", it) }
            cosmetic.sourceRepository?.let { MetaRow("Repositorio", it) }
            cosmetic.collection?.let { MetaRow("Colección", it) }
            MetaRow("Versión", "asset v${cosmetic.assetVersion} · meta v${cosmetic.metadataVersion}")
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Accent).clickable(onClick = onDismiss)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) { Text("Cerrar", color = Color(0xFF06121A), fontWeight = FontWeight.Black, fontSize = 14.sp) }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label.uppercase(), color = Color(0x80FFFFFF), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(110.dp))
        Text(value, color = Color.White, fontSize = 12.sp)
    }
}
