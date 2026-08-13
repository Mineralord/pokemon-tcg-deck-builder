package com.mineralord.tcg.feature.packs

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.InteractiveHoloCard
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.core.designsystem.motion.AnimationCurves
import com.mineralord.tcg.core.designsystem.motion.AnimationSprings
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos

/** Fases de alto nivel de la experiencia. Los BEATS finos viven dentro de [CardScene]. */
private enum class Phase { PACK_OPEN, CARDS, SUMMARY }

/**
 * **Apertura de sobre AAA** (capa de PRESENTACIÓN). Recibe un resultado ya determinado ([cards]) y lo
 * REPRESENTA como una pequeña experiencia cinematográfica por BEATS: el sobre se abre → anticipación →
 * la carta (boca abajo) EMERGE del interior → REVEAL con giro 3D (anticipation/action/impact/settle) →
 * respuesta escalada por RAREZA ([presentationFor]) → confirmación → resumen. Nada de esto decide
 * cartas/RNG/colección.
 *
 * @param speed multiplicador de velocidad para el Lab (0.5x/1x/2x…). No altera el resultado.
 * @param autoAdvance si `true`, encadena cartas solo (modo Play del Lab); si no, avanza al tocar.
 * @param onBeat notifica el beat actual (HUD del Lab).
 */
@Composable
fun PackOpeningOverlay(
    cards: List<RevealedCard>,
    setLabel: String,
    remainingToday: Int,
    onDismiss: () -> Unit,
    speed: Float = 1f,
    autoAdvance: Boolean = false,
    onBeat: (Beat) -> Unit = {},
) {
    val context = LocalContext.current
    val reducedMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val audio = remember { PackAudio() }
    val vibrator = remember { vibratorOf(context) }
    val feedback = remember { BoosterFeedback(audio, vibrator) }
    DisposableEffect(Unit) { onDispose { audio.release() } }

    val spd = speed.coerceIn(0.25f, 3f)
    var phase by remember { mutableStateOf(if (reducedMotion) Phase.SUMMARY else Phase.PACK_OPEN) }
    var index by remember { mutableIntStateOf(0) }

    // Fondo CLARO iridiscente permanente (persiste entre cartas → transición continua).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFDCE7F5), Color(0xFFEDE3F5), Color(0xFFDCEFF0)))),
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            Phase.PACK_OPEN -> {
                LaunchedEffect(Unit) {
                    onBeat(Beat.PACK_OPEN)
                    feedback.sfx(SfxCue.PACK_OPEN)
                    delay((520 / spd).toLong())
                    index = 0
                    phase = Phase.CARDS
                }
                PackOpenBeat()
            }
            Phase.CARDS -> key(index) {
                CardScene(
                    card = cards[index],
                    indexLabel = index + 1,
                    total = cards.size,
                    remaining = cards.size - index - 1,
                    feedback = feedback,
                    speed = spd,
                    autoAdvance = autoAdvance,
                    onBeat = onBeat,
                    onAdvance = { if (index >= cards.size - 1) phase = Phase.SUMMARY else index++ },
                    onSkip = { phase = Phase.SUMMARY },
                )
            }
            Phase.SUMMARY -> {
                LaunchedEffect(Unit) { onBeat(Beat.SUMMARY) }
                SummaryContent(cards, setLabel, onDismiss)
            }
        }
    }
}

// ============================ BEAT 06 · PACK OPEN ============================

/** El sobre (ya rasgado) se abre: estallido de luz interior con profundidad y rayos. */
@Composable
private fun PackOpenBeat() {
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) { burst.animateTo(1f, tween(520, easing = AnimationCurves.EmphasizedDecelerate)) }
    Box(
        Modifier.fillMaxSize().drawBehind {
            val f = burst.value
            // Profundidad: interior cálido que se abre desde el centro.
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFFFFF6D8).copy(alpha = 0.28f * (1f - f)), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.52f),
                    radius = size.minDimension * (0.4f + 0.6f * f),
                ),
            )
            // Flash + rayos radiales.
            val r = size.minDimension * (0.2f + 0.7f * f) * (1f - f * 0.3f)
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.7f * (1f - f)), Color(0xFFBFE3FF).copy(alpha = 0.3f * (1f - f)), Color.Transparent),
                    center = center, radius = r,
                ),
                radius = r, center = center,
            )
        },
    )
}

