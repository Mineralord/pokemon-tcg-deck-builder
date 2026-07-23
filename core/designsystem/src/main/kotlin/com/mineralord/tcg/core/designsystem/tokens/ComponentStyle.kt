package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Component Styling — Fase 10, Iteración 10.1 (Controles básicos).
 *
 * **No define componentes nuevos ni tokens nuevos.** Aplica el lenguaje visual de la Fase 9 a los
 * componentes ya existentes del PDS, en forma de **descriptores de estilo** (apariencia por estado),
 * desacoplados del comportamiento. Un composable (o el componente PDS) lee estos descriptores y se
 * pinta; toda la apariencia sale de tokens: Color (9.1), Typography (9.2), Spacing (9.3), Depth
 * (9.4), Surface (9.5), Motion (9.6). Cero valores hardcodeados.
 *
 * Estados canónicos comunes ([VisualState]) mapeados al Interaction Canon (IC-1/IC-4/IC-5/IC-8) y a
 * la regla de contraste reservado (VG-K): el chasis reposa; solo foco/selección/alerta suben.
 */

/** Estados visuales de un control (IC). Focus es ortogonal y se materializa con [ControlStyle.focus]. */
enum class VisualState { Rest, Hover, Pressed, Focused, Disabled, Selected, Loading, Error }

/** Terna de color resuelta para un estado: fondo, contenido (texto/icono) y borde (o `null`). */
data class StyleColors(val container: Color, val content: Color, val border: Color?)

/** Anillo de foco único de todo el PDS (A5 · IC-5). */
data class FocusRing(val color: (StudioColorTokens.Scheme) -> Color, val width: Dp)

/** Métricas geométricas de un control, tomadas de Spacing/Surface. */
data class ControlMetrics(
    val height: Dp,
    val paddingH: Dp,
    val paddingV: Dp,
    val gap: Dp,
    val radius: Dp,
)

/** Descriptor común: colores por estado + métricas + tipografía + motion + foco. */
interface ControlStyle {
    fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors
    val metrics: ControlMetrics
    val textStyle: TextStyle
    /** Motion de la transición de estado (feedback de pulsación, IC-4). */
    val stateMotion: StudioMotionTokens.Role
    val focus: FocusRing
}

/** Anillo de foco canónico reutilizado por todos los controles. */
val StudioFocusRing = FocusRing(color = { it.focusRing }, width = StudioSurfaceTokens.Border.Focus)

/** Métricas por defecto de un control de línea (altura = densidad activa se resuelve en uso). */
private fun defaultControlMetrics(height: Dp) = ControlMetrics(
    height = height,
    paddingH = StudioSpacingTokens.Inset.Md,
    paddingV = StudioSpacingTokens.Relation.Within,
    gap = StudioSpacingTokens.Relation.Related,
    radius = StudioSurfaceTokens.Radius.Control,
)

/** Altura estándar de un control (fila de densidad Default). */
private val ControlHeight: Dp = StudioSpacingTokens.Density.Default.rowHeight

// ─────────────────────────────────────────────────────────────────────────────
// Button
// ─────────────────────────────────────────────────────────────────────────────

/** Variantes de botón (jerarquía de acción, VG-J). Danger = acción destructiva (VL Alerta). */
enum class ButtonVariant { Primary, Neutral, Ghost, Danger }

class ButtonStyle(private val variant: ButtonVariant) : ControlStyle {
    override val metrics = defaultControlMetrics(ControlHeight)
    override val textStyle = StudioTypographyTokens.Role.Label
    override val stateMotion = StudioMotionTokens.Role.Confirmation
    override val focus = StudioFocusRing

