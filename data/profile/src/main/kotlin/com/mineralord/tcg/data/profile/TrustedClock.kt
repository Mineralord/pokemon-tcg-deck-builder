package com.mineralord.tcg.data.profile

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** DataStore propio del reloj confiable (separado del perfil). */
private val Context.timeDataStore by preferencesDataStore(name = "tcg_trusted_time")

/**
 * Reloj de tiempo **a prueba de trampa de reloj**: el crédito de sobres/eventos/recompensas se basa en
 * este tiempo, NO en el reloj de pared del dispositivo (que el jugador puede adelantar/atrasar).
 *
 * Estrategia (tolerante a offline):
 * - **Monótono**: usa [SystemClock.elapsedRealtime] (ms desde el arranque; INMUNE a cambiar la hora)
 *   para medir el tiempo transcurrido dentro de un arranque. Cambiar el reloj de pared no lo mueve.
 * - **Ancla real**: al abrir la app con red, [sync] obtiene la hora de un servidor (cabecera `Date` de
 *   una petición HTTPS) y re-ancla. Entre sincronizaciones se avanza con el reloj monótono.
 * - **Reinicio**: al reiniciar el móvil, `elapsedRealtime` vuelve a 0; se detecta y se re-ancla desde
 *   el último tiempo confiable conocido (continúa monótono, sin acreditar el tiempo apagado — es
 *   conservador, nunca de más). La próxima [sync] con red lo corrige hacia arriba.
 * - **Nunca retrocede**: el resultado se acota con el último valor conocido.
 *
 * Resultado: adelantar el reloj del teléfono no regenera sobres ni desbloquea eventos; a lo sumo el
 * jugador se PERJUDICA (undercrédito) hasta la próxima sincronización con red.
 */
class TrustedClock(context: Context) {

    private val store = context.applicationContext.timeDataStore

    /**
     * Tiempo confiable actual (ms epoch, aproximado a partir del ancla + reloj monótono). No usa la
     * hora de pared salvo como cota inferior si nunca hubo ancla. Persiste el máximo conocido.
     */
    suspend fun nowMs(): Long {
        val prefs = store.data.first()
        val anchorTrusted = prefs[ANCHOR_TRUSTED]
        val anchorElapsed = prefs[ANCHOR_ELAPSED]
        val lastKnown = prefs[LAST_KNOWN] ?: 0L
        val elapsedNow = SystemClock.elapsedRealtime()

        val (baseTrusted, baseElapsed) =
            if (anchorTrusted != null && anchorElapsed != null && elapsedNow >= anchorElapsed) {
                anchorTrusted to anchorElapsed
            } else {
                // Primer uso o REINICIO (elapsed reseteó): re-ancla desde el último conocido y este boot.
                store.edit { it[ANCHOR_TRUSTED] = lastKnown; it[ANCHOR_ELAPSED] = elapsedNow }
                lastKnown to elapsedNow
            }
        val candidate = baseTrusted + (elapsedNow - baseElapsed)
        val result = maxOf(candidate, lastKnown)
        if (result > lastKnown) store.edit { it[LAST_KNOWN] = result }
        return result
    }

    /**
     * Sincroniza con la hora de un servidor (cabecera `Date`), y re-ancla. Best-effort: si no hay red,
     * no cambia nada. Llamar al abrir la app (fuera del hilo principal).
     */
    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        val server = fetchServerTimeMs() ?: return@withContext false
        val elapsed = SystemClock.elapsedRealtime()
        store.edit {
            it[ANCHOR_TRUSTED] = server
            it[ANCHOR_ELAPSED] = elapsed
            it[LAST_KNOWN] = maxOf(server, it[LAST_KNOWN] ?: 0L)
        }
        true
    }

    /** Lee la hora del servidor por la cabecera `Date` de una petición HTTPS (sin dependencias). */
    private fun fetchServerTimeMs(): Long? = runCatching {
        val conn = (URL(TIME_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = 4000
            readTimeout = 4000
            useCaches = false
        }
        try {
            conn.connect()
            conn.getHeaderFieldDate("Date", 0L).takeIf { it > 0L }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private companion object {
        val ANCHOR_TRUSTED = longPreferencesKey("anchor_trusted_ms")
        val ANCHOR_ELAPSED = longPreferencesKey("anchor_elapsed_ms")
        val LAST_KNOWN = longPreferencesKey("last_known_trusted_ms")
        // Endpoint estable HTTPS; solo se lee la cabecera Date (hora del servidor).
        const val TIME_URL = "https://www.google.com/generate_204"
    }
}
