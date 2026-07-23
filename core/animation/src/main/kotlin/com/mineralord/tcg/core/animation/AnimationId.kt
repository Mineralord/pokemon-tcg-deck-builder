package com.mineralord.tcg.core.animation

/**
 * Identificador único y fuertemente tipado de una [AnimationDefinition] / instancia.
 *
 * ¿Por qué un value class y no un `String`?
 * - Evita el *stringly-typing*: el compilador impide pasar por error un id de carta
 *   o de jugador donde se espera un id de animación.
 * - Coste cero en runtime (`@JvmInline`): se compila al propio `String`.
 *
 * Kotlin puro; sin dependencias.
 */
@JvmInline
value class AnimationId(val value: String)