// ============================ BEATS 08–12 · CARTA ============================

/**
 * Escena de UNA carta con sus beats: EMERGENCE → CARD_BACK (anticipación) → REVEAL (giro 3D) →
 * RARITY_RESPONSE (escalado por rareza) → CONFIRM. La secuencia auto-reproduce el reveal y luego
 * espera el toque (o auto-avanza en modo Play). Al avanzar, la carta SALE (o vuela a la colección si
 * es NUEVA) sin reiniciar el fondo → continuidad.
 */
@Composable
private fun CardScene(
    card: RevealedCard,
    indexLabel: Int,
    total: Int,
    remaining: Int,
    feedback: BoosterFeedback,
    speed: Float,
    autoAdvance: Boolean,
    onBeat: (Beat) -> Unit,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
) {
    val density = LocalDensity.current
    val pres = remember(card.rarity) { presentationFor(card.rarity) }
    val accent = rarityColor(card.rarity)
    fun dur(ms: Int) = (ms / speed).toInt().coerceAtLeast(1)

    // Animatables de la escena.
    val emerge = remember { Animatable(0f) }   // 0 dentro del sobre → 1 presentada
    val flip = remember { Animatable(0f) }      // 0 dorso → 1 frente
    val glow = remember { Animatable(0f) }
    val camera = remember { Animatable(0f) }    // punch de cámara al asentar
    val bgLight = remember { Animatable(0f) }   // fondo iridiscente (premium)
    val exit = remember { Animatable(0f) }      // salida (avance normal)
    val collect = remember { Animatable(0f) }   // vuelo a la colección (carta nueva)

    var beat by remember { mutableStateOf(Beat.EMERGENCE) }
    var revealed by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }   // identidad/recompensa: DESPUÉS del reveal
    var leaving by remember { mutableStateOf(false) }
    var sparkTrigger by remember { mutableIntStateOf(0) }
    val flash = remember { Animatable(0f) }              // destello dirigido del reveal (IMPACT+)

    // Vaivén sutil continuo tras asentar (mantiene vivo el holo en premium).
    val infinite = rememberInfiniteTransition(label = "cardSway")
    val sway by infinite.animateFloat(
        -6f, 6f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "sway",
    )

    // Timeline por BEATS.
    LaunchedEffect(Unit) {
        // BEAT 08 — EMERGENCE: la carta boca abajo sube del interior.
        beat = Beat.EMERGENCE; onBeat(beat)
        feedback.sfx(SfxCue.CARD_EMERGE); feedback.haptic(HapticCue.EMERGE)
        emerge.animateTo(1f, tween(dur(560), easing = AnimationCurves.EmphasizedDecelerate))
        // BEAT 09/07 — CARD_BACK + ANTICIPATION: respiro (más largo cuanto mayor la rareza).
        beat = Beat.CARD_BACK; onBeat(beat)
        delay(dur(pres.anticipationMs).toLong())
        // BEAT 10 — REVEAL: giro 3D dorso→frente. Empieza en el dorso (180°+giros) y cae a 0°.
        beat = Beat.REVEAL; onBeat(beat)
        feedback.sfx(SfxCue.CARD_FLIP)
        launch { glow.animateTo(pres.glow, tween(dur(pres.revealMs))) }
        if (pres.iridescentBg) launch { bgLight.animateTo(1f, tween(dur(pres.revealMs))) }
        flip.animateTo(1f, tween(dur(pres.revealMs), easing = AnimationCurves.EmphasizedDecelerate))
        revealed = true
        // BEAT 11 — RARITY_RESPONSE: sonido/háptica/partículas escalados + impacto + destello dirigido.
        beat = Beat.RARITY_RESPONSE; onBeat(beat)
        sparkTrigger++
        feedback.sfx(pres.sfxReveal)
        feedback.sfx(SfxCue.REWARD_CONFIRM)
        feedback.haptic(if (pres.strongHaptic) HapticCue.RARITY else HapticCue.REVEAL)
        if (pres.tier >= RarityTier.IMPACT) launch {
            flash.animateTo(if (pres.tier >= RarityTier.LEGENDARY) 0.9f else 0.55f, tween(dur(90)))
            flash.animateTo(0f, tween(dur(380)))
        }
        launch {
            camera.animateTo(1f, AnimationSprings.bouncy())
            camera.animateTo(0f, tween(dur(260)))
        }
        delay(dur(pres.settleMs).toLong())
        // BEAT 12 — CONFIRM: recién ahora aparece la IDENTIDAD/recompensa (primero emoción, luego dato).
        beat = Beat.CONFIRM; onBeat(beat)
        showInfo = true
        if (autoAdvance) { delay((900 / speed).toLong()); triggerAdvanceAuto(card, feedback) { leaving = true } }
    }

    // Ejecuta la salida/colección cuando corresponde y luego avanza de verdad.
    LaunchedEffect(leaving) {
        if (!leaving) return@LaunchedEffect
        if (card.isNew) {
            feedback.sfx(SfxCue.REWARD_CONFIRM); feedback.haptic(HapticCue.REVEAL)
            collect.animateTo(1f, tween(dur(950), easing = AnimationCurves.EmphasizedAccelerate))
        } else {
            exit.animateTo(1f, tween(dur(320), easing = AnimationCurves.EmphasizedAccelerate))
        }
        onAdvance()
    }

    fun advance() {
        if (leaving) return
        if (!revealed) return    // no saltar el reveal a medias; el toque solo avanza tras revelar
        leaving = true
    }

    // Fondo claro SIEMPRE → textos oscuros. El realce iridiscente extra sigue reservado a premium.
    val iridescent = true
    val belowPx = with(density) { 380.dp.toPx() }
    val flightPx = with(density) { 330.dp.toPx() }

    Box(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { advance() },
    ) {
        // Fondo iridiscente para revelados premium (aparece con el reveal).
        if (iridescent) {
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = bgLight.value }.drawBehind {
                    drawRect(Brush.verticalGradient(listOf(Color(0xFFDCE7F5), Color(0xFFEDE3F5), Color(0xFFDCEFF0))))
                    drawRect(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(0f, 0f).let { Offset(it.x, it.y) },
                            radius = size.minDimension * 0.9f,
                        ),
                    )
                },
            )
        }

        // Pila de dorsos restantes (contexto de cuántas quedan).
        BackStack(
            remaining = remaining,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp).width(64.dp).fillMaxHeight(0.42f)
                .graphicsLayer { alpha = (1f - exit.value) * (1f - collect.value) },
        )

        // Progreso "n / total" (discreto, esquina superior).
        Text(
            "$indexLabel / $total",
            color = if (iridescent) Color(0x772A2438) else Color(0x66FFFFFF),
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 60.dp),
        )
        // Nombre: aparece con la RECOMPENSA (no durante el reveal) → primero emoción, luego identidad.
        if (showInfo) {
            val nameIn = remember { Animatable(0f) }
            LaunchedEffect(Unit) { nameIn.animateTo(1f, AnimationSprings.bouncy()) }
            Text(
                card.name,
                color = if (iridescent) Color(0xFF2A2438) else Color.White,
                fontWeight = FontWeight.Black, fontSize = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 92.dp, start = 24.dp, end = 24.dp)
                    .graphicsLayer {
                        alpha = nameIn.value * (1f - collect.value) * (1f - exit.value)
                        translationY = (1f - nameIn.value) * -14f
                    },
            )
        }

        // Confeti (premium) por detrás de la carta.
        if (pres.confetti > 0 && revealed) {
            ConfettiBurst(trigger = sparkTrigger, intensity = pres.confetti, modifier = Modifier.fillMaxSize())
        }

        // Sombra de CONTACTO (vende profundidad): elipse bajo la carta, no gira con ella.
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth(0.62f).height(26.dp)
                .graphicsLayer { translationY = with(density) { 250.dp.toPx() } * (0.55f + 0.45f * emerge.value) }
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            listOf(Color.Black.copy(alpha = 0.45f * emerge.value * (1f - collect.value)), Color.Transparent),
                        ),
                        cornerRadius = CornerRadius(60f, 60f),
                    )
                },
        )

        // ---- LA CARTA (protagonista) ----
        // Ángulo del giro leído en composición para elegir cara (dorso vs frente).
        // Ángulo del giro: empieza mostrando el DORSO (múltiplo impar de 180°) y cae a 0° (frente).
        val rotY = (1f - flip.value) * (pres.revealTurns * 360f) + (if (revealed) sway * flip.value else 0f)
        val showBack = (((rotY % 360f) + 360f) % 360f).let { it in 90f..270f }
        Box(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.90f).aspectRatio(0.72f)
                .graphicsLayer {
                    cameraDistance = 16f * this.density
                    // EMERGENCE: sube desde el interior con leve inclinación hacia atrás.
                    val e = emerge.value
                    translationY = (1f - e) * belowPx - flightPx * collect.value - exit.value * belowPx * 0.9f
                    translationX = with(density) { 96.dp.toPx() } * collect.value
                    rotationX = (1f - e) * 26f
                    rotationY = rotY
                    val base = (0.62f + 0.38f * e) * (1f + camera.value * pres.cameraPunch)
                    val s = base * (1f - 0.72f * collect.value) * (1f - 0.5f * exit.value)
                    scaleX = s; scaleY = s
                    alpha = (1f - collect.value * collect.value) * (1f - exit.value)
                },
            contentAlignment = Alignment.Center,
        ) {
            // Halo tras la carta (escala con rareza).
            Box(
                Modifier.fillMaxSize().drawBehind {
                    drawRoundRect(
                        brush = Brush.radialGradient(listOf(accent.copy(alpha = glow.value), Color.Transparent)),
                        cornerRadius = CornerRadius(40f, 40f),
                    )
                },
            )
            if (showBack) {
                // Cara trasera: mismas ESQUINAS que las cartas de la expansión (el arte del dorso ya
                // trae las esquinas redondeadas transparentes, como los frentes → no se recorta a dp).
                Image(
                    painter = painterResource(R.drawable.card_back_default),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(6.dp).graphicsLayer { rotationY = 180f },
                )
            } else {
                // Frente HD interactivo (holo con el dedo). En reposo se puede inclinar; tocar = avanzar.
                InteractiveHoloCard(
                    imageUrl = card.imageLarge ?: card.imageUrl,
                    setCode = card.setCode,
                    cardNumber = card.cardNumber,
                    rarity = card.rarity,
                    contentDescription = card.name,
                    modifier = Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(10.dp)),
                )
            }
            // Barrido de luz al cruzar el canto (premium): brillo cuando la carta está de perfil.
            if (pres.lightSweep && !revealed) {
                val edge = (1f - kotlin.math.abs(cos(Math.toRadians(rotY.toDouble())).toFloat())).coerceIn(0f, 1f)
                Box(
                    Modifier.fillMaxSize().graphicsLayer { alpha = edge * 0.9f }.drawBehind {
                        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White, Color.Transparent)))
                    },
                )
            }
            // Estallido de chispas del reveal (intensidad por rareza).
            if (revealed) {
                SparkleBurst(
                    trigger = sparkTrigger,
                    color = if (pres.tier >= RarityTier.SPECTACLE) accent else Color.White,
                    intensity = pres.sparkles,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // RECOMPENSA (aparece con la identidad, tras el reveal): NUEVA como momento, luego el dato de copias.
        if (showInfo) {
            val rewardIn = remember { Animatable(0f) }
            LaunchedEffect(Unit) { rewardIn.animateTo(1f, AnimationSprings.bouncy()) }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
                    .graphicsLayer {
                        alpha = rewardIn.value * (1f - collect.value) * (1f - exit.value)
                        translationY = (1f - rewardIn.value) * 16f
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (card.isNew) {
                    // Momento de recompensa "¡NUEVA!" (no un badge administrativo).
                    Box(
                        Modifier.graphicsLayer { scaleX = rewardIn.value; scaleY = rewardIn.value }
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF35C4E8), Color(0xFF5AD1F0))))
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                    ) { Text("¡NUEVA!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp) }
                    Spacer(6.dp)
                }
                // Dato de colección (secundario, sin competir con la recompensa).
                Text(
                    "${card.copiesOwned}/${card.cap} en la colección",
                    color = if (iridescent) Color(0x992A2438) else Color(0x99FFFFFF),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                )
            }
        }

        // RECIBIR TODO (salta solo la PRESENTACIÓN; el resultado no cambia). Discreto, no compite.
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 14.dp)
                .clip(RoundedCornerShape(50)).background(Color(0x55000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSkip)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) { Text("Recibir todo »", color = Color(0xCCFFFFFF), fontWeight = FontWeight.Bold, fontSize = 11.sp) }

        // Destello DIRIGIDO del reveal (IMPACT+): breve, no permanente.
        if (flash.value > 0.001f) {
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = flash.value }.drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.White, Color.White.copy(alpha = 0.2f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height * 0.45f),
                            radius = size.minDimension * 0.9f,
                        ),
                    )
                },
            )
        }
    }
}

