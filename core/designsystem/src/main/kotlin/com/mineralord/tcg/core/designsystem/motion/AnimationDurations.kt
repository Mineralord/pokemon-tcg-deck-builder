package com.mineralord.tcg.core.designsystem.motion

/**
 * Duraciones canónicas (ms) del framework. ÚNICA fuente de verdad temporal. Los specs
 * ([AnimationSpecs]) y los componentes derivan SIEMPRE de estas constantes; nunca se escribe
 * un `tween(300)` suelto en el proyecto.
 */
object AnimationDurations {

    // --- Transiciones puntuales -----------------------------------------------
    const val Instant = 0
    const val Fast = 120
    const val Quick = 180
    const val Standard = 260
    const val Slow = 400
    const val Slower = 600
    const val Deliberate = 900

    // --- Bucles infinitos -----------------------------------------------------
    const val Pulse = 1100
    const val Float = 2600
    const val Shake = 420
    const val Shimmer = 1400

    /** Pulso rápido de resalte (aura de objetivo válido al arrastrar). */
    const val GlowPulse = 650
    /** Pulso "prendido" del arco de turno (más lento y sutil que el glow). */
    const val ArcPulse = Deliberate
    /** Giro continuo de la moneda mientras se lanza. */
    const val CoinSpin = 500

    /** Volteo de revelado de una carta (dorso → arte). */
    const val RevealFlip = 520
}
