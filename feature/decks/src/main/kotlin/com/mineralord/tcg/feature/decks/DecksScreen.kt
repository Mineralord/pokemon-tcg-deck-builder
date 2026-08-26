package com.mineralord.tcg.feature.decks

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.LaunchedEffect
import com.mineralord.tcg.data.cards.PrebuiltDecks
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.core.designsystem.TypeEmblem
import com.mineralord.tcg.core.designsystem.typeColor
import com.mineralord.tcg.engine.model.Supertype
import kotlinx.coroutines.launch

/** Máximo de barajas del jugador (mostrado en el chip "n/50"). */
private const val MAX_DECKS = 50

/**
 * "Mis barajas" — réplica del design system claro de TCG Live.
 *
 * Parte A (estructura): cabecera propia (título + Editar + ?), hilo divisor
 * arcoíris, chip "n/25" fijo, fondo claro y rejilla de 2 columnas con la celda
 * "Crear nueva" al inicio. El tile detallado (cinta numerada, arte de caja) y la
 * barra de navegación llegan en las Partes B y C.
 */
@Composable
fun DecksScreen(
    modifier: Modifier = Modifier,
    onHome: () -> Unit = {},
    selectionMode: Boolean = false,
    onSelect: (String) -> Unit = {},
    viewModel: DecksViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var section by remember { mutableStateOf(DeckSection.MINE) }
    var editingDeckId by remember { mutableStateOf<String?>(null) }
    var pickedId by remember { mutableStateOf<String?>(null) }
    var editMode by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    var helpOpen by remember { mutableStateOf(false) }
    var guideOpen by remember { mutableStateOf(false) }
    var videoGuideOpen by remember { mutableStateOf(false) }
    var createMenuOpen by remember { mutableStateOf(false) }
    var infoMsg by remember { mutableStateOf<String?>(null) }
    var expandedGroups by remember { mutableStateOf(setOf(PrebuiltDecks.ACADEMIA_2024)) }

    val gridState = rememberLazyGridState()
    val thematicState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val mineDecks = state.decks.filter { !it.isPrebuilt }
    val thematicDecks = state.decks.filter { it.isPrebuilt }
    val dragging = if (section == DeckSection.MINE) gridState.isScrollInProgress else thematicState.isScrollInProgress

    // Baraja en edición/examen; se conserva durante la animación de salida del editor.
    var lastEditId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(editingDeckId) { if (editingDeckId != null) lastEditId = editingDeckId }
    val editId = editingDeckId ?: lastEditId
    val editingDeck = state.decks.firstOrNull { it.id == editId }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BarajasPalette.BgTop, BarajasPalette.BgBottom))),
        ) {
            Header(
                section = section,
                editMode = editMode,
                selectionMode = selectionMode,
                onToggleSection = {
                    section = if (section == DeckSection.MINE) DeckSection.THEMATIC else DeckSection.MINE
                    editMode = false
                },
                onToggleEdit = { editMode = !editMode },
                onHelp = { helpOpen = true },
            )
            RainbowDivider()

            Box(Modifier.fillMaxSize()) {
                if (state.loading) {
                    CircularProgressIndicator(color = BarajasPalette.NavIcon, modifier = Modifier.align(Alignment.Center))
                } else when (section) {
                    DeckSection.MINE -> {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (!selectionMode) item(key = "create") { CreateDeckTile { createMenuOpen = true } }
                            items(mineDecks.size, key = { mineDecks[it].id }) { i ->
                                val d = mineDecks[i]
                                DeckTile(
                                    deck = d,
                                    number = i + 1,
                                    editMode = editMode,
                                    selected = selectionMode && pickedId == d.id,
                                    onDelete = { deletingId = d.id },
                                    onClick = {
                                        when {
                                            selectionMode -> if (d.playable) pickedId = d.id else infoMsg = "Esta baraja no está lista para jugar (debe tener 60 cartas válidas)."
                                            !editMode -> editingDeckId = d.id
                                        }
                                    },
                                )
                            }
                        }
                        DeckCountChip(
                            count = mineDecks.size, max = MAX_DECKS,
                            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 10.dp),
                        )
                    }
                    DeckSection.THEMATIC -> ThematicContent(
                        decks = thematicDecks,
                        listState = thematicState,
                        expandedGroups = expandedGroups,
                        selectedId = if (selectionMode) pickedId else null,
                        onToggleGroup = { g -> expandedGroups = if (g in expandedGroups) expandedGroups - g else expandedGroups + g },
                        onOpen = { d ->
                            when {
                                selectionMode -> if (d.playable) pickedId = d.id else infoMsg = "Esta baraja no está lista para jugar (te faltan cartas)."
                                else -> editingDeckId = d.id
                            }
                        },
                    )
                }

                // Controles flotantes de scroll (solo Mis barajas, al arrastrar).
                androidx.compose.animation.AnimatedVisibility(
                    visible = dragging && section == DeckSection.MINE,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                ) {
                    ScrollChevrons(
                        onUp = { scope.launch { gridState.animateScrollToItem(0) } },
                        onDown = { scope.launch { gridState.animateScrollToItem(mineDecks.size) } },
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = dragging,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
                ) { HomeFab(onClick = onHome) }

                androidx.compose.animation.AnimatedVisibility(
                    visible = dragging,
                    enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) { BottomNavBar(onHome = onHome) }

                androidx.compose.animation.AnimatedVisibility(
                    visible = editMode && section == DeckSection.MINE,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                ) { SaveButton(onClick = { editMode = false }) }

                // Botón "SELECCIONAR" (cian) del flujo de selección para combate.
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectionMode && pickedId != null,
                    enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 20.dp).navigationBarsPadding(),
                ) {
                    DialogButton(
                        text = "SELECCIONAR",
                        textColor = Color.White,
                        borderColor = Color.Transparent,
                        filled = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { pickedId?.let { viewModel.setActive(it); onSelect(it) } },
                    )
                }
            }
        }

        // Editor / examen con transición premium (entra deslizando suave desde la derecha).
        androidx.compose.animation.AnimatedVisibility(
            visible = editingDeckId != null,
            enter = androidx.compose.animation.slideInHorizontally(
                animationSpec = androidx.compose.animation.core.tween(360),
            ) { it } + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(300),
            ) { it } + androidx.compose.animation.fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            editId?.let { id ->
                DeckEditorScreen(
                    deckId = id,
                    readOnly = editingDeck?.isPrebuilt == true,
                    onDuplicate = {
                        viewModel.duplicateDeck(id)
                        editingDeckId = null
                        section = DeckSection.MINE
                        infoMsg = "Se copió la baraja temática a Mis barajas."
                    },
                    onBack = { editingDeckId = null },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    // Diálogos.
    deletingId?.let { id ->
        DeleteDeckDialog(onCancel = { deletingId = null }, onConfirm = { viewModel.deleteDeck(id); deletingId = null })
    }
    if (helpOpen) {
        HelpMenu(
            onDismiss = { helpOpen = false },
            onVerGuia = { helpOpen = false; guideOpen = true },
            onVideoGuia = { helpOpen = false; videoGuideOpen = true },
        )
    }
    if (guideOpen) GuideCarousel(onClose = { guideOpen = false })
    if (videoGuideOpen) VideoGuide(onClose = { videoGuideOpen = false })
    if (createMenuOpen) {
        CreateMenu(
            onDismiss = { createMenuOpen = false },
            onNueva = { createMenuOpen = false; viewModel.createDeck { id -> editingDeckId = id } },
            onTematica = { createMenuOpen = false; infoMsg = "Crear a partir de baraja temática" },
            onCopia = { createMenuOpen = false; infoMsg = "Crear una copia de una baraja" },
            onEscanear = { createMenuOpen = false; infoMsg = "Escanear código" },
        )
    }
    infoMsg?.let { PlaceholderDialog(title = it, onClose = { infoMsg = null }) }
}

/** Las dos "pantallas" de barajas: propias (banda roja) y temáticas (banda verde). */
private enum class DeckSection { MINE, THEMATIC }

private val HeaderRed = Color(0xFFE0574F)
private val HeaderGreen = Color(0xFF56AF64)
private val SelectCyan = Color(0xFF3FC8E4)

/**
 * Cabecera con banda de color (roja en Mis barajas, verde en Barajas temáticas). El
 * TÍTULO actúa de conmutador entre ambas secciones. En Mis barajas incluye "Editar";
 * en modo edición muestra "Edición en curso" centrado.
 */
@Composable
private fun Header(
    section: DeckSection,
    editMode: Boolean,
    selectionMode: Boolean = false,
    onToggleSection: () -> Unit,
    onToggleEdit: () -> Unit,
    onHelp: () -> Unit,
) {
    val band = if (section == DeckSection.MINE) HeaderRed else HeaderGreen
    Column(Modifier.fillMaxWidth().background(band).statusBarsPadding()) {
        if (editMode && section == DeckSection.MINE) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Edición en curso", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            return@Column
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Título = conmutador de sección.
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onToggleSection).padding(vertical = 2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (section == DeckSection.MINE) "Mis barajas" else "Barajas temáticas",
                        color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, maxLines = 1,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("⇄", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
                Text(
                    when {
                        selectionMode -> "Elige tu baraja y pulsa SELECCIONAR"
                        section == DeckSection.MINE -> "Ver barajas temáticas"
                        else -> "Ver mis barajas"
                    },
                    color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            if (section == DeckSection.MINE && !selectionMode) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(BarajasPalette.Surface)
                        .clickable(onClick = onToggleEdit).padding(horizontal = 20.dp, vertical = 9.dp),
                ) { Text(if (editMode) "Listo" else "Editar", color = BarajasPalette.Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                Spacer(Modifier.size(10.dp))
            }
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(BarajasPalette.Surface).clickable(onClick = onHelp),
                contentAlignment = Alignment.Center,
            ) { Text("?", color = BarajasPalette.Muted, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

/**
 * Contenido de "Barajas temáticas": grupos colapsables por serie/expansión (hoy,
 * Academia de Combate 2024). Cada baraja se puede examinar aunque no se posean sus
 * cartas; al tocarla se abre el examen (reverso para las cartas que faltan).
 */
@Composable
private fun ThematicContent(
    decks: List<DeckUi>,
    listState: LazyListState,
    expandedGroups: Set<String>,
    selectedId: String?,
    onToggleGroup: (String) -> Unit,
    onOpen: (DeckUi) -> Unit,
) {
    val groups = remember(decks) { decks.groupBy { it.folder ?: "Otras" }.toList().sortedBy { it.first } }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        groups.forEach { (folder, list) ->
            val expanded = folder in expandedGroups
            item(key = "hdr-$folder") {
                GroupHeader(title = folder, count = list.size, expanded = expanded, onClick = { onToggleGroup(folder) })
            }
            if (expanded) {
                val rows = list.chunked(2)
                itemsIndexed(rows, key = { ri, _ -> "$folder-row-$ri" }) { ri, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = if (ri == 0) 12.dp else 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        row.forEachIndexed { ci, d ->
                            Box(Modifier.weight(1f)) {
                                DeckTile(deck = d, number = ri * 2 + ci + 1, selected = selectedId == d.id, onClick = { onOpen(d) })
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Cabecera de grupo colapsable (nombre de serie/expansión + conteo + chevron). */
@Composable
private fun GroupHeader(title: String, count: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BarajasPalette.Surface)
            .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("$count", color = BarajasPalette.Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.size(8.dp))
        Text(if (expanded) "▾" else "▸", color = BarajasPalette.Muted, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

/** Hilo divisor arcoíris (azul→lima) bajo la cabecera. */
@Composable
private fun RainbowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Brush.horizontalGradient(BarajasPalette.DividerGradient)),
    )
}

/** Chip "n/25": píldora blanca con icono y conteo de barajas. */
@Composable
private fun DeckCountChip(count: Int, max: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(BarajasPalette.Surface)
            .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🗂", fontSize = 14.sp)
        Spacer(Modifier.size(6.dp))
        Text(
            "$count/$max",
            color = BarajasPalette.Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

/** Celda "Crear nueva": hueca con un "+" grande y label debajo. */
@Composable
private fun CreateDeckTile(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.9f)
                .clip(RoundedCornerShape(18.dp))
                .background(BarajasPalette.Hollow)
                .border(1.dp, BarajasPalette.HollowBorder, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("＋", color = BarajasPalette.Muted, fontSize = 48.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(8.dp))
        Text("Crear nueva", color = BarajasPalette.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Tile de baraja (Parte B): tarjeta blanca con la cinta de acento numerada en la
 * esquina, la "caja" 3D con la carta destacada dentro, el emblema del tipo, una
 * carta secundaria asomando y el nombre en su propio panel inferior. Replica la
 * silueta de TCG Live con arte propio; el color de acento lo elige el jugador
 * (derivado por ahora del tipo dominante de la baraja).
 */
@Composable
private fun DeckTile(
    deck: DeckUi,
    number: Int,
    editMode: Boolean = false,
    selected: Boolean = false,
    onDelete: () -> Unit = {},
    onClick: () -> Unit,
) {
    val accent = typeColor(deck.type)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BarajasPalette.Surface)
            .then(if (selected) Modifier.border(3.dp, SelectCyan, RoundedCornerShape(18.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            DeckBoxArt(deck = deck, accent = accent, modifier = Modifier.fillMaxSize())
            NumberRibbon(
                number = number,
                accent = accent,
                modifier = Modifier.align(Alignment.TopStart),
            )
            // Badge "−" rojo (modo edición): abre la confirmación de borrado.
            if (editMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(BarajasPalette.DeleteRed)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("−", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // Panel del nombre (sub-tarjeta ligeramente hundida).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BarajasPalette.Hollow)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                deck.name.uppercase(),
                color = BarajasPalette.DeckName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Carta destacada de la baraja (para la cara de la caja): 1er Pokémon con arte ES. */
private fun DeckUi.headlinerImage(): String? =
    cards.firstOrNull { it.supertype == Supertype.POKEMON && it.imageEs != null }?.imageEs
        ?: cards.firstOrNull { it.imageEs != null }?.imageEs

/** Segunda carta que asoma delante de la caja (distinta del headliner si es posible). */
private fun DeckUi.secondImage(): String? {
    val imgs = cards.mapNotNull { it.imageEs }
    return imgs.getOrNull(1) ?: imgs.getOrNull(0)
}

/** La "caja" 3D tintada con el acento, con la carta destacada en la cara frontal. */
@Composable
private fun DeckBoxArt(deck: DeckUi, accent: Color, modifier: Modifier = Modifier) {
    val accentLight = lerp(accent, Color.White, 0.28f)
    val accentDark = lerp(accent, Color.Black, 0.30f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Cuerpo de la caja.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.74f)
                .aspectRatio(0.84f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(accentLight, accent))),
        ) {
            // Lomo derecho (efecto 3D).
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.12f)
                    .background(Brush.horizontalGradient(listOf(accent, accentDark))),
            )
            // Carta destacada en la cara frontal.
            val head = deck.headlinerImage()
            if (head != null) {
                AsyncImage(
                    model = head,
                    contentDescription = deck.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp, start = 12.dp, end = 20.dp)
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(4.dp)),
                )
            }
            // Emblema del tipo, abajo-izquierda de la caja.
            TypeEmblem(
                type = deck.type,
                size = 26.dp,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            )
        }
        // Carta secundaria asomando delante, abajo-derecha.
        val second = deck.secondImage()
        if (second != null) {
            AsyncImage(
                model = second,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 10.dp)
                    .fillMaxWidth(0.30f)
                    .aspectRatio(0.72f)
                    .rotate(6f)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.5.dp, Color.White, RoundedCornerShape(4.dp)),
            )
        }
    }
}

/** Botón circular apilado con chevrons ▲▼ para saltar al inicio/fin de la lista. */
@Composable
private fun ScrollChevrons(onUp: () -> Unit, onDown: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BarajasPalette.Surface.copy(alpha = 0.92f))
            .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(50)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⌃", color = BarajasPalette.NavIcon, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onUp).padding(horizontal = 12.dp, vertical = 8.dp))
        Text("⌄", color = BarajasPalette.NavIcon, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onDown).padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/** Botón flotante ↩ que lleva a la pantalla de Inicio (comportamiento de TCG Live). */
@Composable
private fun HomeFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(BarajasPalette.Surface)
            .border(1.dp, BarajasPalette.HairlineBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("↩", color = BarajasPalette.NavIcon, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/** Botón "Guardar" del modo edición: píldora cian con texto blanco. */
@Composable
private fun SaveButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(BarajasPalette.SaveGradient))
            .clickable(onClick = onClick)
            .padding(horizontal = 56.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/**
 * B) Diálogo "Eliminar baraja": "Vale" rojo-outline (destructivo). Aclara que las
 * cartas no se pierden.
 */
@Composable
private fun DeleteDeckDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    ConfirmDialog(
        title = "Eliminar baraja",
        body = "¿Quieres eliminar esta baraja?\nLas cartas permanecerán en tu colección.",
        confirmText = "Vale",
        confirmFilled = false,
        confirmColor = BarajasPalette.DeleteRed,
        onCancel = onCancel,
        onConfirm = onConfirm,
    )
}

/**
 * 1) Menú de ayuda del botón "?": tarjeta anclada arriba-derecha con dos opciones
 * (Ver guía · Guía en vídeo sobre los controles). Se cierra al tocar fuera.
 */
@Composable
private fun HelpMenu(onDismiss: () -> Unit, onVerGuia: () -> Unit, onVideoGuia: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onDismiss,
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 66.dp, end = 16.dp)
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(BarajasPalette.Surface)
                .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(18.dp)),
        ) {
            HelpMenuRow("Ver guía", "🔍", onVerGuia)
            Box(Modifier.fillMaxWidth().height(1.dp).background(BarajasPalette.HairlineBorder))
            HelpMenuRow("Guía en vídeo sobre los controles", "▶", onVideoGuia)
        }
    }
}

@Composable
private fun HelpMenuRow(text: String, icon: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text, color = BarajasPalette.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .border(1.dp, BarajasPalette.HairlineBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(icon, fontSize = 13.sp, color = BarajasPalette.Muted) }
    }
}

/**
 * 2) Menú "Crear" (desde la celda +): fila-cabecera oscura "Crear nueva baraja" +
 * tres opciones sobre panel claro. Cierra al tocar fuera.
 */
@Composable
private fun CreateMenu(
    onDismiss: () -> Unit,
    onNueva: () -> Unit,
    onTematica: () -> Unit,
    onCopia: () -> Unit,
    onEscanear: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onDismiss,
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(BarajasPalette.Surface),
        ) {
            // Fila-cabecera oscura (acción primaria).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BarajasPalette.MenuHeaderBg)
                    .clickable(onClick = onNueva)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Crear nueva baraja", color = Color.White, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("🗂", fontSize = 16.sp)
            }
            CreateMenuRow("Crear a partir de baraja temática", "📋", onTematica)
            CreateMenuRow("Crear una copia de una baraja", "❐", onCopia)
            CreateMenuRow("Escanear código", "▦", onEscanear)
        }
    }
}

@Composable
private fun CreateMenuRow(text: String, icon: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text, color = BarajasPalette.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f))
        Text(icon, fontSize = 16.sp, color = BarajasPalette.Muted)
    }
}

