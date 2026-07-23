package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Component Styling — Fase 10, Iteración 10.5 (Preview y manipulación directa del Developer Studio).
 *
 * **No define componentes nuevos, tokens nuevos, principios nuevos, estados nuevos ni materiales
 * nuevos.** Materializa la *apariencia* (nunca el comportamiento) del lienzo de Preview y sus
 * adornos de edición: Preview · PreviewOverlay · SelectionFrame · TransformHandle · ResizeHandle ·
 * GuideLine · DropTarget. La Timeline vive en [TimelineStyle.kt].
 *
 * **Cadena oficial de dependencia (congelada, 10.2):**
 * `Component → ComponentStyle → Visual Tokens → Theme`. Único punto que lee Visual Tokens. Cero literales.
 *
 * **Protagonismo absoluto del contenido (VL-CONT · VG-K):** el telón es acromático, los adornos de
 * edición NUNCA compiten con el contenido; la selección usa EXCLUSIVAMENTE `selectionEdge`. Movimiento
 * solo con roles de 9.6.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Preview (organismo) — lienzo de contenido
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Preview: telón acromático donde el contenido es el protagonista absoluto. Permanece en
 * **Plane.Content**; aire generoso alrededor; sin chrome que compita.
 *
 * Composición: [material] (Preview, `previewBackdrop`) · [padding] generoso · contenido centrado.
 * Estados — el lienzo reposa; hover/selección/edición viven en los adornos (overlay/frame/handles).
 * loading = [SkeletonStyle]/[BusyOverlayStyle]; no introduce estado propio.
 * focus/pressed/selected/disabled: no aplican al lienzo (los aportan los objetos editados).
 * Accesibilidad: contraste del telón subordinado al contenido; sin ruido visual.
 * Restricciones: Plane.Content SIEMPRE (no sube de plano); sin sombra ni borde; telón neutro.
 * Composición: aloja PreviewOverlay y los adornos de manipulación directa.
 */
object PreviewStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Preview
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Preview
    val padding: Dp = StudioSpacingTokens.Context.Preview.Padding

    /** Telón alterno (p. ej. tablero de transparencia) — acromático de 9.1. */
    fun backdropAlt(scheme: StudioColorTokens.Scheme): Color = scheme.previewBackdropAlt
}

// ─────────────────────────────────────────────────────────────────────────────
// PreviewOverlay (organismo) — capa de controles sobre el lienzo
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Capa de controles del Preview (zoom, encuadre, herramientas): se eleva apenas a
 * [StudioDepthTokens.Context.PreviewControls] (Plane.Raised) sin robar protagonismo. Chasis
 * transparente: los controles son botones/segmentos de 10.1.
 *
 * Composición: [surface] transparente · controles de 10.1 con [padding].
 * Estados — los aportan los controles (hover/pressed/focus/disabled/selected). loading: no aplica.
 * Accesibilidad: foco único en los controles; no atrapa foco (no es modal).
 * Restricciones: Plane.Raised máximo (nunca Floating/Modal); mínima superficie; se desvanece cuando
 * no se usa (Change) en el punto de uso.
 * Composición: nunca contiene contenido editable; solo controles del visor.
 */
object PreviewOverlayStyle {
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.PreviewControls
    val padding: Dp = StudioSpacingTokens.Inset.Sm
    val gap: Dp = StudioSpacingTokens.Relation.Related
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change

    /** Chasis transparente: hereda el telón del Preview (protagonismo del contenido). */
    fun surface(scheme: StudioColorTokens.Scheme): Color = Color.Transparent

    fun elevation(): Dp = StudioSurfaceTokens.Elevation.forPlane(plane)
}

// ─────────────────────────────────────────────────────────────────────────────
// SelectionFrame (átomo) — marco de selección del objeto
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Marco de selección: recuadro que delimita el objeto seleccionado. Usa ÚNICAMENTE `selectionEdge`
 * (canon), sin relleno que oculte el contenido. Aparece con [StudioMotionTokens.Role.Change].
 *
 * Composición: borde [edge] (`selectionEdge`, [strokeWidth]) alrededor del bounding box.
 * Estados — Selected activa; Disabled lo atenúa a `borderStrong`. hover: previsualización del marco
 * (mismo color, menor énfasis en el punto de uso). focus: coincide con la selección.
 * loading/pressed: no aplican al marco.
 * Accesibilidad: la selección no depende solo del color (marco + asas); el objeto se anuncia seleccionado.
 * Restricciones: exclusivamente `selectionEdge`; sin sombra ni relleno; grosor fino.
 * Composición: acompaña a TransformHandle/ResizeHandle; nunca compite con el contenido.
 */
object SelectionFrameStyle {
    val strokeWidth: Dp = StudioSurfaceTokens.Border.Focus
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change

