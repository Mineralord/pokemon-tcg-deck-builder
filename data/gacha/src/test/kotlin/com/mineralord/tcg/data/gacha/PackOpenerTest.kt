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

    // ---------------------------- SOBRE FIEL DE 151 ----------------------------

    private val energyIds = listOf(
        "grass", "fire", "water", "lightning", "psychic", "fighting", "darkness", "metal",
    ).map { CardId("energy-basic-$it-energy") }

    @Test
    fun `el sobre de 151 entrega 10 cartas incluida 1 Energia Basica`() {
        // Desglose oficial del sobre 151: 4 Comunes + 3 Infrecuentes + 1 Reverse + 1 Rara+ + 1 Energía.
        val cards = opener.open(RarityWeights.SET_151_PACK, pool(), Random(3), energyIds)
        assertEquals(10, cards.size)
        // La última carta es la Energía Básica del slot dedicado.
        val energy = cards.last()
        assertTrue(energy.id in energyIds, "la última carta debe ser una Energía Básica")
    }

    @Test
    fun `el sobre de 151 siempre trae una Rara o superior en el slot hit`() {
        repeat(300) { seed ->
            val cards = opener.open(RarityWeights.SET_151_PACK, pool(), Random(seed.toLong()), energyIds)
            assertTrue(
                cards.any { it.rarity.ordinal >= Rarity.RARE.ordinal },
                "El sobre 151 con semilla $seed no tuvo ninguna carta rara+",
            )
        }
    }

    @Test
    fun `el slot de energia no se pisa aunque el pool carezca de rarezas altas`() {
        // Pool sin nada ≥ Rara: la garantía sustituye el PRIMER slot, jamás la Energía Básica final.
        val poorPool = PackPool.ofIds(
            mapOf(
                Rarity.COMMON to (1..5).map { CardId("c$it") },
                Rarity.UNCOMMON to (1..5).map { CardId("u$it") },
            ),
        )
        val cards = opener.open(RarityWeights.SET_151_PACK, poorPool, Random(9), energyIds)
        assertEquals(10, cards.size)
        assertTrue(cards.last().id in energyIds, "la Energía Básica debe conservarse")
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
