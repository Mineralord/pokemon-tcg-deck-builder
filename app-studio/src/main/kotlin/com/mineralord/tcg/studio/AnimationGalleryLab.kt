package com.mineralord.tcg.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animationcompose.AnimationRenderState
import com.mineralord.tcg.core.animationcompose.AnimationStage
import com.mineralord.tcg.core.animationcompose.CoordinateRegistry
import com.mineralord.tcg.core.animationcompose.EvolveVariants
import com.mineralord.tcg.core.animationcompose.LocalCoordinateRegistry
import com.mineralord.tcg.core.animationcompose.SlotId
import com.mineralord.tcg.core.animationcompose.activeSlotId
import com.mineralord.tcg.core.animationcompose.deckSlotId
import com.mineralord.tcg.core.animationcompose.handSlotId
import com.mineralord.tcg.core.animationcompose.rememberCanonicalAnimationDirector
import com.mineralord.tcg.core.animationcompose.trackBounds
import com.mineralord.tcg.core.designsystem.tokens.ButtonStyle
import com.mineralord.tcg.core.designsystem.tokens.ButtonVariant
import com.mineralord.tcg.core.designsystem.tokens.ListRowStyle
import com.mineralord.tcg.core.designsystem.tokens.PanelStyle
import com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.core.designsystem.tokens.StudioTypographyTokens
import com.mineralord.tcg.core.designsystem.tokens.VisualState

/**
 * **Galería de Animaciones — Lab v3: laboratorio de I+D por categorías.**
 *
 * Es un Lab NORMAL (su único contacto con el Shell es ser el `content` de un `Lab`). El Studio actúa
 * como **anfitrión del motor real**: obtiene el `AnimationDirector` del composition root ÚNICO
 * compartido ([rememberCanonicalAnimationDirector]) y reproduce cada variante por la misma fachada
 * que usará el juego (`director.submit(...)`). No hay ninguna animación exclusiva del Studio ni mocks.
 *
 * Organiza las animaciones por **categorías** (carpetas); cada categoría contiene todas sus
 * **variantes**, cada una con un **estado** ([AnimationVariantState]: Experimental / Candidata /
 * Canon). Sólo una variante puede ser Canon por categoría (invariante verificado en construcción);
 * las demás nunca se eliminan: quedan disponibles para comparar. La decisión de cuál es Canon es
 * exclusiva del desarrollador (esta pantalla sólo permite probarlas).
 *
 * Se compone con el Design System congelado; cero tokens/componentes nuevos.
 */

private const val PREVIEW_PLAYER = "studio"

/** Estado de curación de una variante dentro de su categoría. */
private enum class AnimationVariantState(val label: String) {
    Experimental("Experimental"),
    Candidata("Candidata"),
    Canon("Canon"),
}

/** Una variante concreta: metadatos + la request de dominio REAL que la dispara en el pipeline. */
private data class GalleryVariant(
    val id: String,
    val name: String,
    val description: String,
    val inspiration: String,
    val duration: String,
    val state: AnimationVariantState,
    val request: AnimationRequest,
)

/** Una categoría (carpeta) con todas sus variantes. Invariante: ≤ 1 Canon por categoría. */
private data class GalleryCategory(
    val id: String,
    val name: String,
    val variants: List<GalleryVariant>,
) {
    init {
        require(variants.count { it.state == AnimationVariantState.Canon } <= 1) {
            "La categoría '$name' no puede tener más de una variante Canon."
        }
    }
}

