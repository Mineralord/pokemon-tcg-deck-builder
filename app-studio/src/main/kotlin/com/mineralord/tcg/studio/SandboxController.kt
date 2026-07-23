package com.mineralord.tcg.studio

import androidx.compose.ui.geometry.Offset
import com.mineralord.tcg.core.combatscene.CombatSceneController
import com.mineralord.tcg.core.combatscene.CombatSceneEngine
import com.mineralord.tcg.core.combatscene.CombatSceneEvent
import com.mineralord.tcg.core.combatscene.CombatSceneState
import com.mineralord.tcg.core.combatscene.SceneAction
import com.mineralord.tcg.core.combatscene.SceneCard
import com.mineralord.tcg.core.combatscene.SceneOption
import com.mineralord.tcg.core.combatscene.SceneOverlay
import com.mineralord.tcg.studio.assets.AnimationAsset
import com.mineralord.tcg.studio.assets.AssetRegistry
import com.mineralord.tcg.studio.assets.AssetStatus
import com.mineralord.tcg.studio.assets.AssetType

/**
 * **Sandbox Controller: el control MANUAL de la Combat Scene en el Studio.**
 *
 * Implementa el contrato [CombatSceneController]: **conduce** la escena compartida sin dibujar nada.
 * Su única diferencia frente al futuro Game Controller es el origen de las decisiones: aquí es el
 * usuario (mover cartas libremente, superponerlas, elegir variantes, repetir animaciones), no las
 * reglas. Toda la representación visual la aporta la `CombatScene` del núcleo compartido.
 *
 * Consumo del Asset Registry **por categoría, nunca por id**: al soltar una carta sobre otra con la
 * acción Evolution armada, pregunta al [AssetRegistry] qué variantes hay en [EVOLUTION_CATEGORY] y
 * ofrece las que devuelva; al confirmar, reproduce con el motor real (`engine.submit`).
 */
class SandboxController(private val registry: AssetRegistry) : CombatSceneController {

    override val state: CombatSceneState = CombatSceneState().apply {
        hudTitle = "Board Simulator — arma «Evolution» y arrastra una carta sobre otra"
        // Cartas reales fijas: Bulbasaur, Ivysaur, Venusaur (identidad local; el Studio no enlaza data:cards).
        cards += SceneCard("bulbasaur", "Bulbasaur", Offset(0.42f, 0.49f), widthFrac = 0.14f) // sobre el Activo
        cards += SceneCard("ivysaur", "Ivysaur", Offset(0.20f, 0.80f), widthFrac = 0.14f)     // en la Mano
        cards += SceneCard("venusaur", "Venusaur", Offset(0.66f, 0.80f), widthFrac = 0.14f)
        cards.forEach { zOrder += it.id }
        actions += SANDBOX_ACTIONS
    }

    private var engine: CombatSceneEngine? = null
    /** Variantes ofrecidas en el overlay actual (mapa opción→request), obtenidas del Registry. */
    private var overlayAssets: List<AnimationAsset> = emptyList()

    override fun attachEngine(engine: CombatSceneEngine) {
        this.engine = engine
    }

    override fun onEvent(event: CombatSceneEvent) {
        when (event) {
            is CombatSceneEvent.CardDragStart -> state.bringToFront(event.cardId)

            is CombatSceneEvent.CardDrag -> state.cardById(event.cardId)?.let { card ->
                card.position = Offset(
                    (card.position.x + event.deltaFrac.x).coerceIn(0f, 0.95f),
                    (card.position.y + event.deltaFrac.y).coerceIn(0f, 0.95f),
                )
            }

            is CombatSceneEvent.CardDrop -> maybeEvolve(event.cardId)

            is CombatSceneEvent.ActionClick ->
                state.armedActionId = if (state.armedActionId == event.actionId) null else event.actionId

            is CombatSceneEvent.OverlayOption ->
                state.overlay = state.overlay?.copy(selectedId = event.optionId)

            CombatSceneEvent.OverlayConfirm -> {
                val selected = overlayAssets.firstOrNull { it.id == state.overlay?.selectedId }
                selected?.let { engine?.submit(it.request) }
                state.overlay = null
            }

            CombatSceneEvent.OverlayDismiss -> state.overlay = null
        }
    }

    /** Si Evolution está armada y la carta soltada se solapa con otra, abre el selector de variantes. */
    private fun maybeEvolve(droppedId: String) {
        if (state.armedActionId != "evolution") return
        val dropped = state.cardById(droppedId) ?: return
        val target = state.cards.firstOrNull { other ->
            other.id != droppedId && overlaps(dropped.position, other.position)
        } ?: return

        val variants = registry.byCategory(AssetType.Animation, EVOLUTION_CATEGORY).filterIsInstance<AnimationAsset>()
        overlayAssets = variants
        state.overlay = SceneOverlay(
            title = "Evolución — ${dropped.label} sobre ${target.label}",
            subtitle = "Categoría «$EVOLUTION_CATEGORY» · ${variants.size} variantes del Asset Registry",
            options = variants.map { SceneOption(it.id, it.name, highlighted = it.status == AssetStatus.Canon) },
            selectedId = variants.firstOrNull()?.id.orEmpty(),
        )
    }

    private fun overlaps(a: Offset, b: Offset): Boolean =
        kotlin.math.abs(a.x - b.x) < 0.12f && kotlin.math.abs(a.y - b.y) < 0.16f
}

/** Categoría de Asset que la acción Evolution consulta en el Registry. El Sandbox sólo conoce esto. */
const val EVOLUTION_CATEGORY = "Evolución"

/**
 * Catálogo GENÉRICO de acciones del Sandbox (sólo Evolution implementada). Es el punto de extensión
 * del laboratorio: nuevas capacidades futuras (partículas, sonidos, cámaras…) se añaden aquí como
 * acciones sobre la MISMA Combat Scene, sin tocar la escena ni el render.
 */
val SANDBOX_ACTIONS: List<SceneAction> = listOf(
    SceneAction("draw", "Draw Card", enabled = false),
    SceneAction("evolution", "Evolution", enabled = true),
    SceneAction("play", "Play Pokémon", enabled = false),
    SceneAction("energy", "Attach Energy", enabled = false),
    SceneAction("tool", "Attach Tool", enabled = false),
    SceneAction("retreat", "Retreat", enabled = false),
    SceneAction("ko", "Knock Out", enabled = false),
    SceneAction("prize", "Prize", enabled = false),
    SceneAction("shuffle", "Shuffle", enabled = false),
    SceneAction("coin", "Coin", enabled = false),
    SceneAction("dice", "Dice", enabled = false),
    SceneAction("ability", "Ability", enabled = false),
    SceneAction("attack", "Attack", enabled = false),
)
