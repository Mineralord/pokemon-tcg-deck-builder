package com.mineralord.tcg.core.animation

/**
 * Estado observable del sistema de animaciones que la UI recolecta (vía StateFlow).
 *
 * Responsabilidad:
 * - Representar de forma **inmutable** qué está haciendo el director en este instante:
 *   si está en reposo, reproduciendo un evento, y cuántos quedan encolados.
 * - Ser la ÚNICA fuente de verdad que la capa Compose observa; la UI es una función
 *   pura de este estado (`render(state)`), sin conocer al director ni al engine.
 *
 * En esta fase NO contiene progreso, timings ni handles de reproducción: sólo la
 * estructura mínima. Se ampliará cuando se implemente la reproducción real.
 */
data class AnimationState(
    /** Solicitud que se está sirviendo ahora mismo, o `null` si el director está ocioso. */
    val current: AnimationRequest? = null,

    /** Número de solicitudes pendientes en la cola. Preparado para la futura [AnimationQueue]. */
    val pending: Int = 0,

    /** Fase de alto nivel del director; facilita a la UI decidir overlays/bloqueos. */
    val phase: Phase = Phase.Idle,
) {
    /** Fases de vida del director. Sin transiciones implementadas todavía. */
    enum class Phase { Idle, Playing }

    /** Azúcar para la UI: ¿hay alguna animación activa? */
    val isPlaying: Boolean get() = phase == Phase.Playing

    companion object {
        /** Estado inicial neutro: nada reproduciéndose, cola vacía. */
        val Idle: AnimationState = AnimationState()
    }
}
