package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Component Styling — Fase 10, Iteración 10.4 (Diálogos y overlays bloqueantes).
 *
 * **No define componentes nuevos, tokens nuevos, principios nuevos ni estados nuevos.** Materializa la
 * *apariencia* (nunca el comportamiento) de los componentes que **interrumpen** el flujo: Dialog ·
 * ModalDialog · ConfirmationDialog · ProgressOverlay · BusyOverlay · BlockingOverlay. El feedback no
 * bloqueante vive en [FeedbackStyle.kt].
 *
 * **Cadena oficial de dependencia (congelada, 10.2):**
 * `Component → ComponentStyle → Visual Tokens → Theme`. Único punto que lee Visual Tokens. Cero literales.
 *
 * **Jerarquía de interrupción respetada al pie de la letra:** Dialog→[StudioDepthTokens.Plane.Modal],
 * BlockingOverlay→[StudioDepthTokens.Plane.Alert]; la elevación sale SIEMPRE de
 * [StudioSurfaceTokens.Elevation.forPlane]; el scrim de `overlayScrim` (9.1). Materiales existentes
 * (Dialog/Toast/Floating) sin inventar ninguno. Motion solo con roles de 9.6; foco = anillo único.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Dialog / ModalDialog (organismo) — interrupción con scrim
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Diálogo modal: interrumpe el flujo con scrim y atrapa el foco. Plano Modal; material Dialog. Entra
 * **centrado** (`transform-origin: center`, no anclado a disparador) con [StudioMotionTokens.Role.Change];
 * nunca `scale(0)`; salida más rápida que entrada.
 *
 * Composición: [scrim] a pantalla completa · [material] (Dialog) · título ([titleStyle]) · separador
 * ([titleDivider]) · cuerpo (Body) con [padding] · barra de acciones ([actionGap], botones de 10.1).
 * Estados — el chasis reposa; Focused mantiene el anillo único en el control activo. loading: cuerpo
 * con [SkeletonStyle]/[SpinnerStyle] (10.1). error: mensaje con `feedbackDanger` en el cuerpo.
 * hover/pressed/disabled/selected: los aportan los controles internos.
 * Teclado: foco atrapado; Tab cíclico; Enter confirma la acción primaria; Esc cancela (cierre por
 * teclado no se anima).
 * Restricciones: Plane.Modal (nunca Alert salvo pérdida de trabajo → BlockingOverlay); un diálogo activo.
 * Composición: no anida diálogos; los toasts/tooltips quedan por encima sin ser tapados.
 */
object DialogStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Dialog
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Dialog
    val titleStyle: TextStyle = StudioTypographyTokens.Role.Title
    val bodyStyle: TextStyle = StudioTypographyTokens.Role.Body
    val padding: Dp = StudioSpacingTokens.Context.Dialog.Padding
    val gap: Dp = StudioSpacingTokens.Context.Dialog.Gap
    val actionGap: Dp = StudioSpacingTokens.Context.Dialog.ActionGap
    val enterMotion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change
    val focus: FocusRing = StudioFocusRing

    fun elevation(): Dp = StudioSurfaceTokens.Elevation.forPlane(plane)
    fun scrim(scheme: StudioColorTokens.Scheme): Color = scheme.overlayScrim
    fun titleColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentEmphasis
    fun bodyColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentPrimary
    fun titleDivider(scheme: StudioColorTokens.Scheme): Color = scheme.borderDefault
}

/** Diálogo modal: alias de la materialización canónica de [DialogStyle] (todo Dialog es modal en el PDS). */
object ModalDialogStyle {
    val material: StudioSurfaceTokens.Material = DialogStyle.material
    val plane: StudioDepthTokens.Plane = DialogStyle.plane
    val titleStyle: TextStyle = DialogStyle.titleStyle
    val bodyStyle: TextStyle = DialogStyle.bodyStyle
    val padding: Dp = DialogStyle.padding
    val gap: Dp = DialogStyle.gap
    val actionGap: Dp = DialogStyle.actionGap
    val enterMotion: StudioMotionTokens.Role = DialogStyle.enterMotion
    val focus: FocusRing = DialogStyle.focus

    fun elevation(): Dp = DialogStyle.elevation()
    fun scrim(scheme: StudioColorTokens.Scheme): Color = DialogStyle.scrim(scheme)
}

// ─────────────────────────────────────────────────────────────────────────────
// ConfirmationDialog (organismo) — decisión sí/no, posible destructiva
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Diálogo de confirmación: decisión binaria sobre [DialogStyle]. La acción primaria elige su variante
 * de botón de 10.1 según la intención: [ConfirmIntent.Neutral] (acción normal) o [ConfirmIntent.Danger]
 * (destructiva → botón `Danger`, feedback Alerta). No introduce estilo nuevo: reutiliza `ButtonStyle`.
 *
 * Composición: [material]/[scrim] de [DialogStyle] · título · cuerpo · dos acciones (cancelar Neutral +
 * confirmar según [primaryButton]).
 * Estados/focus/hover/pressed/disabled/loading/error: heredados de [DialogStyle] y de los botones de 10.1.
 * Teclado: Enter = confirmar, Esc = cancelar; foco por defecto en la acción **menos** destructiva.
 * Restricciones: exactamente dos acciones; la destructiva nunca es la acción por defecto de foco.
 * Composición: no aloja formularios extensos (para eso, Dialog completo).
 */
object ConfirmationDialogStyle {
    /** Intención de la acción primaria; selecciona la variante de botón de 10.1. */
    enum class ConfirmIntent { Neutral, Danger }

    val dialog = DialogStyle

