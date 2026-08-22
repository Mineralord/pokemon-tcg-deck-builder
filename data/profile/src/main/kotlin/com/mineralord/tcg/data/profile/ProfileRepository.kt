package com.mineralord.tcg.data.profile

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mineralord.tcg.data.cards.Deck
import com.mineralord.tcg.data.cards.DeckEntry
import com.mineralord.tcg.data.gacha.DailyPackState
import com.mineralord.tcg.data.gacha.PackWallet
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** DataStore único por proceso (el delegate garantiza una sola instancia). */
private val Context.dataStore by preferencesDataStore(name = "tcg_profile")

/**
 * Persiste el [PlayerProfile] con DataStore. La colección se guarda como JSON
 * (id -> copias); el límite diario y la bandera de sembrado, como claves
 * simples. Expone un [Flow] reactivo para que la UI observe los cambios.
 */
class ProfileRepository(context: Context) {

    private val store = context.applicationContext.dataStore
    private val json = Json
    private val mapSerializer = MapSerializer(String.serializer(), Int.serializer())
    private val decksSerializer = ListSerializer(DeckDto.serializer())
    private val favoritesSerializer = SetSerializer(String.serializer())
    private val stringMapSerializer = MapSerializer(String.serializer(), String.serializer())

    val profile: Flow<PlayerProfile> = store.data.map { prefs ->
        val ownedJson = prefs[OWNED] ?: "{}"
        val owned = runCatching {
            json.decodeFromString(mapSerializer, ownedJson)
        }.getOrDefault(emptyMap())
        val balances = decodeBalances(prefs[BALANCES])
        val decks = runCatching {
            json.decodeFromString(decksSerializer, prefs[DECKS] ?: "[]").map { it.toDeck() }
        }.getOrDefault(emptyList())
        val favorites = runCatching {
            json.decodeFromString(favoritesSerializer, prefs[FAVORITES] ?: "[]")
        }.getOrDefault(emptySet())
        val ownedCosmetics = runCatching {
            json.decodeFromString(favoritesSerializer, prefs[OWNED_COSMETICS] ?: "[]")
        }.getOrDefault(emptySet())
        val equippedCosmetics = decodeEquipped(prefs[EQUIPPED_COSMETICS])
        PlayerProfile(
            owned = owned,
            balances = balances,
            ownedCosmetics = ownedCosmetics,
            equippedCosmetics = equippedCosmetics,
            daily = DailyPackState(
                dayId = prefs[DAILY_DAY] ?: 0L,
                openedToday = prefs[DAILY_OPENED] ?: 0,
            ),
            seeded = prefs[SEEDED] ?: false,
            decks = decks,
            activeDeckId = prefs[ACTIVE_DECK],
            favoriteDeckIds = favorites,
            lastModified = prefs[LAST_MODIFIED] ?: 0L,
        )
    }

    /** Siembra la colección inicial una sola vez (mazos desbloqueados). */
    suspend fun seedOnce(initialCardIds: List<String>) {
        store.edit { prefs ->
            if (prefs[SEEDED] == true) return@edit
            val counts = HashMap<String, Int>()
            initialCardIds.forEach { id -> counts[id] = (counts[id] ?: 0).coerceIncrement(id) }
            prefs[OWNED] = json.encodeToString(mapSerializer, counts)
            prefs[SEEDED] = true
            prefs.touch()
        }
    }