/** Catálogo del laboratorio. La categoría Evolución trae las cinco variantes de I+D (Experimental). */
private val GalleryCatalog: List<GalleryCategory> = listOf(
    GalleryCategory(
        id = "draw",
        name = "Robar carta",
        variants = listOf(
            GalleryVariant(
                id = "DRAW_001",
                name = "Robar carta",
                description = "Carta del mazo a la mano con arco leve, escala y overshoot sobrio.",
                inspiration = "Marvel Snap (estándar «Snap sobrio»)",
                duration = "~320 ms",
                state = AnimationVariantState.Canon,
                request = AnimationRequest.CardDrawn(PREVIEW_PLAYER, "preview"),
            ),
        ),
    ),
    GalleryCategory(
        id = "basic",
        name = "Pokémon Básico",
        variants = listOf(
            GalleryVariant(
                id = "BASIC_001",
                name = "Poner en juego",
                description = "Desplazamiento de la carta de la mano al Activo por la capa de vuelo.",
                inspiration = "Pokémon TCG Live",
                duration = "~260 ms",
                state = AnimationVariantState.Canon,
                request = AnimationRequest.PokemonPlayed(PREVIEW_PLAYER, "preview"),
            ),
        ),
    ),
    GalleryCategory(
        id = "evolution",
        name = "Evolución",
        variants = listOf(
            evolveVariant(
                EvolveVariants.EVO_001, "Crystal Bloom",
                "Bombeo suave con anillo cristalino frío que florece alrededor de la carta.",
                "Hearthstone (cristal/escarcha) + Legends of Runeterra", "~700 ms",
            ),
            evolveVariant(
                EvolveVariants.EVO_002, "Energy Spiral",
                "La carta gira y asciende envuelta en una espiral de energía violeta.",
                "Genshin/Honkai (revelado gacha en espiral)", "~850 ms",
            ),
            evolveVariant(
                EvolveVariants.EVO_003, "Radiant Ascension",
                "Ascenso pronunciado sobre una columna de luz dorada con gran destello.",
                "Legends of Runeterra (subida de nivel de campeón)", "~950 ms",
            ),
            evolveVariant(
                EvolveVariants.EVO_004, "DNA Morph",
                "Volteo/morfología rápida de la carta con tono verde, sin anillo.",
                "Evolución del anime Pokémon (silueta que muta)", "~600 ms",
            ),
            evolveVariant(
                EvolveVariants.EVO_005, "Celestial Burst",
                "Estallido: bombeo fuerte, destello intenso y anillo amplio azul-blanco.",
                "Marvel Snap (reveal) + estallido del anime", "~800 ms",
            ),
        ),
    ),
)

/** Constructor de una variante de evolución: su request es `Evolved(variantId)` (pipeline real). */
private fun evolveVariant(
    id: String,
    name: String,
    description: String,
    inspiration: String,
    duration: String,
): GalleryVariant = GalleryVariant(
    id = id,
    name = name,
    description = description,
    inspiration = inspiration,
    duration = duration,
    state = AnimationVariantState.Experimental, // ninguna es Canon todavía: lo decide el desarrollador
    request = AnimationRequest.Evolved(PREVIEW_PLAYER, "preview", id),
)

private val AllVariants: List<GalleryVariant> = GalleryCatalog.flatMap { it.variants }

