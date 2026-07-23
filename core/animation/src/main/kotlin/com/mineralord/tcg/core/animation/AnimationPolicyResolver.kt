package com.mineralord.tcg.core.animation

/**
 * Obtiene la [AnimationPolicy] que rige a una [AnimationRequest].
 *
 * La política vive en la [AnimationDefinition], no en la request; pero el
 * [AnimationScheduler] necesita conocerla ANTES de arrancar la animación para poder
 * decidir (ignorar, reemplazar, encolar…). Este resolutor cierra ese hueco sin
 * acoplar el scheduler a cómo se mapea una request a su definición.
 *
 * OJO: "resolver la política" (esto) es distinto de "arbitrar con la política"
 * ([PolicyArbiter]). Uno la obtiene; el otro decide qué hacer con ella.
 */
fun interface AnimationPolicyResolver {
    fun resolve(request: AnimationRequest): AnimationPolicy
}
