package com.mineralord.tcg.core.combatscene

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.mineralord.tcg.core.animation.AnimationRequest

/**
 * **Contrato de la Combat Scene: separa el RENDER (escena) del CONTROL (controlador).**
 *
 * La [CombatScene] dibuja; el [CombatSceneController] decide. La escena no sabe quién la usa: el
 * juego provee un Game Controller (dirigido por reglas) y el Studio un Sandbox Controller (manual).
 * Ambos conducen la MISMA escena. El controlador NUNCA dibuja: sólo muta el [CombatSceneState] y
 * reacciona a [CombatSceneEvent]; para reproducir animaciones usa el [CombatSceneEngine] que la
 * escena le entrega (el motor canónico compartido).
 */
interface CombatSceneController {
    /** Estado visual observable que la escena renderiza. Lo posee y muta el controlador. */
    val state: CombatSceneState

    /** La escena inyecta su motor (una sola vez) para que el controlador pueda solicitar animaciones. */
    fun attachEngine(engine: CombatSceneEngine)

    /** Interacciones del usuario capturadas por la escena. El controlador decide qué hacer. */
    fun onEvent(event: CombatSceneEvent)
}

/** Acceso mínimo al motor real (mismo pipeline que el juego): reproducir una animación. */
interface CombatSceneEngine {
    fun submit(request: AnimationRequest)
}

/** Interacciones que la escena reporta al controlador. Deltas de arrastre en fracción del tapete (0..1). */
sealed interface CombatSceneEvent {
    data class CardDragStart(val cardId: String) : CombatSceneEvent
    data class CardDrag(val cardId: String, val deltaFrac: Offset) : CombatSceneEvent
    data class CardDrop(val cardId: String) : CombatSceneEvent
    data class ActionClick(val actionId: String) : CombatSceneEvent
    data class OverlayOption(val optionId: String) : CombatSceneEvent
    data object OverlayConfirm : CombatSceneEvent
    data object OverlayDismiss : CombatSceneEvent
}

/**
 * Estado visual de la escena (observable). Es el único lenguaje entre controlador y render: el
 * controlador coloca cartas, acciones y overlays; la escena los dibuja. Sin lógica de reglas.
 */
@Stable
class CombatSceneState {
    /** Título del HUD. */
    var hudTitle by mutableStateOf("")

    /** Cartas en el tapete (posición normalizada 0..1 respecto al área de tapete). */
    val cards: MutableList<SceneCard> = mutableStateListOf()

    /** Botones/acciones del panel (genéricos; su semántica la da el controlador). */
    val actions: MutableList<SceneAction> = mutableStateListOf()

    /** Id de la acción armada (resaltada), o null. */
    var armedActionId by mutableStateOf<String?>(null)

    /** Overlay modal activo (p. ej. selector de variantes), o null si no hay ninguno. */
    var overlay by mutableStateOf<SceneOverlay?>(null)

    /** Orden Z de las cartas (fondo→frente por id). */
    val zOrder: MutableList<String> = mutableStateListOf()

    fun bringToFront(cardId: String) {
        zOrder.remove(cardId)
        zOrder.add(cardId)
    }

    fun zIndexOf(cardId: String): Float = zOrder.indexOf(cardId).toFloat()

    fun cardById(cardId: String): SceneCard? = cards.firstOrNull { it.id == cardId }
}

/** Una carta en el tapete. [position] es la esquina superior-izquierda normalizada (0..1). */
@Stable
class SceneCard(
    val id: String,
    val label: String,
    position: Offset,
    val widthFrac: Float,
) {
    var position by mutableStateOf(position)
}

/** Acción del panel (genérica). [enabled] = false ⇒ visible pero marcada como no disponible aún. */
data class SceneAction(
    val id: String,
    val label: String,
    val enabled: Boolean = true,
)

/** Overlay modal con una lista de opciones seleccionables + confirmar/cancelar. */
data class SceneOverlay(
    val title: String,
    val subtitle: String,
    val options: List<SceneOption>,
    val selectedId: String,
    val confirmLabel: String = "Play",
)

/** Una opción del overlay (p. ej. una variante de animación del Asset Registry). */
data class SceneOption(
    val id: String,
    val label: String,
    val note: String? = null,
    val highlighted: Boolean = false,
)
