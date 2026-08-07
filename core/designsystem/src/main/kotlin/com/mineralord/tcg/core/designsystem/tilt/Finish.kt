package com.mineralord.tcg.core.designsystem.tilt

import com.mineralord.tcg.engine.model.Rarity

/**
 * Acabado visual de una carta, **desacoplado de la rareza** (regla de oro: `Finish` =
 * datos; `Renderer` = AGSL). Es el eje que decide QUÉ efecto se pinta; el shader lo
 * traduce a máscara + color + bandas. 80/20: cubrimos las familias frecuentes primero.
 */
enum class Finish {
    NONE,
    REVERSE_HOLO,
    REGULAR_HOLO,
    DOUBLE_RARE,               // ex
    ULTRA_RARE,                // full art
    ILLUSTRATION_RARE,         // IR
    SPECIAL_ILLUSTRATION_RARE, // SIR
    HYPER_GOLD,                // dorada
}

/**
 * Resolución del acabado desde la rareza (fuente de datos v1 = la `Rarity` que ya trae
 * cada carta; los metadatos de foil TCGL se añadirán como parámetros extra después).
 * `null` → holo genérico (cuando la pantalla aún no propaga la rareza).
 */
fun resolveFinish(rarity: Rarity?): Finish = when (rarity) {
    null -> Finish.REGULAR_HOLO
    Rarity.COMMON, Rarity.UNCOMMON -> Finish.NONE
    Rarity.RARE, Rarity.RARE_HOLO -> Finish.REGULAR_HOLO
    Rarity.DOUBLE_RARE -> Finish.DOUBLE_RARE
    Rarity.ULTRA_RARE -> Finish.ULTRA_RARE
    Rarity.ILLUSTRATION_RARE -> Finish.ILLUSTRATION_RARE
    Rarity.SPECIAL_ILLUSTRATION_RARE -> Finish.SPECIAL_ILLUSTRATION_RARE
    Rarity.HYPER_RARE -> Finish.HYPER_GOLD
    Rarity.PROMO -> Finish.REGULAR_HOLO
}

/**
 * Resolución más precisa usando los metadatos de foil REALES de TCGL (`foil.type`/
 * `foil.mask`), que son la verdad de origen. Corrige casos que la rareza sola no capta
 * (p. ej. una Común/Infrecuente **reverse holo** SÍ tiene foil). La rareza da el "grado";
 * la máscara decide reverse vs holo vs etch.
 */
fun resolveFinish(rarity: Rarity?, foilType: String?, foilMask: String?): Finish {
    val byRarity = resolveFinish(rarity)
    return when (foilMask?.uppercase()) {
        "ETCHED" -> when (byRarity) {
            Finish.SPECIAL_ILLUSTRATION_RARE, Finish.HYPER_GOLD,
            Finish.ULTRA_RARE, Finish.ILLUSTRATION_RARE -> byRarity
            else -> Finish.ULTRA_RARE
        }
        "REVERSE" -> Finish.REVERSE_HOLO
        "HOLO" -> if (byRarity == Finish.NONE) Finish.REGULAR_HOLO else byRarity
        else -> byRarity
    }
}

/** Perfil numérico que consume el shader AGSL (uProfile). */
internal val Finish.shaderProfile: Float
    get() = when (this) {
        Finish.NONE -> -1f
        Finish.REVERSE_HOLO -> 0f
        Finish.REGULAR_HOLO -> 1f
        Finish.DOUBLE_RARE -> 2f
        Finish.ULTRA_RARE -> 3f
        Finish.ILLUSTRATION_RARE -> 4f
        Finish.SPECIAL_ILLUSTRATION_RARE -> 5f
        Finish.HYPER_GOLD -> 6f
    }
