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

    /**
     * Slot de ENERGÍA BÁSICA: toma una carta uniformemente del conjunto de energías básicas que
     * el [PackOpener] recibe aparte (una por tipo). Los sobres reales de 151 incluyen 1 Energía
     * Básica (holo "Cosmos") por sobre; aquí sale una Básica de cualquier tipo con igual probabilidad.
     */
    data object BasicEnergy : PackSlot
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

    // ======================= SOBRE FIEL DE 151 (sv3pt5) =======================
    // Investigación de la estructura y probabilidades REALES del sobre de mejora inglés de
    // «Escarlata y Púrpura — 151»: 10 cartas por sobre (9 numeradas + 1 Energía Básica; más un código
    // digital que aquí no aplica). Estructura oficial del sobre moderno S&V: 4 Comunes, 3 Infrecuentes,
    // 1 Reverse Holo, 1 slot «Rara o mejor» (garantiza Rara Holo+) y 1 Energía Básica. (En 151 la
    // rareza «Rare» YA es holográfica: no hay RARE_HOLO aparte, así que el slot hit tope-base es RARE.)
    //
    // Probabilidades por sobre del slot «Rara o mejor», tomadas de un muestreo público de 700 sobres
    // (DigitalTQ): Rara(holo) 80,43 % · Doble Rara(ex) 13,29 % · Rara Ilustración 7,71 % ·
    // Ultra Rara(Full Art) 6,00 % · Rara Ilustración Especial 3,57 % · Hiper Rara 2,86 %. Los pesos
    // enteros de abajo son ESAS tasas ×100, de modo que la distribución del sorteo las reproduce.

    /** Slot «Rara o mejor» del 151: pesos = tasas por sobre (×100) del muestreo de 700 sobres. */
    val HIT_SLOT_151: List<WeightedEntry> = listOf(
        WeightedEntry(Rarity.RARE, 8043),                       // Rara holográfica (#… holo del set)
        WeightedEntry(Rarity.DOUBLE_RARE, 1329),                // Pokémon ex
        WeightedEntry(Rarity.ILLUSTRATION_RARE, 771),           // Rara Ilustración (arte alternativo)
        WeightedEntry(Rarity.ULTRA_RARE, 600),                  // Full Art (Entrenador/ex Full Art)
        WeightedEntry(Rarity.SPECIAL_ILLUSTRATION_RARE, 357),   // Rara Ilustración Especial
        WeightedEntry(Rarity.HYPER_RARE, 286),                  // Hiper Rara (dorada)
    )

    /** Slot Reverse Holo del 151: reverse de una Común/Infrecuente/Rara (mayoría bajas). */
    val REVERSE_SLOT_151: List<WeightedEntry> = listOf(
        WeightedEntry(Rarity.COMMON, 50),
        WeightedEntry(Rarity.UNCOMMON, 40),
        WeightedEntry(Rarity.RARE, 10),
    )

    /** Sobre fiel de 151: 4 Comunes · 3 Infrecuentes · 1 Reverse Holo · 1 Rara+ · 1 Energía Básica. */
    val SET_151_PACK = PackTemplate(
        id = "sv3pt5-11",
        slots = buildList {
            repeat(4) { add(PackSlot.Fixed(Rarity.COMMON)) }
            repeat(3) { add(PackSlot.Fixed(Rarity.UNCOMMON)) }
            add(PackSlot.Weighted(REVERSE_SLOT_151))
            add(PackSlot.Weighted(HIT_SLOT_151))
            add(PackSlot.BasicEnergy)
        },
    )

    /**
     * Plantilla de sobre por código de set. Las expansiones MODERNAS de S&V comparten la misma
     * estructura fiel de 10 cartas (4C · 3I · 1 Reverse · 1 Rara+ · 1 Energía Básica); Brecha
     * Paradójica (sv4) usa las mismas rarezas que 151, así que reutiliza esa plantilla. El resto
     * cae al sobre estándar.
     */
    fun templateFor(setCode: String): PackTemplate = when (setCode) {
        "sv3pt5", "sv4" -> SET_151_PACK
        else -> STANDARD_PACK
    }
}
