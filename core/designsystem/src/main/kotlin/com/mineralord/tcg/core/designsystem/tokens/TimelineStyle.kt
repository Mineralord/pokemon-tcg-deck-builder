package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Component Styling — Fase 10, Iteración 10.5 (Timeline del Developer Studio).
 *
 * **No define componentes nuevos, tokens nuevos, principios nuevos, estados nuevos ni materiales
 * nuevos.** Materializa la *apariencia* (nunca el comportamiento) de los componentes de línea de
 * tiempo del Studio: Timeline · TimelineTrack · TimelineMarker · TimelineSelection. Los componentes de
 * Preview viven en [PreviewStyle.kt].
 *
 * **Cadena oficial de dependencia (congelada, 10.2):**
 * `Component → ComponentStyle → Visual Tokens → Theme`. Único punto que lee Visual Tokens. Cero literales.
 *
 * **Legibilidad sobre decoración (VG-K · VL-fatiga):** chrome de contenido en silencio; las marcas de
 * tiempo son numéricas (mono, alineación), la selección usa `selectionEdge`/`selectionBackground`.
 * Movimiento solo con roles de 9.6.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Timeline (organismo) — eje temporal
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Línea de tiempo: eje horizontal con reglilla de tiempo y pistas apiladas. Chrome de contenido
 * ([material]); prioriza la legibilidad de las marcas sobre cualquier adorno.
 *
 * Composición: [material] (Timeline) · reglilla con etiquetas [rulerStyle] (Numeric) en [rulerColor] ·
 * líneas de división [gridLine] (hairline) · pistas ([TimelineTrackStyle]) con [trackGap] · cabezal de
 * reproducción [playhead].
 * Estados — el chasis reposa; hover/selección viven en pistas y marcadores. loading = pistas con
 * [SkeletonStyle] (10.1).
 * focus/pressed/selected/disabled: los aportan pistas/marcadores.
 * Accesibilidad: marcas numéricas legibles; el cabezal no depende solo del color (posición + línea).
 * Restricciones: Plane.Content (no flota); sin sombra; densidad de datos, no decoración.
 * Composición: aloja TimelineTrack; la selección de rango es [TimelineSelectionStyle].
 */
object TimelineStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Timeline
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Timeline
    val padding: Dp = StudioSpacingTokens.Context.Timeline.Padding
    val trackGap: Dp = StudioSpacingTokens.Context.Timeline.TrackGap
    val rulerStyle: TextStyle = StudioTypographyTokens.Role.Numeric
    val stateMotion: StudioMotionTokens.Role = StudioMotionTokens.Role.Continuity

    fun rulerColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentMuted
    fun gridLine(scheme: StudioColorTokens.Scheme): Color = scheme.borderSubtle
    /** Cabezal de reproducción: acento único, fino (no compite con el contenido). */
    fun playhead(scheme: StudioColorTokens.Scheme): Color = scheme.accentRest
}

// ─────────────────────────────────────────────────────────────────────────────
// TimelineTrack (molécula) — pista/carril de la línea de tiempo
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pista de la línea de tiempo: carril horizontal que aloja marcadores/segmentos. Fondo hundido
 * discreto; cabecera de pista con etiqueta (Label).
 *
 * Composición: cabecera ([headerColors], Label) · carril ([laneColors]) de [height] · [gridLine].
 * Estados — rest: carril `surfaceSunken` · Hover: `surfaceOverlay` · Selected (pista activa):
 * `selectionBackground` + borde `selectionEdge` · Disabled: `contentDisabled` (atenuada).
 * pressed: transitorio de arrastre (surfaceRaised). loading: [SkeletonStyle].
 * focus: anillo único al enfocar la pista por teclado.
 * Accesibilidad: cabecera nombra la pista; estado activo por borde + fondo, no solo color.
 * Restricciones: altura de fila de densidad; sin adornos; una pista activa marcada.
 * Composición: contiene TimelineMarker; participa en TimelineSelection.
 */
object TimelineTrackStyle {
    val height: Dp = StudioSpacingTokens.Density.Default.rowHeight
    val headerStyle: TextStyle = StudioTypographyTokens.Role.Label
    val radius: Dp = StudioSurfaceTokens.Radius.None
    val stateMotion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change
    val focus: FocusRing = StudioFocusRing

