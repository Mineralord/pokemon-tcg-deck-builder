package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.EnergyType
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
}
