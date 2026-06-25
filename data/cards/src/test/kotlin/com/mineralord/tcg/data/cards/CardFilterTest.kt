package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.Supertype
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardFilterTest {

    private val repo = CardRepository.load()
    // Pool de prueba: cartas del set 151 (mezcla de Pokémon/Entrenador/Energía).
    private val pool = repo.all.filter { it.id.raw.startsWith("sv3pt5-") }

    @Test
    fun `filtro por supertipo deja solo Pokemon`() {
        val f = CardFilter(supertypes = setOf(Supertype.POKEMON))
        val out = applyFilterSort(pool, f, CardSort())
        assertTrue(out.isNotEmpty())
        assertTrue(out.all { it.supertype == Supertype.POKEMON })
    }

    @Test
    fun `filtro por tipo de energia deja solo ese tipo`() {
        val f = CardFilter(energyTypes = setOf(EnergyType.FIRE))
        val out = applyFilterSort(pool, f, CardSort())
        assertTrue(out.isNotEmpty())
        assertTrue(out.all { (it as? PokemonCard)?.types?.contains(EnergyType.FIRE) == true })
    }

    @Test
    fun `filtro por expansion respeta el codigo de set`() {
        val code = pool.first().set.code   // el modelo guarda el nombre del set como "code"
        val f = CardFilter(expansions = setOf(code))
        val out = applyFilterSort(repo.all, f, CardSort())
        assertTrue(out.isNotEmpty())
        assertTrue(out.all { it.set.code == code })
    }

    @Test
    fun `caracteristica BASIC deja solo basicos`() {
        val f = CardFilter(characteristics = setOf(Characteristic.BASIC))
        val out = applyFilterSort(pool, f, CardSort())
        assertTrue(out.isNotEmpty())
        assertTrue(out.all { (it as PokemonCard).isBasic })
    }

    @Test
    fun `orden alfabetico ascendente y descendente`() {
        val asc = applyFilterSort(pool, CardFilter(), CardSort(SortMethod.ALPHABETICAL, descending = false))
        val desc = applyFilterSort(pool, CardFilter(), CardSort(SortMethod.ALPHABETICAL, descending = true))
        assertEquals(asc.map { it.name.es }, desc.map { it.name.es }.reversed())
        val names = asc.map { it.name.es.lowercase() }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun `filtro vacio devuelve todo`() {
        val out = applyFilterSort(pool, CardFilter(), CardSort())
        assertEquals(pool.size, out.size)
    }

    @Test
    fun `dominantType por catalogo infiere el tipo de los Pokemon`() {
        assertEquals(EnergyType.LIGHTNING, dominantType(StarterDecks.PIKACHU.entries, repo))
        assertEquals(EnergyType.FIRE, dominantType(StarterDecks.ARMAROUGE.entries, repo))
        assertEquals(EnergyType.DARKNESS, dominantType(StarterDecks.DARKRAI.entries, repo))
    }
}
