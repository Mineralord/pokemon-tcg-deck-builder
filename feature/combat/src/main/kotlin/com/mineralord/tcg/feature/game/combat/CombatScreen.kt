package com.mineralord.tcg.feature.game.combat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.mineralord.tcg.feature.game.anim.FxCue
import com.mineralord.tcg.feature.game.anim.rememberLunge
import com.mineralord.tcg.feature.game.anim.rememberShake
import com.mineralord.tcg.feature.game.board.typeColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.TrainerCard
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mineralord.tcg.engine.model.Ability
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.rules.AbilityGlow
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.Target
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.feature.game.CoinFlipOverlay
import com.mineralord.tcg.feature.game.DealOverlay
import com.mineralord.tcg.feature.game.SetupUiState
import com.mineralord.tcg.feature.game.cardTargetsPokemon
import com.mineralord.tcg.feature.game.itemTargetChoose
import com.mineralord.tcg.feature.game.toolAttachScope
import com.mineralord.tcg.core.designsystem.CardDetailDialog
import com.mineralord.tcg.core.designsystem.actor.actorControlled
import com.mineralord.tcg.core.designsystem.motion.MotionContainer
import com.mineralord.tcg.core.designsystem.motion.MotionDialog
import com.mineralord.tcg.core.designsystem.motion.MotionEdge
import com.mineralord.tcg.core.designsystem.motion.MotionPanel
import com.mineralord.tcg.core.designsystem.motion.MotionTransitions
import com.mineralord.tcg.core.designsystem.motion.FlightHost
import com.mineralord.tcg.core.designsystem.motion.FlightOverlay
import com.mineralord.tcg.core.designsystem.motion.rememberFlightHost
import com.mineralord.tcg.core.designsystem.motion.motionAppear
import com.mineralord.tcg.core.designsystem.motion.motionElevate
import com.mineralord.tcg.core.designsystem.motion.motionPress
import com.mineralord.tcg.core.designsystem.EnergySphere
import com.mineralord.tcg.core.designsystem.typeColor
import com.mineralord.tcg.core.designsystem.tilt.resolveFinish
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animationcompose.AnimationRenderState
import com.mineralord.tcg.core.animationcompose.AnimationStage
import com.mineralord.tcg.core.animationcompose.AbilityGlowVisuals
import com.mineralord.tcg.core.animationcompose.CoordinateRegistry
import com.mineralord.tcg.core.animationcompose.PersistentGlow
import com.mineralord.tcg.core.animationcompose.SlotId
import com.mineralord.tcg.core.animationcompose.handSlotId
import com.mineralord.tcg.core.animationcompose.rememberCanonicalAnimationDirector
import com.mineralord.tcg.core.animationcompose.stadiumSlotId
import com.mineralord.tcg.core.animationcompose.trackBounds
import com.mineralord.tcg.feature.game.board.BoardGeometry
import com.mineralord.tcg.feature.game.board.HandCat
import com.mineralord.tcg.feature.game.board.HandFan
import com.mineralord.tcg.feature.game.board.HandGlow
import com.mineralord.tcg.feature.game.board.HandFilterBar
import com.mineralord.tcg.feature.game.board.handCatOf
import com.mineralord.tcg.feature.game.board.sortedHandCards

/**
 * Pantalla de combate canónica — replica 1:1 las MEDIDAS y POSICIONES de [BoardGeometry]
 * (mismas cajas que la pantalla clásica) pero es 100% DINÁMICA: el tapete se DIBUJA en
 * Compose ([CombatMat], arte propio, responsive) en vez del bitmap horneado, y cada zona
 * es un Composable colocado por su NBox. Sin assets de terceros.
 *
 * Contrato intacto: solo lee [CombatSceneController.ui] y emite por sus métodos. No toca motor,
 * reglas ni ViewModels. La pantalla es la MISMA para el juego y para el Studio; sólo cambia el
 * controlador que se le inyecta (el anfitrión provee el suyo; ya no hay `viewModel` por defecto).
 */
/**
 * **Auto-animaciones internas de la CombatScreen** (vuelo entre zonas + entrada "aterrizaje" de
 * cartas). Por defecto `true` ⇒ el juego se comporta igual que siempre. Un anfitrión que anime las
 * cartas por OTRO medio (el Studio, con su sistema de actores) lo pone en `false` para que estas
 * animaciones internas no compitan ni produzcan un segundo desplazamiento tras un cambio de estado.
 */
val LocalCombatAutoMotion = staticCompositionLocalOf { true }

