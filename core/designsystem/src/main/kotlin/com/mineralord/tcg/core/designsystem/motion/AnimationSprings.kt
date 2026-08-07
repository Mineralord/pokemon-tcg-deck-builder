package com.mineralord.tcg.core.designsystem.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Resortes (spring) canónicos, genéricos en el tipo animado [T] para servir a Float, Dp, Offset,
 * Color, etc. ÚNICA fuente de verdad de la física: los componentes eligen un preset por NOMBRE
 * en vez de repetir parejas dampingRatio/stiffness.
 *
 * La física está afinada al lenguaje de "movimiento expresivo" (Material 3 Expressive / Pixel):
 * rigideces explícitas en vez de los tramos genéricos [androidx.compose.animation.core.Spring],
 * amortiguaciones ligeramente por debajo del crítico para un asentamiento vivo pero sin oscilar,
 * y un rebote controlado solo donde comunica "aterrizaje". Así toda la app comparte una misma
 * curva de reacción física, base de la coherencia visual.
 */
object AnimationSprings {

    // --- Amortiguaciones canónicas (relación de amortiguamiento) ---------------
    // Crítico = 1.0 (sin rebote). Valores algo menores dan un asentamiento "vivo".
    private const val DampingCritical = 1f
    private const val DampingTight = 0.92f
    private const val DampingSoft = 0.85f
    private const val DampingBounce = 0.68f
    private const val DampingElastic = 0.5f

    // --- Rigideces canónicas (fuerza de recuperación) --------------------------
    // Más alto = reacción más rápida y firme.
    private const val StiffnessCalm = 200f
    private const val StiffnessSpatial = 380f
    private const val StiffnessBrisk = 700f
    private const val StiffnessSnap = 1400f

    /** Muy suave, sin rebote — grandes desplazamientos y layouts (reflujo, reubicación). */
    fun <T> gentle(): SpringSpec<T> =
        spring(dampingRatio = DampingCritical, stiffness = StiffnessCalm)

    /** Equilibrado, asentamiento vivo — el default espacial para propiedades continuas. */
    fun <T> standard(): SpringSpec<T> =
        spring(dampingRatio = DampingTight, stiffness = StiffnessSpatial)

    /** Rápido y firme — feedback de pulsación e interacciones inmediatas (tacto). */
    fun <T> snappy(): SpringSpec<T> =
        spring(dampingRatio = DampingTight, stiffness = StiffnessSnap)

    /** Rebote medio — apariciones con carácter (cartas que aterrizan, badges). */
    fun <T> bouncy(): SpringSpec<T> =
        spring(dampingRatio = DampingBounce, stiffness = StiffnessBrisk)

    /** Rebote alto y elástico — énfasis lúdico puntual. */
    fun <T> wobbly(): SpringSpec<T> =
        spring(dampingRatio = DampingElastic, stiffness = StiffnessSpatial)

    /**
     * Resorte de EFECTOS (no espacial): alpha, color, sombra. Crítico y algo más rígido para
     * que las transiciones de "no-movimiento" resuelvan sin rebote perceptible.
     */
    fun <T> effect(): SpringSpec<T> =
        spring(dampingRatio = DampingCritical, stiffness = StiffnessBrisk)

    /** Suave con rebote muy leve — arrastre/selección donde se busca respuesta pero no salto. */
    fun <T> soft(): SpringSpec<T> =
        spring(dampingRatio = DampingSoft, stiffness = StiffnessSpatial)
}
