package com.mineralord.tcg.studio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mineralord.tcg.core.animationcompose.AnimationRenderState
import com.mineralord.tcg.core.animationcompose.AnimationStage
import com.mineralord.tcg.core.animationcompose.CoordinateRegistry
import com.mineralord.tcg.core.animationcompose.LocalCoordinateRegistry
import com.mineralord.tcg.core.animationcompose.activeSlotId
import com.mineralord.tcg.core.animationcompose.rememberCanonicalAnimationDirector
import com.mineralord.tcg.core.animationcompose.trackBounds
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.feature.game.board.BoardGeometry
import com.mineralord.tcg.feature.game.combat.CombatScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * **Board Simulator — Host: pantalla de combate REAL a pantalla completa + consola flotante.**
 *
 * La `CombatScreen` (la misma del juego, conducida por [SandboxController]) ocupa SIEMPRE el 100% de
 * la pantalla: **ningún panel la reduce**. Las herramientas viven en un **bottom sheet superpuesto**
 * ([StudioSheet]) que por defecto sólo muestra un pequeño **grab handle** centrado abajo; se abre
 * deslizando hacia arriba o tocando el tirador, y se cierra deslizando hacia abajo o tocándolo. Al
 * abrirse, el tablero se difumina por detrás (translúcido, estilo Material) pero permanece visible.
 * Sólo se usa la infraestructura de animación de Compose del proyecto (Animatable + Modifier.blur).
 */
@Composable
fun BoardSimulatorLabContent() {
    val registry = remember { studioAssetRegistry() }
    val toolController = remember { ToolController(registry, studioTools()) }
    val sandbox = remember { SandboxController(onInteraction = { toolController.onInteraction(it) }) }

    LaunchedEffect(sandbox) {
        val repo = withContext(Dispatchers.Default) { CardRepository.load() }
        sandbox.seed(repo)
    }

    val coordinates = remember { CoordinateRegistry() }
    val renderState = remember { AnimationRenderState() }
    val director = rememberCanonicalAnimationDirector(coordinates, renderState)
    LaunchedEffect(director) { toolController.attachEngine { request -> director.submit(request) } }

    val density = LocalDensity.current
    val travelPx = with(density) { (SheetHeight - HandleStrip).toPx() }
    val sheet = remember(travelPx) { StudioSheetState(travelPx) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Tablero a PANTALLA COMPLETA (nunca se redimensiona). Se difumina por detrás al abrir la consola.
        Box(modifier = Modifier.fillMaxSize().blur((sheet.progress * 16f).dp)) {
            CombatScreen(onExit = {}, vm = sandbox)
            AssetPlaybackOverlay(coordinates, renderState)
        }

        // Scrim translúcido sólo cuando la consola está (parcialmente) abierta: atenúa y captura el
        // toque fuera para cerrar. Cerrado (progress 0) no existe → el tablero recibe todos los toques.
        if (sheet.progress > 0.001f) {
            val scope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = sheet.progress * 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { scope.launch { sheet.close() } },
            )
        }

        // La consola flotante (bottom sheet) con el grab handle y el contenido de herramientas.
        StudioSheet(sheet) {
            AssetToolPanel(toolController, onReset = { sandbox.reset() }, modifier = Modifier.fillMaxSize())
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Sheet flotante + Grab Handle
// ─────────────────────────────────────────────────────────────────────────────

private val SheetHeight: Dp = 360.dp
private val HandleStrip: Dp = 34.dp
private val SheetMaxWidth: Dp = 560.dp

/**
 * Estado del bottom sheet: una traslación vertical animada entre 0 (abierto) y [travelPx] (cerrado,
 * sólo el grab handle visible). [progress] va de 0 (cerrado) a 1 (abierto) y alimenta el blur/scrim.
 */
private class StudioSheetState(val travelPx: Float) {
    val translateY = Animatable(travelPx) // arranca cerrado
    val progress: Float get() = (1f - (translateY.value / travelPx)).coerceIn(0f, 1f)

    suspend fun open() = translateY.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
    suspend fun close() = translateY.animateTo(travelPx, tween(300, easing = FastOutSlowInEasing))
    suspend fun toggle() = if (progress > 0.5f) close() else open()
    suspend fun drag(deltaPx: Float) = translateY.snapTo((translateY.value + deltaPx).coerceIn(0f, travelPx))
    suspend fun settle() = if (translateY.value < travelPx / 2f) open() else close()
}

/**
 * Consola flotante anclada abajo. El grab handle (cápsula) queda SIEMPRE visible; deslizar arriba/abajo
 * sobre él (o tocarlo) abre/cierra. El fondo es translúcido para seguir viendo el tablero difuminado.
 */
@Composable
private fun StudioSheet(state: StudioSheetState, content: @Composable () -> Unit) {
    val scheme = StudioTheme.colors
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sheetWidth = if (maxWidth > SheetMaxWidth) SheetMaxWidth else maxWidth
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(sheetWidth)
                .height(SheetHeight)
                .graphicsLayer { translationY = state.translateY.value }
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                // Translúcido (estilo Material moderno): el tablero difuminado se ve por detrás.
                .background(scheme.surfacePanel.copy(alpha = 0.82f)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                GrabHandle(
                    scheme = scheme,
                    onTap = { scope.launch { state.toggle() } },
                    onDrag = { dy -> scope.launch { state.drag(dy) } },
                    onDragEnd = { scope.launch { state.settle() } },
                )
                Box(modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }
}

/** Grab handle minimalista: una cápsula centrada, semitransparente, sobre una franja de arrastre. */
@Composable
private fun GrabHandle(
    scheme: com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens.Scheme,
    onTap: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HandleStrip)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dy -> change.consume(); onDrag(dy) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(scheme.contentMuted.copy(alpha = 0.6f)),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overlay de reproducción de assets (capa de vuelo alineada al tapete real)
// ─────────────────────────────────────────────────────────────────────────────

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
