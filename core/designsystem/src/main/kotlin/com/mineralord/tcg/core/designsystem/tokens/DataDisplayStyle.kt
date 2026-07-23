package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Component Styling — Fase 10, Iteración 10.3 (Visualización de datos).
 *
 * **No define componentes nuevos, tokens nuevos ni principios nuevos.** Materializa la *apariencia*
 * (nunca el comportamiento) de los componentes de datos del PDS: Table · Tree · TreeRow · ListView ·
 * ListRow · EmptyState · Placeholder · LoadingPlaceholder · Skeleton. Los componentes del Inspector
 * (PropertyRow · InspectorSection · PropertyGroup) viven en [InspectorStyle.kt].
 *
 * **Cadena oficial de dependencia (congelada, 10.2):**
 * `Component → ComponentStyle → Visual Tokens → Theme`.
 * Estos descriptores son el ÚNICO punto autorizado a leer Visual Tokens (9.1–9.6). Cero literales.
 *
 * **Estado `editing`:** no existe un `VisualState` propio (añadirlo alteraría el `ComponentStyle`
 * congelado de 10.1). La edición en línea se materializa **reutilizando** el descriptor de campo de
 * 10.1 ([TextFieldStyle], expuesto aquí como [InlineEditFieldStyle]): la celda/fila en edición aloja
 * ese campo, cuyo estado `Focused`/`Error` ya está definido. El chasis reposa; solo foco/selección/
 * alerta suben contraste (VG-K · IC-1/5/8).
 */

/** Campo de edición en línea (celdas de tabla / valores de propiedad). Reutiliza [TextFieldStyle] de 10.1. */
val InlineEditFieldStyle: TextFieldStyle = object : TextFieldStyle() {}

// ─────────────────────────────────────────────────────────────────────────────
// Contrato común de fila de datos
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Descriptor de una **fila de datos seleccionable** (fila de tabla, de árbol o de lista): colores por
 * estado + motion + foco. La geometría (altura, sangría, paddings) la aporta cada componente.
 */
interface DataRowStyle {
    fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors
    val stateMotion: StudioMotionTokens.Role
    val focus: FocusRing
    /** Marcador de selección (borde inicial); el fondo por sí solo no comunica estado (VG-K). */
    fun selectionMarker(scheme: StudioColorTokens.Scheme): Color
}

/** Colores canónicos de una fila de datos (compartidos por Table/Tree/List). */
private fun dataRowColors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
    VisualState.Selected -> StyleColors(scheme.selectionBackground, scheme.contentEmphasis, scheme.selectionEdge)
    VisualState.Hover -> StyleColors(scheme.surfaceOverlay, scheme.contentPrimary, null)
    VisualState.Pressed -> StyleColors(scheme.surfaceRaised, scheme.contentPrimary, null)
    // Editing = foco de campo en línea (ver InlineEditFieldStyle); la fila mantiene contraste de foco.
    VisualState.Focused -> StyleColors(scheme.selectionBackground, scheme.contentPrimary, scheme.focusRing)
    VisualState.Disabled -> StyleColors(Color.Transparent, scheme.contentDisabled, null)
    VisualState.Error -> StyleColors(scheme.feedbackDangerSurface, scheme.contentPrimary, scheme.feedbackDanger)
    else -> StyleColors(Color.Transparent, scheme.contentSecondary, null)
}

