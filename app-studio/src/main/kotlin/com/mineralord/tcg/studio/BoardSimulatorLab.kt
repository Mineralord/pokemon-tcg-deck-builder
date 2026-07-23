package com.mineralord.tcg.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mineralord.tcg.core.combatscene.CombatScene
import com.mineralord.tcg.studio.assets.AssetRegistry

/**
 * **Board Simulator — Host de la Combat Scene compartida (ya NO renderiza nada).**
 *
 * Tras la unificación (Combat Scene Unification v1), el Board Simulator dejó de dibujar su propio
 * tapete: ahora es sólo un anfitrión que entrega el [SandboxController] a la ÚNICA `CombatScene` del
 * proyecto —la misma que ejecutará el juego—. Toda la representación visual (tapete, zonas, cartas,
 * HUD, botones, overlays, `AnimationStage`) vive en el núcleo compartido `core:combat-scene`.
 *
 * La única diferencia entre el Studio y el juego es el controlador: aquí un Sandbox Controller
 * (manual); allí un Game Controller (reglas). La escena no sabe quién la usa.
 */
@Composable
fun BoardSimulatorLabContent(registry: AssetRegistry) {
    val controller = remember(registry) { SandboxController(registry) }
    CombatScene(controller)
}