    override fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors =
        when (variant) {
            ButtonVariant.Primary -> when (state) {
                VisualState.Hover -> StyleColors(scheme.accentHover, scheme.contentOnAccent, null)
                VisualState.Pressed -> StyleColors(scheme.accentActive, scheme.contentOnAccent, null)
                VisualState.Disabled -> StyleColors(scheme.accentMuted, scheme.contentDisabled, null)
                else -> StyleColors(scheme.accentRest, scheme.contentOnAccent, null)
            }
            ButtonVariant.Neutral -> when (state) {
                VisualState.Hover -> StyleColors(scheme.surfaceOverlay, scheme.contentPrimary, scheme.borderDefault)
                VisualState.Pressed -> StyleColors(scheme.surfaceRaised, scheme.contentPrimary, scheme.borderStrong)
                VisualState.Disabled -> StyleColors(scheme.surfacePanel, scheme.contentDisabled, scheme.borderSubtle)
                else -> StyleColors(scheme.surfaceRaised, scheme.contentPrimary, scheme.borderDefault)
            }
            ButtonVariant.Ghost -> when (state) {
                VisualState.Hover -> StyleColors(scheme.accentMuted, scheme.contentPrimary, null)
                VisualState.Pressed -> StyleColors(scheme.accentMuted, scheme.contentEmphasis, null)
                VisualState.Disabled -> StyleColors(Color.Transparent, scheme.contentDisabled, null)
                else -> StyleColors(Color.Transparent, scheme.accentRest, null)
            }
            ButtonVariant.Danger -> when (state) {
                VisualState.Hover -> StyleColors(scheme.feedbackDangerHover, scheme.contentOnAccent, null)
                VisualState.Pressed -> StyleColors(scheme.feedbackDangerActive, scheme.contentOnAccent, null)
                VisualState.Disabled -> StyleColors(scheme.surfacePanel, scheme.contentDisabled, scheme.borderSubtle)
                else -> StyleColors(scheme.feedbackDanger, scheme.contentOnAccent, null)
            }
        }
}

/** Botón de solo icono: cuadrado, misma altura, variantes Neutral/Ghost. */
class IconButtonStyle(variant: ButtonVariant = ButtonVariant.Ghost) : ControlStyle by ButtonStyle(variant) {
    val square: Dp = ControlHeight
}

// ─────────────────────────────────────────────────────────────────────────────
// Toggle / Segmented
// ─────────────────────────────────────────────────────────────────────────────

/** Botón conmutable: no seleccionado = Neutral; seleccionado = acento tenue + borde de selección. */
object ToggleButtonStyle : ControlStyle {
    override val metrics = defaultControlMetrics(ControlHeight)
    override val textStyle = StudioTypographyTokens.Role.Label
    override val stateMotion = StudioMotionTokens.Role.Confirmation
    override val focus = StudioFocusRing

    override fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Selected -> StyleColors(scheme.accentMuted, scheme.contentEmphasis, scheme.selectionEdge)
        VisualState.Hover -> StyleColors(scheme.surfaceOverlay, scheme.contentPrimary, scheme.borderDefault)
        VisualState.Pressed -> StyleColors(scheme.surfaceRaised, scheme.contentPrimary, scheme.borderStrong)
        VisualState.Disabled -> StyleColors(scheme.surfacePanel, scheme.contentDisabled, scheme.borderSubtle)
        else -> StyleColors(scheme.surfaceRaised, scheme.contentSecondary, scheme.borderDefault)
    }
}

/** Grupo segmentado: fondo del contenedor + segmento activo con acento tenue. */
object SegmentedButtonStyle : ControlStyle {
    override val metrics = defaultControlMetrics(ControlHeight)
    override val textStyle = StudioTypographyTokens.Role.Label
    override val stateMotion = StudioMotionTokens.Role.Change
    override val focus = StudioFocusRing

    /** Contenedor del grupo. */
    fun track(scheme: StudioColorTokens.Scheme): StyleColors =
        StyleColors(scheme.surfaceSunken, scheme.contentSecondary, scheme.borderSubtle)