    /**
     * Añade cartas a la colección (acumula copias) RESPETANDO el tope por carta: 4 copias para una
     * carta normal; las Energías Básicas son ILIMITADAS (nunca desbordan). Las copias que rebasan el
     * tope de una carta normal NO se pierden: se acumulan en [PENDING_SHARDS] para su futura
     * conversión a Fichas (Fase 5), preservando el patrimonio del jugador (Preservación Absoluta /
     * Reversibilidad). Devuelve, por id, cuántas se DESVIARON al excedente en esta llamada (0 si
     * ninguna) para que la capa superior pueda informar al jugador.
     */
    suspend fun addCards(cardIds: List<String>): Map<String, Int> {
        val overflow = HashMap<String, Int>()
        store.edit { prefs ->
            val current = runCatching {
                json.decodeFromString(mapSerializer, prefs[OWNED] ?: "{}")
            }.getOrDefault(emptyMap())
            val merged = HashMap(current)
            cardIds.forEach { id ->
                val cap = capFor(id)
                val now = merged[id] ?: 0
                if (now >= cap) overflow[id] = (overflow[id] ?: 0) + 1
                else merged[id] = now + 1
            }
            prefs[OWNED] = json.encodeToString(mapSerializer, merged)
            if (overflow.isNotEmpty()) {
                // Preserva el excedente (nunca se descarta) hasta que exista la conversión a Fichas.
                val pending = runCatching {
                    json.decodeFromString(mapSerializer, prefs[PENDING_SHARDS] ?: "{}")
                }.getOrDefault(emptyMap())
                val mergedPending = HashMap(pending)
                overflow.forEach { (id, n) -> mergedPending[id] = (mergedPending[id] ?: 0) + n }
                prefs[PENDING_SHARDS] = json.encodeToString(mapSerializer, mergedPending)
            }
            prefs.touch()
        }
        return overflow
    }

    /** Excedente de duplicados preservado (id -> nº) pendiente de convertir a Fichas (Fase 5). */
    val pendingShards: Flow<Map<String, Int>> = store.data.map { prefs ->
        runCatching {
            json.decodeFromString(mapSerializer, prefs[PENDING_SHARDS] ?: "{}")
        }.getOrDefault(emptyMap())
    }

    /**
     * Concesión inicial ÚNICA de Cristales (estado inicial de la cuenta, no recompensa por inicio
     * de sesión). Gate propio para que también se aplique en instalaciones ya sembradas de cartas.
     */
    suspend fun seedBalancesOnce() {
        store.edit { prefs ->
            val balances = HashMap(decodeBalances(prefs[BALANCES]).mapKeys { it.key.name })
            var changed = false
            // Cristales (gate original). Concesión inicial para poder comprar sobres.
            if (prefs[BALANCES_SEEDED] != true) {
                balances[CurrencyKind.CRISTALES.name] =
                    (balances[CurrencyKind.CRISTALES.name] ?: 0) + EconomyRules.STARTER_CRISTALES
                prefs[BALANCES_SEEDED] = true
                changed = true
            }
            // Monedas (gate propio). Se añadió con la Tienda de Cosméticos; su gate independiente
            // permite que también corra en instalaciones cuyos Cristales ya se sembraron antes.
            if (prefs[MONEDAS_SEEDED] != true) {
                balances[CurrencyKind.MONEDAS.name] =
                    (balances[CurrencyKind.MONEDAS.name] ?: 0) + EconomyRules.STARTER_MONEDAS
                prefs[MONEDAS_SEEDED] = true
                changed = true
            }
            if (changed) {
                prefs[BALANCES] = json.encodeToString(mapSerializer, balances)
                prefs.touch()
            }
        }
    }

    /**
     * Compra un cosmético con **Monedas** (Fase 3 §3.3/§14). Operación ATÓMICA: si ya se
     * posee o no hay saldo suficiente NO modifica nada y devuelve `false`. Un cosmético
     * comprado queda vinculado permanentemente a la cuenta (§13.2).
     */
    suspend fun buyCosmetic(cosmetic: Cosmetic): Boolean {
        var ok = false
        store.edit { prefs ->
            val owned = runCatching {
                json.decodeFromString(favoritesSerializer, prefs[OWNED_COSMETICS] ?: "[]")
            }.getOrDefault(emptySet())
            if (cosmetic.id in owned) return@edit
            val balances = HashMap(decodeBalances(prefs[BALANCES]).mapKeys { it.key.name })
            val current = balances[CurrencyKind.MONEDAS.name] ?: 0
            if (current < cosmetic.priceMonedas) return@edit
            balances[CurrencyKind.MONEDAS.name] = current - cosmetic.priceMonedas
            prefs[BALANCES] = json.encodeToString(mapSerializer, balances)
            prefs[OWNED_COSMETICS] = json.encodeToString(favoritesSerializer, owned + cosmetic.id)
            prefs.touch()
            ok = true
        }
        return ok
    }

