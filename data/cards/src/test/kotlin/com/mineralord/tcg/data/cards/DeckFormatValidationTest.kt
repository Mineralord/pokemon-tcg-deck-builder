package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.MatchFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Legalidad de baraja por formato (filtro de pool por serie) contra el catálogo real. */
class DeckFormatValidationTest {

    private val repo = CardRepository.load()
    private val sv = repo.all.first { it.set.series == "Scarlet & Violet" && it !is BasicEnergy }
    private val me = repo.all.first { it.set.series == "Mega Evolution" && it !is BasicEnergy }

    private fun deckOf(ids: List<com.mineralord.tcg.engine.model.CardId>) =
        Deck("t", "t", me.id, EnergyType.COLORLESS, entries = ids.map { DeckEntry(it, 1) })

    @Test
    fun `illegalCards marca el Pokemon de otra serie en Standard y nada en Expandido`() {
        val deck = deckOf(listOf(sv.id, me.id))
        assertEquals(listOf(sv.id), DeckValidation.illegalCards(deck, repo, MatchFormat.Standard("Mega Evolution")))
        assertTrue(DeckValidation.illegalCards(deck, repo, MatchFormat.Expanded).isEmpty())
    }

    @Test
    fun `availableSeries incluye las series del catalogo`() {
        val series = DeckValidation.availableSeries(repo)
        assertTrue("Scarlet & Violet" in series)
        assertTrue("Mega Evolution" in series)
    }

    @Test
    fun `validate con formato anade la razon de cartas no permitidas`() {
        val v = DeckValidation.validate(deckOf(listOf(sv.id, me.id)), repo, MatchFormat.Standard("Mega Evolution"))
        assertFalse(v.valid)
        assertTrue(v.reasons.any { it.contains("no permitidas") }, v.reasons.toString())
    }
}
