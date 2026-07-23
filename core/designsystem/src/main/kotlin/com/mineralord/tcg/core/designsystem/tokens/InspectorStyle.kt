package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Component Styling — Fase 10, Iteración 10.3 (Inspector).
 *
 * **No define componentes nuevos, tokens nuevos ni principios nuevos.** Materializa la *apariencia*
 * (nunca el comportamiento) de los componentes de edición estructurada del Inspector: PropertyRow ·
 * InspectorSection · PropertyGroup. Los componentes tabulares/listas viven en [DataDisplayStyle.kt].
 *
 * **Cadena oficial de dependencia (congelada, 10.2):**
 * `Component → ComponentStyle → Visual Tokens → Theme`.
 * Estos descriptores son el ÚNICO punto autorizado a leer Visual Tokens (9.1–9.6). Cero literales.
 *
 * **Estado `editing`:** se materializa reutilizando el campo de 10.1 ([InlineEditFieldStyle]); no se
 * añade `VisualState` (el `ComponentStyle` de 10.1 está congelado). El chasis reposa; solo foco/
 * selección/alerta suben contraste (VG-K · IC-1/5/8).
 */

// ─────────────────────────────────────────────────────────────────────────────
// PropertyRow (molécula) — par etiqueta / valor
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Fila de propiedad: etiqueta (Label, atenuada) + valor (Body/Numeric) editable en línea. Alineación
 * de columnas etiqueta/valor constante (VG-U4). Reposo neutral; edición usa el campo de 10.1.
 *
 * Composición: [labelStyle] en `contentSecondary` · separación [labelValueGap] · valor ([valueStyle]/
 * [numericStyle]) · campo de edición [editField] en `editing`.
 * Estados — rest: valor `contentPrimary` · Hover: fondo `surfaceOverlay` · Focused/editing: campo con
 * borde `focusRing` (vía [editField]) · Disabled: `contentDisabled` · Error: valor/borde `feedbackDanger`.
 * pressed/selected: no aplican (la fila no se selecciona; se edita su valor).
 * Focus/teclado: Tab recorre propiedades; Enter confirma edición; Esc revierte; anillo único.
 * Restricciones: una etiqueta y un valor por fila; alto de fila de propiedad; sin sombra.
 * Accesibilidad: etiqueta asociada al control de valor en el punto de uso; estado de error con
 * mensaje adyacente (no solo color).
 */
object PropertyRowStyle {
    val rowHeight: Dp = StudioSpacingTokens.Context.Property.RowHeight
    val rowGap: Dp = StudioSpacingTokens.Context.Property.RowGap
    val labelValueGap: Dp = StudioSpacingTokens.Context.Property.LabelValueGap
    val radius: Dp = StudioSurfaceTokens.Radius.Control
    val labelStyle: TextStyle = StudioTypographyTokens.Role.Label
    val valueStyle: TextStyle = StudioTypographyTokens.Role.Body
    val numericStyle: TextStyle = StudioTypographyTokens.Role.Numeric
    val stateMotion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change
    val focus: FocusRing = StudioFocusRing
    /** Campo para la edición en línea del valor (descriptor de 10.1). */
    val editField: TextFieldStyle = InlineEditFieldStyle

    /** Color de la etiqueta (siempre secundaria: el valor es el protagonista). */
    fun labelColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentSecondary

