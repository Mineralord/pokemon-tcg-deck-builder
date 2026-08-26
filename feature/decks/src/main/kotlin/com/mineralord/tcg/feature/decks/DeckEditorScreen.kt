package com.mineralord.tcg.feature.decks

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.core.designsystem.DeckBox
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
    var autoBuildOpen by remember { mutableStateOf(false) }

    fun soon(area: String) { infoMsg = area to "Esta función aún no está disponible." }
    fun attemptExit() { if (state.dirty) exitPrompt = true else onBack() }
    BackHandler {
        if (autoBuildOpen) autoBuildOpen = false
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
            onSoon = { area -> infoMsg = area to "Esta función aún no está disponible." },
            onOpenFilters = { filtersOpen = true },
            filtersActive = !viewModel.currentFilter.isEmpty,
            onDone = { pickerOpen = false },
        )
        if (filtersOpen) {
            FilterSortSheet(
                initialFilter = viewModel.currentFilter,
                initialSort = viewModel.currentSort,
                expansions = state.availableExpansions,
                count = viewModel::countMatching,
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
                        .clickable { renaming = true }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(state.name, color = BarajasPalette.Ink, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    Text("✎", color = BarajasPalette.Ink, fontSize = 15.sp)
                }
                Spacer(Modifier.size(10.dp))
                HeaderIcon("⋮") { menuOpen = true }
                Spacer(Modifier.size(8.dp))
                HeaderIcon("☆") { soon("Marcar como favorita") }
            }
            Spacer(Modifier.height(14.dp))
            // Caja 3D + "Modificar".
            Row(verticalAlignment = Alignment.Bottom) {
                DeckBox(type = state.type, modifier = Modifier.size(84.dp, 100.dp))
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BarajasPalette.Surface)
                        .clickable { soon("Modificar portada") }
                        .padding(horizontal = 22.dp, vertical = 9.dp),
                ) { Text("Modificar", color = BarajasPalette.Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }

        // Sub-paneles Energía / Accesorios / Cartas destacadas.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SubPanel("Energía", Modifier.weight(1f)) { soon("Energía") }
            SubPanel("Accesorios", Modifier.weight(1f)) { soon("Accesorios") }
            SubPanel("Cartas destacadas", Modifier.weight(1f)) { soon("Cartas destacadas") }
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

        // Rejilla del mazo (cartas actuales, solo lectura) + hueco "+".
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            items(state.deckCards.size, key = { state.deckCards[it].id }) { i ->
                CardSlot(imageEs = state.deckCards[i].imageEs) { pickerOpen = true }
            }
            item(key = "add") { CardSlot(imageEs = null) { pickerOpen = true } }
        }

        // Pie: Cancelar / Guardar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
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

/** Hueco de carta del mazo (miniatura o "+"). */
@Composable
private fun CardSlot(imageEs: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(8.dp))
            .background(BarajasPalette.Hollow)
            .border(1.dp, BarajasPalette.HollowBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (imageEs != null) {
            AsyncImage(
                model = imageEs,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Text("＋", color = BarajasPalette.Muted, fontSize = 32.sp, fontWeight = FontWeight.Light)
        }
    }
}
