package com.mineralord.tcg.engine.model

/**
 * Tipos base y vocabulario compartido del modelo de cartas.
 *
 * Este módulo es **Kotlin puro (JVM)**: no depende de Android ni de ningún
 * framework. Todo aquí es data inmutable, deterministicamente serializable y
 * testeable a velocidad de unidad. La fuente de verdad de los datos reales son
 * las 487 cartas de `data/cartas-db.js` (web), que se importan vía loader en
 * `:data:cards` — aquí solo vive el *modelo*, no los datos.
 */

/** Los 11 tipos de energía del TCG moderno. */
enum class EnergyType {
    GRASS, FIRE, WATER, LIGHTNING, PSYCHIC, FIGHTING,
    DARKNESS, METAL, FAIRY, DRAGON, COLORLESS;
}

/** Condiciones especiales que un Pokémon Activo puede sufrir. */
enum class Status { ASLEEP, CONFUSED, PARALYZED, POISONED, BURNED }

/**
 * Rareza oficial mapeada desde el campo `rareza` del DB de referencia.
 * El orden importa: se usa para escalones del slot "hit" del gacha (§4).
 */
enum class Rarity {
    COMMON,
    UNCOMMON,
    RARE,
    RARE_HOLO,
    DOUBLE_RARE,                 // Pokémon ex (era SV)
    ULTRA_RARE,                  // V / Full Art
    ILLUSTRATION_RARE,
    SPECIAL_ILLUSTRATION_RARE,
    HYPER_RARE,                  // Secret / "rainbow"
    PROMO;
}

/** Identificador estable de carta (p.ej. "sv3pt5-6"). Value class: cero overhead. */
@JvmInline
value class CardId(val raw: String) {
    init { require(raw.isNotBlank()) { "CardId no puede estar en blanco" } }
}

/**
 * Puntero tipado hacia el [EffectRegistry]. Desacopla los **datos** de carta
 * de su **comportamiento**: la carta solo guarda el id del efecto, el motor lo
 * resuelve. Permite autorar efectos carta a carta sin tocar el modelo.
 */
@JvmInline
value class EffectId(val raw: String) {
    init { require(raw.isNotBlank()) { "EffectId no puede estar en blanco" } }
}

/** Texto bilingüe ES/EN. El cliente elige idioma; el modelo guarda ambos. */
data class LocalizedText(val es: String, val en: String) {
    fun get(spanish: Boolean): String = if (spanish) es else en
}

/** Metadatos del set al que pertenece la carta. */
data class SetInfo(
    val code: String,            // p.ej. "sv3pt5"
    val name: LocalizedText,
    val series: String,
)

/** URLs de arte para ES/EN, tamaño chico y grande. */
data class ArtworkRefs(
    val smallEs: String?,
    val largeEs: String?,
    val smallEn: String,
    val largeEn: String,
) {
    fun small(spanish: Boolean): String = (if (spanish) smallEs else null) ?: smallEn
    fun large(spanish: Boolean): String = (if (spanish) largeEs else null) ?: largeEn
}

/** Modificador por tipo: debilidad ("×2") o resistencia ("-30"). */
data class TypeModifier(val type: EnergyType, val value: String)
