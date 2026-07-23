package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spatial / spacing tokens del **ecosistema Pokémon TCG** (Studio, Launcher, Card Viewer,
 * herramientas) — Fase 9.3. NO confundir con el espaciado del *juego*.
 *
 * Fuente de verdad: `docs/developer-studio/tokens/tokens.spacing.json`. Este fichero se
 * **regenera desde el JSON**; no editar valores a mano sin actualizar el JSON.
 *
 * Arquitectura (Fase 9): `Foundation → Semantic → Context`. Los componentes consumen la capa
 * [Semantic] (relaciones/insets) o su [Context]; nunca el ramp primitivo [Foundation.Space]
 * directamente (TD-2). Cada token implementa una ley del Visual Grammar / Visual Language (TA-0).
 * Ver Visual-Tokens-Architecture, Anexo 9.3.
 *
 * Rejilla base **4dp** (VG-U4). El único sub-múltiplo permitido es 2dp, exclusivamente como
 * *hairline* interno. La densidad ([Density]) es un modificador que altera SOLO alturas de fila;
 * los insets horizontales permanecen constantes entre densidades para preservar la memoria
 * muscular y la alineación de columnas (VG-U4).
 */
object StudioSpacingTokens {

    /** Primitivos. Uso interno de la capa semántica; NO referenciar desde componentes (TD-2). */
    object Foundation {
        /** Rejilla base: toda distancia es múltiplo de esta salvo el hairline de 2dp. */
        val Baseline: Dp = 4.dp

        object Space {
            val S0: Dp = 0.dp
            val S2: Dp = 2.dp   // hairline interno
            val S4: Dp = 4.dp
            val S8: Dp = 8.dp
            val S12: Dp = 12.dp
            val S16: Dp = 16.dp
            val S20: Dp = 20.dp
            val S24: Dp = 24.dp
            val S32: Dp = 32.dp
            val S40: Dp = 40.dp
            val S48: Dp = 48.dp
            val S64: Dp = 64.dp
        }
    }

    /**
     * Familia finita de distancias con significado (VG-U3). Un componente elige una *relación*
     * por lo que significa, nunca un valor suelto.
     */
    object Relation {
        /** Separación mínima interna (VG-E). */
        val Hairline: Dp = Foundation.Space.S2
        /** Dentro de un grupo / items muy próximos = pertenencia (VG-E1). */
        val Within: Dp = Foundation.Space.S4
        /** Items relacionados dentro de un grupo (VG-E1). */
        val Related: Dp = Foundation.Space.S8
        /** Entre grupos distintos (VG-E2). */
        val Group: Dp = Foundation.Space.S16
        /** Entre secciones (VG-E2). */
        val Section: Dp = Foundation.Space.S24
        /** Entre zonas/regiones (VG-E · Arquitectura Espacial). */
        val Zone: Dp = Foundation.Space.S32
    }

    /** Relleno interno de contenedores (respiración, VG-E3). */
    object Inset {
        val Xs: Dp = Foundation.Space.S4
        val Sm: Dp = Foundation.Space.S8
        val Md: Dp = Foundation.Space.S12
        val Lg: Dp = Foundation.Space.S16
    }

    /** Retícula de disposición (VG-U4). */
    object Layout {
        /** Separación entre columnas. */
        val Gutter: Dp = Foundation.Space.S16
        /** Margen exterior de una región. */
        val Margin: Dp = Foundation.Space.S16
        /** Ancho máximo de columna de lectura (≈65–75 car. a 13sp) — VL-fatiga. */
        val ReadingMaxWidth: Dp = 640.dp
    }

    /**
     * Densidad: modificador que altera SOLO la altura de fila. No cambia insets horizontales
     * (memoria muscular / alineación). [Default] para el Studio; [Compact] para zonas de máxima
     * densidad; [Comfortable] para launcher/visor u onboarding.
     */
    enum class Density(val rowHeight: Dp) {
        Comfortable(32.dp),
        Default(28.dp),
        Compact(24.dp),
    }

    /**
     * Espaciados por contexto. Aliases semánticos sobre [Relation]/[Inset]; no introducen valores
     * nuevos. `rowHeight` de listas/propiedades se toma de [Density] en el punto de uso.
     */
    object Context {
        object Dock {
            val Padding: Dp = Inset.Sm
            val Gap: Dp = Relation.Within
        }
        object Panel {
            val Padding: Dp = Inset.Md
            val HeaderPadding: Dp = Inset.Md
            val ContentGap: Dp = Relation.Group
        }
        object Toolbar {
            val PaddingH: Dp = Inset.Sm
            val PaddingV: Dp = Relation.Within
            val Gap: Dp = Relation.Related
        }
        object Sidebar {
            val Padding: Dp = Inset.Sm
            val ItemGap: Dp = Relation.Hairline
            val ItemHeight: Dp = Density.Default.rowHeight
        }
        object Inspector {
            val Padding: Dp = Inset.Md
            val RowGap: Dp = Relation.Within
            val LabelValueGap: Dp = Relation.Related
            val GroupGap: Dp = Relation.Group
        }
        object Dialog {
            val Padding: Dp = Inset.Lg
            val Gap: Dp = Relation.Group
            val ActionGap: Dp = Relation.Related
        }
        object Overlay {
            val Padding: Dp = Inset.Sm
            val ItemGap: Dp = Relation.Hairline
        }
        object Timeline {
            val Padding: Dp = Inset.Sm
            val TrackGap: Dp = Relation.Within
        }
        object Preview {
            /** Aire generoso alrededor del contenido (protagonismo, VL-CONT). */
            val Padding: Dp = Relation.Section
        }
        object Tree {
            val Indent: Dp = Relation.Group
            val RowGap: Dp = Relation.Hairline
            val IconGap: Dp = Relation.Related
        }
        object Property {
            val RowHeight: Dp = Density.Default.rowHeight
            val RowGap: Dp = Relation.Within
            val LabelValueGap: Dp = Relation.Related
        }
        object Table {
            val CellPaddingX: Dp = Relation.Related
            val CellPaddingY: Dp = Relation.Within
            val HeaderPaddingY: Dp = Relation.Related
            val RowGap: Dp = Foundation.Space.S0
        }
    }
}