/** Diálogo informativo simple ("Próximamente") para flujos aún no implementados. */
@Composable
private fun PlaceholderDialog(title: String, onClose: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(BarajasPalette.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(24.dp))
                .background(BarajasPalette.Surface)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 18.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text("Próximamente.", color = BarajasPalette.DeckName, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            DialogButton(
                text = "Cerrar",
                textColor = BarajasPalette.Muted,
                borderColor = BarajasPalette.HairlineBorder,
                modifier = Modifier.fillMaxWidth(0.6f),
                onClick = onClose,
            )
        }
    }
}

/** Una diapositiva del tutorial: ilustración estilizada + texto. */
private data class GuideSlide(val caption: String)

private val GUIDE_SLIDES = listOf(
    GuideSlide("En \"Mis barajas\" puedes crear barajas con total libertad para usarlas en las partidas."),
    GuideSlide("Pulsa \"Crear nueva\" para empezar una baraja desde cero o a partir de una temática."),
    GuideSlide("Entra en el modo edición para reordenar tus barajas o eliminar las que no uses."),
    GuideSlide("Cada baraja muestra su carta destacada y un color de acento que puedes personalizar."),
)

/**
 * "Ver guía": carrusel de diapositivas ilustradas con botón Siguiente y puntos de
 * página. La última diapositiva cierra la guía.
 */
