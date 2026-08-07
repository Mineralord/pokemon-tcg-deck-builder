package com.mineralord.tcg.core.designsystem.motion

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween

/**
 * Specs de animación compuestos a partir de [AnimationDurations] + [AnimationCurves]. Son la
 * capa que consumen los componentes cuando quieren un tween/repeatable con nombre semántico,
 * evitando reconstruir `tween(duración, curva)` en cada sitio.
 *
 * Genéricos en [T] para servir a cualquier propiedad animable (Float, Dp, Color, Offset…).
 */
object AnimationSpecs {

    fun <T> fast(easing: Easing = AnimationCurves.Standard): TweenSpec<T> =
        tween(durationMillis = AnimationDurations.Fast, easing = easing)

    fun <T> quick(easing: Easing = AnimationCurves.Standard): TweenSpec<T> =
        tween(durationMillis = AnimationDurations.Quick, easing = easing)

    fun <T> standard(easing: Easing = AnimationCurves.Standard): TweenSpec<T> =
        tween(durationMillis = AnimationDurations.Standard, easing = easing)

    fun <T> slow(easing: Easing = AnimationCurves.Standard): TweenSpec<T> =
        tween(durationMillis = AnimationDurations.Slow, easing = easing)

    /** Entrada estándar (desacelera). */
    fun <T> enter(): TweenSpec<T> =
        tween(durationMillis = AnimationDurations.Standard, easing = AnimationCurves.EmphasizedDecelerate)

    /** Salida estándar (acelera, algo más corta). */
    fun <T> exit(): TweenSpec<T> =
        tween(durationMillis = AnimationDurations.Quick, easing = AnimationCurves.EmphasizedAccelerate)

    /** Interpolación de color por defecto. */
    fun <T> color(): TweenSpec<T> =
        tween(durationMillis = AnimationDurations.Standard, easing = AnimationCurves.Standard)

    /** Bucle infinito con rebote (pulse/float). */
    fun <T> loop(
        durationMillis: Int,
        easing: Easing = AnimationCurves.Standard,
        repeatMode: RepeatMode = RepeatMode.Reverse,
    ): InfiniteRepeatableSpec<T> =
        infiniteRepeatable(animation = tween(durationMillis, easing = easing), repeatMode = repeatMode)

    /** Bucle infinito continuo (shimmer/rotación), sin rebote. */
    fun <T> loopLinear(durationMillis: Int): InfiniteRepeatableSpec<T> =
        infiniteRepeatable(animation = tween(durationMillis, easing = AnimationCurves.Linear), repeatMode = RepeatMode.Restart)
}
