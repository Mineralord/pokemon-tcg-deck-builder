package com.mineralord.tcg.core.designsystem

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Nivel gráfico efectivo. Qué cambia en cada uno:
 * - [LOW]: cartas planas en listas/mano/tablero (sin capa holo ni su PNG de ~1 MB por carta), sin
 *   animaciones decorativas en bucle. El holo solo se ve, estático, en el visor a pantalla completa.
 * - [MEDIUM]: holo real en todas partes pero ESTÁTICO en listas/mano/tablero (sin redibujar cada
 *   frame); animado solo en el visor a pantalla completa.
 * - [HIGH]: todo animado (calidad máxima).
 */
enum class GraphicsQuality { LOW, MEDIUM, HIGH }

/** Lo que elige el jugador en Ajustes. [AUTO] = según el dispositivo ([GraphicsSettings.detect]). */
enum class GraphicsPreference(val label: String) {
    AUTO("Automática"), LOW("Baja"), MEDIUM("Media"), HIGH("Alta"),
}

/** Preferencia de calidad gráfica, persistida en SharedPreferences y observable en vivo. */
object GraphicsSettings {
    private const val PREFS = "graphics"
    private const val KEY = "quality"

    private val _preference = MutableStateFlow(GraphicsPreference.AUTO)
    val preference: StateFlow<GraphicsPreference> = _preference.asStateFlow()

    private val _effective = MutableStateFlow(GraphicsQuality.HIGH)
    /** Nivel a aplicar (la preferencia, o el detectado si es [GraphicsPreference.AUTO]). */
    val effective: StateFlow<GraphicsQuality> = _effective.asStateFlow()

    /** Nivel que corresponde a este dispositivo (se muestra junto a "Automática"). */
    var detected: GraphicsQuality = GraphicsQuality.HIGH
        private set

    /** Llamar una vez al arrancar (Application.onCreate). */
    fun init(context: Context) {
        detected = detect(context)
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        apply(saved?.let { runCatching { GraphicsPreference.valueOf(it) }.getOrNull() } ?: GraphicsPreference.AUTO)
    }

    fun set(context: Context, pref: GraphicsPreference) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, pref.name).apply()
        apply(pref)
    }

    private fun apply(pref: GraphicsPreference) {
        _preference.value = pref
        _effective.value = when (pref) {
            GraphicsPreference.AUTO -> detected
            GraphicsPreference.LOW -> GraphicsQuality.LOW
            GraphicsPreference.MEDIUM -> GraphicsQuality.MEDIUM
            GraphicsPreference.HIGH -> GraphicsQuality.HIGH
        }
    }

    /**
     * Gama por RAM total, núcleos y versión de Android: el holo usa AGSL (Android 13+); por debajo de
     * eso tampoco hay holo animado, así que esos equipos (normalmente antiguos) van a gama baja/media.
     */
    fun detect(context: Context): GraphicsQuality {
        val am = context.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        val ramGb = info.totalMem / (1024.0 * 1024 * 1024)
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            am?.isLowRamDevice == true || ramGb < 3.5 || cores <= 4 -> GraphicsQuality.LOW
            ramGb < 6.5 || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> GraphicsQuality.MEDIUM
            else -> GraphicsQuality.HIGH
        }
    }
}

/** Nivel gráfico actual, observado en vivo (cambiarlo en Ajustes repinta al instante). */
@Composable
fun graphicsQuality(): GraphicsQuality = GraphicsSettings.effective.collectAsState().value
