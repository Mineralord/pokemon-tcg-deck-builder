package com.mineralord.tcg.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.feature.game.combat.CombatScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * **Board Simulator — Host de la pantalla de combate REAL (ya NO renderiza nada).**
 *
 * Tras la unificación (Studio adopts Existing Combat Screen), el Board Simulator no dibuja tapete
 * propio: hospeda EXACTAMENTE la misma `CombatScreen` que usa Pokémon TCG Clone, conducida por un
 * [SandboxController] en lugar del `GameController` del juego. Es el `ExistingCombatScreen(controller
 * = SandboxController)` que pediste: misma pantalla, misma composición, mismo render; la única
 * diferencia es el controlador.
 *
 * Cualquier mejora visual en la pantalla de combate del juego se refleja aquí automáticamente.
 */
@Composable
fun BoardSimulatorLabContent() {
    val controller = remember { SandboxController() }
    // Carga de cartas fuera del hilo principal; siembra el tapete real cuando esté lista.
    LaunchedEffect(controller) {
        val repo = withContext(Dispatchers.Default) { CardRepository.load() }
        controller.seed(repo)
    }
    CombatScreen(onExit = {}, vm = controller)
}
