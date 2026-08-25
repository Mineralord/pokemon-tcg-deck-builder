package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.Stage
import com.mineralord.tcg.engine.model.Supertype
import com.mineralord.tcg.engine.model.PokemonMechanic

/** Característica filtrable (subconjunto fiel del panel de TCG Live). */
enum class Characteristic { HAS_ABILITY, POKEMON_EX, BASIC, STAGE_1, STAGE_2 }

/** Método de ordenación (espejo del panel ORDENAR). */
enum class SortMethod { EVOLUTION_CHAIN, ALPHABETICAL, EXPANSION, ENERGY_TYPE }

/**
 * Filtro de cartas. Un conjunto vacío = "sin restricción" en esa faceta. La UI
 * lo construye desde el panel FILTROS; es Kotlin puro y testeable.
 */
data class CardFilter(
    val supertypes: Set<Supertype> = emptySet(),
    val energyTypes: Set<EnergyType> = emptySet(),      // tipos del Pokémon
    val weaknesses: Set<EnergyType> = emptySet(),
    val expansions: Set<String> = emptySet(),           // códigos de set
    val characteristics: Set<Characteristic> = emptySet(),
) {
    val isEmpty: Boolean
        get() = supertypes.isEmpty() && energyTypes.isEmpty() && weaknesses.isEmpty() &&
            expansions.isEmpty() && characteristics.isEmpty()

    fun matches(card: Card): Boolean {
        if (supertypes.isNotEmpty() && card.supertype !in supertypes) return false
        if (expansions.isNotEmpty() && card.set.code !in expansions) return false

        val poke = card as? PokemonCard
        if (energyTypes.isNotEmpty()) {
            if (poke == null || poke.types.none { it in energyTypes }) return false
        }
        if (weaknesses.isNotEmpty()) {
            if (poke == null || poke.weaknesses.none { it.type in weaknesses }) return false
        }
        if (characteristics.isNotEmpty()) {
            if (!characteristics.all { hasCharacteristic(card, it) }) return false
        }
        return true
    }

    private fun hasCharacteristic(card: Card, c: Characteristic): Boolean {
        val poke = card as? PokemonCard ?: return false
        return when (c) {
            Characteristic.HAS_ABILITY -> poke.abilities.isNotEmpty()
            Characteristic.POKEMON_EX -> poke.mechanic is PokemonMechanic.ExLower ||
                poke.mechanic is PokemonMechanic.ExUpper || poke.mechanic is PokemonMechanic.Tera
            Characteristic.BASIC -> poke.stage is Stage.Basic
            Characteristic.STAGE_1 -> poke.stage is Stage.Stage1
            Characteristic.STAGE_2 -> poke.stage is Stage.Stage2
        }
    }
}

/** Ordenación: método + dirección (false = ascendente / Básico→Ev.Final). */
data class CardSort(val method: SortMethod = SortMethod.EVOLUTION_CHAIN, val descending: Boolean = false)

private fun stageRank(card: Card): Int = when ((card as? PokemonCard)?.stage) {
    Stage.Basic -> 0
    Stage.Stage1 -> 1
    Stage.Stage2 -> 2
    Stage.BabyRestored -> 0
    null -> 3 // entrenadores/energías al final en cadena de evolución
}

private fun typeRank(card: Card): Int =
    (card as? PokemonCard)?.types?.firstOrNull()?.ordinal ?: EnergyType.entries.size

/** Aplica filtro + orden. Función pura: la UI la consume, los tests la cubren. */
fun applyFilterSort(cards: List<Card>, filter: CardFilter, sort: CardSort): List<Card> {
    val filtered = if (filter.isEmpty) cards else cards.filter { filter.matches(it) }
    val comparator: Comparator<Card> = when (sort.method) {
        SortMethod.ALPHABETICAL -> compareBy { it.name.es.lowercase() }
        SortMethod.EXPANSION -> compareBy({ it.set.code }, { it.name.es.lowercase() })
        SortMethod.ENERGY_TYPE -> compareBy({ typeRank(it) }, { it.name.es.lowercase() })
        SortMethod.EVOLUTION_CHAIN -> compareBy({ stageRank(it) }, { it.name.es.lowercase() })
    }
    val sorted = filtered.sortedWith(comparator)
    return if (sort.descending) sorted.reversed() else sorted
}
