package com.mineralord.tcg.feature.decks

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.core.designsystem.fadingScrollbar

/**
 * Selector de "Cartas destacadas" (réplica de TCG Pocket): popup que entra **desde abajo**,
 * lista las cartas del mazo en rejilla y permite marcar hasta **3**. Las marcadas se
 * **oscurecen** con un número grande centrado (1 = PORTADA, 2 y 3 = laterales). Se cierra
 * tocando el velo o la **X redonda** inferior; al cerrar aplica la selección vía [onApply].
 */
@Composable
internal fun FeaturedPickerSheet(
    cards: List<EditorCardUi>,
    initialSelected: List<String>,
    onApply: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialSelected.take(3)) } }
    val state = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { state.targetState = true }
    LaunchedEffect(state.currentState, state.targetState) {
        if (!state.targetState && !state.currentState) { onApply(selected.toList()); onDismiss() }
    }
    fun close() { state.targetState = false }

    // El botón Atrás cierra con la MISMA vía animada que la X/velo, de modo que
    // [onApply] se dispara y la selección se guarda (si no, se perdería al descartar en seco).
    BackHandler(enabled = state.targetState) { close() }

    fun toggle(id: String) {
        if (id in selected) selected.remove(id)
        else if (selected.size < 3) selected.add(id)
    }

    val gridState = rememberLazyGridState()

    Box(Modifier.fillMaxSize()) {
        // Velo tenue (toca para cerrar).
        Box(
            Modifier.fillMaxSize().background(Color(0x33000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
        )

        AnimatedVisibility(
            visibleState = state,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(BarajasPalette.BgBottom)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                // Cabecera (píldora de título).
                Box(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.shadow(3.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                            .background(BarajasPalette.Surface).padding(horizontal = 22.dp, vertical = 12.dp),
                    ) {
                        Text("Elige las cartas destacadas", color = BarajasPalette.DeckName,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Rejilla de cartas del mazo.
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 70.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().fadingScrollbar(gridState),
                ) {
                    items(cards.size, key = { cards[it].id }) { i ->
                        val c = cards[i]
                        val order = selected.indexOf(c.id)
                        FeaturedCell(imageEs = c.imageEs, order = if (order >= 0) order + 1 else 0) { toggle(c.id) }
                    }
                }

                // X redonda inferior.
                Box(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).navigationBarsPadding()
                        .size(54.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(BarajasPalette.Surface)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
                    contentAlignment = Alignment.Center,
                ) { Text("✕", color = BarajasPalette.Muted, fontSize = 20.sp, fontWeight = FontWeight.Black) }
            }
        }
    }
}

/** Carta del selector: si está destacada, se oscurece y muestra su número (1=portada). */
@Composable
private fun FeaturedCell(imageEs: String?, order: Int, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(6.dp))
            .background(BarajasPalette.Hollow)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        if (imageEs != null) {
            AsyncImage(
                model = imageEs, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                    .alpha(if (order > 0) 0.45f else 1f),
            )
        }
        if (order > 0) {
            Text(
                "$order", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
