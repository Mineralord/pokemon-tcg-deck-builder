package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Component Styling — Fase 10, Iteración 10.2 (Navegación).
 *
 * **No define componentes nuevos, tokens nuevos ni principios nuevos.** Materializa la *apariencia*
 * (nunca el comportamiento) de los organismos de navegación del PDS: Tabs · Toolbar · Sidebar ·
 * Dock · Menu · ContextMenu · StatusBar. Los contenedores puros (Divider · ScrollArea · Scrollbar ·
 * Panel · Window) viven en [ContainerStyle.kt].
 *
 * **Cadena oficial de dependencia (Design System):**
 * `Component → ComponentStyle → Visual Tokens → Theme`.
 * Ningún componente/composable puede consumir Visual Tokens directamente: consume EXCLUSIVAMENTE su
 * ComponentStyle (los descriptores de este fichero). Estos descriptores son el ÚNICO punto que lee
 * Color (9.1), Typography (9.2), Spacing (9.3), Depth (9.4), Surface (9.5) y Motion (9.6). Cero
 * valores hardcodeados.
 *
 * Reutiliza el vocabulario común de 10.1 ([VisualState], [StyleColors], [ControlStyle], [FocusRing],
 * [StudioFocusRing]). El chasis reposa; solo foco/selección/alerta suben contraste (VG-K · IC-1/5/8).
 */

// ─────────────────────────────────────────────────────────────────────────────
// Contrato común de navegación
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Descriptor de un **item de navegación** (pestaña, fila de sidebar, entrada de menú): colores por
 * estado + tipografía + motion + foco. El chrome del contenedor que lo aloja se expresa como
 * [StudioSurfaceTokens.Material] aparte.
 */
interface NavItemStyle {
    fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors
    val textStyle: TextStyle
    val stateMotion: StudioMotionTokens.Role
    val focus: FocusRing
}

// ─────────────────────────────────────────────────────────────────────────────
// Tabs (molécula) — franja de pestañas con indicador de selección
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pestañas: navegación entre vistas hermanas. El indicador (subrayado de [StudioColorTokens.Scheme.selectionEdge])
 * se desliza con [StudioMotionTokens.Role.Continuity] (misma identidad que se reposiciona). El chasis
 * de la franja reposa; solo la pestaña activa sube a `contentEmphasis` + indicador (VG-K).
 *
 * Composición: franja ([track]) con hairline inferior · items de texto (o texto+icono) alineados ·
 * indicador fino bajo la pestaña activa.
 * Restricciones: no lleva sombra (chrome de Plane.Content); la selección es única y exclusiva; el
 * borde en [StyleColors] representa el **indicador**, no un recuadro completo.
 * Accesibilidad: foco = anillo único ([StudioFocusRing], A5/IC-5); estado activo NO se comunica solo
 * por color (indicador + `contentEmphasis`); altura ≥ fila de densidad para área de toque.
 */
object TabStyle : NavItemStyle {
    override val textStyle = StudioTypographyTokens.Role.Label
    override val stateMotion = StudioMotionTokens.Role.Continuity
    override val focus = StudioFocusRing

    val height: Dp = StudioSpacingTokens.Density.Default.rowHeight
    val paddingH: Dp = StudioSpacingTokens.Inset.Md
    val gap: Dp = StudioSpacingTokens.Relation.Related
    /** Grosor del indicador de la pestaña activa. */
    val indicatorThickness: Dp = StudioSurfaceTokens.Border.Focus

    /** Chrome de la franja: hairline inferior separadora, sin redondeo (chrome de contenido). */
    fun track(scheme: StudioColorTokens.Scheme): StyleColors =
        StyleColors(scheme.surfacePanel, scheme.contentSecondary, scheme.borderSubtle)

    /** Color del indicador de la pestaña activa. */
    fun indicator(scheme: StudioColorTokens.Scheme): Color = scheme.selectionEdge

