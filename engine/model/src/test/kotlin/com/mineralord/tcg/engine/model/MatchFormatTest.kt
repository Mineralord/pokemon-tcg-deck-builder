package com.mineralord.tcg.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Formato = filtro de pool sobre un ruleset único. Verifica la legalidad por serie y que el perfil
 * de reglas activo sea Modern en todo el catálogo actual (el seam pre-BW no altera nada hoy).
 */
class MatchFormatTest {

    private fun setInfo(series: String) = SetInfo("code", LocalizedText("N", "N"), series)

    private fun energy(id: String, series: String) = BasicEnergy(
        CardId(id), LocalizedText("Energía", "Energy"), setInfo(series),
        Rarity.COMMON, null, ArtworkRefs(null, null, "s", "l"), EnergyType.WATER,
    )

    private fun pokemon(id: String, series: String) = PokemonCard(
        id = CardId(id), name = LocalizedText(id, id), set = setInfo(series),
        rarity = Rarity.COMMON, regulationMark = "H", artwork = ArtworkRefs(null, null, "s", "l"),
        stage = Stage.Basic, mechanic = PokemonMechanic.Normal, hp = 60,
        types = listOf(EnergyType.WATER), evolvesFrom = null, abilities = emptyList(),
        attacks = emptyList(), weaknesses = emptyList(),
        resistances = emptyList(), retreatCost = emptyList(), rulesText = emptyList(),
    )

    @Test
    fun `Expanded permite cualquier serie`() {
        assertTrue(FormatLegality.isLegal(setInfo("Scarlet & Violet"), MatchFormat.Expanded))
        assertTrue(FormatLegality.isLegal(setInfo("Mega Evolution"), MatchFormat.Expanded))
    }

    @Test
    fun `Standard solo permite la serie elegida`() {
        val mega = MatchFormat.Standard("Mega Evolution")
        assertTrue(FormatLegality.isLegal(setInfo("Mega Evolution"), mega))
        assertFalse(FormatLegality.isLegal(setInfo("Scarlet & Violet"), mega))
    }

    @Test
    fun `la Energia Basica es legal en cualquier formato`() {
        val mega = MatchFormat.Standard("Mega Evolution")
        assertTrue(FormatLegality.isLegal(energy("e", "Scarlet & Violet"), mega))  // serie distinta, pero es Básica
    }

    @Test
    fun `illegalCards lista los Pokemon fuera de la serie Standard (Energia exenta)`() {
        val deck = listOf(
            pokemon("a", "Mega Evolution"),
            pokemon("b", "Scarlet & Violet"),   // fuera de serie → ilegal
            energy("e", "Scarlet & Violet"),    // Básica → legal aunque sea de otra serie
        )
        val mega = MatchFormat.Standard("Mega Evolution")
        assertEquals(listOf(CardId("b")), FormatLegality.illegalCards(deck, mega))
        assertFalse(FormatLegality.isDeckLegal(deck, mega))
        assertTrue(FormatLegality.isDeckLegal(deck, MatchFormat.Expanded))
    }

    @Test
    fun `encode y decode hacen round-trip`() {
        listOf(MatchFormat.Expanded, MatchFormat.Standard("Mega Evolution"), MatchFormat.Standard("Scarlet & Violet"))
            .forEach { assertEquals(it, decodeMatchFormat(encodeMatchFormat(it))) }
        assertEquals(MatchFormat.Expanded, decodeMatchFormat(""))       // vacío → Expandido
        assertEquals(MatchFormat.Expanded, decodeMatchFormat(null))     // nulo → Expandido
    }

    @Test
    fun `el perfil de reglas activo es Modern en todo el catalogo actual`() {
        assertEquals(RuleProfiles.Modern, RuleProfiles.forFormat(MatchFormat.Expanded))
        assertEquals(RuleProfiles.Modern, RuleProfiles.forFormat(MatchFormat.Standard("Mega Evolution")))
        assertEquals(RuleProfiles.Modern, RuleProfiles.forSeries("Scarlet & Violet"))
        assertEquals(WeaknessMode.TIMES_TWO, RuleProfiles.Modern.weakness)
    }
}