    /**
     * Equipa el cosmético [cosmeticId] en su [category] (uno por categoría). Requiere poseerlo;
     * en caso contrario no hace nada. Un cosmético es exclusivamente estético (§13.4).
     */
    suspend fun equipCosmetic(category: CosmeticCategory, cosmeticId: String) {
        store.edit { prefs ->
            val owned = runCatching {
                json.decodeFromString(favoritesSerializer, prefs[OWNED_COSMETICS] ?: "[]")
            }.getOrDefault(emptySet())
            if (cosmeticId !in owned) return@edit
            val equipped = HashMap(decodeEquipped(prefs[EQUIPPED_COSMETICS]).mapKeys { it.key.name })
            equipped[category.name] = cosmeticId
            prefs[EQUIPPED_COSMETICS] = json.encodeToString(stringMapSerializer, equipped)
            prefs.touch()
        }
    }

    /** Decodifica el JSON de equipado (nombre de categoría -> id), ignorando categorías desconocidas. */
    private fun decodeEquipped(raw: String?): Map<CosmeticCategory, String> {
        val byName = runCatching {
            json.decodeFromString(stringMapSerializer, raw ?: "{}")
        }.getOrDefault(emptyMap())
        return byName.mapNotNull { (name, id) ->
            runCatching { CosmeticCategory.valueOf(name) }.getOrNull()?.let { it to id }
        }.toMap()
    }

    /** Acredita [amount] unidades del recurso [kind] (Fase 2, Cap. 6). [amount] debe ser >= 0. */
    suspend fun credit(kind: CurrencyKind, amount: Int) {
        if (amount <= 0) return
        store.edit { prefs ->
            val balances = HashMap(decodeBalances(prefs[BALANCES]).mapKeys { it.key.name })
            balances[kind.name] = (balances[kind.name] ?: 0) + amount
            prefs[BALANCES] = json.encodeToString(mapSerializer, balances)
            prefs.touch()
        }
    }

    /**
     * Intenta gastar [amount] unidades del recurso [kind]. Operación atómica: si no hay saldo
     * suficiente NO modifica nada y devuelve `false`. El saldo nunca queda negativo.
     */
    suspend fun spend(kind: CurrencyKind, amount: Int): Boolean {
        if (amount <= 0) return true
        var ok = false
        store.edit { prefs ->
            val balances = HashMap(decodeBalances(prefs[BALANCES]).mapKeys { it.key.name })
            val current = balances[kind.name] ?: 0
            if (current < amount) return@edit
            balances[kind.name] = current - amount
            prefs[BALANCES] = json.encodeToString(mapSerializer, balances)
            prefs.touch()
            ok = true
        }
        return ok
    }

    /** Decodifica el JSON de saldos (nombre de recurso -> saldo), ignorando claves desconocidas. */
    private fun decodeBalances(raw: String?): Map<CurrencyKind, Int> {
        val byName = runCatching {
            json.decodeFromString(mapSerializer, raw ?: "{}")
        }.getOrDefault(emptyMap())
        return byName.mapNotNull { (name, value) ->
            runCatching { CurrencyKind.valueOf(name) }.getOrNull()?.let { it to value }
        }.toMap()
    }

    /** Incremento de una copia respetando el tope de la carta [id] (usado en el sembrado). */
    private fun Int.coerceIncrement(id: String): Int = (this + 1).coerceAtMost(capFor(id))

    /**
     * Monedero de sobres (regeneración acumulable). null si aún no se ha sembrado (primer uso): el
     * llamador lo inicializa con el saldo tope y el tiempo CONFIABLE actual. Persistido aparte del
     * snapshot de nube por ahora.
     */
    val wallet: Flow<PackWallet?> = store.data.map { prefs ->
        val bal = prefs[PACK_BALANCE]
        val last = prefs[PACK_LAST_CREDIT]
        if (bal != null && last != null) PackWallet(bal, last) else null
    }

    /** Persiste el monedero de sobres. */
    suspend fun saveWallet(w: PackWallet) {
        store.edit { prefs ->
            prefs[PACK_BALANCE] = w.balance
            prefs[PACK_LAST_CREDIT] = w.lastCreditAt
            prefs.touch()
        }
    }

    /** Persiste el estado del límite diario tras abrir un sobre. */
    suspend fun setDaily(state: DailyPackState) {
        store.edit { prefs ->
            prefs[DAILY_DAY] = state.dayId
            prefs[DAILY_OPENED] = state.openedToday
            prefs.touch()
        }
    }

