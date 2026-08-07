package com.mineralord.tcg.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * Curvas de aceleración (easing) canónicas. ÚNICA fuente de verdad de las curvas: cualquier
 * spec del proyecto referencia estas y nunca crea un [CubicBezierEasing] suelto.
 */
object AnimationCurves {

    /** Lineal — para bucles continuos (shimmer, rotaciones constantes). */
    val Linear: Easing = LinearEasing

    /** Estándar simétrica — el default para la mayoría de transiciones. */
    val Standard: Easing = FastOutSlowInEasing

    /** Entra desacelerando — para elementos que APARECEN. */
    val Decelerate: Easing = LinearOutSlowInEasing

    /** Sale acelerando — para elementos que DESAPARECEN. */
    val Accelerate: Easing = FastOutLinearInEasing

    /** Enfática (Material 3) — movimiento expresivo con arranque decidido. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Enfática de entrada — desaceleración larga y suave. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Enfática de salida — aceleración corta y firme. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}
