package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.withFrameNanos
import com.mineralord.tcg.core.animation.AnimationExecutionContext
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.AnimationStepExecutor

/**
 * Ejecutor REAL de la evolución in-situ ([AnimationStep.Evolve]). Comparte el esqueleto de
 * [MoveExecutor]/[DrawCardExecutor] (resolver rect → frame-loop `withFrameNanos` → limpieza en
 * `finally`), pero da vida a un [EvolveRenderNode] en la ranura indicada: sólo avanza el
 * progreso lineal 0→1; el renderer convierte progreso + [AnimationStep.Evolve.visual] en la
 * imagen (ascenso/giro/bombeo/destello/anillo). Ranura ausente ⇒ no-op seguro.
 */
class EvolveExecutor(
    private val coordinates: CoordinateRegistry,
    private val renderState: AnimationRenderState,
) : AnimationStepExecutor<AnimationStep.Evolve> {

    override suspend fun execute(step: AnimationStep.Evolve, context: AnimationExecutionContext) {
        val bounds = coordinates.bounds(SlotId(step.slotId))?.toRect() ?: return

        val nodeId = RenderNodeId("${context.animationId.value}:evolve:${step.slotId}")
        val durationNanos = step.duration.inWholeNanoseconds.coerceAtLeast(1L)

        try {
            renderState.put(EvolveRenderNode(nodeId, bounds, progress = 0f, visual = step.visual))

            var startNanos = -1L
            while (true) {
                val frameNanos = withFrameNanos { it }
                if (startNanos < 0L) startNanos = frameNanos
                val progress = ((frameNanos - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
                renderState.put(EvolveRenderNode(nodeId, bounds, progress, step.visual))
                if (progress >= 1f) break
            }
        } finally {
            renderState.remove(nodeId)
        }
    }
}
