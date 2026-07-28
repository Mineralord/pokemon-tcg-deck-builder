package com.mineralord.tcg.studio.preparation

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * **V0.1 — Entrada al combate: modelo de beats DATA-DRIVEN + las 5 propuestas.**
 *
 * No hay una clase por propuesta ni por animación: una propuesta ([EventVariant]) es sólo una lista
 * de [Beat] (dato), y un beat es un conjunto de valores objetivo de **canales visuales**
 * ([SceneChannels]) con una duración y un easing. El Studio Player interpola los canales en el tiempo
 * y el renderer los aplica. Cambiar/añadir una propuesta = cambiar datos; nunca código de render.
 */

/** Valores de los canales visuales en un instante. Todo Float para poder interpolarse. */
data class SceneChannels(
    // Menú saliente
    val menuAlpha: Float = 1f,
    val menuBlur: Float = 0f,       // dp
    val menuScale: Float = 1f,
    // Velo (fundido / cruce a negro)
    val scrimAlpha: Float = 0f,
    // Arena entrante (el escenario del duelo)
    val arenaAlpha: Float = 0f,
    val arenaScale: Float = 1f,
    val arenaBlur: Float = 0f,       // dp (rack focus)
    val rootOffsetY: Float = 0f,     // dp (descenso / viaje de cámara)
    val rootRotation: Float = 0f,    // grados (tilt)
    // Luz
    val lightBand: Float = -0.5f,    // 0..1 posición del barrido; fuera de [0,1] = apagado
    val lightIntensity: Float = 0f,  // 0..1
    val bloom: Float = 0f,           // 0..1 destello central
    // Partículas
    val particles: Float = 0f,       // 0..1
)

/** Estado inicial "en el menú" del que parten todas las propuestas. */
val SceneStart = SceneChannels(menuAlpha = 1f, arenaAlpha = 0f)

/** Un beat: duración, curva y estado objetivo al que llega. [cue] = marcador de audio (sin sonido aún). */
class Beat(
    val name: String,
    val durationMs: Float,
    val easing: Easing,
    val cue: String?,
    val target: SceneChannels,
)

/** Una propuesta = secuencia de beats. Es el "dato" que el Studio Player reproduce. */
class EventVariant(
    val id: String,
    val name: String,
    val beats: List<Beat>,
) {
    val totalMs: Float = beats.fold(0f) { acc, b -> acc + b.durationMs }
}

/** Fotograma resuelto por el Player: canales + beat actual + cue de audio. */
data class Frame(val channels: SceneChannels, val beat: String, val cue: String?, val progress: Float)

/** Resuelve el fotograma en [elapsedMs] interpolando linealmente entre objetivos de beats con easing. */
fun EventVariant.frameAt(elapsedMs: Float): Frame {
    val t = elapsedMs.coerceIn(0f, totalMs)
    var start = 0f
    var from = SceneStart
    for ((index, beat) in beats.withIndex()) {
        val end = start + beat.durationMs
        val last = index == beats.lastIndex
        if (t <= end || last) {
            val local = if (beat.durationMs <= 0f) 1f else ((t - start) / beat.durationMs).coerceIn(0f, 1f)
            val e = beat.easing.transform(local)
            return Frame(lerp(from, beat.target, e), beat.name, beat.cue, t / totalMs)
        }
        start = end
        from = beat.target
    }
    return Frame(SceneStart, beats.first().name, null, 0f)
}