    fun gridLine(scheme: StudioColorTokens.Scheme): Color = scheme.borderSubtle

    /** Cabecera de la pista. */
    fun headerColors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Disabled -> StyleColors(scheme.surfacePanel, scheme.contentDisabled, scheme.borderSubtle)
        VisualState.Selected -> StyleColors(scheme.surfacePanel, scheme.contentEmphasis, scheme.selectionEdge)
        else -> StyleColors(scheme.surfacePanel, scheme.contentSecondary, scheme.borderSubtle)
    }

    /** Carril (lane) donde caen los marcadores. */
    fun laneColors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Selected -> StyleColors(scheme.selectionBackground, scheme.contentPrimary, scheme.selectionEdge)
        VisualState.Hover -> StyleColors(scheme.surfaceOverlay, scheme.contentPrimary, null)
        VisualState.Pressed -> StyleColors(scheme.surfaceRaised, scheme.contentPrimary, null)
        VisualState.Disabled -> StyleColors(scheme.surfacePanel, scheme.contentDisabled, null)
        else -> StyleColors(scheme.surfaceSunken, scheme.contentSecondary, null)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TimelineMarker (átomo) — keyframe / evento puntual
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Marcador de la línea de tiempo: keyframe/evento puntual sobre una pista. Discreto en reposo; sube a
 * acento al resaltar y a `selectionEdge` al seleccionar. Se reposiciona con
 * [StudioMotionTokens.Role.Continuity] (misma identidad que se mueve).
 *
 * Composición: figura de [size] rellena por [fill], borde por [border].
 * Estados — rest: `contentMuted` · Hover: `accentRest` · Pressed (arrastre): `accentActive` ·
 * Selected: relleno `accentRest` + borde `selectionEdge` · Disabled: `contentDisabled`.
 * focus: anillo único cuando se enfoca por teclado.
 * Accesibilidad: no depende solo del color (forma + posición); área de toque suficiente.
 * Restricciones: tamaño mínimo; no compite con el contenido de la pista; acento único.
 * Composición: vive dentro de TimelineTrack; su rango de influencia es TimelineSelection.
 */
object TimelineMarkerStyle {
    val size: Dp = StudioSpacingTokens.Foundation.Space.S12
    val radius: Dp = StudioSurfaceTokens.Radius.Full
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Continuity
    val focus: FocusRing = StudioFocusRing

    fun fill(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Hover -> scheme.accentRest
        VisualState.Pressed -> scheme.accentActive
        VisualState.Selected -> scheme.accentRest
        VisualState.Disabled -> scheme.contentDisabled
        else -> scheme.contentMuted
    }

    fun border(scheme: StudioColorTokens.Scheme, state: VisualState): Color? = when (state) {
        VisualState.Selected -> scheme.selectionEdge
        else -> null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TimelineSelection (átomo) — selección de rango temporal
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Selección de rango en la línea de tiempo: banda que cubre el intervalo elegido. Relleno tenue
 * ([fill]) + bordes de recorte ([edge]) con `selectionEdge` (único color de selección del PDS).
 * Aparece/ajusta con [StudioMotionTokens.Role.Change].
 *
 * Composición: banda [fill] (`selectionBackground`) entre dos [edge] verticales (`selectionEdge`, [edgeWidth]).
 * Estados — rest/Selected activa; Disabled la atenúa. hover/pressed: ajuste de asas en el punto de uso.
 * focus/loading/accesibilidad: el rango se anuncia numéricamente; no depende solo del color.
 * Restricciones: usa EXCLUSIVAMENTE `selectionEdge`/`selectionBackground` (canon); sin sombra.
 * Composición: se superpone a TimelineTrack; no interactúa con marcadores fuera del rango.
 */
object TimelineSelectionStyle {
    val edgeWidth: Dp = StudioSurfaceTokens.Border.Focus
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change

    fun fill(scheme: StudioColorTokens.Scheme): Color = scheme.selectionBackground
    fun edge(scheme: StudioColorTokens.Scheme): Color = scheme.selectionEdge
}
