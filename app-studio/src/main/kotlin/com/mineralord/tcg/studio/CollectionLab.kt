package com.mineralord.tcg.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.CardDetailDialog
import com.mineralord.tcg.core.designsystem.HoloCardImage
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.cards.StarterDecks
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.model.Ability
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import com.mineralord.tcg.engine.model.Rarity
import com.mineralord.tcg.engine.model.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// COLECCIÓN DE CARTAS — Lab del Studio. Réplica 1:1 de la colección de Pokémon TCG
// Live ("Mis cartas"), según el vídeo de referencia COLECCION TCG POCKET.mp4. Estética
// NEUMÓRFICA clara (paneles blancos con sombra suave, línea arcoíris fina, acentos azul/verde).
//
// Flujo replicado: cabecera "Mis cartas" · toolbar (contador · interruptor rejilla↔agrupado ·
// buscar) · dos modos (rejilla continua / agrupado por expansión con progreso) · huecos numerados
// para cartas no obtenidas · hoja de ORDENAR · hoja de DETALLE (copias, "Obtener efecto visual",
// tabla de datos, cartas relacionadas, ¡La quiero!, variantes de idioma, tilt inmersivo).
//
// Aquí se ITERA y se valida el look; una vez aprobado, se hace canon en el juego (app:CollectionScreen).
// ─────────────────────────────────────────────────────────────────────────────

// Paleta neumórfica clara (local al Lab; al hacer canon se moverán a tokens del designsystem).
private val BgTop = Color(0xFFEDF1F8)
private val BgBottom = Color(0xFFDBE3EF)
private val Panel = Color(0xFFFFFFFF)
private val Ink = Color(0xFF2B3346)
private val Muted = Color(0xFF8A93A6)
private val Accent = Color(0xFF35C4E8)          // cian TCG Live (botón Buscar / obtener efecto)
private val AccentBlue = Color(0xFF3D6CF0)
private val ToggleOn = Color(0xFF19D08B)         // verde del interruptor
private val SlotEmpty = Color(0x0F2B3346)        // hueco de carta no obtenida (translúcido)
private val SheetScrim = Color(0x66000000)

/** Un hueco de la colección: una carta del catálogo, poseída o no. Conserva el [Card] para el detalle. */
private data class DexCard(
    val card: Card,
    val number: Int,
    val name: String,
    val rarity: Rarity,
    val owned: Boolean,
    val count: Int,
    val cap: Int,
    val setCode: String,
    val imageEs: String?,
    val imageLarge: String,
)

/** Una expansión con su progreso (como las tarjetas del modo agrupado). */
private data class DexSet(
    val code: String,
    val name: String,
    val series: String,
    val cards: List<DexCard>,
) {
    val owned: Int get() = cards.count { it.owned }
    val total: Int get() = cards.size
    /** Estrellas: obtenidas de rareza secreta (Ultra+). */
    val stars: Int get() = cards.count { it.owned && it.rarity.ordinal >= Rarity.ULTRA_RARE.ordinal }
    /** Brillantes (✵): obtenidas Hyper/dorada. */
    val shiny: Int get() = cards.count { it.owned && it.rarity == Rarity.HYPER_RARE }
    val complete: Boolean get() = total > 0 && owned >= total
}

private enum class DexSort(val label: String, val glyph: String) {
    NUMBER("Por n.º de carta coleccionable", "▤"),
    TYPE("Por tipo", "✳"),
    RARITY("Por rareza", "◇"),
    FECHA_CARTAS("Por fecha de obtención de cartas", "🕘"),
    COUNT("Por cantidad de cartas", "▥"),
    EXPANSION("Por expansión", "▦"),
    FECHA_EFECTOS("Por fecha de obtención de efectos visuales", "✦"),
    FAVORITES("Por favoritos", "★"),
}

/** Filtro del menú "Buscar" (lupa). Idéntico en ambas vistas. `active` = hay algún criterio. */
private data class DexFilter(
    val query: String = "",
    val onlyFav: Boolean = false,
    val onlyNotFav: Boolean = false,
    val onlyWish: Boolean = false,
    val onlyNotWish: Boolean = false,
    val rarities: Set<Rarity> = emptySet(),
    val types: Set<EnergyType> = emptySet(),
    val withAbility: Boolean = false,
    val withoutAbility: Boolean = false,
    val ex: Boolean = false,
    val sets: Set<String> = emptySet(),
    val hpMin: Int? = null,
    val hpMax: Int? = null,
    val dmgMin: Int? = null,
    val dmgMax: Int? = null,
    val trainerCats: Set<TrainerCat> = emptySet(),
    /** Idioma (como TCG Live, 9). Visual: el dataset del Studio es monolingüe → no filtra resultados. */
    val languages: Set<String> = emptySet(),
) {
    val active: Boolean
        get() = query.isNotBlank() || onlyFav || onlyNotFav || onlyWish || onlyNotWish ||
            rarities.isNotEmpty() || types.isNotEmpty() || withAbility || withoutAbility || ex || sets.isNotEmpty() ||
            hpMin != null || hpMax != null || dmgMin != null || dmgMax != null ||
            trainerCats.isNotEmpty() || languages.isNotEmpty()
}

/** Categoría de Carta de Entrenador para el filtro (TCG Live: Objeto/Herramienta/Partidario/Estadio). */
private enum class TrainerCat(val label: String) {
    ITEM("Objeto"), TOOL("Herramienta"), SUPPORTER("Partidario"), STADIUM("Estadio");

    fun matches(k: TrainerKind): Boolean = when (this) {
        ITEM -> k is TrainerKind.Item
        TOOL -> k is TrainerKind.Tool
        SUPPORTER -> k is TrainerKind.Supporter
        STADIUM -> k is TrainerKind.Stadium
    }
}

/** Idiomas de TCG Live (visual; el dataset del Studio no distingue idioma). */
private val LANGUAGES = listOf(
    "Español", "Inglés", "Alemán", "Francés", "Italiano", "Portugués", "Japonés", "Coreano", "Chino",
)

/** Un conjunto de filtros guardado por el usuario (TCG Live: "Conjuntos de filtros"). Sesión en memoria. */
private data class SavedFilterSet(val name: String, val filter: DexFilter)

/** Daño máximo IMPRESO (mayor `baseDamage` fijo entre los ataques). 0 si no tiene ataques con daño. */
private fun PokemonCard.maxPrintedDamage(): Int =
    attacks.maxOfOrNull { (it.baseDamage as? Damage.Fixed)?.value ?: 0 } ?: 0

/** Orden de expansiones: MÁS NUEVAS ARRIBA → más viejas abajo (por fecha de lanzamiento),
 *  con promos y energías al final. sv4 (nov'23) ▸ 151/sv3pt5 (sep'23) ▸ sv3 (ago'23) ▸ sv2 ▸ sv1. */
private val ORDERED_SETS = listOf("sv4", "sv3pt5", "sv3", "sv2", "sv1", "svp", "energy")

/** Logo HD oficial EN ESPAÑOL por expansión (fuente WikiDex). null → sin logo → fallback a nombre arcoíris. */
private fun setLogoRes(code: String): Int? = when (code) {
    "sv1" -> R.drawable.set_sv1_logo          // Escarlata y Púrpura
    "sv2" -> R.drawable.set_sv2_logo          // Evoluciones en Paldea
    "sv3" -> R.drawable.set_sv3_logo          // Llamas Obsidianas
    "sv3pt5" -> R.drawable.set_sv3pt5_logo    // 151
    "sv4" -> R.drawable.set_sv4_logo          // Brecha Paradójica
    else -> null                              // svp (promos) / energy → nombre arcoíris
}

/** Paleta arcoíris (firma visual TCG Live) para el relleno del nombre cuando no hay logo. */
private val RAINBOW = listOf(
    Color(0xFFFF4D6D), Color(0xFFFF9E3D), Color(0xFFFFE14D),
    Color(0xFF35D07F), Color(0xFF35C4E8), Color(0xFF6C7BFF), Color(0xFFB05CF0),
)

