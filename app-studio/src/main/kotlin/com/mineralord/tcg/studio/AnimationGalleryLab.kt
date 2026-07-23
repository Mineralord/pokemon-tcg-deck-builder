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
 * **Galería de Animaciones — Lab v2: Preview real (contenido del Lab).**
 *
 * Es un Lab NORMAL: su único contacto con el Shell es ser el `content` de un `Lab` en [studioLabs].
 * El Studio actúa aquí como **anfitrión del motor de animaciones**: monta el `AnimationStage` real y
 * obtiene el `AnimationDirector` desde el composition root ÚNICO compartido
 * ([rememberCanonicalAnimationDirector]). Al pulsar «Reproducir» se envía la request de dominio real
 * por la misma fachada que usará el juego (`director.submit(...)`), y el mismo pipeline (Scheduler →
 * Runner → Definition → Executor → RenderNode) reproduce la animación en el escenario.
 *
 * **No existe ninguna implementación de animación exclusiva del Studio:** las recetas, ejecutores y
 * el director viven en `core:animation-compose`; el Studio sólo provee dónde dibujar (registro de
 * coordenadas + estado de render) y las ranuras rastreadas.
 *
 * Fuera de alcance (diferido): timeline, inspector, comparador, edición, parámetros, importación,
 * catálogo automático. Se compone con el Design System congelado; cero tokens/componentes nuevos.
 */

private const val PREVIEW_PLAYER = "studio"

/** Ficha de una animación canónica: metadatos + la request de dominio REAL que la dispara. */
private data class GalleryAnimation(
    val id: String,
    val name: String,
    val category: String,
    val duration: String,
    val description: String,
    val request: AnimationRequest,
)

/** Animaciones canónicas ya existentes en el pipeline compartido (v1.0). */
private val CanonicalAnimations: List<GalleryAnimation> = listOf(
    GalleryAnimation(
        id = "draw-card",
        name = "Robar carta",
        category = "N1 · Gameplay",
        duration = "~320 ms",
        description = "Carta que viaja del mazo a la mano con arco leve, escala y overshoot sobrio " +
            "(estándar «Snap sobrio»). Primera animación canónica del backlog (ANIM #001).",
        request = AnimationRequest.CardDrawn(playerId = PREVIEW_PLAYER, cardId = "preview"),
    ),
    GalleryAnimation(
        id = "move-card",
        name = "Mover carta",
        category = "N1 · Gameplay",
        duration = "~260 ms",
        description = "Desplazamiento de una carta entre dos ranuras (mano → Activo) por la capa de " +
            "vuelo. Slice vertical que validó el pipeline de extremo a extremo.",
        request = AnimationRequest.PokemonPlayed(playerId = PREVIEW_PLAYER, pokemonId = "preview"),
    ),
)

/** Contenido del Lab «Galería de Animaciones», dibujado en el Host del Shell. */
@Composable
fun AnimationGalleryLabContent() {
    val scheme = StudioTheme.colors
    var selectedId by remember { mutableStateOf(CanonicalAnimations.first().id) }
    val selected = CanonicalAnimations.first { it.id == selectedId }

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
            text = "${CanonicalAnimations.size} animaciones canónicas · motor compartido",
            style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
        )

        // Rejilla mínima (2 columnas) de animaciones seleccionables.
        CanonicalAnimations.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
            ) {
                rowItems.forEach { anim ->
                    GalleryTile(
                        scheme = scheme,
                        anim = anim,
                        selected = anim.id == selectedId,
                        onClick = { selectedId = anim.id },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }

        // Escenario de Preview: el AnimationStage REAL con las ranuras rastreadas.
        PreviewStage(scheme, coordinates, renderState)

        // Acción: reproducir la animación seleccionada por la fachada real del motor.
        PrimaryAction(scheme, label = "Reproducir «${selected.name}»") {
            director.submit(selected.request)
        }

        // Información básica de la animación seleccionada.
        DetailPanel(scheme, selected)
    }
}

private val StageHeight: Dp = 320.dp
private val SlotWidth: Dp = 56.dp
private val SlotHeight: Dp = 80.dp

/**
 * Escenario del Preview: monta el [AnimationStage] real y publica las tres ranuras canónicas
 * (mazo, mano, Activo) con [trackBounds], para que los ejecutores resuelvan sus rectángulos reales.
 * La animación en vuelo la dibuja el pipeline en la capa de vuelo del Stage.
 */
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
                    SlotMarker(scheme, "Mano", handSlotId(PREVIEW_PLAYER), Modifier.align(Alignment.Center))
                    SlotMarker(scheme, "Activo", activeSlotId(PREVIEW_PLAYER), Modifier.align(Alignment.BottomEnd))
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

/** Casilla seleccionable de la rejilla, materializada con [ListRowStyle] (fila de datos). */
@Composable
private fun GalleryTile(
    scheme: StudioColorTokens.Scheme,
    anim: GalleryAnimation,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = if (selected) VisualState.Selected else VisualState.Rest
    val colors = ListRowStyle.colors(scheme, state)
    Column(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(colors.container)
            .selectionGuide(colors.border, ListRowStyle.selectionMarker(scheme))
            .clickable(onClick = onClick)
            .padding(horizontal = ListRowStyle.paddingH, vertical = ListRowStyle.paddingH),
        verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
    ) {
        BasicText(text = anim.name, style = coloredText(ListRowStyle.textStyle, colors.content))
        BasicText(
            text = anim.category,
            style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
        )
    }
}

/** Botón primario materializado con [ButtonStyle] (10.1). El comportamiento (clic) es del anfitrión. */
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

/** Panel con la información básica de la animación seleccionada ([PanelStyle]). */
@Composable
private fun DetailPanel(scheme: StudioColorTokens.Scheme, anim: GalleryAnimation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(PanelStyle.material.surface(scheme))
            .topHairline(PanelStyle.headerDivider(scheme), PanelStyle.material.borderWidth)
            .padding(PanelStyle.contentPadding),
        verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
    ) {
        BasicText(text = anim.name, style = coloredText(PanelStyle.titleStyle, scheme.contentEmphasis))
        InfoRow(scheme, "Categoría", anim.category)
        InfoRow(scheme, "Duración", anim.duration)
        BasicText(
            text = anim.description,
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
