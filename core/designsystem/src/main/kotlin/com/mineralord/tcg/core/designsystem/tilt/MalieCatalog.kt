package com.mineralord.tcg.core.designsystem.tilt

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Catálogo de foil de TCGL/malie.io resuelto **dinámicamente** desde el export oficial, sin
 * manifiestos hechos a mano. Así CUALQUIER set (presente o futuro) que malie publique queda
 * cubierto automáticamente: dado el código de set de la app + número de carta, descarga (una
 * vez, cacheado en disco) el JSON de export del set y lee por carta sus URLs reales de
 * front/foil/etch y sus metadatos de foil (`FoilType`/`FoilMask` de `ext.tcgl.longFormID`).
 *
 * EXPERIMENTAL: las capas foil son assets de Pokémon TCG Live (uso personal).
 */
object MalieCatalog {

    private const val INDEX_URL = "https://cdn.malie.io/file/malie-io/tcgl/export/index.json"
    private const val EXPORT_BASE = "https://cdn.malie.io/file/malie-io/tcgl/export/"
    private const val LOCALE = "es-ES"

    /** Una impresión concreta de una carta (una misma carta puede tener holo Y reverse). */
    data class Printing(
        val front: String,
        val foil: String?,
        val etch: String?,
        val foilType: String,  // FlatSilver / SunPillar / SvHolo / SvUltra / NonFoil
        val foilMask: String,  // None / Reverse / Holo / Etched
    )

    // setKey malie -> (número -> impresiones). Cache en memoria (proceso).
    private val setCache = HashMap<String, Map<Int, List<Printing>>>()
    // setKey malie -> ruta del export ("v0.1.9.12/sv3-5.es-ES.json").
    @Volatile private var indexCache: Map<String, String>? = null

    /**
     * Traduce el código de set INTERNO de la app al código de malie. Regla general: `pt` → `-`
     * (p. ej. `sv3pt5` → `sv3-5`); el resto es identidad (`sv1`, `sv2`, …). Casos especiales
     * (promos, etc.) se añaden aquí explícitamente.
     */
    fun malieKey(appSetCode: String): String {
        val c = appSetCode.lowercase().trim()
        return when (c) {
            "svp" -> "svbsp"          // SV Black Star Promos
            else -> c.replace("pt", "-")
        }
    }

    /** Devuelve las impresiones de foil de [number] en [appSetCode], o vacío si no hay datos. */
    suspend fun printings(context: Context, appSetCode: String, number: Int): List<Printing> =
        withContext(Dispatchers.IO) {
            val key = malieKey(appSetCode)
            val set = setCache[key] ?: loadSet(context, key)?.also { setCache[key] = it }
            set?.get(number) ?: emptyList()
        }

    private fun loadSet(context: Context, setKey: String): Map<Int, List<Printing>>? {
        val path = index(context)[setKey] ?: return null
        val json = cachedJson(context, "catalog/$setKey.json", EXPORT_BASE + path) ?: return null
        return runCatching { parseSet(json) }.getOrNull()
    }

    private fun index(context: Context): Map<String, String> {
        indexCache?.let { return it }
        val json = cachedJson(context, "catalog/index.json", INDEX_URL) ?: return emptyMap()
        val parsed = runCatching {
            val loc = JSONObject(json).optJSONObject(LOCALE) ?: return@runCatching emptyMap<String, String>()
            buildMap {
                loc.keys().forEach { setKey ->
                    loc.optJSONObject(setKey)?.optString("path")?.takeIf { it.isNotEmpty() }
                        ?.let { put(setKey, it) }
                }
            }
        }.getOrDefault(emptyMap())
        indexCache = parsed
        return parsed
    }

    /** Parsea un export de set (lista de cartas) a número → impresiones. */
    private fun parseSet(json: String): Map<Int, List<Printing>> {
        val arr = JSONArray(json)
        val out = HashMap<Int, MutableList<Printing>>()
        for (i in 0 until arr.length()) {
            val c = arr.optJSONObject(i) ?: continue
            val num = c.optJSONObject("collector_number")?.optInt("numeric", -1) ?: -1
            if (num < 0) continue
            val png = c.optJSONObject("images")?.optJSONObject("tcgl")?.optJSONObject("png") ?: continue
            val front = png.optString("front", "")
            if (front.isEmpty()) continue
            val foil = png.optString("foil", "").ifEmpty { null }
            val etch = png.optString("etch", "").ifEmpty { null }
            // longFormID acaba en "..._FoilType_FoilMask" (p. ej. "..._SunPillar_Holo").
            val lf = c.optJSONObject("ext")?.optJSONObject("tcgl")?.optString("longFormID", "") ?: ""
            val parts = lf.split("_")
            val foilType = parts.getOrNull(parts.size - 2) ?: "NonFoil"
            val foilMask = parts.getOrNull(parts.size - 1) ?: "None"
            out.getOrPut(num) { mutableListOf() }.add(Printing(front, foil, etch, foilType, foilMask))
        }
        return out
    }

    /** Descarga (con User-Agent) o lee de disco un JSON del export; lo cachea en cacheDir/holo. */
    private fun cachedJson(context: Context, relPath: String, url: String): String? {
        val file = File(context.cacheDir, "holo/$relPath").apply { parentFile?.mkdirs() }
        if (file.exists() && file.length() > 0) {
            file.setLastModified(System.currentTimeMillis())
            return runCatching { file.readText() }.getOrNull()
        }
        return runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 15000
                readTimeout = 20000
            }
            val text = conn.inputStream.use { it.readBytes().decodeToString() }
            file.writeText(text)
            text
        }.getOrNull()
    }
}
