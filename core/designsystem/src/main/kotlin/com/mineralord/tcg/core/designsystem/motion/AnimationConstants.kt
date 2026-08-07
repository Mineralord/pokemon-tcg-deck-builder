package com.mineralord.tcg.core.designsystem.motion

/**
 * Magnitudes crudas (sin unidad de tiempo) del framework de animación. ÚNICA fuente de verdad
 * para escalas, opacidades, desplazamientos y rotaciones. Ningún componente debe declarar estos
 * números por su cuenta: siempre referenciar aquí (o los specs derivados) para evitar duplicación.
 */
object AnimationConstants {

    // --- Escala ---------------------------------------------------------------
    const val ScaleFull = 1f
    const val ScalePressed = 0.94f
    const val ScaleCollapsed = 0.85f
    const val ScaleZoomIn = 1.06f
    const val ScaleZoomOut = 0.50f
    const val ScaleDialog = 0.90f
    const val ScalePulseMin = 0.97f
    const val ScalePulseMax = 1.03f

    // --- Opacidad -------------------------------------------------------------
    const val AlphaTransparent = 0f
    const val AlphaOpaque = 1f
    const val AlphaScrim = 0.60f
    const val AlphaDim = 0.40f

    // --- Desplazamiento como FRACCIÓN del tamaño del contenedor (slides) ------
    const val SlideFractionFull = 1f
    const val SlideFractionPartial = 0.12f

    // --- Desplazamiento ABSOLUTO en dp (float/shake) --------------------------
    const val FloatOffsetDp = 6f
    const val ShakeOffsetDp = 8f
    const val ShakeCycles = 6

    // --- Rotación (grados) ----------------------------------------------------
    const val RotationNone = 0f
    const val RotationQuarter = 90f
    const val RotationFlip = 180f
    const val RotationFull = 360f

    // --- Configuración global por defecto -------------------------------------
    const val SpeedNormal = 1f
}
