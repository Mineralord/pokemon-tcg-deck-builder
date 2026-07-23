package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Surface tokens del **ecosistema Pokémon TCG** (Studio, Launcher, Card Viewer, herramientas)
 * — Fase 9.5. Materializan la **apariencia física** de los planos definidos por
 * [StudioDepthTokens] (9.4): forma de esquina, radius, border, elevación (shadow), blur y opacidad.
 *
 * **No redefine la jerarquía**: consume Depth; nunca altera el orden de planos. Fuente de verdad:
 * `docs/developer-studio/tokens/tokens.surface.json`. Regenerar desde el JSON.
 *
 * Escala deliberadamente mínima ("menos opciones, mejores decisiones"): 3 radios + pill, 2 anchos
 * de borde, 3 niveles de shadow. **Glass rechazado** (falla la Prueba de Gramática: decorativo,
 * compite con el contenido). Chasis **opaco** por defecto (reposo, protagonismo del contenido).
 * Esquinas **redondeadas uniformes** (sin asimetrías). Cada token implementa una ley VG/VL (TA-0).
 * Ver Anexo 9.5.
 */
object StudioSurfaceTokens {

    /** Radio de esquina (uniforme en las 4 esquinas). */
    object Radius {
        /** Datos, tablas, hairlines: sin redondeo. */
        val None: Dp = 0.dp
        /** Controles: botones, inputs, chips, resource card. */
        val Control: Dp = 6.dp
        /** Contenedores: paneles, popovers, diálogos, toasts. */
        val Container: Dp = 10.dp
        /** Pills / badges circulares. */
        val Full: Dp = 1000.dp
    }

    /** Ancho de borde (la frontera se distingue por color, no por grosor; VG borde=frontera). */
    object Border {
        val Thin: Dp = 1.dp
        /** Anillo de foco (IC-5 · A5): más grueso para visibilidad. */
        val Focus: Dp = 2.dp
    }

    /**
     * Elevación (shadow) que materializa cada plano de Depth. En dark UI la separación primaria la
     * da el **escalón de color de superficie** (9.1) y el hairline; el shadow es un refuerzo sutil
     * (VL luz difusa, casi plano). Valores pequeños y estables.
     */
    object Elevation {
        val Content: Dp = 0.dp
        val Raised: Dp = 2.dp
        val Floating: Dp = 8.dp
        val Modal: Dp = 16.dp
        val Notification: Dp = 8.dp
        val Alert: Dp = 16.dp

        /** Materializa el plano ordinal de Depth (9.4) como elevación concreta (DC-DP2). */
        fun forPlane(plane: StudioDepthTokens.Plane): Dp = when (plane) {
            StudioDepthTokens.Plane.Content -> Content
            StudioDepthTokens.Plane.Raised -> Raised
            StudioDepthTokens.Plane.Floating -> Floating
            StudioDepthTokens.Plane.Modal -> Modal
            StudioDepthTokens.Plane.Notification -> Notification
            StudioDepthTokens.Plane.Alert -> Alert
        }
    }

    /**
     * Blur. Chasis opaco por defecto ([None]). [Backdrop] queda RESERVADO/RESTRINGIDO: solo para un
     * overlay a pantalla completa que deba ocultar contenido ocupado; nunca por estética (VG-0).
     */
    object Blur {
        val None: Dp = 0.dp
        val Backdrop: Dp = 12.dp
    }

    /** Opacidad de superficie. El chasis es opaco (reposo); el scrim atenúa el contexto (VG-D). */
    object Opacity {
        const val Full: Float = 1.0f
        /** Coincide con color.overlay.scrim (9.1). */
        const val Scrim: Float = 0.55f
    }

