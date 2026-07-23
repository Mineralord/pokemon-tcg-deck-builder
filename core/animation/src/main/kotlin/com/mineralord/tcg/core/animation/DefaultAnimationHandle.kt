package com.mineralord.tcg.core.animation

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

/**
 * Handle respaldado por corrutinas para una animación lanzada por el [AnimationPlayer].
 *
 * - [cancel] cancela el [Job] del recorrido (cancelación cooperativa: los ejecutores
 *   `suspend` la respetan en sus puntos de suspensión).
 * - [await] delega en el [Deferred] de finalización que el player completa con el
 *   [AnimationResult] final.
 * - No implementa [PausableHandle]: pausar/reanudar se abordará en una fase posterior;
 *   dejarlo fuera es coherente con ISP (no fingir una capacidad ausente).
 */
internal class DefaultAnimationHandle(
    override val id: AnimationId,
    private val job: Job,
    private val completion: Deferred<AnimationResult>,
) : AnimationHandle {

    override val state: AnimationHandle.RunningState
        get() = if (job.isActive) AnimationHandle.RunningState.Running
        else AnimationHandle.RunningState.Finished

    override fun cancel() {
        job.cancel()
    }

    override suspend fun await(): AnimationResult = completion.await()
}