/** Sub-variantes por expansión = artes de sobre (como el selector de TCG Live). Visual: no mapea a datos
 *  de carta (el dataset no distingue arte de sobre), por eso la selección es local al selector. 151 lleva
 *  los 3 iniciales de Kanto en sus sobres. Rellenar el resto cuando haya datos reales por expansión. */
private val EXPANSION_VARIANTS: Map<String, List<String>> = mapOf(
    "sv3pt5" to listOf("Bulbasaur", "Charmander", "Squirtle"),
)

/** Logo HD de la expansión; si no hay asset, el nombre con relleno arcoíris. */
@Composable
private fun ExpansionLogo(set: DexSet, modifier: Modifier = Modifier, maxHeight: Dp = 52.dp) {
    val res = setLogoRes(set.code)
    if (res != null) {
        Image(
            painter = painterResource(res), contentDescription = set.name,
            contentScale = ContentScale.Fit, modifier = modifier.heightIn(max = maxHeight),
        )
    } else {
        BasicText(
            text = set.name.uppercase(),
            style = TextStyle(
                brush = Brush.horizontalGradient(RAINBOW),
                fontSize = 15.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
            ),
            modifier = modifier,
        )
    }
}

private fun setCodeOf(id: String): String =
    if (id.startsWith("energy")) "energy" else id.substringBeforeLast('-')

private fun numberOf(id: String): Int = id.substringAfterLast('-').toIntOrNull() ?: 0

// ─────────────────────────────────────────────────────────────────────────────
// Carga de datos (colección REAL del perfil del Studio + catálogo completo).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun rememberDexSets(): List<DexSet>? {
    val context = LocalContext.current
    var sets by remember { mutableStateOf<List<DexSet>?>(null) }
    var catalog by remember { mutableStateOf<Map<String, List<Card>>?>(null) }
    var owned by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val repo = withContext(Dispatchers.Default) { CardRepository.load() }
        catalog = withContext(Dispatchers.Default) { repo.all.groupBy { setCodeOf(it.id.raw) } }
        val profile = ProfileRepository(context)
        profile.seedOnce(StarterDecks.ALL.flatMap { it.expandedCardIds() }.map { it.raw })
        profile.profile.collectLatest { owned = it.owned }
    }

    LaunchedEffect(catalog, owned) {
        val cat = catalog ?: return@LaunchedEffect
        sets = withContext(Dispatchers.Default) {
            ORDERED_SETS.mapNotNull { code ->
                val cards = cat[code].orEmpty()
                if (cards.isEmpty()) return@mapNotNull null
                val sample = cards.first()
                DexSet(
                    code = code,
                    name = sample.set.name.es,
                    series = sample.set.series,
                    cards = cards.sortedBy { numberOf(it.id.raw) }.map { c ->
                        val copies = owned[c.id.raw] ?: 0
                        DexCard(
                            card = c,
                            number = numberOf(c.id.raw),
                            name = c.name.es,
                            rarity = c.rarity,
                            owned = copies > 0,
                            count = copies,
                            cap = ProfileRepository.capFor(c.id.raw),
                            setCode = setCodeOf(c.id.raw),
                            imageEs = c.artwork.smallEs,
                            imageLarge = c.artwork.large(spanish = true),
                        )
                    },
                )
            }
        }
    }
    return sets
}

// ─────────────────────────────────────────────────────────────────────────────
// Entry point del Lab: "Mis cartas".
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CollectionLabContent(
    onEnterFullscreen: (@Composable () -> Unit) -> Unit,
    onExitFullscreen: () -> Unit,
) {
    // La colección debe verse A PANTALLA COMPLETA (sin cromo del Shell), como el Modo Partida y el
    // Simulador de Sobres. Entra automáticamente al abrir el Lab; también hay botón por si se sale.
    val open = { onEnterFullscreen { CollectionScreen(onExit = onExitFullscreen) } }
    LaunchedEffect(Unit) { open() }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgTop, BgBottom))), contentAlignment = Alignment.Center) {
        Box(
            Modifier.shadow(4.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(Panel)
                .noRippleClick { open() }.padding(horizontal = 20.dp, vertical = 14.dp),
        ) { Txt("⤢  Abrir colección (pantalla completa)", 14.sp, Ink, FontWeight.Black) }
    }
}

/** Pantalla "Mis cartas" a pantalla completa. */
@Composable
private fun CollectionScreen(onExit: () -> Unit) {
    val sets = rememberDexSets()
    var grouped by remember { mutableStateOf(false) }           // interruptor rejilla ↔ agrupado
    var showAll by remember { mutableStateOf(true) }            // "Mostrar todo" (huecos vacíos en vista agrupada)
    var sort by remember { mutableStateOf(DexSort.NUMBER) }
    var sortAsc by remember { mutableStateOf(true) }
    var showSort by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(DexFilter()) }
    val savedSets = remember { mutableStateListOf<SavedFilterSet>() }
    var detail by remember { mutableStateOf<DexCard?>(null) }
    val favorites = remember { mutableStateListOf<String>() }
    val wishlist = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val flatState = rememberLazyGridState()
    val groupedState = rememberLazyListState()

    // Comportamiento del desplazamiento (como TCG Live):
    //  · Contador: se oculta MIENTRAS se desplaza y reaparece al soltar.
    //  · "Mostrar todo": se oculta al deslizar hacia ABAJO y reaparece al deslizar hacia ARRIBA.
    val scrolling = if (grouped) groupedState.isScrollInProgress else flatState.isScrollInProgress
    var toggleVisible by remember { mutableStateOf(true) }
    val nested = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -3f) toggleVisible = false      // desliza contenido hacia arriba (scroll down)
                else if (available.y > 3f) toggleVisible = true    // desliza contenido hacia abajo (scroll up)
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgTop, BgBottom))),
    ) {
        if (sets == null) {
            Txt("Cargando colección…", 14.sp, Muted, FontWeight.SemiBold, Modifier.align(Alignment.Center))
            return@Box
        }
        val totalOwned = remember(sets) { sets.sumOf { s -> s.cards.sumOf { it.count } } }

        Column(Modifier.fillMaxSize()) {
            CollectionHeader(onExit = onExit)
            RainbowRule()
            Toolbar(
                totalOwned = totalOwned, grouped = grouped, counterVisible = !scrolling,
                onToggle = { grouped = it }, onSearch = { showSearch = true },
            )
            // "Mostrar todo" solo en la vista agrupada; se oculta/muestra según la dirección del scroll.
            if (grouped) {
                AnimatedVisibility(
                    visible = toggleVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) { MostrarTodoRow(showAll, onChange = { showAll = it }) }
            }
            Box(Modifier.fillMaxSize().nestedScroll(nested)) {
                if (grouped) {
                    GroupedView(sets, favorites, wishlist, filter, showAll, sort, sortAsc, groupedState, onOpen = { detail = it })
                } else {
                    FlatView(sets, favorites, wishlist, filter, sort, sortAsc, flatState, onOpen = { detail = it })
                }
                // Botón flotante inferior-derecho = ORDENAR (como TCG Live). Sus opciones cambian con la vista.
                OrdenarButton(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    onClick = { showSort = true },
                )
            }
        }

        if (showSort) {
            SortSheet(
                current = sort, asc = sortAsc, grouped = grouped,
                onPick = { s -> if (s == sort) sortAsc = !sortAsc else { sort = s; sortAsc = true } },
                onDismiss = { showSort = false },
            )
        }
        if (showSearch) {
            SearchSheet(
                sets = sets, filter = filter, savedSets = savedSets,
                onApply = { filter = it; showSearch = false },
                onSaveSet = { name, f ->
                    savedSets.removeAll { it.name == name }
                    savedSets.add(SavedFilterSet(name, f))
                },
                onDeleteSet = { name -> savedSets.removeAll { it.name == name } },
                onDismiss = { showSearch = false },
            )
        }
        detail?.let { card ->
            val allCards = remember(sets) { sets.flatMap { it.cards } }
            CardDetailSheet(
                card = card,
                allCards = allCards,
                isFavorite = card.card.id.raw in favorites,
                isWished = card.card.id.raw in wishlist,
                onToggleFavorite = { toggle(favorites, card.card.id.raw) },
                onToggleWish = { toggle(wishlist, card.card.id.raw) },
                onOpenRelated = { detail = it },
                onDismiss = { detail = null },
            )
        }
    }
}