    /**
     * Siembra las barajas iniciales una sola vez (gate independiente del de la
     * colección, para que también corra en instalaciones ya sembradas). Deja la
     * primera baraja como activa.
     */
    suspend fun seedDecksOnce(initialDecks: List<Deck>) {
        store.edit { prefs ->
            // Sembrar si nunca se sembró O si el usuario se quedó SIN barajas (p.ej.
            // tras importar de la nube un snapshot vacío/antiguo, que además marca
            // DECKS_SEEDED=true). Así los starters siempre reaparecen; no pisa barajas
            // existentes (si ya hay alguna, no se hace nada).
            val current = readDecks(prefs)
            if (prefs[DECKS_SEEDED] == true && current.isNotEmpty()) return@edit
            prefs[DECKS] = json.encodeToString(decksSerializer, initialDecks.map { it.toDto() })
            if (prefs[ACTIVE_DECK] == null) initialDecks.firstOrNull()?.let { prefs[ACTIVE_DECK] = it.id }
            prefs[DECKS_SEEDED] = true
            prefs.touch()
        }
    }

    /** Marca una baraja como la activa (la que usará el combate). */
    suspend fun setActiveDeck(deckId: String) {
        store.edit { prefs ->
            prefs[ACTIVE_DECK] = deckId
            prefs.touch()
        }
    }

    /** Alterna el estado de favorita de una baraja. */
    suspend fun toggleFavorite(deckId: String) {
        store.edit { prefs ->
            val current = runCatching {
                json.decodeFromString(favoritesSerializer, prefs[FAVORITES] ?: "[]")
            }.getOrDefault(emptySet())
            val next = if (deckId in current) current - deckId else current + deckId
            prefs[FAVORITES] = json.encodeToString(favoritesSerializer, next)
            prefs.touch()
        }
    }

    /**
     * Inserta o reemplaza una baraja (por id) y le sella [Deck.updatedAt] = ahora.
     * Sirve para editar contenido, crear, duplicar y renombrar.
     */
    suspend fun upsertDeck(deck: Deck) {
        store.edit { prefs ->
            val list = readDecks(prefs).toMutableList()
            val stamped = deck.copy(updatedAt = System.currentTimeMillis())
            val i = list.indexOfFirst { it.id == deck.id }
            if (i >= 0) list[i] = stamped else list += stamped
            prefs[DECKS] = json.encodeToString(decksSerializer, list.map { it.toDto() })
            prefs.touch()
        }
    }

    /** Snapshot de una baraja por id (lectura puntual, no reactiva). */
    suspend fun currentDeck(deckId: String): Deck? =
        profile.first().decks.firstOrNull { it.id == deckId }

    /** Borra una baraja; si era la activa, reasigna a la primera restante. */
    suspend fun deleteDeck(deckId: String) {
        store.edit { prefs ->
            val list = readDecks(prefs).filterNot { it.id == deckId }
            prefs[DECKS] = json.encodeToString(decksSerializer, list.map { it.toDto() })
            if (prefs[ACTIVE_DECK] == deckId) {
                val first = list.firstOrNull()?.id
                if (first != null) prefs[ACTIVE_DECK] = first else prefs.remove(ACTIVE_DECK)
            }
            prefs.touch()
        }
    }

    /**
     * Sobrescribe TODO el perfil con un snapshot descargado de la nube (un solo
     * [edit] para que el [Flow] emita un estado coherente). Preserva el
     * [PlayerProfile.lastModified] importado (no sella "ahora") para no disparar
     * una re-subida en bucle, y marca las barajas como sembradas.
     */
    suspend fun importSnapshot(profile: PlayerProfile) {
        store.edit { prefs ->
            prefs[OWNED] = json.encodeToString(mapSerializer, profile.owned)
            prefs[BALANCES] = json.encodeToString(mapSerializer, profile.balances.mapKeys { it.key.name })
            prefs[OWNED_COSMETICS] = json.encodeToString(favoritesSerializer, profile.ownedCosmetics)
            prefs[EQUIPPED_COSMETICS] =
                json.encodeToString(stringMapSerializer, profile.equippedCosmetics.mapKeys { it.key.name })
            prefs[DAILY_DAY] = profile.daily.dayId
            prefs[DAILY_OPENED] = profile.daily.openedToday
            prefs[SEEDED] = profile.seeded
            prefs[DECKS] = json.encodeToString(decksSerializer, profile.decks.map { it.toDto() })
            if (profile.activeDeckId != null) prefs[ACTIVE_DECK] = profile.activeDeckId
            else prefs.remove(ACTIVE_DECK)
            prefs[FAVORITES] = json.encodeToString(favoritesSerializer, profile.favoriteDeckIds)
            prefs[DECKS_SEEDED] = true
            prefs[LAST_MODIFIED] = profile.lastModified
        }
    }

