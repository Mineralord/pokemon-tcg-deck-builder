package com.mineralord.tcg.studio.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
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
 * **Pokémon TCG Studio — Shell inmersivo y adaptativo: el contenedor permanente.**
 *
 * Única superficie persistente del Studio. Es una **Plantilla** (VG): coloca organismos en regiones
 * fijas (R1 Toolbar · R2 Rail de Labs · R3 Host del Workspace · R4 Status Bar).
 *
 * **Infraestructura visual permanente (Studio Window & Adaptive Layout).** El Shell es el único
 * responsable de entregar a cada Lab un **área de trabajo ya adaptada**:
 *  - **Inmersión / edge-to-edge**: el lienzo de tema pinta a sangre completa (bajo el notch y las
 *    esquinas), pero el contenido se enmarca dentro de los **Safe Areas** con
 *    `WindowInsets.safeDrawing` (notch, cutout, barras transitorias): nunca tapa texto ni botones.
 *  - **Adaptación de tamaño/orientación**: un **único sistema adaptativo** (sin layouts Portrait /
 *    Landscape duplicados). Con `BoxWithConstraints` se mide el workspace real y se deriva la clase de
 *    ancho oficial ([StudioWindowWidth]); en ancho **Compacto** el Rail se coloca abajo (Portrait
 *    teléfono), en **Medio/Expandido** el Rail va al lateral (Landscape, tablet, plegable, ChromeOS).
 *
 * **Responsabilidad única frente a los Labs:** hospedarlos, conmutar entre ellos y darles un espacio
 * inset-safe y responsive. El Shell **no conoce el interior de ningún Lab**; los Labs no se preocupan
 * por orientación, notch, barras del sistema ni tamaño de pantalla.
 *
 * Toda la apariencia sale del Design System congelado a través de sus **ComponentStyle** y el
 * `StudioTheme`. **No introduce ningún componente, token, estilo ni principio nuevo.**
 *
 * @param labs Labs registrados que el Shell hospeda, en el orden en que aparecen en el Rail.
 */
