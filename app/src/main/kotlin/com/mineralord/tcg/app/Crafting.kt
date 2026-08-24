package com.mineralord.tcg.app

import com.mineralord.tcg.data.profile.CraftingRules
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Rarity
import com.mineralord.tcg.engine.model.TrainerCard

// Helpers compartidos del Sistema de Fabricación/Reciclaje (Canon Fase 5). La rareza del motor
// se mapea al ESCALÓN económico I..IX (§3.2); las cantidades viven en data:profile/CraftingRules.

/** Escalón económico I..IX del Canon (§3.2) para una rareza del motor. */
internal fun tierOf(r: Rarity): Int = when (r) {
    Rarity.COMMON -> 1
    Rarity.UNCOMMON -> 2
    Rarity.RARE -> 3
    Rarity.RARE_HOLO -> 4
    Rarity.DOUBLE_RARE -> 5          // Pokémon ex (Premium Base)
    Rarity.ULTRA_RARE -> 6           // V / Full Art
    Rarity.ILLUSTRATION_RARE -> 7
    Rarity.SPECIAL_ILLUSTRATION_RARE -> 8
    Rarity.HYPER_RARE -> 9
    Rarity.PROMO -> 1
}

/**
 * Tamaño del PLAYSET de una carta = copias máximas útiles en un mazo (regla oficial): **4** para la
 * mayoría, **1** para singletons (ACE SPEC; Radiante, que también es único por mazo). Es el suelo que
 * la Destrucción conserva y el tope hasta el que se puede Fabricar (Fase 5 §3.6). Las Energías Básicas
 * usan su propio tope de colección ([ProfileRepository.ENERGY_CAP] = 25): se fabrican hasta él y su
 * excedente (26+) se destruye conservando esas 25.
 */
internal fun playsetSize(card: Card): Int = when {
    ProfileRepository.isEnergyId(card.id.raw) -> ProfileRepository.ENERGY_CAP
    card is TrainerCard && card.kind.isAceSpec -> 1
    card is PokemonCard && card.mechanic == PokemonMechanic.Radiant -> 1
    else -> 4
}

/** Fichas para fabricar una copia de [card] (Canon §3.4, por escalón de rareza). */
internal fun craftCostOf(card: Card): Int = CraftingRules.craftCost(tierOf(card.rarity))

/** Fichas obtenidas al destruir un duplicado de [card] (Canon §3.7). */
internal fun destroyValueOf(card: Card): Int = CraftingRules.recycleFichas(tierOf(card.rarity))

/** Nombre en español de la rareza (para fichas/etiquetas). */
internal fun rarityLabelEs(r: Rarity): String = when (r) {
    Rarity.COMMON -> "Común"
    Rarity.UNCOMMON -> "Poco común"
    Rarity.RARE -> "Rara"
    Rarity.RARE_HOLO -> "Rara Holo"
    Rarity.DOUBLE_RARE -> "Doble Rara (ex)"
    Rarity.ULTRA_RARE -> "Ultra Rara"
    Rarity.ILLUSTRATION_RARE -> "Illustration Rare"
    Rarity.SPECIAL_ILLUSTRATION_RARE -> "Special Illustration"
    Rarity.HYPER_RARE -> "Hyper Rara"
    Rarity.PROMO -> "Promo"
}
