package com.mineralord.tcg.core.designsystem.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Borde/eje desde el que un contenido se desliza al entrar o salir. */
enum class MotionEdge { Start, End, Top, Bottom }

/**
 * Fábrica CENTRAL de [EnterTransition]/[ExitTransition], consciente de la config global
 * ([AnimationManager]). Los componentes Motion consumen estas y nunca construyen sus propias
 * transiciones inline, garantizando ritmo y curvas coherentes en todo el proyecto.
 */
object MotionTransitions {

    @Composable
    fun enter(): EnterTransition {
        val d = AnimationManager.duration(AnimationDurations.Standard)
        return fadeIn(tween(d, AnimationCurves.EmphasizedDecelerate)) +
            scaleIn(
                initialScale = AnimationConstants.ScaleCollapsed,
                animationSpec = tween(d, AnimationCurves.EmphasizedDecelerate),
            )
    }

    @Composable
    fun exit(): ExitTransition {
        val d = AnimationManager.duration(AnimationDurations.Quick)
        return fadeOut(tween(d, AnimationCurves.EmphasizedAccelerate)) +
            scaleOut(
                targetScale = AnimationConstants.ScaleCollapsed,
                animationSpec = tween(d, AnimationCurves.EmphasizedAccelerate),
            )
    }

    @Composable
    fun dialogEnter(): EnterTransition {
        val d = AnimationManager.duration(AnimationDurations.Standard)
        return fadeIn(tween(d, AnimationCurves.EmphasizedDecelerate)) +
            scaleIn(
                initialScale = AnimationConstants.ScaleDialog,
                animationSpec = tween(d, AnimationCurves.EmphasizedDecelerate),
            )
    }

    @Composable
    fun dialogExit(): ExitTransition {
        val d = AnimationManager.duration(AnimationDurations.Quick)
        return fadeOut(tween(d, AnimationCurves.EmphasizedAccelerate)) +
            scaleOut(
                targetScale = AnimationConstants.ScaleDialog,
                animationSpec = tween(d, AnimationCurves.EmphasizedAccelerate),
            )
    }

    @Composable
    fun overlayEnter(): EnterTransition =
        fadeIn(tween(AnimationManager.duration(AnimationDurations.Quick), AnimationCurves.Standard))

    @Composable
    fun overlayExit(): ExitTransition =
        fadeOut(tween(AnimationManager.duration(AnimationDurations.Quick), AnimationCurves.Standard))

    @Composable
    fun slideEnter(edge: MotionEdge): EnterTransition {
        val d = AnimationManager.duration(AnimationDurations.Standard)
        val fade = fadeIn(tween(d, AnimationCurves.Decelerate))
        val spec = tween<androidx.compose.ui.unit.IntOffset>(d, easing = AnimationCurves.EmphasizedDecelerate)
        val slide = when (edge) {
            MotionEdge.Start -> slideInHorizontally(spec) { -it }
            MotionEdge.End -> slideInHorizontally(spec) { it }
            MotionEdge.Top -> slideInVertically(spec) { -it }
            MotionEdge.Bottom -> slideInVertically(spec) { it }
        }
        return slide + fade
    }

    @Composable
    fun slideExit(edge: MotionEdge): ExitTransition {
        val d = AnimationManager.duration(AnimationDurations.Quick)
        val fade = fadeOut(tween(d, AnimationCurves.Accelerate))
        val spec = tween<androidx.compose.ui.unit.IntOffset>(d, easing = AnimationCurves.EmphasizedAccelerate)
        val slide = when (edge) {
            MotionEdge.Start -> slideOutHorizontally(spec) { -it }
            MotionEdge.End -> slideOutHorizontally(spec) { it }
            MotionEdge.Top -> slideOutVertically(spec) { -it }
            MotionEdge.Bottom -> slideOutVertically(spec) { it }
        }
        return slide + fade
    }
}

// tween sin importar el símbolo global (evita choque de nombres al reexportar arriba).
private fun <T> tween(durationMillis: Int, easing: androidx.compose.animation.core.Easing) =
    androidx.compose.animation.core.tween<T>(durationMillis = durationMillis, easing = easing)

// ---------------------------------------------------------------------------
// Modificadores de movimiento reutilizables (feedback e infinitos)
// ---------------------------------------------------------------------------

/**
 * Feedback de PULSACIÓN: encoge a [pressedScale] mientras el dedo está abajo (resorte snappy).
 * Si [onClick] no es null, añade el clickable SIN indicación (el propio escalado es el feedback).
 */
fun Modifier.motionPress(
    pressedScale: Float = AnimationConstants.ScalePressed,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val target = if (pressed && enabled && AnimationManager.enabled()) pressedScale else AnimationConstants.ScaleFull
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = AnimationSprings.snappy(),
        label = "motionPressScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
            } else {
                Modifier
            },
        )
}

/** Latido infinito (escala oscilante) entre [min] y [max]. Respeta la config global. */
fun Modifier.motionPulse(
    min: Float = AnimationConstants.ScalePulseMin,
    max: Float = AnimationConstants.ScalePulseMax,
    durationMillis: Int = AnimationDurations.Pulse,
): Modifier = composed {
    val enabled = AnimationManager.enabled()
    val transition = rememberInfiniteTransition(label = "motionPulse")
    val scale by transition.animateFloat(
        initialValue = min,
        targetValue = max,
        animationSpec = AnimationSpecs.loop(durationMillis),
        label = "motionPulseScale",
    )
    val applied = if (enabled) scale else AnimationConstants.ScaleFull
    graphicsLayer { scaleX = applied; scaleY = applied }
}

/** Flotación infinita (desplazamiento vertical suave) de amplitud [offsetDp]. */
fun Modifier.motionFloat(
    offsetDp: Float = AnimationConstants.FloatOffsetDp,
    durationMillis: Int = AnimationDurations.Float,
): Modifier = composed {
    val enabled = AnimationManager.enabled()
    val px = with(LocalDensity.current) { offsetDp.dp.toPx() }
    val transition = rememberInfiniteTransition(label = "motionFloat")
    val y by transition.animateFloat(
        initialValue = -px / 2f,
        targetValue = px / 2f,
        animationSpec = AnimationSpecs.loop(durationMillis),
        label = "motionFloatY",
    )
    graphicsLayer { translationY = if (enabled) y else 0f }
}

/**
 * Sacudida DISPARADA por [trigger]: cada vez que [trigger] cambia (y no es null) reproduce una
 * oscilación horizontal amortiguada de [cycles] ciclos y amplitud [offsetDp].
 */
fun Modifier.motionShake(
    trigger: Any?,
    offsetDp: Float = AnimationConstants.ShakeOffsetDp,
    cycles: Int = AnimationConstants.ShakeCycles,
): Modifier = composed {
    val enabled = AnimationManager.enabled()
    val offsetX = remember { Animatable(0f) }
    val px = with(LocalDensity.current) { offsetDp.dp.toPx() }
    androidx.compose.runtime.LaunchedEffect(trigger) {
        if (trigger == null || !enabled) return@LaunchedEffect
        offsetX.snapTo(0f)
        offsetX.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = AnimationDurations.Shake
                val step = AnimationDurations.Shake / (cycles + 1)
                for (i in 1..cycles) {
                    val decay = 1f - i.toFloat() / (cycles + 1)
                    val amp = px * decay * if (i % 2 == 0) 1f else -1f
                    amp at step * i
                }
            },
        )
    }
    graphicsLayer { translationX = offsetX.value }
}
