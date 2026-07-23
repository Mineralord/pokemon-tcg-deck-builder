package com.mineralord.tcg.studio.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
 * **Pokémon TCG Studio — Boot + Shell + Navegación v1: el contenedor permanente conmutable.**
 *
 * Única superficie persistente del Studio. Es una **Plantilla** (VG): solo *coloca organismos* en
 * regiones fijas (R1 Toolbar · R2 Rail de Labs · R3 Host del Workspace · R4 Status Bar).
 *
 * **Responsabilidad única frente a los Labs:** hospedarlos y permitir conmutar entre ellos. El Shell
 * recibe la lista de [labs] registrados y **no conoce el interior de ninguno** (ni siquiera de la
 * Galería de Animaciones, que es solo el primer Lab registrado). Al seleccionar un Lab en el Rail:
 * el Host (R3) muestra su [Lab.content] y la ruta de contexto (R1) refleja su [Lab.title]. Sin ningún
 * Lab activo, el Host muestra el estado vacío ("home"). Este mecanismo sirve para cualquier Lab futuro.
 *
 * Toda la apariencia sale del Design System congelado a través de sus **ComponentStyle**
 * (`ToolbarStyle`, `SidebarStyle`, `StatusBarStyle`, `EmptyStateStyle`, `ButtonStyle`, `DividerStyle`)
 * y el `StudioTheme`. **No introduce ningún componente, token, estilo ni principio nuevo.**
 *
 * Debe invocarse dentro de un `StudioTheme { … }` (que provee el esquema de color activo).
 *
 * @param labs Labs registrados que el Shell hospeda, en el orden en que aparecen en el Rail.
 */
@Composable
fun StudioShell(labs: List<Lab> = emptyList()) {
    val scheme = StudioTheme.colors

    // Estado de navegación: el Lab activo (único y exclusivo). null = "home" (ningún Lab abierto).
    var activeLabId by remember { mutableStateOf<String?>(null) }
    val activeLab = labs.firstOrNull { it.id == activeLabId }

    // R0 · Lienzo raíz: responsabilidad del Theme (surfaceCanvas). Los huecos entre regiones lo
    // muestran; el Shell no pinta superficie propia más allá de este lienzo de tema.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surfaceCanvas),
    ) {
        ToolbarRegion(scheme, activeLabTitle = activeLab?.title)   // R1
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            RailRegion(                                            // R2
                scheme = scheme,
                labs = labs,
                activeLabId = activeLabId,
                onSelect = { activeLabId = it },
            )
            HostRegion(                                            // R3
                scheme = scheme,
                activeLab = activeLab,
                labs = labs,
                onOpen = { activeLabId = it },
                modifier = Modifier.weight(1f),
            )
        }
        StatusBarRegion(scheme, activeLabTitle = activeLab?.title) // R4
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
private fun ToolbarRegion(scheme: StudioColorTokens.Scheme, activeLabTitle: String?) {
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
        // La ruta refleja el Lab activo; sin Lab, muestra la raíz "Studio".
        BasicText(
            text = "›  ${activeLabTitle ?: "Studio"}",
            style = coloredText(StudioTypographyTokens.Role.Body, scheme.contentMuted),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// R2 · Rail de Labs (Sidebar) — lista conmutable de Labs registrados.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RailRegion(
    scheme: StudioColorTokens.Scheme,
    labs: List<Lab>,
    activeLabId: String?,
    onSelect: (String) -> Unit,
) {
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
        labs.forEach { lab ->
            RailItem(
                scheme = scheme,
                label = lab.title,
                selected = lab.id == activeLabId,
                onClick = { onSelect(lab.id) },
            )
        }
    }
}

/**
 * Fila del Rail materializada con [SidebarStyle]: fondo `selectionBackground` + barra guía de
 * `selectionEdge` cuando está activa (VG-K: no solo color). El comportamiento (clic → selección) lo
 * aporta la Plantilla; la apariencia por estado la resuelve el ComponentStyle.
 */
@Composable
private fun RailItem(
    scheme: StudioColorTokens.Scheme,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val state = if (selected) VisualState.Selected else VisualState.Rest
    val colors = SidebarStyle.colors(scheme, state)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SidebarStyle.itemHeight)
            .clip(RoundedCornerShape(SidebarStyle.itemRadius))
            .background(colors.container)
            .selectionGuide(colors.border, SidebarStyle.selectionBarWidth)
            .clickable(onClick = onClick)
            .padding(horizontal = SidebarStyle.iconGap),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(text = label, style = coloredText(SidebarStyle.textStyle, colors.content))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// R3 · Host del Workspace (Dock) — Lab activo o estado vacío "home".
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HostRegion(
    scheme: StudioColorTokens.Scheme,
    activeLab: Lab?,
    labs: List<Lab>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = if (activeLab == null) Alignment.Center else Alignment.TopStart,
    ) {
        if (activeLab != null) {
            // El Shell delega por completo el interior al Lab: no conoce su contenido.
            activeLab.content()
        } else {
            HomeEmptyState(scheme, labs, onOpen)
        }
    }
}

/** Estado vacío del Host cuando ningún Lab está activo. Genérico: no cita ningún Lab por nombre. */
@Composable
private fun HomeEmptyState(scheme: StudioColorTokens.Scheme, labs: List<Lab>, onOpen: (String) -> Unit) {
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
            text = if (labs.isEmpty()) "Ningún laboratorio disponible." else "Elige un laboratorio en el Rail.",
            style = coloredText(EmptyStateStyle.bodyStyle, EmptyStateStyle.bodyColor(scheme)),
        )
        // Acción primaria del "home": abre el primer Lab registrado (dirigida por datos, no acoplada).
        labs.firstOrNull()?.let { first ->
            PrimaryAction(scheme, label = "Abrir ${first.title}", onClick = { onOpen(first.id) })
        }
    }
}

/** Botón primario materializado con [ButtonStyle] (10.1). El comportamiento (clic) es de la Plantilla. */
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

// ─────────────────────────────────────────────────────────────────────────────
// R4 · Status Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatusBarRegion(scheme: StudioColorTokens.Scheme, activeLabTitle: String?) {
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
        StatusSegment(scheme, "Lab: ${activeLabTitle ?: "—"}", BadgeStyle.Tone.Neutral)
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

/** Barra guía de selección al borde inicial (izquierdo) de la fila activa (SidebarStyle). */
private fun Modifier.selectionGuide(color: Color?, thickness: Dp): Modifier =
    if (color == null) this else drawBehind {
        val t = thickness.toPx()
        drawRect(color, topLeft = Offset(0f, 0f), size = Size(t, size.height))
    }

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

@Preview(name = "Studio Shell — Navegación v1", widthDp = 1280, heightDp = 800)
@Composable
private fun StudioShellPreview() {
    StudioTheme {
        StudioShell(
            labs = listOf(
                Lab(id = "preview-a", title = "Galería de Animaciones") {},
                Lab(id = "preview-b", title = "Otro Lab") {},
            ),
        )
    }
}
