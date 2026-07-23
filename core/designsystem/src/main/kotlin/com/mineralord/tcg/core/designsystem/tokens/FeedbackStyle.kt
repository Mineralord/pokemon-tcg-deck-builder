package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Component Styling — Fase 10, Iteración 10.4 (Feedback no bloqueante y popovers).
 *
 * **No define componentes nuevos, tokens nuevos, principios nuevos ni estados nuevos.** Materializa la
 * *apariencia* (nunca el comportamiento) del feedback transitorio del PDS: Tooltip · Popover ·
 * Snackbar · Toast. Los diálogos y overlays bloqueantes viven en [OverlayStyle.kt].
 *
 * **Cadena oficial de dependencia (congelada, 10.2):**
 * `Component → ComponentStyle → Visual Tokens → Theme`. Estos descriptores son el ÚNICO punto que lee
 * Visual Tokens (9.1–9.6). Cero literales.
 *
 * **Jerarquía respetada al pie de la letra:** cada componente declara su [StudioDepthTokens.Plane] y
 * su [StudioSurfaceTokens.Material] existentes; la elevación sale SIEMPRE de
 * [StudioSurfaceTokens.Elevation.forPlane]. Motion solo con roles de 9.6; foco = anillo único.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Tooltip (átomo) — etiqueta contextual efímera
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Tooltip: aclaración breve anclada a su disparador. Superficie Floating; texto Caption. Entra
 * origen-consciente (desde el disparador) con [StudioMotionTokens.Role.Change] (salida más rápida);
 * nunca `scale(0)`. Tras el primer tooltip, los siguientes aparecen sin retardo ni animación (Emil).
 *
 * Composición: [material] (Floating) · [textColor] (contentPrimary) sobre `surfaceOverlay` · padding
 * [padding].
 * Estados/focus/hover/pressed/disabled/loading/error: **no aplica** — el tooltip es puramente
 * informativo y NO interactivo (A2); no recibe foco.
 * Teclado: aparece con foco por teclado del disparador; se descarta con Esc; su aparición por teclado
 * no se anima (alta frecuencia).
 * Restricciones: una línea corta preferente; no contiene controles; Plane.Floating (no interrumpe).
 * Composición: nunca dentro de otro tooltip; convive bajo diálogos/toasts sin taparlos.
 */
object TooltipStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Floating
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Tooltip
    val textStyle: TextStyle = StudioTypographyTokens.Role.Caption
    val padding: Dp = StudioSpacingTokens.Inset.Sm
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change

    fun elevation(): Dp = StudioSurfaceTokens.Elevation.forPlane(plane)
    fun textColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentPrimary
}

// ─────────────────────────────────────────────────────────────────────────────
// Popover (organismo) — panel flotante anclado con contenido interactivo
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Popover: contenedor flotante anclado a un disparador que aloja contenido interactivo (controles de
 * 10.1). Superficie Floating; entra origen-consciente con [StudioMotionTokens.Role.Change].
 *
 * Composición: [material] (Floating) · cuerpo con [padding] y [contentGap] · controles de 10.1.
 * Estados — el chasis reposa (rest); Focused eleva la frontera a `focusRing` cuando el foco está
 * dentro. hover/pressed/selected/disabled/loading/error los aportan los controles internos.
 * Teclado: atrapa foco mientras abierto; Tab recorre; Esc cierra (cierre por teclado no se anima).
 * Restricciones: Plane.Floating (no bloquea el fondo, no lleva scrim); origen anclado al disparador.
 * Composición: puede contener Menu/controles; nunca un Dialog (eso sube a Modal).
 */
object PopoverStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Floating
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Popover
    val padding: Dp = StudioSpacingTokens.Context.Panel.Padding
    val contentGap: Dp = StudioSpacingTokens.Relation.Related
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change
    val focus: FocusRing = StudioFocusRing

    fun elevation(): Dp = StudioSurfaceTokens.Elevation.forPlane(plane)

    /** Frontera del chasis según el foco esté dentro o no. */
    fun chassisBorder(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Focused -> scheme.focusRing
        else -> scheme.borderDefault
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Snackbar (molécula) — feedback breve con acción opcional
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Snackbar: mensaje breve no bloqueante con acción opcional (deshacer). Plano Notification (nunca
 * queda bajo un modal); material Toast; texto Status; acción en acento. Entra/sale con
 * [StudioMotionTokens.Role.Change] (salida más rápida); pausa su temporizador al hover (Emil, punto de uso).
 *
 * Composición: [material] (Toast) · [textStyle] (Status, contentPrimary) · acción ([actionColor]).
 * Estados — rest neutral; hover pausa (sin cambio de color); error = tono `feedbackDanger` en [tone].
 * focus/pressed/disabled/selected: la acción es un botón de 10.1 (aporta sus estados). loading: no aplica.
 * Teclado: foco alcanza la acción; se descarta con Esc; su aparición no roba foco.
 * Restricciones: una snackbar a la vez; una sola acción; Plane.Notification.
 * Composición: nunca contiene controles complejos ni formularios.
 */
object SnackbarStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Toast
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Toast
    val textStyle: TextStyle = StudioTypographyTokens.Role.Status
    val padding: Dp = StudioSpacingTokens.Inset.Md
    val actionGap: Dp = StudioSpacingTokens.Relation.Group
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change

    fun elevation(): Dp = StudioSurfaceTokens.Elevation.forPlane(plane)
    fun actionColor(scheme: StudioColorTokens.Scheme): Color = scheme.accentRest

    /** Acento del mensaje según su significado (reutiliza la escala de [BadgeStyle.Tone]). */
    fun tone(scheme: StudioColorTokens.Scheme, tone: BadgeStyle.Tone): Color =
        BadgeStyle.colors(scheme, tone).content
}

// ─────────────────────────────────────────────────────────────────────────────
// Toast (molécula) — notificación del sistema apilable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Toast: notificación del sistema, apilable, no bloqueante. Plano Notification; material Toast; banda
 * de tono según significado (IC-8). Transiciones interrumpibles (CSS-like, no keyframes) para el
 * apilado rápido; salida más rápida que entrada. Nunca `scale(0)`.
 *
 * Composición: [material] (Toast) · icono/banda de [accent] por tono · título (Body) + detalle
 * (Caption, [detailColor]) · cierre (IconButton de 10.1).
 * Estados — rest; hover pausa el temporizador; error = tono Danger; loading = variante con
 * [SpinnerStyle] (10.1) en lugar de icono de tono.
 * focus/pressed/disabled/selected: los aporta el botón de cierre/acción.
 * Teclado: foco accesible al cierre/acción; Esc descarta el toast enfocado.
 * Restricciones: apilado con hueco relleno para no perder el hover (Emil, punto de uso); Plane.Notification.
 * Composición: sin formularios; máximo una acción + cierre.
 */
object ToastStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Toast
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Toast
    val titleStyle: TextStyle = StudioTypographyTokens.Role.Body
    val detailStyle: TextStyle = StudioTypographyTokens.Role.Caption
    val padding: Dp = StudioSpacingTokens.Inset.Md
    val gap: Dp = StudioSpacingTokens.Relation.Related
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change

    fun elevation(): Dp = StudioSurfaceTokens.Elevation.forPlane(plane)
    fun detailColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentSecondary

    /** Acento/banda del toast según su tono (reutiliza [BadgeStyle.Tone]). */
    fun accent(scheme: StudioColorTokens.Scheme, tone: BadgeStyle.Tone): Color =
        BadgeStyle.colors(scheme, tone).content
}
