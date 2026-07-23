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
import com.mineralord.tcg.core.animationcompose.AnimationRenderState
import com.mineralord.tcg.core.animationcompose.AnimationStage
import com.mineralord.tcg.core.animationcompose.CoordinateRegistry
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
import com.mineralord.tcg.studio.assets.AnimationAsset
import com.mineralord.tcg.studio.assets.AssetRegistry
import com.mineralord.tcg.studio.assets.AssetStatus
import com.mineralord.tcg.studio.assets.AssetType

/**
 * **Galería de Animaciones — Lab v4: consumidora del Asset Registry.**
 *
 * Es un Lab NORMAL (su único contacto con el Shell es ser el `content` de un `Lab`) y **no mantiene
 * ninguna lista propia**: todo —categorías, variantes y metadatos— proviene EXCLUSIVAMENTE del
 * [AssetRegistry] (fuente única de verdad). Su trabajo es sólo: registry → categorías → assets →
 * mostrar información → solicitar reproducción.
 *
 * El Studio actúa como **anfitrión del motor real**: obtiene el `AnimationDirector` del composition
 * root ÚNICO compartido ([rememberCanonicalAnimationDirector]) y reproduce cada asset por la misma
 * fachada que usa el juego (`director.submit(asset.request)`). No hay animaciones exclusivas del
 * Studio, ni mocks, ni caminos paralelos.
 *
 * Se compone con el Design System congelado; cero tokens/componentes nuevos.
 */

/** Contenido del Lab «Galería de Animaciones», dibujado en el Host del Shell. */
@Composable
fun AnimationGalleryLabContent(registry: AssetRegistry) {
    val scheme = StudioTheme.colors

    // Datos derivados EXCLUSIVAMENTE del registro (nunca listas propias).
    val categories = registry.categories(AssetType.Animation)
    val animations = registry.byType(AssetType.Animation).filterIsInstance<AnimationAsset>()

    var selectedId by remember { mutableStateOf(animations.firstOrNull()?.id.orEmpty()) }
    val selected = registry.byId(selectedId) as? AnimationAsset ?: animations.firstOrNull()

    // Carpetas abiertas: por defecto Evolución (categoría de I+D en curso), si existe.
    val expanded = remember { mutableStateMapOf("Evolución" to true) }

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
            text = "${categories.size} categorías · ${animations.size} variantes · Asset Registry · motor compartido",
            style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
        )

        // Árbol de carpetas (categorías del registro) → variantes seleccionables.
        categories.forEach { category ->
            CategoryFolder(
                scheme = scheme,
                category = category,
                assets = registry.byCategory(AssetType.Animation, category).filterIsInstance<AnimationAsset>(),
                open = expanded[category] == true,
                onToggle = { expanded[category] = !(expanded[category] == true) },
                selectedId = selectedId,
                onSelect = { selectedId = it },
            )
        }

        // Escenario de Preview real + acción + información del asset seleccionado.
        PreviewStage(scheme, coordinates, renderState)
        if (selected != null) {
            PrimaryAction(scheme, label = "Reproducir «${selected.name}»") {
                director.submit(selected.request)
            }
            DetailPanel(scheme, selected)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Árbol de categorías/variantes (todo desde el registro)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryFolder(
    scheme: StudioColorTokens.Scheme,
    category: String,
    assets: List<AnimationAsset>,
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
                text = "📁 $category",
                style = coloredText(ListRowStyle.textStyle, scheme.contentEmphasis),
            )
            BasicText(
                text = "${assets.size}",
                style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
            )
        }
        if (open) {
            assets.forEach { asset ->
                VariantRow(
                    scheme = scheme,
                    asset = asset,
                    selected = asset.id == selectedId,
                    onClick = { onSelect(asset.id) },
                )
            }
        }
    }
}

/** Fila de variante (sangrada), materializada con [ListRowStyle]. */
@Composable
private fun VariantRow(
    scheme: StudioColorTokens.Scheme,
    asset: AnimationAsset,
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
            text = asset.id,
            style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
        )
        BasicText(text = asset.name, style = coloredText(ListRowStyle.textStyle, colors.content))
        Box(modifier = Modifier.weight(1f))
        StateBadge(scheme, asset.status)
    }
}

/** Insignia textual del estado (color por estado; no sólo color: lleva etiqueta). */
@Composable
private fun StateBadge(scheme: StudioColorTokens.Scheme, status: AssetStatus) {
    val color = when (status) {
        AssetStatus.Experimental -> scheme.contentMuted
        AssetStatus.Candidate -> scheme.contentPrimary
        AssetStatus.Canon -> scheme.selectionEdge
        AssetStatus.Deprecated -> scheme.contentSecondary
    }
    val text = when (status) {
        AssetStatus.Canon -> "★ ${status.label}"
        AssetStatus.Deprecated -> "⏷ ${status.label}"
        else -> status.label
    }
    BasicText(text = text, style = coloredText(StudioTypographyTokens.Role.Status, color))
}

// ─────────────────────────────────────────────────────────────────────────────
// Escenario de Preview (motor real)
// ─────────────────────────────────────────────────────────────────────────────

private const val PREVIEW_PLAYER = "studio"
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
private fun DetailPanel(scheme: StudioColorTokens.Scheme, asset: AnimationAsset) {
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
            BasicText(text = "${asset.id} · ${asset.name}", style = coloredText(PanelStyle.titleStyle, scheme.contentEmphasis))
            StateBadge(scheme, asset.status)
        }
        asset.durationMillis?.let { InfoRow(scheme, "Duración", "~$it ms") }
        InfoRow(scheme, "Categoría", asset.category)
        InfoRow(scheme, "Inspiración", asset.inspiration)
        BasicText(
            text = asset.description,
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
