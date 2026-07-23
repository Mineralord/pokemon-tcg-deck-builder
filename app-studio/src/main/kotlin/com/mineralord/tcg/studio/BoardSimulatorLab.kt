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
import com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.feature.game.board.BoardGeometry
import com.mineralord.tcg.feature.game.combat.CombatScreen
import com.mineralord.tcg.studio.assets.AssetRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * **Sesión del Board Simulator: el estado compartido entre Workspace y Presentation.**
 *
 * Hoistada por encima del Shell (en `MainActivity`): existe **una única CombatScreen** y un único
 * controlador vivos; al conmutar de modo no se reinicia ni se duplica nada, sólo cambia el marco.
 */
class BoardSimSession(
    val registry: AssetRegistry,
    val sandbox: SandboxController,
    val toolController: ToolController,
    val coordinates: CoordinateRegistry,
    val renderState: AnimationRenderState,
)

/** Crea (una vez) y recuerda la sesión: controladores, motor canónico y siembra del tapete real. */
@Composable
fun rememberBoardSimSession(): BoardSimSession {
    val registry = remember { studioAssetRegistry() }
    val toolController = remember { ToolController(registry, studioTools()) }
    val sandbox = remember { SandboxController(onInteraction = { toolController.onInteraction(it) }) }
    val coordinates = remember { CoordinateRegistry() }
    val renderState = remember { AnimationRenderState() }
    val director = rememberCanonicalAnimationDirector(coordinates, renderState)

    LaunchedEffect(Unit) {
        val repo = withContext(Dispatchers.Default) { CardRepository.load() }
        sandbox.seed(repo)
    }
    LaunchedEffect(director) { toolController.attachEngine { request -> director.submit(request) } }

    return remember { BoardSimSession(registry, sandbox, toolController, coordinates, renderState) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Workspace Mode: tapete a pantalla completa (en el Host) + consola con grab handle fijo.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BoardSimulatorLabContent(session: BoardSimSession, onEnterPresentation: () -> Unit) {
    val density = LocalDensity.current
    val sheetPx = with(density) { SheetHeight.toPx() }
    val handlePx = with(density) { HandleStrip.toPx() }
    // En Workspace el grab handle es PERMANENTE: la posición de reposo es "peek" (handle visible).
    val sheet = remember(sheetPx) { StudioSheetState(sheetPx, handlePx, start = sheetPx - handlePx) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().blur((sheet.openProgress * 16f).dp)) {
            CombatScreen(onExit = { scope.launch { sheet.open() } }, vm = session.sandbox)
            AssetPlaybackOverlay(session.coordinates, session.renderState)
        }
        Scrim(sheet) { scope.launch { sheet.peek() } }
        StudioSheet(sheet, closedIsGone = false) {
            AssetToolPanel(
                controller = session.toolController,
                onReset = { session.sandbox.reset() },
                presentationToggleLabel = "Entrar en Presentation Mode",
                onPresentationToggle = onEnterPresentation,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Presentation Mode: CombatScreen limpia (indistinguible del juego). Sin marco, sin
// controles ni grab handle permanentes. El acceso al Workspace es el BOTÓN DE OPCIONES
// del propio tapete (onExit de CombatScreen); el grab handle asoma ~2 s al entrar y se oculta.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PresentationWorkspace(session: BoardSimSession, onExitPresentation: () -> Unit) {
    val density = LocalDensity.current
    val sheetPx = with(density) { SheetHeight.toPx() }
    val handlePx = with(density) { HandleStrip.toPx() }
    // Reposo = OCULTO (fuera de pantalla, ni siquiera el handle): pantalla completamente limpia.
    val sheet = remember(sheetPx) { StudioSheetState(sheetPx, handlePx, start = sheetPx - handlePx) }
    val scope = rememberCoroutineScope()

    // Al entrar: el grab handle asoma ~2 s (pista visual) y luego se oculta con animación suave.
    LaunchedEffect(Unit) {
        delay(2000)
        if (sheet.openProgress < 0.01f) sheet.gone()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().blur((sheet.openProgress * 16f).dp)) {
            // El BOTÓN DE OPCIONES del tapete (onExit) abre el Workspace del Studio. En el juego este
            // mismo callback hace lo suyo (salir de la partida): la pantalla es idéntica.
            CombatScreen(onExit = { scope.launch { sheet.open() } }, vm = session.sandbox)
            AssetPlaybackOverlay(session.coordinates, session.renderState)
        }
        // Al cerrar en Presentation, el sheet se oculta del TODO (handle incluido) → pantalla limpia.
        Scrim(sheet) { scope.launch { sheet.gone() } }
        StudioSheet(sheet, closedIsGone = true) {
            AssetToolPanel(
                controller = session.toolController,
                onReset = { session.sandbox.reset() },
                presentationToggleLabel = "Salir de Presentation Mode",
                onPresentationToggle = onExitPresentation,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Sheet flotante + Grab Handle (tres anclas: oculto / peek / abierto)
// ─────────────────────────────────────────────────────────────────────────────

private val SheetHeight: Dp = 360.dp
private val HandleStrip: Dp = 34.dp
private val SheetMaxWidth: Dp = 560.dp

/**
 * Traslación vertical animada del sheet: 0 = abierto · [peekPx] = sólo el grab handle asoma ·
 * [sheetPx] = oculto por completo (ni el handle). [openProgress] (0→1) mide sólo la apertura del
 * CONTENIDO (para el blur/scrim); el tramo peek↔oculto no cuenta como apertura.
 */
private class StudioSheetState(val sheetPx: Float, val handlePx: Float, start: Float) {
    val translateY = Animatable(start)
    val peekPx: Float get() = sheetPx - handlePx
    val openProgress: Float get() = (1f - (translateY.value / peekPx)).coerceIn(0f, 1f)

    private val spec = tween<Float>(300, easing = FastOutSlowInEasing)
    suspend fun open() = translateY.animateTo(0f, spec)
    suspend fun peek() = translateY.animateTo(peekPx, spec)
    suspend fun gone() = translateY.animateTo(sheetPx, spec)
    suspend fun drag(deltaPx: Float) = translateY.snapTo((translateY.value + deltaPx).coerceIn(0f, sheetPx))
    suspend fun settle(closedIsGone: Boolean) =
        translateY.animateTo(if (translateY.value < peekPx / 2f) 0f else if (closedIsGone) sheetPx else peekPx, spec)
}

@Composable
private fun Scrim(sheet: StudioSheetState, onTapOutside: () -> Unit) {
    if (sheet.openProgress > 0.001f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = sheet.openProgress * 0.28f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTapOutside() },
        )
    }
}

@Composable
private fun StudioSheet(state: StudioSheetState, closedIsGone: Boolean, content: @Composable () -> Unit) {
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
                .background(scheme.surfacePanel.copy(alpha = 0.82f)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                GrabHandle(
                    scheme = scheme,
                    onTap = { scope.launch { if (state.openProgress > 0.5f) (if (closedIsGone) state.gone() else state.peek()) else state.open() } },
                    onDrag = { dy -> scope.launch { state.drag(dy) } },
                    onDragEnd = { scope.launch { state.settle(closedIsGone) } },
                )
                Box(modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }
}

@Composable
private fun GrabHandle(
    scheme: StudioColorTokens.Scheme,
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
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp).height(5.dp)
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
