package com.mineralord.tcg.feature.packs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineralord.tcg.core.designsystem.motion.AnimationCurves
import com.mineralord.tcg.engine.model.Rarity
import kotlin.math.abs
import kotlinx.coroutines.launch

/** Paso del flujo de apertura, al estilo Pokémon TCG Pocket. */
private enum class PackStep { EXPANSION, PACK }

/**
 * Pantalla de sobres al estilo Pokémon TCG Pocket: SERIE → EXPANSIÓN → abrir el sobre (deslizando
 * hacia arriba). Sin ruleta de sobres. Hoy solo la serie «Escarlata y Púrpura» con la expansión 151.
 */
@Composable
fun PacksScreen(
    modifier: Modifier = Modifier,
    viewModel: PacksViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Apertura a pantalla completa (revelado carta a carta). Tiene prioridad sobre el navegador.
    if (state.opening) {
        PackOpeningOverlay(
            cards = state.revealed,
            setLabel = state.setLabel,
            remainingToday = state.remainingToday,
            onDismiss = viewModel::dismissOpening,
        )
        return
    }

    // Flujo directo a "Elige una expansión" (sin pantalla de serie). Serie única por ahora.
    var step by remember { mutableStateOf(PackStep.EXPANSION) }
    val series = remember { PACK_CATALOG.first() }
    var expansion by remember { mutableStateOf<ExpansionUi?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF161B2E), Color(0xFF0A0E1A)))),
    ) {
        if (state.loading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val dir = if (forward) 1 else -1
                (slideInHorizontally(tween(320)) { w -> dir * w } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(320)) { w -> -dir * w } + fadeOut(tween(180)))
            },
            label = "packStep",
        ) { s ->
            when (s) {
                PackStep.EXPANSION -> ExpansionSelectStage(
                    series = series,
                    ownedInSet = state.ownedInSet,
                    totalInSet = state.totalInSet,
                    onSelect = { expansion = it; step = PackStep.PACK },
                )
                PackStep.PACK -> PackStage(
                    expansion = expansion ?: PACK_CATALOG.first().expansions.first(),
                    remainingToday = state.remainingToday,
                    maxPerDay = state.maxPerDay,
                    secondsToNext = state.secondsToNext,
                    deniedMessage = state.deniedMessage,
                    onBack = { step = PackStep.EXPANSION },
                    onOpen = viewModel::openPack,
                    onExpire = viewModel::refresh,
                )
            }
        }
    }
}

/**
 * Etapa del sobre estilo Pokémon TCG Pocket: el envoltorio aparece GRANDE y hace un acercamiento a
 * su parte SUPERIOR; para abrirlo se **rasga** deslizando el dedo en horizontal por la tira de arriba
 * (no se desliza hacia arriba). Al completar el rasgado se dispara [onOpen] y arranca el revelado.
 */