private fun toggle(list: MutableList<String>, id: String) { if (id in list) list.remove(id) else list.add(id) }

/** Cabecera: salir de pantalla completa (izq) · título "Mis cartas" centrado · ayuda (der). */
@Composable
private fun CollectionHeader(onExit: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
        Txt("Mis cartas", 22.sp, Ink, FontWeight.Black)
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = 16.dp).size(30.dp)
                .shadow(2.dp, CircleShape).clip(CircleShape).background(Panel).noRippleClick(onExit),
            contentAlignment = Alignment.Center,
        ) { Txt("⤢", 15.sp, Muted, FontWeight.Black) }
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(30.dp)
                .shadow(2.dp, CircleShape).clip(CircleShape).background(Panel),
            contentAlignment = Alignment.Center,
        ) { Txt("?", 15.sp, Muted, FontWeight.Black) }
    }
}

/** Línea arcoíris fina bajo la cabecera (firma visual de TCG Live). */
@Composable
private fun RainbowRule() {
    Box(
        Modifier.fillMaxWidth().height(3.dp).background(
            Brush.horizontalGradient(
                listOf(Color(0xFFFF5D5D), Color(0xFFFFC53D), Color(0xFF57C06B), Color(0xFF35C4E8), Color(0xFF7E7BFF), Color(0xFFEC5F94)),
            ),
        ),
    )
}

/** Toolbar flotante: contador · icono álbum+pokébola · interruptor rejilla↔agrupado · lupa (Buscar). */
@Composable
private fun Toolbar(
    totalOwned: Int, grouped: Boolean, counterVisible: Boolean,
    onToggle: (Boolean) -> Unit, onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Panel)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Contador total de cartas — se oculta al desplazar y reaparece al soltar.
        AnimatedVisibility(visible = counterVisible, enter = fadeIn(), exit = fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).border(1.5.dp, Muted, RoundedCornerShape(3.dp)))
                Txt("$totalOwned", 16.sp, Ink, FontWeight.Black)
            }
        }
        Spacer(Modifier.weight(1f))
        // Icono "álbum de cartas con pokébola" (extraído del original de TCG Live) — junto al interruptor.
        AlbumPokeballIcon(Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        GreenSwitch(on = grouped, onChange = onToggle)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.width(1.dp).height(22.dp).background(Color(0x1A000000)))
        Spacer(Modifier.width(14.dp))
        // Lupa = Buscar (menú de filtros), separada del Ordenar (botón flotante inferior).
        Box(Modifier.size(24.dp).noRippleClick(onSearch), contentAlignment = Alignment.Center) {
            Txt("⌕", 22.sp, Ink, FontWeight.Black)
        }
    }
}

/** "Mostrar todo": muestra/oculta los huecos numerados de cartas no obtenidas (solo vista agrupada). */
@Composable
private fun MostrarTodoRow(on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.shadow(3.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Txt("Mostrar todo", 13.sp, Ink, FontWeight.Black)
            GreenSwitch(on = on, onChange = onChange)
        }
    }
}

/** Icono de álbum/pila de cartas con una pokébola al frente (recreado como vector nítido). */
@Composable
private fun AlbumPokeballIcon(modifier: Modifier, tint: Color = Ink) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
        val line = tint
        // Cartas traseras desplazadas (dan sensación de pila/álbum).
        val cardW = w * 0.60f; val cardH = h * 0.72f
        val r = androidx.compose.ui.geometry.CornerRadius(w * 0.10f, w * 0.10f)
        drawRoundRect(
            color = line, topLeft = Offset(w * 0.34f, h * 0.10f), size = androidx.compose.ui.geometry.Size(cardW, cardH),
            cornerRadius = r, style = stroke,
        )
        drawRoundRect(
            color = line, topLeft = Offset(w * 0.20f, h * 0.16f), size = androidx.compose.ui.geometry.Size(cardW, cardH),
            cornerRadius = r, style = stroke,
        )
        // Carta frontal con la pokébola.
        val fx = w * 0.06f; val fy = h * 0.22f
        drawRoundRect(
            color = line, topLeft = Offset(fx, fy), size = androidx.compose.ui.geometry.Size(cardW, cardH),
            cornerRadius = r, style = stroke,
        )
        // Pokébola centrada en la carta frontal.
        val cx = fx + cardW / 2f; val cy = fy + cardH / 2f; val pr = cardW * 0.30f
        drawCircle(line, radius = pr, center = Offset(cx, cy), style = stroke)
        drawLine(line, Offset(cx - pr, cy), Offset(cx + pr, cy), strokeWidth = w * 0.06f)
        drawCircle(line, radius = pr * 0.34f, center = Offset(cx, cy), style = Stroke(width = w * 0.06f))
    }
}

/** Interruptor verde estilo TCG Live. */
@Composable
private fun GreenSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    val t by animateFloatAsState(if (on) 1f else 0f, tween(180), label = "switch")
    val track by animateColorAsState(if (on) ToggleOn else Color(0xFFCBD3E0), tween(180), label = "track")
    Box(
        Modifier.width(44.dp).height(26.dp).clip(RoundedCornerShape(50)).background(track)
            .noRippleClick { onChange(!on) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(horizontal = 3.dp).graphicsLayer { translationX = t * 18.dp.toPx() }
                .size(20.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(Color.White),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MODO REJILLA (todas las cartas en una rejilla continua de 3 columnas).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FlatView(sets: List<DexSet>, favorites: List<String>, wishlist: List<String>, filter: DexFilter, sort: DexSort, asc: Boolean, state: LazyGridState, onOpen: (DexCard) -> Unit) {
    // Modo rejilla continua (interruptor en BLANCO): como TCG Pocket, SIN huecos vacíos —
    // solo se muestran las cartas realmente obtenidas (y que pasen el filtro de Buscar).
    val cards = remember(sets, sort, asc, favorites.size, wishlist.size, filter) {
        val base = sets.flatMap { it.cards }.filter { it.owned }
        sortCards(applyFilter(base, filter, favorites, wishlist), sort, asc, favorites)
    }
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        gridItems(cards, key = { it.card.id.raw }) { c -> GridSlot(c, onClick = { onOpen(c) }) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MODO AGRUPADO (tarjetas de expansión con progreso; se expanden a su rejilla).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GroupedView(
    sets: List<DexSet>, favorites: List<String>, wishlist: List<String>, filter: DexFilter, showAll: Boolean,
    sort: DexSort, asc: Boolean, state: LazyListState, onOpen: (DexCard) -> Unit,
) {
    val expanded = remember { mutableStateListOf<String>() }
    // Si hay filtro activo, oculta expansiones sin resultados. La dirección ↑/↓ (asc) invierte el
    // orden de expansiones: por defecto NUEVAS arriba (ORDERED_SETS); al invertir, VIEJAS arriba.
    val shownSets = remember(sets, filter, asc) {
        val base = if (!filter.active) sets else sets.filter { s -> applyFilter(s.cards, filter, favorites, wishlist).isNotEmpty() }
        if (asc) base else base.reversed()
    }
    androidx.compose.foundation.lazy.LazyColumn(
        state = state,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(shownSets, key = { it.code }) { set ->
            ExpansionCard(set, isExpanded = set.code in expanded, onToggle = { toggle(expanded, set.code) })
            if (set.code in expanded) {
                Spacer(Modifier.height(10.dp))
                // "Mostrar todo" OFF → sólo cartas obtenidas (sin huecos); ON → todas (con huecos numerados).
                val filtered = applyFilter(set.cards, filter, favorites, wishlist)
                val visible = if (showAll) filtered else filtered.filter { it.owned }
                val cards = sortCards(visible, sort, asc, favorites)
                // Rejilla embebida de 5 columnas (idéntica a TCG Pocket). Altura acotada por filas.
                val rows = (cards.size + 4) / 5
                Box(Modifier.fillMaxWidth().height((rows * 98).dp.coerceAtMost(6000.dp))) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false,
                        modifier = Modifier.fillMaxSize(),
                    ) { gridItems(cards, key = { it.card.id.raw }) { c -> GridSlot(c, onClick = { onOpen(c) }) } }
                }
            }
        }
    }
}

/** Tarjeta de expansión: nombre estilizado + progreso ◇ x/y · ⭐ estrellas · ✵ brillantes + chevron. */
@Composable
private fun ExpansionCard(set: DexSet, isExpanded: Boolean, onToggle: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp))
            .background(Panel).noRippleClick(onToggle).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            ExpansionLogo(set, Modifier.weight(6f, fill = false))
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(30.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(BgTop),
                contentAlignment = Alignment.Center,
            ) { Txt(if (isExpanded) "▲" else "▼", 10.sp, Muted, FontWeight.Black) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatPill("◇", "${set.owned}/${set.total}", set.complete)
            if (set.stars > 0) StatPill("★", "${set.stars}", false, Color(0xFFE7B10A))
            if (set.shiny > 0) StatPill("✵", "${set.shiny}", false, Color(0xFFEC5F94))
        }
        if (set.complete) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0x1419D08B)).padding(horizontal = 12.dp, vertical = 3.dp)) {
                Txt("◇ ¡Completada!", 11.sp, ToggleOn, FontWeight.Black)
            }
        }
    }
}

