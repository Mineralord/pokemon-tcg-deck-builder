package com.mineralord.tcg.feature.decks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.core.designsystem.fadingScrollbar
import com.mineralord.tcg.feature.carddetail.CardDetailDialogSheet

/**
 * #8 — Selector de cartas para añadir al mazo. Panel superior con el mazo actual
 * (toolbar "Quitar todas"/"Autocreación"/zoom + rejilla, tocar quita) y panel
 * inferior con la colección (chip n/60 + toggle columnas + búsqueda + rejilla,
 * tocar añade). El zoom −/% /+ reescala la rejilla del mazo (columnas).
 */
@Composable
internal fun DeckCardPicker(
    total: Int,
    deckCards: List<EditorCardUi>,
    collection: List<EditorCardUi>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    onAutoBuild: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenSort: () -> Unit,
    onInspect: (EditorCardUi) -> Unit,
    filtersActive: Boolean,
    onDone: () -> Unit,
) {
    var cols by remember { mutableStateOf(5) }
    // Zoom de la rejilla del mazo: % → nº de columnas (menor % = cartas más pequeñas).
    val zoomLevels = listOf(25 to 8, 50 to 5, 75 to 4, 100 to 3)
    var zoomIdx by remember { mutableStateOf(1) } // 50%
    val (zoomPct, deckCols) = zoomLevels[zoomIdx]
    val deckGridState = rememberLazyGridState()
    val collectionGridState = rememberLazyGridState()
    // Aviso transitorio (cápsula opaca semitransparente, ~1.5 s) para límites al añadir.
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(1500)
            toast = null
        }
    }

    // Tocar una carta de la colección: añade si se puede, si no muestra el motivo.
    fun attemptAdd(c: EditorCardUi) {
        if (c.canAdd) onAdd(c.id) else c.limitReason?.let { toast = it }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(BarajasPalette.BgBottom)) {
        // Toolbar del mazo.
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToolbarPill("− Quitar todas", BarajasPalette.DeleteRed) { onClearAll() }
            ToolbarPill("Autocreación", BarajasPalette.Muted) { onAutoBuild() }
            Spacer(Modifier.weight(1f))
            ToolbarIcon("－") { if (zoomIdx > 0) zoomIdx-- }
            Text(
                "$zoomPct%",
                color = BarajasPalette.DeckName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            ToolbarIcon("＋") { if (zoomIdx < zoomLevels.lastIndex) zoomIdx++ }
        }

        // Rejilla del mazo actual (tocar = quitar 1).
        LazyVerticalGrid(
            state = deckGridState,
            columns = GridCells.Fixed(deckCols),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).padding(bottom = 8.dp)
                .fadingScrollbar(deckGridState),
        ) {
            items(deckCards.size, key = { deckCards[it].id }) { i ->
                val c = deckCards[i]
                PickerCard(imageEs = c.imageEs, badgeMinus = true, count = c.inDeck,
                    onLongClick = { onInspect(c) }) { onRemove(c.id) }
            }
        }

        // Chip n/60 + toggle columnas + búsqueda.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BarajasPalette.Surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🗂", fontSize = 12.sp)
                Spacer(Modifier.size(6.dp))
                Text("$total/60", color = BarajasPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "$cols columnas",
                color = BarajasPalette.DeckName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { cols = if (cols == 5) 3 else 5 }.padding(horizontal = 8.dp),
            )
            ToolbarIcon("🔍", active = filtersActive) { onOpenFilters() }
        }

        // Rejilla de la colección (tocar = añadir 1 si se puede).
        LazyVerticalGrid(
            state = collectionGridState,
            columns = GridCells.Fixed(cols),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f).fillMaxWidth().background(BarajasPalette.Surface)
                .fadingScrollbar(collectionGridState),
        ) {
            items(collection.size, key = { collection[it].id }) { i ->
                val c = collection[i]
                CollectionCard(
                    card = c,
                    onAdd = { attemptAdd(c) },
                    onRemove = { onRemove(c.id) },
                    onLongClick = { onInspect(c) },
                )
            }
        }

        // Pie: Vale.
        Box(
            modifier = Modifier.fillMaxWidth().background(BarajasPalette.Surface)
                .padding(horizontal = 40.dp, vertical = 12.dp).navigationBarsPadding(),
        ) {
            DialogButton(
                text = "Vale",
                textColor = Color.White,
                borderColor = Color.Transparent,
                filled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDone,
            )
        }
    }

        // Cápsula de aviso transitorio, centrada sobre la rejilla de la colección.
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(BarajasPalette.Ink.copy(alpha = 0.88f))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(
                    toast ?: "",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Botón redondo de orden (flotante, abajo-derecha sobre la rejilla).
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 78.dp)
                .navigationBarsPadding()
                .size(48.dp)
                .clip(CircleShape)
                .background(BarajasPalette.NavIcon)
                .clickable(onClick = onOpenSort),
            contentAlignment = Alignment.Center,
        ) { Text("⇅", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black) }
    }
}

