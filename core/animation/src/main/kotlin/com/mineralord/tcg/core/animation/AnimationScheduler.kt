package com.mineralord.tcg.core.animation

/**
 * Orquestador temporal del subsistema de animaciones.
 *
 * Posición en el pipeline:
 * `… → AnimationQueue → **AnimationScheduler** → AnimationDirector`.
 *
 * Responsabilidad:
 * - Ser el intermediario que decide *cuándo* una solicitud pasa de la
 *   [AnimationQueue] al [AnimationDirector]: drenar la cola y entregar la siguiente
 *   solicitud sólo cuando el director esté libre (serializar), o según la política
 *   futura (agrupar, saltar por prioridad…). Aísla esa política de temporización del
 *   director, que sólo sabe servir una solicitud.
 *
 * Fronteras (Clean Architecture):
 * - NO conoce el motor ni Compose.
 * - Depende de las abstracciones [AnimationQueue] y [AnimationDirector], no de
 *   implementaciones concretas (Dependency Inversion).
 *
 * En esta fase se declara SÓLO la estructura; NO hay bucle de drenado, timers ni
 * lógica de reproducción. Deliberadamente sin temporizadores (coherente con mantener
 * [AnimationState] sin progreso/duración/pausa).
 */
interface AnimationScheduler {

    /**
     * Ofrece una solicitud al pipeline. La implementación la encolará en la
     * [AnimationQueue] y disparará el drenado hacia el director cuando corresponda.
     * Contrato preparado; sin comportamiento todavía.
     */
    fun schedule(request: AnimationRequest)

    /** Ofrece varias solicitudes preservando su orden. */
    fun schedule(requests: List<AnimationRequest>)

    /** Cancela lo pendiente y detiene el drenado (p. ej. al salir de la partida). */
    fun cancel()
}