@Composable
private fun StatPill(glyph: String, value: String, complete: Boolean, glyphColor: Color = Muted) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(BgTop).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Txt(glyph, 12.sp, glyphColor, FontWeight.Black)
        Txt(value, 13.sp, if (complete) ToggleOn else Ink, FontWeight.Black)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Slot de rejilla: obtenida = arte real con holo; no obtenida = hueco translúcido con número.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GridSlot(card: DexCard, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(0.72f).noRippleClick(onClick), contentAlignment = Alignment.Center) {
        if (card.owned) {
            Box(Modifier.fillMaxSize().shadow(5.dp, RoundedCornerShape(8.dp), clip = false).clip(RoundedCornerShape(8.dp))) {
                HoloCardImage(
                    imageUrl = card.imageEs ?: card.imageLarge,
                    setCode = card.setCode, cardNumber = card.number, rarity = card.rarity,
                    contentDescription = card.name, intensity = 0.8f, modifier = Modifier.fillMaxSize(),
                )
            }
            if (card.count > 1) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(7.dp))
                        .background(Color(0xE6121B2B)).padding(horizontal = 6.dp, vertical = 1.dp),
                ) { Txt("×${card.count}", 10.sp, Color.White, FontWeight.Black) }
            }
        } else {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(SlotEmpty)
                    .border(1.dp, Color(0x14000000), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Txt("%03d".format(card.number), 17.sp, Color(0x332B3346), FontWeight.Black) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hoja de ORDENAR.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SortSheet(current: DexSort, asc: Boolean, grouped: Boolean, onPick: (DexSort) -> Unit, onDismiss: () -> Unit) {
    // Las opciones cambian según el interruptor (como TCG Live):
    //  · Agrupado (verde): SOLO "Por expansión" (la flecha ↑/↓ invierte nuevas↔viejas).
    //  · Rejilla (blanco): lista completa.
    val options = if (grouped) listOf(DexSort.EXPANSION) else DexSort.values().toList()
    Box(Modifier.fillMaxSize().background(SheetScrim).noRippleClick(onDismiss), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(BgTop)
                .noRippleClick { }.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Txt("Ordenar", 18.sp, Ink, FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.width(40.dp).height(2.dp).background(Color(0x22000000)))
            Spacer(Modifier.height(10.dp))
            options.forEach { s ->
                val active = s == current
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .then(if (active) Modifier.shadow(3.dp, RoundedCornerShape(50)) else Modifier)
                        .clip(RoundedCornerShape(50)).background(if (active) Ink else Color.Transparent)
                        .noRippleClick { onPick(s) }.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt(s.label, 14.sp, if (active) Color.White else Ink, if (active) FontWeight.Black else FontWeight.SemiBold, Modifier.weight(1f), align = TextAlign.Start)
                    if (active) Txt(if (asc) "↑" else "↓", 16.sp, Color.White, FontWeight.Black)
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(48.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Panel).noRippleClick(onDismiss), contentAlignment = Alignment.Center) {
                Txt("✕", 18.sp, Muted, FontWeight.Black)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Menú BUSCAR (lupa). Idéntico en ambas vistas. Edita un borrador y aplica con "Buscar".
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchSheet(
    sets: List<DexSet>,
    filter: DexFilter,
    savedSets: List<SavedFilterSet>,
    onApply: (DexFilter) -> Unit,
    onSaveSet: (String, DexFilter) -> Unit,
    onDeleteSet: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(filter) }
    var showExpSel by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgTop, BgBottom)))) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 120.dp),
        ) {
            // Campo de búsqueda.
            Row(
                Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Txt("⌕", 18.sp, Muted, FontWeight.Black)
                Box(Modifier.weight(1f)) {
                    if (draft.query.isEmpty()) Txt("Buscar por nombre…", 14.sp, Muted, FontWeight.SemiBold)
                    BasicTextField(
                        value = draft.query, onValueChange = { draft = draft.copy(query = it) },
                        singleLine = true, textStyle = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Section("Favoritas")
            ChipRow {
                Chip("Favoritas", draft.onlyFav) { draft = draft.copy(onlyFav = !draft.onlyFav, onlyNotFav = false) }
                Chip("No favoritas", draft.onlyNotFav) { draft = draft.copy(onlyNotFav = !draft.onlyNotFav, onlyFav = false) }
            }

            Section("Mi lista de deseadas")
            ChipRow {
                Chip("Solo cartas de la lista", draft.onlyWish) { draft = draft.copy(onlyWish = !draft.onlyWish, onlyNotWish = false) }
                Chip("Cartas fuera de la lista", draft.onlyNotWish) { draft = draft.copy(onlyNotWish = !draft.onlyNotWish, onlyWish = false) }
            }

            SectionWithAll("Rareza", allOn = draft.rarities.size == Rarity.values().size,
                onAll = { on -> draft = draft.copy(rarities = if (on) Rarity.values().toSet() else emptySet()) })
            FlowChips {
                Rarity.values().forEach { r ->
                    val sel = r in draft.rarities
                    Row(
                        Modifier.shadow(if (sel) 3.dp else 1.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                            .background(if (sel) Ink else Panel).noRippleClick { draft = draft.copy(rarities = draft.rarities.toggleSet(r)) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RaritySymbol(r, height = 15.dp)
                        Txt(rarityEs(r), 12.sp, if (sel) Color.White else Ink, FontWeight.Bold)
                    }
                }
            }

            Section("Pokémon · Tipo")
            SectionWithAll("Tipo", allOn = draft.types.size == 10,
                onAll = { on -> draft = draft.copy(types = if (on) POKE_TYPES.toSet() else emptySet()) })
            FlowChips {
                POKE_TYPES.forEach { t ->
                    Chip(energyEs(t), t in draft.types) { draft = draft.copy(types = draft.types.toggleSet(t)) }
                }
            }

            Section("Otros")
            FlowChips {
                Chip("Con habilidad", draft.withAbility) { draft = draft.copy(withAbility = !draft.withAbility, withoutAbility = false) }
                Chip("Sin habilidad", draft.withoutAbility) { draft = draft.copy(withoutAbility = !draft.withoutAbility, withAbility = false) }
                Chip("Pokémon ex", draft.ex) { draft = draft.copy(ex = !draft.ex) }
            }

            Section("PS")
            RangeRow(
                min = draft.hpMin, max = draft.hpMax,
                onMin = { draft = draft.copy(hpMin = it) }, onMax = { draft = draft.copy(hpMax = it) },
            )

            Section("Daño de ataque")
            RangeRow(
                min = draft.dmgMin, max = draft.dmgMax,
                onMin = { draft = draft.copy(dmgMin = it) }, onMax = { draft = draft.copy(dmgMax = it) },
            )

            Section("Carta de Entrenador")
            FlowChips {
                TrainerCat.values().forEach { cat ->
                    Chip(cat.label, cat in draft.trainerCats) { draft = draft.copy(trainerCats = draft.trainerCats.toggleSet(cat)) }
                }
            }

            SectionWithAll("Idioma", allOn = draft.languages.size == LANGUAGES.size,
                onAll = { on -> draft = draft.copy(languages = if (on) LANGUAGES.toSet() else emptySet()) })
            FlowChips {
                LANGUAGES.forEach { lang ->
                    Chip(lang, lang in draft.languages) { draft = draft.copy(languages = draft.languages.toggleSet(lang)) }
                }
            }

            Section("Expansiones")
            Row(
                Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                    .background(if (draft.sets.isEmpty()) Panel else Ink).noRippleClick { showExpSel = true }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(
                    if (draft.sets.isEmpty()) "Sin seleccionar" else "Selección efectuada",
                    14.sp, if (draft.sets.isEmpty()) Ink else Color.White, FontWeight.Black, Modifier.weight(1f), align = TextAlign.Center,
                )
                Txt("›", 18.sp, if (draft.sets.isEmpty()) Muted else Color.White, FontWeight.Black)
            }

            Section("Conjuntos de filtros")
            // Guardados existentes: tocar aplica; ✕ elimina.
            if (savedSets.isEmpty()) {
                Txt("Guarda el filtro actual para reutilizarlo.", 12.sp, Muted, FontWeight.SemiBold, Modifier.padding(bottom = 4.dp), align = TextAlign.Start)
            } else {
                FlowChips {
                    savedSets.forEach { s ->
                        Row(
                            Modifier.shadow(1.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
                                .noRippleClick { draft = s.filter }.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Txt(s.name, 13.sp, Ink, FontWeight.Bold)
                            Txt("✕", 13.sp, Muted, FontWeight.Black, Modifier.noRippleClick { onDeleteSet(s.name) })
                        }
                    }
                }
            }
            // Guardar el filtro actual con un nombre.
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.weight(1f).shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (saveName.isEmpty()) Txt("Nombre del conjunto…", 13.sp, Muted, FontWeight.SemiBold)
                        BasicTextField(
                            value = saveName, onValueChange = { saveName = it }, singleLine = true,
                            textStyle = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                val canSave = saveName.isNotBlank() && draft.active
                Box(
                    Modifier.shadow(if (canSave) 3.dp else 0.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                        .background(if (canSave) ToggleOn else SlotEmpty)
                        .noRippleClick { if (canSave) { onSaveSet(saveName.trim(), draft); saveName = "" } }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Txt("Guardar", 13.sp, if (canSave) Color.White else Muted, FontWeight.Black) }
            }
        }

        // Barra inferior fija: Buscar (centro) · ✕ (izq) · Restablecer (der).
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(48.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Panel).noRippleClick(onDismiss), contentAlignment = Alignment.Center) {
                Txt("✕", 18.sp, Muted, FontWeight.Black)
            }
            Box(
                Modifier.weight(1f).shadow(4.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF4FD3F2), Accent))).noRippleClick { onApply(draft) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Txt("Buscar", 15.sp, Color.White, FontWeight.Black) }
            Box(
                Modifier.shadow(3.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
                    .noRippleClick { draft = DexFilter() }.padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Txt("Restablecer", 13.sp, Muted, FontWeight.Black) }
        }

        if (showExpSel) {
            ExpansionSelectorSheet(
                sets = sets, selected = draft.sets,
                onChange = { draft = draft.copy(sets = it) },
                onDismiss = { showExpSel = false },
            )
        }
    }
}

/** Selector de Expansiones con desplegable de Serie (Serie mostrada actualmente). */
@Composable
private fun ExpansionSelectorSheet(sets: List<DexSet>, selected: Set<String>, onChange: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    // Agrupa las expansiones por Serie (p. ej. "Escarlata y Púrpura").
    val bySeries = remember(sets) { sets.groupBy { it.series } }
    val seriesNames = remember(bySeries) { bySeries.keys.toList() }
    var series by remember { mutableStateOf(seriesNames.firstOrNull() ?: "") }
    var dropdown by remember { mutableStateOf(false) }
    val current = bySeries[series].orEmpty()
    // Sub-variantes (artes de sobre): qué expansión está desplegada y qué variantes marcó (local/visual).
    var variantOpen by remember { mutableStateOf<String?>(null) }
    val variantSel = remember { mutableStateMapOf<String, Set<String>>() }

    Box(Modifier.fillMaxSize().background(SheetScrim).noRippleClick(onDismiss), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.7f).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BgTop).noRippleClick { }.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Txt("Serie mostrada actualmente", 13.sp, Muted, FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            // Desplegable de Serie.
            Box(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                        .background(Panel).noRippleClick { dropdown = !dropdown }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumPokeballIcon(Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Txt(series, 15.sp, Ink, FontWeight.Black, Modifier.weight(1f), align = TextAlign.Start)
                    Txt(if (dropdown) "▲" else "▼", 12.sp, Muted, FontWeight.Black)
                }
                if (dropdown) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 56.dp).shadow(6.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp)).background(Panel),
                    ) {
                        seriesNames.forEach { s ->
                            val active = s == series
                            Box(
                                Modifier.fillMaxWidth().background(if (active) Ink else Color.Transparent)
                                    .noRippleClick { series = s; dropdown = false }.padding(horizontal = 16.dp, vertical = 12.dp),
                            ) { Txt(s, 14.sp, if (active) Color.White else Ink, FontWeight.Black) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            // Expansiones de la serie (seleccionables como filtro).
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                current.forEach { set ->
                    val sel = set.code in selected
                    val variants = EXPANSION_VARIANTS[set.code].orEmpty()
                    val open = variantOpen == set.code
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp).shadow(if (sel) 4.dp else 2.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp)).background(if (sel) Ink else Panel),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().noRippleClick { onChange(selected.toggleSet(set.code)) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExpansionLogo(set, Modifier.weight(1f), maxHeight = 34.dp)
                            if (sel) Txt("✓", 16.sp, ToggleOn, FontWeight.Black, Modifier.padding(start = 8.dp))
                            // Chevron para desplegar los artes de sobre (solo si la expansión tiene variantes).
                            if (variants.isNotEmpty()) {
                                Txt(
                                    if (open) "  ▲" else "  ▼", 12.sp, if (sel) Color.White else Muted, FontWeight.Black,
                                    Modifier.noRippleClick { variantOpen = if (open) null else set.code },
                                )
                            }
                        }
                        if (open && variants.isNotEmpty()) {
                            val marked = variantSel[set.code].orEmpty()
                            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                                // "Marcar todo" de las variantes.
                                Row(
                                    Modifier.fillMaxWidth().noRippleClick {
                                        variantSel[set.code] = if (marked.size == variants.size) emptySet() else variants.toSet()
                                    }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Txt("Artes de sobre", 12.sp, if (sel) Color(0xFFB9C1D4) else Muted, FontWeight.Bold, Modifier.weight(1f), align = TextAlign.Start)
                                    Txt("Marcar todo ${if (marked.size == variants.size) "☑" else "☐"}", 12.sp, if (marked.size == variants.size) ToggleOn else Muted, FontWeight.Black)
                                }
                                variants.forEach { v ->
                                    val on = v in marked
                                    Row(
                                        Modifier.fillMaxWidth().noRippleClick {
                                            variantSel[set.code] = marked.toggleSet(v)
                                        }.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Txt(if (on) "◉" else "○", 14.sp, if (on) ToggleOn else Muted, FontWeight.Black, Modifier.padding(end = 10.dp))
                                        Txt(v, 13.sp, if (sel) Color.White else Ink, FontWeight.Bold, Modifier.weight(1f), align = TextAlign.Start)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Panel).noRippleClick(onDismiss), contentAlignment = Alignment.Center) {
                    Txt("✕", 18.sp, Muted, FontWeight.Black)
                }
                Box(
                    Modifier.shadow(3.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
                        .noRippleClick { onChange(emptySet()) }.padding(horizontal = 18.dp, vertical = 14.dp),
                ) { Txt("Restablecer", 13.sp, Muted, FontWeight.Black) }
            }
        }
    }
}

private val POKE_TYPES = listOf(
    EnergyType.GRASS, EnergyType.FIRE, EnergyType.WATER, EnergyType.LIGHTNING, EnergyType.PSYCHIC,
    EnergyType.FIGHTING, EnergyType.DARKNESS, EnergyType.METAL, EnergyType.DRAGON, EnergyType.COLORLESS,
)

private fun <T> Set<T>.toggleSet(v: T): Set<T> = if (v in this) this - v else this + v

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(18.dp))
    Txt(title, 16.sp, Ink, FontWeight.Black, Modifier.fillMaxWidth().padding(bottom = 8.dp), align = TextAlign.Start)
}

