package com.mineralord.tcg.core.animationcompose

import androidx.compose.ui.geometry.Rect
import com.mineralord.tcg.core.animation.GlowVisual

/**
 * [RenderNode] del aura de habilidad, dibujado en la capa [RenderLayer.Underglow] (bajo las cartas).
 *
 * - [bounds]: rectángulo de la ranura/carta (resuelto por el ejecutor vía [CoordinateRegistry]).
 * - [progress]: avance 0f→1f de la duración; el renderer deriva la respiración/shimmer.
 * - [visual]: perillas (tinte rojo pasiva / dorado manual, radio del bloom, rim, ciclos, shimmer).
 */
data class AbilityGlowRenderNode(
    override val id: RenderNodeId,
    val bounds: Rect,
    val progress: Float,
    val visual: GlowVisual,
) : RenderNode {
    override val layer: RenderLayer get() = RenderLayer.Underglow
}