    /** Sella la marca de tiempo de última modificación (versión del perfil). */
    private fun androidx.datastore.preferences.core.MutablePreferences.touch() {
        this[LAST_MODIFIED] = System.currentTimeMillis()
    }

    private fun readDecks(prefs: androidx.datastore.preferences.core.Preferences): List<Deck> =
        runCatching {
            json.decodeFromString(decksSerializer, prefs[DECKS] ?: "[]").map { it.toDeck() }
        }.getOrDefault(emptyList())

    // --- Serialización de barajas (DTO local; el modelo de dominio usa value classes) ---

    @Serializable
    private data class DeckDto(
        val id: String,
        val name: String,
        val headliner: String,
        val type: String,
        val entries: List<EntryDto>,
        val isCustom: Boolean,
        val updatedAt: Long = 0L,
    )

    @Serializable
    private data class EntryDto(val cardId: String, val count: Int)

    private fun Deck.toDto() = DeckDto(
        id = id,
        name = name,
        headliner = headliner.raw,
        type = type.name,
        entries = entries.map { EntryDto(it.cardId.raw, it.count) },
        isCustom = isCustom,
        updatedAt = updatedAt,
    )

    private fun DeckDto.toDeck() = Deck(
        id = id,
        name = name,
        headliner = CardId(headliner),
        type = EnergyType.valueOf(type),
        entries = entries.map { DeckEntry(CardId(it.cardId), it.count) },
        isCustom = isCustom,
        updatedAt = updatedAt,
    )

    companion object {
        /** Tope de copias por carta normal en la colección (el excedente se preserva para futura moneda). */
        const val CARD_CAP = 4

        /**
         * Tope "ilimitado". Por Canon (Fase 5 + directriz del propietario 2026-08-21), las Energías
         * Básicas NO tienen tope de colección y NO generan Fichas por duplicado. Se representa con el
         * máximo entero: nunca desbordan, nunca se descartan y nunca alimentan la conversión.
         */
        const val UNLIMITED = Int.MAX_VALUE

        /** ¿El id corresponde a una Energía Básica? (ids "energy-basic-<tipo>-energy"). */
        fun isEnergyId(id: String): Boolean = id.startsWith("energy")

        /** ¿La carta [id] tiene tope de colección? Las Energías Básicas son ilimitadas. */
        fun isCapped(id: String): Boolean = !isEnergyId(id)

        /** Tope de copias aplicable a la carta [id]: 4 para cartas normales, ilimitado para energías. */
        fun capFor(id: String): Int = if (isEnergyId(id)) UNLIMITED else CARD_CAP

        val OWNED = stringPreferencesKey("owned_json")
        val DAILY_DAY = longPreferencesKey("daily_day")
        val DAILY_OPENED = intPreferencesKey("daily_opened")
        val PACK_BALANCE = intPreferencesKey("pack_balance")
        val PACK_LAST_CREDIT = longPreferencesKey("pack_last_credit")
        val SEEDED = booleanPreferencesKey("seeded")
        val DECKS = stringPreferencesKey("decks_json")
        val ACTIVE_DECK = stringPreferencesKey("active_deck_id")
        val FAVORITES = stringPreferencesKey("favorite_deck_ids")
        val DECKS_SEEDED = booleanPreferencesKey("decks_seeded")
        val LAST_MODIFIED = longPreferencesKey("last_modified")
        val PENDING_SHARDS = stringPreferencesKey("pending_shards_json")
        val BALANCES = stringPreferencesKey("balances_json")
        val BALANCES_SEEDED = booleanPreferencesKey("balances_seeded")
        val MONEDAS_SEEDED = booleanPreferencesKey("monedas_seeded")
        val OWNED_COSMETICS = stringPreferencesKey("owned_cosmetics_json")
        val EQUIPPED_COSMETICS = stringPreferencesKey("equipped_cosmetics_json")
    }
}
