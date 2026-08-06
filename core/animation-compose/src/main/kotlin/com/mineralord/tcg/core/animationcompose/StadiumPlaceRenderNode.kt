package com.mineralord.tcg.core.animationcompose

import androidx.compose.ui.geometry.Rect

/**
 * Nodo de render de la COLOCACIÓN de un Estadio: la carta en vuelo de la mano al slot de Estadio,
 * dibujada en la capa [RenderLayer.Flight]. Comparte forma con [DrawCardRenderNode] (origin/
 * destination/arco/progress) pero tiene identidad propia: aterriza como un objeto PESADO (menos
 * escala inicial, overshoot mayor y un destello radial al tocar), por eso su renderer es distinto.
 *
 * - [origin]/[destination]: rects ya resueltos por el ejecutor (mano → slot de Estadio).
 * - [arcHeightPx]: elevación del punto de control del arco sobre la recta origen→destino (px).
 * - [progress]: avance YA suavizado por el ejecutor (incluye el overshoot del asentamiento). El
 *   renderer deriva posición (Bézier), escala, inclinación y el flash de aterrizaje de este valor.
 */
data class StadiumPlaceRenderNode(
    override val id: RenderNodeId,
    val origin: Rect,
    val destination: Rect,
    val arcHeightPx: Float,
    val progress: Float,
) : RenderNode {
    override val layer: RenderLayer get() = RenderLayer.Flight
}
