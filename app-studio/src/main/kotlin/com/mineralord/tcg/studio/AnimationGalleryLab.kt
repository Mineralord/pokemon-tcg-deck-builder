package com.mineralord.tcg.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.mineralord.tcg.core.designsystem.tokens.ListRowStyle
import com.mineralord.tcg.core.designsystem.tokens.PanelStyle
import com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.core.designsystem.tokens.StudioTypographyTokens
import com.mineralord.tcg.core.designsystem.tokens.VisualState

/**
 * **Galería de Animaciones — Lab v1 (contenido real).**
 *
 * Es un Lab NORMAL: su único punto de contacto con el Shell es ser el `content` de un `Lab` en
 * [studioLabs]. No recibe trato especial por ser el primero; el mismo mecanismo (registrar → hospedar
 * → conmutar) sirve para cualquier Lab futuro. Este Sprint demuestra que la infraestructura de Labs ya
 * soporta contenido real: una rejilla de las animaciones canónicas existentes, seleccionables, con su
 * información básica, todo dentro del Host del Shell.
 *
 * Fuera de alcance (no aquí): preview, timeline, inspector, comparador, edición, pipeline, importación.
 *
 * Se compone EXCLUSIVAMENTE con el Design System congelado ([PanelStyle], [ListRowStyle], tipografía):
 * cero tokens/componentes nuevos.
 */

/** Ficha de una animación canónica del catálogo (solo información básica en v1). */
private data class GalleryAnimation(
    val id: String,
    val name: String,
    val category: String,
    val duration: String,
    val description: String,
)

/**
 * Animaciones canónicas ya existentes en el pipeline (v1.0). Catálogo mínimo de v1; crece cuando el
 * backlog de animaciones canonice nuevas piezas.
 */
private val CanonicalAnimations: List<GalleryAnimation> = listOf(
    GalleryAnimation(
        id = "draw-card",
        name = "Robar carta",
        category = "N1 · Gameplay",
        duration = "~320 ms",
        description = "Carta que viaja del mazo a la mano con arco leve, escala y overshoot sobrio " +
            "(estándar «Snap sobrio»). Primera animación canónica del backlog (ANIM #001).",
    ),
    GalleryAnimation(
        id = "move-card",
        name = "Mover carta",
        category = "N1 · Gameplay",
        duration = "variable",
        description = "Desplazamiento de una carta entre dos ranuras (slot → slot) por la capa de " +
            "vuelo. Slice vertical que validó el pipeline de extremo a extremo.",
    ),
)

/** Contenido del Lab «Galería de Animaciones», dibujado en el Host del Shell. */
@Composable
fun AnimationGalleryLabContent() {
    val scheme = StudioTheme.colors
    var selectedId by remember { mutableStateOf(CanonicalAnimations.first().id) }
    val selected = CanonicalAnimations.first { it.id == selectedId }

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
            text = "${CanonicalAnimations.size} animaciones canónicas",
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
                // Rellena la fila impar para conservar el ancho de columna.
                if (rowItems.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }

        // Información básica de la animación seleccionada.
        DetailPanel(scheme, selected)
    }
}

private val TileHeight: Dp = 72.dp

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
            .height(TileHeight)
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
