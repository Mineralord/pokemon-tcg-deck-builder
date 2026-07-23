package com.mineralord.tcg.studio.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mineralord.tcg.core.designsystem.tokens.BadgeStyle
import com.mineralord.tcg.core.designsystem.tokens.ButtonStyle
import com.mineralord.tcg.core.designsystem.tokens.ButtonVariant
import com.mineralord.tcg.core.designsystem.tokens.DividerStyle
import com.mineralord.tcg.core.designsystem.tokens.EmptyStateStyle
import com.mineralord.tcg.core.designsystem.tokens.SidebarStyle
import com.mineralord.tcg.core.designsystem.tokens.StatusBarStyle
import com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.core.designsystem.tokens.StudioTypographyTokens
import com.mineralord.tcg.core.designsystem.tokens.ToolbarStyle
import com.mineralord.tcg.core.designsystem.tokens.VisualState

/**
 * **Pokémon TCG Studio — Boot + Shell (11.1): el contenedor permanente.**
 *
 * Única superficie persistente del Studio. Es una **Plantilla** (VG): solo *coloca organismos* en
 * regiones fijas (R1 Toolbar · R2 Rail de Labs · R3 Host del Workspace · R4 Status Bar). No aloja
 * ninguna herramienta todavía; el Host muestra el estado vacío (`EmptyStateStyle`) del "home".
 *
 * Toda la apariencia sale del Design System congelado a través de sus **ComponentStyle**
 * (`ToolbarStyle`, `SidebarStyle`, `StatusBarStyle`, `EmptyStateStyle`, `ButtonStyle`, `DividerStyle`)
 * y el `StudioTheme`. **No introduce ningún componente, token, estilo ni principio nuevo.**
 *
 * Debe invocarse dentro de un `StudioTheme { … }` (que provee el esquema de color activo).
 */
@Composable
fun StudioShell() {
    val scheme = StudioTheme.colors

    // R0 · Lienzo raíz: responsabilidad del Theme (surfaceCanvas). Los huecos entre regiones lo
    // muestran; el Shell no pinta superficie propia más allá de este lienzo de tema.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surfaceCanvas),
    ) {
        ToolbarRegion(scheme)                       // R1
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            RailRegion(scheme)                      // R2
            HostRegion(scheme, modifier = Modifier.weight(1f))  // R3
        }
        StatusBarRegion(scheme)                     // R4
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dimensiones de PLANTILLA (geometría de regiones, no tokens de estilo).
// El Design System gobierna la apariencia (color/tipografía/relaciones/materiales); la Plantilla
// decide dónde y con qué tamaño se colocan las regiones (11.1).
// ─────────────────────────────────────────────────────────────────────────────
private val RailWidth: Dp = 220.dp