@Composable
fun CombatScreen(
    onExit: () -> Unit,
    vm: CombatSceneController,
    modifier: Modifier = Modifier,
    /** Id del cosmético TAPETE equipado (Fase 3); null = tapete por defecto. */
    matThemeId: String? = null,
    /** Id del cosmético FUNDA equipado (Fase 3); null = dorso por defecto. */
    sleeveId: String? = null,
    /** Id del cosmético EFECTO DE VICTORIA equipado; null = confeti por defecto. */
    victoryEffectId: String? = null,
    /** Id del cosmético EFECTO DE DERROTA equipado; null = brasas por defecto. */
    defeatEffectId: String? = null,
    /** Recompensa PvE a mostrar al terminar (Fase 2 §6.8): Cristales/Monedas por victoria y derrota. */
    winCristales: Int = 0,
    winMonedas: Int = 0,
    lossCristales: Int = 0,
    lossMonedas: Int = 0,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val state = ui.state
    val autoMotion = LocalCombatAutoMotion.current
    // Cosméticos equipados aplicados a la presentación (tapete + funda). Neutros para la jugabilidad.
    val matTheme = matThemeFor(matThemeId)
    val sleeve = sleeveFor(sleeveId)

    // FX PREMIUM de ataque en el lente central (color+motivo por TIPO, ~1 s). SINCRONIZADO con
    // el IMPACTO de daño (FxCue.Damage): revienta sobre el Pokémon golpeado (el defensor =
    // cue.side) con el color/motivo del TIPO del ATACANTE (el activo del lado contrario).
    var lensFx by remember { mutableStateOf<LensFxSpec?>(null) }
    LaunchedEffect(Unit) {
        vm.fx.collect { cue ->
            if (cue is FxCue.Damage) {
                val st = vm.ui.value.state ?: return@collect
                // El defensor recibe el daño (cue.side); el atacante es el activo del OTRO lado.
                val attacker = if (cue.side == Side.PLAYER) st.opponent.active else st.player.active
                val type = attacker?.card?.types?.firstOrNull()
                lensFx = LensFxSpec(
                    id = System.nanoTime(),
                    color = typeColor(type),
                    motif = motifFor(type),
                    // fromPlayer = ataca el jugador → impacto arriba (activo rival golpeado).
                    fromPlayer = cue.side == Side.OPPONENT,
                )
            }
        }
    }

    // ---- Motor de animación CANÓNICO (rótulos AAA + auras de habilidad): MISMO pipeline que el
    // Studio (contribuidores/ejecutores canónicos). Dos anfitriones de dibujo: uno para las auras
    // (bajo las cartas) y otro para los rótulos (al frente). El anfitrión sólo aporta dónde dibujar.
    val bannerCoords = remember { CoordinateRegistry() }
    val bannerRender = remember { AnimationRenderState() }
    val bannerDirector = rememberCanonicalAnimationDirector(bannerCoords, bannerRender)
    val glowCoords = remember { CoordinateRegistry() }
    val glowRender = remember { AnimationRenderState() }
    val glowDirector = rememberCanonicalAnimationDirector(glowCoords, glowRender)
    // Vuelo de colocación de ESTADIO: la carta viaja de la mano al slot de Estadio y se asienta con
    // peso (familia canónica StadiumPlace, mismo motor que el Studio). Anfitrión propio para la capa
    // de vuelo del Estadio; las ranuras (mano origen + slot de Estadio) se rastrean abajo en su Stage.
    val stadiumCoords = remember { CoordinateRegistry() }
    val stadiumRender = remember { AnimationRenderState() }
    val stadiumDirector = rememberCanonicalAnimationDirector(stadiumCoords, stadiumRender)

    // Menú de PAUSA / OPCIONES (lo abre el botón superior-izquierdo, sobre el descarte rival). Los
    // ajustes se hoistean aquí para persistir durante la partida (rememberSaveable). Único para PvE/PvP.
    // · `cardVfxEnabled` (pestaña ACCESIBILIDAD): efectos visuales de animación de las cartas para
    //   ataques y Evolución. Cuando está DESACTIVADO se suprimen los rótulos/embate/sacudida/aura/estadio
    //   (leído en vivo por los colectores de FX de abajo).
    // · `disableVfx` (pestaña GENERAL → registro de combate): ajuste del registro; hoy solo estado.
    var showPause by remember { mutableStateOf(false) }
    var showSurrender by remember { mutableStateOf(false) }
    var musicVol by rememberSaveable { mutableStateOf(0.6f) }
    var sfxVol by rememberSaveable { mutableStateOf(0.6f) }
    var disableVfx by rememberSaveable { mutableStateOf(false) }
    var cardVfxEnabled by rememberSaveable { mutableStateOf(true) }

    // GRITO del Pokémon al ponerlo en juego / evolucionar (streaming + caché por nº de Pokédex).
    // El volumen sigue al deslizador de "Efectos de sonido"; se libera al salir del combate.
    val cryContext = androidx.compose.ui.platform.LocalContext.current
    val cryPlayer = remember { CryPlayer(cryContext) }
    LaunchedEffect(sfxVol) { cryPlayer.setVolume(sfxVol) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { cryPlayer.release() } }
    LaunchedEffect(Unit) {
        vm.fx.collect { cue ->
            // El grito oficial suena cuando el Pokémon USA un ataque (no al jugarlo/evolucionar).
            if (cue is FxCue.Attack) cue.attacker?.let { id ->
                (vm.card(id) as? PokemonCard)?.nationalDex?.let { cryPlayer.play(it) }
            }
        }
    }

    // Al jugar un Estadio (cualquiera de los dos lados): dispara la colocación AAA.
    LaunchedEffect(Unit) {
        vm.fx.collect { cue ->
            if (cue is FxCue.StadiumPlaced && cardVfxEnabled) {
                stadiumDirector.submit(AnimationRequest.StadiumPlaced(cue.side.name, cue.card.raw))
            }
        }
    }

    // Rótulo de ATAQUE: anuncio cinematográfico (dirección por lado). El impacto (FxCue.Damage) llega
    // después por el espaciado de `playFx`, cumpliendo el orden anuncio → acción.
    LaunchedEffect(Unit) {
        vm.fx.collect { cue ->
            if (cue is FxCue.Attack && cardVfxEnabled) {
                bannerDirector.submit(
                    AnimationRequest.AttackStarted(
                        attackerId = cue.side.name,
                        attackName = cue.attackName.ifBlank { "Ataque" },
                        incoming = cue.side == Side.OPPONENT,
                    ),
                )
            }
        }
    }

    // Habilidad (manual o pasiva/al evolucionar): rótulo con el NOMBRE DEL POKÉMON + aura sobre su
    // carta (dorado manual / rojo pasiva). Dirigido por el EVENTO del motor → también anima las
    // habilidades del rival y las pasivas automáticas. El efecto llega después (orden anuncio → acción).
    LaunchedEffect(Unit) {
        vm.fx.collect { cue ->
            if (cue is FxCue.AbilityUse && cardVfxEnabled) {
                bannerDirector.submit(
                    AnimationRequest.AbilityActivated(
                        sourcePokemonId = cue.pokemon.raw,
                        pokemonName = vm.cardName(cue.pokemon),
                        abilityName = "",
                        manual = cue.manual,
                    ),
                )
                glowDirector.submit(
                    AnimationRequest.AbilityGlowRequested(cue.pokemon.raw, glowSlot(cue.pokemon.raw).value, cue.manual),
                )
            }
        }
    }

    var inspect by remember { mutableStateOf<Card?>(null) }
    var zonePanel by remember { mutableStateOf<Pair<String, List<Card>>?>(null) }
    // Registro de batalla (lo abre el botón de lista+lupa de la columna derecha).
    var showLog by remember { mutableStateOf(false) }
    // Estado EFÍMERO de interacción del tablero (arrastres, bounds de zonas, foco de mano, anclas de
    // vuelo), agrupado para reducir el acoplamiento interno; se comparte entre el compositor y sus capas.
    val bi = remember { CombatBoardInteraction() }
    // EMBATE + SACUDIDA sincronizados con el IMPACTO (FxCue.Damage): el defensor (cue.side) se sacude
    // y el atacante (el otro lado) embiste hacia él. Llega tras el rótulo (playFx espacia Attack→Damage),
    // cumpliendo la jerarquía rótulo → embate/impacto → (KO). Se aplican en MatchLayer sobre cada Activo.
    LaunchedEffect(Unit) {
        vm.fx.collect { cue ->
            if (cue is FxCue.Damage && cardVfxEnabled) when (cue.side) {
                Side.PLAYER -> { bi.shakeSeqPlayer++; bi.lungeSeqOpp++ }
                Side.OPPONENT -> { bi.shakeSeqOpp++; bi.lungeSeqPlayer++ }
            }
        }
    }
    // ---- VUELO REAL entre zonas (capa Motion) ----
    // Host de vuelos + última clasificación de cartas para detectar movimientos por diffing de estado.
    val flightHost = rememberFlightHost()
    var prevZones by remember { mutableStateOf<Map<CardId, CardZone>?>(null) }
    // Reloj de partida por lado (banco de tiempo mm:ss) que descuenta en el lado ACTIVO,
    // como el temporizador de las pestañas de estado de TCG Live. Arranca en 25:00.
    var oppClock by remember { mutableStateOf(25 * 60) }
    var meClock by remember { mutableStateOf(25 * 60) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val u = vm.ui.value
            val s = u.state ?: continue
            if (s.winner != null || u.setup != null || u.revealing) continue
            if (s.activeSide == Side.PLAYER) meClock = (meClock - 1).coerceAtLeast(0)
            else oppClock = (oppClock - 1).coerceAtLeast(0)
        }
    }
    // El estado de mano (pendingPlay/foco), arrastre (dragCard/dragPos + bounds), y retirada
    // (retreatMode/benchDrag*) vive en `bi` (CombatBoardInteraction), compartido por las capas.

    ProvideCardSleeve(sleeve) {
    Box(modifier.fillMaxSize().background(Color(0xFF06080D))) {

        // ---- Ceremonia inicial: reutiliza los overlays existentes ----
        val coinFlip = ui.coinFlip
        if (coinFlip != null) {
            CoinFlipOverlay(coinFlip, onCall = vm::chooseCoin, onChooseFirst = vm::chooseFirst)
            return@Box
        }
        val dealing = ui.dealing
        if (dealing != null) {
            DealOverlay(cards = dealing.hand, onComplete = vm::onDealComplete)
            return@Box
        }
        if (state == null) {
            CombatLoading()
            return@Box
        }

        val setup = ui.setup
        val inSetup = setup != null
        val revealing = ui.revealing
        val mustPromote = state.pendingPromotion.contains(Side.PLAYER)
        val myTurn = state.activeSide == Side.PLAYER &&
            !revealing && !inSetup && state.interaction == null && !mustPromote

        // ---- VUELO REAL: diff de zonas entre estados → una carta que cambia de zona VUELA
        // desde su ancla de origen a la de destino con trayectoria física. Se omite durante
        // la ceremonia inicial (reparto/preparación/revelado) para no encadenar decenas de vuelos.
        LaunchedEffect(state, inSetup, revealing) {
            val now = classifyZones(state)
            val prev = prevZones
            val quiet = autoMotion && !inSetup && !revealing && ui.dealing == null
            if (prev != null && quiet) {
                val cards = allCardsById(state)
                now.forEach { (id, zone) ->
                    val old = prev[id]
                    if (old != null && old != zone) {
                        val from = bi.anchorBounds[old]
                        val to = bi.anchorBounds[zone]
                        val card = cards[id]
                        if (from != null && to != null && card != null) {
                            val faceUp = zone.faceUp
                            flightHost.launch(from, to) {
                                CombatCard(
                                    imageUrl = if (faceUp) card.artwork.small(true) else null,
                                    faceDown = !faceUp,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
            prevZones = now
        }

        // ---- Tablero proporcional 1:1 (bloqueado a 1080:2400, centrado) ----
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val availW = maxWidth
            val availH = maxHeight
            val boardW: Dp
            val boardH: Dp
            if (availW <= availH * BoardGeometry.Aspect) {
                boardW = availW; boardH = availW / BoardGeometry.Aspect
            } else {
                boardH = availH; boardW = availH * BoardGeometry.Aspect
            }

            Box(Modifier.size(boardW, boardH).align(Alignment.Center)) {
                // Tapete ORIGINAL dibujado, alineado a las bandas de BoardGeometry.
                // El riel del lado activo se enciende como tubo de neón (indicador de turno);
                // oculto en preparación/revelado/fin de partida.
                val litSide = if (inSetup || revealing || state.winner != null) null else state.activeSide
                // Tipo del Pokémon Activo en foco (lado en turno): alimenta el ambiente reactivo
                // de la lente central. Solo lectura del estado; el ambiente es pura presentación.
                val activeType = if (litSide == null) null
                    else state.activePlayer.active?.card?.types?.firstOrNull()
                SceneLayer(litSide = litSide, activeType = activeType, theme = matTheme, modifier = Modifier.matchParentSize())

                // ---------- Capa AURAS (canónica, POR DEBAJO de las cartas) ----------
                // Rastrea los Activos en el registro de coordenadas del motor; el glow (pasivo rojo /
                // manual dorado) se dibuja en la capa Underglow, quedando bajo la carta real.
                AnimationStage(
                    modifier = Modifier.matchParentSize(),
                    registry = glowCoords,
                    renderState = glowRender,
                    board = {
                        // Rastrea TODOS los Pokémon en juego (Activo + Banca, ambos lados) por CardId,
                        // para que el aura pueda encenderse sobre cualquiera que active una Habilidad.
                        state.player.active?.let { pip ->
                            Box(Modifier.place(BoardGeometry.MeActive, boardW, boardH).trackBounds(glowSlot(pip.card.id.raw), glowCoords))
                        }
                        state.player.bench.forEachIndexed { i, pip ->
                            BoardGeometry.MeBenchSlots.getOrNull(i)?.let { slot ->
                                Box(Modifier.place(slot, boardW, boardH).trackBounds(glowSlot(pip.card.id.raw), glowCoords))
                            }
                        }
                        state.opponent.active?.let { pip ->
                            Box(Modifier.place(BoardGeometry.OppActive, boardW, boardH).trackBounds(glowSlot(pip.card.id.raw), glowCoords))
                        }
                        state.opponent.bench.forEachIndexed { i, pip ->
                            BoardGeometry.OppBenchSlots.getOrNull(i)?.let { slot ->
                                Box(Modifier.place(slot, boardW, boardH).trackBounds(glowSlot(pip.card.id.raw), glowCoords))
                            }
                        }
                    },
                )

                // ---------- Capa PARTIDA (zonas, cartas, mano) ----------
                MatchLayer(
                    state = state,
                    vm = vm,
                    bi = bi,
                    boardW = boardW,
                    boardH = boardH,
                    inSetup = inSetup,
                    revealing = revealing,
                    myTurn = myTurn,
                    mustPromote = mustPromote,
                    setup = setup,
                    onInspect = { inspect = it },
                    onZonePanel = { zonePanel = it },
                )

                // ---------- Capa VUELO de ESTADIO (canónica, sobre las cartas) ----------
                // Rastrea el origen (mano de cada lado) y el destino (slot de Estadio) en su propio
                // registro; el nodo StadiumPlace viaja en arco y aterriza con peso + destello.
                AnimationStage(
                    modifier = Modifier.matchParentSize(),
                    registry = stadiumCoords,
                    renderState = stadiumRender,
                    board = {
                        Box(Modifier.place(BoardGeometry.Stadium, boardW, boardH).trackBounds(stadiumSlotId(), stadiumCoords))
                        Box(Modifier.place(BoardGeometry.MeHandOrigin, boardW, boardH).trackBounds(handSlotId(Side.PLAYER.name), stadiumCoords))
                        Box(Modifier.place(BoardGeometry.OppHandOrigin, boardW, boardH).trackBounds(handSlotId(Side.OPPONENT.name), stadiumCoords))
                    },
                )

                // ---------- HUD persistente (rieles/estado/fin de turno/banner) ----------
                HudLayer(
                    vm = vm,
                    boardW = boardW,
                    boardH = boardH,
                    oppPrizes = state.opponent.prizesRemaining,
                    mePrizes = state.player.prizesRemaining,
                    litSide = litSide,
                    oppClock = oppClock,
                    meClock = meClock,
                    inSetup = inSetup,
                    canConfirmSetup = setup?.canConfirm == true,
                    myTurn = myTurn,
                    mustPromote = mustPromote,
                    retreatMode = bi.retreatMode,
                    onOpenSettings = { showPause = true },
                    onOpenLog = { showLog = true },
                    onCancelRetreat = { bi.retreatMode = false; bi.benchDragCard = null },
                )

                // ---------- Capa RÓTULOS (canónica, AL FRENTE) ----------
                // Centrada exactamente en el medio del tapete (matchParentSize del board Box). No
                // intercepta toques (sin gestos), así el HUD y las cartas siguen siendo interactivos.
                AnimationStage(
                    modifier = Modifier.matchParentSize(),
                    registry = bannerCoords,
                    renderState = bannerRender,
                )
            }
        }

        // ---- Capa MODAL / OVERLAY (todo animado con el framework Motion) ----
        // Decisión pendiente del jugador: entra/sale deslizando desde abajo (retiene su
        // contenido durante la salida para no cortar la animación).
        val decisionInter = state.interaction?.takeIf { it.side == Side.PLAYER }
        var lastDecision by remember { mutableStateOf(decisionInter?.decision) }
        if (decisionInter != null) lastDecision = decisionInter.decision
        MotionPanel(
            visible = decisionInter != null,
            edge = MotionEdge.Bottom,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            lastDecision?.let { dec ->
                DecisionPanel(decision = dec, cardOf = { id -> vm.card(id) }, onResolve = { vm.onResolve(it) })
            }
        }

        // Inspector de zona (descartes): diálogo Motion (scrim fundido + contenido con pop).
        var lastZone by remember { mutableStateOf(zonePanel) }
        if (zonePanel != null) lastZone = zonePanel
        MotionDialog(visible = zonePanel != null, onDismiss = { zonePanel = null }) {
            lastZone?.let { (title, cards) ->
                ZoneInspectorBody(title, cards, onCardTap = { inspect = it }, onDismiss = { zonePanel = null })
            }
        }

        // Registro de batalla: diálogo Motion.
        MotionDialog(visible = showLog, onDismiss = { showLog = false }) {
            BattleLogBody(ui.log, onDismiss = { showLog = false })
        }

        // Menú de PAUSA / OPCIONES (velo semitransparente sobre el tablero). RENDIRSE = salir de la
        // partida (onExit). Único para PvE y PvP.
        if (showPause) {
            PauseOptionsSheet(
                music = musicVol, onMusic = { musicVol = it },
                sfx = sfxVol, onSfx = { sfxVol = it },
                disableVfx = disableVfx, onDisableVfx = { disableVfx = it },
                cardVfxEnabled = cardVfxEnabled, onCardVfxEnabled = { cardVfxEnabled = it },
                // RENDIRSE cierra el menú y abre la confirmación que sube desde abajo.
                onSurrender = { showPause = false; showSurrender = true },
                onDismiss = { showPause = false },
            )
        }

        // Confirmación de RENDIRSE: panel inferior que sube suavemente sobre el tablero (sin velo),
        // como en TCG Live. "ME RINDO" abandona (onExit); "MEJOR NO" (o tocar fuera) cancela.
        if (showSurrender) {
            Box(
                Modifier.fillMaxSize().clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                ) { showSurrender = false },
            )
        }
        MotionPanel(
            visible = showSurrender,
            edge = MotionEdge.Bottom,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SurrenderConfirmBody(
                onConfirm = { showSurrender = false; onExit() },
                onCancel = { showSurrender = false },
            )
        }

        // Visor a DETALLE (holo + pinch): el propio [CardDetailDialog] anima su entrada/salida como
        // popup "de arriba a abajo" (salida rápida) y difiere el cierre hasta terminar la animación,
        // por lo que basta mostrarlo mientras haya carta inspeccionada.
        inspect?.let { card ->
                val activePip = state.player.active
                // El Pokémon inspeccionado, si es MÍO (Activo o Banca): así también ofrecemos las
                // Habilidades manuales usables desde la Banca (Dodrio, Starmie, Mew ex, Persian, Snorlax…),
                // que no son `activeOnly`. El motor (legalIntents) es la única autoridad de qué es legal.
                val myPip = state.player.allInPlay.firstOrNull { it.card.id == card.id }
                val isMyActive = activePip != null && card.id == activePip.card.id
                // Mostramos acciones en tu turno para: tu Activo (ataques+retirada+Habilidades) o
                // cualquier Pokémon tuyo de la Banca CON Habilidades (solo la sección de Habilidades).
                // Pokémon con acciones a mostrar (null = sin barra inferior). Evita el check redundante
                // `myPip != null` en el `bottomBar` (que el compilador marcaría como siempre cierto).
                val actionPip = myPip?.takeIf {
                    myTurn && (isMyActive || it.card.abilities.isNotEmpty())
                }
                // Pokémon inspeccionado en juego (propio o RIVAL): habilita el visor estilo TCG Live
                // (badge PS+tipo sobre la carta + panel de estadísticas debajo) para cualquier Pokémon.
                val inspectedPip = (state.player.allInPlay + state.opponent.allInPlay)
                    .firstOrNull { it.card.id == card.id }
                // Estadio "Camino de Bicis": si tocas el Estadio en juego en TU turno y aún no lo usaste,
                // puedes descartar 1 Energía Básica de tu mano para robar (motor: GameIntent.UseStadium).
                val isStadium = state.stadium?.id == card.id
                val stadiumEnergies = if (isStadium && myTurn) state.player.hand.filterIsInstance<BasicEnergy>() else emptyList()
                val stadiumUsable = isStadium && stadiumEnergies.isNotEmpty() &&
                    vm.legalIntents().any { it is GameIntent.UseStadium }
                CardDetailDialog(
                    imageUrl = card.artwork.large(true),
                    contentDescription = card.name.es,
                    onDismiss = { inspect = null },
                    // Velo LIGERO (como el visor de la Colección): se ve el tablero detrás.
                    scrimColor = Color(0x40000000),
                    rarity = card.rarity,
                    cardNumber = card.id.printed.raw.substringAfterLast('-').toIntOrNull(),
                    // Sin esto el visor asumía set 151 → las cartas de otros sets no resolvían foil.
                    setCode = card.id.printed.raw.let { if (it.startsWith("energy")) "energy" else it.substringBeforeLast('-') },
                    // Badge PS+tipo de TCG Live, anclado a la esquina superior-derecha de la carta.
                    overlay = inspectedPip?.let { p ->
                        { HpTypeBadge(p.remainingHp, p.card.types.firstOrNull(), Modifier.align(Alignment.TopEnd).padding(8.dp)) }
                    },
                    bottomBar = if (actionPip != null) {
                        {
                            // Intents legales AHORA → distinguen habilidades manuales disponibles.
                            val legal = vm.legalIntents()
                            val onUseAbility: (Ability) -> Unit = { ability ->
                                // El rótulo/aura los dispara el EVENTO del motor (AbilityUsed),
                                // no la UI: así también animan al rival y las pasivas automáticas.
                                inspect = null
                                vm.onIntent(GameIntent.UseAbility(actionPip.card.id, ability.name.es))
                            }
                            val canUseAbility: (Ability) -> Boolean = { ability ->
                                GameIntent.UseAbility(actionPip.card.id, ability.name.es) in legal
                            }
                            // El motor es la ÚNICA autoridad de qué ataque es usable AHORA: coste por TIPOS
                            // de energía, recargos, turno 1, restricciones y excepciones (ataque gratis por
                            // aliado como Nidoking, y las que añadamos en el futuro). Si el motor lo ofrece
                            // como intent legal, el botón se habilita; si no, se atenúa.
                            val canUseAttack: (String) -> Boolean = { name ->
                                GameIntent.Attack(name) in legal
                            }
                            val abilityUsed: (Ability) -> Boolean = { actionPip.card.id in state.abilitiesUsedThisTurn }
                            if (isMyActive) {
                                ActiveActions(
                                    pip = actionPip,
                                    canRetreat = state.player.bench.isNotEmpty() &&
                                        actionPip.attachedEnergyCount >= actionPip.card.retreatCost.size,
                                    onAttack = { name -> inspect = null; vm.onIntent(GameIntent.Attack(name)) },
                                    onRetreat = { inspect = null; bi.retreatMode = true },
                                    onUseAbility = onUseAbility,
                                    canUseAbility = canUseAbility,
                                    canUseAttack = canUseAttack,
                                    abilityUsed = abilityUsed,
                                )
                            } else {
                                // Pokémon de la Banca: solo la sección de Habilidades (sin ataque/retirada).
                                BenchAbilityActions(
                                    pip = actionPip,
                                    onUseAbility = onUseAbility,
                                    canUseAbility = canUseAbility,
                                    abilityUsed = abilityUsed,
                                )
                            }
                        }
                    } else if (inspectedPip != null) {
                        // Pokémon en juego sin acciones (rival o propio fuera de turno): solo el panel
                        // de estadísticas estilo TCG Live.
                        {
                            Column(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .background(Color(0xFFE3EEF4))
                                    .verticalScroll(rememberScrollState()),
                            ) { PokemonDetailSheet(inspectedPip) }
                        }
                    } else if (stadiumUsable) {
                        // Estadio con acción usable (Camino de Bicis): elegir Energía Básica a descartar.
                        {
                            StadiumActionSheet(
                                energies = stadiumEnergies,
                                onDiscard = { id -> inspect = null; vm.onIntent(GameIntent.UseStadium(id)) },
                            )
                        }
                    } else {
                        null
                    },
                )
            }

        // Capa de ANIMACIÓN (fantasmas de arrastre + vuelo entre zonas + estallido de ataque).
        AnimationLayer(
            benchDragCard = bi.benchDragCard,
            benchDragPos = bi.benchDragPos,
            dragCard = bi.dragCard,
            dragPos = bi.dragPos,
            flightHost = flightHost,
            lensFx = lensFx,
            onLensEnd = { lensFx = null },
        )

        // Fin de partida: fundido Motion.
        MotionContainer(
            visible = state.isOver,
            enter = MotionTransitions.overlayEnter(),
            exit = MotionTransitions.overlayExit(),
        ) {
            val playerWon = state.winner == Side.PLAYER
            GameOverPanel(
                won = playerWon,
                effectId = if (playerWon) victoryEffectId else defeatEffectId,
                cristales = if (playerWon) winCristales else lossCristales,
                monedas = if (playerWon) winMonedas else lossMonedas,
                onExit = onExit,
            )
        }
    }
    }
}

/**
 * **Capa ESCENARIO** — el tapete oficial ([CombatMat]) y nada más: sin cartas, sin HUD, sin estado de
 * partida. Primera capa del compositor interno de [CombatScreen]. Neutra: no conoce perfiles, modos ni
 * conceptos del Studio; solo recibe parámetros de presentación ([litSide], [activeType], [theme]).
 */
@Composable
private fun SceneLayer(litSide: Side?, activeType: EnergyType?, theme: MatTheme, modifier: Modifier = Modifier) {
    CombatMat(modifier, litSide = litSide, activeType = activeType, theme = theme)
}

/**
 * **Capa de ANIMACIÓN** — efectos que viven POR ENCIMA del tablero: los fantasmas de la carta y del
 * Pokémon de Banca arrastrados, la capa de VUELO entre zonas ([FlightOverlay]) y el estallido de ataque
 * del lente ([LensAttackFx]). Se emite en el MISMO orden z que antes (fantasmas → vuelo → estallido).
 * Neutra: solo recibe estado de presentación; no conoce perfiles ni conceptos del Studio.
 */
@Composable
private fun AnimationLayer(
    benchDragCard: Card?,
    benchDragPos: Offset,
    dragCard: Card?,
    dragPos: Offset,
    flightHost: FlightHost,
    lensFx: LensFxSpec?,
    onLensEnd: () -> Unit,
) {
    // Fantasma del Pokémon de Banca arrastrado hacia el Activo (retirada).
    benchDragCard?.let { c ->
        Box(
            Modifier
                .offset { IntOffset(benchDragPos.x.roundToInt() - 42, benchDragPos.y.roundToInt() - 59) }
                .size(84.dp, 118.dp),
        ) {
            CombatCard(imageUrl = c.artwork.small(true), modifier = Modifier.fillMaxSize())
        }
    }

    // Fantasma de la carta arrastrada siguiendo el dedo (coords de raíz).
    dragCard?.let { c ->
        Box(
            Modifier
                .offset { IntOffset(dragPos.x.roundToInt() - 42, dragPos.y.roundToInt() - 59) }
                .size(84.dp, 118.dp),
        ) {
            CombatCard(imageUrl = c.artwork.small(true), modifier = Modifier.fillMaxSize())
        }
    }

    // Capa de VUELO entre zonas (por encima del tablero, bajo el fantasma de arrastre).
    FlightOverlay(flightHost)

    // Estallido de ataque en el lente (efímero, ~1 s, color/motivo por tipo).
    lensFx?.let { spec -> LensAttackFx(spec, onLensEnd) }
}

/**
 * **Capa HUD (persistente)** — los elementos SIEMPRE presentes del interfaz de combate: botón de
 * ajustes, pestañas de estado (premios/temporizador/turno) de ambos lados, botón de registro, botón de
 * FIN/LISTO y el banner de preparación/promoción. Se coloca dentro del board Box (usa [boardW]/[boardH]).
 *
 * NOTA de responsabilidades: el HUD del combate tiene DOS naturalezas distintas —
 *   (1) **HUD persistente** (esta capa), y
 *   (2) **Presentación modal** (diálogos: decisión, inspector de zona, Battle Log, visor a detalle,
 *       fin de partida), que hoy permanece inline en [CombatScreen] porque su z-order NO es contiguo
 *       (la [AnimationLayer] se dibuja entre sus sub-grupos). Se mantienen SEPARADAS conceptualmente;
 *       la presentación modal podrá independizarse en su propia capa cuando exista un consumidor real
 *       (p. ej. replay/espectador), sin mezclarla aquí.
 *
 * Neutra: no conoce perfiles ni conceptos del Studio; solo estado de presentación + el controlador.
 */
@Composable
private fun HudLayer(
    vm: CombatSceneController,
    boardW: Dp,
    boardH: Dp,
    oppPrizes: Int,
    mePrizes: Int,
    litSide: Side?,
    oppClock: Int,
    meClock: Int,
    inSetup: Boolean,
    canConfirmSetup: Boolean,
    myTurn: Boolean,
    mustPromote: Boolean,
    retreatMode: Boolean,
    onOpenSettings: () -> Unit,
    onOpenLog: () -> Unit,
    onCancelRetreat: () -> Unit,
) {
    Box(Modifier.place(BoardGeometry.TopSettings, boardW, boardH)) {
        // Abre el menú de PAUSA / OPCIONES (RENDIRSE vive dentro del menú, no aquí).
        IconChip("⚙", CombatTheme.OnSurface, onOpenSettings)
    }
    // Columna derecha 1:1 con TCG Live: pestañas de estado (rival arriba / jugador
    // abajo, espejo) con barra de turno + chevrones + temporizador + premios, y el
    // botón de registro de batalla debajo.
    Box(Modifier.place(BoardGeometry.RailStatusOpp, boardW, boardH)) {
        PlayerStatusPanel(
            prizes = oppPrizes,
            prizeAccent = Color(0xFFCE3B39),
            isOpponent = true,
            active = litSide == Side.OPPONENT,
            timerText = mmss(oppClock),
        )
    }
    Box(Modifier.place(BoardGeometry.RailStatusMe, boardW, boardH)) {
        PlayerStatusPanel(
            prizes = mePrizes,
            prizeAccent = Color(0xFF3977BE),
            isOpponent = false,
            active = litSide == Side.PLAYER,
            timerText = mmss(meClock),
        )
    }
    Box(Modifier.place(BoardGeometry.RailLog, boardW, boardH)) {
        BattleLogButton(onClick = onOpenLog)
    }
    Box(Modifier.place(BoardGeometry.RailEndTurn, boardW, boardH), contentAlignment = Alignment.Center) {
        val enabled = if (inSetup) canConfirmSetup else myTurn
        PillButton(
            label = if (inSetup) "LISTO" else "FIN",
            enabled = enabled,
            accent = if (inSetup) CombatTheme.Good else CombatTheme.Gold,
            onClick = { if (inSetup) vm.confirmSetup() else vm.onIntent(GameIntent.EndTurn) },
        )
    }
    // (El ATAQUE y la RETIRADA viven ahora en el VISOR A DETALLE del Activo propio.)
    // Banner de preparación / promoción.
    val banner = when {
        retreatMode -> "Arrastra un Pokémon de la Banca al Activo para retirarte · toca aquí para cancelar"
        inSetup -> "Coloca tu Activo y tu Banca, luego pulsa LISTO"
        mustPromote -> "Elige tu nuevo Pokémon Activo"
        else -> null
    }
    banner?.let {
        Box(
            Modifier.place(BoardGeometry.SetupBanner, boardW, boardH)
                .clickable(enabled = retreatMode) { onCancelRetreat() },
            contentAlignment = Alignment.Center,
        ) {
            BannerText(it)
        }
    }
}

/**
 * Estado EFÍMERO de interacción del tablero de combate: carta/posición arrastrada, límites (rects) de
 * zonas y huecos, modo retirada + arrastre de Banca, foco de la mano y anclas de vuelo. Existe SOLO para
 * reducir el acoplamiento interno de [CombatScreen] agrupando su estado transitorio compartido entre el
 * compositor y sus capas. NO contiene lógica de negocio, ni estado de juego (GameState), ni referencias
 * al Studio. Privado al módulo; no forma parte de ninguna API pública.
 */
@Stable
private class CombatBoardInteraction {
    var dragCard by mutableStateOf<Card?>(null)
    var dragPos by mutableStateOf(Offset.Zero)
    val targetBounds = mutableStateMapOf<CardId, Rect>()
    val benchSlotBounds = mutableStateMapOf<Int, Rect>()
    var activeSlotBounds by mutableStateOf<Rect?>(null)
    var centerBounds by mutableStateOf<Rect?>(null)
    var retreatMode by mutableStateOf(false)
    var benchDragCard by mutableStateOf<Card?>(null)
    var benchDragPos by mutableStateOf(Offset.Zero)
    var pendingPlay by mutableStateOf<Card?>(null)
    var handFocus by mutableStateOf<HandCat?>(null)
    var handFocusNonce by mutableStateOf(0)
    val anchorBounds = mutableStateMapOf<CardZone, Rect>()
    // Secuencias de animación de COMBATE (se incrementan por cue de FX; disparan las animaciones de
    // embate/sacudida del Activo correspondiente, sincronizadas con el impacto de daño). Ver el
    // colector de FxCue.Damage en CombatScreen y su aplicación en MatchLayer.
    var lungeSeqPlayer by mutableStateOf(0)  // el Activo del jugador embiste (ataca)
    var lungeSeqOpp by mutableStateOf(0)     // el Activo rival embiste (ataca)
    var shakeSeqPlayer by mutableStateOf(0)  // el Activo del jugador se sacude (recibe)
    var shakeSeqOpp by mutableStateOf(0)     // el Activo rival se sacude (recibe)
}

/**
 * **Capa PARTIDA** — las zonas, cartas y la mano dibujadas a partir del [GameState]: anclas de vuelo,
 * panel central, lado rival (mano/banca/activo/premios/mazo/descarte), estadio, lado del jugador y el
 * abanico de mano con su barra de filtros. Contiene los gestos de arrastre/jugada y publica hacia arriba
 * la inspección de cartas ([onInspect]) y el inspector de zona/descartes ([onZonePanel]). Neutra: solo
 * lee el estado + escribe el estado efímero compartido ([bi]); no conoce perfiles ni conceptos del Studio.
 */
@Composable
private fun MatchLayer(
    state: GameState,
    vm: CombatSceneController,
    bi: CombatBoardInteraction,
    boardW: Dp,
    boardH: Dp,
    inSetup: Boolean,
    revealing: Boolean,
    myTurn: Boolean,
    mustPromote: Boolean,
    setup: SetupUiState?,
    onInspect: (Card) -> Unit,
    onZonePanel: (Pair<String, List<Card>>) -> Unit,
) {
    // Anclas invisibles por zona: capturan el rect (coords de raíz) de cada zona
    // para que la capa de VUELO sepa de/hacia dónde viajan las cartas. Sin pointer
    // input → no interceptan toques; se dibujan bajo el resto de zonas.
    CardZone.values().forEach { z ->
        Box(
            Modifier.place(zoneNBox(z), boardW, boardH)
                .onGloballyPositioned { bi.anchorBounds[z] = it.boundsInRoot() },
        )
    }

    val opp = state.opponent
    val me = state.player
    val faceDownOpp = inSetup || revealing

    // Embate (Y, hacia el rival) y sacudida (X) de cada Activo, disparados por las secuencias de FX.
    // El atacante embiste; el defensor se sacude. Identidad (0) cuando no hay combate en curso.
    val lungeMe = rememberLunge(bi.lungeSeqPlayer, mine = true)
    val lungeOpp = rememberLunge(bi.lungeSeqOpp, mine = false)
    val shakeMe = rememberShake(bi.shakeSeqPlayer)
    val shakeOpp = rememberShake(bi.shakeSeqOpp)

    // Objetivo (Pokémon propio) bajo el dedo según lo que se arrastra:
    //  - energía/evolución → dropTargetUnder (isPlayTarget)
    //  - Objeto dirigido (Poción) → itemTargetUnder (ámbito del efecto, solo dañados si aplica)
    //  - Herramienta → toolTargetUnder (Pokémon sin Herramienta)
    fun dropTargetUnder(card: Card): CardId? =
        (listOfNotNull(me.active) + me.bench).firstOrNull { pip ->
            isPlayTarget(card, pip) && bi.targetBounds[pip.card.id]?.contains(bi.dragPos) == true
        }?.card?.id
    fun itemTargetUnder(choose: EffectOp.ChooseTarget): CardId? {
        val cands = when (choose.from) {
            Target.OWN_ACTIVE -> listOfNotNull(me.active)
            Target.OWN_BENCH -> me.bench
            else -> me.allInPlay
        }.filter { !choose.onlyDamaged || it.damage > 0 }
        return cands.firstOrNull { bi.targetBounds[it.card.id]?.contains(bi.dragPos) == true }?.card?.id
    }
    fun toolTargetUnder(): CardId? =
        me.allInPlay.firstOrNull { it.attachedTools.isEmpty() && bi.targetBounds[it.card.id]?.contains(bi.dragPos) == true }?.card?.id

    val dragging = bi.dragCard
    val dragItemChoose = (dragging as? TrainerCard)?.let { itemTargetChoose(it) }
    val dragToolScope = (dragging as? TrainerCard)?.let { toolAttachScope(it) }
    val hoverTargetId: CardId? = when {
        dragging == null -> null
        dragItemChoose != null -> itemTargetUnder(dragItemChoose)
        dragToolScope != null -> toolTargetUnder()
        else -> dropTargetUnder(dragging)
    }
    // El panel central solo se ilumina para Entrenadores que NO apuntan a un Pokémon.
    val centerHot = dragging is TrainerCard && !cardTargetsPokemon(dragging) &&
        bi.centerBounds?.contains(bi.dragPos) == true
    val activeSetupHot = inSetup && dragging is PokemonCard && dragging.isBasic &&
        bi.activeSlotBounds?.contains(bi.dragPos) == true

    // Panel central (soltar Entrenadores): captura límites + glow al sobrevolar.
    Box(
        Modifier.place(BoardGeometry.CenterPanel, boardW, boardH)
            .onGloballyPositioned { bi.centerBounds = it.boundsInRoot() }
            .then(if (centerHot) Modifier.border(2.dp, CombatTheme.Gold, RoundedCornerShape(20.dp)) else Modifier),
    )

    // ---------- RIVAL ----------
    // Mano rival (dorsos).
    Box(Modifier.place(BoardGeometry.OppHand, boardW, boardH)) {
        OppHandBacks(opp.hand.size)
    }
    // Banca rival (por slots del panal).
    opp.bench.forEachIndexed { i, pip ->
        BoardGeometry.OppBenchSlots.getOrNull(i)?.let { slot ->
            Box(Modifier.place(slot, boardW, boardH)) {
                FieldCard(pip, faceDown = faceDownOpp, glow = vm.abilityGlow(pip.card.id), onTap = { onInspect(pip.card) })
            }
        }
    }
    // Activo rival (con embate al atacar / sacudida al recibir).
    Box(
        Modifier.place(BoardGeometry.OppActive, boardW, boardH)
            .graphicsLayer { translationY = lungeOpp.dp.toPx(); translationX = shakeOpp.dp.toPx() },
    ) {
        FieldCard(opp.active, faceDown = faceDownOpp, glow = opp.active?.card?.id?.let { vm.abilityGlow(it) }, onTap = { opp.active?.let { onInspect(it.card) } })
    }
    // Premios / mazo / descarte rival.
    Box(Modifier.place(BoardGeometry.OppPrizes, boardW, boardH)) {
        PrizeFan(opp.prizesRemaining)
    }
    Box(
        Modifier.place(BoardGeometry.OppDiscard, boardW, boardH)
            .clickable(enabled = opp.discard.isNotEmpty()) { onZonePanel("Descarte del rival" to opp.discard) },
    ) {
        PileStack(opp.discard.size, faceDown = false, topCard = opp.discard.lastOrNull())
    }
    Box(Modifier.place(BoardGeometry.OppDeck, boardW, boardH)) {
        PileStack(opp.deck.size, faceDown = true)
    }

    // ---------- ESTADIO ----------
    // Como en TCG Live: la carta de Estadio en una PLACA plateada redondeada (marco del slot), en el
    // centro-izquierda. La placa despega la carta del mat y la marca como el Estadio en juego.
    state.stadium?.let { stad ->
        Box(
            Modifier.place(BoardGeometry.Stadium, boardW, boardH)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xF2E9ECF3), Color(0xF2C4CAD6))),
                )
                .border(1.5.dp, Color(0xCCFFFFFF), RoundedCornerShape(8.dp))
                .padding(3.dp),
        ) {
            FieldCardImage(stad.artwork.small(true), stad.name.es) { onInspect(stad) }
        }
    }

    // ---------- JUGADOR ----------
    // Intents legales AHORA (autoridad única del motor), para pintar los glows de jugabilidad:
    // azul = carta usable / Activo que puede atacar; amarillo = carta de evolución jugable. Solo
    // en TU turno, en juego y sin decisión pendiente (si no, nada brilla).
    val legalNow = if (!inSetup && myTurn && state.interaction == null) vm.legalIntents() else emptyList()
    val activeCanAttack = me.active != null && legalNow.any {
        it is GameIntent.Attack && (it.attacker == null || it.attacker == me.active!!.card.id)
    }
    Box(
        Modifier.place(BoardGeometry.MeActive, boardW, boardH)
            .onGloballyPositioned { c ->
                val r = c.boundsInRoot(); bi.activeSlotBounds = r
                me.active?.card?.id?.let { bi.targetBounds[it] = r }
            }
            .graphicsLayer { translationY = lungeMe.dp.toPx(); translationX = shakeMe.dp.toPx() },
    ) {
        FieldCard(
            me.active,
            selected = false,
            glow = me.active?.card?.id?.let { vm.abilityGlow(it) },
            highlighted = activeSetupHot ||
                (hoverTargetId != null && hoverTargetId == me.active?.card?.id) ||
                (bi.retreatMode && bi.benchDragCard != null && bi.activeSlotBounds?.contains(bi.benchDragPos) == true),
            onTap = {
                when {
                    inSetup -> vm.clearActive()
                    me.active == null -> {}
                    // Tocar el Activo = VER A DETALLE (holo/3D). Atacar = botón ⚔.
                    else -> me.active?.let { onInspect(it.card) }
                }
            },
        )
        // Glow azul "listo para atacar": el Activo tiene energía suficiente para algún ataque legal.
        if (activeCanAttack) PersistentGlow(AbilityGlowVisuals.Playable, Modifier.matchParentSize(), edgeOnly = true)
    }
    me.bench.forEachIndexed { i, pip ->
        BoardGeometry.MeBenchSlots.getOrNull(i)?.let { slot ->
            Box(
                Modifier.place(slot, boardW, boardH)
                    .onGloballyPositioned { bi.targetBounds[pip.card.id] = it.boundsInRoot() },
            ) {
                FieldCard(
                    pip,
                    glow = vm.abilityGlow(pip.card.id),
                    highlighted = mustPromote || (hoverTargetId == pip.card.id),
                    onTap = {
                        when {
                            inSetup -> vm.toggleBench(pip.card.id)
                            mustPromote -> vm.onIntent(GameIntent.PromoteActive(pip.card.id))
                            // Tocar un Pokémon de Banca = VER A DETALLE (holo/3D).
                            else -> onInspect(pip.card)
                        }
                    },
                    // Retirada: arrastrar este Pokémon sobre el Activo lo reemplaza.
                    draggable = bi.retreatMode,
                    onDragStart = { pos -> bi.benchDragCard = pip.card; bi.benchDragPos = pos },
                    onDrag = { pos -> bi.benchDragPos = pos },
                    onDragEnd = {
                        if (bi.retreatMode && bi.activeSlotBounds?.contains(bi.benchDragPos) == true) {
                            vm.onIntent(GameIntent.Retreat(pip.card.id))
                        }
                        bi.benchDragCard = null; bi.retreatMode = false
                    },
                )
            }
        }
    }
    // Slots de Banca VACÍOS: capturan sus límites (para soltar un Básico) y se
    // iluminan mientras se arrastra un Básico sobre ellos.
    for (i in me.bench.size until BoardGeometry.MeBenchSlots.size) {
        val slot = BoardGeometry.MeBenchSlots[i]
        val hot = dragging is PokemonCard && dragging.isBasic &&
            bi.benchSlotBounds[i]?.contains(bi.dragPos) == true
        Box(
            Modifier.place(slot, boardW, boardH)
                .onGloballyPositioned { bi.benchSlotBounds[i] = it.boundsInRoot() },
        ) { EmptySlot(Modifier.fillMaxSize(), highlighted = hot) }
    }
    // Los PREMIOS propios NO se inspeccionan (información oculta para ti mismo).
    Box(Modifier.place(BoardGeometry.MePrizes, boardW, boardH)) {
        PrizeFan(me.prizesRemaining)
    }
    Box(
        Modifier.place(BoardGeometry.MeDiscard, boardW, boardH)
            .clickable(enabled = me.discard.isNotEmpty()) { onZonePanel("Tu descarte" to me.discard) },
    ) { PileStack(me.discard.size, faceDown = false, topCard = me.discard.lastOrNull()) }
    // El MAZO propio NO se inspecciona (información oculta).
    Box(Modifier.place(BoardGeometry.MeDeck, boardW, boardH)) {
        PileStack(me.deck.size, faceDown = true)
    }

    // Mano del jugador — REUTILIZA el HandFan de TCG Live (abanico, agrupación de
    // copias idénticas con contador, orden por supertipo, scroll para ojear, y
    // dimensiones 1:1 = alto·aspecto de carta). Comportamiento idéntico a
    // la pantalla clásica; aquí se juega por TOQUE (canDrag por defecto = false).
    // Alto MEDIDO en ref_0034 (Combate 1): carta de mano ≈0.26W → alto ≈0.163H
    // (la "Ultra Ball" ocupa x≈0.44..0.70). Local al combate para no alterar la clásica.
    val handCardH = boardH * 0.163f
    val handCardW = handCardH * BoardGeometry.CardAspect
    Box(Modifier.place(BoardGeometry.MeHand, boardW, boardH)) {
        HandFan(
            cards = sortedHandCards(me.hand),
            enabled = inSetup || (myTurn && state.interaction == null),
            // La barra no atenúa: DESLIZA la mano a la categoría pulsada.
            focusCategory = bi.handFocus,
            focusNonce = bi.handFocusNonce,
            cardW = handCardW,
            cardH = handCardH,
            selectedId = bi.pendingPlay?.id,
            // Glow de jugabilidad por carta, derivado de los intents legales del motor:
            // amarillo = esta carta hace evolucionar algo; azul = usable ahora (Energía/Objeto/
            // Herramienta/Apoyo no gastado/Básico a Banca/acción de Estadio).
            glowOf = { card ->
                when {
                    legalNow.any { it is GameIntent.Evolve && it.evolution == card.id } -> HandGlow.EVOLVE
                    legalNow.any {
                        (it is GameIntent.AttachEnergy && it.energy == card.id) ||
                            (it is GameIntent.PlayTrainer && it.card == card.id) ||
                            (it is GameIntent.AttachTool && it.tool == card.id) ||
                            (it is GameIntent.PlayBasicToBench && it.card == card.id) ||
                            (it is GameIntent.UseStadium && it.energy == card.id)
                    } -> HandGlow.PLAYABLE
                    else -> null
                }
            },
            onSelect = { card ->
                when {
                    // En preparación, tocar un Básico lo coloca (Activo/Banca).
                    inSetup -> if (card is PokemonCard && card.isBasic) {
                        if (setup?.activeId == null) vm.chooseActive(card.id) else vm.toggleBench(card.id)
                    }
                    // En juego, tocar una carta la MUESTRA A DETALLE (holo/3D).
                    // Para JUGARLA, arrástrala (energía→Pokémon, evolución, Básico→Banca, Entrenador→centro).
                    else -> onInspect(card)
                }
            },
            // ---- Arrastre 1:1: energía→Pokémon, evolución→pre-evolución, Básico→
            //      Banca/Activo(setup), Entrenador→panel central. ----
            canDrag = { card ->
                if (inSetup) card is PokemonCard && card.isBasic
                else card is EnergyCard || card is PokemonCard || card is TrainerCard
            },
            onCardDragStart = { card, pos -> bi.dragCard = card; bi.dragPos = pos },
            onCardDrag = { pos -> bi.dragPos = pos },
            onCardDragCancel = { bi.dragCard = null },
            onCardDragEnd = {
                val card = bi.dragCard
                if (card != null) {
                    if (inSetup) {
                        if (card is PokemonCard && card.isBasic) {
                            when {
                                bi.activeSlotBounds?.contains(bi.dragPos) == true -> vm.chooseActive(card.id)
                                bi.benchSlotBounds.values.any { it.contains(bi.dragPos) } -> vm.toggleBench(card.id)
                            }
                        }
                    } else {
                        when (card) {
                            is EnergyCard ->
                                dropTargetUnder(card)?.let { vm.onIntent(GameIntent.AttachEnergy(card.id, it)) }
                            is PokemonCard ->
                                if (card.isBasic) {
                                    if (bi.benchSlotBounds.values.any { it.contains(bi.dragPos) }) {
                                        vm.onIntent(GameIntent.PlayBasicToBench(card.id))
                                    }
                                } else {
                                    dropTargetUnder(card)?.let { vm.onIntent(GameIntent.Evolve(card.id, it)) }
                                }
                            is TrainerCard -> {
                                val itemChoose = itemTargetChoose(card)
                                val toolScope = toolAttachScope(card)
                                when {
                                    // Objeto dirigido (Poción…) soltado sobre un Pokémon válido → un gesto.
                                    itemChoose != null ->
                                        itemTargetUnder(itemChoose)?.let { vm.playItemOn(card.id, it) }
                                    // Herramienta soltada sobre un Pokémon sin Herramienta.
                                    toolScope != null ->
                                        toolTargetUnder()?.let { vm.onIntent(GameIntent.AttachTool(card.id, it)) }
                                    // Entrenador normal → panel central.
                                    bi.centerBounds?.contains(bi.dragPos) == true ->
                                        vm.onIntent(GameIntent.PlayTrainer(card.id))
                                }
                            }
                        }
                    }
                }
                bi.dragCard = null
            },
        )
    }
    // Barra de filtros por tipo (contador total + iconos Pokémon/Entrenador/Energía).
    // POSICIÓN 1:1 medida en ref_0034: pill blanco al FONDO, centrado, solapando la
    // parte baja del abanico (centro ≈ x0.50, y0.967; barra sólida y≈0.950..0.984).
    // El pill se auto-dimensiona; la caja solo lo centra. Se dibuja DESPUÉS del abanico
    // (queda por encima), como en TCG Live.
    if (me.hand.isNotEmpty()) {
        Box(
            Modifier.place(BoardGeometry.NBox(0.20f, 0.945f, 0.60f, 0.044f), boardW, boardH),
            contentAlignment = Alignment.Center,
        ) {
            HandFilterBar(
                cards = me.hand,
                active = bi.handFocus,
                onToggle = { cat -> bi.handFocus = cat; bi.handFocusNonce++ },
            )
        }
    }
}