/** Contenido del Lab «Galería de Animaciones», dibujado en el Host del Shell. */
@Composable
fun AnimationGalleryLabContent() {
    val scheme = StudioTheme.colors
    var selectedId by remember { mutableStateOf(EvolveVariants.EVO_001) }
    val selected = AllVariants.first { it.id == selectedId }

    // Carpetas abiertas: por defecto Evolución (categoría de I+D de este Sprint).
    val expanded = remember { mutableStateMapOf("evolution" to true) }

    // Anfitrión del motor real: el Studio provee dónde dibujar; el pipeline llega del núcleo.
    val coordinates = remember { CoordinateRegistry() }
    val renderState = remember { AnimationRenderState() }
    val director = rememberCanonicalAnimationDirector(coordinates, renderState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PanelStyle.padding),
        verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
    ) {
        BasicText(
            text = "Galería de Animaciones",
            style = coloredText(PanelStyle.titleStyle, scheme.contentEmphasis),
        )
        BasicText(
            text = "${GalleryCatalog.size} categorías · ${AllVariants.size} variantes · motor compartido",
            style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
        )

        // Árbol de carpetas (categorías) → variantes seleccionables.
        GalleryCatalog.forEach { category ->
            CategoryFolder(
                scheme = scheme,
                category = category,
                open = expanded[category.id] == true,
                onToggle = { expanded[category.id] = !(expanded[category.id] == true) },
                selectedId = selectedId,
                onSelect = { selectedId = it },
            )
        }

        // Escenario de Preview real + acción + información de la variante seleccionada.
        PreviewStage(scheme, coordinates, renderState)
        PrimaryAction(scheme, label = "Reproducir «${selected.name}»") {
            director.submit(selected.request)
        }
        DetailPanel(scheme, selected)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Árbol de categorías/variantes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryFolder(
    scheme: StudioColorTokens.Scheme,
    category: GalleryCategory,
    open: Boolean,
    onToggle: () -> Unit,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap)) {
        // Cabecera de carpeta (clic para plegar/desplegar).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ListRowStyle.rowHeight)
                .clip(RoundedCornerShape(ListRowStyle.radius))
                .background(scheme.surfacePanel)
                .clickable(onClick = onToggle)
                .padding(horizontal = ListRowStyle.paddingH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ListRowStyle.paddingH),
        ) {
            BasicText(
                text = if (open) "▾" else "▸",
                style = coloredText(ListRowStyle.textStyle, scheme.contentSecondary),
            )
            BasicText(
                text = "📁 ${category.name}",
                style = coloredText(ListRowStyle.textStyle, scheme.contentEmphasis),
            )
            BasicText(
                text = "${category.variants.size}",
                style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
            )
        }
        if (open) {
            category.variants.forEach { variant ->
                VariantRow(
                    scheme = scheme,
                    variant = variant,
                    selected = variant.id == selectedId,
                    onClick = { onSelect(variant.id) },
                )
            }
        }
    }
}

/** Fila de variante (sangrada), materializada con [ListRowStyle]. */
@Composable
private fun VariantRow(
    scheme: StudioColorTokens.Scheme,
    variant: GalleryVariant,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val state = if (selected) VisualState.Selected else VisualState.Rest
    val colors = ListRowStyle.colors(scheme, state)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ListRowStyle.rowHeight)
            .padding(start = ListRowStyle.paddingH)
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(colors.container)
            .selectionGuide(colors.border, ListRowStyle.selectionMarker(scheme))
            .clickable(onClick = onClick)
            .padding(horizontal = ListRowStyle.paddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ListRowStyle.paddingH),
    ) {
        BasicText(
            text = variant.id,
            style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
        )
        BasicText(text = variant.name, style = coloredText(ListRowStyle.textStyle, colors.content))
        Box(modifier = Modifier.weight(1f))
        StateBadge(scheme, variant.state)
    }
}

/** Insignia textual del estado (color por estado; no sólo color: lleva etiqueta). */
@Composable
private fun StateBadge(scheme: StudioColorTokens.Scheme, state: AnimationVariantState) {
    val color = when (state) {
        AnimationVariantState.Experimental -> scheme.contentMuted
        AnimationVariantState.Candidata -> scheme.contentPrimary
        AnimationVariantState.Canon -> scheme.selectionEdge
    }
    val text = if (state == AnimationVariantState.Canon) "★ ${state.label}" else state.label
    BasicText(text = text, style = coloredText(StudioTypographyTokens.Role.Status, color))
}

// ─────────────────────────────────────────────────────────────────────────────
// Escenario de Preview (motor real)
// ─────────────────────────────────────────────────────────────────────────────

private val StageHeight: Dp = 320.dp
private val SlotWidth: Dp = 56.dp
private val SlotHeight: Dp = 80.dp

