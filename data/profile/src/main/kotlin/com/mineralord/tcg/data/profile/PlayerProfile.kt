package com.mineralord.tcg.data.profile

import com.mineralord.tcg.data.cards.Deck
import com.mineralord.tcg.data.gacha.DailyPackState

/**
 * Estado persistente del jugador. Inmutable; el repositorio entrega copias.
 *
 * - [owned]: cartas en la colección (id -> nº de copias).
 * - [daily]: estado del límite diario de sobres.
 * - [seeded]: si ya se sembró la colección inicial (mazos desbloqueados).
 * - [decks]: barajas del jugador (starters + personalizadas).
 * - [activeDeckId]: baraja marcada como activa (alimentará el futuro combate).
 * - [favoriteDeckIds]: barajas marcadas como favoritas.
 * - [lastModified]: marca de tiempo (epoch ms) de la última mutación local;
 *   es la "versión" del perfil para resolver conflictos con la nube (LWW).
 */
data class PlayerProfile(
    val owned: Map<String, Int> = emptyMap(),
    val daily: DailyPackState = DailyPackState(),
    val seeded: Boolean = false,
    val decks: List<Deck> = emptyList(),
    val activeDeckId: String? = null,
    val favoriteDeckIds: Set<String> = emptySet(),
    val lastModified: Long = 0L,
) {
    val distinctOwned: Int get() = owned.size
    val totalOwned: Int get() = owned.values.sum()
}
