package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class AutoDeckBuilderTest {

    private val repo = CardRepository.load()
    // Simula un jugador que posee 4 copias de cada carta del catálogo.
    private val owned: Map<String, Int> = repo.all.associate { it.id.raw to 4 }

    private fun buildFor(focus: Set<EnergyType>, seed: Long, own: Map<String, Int> = owned): List<DeckEntry> =
        AutoDeckBuilder.build(own, repo.all, focus, Random(seed))

    @Test
    fun `siempre produce exactamente 60 cartas`() {
        for (seed in 0L until 30L) {
            val deck = buildFor(setOf(EnergyType.FIRE), seed)
            assertTrue(deck.sumOf { it.count } == 60, "seed=$seed total=${deck.sumOf { it.count }}")
        }
    }

    @Test
    fun `nunca incluye mas copias de las poseidas (ni energia)`() {
        // Poseer sólo 5 de UNA energía de Lucha, 0 del resto de energías de Lucha.
        val fighting = repo.all.filterIsInstance<BasicEnergy>().filter { it.type == EnergyType.FIGHTING }
        assertTrue(fighting.isNotEmpty())
        val own = owned.toMutableMap()
        fighting.forEach { own[it.id.raw] = 0 }
        own[fighting.first().id.raw] = 5
        for (seed in 0L until 20L) {
            val deck = buildFor(setOf(EnergyType.FIGHTING), seed, own)
            for (e in deck) {
                assertTrue(e.count <= (own[e.cardId.raw] ?: 0), "seed=$seed ${e.cardId.raw}: ${e.count} > ${own[e.cardId.raw]}")
            }
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
        assertTrue(deck.any { (repo[it.cardId] as? PokemonCard)?.isBasic == true })
    }

    @Test
    fun `excluye entrenadores de afinidad a un tipo fuera del foco`() {
        // Melo (sv4-167) sólo beneficia a Fuego → NO debe salir en un mazo de Agua…
        val water = buildFor(setOf(EnergyType.WATER), 5)
        assertTrue(water.none { it.cardId.raw == "sv4-167" }, "Melo no debería estar en un mazo de Agua")
        // …pero SÍ es elegible en un mazo de Fuego (si el motor lo selecciona en alguna semilla).
        val fireHasMela = (0L until 40L).any { seed ->
            buildFor(setOf(EnergyType.FIRE), seed).any { it.cardId.raw == "sv4-167" }
        }
        assertTrue(fireHasMela, "Melo debería poder aparecer en un mazo de Fuego")
    }

    @Test
    fun `no rellena con incoloros si hay suficientes Pokemon del foco`() {
        // Con la colección completa (Agua/Lucha tienen Pokémon de sobra), el mazo no debería
        // apoyarse en Pokémon puramente Incoloros.
        val deck = buildFor(setOf(EnergyType.WATER, EnergyType.FIGHTING), 9)
        val colorless = deck.count { e ->
            val p = repo[e.cardId] as? PokemonCard
            p != null && p.types.all { it == EnergyType.COLORLESS }
        }
        assertTrue(colorless == 0, "no debería haber Pokémon Incoloros de relleno (había $colorless)")
    }

    @Test
    fun `distintas semillas dan mazos distintos (variedad)`() {
        val a = buildFor(setOf(EnergyType.FIRE), 1).associate { it.cardId.raw to it.count }
        val b = buildFor(setOf(EnergyType.FIRE), 999).associate { it.cardId.raw to it.count }
        assertTrue(a != b, "las semillas deberían producir mazos distintos")
    }
}