    /** Botón de la acción primaria según la intención (descriptor de 10.1). */
    fun primaryButton(intent: ConfirmIntent): ButtonStyle = when (intent) {
        ConfirmIntent.Danger -> ButtonStyle(ButtonVariant.Danger)
        ConfirmIntent.Neutral -> ButtonStyle(ButtonVariant.Primary)
    }

    /** Botón de la acción secundaria (cancelar): siempre neutral. */
    val secondaryButton: ButtonStyle = ButtonStyle(ButtonVariant.Neutral)
}

// ─────────────────────────────────────────────────────────────────────────────
// ProgressOverlay / BusyOverlay (organismo) — bloqueo temporal con progreso
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Overlay de progreso: bloquea una región mientras una tarea determinada avanza. Plano Modal (scrim,
 * interrumpe); barra de progreso de 10.1 ([progress]) + leyenda (Caption). El movimiento se limita al
 * progreso (VG-MOV): sin más animación.
 *
 * Composición: [scrim] sobre la región · [progress] (ProgressStyle) · [labelStyle] en [labelColor].
 * Estados — loading es su razón de ser; error = fin fallido → sustituir por Dialog/Snackbar de error.
 * focus/hover/pressed/disabled/selected: **no aplica** (bloquea; no es interactivo salvo cancelación
 * opcional con un botón de 10.1).
 * Teclado: si admite cancelar, Esc lo dispara; en caso contrario, absorbe la entrada.
 * Restricciones: Plane.Modal; una barra por región; no compite con otro overlay.
 * Composición: no anida diálogos; puede coexistir bajo un BlockingOverlay de mayor prioridad.
 */
object ProgressOverlayStyle {
    val progress = ProgressStyle
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Plane.Modal
    val labelStyle: TextStyle = StudioTypographyTokens.Role.Caption
    val gap: Dp = StudioSpacingTokens.Relation.Related

    fun elevation(): Dp = StudioSurfaceTokens.Elevation.forPlane(plane)
    fun scrim(scheme: StudioColorTokens.Scheme): Color = scheme.overlayScrim
    fun labelColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentSecondary
}

/**
 * Overlay de ocupación: bloqueo temporal con progreso **indeterminado**. Igual que [ProgressOverlayStyle]
 * pero con spinner de 10.1 ([spinner]) en lugar de barra. Plano Modal.
 *
 * Composición: [scrim] · [spinner] (SpinnerStyle, giro Linear) · leyenda opcional (Caption, [labelColor]).
 * Estados — loading único; el resto no aplica (bloquea).
 * Teclado/accesibilidad: anuncia "ocupado"; respeta `prefers-reduced-motion` (el spinner es la
 * excepción funcional mínima); absorbe la entrada mientras dura.
 * Restricciones: Plane.Modal; un spinner; sin más movimiento.
 * Composición: coexiste bajo BlockingOverlay; nunca por encima de un Alert.
 */
object BusyOverlayStyle {
    val spinner = SpinnerStyle
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Plane.Modal
    val labelStyle: TextStyle = StudioTypographyTokens.Role.Caption
    val gap: Dp = StudioSpacingTokens.Relation.Related

    fun elevation(): Dp = StudioSurfaceTokens.Elevation.forPlane(plane)
    fun scrim(scheme: StudioColorTokens.Scheme): Color = scheme.overlayScrim
    fun labelColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentSecondary
}

// ─────────────────────────────────────────────────────────────────────────────
// BlockingOverlay (organismo) — alerta crítica topmost
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Overlay bloqueante crítico: máxima prioridad de interrupción (pérdida de trabajo / bloqueo del
 * sistema). Plano **Alert** (topmost reservado); elevación de Alert vía [StudioSurfaceTokens.Elevation.forPlane].
 * Material Dialog para el panel + scrim; motion [StudioMotionTokens.Role.Error] (reclama atención,
 * rarísimo). Reutiliza materiales/planos existentes; NO introduce nivel ni material nuevo.
 *
 * Composición: [scrim] a pantalla completa · panel [material] (Dialog) elevado a Alert · título
 * ([titleColor], Title) · cuerpo (Body) · acción(es) de 10.1 (confirmar Danger/Neutral).
 * Estados — el chasis reposa; error/atención es su naturaleza (color de acción, no del chasis).
 * focus/hover/pressed/disabled/loading/selected: los aportan los controles internos.
 * Teclado: foco atrapado con máxima prioridad; no se descarta por Esc si implica pérdida de trabajo
 * (confirmación explícita requerida).
 * Restricciones: Plane.Alert (por encima de modales y toasts); uno a la vez; reservado para lo crítico.
 * Composición: nunca coexiste con otro Alert; queda siempre por encima de cualquier otro overlay.
 */
object BlockingOverlayStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Dialog
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.CriticalAlert
    val titleStyle: TextStyle = StudioTypographyTokens.Role.Title
    val bodyStyle: TextStyle = StudioTypographyTokens.Role.Body
    val padding: Dp = StudioSpacingTokens.Context.Dialog.Padding
    val gap: Dp = StudioSpacingTokens.Context.Dialog.Gap
    val actionGap: Dp = StudioSpacingTokens.Context.Dialog.ActionGap
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Error
    val focus: FocusRing = StudioFocusRing

    fun elevation(): Dp = StudioSurfaceTokens.Elevation.forPlane(plane)
    fun scrim(scheme: StudioColorTokens.Scheme): Color = scheme.overlayScrim
    fun titleColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentEmphasis
    fun bodyColor(scheme: StudioColorTokens.Scheme): Color = scheme.contentPrimary
}
