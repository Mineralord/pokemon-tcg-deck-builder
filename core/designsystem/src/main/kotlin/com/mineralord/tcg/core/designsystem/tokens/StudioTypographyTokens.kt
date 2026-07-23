package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tokens del **ecosistema Pokémon TCG** (Studio, Launcher, Card Viewer, herramientas)
 * — Fase 9.2. NO confundir con la tipografía del *juego*.
 *
 * Fuente de verdad: `docs/developer-studio/tokens/tokens.typography.json`. Este fichero se
 * **regenera desde el JSON**; no editar valores a mano sin actualizar el JSON.
 *
 * Arquitectura (Fase 9): `Foundation → Semantic(role)`. Los componentes consumen SOLO los
 * roles ([Role]); nunca los primitivos ([Foundation]) directamente (TD-2). Cada rol implementa
 * una ley del Visual Grammar / Visual Language (TA-0). Ver Visual-Tokens-Architecture, Anexo 9.2.
 *
 * Escala: modular anclada en base = 13sp, ratio ~1.2, **redondeada a sp enteros** para nitidez
 * en escritorio denso (una escala modular estricta produce medios píxeles borrosos en UI densa).
 *
 * Familias: se declaran por *intención* (Inter / JetBrains Mono). Hasta añadir los recursos de
 * fuente, se resuelven con los fallbacks del sistema ([FontFamily.SansSerif] / [FontFamily.Monospace]);
 * cuando se incorporen los .ttf, basta cambiar [Foundation.Family] en un único lugar.
 */
object StudioTypographyTokens {

    /** Primitivos. Uso interno de los roles; NO referenciar desde componentes (TD-2). */
    object Foundation {
        object Family {
            // Intención: Inter (UI) / JetBrains Mono (código-datos). Fallback de sistema por ahora.
            val Sans: FontFamily = FontFamily.SansSerif
            val Mono: FontFamily = FontFamily.Monospace
        }

        object Weight {
            val Regular = FontWeight.Normal   // 400
            val Medium = FontWeight.Medium    // 500
            val SemiBold = FontWeight.SemiBold // 600
        }
    }

    /**
     * Roles semánticos. Cada uno justifica su existencia (ver Anexo 9.2). Los componentes del PDS
     * eligen un rol por su significado, nunca un tamaño suelto.
     */
    object Role {
        /** Estados vacíos / bienvenida / grandes encabezados de preview. Raro (VG-K5). */
        val Display = TextStyle(
            fontFamily = Foundation.Family.Sans,
            fontWeight = Foundation.Weight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.4).sp,
        )

        /** Primer nivel de lectura de una pantalla/región (VG-J). */
        val Heading = TextStyle(
            fontFamily = Foundation.Family.Sans,
            fontWeight = Foundation.Weight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.2).sp,
        )

        /** Títulos de Panel / Dialog. */
        val Title = TextStyle(
            fontFamily = Foundation.Family.Sans,
            fontWeight = Foundation.Weight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.2).sp,
        )

        /** Subsecciones dentro de un panel. */
        val Section = TextStyle(
            fontFamily = Foundation.Family.Sans,
            fontWeight = Foundation.Weight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
        )

        /** Texto de lectura primario (descripciones, cuerpo). lh 1.5 para jornadas largas (VL-fatiga). */
        val Body = TextStyle(
            fontFamily = Foundation.Family.Sans,
            fontWeight = Foundation.Weight.Regular,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
        )

        /** Texto de apoyo secundario / ayuda. */
        val Caption = TextStyle(
            fontFamily = Foundation.Family.Sans,
            fontWeight = Foundation.Weight.Regular,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.1.sp,
        )

        /** Nombres de propiedad (Inspector), etiquetas de formulario. Tracking leve para tamaño pequeño. */
        val Label = TextStyle(
            fontFamily = Foundation.Family.Sans,
            fontWeight = Foundation.Weight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        )

        /** Código / DSL de efectos / JSON. Monoespaciado. */
        val Mono = TextStyle(
            fontFamily = Foundation.Family.Mono,
            fontWeight = Foundation.Weight.Regular,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
        )

        /** Datos numéricos en tablas/Inspector. Mono garantiza alineación de columnas (VG-U4). */
        val Numeric = TextStyle(
            fontFamily = Foundation.Family.Mono,
            fontWeight = Foundation.Weight.Regular,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
        )

        /** Pistas de atajo de teclado (Shortcut Hint, A7 · IC-5). */
        val Shortcut = TextStyle(
            fontFamily = Foundation.Family.Mono,
            fontWeight = Foundation.Weight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp,
        )

        /** Contador / marcador de estado sobre un objeto (Badge, A2). */
        val Badge = TextStyle(
            fontFamily = Foundation.Family.Sans,
            fontWeight = Foundation.Weight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.2.sp,
        )

        /** Segmentos de Status Bar / texto de notificación (IC-8). */
        val Status = TextStyle(
            fontFamily = Foundation.Family.Sans,
            fontWeight = Foundation.Weight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp,
        )
    }
}
