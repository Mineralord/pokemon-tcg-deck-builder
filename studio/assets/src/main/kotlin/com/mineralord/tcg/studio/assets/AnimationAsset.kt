package com.mineralord.tcg.studio.assets

import com.mineralord.tcg.core.animation.AnimationRequest

/**
 * **Asset de tipo Animación: el primer (y por ahora único) tipo del Asset Registry.**
 *
 * Añade a los metadatos comunes de [Asset] la *referencia al recurso real*: la [request] de dominio
 * que dispara la animación en el pipeline compartido (`director.submit(request)`). Así el Studio y el
 * juego reproducen exactamente la MISMA animación por el MISMO motor; no hay datos ni lógica
 * duplicados. El registro guarda la request; quien reproduce (Gallery/Preview/juego) sólo la envía.
 *
 * Es el patrón que seguirán los tipos futuros: cada uno añade su propia referencia al recurso real
 * (p. ej. un audio, un shader) sin tocar el modelo base.
 */
data class AnimationAsset(
    override val id: String,
    override val name: String,
    override val category: String,
    override val status: AssetStatus,
    /** Referencia al recurso real: la request que reproduce esta animación en el pipeline. */
    val request: AnimationRequest,
    override val version: Int = 1,
    override val author: String = "",
    override val createdAt: String = "",
    override val inspiration: String = "",
    override val description: String = "",
    override val tags: List<String> = emptyList(),
    override val durationMillis: Long? = null,
) : Asset {
    override val type: AssetType get() = AssetType.Animation
}
