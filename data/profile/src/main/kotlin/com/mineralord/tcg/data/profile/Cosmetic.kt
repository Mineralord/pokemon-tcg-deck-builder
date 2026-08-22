package com.mineralord.tcg.data.profile

/**
 * Sistema de cosméticos (Canon: Fase 3 — Tienda y Precios, Cap. 8–14).
 *
 * Los cosméticos son elementos EXCLUSIVAMENTE estéticos (neutralidad competitiva,
 * §8.2/§13.4), vinculados permanentemente a la cuenta (§13.2) y NO intercambiables
 * (§13.3). Se adquieren con **Monedas** (§3.3). Este archivo es la identidad de
 * dominio; el color/glow de cada rareza es un detalle de la capa de UI.
 */

/** Categorías de personalización (subconjunto visual de §8.3). */
enum class CosmeticCategory(val displayName: String) {
    AVATAR("Avatares"),
    MARCO("Marcos"),
    FONDO("Fondos"),
    TAPETE("Tapetes"),
    FUNDA("Fundas"),
    CAJA("Cajas de mazo"),
}

/**
 * Escala oficial de rareza cosmética (§9.3), INDEPENDIENTE de la rareza de las
 * cartas (§9.2). Ordenada de menor a mayor exclusividad.
 */
enum class CosmeticRarity(val displayName: String) {
    COMUN("Común"),
    POCO_COMUN("Poco común"),
    RARO("Raro"),
    EPICO("Épico"),
    LEGENDARIO("Legendario"),
    MITICO("Mítico"),
}

/**
 * Un elemento cosmético del catálogo.
 *
 * @property id identificador estable (clave de posesión/equipado, nunca cambia).
 * @property category a qué aspecto de personalización pertenece.
 * @property rarity nivel de exclusividad (§9); determina el precio (§14.3).
 * @property name nombre mostrado.
 * @property colors paleta ARGB para el previsualizado procedural (arte propio, sin
 *   copyright); es metadato de catálogo, la UI decide cómo pintarlo.
 * @property prestige cosmético de prestigio (§14.5): precio muy superior al de su
 *   rareza; objetivo de muy largo plazo.
 */
data class Cosmetic(
    val id: String,
    val category: CosmeticCategory,
    val rarity: CosmeticRarity,
    val name: String,
    val colors: List<Long>,
    val prestige: Boolean = false,
) {
    /** Precio en Monedas (derivado de la rareza; prestigio multiplica, §14.3/§14.5). */
    val priceMonedas: Int get() = EconomyRules.cosmeticPrice(rarity, prestige)
}

/**
 * Catálogo permanente de cosméticos (§8.5, §5.6: conservación histórica). Es
 * estático y aditivo: incorporar contenido nuevo AÑADE entradas, nunca sustituye
 * ni elimina las existentes. Arte propio (paletas procedurales) para no incurrir
 * en copyright, igual que los placeholders de la pantalla de inicio.
 */
object CosmeticCatalog {

