package com.mineralord.tcg.core.animationcompose

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.withFrameNanos
import com.mineralord.tcg.core.animation.AnimationExecutionContext
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.AnimationStepExecutor

/**
 * Easing de asentamiento del Estadio: salida rápida y **overshoot algo mayor que el robo (~12%)** al
 * final, para leer "objeto pesado que aterriza con convicción". `p2y = 1.12` fija el rebasar-y-asentar
 * (más peso que `DrawCardEasing`, que usa 1.08). Reutiliza `CubicBezierEasing` (overshoot nativo >1).
 */
val StadiumPlaceEasing: Easing = CubicBezierEasing(0.20f, 0.90f, 0.25f, 1.12f)

/** Altura del arco de colocación, como fracción de la distancia mano→slot (más plano que el robo). */
private const val STADIUM_ARC_HEIGHT_FRACTION = 0.12f

/**
 * Ejecutor REAL de la colocación de Estadio ([AnimationStep.StadiumPlace]). Da vida temporal a un
 * [StadiumPlaceRenderNode] y lo limpia al terminar. Mismo esqueleto que [DrawCardExecutor] (resolver
 * rects → frame-loop `withFrameNanos` → limpieza en `finally`), pero aplica [StadiumPlaceEasing]: el
 * nodo recibe un `progress` con más overshoot y el renderer produce el arco + escala + inclinación +
 * flash de aterrizaje sin conocer el tiempo. Ranura ausente ⇒ no-op seguro; cancelación ⇒ el
 * `finally` elimina el nodo igualmente.
 */
class StadiumPlaceExecutor(
    private val coordinates: CoordinateRegistry,
    private val renderState: AnimationRenderState,
) : AnimationStepExecutor<AnimationStep.StadiumPlace> {

    override suspend fun execute(step: AnimationStep.StadiumPlace, context: AnimationExecutionContext) {
        val origin = coordinates.bounds(SlotId(step.fromSlotId))?.toRect() ?: return
        val destination = coordinates.bounds(SlotId(step.toSlotId))?.toRect() ?: return

        val nodeId = RenderNodeId("${context.animationId.value}:stadium:${step.fromSlotId}->${step.toSlotId}")
        val durationNanos = step.duration.inWholeNanoseconds.coerceAtLeast(1L)
        val arcHeightPx = (destination.center - origin.center).getDistance() * STADIUM_ARC_HEIGHT_FRACTION

        try {
            renderState.put(StadiumPlaceRenderNode(nodeId, origin, destination, arcHeightPx, progress = 0f))

            var startNanos = -1L
            while (true) {
                val frameNanos = withFrameNanos { it }
                if (startNanos < 0L) startNanos = frameNanos
                val linear = ((frameNanos - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
                val eased = StadiumPlaceEasing.transform(linear)
                renderState.put(StadiumPlaceRenderNode(nodeId, origin, destination, arcHeightPx, eased))
                if (linear >= 1f) break
            }
        } finally {
            renderState.remove(nodeId)
        }
    }
}
