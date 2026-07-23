package com.mineralord.tcg.studio

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mineralord.tcg.studio.assets.Asset
import com.mineralord.tcg.studio.assets.AssetRegistry

/** Una herramienta ACTIVA = la herramienta elegida + su asset (variante) seleccionado. */
data class ActiveTool(val tool: AssetTool, val asset: Asset)

/**
 * **Active Tool: el estado central del flujo herramienta-primero.**
 *
 * Sólo puede existir UNA herramienta activa. Mientras la haya, cada interacción compatible del
 * tablero ejecuta automáticamente su asset —sin diálogos ni selectores—; el asset permanece activo
 * para repetir la prueba infinitas veces. Sin herramienta activa (modo neutro), ninguna interacción
 * ejecuta assets.
 *
 * Consulta el [AssetRegistry] SÓLO por categoría y SÓLO al construir la lista de variantes de una
 * herramienta ([variantsFor]); una vez [activate]d, recuerda el asset y no vuelve a consultar el
 * Registry hasta que se cambie de herramienta/variante. El coordinador nunca conoce variantes
 * concretas: las provee el Registry.
 */
@Stable
class ToolController(
    private val registry: AssetRegistry,
    /** Herramientas disponibles (composition root). El panel las ofrece; el controller no las conoce. */
    val tools: List<AssetTool>,
) {
    /** Herramienta activa, o null en modo neutro. */
    var active by mutableStateOf<ActiveTool?>(null)
        private set

    /** Último objetivo recibido por la herramienta activa (para el HUD "Objetivo"). */
    var lastTarget by mutableStateOf<String?>(null)
        private set

    private var engine: ToolEngine? = null

    /** La escena de reproducción inyecta el motor real (una vez). */
    fun attachEngine(engine: ToolEngine) {
        this.engine = engine
    }

    /** Variantes de una herramienta, tomadas del Registry por categoría (sólo al abrir el selector). */
    fun variantsFor(tool: AssetTool): List<Asset> = registry.byCategory(tool.type, tool.category)

    /** Activa una herramienta con un asset concreto. A partir de aquí, el asset permanece activo. */
    fun activate(tool: AssetTool, asset: Asset) {
        active = ActiveTool(tool, asset)
        lastTarget = null
    }

    /** Vuelve al modo neutro: ninguna interacción ejecutará assets. */
    fun cancel() {
        active = null
        lastTarget = null
    }

    /**
     * Coordina una interacción del tablero: si hay herramienta activa y ACEPTA la interacción,
     * ejecuta su asset por el motor real. Genérico: no conoce Evolution ni ninguna categoría.
     */
    fun onInteraction(interaction: BoardInteraction) {
        val current = active ?: return
        if (!current.tool.accepts(interaction)) return
        lastTarget = interaction.targetCardId ?: "objetivo"
        engine?.let { current.tool.execute(current.asset, interaction, it) }
    }
}
