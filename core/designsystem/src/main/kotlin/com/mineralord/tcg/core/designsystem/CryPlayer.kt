package com.mineralord.tcg.core.designsystem

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reproduce los GRITOS oficiales de Pokémon por nº de Pokédex NACIONAL desde el repo abierto
 * **PokeAPI/cries** (STREAMING + CACHÉ en disco). Se usa en los VISORES de carta (colección,
 * editor de barajas, etc.): al abrir el visor de un Pokémon suena su grito. En COMBATE el grito
 * lo gestiona su propio reproductor (suena al atacar), no este.
 *
 * Cualquier fallo (sin red, 404…) es silencioso: mejor no oír un grito que romper la UI.
 */
class CryPlayer(context: Context) {

    // Persistente (filesDir, no cacheDir): los gritos se guardan SIEMPRE y sin límite, y no los
    // borra el sistema bajo presión de almacenamiento. Offline total tras oírse una vez.
    private val cacheDir = File(context.filesDir, "cries").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val locks = HashMap<Int, Mutex>()

    @Volatile private var volume = 1f
    private var player: MediaPlayer? = null

    fun setVolume(v: Float) { volume = v.coerceIn(0f, 1f) }

    /** Reproduce el grito del Pokédex [dex] (lo descarga+cachea si hace falta). No bloquea la UI. */
    fun play(dex: Int?) {
        val n = dex ?: return
        if (n <= 0 || volume <= 0f) return
        scope.launch {
            val file = ensureCached(n) ?: return@launch
            withContext(Dispatchers.Main) { start(file) }
        }
    }

    private suspend fun ensureCached(dex: Int): File? {
        val file = File(cacheDir, "$dex.ogg")
        if (file.exists() && file.length() > 0) return file
        val mutex = synchronized(locks) { locks.getOrPut(dex) { Mutex() } }
        return mutex.withLock {
            if (file.exists() && file.length() > 0) file
            else runCatching {
                val conn = (URL("$BASE/$dex.ogg").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000; readTimeout = 8000; instanceFollowRedirects = true
                }
                conn.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                conn.disconnect()
                file.takeIf { it.length() > 0 }
            }.getOrElse { file.delete(); null }
        }
    }

    private fun start(file: File) {
        runCatching {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setVolume(volume, volume)
                setOnCompletionListener { mp -> mp.release(); if (player === mp) player = null }
                prepare()
                start()
            }
        }
    }

    fun release() {
        runCatching { player?.release() }
        player = null
    }

    private companion object {
        const val BASE = "https://raw.githubusercontent.com/PokeAPI/cries/main/cries/pokemon/latest"
    }
}
