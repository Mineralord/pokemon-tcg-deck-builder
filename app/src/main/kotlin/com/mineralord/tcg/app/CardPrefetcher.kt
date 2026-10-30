package com.mineralord.tcg.app

import android.content.Context
import android.net.ConnectivityManager
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.mineralord.tcg.data.cards.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** ¿La conexión actual es de pago (datos móviles / punto de acceso)? Sin red también cuenta como "no descargar". */
fun isMeteredOrOffline(context: Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
    if (cm.activeNetwork == null) return true
    return cm.isActiveNetworkMetered
}

/**
 * Precarga en la caché de disco de Coil (persistente, 1 GB) TODAS las imágenes de carta del catálogo
 * en las dos variantes que usa la UI (`small(true)` y `large(true)`), solo con Wi-Fi. Así cualquier
 * pantalla las pinta al instante desde disco y no gasta datos móviles. Unos ~85 MB una sola vez; el
 * progreso se recuerda por URL (Coil no re-descarga lo que ya tiene), y si se pasa a datos móviles a
 * mitad, se para y continúa en la siguiente sesión con Wi-Fi.
 */
object CardPrefetcher {

    /** Marca de "catálogo completo descargado" ligada al nº de URLs (cambia si llegan cartas nuevas). */
    private fun doneMarker(context: Context) = File(context.filesDir, "card_images_prefetch.done")

    suspend fun run(context: Context) = withContext(Dispatchers.IO) {
        if (isMeteredOrOffline(context)) return@withContext
        val urls = CardRepository.load().all
            .flatMap { listOf(it.artwork.small(true), it.artwork.large(true)) }
            .filter { it.isNotBlank() }
            .distinct()
        val marker = doneMarker(context)
        if (marker.exists() && marker.readText() == urls.size.toString()) return@withContext

        val loader = context.imageLoader
        val disk = loader.diskCache
        for (url in urls) {
            if (disk?.openSnapshot(url)?.use { true } == true) continue
            if (isMeteredOrOffline(context)) return@withContext // se cortó el Wi-Fi: seguir otro día
            loader.execute(
                ImageRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.DISABLED) // solo a disco: no desplaza la RAM de la UI
                    .build(),
            )
        }
        marker.writeText(urls.size.toString())
    }
}