    /**
     * Material = composición de un rol de color (resuelto por tema vía [StudioColorTokens.Scheme]) +
     * geometría (radius, border, elevación). Bundle inmutable que un componente aplica tal cual, sin
     * inventar apariencia. Los colores se expresan como selectores del [StudioColorTokens.Scheme]
     * activo, de modo que el mismo material sirve a `studio-dark`/`studio-light`/launcher sin cambios.
     *
     * @property surface color de fondo de la superficie.
     * @property border color del borde, o `null` si el material no lleva borde.
     * @property borderWidth ancho del borde (aplica solo si [border] != null).
     * @property radius radio de esquina.
     * @property elevation shadow del plano correspondiente.
     */
    data class Material(
        val surface: (StudioColorTokens.Scheme) -> Color,
        val border: ((StudioColorTokens.Scheme) -> Color)?,
        val borderWidth: Dp,
        val radius: Dp,
        val elevation: Dp,
    )

    /**
     * Catálogo de materiales por rol/contexto. Composiciones de tokens existentes; no introducen
     * valores nuevos. El plano de cada material coincide con el asignado en Depth (9.4).
     */
    object Materials {
        /** Plane.Content — territorio permanente (VL materiales). */
        val Panel = Material(
            surface = { it.surfacePanel },
            border = { it.borderSubtle },
            borderWidth = Border.Thin,
            radius = Radius.Container,
            elevation = Elevation.Content,
        )

        /** Plane.Content — chrome con hairline separadora, sin redondeo. */
        val Toolbar = Material(
            surface = { it.surfacePanel },
            border = { it.borderSubtle },
            borderWidth = Border.Thin,
            radius = Radius.None,
            elevation = Elevation.Content,
        )

        /** Plane.Content — chrome estructural. */
        val Sidebar = Material(
            surface = { it.surfacePanel },
            border = { it.borderSubtle },
            borderWidth = Border.Thin,
            radius = Radius.None,
            elevation = Elevation.Content,
        )

        /** Plane.Content — instrumento de medición en base. */
        val Inspector = Material(
            surface = { it.surfacePanel },
            border = { it.borderSubtle },
            borderWidth = Border.Thin,
            radius = Radius.None,
            elevation = Elevation.Content,
        )

        /** Plane.Content — eje temporal, chrome base. */
        val Timeline = Material(
            surface = { it.surfacePanel },
            border = { it.borderSubtle },
            borderWidth = Border.Thin,
            radius = Radius.None,
            elevation = Elevation.Content,
        )

        /** Plane.Content — objeto autocontenido en reposo (VG-CONTENEDOR). */
        val Card = Material(
            surface = { it.surfacePanel },
            border = { it.borderSubtle },
            borderWidth = Border.Thin,
            radius = Radius.Control,
            elevation = Elevation.Content,
        )

        /** Plane.Raised — seleccionado/arrastre: reclamo suave (VG-D3). */
        val CardRaised = Material(
            surface = { it.surfaceRaised },
            border = null,
            borderWidth = Border.Thin,
            radius = Radius.Control,
            elevation = Elevation.Raised,
        )

        /** Plane.Content — telón acromático del preview; sin chrome que compita (VL-CONT). */
        val Preview = Material(
            surface = { it.previewBackdrop },
            border = null,
            borderWidth = Border.Thin,
            radius = Radius.None,
            elevation = Elevation.Content,
        )

        /** Plane.Floating — tooltip, popover, menú, Command Palette. */
        val Floating = Material(
            surface = { it.surfaceOverlay },
            border = { it.borderDefault },
            borderWidth = Border.Thin,
            radius = Radius.Container,
            elevation = Elevation.Floating,
        )

        /** Plane.Modal — diálogo con scrim (VG-D interrupción). */
        val Dialog = Material(
            surface = { it.surfaceOverlay },
            border = { it.borderDefault },
            borderWidth = Border.Thin,
            radius = Radius.Container,
            elevation = Elevation.Modal,
        )

        /** Plane.Notification — feedback no bloqueante (IC-8). */
        val Toast = Material(
            surface = { it.surfaceRaised },
            border = { it.borderSubtle },
            borderWidth = Border.Thin,
            radius = Radius.Container,
            elevation = Elevation.Notification,
        )
    }
}
