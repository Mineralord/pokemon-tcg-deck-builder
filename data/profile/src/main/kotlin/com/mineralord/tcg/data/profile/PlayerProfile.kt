package com.mineralord.tcg.data.profile

import com.mineralord.tcg.data.cards.Deck
import com.mineralord.tcg.data.gacha.DailyPackState

/**
 * Estado persistente del jugador. Inmutable; el repositorio entrega copias.
 *
 * - [owned]: cartas en la colección (id -> nº de copias).
 * - [balances]: saldo de los 4 recursos económicos oficiales (Fase 2, Cap. 6).
 * - [ownedCosmetics]: cosméticos desbloqueados (ids), vinculados a la cuenta (Fase 3 §13.2).
 * - [equippedCosmetics]: cosmético equipado por categoría (categoría -> id).
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
    val balances: Map<CurrencyKind, Int> = emptyMap(),
    val ownedCosmetics: Set<String> = emptySet(),
    val equippedCosmetics: Map<CosmeticCategory, String> = emptyMap(),
    val daily: DailyPackState = DailyPackState(),
    val seeded: Boolean = false,
    val decks: List<Deck> = emptyList(),
    val activeDeckId: String? = null,
    val favoriteDeckIds: Set<String> = emptySet(),
    val lastModified: Long = 0L,
) {
    val distinctOwned: Int get() = owned.size
    val totalOwned: Int get() = owned.values.sum()

    /** Saldo del recurso [kind] (0 si nunca se ha acreditado). */
    fun balanceOf(kind: CurrencyKind): Int = balances[kind] ?: 0

    /** ¿El cosmético [id] está desbloqueado? */
    fun ownsCosmetic(id: String): Boolean = id in ownedCosmetics

    /** Id del cosmético equipado en [category], o null si ninguno. */
    fun equippedIn(category: CosmeticCategory): String? = equippedCosmetics[category]
}
