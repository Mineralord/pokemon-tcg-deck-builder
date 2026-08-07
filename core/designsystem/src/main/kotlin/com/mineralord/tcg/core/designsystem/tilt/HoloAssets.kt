package com.mineralord.tcg.core.designsystem.tilt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Front + máscara (+ etch opcional) de foil, decodificados y alineados, y el [Finish]
 *  resuelto desde los metadatos de foil REALES del manifiesto. */
class HoloBitmaps(
    val front: ImageBitmap,
    val mask: Bitmap,
    val etch: Bitmap?,
    val finish: Finish,
    /** Código del `foil.type` real para el patrón de color del shader (ver [foilTypeCode]). */
    val foilCode: Float,
)

/** Mapea el `foil.type` de TCGL a un código para el shader (uFoilType). -1 = desconocido. */
fun foilTypeCode(type: String?): Float = when (type?.uppercase()) {
    "FLAT_SILVER" -> 0f
    "SUN_PILLAR" -> 1f
    "SV_HOLO" -> 2f
    "SV_ULTRA" -> 3f
    else -> -1f
}

/**
 * Provee front + máscara de foil **bajo demanda**: lee un manifiesto de URLs empaquetado
 * (`assets/holo/<set>/manifest.json`, solo texto → NO infla el APK) y descarga las
 * imágenes la primera vez que se piden, cacheándolas en el almacenamiento de la app. Así
 * el APK no crece aunque se cubran todas las cartas; solo se baja lo que se ve.
 *
 * EXPERIMENTAL: las capas foil son assets de Pokémon TCG Live (uso personal).
 */
object HoloAssets {

    // set -> (numero -> objeto con "f"=front, "m"=mask)
    private val manifests = HashMap<String, JSONObject>()

    private fun manifest(context: Context, set: String): JSONObject =
        manifests.getOrPut(set) {
            runCatching {
                context.assets.open("holo/$set/manifest.json").use {
                    JSONObject(it.readBytes().decodeToString())
                }
            }.getOrDefault(JSONObject())
        }

    /**
     * Resuelve el foil de una carta por su código de set de la app + número, para CUALQUIER
     * set (presente o futuro) vía [MalieCatalog] (export dinámico). Cae al manifiesto local
     * empaquetado si la red falla o el set no está en malie. `null` → sin foil real (el
     * llamador usa máscara procedimental).
     */
    suspend fun resolve(context: Context, appSetCode: String, number: Int?, rarity: Rarity?): HoloBitmaps? {
        if (number == null) return null
        loadDynamic(context, appSetCode, number, rarity)?.let { return it }
        // Fallback offline: el único manifiesto empaquetado es el del set 151 (malie sv3-5).
        if (MalieCatalog.malieKey(appSetCode) == "sv3-5") return load(context, "151", number, rarity)
        return null
    }

    /** Descarga front+foil(+etch) reales del export de malie, eligiendo la impresión acorde a
     *  la rareza (holo/etched para raras+, ninguna para comunes). */
    private suspend fun loadDynamic(context: Context, appSetCode: String, number: Int, rarity: Rarity?): HoloBitmaps? =
        withContext(Dispatchers.IO) {
            val prints = MalieCatalog.printings(context, appSetCode, number)
            if (prints.isEmpty()) return@withContext null
            val p = pickPrinting(prints, rarity) ?: return@withContext null
            val foilUrl = p.foil ?: return@withContext null  // sin capa foil = carta plana (NonFoil)
            val front = cachedOrDownload(context, "$appSetCode/${number}_front.png", p.front) ?: return@withContext null
            val mask = cachedOrDownload(context, "$appSetCode/${number}_mask.png", foilUrl) ?: return@withContext null
            val etch = p.etch?.let { cachedOrDownload(context, "$appSetCode/${number}_etch.png", it) }
            val finish = resolveFinish(rarity, p.foilType, p.foilMask)
            HoloBitmaps(front.asImageBitmap(), mask, etch, finish, foilTypeCode(p.foilType))
        }

    /** Elige la impresión que corresponde a la rareza: comunes/infrecuentes = plana (NonFoil),
     *  raras = holo/etched (no reverse). El orden de preferencia es por máscara de foil. */
    private fun pickPrinting(prints: List<MalieCatalog.Printing>, rarity: Rarity?): MalieCatalog.Printing? {
        fun byMask(vararg masks: String) =
            masks.firstNotNullOfOrNull { m -> prints.firstOrNull { it.foilMask.equals(m, true) } }
        return when (resolveFinish(rarity)) {
            Finish.NONE -> byMask("None") ?: prints.first()   // común: preferimos la plana
            Finish.REVERSE_HOLO -> byMask("Reverse", "Holo", "None")
            else -> byMask("Etched", "Holo", "Reverse", "None")
        }
    }

    /** Devuelve el front+máscara de [number] en [set], o `null` si no está en el manifiesto
     *  o falla la descarga (→ el llamador cae a imagen remota + máscara procedimental). */
    suspend fun load(context: Context, set: String, number: Int?, rarity: Rarity?): HoloBitmaps? {
        if (number == null) return null
        return withContext(Dispatchers.IO) {
            val entry = manifest(context, set).optJSONObject(number.toString()) ?: return@withContext null
            val frontUrl = entry.optString("f", "")
            val maskUrl = entry.optString("m", "")
            if (frontUrl.isEmpty() || maskUrl.isEmpty()) return@withContext null
            val front = cachedOrDownload(context, "$set/${number}_front.png", frontUrl) ?: return@withContext null
            val mask = cachedOrDownload(context, "$set/${number}_mask.png", maskUrl) ?: return@withContext null
            // Capa etch (grabado) solo en cartas ETCHED; el shader la usa si existe.
            val etchUrl = entry.optString("e", "")
            val etch = if (etchUrl.isNotEmpty())
                cachedOrDownload(context, "$set/${number}_etch.png", etchUrl) else null
            // Finish desde los metadatos de foil REALES (verdad de origen).
            val type = entry.optString("t", null)
            val finish = resolveFinish(rarity, type, entry.optString("k", null))
            HoloBitmaps(front.asImageBitmap(), mask, etch, finish, foilTypeCode(type))
        }
    }

    /** Devuelve el bitmap desde caché o lo descarga (con User-Agent) y lo cachea. */
    private fun cachedOrDownload(context: Context, relPath: String, url: String): Bitmap? {
        val file = File(context.cacheDir, "holo/$relPath").apply { parentFile?.mkdirs() }
        if (file.exists() && file.length() > 0) {
            BitmapFactory.decodeFile(file.path)?.let {
                file.setLastModified(System.currentTimeMillis()) // marca uso (LRU)
                return it
            }
        }
        return runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 15000
                readTimeout = 15000
            }
            conn.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
            trimCache(File(context.cacheDir, "holo"))
            BitmapFactory.decodeFile(file.path)
        }.getOrNull()
    }

    // Límite de caché en disco: al superar [MAX_CACHE_BYTES], borra los ficheros MENOS
    // usados recientemente (por lastModified) hasta bajar de [TRIM_TARGET_BYTES].
    private const val MAX_CACHE_BYTES = 150L * 1024 * 1024   // 150 MB
    private const val TRIM_TARGET_BYTES = 120L * 1024 * 1024 // objetivo tras purga

    private fun trimCache(root: File) {
        val files = root.walkTopDown().filter { it.isFile }.toMutableList()
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return
        files.sortBy { it.lastModified() } // más antiguos primero
        for (f in files) {
            if (total <= TRIM_TARGET_BYTES) break
            val len = f.length()
            if (f.delete()) total -= len
        }
    }
}
