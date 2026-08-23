package com.mineralord.tcg.data.cosmetics

/**
 * Modelo de dominio de un cosmético (Canon: Fase 3 — Tienda y Precios, Cap. 8–14).
 *
 * Los cosméticos son EXCLUSIVAMENTE estéticos (neutralidad competitiva §8.2/§13.4),
 * vinculados permanentemente a la cuenta (§13.2) y NO intercambiables (§13.3). Se compran
 * con **Monedas** (§3.3). Este modelo es tipado y estable; se construye desde metadata
 * data-driven ([CosmeticDto]) para poder añadir miles de cosméticos SIN escribir código.
 *
 * La identidad de posesión/equipado del jugador vive en `data:profile` (owned/equipped);
 * aquí sólo vive el CATÁLOGO (contenido).
 */

/** Categorías de personalización (§8.3). Ampliable sin romper datos antiguos. */
enum class CosmeticCategory(val displayName: String) {
    AVATAR("Avatares"),
    MARCO("Marcos"),
    FONDO("Fondos"),
    TAPETE("Tapetes"),
    FUNDA("Fundas"),
    CAJA("Cajas de mazo"),
    MONEDA("Monedas de sorteo"),
    BADGE("Insignias"),
    EMOTE("Emotes"),
    VICTORIA("Efectos de victoria"),
    DERROTA("Efectos de derrota"),
    ;

    companion object {
        /** Categoría por nombre, tolerante (ids desconocidos → null). */
        fun fromName(name: String?): CosmeticCategory? =
            entries.firstOrNull { it.name == name }
    }
}

/** Escala oficial de rareza cosmética (§9.3), independiente de la rareza de cartas (§9.2). */
enum class CosmeticRarity(val displayName: String) {
    COMUN("Común"),
    POCO_COMUN("Poco común"),
    RARO("Raro"),
    EPICO("Épico"),
    LEGENDARIO("Legendario"),
    MITICO("Mítico"),
    ;

    companion object {
        fun fromName(name: String?): CosmeticRarity =
            entries.firstOrNull { it.name == name } ?: COMUN
    }
}

/**
 * Estado del cosmético en el ciclo de vida (preservación histórica §22). Distingue los
 * casos A–G del encargo: incluido/disponible/equipable/archivado/experimental/restringido/
 * retirado-pero-preservado.
 */
enum class CosmeticStatus {
    ACTIVE,        // A · incluido y distribuido en el juego
    AVAILABLE,     // B · existe pero aún no desbloqueado por el jugador
    EXPERIMENTAL,  // E · en pruebas (Studio); no visible en tienda estable
    ARCHIVED,      // G · retirado de distribución pero preservado (Museo/Legado)
    LEGACY,        // pertenece a una versión anterior del proyecto
    RESTRICTED,    // F · catalogado por trazabilidad; NO empaquetable (licencia RED)
    ;

    companion object {
        fun fromName(name: String?): CosmeticStatus =
            entries.firstOrNull { it.name == name } ?: ACTIVE
    }
}

/** Clasificación de licencia para empaquetado (ver docs/cosmetics/ASSET-LEDGER.md). */
enum class CosmeticLicense {
    GREEN,        // uso permitido (procedural propio o CC0)
    YELLOW,       // permitido con condiciones (p.ej. CC-BY: requiere atribución)
    RED,          // NO empaquetable en la distribución
    ARCHIVE_ONLY, // sólo referencia/histórico; nunca en el APK
    ;

    companion object {
        fun fromName(name: String?): CosmeticLicense =
            entries.firstOrNull { it.name == name } ?: GREEN
    }
}

/** Cómo se materializa visualmente/sonoramente el cosmético. */
enum class CosmeticAssetType {
    PROCEDURAL,  // dibujado por código (renderer); 0 bytes, offline, escala infinita
    IMAGE,       // bitmap/vector empaquetado o remoto (assetRef)
    LOTTIE,      // animación vectorial (assetRef)
    AUDIO,       // sfx/música (assetRef)
    MODEL3D,     // reservado (deckboxes 3D); sin runtime 3D por ahora
    ;

    companion object {
        fun fromName(name: String?): CosmeticAssetType =
            entries.firstOrNull { it.name == name } ?: PROCEDURAL
    }
}

/**
 * Cosmético del catálogo. `owned`/`equipped` NO viven aquí (son estado del jugador).
 *
 * @property id identificador ESTABLE (nunca cambia aunque cambie el arte, §29).
 * @property colors paleta ARGB para el renderizado procedural (metadato de catálogo).
 * @property renderer id del renderizador procedural (la capa UI lo interpreta); null si
 *   el cosmético es por asset (IMAGE/AUDIO/…).
 * @property assetRef ruta/URL del binario (para assets no procedurales); null si procedural.
 * @property priceOverride precio explícito en Monedas; si null, se deriva de la rareza.
 */
data class Cosmetic(
    val id: String,
    val category: CosmeticCategory,
    val rarity: CosmeticRarity,
    val name: String,
    val colors: List<Long> = emptyList(),
    val prestige: Boolean = false,
    val status: CosmeticStatus = CosmeticStatus.ACTIVE,
    val license: CosmeticLicense = CosmeticLicense.GREEN,
    val assetType: CosmeticAssetType = CosmeticAssetType.PROCEDURAL,
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
    /** Precio en Monedas (§14.3: por rareza; prestigio §14.5; override explícito si existe). */
    val priceMonedas: Int get() = priceOverride ?: CosmeticPricing.priceFor(rarity, prestige)

    /** ¿Puede empaquetarse/mostrarse desde assets propios? (RESTRICTED/RED nunca se empaqueta). */
    val isDistributable: Boolean
        get() = license == CosmeticLicense.GREEN || license == CosmeticLicense.YELLOW
}

/**
 * Tabla de precios cosméticos por rareza (§14.3). Vive con el dominio cosmético para
 * mantener `data:cosmetics` autocontenido (evita dependencia circular con la economía de
 * `data:profile`). Estable y transparente (§14.6).
 */
object CosmeticPricing {
    private const val PRESTIGE_MULTIPLIER = 3

    fun priceFor(rarity: CosmeticRarity, prestige: Boolean = false): Int {
        val base = when (rarity) {
            CosmeticRarity.COMUN -> 200
            CosmeticRarity.POCO_COMUN -> 400
            CosmeticRarity.RARO -> 800
            CosmeticRarity.EPICO -> 1_500
            CosmeticRarity.LEGENDARIO -> 3_000
            CosmeticRarity.MITICO -> 6_000
        }
        return if (prestige) base * PRESTIGE_MULTIPLIER else base
    }
}
