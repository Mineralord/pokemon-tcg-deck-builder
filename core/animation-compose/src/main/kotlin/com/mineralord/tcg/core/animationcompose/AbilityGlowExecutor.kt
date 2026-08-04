package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.withFrameNanos
import com.mineralord.tcg.core.animation.AnimationExecutionContext
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.AnimationStepExecutor

/**
 * Ejecutor REAL del aura de habilidad ([AnimationStep.AbilityGlow]). Mismo esqueleto que
 * [EvolveExecutor] (resolver rect de ranura → frame-loop → limpieza), pero da vida a un
 * [AbilityGlowRenderNode] bajo la carta. Sólo avanza el progreso 0→1; el renderer convierte
 * progreso + [GlowVisual] en la respiración/bloom/rim/shimmer. Ranura ausente ⇒ no-op seguro.
 */
class AbilityGlowExecutor(
    private val coordinates: CoordinateRegistry,
    private val renderState: AnimationRenderState,
) : AnimationStepExecutor<AnimationStep.AbilityGlow> {

    override suspend fun execute(step: AnimationStep.AbilityGlow, context: AnimationExecutionContext) {
        val bounds = coordinates.bounds(SlotId(step.slotId))?.toRect() ?: return

        val nodeId = RenderNodeId("${context.animationId.value}:glow:${step.slotId}")
        val durationNanos = step.duration.inWholeNanoseconds.coerceAtLeast(1L)

        try {
            renderState.put(AbilityGlowRenderNode(nodeId, bounds, progress = 0f, visual = step.visual))

            var startNanos = -1L
            while (true) {
                val frameNanos = withFrameNanos { it }
                if (startNanos < 0L) startNanos = frameNanos
                val progress = ((frameNanos - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
                renderState.put(AbilityGlowRenderNode(nodeId, bounds, progress, step.visual))
                if (progress >= 1f) break
            }
        } finally {
            renderState.remove(nodeId)
        }
    }
}
