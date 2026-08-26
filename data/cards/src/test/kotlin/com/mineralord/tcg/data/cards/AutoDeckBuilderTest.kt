package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class AutoDeckBuilderTest {

    private val repo = CardRepository.load()
    // Simula un jugador que posee 4 copias de cada carta del set 151.
    private val owned: Map<String, Int> = repo.all.associate { it.id.raw to 4 }

    private fun buildFor(focus: Set<EnergyType>, seed: Long): List<DeckEntry> =
        AutoDeckBuilder.build(owned, repo.all, focus, Random(seed))

    @Test
    fun `siempre produce exactamente 60 cartas`() {
        for (seed in 0L until 30L) {
            val deck = buildFor(setOf(EnergyType.FIRE), seed)
            assertTrue(deck.sumOf { it.count } == 60, "seed=$seed total=${deck.sumOf { it.count }}")
        }
    }

    @Test
    fun `respeta el maximo de 4 copias por nombre salvo energia basica`() {
        val deck = buildFor(setOf(EnergyType.WATER), 7)
        val byName = HashMap<String, Int>()
        for (e in deck) {
            val c = repo[e.cardId]!!
            if (c is BasicEnergy) continue
            byName[c.name.en] = (byName[c.name.en] ?: 0) + e.count
        }
        assertTrue(byName.values.all { it <= 4 }, "copias por nombre: $byName")
    }

    @Test
    fun `incluye al menos un Pokemon Basico`() {
        val deck = buildFor(setOf(EnergyType.PSYCHIC), 3)
        val hasBasic = deck.any { (repo[it.cardId] as? PokemonCard)?.isBasic == true }
        assertTrue(hasBasic)
    }

    @Test
    fun `respeta rangos competitivos de categoria pkmn trainer energia`() {
        // Con colección completa deben cumplirse los rangos de algún perfil.
        val deck = buildFor(setOf(EnergyType.LIGHTNING), 11)
        var pk = 0; var tr = 0; var en = 0
        for (e in deck) when {
            repo[e.cardId] is PokemonCard -> pk += e.count
            repo[e.cardId] is BasicEnergy -> en += e.count
            else -> tr += e.count
        }
        assertTrue(pk in 10..24, "pokemon=$pk")
        assertTrue(en in 6..24, "energia=$en")
        assertTrue(tr in 14..42, "trainer=$tr")
    }

    @Test
    fun `distintas semillas dan mazos distintos (variedad)`() {
        val a = buildFor(setOf(EnergyType.FIRE), 1).associate { it.cardId.raw to it.count }
        val b = buildFor(setOf(EnergyType.FIRE), 999).associate { it.cardId.raw to it.count }
        assertTrue(a != b, "las semillas deberían producir mazos distintos")
    }
}