@Composable
private fun GuideCarousel(onClose: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val last = page == GUIDE_SLIDES.lastIndex
    Box(
        modifier = Modifier.fillMaxSize().background(BarajasPalette.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(24.dp))
                .background(BarajasPalette.Surface),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Ilustración estilizada (mockup de "Mis barajas").
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .background(Brush.verticalGradient(listOf(BarajasPalette.BgTop, BarajasPalette.Hollow))),
                contentAlignment = Alignment.Center,
            ) {
                Text("🗂", fontSize = 72.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                GUIDE_SLIDES[page].caption,
                color = BarajasPalette.DeckName,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(18.dp))
            // Puntos de página.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GUIDE_SLIDES.indices.forEach { i ->
                    Box(
                        Modifier
                            .size(if (i == page) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i == page) BarajasPalette.NavIcon else BarajasPalette.NavIconInactive),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            DialogButton(
                text = if (last) "Entendido" else "Siguiente",
                textColor = BarajasPalette.Muted,
                borderColor = BarajasPalette.HairlineBorder,
                modifier = Modifier.fillMaxWidth(0.6f),
                onClick = { if (last) onClose() else page++ },
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** "Guía en vídeo sobre los controles": reproductor (placeholder por ahora). */
@Composable
private fun VideoGuide(onClose: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(BarajasPalette.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(24.dp))
                .background(BarajasPalette.Surface),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF2C3846)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center,
                ) { Text("▶", color = Color.White, fontSize = 28.sp) }
            }
            Spacer(Modifier.height(16.dp))
            Text("Guía en vídeo sobre los controles", color = BarajasPalette.Ink,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text("Próximamente.", color = BarajasPalette.DeckName, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            DialogButton(
                text = "Cerrar",
                textColor = BarajasPalette.Muted,
                borderColor = BarajasPalette.HairlineBorder,
                modifier = Modifier.fillMaxWidth(0.6f),
                onClick = onClose,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Barra de navegación inferior con 5 destinos (Inicio activo · Barajas · etc.). */
@Composable
private fun BottomNavBar(onHome: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BarajasPalette.Surface)
            .border(1.dp, BarajasPalette.HairlineBorder)
            .statusBarsPadding()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem("🏠", active = false, onClick = onHome)
        NavItem("🗂", active = true, onClick = {})
        NavItem("🏛", active = false, onClick = {})
        NavItem("⚔", active = false, badge = true, onClick = {})
        NavItem("≡", active = false, onClick = {})
    }
}

@Composable
private fun NavItem(glyph: String, active: Boolean, badge: Boolean = false, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        Text(
            glyph,
            fontSize = 22.sp,
            color = if (active) BarajasPalette.NavIcon else BarajasPalette.NavIconInactive,
            modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
        )
        if (badge) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935)),
            )
        }
    }
}

/** Cinta de acento en la esquina superior izquierda con el número de baraja. */
@Composable
private fun NumberRibbon(number: Int, accent: Color, modifier: Modifier = Modifier) {
    val accentLight = lerp(accent, Color.White, 0.20f)
    Box(modifier = modifier.size(58.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path,
                Brush.linearGradient(
                    listOf(accentLight, accent),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
            )
        }
        Text(
            "%02d".format(number),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 1.dp),
        )
    }
}
