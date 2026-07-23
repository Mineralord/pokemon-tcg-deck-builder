package com.mineralord.tcg.core.animation

import java.util.EnumMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Primera versión funcional de [AnimationQueue]: **buckets FIFO por prioridad**.
 *
 * Estructura: un `ArrayDeque` por cada [AnimationPriority]. Se elige frente a una
 * `java.util.PriorityQueue` (heap) porque:
 * - **Estabilidad gratis**: un heap no garantiza FIFO entre elementos de igual
 *   prioridad; un `ArrayDeque` preserva el orden de inserción por construcción.
 * - **Rendimiento**: enqueue O(1); dequeue/peek O(k) con k = nº de niveles de
 *   prioridad, una constante pequeña y fija (4) ⇒ O(1) efectivo. El heap sería
 *   O(log n) sin aportar ventaja con tan pocos niveles.
 *
 * Thread-safety: TODAS las operaciones se serializan con un único [lock]. Es
 * necesario un candado global (no por-bucket) porque [size], [peek] y [dequeue] deben
 * observar un estado consistente ENTRE buckets. Las secciones críticas son O(1), así
 * que es seguro invocarlas desde corrutinas sin `suspend`.
 *
 * Extensibilidad (arquitectura ABIERTA, sin implementar todavía): el almacén está
 * encapsulado tras esta clase; introducir expiración, cancelación por id, inspección,
 * métricas, depuración o persistencia se hará envolviendo [AnimationRequest] en una
 * entrada interna y/o añadiendo métodos, sin cambiar la semántica pública actual.
 *
 * Responsabilidad ÚNICA: almacenar y entregar. No aplica [AnimationPolicy], no
 * reproduce, no conoce al player.
 */
class DefaultAnimationQueue : AnimationQueue {

    private val lock = ReentrantLock()

    /** Un FIFO por prioridad. `EnumMap` = acceso denso y rápido por clave enum. */
    private val buckets: EnumMap<AnimationPriority, ArrayDeque<AnimationRequest>> =
        EnumMap<AnimationPriority, ArrayDeque<AnimationRequest>>(AnimationPriority::class.java).apply {
            AnimationPriority.entries.forEach { put(it, ArrayDeque()) }
        }

    /** Prioridades de mayor a menor: orden en que se inspeccionan los buckets. */
    private val byDescendingPriority: List<AnimationPriority> =
        AnimationPriority.entries.sortedByDescending { it.ordinal }

    /** Contador mantenido para que [size] sea O(1) sin recorrer buckets. */
    private var count = 0

    override val size: Int
        get() = lock.withLock { count }

    override val isEmpty: Boolean
        get() = lock.withLock { count == 0 }

    override fun enqueue(request: AnimationRequest) {
        lock.withLock {
            buckets.getValue(request.priority).addLast(request)
            count++
        }
    }

    override fun dequeue(): AnimationRequest? = lock.withLock {
        val bucket = firstNonEmptyBucket() ?: return@withLock null
        count--
        bucket.removeFirst()
    }

    override fun peek(): AnimationRequest? = lock.withLock {
        firstNonEmptyBucket()?.first()
    }

    override fun clear() = lock.withLock {
        buckets.values.forEach { it.clear() }
        count = 0
    }

    /** Primer bucket no vacío recorriendo de mayor a menor prioridad. */
    private fun firstNonEmptyBucket(): ArrayDeque<AnimationRequest>? {
        for (priority in byDescendingPriority) {
            val bucket = buckets.getValue(priority)
            if (bucket.isNotEmpty()) return bucket
        }
        return null
    }
}