@Composable
private fun SectionWithAll(title: String, allOn: Boolean, onAll: (Boolean) -> Unit) {
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(title, 16.sp, Ink, FontWeight.Black, Modifier.weight(1f), align = TextAlign.Start)
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(Panel).noRippleClick { onAll(!allOn) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Txt("Marcar todo", 12.sp, Muted, FontWeight.Bold)
            Txt(if (allOn) "☑" else "☐", 13.sp, if (allOn) ToggleOn else Muted, FontWeight.Black)
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { content() }
}

/** Fila de chips que envuelve (FlowRow). El contenido no usa el scope experimental, así el opt-in
 *  queda contenido aquí y los puntos de llamada no necesitan anotación. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.shadow(if (selected) 3.dp else 1.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
            .background(if (selected) Ink else Panel).noRippleClick(onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(label, 13.sp, if (selected) Color.White else Ink, FontWeight.Bold) }
}

/** Fila de rango numérico "Mín — Máx" (PS, Daño). null = sin límite. Solo dígitos. */
@Composable
private fun RangeRow(min: Int?, max: Int?, onMin: (Int?) -> Unit, onMax: (Int?) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumField(value = min, placeholder = "Mín.", onChange = onMin, modifier = Modifier.weight(1f))
        Txt("—", 16.sp, Muted, FontWeight.Black)
        NumField(value = max, placeholder = "Máx.", onChange = onMax, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NumField(value: Int?, placeholder: String, onChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }
    Row(
        modifier.shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) Txt(placeholder, 13.sp, Muted, FontWeight.SemiBold)
            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(4)
                    text = digits
                    onChange(digits.toIntOrNull())
                },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                ),
                textStyle = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hoja de DETALLE de carta.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardDetailSheet(
    card: DexCard,
    allCards: List<DexCard>,
    isFavorite: Boolean,
    isWished: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleWish: () -> Unit,
    onOpenRelated: (DexCard) -> Unit,
    onDismiss: () -> Unit,
) {
    var immersive by remember(card) { mutableStateOf(false) }
    var showLangs by remember(card) { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFDCD8F0), Color(0xFFE9ECF4))))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Toolbar superior: efectos · idiomas · ••• · favorito · ¡La quiero!
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DropPill("▦ 0")
                Spacer(Modifier.width(8.dp))
                Box(Modifier.noRippleClick { showLangs = !showLangs }) { DropPill("🌐 1") }
                Spacer(Modifier.weight(1f))
                Txt("•••", 16.sp, Muted, FontWeight.Black)
                Spacer(Modifier.width(14.dp))
                Txt(if (isFavorite) "★" else "☆", 20.sp, if (isFavorite) Color(0xFFE7B10A) else Muted, FontWeight.Black, Modifier.noRippleClick(onToggleFavorite))
                Spacer(Modifier.width(14.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.noRippleClick(onToggleWish)) {
                    Txt(if (isWished) "♥" else "♡", 20.sp, if (isWished) Color(0xFFEC5F94) else Muted, FontWeight.Black)
                    Txt("¡La quiero!", 9.sp, Muted, FontWeight.Bold)
                }
            }

            // Carta grande (obtenida = holo real; no obtenida = dorso "No la tienes").
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.fillMaxWidth(0.62f).aspectRatio(0.72f).noRippleClick { if (card.owned) immersive = true },
                    contentAlignment = Alignment.Center,
                ) {
                    if (card.owned) {
                        Box(Modifier.fillMaxSize().shadow(12.dp, RoundedCornerShape(10.dp), clip = false).clip(RoundedCornerShape(10.dp))) {
                            HoloCardImage(
                                imageUrl = card.imageLarge, setCode = card.setCode, cardNumber = card.number,
                                rarity = card.rarity, contentDescription = card.name, intensity = 1f, modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } else {
                        Box(
                            Modifier.fillMaxSize().shadow(10.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFF2A3550), Color(0xFF161E30)))),
                            contentAlignment = Alignment.Center,
                        ) { Txt("No la tienes", 13.sp, Color(0xCCFFFFFF), FontWeight.Black) }
                    }
                }
            }

            // Fila: copias · efectos visuales · obtener efecto visual.
            if (card.owned) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CountChip("▤", "${card.count}")
                    CountChip("▣", "0")
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
                            .border(1.5.dp, Accent, RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Txt("✦", 14.sp, Accent, FontWeight.Black)
                        Txt("Obtener efecto visual", 12.sp, Ink, FontWeight.Black)
                    }
                }
            }

            // Nombre + rareza.
            Spacer(Modifier.height(10.dp))
            Txt(card.name, 24.sp, Ink, FontWeight.Black, Modifier.fillMaxWidth(), align = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                RaritySymbol(card.rarity, height = 20.dp)
                Spacer(Modifier.width(8.dp))
                Txt(rarityEs(card.rarity), 13.sp, Muted, FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))

            // Panel blanco con datos + relacionadas.
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Panel).padding(16.dp),
            ) {
                DetailTable(card.card)
                (card.card as? PokemonCard)?.let { p ->
                    if (p.abilities.isNotEmpty() || p.attacks.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(BgTop).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Txt("Ataques", 14.sp, Ink, FontWeight.Black)
                        }
                        Spacer(Modifier.height(12.dp))
                        AttacksSection(p)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(BgTop).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Txt("Cartas relacionadas", 14.sp, Ink, FontWeight.Black)
                }
                Spacer(Modifier.height(12.dp))
                RelatedCards(card, allCards, onOpenRelated)
            }
            Spacer(Modifier.height(90.dp))
        }

        // Cerrar (X) fijo abajo.
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).size(54.dp)
                .shadow(4.dp, CircleShape).clip(CircleShape).background(Panel).noRippleClick(onDismiss),
            contentAlignment = Alignment.Center,
        ) { Txt("✕", 20.sp, Muted, FontWeight.Black) }

        // Popup de variantes de idioma.
        if (showLangs) LanguagePopup(ownedCount = card.count, onDismiss = { showLangs = false })
    }

    // Tilt inmersivo a pantalla completa (reutiliza el visor holo con giroscopio del juego).
    if (immersive) {
        CardDetailDialog(
            imageUrl = card.imageLarge, contentDescription = card.name, onDismiss = { immersive = false },
            rarity = card.rarity, cardNumber = card.number, setCode = card.setCode,
            copiesOwned = card.count, copiesCap = card.cap,
        )
    }
}