    val ALL: List<Cosmetic> = buildList {
        // ---- Avatares ----
        add(Cosmetic("avatar-brasa", CosmeticCategory.AVATAR, CosmeticRarity.COMUN, "Brasa", listOf(0xFFFF7043, 0xFFE64A19)))
        add(Cosmetic("avatar-marea", CosmeticCategory.AVATAR, CosmeticRarity.POCO_COMUN, "Marea", listOf(0xFF29B6F6, 0xFF0277BD)))
        add(Cosmetic("avatar-fronda", CosmeticCategory.AVATAR, CosmeticRarity.RARO, "Fronda", listOf(0xFF66BB6A, 0xFF2E7D32)))
        add(Cosmetic("avatar-quimera", CosmeticCategory.AVATAR, CosmeticRarity.EPICO, "Quimera", listOf(0xFFAB47BC, 0xFF6A1B9A)))
        add(Cosmetic("avatar-solaris", CosmeticCategory.AVATAR, CosmeticRarity.LEGENDARIO, "Solaris", listOf(0xFFFFD54F, 0xFFFF8F00)))
        add(Cosmetic("avatar-cosmos", CosmeticCategory.AVATAR, CosmeticRarity.MITICO, "Cosmos", listOf(0xFFFF4081, 0xFF7C4DFF, 0xFF18FFFF), prestige = true))

        // ---- Marcos de perfil ----
        add(Cosmetic("marco-acero", CosmeticCategory.MARCO, CosmeticRarity.COMUN, "Acero", listOf(0xFFB0BEC5, 0xFF607D8B)))
        add(Cosmetic("marco-jade", CosmeticCategory.MARCO, CosmeticRarity.POCO_COMUN, "Jade", listOf(0xFF26A69A, 0xFF00695C)))
        add(Cosmetic("marco-zafiro", CosmeticCategory.MARCO, CosmeticRarity.RARO, "Zafiro", listOf(0xFF42A5F5, 0xFF1565C0)))
        add(Cosmetic("marco-amatista", CosmeticCategory.MARCO, CosmeticRarity.EPICO, "Amatista", listOf(0xFF7E57C2, 0xFF4527A0)))
        add(Cosmetic("marco-aureo", CosmeticCategory.MARCO, CosmeticRarity.LEGENDARIO, "Áureo", listOf(0xFFFFCA28, 0xFFC49000)))

        // ---- Fondos ----
        add(Cosmetic("fondo-alba", CosmeticCategory.FONDO, CosmeticRarity.COMUN, "Alba", listOf(0xFFFFE0B2, 0xFFFFB74D)))
        add(Cosmetic("fondo-bosque", CosmeticCategory.FONDO, CosmeticRarity.POCO_COMUN, "Bosque", listOf(0xFF81C784, 0xFF2E7D32)))
        add(Cosmetic("fondo-abismo", CosmeticCategory.FONDO, CosmeticRarity.RARO, "Abismo", listOf(0xFF1A237E, 0xFF000051)))
        add(Cosmetic("fondo-aurora", CosmeticCategory.FONDO, CosmeticRarity.EPICO, "Aurora", listOf(0xFF00E5FF, 0xFF7C4DFF, 0xFF64FFDA)))
        add(Cosmetic("fondo-nebulosa", CosmeticCategory.FONDO, CosmeticRarity.LEGENDARIO, "Nebulosa", listOf(0xFFFF4081, 0xFF7C4DFF, 0xFF3D5AFE)))

        // ---- Tapetes de juego ----
        add(Cosmetic("tapete-arena", CosmeticCategory.TAPETE, CosmeticRarity.COMUN, "Arena", listOf(0xFFD7CCC8, 0xFF8D6E63)))
        add(Cosmetic("tapete-liga", CosmeticCategory.TAPETE, CosmeticRarity.RARO, "Liga", listOf(0xFF3949AB, 0xFF1A237E)))
        add(Cosmetic("tapete-campeon", CosmeticCategory.TAPETE, CosmeticRarity.EPICO, "Campeón", listOf(0xFFFF7043, 0xFFAD1457)))
        add(Cosmetic("tapete-maestro", CosmeticCategory.TAPETE, CosmeticRarity.LEGENDARIO, "Maestro", listOf(0xFFFFD54F, 0xFFEF6C00, 0xFFB71C1C)))

        // ---- Fundas para cartas ----
        add(Cosmetic("funda-lisa", CosmeticCategory.FUNDA, CosmeticRarity.COMUN, "Lisa", listOf(0xFFCFD8DC, 0xFF90A4AE)))
        add(Cosmetic("funda-ola", CosmeticCategory.FUNDA, CosmeticRarity.POCO_COMUN, "Ola", listOf(0xFF4FC3F7, 0xFF0288D1)))
        add(Cosmetic("funda-prisma", CosmeticCategory.FUNDA, CosmeticRarity.EPICO, "Prisma", listOf(0xFF00BCD4, 0xFF7C4DFF, 0xFFEC407A)))
        add(Cosmetic("funda-eclipse", CosmeticCategory.FUNDA, CosmeticRarity.MITICO, "Eclipse", listOf(0xFF212121, 0xFFFFC107, 0xFF212121), prestige = true))

        // ---- Cajas de mazo ----
        add(Cosmetic("caja-roble", CosmeticCategory.CAJA, CosmeticRarity.COMUN, "Roble", listOf(0xFFA1887F, 0xFF5D4037)))
        add(Cosmetic("caja-cromo", CosmeticCategory.CAJA, CosmeticRarity.RARO, "Cromo", listOf(0xFFB0BEC5, 0xFF455A64)))
        add(Cosmetic("caja-real", CosmeticCategory.CAJA, CosmeticRarity.LEGENDARIO, "Real", listOf(0xFFFFD54F, 0xFF6A1B9A)))
    }

    private val byId = ALL.associateBy { it.id }

    /** Cosmético por id, o null si no existe (catálogos desalineados). */
    operator fun get(id: String): Cosmetic? = byId[id]

    /** Cosméticos de una categoría, ordenados por rareza ascendente. */
    fun byCategory(category: CosmeticCategory): List<Cosmetic> =
        ALL.filter { it.category == category }.sortedBy { it.rarity.ordinal }
}