    fun edge(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Disabled -> scheme.borderStrong
        else -> scheme.selectionEdge
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TransformHandle / ResizeHandle (átomos) — asas de manipulación directa
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Asa de transformación: punto de agarre para mover/rotar. Pequeña y neutra en reposo; NUNCA compite
 * visualmente con el contenido. Sube contraste solo bajo el puntero/arrastre. Se mueve con
 * [StudioMotionTokens.Role.Continuity].
 *
 * Composición: cuadro de [size] relleno [fill] con borde [border] (contraste sobre el contenido).
 * Estados — rest: `surfaceRaised` + borde `selectionEdge` · Hover: relleno `contentEmphasis` ·
 * Pressed (arrastre): `accentActive` · Disabled: `contentDisabled` · Selected = rest visible.
 * focus: anillo único al tabular entre asas.
 * Accesibilidad: área de agarre ≥ mínimo; cursor/forma indican la acción (no solo color).
 * Restricciones: tamaño mínimo; contraste reservado (VG-K); acento único.
 * Composición: acompaña a SelectionFrame; varias asas por objeto sin saturar.
 */
object TransformHandleStyle {
    val size: Dp = StudioSpacingTokens.Foundation.Space.S8
    val radius: Dp = StudioSurfaceTokens.Radius.Control
    val borderWidth: Dp = StudioSurfaceTokens.Border.Thin
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Continuity
    val focus: FocusRing = StudioFocusRing

    fun fill(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Hover -> scheme.contentEmphasis
        VisualState.Pressed -> scheme.accentActive
        VisualState.Disabled -> scheme.contentDisabled
        else -> scheme.surfaceRaised
    }

    fun border(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Disabled -> scheme.borderSubtle
        else -> scheme.selectionEdge
    }
}

/** Asa de redimensionado: misma materialización que [TransformHandleStyle] (asas de borde/esquina). */
object ResizeHandleStyle {
    val size: Dp = TransformHandleStyle.size
    val radius: Dp = TransformHandleStyle.radius
    val borderWidth: Dp = TransformHandleStyle.borderWidth
    val motion: StudioMotionTokens.Role = TransformHandleStyle.motion
    val focus: FocusRing = TransformHandleStyle.focus

    fun fill(scheme: StudioColorTokens.Scheme, state: VisualState): Color =
        TransformHandleStyle.fill(scheme, state)

    fun border(scheme: StudioColorTokens.Scheme, state: VisualState): Color =
        TransformHandleStyle.border(scheme, state)
}

// ─────────────────────────────────────────────────────────────────────────────
// GuideLine (átomo) — guía de alineación / snapping
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Guía de alineación: línea fina que aparece al alinear/snappear. Permanece **neutra** (no acento):
 * es una ayuda, no contenido. Aparece/desaparece con [StudioMotionTokens.Role.Change].
 *
 * Composición: línea de [thickness] con color [color] (neutro).
 * Estados — visible durante el arrastre/alineación; Disabled no aplica. hover/pressed/selected/focus:
 * no aplican (no interactiva, A2). loading: no aplica.
 * Accesibilidad: puramente presentacional; complementa el snapping, no lo sustituye.
 * Restricciones: neutra (`borderDefault`), fina, sin sombra; nunca usa el acento ni `selectionEdge`.
 * Composición: se superpone al Preview durante la manipulación; no persiste en reposo.
 */
object GuideLineStyle {
    val thickness: Dp = StudioSurfaceTokens.Border.Thin
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change

    /** Guía neutra (no compite; no es selección ni acento). */
    fun color(scheme: StudioColorTokens.Scheme): Color = scheme.borderDefault
}

// ─────────────────────────────────────────────────────────────────────────────
// DropTarget (átomo) — zona de destino de arrastre
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Objetivo de soltado: resalta la zona donde caería el elemento arrastrado. Relleno tenue + borde de
 * acento tenue; rechazo usa feedback Alerta. Aparece con [StudioMotionTokens.Role.Change].
 *
 * Composición: relleno [fill] + borde [border] ([borderWidth]).
 * Estados — rest: invisible (transparente) · Hover/Selected (destino válido activo): `accentMuted` +
 * borde `selectionEdge` · Error (destino inválido): `feedbackDangerSurface` + borde `feedbackDanger` ·
 * Disabled: transparente.
 * pressed/focus/loading: no aplican (indicador transitorio de arrastre).
 * Accesibilidad: la validez no se comunica solo por color (borde + cambio de cursor en el punto de uso).
 * Restricciones: transitorio (solo durante drag); acento tenue; sin sombra.
 * Composición: se superpone a la zona destino (lista, carril, lienzo); no persiste tras soltar.
 */
object DropTargetStyle {
    val borderWidth: Dp = StudioSurfaceTokens.Border.Focus
    val radius: Dp = StudioSurfaceTokens.Radius.Control
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change

    fun fill(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Hover, VisualState.Selected -> scheme.accentMuted
        VisualState.Error -> scheme.feedbackDangerSurface
        else -> Color.Transparent
    }

    fun border(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Error -> scheme.feedbackDanger
        VisualState.Disabled -> scheme.borderSubtle
        else -> scheme.selectionEdge
    }
}