    /** Fondo + color de valor + borde por estado de la fila. */
    fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Hover -> StyleColors(scheme.surfaceOverlay, scheme.contentPrimary, null)
        VisualState.Focused -> StyleColors(scheme.surfaceSunken, scheme.contentPrimary, scheme.focusRing)
        VisualState.Disabled -> StyleColors(Color.Transparent, scheme.contentDisabled, null)
        VisualState.Error -> StyleColors(Color.Transparent, scheme.feedbackDanger, scheme.feedbackDanger)
        else -> StyleColors(Color.Transparent, scheme.contentPrimary, null)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PropertyGroup (molécula) — conjunto de propiedades relacionadas
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Grupo de propiedades: agrupa [PropertyRowStyle] relacionadas bajo una etiqueta de grupo (Label).
 * La proximidad comunica pertenencia (VG-E1); la separación entre grupos (VG-E2) usa [groupGap].
 *
 * Composición: encabezado de grupo ([groupLabelStyle] en `contentSecondary`) · filas con [rowGap] ·
 * separación exterior [groupGap] entre grupos.
 * Estados/focus/hover/pressed/selected/editing/disabled: los aportan las filas; el grupo no tiene
 * estado propio salvo Disabled (atenúa el encabezado). loading → filas con [SkeletonStyle].
 * Focus/teclado: recorre las filas contenidas; el encabezado no es enfocable salvo colapsable en el
 * punto de uso.
 * Restricciones: sin recuadro decorativo (la proximidad basta); un nivel de etiqueta por grupo.
 * Accesibilidad: el encabezado nombra el grupo de controles asociado.
 */
object PropertyGroupStyle {
    val groupLabelStyle: TextStyle = StudioTypographyTokens.Role.Label
    val rowGap: Dp = StudioSpacingTokens.Context.Property.RowGap
    val groupGap: Dp = StudioSpacingTokens.Context.Inspector.GroupGap
    val labelGap: Dp = StudioSpacingTokens.Relation.Related

    fun groupLabelColor(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Disabled -> scheme.contentDisabled
        else -> scheme.contentSecondary
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// InspectorSection (organismo) — sección colapsable del Inspector
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sección del Inspector: bloque colapsable con encabezado (Section) que agrupa [PropertyGroupStyle].
 * Chrome de contenido (Inspector material); el chevron indica expandida/colapsada.
 *
 * Composición: [material] (surfacePanel + hairline) · encabezado ([headerColors], [titleStyle]) con
 * [chevron] · separador ([headerDivider]) · cuerpo con [padding] y [groupGap] entre grupos.
 * Estados del encabezado — rest: `contentPrimary` · Hover: `surfaceOverlay` · Focused: borde `focusRing`
 * · Disabled: `contentDisabled`. selected/editing: no aplican al encabezado (residen en las filas).
 * Focus/teclado: Enter/Space colapsan-expanden el encabezado enfocado; Tab entra a las propiedades;
 * anillo único; el colapso de alta frecuencia no se anima.
 * Restricciones: un nivel de sección; sin sombra (no flota); no compite con el contenido.
 * Accesibilidad: encabezado con estado expandido/colapsado expuesto; el chevron tiene área suficiente.
 */
object InspectorSectionStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Inspector
    val padding: Dp = StudioSpacingTokens.Context.Inspector.Padding
    val rowGap: Dp = StudioSpacingTokens.Context.Inspector.RowGap
    val groupGap: Dp = StudioSpacingTokens.Context.Inspector.GroupGap
    val titleStyle: TextStyle = StudioTypographyTokens.Role.Section
    val stateMotion: StudioMotionTokens.Role = StudioMotionTokens.Role.Transformation
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Inspector
    val focus: FocusRing = StudioFocusRing

    /** Chevron de expandir/colapsar la sección. */
    fun chevron(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Disabled -> scheme.contentDisabled
        VisualState.Hover -> scheme.contentPrimary
        else -> scheme.contentMuted
    }

    /** Encabezado: fondo, título y borde inferior por estado. */
    fun headerColors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Hover -> StyleColors(scheme.surfaceOverlay, scheme.contentEmphasis, scheme.borderSubtle)
        VisualState.Focused -> StyleColors(scheme.surfacePanel, scheme.contentEmphasis, scheme.focusRing)
        VisualState.Disabled -> StyleColors(scheme.surfacePanel, scheme.contentDisabled, scheme.borderSubtle)
        else -> StyleColors(scheme.surfacePanel, scheme.contentPrimary, scheme.borderSubtle)
    }

    /** Separador entre encabezado y cuerpo (hairline). */
    fun headerDivider(scheme: StudioColorTokens.Scheme): Color = scheme.borderSubtle
}
