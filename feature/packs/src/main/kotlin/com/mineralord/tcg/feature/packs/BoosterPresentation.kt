package com.mineralord.tcg.feature.packs

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.mineralord.tcg.engine.model.Rarity

/**
 * ============================================================================
 * CAPA DE PRESENTACIÓN de la apertura de sobres (AAA).
 * ============================================================================
 *
 * Regla fundamental: la PRESENTACIÓN sólo REPRESENTA un resultado ya determinado
 * ([RevealedCard] ya resueltas por el motor/gacha). Nada de esto decide cartas,
 * cantidades, RNG ni colección.
 *
 *   PackOpeningResult (RevealedCard[])  →  BoosterOpeningPresentation (beats)  →  Animación
 *
 * Aquí viven las abstracciones DATA-DRIVEN de la presentación: los BEATS de la
 * secuencia, los HOOKS de audio/háptica (puntos de integración, hoy mapeados a la
 * síntesis procedural existente) y la [RarityPresentation] que escala la
 * espectacularidad según la rareza REAL de la carta.
 */

/** Beats de la experiencia. La secuencia NO es una animación monolítica: avanza por beats. */
enum class Beat {
    PACK_OPEN,       // el sobre ya rasgado se abre: interior + luz + profundidad
    ANTICIPATION,    // "¿qué habrá dentro?" — respiro previo a la primera carta
    EMERGENCE,       // la carta (boca abajo) emerge físicamente del interior
    CARD_BACK,       // dorso presentado y estabilizado (herramienta de anticipación)
    REVEAL,          // giro dorso→frente con anticipation/action/impact/settle
    RARITY_RESPONSE, // respuesta audiovisual escalada por rareza
    CONFIRM,         // confirmación/recompensa (contador, "NUEVA" → colección)
    SUMMARY,         // resumen final (recompensa)
}

/**
 * Puntos de integración de AUDIO (hooks). Existen como contrato estable aunque hoy se resuelvan
 * con SFX sintetizados; mañana pueden apuntar a assets reales sin tocar la secuencia.
 */
enum class SfxCue {
    PACK_HOVER, PACK_GRAB, PACK_TENSION, PACK_TEAR, PACK_OPEN,
    CARD_EMERGE, CARD_FLIP, CARD_REVEAL,
    RARITY_COMMON, RARITY_RARE, RARITY_HIGH, REWARD_CONFIRM,
}

/** Puntos de integración HÁPTICA (hooks). Siempre pulsos cortos, nunca vibración continua. */
enum class HapticCue { CONTACT, TENSION, TEAR, EMERGE, REVEAL, RARITY }

/** Mapa cue→SFX sintetizado. `null` = silencio intencional (el contraste da valor). */
private fun SfxCue.toSfx(): Sfx? = when (this) {
    SfxCue.PACK_HOVER -> Sfx.GRAB
    SfxCue.PACK_GRAB -> Sfx.GRAB
    SfxCue.PACK_TENSION -> null           // tensión = silencio relativo (sube por háptica)
    SfxCue.PACK_TEAR -> Sfx.TEAR
    SfxCue.PACK_OPEN -> Sfx.OPEN
    SfxCue.CARD_EMERGE -> Sfx.WHOOSH
    SfxCue.CARD_FLIP -> Sfx.WHOOSH
    SfxCue.CARD_REVEAL -> Sfx.SPARKLE
    SfxCue.RARITY_COMMON -> null          // común = limpio, sin fanfarria
    SfxCue.RARITY_RARE -> Sfx.RARE
    SfxCue.RARITY_HIGH -> Sfx.HIGH
    SfxCue.REWARD_CONFIRM -> Sfx.IMPACT
}

/**
 * Fachada de feedback (audio + háptica) para la secuencia. Centraliza los hooks: la animación pide
 * cues semánticos ([SfxCue]/[HapticCue]) sin conocer la síntesis ni el hardware.
 */
class BoosterFeedback(
    private val audio: PackAudio,
    private val vibrator: Vibrator?,
) {
    fun sfx(cue: SfxCue) { cue.toSfx()?.let { audio.play(it) } }

    fun haptic(cue: HapticCue) {
        val ms = when (cue) {
            HapticCue.CONTACT -> 12L
            HapticCue.TENSION -> 8L
            HapticCue.TEAR -> 28L
            HapticCue.EMERGE -> 14L
            HapticCue.REVEAL -> 22L
            HapticCue.RARITY -> 55L
        }
        vibrate(ms)
    }

    /** Tic de tensión: micro-pulso mientras se tira del plástico (escala con [strength] 0..1). */
    fun tensionTick(strength: Float) = vibrate((6 + 14 * strength).toLong())

    private fun vibrate(ms: Long) {
        val v = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v.vibrate(ms)
            }
        }
    }
}

