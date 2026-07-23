package com.mineralord.tcg.core.animation

/**
 * Contrato de la cola de solicitudes de animación.
 *
 * Posición en el pipeline:
 * `… → AnimationRequest → **AnimationQueue** → AnimationScheduler → AnimationDirector`.
 *
 * Responsabilidad ÚNICA: **almacenar y entregar** [AnimationRequest]. La cola NO
 * decide prioridades de negocio, NO reproduce, NO aplica [AnimationPolicy] ni conoce
 * al player: sólo ordena la entrega respetando prioridad y estabilidad (FIFO dentro
 * de cada prioridad). Cambiar la estrategia = otra implementación (Strategy + OCP).
 *
 * API primaria: [enqueue] / [dequeue] / [peek]. Los métodos [offer] / [poll] son
 * ALIAS por defecto que delegan en ellos, para no obligar a modificar consumidores
 * existentes (p. ej. el [AnimationScheduler]).
 */
interface AnimationQueue {

    /** ¿La cola no tiene solicitudes pendientes? */
    val isEmpty: Boolean

    /** Número de solicitudes pendientes. */
    val size: Int

    /** Añade una solicitud, respetando prioridad y orden de llegada. */
    fun enqueue(request: AnimationRequest)

    /** Extrae la siguiente solicitud a servir, o `null` si está vacía. */
    fun dequeue(): AnimationRequest?

    /** Consulta la siguiente solicitud sin extraerla, o `null` si está vacía. */
    fun peek(): AnimationRequest?

    /** Descarta todas las solicitudes pendientes. */
    fun clear()

    // --- Alias de compatibilidad (no modificar consumidores existentes) ---

    /** Alias de [enqueue]. */
    fun offer(request: AnimationRequest) = enqueue(request)

    /** Alias de [dequeue]. */
    fun poll(): AnimationRequest? = dequeue()
}
