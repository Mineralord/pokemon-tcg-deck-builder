package com.mineralord.tcg.core.animationcompose

import androidx.compose.ui.geometry.Rect

/**
 * 2º [RenderNode] real: una carta ROBADA en vuelo del mazo a la mano, dibujada en la
 * capa [RenderLayer.Flight].
 *
 * Contiene sólo lo imprescindible para el robo:
 * - [origin]/[destination]: rects ya resueltos por el ejecutor (mazo → hueco de mano).
 * - [arcHeightPx]: cuánto se eleva el punto de control del arco sobre la recta origen→
 *   destino (px). 0 = recta; positivo = la carta "despega" y cae al fan.
 * - [progress]: avance YA suavizado por el ejecutor (incluye overshoot). El renderer
 *   deriva posición (Bézier), escala y rotación de este único valor — el nodo no calcula.
 *
 * Se mantiene separado de [MoveRenderNode]: aunque comparten origin/destination/progress,
 * el robo tiene identidad visual propia (arco + escala + rotación) y su modelo no debe
 * contaminar el nodo de movimiento lineal. Cada animación crece con su propio nodo.
 */
data class DrawCardRenderNode(
    override val id: RenderNodeId,
    val origin: Rect,
    val destination: Rect,
    val arcHeightPx: Float,
    val progress: Float,
) : RenderNode {
    override val layer: RenderLayer get() = RenderLayer.Flight
}
