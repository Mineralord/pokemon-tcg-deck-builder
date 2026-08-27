package com.mineralord.tcg.feature.decks

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.core.designsystem.fadingScrollbar
import com.mineralord.tcg.core.designsystem.typeColor

/**
 * #7 — Editor de baraja (chasis, réplica clara de TCG Live): cabecera tintada con
 * el acento de la baraja (nombre + lápiz + "…" + estrella), caja 3D + "Modificar",
 * sub-paneles Energía/Accesorios/Cartas destacadas, chip n/60 + Editar, rejilla de
 * huecos y pie Cancelar/Guardar.
 *
 * Lo funcional cableado: renombrar (lápiz), menú "…" (eliminar/copiar), guardar y
 * cancelar (con "Salir sin guardar"). Todo lo aún no implementado (Modificar,
 * sub-paneles, Editar, añadir/editar cartas, estrella) abre un `InfoDialog`
 * "Próximamente".
 */
@Composable
fun DeckEditorScreen(
    deckId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onDuplicate: () -> Unit = {},
    viewModel: DeckEditorViewModel = viewModel(),
) {
    LaunchedEffect(deckId) { viewModel.start(deckId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    var renaming by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var exitPrompt by remember { mutableStateOf(false) }
    var deletePrompt by remember { mutableStateOf(false) }
    var infoMsg by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }
    var openSortTab by remember { mutableStateOf(false) }
    var autoBuildOpen by remember { mutableStateOf(false) }
    var customizeOpen by remember { mutableStateOf(false) }
    var featuredOpen by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<EditorCardUi?>(null) }

    fun soon(area: String) { infoMsg = area to "Esta función aún no está disponible." }
    fun attemptExit() { if (state.dirty) exitPrompt = true else onBack() }
    BackHandler {
        if (viewer != null) viewer = null
        else if (featuredOpen) featuredOpen = false
        else if (customizeOpen) customizeOpen = false
        else if (autoBuildOpen) autoBuildOpen = false
        else if (filtersOpen) filtersOpen = false
        else if (pickerOpen) pickerOpen = false
        else attemptExit()
    }

    if (!state.exists && !state.loading) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // Selector de cartas (#8) a pantalla completa.
    if (pickerOpen) {
        DeckCardPicker(
            total = state.total,
            deckCards = state.deckCards,
            collection = state.collection,
            onAdd = viewModel::addCard,
            onRemove = viewModel::removeCard,
            onClearAll = viewModel::clearDeck,
            onAutoBuild = { autoBuildOpen = true },
            onOpenFilters = { openSortTab = false; filtersOpen = true },
            onOpenSort = { openSortTab = true; filtersOpen = true },
            onInspect = { viewer = it },
            filtersActive = !viewModel.currentFilter.isEmpty,
            onDone = { pickerOpen = false },
        )
        viewer?.let { DeckCardDetailViewer(it.id, viewModel) { viewer = null } }
        if (filtersOpen) {
            FilterSortSheet(
                initialFilter = viewModel.currentFilter,
                initialSort = viewModel.currentSort,
                expansions = state.availableExpansions,
                count = viewModel::countMatching,
                openOnSort = openSortTab,
                onApply = { f, s -> viewModel.setFilter(f); viewModel.setSort(s); filtersOpen = false },
                onDismiss = { filtersOpen = false },
            )
        }
        if (autoBuildOpen) {
            AutoBuildDialog(
                onConfirm = { types ->
                    autoBuildOpen = false
                    viewModel.autoComplete(types) { total ->
                        infoMsg = "Autocreación" to
                            if (total > 0) "Se generó una baraja de $total cartas siguiendo una estrategia de construcción."
                            else "No hay suficientes cartas en tu colección para autocrear con esos tipos."
                    }
                },
                onCancel = { autoBuildOpen = false },
            )
        }
        infoMsg?.let { (title, body) -> InfoDialog(title = title, body = body, onClose = { infoMsg = null }) }
        return
    }

    val accent = typeColor(state.type)
    // Portada (1ª destacada) + laterales (2ª y 3ª) para la vista de la caja.
    fun imgOf(id: String?): String? = id?.let { i -> state.deckCards.firstOrNull { it.id == i }?.imageEs }
    val boxCover = imgOf(state.featured.getOrNull(0)) ?: state.deckCards.firstOrNull { it.imageEs != null }?.imageEs
    val boxSides = state.featured.drop(1).mapNotNull { imgOf(it) }.take(2)

    Column(modifier = modifier.fillMaxSize().background(BarajasPalette.BgBottom)) {
        // Cabecera tintada con el acento de la baraja.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(lerp(accent, Color.White, 0.18f), accent)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // Fila: nombre + lápiz · "…" · estrella.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.35f))
                        .then(if (readOnly) Modifier else Modifier.clickable { renaming = true })
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(state.name, color = BarajasPalette.Ink, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    if (!readOnly) Text("✎", color = BarajasPalette.Ink, fontSize = 15.sp)
                }
                Spacer(Modifier.size(10.dp))
                if (!readOnly) {
                    HeaderIcon("⋮") { menuOpen = true }
                    Spacer(Modifier.size(8.dp))
                }
                HeaderIcon("☆") { soon("Marcar como favorita") }
            }
            Spacer(Modifier.height(14.dp))
            // Caja 3D + "Modificar".
            Row(verticalAlignment = Alignment.Bottom) {
                DeckBoxPreview(
                    type = state.type,
                    cover = boxCover,
                    sides = boxSides,
                    modifier = Modifier.size(120.dp, 100.dp)
                        .then(if (readOnly) Modifier else Modifier.clickable { customizeOpen = true }),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BarajasPalette.Surface)
                        .clickable { if (!readOnly) customizeOpen = true }
                        .padding(horizontal = 22.dp, vertical = 9.dp),
                ) { Text("Modificar", color = BarajasPalette.Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }

        // Sub-paneles Energía / Accesorios / Cartas destacadas.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SubPanel("Energía", Modifier.weight(1f)) { if (!readOnly) customizeOpen = true }
            SubPanel("Accesorios", Modifier.weight(1f)) { if (!readOnly) customizeOpen = true }
            SubPanel("Cartas destacadas", Modifier.weight(1f)) { if (!readOnly) customizeOpen = true }
        }

        // Chip n/60 + Editar.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(BarajasPalette.Surface)
                    .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🗂", fontSize = 13.sp)
                Spacer(Modifier.size(6.dp))
                Text("${state.total}/60", color = BarajasPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (!readOnly) {
                Spacer(Modifier.size(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BarajasPalette.Surface)
                        .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(50))
                        .clickable { pickerOpen = true }
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                ) { Text("Editar", color = BarajasPalette.Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }

        // Rejilla del mazo (cartas actuales, solo lectura) + hueco "+".
        val gridState = rememberLazyGridState()
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fillMaxWidth().fadingScrollbar(gridState),
        ) {
            items(state.deckCards.size, key = { state.deckCards[it].id }) { i ->
                val c = state.deckCards[i]
                // En examen (readOnly), las cartas no poseídas se muestran con el reverso.
                CardSlot(
                    imageEs = c.imageEs,
                    showBack = readOnly && c.owned <= 0,
                    count = c.inDeck,
                    onLongClick = { viewer = c },
                    onClick = { if (!readOnly) pickerOpen = true },
                )
            }
            if (!readOnly) item(key = "add") { CardSlot(imageEs = null) { pickerOpen = true } }
        }

        // Pie: examen → "Duplicar"; edición → Cancelar / Guardar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (readOnly) {
                DialogButton(
                    text = "Volver",
                    textColor = BarajasPalette.Muted,
                    borderColor = BarajasPalette.HairlineBorder,
                    modifier = Modifier.weight(1f),
                    onClick = onBack,
                )
                DialogButton(
                    text = "Duplicar en Mis barajas",
                    textColor = Color.White,
                    borderColor = Color.Transparent,
                    filled = true,
                    modifier = Modifier.weight(1.4f),
                    onClick = onDuplicate,
                )
            } else {
                DialogButton(
                    text = "Cancelar",
                    textColor = BarajasPalette.Muted,
                    borderColor = BarajasPalette.HairlineBorder,
                    modifier = Modifier.weight(1f),
                    onClick = { attemptExit() },
                )
                DialogButton(
                    text = "Guardar",
                    textColor = Color.White,
                    borderColor = Color.Transparent,
                    filled = true,
                    modifier = Modifier.weight(1f),
                    onClick = onBack,
                )
            }
        }
    }

    if (renaming) {
        RenameDeckDialog(
            current = state.name,
            onCancel = { renaming = false },
            onConfirm = { newName -> viewModel.rename(newName); renaming = false },
        )
    }
    if (exitPrompt) {
        ExitWithoutSaveDialog(
            onCancel = { exitPrompt = false },
            onConfirm = { exitPrompt = false; viewModel.discardChanges(onBack) },
        )
    }
    if (menuOpen) {
        DeckOptionsMenu(
            onDismiss = { menuOpen = false },
            onEliminar = { menuOpen = false; deletePrompt = true },
            onVerCartas = { menuOpen = false; soon("Ver todas las cartas de la baraja") },
            onMostrarCodigo = { menuOpen = false; soon("Mostrar código") },
            onCopiar = { menuOpen = false; viewModel.duplicateDeck(); infoMsg = "Copia creada" to "Se ha creado una copia de esta baraja." },
        )
    }
    if (deletePrompt) {
        ConfirmDialog(
            title = "Eliminar baraja",
            body = "¿Quieres eliminar esta baraja?\nLas cartas permanecerán en tu colección.",
            confirmText = "Vale",
            confirmFilled = false,
            confirmColor = BarajasPalette.DeleteRed,
            onCancel = { deletePrompt = false },
            onConfirm = { deletePrompt = false; viewModel.deleteDeck(onBack) },
        )
    }
    infoMsg?.let { (title, body) -> InfoDialog(title = title, body = body, onClose = { infoMsg = null }) }
    viewer?.let { DeckCardDetailViewer(it.id, viewModel) { viewer = null } }
    if (customizeOpen) {
        // Imágenes de las destacadas actuales (según ids guardados; portada primero).
        val featuredImgs = state.featured.mapNotNull { id -> state.deckCards.firstOrNull { it.id == id }?.imageEs }
        CustomizeDeckSheet(
            type = state.type,
            featured = featuredImgs,
            onSoon = { area -> soon(area) },
            onEditFeatured = { featuredOpen = true },
            onDismiss = { customizeOpen = false },
        )
    }
    if (featuredOpen) {
        FeaturedPickerSheet(
            cards = state.deckCards,
            initialSelected = state.featured,
            onApply = { viewModel.setFeatured(it) },
            onDismiss = { featuredOpen = false },
        )
    }
}

@Composable
private fun HeaderIcon(glyph: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(BarajasPalette.Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, color = BarajasPalette.Muted, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

/** Sub-panel de la cabecera (Energía / Accesorios / Cartas destacadas). */
@Composable
private fun SubPanel(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BarajasPalette.Surface)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = BarajasPalette.DeckName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Text("＋", color = BarajasPalette.Muted, fontSize = 22.sp, fontWeight = FontWeight.Light)
    }
}

/** Hueco de carta del mazo (miniatura, reverso si no se posee, o "+"). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardSlot(
    imageEs: String?,
    showBack: Boolean = false,
    count: Int = 0,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(8.dp))
            .background(BarajasPalette.Hollow)
            .border(1.dp, BarajasPalette.HollowBorder, RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        if (showBack) {
            Image(
                painter = painterResource(com.mineralord.tcg.feature.decks.R.drawable.card_back_default),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            )
        } else if (imageEs != null) {
            AsyncImage(
                model = imageEs,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Text("＋", color = BarajasPalette.Muted, fontSize = 32.sp, fontWeight = FontWeight.Light)
        }
        if (count > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BarajasPalette.NavIcon)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) { Text("×$count", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black) }
        }
    }
}
