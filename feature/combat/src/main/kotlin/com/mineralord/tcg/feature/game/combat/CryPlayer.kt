package com.mineralord.tcg.feature.game.combat

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
 * Reproduce los GRITOS oficiales de Pokémon (de los videojuegos) por número de Pokédex NACIONAL,
 * tomándolos del repo abierto **PokeAPI/cries**. Estrategia STREAMING + CACHÉ en disco: la primera
 * vez que suena una especie se descarga su `.ogg` y se guarda en `cacheDir/cries`; a partir de ahí
 * se reproduce en local (offline). Así NO engorda el APK. Cualquier fallo (sin red, 404…) es
 * silencioso: mejor no oír un grito que romper el combate.
 */
class CryPlayer(context: Context) {

    private val cacheDir = File(context.cacheDir, "cries").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** Un cerrojo por dex para no descargar dos veces la misma especie a la vez. */
    private val locks = HashMap<Int, Mutex>()

    @Volatile private var volume = 1f
    private var player: MediaPlayer? = null

    /** Volumen 0..1 (se enlaza al deslizador de "Efectos de sonido" del combate). */
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
