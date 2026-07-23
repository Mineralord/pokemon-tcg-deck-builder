package com.mineralord.tcg.core.animation

/**
 * Abstracción que arranca la reproducción de una [AnimationRequest] y devuelve su
 * [AnimationHandle].
 *
 * ¿Por qué existe? El [AnimationScheduler] NO debe conocer al `AnimationPlayer`
 * concreto ni cómo una `AnimationRequest` se resuelve a una `AnimationDefinition`.
 * Esta interfaz encapsula ambas cosas tras un único punto (Dependency Inversion): el
 * scheduler sólo dice "arranca esto" y recibe un handle para coordinar.
 *
 * La implementación real (fase posterior) resolverá request→definition y llamará a
 * `AnimationPlayer.play(...)`. Aquí sólo se declara el contrato.
 */
fun interface AnimationRunner {
    fun start(request: AnimationRequest): AnimationHandle
}
