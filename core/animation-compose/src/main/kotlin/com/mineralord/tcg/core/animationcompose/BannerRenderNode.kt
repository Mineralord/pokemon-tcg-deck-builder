package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.BannerVisual

/**
 * [RenderNode] de un rótulo/banner de anuncio, dibujado en la capa [RenderLayer.Overlay] (al frente).
 *
 * - [kicker]/[title]: textos ya resueltos por la receta (p. ej. "ATAQUE" / "Rayo Trueno").
 * - [progress]: avance 0f→1f de la línea temporal (barrido-in → sostenido → barrido-out).
 * - [visual]: perillas de dirección/tinte/énfasis. El nodo NO calcula curvas; el renderer deriva todo.
 */
data class BannerRenderNode(
    override val id: RenderNodeId,
    val kicker: String,
    val title: String,
    val progress: Float,
    val visual: BannerVisual,
) : RenderNode {
    override val layer: RenderLayer get() = RenderLayer.Overlay
}