@Composable
private fun DropPill(text: String) {
    Row(
        Modifier.shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Txt(text, 13.sp, Ink, FontWeight.Black)
        Txt("▾", 10.sp, Muted, FontWeight.Black)
    }
}

@Composable
private fun CountChip(glyph: String, value: String) {
    Row(
        Modifier.shadow(1.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Txt(glyph, 13.sp, Muted, FontWeight.Black)
        Txt(value, 14.sp, Ink, FontWeight.Black)
    }
}

/** Tabla de datos (N.º · Pokémon/stage · Tipo · PS · Debilidad · Coste de Retirada · Serie). */
@Composable
private fun DetailTable(card: Card) {
    val p = card as? PokemonCard
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TableRow("N.º", "%04d".format(numberOf(card.id.raw)))
        if (p != null) {
            TableRow("Pokémon", stageLabel(p.stage))
            TableEnergyRow("Tipo", p.types)
            TableRow("PS", "${p.hp}")
            TableEnergyRow("Debilidad", p.weaknesses.map { it.type }, suffix = p.weaknesses.firstOrNull()?.value)
            TableEnergyRow("Coste de Retirada", p.retreatCost)
        }
        TableRow("Serie", card.set.series)
    }
}

/** Icono de energía REAL (assets de core:designsystem). Hada no tiene asset → punto de color con inicial. */
private fun energyIconRes(t: EnergyType): Int? = when (t) {
    EnergyType.GRASS -> com.mineralord.tcg.core.designsystem.R.drawable.energy_grass
    EnergyType.FIRE -> com.mineralord.tcg.core.designsystem.R.drawable.energy_fire
    EnergyType.WATER -> com.mineralord.tcg.core.designsystem.R.drawable.energy_water
    EnergyType.LIGHTNING -> com.mineralord.tcg.core.designsystem.R.drawable.energy_lightning
    EnergyType.PSYCHIC -> com.mineralord.tcg.core.designsystem.R.drawable.energy_psychic
    EnergyType.FIGHTING -> com.mineralord.tcg.core.designsystem.R.drawable.energy_fighting
    EnergyType.DARKNESS -> com.mineralord.tcg.core.designsystem.R.drawable.energy_darkness
    EnergyType.METAL -> com.mineralord.tcg.core.designsystem.R.drawable.energy_metal
    EnergyType.DRAGON -> com.mineralord.tcg.core.designsystem.R.drawable.energy_dragon
    EnergyType.COLORLESS -> com.mineralord.tcg.core.designsystem.R.drawable.energy_colorless
    EnergyType.FAIRY -> null
}

