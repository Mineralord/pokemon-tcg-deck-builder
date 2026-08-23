package com.mineralord.tcg.data.cosmetics

import kotlinx.serialization.Serializable

/**
 * DTOs data-driven del catálogo de cosméticos. El catálogo se declara en recursos JSON
 * (`cosmetics/index.json` + archivos por pack) y se mapea a [Cosmetic]. Añadir un cosmético
 * = añadir una entrada de metadata (y su asset si no es procedural), SIN escribir Kotlin
 * (§17). Campos laxos con defaults para compatibilidad retroactiva (§30): un JSON antiguo
 * sin un campo nuevo sigue cargando.
 */

/** Índice raíz: lista los archivos de packs/colecciones que componen el catálogo. */
@Serializable
data class CosmeticIndexDto(
    val packs: List<String> = emptyList(),
)

/** Un archivo de pack: metadatos del pack + sus cosméticos. */
@Serializable
data class CosmeticPackDto(
    val pack: String = "",
    val items: List<CosmeticDto> = emptyList(),
)

/** Metadata serializable de un cosmético (enums como String para tolerancia). */
@Serializable
data class CosmeticDto(
    val id: String,
    val category: String,
    val rarity: String = "COMUN",
    val name: String,
    /** Paleta ARGB como cadenas hex ("0xFFRRGGBB" o "FFRRGGBB") — cómodo de autorar en JSON. */
    val colors: List<String> = emptyList(),
    val prestige: Boolean = false,
    val status: String = "ACTIVE",
    val license: String = "GREEN",
    val assetType: String = "PROCEDURAL",
    val renderer: String? = null,
    val assetRef: String? = null,
    val subcategory: String? = null,
    val collection: String? = null,
    val source: String? = null,
    val author: String? = null,
    val originalUrl: String? = null,
    val sourceRepository: String? = null,
    val acquisitionMethod: String? = null,
    val unlockMethod: String? = null,
    val animated: Boolean = false,
    val releaseDate: String? = null,
    val priceOverride: Int? = null,
    val assetVersion: Int = 1,
    val metadataVersion: Int = 1,
) {
    fun toDomain(): Cosmetic = Cosmetic(
        id = id,
        category = CosmeticCategory.fromName(category) ?: CosmeticCategory.AVATAR,
        rarity = CosmeticRarity.fromName(rarity),
        name = name,
        colors = colors.mapNotNull { hex ->
            runCatching { hex.removePrefix("0x").removePrefix("#").toLong(16) }.getOrNull()
        },
        prestige = prestige,
        status = CosmeticStatus.fromName(status),
        license = CosmeticLicense.fromName(license),
        assetType = CosmeticAssetType.fromName(assetType),
        renderer = renderer,
        assetRef = assetRef,
        subcategory = subcategory,
        collection = collection,
        source = source,
        author = author,
        originalUrl = originalUrl,
        sourceRepository = sourceRepository,
        acquisitionMethod = acquisitionMethod,
        unlockMethod = unlockMethod,
        animated = animated,
        releaseDate = releaseDate,
        priceOverride = priceOverride,
        assetVersion = assetVersion,
        metadataVersion = metadataVersion,
    )
}