/** Formatea segundos como mm:ss para el temporizador de las pestañas de estado. */
private fun mmss(sec: Int): String = "%02d:%02d".format(sec / 60, sec % 60)

/**
 * Zonas del tablero usadas como ANCLAS de vuelo. [faceUp] indica si la carta se ve de frente
 * al ATERRIZAR en esa zona (mano propia, descartes, activos y bancas) o de dorso (mazos,
 * premios, mano rival — información oculta).
 */
private enum class CardZone(val faceUp: Boolean) {
    ME_DECK(false), ME_HAND(true), ME_DISCARD(true), ME_PRIZES(false), ME_BENCH(true), ME_ACTIVE(true),
    OPP_DECK(false), OPP_HAND(false), OPP_DISCARD(true), OPP_PRIZES(false), OPP_BENCH(true), OPP_ACTIVE(true),
}

/** Caja del TAMAÑO DE UNA CARTA centrada en el centro de [base] (para zonas que son franjas
 *  anchas — mano, banca, abanico de premios — cuyo NBox completo haría volar cartas enormes). */
private fun centeredCard(base: BoardGeometry.NBox, wf: Float, hf: Float): BoardGeometry.NBox =
    BoardGeometry.NBox(base.cx - wf / 2f, base.cy - hf / 2f, wf, hf)