// ─────────────────────────────────────────────────────────────────────────────
// Table (organismo) — rejilla de filas/columnas
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Tabla: datos tabulares alineados en columnas. Encabezado fijo (Label), celdas de texto (Body) y
 * numéricas (Numeric, mono para alinear columnas, VG-U4). Líneas de rejilla hairline; sin redondeo
 * (radio None: los datos no llevan esquina). Fila activa = `selectionBackground` + `selectionEdge`.
 *
 * Composición: [headerColors] · filas ([colors]) de altura de densidad · [gridLine] entre celdas ·
 * indicador de orden ([sortIndicator]). Edición en celda vía [InlineEditFieldStyle].
 * Estados de fila — rest/hover/pressed/selected/focused(editing)/disabled/error en [colors]. Loading =
 * [SkeletonStyle] en las celdas; vacío = [EmptyStateStyle].
 * Focus/teclado: navegación por celdas/filas con anillo único; Enter entra a edición; Esc cancela;
 * flechas mueven la selección; no se anima el desplazamiento (alta frecuencia).
 * Restricciones: sin sombra ni zebra decorativa; una fila seleccionada visible por marcador, no solo
 * por fondo; insets de columna constantes entre densidades.
 * Accesibilidad: encabezados asociados a columnas en el punto de uso; el orden no se comunica solo por
 * color (icono de orden + estado).
 */
object TableStyle : DataRowStyle {
    override val stateMotion = StudioMotionTokens.Role.Change
    override val focus = StudioFocusRing

    val rowHeight: Dp = StudioSpacingTokens.Density.Default.rowHeight
    val cellPaddingX: Dp = StudioSpacingTokens.Context.Table.CellPaddingX
    val cellPaddingY: Dp = StudioSpacingTokens.Context.Table.CellPaddingY
    val headerPaddingY: Dp = StudioSpacingTokens.Context.Table.HeaderPaddingY
    val rowGap: Dp = StudioSpacingTokens.Context.Table.RowGap
    val radius: Dp = StudioSurfaceTokens.Radius.None
    val textStyle: TextStyle = StudioTypographyTokens.Role.Body
    val numericStyle: TextStyle = StudioTypographyTokens.Role.Numeric
    val headerStyle: TextStyle = StudioTypographyTokens.Role.Label
    val editField: TextFieldStyle = InlineEditFieldStyle

    /** Encabezado de columna. */
    fun headerColors(scheme: StudioColorTokens.Scheme): StyleColors =
        StyleColors(scheme.surfacePanel, scheme.contentSecondary, scheme.borderSubtle)

    /** Líneas de rejilla (hairline). */
    fun gridLine(scheme: StudioColorTokens.Scheme): Color = scheme.borderSubtle

    /** Indicador de columna ordenada (acento). */
    fun sortIndicator(scheme: StudioColorTokens.Scheme): Color = scheme.accentRest

    override fun selectionMarker(scheme: StudioColorTokens.Scheme): Color = scheme.selectionEdge
    override fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors =
        dataRowColors(scheme, state)
}

// ─────────────────────────────────────────────────────────────────────────────
// Tree / TreeRow (organismo + molécula) — jerarquía expansible
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Árbol: jerarquía navegable con sangría por nivel. La fila ([TreeRowStyle]) es el elemento atómico
 * seleccionable; el chevron ([TreeRowStyle.chevron]) indica expandido/colapsado.
 *
 * Composición: filas de [TreeRowStyle] con sangría [indent] por nivel · guía [guideLine] opcional.
 * Restricciones: chrome de contenido (no flota); sangría fija por token.
 * Focus/teclado/accesibilidad: ver [TreeRowStyle].
 */
object TreeStyle {
    val indent: Dp = StudioSpacingTokens.Context.Tree.Indent
    val rowGap: Dp = StudioSpacingTokens.Context.Tree.RowGap
    val row: DataRowStyle = TreeRowStyle

    /** Guía vertical de jerarquía (hairline discreto). */
    fun guideLine(scheme: StudioColorTokens.Scheme): Color = scheme.borderSubtle
}

/**
 * Fila de árbol: chevron + icono + etiqueta, con estado de selección/expansión.
 *
 * Composición: [chevron] (expandir/colapsar) · icono ([iconGap]) · etiqueta (Body). Selección =
 * `selectionBackground` + barra [selectionMarker].
 * Estados — rest/hover/pressed/selected/focused(editing, renombrado en línea vía [InlineEditFieldStyle])/
 * disabled/error en [colors].
 * Focus/teclado: flechas ↑/↓ mueven; →/← expanden/colapsan; Enter activa; F2 renombra; anillo único.
 * Restricciones: expansión no se anima si es de alta frecuencia; selección por marcador (no solo fondo).
 * Accesibilidad: nivel/estado expandido expuestos en el punto de uso; el chevron tiene área de toque
 * suficiente.
 */
