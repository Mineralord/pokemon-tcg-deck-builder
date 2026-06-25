package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Stage
import com.mineralord.tcg.engine.model.Supertype
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CardRepositoryTest {

    private val repo = CardRepository.load()

    @Test
    fun `carga el dataset completo (487 base + cartas de los mazos)`() {
        // 487 originales + 29 cartas de Battle Academy + Basic Fire Energy.
        assertEquals(517, repo.size)
    }

    @Test
    fun `todas las cartas tienen id unico`() {
        assertEquals(repo.size, repo.all.map { it.id }.toSet().size)
    }

    @Test
    fun `cubre los tres supertipos`() {
        val supertipos = repo.all.map { it.supertype }.toSet()
        assertEquals(setOf(Supertype.POKEMON, Supertype.TRAINER, Supertype.ENERGY), supertipos)
    }

    @Test
    fun `mapea Pikachu ex correctamente`() {
        val card = repo[CardId("svp-106")]
        assertNotNull(card)
        assertTrue(card is PokemonCard)
        card as PokemonCard
        assertEquals(200, card.hp)
        assertEquals(Stage.Basic, card.stage)
        assertEquals(PokemonMechanic.ExLower, card.mechanic)
        assertEquals(2, card.prizeValue)              // ex => 2 premios
        assertTrue(EnergyType.LIGHTNING in card.types)
        assertTrue(card.attacks.isNotEmpty())
    }

    @Test
    fun `los Pokemon tienen PS positivo y al menos un tipo`() {
        val pokemon = repo.all.filterIsInstance<PokemonCard>()
        assertTrue(pokemon.isNotEmpty())
        assertTrue(pokemon.all { it.hp > 0 }, "Hay Pokémon con PS no positivo")
        assertTrue(pokemon.all { it.types.isNotEmpty() }, "Hay Pokémon sin tipo")
    }

    @Test
    fun `los Entrenadores se clasifican en categorias conocidas`() {
        val trainers = repo.all.filterIsInstance<TrainerCard>()
        assertTrue(trainers.isNotEmpty())
        // Debe haber al menos un Partidario y un Objeto en el dataset.
        assertTrue(trainers.any { it.kind is TrainerKind.Supporter })
        assertTrue(trainers.any { it.kind is TrainerKind.Item })
    }
}
