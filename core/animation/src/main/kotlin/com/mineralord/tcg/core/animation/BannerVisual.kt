package com.mineralord.tcg.core.animation

/**
 * Parámetros visuales PUROS de un rótulo/banner de anuncio ([AnimationStep.Banner]). "Perillas"
 * numéricas sin tipos de Compose; el renderer las interpreta a lo largo del progreso 0→1.
 *
 * Diseñado a partir de referencias AAA (Marvel Snap reveal, Legends of Runeterra spell announce,
 * Hearthstone): la DIRECCIÓN codifica quién actúa y el tinte refuerza el tono (ataque/pasiva/manual).
 *
 * @param accentHue matiz principal de la placa/tinte en grados (0..360). Rojo≈0, dorado≈45, cian≈190.
 * @param secondaryHue matiz del degradado/sheen secundario en grados (0..360).
 * @param fromRight true = barre entrante desde la derecha (acción del RIVAL hacia mí);
 *                  false = barre saliente desde la izquierda (acción MÍA hacia el rival).
 * @param emphasis fuerza del overshoot/escala de asentado del título (0f = sobrio, 1f = dramático).
 * @param sheen intensidad del brillo que cruza la placa durante el sostenido (0f..1f).
 */
data class BannerVisual(
    val accentHue: Float = 0f,
    val secondaryHue: Float = 40f,
    val fromRight: Boolean = false,
    val emphasis: Float = 0.5f,
    val sheen: Float = 0.6f,
)
