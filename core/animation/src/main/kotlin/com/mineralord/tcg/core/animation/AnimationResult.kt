package com.mineralord.tcg.core.animation

/**
 * Resultado final de una animación una vez concluida (por la vía que sea).
 *
 * Modelado como `sealed interface`: la fase del reproductor podrá hacer `when`
 * exhaustivo sobre el desenlace sin ramas `else`. Sólo estructura; sin comportamiento.
 */
sealed interface AnimationResult {

    /** Terminó reproduciéndose por completo con normalidad. */
    data object Completed : AnimationResult

    /** Se canceló externamente (p. ej. el usuario salió de la partida). */
    data object Cancelled : AnimationResult

    /** Fue interrumpida por otra animación (política [AnimationPolicy.Conflict.Replace]). */
    data object Interrupted : AnimationResult

    /** Falló durante la reproducción; conserva la causa para diagnóstico. */
    data class Failed(val cause: Throwable) : AnimationResult
}
