package com.mineralord.tcg.core.animation

/**
 * Decisión que toma el [PolicyArbiter]: qué debe hacer el scheduler con una solicitud
 * entrante dada la política y lo que ya está corriendo. Sólo describe la acción; no la
 * ejecuta.
 */
sealed interface SchedulingDecision {

    /** Descartar la solicitud entrante (política Ignore con algo en ejecución). */
    data object Skip : SchedulingDecision

    /** Arrancar de inmediato, conviviendo con lo que ya corre (concurrencia Parallel). */
    data object StartParallel : SchedulingDecision

    /**
     * Arrancar en exclusiva: el scheduler debe esperar a que se vacíe lo activo y
     * bloquear la cola hasta que esta animación termine.
     */
    data object StartExclusive : SchedulingDecision

    /**
     * Cancelar [cancel] (que sí son cancelables) y luego arrancar la entrante en
     * exclusiva (política Replace).
     */
    data class ReplaceThenStart(val cancel: List<AnimationHandle>) : SchedulingDecision
}

/**
 * El **cerebro** del sistema: función PURA que decide, sin efectos secundarios ni
 * corrutinas. Recibe la política de la entrante y el estado activo; devuelve una
 * [SchedulingDecision]. Al ser pura, se testea exhaustivamente en aislamiento.
 */
fun interface PolicyArbiter {
    fun decide(incoming: AnimationPolicy, running: List<RunningAnimation>): SchedulingDecision
}

/**
 * Primera implementación funcional del arbitraje.
 *
 * Reglas (primera versión):
 * - Nada en ejecución ⇒ arrancar (Parallel o Exclusive según la propia política).
 * - Entrante Parallel ⇒ arrancar en paralelo, siempre.
 * - Entrante Exclusive:
 *     · Conflict.Ignore  ⇒ [SchedulingDecision.Skip].
 *     · Conflict.Enqueue ⇒ [SchedulingDecision.StartExclusive] (espera a que drene).
 *     · Conflict.Replace ⇒ si TODO lo activo es cancelable, [ReplaceThenStart]; si algo
 *       es [AnimationPolicy.Interruptibility.MustFinish], no se puede reemplazar sin
 *       romper esa garantía, así que se degrada a [StartExclusive] (esperar).
 */
class DefaultPolicyArbiter : PolicyArbiter {

    override fun decide(
        incoming: AnimationPolicy,
        running: List<RunningAnimation>,
    ): SchedulingDecision {
        if (running.isEmpty()) {
            return if (incoming.concurrency == AnimationPolicy.Concurrency.Parallel) {
                SchedulingDecision.StartParallel
            } else {
                SchedulingDecision.StartExclusive
            }
        }

        if (incoming.concurrency == AnimationPolicy.Concurrency.Parallel) {
            return SchedulingDecision.StartParallel
        }

        // Entrante exclusiva con algo corriendo: resolver por conflicto.
        return when (incoming.conflict) {
            AnimationPolicy.Conflict.Ignore -> SchedulingDecision.Skip
            AnimationPolicy.Conflict.Enqueue -> SchedulingDecision.StartExclusive
            AnimationPolicy.Conflict.Replace -> {
                val anyMustFinish = running.any {
                    it.policy.interruptibility == AnimationPolicy.Interruptibility.MustFinish
                }
                if (anyMustFinish) {
                    SchedulingDecision.StartExclusive
                } else {
                    SchedulingDecision.ReplaceThenStart(running.map { it.handle })
                }
            }
        }
    }
}
