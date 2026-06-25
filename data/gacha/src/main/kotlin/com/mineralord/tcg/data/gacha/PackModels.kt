package com.mineralord.tcg.data.gacha

import com.mineralord.tcg.engine.model.Rarity

/**
 * Estructura de un sobre como una lista ordenada de "slots". Igual que los
 * sobres oficiales: la mayoría de slots tienen rareza fija (comunes/poco
 * comunes) y uno o dos son ponderados (el "hit") con probabilidad de subir a
 * rarezas altas.
 */
sealed interface PackSlot {
    /** Slot de rareza fija. */
    data class Fixed(val rarity: Rarity) : PackSlot

    /** Slot ponderado: se sortea la rareza según [table]. */
    data class Weighted(val table: List<WeightedEntry>) : PackSlot
}

/** Entrada de la tabla ponderada: una rareza con su peso relativo. */
data class WeightedEntry(val rarity: Rarity, val weight: Int) {
    init { require(weight > 0) { "El peso debe ser positivo" } }
}

/** Plantilla de un sobre: lista ordenada de slots (normalmente 10). */
data class PackTemplate(val id: String, val slots: List<PackSlot>) {
    val size: Int get() = slots.size
}

/**
 * Plantillas y pesos por defecto. Aproximan la estructura de un sobre estándar
 * de SV (10 cartas). Los pesos son configurables por set (a futuro, remotos).
 */
object RarityWeights {

    /** Slot "hit": casi siempre Rara, con cola hacia rarezas altas. */
    val HIT_SLOT: List<WeightedEntry> = listOf(
        WeightedEntry(Rarity.RARE, 60),
        WeightedEntry(Rarity.RARE_HOLO, 22),
        WeightedEntry(Rarity.DOUBLE_RARE, 10),     // Pokémon ex
        WeightedEntry(Rarity.ULTRA_RARE, 5),       // Full Art / V
        WeightedEntry(Rarity.ILLUSTRATION_RARE, 2),
        WeightedEntry(Rarity.SPECIAL_ILLUSTRATION_RARE, 1),
        // HYPER_RARE se alcanza muy rara vez; se deja para pity/eventos.
    )

    /** Slot "reverse": holo común/poco común con opción a rara. */
    val REVERSE_SLOT: List<WeightedEntry> = listOf(
        WeightedEntry(Rarity.UNCOMMON, 55),
        WeightedEntry(Rarity.COMMON, 30),
        WeightedEntry(Rarity.RARE, 15),
    )

    /** Sobre estándar de 10 cartas: 4 comunes, 3 poco comunes, 2 reverse, 1 hit. */
    val STANDARD_PACK = PackTemplate(
        id = "standard-10",
        slots = buildList {
            repeat(4) { add(PackSlot.Fixed(Rarity.COMMON)) }
            repeat(3) { add(PackSlot.Fixed(Rarity.UNCOMMON)) }
            repeat(2) { add(PackSlot.Weighted(REVERSE_SLOT)) }
            add(PackSlot.Weighted(HIT_SLOT))
        },
    )
}
