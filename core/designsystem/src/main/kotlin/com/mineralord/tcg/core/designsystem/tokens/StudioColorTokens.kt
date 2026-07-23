package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.ui.graphics.Color

/**
 * Color tokens del **Pokémon TCG Developer Studio Design System (PDS)** — Fase 9.1.
 *
 * NO confundir con [com.mineralord.tcg.core.designsystem.TcgColors], que son los colores del
 * *juego* (clon de TCG Live). Estos tokens visten el **Studio** (herramienta), no el juego.
 *
 * Fuente de verdad: `docs/developer-studio/tokens/tokens.color.json`. Este fichero se
 * **regenera desde el JSON**; no editar valores a mano sin actualizar el JSON (DC-C10).
 *
 * Arquitectura (Fase 9): `Foundation → Semantic`. Los componentes consumen SOLO la capa
 * [semantic] ([StudioColorTokens.Dark]); nunca los primitivos [foundation] directamente
 * (regla de dependencia TD-2).
 *
 * Cada token implementa una ley del Visual Grammar / Visual Language (TA-0). Ver COLOR-TOKENS.md.
 */
object StudioColorTokens {

    /** Primitivos. Uso interno de la capa semántica; NO referenciar desde componentes (TD-2). */
    object Foundation {
        val Neutral0 = Color(0xFF0E1116)
        val Neutral5 = Color(0xFF14181F)
        val Neutral10 = Color(0xFF1A1F27)
        val Neutral15 = Color(0xFF212732)
        val Neutral20 = Color(0xFF2A313D)
        val Neutral30 = Color(0xFF3D4653)
        val Neutral40 = Color(0xFF4C5563)
        val Neutral50 = Color(0xFF5E6773)
        val Neutral60 = Color(0xFF78828F)
        val Neutral70 = Color(0xFF9AA4B0)
        val Neutral80 = Color(0xFFC2CAD3)
        val Neutral90 = Color(0xFFE2E7EC)
        val Neutral100 = Color(0xFFF5F7FA)

        val Accent50 = Color(0xFF3E88AD)
        val Accent60 = Color(0xFF4FA3CC)
        val Accent70 = Color(0xFF6FBBDE)
        val AccentMuted = Color(0xFF21404E)

        val Success60 = Color(0xFF4F9D6E)
        val Warning60 = Color(0xFFC79445)
        val Danger50 = Color(0xFFA94B4B)
        val Danger60 = Color(0xFFCF5D5D)
        val Danger70 = Color(0xFFE07B7B)
    }

    /**
     * Capa semántica de un tema. Un mismo conjunto de nombres se re-mapea por tema
     * (Theme = dimensión de Semantic, DC-C1). Los componentes dependen de esta interfaz,
     * no de valores concretos, para permitir `studio-light`/launcher/visor sin renombrar.
     */
    interface Scheme {
        // surface — planos (VG-CONTENEDOR / VG-D)
        val surfaceCanvas: Color
        val surfacePanel: Color
        val surfaceRaised: Color
        val surfaceOverlay: Color
        val surfaceSunken: Color
        // content — foreground (VG-J / VL-fatiga)
        val contentPrimary: Color
        val contentSecondary: Color
        val contentMuted: Color
        val contentDisabled: Color
        val contentEmphasis: Color
        val contentOnAccent: Color
        // border — límites (VG-CONTENEDOR)
        val borderSubtle: Color
        val borderDefault: Color
        val borderStrong: Color
        // accent — acento único (CC-6)
        val accentRest: Color
        val accentHover: Color
        val accentActive: Color
        val accentMuted: Color
        // feedback — modo Alerta (CC-7)
        val feedbackSuccess: Color
        val feedbackSuccessSurface: Color
        val feedbackWarning: Color
        val feedbackWarningSurface: Color
        val feedbackDanger: Color
        val feedbackDangerHover: Color
        val feedbackDangerActive: Color
        val feedbackDangerSurface: Color
        // selection (IC-1) / focus (IC-5)
        val selectionBackground: Color
        val selectionEdge: Color
        val focusRing: Color
        // overlay scrim (VG-D)
        val overlayScrim: Color
        // preview — telón acromático (CC-5 / VL-CONT)
        val previewBackdrop: Color
        val previewBackdropAlt: Color
    }

    /** Tema canónico por defecto del Studio. */
    val Dark: Scheme = object : Scheme {
        override val surfaceCanvas = Foundation.Neutral5
        override val surfacePanel = Foundation.Neutral10
        override val surfaceRaised = Foundation.Neutral15
        override val surfaceOverlay = Foundation.Neutral20
        override val surfaceSunken = Foundation.Neutral0

        override val contentPrimary = Foundation.Neutral80
        override val contentSecondary = Foundation.Neutral70
        override val contentMuted = Foundation.Neutral60
        override val contentDisabled = Foundation.Neutral50
        override val contentEmphasis = Foundation.Neutral90
        override val contentOnAccent = Foundation.Neutral0

        override val borderSubtle = Foundation.Neutral20
        override val borderDefault = Foundation.Neutral30
        override val borderStrong = Foundation.Neutral40

        override val accentRest = Foundation.Accent60
        override val accentHover = Foundation.Accent70
        override val accentActive = Foundation.Accent50
        override val accentMuted = Foundation.AccentMuted

        override val feedbackSuccess = Foundation.Success60
        override val feedbackSuccessSurface = Color(0xFF1B2E24)
        override val feedbackWarning = Foundation.Warning60
        override val feedbackWarningSurface = Color(0xFF2E2717)
        override val feedbackDanger = Foundation.Danger60
        override val feedbackDangerHover = Foundation.Danger70
        override val feedbackDangerActive = Foundation.Danger50
        override val feedbackDangerSurface = Color(0xFF2E1B1B)

        override val selectionBackground = Color(0xFF22404E)
        override val selectionEdge = Foundation.Accent60
        override val focusRing = Foundation.Accent70

        // scrim neutral-azulado #080B10 al 55% → ARGB 0x8C080B10
        override val overlayScrim = Color(0x8C080B10)

        override val previewBackdrop = Color(0xFF15171A)
        override val previewBackdropAlt = Color(0xFF1E2024)
    }
}