    override fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Selected -> StyleColors(Color.Transparent, scheme.contentEmphasis, scheme.selectionEdge)
        VisualState.Hover -> StyleColors(scheme.surfaceOverlay, scheme.contentPrimary, null)
        VisualState.Pressed -> StyleColors(scheme.surfaceRaised, scheme.contentPrimary, null)
        VisualState.Disabled -> StyleColors(Color.Transparent, scheme.contentDisabled, null)
        else -> StyleColors(Color.Transparent, scheme.contentSecondary, null)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Toolbar (organismo) — barra de acciones
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Barra de herramientas: chrome estructural que aloja botones (10.1 `IconButtonStyle`/`ButtonStyle`)
 * y grupos separados por hairline. No redefine los botones; aporta el material del chasis, las
 * métricas de la barra y el color del separador de grupos.
 *
 * Composición: [material] (surfacePanel + hairline, sin redondeo) · grupos con [groupDivider] ·
 * items = controles de 10.1.
 * Restricciones: Plane.Content (no flota); no introduce estados propios (los estados viven en los
 * botones); insets horizontales constantes entre densidades (memoria muscular, VG-U4).
 * Accesibilidad: los botones aportan foco/hover/pressed/disabled; el divisor es decorativo (no
 * enfocable). Altura de fila = densidad activa.
 */
object ToolbarStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Toolbar
    val paddingH: Dp = StudioSpacingTokens.Context.Toolbar.PaddingH
    val paddingV: Dp = StudioSpacingTokens.Context.Toolbar.PaddingV
    val gap: Dp = StudioSpacingTokens.Context.Toolbar.Gap
    val height: Dp = StudioSpacingTokens.Density.Default.rowHeight
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Dock

    /** Separador vertical entre grupos de acciones (hairline). */
    fun groupDivider(scheme: StudioColorTokens.Scheme): Color = scheme.borderSubtle
}

// ─────────────────────────────────────────────────────────────────────────────
// Sidebar (organismo) — lista de navegación vertical
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Barra lateral: navegación jerárquica persistente. Fila seleccionada = `selectionBackground` +
 * barra guía izquierda de `selectionEdge` (IC-1); hover discreto; sin sombra (Plane.Content).
 *
 * Composición: [material] (surfacePanel + hairline) · filas de altura de densidad · barra de
 * selección ([selectionBar]) al borde inicial de la fila activa · icono + etiqueta.
 * Restricciones: selección única visible; la barra guía es el marcador primario (no solo color de
 * fondo, VG-K); sangría de sub-niveles = `Context.Tree.Indent` en el punto de uso.
 * Accesibilidad: foco = anillo único; estado activo por barra + `contentEmphasis`, no solo por color.
 */
object SidebarStyle : NavItemStyle {
    override val textStyle = StudioTypographyTokens.Role.Body
    override val stateMotion = StudioMotionTokens.Role.Confirmation
    override val focus = StudioFocusRing

    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Sidebar
    val padding: Dp = StudioSpacingTokens.Context.Sidebar.Padding
    val itemGap: Dp = StudioSpacingTokens.Context.Sidebar.ItemGap
    val itemHeight: Dp = StudioSpacingTokens.Context.Sidebar.ItemHeight
    val itemRadius: Dp = StudioSurfaceTokens.Radius.Control
    val indent: Dp = StudioSpacingTokens.Context.Tree.Indent
    val iconGap: Dp = StudioSpacingTokens.Context.Tree.IconGap
    /** Grosor de la barra guía de la fila seleccionada. */
    val selectionBarWidth: Dp = StudioSurfaceTokens.Border.Focus

    /** Color de la barra guía de la fila activa (borde inicial). */
    fun selectionBar(scheme: StudioColorTokens.Scheme): Color = scheme.selectionEdge

    override fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Selected -> StyleColors(scheme.selectionBackground, scheme.contentEmphasis, scheme.selectionEdge)
        VisualState.Hover -> StyleColors(scheme.surfaceOverlay, scheme.contentPrimary, null)
        VisualState.Pressed -> StyleColors(scheme.surfaceRaised, scheme.contentPrimary, null)
        VisualState.Disabled -> StyleColors(Color.Transparent, scheme.contentDisabled, null)
        else -> StyleColors(Color.Transparent, scheme.contentSecondary, null)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dock (organismo) — contenedor de paneles acoplables con pestañas
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dock: aloja paneles acoplables y sus pestañas de encabezado. Chrome de contenido; el flyout de un
 * panel colapsado se eleva a Plane.Floating (material [flyout]). Las pestañas del dock reutilizan la
 * semántica de [TabStyle].
 *
 * Composición: [material] del chasis (surfacePanel + hairline) · tira de pestañas ([tab]) · área de
 * contenido del panel activo · [flyout] flotante cuando el panel está colapsado.
 * Restricciones: solo el flyout sube de plano (VG-D3); una pestaña activa por dock.
 * Accesibilidad: pestañas con foco único; el flyout no roba foco salvo interacción explícita.
 */
object DockStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Panel
    val flyout: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Floating
    val padding: Dp = StudioSpacingTokens.Context.Dock.Padding
    val gap: Dp = StudioSpacingTokens.Context.Dock.Gap
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Dock
    val flyoutPlane: StudioDepthTokens.Plane = StudioDepthTokens.Context.DockFlyout