/**
 * NBox de referencia (del tamaño de una carta) de cada zona para la capa de VUELO. Las zonas
 * que son franjas (mano, banca, premios) se reducen a una carta centrada; el resto ya son
 * cajas del tamaño de su carta.
 */
private fun zoneNBox(z: CardZone): BoardGeometry.NBox = when (z) {
    CardZone.ME_DECK -> BoardGeometry.MeDeck
    CardZone.ME_HAND -> centeredCard(BoardGeometry.MeHand, 0.26f, 0.163f)
    CardZone.ME_DISCARD -> BoardGeometry.MeDiscard
    CardZone.ME_PRIZES -> centeredCard(BoardGeometry.MePrizes, 0.144f, 0.090f)
    CardZone.ME_BENCH -> centeredCard(BoardGeometry.MeBench, 0.178f, 0.112f)
    CardZone.ME_ACTIVE -> BoardGeometry.MeActive
    CardZone.OPP_DECK -> BoardGeometry.OppDeck
    CardZone.OPP_HAND -> centeredCard(BoardGeometry.OppHand, 0.130f, 0.081f)
    CardZone.OPP_DISCARD -> BoardGeometry.OppDiscard
    CardZone.OPP_PRIZES -> centeredCard(BoardGeometry.OppPrizes, 0.144f, 0.090f)
    CardZone.OPP_BENCH -> centeredCard(BoardGeometry.OppBench, 0.150f, 0.094f)
    CardZone.OPP_ACTIVE -> BoardGeometry.OppActive
}