@Composable
private fun PackStage(
    expansion: ExpansionUi,
    remainingToday: Int,
    maxPerDay: Int,
    secondsToNext: Long?,
    deniedMessage: String?,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onExpire: () -> Unit,
) {
    val canOpen = remainingToday > 0
    // Cuenta atrás VIVA al próximo sobre: se siembra con el snapshot (tiempo confiable) y baja 1/s
    // localmente; al llegar a 0 pide recalcular (acredita el sobre y reprograma el siguiente).
    var countdown by remember(secondsToNext) { mutableStateOf(secondsToNext) }
    LaunchedEffect(secondsToNext) {
        var s = secondsToNext ?: return@LaunchedEffect
        while (s > 0) {
            kotlinx.coroutines.delay(1000)
            s -= 1
            countdown = s
        }
        onExpire()
    }
    val scope = rememberCoroutineScope()
    var firing by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // Feedback físico: audio (agarre/tensión/rasgado) + háptica. Hooks centralizados.
    val context = LocalContext.current
    val audio = remember { PackAudio() }
    val feedback = remember { BoosterFeedback(audio, vibratorOf(context)) }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    var lastTick by remember { mutableStateOf(0f) }

    // PACK APPROACH: el sobre "llega" al jugador (viene de más lejos/abajo, inclinado) y se posiciona
    // con un overshoot muy pequeño → settle → listo. No es un zoom lineal: es una cámara acercándose.
    val approach = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        approach.snapTo(0f)
        feedback.sfx(SfxCue.PACK_HOVER)
        approach.animateTo(1.05f, tween(560, easing = AnimationCurves.EmphasizedDecelerate)) // overshoot
        approach.animateTo(1f, tween(200))                                                    // settle → READY
    }

    // Progreso de rasgado (0..1) ACUMULADO por delta del dedo (no por posición absoluta): así el gesto
    // es CONTINUO y no se pierde al llegar al borde de la pantalla. Umbral acorde a un solo barrido.
    val tear = remember { Animatable(0f) }
    val tearThresholdPx = with(density) { 300.dp.toPx() }

    // Dedo-guía que va y viene en horizontal sobre la línea de rasgado.
    val hintAnim = rememberInfiniteTransition(label = "tearHint")
    val hint by hintAnim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "hintX",
    )

    Box(Modifier.fillMaxSize().padding(20.dp)) {
        // Botón "Volver" (toca para retroceder a la elección de expansión).
        Box(
            Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(50))
                .background(Color(0x22FFFFFF))
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) { Text("‹  Volver", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (canOpen) "Rasga el sobre por arriba" else "Sin sobres disponibles",
                color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text("Sobres: $remainingToday/$maxPerDay", color = Color(0xB3FFFFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            countdown?.let {
                Text("Próximo sobre en ${fmtCountdown(it)}", color = Color(0x99FFFFFF), fontSize = 11.sp)
            }
            Spacer(Modifier.height(18.dp))

            // El sobre GRANDE con acercamiento a la parte superior. Se rasga con UN gesto continuo.
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(0.62f)
                    // GESTO: agarre en TODO el sobre; el progreso acumula el DELTA del dedo (dirección
                    // tolerante), así llegar al borde no interrumpe el rasgado. Un solo gesto continuo.
                    .pointerInput(canOpen) {
                        if (!canOpen) return@pointerInput
                        detectDragGestures(
                            onDragStart = {
                                lastTick = 0f
                                feedback.sfx(SfxCue.PACK_GRAB); feedback.haptic(HapticCue.CONTACT)
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                // Magnitud del movimiento (X domina; Y aporta un poco) → intención de rasgar.
                                val mag = abs(drag.x) + abs(drag.y) * 0.35f
                                val next = (tear.value + mag / tearThresholdPx).coerceIn(0f, 1f)
                                scope.launch { tear.snapTo(next) }
                                if (next - lastTick >= 0.11f) { lastTick = next; feedback.tensionTick(next) }
                                if (next >= 1f && !firing) {
                                    firing = true
                                    feedback.sfx(SfxCue.PACK_TEAR); feedback.haptic(HapticCue.TEAR)
                                    onOpen()
                                }
                            },
                            onDragEnd = {
                                lastTick = 0f
                                if (tear.value < 1f) scope.launch { tear.animateTo(0f, tween(260)) }
                            },
                        )
                    }
                    .graphicsLayer {
                        val a = approach.value
                        val ac = a.coerceAtMost(1f)
                        val t = tear.value
                        cameraDistance = 20f * this.density
                        // Llega desde más lejos/abajo, inclinado, y se endereza al posicionarse.
                        val s = 0.74f + 0.30f * a
                        scaleX = s
                        scaleY = s * (1f + t * 0.05f)   // TENSIÓN: el plástico se estira al tirar
                        rotationX = (1f - ac) * 16f     // inclinado al aproximarse → plano al settle
                        rotationZ = t * 1.4f            // leve torsión del material al tirar
                        translationY = (1f - ac) * 120f + 44f * ac   // de abajo hacia su posición
                    },
            ) {
                // Interior del sobre asomando por la abertura (profundidad exterior→interior).
                PackInterior(tear = tear.value, modifier = Modifier.fillMaxSize())
                // Sobre TROCEADO: la tira superior se despega con el rasgado (arte real partido).
                TearablePack(artUrl = expansion.packArtUrl, tear = tear.value, modifier = Modifier.fillMaxSize())
                // Costura de luz irregular que avanza con el dedo (encima del arte).
                TornTopOverlay(progress = tear.value, modifier = Modifier.fillMaxSize())

                // Dedo-guía animado (solo antes de empezar a rasgar).
                if (canOpen && tear.value < 0.02f) {
                    Text(
                        "👆",
                        fontSize = 26.sp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .graphicsLayer { translationX = size.width * (0.12f + 0.62f * hint); translationY = size.height * 0.12f },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            if (canOpen) {
                Text(
                    "Desliza para rasgar  →",
                    color = Color.White.copy(alpha = 0.55f + 0.35f * hint),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                )
            }
            deniedMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * Interior del sobre visible por la abertura: profundidad oscura (detrás de la tira levantada) con una
 * luz cálida contenida que crece con el rasgado. Da la lectura EXTERIOR → BORDE → INTERIOR.
 */
@Composable
private fun PackInterior(tear: Float, modifier: Modifier = Modifier) {
    if (tear <= 0f) return
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val seamY = size.height * 0.16f
        val a = tear.coerceIn(0f, 1f)
        val frontX = size.width * a          // interior SOLO en la zona ya rasgada (x < front)
        // Profundidad interior (tras la tira despegada), limitada al tramo abierto.
        drawRect(
            color = Color(0xFF07090F),
            topLeft = Offset(0f, 0f),
            size = Size(frontX, seamY),
        )
        // Luz interior cálida contenida, también limitada al tramo abierto.
        val glowH = size.height * 0.12f
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0x00FFE9A6), Color(0xFFFFE3A0).copy(alpha = 0.5f)),
                startY = seamY - glowH, endY = seamY,
            ),
            topLeft = Offset(0f, seamY - glowH),
            size = Size(frontX, glowH),
        )
    }
}

/**
 * Luz del rasgado: SOLO en el TEAR FRONT (el material recién roto). NO es una línea que sigue al dedo:
 * es una ventana corta pegada al frente que ilumina lo recién abierto y se apaga por detrás; nunca
 * ilumina material aún cerrado (x > front) ni se adelanta al rasgado.
 */
@Composable
private fun TornTopOverlay(progress: Float, modifier: Modifier = Modifier) {
    if (progress <= 0f) return
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val seamY = size.height * 0.16f
        val ys = seamYs(seamY, size.height * SEAM_AMPLITUDE, SEAM_SEGMENTS, SEAM_SEED)
        val frontX = (size.width * progress).coerceIn(0f, size.width)
        val winW = size.width * 0.16f                 // longitud limitada de la luz, detrás del frente
        val x0 = (frontX - winW).coerceAtLeast(0f)
        val segments = ys.size - 1
        val step = size.width / segments

        // Resplancor corto pegado al frente (más brillante cerca del frente, se apaga por detrás).
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color(0x00FFF3C4), Color(0xCCFFF3C4)),
                startX = x0, endX = frontX,
            ),
            topLeft = Offset(x0, seamY - size.height * 0.05f),
            size = Size((frontX - x0).coerceAtLeast(0f), size.height * 0.12f),
        )
        // Borde irregular brillante SOLO en la ventana del frente (recorriendo el perfil real).
        val path = Path()
        var started = false
        var i = 0
        while (i <= segments) {
            val x = i * step
            if (x in x0..frontX) {
                if (!started) { path.moveTo(x, ys[i]); started = true } else path.lineTo(x, ys[i])
            }
            i++
        }
        if (started) drawPath(path, color = Color.White.copy(alpha = 0.9f), style = Stroke(width = 3f))
        // Punta del rasgado: fragmentos que saltan (solo mientras se propaga).
        if (progress in 0.02f..0.99f) {
            val frn = kotlin.random.Random((progress * 40f).toInt())
            repeat(4) {
                val dx = (frn.nextFloat() - 0.5f) * 22f
                val dy = -frn.nextFloat() * 20f
                drawRect(
                    color = Color(0xFFF3ECE0).copy(alpha = 0.85f),
                    topLeft = Offset(frontX + dx, seamY + dy),
                    size = Size(3f + frn.nextFloat() * 3f, 3f + frn.nextFloat() * 3f),
                )
            }
            drawCircle(Color.White, radius = 5f, center = Offset(frontX, seamY))
        }
    }
}

