package com.mineralord.tcg.studio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.layout.padding
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
import com.mineralord.tcg.core.animationcompose.CoordinateRegistry
import com.mineralord.tcg.core.animationcompose.LocalCoordinateRegistry
import com.mineralord.tcg.core.animationcompose.activeSlotId
import com.mineralord.tcg.core.animationcompose.rememberCanonicalAnimationDirector
import com.mineralord.tcg.core.animationcompose.trackBounds
import com.mineralord.tcg.core.designsystem.actor.ActorVisualController
import com.mineralord.tcg.core.designsystem.actor.LocalActorVisuals
import androidx.compose.foundation.text.BasicText
import com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.core.designsystem.tokens.StudioTypographyTokens
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.feature.game.board.BoardGeometry
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineralord.tcg.feature.game.GameViewModel
import com.mineralord.tcg.feature.game.combat.CombatScreen
import com.mineralord.tcg.feature.game.combat.LocalCombatAutoMotion
import com.mineralord.tcg.studio.assets.AssetRegistry
import com.mineralord.tcg.studio.preparation.EmptyMat
import com.mineralord.tcg.studio.preparation.EventOverlay
import com.mineralord.tcg.studio.preparation.EventPlayerPanel
import com.mineralord.tcg.studio.preparation.EventPlayerState
import com.mineralord.tcg.studio.preparation.PREP_EVENTS
import com.mineralord.tcg.studio.preparation.cameraChannels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * **StudioSession: el estado compartido por los Labs del Studio (Event Lab · Match Builder).**
 *
 * Hoistada por encima del Shell (en `MainActivity`): existe **una única CombatScreen** y un único
 * controlador vivos; al conmutar de Lab o de modo no se reinicia ni se duplica nada, sólo cambia el marco.
 */
class StudioSession(
    val registry: AssetRegistry,
    val sandbox: SandboxController,
    val toolController: ToolController,
    val coordinates: CoordinateRegistry,
    val renderState: AnimationRenderState,
    /** Canal de actores visuales: permite que las animaciones controlen la carta REAL de la escena. */
    val actors: ActorVisualController,
    /** Reproductor UNIVERSAL de eventos del Event Catalog (compartido por Workspace y Presentation). */
    val player: EventPlayerState,
)

/** Crea (una vez) y recuerda la sesión: controladores, motor canónico y siembra del tapete real. */
@Composable
fun rememberStudioSession(): StudioSession {
    val registry = remember { studioAssetRegistry() }
    val toolController = remember { ToolController(registry, studioTools()) }
    val sandbox = remember { SandboxController(onInteraction = { toolController.onInteraction(it) }) }
    val coordinates = remember { CoordinateRegistry() }
    val renderState = remember { AnimationRenderState() }
    val actors = remember { ActorVisualController() }
    val player = remember { EventPlayerState(PREP_EVENTS) }
    val director = rememberCanonicalAnimationDirector(coordinates, renderState)

    LaunchedEffect(Unit) {
        val repo = withContext(Dispatchers.Default) { CardRepository.load() }
        sandbox.seed(repo)
    }
    LaunchedEffect(director) { toolController.attachEngine { request -> director.submit(request) } }

    return remember { StudioSession(registry, sandbox, toolController, coordinates, renderState, actors, player) }
}

/**
 * Monta la CombatScreen dentro del canal de actores + el registro de coordenadas del Studio, y añade
 * el rastreo de la zona Activa (para que el executor resuelva su rect) y el puente actor↔animación
 * ([EvolveActorBridge]). No dibuja placeholders: la carta real es el actor.
 */
@Composable
private fun ActorHostedBoard(session: StudioSession, onExit: () -> Unit) {
    // Actor id REACTIVO: la carta actualmente en el Activo. Se actualiza al aterrizar la evolución, de
    // modo que el puente controla SIEMPRE la carta real visible (corrige el actor obsoleto).
    val ui by session.sandbox.ui.collectAsState()
    val actorId = ui.state?.player?.active?.card?.id?.raw
    CompositionLocalProvider(
        LocalActorVisuals provides session.actors,
        LocalCoordinateRegistry provides session.coordinates,
        // El sistema de actores es el ÚNICO animador en el Studio: desactiva el vuelo/entrada internos
        // de la CombatScreen para que no compitan ni produzcan un desplazamiento residual tras el Drop.
        LocalCombatAutoMotion provides false,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CombatScreen(onExit = onExit, vm = session.sandbox)
            ActiveSlotTracker()
            EvolveActorBridge(session.renderState, session.actors, actorId)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reproductor de eventos COMPARTIDO por Workspace y Presentation (mismo pipeline de render).
// ─────────────────────────────────────────────────────────────────────────────

/** Reloj del reproductor: avanza mientras haya evento cargado y en reproducción (respeta loop/fin). */
@Composable
private fun EventPlayerClock(player: EventPlayerState) {
    LaunchedEffect(player.active, player.playing, player.speed, player.eventIndex, player.variantIndex) {
        if (!player.active || !player.playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dtMs = (now - last) / 1_000_000f * player.speed
            last = now
            var next = player.elapsed + dtMs
            if (next >= player.variant.totalMs) {
                if (player.loop) next = 0f else { player.elapsed = player.variant.totalMs; player.playing = false; break }
            }
            player.elapsed = next
        }
    }
}

/**
 * **Tablero del EVENT LAB (board-first).** Escenario OFICIAL vacío (`EmptyMat`) con la "cámara" del evento
 * aplicada + el overlay del fotograma. El Event Lab reproduce eventos sobre el escenario oficial; la
 * PARTIDA sembrada (CombatScreen real) NO vive aquí — pertenece a Match Builder (principio Un Lab = Un
 * Dominio). Sin evento cargado, se ve el escenario oficial vacío en reposo.
 */
@Composable
private fun EventLabBoard(session: StudioSession, sheetBlur: Float) {
    val player = session.player
    EventPlayerClock(player)
    val frame = if (player.active) player.frame() else null
    // Con un evento activo NO difuminamos el tablero por el sheet: hay que VER la cinemática.
    val boardBlur = if (player.active) 0f else sheetBlur
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().blur(boardBlur.dp).cameraChannels(frame?.channels)) {
            EmptyMat()
        }
        if (frame != null) EventOverlay(frame.channels, player.variant.id)
    }
}

