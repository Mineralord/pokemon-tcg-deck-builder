package com.mineralord.tcg.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mineralord.tcg.core.animationcompose.AnimationRenderState
import com.mineralord.tcg.core.animationcompose.AnimationStage
import com.mineralord.tcg.core.animationcompose.CoordinateRegistry
import com.mineralord.tcg.core.animationcompose.LocalCoordinateRegistry
import com.mineralord.tcg.core.animationcompose.activeSlotId
import com.mineralord.tcg.core.animationcompose.rememberCanonicalAnimationDirector
import com.mineralord.tcg.core.animationcompose.trackBounds
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.feature.game.board.BoardGeometry
import com.mineralord.tcg.feature.game.combat.CombatScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * **Board Simulator — Host: pantalla de combate REAL + panel Asset Tool (flujo herramienta-primero).**
 *
 * Hospeda EXACTAMENTE la misma `CombatScreen` que el juego (conducida por [SandboxController]) y le
 * añade el panel [AssetToolPanel] permanente. El flujo es el de una herramienta profesional: se elige
 * y activa un Asset ANTES de tocar el tablero; después, cada interacción compatible ejecuta el Asset
 * activo automáticamente (sin diálogos), por el MISMO motor canónico (`AnimationDirector`).
 *
 * El Studio sólo coordina: `SandboxController` traduce las intenciones de la pantalla a
 * [BoardInteraction] neutrales, [ToolController] decide si la herramienta activa las acepta y ejecuta
 * su asset, y el overlay reproduce la animación alineada con la zona Activa del tapete.
 */
@Composable
fun BoardSimulatorLabContent() {
    val registry = remember { studioAssetRegistry() }
    val toolController = remember { ToolController(registry, studioTools()) }
    val sandbox = remember { SandboxController(onInteraction = { toolController.onInteraction(it) }) }

    // Cartas reales fuera del hilo principal.
    LaunchedEffect(sandbox) {
        val repo = withContext(Dispatchers.Default) { CardRepository.load() }
        sandbox.seed(repo)
    }

    // Motor canónico (mismo pipeline que el juego); se lo entregamos a la herramienta activa.
    val coordinates = remember { CoordinateRegistry() }
    val renderState = remember { AnimationRenderState() }
    val director = rememberCanonicalAnimationDirector(coordinates, renderState)
    LaunchedEffect(director) { toolController.attachEngine { request -> director.submit(request) } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        if (compact) {
            Column(modifier = Modifier.fillMaxSize()) {
                BoardWithPlayback(sandbox, coordinates, renderState, Modifier.fillMaxWidth().weight(1f))
                AssetToolPanel(toolController, onReset = { sandbox.reset() }, modifier = Modifier.fillMaxWidth().height(220.dp))
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                BoardWithPlayback(sandbox, coordinates, renderState, Modifier.fillMaxHeight().weight(1f))
                AssetToolPanel(toolController, onReset = { sandbox.reset() }, modifier = Modifier.fillMaxHeight().width(260.dp))
            }
        }
    }
}

/** La pantalla real + el overlay de reproducción de assets (capa de vuelo alineada al tapete). */
@Composable
private fun BoardWithPlayback(
    sandbox: SandboxController,
    coordinates: CoordinateRegistry,
    renderState: AnimationRenderState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        CombatScreen(onExit = {}, vm = sandbox)
        AssetPlaybackOverlay(coordinates, renderState)
    }
}

/**
 * Overlay transparente que replica el mismo encuadre (letterbox 1080:2400 centrado) que la
 * `CombatScreen` y rastrea la zona **Activa** ([BoardGeometry.MeActive]) como slot del motor. Así el
 * asset activo (p. ej. una evolución) se reproduce sobre el Pokémon Activo real. No intercepta toques.
 */
@Composable
private fun AssetPlaybackOverlay(coordinates: CoordinateRegistry, renderState: AnimationRenderState) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availW = maxWidth
        val availH = maxHeight
        val boardW: Dp
        val boardH: Dp
        if (availW <= availH * BoardGeometry.Aspect) {
            boardW = availW; boardH = availW / BoardGeometry.Aspect
        } else {
            boardH = availH; boardW = availH * BoardGeometry.Aspect
        }
        Box(modifier = Modifier.size(boardW, boardH).align(Alignment.Center)) {
            AnimationStage(
                registry = coordinates,
                renderState = renderState,
                board = {
                    val reg = LocalCoordinateRegistry.current
                    val b = BoardGeometry.MeActive
                    Box(
                        Modifier
                            .offset(x = boardW * b.x, y = boardH * b.y)
                            .size(width = boardW * b.w, height = boardH * b.h)
                            .trackBounds(activeSlotId("studio"), reg),
                    )
                },
            )
        }
    }
}