    override fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Selected -> StyleColors(scheme.accentMuted, scheme.contentEmphasis, scheme.selectionEdge)
        VisualState.Hover -> StyleColors(scheme.surfaceRaised, scheme.contentPrimary, null)
        VisualState.Disabled -> StyleColors(Color.Transparent, scheme.contentDisabled, null)
        else -> StyleColors(Color.Transparent, scheme.contentSecondary, null)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TextField / SearchField
// ─────────────────────────────────────────────────────────────────────────────

/** Campo de texto: pozo hundido, borde por estado; foco y error suben contraste (VG-K). */
open class TextFieldStyle : ControlStyle {
    override val metrics = defaultControlMetrics(ControlHeight)
    override val textStyle = StudioTypographyTokens.Role.Body
    val labelStyle = StudioTypographyTokens.Role.Label
    val placeholderColor: (StudioColorTokens.Scheme) -> Color = { it.contentMuted }
    override val stateMotion = StudioMotionTokens.Role.Change
    override val focus = StudioFocusRing

    override fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Focused -> StyleColors(scheme.surfaceSunken, scheme.contentPrimary, scheme.focusRing)
        VisualState.Error -> StyleColors(scheme.surfaceSunken, scheme.contentPrimary, scheme.feedbackDanger)
        VisualState.Disabled -> StyleColors(scheme.surfacePanel, scheme.contentDisabled, scheme.borderSubtle)
        VisualState.Hover -> StyleColors(scheme.surfaceSunken, scheme.contentPrimary, scheme.borderStrong)
        else -> StyleColors(scheme.surfaceSunken, scheme.contentPrimary, scheme.borderDefault)
    }
}

/** Campo de búsqueda: mismo material que TextField, con icono guía (gap = Related). */
object SearchFieldStyle : TextFieldStyle()

// ─────────────────────────────────────────────────────────────────────────────
// Selection controls: Checkbox / Radio / Switch
// ─────────────────────────────────────────────────────────────────────────────

/** Casilla: caja con borde en reposo; marcada = acento + check on-accent. */
object CheckboxStyle {
    val size: Dp = StudioSpacingTokens.Foundation.Space.S16
    val radius: Dp = StudioSurfaceTokens.Radius.Control
    val motion = StudioMotionTokens.Role.Confirmation
    val focus = StudioFocusRing

    fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Selected -> StyleColors(scheme.accentRest, scheme.contentOnAccent, null)
        VisualState.Disabled -> StyleColors(scheme.surfacePanel, scheme.contentDisabled, scheme.borderSubtle)
        VisualState.Hover -> StyleColors(scheme.surfaceSunken, scheme.contentPrimary, scheme.borderStrong)
        else -> StyleColors(scheme.surfaceSunken, scheme.contentPrimary, scheme.borderDefault)
    }
}

/** Radio: círculo con punto de acento cuando está activo. */
object RadioButtonStyle {
    val size: Dp = StudioSpacingTokens.Foundation.Space.S16
    val motion = StudioMotionTokens.Role.Confirmation
    val focus = StudioFocusRing

    fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Selected -> StyleColors(scheme.surfaceSunken, scheme.accentRest, scheme.accentRest)
        VisualState.Disabled -> StyleColors(scheme.surfacePanel, scheme.contentDisabled, scheme.borderSubtle)
        VisualState.Hover -> StyleColors(scheme.surfaceSunken, scheme.contentPrimary, scheme.borderStrong)
        else -> StyleColors(scheme.surfaceSunken, scheme.contentPrimary, scheme.borderDefault)
    }
}

/** Interruptor: pista off/on y pulgar. Pista on = acento. */
object SwitchStyle {
    val trackWidth: Dp = StudioSpacingTokens.Foundation.Space.S32
    val trackHeight: Dp = StudioSpacingTokens.Foundation.Space.S16
    val thumbSize: Dp = StudioSpacingTokens.Foundation.Space.S12
    val radius: Dp = StudioSurfaceTokens.Radius.Full
    val motion = StudioMotionTokens.Role.Change
    val focus = StudioFocusRing

    /** Pista. */
    fun track(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Selected -> scheme.accentRest
        VisualState.Disabled -> scheme.surfacePanel
        else -> scheme.surfaceRaised
    }