object TreeRowStyle : DataRowStyle {
    override val stateMotion = StudioMotionTokens.Role.Confirmation
    override val focus = StudioFocusRing

    val rowHeight: Dp = StudioSpacingTokens.Density.Default.rowHeight
    val iconGap: Dp = StudioSpacingTokens.Context.Tree.IconGap
    val radius: Dp = StudioSurfaceTokens.Radius.Control
    val textStyle: TextStyle = StudioTypographyTokens.Role.Body
    val editField: TextFieldStyle = InlineEditFieldStyle

    /** Chevron de expandir/colapsar: atenuado en reposo, primario al resaltar. */
    fun chevron(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Disabled -> scheme.contentDisabled
        VisualState.Hover, VisualState.Selected -> scheme.contentPrimary
        else -> scheme.contentMuted
    }

    override fun selectionMarker(scheme: StudioColorTokens.Scheme): Color = scheme.selectionEdge
    override fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors =
        dataRowColors(scheme, state)
}

// ─────────────────────────────────────────────────────────────────────────────
// ListView / ListRow (organismo + molécula) — lista plana
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Lista plana: filas homogéneas de una sola columna, sin jerarquía. Chasis transparente (hereda la
 * superficie del contenedor). Fila ([ListRowStyle]) seleccionable con marcador.
 *
 * Composición: filas de [ListRowStyle] separadas por [rowGap]; separador opcional [separator].
 * Restricciones: sin sombra; selección múltiple o única resuelta en el punto de uso.
 * Focus/teclado/accesibilidad: ver [ListRowStyle].
 */
object ListViewStyle {
    val rowGap: Dp = StudioSpacingTokens.Relation.Hairline
    val row: DataRowStyle = ListRowStyle

    /** Chasis: transparente (hereda superficie del host). */
    fun surface(scheme: StudioColorTokens.Scheme): Color = Color.Transparent

    /** Separador entre filas (hairline). */
    fun separator(scheme: StudioColorTokens.Scheme): Color = scheme.borderSubtle
}

/**
 * Fila de lista: contenido en una columna con estado de selección.
 *
 * Composición: icono/contenido + etiqueta (Body). Selección = `selectionBackground` + [selectionMarker].
 * Estados — rest/hover/pressed/selected/focused(editing en línea vía [InlineEditFieldStyle])/disabled/
 * error en [colors].
 * Focus/teclado: ↑/↓ mueven; Enter/Space activan; anillo único.
 * Restricciones: altura de densidad; selección por marcador; sin animación de scroll.
 * Accesibilidad: rol de opción/fila en el punto de uso; estado no solo por color.
 */
object ListRowStyle : DataRowStyle {
    override val stateMotion = StudioMotionTokens.Role.Confirmation
    override val focus = StudioFocusRing

    val rowHeight: Dp = StudioSpacingTokens.Density.Default.rowHeight
    val paddingH: Dp = StudioSpacingTokens.Inset.Sm
    val iconGap: Dp = StudioSpacingTokens.Relation.Related
    val radius: Dp = StudioSurfaceTokens.Radius.Control
    val textStyle: TextStyle = StudioTypographyTokens.Role.Body
    val editField: TextFieldStyle = InlineEditFieldStyle

    override fun selectionMarker(scheme: StudioColorTokens.Scheme): Color = scheme.selectionEdge
    override fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors =
        dataRowColors(scheme, state)
}

