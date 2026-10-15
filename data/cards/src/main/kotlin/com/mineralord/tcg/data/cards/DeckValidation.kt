package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.FormatLegality
import com.mineralord.tcg.engine.model.MatchFormat
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.TrainerCard

/** Resultado de validar una baraja: si es legal y los motivos si no lo es. */
data class DeckValidity(val valid: Boolean, val reasons: List<String>)

/**
 * Validación de baraja para formato Estándar (simplificada):
 *  - exactamente 60 cartas,
 *  - máximo 4 copias por NOMBRE de carta, salvo energía básica (ilimitada),
 *  - al menos un Pokémon Básico,
 *  - como máximo 1 carta ACE SPEC en total (regla oficial: solo 1 por baraja,
 *    contando TODAS las ACE SPEC juntas, no por nombre).
 *
 * Regla oficial (Web Rulebook 2026, pág. 22): el límite de 4 copias se cuenta
 * por NOMBRE, no por versión/expansión. Así, 3 "Charmander" de un set + 2 de
 * otro son 5 "Charmander" → ilegal. En cambio "Charizard" y "Charizard ex" son
 * nombres DISTINTOS (el sufijo es parte del nombre) → 4 de cada uno es legal.
 * Por eso agrupamos por [name.en], que incluye el sufijo (ex/V/…).
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

        // Sumar copias por NOMBRE (agrupando distintas versiones/expansiones de la
        // misma carta). Las energías básicas están exentas del límite.
        deck.entries
            .mapNotNull { e -> repo[e.cardId]?.let { it to e.count } }
            .filter { (card, _) -> card !is BasicEnergy }
            .groupBy { (card, _) -> card.name.en }
            .forEach { (nameEn, group) ->
                val total = group.sumOf { it.second }
                if (total > MAX_COPIES) {
                    val display = group.first().first.name.es.ifBlank { nameEn }
                    reasons += "Máximo $MAX_COPIES copias de \"$display\" (tiene $total)."
                }
            }

        val hasBasic = deck.entries.any { e -> (repo[e.cardId] as? PokemonCard)?.isBasic == true }
        if (!hasBasic) reasons += "Necesitas al menos un Pokémon Básico."

        // Regla ACE SPEC: como mucho 1 en total. Hoy solo las cartas de Entrenador exponen
        // `isAceSpec`; una Energía Especial ACE SPEC (p. ej. Neo Upper Energy) necesitaría
        // soporte en el modelo — deuda anotada.
        val aceSpecCount = deck.entries.sumOf { e ->
            if ((repo[e.cardId] as? TrainerCard)?.kind?.isAceSpec == true) e.count else 0
        }
        if (aceSpecCount > 1) {
            reasons += "Solo puedes incluir 1 carta ACE SPEC por baraja (tiene $aceSpecCount)."
        }

        return DeckValidity(reasons.isEmpty(), reasons)
    }

    /**
     * Valida la baraja y, ADEMÁS, su legalidad en [format] (filtro de pool por serie). Expandido
     * acepta todas las series; Estándar solo la serie elegida. La Energía Básica está exenta
     * (legal en cualquier formato) — lo decide [FormatLegality].
     */
    fun validate(deck: Deck, repo: CardRepository, format: MatchFormat): DeckValidity {
        val base = validate(deck, repo)
        val reasons = base.reasons.toMutableList()
        val illegal = deck.entries.mapNotNull { repo[it.cardId] }
            .filterNot { FormatLegality.isLegal(it, format) }
            .distinctBy { it.name.en }
        if (illegal.isNotEmpty()) {
            val names = illegal.joinToString(", ") { it.name.es.ifBlank { it.name.en } }
            reasons += "Cartas no permitidas en ${formatLabel(format)}: $names."
        }
        return DeckValidity(reasons.isEmpty(), reasons)
    }

    /** Ids de las cartas de la baraja NO legales en [format] (para marcarlas en la UI). */
    fun illegalCards(deck: Deck, repo: CardRepository, format: MatchFormat): List<CardId> =
        deck.entries.map { it.cardId }
            .filter { id -> repo[id]?.let { !FormatLegality.isLegal(it, format) } ?: false }
            .distinct()

    /** Series disponibles en el catálogo, para poblar el selector de formato Estándar. */
    fun availableSeries(repo: CardRepository): List<String> =
        repo.all.map { it.set.series }.distinct().sorted()

    fun formatLabel(format: MatchFormat): String = when (format) {
        MatchFormat.Expanded -> "Expandido"
        is MatchFormat.Standard -> "Estándar · ${format.series}"
    }
}
