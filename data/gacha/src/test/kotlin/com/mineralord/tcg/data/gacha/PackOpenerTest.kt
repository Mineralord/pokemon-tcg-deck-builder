package com.mineralord.tcg.data.gacha

import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Rarity
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackOpenerTest {

    /** Pool sintético con cartas en cada rareza (ids no copyrightados). */
    private fun pool(): PackPool {
        val byRarity = Rarity.entries.associateWith { r ->
            (1..10).map { CardId("${r.name.lowercase()}-$it") }
        }
        return PackPool.ofIds(byRarity)
    }

    private val opener = PackOpener()

    @Test
    fun `un sobre estandar entrega exactamente 10 cartas`() {
        val cards = opener.open(RarityWeights.STANDARD_PACK, pool(), Random(1))
        assertEquals(10, cards.size)
    }

    @Test
    fun `siempre hay al menos una carta rara o superior`() {
        repeat(200) { seed ->
            val cards = opener.open(RarityWeights.STANDARD_PACK, pool(), Random(seed.toLong()))
            assertTrue(
                cards.any { it.rarity.ordinal >= Rarity.RARE.ordinal },
                "El sobre con semilla $seed no tuvo ninguna carta rara+",
            )
        }
    }

    @Test
    fun `apertura determinista- misma semilla, mismas cartas`() {
        val a = opener.open(RarityWeights.STANDARD_PACK, pool(), Random(42))
        val b = opener.open(RarityWeights.STANDARD_PACK, pool(), Random(42))
        assertEquals(a, b)
    }

    @Test
    fun `fallback de rareza cuando el pool carece de altas rarezas`() {
        // Pool solo con comunes y poco comunes: el "hit" debe degradar sin romper.
        val limited = PackPool.ofIds(
            mapOf(
                Rarity.COMMON to (1..5).map { CardId("c$it") },
                Rarity.UNCOMMON to (1..5).map { CardId("u$it") },
                Rarity.RARE to listOf(CardId("r1")),
            ),
        )
        val cards = opener.open(RarityWeights.STANDARD_PACK, limited, Random(7))
        assertEquals(10, cards.size)
        // La garantía de rara+ se cumple con la única rara disponible.
        assertTrue(cards.any { it.rarity == Rarity.RARE })
    }

    @Test
    fun `la distribucion del slot hit favorece Rara sobre rarezas altas`() {
        var rare = 0
        var ultraPlus = 0
        repeat(5000) { seed ->
            val cards = opener.open(RarityWeights.STANDARD_PACK, pool(), Random(seed.toLong()))
            val best = cards.maxBy { it.rarity.ordinal }
            if (best.rarity == Rarity.RARE) rare++
            if (best.rarity.ordinal >= Rarity.ULTRA_RARE.ordinal) ultraPlus++
        }
        // Rara como mejor carta debe ser mucho más frecuente que ultra+.
        assertTrue(rare > ultraPlus, "rare=$rare ultraPlus=$ultraPlus")
    }
}
