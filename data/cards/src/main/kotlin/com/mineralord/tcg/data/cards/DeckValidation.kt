package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.PokemonCard

/** Resultado de validar una baraja: si es legal y los motivos si no lo es. */
data class DeckValidity(val valid: Boolean, val reasons: List<String>)

/**
 * Validación de baraja para formato Estándar (simplificada):
 *  - exactamente 60 cartas,
 *  - máximo 4 copias por carta, salvo energía básica (ilimitada),
 *  - al menos un Pokémon Básico.
 *
 * Requiere el [CardRepository] para conocer el tipo de cada carta. Es Kotlin
 * puro → testeable a velocidad de unidad.
 */
object DeckValidation {
    const val DECK_SIZE = 60
    const val MAX_COPIES = 4

    fun validate(deck: Deck, repo: CardRepository): DeckValidity {
        val reasons = mutableListOf<String>()

        if (deck.totalCards != DECK_SIZE) {
            reasons += "La baraja debe tener $DECK_SIZE cartas (tiene ${deck.totalCards})."
        }

        deck.entries.forEach { e ->
            val card = repo[e.cardId]
            val isBasicEnergy = card is BasicEnergy
            if (!isBasicEnergy && e.count > MAX_COPIES) {
                val nm = card?.name?.es ?: e.cardId.raw
                reasons += "Máximo $MAX_COPIES copias de \"$nm\" (tiene ${e.count})."
            }
        }

        val hasBasic = deck.entries.any { e -> (repo[e.cardId] as? PokemonCard)?.isBasic == true }
        if (!hasBasic) reasons += "Necesitas al menos un Pokémon Básico."

        return DeckValidity(reasons.isEmpty(), reasons)
    }
}
