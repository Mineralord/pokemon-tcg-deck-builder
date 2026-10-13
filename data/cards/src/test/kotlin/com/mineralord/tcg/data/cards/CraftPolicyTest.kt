package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.Rarity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifica, para CADA expansión del catálogo (presentes y futuras), que [CraftPolicy] bloquea
 * exactamente las DOS rarezas más altas (de Ultra Rara en adelante) y deja fabricable el resto.
 *
 * Es data-driven: al añadir expansiones nuevas, se evalúan automáticamente con la misma regla.
 */
class CraftPolicyTest {

    private val repo = CardRepository.load()
    private val rareFloor = Rarity.ULTRA_RARE.ordinal
    private val bySet = repo.all.groupBy { it.set.code }

    /** Las dos rarezas tope (>= Ultra Rara, excluyendo PROMO) esperadas como bloqueadas en un set. */
    private fun expectedBlocked(cards: List<com.mineralord.tcg.engine.model.Card>): Set<Int> =
        cards.map { it.rarity.ordinal }
            .filter { it >= rareFloor && it != Rarity.PROMO.ordinal }
            .distinct()
            .sortedDescending()
            .take(2)
            .toSet()

    @Test
    fun `bloquea exactamente las dos rarezas mas altas de cada expansion`() {
        for ((set, cards) in bySet) {
            val expected = expectedBlocked(cards)
            for (card in cards) {
                val shouldBlock = card.rarity.ordinal in expected
                assertEquals(
                    shouldBlock,
                    CraftPolicy.isTopRarityOfSet(card),
                    "Set $set, carta ${card.id.raw} (${card.rarity}): bloqueo esperado=$shouldBlock",
                )
            }
        }
    }

    @Test
    fun `nunca bloquea rarezas por debajo de Ultra Rara`() {
        for (card in repo.all) {
            if (card.rarity.ordinal < rareFloor) {
                assertFalse(
                    CraftPolicy.isTopRarityOfSet(card),
                    "No debería bloquearse ${card.id.raw} (${card.rarity})",
                )
            }
        }
    }

    @Test
    fun `cada expansion bloquea como mucho dos rarezas`() {
        for ((set, cards) in bySet) {
            val blocked = cards.filter { CraftPolicy.isTopRarityOfSet(it) }
                .map { it.rarity }.toSet()
            assertTrue(blocked.size <= 2, "Set $set bloquea ${blocked.size} rarezas: $blocked")
        }
    }

    /** Informe legible: qué rarezas quedan bloqueadas por expansión (para revisión de un vistazo). */
    @Test
    fun `informe de rarezas bloqueadas por expansion`() {
        val report = bySet.entries
            .sortedBy { it.key }
            .joinToString("\n") { (set, cards) ->
                val blocked = cards.filter { CraftPolicy.isTopRarityOfSet(it) }
                    .map { it.rarity }.toSortedSet(compareByDescending { it.ordinal })
                val blockedStr = if (blocked.isEmpty()) "(ninguna)" else blocked.joinToString(", ")
                "  $set → $blockedStr"
            }
        println("Rarezas NO fabricables por expansión:\n$report")
    }
}