/** Clasifica CADA carta del estado a su zona (para diffing entre estados sucesivos). */
private fun classifyZones(s: GameState): Map<CardId, CardZone> {
    val m = HashMap<CardId, CardZone>()
    s.player.let { p ->
        p.deck.forEach { m[it.id] = CardZone.ME_DECK }
        p.hand.forEach { m[it.id] = CardZone.ME_HAND }
        p.discard.forEach { m[it.id] = CardZone.ME_DISCARD }
        p.prizes.forEach { m[it.id] = CardZone.ME_PRIZES }
        p.bench.forEach { m[it.card.id] = CardZone.ME_BENCH }
        p.active?.let { m[it.card.id] = CardZone.ME_ACTIVE }
    }
    s.opponent.let { o ->
        o.deck.forEach { m[it.id] = CardZone.OPP_DECK }
        o.hand.forEach { m[it.id] = CardZone.OPP_HAND }
        o.discard.forEach { m[it.id] = CardZone.OPP_DISCARD }
        o.prizes.forEach { m[it.id] = CardZone.OPP_PRIZES }
        o.bench.forEach { m[it.card.id] = CardZone.OPP_BENCH }
        o.active?.let { m[it.card.id] = CardZone.OPP_ACTIVE }
    }
    return m
}

/** Índice CardId → Card sobre todas las zonas del estado (para dibujar la carta que vuela). */
private fun allCardsById(s: GameState): Map<CardId, Card> {
    val m = HashMap<CardId, Card>()
    listOf(s.player, s.opponent).forEach { ps ->
        (ps.deck + ps.hand + ps.discard + ps.prizes).forEach { m[it.id] = it }
        ps.bench.forEach { m[it.card.id] = it.card }
        ps.active?.let { m[it.card.id] = it.card }
    }
    return m
}

