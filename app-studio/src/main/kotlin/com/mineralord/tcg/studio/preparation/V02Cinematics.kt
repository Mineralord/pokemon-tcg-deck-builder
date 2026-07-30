package com.mineralord.tcg.studio.preparation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * **V0.2 — «El tablero toma bando» (Momento Firma).** El recinto revelado en V0.1 se convierte en CAMPO DE
 * JUEGO: nace la FRONTERA central, el campo se parte en dos TERRITORIOS enfrentados, cada bando reclama sus
 * puestos y la línea de batalla queda trazada. Arranca en `SCENE_REST` (blend imperceptible con V0.1).
 *
 * **Ciclo G6 (comparación):** DOS direcciones que cumplen EXACTAMENTE el mismo beat sheet (mismos tiempos,
 * ritmo, cámara, orden de beats, gramática y duración). **Lo único que cambia es la INTERPRETACIÓN visual**
 * de los canales semánticos `frontier`/`territory`, resuelta en el render:
 *   · **A · Ensamblaje por luz** — la frontera y las zonas se DEFINEN trazándose con luz cálida.
 *   · **B · Enfoque del terreno** — la división se revela por CONTRASTE/jerarquía y foco (sin trazo).
 * Por eso ambas comparten esta MISMA lista de beats; la diferencia vive en `EventOverlay` según el id.
 */

private val InOut = FastOutSlowInEasing
private val Out = LinearOutSlowInEasing

/** Base de reposo (idéntica a SCENE_REST) + intensidad de frontera/territorio por beat. */
private fun field(frontier: Float, territory: Float, bloom: Float = 0.05f, vignette: Float = 0.24f): SceneChannels =
    SCENE_REST.copy(frontier = frontier, territory = territory, bloom = bloom, vignette = vignette)

/** Beats compartidos por A y B (5 beats + handoff). Arrancan desde SCENE_REST (ver `start`). */
private val V02_BEATS: List<Beat> = listOf(
    // B1 · Nace la frontera — el centro toma bando. Foco: el eje central. (R1)
    Beat("Nace la frontera", 500f, Out, "resonancia", field(frontier = 0.5f, territory = 0f, vignette = 0.26f)),
    // B2 · Dos territorios — la frontera divide. Foco: el frente de división. (R1)
    Beat("Dos territorios", 460f, InOut, "aire", field(frontier = 0.7f, territory = 0.5f, vignette = 0.28f)),
    // B3 · Cada bando reclama sus puestos — el campo comunica reglas. Foco: la retícula como un todo. (R2, pico)
    Beat("Cada bando reclama sus puestos", 640f, Out, "ambiente", field(frontier = 0.85f, territory = 1f, bloom = 0.10f, vignette = 0.30f)),
    // B4 · La línea de batalla queda trazada — settle cargado. Foco: el tablero en reposo. (R2→settle)
    Beat("Línea de batalla trazada", 460f, Out, "resonancia", field(frontier = 0.72f, territory = 1f, vignette = 0.30f)),
    // B5 · Entrega — handoff a V0.3 (sin cierre). Foco: el punto de entrada a V0.3. (R1)
    Beat("Entrega", 380f, Out, null, field(frontier = 0.60f, territory = 1f, vignette = 0.28f)),
)

/** Las DOS direcciones de V0.2 (mismo beat sheet; distinta interpretación visual por render). */
val V02_VARIANTS: List<EventVariant> = listOf(
    EventVariant("V02A", "Ensamblaje por luz", V02_BEATS, start = SCENE_REST),
    EventVariant("V02B", "Enfoque del terreno", V02_BEATS, start = SCENE_REST),
)