/** Escalón de espectacularidad. La jerarquía es fundamental: si todo brilla, nada brilla. */
enum class RarityTier { CLEAN, IMPACT, SPECTACLE, LEGENDARY }

/**
 * Definición DATA-DRIVEN de cómo se PRESENTA una rareza. Reutilizable para cualquier expansión
 * (presente, histórica o futura) y evolucionable: si aparecen rarezas nuevas, se añaden aquí sin
 * reescribir la secuencia. Controla duración, cámara, luz, partículas, sonido, háptica y movimiento.
 */
data class RarityPresentation(
    val tier: RarityTier,
    /** Vueltas del giro 3D del reveal (0.5 = medio giro simple; >1 = giros dramáticos). */
    val revealTurns: Float,
    val anticipationMs: Int,
    val revealMs: Int,
    val settleMs: Int,
    val glow: Float,             // 0..1 intensidad del halo tras la carta
    val confetti: Int,           // nº de serpentinas (0 = ninguna)
    val sparkles: Int,           // nº de chispas del estallido
    val cameraPunch: Float,      // 0..1 empuje/acercamiento de cámara al asentar
    val lightSweep: Boolean,     // barrido de luz al cruzar el canto en el giro
    val iridescentBg: Boolean,   // fondo claro iridiscente (revelados premium)
    val sfxReveal: SfxCue,       // cue de sonido del reveal
    val strongHaptic: Boolean,   // pulso háptico marcado en el impacto
)

/**
 * Mapea la rareza REAL del proyecto a su presentación. Fuente única de verdad de la JERARQUÍA:
 *  - CLEAN     → común/infrecuente: rápido, limpio, elegante, sin fanfarria.
 *  - IMPACT    → rara/holo: sube luz, sonido y un punto de cámara.
 *  - SPECTACLE → doble rara y superiores: giro 3D, confeti, fondo iridiscente, cámara.
 *  - LEGENDARY → ilustración especial/hiper: EVENTO — flash, carillón, pausa, impacto final.
 */
fun presentationFor(r: Rarity): RarityPresentation = when (r) {
    Rarity.COMMON, Rarity.UNCOMMON -> RarityPresentation(
        tier = RarityTier.CLEAN, revealTurns = 0.5f,
        anticipationMs = 120, revealMs = 430, settleMs = 180,
        glow = 0.28f, confetti = 0, sparkles = 8, cameraPunch = 0.05f,
        lightSweep = false, iridescentBg = false,
        sfxReveal = SfxCue.RARITY_COMMON, strongHaptic = false,
    )
    Rarity.RARE, Rarity.RARE_HOLO, Rarity.PROMO -> RarityPresentation(
        tier = RarityTier.IMPACT, revealTurns = 0.5f,
        anticipationMs = 260, revealMs = 560, settleMs = 260,
        glow = 0.55f, confetti = 0, sparkles = 16, cameraPunch = 0.12f,
        lightSweep = true, iridescentBg = false,
        sfxReveal = SfxCue.RARITY_RARE, strongHaptic = false,
    )
    Rarity.DOUBLE_RARE, Rarity.ULTRA_RARE, Rarity.ILLUSTRATION_RARE -> RarityPresentation(
        tier = RarityTier.SPECTACLE, revealTurns = 1.5f,
        anticipationMs = 460, revealMs = 1150, settleMs = 420,
        glow = 0.85f, confetti = 42, sparkles = 26, cameraPunch = 0.20f,
        lightSweep = true, iridescentBg = true,
        sfxReveal = SfxCue.RARITY_RARE, strongHaptic = true,
    )
    Rarity.SPECIAL_ILLUSTRATION_RARE, Rarity.HYPER_RARE -> RarityPresentation(
        tier = RarityTier.LEGENDARY, revealTurns = 2.5f,
        anticipationMs = 700, revealMs = 1500, settleMs = 620,
        glow = 1f, confetti = 64, sparkles = 34, cameraPunch = 0.28f,
        lightSweep = true, iridescentBg = true,
        sfxReveal = SfxCue.RARITY_HIGH, strongHaptic = true,
    )
}
