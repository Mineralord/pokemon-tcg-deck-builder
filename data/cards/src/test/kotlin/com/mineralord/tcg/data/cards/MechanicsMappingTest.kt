package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.AbilityKind
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.PokemonCard
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifica el modelo de mecánicas por época: clasificación Habilidad / Poké-Power /
 * Poké-Body y la localización ES de habilidades desde el bloque `es.habilidades`.
 */
class MechanicsMappingTest {

    @Test
    fun `abilityKind clasifica por etiqueta de epoca (con variantes de acento y guion)`() {
        assertEquals(AbilityKind.ABILITY, CardMapper.abilityKind("Ability"))
        assertEquals(AbilityKind.ABILITY, CardMapper.abilityKind(null))
        assertEquals(AbilityKind.POKE_POWER, CardMapper.abilityKind("Poké-Power"))
        assertEquals(AbilityKind.POKE_POWER, CardMapper.abilityKind("Poke Power"))
        assertEquals(AbilityKind.POKE_POWER, CardMapper.abilityKind("Pokémon Power"))
        assertEquals(AbilityKind.POKE_BODY, CardMapper.abilityKind("Poké-Body"))
    }

    @Test
    fun `Gyarados 151 mapea su habilidad como ABILITY con nombre ES localizado`() {
        val gyarados = CardRepository.load()[CardId("sv3pt5-130")] as PokemonCard
        val abi = gyarados.abilities.single()
        assertEquals("Untamed One", abi.name.en)
        assertEquals("Indomable", abi.name.es)
        assertEquals(AbilityKind.ABILITY, abi.kind)   // 151 es moderno: todo Habilidad
    }
}
