package com.mineralord.tcg.core.animation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Primera versión funcional del [AnimationScheduler]: el **coordinador** del sistema.
 *
 * Responsabilidad: decidir *cuándo, cómo y bajo qué condiciones* cada
 * [AnimationRequest] de la [AnimationQueue] llega a reproducirse, aplicando
 * [AnimationPolicy]. NO es una cola (usa una), NO reproduce (delega en
 * [AnimationRunner]) y NO decide las reglas por sí mismo (delega en [PolicyArbiter]).
 *
 * Colaboradores (todos inyectados; Dependency Inversion, sin acoplar a Player/cola
 * concreta):
 * - [queue]           fuente de solicitudes.
 * - [runner]          arranca una solicitud y devuelve su handle.
 * - [policyResolver]  obtiene la política de cada solicitud.
 * - [arbiter]         decide qué hacer (función pura).
 * - [running]         registro de lo que está activo.
 * - [scope]           dónde viven las corrutinas de drenado/reproducción.
 *
 * Concurrencia: un único drenado a la vez (protegido por [drainMutex]). Mientras una
 * animación **exclusiva** corre, el drenado se suspende en su `await()`, bloqueando la
 * cola; las **paralelas** se lanzan y el drenado continúa de inmediato.
 */
class DefaultAnimationScheduler(
    private val queue: AnimationQueue,
    private val runner: AnimationRunner,
    private val policyResolver: AnimationPolicyResolver,
    private val arbiter: PolicyArbiter,
    private val running: RunningAnimations,
    private val scope: CoroutineScope,
) : AnimationScheduler {

    private val drainMutex = Mutex()

    override fun schedule(request: AnimationRequest) {
        queue.offer(request)
        scope.launch { drain() }
    }

    override fun schedule(requests: List<AnimationRequest>) {
        requests.forEach(queue::offer)
        scope.launch { drain() }
    }

    override fun cancel() {
        running.snapshot().forEach { it.handle.cancel() }
        queue.clear()
    }

    /** Drena la cola en serie. Sólo un drenado activo a la vez (el resto espera). */
    private suspend fun drain() = drainMutex.withLock {
        while (true) {
            val request = queue.poll() ?: break
            process(request)
        }
    }

    private suspend fun process(request: AnimationRequest) {
        val policy = policyResolver.resolve(request)
        when (val decision = arbiter.decide(policy, running.snapshot())) {
            SchedulingDecision.Skip -> Unit

            // Paralela: arranca y el drenado sigue sin esperar (permite concurrencia).
            SchedulingDecision.StartParallel -> startTracked(request, policy)

            // Exclusiva: espera a que drene lo activo, arranca y espera a que termine
            // (bloquea la cola mientras dura).
            SchedulingDecision.StartExclusive -> {
                awaitAll(running.snapshot())
                startTracked(request, policy).await()
            }

            // Reemplazo: cancela lo indicado, espera su cierre y arranca en exclusiva.
            is SchedulingDecision.ReplaceThenStart -> {
                decision.cancel.forEach { it.cancel() }
                decision.cancel.forEach { runCatching { it.await() } }
                startTracked(request, policy).await()
            }
        }
    }

    /**
     * Arranca la solicitud, la registra como activa y programa su des-registro cuando
     * termine. Devuelve el handle para que el llamador espere si la política lo exige.
     */
    private fun startTracked(request: AnimationRequest, policy: AnimationPolicy): AnimationHandle {
        val handle = runner.start(request)
        running.add(RunningAnimation(handle, policy))
        scope.launch {
            runCatching { handle.await() }
            running.remove(handle)
        }
        return handle
    }

    private suspend fun awaitAll(animations: List<RunningAnimation>) {
        animations.forEach { runCatching { it.handle.await() } }
    }
}
