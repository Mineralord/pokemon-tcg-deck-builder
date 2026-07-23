package com.mineralord.tcg.core.animation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Núcleo del reproductor: **intérprete del árbol de [AnimationStep]**.
 *
 * Responsabilidad ÚNICA: recorrer el árbol de una [AnimationDefinition] y coordinar
 * la ejecución de sus nodos. NO dibuja, NO mueve cartas, NO conoce Compose/Android/
 * engine. Las acciones reales viven en los [AnimationStepExecutor] registrados; el
 * player sólo los orquesta.
 *
 * Reparto de responsabilidades (decisión de diseño):
 * - **Compuestos** ([AnimationStep.Sequence], [AnimationStep.Parallel]): control de
 *   flujo → los resuelve el propio player por recursión. Conjunto cerrado y conocido.
 * - **Hojas** (Move, Scale, …): conjunto abierto → se delegan al
 *   [AnimationStepExecutorRegistry]. Añadir una hoja no toca esta clase (Open-Closed).
 *
 * Concurrencia: usa corrutinas. `Sequence` se recorre secuencialmente; `Parallel`
 * lanza sus hijos concurrentemente y espera a todos. La cancelación del handle se
 * propaga cooperativamente a través de la jerarquía de corrutinas.
 *
 * Fase actual: el recorrido está COMPLETO y es correcto, pero como aún no hay
 * ejecutores registrados, las hojas hacen no-op. No se reproduce ninguna animación
 * real todavía.
 *
 * @param registry ejecutores de hojas (inyectado; Dependency Inversion).
 * @param scope alcance de corrutinas donde vive cada reproducción (inyectado: el
 *              llamador decide el dispatcher/ciclo de vida, sin acoplar a Android).
 */
class AnimationPlayer(
    private val registry: AnimationStepExecutorRegistry,
    private val scope: CoroutineScope,
) {

    /**
     * Lanza la reproducción de [definition] y devuelve de inmediato un
     * [AnimationHandle] para operar sobre ella (cancelar, esperar su desenlace).
     */
    fun play(definition: AnimationDefinition): AnimationHandle {
        val completion = CompletableDeferred<AnimationResult>()
        val context = AnimationExecutionContext(definition.id)

        val job = scope.launch {
            try {
                executeAll(definition.steps, context)
                completion.complete(AnimationResult.Completed)
            } catch (cancellation: CancellationException) {
                completion.complete(AnimationResult.Cancelled)
                throw cancellation // respeta la semántica estructurada de corrutinas
            } catch (error: Throwable) {
                completion.complete(AnimationResult.Failed(error))
            }
        }

        return DefaultAnimationHandle(definition.id, job, completion)
    }

    /** Recorre una lista de pasos en orden (la raíz de una definición es una lista). */
    private suspend fun executeAll(
        steps: List<AnimationStep>,
        context: AnimationExecutionContext,
    ) {
        for (step in steps) execute(step, context)
    }

    /**
     * Interpreta un nodo. Los compuestos se recursan aquí; las hojas se despachan al
     * ejecutor registrado (o no-op si no hay ninguno en esta fase).
     */
    private suspend fun execute(step: AnimationStep, context: AnimationExecutionContext) {
        when (step) {
            is AnimationStep.Sequence ->
                executeAll(step.children, context)

            is AnimationStep.Parallel ->
                coroutineScope {
                    for (child in step.children) {
                        launch { execute(child, context) }
                    }
                }

            else ->
                dispatchLeaf(step, context)
        }
    }

    /** Entrega una hoja a su ejecutor. Sin ejecutor registrado ⇒ no-op seguro. */
    private suspend fun dispatchLeaf(
        step: AnimationStep,
        context: AnimationExecutionContext,
    ) {
        val executor = registry.executorFor(step) ?: return
        executor.execute(step, context)
    }
}
