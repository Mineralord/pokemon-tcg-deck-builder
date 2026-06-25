package com.mineralord.tcg.data.cards

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StarterDecksTest {

    private val repo = CardRepository.load()

    @Test
    fun `hay tres barajas desbloqueadas`() {
        assertEquals(3, StarterDecks.ALL.size)
        assertEquals(
            listOf("Pikachu ex", "Armarouge ex", "Darkrai ex"),
            StarterDecks.ALL.map { it.name },
        )
    }

    @Test
    fun `cada baraja tiene exactamente 60 cartas`() {
        StarterDecks.ALL.forEach { deck ->
            assertEquals(60, deck.totalCards, "La baraja ${deck.name} no tiene 60 cartas")
        }
    }

    @Test
    fun `todas las cartas de las barajas existen en el catalogo`() {
        StarterDecks.ALL.forEach { deck ->
            deck.entries.forEach { entry ->
                assertTrue(
                    repo[entry.cardId] != null,
                    "Falta en el catálogo: ${entry.cardId.raw} (baraja ${deck.name})",
                )
            }
        }
    }

    @Test
    fun `el cabecera de cada baraja es su Pokemon ex`() {
        StarterDecks.ALL.forEach { deck ->
            assertTrue(repo[deck.headliner] != null, "Cabecera ausente en ${deck.name}")
        }
    }
}
