package com.mineralord.tcg.core.animationcompose

import androidx.compose.ui.geometry.Rect

/**
 * Primer [RenderNode] REAL del framework: una carta/elemento "en vuelo" entre dos
 * ranuras, dibujado en la capa [RenderLayer.Flight].
 *
 * Contiene SÓLO lo imprescindible para representar un movimiento:
 * - [origin] / [destination]: rectángulos ya resueltos por el ejecutor a partir del
 *   [CoordinateRegistry] (frontera lógico↔pantalla ya cruzada; el render no consulta
 *   coordenadas, sólo dibuja lo que este nodo describe).
 * - [progress]: avance lineal 0f→1f de la traslación; lo actualiza el ejecutor cada
 *   frame. El renderer deriva la posición interpolada — el nodo no calcula nada.
 *
 * Deliberadamente NO trae campos "por si acaso" (rotación, escala, imagen, curva…): el
 * modelo crecerá cuando exista una necesidad real. La capa es fija ([Flight]): un nodo
 * de movimiento no existe en ninguna otra.
 */
data class MoveRenderNode(
    override val id: RenderNodeId,
    val origin: Rect,
    val destination: Rect,
    val progress: Float,
) : RenderNode {
    override val layer: RenderLayer get() = RenderLayer.Flight
}