// ─────────────────────────────────────────────────────────────────────────────
// R1 · Barra global (Toolbar)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ToolbarRegion(scheme: StudioColorTokens.Scheme) {
    val material = ToolbarStyle.material
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ToolbarStyle.height)
            .background(material.surface(scheme))
            .bottomHairline(material.border?.invoke(scheme), material.borderWidth)
            .padding(horizontal = ToolbarStyle.paddingH, vertical = ToolbarStyle.paddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ToolbarStyle.gap),
    ) {
        // Identidad + ruta de contexto (compuesta con tipografía; sin componente "breadcrumb" nuevo).
        BasicText(
            text = "Pokémon TCG Studio",
            style = coloredText(StudioTypographyTokens.Role.Title, scheme.contentEmphasis),
        )
        BasicText(
            text = "›  Studio",
            style = coloredText(StudioTypographyTokens.Role.Body, scheme.contentMuted),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// R2 · Rail de Labs (Sidebar) — vacío en 11.1 (aún no hay Labs).
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RailRegion(scheme: StudioColorTokens.Scheme) {
    val material = SidebarStyle.material
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(RailWidth)
            .background(material.surface(scheme))
            .endHairline(material.border?.invoke(scheme), material.borderWidth)
            .padding(SidebarStyle.padding),
        verticalArrangement = Arrangement.spacedBy(SidebarStyle.itemGap),
    ) {
        // Sin Labs registrados todavía: el Rail queda como lista persistente vacía (11.1).
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// R3 · Host del Workspace (Dock) — estado vacío "home".
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HostRegion(scheme: StudioColorTokens.Scheme, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(EmptyStateStyle.padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EmptyStateStyle.gap),
        ) {
            BasicText(
                text = "Studio listo",
                style = coloredText(EmptyStateStyle.titleStyle, EmptyStateStyle.titleColor(scheme)),
            )
            BasicText(
                text = "Ningún laboratorio abierto.",
                style = coloredText(EmptyStateStyle.bodyStyle, EmptyStateStyle.bodyColor(scheme)),
            )
            // Acción primaria del "home". Deshabilitada: la Galería de Animaciones aún no existe.
            DisabledPrimaryAction(scheme, label = "Abrir la Galería de Animaciones")
        }
    }
}

/** Botón primario en estado deshabilitado, materializado con [ButtonStyle] (10.1). No interactivo aún. */
@Composable
private fun DisabledPrimaryAction(scheme: StudioColorTokens.Scheme, label: String) {
    val style = ButtonStyle(ButtonVariant.Primary)
    val colors = style.colors(scheme, VisualState.Disabled)
    Box(
        modifier = Modifier
            .height(style.metrics.height)
            .background(colors.container, RoundedCornerShape(style.metrics.radius))
            .padding(horizontal = style.metrics.paddingH),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = label, style = coloredText(style.textStyle, colors.content))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// R4 · Status Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatusBarRegion(scheme: StudioColorTokens.Scheme) {
    val material = StatusBarStyle.material
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(StatusBarStyle.height)
            .background(material.surface(scheme))
            .topHairline(material.border?.invoke(scheme), material.borderWidth)
            .padding(horizontal = StatusBarStyle.paddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StatusBarStyle.segmentGap),
    ) {
        StatusSegment(scheme, "Motor: listo", BadgeStyle.Tone.Neutral)
        Box(
            modifier = Modifier
                .width(DividerStyle.thickness)
                .fillMaxHeight()
                .background(DividerStyle.subtle(scheme)),
        )
        StatusSegment(scheme, "Validación: —", BadgeStyle.Tone.Neutral)
    }
}

@Composable
private fun StatusSegment(scheme: StudioColorTokens.Scheme, text: String, tone: BadgeStyle.Tone) {
    BasicText(
        text = text,
        style = coloredText(StatusBarStyle.textStyle, StatusBarStyle.segment(scheme, tone).content),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers de Plantilla (composición pura; no definen apariencia nueva).
// ─────────────────────────────────────────────────────────────────────────────

/** Aplica el color resuelto por el ComponentStyle a un rol tipográfico del tema. */
private fun coloredText(role: TextStyle, color: Color): TextStyle = role.copy(color = color)

/** Hairline inferior (frontera de región); el color/grosor vienen del Material del ComponentStyle. */
private fun Modifier.bottomHairline(color: Color?, thickness: Dp): Modifier =
    if (color == null) this else drawBehind {
        val t = thickness.toPx()
        drawRect(color, topLeft = Offset(0f, size.height - t), size = Size(size.width, t))
    }

/** Hairline superior (frontera de región). */
private fun Modifier.topHairline(color: Color?, thickness: Dp): Modifier =
    if (color == null) this else drawBehind {
        val t = thickness.toPx()
        drawRect(color, topLeft = Offset(0f, 0f), size = Size(size.width, t))
    }

/** Hairline en el borde final (derecho) — separa el Rail del Host. */
private fun Modifier.endHairline(color: Color?, thickness: Dp): Modifier =
    if (color == null) this else drawBehind {
        val t = thickness.toPx()
        drawRect(color, topLeft = Offset(size.width - t, 0f), size = Size(t, size.height))
    }

@Preview(name = "Studio Shell 11.1", widthDp = 1280, heightDp = 800)
@Composable
private fun StudioShellPreview() {
    StudioTheme {
        StudioShell()
    }
}