@Composable
private fun PreviewStage(
    scheme: StudioColorTokens.Scheme,
    coordinates: CoordinateRegistry,
    renderState: AnimationRenderState,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(StageHeight)
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(scheme.surfacePanel)
            .border(1.dp, scheme.borderSubtle, RoundedCornerShape(ListRowStyle.radius)),
    ) {
        AnimationStage(
            registry = coordinates,
            renderState = renderState,
            board = {
                Box(modifier = Modifier.fillMaxSize().padding(PanelStyle.padding)) {
                    SlotMarker(scheme, "Mazo", deckSlotId(PREVIEW_PLAYER), Modifier.align(Alignment.TopStart))
                    SlotMarker(scheme, "Mano", handSlotId(PREVIEW_PLAYER), Modifier.align(Alignment.BottomEnd))
                    SlotMarker(scheme, "Activo", activeSlotId(PREVIEW_PLAYER), Modifier.align(Alignment.Center))
                }
            },
        )
    }
}

/** Ranura visible del escenario, rastreada en el registro compartido vía [trackBounds]. */
@Composable
private fun SlotMarker(
    scheme: StudioColorTokens.Scheme,
    label: String,
    slotId: SlotId,
    modifier: Modifier = Modifier,
) {
    val registry = LocalCoordinateRegistry.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
    ) {
        Box(
            modifier = Modifier
                .size(SlotWidth, SlotHeight)
                .trackBounds(slotId, registry)
                .clip(RoundedCornerShape(ListRowStyle.radius))
                .background(scheme.surfaceRaised)
                .border(1.dp, scheme.borderDefault, RoundedCornerShape(ListRowStyle.radius)),
        )
        BasicText(text = label, style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Acción + detalle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrimaryAction(scheme: StudioColorTokens.Scheme, label: String, onClick: () -> Unit) {
    val style = ButtonStyle(ButtonVariant.Primary)
    val colors = style.colors(scheme, VisualState.Rest)
    Box(
        modifier = Modifier
            .height(style.metrics.height)
            .clip(RoundedCornerShape(style.metrics.radius))
            .background(colors.container)
            .clickable(onClick = onClick)
            .padding(horizontal = style.metrics.paddingH),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = label, style = coloredText(style.textStyle, colors.content))
    }
}

@Composable
private fun DetailPanel(scheme: StudioColorTokens.Scheme, variant: GalleryVariant) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(PanelStyle.material.surface(scheme))
            .topHairline(PanelStyle.headerDivider(scheme), PanelStyle.material.borderWidth)
            .padding(PanelStyle.contentPadding),
        verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
        ) {
            BasicText(text = "${variant.id} · ${variant.name}", style = coloredText(PanelStyle.titleStyle, scheme.contentEmphasis))
            StateBadge(scheme, variant.state)
        }
        InfoRow(scheme, "Duración", variant.duration)
        InfoRow(scheme, "Inspiración", variant.inspiration)
        BasicText(
            text = variant.description,
            style = coloredText(StudioTypographyTokens.Role.Body, scheme.contentPrimary),
        )
    }
}

@Composable
private fun InfoRow(scheme: StudioColorTokens.Scheme, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap)) {
        BasicText(
            text = label,
            style = coloredText(StudioTypographyTokens.Role.Label, scheme.contentSecondary),
        )
        BasicText(
            text = value,
            style = coloredText(StudioTypographyTokens.Role.Body, scheme.contentPrimary),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers de composición (no definen apariencia nueva).
// ─────────────────────────────────────────────────────────────────────────────

private fun coloredText(role: TextStyle, color: Color): TextStyle = role.copy(color = color)

/** Barra guía de selección al borde inicial (VG-K: el estado no se comunica solo por fondo). */
private fun Modifier.selectionGuide(marker: Color?, color: Color): Modifier =
    if (marker == null) this else drawBehind {
        val t = 2.dp.toPx()
        drawRect(color, topLeft = Offset(0f, 0f), size = Size(t, size.height))
    }

/** Hairline superior (frontera del panel de detalle). */
private fun Modifier.topHairline(color: Color, thickness: Dp): Modifier =
    drawBehind {
        val t = thickness.toPx()
        drawRect(color, topLeft = Offset(0f, 0f), size = Size(size.width, t))
    }