// ─────────────────────────────────────────────────────────────────────────────
// EmptyState (molécula) — estado sin datos
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Estado vacío: comunica ausencia de datos con título (Display, raro) + descripción (Body atenuada) +
 * acción opcional (botón de 10.1). Silencio por defecto; icono/ilustración discreta.
 *
 * Composición: icono ([iconColor]) · [titleStyle] · [bodyStyle] en `contentMuted` · acción (Button 10.1).
 * Estados/focus/hover/pressed/selected/editing: **no aplica** (no interactivo salvo su acción, que es
 * un botón de 10.1). loading → usar [LoadingPlaceholderStyle]; error → tono `feedbackDanger` en el texto.
 * Restricciones: centrado, aire generoso (Section); no compite con el chrome.
 * Accesibilidad: texto legible con contraste; la acción hereda foco único del botón.
 */
object EmptyStateStyle {
    val titleStyle: TextStyle = StudioTypographyTokens.Role.Display
    val bodyStyle: TextStyle = StudioTypographyTokens.Role.Body
    val gap: Dp = StudioSpacingTokens.Relation.Group
    val padding: Dp = StudioSpacingTokens.Relation.Section

    fun titleColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentSecondary
    fun bodyColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentMuted
    fun iconColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentMuted
}

// ─────────────────────────────────────────────────────────────────────────────
// Placeholder / LoadingPlaceholder / Skeleton (átomos) — contenido diferido
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Marcador de contenido genérico (hueco reservado antes de haber datos). Pozo hundido neutral con
 * texto guía atenuado; reserva espacio para evitar saltos de layout.
 *
 * Composición: superficie [surface] (sunken) + texto guía ([textColor], Caption).
 * Estados/interacción: **no aplica** (no interactivo). error → variante con borde `feedbackDanger` en
 * el punto de uso.
 * Restricciones: sin sombra; radio Container; nunca decorativo.
 * Accesibilidad: se anuncia como región de estado en el punto de uso.
 */
object PlaceholderStyle {
    val radius: Dp = StudioSurfaceTokens.Radius.Container
    val padding: Dp = StudioSpacingTokens.Inset.Md
    val textStyle: TextStyle = StudioTypographyTokens.Role.Caption

    fun surface(scheme: StudioColorTokens.Scheme): Color = scheme.surfaceSunken
    fun textColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentMuted
}

/**
 * Marcador de carga: spinner (10.1 [SpinnerStyle]) + leyenda (Caption atenuada). Giro constante
 * (Linear); el spinner comunica progreso indeterminado sin bloquear el layout.
 *
 * Composición: [SpinnerStyle] centrado + [labelStyle] en `contentMuted`.
 * Estados: loading es su único estado; al resolver, se sustituye por el contenido o por [EmptyStateStyle].
 * Restricciones: un spinner por región; no se acompaña de más movimiento (VG-MOV).
 * Accesibilidad: se anuncia estado "cargando"; respeta `prefers-reduced-motion` (el spinner es la
 * excepción funcional mínima).
 */
object LoadingPlaceholderStyle {
    val spinner = SpinnerStyle
    val labelStyle: TextStyle = StudioTypographyTokens.Role.Caption
    val gap: Dp = StudioSpacingTokens.Relation.Related

    fun labelColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentMuted
}

/**
 * Esqueleto: bloque de carga con brillo que recorre. Base [baseColor] (raised) + destello
 * [highlightColor] (overlay); movimiento constante (Linear, [motion]) que no compite con el contenido.
 *
 * Composición: bloques de radio [radius] rellenos con base→destello→base.
 * Estados: loading es su único estado; al resolver se reemplaza por el contenido real.
 * Restricciones: nunca `scale(0)`; el brillo es lineal y sutil; se detiene con `prefers-reduced-motion`
 * (queda el bloque base estático) en el punto de uso.
 * Accesibilidad: puramente presentacional; la región anuncia "cargando" en el punto de uso.
 */
object SkeletonStyle {
    val radius: Dp = StudioSurfaceTokens.Radius.Control
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Continuity
    val shimmerEasing: Easing = StudioMotionTokens.Foundation.Easing.Linear

    fun baseColor(scheme: StudioColorTokens.Scheme): Color = scheme.surfaceRaised
    fun highlightColor(scheme: StudioColorTokens.Scheme): Color = scheme.surfaceOverlay
}
