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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineralord.tcg.core.designsystem.motion.AnimationCurves
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

    // Acercamiento de entrada: el sobre crece y se acerca a su parte superior.
    val zoom = remember { Animatable(0f) }
    LaunchedEffect(Unit) { zoom.animateTo(1f, tween(560, easing = AnimationCurves.EmphasizedDecelerate)) }

    // Progreso de rasgado (0..1) acumulado por arrastre HORIZONTAL sobre la tira superior.
    val tear = remember { Animatable(0f) }
    val tearThresholdPx = with(density) { 620.dp.toPx() }   // recorrido total del dedo para abrir

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

            // El sobre GRANDE con acercamiento a la parte superior. La tira de arriba se rasga con el dedo.
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .aspectRatio(0.62f)
                    .graphicsLayer {
                        val z = zoom.value
                        val s = 1f + z * 0.30f          // crece al entrar
                        scaleX = s; scaleY = s
                        translationY = z * 46f          // baja para enfatizar la parte SUPERIOR
                    },
            ) {
                // Sobre TROCEADO: la tira superior se despega con el rasgado (arte real partido).
                TearablePack(artUrl = expansion.packArtUrl, tear = tear.value, modifier = Modifier.fillMaxSize())

                // Costura de luz dentada que avanza con el dedo (encima del arte).
                TornTopOverlay(progress = tear.value, modifier = Modifier.fillMaxSize())

                // Zona de gesto: la franja SUPERIOR del sobre. Arrastre horizontal = rasgar.
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.30f)
                        .pointerInput(canOpen) {
                            if (!canOpen) return@pointerInput
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, delta ->
                                    change.consume()
                                    val next = (tear.value + abs(delta) / tearThresholdPx).coerceIn(0f, 1f)
                                    scope.launch { tear.snapTo(next) }
                                    if (next >= 1f && !firing) {
                                        firing = true
                                        onOpen()
                                    }
                                },
                                onDragEnd = {
                                    if (tear.value < 1f) scope.launch { tear.animateTo(0f, tween(260)) }
                                },
                            )
                        },
                ) {
                    // Dedo-guía animado sobre la línea de rasgado (solo antes de empezar a rasgar).
                    if (canOpen && tear.value < 0.02f) {
                        Text(
                            "👆",
                            fontSize = 26.sp,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .graphicsLayer {
                                    translationX = size.width * (0.12f + 0.62f * hint)
                                },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            if (canOpen) {
                Text(
                    "Desliza el dedo →",
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
 * Superposición del rasgado sobre el sobre: una línea dentada de luz que barre de izquierda a
 * derecha con [progress] (0..1) mientras la tira superior se separa dejando ver un resplandor.
 */
@Composable
private fun TornTopOverlay(progress: Float, modifier: Modifier = Modifier) {
    if (progress <= 0f) return
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val tearY = size.height * 0.16f
        val lift = progress * size.height * 0.09f          // cuánto se separa la tira
        val revealW = size.width * progress                 // avance del rasgado
        // Resplandor cálido que asoma por la abertura.
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFFF3C4), Color(0x00FFF3C4)),
                startY = tearY - lift, endY = tearY + lift * 2f,
            ),
            topLeft = Offset(0f, tearY - lift),
            size = Size(revealW, lift * 3f),
        )
        // La tira superior levantada (oscurece levemente y se despega).
        drawRect(
            color = Color(0x33000000),
            topLeft = Offset(0f, 0f),
            size = Size(revealW, (tearY - lift).coerceAtLeast(0f)),
        )
        // Borde DENTADO brillante en el frente del rasgado — MISMO zigzag que la máscara del sobre.
        val amp = size.height * SEAM_AMPLITUDE
        val step = size.width / SEAM_TEETH
        val edgeX = revealW
        val path = Path()
        path.moveTo(0f, tearY - amp)
        var i = 1
        while (i * step <= edgeX && i <= SEAM_TEETH) {
            val x = i * step
            path.lineTo(x, if (i % 2 == 0) tearY - amp else tearY + amp)
            i++
        }
        drawPath(path, color = Color.White.copy(alpha = 0.95f), style = Stroke(width = 3f))
        // Chispita en la punta del rasgado.
        if (progress < 1f) {
            drawCircle(Color.White, radius = 6f, center = Offset(edgeX.coerceIn(0f, size.width), tearY))
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

    fun generate(): List<RevealedCard> {
        val opened = opener.open(
            com.mineralord.tcg.data.gacha.RarityWeights.templateFor(SET_151_CODE),
            pool, kotlin.random.Random(System.nanoTime()), energyIds,
        )
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

/**
 * **Simulador de apertura de sobres a PANTALLA COMPLETA** (para el Studio). Recorre el MISMO flujo del
 * juego —elegir expansión → rasgar el sobre → revelado carta a carta— pero SIN monedero ni límites: se
 * puede abrir cuantas veces se quiera. No usa [PacksViewModel] (no persiste nada): mantiene una
 * colección EFÍMERA en memoria solo para decidir el badge "NUEVA". [onExit] vuelve al Studio.
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
            PackOpeningOverlay(
                cards = revealed,
                setLabel = "${series.title} · ${expansion?.name ?: "151"}",
                remainingToday = 999,
                onDismiss = { opening = false; step = PackStep.PACK },
            )
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
                    onOpen = { revealed = s.generate(); opening = true },
                    onExpire = {},
                )
            }
        }

        // Salir del simulador (vuelve al Studio). Oculto durante el revelado (que ocupa todo).
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x66000000))
                .clickable(onClick = onExit)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) { Text("✕ Salir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
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
