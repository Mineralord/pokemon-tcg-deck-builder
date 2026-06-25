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

    val profile: Flow<PlayerProfile> = store.data.map { prefs ->
        val ownedJson = prefs[OWNED] ?: "{}"
        val owned = runCatching {
            json.decodeFromString(mapSerializer, ownedJson)
        }.getOrDefault(emptyMap())
        val decks = runCatching {
            json.decodeFromString(decksSerializer, prefs[DECKS] ?: "[]").map { it.toDeck() }
        }.getOrDefault(emptyList())
        val favorites = runCatching {
            json.decodeFromString(favoritesSerializer, prefs[FAVORITES] ?: "[]")
        }.getOrDefault(emptySet())
        PlayerProfile(
            owned = owned,
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
            initialCardIds.forEach { id -> counts[id] = (counts[id] ?: 0) + 1 }
            prefs[OWNED] = json.encodeToString(mapSerializer, counts)
            prefs[SEEDED] = true
            prefs.touch()
        }
    }

    /** Añade cartas a la colección (acumula copias). */
    suspend fun addCards(cardIds: List<String>) {
        store.edit { prefs ->
            val current = runCatching {
                json.decodeFromString(mapSerializer, prefs[OWNED] ?: "{}")
            }.getOrDefault(emptyMap())
            val merged = HashMap(current)
            cardIds.forEach { id -> merged[id] = (merged[id] ?: 0) + 1 }
            prefs[OWNED] = json.encodeToString(mapSerializer, merged)
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
            if (prefs[DECKS_SEEDED] == true) return@edit
            prefs[DECKS] = json.encodeToString(decksSerializer, initialDecks.map { it.toDto() })
            initialDecks.firstOrNull()?.let { prefs[ACTIVE_DECK] = it.id }
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

    private companion object {
        val OWNED = stringPreferencesKey("owned_json")
        val DAILY_DAY = longPreferencesKey("daily_day")
        val DAILY_OPENED = intPreferencesKey("daily_opened")
        val SEEDED = booleanPreferencesKey("seeded")
        val DECKS = stringPreferencesKey("decks_json")
        val ACTIVE_DECK = stringPreferencesKey("active_deck_id")
        val FAVORITES = stringPreferencesKey("favorite_deck_ids")
        val DECKS_SEEDED = booleanPreferencesKey("decks_seeded")
        val LAST_MODIFIED = longPreferencesKey("last_modified")
    }
}