// ============================ SIMULADOR DE APERTURA (Studio) ============================

/** Datos precargados para el simulador: catálogo de rarezas del 151 + energías + colección efímera. */
private class SimPacks(
    val repo: com.mineralord.tcg.data.cards.CardRepository,
    val pool: com.mineralord.tcg.data.gacha.PackPool,
    val energyIds: List<com.mineralord.tcg.engine.model.CardId>,
    val totalInSet: Int,
) {
    private val opener = com.mineralord.tcg.data.gacha.PackOpener()
    /** Colección EFÍMERA (solo en memoria): permite que "NUEVA" aparezca la primera vez y no después. */
    val owned = HashMap<String, Int>()

    /**
     * @param forceTier si no es null, sustituye (SOLO en el simulador) la rareza de la ÚLTIMA carta por
     * una representativa de ese escalón, para poder previsualizar ese reveal. No toca el RNG del juego.
     */
    fun generate(forceTier: RarityTier? = null): List<RevealedCard> {
        val opened0 = opener.open(
            com.mineralord.tcg.data.gacha.RarityWeights.templateFor(SET_151_CODE),
            pool, kotlin.random.Random(System.nanoTime()), energyIds,
        )
        val opened = if (forceTier == null || opened0.isEmpty()) opened0 else {
            opened0.toMutableList().also { it[it.lastIndex] = it.last().copy(rarity = representativeRarity(forceTier)) }
        }
        val running = HashMap<String, Int>()
        val revealed = opened.map { oc ->
            val card = repo[oc.id]
            val cap = com.mineralord.tcg.data.profile.ProfileRepository.capFor(oc.id.raw)
            val idRaw = oc.id.raw
            val before = ((owned[idRaw] ?: 0) + (running[idRaw] ?: 0)).coerceAtMost(cap)
            running[idRaw] = (running[idRaw] ?: 0) + 1
            RevealedCard(
                name = card?.name?.es ?: idRaw,
                rarity = oc.rarity,
                imageUrl = card?.artwork?.smallEs,
                isNew = before == 0,
                copiesOwned = (before + 1).coerceAtMost(cap),
                cap = cap,
                imageLarge = card?.artwork?.large(true),
                cardNumber = idRaw.substringAfterLast('-').toIntOrNull(),
                setCode = if (idRaw.startsWith("energy")) "energy" else idRaw.substringBeforeLast('-'),
            )
        }
        // Acredita a la colección efímera (para el "NUEVA" de futuras aperturas en la misma sesión).
        opened.forEach { owned[it.id.raw] = (owned[it.id.raw] ?: 0) + 1 }
        return revealed
    }

    companion object {
        private const val SET_151_PREFIX = "sv3pt5-"
        private const val SET_151_CODE = "sv3pt5"

        suspend fun load(): SimPacks {
            val r = com.mineralord.tcg.data.cards.CardRepository.load()
            val pool151 = r.all.filter {
                it.id.raw.startsWith(SET_151_PREFIX) &&
                    !com.mineralord.tcg.data.profile.ProfileRepository.isEnergyId(it.id.raw)
            }
            val energies = r.all
                .filter { com.mineralord.tcg.data.profile.ProfileRepository.isEnergyId(it.id.raw) }
                .map { it.id }
            val total = r.all.count { it.id.raw.startsWith(SET_151_PREFIX) }
            return SimPacks(r, com.mineralord.tcg.data.gacha.PackPool.from(pool151), energies, total)
        }
    }
}

