package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard

/**
 * Baraja genérica (starter o personalizada), apta para persistir.
 *
 * Reusa [DeckEntry] (carta + nº de copias) de [StarterDecks]. A diferencia de
 * [StarterDeck], guarda el [type] dominante (para tintar la "caja" en la UI) y
 * si es una baraja personalizada por el jugador ([isCustom]). [updatedAt] (epoch
 * ms) ordena la pestaña RECIENTES del gestor.
 */
data class Deck(
    val id: String,
    val name: String,
    val headliner: com.mineralord.tcg.engine.model.CardId,
    val type: EnergyType,
    val entries: List<DeckEntry>,
    val isCustom: Boolean = false,
    val updatedAt: Long = 0L,
) {
    val totalCards: Int get() = entries.sumOf { it.count }

    fun expandedCardIds() = entries.flatMap { e -> List(e.count) { e.cardId } }
}

/** Convierte una baraja preconstruida en el modelo genérico persistible. */
fun StarterDeck.toDeck(): Deck = Deck(
    id = id,
    name = name,
    headliner = headliner,
    type = dominantType(entries),
    entries = entries,
    isCustom = false,
)

/**
 * Tipo dominante de una baraja: la energía básica con más copias (las barajas
 * mono-tipo de Academia de Combate tienen 18 de un solo tipo). Se infiere del id
 * de la energía básica para no requerir el catálogo. Fallback: [EnergyType.COLORLESS].
 */
fun dominantType(entries: List<DeckEntry>): EnergyType {
    val byType = HashMap<EnergyType, Int>()
    for (e in entries) {
        val t = basicEnergyType(e.cardId.raw) ?: continue
        byType[t] = (byType[t] ?: 0) + e.count
    }
    return byType.maxByOrNull { it.value }?.key ?: EnergyType.COLORLESS
}

/**
 * Tipo dominante usando el catálogo: el tipo de Pokémon más repetido (ponderado
 * por copias). Para mazos personalizados, donde la energía básica puede faltar al
 * principio. Cae a [dominantType] por energía básica y, en último término, COLORLESS.
 */
fun dominantType(entries: List<DeckEntry>, repo: CardRepository): EnergyType {
    val byType = HashMap<EnergyType, Int>()
    for (e in entries) {
        val poke = repo[e.cardId] as? PokemonCard ?: continue
        val t = poke.types.firstOrNull() ?: continue
        byType[t] = (byType[t] ?: 0) + e.count
    }
    return byType.maxByOrNull { it.value }?.key ?: dominantType(entries)
}

private fun basicEnergyType(id: String): EnergyType? = when {
    id.contains("grass") -> EnergyType.GRASS
    id.contains("fire") -> EnergyType.FIRE
    id.contains("water") -> EnergyType.WATER
    id.contains("lightning") -> EnergyType.LIGHTNING
    id.contains("psychic") -> EnergyType.PSYCHIC
    id.contains("fighting") -> EnergyType.FIGHTING
    id.contains("darkness") -> EnergyType.DARKNESS
    id.contains("metal") -> EnergyType.METAL
    id.contains("fairy") -> EnergyType.FAIRY
    id.contains("dragon") -> EnergyType.DRAGON
    else -> null
}
