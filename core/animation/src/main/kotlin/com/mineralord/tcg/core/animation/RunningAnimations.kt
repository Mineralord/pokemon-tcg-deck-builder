package com.mineralord.tcg.core.animation

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Una animación actualmente en ejecución, junto a la política bajo la que corre.
 *
 * El [PolicyArbiter] necesita la política de las animaciones activas (no sólo sus
 * handles) para decidir, p. ej., si una que corre puede cancelarse
 * ([AnimationPolicy.Interruptibility]).
 */
data class RunningAnimation(
    val handle: AnimationHandle,
    val policy: AnimationPolicy,
)

/**
 * Registro *thread-safe* de las animaciones activas. Responsabilidad ÚNICA: llevar la
 * cuenta de qué corre ahora. No decide nada (eso es del [PolicyArbiter]) ni reproduce
 * nada (eso es del `AnimationRunner`).
 *
 * Es *thread-safe* porque varias animaciones paralelas pueden entrar/salir a la vez.
 */
class RunningAnimations {

    private val active = CopyOnWriteArrayList<RunningAnimation>()

    val isEmpty: Boolean get() = active.isEmpty()

    fun add(animation: RunningAnimation) {
        active += animation
    }

    fun remove(handle: AnimationHandle) {
        active.removeAll { it.handle === handle }
    }

    /** Copia inmutable del estado actual, segura para iterar y decidir. */
    fun snapshot(): List<RunningAnimation> = active.toList()
}
