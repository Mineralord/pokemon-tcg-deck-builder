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
    // Viñeta cinematográfica: oscurece los bordes → profundidad y encuadre de "lugar" (LoR/cine).
    val vignette: Float = 0f,        // 0..1
    // V0.2 · «El tablero toma bando»: intensidad de la FRONTERA central (división) y definición de los
    // dos TERRITORIOS. Semánticos: cada dirección artística (A/B) los interpreta visualmente distinto.
    val frontier: Float = 0f,        // 0..1
    val territory: Float = 0f,       // 0..1
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

/**
 * Una propuesta = secuencia de beats. Es el "dato" que el Studio Player reproduce. [start] = estado del
 * que parte el beat 0 (por defecto `SceneStart`; V0.2 arranca en `SCENE_REST` para blend con V0.1).
 */
class EventVariant(
    val id: String,
    val name: String,
    val beats: List<Beat>,
    val start: SceneChannels = SceneStart,
) {
    val totalMs: Float = beats.fold(0f) { acc, b -> acc + b.durationMs }
}

/** Fotograma resuelto por el Player: canales + beat actual + cue de audio. */
data class Frame(val channels: SceneChannels, val beat: String, val cue: String?, val progress: Float)

/** Resuelve el fotograma en [elapsedMs] interpolando linealmente entre objetivos de beats con easing. */
fun EventVariant.frameAt(elapsedMs: Float): Frame {
    val t = elapsedMs.coerceIn(0f, totalMs)
    var acc = 0f
    var from = start
    for ((index, beat) in beats.withIndex()) {
        val end = acc + beat.durationMs
        val last = index == beats.lastIndex
        if (t <= end || last) {
            val local = if (beat.durationMs <= 0f) 1f else ((t - acc) / beat.durationMs).coerceIn(0f, 1f)
            val e = beat.easing.transform(local)
            return Frame(lerp(from, beat.target, e), beat.name, beat.cue, t / totalMs)
        }
        acc = end
        from = beat.target
    }
    return Frame(start, beats.first().name, null, 0f)
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
    vignette = l(a.vignette, b.vignette, t),
    frontier = l(a.frontier, b.frontier, t),
    territory = l(a.territory, b.territory, t),
)

// Atajos de easing (mapeo del lenguaje de dirección a curvas de Compose).
private val InOut = FastOutSlowInEasing
private val Out = LinearOutSlowInEasing
private val In = FastOutLinearInEasing
private val Lin = LinearEasing

/**
 * **Reposo de escena (contrato del BLEND V0.1 → V0.2).** Estado en reposo del escenario oficial sobre el
 * que arranca la ceremonia de inicio. La cinemática de V0.1 termina EXACTAMENTE aquí para que el paso a
 * la partida no tenga costura. Cualquier ajuste del "look" en reposo se cambia en un solo sitio.
 */
val SCENE_REST: SceneChannels = SceneChannels(
    arenaAlpha = 1f, arenaScale = 1f, arenaBlur = 0f,
    bloom = 0.05f, lightIntensity = 0f, vignette = 0.24f,
)

/**
 * **V0.1 — CINEMÁTICA CANÓNICA ÚNICA (AAA).**
 *
 * Una sola secuencia continua, dirigida como si fuera de un único autor (nueva filosofía: no hay 5
 * variantes activas). Reproduce la dirección aprobada en la investigación AAA para la APERTURA del arco:
 * escenario VIVO con profundidad (LoR) + ritmo tenso sin aire muerto (Marvel Snap) + firma de luz
 * «Umbral de Luz», sobre el TAPETE OFICIAL vacío. Arco de esta pieza: anticipación → asombro (el terreno
 * es real) → settle cargado → HANDOFF limpio hacia V0.2. Solo audio ambiental (marcadores de cue).
 *
 * Los beats son internos: el jugador percibe UNA experiencia continua. Duraciones ajustadas al conjunto.
 */
val V01_CINEMATIC: EventVariant = EventVariant("V01", "Entrada al combate", listOf(
    // B1 · ANTICIPACIÓN — el menú cede rápido y con propósito; el terreno existe pero aún SIN FOCO
    // (rack-focus + viñeta cerrada); una semilla de luz late en el centro de la lente. Foco: la semilla.
    Beat("Anticipación", 460f, In, "aire", SceneChannels(
        menuAlpha = 0f, menuBlur = 14f, menuScale = 0.96f,
        scrimAlpha = 0.5f, arenaAlpha = 0.14f, arenaBlur = 26f, arenaScale = 1.10f,
        bloom = 0.10f, vignette = 0.55f,
    )),
    // B2 · UMBRAL DE LUZ — la luz ENCIENDE y barre; el escenario ENTRA EN FOCO (rack-focus resolviendo)
    // revelando el "lugar". Foco: el frente de luz que descubre la arena.
    Beat("Umbral de luz", 820f, InOut, "whoosh", SceneChannels(
        menuAlpha = 0f, scrimAlpha = 0.12f, arenaAlpha = 0.82f, arenaBlur = 6f, arenaScale = 1.05f,
        lightBand = 1.15f, lightIntensity = 0.85f, bloom = 0.34f, vignette = 0.42f,
    )),
    // B3 · ASOMBRO — push-in que DESACELERA hasta el encuadre de juego; el recinto ya es un LUGAR con
    // profundidad; floración cálida que se abre. Foco: la arena completa como espacio real.
    Beat("Asombro", 980f, Out, "ambiente", SceneChannels(
        arenaAlpha = 1f, arenaBlur = 0f, arenaScale = 1.0f,
        lightBand = 1.4f, lightIntensity = 0.22f, bloom = 0.5f, vignette = 0.34f,
    )),
    // B4 · RESPIRACIÓN — settle cargado, breve (sin aire muerto); la luz se calma. Foco: el terreno en
    // reposo, listo.
    Beat("Respiración", 460f, Out, "resonancia", SceneChannels(
        arenaAlpha = 1f, bloom = 0.18f, lightIntensity = 0.08f, vignette = 0.28f,
    )),
    // B5 · BLEND (entrega → inicio de partida) — NO es un cierre cinematográfico: el arco ATERRIZA en el
    // REPOSO DE JUEGO exacto sobre el que aparecerá la ceremonia (volado). El último fotograma de V0.1 ==
    // el fotograma en reposo de la escena de V0.2 ⇒ transición IMPERCEPTIBLE. Foco: la partida que empieza.
    // Contrato de continuidad (SCENE_REST): arena plena · sin luz · bloom ambiental mínimo · viñeta de juego.
    Beat("Inicio de partida", 420f, Out, null, SCENE_REST),
))

