package com.mineralord.tcg.core.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlinx.coroutines.launch

/**
 * Primitivas de MOVIMIENTO FÍSICO del tablero. Jetpack Compose no ofrece de forma nativa y
 * estable una "animación de reubicación" (que un hijo se DESLICE con inercia cuando su posición
 * dentro del layout cambia por reordenamiento/inserción). Aquí se implementa esa capacidad como
 * abstracción reutilizable con API limpia, además de la entrada/elevación con resorte.
 *
 * Todas respetan la config global ([AnimationManager]) y son puramente visuales.
 */

/**
 * Anima la REUBICACIÓN del elemento dentro de su contenedor: cuando su posición relativa al
 * padre cambia (reordenar mano, rellenar banca, reflujo), se desplaza con un resorte en vez de
 * saltar. Basado en contra-desplazamiento medido con [onPlaced] + [positionInParent].
 */
fun Modifier.animatePlacement(
    stiffness: Float = Spring.StiffnessMediumLow,
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
): Modifier = composed {
    val enabled = AnimationManager.enabled()
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    this
        .onPlaced { coords -> targetOffset = coords.positionInParent().round() }
        .offset {
            val anim = animatable
                ?: Animatable(targetOffset, IntOffset.VectorConverter).also { animatable = it }
            if (anim.targetValue != targetOffset) {
                scope.launch {
                    anim.animateTo(
                        targetValue = targetOffset,
                        animationSpec = if (enabled) {
                            spring(dampingRatio = dampingRatio, stiffness = stiffness, visibilityThreshold = IntOffset(1, 1))
                        } else {
                            snap()
                        },
                    )
                }
            }
            anim.value - targetOffset
        }
}

/**
 * ENTRADA de un elemento (fade + escala + leve subida) reproducida una sola vez por [key]
 * (típicamente el id de la carta). Da la sensación de que la carta "aterriza" en su zona.
 */
fun Modifier.motionAppear(
    key: Any? = Unit,
    slideUpDp: Float = 10f,
    fromScale: Float = 0.90f,
): Modifier = composed {
    val enabled = AnimationManager.enabled()
    val progress = remember(key) { Animatable(if (enabled) 0f else 1f) }
    val slidePx = with(LocalDensity.current) { slideUpDp.dp.toPx() }
    LaunchedEffect(key) {
        if (enabled) progress.animateTo(1f, AnimationSprings.bouncy())
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        val s = fromScale + (1f - fromScale) * p
        scaleX = s
        scaleY = s
        translationY = slidePx * (1f - p)
    }
}

/**
 * ELEVACIÓN/selección: al activarse [active] el elemento escala, sube y proyecta sombra con un
 * resorte (feedback físico de "cogido"/seleccionado). Al desactivarse regresa suavemente.
 */
fun Modifier.motionElevate(
    active: Boolean,
    liftDp: Float = 8f,
    activeScale: Float = 1.06f,
    maxShadow: Float = 14f,
    cornerDp: Float = 10f,
): Modifier = composed {
    val enabled = AnimationManager.enabled()
    val f by animateFloatAsState(
        targetValue = if (active && enabled) 1f else 0f,
        animationSpec = AnimationSprings.bouncy(),
        label = "motionElevate",
    )
    val liftPx = with(LocalDensity.current) { liftDp.dp.toPx() }
    graphicsLayer {
        val s = 1f + (activeScale - 1f) * f
        scaleX = s
        scaleY = s
        translationY = -liftPx * f
        shadowElevation = maxShadow * f
        shape = RoundedCornerShape(cornerDp.dp)
        clip = false
    }
}
