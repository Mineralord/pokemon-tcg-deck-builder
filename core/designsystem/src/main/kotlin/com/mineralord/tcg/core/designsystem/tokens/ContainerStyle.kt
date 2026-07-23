package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Component Styling — Fase 10, Iteración 10.2 (Contenedores).
 *
 * **No define componentes nuevos, tokens nuevos ni principios nuevos.** Materializa la *apariencia*
 * (nunca el comportamiento) de los contenedores del PDS: Divider · ScrollArea · Scrollbar · Panel ·
 * Window. Los organismos de navegación viven en [NavigationStyle.kt].
 *
 * **Cadena oficial de dependencia (Design System):**
 * `Component → ComponentStyle → Visual Tokens → Theme`.
 * Ningún composable consume Visual Tokens directamente: consume EXCLUSIVAMENTE estos descriptores,
 * únicos autorizados a leer Color/Typography/Spacing/Depth/Surface/Motion. Cero literales.
 *
 * Los contenedores usan los [StudioSurfaceTokens.Material] de 9.5 (chasis opaco, esquinas uniformes,
 * shadow sutil). Reutilizan [VisualState]/[StyleColors]/[FocusRing]/[StudioFocusRing] de 10.1.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Divider (átomo) — separador estructural
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Divisor: frontera de 1px entre regiones. La frontera se distingue por **color**, no por grosor
 * (VG borde=frontera): [Subtle] separa dentro de una zona, [Strong] separa zonas.
 *
 * Composición: línea de [thickness] con color por rol; sin redondeo; agnóstico de orientación.
 * Estados/interacción/focus/hover/pressed/disabled/selected: **no aplica** — el divisor es decorativo
 * y no interactivo (A2); no tiene estados.
 * Restricciones: nunca sombra ni degradado; grosor fijo hairline (VG-U4, único submúltiplo 2dp interno).
 * Accesibilidad: puramente presentacional; se marca como separador semántico en el punto de uso, sin
 * rol enfocable.
 */
object DividerStyle {
    val thickness: Dp = StudioSurfaceTokens.Border.Thin

    /** Separación dentro de una misma zona. */
    fun subtle(scheme: StudioColorTokens.Scheme): Color = scheme.borderSubtle

    /** Separación entre zonas distintas (mayor contraste de frontera). */
    fun strong(scheme: StudioColorTokens.Scheme): Color = scheme.borderDefault
}

// ─────────────────────────────────────────────────────────────────────────────
// Scrollbar (átomo) — indicador/control de desplazamiento
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Barra de desplazamiento: pista discreta + pulgar arrastrable. Reposa casi invisible (chasis en
 * silencio) y sube contraste al hover/arrastre (VG-K). Aparece/desaparece con
 * [StudioMotionTokens.Role.Change] (salida más rápida que entrada).
 *
 * Composición: [track] (canal, normalmente transparente) · pulgar ([thumb]) de [thickness], radio
 * completo.
 * Estados del pulgar — rest: `borderStrong` tenue · hover: `contentMuted` · pressed (arrastre):
 * `contentSecondary` · disabled: se oculta (transparente). focus/selected: no aplica (el foco vive en
 * el contenido desplazable, no en la barra).
 * Restricciones: nunca ocupa layout (overlay sobre el contenido); no compite con el contenido en
 * reposo; grosor fijo.
 * Accesibilidad: el área de arrastre respeta el mínimo de puntero; el desplazamiento por teclado
 * actúa sobre el contenido, no anima la barra (alta frecuencia).
 */
object ScrollbarStyle {
    val thickness: Dp = StudioSpacingTokens.Foundation.Space.S8
    val minThumbLength: Dp = StudioSpacingTokens.Foundation.Space.S32
    val radius: Dp = StudioSurfaceTokens.Radius.Full
    val motion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change

    /** Canal de la barra (por defecto transparente: silencio en reposo). */
    fun track(scheme: StudioColorTokens.Scheme): Color = Color.Transparent

