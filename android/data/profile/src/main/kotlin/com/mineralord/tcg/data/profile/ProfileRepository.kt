package com.mineralord.tcg.data.profile

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mineralord.tcg.data.gacha.DailyPackState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
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

    val profile: Flow<PlayerProfile> = store.data.map { prefs ->
        val ownedJson = prefs[OWNED] ?: "{}"
        val owned = runCatching {
            json.decodeFromString(mapSerializer, ownedJson)
        }.getOrDefault(emptyMap())
        PlayerProfile(
            owned = owned,
            daily = DailyPackState(
                dayId = prefs[DAILY_DAY] ?: 0L,
                openedToday = prefs[DAILY_OPENED] ?: 0,
            ),
            seeded = prefs[SEEDED] ?: false,
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
        }
    }

    /** Persiste el estado del límite diario tras abrir un sobre. */
    suspend fun setDaily(state: DailyPackState) {
        store.edit { prefs ->
            prefs[DAILY_DAY] = state.dayId
            prefs[DAILY_OPENED] = state.openedToday
        }
    }

    private companion object {
        val OWNED = stringPreferencesKey("owned_json")
        val DAILY_DAY = longPreferencesKey("daily_day")
        val DAILY_OPENED = intPreferencesKey("daily_opened")
        val SEEDED = booleanPreferencesKey("seeded")
    }
}
