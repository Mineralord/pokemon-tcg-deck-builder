package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.withFrameNanos
import com.mineralord.tcg.core.animation.AnimationExecutionContext
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.AnimationStepExecutor

/**
 * Ejecutor REAL del rótulo de anuncio ([AnimationStep.Banner]). Comparte el esqueleto de los demás
 * ejecutores (frame-loop `withFrameNanos` → limpieza en `finally`), pero da vida a un
 * [BannerRenderNode] a pantalla completa (no depende de ranuras). Sólo avanza el progreso lineal
 * 0→1; el renderer convierte progreso + [AnimationStep.Banner.visual] en el barrido/sostenido/salida.
 */
class BannerExecutor(
    private val renderState: AnimationRenderState,
) : AnimationStepExecutor<AnimationStep.Banner> {

    override suspend fun execute(step: AnimationStep.Banner, context: AnimationExecutionContext) {
        val nodeId = RenderNodeId("${context.animationId.value}:banner")
        val durationNanos = step.duration.inWholeNanoseconds.coerceAtLeast(1L)

        try {
            renderState.put(BannerRenderNode(nodeId, step.kicker, step.title, 0f, step.visual))

            var startNanos = -1L
            while (true) {
                val frameNanos = withFrameNanos { it }
                if (startNanos < 0L) startNanos = frameNanos
                val progress = ((frameNanos - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
                renderState.put(BannerRenderNode(nodeId, step.kicker, step.title, progress, step.visual))
                if (progress >= 1f) break
            }
        } finally {
            renderState.remove(nodeId)
        }
    }
}
