package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.TrainerCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeckValidationTest {

    private val repo = CardRepository.load()

    @Test
    fun `las tres barajas starter son validas`() {
        StarterDecks.ALL.forEach { starter ->
            val v = DeckValidation.validate(starter.toDeck(), repo)
            assertTrue(v.valid, "La baraja ${starter.name} debería ser válida: ${v.reasons}")
        }
    }

    @Test
    fun `toDeck infiere el tipo dominante por la energia basica`() {
        assertEquals(EnergyType.LIGHTNING, StarterDecks.PIKACHU.toDeck().type)
        assertEquals(EnergyType.FIRE, StarterDecks.ARMAROUGE.toDeck().type)
        assertEquals(EnergyType.DARKNESS, StarterDecks.DARKRAI.toDeck().type)
    }

    @Test
    fun `una baraja de menos de 60 cartas es invalida`() {
        val deck = StarterDecks.PIKACHU.toDeck().let {
            it.copy(entries = it.entries.drop(1))
        }
        val v = DeckValidation.validate(deck, repo)
        assertFalse(v.valid)
        assertTrue(v.reasons.any { it.contains("60 cartas") })
    }

    @Test
    fun `el limite de 4 copias se cuenta por nombre entre distintas versiones`() {
        // Dos ids distintos con el MISMO nombre (reimpresiones/artes alternativos).
        val byName = repo.all
            .filterNot { it is com.mineralord.tcg.engine.model.BasicEnergy }
            .groupBy { it.name.en }
            .values.first { it.size >= 2 }
        val idA = byName[0].id
        val idB = byName[1].id

        // 3 de una versión + 2 de otra = 5 cartas del mismo nombre → ilegal,
        // aunque ninguna entrada por separado supere las 4 copias.
        val base = StarterDecks.PIKACHU.toDeck()
        val entries = base.entries
            .filter { it.cardId != idA && it.cardId != idB }
            .toMutableList()
        entries += DeckEntry(idA, 3)
        entries += DeckEntry(idB, 2)
        val deck = base.copy(entries = entries)

        val v = DeckValidation.validate(deck, repo)
        assertFalse(v.valid)
        assertTrue(v.reasons.any { it.contains("Máximo") }, "Debe rechazar 5 copias del mismo nombre: ${v.reasons}")
    }

    @Test
    fun `mas de 4 copias de una carta no basica es invalida`() {
        // Toma una carta Pokémon/entrenador de la baraja y fuerza 5 copias.
        val starter = StarterDecks.PIKACHU
        val nonEnergy = starter.entries.first { !it.cardId.raw.contains("energy-basic") }
        val bumped = starter.entries.map {
            if (it.cardId == nonEnergy.cardId) it.copy(count = 5) else it
        }
        val deck = starter.toDeck().copy(entries = bumped)
        val v = DeckValidation.validate(deck, repo)
        assertFalse(v.valid)
        assertTrue(v.reasons.any { it.contains("Máximo") })
    }

    @Test
    fun `mas de 1 carta ACE SPEC es invalida`() {
        val aces = repo.all.filterIsInstance<TrainerCard>().filter { it.kind.isAceSpec }
        assertTrue(aces.isNotEmpty(), "el catálogo debería incluir alguna carta ACE SPEC")
        // 2 ACE SPEC en total → ilegal, aunque sean nombres distintos (la regla cuenta todas juntas).
        val entries = if (aces.size >= 2)
            listOf(DeckEntry(aces[0].id, 1), DeckEntry(aces[1].id, 1))
        else listOf(DeckEntry(aces[0].id, 2))
        val deck = StarterDecks.PIKACHU.toDeck().copy(entries = entries)

        val v = DeckValidation.validate(deck, repo)
        assertFalse(v.valid)
        assertTrue(v.reasons.any { it.contains("ACE SPEC") }, "Debe rechazar 2 ACE SPEC: ${v.reasons}")
    }
}
