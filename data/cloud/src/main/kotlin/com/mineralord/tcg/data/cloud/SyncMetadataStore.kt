package com.mineralord.tcg.data.cloud

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Sesión de Google recordada para restaurar el estado al reabrir la app. */
data class CloudSession(val email: String?, val name: String?, val avatarUrl: String?)

private val Context.cloudSyncStore by preferencesDataStore(name = "tcg_cloud_sync")

/**
 * Metadatos locales del sincronizador (NO el perfil): el id del archivo en Drive
 * (para no re-buscarlo), la última sincronización y la cuenta recordada.
 */
class SyncMetadataStore(context: Context) {

    private val store = context.applicationContext.cloudSyncStore

    val lastSynced: Flow<Long> = store.data.map { it[LAST_SYNCED] ?: 0L }

    suspend fun getFileId(): String? = store.data.first()[FILE_ID]

    suspend fun setFileId(id: String) {
        store.edit { it[FILE_ID] = id }
    }

    suspend fun setLastSynced(millis: Long) {
        store.edit { it[LAST_SYNCED] = millis }
    }

    suspend fun getSession(): CloudSession? {
        val prefs = store.data.first()
        val email = prefs[EMAIL] ?: return null
        return CloudSession(email = email, name = prefs[NAME], avatarUrl = prefs[AVATAR])
    }

    suspend fun setSession(email: String?, name: String?, avatarUrl: String?) {
        store.edit { prefs ->
            if (email != null) prefs[EMAIL] = email else prefs.remove(EMAIL)
            if (name != null) prefs[NAME] = name else prefs.remove(NAME)
            if (avatarUrl != null) prefs[AVATAR] = avatarUrl else prefs.remove(AVATAR)
        }
    }

    /** Limpia la sesión y el vínculo con Drive (al cerrar sesión). No toca el perfil. */
    suspend fun clear() {
        store.edit { it.clear() }
    }

    private companion object {
        val FILE_ID = stringPreferencesKey("drive_file_id")
        val LAST_SYNCED = longPreferencesKey("last_synced_millis")
        val EMAIL = stringPreferencesKey("signed_in_email")
        val NAME = stringPreferencesKey("signed_in_name")
        val AVATAR = stringPreferencesKey("signed_in_avatar")
    }
}