/** Rareza representativa de un escalón (para el forzado de previsualización del Lab). */
private fun representativeRarity(tier: RarityTier): Rarity = when (tier) {
    RarityTier.CLEAN -> Rarity.COMMON
    RarityTier.IMPACT -> Rarity.RARE_HOLO
    RarityTier.SPECTACLE -> Rarity.DOUBLE_RARE
    RarityTier.LEGENDARY -> Rarity.SPECIAL_ILLUSTRATION_RARE
}

/**
 * **Simulador de apertura de sobres a PANTALLA COMPLETA** = el Booster Opening Lab del Studio. Recorre
 * el MISMO flujo del juego —elegir expansión → rasgar el sobre → revelado por BEATS— pero SIN monedero
 * ni límites, y añade los CONTROLES DE LAB (velocidad, auto/Play, bucle, reiniciar, forzar rareza, beat
 * actual). No usa [PacksViewModel] ni persiste nada: colección EFÍMERA en memoria para el badge "NUEVA".
 * [onExit] vuelve al Studio.
 */
@Composable
fun PackOpeningSimulator(
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
) {
    val sim by androidx.compose.runtime.produceState<SimPacks?>(null) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { SimPacks.load() }
    }
    val series = remember { PACK_CATALOG.first() }
    var step by remember { mutableStateOf(PackStep.EXPANSION) }
    var expansion by remember { mutableStateOf<ExpansionUi?>(null) }
    var opening by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf<List<RevealedCard>>(emptyList()) }
    var session by remember { mutableStateOf(0) }   // reinicia el overlay al incrementarse

    // Controles del Lab.
    var speed by remember { mutableStateOf(1f) }
    var auto by remember { mutableStateOf(false) }
    var loop by remember { mutableStateOf(false) }
    var forceTier by remember { mutableStateOf<RarityTier?>(null) }
    var beat by remember { mutableStateOf<Beat?>(null) }
    var hudVisible by remember { mutableStateOf(false) }   // durante el revelado, HUD oculto por defecto

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF161B2E), Color(0xFF0A0E1A)))),
    ) {
        val s = sim
        if (s == null) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        if (opening) {
            key(session) {
                PackOpeningOverlay(
                    cards = revealed,
                    setLabel = "${series.title} · ${expansion?.name ?: "151"}",
                    remainingToday = 999,
                    speed = speed,
                    autoAdvance = auto,
                    onBeat = { beat = it },
                    onDismiss = {
                        if (loop) { revealed = s.generate(forceTier); session++ }
                        else { opening = false; step = PackStep.PACK }
                    },
                )
            }
        } else {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(320)) { w -> dir * w } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(320)) { w -> -dir * w } + fadeOut(tween(180)))
                },
                label = "simStep",
            ) { st ->
                when (st) {
                    PackStep.EXPANSION -> ExpansionSelectStage(
                        series = series,
                        ownedInSet = s.owned.size,
                        totalInSet = s.totalInSet,
                        onSelect = { expansion = it; step = PackStep.PACK },
                    )
                    PackStep.PACK -> PackStage(
                        expansion = expansion ?: series.expansions.first(),
                        remainingToday = 999,
                        maxPerDay = 999,
                        secondsToNext = null,
                        deniedMessage = null,
                        onBack = { step = PackStep.EXPANSION },
                        onOpen = { revealed = s.generate(forceTier); session++; hudVisible = false; opening = true },
                        onExpire = {},
                    )
                }
            }
        }

        // ---- SEPARACIÓN MODO DISEÑO / MODO PRESENTACIÓN ----
        // En diseño (elección/rasgado) el HUD está visible. Durante el REVELADO se oculta para no
        // competir con la escena (Presentation Mode); un botón discreto lo trae de vuelta.
        val showHud = !opening || hudVisible
        if (showHud) {
            LabChip(
                "✕ Salir",
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                onClick = onExit,
            )
            LabControlBar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                speed = speed,
                auto = auto,
                loop = loop,
                forceTier = forceTier,
                beat = beat,
                opening = opening,
                onSpeed = { speed = when (speed) { 0.5f -> 1f; 1f -> 2f; else -> 0.5f } },
                onAuto = { auto = !auto },
                onLoop = { loop = !loop },
                onForce = {
                    forceTier = when (forceTier) {
                        null -> RarityTier.CLEAN
                        RarityTier.CLEAN -> RarityTier.IMPACT
                        RarityTier.IMPACT -> RarityTier.SPECTACLE
                        RarityTier.SPECTACLE -> RarityTier.LEGENDARY
                        RarityTier.LEGENDARY -> null
                    }
                },
                onRestart = { revealed = s.generate(forceTier); session++; hudVisible = false; opening = true },
            )
        }
        // Botón discreto para mostrar/ocultar el HUD durante la presentación.
        if (opening) {
            LabChip(
                if (hudVisible) "▾ Ocultar HUD" else "⚙",
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                onClick = { hudVisible = !hudVisible },
            )
        }
    }
}

