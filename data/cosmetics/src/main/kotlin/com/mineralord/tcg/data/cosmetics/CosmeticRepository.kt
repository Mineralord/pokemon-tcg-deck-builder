package com.mineralord.tcg.data.cosmetics

import kotlinx.serialization.json.Json

/**
 * Carga el catálogo de cosméticos desde recursos JSON empaquetados (classpath), igual que
 * `CardRepository` carga las cartas. Es Kotlin puro: sirve en tests JVM y en la app Android
 * (los recursos viajan en el AAR/APK). Data-driven: el catálogo se define en datos, no en
 * código (§17).
 */
class CosmeticRepository private constructor(private val items: List<Cosmetic>) {

    /** Todos los cosméticos del catálogo (en orden de declaración). */
    val all: List<Cosmetic> get() = items
    val size: Int get() = items.size
    private val byId: Map<String, Cosmetic> = items.associateBy { it.id }

    /** Cosmético por id estable, o null si no existe (catálogos desalineados → fail-safe). */
    operator fun get(id: String?): Cosmetic? = id?.let { byId[it] }

    /**
     * Cosméticos de una categoría **distribuibles y activos** (lo que se ve en tienda),
     * ordenados por rareza ascendente. Excluye RESTRICTED/ARCHIVED/EXPERIMENTAL.
     */
    fun shopByCategory(category: CosmeticCategory): List<Cosmetic> =
        items.filter {
            it.category == category &&
                it.status == CosmeticStatus.ACTIVE &&
                it.isDistributable
        }.sortedBy { it.rarity.ordinal }

    /** Todos los de una categoría, sin filtrar (para la Colección/Studio/Museo). */
    fun byCategory(category: CosmeticCategory): List<Cosmetic> =
        items.filter { it.category == category }.sortedBy { it.rarity.ordinal }

    companion object {
        const val INDEX_RESOURCE = "/cosmetics/index.json"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        private fun resource(path: String): String? =
            CosmeticRepository::class.java.getResourceAsStream(path)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }

        /**
         * Carga el catálogo: lee `cosmetics/index.json` (lista de packs) y fusiona cada pack.
         * Si el índice falta, devuelve un catálogo vacío (fail-safe; la UI degrada, no crashea).
         */
        fun load(): CosmeticRepository {
            val indexJson = resource(INDEX_RESOURCE) ?: return CosmeticRepository(emptyList())
            val index = json.decodeFromString(CosmeticIndexDto.serializer(), indexJson)
            val items = index.packs.flatMap { rel ->
                val body = resource("/cosmetics/$rel")
                    ?: error("Falta el recurso /cosmetics/$rel declarado en el índice")
                json.decodeFromString(CosmeticPackDto.serializer(), body).items.map { it.toDomain() }
            }
            return CosmeticRepository(items)
        }

        /** Carga desde una cadena JSON de pack (útil para tests). */
        fun fromPackJson(content: String): CosmeticRepository {
            val pack = json.decodeFromString(CosmeticPackDto.serializer(), content)
            return CosmeticRepository(pack.items.map { it.toDomain() })
        }
    }
}

/**
 * Acceso cacheado en proceso al catálogo (carga perezosa una sola vez). La UI y la app lo
 * usan sin recargar recursos en cada composición.
 */
object Cosmetics {
    val repo: CosmeticRepository by lazy { CosmeticRepository.load() }
}