/** Consola del Event Lab: SOLO el Event Player (catálogo + transporte) + acceso a Presentation Mode.
 *  Nada de partidas ni herramientas de asset (eso es Match Builder). */
@Composable
private fun EventConsole(session: StudioSession, presentationToggleLabel: String, onPresentationToggle: () -> Unit) {
    val scheme = StudioTheme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        EventPlayerPanel(session.player, scheme, Modifier.fillMaxWidth().padding(12.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(scheme.surfaceRaised)
                .clickable { onPresentationToggle() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            BasicText(presentationToggleLabel, style = StudioTypographyTokens.Role.Label.copy(color = scheme.contentEmphasis))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EVENT LAB — Workspace Mode: tablero protagonista + consola de eventos retráctil.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EventLabContent(session: StudioSession, onEnterPresentation: () -> Unit) {
    val density = LocalDensity.current
    val sheetPx = with(density) { SheetHeight.toPx() }
    val handlePx = with(density) { HandleStrip.toPx() }
    val sheet = remember(sheetPx) { StudioSheetState(sheetPx, handlePx, start = sheetPx - handlePx) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        EventLabBoard(session, sheetBlur = sheet.openProgress * 16f)
        if (!session.player.active) Scrim(sheet) { scope.launch { sheet.peek() } }
        StudioSheet(sheet, closedIsGone = false) {
            EventConsole(session, "Entrar en Presentation Mode", onEnterPresentation)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Presentation Mode: CombatScreen limpia (indistinguible del juego). Sin marco, sin
// controles ni grab handle permanentes. El acceso al Workspace es el BOTÓN DE OPCIONES
// del propio tapete (onExit de CombatScreen); el grab handle asoma ~2 s al entrar y se oculta.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PresentationWorkspace(session: StudioSession, onExitPresentation: () -> Unit) {
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
        // MISMO reproductor y escenario oficial: la cinemática del evento se ejecuta sobre el tablero limpio.
        EventLabBoard(session, sheetBlur = sheet.openProgress * 16f)
        // Al cerrar en Presentation, el sheet se oculta del TODO (handle incluido) → pantalla limpia.
        if (!session.player.active) Scrim(sheet) { scope.launch { sheet.gone() } }
        StudioSheet(sheet, closedIsGone = true) {
            EventConsole(session, "Salir de Presentation Mode", onExitPresentation)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MATCH BUILDER — dominio: la partida. MODO CONFIGURACIÓN (único modo por ahora).
// Absorbe la preparación del antiguo lab mixto: tablero sembrado real (CombatScreen) +
// herramientas de preparación (Nueva partida · Lanzar partida sembrada · Asset Tools).
// El «Modo Partida» (GameViewModel + IA + checkpoints) es el Punto 4 del roadmap: NO aquí.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MatchBuilderContent(
    session: StudioSession,
    onEnterFullscreen: (@Composable () -> Unit) -> Unit,
    onExitFullscreen: () -> Unit,
) {
    // Modo Configuración vive dentro del Shell (es una herramienta). "Jugar" cambia al Modo Partida,
    // que se presenta a PANTALLA COMPLETA (puentea el Shell, como Presentation Mode): una partida real
    // no debe llevar cromo del Studio. Mismo dominio (la partida), distinto MODO.
    MatchBuilderConfig(session, onPlay = {
        onEnterFullscreen { MatchBuilderPlay(session = session, onExitFullscreen = onExitFullscreen) }
    })
}

// ── Modo Configuración: preparar la partida (tablero sembrado + herramientas). ──────────────────
@Composable
private fun MatchBuilderConfig(session: StudioSession, onPlay: () -> Unit) {
    val density = LocalDensity.current
    val sheetPx = with(density) { SheetHeight.toPx() }
    val handlePx = with(density) { HandleStrip.toPx() }
    val sheet = remember(sheetPx) { StudioSheetState(sheetPx, handlePx, start = sheetPx - handlePx) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().blur((sheet.openProgress * 16f).dp)) {
            ActorHostedBoard(session, onExit = { scope.launch { sheet.open() } })
        }
        Scrim(sheet) { scope.launch { sheet.peek() } }
        StudioSheet(sheet, closedIsGone = false) {
            MatchBuilderConsole(session, onPlay = onPlay)
        }
    }
}

// ── Modo Partida: partida REAL vs IA con el controlador del juego (GameViewModel), misma CombatScreen,
// a PANTALLA COMPLETA. Reutiliza feature:match (GameEngine + SmartAgent) SIN duplicar lógica → FIN, ataques,
// etc. pasan por el MOTOR real. El botón de AJUSTES del tapete (onExit) abre la consola (Asset Tool); su
// botón de PANTALLA COMPLETA sale del modo (vuelve a Configuración). La consola arranca OCULTA (juego limpio).
@Composable
private fun MatchBuilderPlay(session: StudioSession, onExitFullscreen: () -> Unit) {
    val gameVm: GameViewModel = viewModel()
    val density = LocalDensity.current
    val sheetPx = with(density) { SheetHeight.toPx() }
    val handlePx = with(density) { HandleStrip.toPx() }
    val sheet = remember(sheetPx) { StudioSheetState(sheetPx, handlePx, start = sheetPx) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Motor real: el botón de Opciones del tapete (onExit) abre la consola (no sale del fullscreen).
        CombatScreen(onExit = { scope.launch { sheet.open() } }, vm = gameVm)
        Scrim(sheet) { scope.launch { sheet.gone() } }
        StudioSheet(sheet, closedIsGone = true) {
            AssetToolPanel(
                controller = session.toolController,
                onReset = { session.sandbox.reset() },
                fullscreenLabel = "⤢ Salir de pantalla completa",
                onToggleFullscreen = onExitFullscreen,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Consola del Match Builder · Modo Configuración: preparación de la partida + entrada al Modo Partida. */
@Composable
private fun MatchBuilderConsole(session: StudioSession, onPlay: () -> Unit) {
    val scheme = StudioTheme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        BasicText(
            "🧩 Match Builder · Modo Configuración",
            style = StudioTypographyTokens.Role.Title.copy(color = scheme.contentEmphasis),
            modifier = Modifier.padding(12.dp),
        )
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConfigButton("🆕 Nueva partida (sandbox)", scheme) { session.sandbox.reset() }
            ConfigButton("🎬 Partida sembrada (sandbox)", scheme) { session.sandbox.startRitual() }
        }
        // Botón de PANTALLA COMPLETA en el Asset Tool → entra al Modo Partida (partida REAL vs IA, con
        // motor: FIN/ataques funcionan). El sandbox de arriba es solo previsualización SIN motor.
        AssetToolPanel(
            controller = session.toolController,
            onReset = { session.sandbox.reset() },
            fullscreenLabel = "⤢ Pantalla completa · Jugar (vs IA)",
            onToggleFullscreen = onPlay,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun ConfigButton(label: String, scheme: StudioColorTokens.Scheme, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.surfaceRaised)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        BasicText(label, style = StudioTypographyTokens.Role.Label.copy(color = scheme.contentEmphasis))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PACK SIMULATOR — dominio: la apertura de sobres. Un botón entra al simulador a PANTALLA
// COMPLETA (puentea el Shell, como el Modo Partida): elegir expansión → rasgar → revelado.
// Reutiliza feature:packs (PackOpeningSimulator) SIN monedero ni límites.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PackSimulatorLabContent(
    onEnterFullscreen: (@Composable () -> Unit) -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val scheme = StudioTheme.colors
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        BasicText(
            "🎴 Simulador de Sobres",
            style = StudioTypographyTokens.Role.Title.copy(color = scheme.contentEmphasis),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        BasicText(
            "Abre sobres del set 151 sin límites ni monedero. Recorre el mismo flujo del juego:\n" +
                "elegir expansión → rasgar el sobre con el dedo → revelado carta a carta (holo, giro 3D\n" +
                "de raras y cinemática de carta nueva), todo a pantalla completa.",
            style = StudioTypographyTokens.Role.Label.copy(color = scheme.contentMuted),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(scheme.surfaceRaised)
                .clickable {
                    onEnterFullscreen {
                        com.mineralord.tcg.feature.packs.PackOpeningSimulator(onExit = onExitFullscreen)
                    }
                }
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            BasicText(
                "⤢  Abrir simulador (pantalla completa)",
                style = StudioTypographyTokens.Role.Label.copy(color = scheme.contentEmphasis),
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
// Rastreo de la zona Activa (para que el executor resuelva su rect). NO dibuja nada:
// la animación toma el control de la carta REAL vía el canal de actores.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActiveSlotTracker() {
    val reg = LocalCoordinateRegistry.current
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
            val b = BoardGeometry.MeActive
            Box(
                Modifier
                    .offset(x = boardW * b.x, y = boardH * b.y)
                    .size(width = boardW * b.w, height = boardH * b.h)
                    .trackBounds(activeSlotId("studio"), reg),
            )
        }
    }
}
