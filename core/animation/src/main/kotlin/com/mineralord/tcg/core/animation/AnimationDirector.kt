package com.mineralord.tcg.core.animation

import kotlinx.coroutines.flow.StateFlow

/**
 * **API pública única del framework de animaciones** (patrón Facade).
 *
 * El resto de la aplicación (p. ej. `feature:battle`) sólo debe conocer este tipo. La
 * coordinación interna —[AnimationQueue], [AnimationScheduler], [AnimationRunner],
 * `AnimationPlayer`— queda oculta tras esta fachada. El uso previsto es tan simple
 * como:
 *
 * ```
 * animationDirector.submit(DrawCardAnimation(...))
 * ```
 *
 * Responsabilidades del Facade (por qué existe, no por herencia histórica):
 * - **Frontera pública**: el único símbolo que importan las features.
 * - **Punto de entrada**: [submit] entrega solicitudes al pipeline interno.
 * - **Estado observable**: [state] es la ÚNICA fuente de verdad que la UI observa;
 *   ningún componente interno publica [AnimationState], el Facade lo agrega.
 *
 * No conoce Compose, Android ni el engine.
 */
interface AnimationDirector {

    /** Estado observable del framework (para que la UI reaccione declarativamente). */
    val state: StateFlow<AnimationState>

    /** Entrega una solicitud de animación al pipeline. */
    fun submit(request: AnimationRequest)

    /** Entrega varias solicitudes preservando su orden. */
    fun submit(requests: List<AnimationRequest>)

    /** Cancela el trabajo en curso y vacía lo pendiente (p. ej. al salir de la partida). */
    fun clear()
}
