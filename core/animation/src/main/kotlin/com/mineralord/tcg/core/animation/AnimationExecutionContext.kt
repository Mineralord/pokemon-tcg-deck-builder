package com.mineralord.tcg.core.animation

import java.util.concurrent.ConcurrentHashMap

/**
 * Estado compartido durante **una** reproducción, accesible por todos los ejecutores
 * de pasos de esa animación.
 *
 * Fase actual: deliberadamente vacío de elementos visuales. Sólo prepara el mecanismo
 * para almacenar el estado que los ejecutores necesitarán compartir en el futuro
 * (p. ej. posiciones calculadas, handles de partículas, tiempos base). No conoce
 * Compose, Android ni el engine.
 *
 * Concurrencia: como [AnimationStep.Parallel] ejecuta hijos a la vez, varios
 * ejecutores pueden tocar el contexto simultáneamente; por eso [attributes] es
 * *thread-safe*. El almacén es tipado de forma abierta mediante [Key] para no acoplar
 * el contexto a tipos concretos todavía (Open-Closed).
 */
class AnimationExecutionContext(
    /** Id de la definición que se está reproduciendo (para logs/diagnóstico). */
    val animationId: AnimationId,
) {
    private val attributes = ConcurrentHashMap<Key<*>, Any>()

    /** Clave tipada para guardar/leer atributos sin casts inseguros. */
    class Key<T : Any>(val name: String)

    /** Guarda un valor asociado a una clave tipada. */
    fun <T : Any> put(key: Key<T>, value: T) {
        attributes[key] = value
    }

    /** Lee un valor por su clave tipada, o `null` si no existe. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: Key<T>): T? = attributes[key] as T?
}