/** Contador que transiciona con un deslizamiento vertical (sube al subir, baja al bajar). */
@Composable
private fun AnimatedCount(
    count: Int,
    color: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
) {
    androidx.compose.animation.AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            } else {
                (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            }
        },
        label = "AnimatedCount",
    ) { value ->
        Text("$value", color = color, fontWeight = FontWeight.Black, fontSize = fontSize)
    }
}

/** Cuerpo del registro de batalla (lo envuelve MotionDialog, que aporta scrim y cierre). */
@Composable
private fun BattleLogBody(log: List<String>, onDismiss: () -> Unit) {
    run {
        Column(
            Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(CombatTheme.PanelCorner))
                .background(CombatTheme.Surface)
                .border(1.dp, CombatTheme.Border, RoundedCornerShape(CombatTheme.PanelCorner))
                .clickable {}
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Registro de batalla", color = CombatTheme.Gold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text("✕", color = CombatTheme.Muted, fontSize = 20.sp, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (log.isEmpty()) {
                    Text("Sin eventos todavía.", color = CombatTheme.Muted, fontSize = 12.sp)
                }
                log.asReversed().forEach { line ->
                    Text(line, color = CombatTheme.OnSurface, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Slot id canónico del aura de un Pokémon (por id de instancia): sirve para Activo y Banca. */
private fun glowSlot(cardIdRaw: String): SlotId = SlotId("glow:$cardIdRaw")

/** Coloca un Composable en su caja normalizada [BoardGeometry.NBox] (offset + size). */
private fun Modifier.place(b: BoardGeometry.NBox, boardW: Dp, boardH: Dp): Modifier =
    this.offset(x = boardW * b.x, y = boardH * b.y).size(width = boardW * b.w, height = boardH * b.h)

/** ¿Es [pip] un objetivo válido para la carta [card] en curso (energía/evolución)? */
private fun isPlayTarget(card: Card?, pip: PokemonInPlay): Boolean = when (card) {
    is EnergyCard -> true
    is PokemonCard -> card.evolvesFrom != null && pip.turnsInPlay >= 1 &&
        (card.evolvesFrom == pip.card.name.en || card.evolvesFrom == pip.card.name.es)
    else -> false
}

/** Completa una jugada dirigida sobre [target]: energía → adjuntar, evolución → evolucionar. */
private fun completePlay(card: Card, target: PokemonInPlay?, vm: CombatSceneController) {
    if (target == null) return
    when (card) {
        is EnergyCard -> vm.onIntent(GameIntent.AttachEnergy(card.id, target.card.id))
        is PokemonCard -> if (isPlayTarget(card, target)) vm.onIntent(GameIntent.Evolve(card.id, target.card.id))
        else -> {}
    }
}

/**
 * Inicia la jugada de una carta de la mano. Devuelve la carta si necesita ELEGIR un
 * objetivo (energía / evolución); null si se jugó directamente (Básico→Banca, Entrenador)
 * o no es jugable. Los Entrenadores dirigidos (Poción) se juegan con [GameIntent.PlayTrainer]:
 * el motor abre entonces una decisión de objetivo que resuelve el DecisionPanel.
 */
private fun startPlay(card: Card, state: GameState, vm: CombatSceneController): Card? = when (card) {
    is EnergyCard -> card
    is PokemonCard -> when {
        card.isBasic -> { if (state.player.bench.size < 5) vm.onIntent(GameIntent.PlayBasicToBench(card.id)); null }
        card.evolvesFrom != null -> card
        else -> null
    }
    is TrainerCard -> { vm.onIntent(GameIntent.PlayTrainer(card.id)); null }
}

// -------------------------------------------------------------- Zonas colocadas

/** Un Pokémon en su caja: la carta LLENA la caja + HP/energía/estados superpuestos. */
@Composable
private fun FieldCard(
    pip: PokemonInPlay?,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
    selected: Boolean = false,
    highlighted: Boolean = false,
    glow: AbilityGlow? = null,
    onTap: (() -> Unit)? = null,
    draggable: Boolean = false,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier
            .fillMaxSize()
            // Actor visual: por defecto identidad (el juego no provee controller → sin cambios). Una
            // animación puede tomar el control TEMPORAL de ESTA MISMA carta (sin placeholder ni copia).
            .then(if (pip != null) Modifier.actorControlled(pip.card.id.raw) else Modifier)
            .onGloballyPositioned { coords = it }
            .then(if (onTap != null && pip != null) Modifier.clickable(onClick = onTap) else Modifier)
            .then(
                if (draggable && pip != null) Modifier.pointerInput(pip.card.id) {
                    detectDragGestures(
                        onDragStart = { local -> coords?.let { onDragStart(it.localToRoot(local)) } },
                        onDrag = { change, _ -> change.consume(); coords?.let { onDrag(it.localToRoot(change.position)) } },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                    )
                } else Modifier,
            ),
    ) {
        if (pip == null) {
            EmptySlot(Modifier.fillMaxSize(), highlighted = highlighted)
            return@Box
        }
        CombatCard(
            imageUrl = pip.card.artwork.small(true),
            faceDown = faceDown,
            selected = selected || highlighted,
            contentDescription = pip.card.name.es,
            card = if (faceDown) null else pip.card,
            // Entrada al entrar en juego (aterrizaje) + elevación física al resaltar/seleccionar. La
            // entrada se omite si el anfitrión desactiva las auto-animaciones (el Studio anima la carta
            // por su sistema de actores, evitando una entrada residual tras el Drop).
            modifier = Modifier
                .fillMaxSize()
                .then(if (LocalCombatAutoMotion.current) Modifier.motionAppear(pip.card.id) else Modifier)
                .motionElevate(selected || highlighted),
        )
        if (!faceDown) {
            // Aura de Habilidad PERSISTENTE (estado, como TCG Live): anillo pulsante SOBRE el borde de
            // la carta. ROJO = Habilidad pasiva activa (siempre); DORADO = Habilidad manual disponible
            // para activar. Ver GameEngine.abilityGlow. Se dibuja encima del arte (no se recorta).
            if (glow != null) AbilityGlowAura(glow, Modifier.matchParentSize())
            // Herramienta Pokémon anexada: como en TCG Live, una mini-carta apoyada ENCIMA del Pokémon,
            // en el lado izquierdo y a ~1/3 de altura, solapando el arte (con sombra). Máx. 1 (regla).
            pip.attachedTools.forEach { ToolPeek(it) }
            HpPill(pip.remainingHp, pip.card.hp, Modifier.align(Alignment.TopEnd).padding(2.dp))
            if (pip.attachedEnergy.isNotEmpty()) {
                AttachedEnergyRow(pip, Modifier.align(Alignment.BottomEnd).padding(3.dp))
            }
            if (pip.statuses.isNotEmpty()) {
                StatusRow(pip.statuses, Modifier.align(Alignment.BottomStart).padding(2.dp))
            }
        }
    }
}

/**
 * Energías unidas al Pokémon en el tablero, como en Pokémon TCG Live: una fila de pequeñas ESFERAS
 * de tipo (agrupadas por tipo, con ×n si hay varias del mismo), sobre una píldora oscura en la
 * esquina inferior-derecha. Sustituye al antiguo contador numérico (que no mostraba el TIPO).
 */
@Composable
private fun BoxScope.AttachedEnergyRow(pip: PokemonInPlay, modifier: Modifier = Modifier) {
    // Una SOLA fila de esferas individuales (una por energía), agrupadas por tipo y ordenadas de
    // MAYOR a menor número de ese tipo. Las Especiales van al final como esferas incoloras.
    val basics = pip.attachedEnergy.filterIsInstance<BasicEnergy>()
        .groupingBy { it.type }.eachCount().toList().sortedByDescending { it.second }
    val specials = pip.attachedEnergy.size - basics.sumOf { it.second }
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xB3121821))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        basics.forEach { (type, n) -> repeat(n) { EnergySphere(type = type, size = 14.dp) } }
        repeat(specials) { EnergySphere(type = null, size = 14.dp) }
    }
}