private fun l(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun lerp(a: SceneChannels, b: SceneChannels, t: Float): SceneChannels = SceneChannels(
    menuAlpha = l(a.menuAlpha, b.menuAlpha, t),
    menuBlur = l(a.menuBlur, b.menuBlur, t),
    menuScale = l(a.menuScale, b.menuScale, t),
    scrimAlpha = l(a.scrimAlpha, b.scrimAlpha, t),
    arenaAlpha = l(a.arenaAlpha, b.arenaAlpha, t),
    arenaScale = l(a.arenaScale, b.arenaScale, t),
    arenaBlur = l(a.arenaBlur, b.arenaBlur, t),
    rootOffsetY = l(a.rootOffsetY, b.rootOffsetY, t),
    rootRotation = l(a.rootRotation, b.rootRotation, t),
    lightBand = l(a.lightBand, b.lightBand, t),
    lightIntensity = l(a.lightIntensity, b.lightIntensity, t),
    bloom = l(a.bloom, b.bloom, t),
    particles = l(a.particles, b.particles, t),
)

// Atajos de easing (mapeo del lenguaje de dirección a curvas de Compose).
private val InOut = FastOutSlowInEasing
private val Out = LinearOutSlowInEasing
private val In = FastOutLinearInEasing
private val Lin = LinearEasing

/**
 * Las 5 propuestas de V0.1 (misma estructura de 5 beats; distinta dirección). Solo audio ambiental
 * (marcadores de cue). Duraciones/valores son un primer pase AAA para comparar; se iterarán.
 */
val V01_VARIANTS: List<EventVariant> = listOf(
    // P1 · Umbral de Luz — la luz revela; elegancia + continuidad.
    EventVariant("P1", "Umbral de Luz", listOf(
        Beat("Menú cede", 350f, In, "aire", SceneChannels(menuAlpha = 0.2f, menuBlur = 8f, menuScale = 0.98f)),
        Beat("Cruce (luz)", 450f, InOut, "whoosh", SceneChannels(menuAlpha = 0f, arenaAlpha = 0.4f, arenaScale = 1.06f, lightBand = 1.2f, lightIntensity = 0.55f)),
        Beat("Presentación", 600f, Out, "ambiente", SceneChannels(arenaAlpha = 1f, arenaScale = 1.0f, lightBand = 1.3f, lightIntensity = 0.2f, bloom = 0.45f)),
        Beat("Respiración", 250f, InOut, "resonancia", SceneChannels(arenaAlpha = 1f, bloom = 0.2f, lightIntensity = 0.08f)),
        Beat("Entrega", 200f, Out, null, SceneChannels(arenaAlpha = 1f, bloom = 0.1f)),
    )),
    // P2 · Descenso — grúa cenital que aterriza en la mesa; peso/gravedad.
    EventVariant("P2", "Descenso", listOf(
        Beat("Menú cede", 300f, In, "aire", SceneChannels(menuAlpha = 0f, menuScale = 1.1f)),
        Beat("Cruce (caída)", 450f, In, "aire de descenso", SceneChannels(scrimAlpha = 0.35f, arenaAlpha = 0.5f, arenaScale = 1.2f, rootOffsetY = -110f)),
        Beat("Descenso", 600f, Out, "descenso", SceneChannels(arenaAlpha = 1f, arenaScale = 1.04f, rootOffsetY = -14f, scrimAlpha = 0f)),
        Beat("Aterrizaje", 300f, Out, "impacto", SceneChannels(arenaAlpha = 1f, arenaScale = 1.0f, rootOffsetY = 0f, bloom = 0.15f)),
        Beat("Respiración", 200f, InOut, "resonancia", SceneChannels(arenaAlpha = 1f, bloom = 0.08f)),
    )),
    // P3 · Materialización — el tapete se ensambla desde energía; identidad Pokémon.
    EventVariant("P3", "Materialización", listOf(
        Beat("Disolución", 300f, In, "disolución", SceneChannels(menuAlpha = 0f, particles = 0.6f)),
        Beat("Ensamblaje", 500f, InOut, "materialización", SceneChannels(arenaAlpha = 0.7f, arenaScale = 0.98f, particles = 1f, bloom = 0.4f)),
        Beat("Presentación", 600f, Out, "ambiente", SceneChannels(arenaAlpha = 1f, arenaScale = 1.0f, particles = 0.4f, bloom = 0.3f, lightIntensity = 0.25f, lightBand = 0.5f)),
        Beat("La energía calma", 300f, InOut, "resonancia", SceneChannels(arenaAlpha = 1f, particles = 0.12f, bloom = 0.18f)),
        Beat("Entrega", 200f, Out, null, SceneChannels(arenaAlpha = 1f, particles = 0f, bloom = 0.1f)),
    )),
    // P4 · Enfoque — rack-focus contenido; máxima elegancia/accesibilidad.
    EventVariant("P4", "Enfoque", listOf(
        Beat("Desenfoque", 300f, In, "aire", SceneChannels(menuAlpha = 0.3f, menuBlur = 14f, scrimAlpha = 0.3f)),
        Beat("Cruce breve", 300f, InOut, "whoosh suave", SceneChannels(menuAlpha = 0f, scrimAlpha = 0.2f, arenaAlpha = 0.4f, arenaBlur = 20f)),
        Beat("Entra en foco", 500f, Out, "ambiente", SceneChannels(arenaAlpha = 1f, arenaBlur = 0f, scrimAlpha = 0f, bloom = 0.4f)),
        Beat("Respiración", 250f, InOut, "resonancia", SceneChannels(arenaAlpha = 1f, bloom = 0.15f)),
        Beat("Entrega", 150f, Out, null, SceneChannels(arenaAlpha = 1f, bloom = 0.08f)),
    )),
    // P5 · Cruce Diegético — travelling continuo menú→arena; inmersión.
    EventVariant("P5", "Cruce Diegético", listOf(
        Beat("Parte el viaje", 300f, In, "whoosh largo", SceneChannels(menuAlpha = 0.5f, menuScale = 1.25f, arenaAlpha = 0.3f, arenaScale = 1.4f, rootOffsetY = 45f, rootRotation = 2f)),
        Beat("Viaje", 500f, Lin, "viaje", SceneChannels(menuAlpha = 0f, arenaAlpha = 0.75f, arenaScale = 1.12f, rootOffsetY = 16f, rootRotation = 1f)),
        Beat("Llegada", 600f, Out, "llegada", SceneChannels(arenaAlpha = 1f, arenaScale = 1.03f, rootOffsetY = 2f, rootRotation = 0f, bloom = 0.2f)),
        Beat("Frena", 300f, Out, "resonancia", SceneChannels(arenaAlpha = 1f, arenaScale = 1.0f, rootOffsetY = 0f, bloom = 0.1f)),
        Beat("Entrega", 300f, InOut, null, SceneChannels(arenaAlpha = 1f, bloom = 0.06f)),
    )),
)
