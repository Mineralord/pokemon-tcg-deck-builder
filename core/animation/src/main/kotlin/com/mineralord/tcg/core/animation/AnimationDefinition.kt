package com.mineralord.tcg.core.animation

import kotlin.time.Duration

/**
 * Describe una animación **completa** del juego, como DATOS inmutables.
 *
 * Es la "receta": qué pasos la componen y cómo se comporta respecto a otras. NO se
 * reproduce a sí misma; el futuro reproductor la interpretará. No conoce Compose,
 * Android ni el engine.
 *
 * Relación con el resto del modelo:
 * - [steps] es la raíz de un árbol de [AnimationStep] (Composite): permite describir
 *   desde algo trivial hasta secuencias complejas sin ampliar esta clase.
 * - [policy] gobierna su interacción con otras animaciones (cola/scheduler).
 *
 * Inmutabilidad: `data class` con colecciones de sólo lectura; ningún campo mutable.
 */
data class AnimationDefinition(
    /** Identificador único y tipado. */
    val id: AnimationId,

    /** Nombre legible (depuración, logs, editor futuro). */
    val name: String,

    /**
     * Duración estimada, OPCIONAL y sólo como *hint* (presupuestos, tests, timeouts).
     * No es la verdad: la duración real se derivará del árbol de [steps] en la fase
     * del reproductor. Se deja anulable a propósito para no arriesgar desincronización
     * entre un valor fijo y los pasos reales.
     */
    val estimatedDuration: Duration? = null,

    /** Prioridad relativa (reutiliza el eje ya definido en el módulo). */
    val priority: AnimationPriority = AnimationPriority.Normal,

    /** Política de reproducción frente a otras animaciones. */
    val policy: AnimationPolicy = AnimationPolicy.Default,

    /** Árbol de pasos. Vacío por ahora (aún no se definen animaciones reales). */
    val steps: List<AnimationStep> = emptyList(),
)