/** Catálogo ACTIVO de V0.1: UNA sola experiencia canónica (nueva filosofía del proyecto). */
val V01_VARIANTS: List<EventVariant> = listOf(V01_CINEMATIC)

/**
 * **LEGACY** — las 4 direcciones exploratorias iniciales (Descenso · Materialización · Enfoque · Cruce
 * Diegético). **No forman parte del flujo activo** del evento; se conservan SOLO como referencia interna.
 * No se reproducen (no están en `PREP_EVENTS`). Se mantienen para consulta/comparación histórica.
 */
@Suppress("unused")
val V01_LEGACY_VARIANTS: List<EventVariant> = listOf(
    EventVariant("P2", "Descenso", listOf(
        Beat("Menú cede", 300f, In, "aire", SceneChannels(menuAlpha = 0f, menuScale = 1.1f)),
        Beat("Cruce (caída)", 450f, In, "aire de descenso", SceneChannels(scrimAlpha = 0.35f, arenaAlpha = 0.5f, arenaScale = 1.2f, rootOffsetY = -110f)),
        Beat("Descenso", 600f, Out, "descenso", SceneChannels(arenaAlpha = 1f, arenaScale = 1.04f, rootOffsetY = -14f, scrimAlpha = 0f)),
        Beat("Aterrizaje", 300f, Out, "impacto", SceneChannels(arenaAlpha = 1f, arenaScale = 1.0f, rootOffsetY = 0f, bloom = 0.15f)),
        Beat("Respiración", 200f, InOut, "resonancia", SceneChannels(arenaAlpha = 1f, bloom = 0.08f)),
    )),
    EventVariant("P3", "Materialización", listOf(
        Beat("Disolución", 300f, In, "disolución", SceneChannels(menuAlpha = 0f, particles = 0.6f)),
        Beat("Ensamblaje", 500f, InOut, "materialización", SceneChannels(arenaAlpha = 0.7f, arenaScale = 0.98f, particles = 1f, bloom = 0.4f)),
        Beat("Presentación", 600f, Out, "ambiente", SceneChannels(arenaAlpha = 1f, arenaScale = 1.0f, particles = 0.4f, bloom = 0.3f, lightIntensity = 0.25f, lightBand = 0.5f)),
        Beat("La energía calma", 300f, InOut, "resonancia", SceneChannels(arenaAlpha = 1f, particles = 0.12f, bloom = 0.18f)),
        Beat("Entrega", 200f, Out, null, SceneChannels(arenaAlpha = 1f, particles = 0f, bloom = 0.1f)),
    )),
    EventVariant("P4", "Enfoque", listOf(
        Beat("Desenfoque", 300f, In, "aire", SceneChannels(menuAlpha = 0.3f, menuBlur = 14f, scrimAlpha = 0.3f)),
        Beat("Cruce breve", 300f, InOut, "whoosh suave", SceneChannels(menuAlpha = 0f, scrimAlpha = 0.2f, arenaAlpha = 0.4f, arenaBlur = 20f)),
        Beat("Entra en foco", 500f, Out, "ambiente", SceneChannels(arenaAlpha = 1f, arenaBlur = 0f, scrimAlpha = 0f, bloom = 0.4f)),
        Beat("Respiración", 250f, InOut, "resonancia", SceneChannels(arenaAlpha = 1f, bloom = 0.15f)),
        Beat("Entrega", 150f, Out, null, SceneChannels(arenaAlpha = 1f, bloom = 0.08f)),
    )),
    EventVariant("P5", "Cruce Diegético", listOf(
        Beat("Parte el viaje", 300f, In, "whoosh largo", SceneChannels(menuAlpha = 0.5f, menuScale = 1.25f, arenaAlpha = 0.3f, arenaScale = 1.4f, rootOffsetY = 45f, rootRotation = 2f)),
        Beat("Viaje", 500f, Lin, "viaje", SceneChannels(menuAlpha = 0f, arenaAlpha = 0.75f, arenaScale = 1.12f, rootOffsetY = 16f, rootRotation = 1f)),
        Beat("Llegada", 600f, Out, "llegada", SceneChannels(arenaAlpha = 1f, arenaScale = 1.03f, rootOffsetY = 2f, rootRotation = 0f, bloom = 0.2f)),
        Beat("Frena", 300f, Out, "resonancia", SceneChannels(arenaAlpha = 1f, arenaScale = 1.0f, rootOffsetY = 0f, bloom = 0.1f)),
        Beat("Entrega", 300f, InOut, null, SceneChannels(arenaAlpha = 1f, bloom = 0.06f)),
    )),
)