@Composable
fun StudioShell(labs: List<Lab> = emptyList()) {
    val scheme = StudioTheme.colors

    // Estado de navegación: el Lab activo (único y exclusivo). null = "home" (ningún Lab abierto).
    var activeLabId by remember { mutableStateOf<String?>(null) }
    val activeLab = labs.firstOrNull { it.id == activeLabId }

    // R0 · Lienzo raíz: color de tema A SANGRE (edge-to-edge), incluida la zona del notch/cutout.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surfaceCanvas),
    ) {
        // Todo el contenido útil vive dentro del área segura: el Shell descuenta insets de notch,
        // cutout y barras del sistema una sola vez, para todas las regiones y todos los Labs.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val widthClass = StudioWindowWidth.fromWidth(maxWidth)
            if (widthClass == StudioWindowWidth.Compact) {
                CompactLayout(scheme, labs, activeLab, activeLabId) { activeLabId = it }
            } else {
                RegularLayout(scheme, labs, activeLab, activeLabId, widthClass) { activeLabId = it }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layouts adaptativos (un único sistema; sin duplicar Portrait/Landscape).
// ─────────────────────────────────────────────────────────────────────────────

/** Ancho **Medio/Expandido** (Landscape, tablet, plegable, ChromeOS): Rail lateral vertical. */
@Composable
private fun RegularLayout(
    scheme: StudioColorTokens.Scheme,
    labs: List<Lab>,
    activeLab: Lab?,
    activeLabId: String?,
    widthClass: StudioWindowWidth,
    onSelect: (String?) -> Unit,
) {
    val railWidth = if (widthClass == StudioWindowWidth.Expanded) RailWidthExpanded else RailWidth
    Column(modifier = Modifier.fillMaxSize()) {
        ToolbarRegion(scheme, activeLabTitle = activeLab?.title)   // R1
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            SideRailRegion(                                        // R2 (lateral)
                scheme = scheme,
                labs = labs,
                activeLabId = activeLabId,
                width = railWidth,
                onSelect = { onSelect(it) },
            )
            HostRegion(                                            // R3
                scheme = scheme,
                activeLab = activeLab,
                labs = labs,
                onOpen = { onSelect(it) },
                modifier = Modifier.weight(1f),
            )
        }
        StatusBarRegion(scheme, activeLabTitle = activeLab?.title) // R4
    }
}

/** Ancho **Compacto** (teléfono en Portrait): Rail horizontal inferior, sobre la Status Bar. */
@Composable
private fun CompactLayout(
    scheme: StudioColorTokens.Scheme,
    labs: List<Lab>,
    activeLab: Lab?,
    activeLabId: String?,
    onSelect: (String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ToolbarRegion(scheme, activeLabTitle = activeLab?.title)   // R1
        HostRegion(                                                // R3
            scheme = scheme,
            activeLab = activeLab,
            labs = labs,
            onOpen = { onSelect(it) },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        BottomRailRegion(                                          // R2 (inferior)
            scheme = scheme,
            labs = labs,
            activeLabId = activeLabId,
            onSelect = { onSelect(it) },
        )
        StatusBarRegion(scheme, activeLabTitle = activeLab?.title) // R4
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dimensiones de PLANTILLA (geometría de regiones, no tokens de estilo).
// ─────────────────────────────────────────────────────────────────────────────
private val RailWidth: Dp = 220.dp
private val RailWidthExpanded: Dp = 264.dp

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
        BasicText(
            text = "Pokémon TCG Studio",
            style = coloredText(StudioTypographyTokens.Role.Title, scheme.contentEmphasis),
        )
        BasicText(
            text = "›  ${activeLabTitle ?: "Studio"}",
            style = coloredText(StudioTypographyTokens.Role.Body, scheme.contentMuted),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// R2 · Rail de Labs — lista conmutable. Dos disposiciones del mismo mecanismo.
// ─────────────────────────────────────────────────────────────────────────────
/** Rail lateral (ancho medio/expandido). */
@Composable
private fun SideRailRegion(
    scheme: StudioColorTokens.Scheme,
    labs: List<Lab>,
    activeLabId: String?,
    width: Dp,
    onSelect: (String) -> Unit,
) {
    val material = SidebarStyle.material
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(width)
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
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(lab.id) },
            )
        }
    }
}

/** Rail inferior horizontal (ancho compacto / Portrait). Desplazable si no caben todos los Labs. */
@Composable
private fun BottomRailRegion(
    scheme: StudioColorTokens.Scheme,
    labs: List<Lab>,
    activeLabId: String?,
    onSelect: (String) -> Unit,
) {
    val material = SidebarStyle.material
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(material.surface(scheme))
            .topHairline(material.border?.invoke(scheme), material.borderWidth)
            .horizontalScroll(rememberScrollState())
            .padding(SidebarStyle.padding),
        horizontalArrangement = Arrangement.spacedBy(SidebarStyle.itemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labs.forEach { lab ->
            RailItem(
                scheme = scheme,
                label = lab.title,
                selected = lab.id == activeLabId,
                modifier = Modifier.width(RailWidth),
                onClick = { onSelect(lab.id) },
            )
        }
    }
}

/**
 * Fila del Rail materializada con [SidebarStyle]: fondo `selectionBackground` + barra guía de
 * `selectionEdge` cuando está activa (VG-K). El comportamiento (clic → selección) lo aporta la
 * Plantilla; la apariencia por estado la resuelve el ComponentStyle.
 */
@Composable
private fun RailItem(
    scheme: StudioColorTokens.Scheme,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val state = if (selected) VisualState.Selected else VisualState.Rest
    val colors = SidebarStyle.colors(scheme, state)
    Box(
        modifier = modifier
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
// R3 · Host del Workspace — Lab activo o estado vacío "home".
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
            // El Shell delega por completo el interior al Lab: no conoce su contenido. El Lab recibe
            // un área ya adaptada e inset-safe y solo debe rellenarla de forma responsive.
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

/** Hairline en el borde final (derecho) — separa el Rail lateral del Host. */
private fun Modifier.endHairline(color: Color?, thickness: Dp): Modifier =
    if (color == null) this else drawBehind {
        val t = thickness.toPx()
        drawRect(color, topLeft = Offset(size.width - t, 0f), size = Size(t, size.height))
    }

@Preview(name = "Studio Shell — Landscape (Expanded)", widthDp = 1280, heightDp = 800)
@Composable
private fun StudioShellExpandedPreview() {
    StudioTheme {
        StudioShell(
            labs = listOf(
                Lab(id = "preview-a", title = "Galería de Animaciones") {},
                Lab(id = "preview-b", title = "Otro Lab") {},
            ),
        )
    }
}

@Preview(name = "Studio Shell — Portrait (Compact)", widthDp = 380, heightDp = 800)
@Composable
private fun StudioShellCompactPreview() {
    StudioTheme {
        StudioShell(
            labs = listOf(
                Lab(id = "preview-a", title = "Galería de Animaciones") {},
                Lab(id = "preview-b", title = "Otro Lab") {},
            ),
        )
    }
}
