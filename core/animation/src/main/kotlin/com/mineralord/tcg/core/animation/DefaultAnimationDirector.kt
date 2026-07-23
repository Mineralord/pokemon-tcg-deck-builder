package com.mineralord.tcg.core.animation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementación del Facade [AnimationDirector].
 *
 * Reúne tres responsabilidades reales (no es un pass-through):
 * 1. **Frontera pública**: expone `submit` + `state` y oculta todo el pipeline.
 * 2. **Composition root**: [create] cablea queue + arbiter + running + scheduler con
 *    defaults sensatos, para que ninguna feature conozca esas piezas.
 * 3. **Agregador de estado**: es el dueño del [MutableStateFlow]; lo alimenta el
 *    [StateReportingRunner] que inyecta al scheduler (sin modificar el scheduler).
 *
 * `submit`/`clear` delegan en el [AnimationScheduler]; el Facade no coordina por sí
 * mismo, sólo enruta y publica estado.
 */
class DefaultAnimationDirector private constructor(
    private val scheduler: AnimationScheduler,
    private val mutableState: MutableStateFlow<AnimationState>,
) : AnimationDirector {

    override val state: StateFlow<AnimationState> = mutableState.asStateFlow()

    override fun submit(request: AnimationRequest) = scheduler.schedule(request)

    override fun submit(requests: List<AnimationRequest>) = scheduler.schedule(requests)

    override fun clear() = scheduler.cancel()

    companion object {
        /**
         * Construye el framework completo y devuelve su única API pública.
         *
         * Sólo pide lo que es genuinamente específico de la app; el resto tiene
         * defaults internos que las features no necesitan conocer.
         *
         * @param runner cómo una [AnimationRequest] se convierte en reproducción
         *               (resuelve request→definition y usa el `AnimationPlayer`).
         * @param scope alcance de corrutinas del framework (ciclo de vida controlado
         *              por el llamador, sin acoplar a Android).
         * @param policyResolver política por solicitud (por defecto [AnimationPolicy.Default]).
         * @param queue implementación de cola (por defecto [DefaultAnimationQueue]).
         * @param arbiter reglas de arbitraje (por defecto [DefaultPolicyArbiter]).
         */
        fun create(
            runner: AnimationRunner,
            scope: CoroutineScope,
            policyResolver: AnimationPolicyResolver = AnimationPolicyResolver { AnimationPolicy.Default },
            queue: AnimationQueue = DefaultAnimationQueue(),
            arbiter: PolicyArbiter = DefaultPolicyArbiter(),
        ): AnimationDirector {
            val state = MutableStateFlow(AnimationState.Idle)
            val reportingRunner = StateReportingRunner(runner, scope, state)
            val scheduler = DefaultAnimationScheduler(
                queue = queue,
                runner = reportingRunner,
                policyResolver = policyResolver,
                arbiter = arbiter,
                running = RunningAnimations(),
                scope = scope,
            )
            return DefaultAnimationDirector(scheduler, state)
        }
    }
}