@Composable
private fun EnergyIcon(t: EnergyType, size: Dp = 18.dp) {
    val res = energyIconRes(t)
    if (res != null) {
        Image(painterResource(res), energyEs(t), Modifier.size(size))
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(Color(0xFFE9A3D6)), contentAlignment = Alignment.Center) {
            Txt("H", (size.value * 0.55f).sp, Color.White, FontWeight.Black)
        }
    }
}

/** Sección "Ataques" (y habilidades) del detalle, con iconos de energía reales, como TCG Live. */
@Composable
private fun AttacksSection(p: PokemonCard) {
    if (p.abilities.isEmpty() && p.attacks.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        p.abilities.forEach { ability -> AbilityRow(ability) }
        p.attacks.forEach { atk -> AttackRow(atk) }
    }
}

@Composable
private fun AbilityRow(ability: Ability) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgTop).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFE04A6B)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Txt("Habilidad", 10.sp, Color.White, FontWeight.Black)
            }
            Txt(ability.name.es.ifBlank { ability.name.en }, 14.sp, Ink, FontWeight.Black)
        }
        val text = ability.text.es.ifBlank { ability.text.en }
        if (text.isNotBlank()) { Spacer(Modifier.height(6.dp)); Txt(text, 12.sp, Muted, FontWeight.SemiBold, align = TextAlign.Start) }
    }
}

@Composable
private fun AttackRow(atk: Attack) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgTop).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Coste de energía (iconos reales); sin coste → guion.
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (atk.cost.isEmpty()) Txt("—", 14.sp, Muted, FontWeight.Black)
                else atk.cost.forEach { EnergyIcon(it, 20.dp) }
            }
            Spacer(Modifier.width(10.dp))
            Txt(atk.name.es.ifBlank { atk.name.en }, 15.sp, Ink, FontWeight.Black, Modifier.weight(1f), align = TextAlign.Start)
            val dmg = (atk.baseDamage as? Damage.Fixed)?.value?.takeIf { it > 0 }
            if (dmg != null) Txt("$dmg", 18.sp, Ink, FontWeight.Black)
        }
        val text = atk.text.es.ifBlank { atk.text.en }
        if (text.isNotBlank()) { Spacer(Modifier.height(6.dp)); Txt(text, 12.sp, Muted, FontWeight.SemiBold, align = TextAlign.Start) }
    }
}

@Composable
private fun TableRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(BgTop).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label, 13.sp, Muted, FontWeight.Bold, Modifier.weight(1f), align = TextAlign.Start)
        Txt(value, 13.sp, Ink, FontWeight.Black)
    }
}

/** Fila de tabla cuyo valor son iconos de energía reales (Tipo/Debilidad/Retirada). Vacío → guion. */
@Composable
private fun TableEnergyRow(label: String, types: List<EnergyType>, suffix: String? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(BgTop).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label, 13.sp, Muted, FontWeight.Bold, Modifier.weight(1f), align = TextAlign.Start)
        if (types.isEmpty()) {
            Txt("—", 13.sp, Ink, FontWeight.Black)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                types.forEach { EnergyIcon(it, 20.dp) }
                if (suffix != null) Txt(" $suffix", 13.sp, Ink, FontWeight.Black)
            }
        }
    }
}

/** Cartas relacionadas: línea evolutiva (obtenidas a color, faltantes en gris). */
@Composable
private fun RelatedCards(card: DexCard, all: List<DexCard>, onOpen: (DexCard) -> Unit) {
    val related = remember(card) { relatedFamily(card, all) }
    if (related.isEmpty()) {
        Txt("—", 13.sp, Muted, FontWeight.Bold, Modifier.fillMaxWidth(), align = TextAlign.Center)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        related.take(3).forEach { r ->
            Box(Modifier.weight(1f).aspectRatio(0.72f).clip(RoundedCornerShape(8.dp)).noRippleClick { onOpen(r) }) {
                if (r.owned) {
                    HoloCardImage(
                        imageUrl = r.imageEs ?: r.imageLarge, setCode = r.setCode, cardNumber = r.number,
                        rarity = r.rarity, contentDescription = r.name, enabled = false, modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // Faltante: arte en gris (desaturado con capa).
                    Box(Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = r.imageEs ?: r.imageLarge, contentDescription = r.name,
                            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
                        )
                        Box(Modifier.fillMaxSize().background(Color(0x99B0B6C2)))
                    }
                }
            }
        }
    }
}

/** Popup de variantes de idioma (copias por idioma; sólo ING tiene el conteo real). */
@Composable
private fun LanguagePopup(ownedCount: Int, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x22000000)).noRippleClick(onDismiss), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.padding(top = 60.dp).fillMaxWidth(0.7f).shadow(8.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp)).background(Panel).noRippleClick { }.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Txt("Vista previa", 12.sp, Muted, FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            val langs = listOf("ING" to ownedCount, "ES-ES" to 0, "FRA" to 0, "ALE" to 0, "ITA" to 0, "PT-BR" to 0)
            langs.forEachIndexed { i, (lang, n) ->
                val active = i == 0
                Row(
                    Modifier.fillMaxWidth(0.85f).padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(50)).background(if (active) Ink else BgTop)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt(lang, 14.sp, if (active) Color.White else Muted, FontWeight.Black, Modifier.weight(1f), align = TextAlign.Start)
                    Box(Modifier.clip(RoundedCornerShape(50)).background(if (active) Color(0x33FFFFFF) else Panel).padding(horizontal = 10.dp, vertical = 2.dp)) {
                        Txt("$n", 12.sp, if (active) Color.White else Muted, FontWeight.Black)
                    }
                }
            }
        }
    }
}

