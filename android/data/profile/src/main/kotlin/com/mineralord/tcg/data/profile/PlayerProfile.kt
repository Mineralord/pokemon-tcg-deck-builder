package com.mineralord.tcg.data.profile

import com.mineralord.tcg.data.gacha.DailyPackState

/**
 * Estado persistente del jugador. Inmutable; el repositorio entrega copias.
 *
 * - [owned]: cartas en la colección (id -> nº de copias).
 * - [daily]: estado del límite diario de sobres.
 * - [seeded]: si ya se sembró la colección inicial (mazos desbloqueados).
 */
data class PlayerProfile(
    val owned: Map<String, Int> = emptyMap(),
    val daily: DailyPackState = DailyPackState(),
    val seeded: Boolean = false,
) {
    val distinctOwned: Int get() = owned.size
    val totalOwned: Int get() = owned.values.sum()
}
