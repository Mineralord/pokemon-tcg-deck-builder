package com.mineralord.tcg.data.gacha

import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Rarity
import kotlin.random.Random

/** Una carta obtenida al abrir un sobre. */
data class OpenedCard(val id: CardId, val rarity: Rarity)

/**
 * Abre sobres de forma determinista (RNG sembrado → reproducible/testeable).
 *
 * Por cada slot: resuelve la rareza (fija o por sorteo ponderado) y elige una
 * carta uniforme de esa rareza en el [PackPool]. Garantías:
 *  - Si una rareza no tiene cartas, cae a la mejor rareza inferior disponible.
 *  - Cada sobre tiene al menos una carta de rareza ≥ [guaranteedFloor].
 *  - Evita repetir la misma carta ultra/ilustración dentro del mismo sobre
 *    (reintenta unas veces; si no hay alternativa, la deja).
 */
class PackOpener(
    private val guaranteedFloor: Rarity = Rarity.RARE,
    private val dedupeFrom: Rarity = Rarity.ULTRA_RARE,
) {

    fun open(template: PackTemplate, pool: PackPool, random: Random): List<OpenedCard> {
        val result = ArrayList<OpenedCard>(template.size)

        for (slot in template.slots) {
            val rarity = when (slot) {
                is PackSlot.Fixed -> slot.rarity
                is PackSlot.Weighted -> roll(slot.table, random)
            }
            result += pickCard(rarity, pool, random, avoid = result)
        }

        ensureGuarantee(result, pool, random)
        return result
    }

    // --------------------------------------------------------------- internos

    private fun roll(table: List<WeightedEntry>, random: Random): Rarity {
        val total = table.sumOf { it.weight }
        var r = random.nextInt(total)
        for (entry in table) {
            if (r < entry.weight) return entry.rarity
            r -= entry.weight
        }
        return table.last().rarity   // inalcanzable; por seguridad
    }

    private fun pickCard(
        rarity: Rarity,
        pool: PackPool,
        random: Random,
        avoid: List<OpenedCard>,
    ): OpenedCard {
        val effective = if (pool.has(rarity)) rarity else pool.bestAvailableUpTo(rarity)
            ?: pool.rarities.minByOrNull { it.ordinal }
            ?: error("El PackPool está vacío")
        val candidates = pool.cardsOf(effective)

        var chosen = candidates[random.nextInt(candidates.size)]
        if (effective.ordinal >= dedupeFrom.ordinal && candidates.size > 1) {
            var attempts = 0
            val taken = avoid.filter { it.rarity == effective }.map { it.id }.toSet()
            while (chosen in taken && attempts < 5) {
                chosen = candidates[random.nextInt(candidates.size)]
                attempts++
            }
        }
        return OpenedCard(chosen, effective)
    }

    private fun ensureGuarantee(result: MutableList<OpenedCard>, pool: PackPool, random: Random) {
        if (result.any { it.rarity.ordinal >= guaranteedFloor.ordinal }) return
        val floor = if (pool.has(guaranteedFloor)) guaranteedFloor
        else pool.bestAvailableUpTo(Rarity.entries.last())?.takeIf { it.ordinal >= guaranteedFloor.ordinal }
            ?: return   // el pool no tiene nada ≥ floor; no se puede garantizar
        val cards = pool.cardsOf(floor)
        if (cards.isNotEmpty()) {
            result[result.lastIndex] = OpenedCard(cards[random.nextInt(cards.size)], floor)
        }
    }
}
