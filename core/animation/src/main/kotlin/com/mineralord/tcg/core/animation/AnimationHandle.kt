package com.mineralord.tcg.core.animation

/**
 * Referencia a una animación **en ejecución** (una instancia concreta de una
 * [AnimationDefinition]). Permite operar sobre ella una vez lanzada.
 *
 * Fase actual: SÓLO el contrato. Nada se reproduce; ninguna implementación real se
 * incluye todavía.
 *
 * Diseño (Interface Segregation): el handle base expone únicamente lo que TODA
 * instancia puede ofrecer —identidad, estado y cancelación—. La capacidad de
 * pausar/reanudar, que no toda animación soportará y que es cara de implementar bien,
 * se aísla en [PausableHandle]. Así nadie está obligado a fingir un `pause()` que no
 * puede cumplir.
 */
interface AnimationHandle {

    /** Id de la definición que originó esta instancia. */
    val id: AnimationId

    /** Estado actual de la instancia (consulta). */
    val state: RunningState

    /** Solicita cancelar la animación. Idempotente. */
    fun cancel()

    /**
     * Suspende hasta que la animación concluya y devuelve su desenlace. Es el punto
     * de "completion": permite a un scheduler futuro encadenar (esperar a que ésta
     * termine antes de servir la siguiente) sin sondear [state].
     */
    suspend fun await(): AnimationResult

    /** Estados de vida de una animación en ejecución. */
    enum class RunningState { Running, Paused, Finished }
}

/**
 * Capacidad OPCIONAL de pausa/reanudación, separada de [AnimationHandle] por ISP.
 * Sólo las animaciones que realmente puedan suspenderse implementarán esta interfaz.
 */
interface PausableHandle : AnimationHandle {

    /** Pausa la reproducción; puede reanudarse con [resume]. */
    fun pause()

    /** Reanuda una reproducción previamente pausada. */
    fun resume()
}
