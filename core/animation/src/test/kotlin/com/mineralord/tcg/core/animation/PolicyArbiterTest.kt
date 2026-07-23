package com.mineralord.tcg.core.animation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests del cerebro puro: sin corrutinas ni fakes, sólo entrada→decisión. */
class PolicyArbiterTest {

    private val arbiter = DefaultPolicyArbiter()

    private fun handle() = object : AnimationHandle {
        override val id = AnimationId("h")
        override val state = AnimationHandle.RunningState.Running
        override fun cancel() {}
        override suspend fun await() = AnimationResult.Completed
    }

    private fun running(policy: AnimationPolicy) = RunningAnimation(handle(), policy)

    @Test
    fun `sin nada activo, exclusiva arranca en exclusiva`() {
        val d = arbiter.decide(AnimationPolicy(), emptyList())
        assertEquals(SchedulingDecision.StartExclusive, d)
    }

    @Test
    fun `sin nada activo, paralela arranca en paralelo`() {
        val d = arbiter.decide(
            AnimationPolicy(concurrency = AnimationPolicy.Concurrency.Parallel),
            emptyList(),
        )
        assertEquals(SchedulingDecision.StartParallel, d)
    }

    @Test
    fun `paralela siempre corre en paralelo aunque haya algo activo`() {
        val d = arbiter.decide(
            AnimationPolicy(concurrency = AnimationPolicy.Concurrency.Parallel),
            listOf(running(AnimationPolicy())),
        )
        assertEquals(SchedulingDecision.StartParallel, d)
    }

    @Test
    fun `exclusiva Ignore con algo activo se salta`() {
        val d = arbiter.decide(
            AnimationPolicy(conflict = AnimationPolicy.Conflict.Ignore),
            listOf(running(AnimationPolicy())),
        )
        assertEquals(SchedulingDecision.Skip, d)
    }

    @Test
    fun `exclusiva Enqueue con algo activo espera (exclusiva)`() {
        val d = arbiter.decide(
            AnimationPolicy(conflict = AnimationPolicy.Conflict.Enqueue),
            listOf(running(AnimationPolicy())),
        )
        assertEquals(SchedulingDecision.StartExclusive, d)
    }

    @Test
    fun `exclusiva Replace cancela lo cancelable`() {
        val activa = running(AnimationPolicy(interruptibility = AnimationPolicy.Interruptibility.Cancellable))
        val d = arbiter.decide(
            AnimationPolicy(conflict = AnimationPolicy.Conflict.Replace),
            listOf(activa),
        )
        assertTrue(d is SchedulingDecision.ReplaceThenStart)
        assertEquals(listOf(activa.handle), d.cancel)
    }

    @Test
    fun `exclusiva Replace se degrada a espera si algo es MustFinish`() {
        val d = arbiter.decide(
            AnimationPolicy(conflict = AnimationPolicy.Conflict.Replace),
            listOf(running(AnimationPolicy(interruptibility = AnimationPolicy.Interruptibility.MustFinish))),
        )
        assertEquals(SchedulingDecision.StartExclusive, d)
    }
}