/**
 * Herramienta Pokémon anexada, dibujada como en Pokémon TCG Live: una mini-carta apoyada ENCIMA del
 * Pokémon, en el lado IZQUIERDO y a ~1/3 de altura, solapando el arte. Se renderiza después del
 * Pokémon (queda por delante) con una sombra que la despega de la carta. Ref.: frames del video
 * "VISTA DE ESTADIO Y HERRAMIENTA" (la Tool cubre el borde izquierdo-superior del Pokémon).
 */
@Composable
private fun BoxScope.ToolPeek(tool: Card) {
    CombatCard(
        imageUrl = tool.artwork.small(true),
        contentDescription = tool.name.es,
        card = null, // solo el arte: es un adorno de tablero, sin holo ni selección
        modifier = Modifier
            .align(Alignment.CenterStart)
            .fillMaxWidth(0.42f)
            .aspectRatio(CombatTheme.CardAspect)
            .graphicsLayer {
                // Izquierda y algo por encima del centro; asoma ~10% por el borde izquierdo.
                translationX = -size.width * 0.10f
                translationY = -size.height * 0.30f
                shadowElevation = 12f
                shape = RoundedCornerShape(6.dp)
                clip = false
            },
    )
}

/**
 * Aura de Habilidad PERSISTENTE (como TCG Live): un ANILLO pulsante brillante sobre el borde de la
 * carta que indica que la Habilidad está activa/disponible. ROJO = pasiva (siempre visible mientras
 * esté en juego); DORADO = manual disponible para activar. Se dibuja dentro de los límites de la carta
 * (nunca se recorta) con un halo interior suave + trazo brillante que respira.
 */
@Composable
private fun BoxScope.AbilityGlowAura(glow: AbilityGlow, modifier: Modifier = Modifier) {
    // Indicador persistente = MISMA aura que el Studio (bloom + rim-glow + shimmer), data-driven
    // por AbilityGlowVisuals. PASSIVE → rojo estable; MANUAL → dorado con shimmer (call-to-action).
    com.mineralord.tcg.core.animationcompose.PersistentAbilityGlow(
        manual = glow == AbilityGlow.MANUAL,
        modifier = modifier,
    )
}

/** Carta simple (estadio) que llena su caja. */
@Composable
private fun FieldCardImage(url: String?, cd: String?, onTap: () -> Unit) {
    CombatCard(url, contentDescription = cd, modifier = Modifier.fillMaxSize().clickable(onClick = onTap))
}

/** Píldora de PS pequeña sobre la carta. */
@Composable
private fun HpPill(remaining: Int, max: Int, modifier: Modifier = Modifier) {
    val frac = if (max <= 0) 0f else remaining.toFloat() / max
    val accent = when {
        frac > 0.5f -> CombatTheme.Good
        frac > 0.2f -> CombatTheme.Gold
        else -> CombatTheme.Foe
    }
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xF2101319))
            .border(1.dp, accent, RoundedCornerShape(50))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        AnimatedCount(remaining, color = Color.White, fontSize = 10.sp)
    }
}

/**
 * PREMIOS (estilo TCG Live): 6 dorsos VERTICALES de tamaño real (ancho de la caja → alto por
 * CardAspect, NUNCA deformados), solapados con la base abajo y asomando hacia arriba, llenando
 * el alto del slot. Posiciones FIJAS (el paso no depende del conteo): al retirar un premio, las
 * restantes NO se mueven. El frontal (i=0) queda encima.
 */
