package com.mineralord.tcg.core.animationcompose

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.withFrameNanos
import com.mineralord.tcg.core.animation.AnimationExecutionContext
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.AnimationStepExecutor

/**
 * Easing de asentado del robo (estándar "Snap sobrio"): salida rápida y **overshoot
 * contenido (~8%)** al final. Reutiliza `CubicBezierEasing` de Compose (la interfaz
 * [Easing] admite devolver >1 = overshoot nativo, sin librerías). `p2y = 1.08` fija el
 * rebasar-y-asentar; contenido para no verse "goma".
 */
val DrawCardEasing: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1.08f)

/** Altura del arco del robo, como fracción de la distancia origen→destino. */
private const val DRAW_ARC_HEIGHT_FRACTION = 0.18f

/**
 * Ejecutor REAL del robo ([AnimationStep.DrawCard]). Da vida temporal a un
 * [DrawCardRenderNode] y lo limpia al terminar.
 *
 * Comparte el esqueleto del [MoveExecutor] (resolver rects → frame-loop `withFrameNanos`
 * → limpieza en `finally`), pero **aplica [DrawCardEasing]** al progreso lineal antes de
 * publicarlo: así el nodo recibe un `progress` con overshoot y el renderer produce el
 * arco + escala + rebote sin conocer el tiempo. La altura del arco se deriva de la
 * distancia entre ranuras (arcos proporcionales a cuánto viaja la carta).
 *
 * Ranura ausente ⇒ no-op seguro. Cancelación ⇒ el `finally` elimina el nodo igualmente.
 */
class DrawCardExecutor(
    private val coordinates: CoordinateRegistry,
    private val renderState: AnimationRenderState,
) : AnimationStepExecutor<AnimationStep.DrawCard> {

    override suspend fun execute(step: AnimationStep.DrawCard, context: AnimationExecutionContext) {
        val origin = coordinates.bounds(SlotId(step.fromSlotId))?.toRect() ?: return
        val destination = coordinates.bounds(SlotId(step.toSlotId))?.toRect() ?: return

        val nodeId = RenderNodeId("${context.animationId.value}:draw:${step.fromSlotId}->${step.toSlotId}")
        val durationNanos = step.duration.inWholeNanoseconds.coerceAtLeast(1L)
        val arcHeightPx = (destination.center - origin.center).getDistance() * DRAW_ARC_HEIGHT_FRACTION

        try {
            renderState.put(DrawCardRenderNode(nodeId, origin, destination, arcHeightPx, progress = 0f))

            var startNanos = -1L
            while (true) {
                val frameNanos = withFrameNanos { it }
                if (startNanos < 0L) startNanos = frameNanos
                val linear = ((frameNanos - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
                val eased = DrawCardEasing.transform(linear)
                renderState.put(DrawCardRenderNode(nodeId, origin, destination, arcHeightPx, eased))
                if (linear >= 1f) break
            }
        } finally {
            renderState.remove(nodeId)
        }
    }
}
