package com.mineralord.tcg.feature.packs

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.mineralord.tcg.core.designsystem.motion.AnimationSprings
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.CardDetailDialog
import com.mineralord.tcg.core.designsystem.HoloCardImage
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.Rarity
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Phase { OPENING, REVEAL, SUMMARY }

/**
 * Apertura de sobre estilo TCG Live: presentación del sobre → apertura → revelado
 * carta a carta (pila de dorsos, chispas, "NUEVA", contador) → resumen. SFX
 * sintetizados + háptico. Toque = avanzar; "RECIBIR TODO" = saltar al resumen.
 */
@Composable
fun PackOpeningOverlay(
    cards: List<RevealedCard>,
    setLabel: String,
    remainingToday: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val audio = remember { PackAudio() }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { vibratorOf(context) }

    // Arranca directo en OPENING: la presentación del sobre y el gesto de "deslizar para abrir"
    // viven ahora en PackStage (flujo TCG Pocket). Con movimiento reducido va directo al resumen.
    var phase by remember { mutableStateOf(if (reducedMotion) Phase.SUMMARY else Phase.OPENING) }
    var index by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0E16), Color(0xFF141B2A)))),
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            Phase.OPENING -> {
                LaunchedEffect(Unit) { audio.play(Sfx.WHOOSH); delay(440); index = 0; phase = Phase.REVEAL }
                OpeningContent()
            }
            Phase.REVEAL -> RevealContent(
                cards = cards,
                index = index,
                audio = audio,
                haptics = haptics,
                vibrator = vibrator,
                onAdvance = { if (index >= cards.size - 1) phase = Phase.SUMMARY else index++ },
                onSkip = { phase = Phase.SUMMARY },
            )
            Phase.SUMMARY -> SummaryContent(cards, onDismiss)
        }
    }
}

@Composable
private fun OpeningContent() {
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }   // destello radial expandiéndose
    val seam = remember { Animatable(0f) }     // costura de luz que desgarra el sobre
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1.32f, tween(340)) }
        launch { seam.animateTo(1f, tween(300)) }
        launch { delay(110); flash.animateTo(1f, tween(150)); flash.animateTo(0f, tween(280)) }
        launch { delay(150); alpha.animateTo(0f, tween(230)) }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Sobre + costura de luz brillante que se abre por el centro.
        Box(
            modifier = Modifier.width(220.dp).aspectRatio(0.62f).graphicsLayer {
                scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value
            },
            contentAlignment = Alignment.Center,
        ) {
            BoosterPack(modifier = Modifier.fillMaxSize(), floating = false)
            Box(
                Modifier.fillMaxSize().drawBehind {
                    val s = seam.value
                    if (s > 0f) {
                        val h = size.height * 0.02f * (1f + 7f * s)
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.Transparent),
                            ),
                            topLeft = Offset(0f, size.height / 2f - h / 2f),
                            size = Size(size.width, h),
                        )
                    }
                },
            )
        }
        // Destello radial + rayos que estallan al rasgarse el envoltorio.
        Box(
            Modifier.fillMaxSize().drawBehind {
                val f = flash.value
                if (f <= 0f) return@drawBehind
                val r = size.minDimension * (0.25f + 0.85f * f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.9f * f),
                            Color(0xFFBFE3FF).copy(alpha = 0.5f * f),
                            Color.Transparent,
                        ),
                        center = center, radius = r,
                    ),
                    radius = r, center = center,
                )
                // Rayos radiales.
                val rays = 12
                for (i in 0 until rays) {
                    val a = (i.toFloat() / rays) * 6.2832f
                    val inner = r * 0.2f
                    val outer = r * (0.9f + 0.3f * f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.55f * f),
                        start = Offset(center.x + cos(a) * inner, center.y + sin(a) * inner),
                        end = Offset(center.x + cos(a) * outer, center.y + sin(a) * outer),
                        strokeWidth = 4f,
                    )
                }
            },
        )
    }
}