@Composable
private fun PrizeFan(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) {
        Box(modifier.fillMaxSize())
        return
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val cardW = maxWidth
        val cardH = maxHeight
        val pCardH = cardW / CombatTheme.CardAspect
        val total = 6
        val n = count.coerceIn(1, total)
        val step = ((cardH - pCardH) / (total - 1)).coerceAtLeast(pCardH * 0.08f)
        Box(Modifier.fillMaxSize()) {
            for (i in (n - 1) downTo 0) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = -step * i)
                        .width(cardW)
                        .height(pCardH)
                        .clip(RoundedCornerShape(CombatTheme.CardCorner)),
                ) { CardBack(Modifier.fillMaxSize()) }
            }
        }
    }
}

/** Pila (mazo/descarte/premios): dorso o carta superior + contador. */
@Composable
private fun PileStack(count: Int, modifier: Modifier = Modifier, faceDown: Boolean, topCard: Card? = null) {
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(CombatTheme.CardCorner))) {
            if (!faceDown && topCard != null) {
                AsyncImage(
                    model = topCard.artwork.small(true),
                    contentDescription = topCard.name.es,
                    contentScale = ContentScale.Crop,
                    // La carta que llega al descarte "aterriza" con la entrada Motion.
                    modifier = Modifier.fillMaxSize().motionAppear(topCard.id),
                )
            } else {
                CardBack(Modifier.fillMaxSize())
            }
        }
        if (count > 0) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                AnimatedCount(count, fontSize = 11.sp)
            }
        }
    }
}

/** Dorsos de la mano rival (hasta 7 visibles). */
@Composable
private fun OppHandBacks(count: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy((-6).dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(count.coerceAtMost(7)) {
                Box(
                    Modifier
                        .fillMaxHeight(0.9f)
                        .aspectRatio(CombatTheme.CardAspect)
                        .clip(RoundedCornerShape(4.dp)),
                ) { CardBack(Modifier.fillMaxSize()) }
            }
        }
        // Contador de cartas en la mano rival, anclado a un lado del abanico.
        if (count > 0) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(CombatTheme.Surface.copy(alpha = 0.9f))
                    .border(1.dp, CombatTheme.Gold.copy(alpha = 0.7f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    "$count",
                    color = CombatTheme.Gold,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

/** Mano del jugador: abanico dinámico (LazyRow con leve solape) dentro de su banda. */
@Composable
private fun HandRow(hand: List<Card>, onTap: (Card) -> Unit) {
    if (hand.isEmpty()) return
    LazyRow(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy((-14).dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        items(hand, key = { it.id.raw }) { card ->
            CombatCard(
                imageUrl = card.artwork.small(true),
                contentDescription = card.name.es,
                card = card,
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .aspectRatio(CombatTheme.CardAspect)
                    .padding(bottom = 4.dp)
                    .clickable { onTap(card) },
            )
        }
    }
}

// -------------------------------------------------------------- HUD átomos

@Composable
private fun PrizeChip(count: Int, accent: Color) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(9.dp))
            .background(CombatTheme.Surface.copy(alpha = 0.9f))
            .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("★ $count", color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

@Composable
private fun IconChip(symbol: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(CombatTheme.Surface.copy(alpha = 0.85f))
            .border(1.dp, CombatTheme.Border, RoundedCornerShape(10.dp))
            .motionPress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = accent, fontSize = 15.sp)
    }
}

@Composable
private fun BannerText(text: String) {
    Text(
        text,
        color = CombatTheme.OnSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE60F1420))
            .border(1.dp, CombatTheme.Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

// -------------------------------------------------------------- Overlays

/**
 * Acciones del Activo propio en el visor a detalle: filas de ATAQUE (panel achaflanado
 * naranja con coste de energía + nombre + descripción + daño, al estilo TCG Live pero con
 * arte propio) y fila de RETIRADA con su coste.
 */
@Composable
private fun ActiveActions(
    pip: PokemonInPlay,
    canRetreat: Boolean,
    onAttack: (String) -> Unit,
    onRetreat: () -> Unit,
    onUseAbility: (Ability) -> Unit,
    canUseAbility: (Ability) -> Boolean,
    canUseAttack: (String) -> Boolean,
    abilityUsed: (Ability) -> Boolean,
) {
    Column(
        Modifier
            .fillMaxWidth()
            // Recuadro ÚNICO con el gris azulado del design system (BgBottom): stats + ataques juntos.
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFFE3EEF4))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Panel de estadísticas estilo TCG Live (Debilidad/Resistencia/Retirada/Energía/Herramientas)
        // ENCIMA de las acciones, dentro del mismo scroll.
        PokemonDetailSheet(pip)
        // TIPO del Pokémon y su ACABADO (holo) alimentan el nuevo sistema de paneles: el marco
        // plateado se dibuja, el interior toma el color del tipo y el holo del panel reutiliza
        // EXACTAMENTE el shader de la carta (mismo Finish). Ver ActionPanels.kt.
        val type = pip.card.types.firstOrNull()
        val finish = resolveFinish(pip.card.rarity)
        // Habilidades primero (como en la referencia): MANUAL si el motor la ofrece como intent
        // legal (o ya se usó este turno → panel manual con "USADO"); PASIVA en caso contrario.
        pip.card.abilities.forEach { ability ->
            val usable = canUseAbility(ability)
            val used = abilityUsed(ability)
            if (usable || used) {
                ManualAbilityPanel(ability, type, finish, enabled = usable, used = used, onUse = { onUseAbility(ability) })
            } else {
                PassiveAbilityPanel(ability, type, finish)
            }
        }
        pip.card.attacks.forEach { atk ->
            AttackPanel(
                attack = atk,
                type = type,
                finish = finish,
                enabled = canUseAttack(atk.name.es),
                used = false,
                // Texto de regla REAL impreso; en blanco si el ataque solo hace daño → sin descripción.
                description = atk.text.es,
                onUse = { onAttack(atk.name.es) },
            )
        }
        RetreatPanel(
            type = type,
            finish = finish,
            cost = pip.card.retreatCost,
            enabled = canRetreat,
            onRetreat = onRetreat,
        )
    }
}

/**
 * Acciones para un Pokémon MÍO de la BANCA: solo la sección de Habilidades (las manuales usables
 * desde la Banca —Dodrio, Starmie, Mew ex, Persian, Snorlax…— y las pasivas como información). No hay
 * ataque ni retirada porque no es el Activo. Reusa exactamente los mismos paneles que [ActiveActions].
 */
@Composable
private fun BenchAbilityActions(
    pip: PokemonInPlay,
    onUseAbility: (Ability) -> Unit,
    canUseAbility: (Ability) -> Boolean,
    abilityUsed: (Ability) -> Boolean,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFFE3EEF4))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PokemonDetailSheet(pip)
        val type = pip.card.types.firstOrNull()
        val finish = resolveFinish(pip.card.rarity)
        pip.card.abilities.forEach { ability ->
            val usable = canUseAbility(ability)
            val used = abilityUsed(ability)
            if (usable || used) {
                ManualAbilityPanel(ability, type, finish, enabled = usable, used = used, onUse = { onUseAbility(ability) })
            } else {
                PassiveAbilityPanel(ability, type, finish)
            }
        }
    }
}

@Composable
private fun AttackSheet(
    active: PokemonInPlay,
    onAttack: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(CombatTheme.Surface)
            .border(1.dp, CombatTheme.Border, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(active.card.name.es, color = CombatTheme.OnSurface, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Text("✕", color = CombatTheme.Muted, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onDismiss))
        }
        if (active.card.attacks.isEmpty()) {
            Text("Sin ataques.", color = CombatTheme.Muted, fontSize = 12.sp)
        }
        active.card.attacks.forEach { atk ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CombatTheme.Gold.copy(alpha = 0.9f))
                    .clickable { onAttack(atk.name.es) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(atk.name.es, color = Color(0xFF20232B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ZoneInspectorBody(title: String, cards: List<Card>, onCardTap: (Card) -> Unit, onDismiss: () -> Unit) {
    run {
        Column(
            Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(CombatTheme.PanelCorner))
                .background(CombatTheme.Surface)
                .border(1.dp, CombatTheme.Border, RoundedCornerShape(CombatTheme.PanelCorner))
                .clickable {}
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("$title · ${cards.size}", color = CombatTheme.Gold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text("✕", color = CombatTheme.Muted, fontSize = 20.sp, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { card ->
                            CombatCard(
                                imageUrl = card.artwork.large(true),
                                contentDescription = card.name.es,
                                card = card,
                                modifier = Modifier.weight(1f).aspectRatio(CombatTheme.CardAspect).clickable { onCardTap(card) },
                            )
                        }
                        repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardInspector(card: Card, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xE6000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = card.artwork.large(true),
            contentDescription = card.name.es,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(CombatTheme.CardAspect).clip(RoundedCornerShape(18.dp)),
        )
    }
}

@Composable
private fun GameOverPanel(
    won: Boolean,
    effectId: String?,
    cristales: Int,
    monedas: Int,
    onExit: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
        // Efecto cosmético de fin de partida (procedural), DETRÁS del panel.
        CosmeticCelebrationFx(
            theme = if (won) victoryEffectFor(effectId) else defeatEffectFor(effectId),
            modifier = Modifier.fillMaxSize(),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                if (won) "¡VICTORIA!" else "DERROTA",
                color = if (won) CombatTheme.Gold else CombatTheme.Foe,
                fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 2.sp,
            )
            // Recompensa PvE otorgada (Fase 2 §6.8): terminar la partida siempre premia.
            if (cristales > 0 || monedas > 0) {
                Text(
                    "RECOMPENSA",
                    color = CombatTheme.Muted, fontWeight = FontWeight.Bold,
                    fontSize = 11.sp, letterSpacing = 3.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (cristales > 0) RewardChip("+$cristales", "Cristales", Color(0xFF35C4E8))
                    if (monedas > 0) RewardChip("+$monedas", "Monedas", CombatTheme.Gold)
                }
            }
            PillButton("Volver al inicio", enabled = true, accent = CombatTheme.Gold, onClick = onExit)
        }
    }
}

/** Ficha de recurso ganado en el panel de fin de partida (cantidad + nombre, con su color). */
@Composable
private fun RewardChip(amount: String, name: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(amount, color = accent, fontWeight = FontWeight.Black, fontSize = 26.sp)
        Text(name, color = accent.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun PillButton(label: String, enabled: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) accent else accent.copy(alpha = 0.3f))
            .motionPress(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, color = Color(0xFF20232B), fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

@Composable
private fun CombatLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Preparando combate…", color = CombatTheme.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}
