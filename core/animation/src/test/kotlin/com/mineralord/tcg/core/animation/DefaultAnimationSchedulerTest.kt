package com.mineralord.tcg.core.animation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifica la COORDINACIÓN (no reproducción real): que el scheduler serializa las
 * exclusivas y solapa las paralelas, usando handles controlables manualmente.
 * Se usa el dispatcher Unconfined para ejecución determinista hasta cada suspensión.
 */
class DefaultAnimationSchedulerTest {

    private class FakeHandle(override val id: AnimationId) : AnimationHandle {
        val completion = CompletableDeferred<AnimationResult>()
        override val state
            get() = if (completion.isCompleted) AnimationHandle.RunningState.Finished
            else AnimationHandle.RunningState.Running
        override fun cancel() { completion.complete(AnimationResult.Cancelled) }
        override suspend fun await() = completion.await()
        fun finish() { completion.complete(AnimationResult.Completed) }
    }

    private class FifoQueue : AnimationQueue {
        private val items = ArrayDeque<AnimationRequest>()
        override val isEmpty get() = items.isEmpty()
        override val size get() = items.size
        override fun enqueue(request: AnimationRequest) { items.addLast(request) }
        override fun dequeue(): AnimationRequest? = items.removeFirstOrNull()
        override fun peek(): AnimationRequest? = items.firstOrNull()
        override fun clear() = items.clear()
    }

    private fun req(id: String) = AnimationRequest.CardDrawn(playerId = "p", cardId = id)

    private fun scheduler(
        policy: AnimationPolicy,
        started: MutableList<AnimationRequest>,
        handles: MutableList<FakeHandle>,
    ): AnimationScheduler {
        val runner = AnimationRunner { request ->
            started += request
            FakeHandle(AnimationId((request as AnimationRequest.CardDrawn).cardId)).also { handles += it }
        }
        return DefaultAnimationScheduler(
            queue = FifoQueue(),
            runner = runner,
            policyResolver = AnimationPolicyResolver { policy },
            arbiter = DefaultPolicyArbiter(),
            running = RunningAnimations(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
    }

    @Test
    fun `exclusivas se ejecutan de una en una`() = runBlocking {
        val started = CopyOnWriteArrayList<AnimationRequest>()
        val handles = CopyOnWriteArrayList<FakeHandle>()
        val scheduler = scheduler(
            AnimationPolicy(), // Exclusive + Enqueue por defecto
            started, handles,
        )

        scheduler.schedule(req("a"))
        scheduler.schedule(req("b"))

        // Sólo la primera arrancó; la segunda espera en la cola.
        assertEquals(listOf("a"), started.map { (it as AnimationRequest.CardDrawn).cardId })

        handles[0].finish() // al terminar la primera, arranca la segunda
        assertEquals(listOf("a", "b"), started.map { (it as AnimationRequest.CardDrawn).cardId })

        handles[1].finish()
    }

    @Test
    fun `paralelas se solapan`() = runBlocking {
        val started = CopyOnWriteArrayList<AnimationRequest>()
        val handles = CopyOnWriteArrayList<FakeHandle>()
        val scheduler = scheduler(
            AnimationPolicy(concurrency = AnimationPolicy.Concurrency.Parallel),
            started, handles,
        )

        scheduler.schedule(listOf(req("x"), req("y"), req("z")))

        // Las tres arrancan sin necesidad de que ninguna termine.
        assertEquals(
            listOf("x", "y", "z"),
            started.map { (it as AnimationRequest.CardDrawn).cardId },
        )
    }

    @Test
    fun `Ignore descarta la entrante mientras algo corre`() = runBlocking {
        val started = CopyOnWriteArrayList<AnimationRequest>()
        val handles = CopyOnWriteArrayList<FakeHandle>()
        val scheduler = scheduler(
            AnimationPolicy(conflict = AnimationPolicy.Conflict.Ignore),
            started, handles,
        )

        scheduler.schedule(req("a")) // arranca y queda activa (no la terminamos)
        scheduler.schedule(req("b")) // debe ignorarse

        assertEquals(listOf("a"), started.map { (it as AnimationRequest.CardDrawn).cardId })
        handles[0].finish()
    }
}
