package com.mineralord.tcg.studio

import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animationcompose.EvolveVariants
import com.mineralord.tcg.studio.assets.AnimationAsset
import com.mineralord.tcg.studio.assets.AssetRegistry
import com.mineralord.tcg.studio.assets.AssetStatus

/**
 * **Composition root del Asset Registry (APK 2).**
 *
 * Aquí —fuera de toda herramienta— se ensambla la fuente ÚNICA de verdad de los recursos del Studio.
 * Es el análogo de [studioLabs] para los datos: la app declara QUÉ assets existen; la Gallery (y las
 * herramientas futuras) sólo los **consumen** desde el [AssetRegistry], sin mantener listas propias.
 *
 * Migración de las animaciones existentes al registro (DRAW · MOVE/BASIC · EVOLUTION):
 *  - Cada animación referencia su **recurso real**, la [AnimationRequest] que la dispara en el
 *    pipeline compartido (mismo motor para Studio y juego; sin duplicar lógica).
 *  - Las cinco variantes de Evolución nacen `Experimental`; su promoción a Canon la decide el
 *    desarrollador (Canon Manager es un Sprint futuro).
 *
 * Añadir un tipo de Asset futuro (Audio, Shaders, …) = añadir su propio catálogo y sumarlo a esta
 * lista; el registro ya es genérico y no requiere rediseño.
 */
fun studioAssetRegistry(): AssetRegistry = AssetRegistry(animationAssets())

private const val PREVIEW_PLAYER = "studio"

/** Catálogo de animaciones migrado desde la antigua lista propietaria de la Gallery. */
private fun animationAssets(): List<AnimationAsset> = buildList {
    add(
        AnimationAsset(
            id = "DRAW_001",
            name = "Robar carta",
            category = "Robar carta",
            status = AssetStatus.Canon,
            request = AnimationRequest.CardDrawn(PREVIEW_PLAYER, "preview"),
            author = "I+D AAA",
            createdAt = "2026-07-23",
            inspiration = "Marvel Snap (estándar «Snap sobrio»)",
            description = "Carta del mazo a la mano con arco leve, escala y overshoot sobrio.",
            durationMillis = 320,
            tags = listOf("draw", "mano", "mazo"),
        ),
    )
    add(
        AnimationAsset(
            id = "BASIC_001",
            name = "Poner en juego",
            category = "Pokémon Básico",
            status = AssetStatus.Canon,
            request = AnimationRequest.PokemonPlayed(PREVIEW_PLAYER, "preview"),
            author = "I+D AAA",
            createdAt = "2026-07-23",
            inspiration = "Pokémon TCG Live",
            description = "Desplazamiento de la carta de la mano al Activo por la capa de vuelo.",
            durationMillis = 260,
            tags = listOf("move", "activo", "mano"),
        ),
    )
    addAll(evolutionAssets())
}

/** Las cinco variantes de Evolución (I+D AAA), todas `Experimental`: comparten motor, renderer y capa. */
private fun evolutionAssets(): List<AnimationAsset> = listOf(
    evolveAsset(
        EvolveVariants.EVO_001, "Crystal Bloom",
        "Bombeo suave con anillo cristalino frío que florece alrededor de la carta.",
        "Hearthstone (cristal/escarcha) + Legends of Runeterra", 700,
    ),
    evolveAsset(
        EvolveVariants.EVO_002, "Energy Spiral",
        "La carta gira y asciende envuelta en una espiral de energía violeta.",
        "Genshin/Honkai (revelado gacha en espiral)", 850,
    ),
    evolveAsset(
        EvolveVariants.EVO_003, "Radiant Ascension",
        "Ascenso pronunciado sobre una columna de luz dorada con gran destello.",
        "Legends of Runeterra (subida de nivel de campeón)", 950,
    ),
    evolveAsset(
        EvolveVariants.EVO_004, "DNA Morph",
        "Volteo/morfología rápida de la carta con tono verde, sin anillo.",
        "Evolución del anime Pokémon (silueta que muta)", 600,
    ),
    evolveAsset(
        EvolveVariants.EVO_005, "Celestial Burst",
        "Estallido: bombeo fuerte, destello intenso y anillo amplio azul-blanco.",
        "Marvel Snap (reveal) + estallido del anime", 800,
    ),
)

private fun evolveAsset(
    id: String,
    name: String,
    description: String,
    inspiration: String,
    durationMillis: Long,
): AnimationAsset = AnimationAsset(
    id = id,
    name = name,
    category = "Evolución",
    status = AssetStatus.Experimental,
    request = AnimationRequest.Evolved(PREVIEW_PLAYER, "preview", id),
    author = "I+D AAA",
    createdAt = "2026-07-23",
    inspiration = inspiration,
    description = description,
    durationMillis = durationMillis,
    tags = listOf("evolución", id),
)