    /** Pestañas del dock: misma materialización que [TabStyle]. */
    val tab: NavItemStyle = TabStyle
}

// ─────────────────────────────────────────────────────────────────────────────
// Menu / ContextMenu (organismos) — listas de comandos flotantes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Menú flotante (menú de aplicación y menú contextual comparten materialización). Superficie
 * Floating; item resaltado = acento tenue; separador hairline; pista de atajo en `contentMuted`.
 *
 * Composición: [material] (surfaceOverlay + borde + sombra Floating) · items ([colors]) · [separator]
 * entre grupos · etiqueta (Body) + [shortcutStyle]/[shortcutColor] a la derecha.
 * Restricciones: entra desde su disparador (origen-consciente en el punto de uso) con
 * [StudioMotionTokens.Role.Change] (salida más rápida que entrada); nunca `scale(0)`. Item destructivo
 * usa `feedbackDanger` como contenido.
 * Accesibilidad: navegable por teclado (acción iniciada por teclado NO se anima en el punto de uso);
 * foco = anillo único; resaltado no depende solo del color (acento tenue + `contentEmphasis`).
 */
object MenuStyle : NavItemStyle {
    override val textStyle = StudioTypographyTokens.Role.Body
    override val stateMotion = StudioMotionTokens.Role.Change
    override val focus = StudioFocusRing

    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Floating
    val padding: Dp = StudioSpacingTokens.Context.Overlay.Padding
    val itemGap: Dp = StudioSpacingTokens.Context.Overlay.ItemGap
    val itemHeight: Dp = StudioSpacingTokens.Density.Default.rowHeight
    val itemPaddingH: Dp = StudioSpacingTokens.Inset.Sm
    val iconGap: Dp = StudioSpacingTokens.Relation.Related
    val itemRadius: Dp = StudioSurfaceTokens.Radius.Control
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Menu

    /** Tipografía de la pista de atajo (Shortcut Hint, A7 · IC-5). */
    val shortcutStyle: TextStyle = StudioTypographyTokens.Role.Shortcut

    fun shortcutColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentMuted

    /** Separador entre grupos de comandos (hairline). */
    fun separator(scheme: StudioColorTokens.Scheme): Color = scheme.borderSubtle

    override fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        // "Selected" = item resaltado/activo por teclado o puntero (highlight, IC-1).
        VisualState.Selected, VisualState.Hover -> StyleColors(scheme.accentMuted, scheme.contentEmphasis, null)
        VisualState.Pressed -> StyleColors(scheme.accentMuted, scheme.contentPrimary, null)
        VisualState.Disabled -> StyleColors(Color.Transparent, scheme.contentDisabled, null)
        VisualState.Error -> StyleColors(Color.Transparent, scheme.feedbackDanger, null)
        else -> StyleColors(Color.Transparent, scheme.contentPrimary, null)
    }
}

/** Menú contextual: misma materialización flotante que [MenuStyle] (invocado por clic secundario). */
object ContextMenuStyle : NavItemStyle by MenuStyle

// ─────────────────────────────────────────────────────────────────────────────
// StatusBar (organismo) — barra de estado inferior
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Barra de estado: segmentos informativos de baja jerarquía en el borde inferior. Chrome de
 * contenido con hairline superior; texto `Status`; segmentos separados por hairline. Los tonos de
 * feedback se reservan para estado real (IC-8), nunca decoración.
 *
 * Composición: [material] (surfacePanel + hairline superior) · segmentos ([segment]) de altura
 * compacta · separadores ([divider]).
 * Restricciones: silencio por defecto (color neutral); solo un segmento de alerta a la vez; sin
 * sombra (no interrumpe). Densidad compacta (información residual, no protagonista).
 * Accesibilidad: no interactivo por defecto (A2); si un segmento es accionable, hereda foco único;
 * el estado no se comunica solo por color (icono/texto acompaña al tono).
 */
object StatusBarStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Toolbar
    val height: Dp = StudioSpacingTokens.Density.Compact.rowHeight
    val paddingH: Dp = StudioSpacingTokens.Inset.Sm
    val segmentGap: Dp = StudioSpacingTokens.Relation.Related
    val textStyle: TextStyle = StudioTypographyTokens.Role.Status

    /** Tono de un segmento según su significado (reutiliza la escala de [BadgeStyle.Tone]). */
    fun segment(scheme: StudioColorTokens.Scheme, tone: BadgeStyle.Tone): StyleColors =
        BadgeStyle.colors(scheme, tone)

    /** Separador vertical entre segmentos (hairline). */
    fun divider(scheme: StudioColorTokens.Scheme): Color = scheme.borderSubtle
}
