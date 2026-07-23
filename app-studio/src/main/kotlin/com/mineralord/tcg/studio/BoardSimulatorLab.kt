package com.mineralord.tcg.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.mineralord.tcg.core.designsystem.tokens.ButtonStyle
import com.mineralord.tcg.core.designsystem.tokens.ButtonVariant
import com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.core.designsystem.tokens.StudioTypographyTokens
import com.mineralord.tcg.core.designsystem.tokens.VisualState
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.feature.game.board.BoardGeometry
import com.mineralord.tcg.feature.game.combat.CombatScreen
import com.mineralord.tcg.studio.assets.AssetRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * **Sesión del Board Simulator: el estado compartido entre Workspace e Immersive.**
 *
 * Hoistada por encima del Shell (en `MainActivity`), garantiza que exista **una única CombatScreen**
 * y un único controlador vivos: al conmutar de modo no se reinicia ni se duplica nada; sólo cambia el
 * marco (Studio) que la rodea.
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
// Workspace Mode: tapete a pantalla completa (dentro del Host) + consola flotante.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Board Simulator en **modo Workspace**: la `CombatScreen` ocupa todo el Host y las herramientas viven
 * en un bottom sheet flotante ([StudioSheet]) con grab handle. Desde la consola se entra al modo
 * inmersivo ([onEnterImmersive]).
 */
@Composable
fun BoardSimulatorLabContent(session: BoardSimSession, onEnterImmersive: () -> Unit) {
    val density = LocalDensity.current
    val travelPx = with(density) { (SheetHeight - HandleStrip).toPx() }
    val sheet = remember(travelPx) { StudioSheetState(travelPx) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().blur((sheet.progress * 16f).dp)) {
            CombatScreen(onExit = {}, vm = session.sandbox)
            AssetPlaybackOverlay(session.coordinates, session.renderState)
        }

        if (sheet.progress > 0.001f) {
            val scope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = sheet.progress * 0.28f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        scope.launch {sheet.close() }
                    },
            )
        }

        StudioSheet(sheet) {
            AssetToolPanel(
                controller = session.toolController,
                onReset = { session.sandbox.reset() },
                onEnterImmersive = onEnterImmersive,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Immersive Mode: SÓLO la CombatScreen (sin nada del Studio) + controles temporales.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Modo **inmersivo**: reutiliza EXACTAMENTE la misma `CombatScreen` (mismo controlador de [session])
 * a sangre, sin marco, bordes ni superficies del Studio — indistinguible del juego. Los controles
 * para salir aparecen sólo temporalmente (estilo reproductor de vídeo): visibles al entrar, y luego
 * revelables con un gesto desde el borde superior; se auto-ocultan tras unos segundos sin uso.
 */
@Composable
fun ImmersiveWorkspace(session: BoardSimSession, onExit: () -> Unit) {
    val scheme = StudioTheme.colors
    var controlsVisible by remember { mutableStateOf(true) } // visibles al entrar para descubrir la salida
    var revealNonce by remember { mutableStateOf(0) }

    // Auto-ocultado tras 3.5 s sin interacción (se reinicia con cada revelado).
    LaunchedEffect(controlsVisible, revealNonce) {
        if (controlsVisible) {
            delay(3500)
            controlsVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ÚNICA CombatScreen, a sangre completa.
        CombatScreen(onExit = onExit, vm = session.sandbox)
        AssetPlaybackOverlay(session.coordinates, session.renderState)

        // Franja invisible en el borde superior: un swipe hacia abajo revela los controles (sin robar
        // espacio útil; no interfiere con el arrastre de cartas, que ocurre más abajo).
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(onVerticalDrag = { change, dy ->
                        if (dy > 1f) { controlsVisible = true; revealNonce++; change.consume() }
                    })
                },
        )

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(tween(260)) { -it } + fadeIn(tween(260)),
            exit = slideOutVertically(tween(260)) { -it } + fadeOut(tween(260)),
        ) {
            ImmersiveControlsBar(scheme, onExit = onExit, onInteract = { revealNonce++ })
        }
    }
}

/** Barra de controles temporal (translúcida) con el botón «Salir del modo inmersivo». */
@Composable
private fun ImmersiveControlsBar(
    scheme: StudioColorTokens.Scheme,
    onExit: () -> Unit,
    onInteract: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.42f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(
            text = "Modo inmersivo",
            style = StudioTypographyTokens.Role.Status.copy(color = Color.White.copy(alpha = 0.85f)),
        )
        val style = ButtonStyle(ButtonVariant.Neutral)
        val colors = style.colors(scheme, VisualState.Rest)
        Box(
            modifier = Modifier
                .height(style.metrics.height)
                .clip(RoundedCornerShape(style.metrics.radius))
                .background(colors.container)
                .clickable { onInteract(); onExit() }
                .padding(horizontal = style.metrics.paddingH),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text = "Salir del modo inmersivo", style = style.textStyle.copy(color = colors.content))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Sheet flotante + Grab Handle (Workspace)
// ─────────────────────────────────────────────────────────────────────────────

private val SheetHeight: Dp = 360.dp
private val HandleStrip: Dp = 34.dp
private val SheetMaxWidth: Dp = 560.dp

private class StudioSheetState(val travelPx: Float) {
    val translateY = Animatable(travelPx)
    val progress: Float get() = (1f - (translateY.value / travelPx)).coerceIn(0f, 1f)

    suspend fun open() = translateY.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
    suspend fun close() = translateY.animateTo(travelPx, tween(300, easing = FastOutSlowInEasing))
    suspend fun toggle() = if (progress > 0.5f) close() else open()
    suspend fun drag(deltaPx: Float) = translateY.snapTo((translateY.value + deltaPx).coerceIn(0f, travelPx))
    suspend fun settle() = if (translateY.value < travelPx / 2f) open() else close()
}

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
                .background(scheme.surfacePanel.copy(alpha = 0.82f)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                GrabHandle(
                    scheme = scheme,
                    onTap = { scope.launch {state.toggle() } },
                    onDrag = { dy -> scope.launch {state.drag(dy) } },
                    onDragEnd = { scope.launch {state.settle() } },
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
