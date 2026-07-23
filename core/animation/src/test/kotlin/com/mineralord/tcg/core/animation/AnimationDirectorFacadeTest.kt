package com.mineralord.tcg.core.animation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifica que el Facade expone la API pública correcta y agrega estado veraz,
 * usando SÓLO `AnimationDirector` (ninguna pieza interna asomando).
 */
class AnimationDirectorFacadeTest {

    private class FakeHandle : AnimationHandle {
        val completion = CompletableDeferred<AnimationResult>()
        override val id = AnimationId("fake")
        override val state
            get() = if (completion.isCompleted) AnimationHandle.RunningState.Finished
            else AnimationHandle.RunningState.Running
        override fun cancel() { completion.complete(AnimationResult.Cancelled) }
        override suspend fun await() = completion.await()
        fun finish() { completion.complete(AnimationResult.Completed) }
    }

    private fun req(id: String) = AnimationRequest.CardDrawn(playerId = "p", cardId = id)

    @Test
    fun `submit arranca la animacion y el estado pasa a Playing`() = runBlocking {
        val handles = mutableListOf<FakeHandle>()
        val director = DefaultAnimationDirector.create(
            runner = AnimationRunner { FakeHandle().also { handles += it } },
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        assertEquals(AnimationState.Idle, director.state.value)

        director.submit(req("a"))

        val state = director.state.value
        assertTrue(state.isPlaying)
        assertEquals("a", (state.current as AnimationRequest.CardDrawn).cardId)
    }

    @Test
    fun `al terminar la ultima animacion el estado vuelve a Idle`() = runBlocking {
        val handles = mutableListOf<FakeHandle>()
        val director = DefaultAnimationDirector.create(
            runner = AnimationRunner { FakeHandle().also { handles += it } },
            scope = CoroutineScope(Dispatchers.Unconfined),
            // Paralela para que ambas arranquen de inmediato.
            policyResolver = AnimationPolicyResolver {
                AnimationPolicy(concurrency = AnimationPolicy.Concurrency.Parallel)
            },
        )

        director.submit(listOf(req("a"), req("b")))
        assertTrue(director.state.value.isPlaying)

        handles[0].finish()
        assertTrue(director.state.value.isPlaying) // aún queda una activa

        handles[1].finish()
        assertEquals(AnimationState.Idle, director.state.value)
        assertNull(director.state.value.current)
    }
}
