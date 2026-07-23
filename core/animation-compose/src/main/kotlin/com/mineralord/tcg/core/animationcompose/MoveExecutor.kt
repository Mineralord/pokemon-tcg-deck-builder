package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.withFrameNanos
import com.mineralord.tcg.core.animation.AnimationExecutionContext
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.AnimationStepExecutor

/**
 * Ejecutor REAL del primer paso hoja: [AnimationStep.Move].
 *
 * Responsabilidad: dar VIDA temporal a un [MoveRenderNode] y limpiarlo al terminar.
 * No dibuja (eso es del renderer); traduce la intención "mueve de A a B" a mutaciones
 * del [AnimationRenderState] a lo largo del tiempo:
 *
 *   1. Resuelve `fromSlotId`/`toSlotId` → rectángulos reales vía [CoordinateRegistry]
 *      (cruza aquí la frontera lógico↔pantalla). Ranura ausente ⇒ no-op seguro.
 *   2. Crea el nodo en la capa [RenderLayer.Flight] con `progress = 0`.
 *   3. Avanza `progress` 0→1 con la duración del paso, un frame a la vez vía
 *      [withFrameNanos] (reloj de frames de Compose ⇒ cancelación cooperativa gratis).
 *   4. `finally`: elimina el nodo — pase lo que pase (fin normal o cancelación).
 *
 * La interpolación geométrica NO vive aquí: el ejecutor sólo mueve el reloj (progress);
 * el renderer convierte progress→posición. Trayectoria lineal en esta fase.
 *
 * Dependencias inyectadas (las provee el composition root en su fase): comparte el
 * mismo [CoordinateRegistry] y [AnimationRenderState] que el `AnimationStage`.
 */
class MoveExecutor(
    private val coordinates: CoordinateRegistry,
    private val renderState: AnimationRenderState,
) : AnimationStepExecutor<AnimationStep.Move> {

    override suspend fun execute(step: AnimationStep.Move, context: AnimationExecutionContext) {
        val origin = coordinates.bounds(SlotId(step.fromSlotId))?.toRect() ?: return
        val destination = coordinates.bounds(SlotId(step.toSlotId))?.toRect() ?: return

        // Id de nodo único por instancia de animación + paso concreto, para no colisionar
        // si otra animación mueve algo a la vez.
        val nodeId = RenderNodeId("${context.animationId.value}:move:${step.fromSlotId}->${step.toSlotId}")
        val durationNanos = step.duration.inWholeNanoseconds.coerceAtLeast(1L)

        try {
            renderState.put(MoveRenderNode(nodeId, origin, destination, progress = 0f))

            var startNanos = -1L
            while (true) {
                val frameNanos = withFrameNanos { it }
                if (startNanos < 0L) startNanos = frameNanos
                val elapsed = frameNanos - startNanos
                val progress = (elapsed.toFloat() / durationNanos).coerceIn(0f, 1f)
                renderState.put(MoveRenderNode(nodeId, origin, destination, progress))
                if (progress >= 1f) break
            }
        } finally {
            renderState.remove(nodeId)
        }
    }
}