/** Píldora táctil reutilizable del HUD del Lab. */
@Composable
private fun LabChip(label: String, modifier: Modifier = Modifier, active: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) Color(0xCC35C4E8) else Color(0x66000000))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) { Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

/** Barra de herramientas del Booster Opening Lab (velocidad · Play · bucle · forzar rareza · reiniciar). */
@Composable
private fun LabControlBar(
    modifier: Modifier,
    speed: Float,
    auto: Boolean,
    loop: Boolean,
    forceTier: RarityTier?,
    beat: Beat?,
    opening: Boolean,
    onSpeed: () -> Unit,
    onAuto: () -> Unit,
    onLoop: () -> Unit,
    onForce: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (opening && beat != null) {
            Text("BEAT · ${beat.name}", color = Color(0xAAFFFFFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LabChip("${if (speed == 0.5f) "0.5" else if (speed == 2f) "2" else "1"}×", onClick = onSpeed)
            LabChip("Play", active = auto, onClick = onAuto)
            LabChip("Bucle", active = loop, onClick = onLoop)
            LabChip(
                "Rareza: ${forceTier?.name ?: "RNG"}",
                active = forceTier != null,
                onClick = onForce,
            )
            LabChip(if (opening) "Reiniciar" else "Abrir", active = false, onClick = onRestart)
        }
    }
}

/** Formatea segundos como HH:MM:SS (o MM:SS si <1h) para la cuenta atrás del próximo sobre. */
private fun fmtCountdown(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}