    /** Pulgar. */
    fun thumb(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Selected -> scheme.contentOnAccent
        VisualState.Disabled -> scheme.contentDisabled
        else -> scheme.contentEmphasis
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Slider / Progress / Spinner
// ─────────────────────────────────────────────────────────────────────────────

/** Deslizador: pista, relleno y pomo. */
object SliderStyle {
    val trackHeight: Dp = StudioSpacingTokens.Foundation.Space.S4
    val thumbSize: Dp = StudioSpacingTokens.Foundation.Space.S12
    val radius: Dp = StudioSurfaceTokens.Radius.Full
    val focus = StudioFocusRing

    fun track(scheme: StudioColorTokens.Scheme): Color = scheme.surfaceSunken
    fun fill(scheme: StudioColorTokens.Scheme, state: VisualState): Color =
        if (state == VisualState.Disabled) scheme.contentDisabled else scheme.accentRest
    fun thumb(scheme: StudioColorTokens.Scheme, state: VisualState): Color =
        if (state == VisualState.Disabled) scheme.contentDisabled else scheme.contentEmphasis
}

/** Progreso determinado: canal + relleno de acento. Indeterminado = movimiento lineal. */
object ProgressStyle {
    val trackHeight: Dp = StudioSpacingTokens.Foundation.Space.S4
    val radius: Dp = StudioSurfaceTokens.Radius.Full
    val indeterminateEasing: Easing = StudioMotionTokens.Foundation.Easing.Linear

    fun track(scheme: StudioColorTokens.Scheme): Color = scheme.surfaceSunken
    fun fill(scheme: StudioColorTokens.Scheme): Color = scheme.accentRest
}

/** Spinner: color de acento; giro constante (Linear). */
object SpinnerStyle {
    val size: Dp = StudioSpacingTokens.Foundation.Space.S16
    val stroke: Dp = StudioSurfaceTokens.Border.Focus
    val easing: Easing = StudioMotionTokens.Foundation.Easing.Linear
    fun color(scheme: StudioColorTokens.Scheme): Color = scheme.accentRest
}

// ─────────────────────────────────────────────────────────────────────────────
// Chip / Badge
// ─────────────────────────────────────────────────────────────────────────────

/** Chip: token compacto seleccionable/filtrable. */
object ChipStyle {
    val height: Dp = StudioSpacingTokens.Foundation.Space.S24
    val paddingH: Dp = StudioSpacingTokens.Relation.Related
    val gap: Dp = StudioSpacingTokens.Relation.Within
    val radius: Dp = StudioSurfaceTokens.Radius.Full
    val textStyle = StudioTypographyTokens.Role.Label
    val motion = StudioMotionTokens.Role.Confirmation
    val focus = StudioFocusRing

    fun colors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Selected -> StyleColors(scheme.accentMuted, scheme.contentEmphasis, scheme.selectionEdge)
        VisualState.Hover -> StyleColors(scheme.surfaceOverlay, scheme.contentPrimary, scheme.borderDefault)
        VisualState.Disabled -> StyleColors(scheme.surfacePanel, scheme.contentDisabled, scheme.borderSubtle)
        else -> StyleColors(scheme.surfaceRaised, scheme.contentSecondary, scheme.borderSubtle)
    }
}

/** Badge: contador o marcador de estado. No interactivo (A2). */
object BadgeStyle {
    val minSize: Dp = StudioSpacingTokens.Foundation.Space.S16
    val paddingH: Dp = StudioSpacingTokens.Relation.Within
    val radius: Dp = StudioSurfaceTokens.Radius.Full
    val textStyle = StudioTypographyTokens.Role.Badge

    /** Tono según el significado. `Neutral` por defecto; feedback para estado. */
    enum class Tone { Neutral, Accent, Success, Warning, Danger }

    fun colors(scheme: StudioColorTokens.Scheme, tone: Tone): StyleColors = when (tone) {
        Tone.Neutral -> StyleColors(scheme.surfaceRaised, scheme.contentSecondary, null)
        Tone.Accent -> StyleColors(scheme.accentRest, scheme.contentOnAccent, null)
        Tone.Success -> StyleColors(scheme.feedbackSuccessSurface, scheme.feedbackSuccess, null)
        Tone.Warning -> StyleColors(scheme.feedbackWarningSurface, scheme.feedbackWarning, null)
        Tone.Danger -> StyleColors(scheme.feedbackDangerSurface, scheme.feedbackDanger, null)
    }
}
