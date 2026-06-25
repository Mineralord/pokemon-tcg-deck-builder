package com.mineralord.tcg.feature.packs

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.delay

private enum class Phase { PRESENT, OPENING, REVEAL, SUMMARY }

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

    var phase by remember { mutableStateOf(if (reducedMotion) Phase.SUMMARY else Phase.PRESENT) }
    var index by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0E16), Color(0xFF141B2A)))),
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            Phase.PRESENT -> PresentContent(setLabel, remainingToday, onOpen = { phase = Phase.OPENING }, onSkip = { phase = Phase.SUMMARY })
            Phase.OPENING -> {
                LaunchedEffect(Unit) { audio.play(Sfx.WHOOSH); delay(380); index = 0; phase = Phase.REVEAL }
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
private fun PresentContent(setLabel: String, remaining: Int, onOpen: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { onOpen() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Sobre de mejora de", color = Color(0xCCFFFFFF), fontSize = 14.sp)
        Text(setLabel, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(20.dp)
        BoosterPack(modifier = Modifier.width(220.dp).aspectRatio(0.62f))
        Spacer(20.dp)
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 28.dp, vertical = 10.dp),
        ) { Text("Sobres restantes: $remaining", color = TcgColors.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Spacer(8.dp)
        Text("Toca el sobre para abrir", color = Color(0x99FFFFFF), fontSize = 12.sp)
        Spacer(16.dp)
        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(containerColor = TcgColors.Red, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
        ) { Text("RECIBIR TODO", fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun OpeningContent() {
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1.25f, tween(300))
    }
    LaunchedEffect(Unit) {
        delay(140); alpha.animateTo(0f, tween(240))
    }
    BoosterPack(
        modifier = Modifier.width(220.dp).aspectRatio(0.62f).graphicsLayer {
            scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value
        },
        floating = false,
    )
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
    val rare = card.rarity.ordinal >= Rarity.DOUBLE_RARE.ordinal
    val accent = rarityColor(card.rarity)

    // Entrada de la carta (escala con rebote) + sonidos/háptico por carta.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(index) {
        entrance.snapTo(0.6f)
        audio.play(Sfx.WHOOSH)
        if (rare) {
            audio.play(Sfx.RARE)
            vibrator?.let { it.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)) }
        } else {
            audio.play(Sfx.SPARKLE)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        entrance.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
    }

    Box(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { onAdvance() },
    ) {
        // Pila de dorsos a la izquierda (cartas restantes).
        BackStack(
            remaining = cards.size - index - 1,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp).width(70.dp).fillMaxHeight(0.5f),
        )

        // Nombre de la carta.
        Text(
            card.name,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 110.dp, start = 24.dp, end = 24.dp),
        )

        // Carta central + chispas + glow.
        Box(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.62f).aspectRatio(0.72f).graphicsLayer {
                scaleX = entrance.value; scaleY = entrance.value
            },
            contentAlignment = Alignment.Center,
        ) {
            // Glow detrás.
            Box(
                Modifier.fillMaxSize().drawBehind {
                    drawRoundRect(
                        brush = Brush.radialGradient(listOf(accent.copy(alpha = if (rare) 0.85f else 0.5f), Color.Transparent)),
                        cornerRadius = CornerRadius(40f, 40f),
                    )
                },
            )
            CardFace(card, Modifier.fillMaxSize().padding(8.dp))
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

        // Contador "COPIAS EN LA COLECCIÓN n/4".
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("COPIAS EN LA COLECCIÓN", color = Color(0xAAFFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(4.dp)
            val pop = remember(index) { Animatable(0.5f) }
            LaunchedEffect(index) { pop.snapTo(0.5f); pop.animateTo(1f, spring(dampingRatio = 0.45f)) }
            Box(
                Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                    .clip(RoundedCornerShape(50)).background(Color(0x33FFFFFF)).padding(horizontal = 18.dp, vertical = 4.dp),
            ) { Text("${card.copiesOwned}/4", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
        }

        // RECIBIR TODO.
        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(containerColor = TcgColors.Red, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Text("RECIBIR TODO", fontWeight = FontWeight.Black, fontSize = 12.sp) }
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

/** Pila de dorsos "TCG" abanicada (cartas que quedan por revelar). */
@Composable
private fun BackStack(remaining: Int, modifier: Modifier = Modifier) {
    if (remaining <= 0) return
    val n = remaining.coerceAtMost(5)
    Box(modifier = modifier) {
        repeat(n) { i ->
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(0.72f)
                    .graphicsLayer { translationX = i * 6f }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(TcgColors.Red, TcgColors.RedDark))),
                contentAlignment = Alignment.Center,
            ) {
                if (i == n - 1) Text("TCG", color = TcgColors.Gold, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
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
