package com.mineralord.tcg.data.gacha

import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Rarity

/**
 * Conjunto de cartas obtenibles, indexadas por rareza. Es la "bolsa" de la que
 * el [PackOpener] extrae. Se construye filtrando el catálogo por los sets
 * marcados como obtenibles.
 */
class PackPool private constructor(
    private val byRarity: Map<Rarity, List<CardId>>,
) {
    fun cardsOf(rarity: Rarity): List<CardId> = byRarity[rarity].orEmpty()
    fun has(rarity: Rarity): Boolean = !byRarity[rarity].isNullOrEmpty()
    val rarities: Set<Rarity> get() = byRarity.keys
    val totalCards: Int get() = byRarity.values.sumOf { it.size }

    /** Mejor rareza disponible con cartas, no mayor que [max] (para fallbacks). */
    fun bestAvailableUpTo(max: Rarity): Rarity? =
        Rarity.entries.filter { it.ordinal <= max.ordinal && has(it) }.maxByOrNull { it.ordinal }

    companion object {
        fun from(cards: List<Card>): PackPool =
            PackPool(cards.groupBy({ it.rarity }, { it.id }))

        fun ofIds(byRarity: Map<Rarity, List<CardId>>): PackPool = PackPool(byRarity)
    }
}