/** Único botón flotante inferior-derecho = ORDENAR (como TCG Live). */
@Composable
private fun OrdenarButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(52.dp).shadow(6.dp, CircleShape).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF4FD3F2), Accent)))
            .noRippleClick(onClick),
        contentAlignment = Alignment.Center,
    ) { AlbumPokeballIcon(Modifier.size(26.dp).padding(1.dp), tint = Color.White) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lógica de orden / familia / etiquetas.
// ─────────────────────────────────────────────────────────────────────────────

/** Aplica el filtro del menú Buscar. Vacío = devuelve la lista intacta. */
private fun applyFilter(cards: List<DexCard>, f: DexFilter, favorites: List<String>, wishlist: List<String> = emptyList()): List<DexCard> {
    if (!f.active) return cards
    val q = f.query.trim().lowercase()
    return cards.filter { c ->
        val id = c.card.id.raw
        val p = c.card as? PokemonCard
        val t = c.card as? TrainerCard
        val dmg = p?.maxPrintedDamage()
        (q.isBlank() || c.name.lowercase().contains(q) || c.card.name.en.lowercase().contains(q)) &&
            (!f.onlyFav || id in favorites) &&
            (!f.onlyNotFav || id !in favorites) &&
            (!f.onlyWish || id in wishlist) &&
            (!f.onlyNotWish || id !in wishlist) &&
            (f.rarities.isEmpty() || c.rarity in f.rarities) &&
            (f.types.isEmpty() || (p != null && p.types.any { it in f.types })) &&
            (!f.withAbility || (p != null && p.abilities.isNotEmpty())) &&
            (!f.withoutAbility || (p == null || p.abilities.isEmpty())) &&
            (!f.ex || c.card.name.en.lowercase().contains(" ex") || c.rarity == Rarity.DOUBLE_RARE) &&
            (f.sets.isEmpty() || c.setCode in f.sets) &&
            // PS: solo Pokémon; los rangos descartan lo que no sea Pokémon.
            (f.hpMin == null || (p != null && p.hp >= f.hpMin)) &&
            (f.hpMax == null || (p != null && p.hp <= f.hpMax)) &&
            // Daño de ataque impreso (máximo entre ataques): solo Pokémon.
            (f.dmgMin == null || (dmg != null && dmg >= f.dmgMin)) &&
            (f.dmgMax == null || (dmg != null && dmg <= f.dmgMax)) &&
            // Carta de Entrenador (categoría): solo entrenadores.
            (f.trainerCats.isEmpty() || (t != null && f.trainerCats.any { it.matches(t.kind) }))
        // Idioma: visual, no filtra (dataset monolingüe).
    }
}

private fun sortCards(cards: List<DexCard>, sort: DexSort, asc: Boolean, favorites: List<String>): List<DexCard> {
    val base = when (sort) {
        DexSort.NUMBER, DexSort.EXPANSION, DexSort.FECHA_CARTAS, DexSort.FECHA_EFECTOS ->
            cards.sortedWith(compareBy({ ORDERED_SETS.indexOf(it.setCode) }, { it.number }))
        DexSort.TYPE -> cards.sortedWith(compareBy({ (it.card as? PokemonCard)?.types?.firstOrNull()?.ordinal ?: 99 }, { it.number }))
        DexSort.RARITY -> cards.sortedWith(compareByDescending<DexCard> { it.rarity.ordinal }.thenBy { it.number })
        DexSort.COUNT -> cards.sortedWith(compareByDescending<DexCard> { it.count }.thenBy { it.number })
        DexSort.FAVORITES -> cards.sortedWith(compareByDescending<DexCard> { it.card.id.raw in favorites }.thenBy { it.number })
    }
    return if (asc) base else base.reversed()
}

private fun relatedFamily(card: DexCard, all: List<DexCard>): List<DexCard> {
    val p = card.card as? PokemonCard ?: return emptyList()
    val nameEn = p.name.en
    val evo = p.evolvesFrom
    return all.filter { d ->
        val pc = d.card as? PokemonCard ?: return@filter false
        pc.name.en == nameEn || pc.evolvesFrom == nameEn || (evo != null && pc.name.en == evo)
    }.distinctBy { (it.card as? PokemonCard)?.name?.en ?: it.card.id.raw }
        .sortedBy { stageOrder((it.card as? PokemonCard)?.stage) }
}

private fun stageOrder(stage: Stage?): Int = when (stage) {
    Stage.Basic -> 0; Stage.Stage1 -> 1; Stage.Stage2 -> 2; else -> 3
}

private fun stageLabel(stage: Stage): String = when (stage) {
    Stage.Basic -> "Básico"; Stage.Stage1 -> "Fase 1"; Stage.Stage2 -> "Fase 2"; Stage.BabyRestored -> "Restaurado"
}

private fun energyEs(t: EnergyType?): String = when (t) {
    EnergyType.GRASS -> "Planta"; EnergyType.FIRE -> "Fuego"; EnergyType.WATER -> "Agua"
    EnergyType.LIGHTNING -> "Rayo"; EnergyType.PSYCHIC -> "Psíquico"; EnergyType.FIGHTING -> "Lucha"
    EnergyType.DARKNESS -> "Oscuro"; EnergyType.METAL -> "Metálico"; EnergyType.FAIRY -> "Hada"
    EnergyType.DRAGON -> "Dragón"; EnergyType.COLORLESS -> "Incoloro"; null -> "—"
}

/** Símbolo OFICIAL de rareza del TCG Pokémon (era Escarlata y Púrpura, fuente Bulbapedia).
 *  RARE_HOLO usa la misma estrella negra que RARE (el holo es acabado, no cambia el símbolo);
 *  PROMO usa la estrella negra de promo. Devuelve el drawable del símbolo real. */
private fun rarityRes(r: Rarity): Int = when (r) {
    Rarity.COMMON -> R.drawable.rarity_common
    Rarity.UNCOMMON -> R.drawable.rarity_uncommon
    Rarity.RARE -> R.drawable.rarity_rare
    Rarity.RARE_HOLO -> R.drawable.rarity_rare
    Rarity.DOUBLE_RARE -> R.drawable.rarity_double_rare
    Rarity.ULTRA_RARE -> R.drawable.rarity_ultra_rare
    Rarity.ILLUSTRATION_RARE -> R.drawable.rarity_illustration_rare
    Rarity.SPECIAL_ILLUSTRATION_RARE -> R.drawable.rarity_special_illustration_rare
    Rarity.HYPER_RARE -> R.drawable.rarity_hyper_rare
    Rarity.PROMO -> R.drawable.rarity_rare
}

/** Nombre en español de la rareza (para el filtro Buscar). */
private fun rarityEs(r: Rarity): String = when (r) {
    Rarity.COMMON -> "Común"
    Rarity.UNCOMMON -> "Poco común"
    Rarity.RARE -> "Rara"
    Rarity.RARE_HOLO -> "Rara holográfica"
    Rarity.DOUBLE_RARE -> "Doble rara"
    Rarity.ULTRA_RARE -> "Ultra rara"
    Rarity.ILLUSTRATION_RARE -> "Rara ilustración"
    Rarity.SPECIAL_ILLUSTRATION_RARE -> "Rara ilustración especial"
    Rarity.HYPER_RARE -> "Hiperrara"
    Rarity.PROMO -> "Promocional"
}

/** Símbolo oficial de rareza como imagen. Altura del símbolo (el ancho se ajusta solo). */
@Composable
private fun RaritySymbol(r: Rarity, height: Dp = 16.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(rarityRes(r)), contentDescription = rarityEs(r),
        contentScale = ContentScale.Fit, modifier = modifier.height(height),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers de bajo nivel.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Txt(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    color: Color,
    weight: FontWeight,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Start,
) {
    BasicText(
        text = text, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = color, fontSize = size, fontWeight = weight, textAlign = align),
    )
}

private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