    /** Pulgar arrastrable por estado. */
    fun thumb(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Hover -> scheme.contentMuted
        VisualState.Pressed -> scheme.contentSecondary
        VisualState.Disabled -> Color.Transparent
        else -> scheme.borderStrong
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ScrollArea (átomo) — contenedor desplazable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Área de desplazamiento: recorta y desplaza su contenido, mostrando la [scrollbar] al interactuar.
 * No aporta chasis propio (transparente): hereda la superficie del contenedor que la aloja para no
 * añadir un escalón de color innecesario (protagonismo del contenido, VL-CONT).
 *
 * Composición: viewport que recorta · [scrollbar] overlay en el borde.
 * Estados/focus/hover/pressed/disabled/selected: no aplica al área en sí (los aporta su contenido y
 * la barra). No introduce estados propios.
 * Restricciones: sin sombra ni borde propios; nunca provoca reflujo horizontal (el contenido cabe en
 * el viewport); el desplazamiento no se anima (alta frecuencia).
 * Accesibilidad: mantiene el orden de foco del contenido; respeta `prefers-reduced-motion` (sin
 * scroll animado) en el punto de uso.
 */
object ScrollAreaStyle {
    val scrollbar: ScrollbarStyle = ScrollbarStyle
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Plane.Content

    /** Chasis del área: transparente (hereda la superficie del contenedor host). */
    fun surface(scheme: StudioColorTokens.Scheme): Color = Color.Transparent
}

// ─────────────────────────────────────────────────────────────────────────────
// Panel (organismo) — contenedor con encabezado
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Panel: contenedor de contenido con encabezado. Chasis opaco de contenido ([material]); el panel
 * **activo** (con foco dentro) eleva su frontera a `focusRing` (IC-5) sin cambiar de plano.
 *
 * Composición: [material] (surfacePanel + hairline + radio Container) · encabezado ([headerColors],
 * [titleStyle]) · separador ([headerDivider]) · cuerpo con padding [contentPadding].
 * Estados del chasis — rest: borde `borderSubtle` · Focused (panel activo): borde `focusRing` ·
 * Disabled (panel inhabilitado): contenido `contentDisabled`. hover/pressed/selected: no aplica al
 * chasis (los aportan los controles internos).
 * Restricciones: elevación de contenido (no flota); una sola jerarquía de encabezado por panel; no
 * compite con el contenido (VG-K).
 * Accesibilidad: título con rol de encabezado en el punto de uso; el marco activo se comunica por
 * borde `focusRing`, no solo por color de fondo.
 */
object PanelStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Panel
    val padding: Dp = StudioSpacingTokens.Context.Panel.Padding
    val headerPadding: Dp = StudioSpacingTokens.Context.Panel.HeaderPadding
    val contentPadding: Dp = StudioSpacingTokens.Context.Panel.Padding
    val contentGap: Dp = StudioSpacingTokens.Context.Panel.ContentGap
    val titleStyle: TextStyle = StudioTypographyTokens.Role.Title
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Plane.Content
    val focus: FocusRing = StudioFocusRing

    /** Encabezado: fondo, título y borde inferior por estado del panel. */
    fun headerColors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Focused -> StyleColors(scheme.surfacePanel, scheme.contentEmphasis, scheme.focusRing)
        VisualState.Disabled -> StyleColors(scheme.surfacePanel, scheme.contentDisabled, scheme.borderSubtle)
        else -> StyleColors(scheme.surfacePanel, scheme.contentPrimary, scheme.borderSubtle)
    }

    /** Frontera del chasis según el panel esté activo o en reposo. */
    fun chassisBorder(scheme: StudioColorTokens.Scheme, state: VisualState): Color = when (state) {
        VisualState.Focused -> scheme.focusRing
        else -> scheme.borderSubtle
    }

    /** Separador entre encabezado y cuerpo (hairline). */
    fun headerDivider(scheme: StudioColorTokens.Scheme): Color = scheme.borderSubtle
}

// ─────────────────────────────────────────────────────────────────────────────
// Window (plantilla/organismo) — ventana de nivel superior
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Ventana: contenedor de nivel superior (diálogo/ventana flotante con barra de título) sobre scrim.
 * Material Modal ([material]) + telón [scrim] (VG-D, interrupción). Distingue **activa** (con foco)
 * de **inactiva** (atenuada), sin recolocar ni cambiar de plano.
 *
 * Composición: [scrim] a pantalla completa · [material] (surfaceOverlay + borde + sombra Modal) ·
 * barra de título ([titleBarColors], [titleStyle]) · [titleDivider] · cuerpo con [contentPadding].
 * Entra centrada (`transform-origin: center`, no anclada a disparador) con
 * [StudioMotionTokens.Role.Change]; nunca `scale(0)`.
 * Estados — rest/activa: título `contentEmphasis` · inactiva (sin foco): título `contentSecondary` ·
 * Disabled: `contentDisabled`. hover/pressed/selected: no aplican al chasis.
 * Restricciones: solo la ventana enfocada muestra título a pleno contraste; el scrim atenúa el
 * contexto (opacidad de 9.5); una alerta crítica reservaría Plane.Alert (no aquí).
 * Accesibilidad: foco atrapado dentro mientras es modal; cierre por teclado no se anima; el estado
 * activo/inactivo no depende solo del color de fondo.
 */
object WindowStyle {
    val material: StudioSurfaceTokens.Material = StudioSurfaceTokens.Materials.Dialog
    val titleStyle: TextStyle = StudioTypographyTokens.Role.Title
    val padding: Dp = StudioSpacingTokens.Context.Dialog.Padding
    val contentPadding: Dp = StudioSpacingTokens.Context.Dialog.Padding
    val contentGap: Dp = StudioSpacingTokens.Context.Dialog.Gap
    val actionGap: Dp = StudioSpacingTokens.Context.Dialog.ActionGap
    val plane: StudioDepthTokens.Plane = StudioDepthTokens.Context.Dialog
    val enterMotion: StudioMotionTokens.Role = StudioMotionTokens.Role.Change
    val focus: FocusRing = StudioFocusRing

    /** Telón que atenúa el contexto tras la ventana modal. */
    fun scrim(scheme: StudioColorTokens.Scheme): Color = scheme.overlayScrim

    /** Barra de título: fondo, texto y borde inferior según la ventana esté activa o inactiva. */
    fun titleBarColors(scheme: StudioColorTokens.Scheme, state: VisualState): StyleColors = when (state) {
        VisualState.Disabled -> StyleColors(scheme.surfaceOverlay, scheme.contentDisabled, scheme.borderSubtle)
        VisualState.Focused -> StyleColors(scheme.surfaceOverlay, scheme.contentEmphasis, scheme.borderDefault)
        // Inactiva (sin foco): título atenuado.
        else -> StyleColors(scheme.surfaceOverlay, scheme.contentSecondary, scheme.borderSubtle)
    }

    /** Separador entre barra de título y cuerpo (hairline). */
    fun titleDivider(scheme: StudioColorTokens.Scheme): Color = scheme.borderDefault
}
