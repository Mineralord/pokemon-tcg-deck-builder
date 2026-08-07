package com.mineralord.tcg.core.designsystem.motion

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import kotlin.math.roundToInt

/**
 * Punto central que resuelve specs HONRANDO la configuración global ([AnimationConfig]):
 * aplica el factor de velocidad y el "reducir movimiento". Los componentes piden aquí sus
 * duraciones/specs en vez de leer la config y escalar por su cuenta, de modo que la política
 * de accesibilidad y ritmo vive en un solo lugar.
 */
object AnimationManager {

    /** ¿Están habilitadas las animaciones? (accesibilidad / preferencia). */
    @Composable
    @ReadOnlyComposable
    fun enabled(): Boolean = LocalAnimationConfig.current.enabled

    /** Duración base [AnimationDurations] ajustada por config: 0 si está deshabilitado. */
    @Composable
    @ReadOnlyComposable
    fun duration(base: Int): Int {
        val cfg = LocalAnimationConfig.current
        return if (!cfg.enabled) AnimationDurations.Instant
        else (base * cfg.speedFactor).roundToInt().coerceAtLeast(AnimationDurations.Instant)
    }

    /** Tween genérico consciente de la config (curva por defecto estándar). */
    @Composable
    @ReadOnlyComposable
    fun <T> tweenSpec(base: Int, easing: Easing = AnimationCurves.Standard): FiniteAnimationSpec<T> =
        tween(durationMillis = duration(base), easing = easing)

    /**
     * Resorte consciente de la config: si las animaciones están apagadas devuelve un tween
     * instantáneo; si no, el [spring] pedido. Genérico para cualquier propiedad animable.
     */
    @Composable
    @ReadOnlyComposable
    fun <T> springOrInstant(spring: SpringSpec<T>): FiniteAnimationSpec<T> =
        if (LocalAnimationConfig.current.enabled) spring else tween(AnimationDurations.Instant)
}