/** Auto-avance en modo Play: reproduce cue y dispara la salida. */
private fun triggerAdvanceAuto(card: RevealedCard, feedback: BoosterFeedback, leave: () -> Unit) {
    if (!card.isNew) feedback.sfx(SfxCue.CARD_FLIP)
    leave()
}

// ============================ BEAT 14 · RESUMEN ============================

@Composable
private fun SummaryContent(cards: List<RevealedCard>, setLabel: String, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("¡SOBRE ABIERTO!", color = Color(0xFF2A2438), fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(top = 12.dp))
        Text(setLabel, color = Color(0x992A2438), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(cards) { _, c ->
                Box(contentAlignment = Alignment.TopEnd) {
                    CardFace(c, Modifier.fillMaxWidth().aspectRatio(0.72f))
                    if (c.isNew) {
                        Box(
                            Modifier.padding(4.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF35C4E8)).padding(horizontal = 5.dp, vertical = 1.dp),
                        ) { Text("NUEVA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 7.sp) }
                    }
                }
            }
        }
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = TcgColors.Gold, contentColor = TcgColors.Ink),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(8.dp),
        ) { Text("CONTINUAR", fontWeight = FontWeight.Black) }
    }
}

/** Cara de carta plana (para el resumen). */
@Composable
private fun CardFace(card: RevealedCard, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(TcgColors.Navy), contentAlignment = Alignment.Center) {
        if (card.imageUrl != null) {
            AsyncImage(
                model = card.imageUrl,
                contentDescription = card.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFFE53935)))
                Text(card.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

/** Pila abanicada de dorsos oficiales (cartas por revelar). */
@Composable
private fun BackStack(remaining: Int, modifier: Modifier = Modifier) {
    if (remaining <= 0) return
    val n = remaining.coerceAtMost(5)
    Box(modifier = modifier) {
        repeat(n) { i ->
            Image(
                painter = painterResource(R.drawable.card_back_default),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(0.72f)
                    .graphicsLayer { translationX = i * 6f }
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Composable
private fun Spacer(h: androidx.compose.ui.unit.Dp) {
    Box(Modifier.height(h))
}

internal fun vibratorOf(context: Context): Vibrator? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}.getOrNull()

internal fun rarityColor(r: Rarity): Color = when (r) {
    Rarity.COMMON -> Color(0xFF9AA4AE)
    Rarity.UNCOMMON -> Color(0xFF4FC3F7)
    Rarity.RARE -> Color(0xFF66BB6A)
    Rarity.RARE_HOLO -> Color(0xFF42A5F5)
    Rarity.DOUBLE_RARE -> Color(0xFFFFC107)
    Rarity.ULTRA_RARE -> Color(0xFFAB47BC)
    Rarity.ILLUSTRATION_RARE -> Color(0xFFFF7043)
    Rarity.SPECIAL_ILLUSTRATION_RARE -> Color(0xFFEC407A)
    Rarity.HYPER_RARE -> Color(0xFFFFD54F)
    Rarity.PROMO -> Color(0xFF90A4AE)
}
