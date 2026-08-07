package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.BasicEnergy

/**
 * Registro de barajas temáticas / preconstruidas (Academia de Combate y futuras).
 *
 * Las preconstruidas se identifican por id: viven en su propia pestaña
 * ("PRECONSTRUIDAS"), agrupadas por [folder], y NO se pueden editar ni borrar.
 * En el futuro se irán añadiendo más y solo serán jugables si el jugador posee
 * todas las cartas necesarias (ver [ownsAll]).
 */
object PrebuiltDecks {

    const val ACADEMIA_2024 = "Academia de Combate 2024"

    /** id de baraja -> carpeta a la que pertenece. */
    val folderById: Map<String, String> = StarterDecks.ALL.associate { it.id to it.folder }

    /** ¿Es una baraja preconstruida (no editable/borrable)? */
    fun isPrebuilt(deckId: String): Boolean = deckId in folderById

    /** Carpeta de una baraja preconstruida (null si no lo es). */
    fun folderOf(deckId: String): String? = folderById[deckId]

    /**
     * ¿El jugador posee todas las cartas necesarias para esta baraja? Las energías
     * básicas son ilimitadas (no cuentan), igual que en la validación de baraja.
     */
    fun ownsAll(deck: Deck, owned: Map<String, Int>, repo: CardRepository): Boolean =
        deck.entries.all { e ->
            repo[e.cardId] is BasicEnergy || (owned[e.cardId.raw] ?: 0) >= e.count
        }
}
