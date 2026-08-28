package com.mineralord.tcg.data.profile

import com.mineralord.tcg.data.cards.Deck
import com.mineralord.tcg.data.cosmetics.CosmeticCategory
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
    val favoriteCosmetics: Set<String> = emptySet(),
    val daily: DailyPackState = DailyPackState(),
    val seeded: Boolean = false,
    val decks: List<Deck> = emptyList(),
    val activeDeckId: String? = null,
    val favoriteDeckIds: Set<String> = emptySet(),
    /**
     * Nombre de Usuario visible del jugador. Editable desde Perfil; al cambiarlo se propaga a
     * toda la app (menú, perfil, combate) y viaja con la cuenta vía snapshot de nube. Vacío hasta
     * que el jugador lo defina la primera vez (la UI muestra "Entrenador" como marcador).
     */
    val username: String = "",
    /** Código de amigo único del jugador (12 dígitos). Vacío hasta generarse la 1ª vez. */
    val friendCode: String = "",
    /**
     * Monedero de sobres gratis (saldo). Viaja con la cuenta para que reinstalar +
     * login NO regale sobres: al importar el snapshot se restaura el saldo real en
     * vez de re-sembrar el tope. null solo en el primerísimo arranque local.
     */
    val packBalance: Int? = null,
    /** Instante (ms, reloj confiable) del último crédito de sobre. Acompaña a [packBalance]. */
    val packLastCreditAt: Long? = null,
    val lastModified: Long = 0L,
) {
    val distinctOwned: Int get() = owned.size
    val totalOwned: Int get() = owned.values.sum()

    /** Código de amigo formateado en grupos de 4: "0007 4583 9120" (o crudo si no tiene 12 dígitos). */
    val friendCodeFormatted: String
        get() = if (friendCode.length == 12)
            "${friendCode.substring(0, 4)} ${friendCode.substring(4, 8)} ${friendCode.substring(8, 12)}"
        else friendCode

    /** Saldo del recurso [kind] (0 si nunca se ha acreditado). */
    fun balanceOf(kind: CurrencyKind): Int = balances[kind] ?: 0

    /** ¿El cosmético [id] está desbloqueado? */
    fun ownsCosmetic(id: String): Boolean = id in ownedCosmetics

    /** Id del cosmético equipado en [category], o null si ninguno. */
    fun equippedIn(category: CosmeticCategory): String? = equippedCosmetics[category]
}
