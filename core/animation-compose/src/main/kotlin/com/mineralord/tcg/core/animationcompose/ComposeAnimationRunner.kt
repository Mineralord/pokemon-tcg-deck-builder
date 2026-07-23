package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.AnimationHandle
import com.mineralord.tcg.core.animation.AnimationId
import com.mineralord.tcg.core.animation.AnimationPlayer
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animation.AnimationResult
import com.mineralord.tcg.core.animation.AnimationRunner
import com.mineralord.tcg.core.animation.AnimationStepExecutorRegistry
import kotlinx.coroutines.CoroutineScope

/**
 * Implementación Compose del seam [AnimationRunner] que `core:animation` dejó abierto.
 *
 * Cierra el eslabón `AnimationRequest → AnimationDefinition → AnimationPlayer`:
 * 1. resuelve la receta de la request en el [AnimationDefinitionRegistry] distribuido;
 * 2. delega su reproducción en el [AnimationPlayer] (que interpreta el árbol de pasos);
 * 3. devuelve el [AnimationHandle] resultante.
 *
 * Es el `runner` que el composition root pasa a `DefaultAnimationDirector.create(...)`.
 * No conoce cola, scheduler ni estado: sólo traduce request→reproducción.
 *
 * Request sin receta registrada ⇒ [CompletedHandle] inmediato: no se rompe el pipeline
 * ni se cuelga el scheduler; simplemente no hay animación visual para ese evento.
 */
class ComposeAnimationRunner(
    private val definitions: AnimationDefinitionRegistry,
    private val player: AnimationPlayer,
) : AnimationRunner {

    override fun start(request: AnimationRequest): AnimationHandle {
        val definition = definitions.resolve(request)
            ?: return CompletedHandle(AnimationId("unresolved:${request::class.simpleName}"))
        return player.play(definition)
    }

    companion object {
        /**
         * Construye el runner con su [AnimationPlayer] interno.
         *
         * @param executors registro de ejecutores de pasos (vacío hasta que se
         *        implementen executors reales en la siguiente fase).
         * @param scope alcance de corrutinas de reproducción.
         */
        fun create(
            definitions: AnimationDefinitionRegistry,
            executors: AnimationStepExecutorRegistry,
            scope: CoroutineScope,
        ): ComposeAnimationRunner =
            ComposeAnimationRunner(definitions, AnimationPlayer(executors, scope))
    }
}

/** Handle inerte ya finalizado, para requests sin receta o efectos no visuales. */
private class CompletedHandle(override val id: AnimationId) : AnimationHandle {
    override val state = AnimationHandle.RunningState.Finished
    override fun cancel() = Unit
    override suspend fun await(): AnimationResult = AnimationResult.Completed
}