@Composable
private fun RevealContent(
    cards: List<RevealedCard>,
    index: Int,
    audio: PackAudio,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    vibrator: Vibrator?,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
) {
    val card = cards[index]
    // Rareza doble-rara o superior: giro 3D + brillo holográfico + confeti (como el Venusaur ex).
    val rare = card.rarity.ordinal >= Rarity.DOUBLE_RARE.ordinal
    val accent = rarityColor(card.rarity)
    val density = LocalDensity.current

    // Sub-fase de la carta: entrada (giro/escala) → asentada → (si es NUEVA) cinemática "a la colección".
    var collecting by remember { mutableStateOf(false) }
    val entrance = remember { Animatable(0.6f) }   // escala para cartas normales
    val flip = remember { Animatable(0f) }          // 0=canto → 1=de frente (giro 3D de raras)
    val bgLight = remember { Animatable(0f) }        // fondo iridiscente claro en raras
    val collect = remember { Animatable(0f) }        // vuelo hacia la colección (cartas nuevas)

    LaunchedEffect(index) {
        collecting = false
        collect.snapTo(0f)
        bgLight.snapTo(0f)
        audio.play(Sfx.WHOOSH)
        if (rare) {
            entrance.snapTo(1f)
            flip.snapTo(0f)
            audio.play(Sfx.RARE)
            vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            launch { bgLight.animateTo(1f, tween(650)) }
            // Varias vueltas que desaceleran hasta quedar de frente.
            flip.animateTo(1f, tween(1250, easing = com.mineralord.tcg.core.designsystem.motion.AnimationCurves.EmphasizedDecelerate))
        } else {
            flip.snapTo(1f)
            entrance.snapTo(0.6f)
            audio.play(Sfx.SPARKLE)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            entrance.animateTo(1f, AnimationSprings.bouncy())
        }
    }

    // Vaivén sutil continuo en raras: mantiene vivo el brillo holográfico tras asentarse.
    val infinite = rememberInfiniteTransition(label = "raroSway")
    val sway by infinite.animateFloat(
        initialValue = -7f, targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "swayY",
    )

    // Cinemática "añadida a tu colección": la carta se encoge y vuela hacia el contador de la Cartadex.
    LaunchedEffect(collecting) {
        if (collecting) {
            audio.play(Sfx.SPARKLE)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            collect.animateTo(1f, tween(1050, easing = com.mineralord.tcg.core.designsystem.motion.AnimationCurves.EmphasizedAccelerate))
            onAdvance()
        }
    }

    // Toque = avanzar. Si la carta es NUEVA, primero reproduce la cinemática de colección.
    fun advance() {
        if (collecting) return
        if (card.isNew) collecting = true else onAdvance()
    }

    // Carta que se está viendo a detalle (mismo visor de la colección: holo + dedo). Null = revelado.
    var inspect by remember { mutableStateOf<RevealedCard?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { advance() },
    ) {
        // Fondo iridiscente claro que asoma en revelados raros (estilo TCG Pocket).
        if (rare) {
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = bgLight.value }.drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color(0xFFDCE7F5), Color(0xFFEDE3F5), Color(0xFFDCEFF0)),
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height * 0.42f),
                            radius = size.minDimension * 0.75f,
                        ),
                    )
                },
            )
        }

        // Pila de dorsos a la izquierda (cartas restantes).
        BackStack(
            remaining = cards.size - index - 1,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp).width(70.dp).fillMaxHeight(0.5f),
        )

        // Nombre de la carta (se atenúa durante la cinemática de colección).
        Text(
            card.name,
            color = if (rare) Color(0xFF2A2438) else Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 110.dp, start = 24.dp, end = 24.dp)
                .graphicsLayer { alpha = 1f - collect.value },
        )

        // Confeti para raras (por detrás de la carta, estalla al revelar).
        if (rare) {
            ConfettiBurst(trigger = index, intensity = 42, modifier = Modifier.fillMaxSize())
        }

        // Carta central. Giro 3D en raras, escala con rebote en comunes; luego "vuela" a la colección.
        val flightPx = with(density) { 320.dp.toPx() }
        // Ángulo del giro leído en composición: decide qué cara se ve (frente vs. dorso oficial).
        val rotY = (1f - flip.value) * 540f + (if (rare) sway * flip.value else 0f)
        val showingBack = rare && (((rotY % 360f) + 360f) % 360f).let { it > 90f && it < 270f }
        Box(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.80f).aspectRatio(0.72f).graphicsLayer {
                cameraDistance = 16f * this.density
                rotationY = rotY
                val base = if (rare) 1f else entrance.value
                val s = base * (1f - 0.72f * collect.value)
                scaleX = s; scaleY = s
                translationY = -flightPx * collect.value
                translationX = with(density) { 90.dp.toPx() } * collect.value
                alpha = 1f - (collect.value * collect.value)
            }.clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
            ) { if (!collecting) inspect = card },
            contentAlignment = Alignment.Center,
        ) {
            if (showingBack) {
                // Cara trasera: dorso oficial Pokémon (contra-rotado 180° para no verse en espejo).
                Image(
                    painter = painterResource(R.drawable.card_back_default),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                        .graphicsLayer { rotationY = 180f }
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                // Glow detrás.
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(listOf(accent.copy(alpha = if (rare) 0.85f else 0.5f), Color.Transparent)),
                            cornerRadius = CornerRadius(40f, 40f),
                        )
                    },
                )
                // Raras: holo REAL (mismo motor que la colección); comunes: arte plano.
                if (rare) {
                    HoloCardImage(
                        imageUrl = card.imageLarge ?: card.imageUrl,
                        setCode = card.setCode,
                        cardNumber = card.cardNumber,
                        rarity = card.rarity,
                        contentDescription = card.name,
                        intensity = 1.15f,
                        modifier = Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    CardFace(card, Modifier.fillMaxSize().padding(8.dp))
                }
                // NUEVA badge.
                if (card.isNew) {
                    Box(
                        Modifier.align(Alignment.TopCenter).padding(top = 2.dp).clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF35C4E8)).padding(horizontal = 10.dp, vertical = 3.dp),
                    ) { Text("NUEVA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp) }
                }
                SparkleBurst(
                    trigger = index,
                    color = if (rare) accent else Color.White,
                    intensity = if (rare) 26 else 14,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Cartel de la cinemática de colección.
        if (card.isNew) {
            Text(
                "¡Añadida a tu colección!",
                color = if (rare) Color(0xFF2A2438) else Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(top = 260.dp)
                    .graphicsLayer { alpha = collect.value },
            )
        }

        // Contador "COPIAS EN LA COLECCIÓN n/4".
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
                .graphicsLayer { alpha = 1f - collect.value },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("COPIAS EN LA COLECCIÓN", color = if (rare) Color(0xCC2A2438) else Color(0xAAFFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(4.dp)
            val pop = remember(index) { Animatable(0.5f) }
            LaunchedEffect(index) { pop.snapTo(0.5f); pop.animateTo(1f, AnimationSprings.bouncy()) }
            Box(
                Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                    .clip(RoundedCornerShape(50)).background(if (rare) Color(0x332A2438) else Color(0x33FFFFFF)).padding(horizontal = 18.dp, vertical = 4.dp),
            ) { Text("${card.copiesOwned}/${card.cap}", color = if (rare) Color(0xFF2A2438) else Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
        }

        // RECIBIR TODO.
        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(containerColor = TcgColors.Red, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                .graphicsLayer { alpha = 1f - collect.value },
        ) { Text("RECIBIR TODO", fontWeight = FontWeight.Black, fontSize = 12.sp) }
    }

    // Visor a detalle = EL MISMO de la colección (holo + inclinación/zoom con el dedo).
    inspect?.let { c ->
        CardDetailDialog(
            imageUrl = c.imageLarge ?: c.imageUrl ?: "",
            contentDescription = c.name,
            onDismiss = { inspect = null },
            rarity = c.rarity,
            cardNumber = c.cardNumber,
            setCode = c.setCode,
            copiesOwned = c.copiesOwned,
            copiesCap = c.cap,
        )
    }
}

@Composable
private fun SummaryContent(cards: List<RevealedCard>, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("¡SOBRE ABIERTO!", color = TcgColors.Parchment, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(vertical = 12.dp))
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

/** Cara de carta: arte ES o, si no hay versión española, nombre + punto rojo. */
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

/** Pila abanicada de dorsos oficiales Pokémon (cartas que quedan por revelar). */
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

private fun vibratorOf(context: Context): Vibrator? = runCatching {
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