/**
 * Wrapper del **detalle completo compartido** (colección) abierto desde el editor con long-press.
 * Gestiona favoritos/¡La quiero! en memoria (como la colección), fichas y Fabricar/Destruir vía
 * [DeckEditorViewModel], y la navegación a cartas relacionadas.
 */
@Composable
internal fun DeckCardDetailViewer(initialId: String, viewModel: DeckEditorViewModel, onDismiss: () -> Unit) {
    val fichas by viewModel.fichas.collectAsStateWithLifecycle()
    val favorites = remember { mutableStateListOf<String>() }
    val wishlist = remember { mutableStateListOf<String>() }
    val allDetails = remember { viewModel.allDetails() }
    var currentId by remember { mutableStateOf(initialId) }
    val card = viewModel.detailFor(currentId)
    if (card == null) { onDismiss(); return }
    CardDetailDialogSheet(
        card = card,
        allCards = allDetails,
        isFavorite = currentId in favorites,
        isWished = currentId in wishlist,
        fichas = fichas,
        onToggleFavorite = { if (currentId in favorites) favorites.remove(currentId) else favorites.add(currentId) },
        onToggleWish = { if (currentId in wishlist) wishlist.remove(currentId) else wishlist.add(currentId) },
        onCraft = { viewModel.craft(currentId) },
        onDestroy = { viewModel.destroy(currentId) },
        onOpenRelated = { currentId = it.card.id.raw },
        onDismiss = onDismiss,
    )
}

@Composable
private fun ToolbarPill(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BarajasPalette.Surface)
            .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) { Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
}

@Composable
private fun ToolbarIcon(glyph: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (active) BarajasPalette.NavIcon else BarajasPalette.Surface)
            .border(1.dp, if (active) BarajasPalette.NavIcon else BarajasPalette.HairlineBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, color = if (active) Color.White else BarajasPalette.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

/**
 * Carta del selector. En el mazo: badge "−" y nº de copias. En la colección:
 * contador de poseídas (abajo) y, si está en el mazo, nº de copias (arriba-dcha).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickerCard(
    imageEs: String?,
    owned: Int? = null,
    count: Int = 0,
    badgeMinus: Boolean = false,
    dim: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .alpha(if (dim) 0.4f else 1f)
            .clip(RoundedCornerShape(6.dp))
            .background(BarajasPalette.Hollow)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        if (imageEs != null) {
            AsyncImage(
                model = imageEs,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
            )
        }
        // Badge "−" (quitar) para las cartas del mazo.
        if (badgeMinus) {
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(2.dp).size(18.dp)
                    .clip(CircleShape).background(BarajasPalette.DeleteRed)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("−", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black) }
        }
        // Copias en el mazo (arriba-dcha) para la colección.
        if (!badgeMinus && count > 0) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                    .clip(RoundedCornerShape(6.dp)).background(BarajasPalette.NavIcon)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) { Text("×$count", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black) }
        }
        // Poseídas (abajo-dcha).
        if (owned != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Color(0xCC000000))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) { Text("$owned", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black) }
        }
    }
}

/**
 * Carta de la colección (panel inferior). Tocar la añade (el llamador decide si
 * cabe y, si no, muestra el aviso). Si ya tiene copias en el mazo, se oscurece,
 * muestra un botón redondo "−" (arriba-izq.) para quitar una copia y una etiqueta
 * "enMazo/máx" (abajo). Si no está en el mazo, muestra las copias poseídas.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionCard(
    card: EditorCardUi,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val inDeck = card.inDeck > 0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(6.dp))
            .background(BarajasPalette.Hollow)
            .combinedClickable(onClick = onAdd, onLongClick = onLongClick),
    ) {
        card.imageEs?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                    .alpha(if (inDeck) 0.45f else 1f),
            )
        }
        // Copias poseídas: badge oscuro abajo-izquierda (siempre visible).
        Box(
            modifier = Modifier.align(Alignment.BottomStart).padding(2.dp)
                .clip(RoundedCornerShape(6.dp)).background(Color(0xCC000000))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) { Text("${card.owned}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black) }

        if (inDeck) {
            // "n / máx" grande centrado (seleccionadas / playset).
            Text(
                "${card.inDeck}/${card.maxCopies}",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center),
            )
            // Botón redondo "−" arriba-centro para quitar una copia.
            Box(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp).size(22.dp)
                    .clip(CircleShape).background(Color.White)
                    .border(1.dp, BarajasPalette.HairlineBorder, CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) { Text("−", color = BarajasPalette.DeleteRed, fontSize = 15.sp, fontWeight = FontWeight.Black) }
        }
    }
}
