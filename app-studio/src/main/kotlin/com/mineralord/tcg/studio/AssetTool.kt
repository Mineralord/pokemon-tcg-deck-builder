package com.mineralord.tcg.studio

import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.studio.assets.AnimationAsset
import com.mineralord.tcg.studio.assets.Asset
import com.mineralord.tcg.studio.assets.AssetType

/**
 * **Sistema de herramientas del Studio (Active Tool) — arquitectura GENÉRICA.**
 *
 * El Studio es una herramienta profesional: **primero se elige la herramienta, luego se aplica**
 * (como Unity/Blender/Photoshop). Este archivo define el contrato genérico; NO contiene lógica de
 * Evolution, Draw, KO ni ninguna categoría concreta. Cada [AssetTool] declara qué interacción del
 * tablero acepta y qué asset ejecuta; el Studio sólo coordina.
 *
 * Añadir una herramienta futura (Draw, KO, Partículas, Sonido, Cámara, Iluminación…) = añadir una
 * entrada de datos a [studioTools] (o una nueva clase de tool para otros tipos de Asset). No se toca
 * el coordinador ni la pantalla.
 */

/** Tipos de interacción del tablero, normalizados y sin semántica de categoría. */
enum class InteractionKind {
    EvolveDrop, DrawFromDeck, PlayBasic, AttachEnergy, Retreat, KnockOut, Attack, EndTurn, Other,
}

/** Una interacción del tablero traducida a datos neutrales (la produce el `SandboxController`). */
data class BoardInteraction(
    val kind: InteractionKind,
    val sourceCardId: String? = null,
    val targetCardId: String? = null,
)

/** Acceso mínimo al motor real para que una herramienta reproduzca su asset (mismo pipeline canónico). */
fun interface ToolEngine {
    fun submit(request: AnimationRequest)
}

/**
 * Contrato de una herramienta de asset. Cada implementación declara **qué objetivo acepta**, **cómo
 * lo valida** y **qué hace al aplicarse**. El coordinador del Studio nunca conoce estos detalles.
 */
interface AssetTool {
    /** Etiqueta legible (p. ej. "Evolution"). */
    val label: String

    /** Tipo de Asset que gestiona (hoy sólo Animation). */
    val type: AssetType

    /** Categoría del Asset Registry de la que toma sus variantes (p. ej. "Evolución"). */
    val category: String

    /** ¿Esta interacción del tablero dispara la herramienta? */
    fun accepts(interaction: BoardInteraction): Boolean

    /** Ejecuta el [asset] activo sobre la interacción aceptada, usando el motor real. */
    fun execute(asset: Asset, interaction: BoardInteraction, engine: ToolEngine)
}

/**
 * Herramienta GENÉRICA para assets de tipo **Animation**: se dispara con [trigger] y reproduce la
 * `request` del asset por el motor canónico. No hay lógica de ninguna categoría concreta: Evolution,
 * Draw o KO son sólo distintas configuraciones (categoría + interacción) de esta misma clase.
 */
class AnimationTool(
    override val label: String,
    override val category: String,
    private val trigger: InteractionKind,
) : AssetTool {
    override val type: AssetType = AssetType.Animation

    override fun accepts(interaction: BoardInteraction): Boolean = interaction.kind == trigger

    override fun execute(asset: Asset, interaction: BoardInteraction, engine: ToolEngine) {
        // El asset ya lleva su recurso real (la AnimationRequest): el tool sólo lo envía al motor.
        (asset as? AnimationAsset)?.let { engine.submit(it.request) }
    }
}

/** Categoría de evolución en el Asset Registry (dato de configuración, no lógica). */
const val EVOLUTION_CATEGORY = "Evolución"

/**
 * Catálogo de herramientas disponibles del Studio (composition root). Evolution es sólo la PRIMERA
 * herramienta configurada; el resto se añadirán como más entradas de datos con la MISMA clase
 * genérica (o una nueva por tipo de Asset), sin tocar el coordinador.
 */
fun studioTools(): List<AssetTool> = listOf(
    AnimationTool(label = "Evolution", category = EVOLUTION_CATEGORY, trigger = InteractionKind.EvolveDrop),
    // Futuras (misma clase genérica, sólo datos):
    //   AnimationTool("Draw", "Robar carta", InteractionKind.DrawFromDeck),
    //   AnimationTool("KO", "KO", InteractionKind.KnockOut), …
)
