package com.mineralord.tcg.core.animationcompose

import androidx.compose.ui.geometry.Rect
import com.mineralord.tcg.core.animation.EvolveVisual

/**
 * [RenderNode] de una evolución in-situ, dibujado en la capa [RenderLayer.Flight].
 *
 * - [bounds]: rectángulo de la ranura (ya resuelto por el ejecutor vía [CoordinateRegistry]).
 * - [progress]: avance 0f→1f de la transformación; lo actualiza el ejecutor cada frame.
 * - [visual]: perillas de la variante (ascenso, giro, volteo, bombeo, destello, anillo, matiz).
 *
 * El nodo NO calcula curvas: sólo transporta el estado; `EvolveNodeRenderer` deriva la imagen.
 */
data class EvolveRenderNode(
    override val id: RenderNodeId,
    val bounds: Rect,
    val progress: Float,
    val visual: EvolveVisual,
) : RenderNode {
    override val layer: RenderLayer get() = RenderLayer.Flight
}
