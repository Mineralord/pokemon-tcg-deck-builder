package com.mineralord.tcg.feature.packs

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.delay

/**
 * Overlay de apertura de sobre: revela las cartas una a una con volteo 3D,
 * escala y brillo proporcional a la rareza. El timing se inspiró en la cadencia
 * del reveal del vídeo de referencia (§0). Toque = saltar al revelado completo.
 *
 * Accesibilidad: si el usuario tiene las animaciones del sistema desactivadas
 * (animator_duration_scale == 0), se revelan todas de golpe sin transiciones.
 */
@Composable
fun PackRevealOverlay(
    cards: List<RevealedCard>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }

    var revealedCount by remember { mutableIntStateOf(if (reducedMotion) cards.size else 0) }
    val allRevealed = revealedCount >= cards.size

    // Auto-avance escalonado: cartas de mayor rareza esperan un poco más (suspense).
    LaunchedEffect(cards) {
        if (reducedMotion) return@LaunchedEffect
        for (i in cards.indices) {
            val pause = if (cards[i].rarity.ordinal >= Rarity.DOUBLE_RARE.ordinal) 520L else 230L
            delay(pause)
            revealedCount = i + 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(TcgColors.RedDark, TcgColors.Navy)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { revealedCount = cards.size },   // toque = revelar todo
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (allRevealed) "¡SOBRE ABIERTO!" else "ABRIENDO SOBRE…",
                color = TcgColors.Parchment,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            if (!allRevealed) {
                Text(
                    "Toca para revelar todo",
                    color = TcgColors.Parchment.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp),
            ) {
                itemsIndexed(cards) { index, card ->
                    RevealCard(card = card, faceUp = index < revealedCount)
                }
            }

            Button(
                onClick = onDismiss,
                enabled = allRevealed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TcgColors.Gold,
                    contentColor = TcgColors.Ink,
                    disabledContainerColor = TcgColors.Gold.copy(alpha = 0.35f),
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("CONTINUAR", fontWeight = FontWeight.Black)
            }
        }
    }
}

/** Una carta que voltea de dorso a cara cuando [faceUp] pasa a true. */
@Composable
private fun RevealCard(card: RevealedCard, faceUp: Boolean) {
    val rarity = card.rarity
    val angle by animateFloatAsState(
        targetValue = if (faceUp) 0f else 180f,
        animationSpec = tween(durationMillis = 420),
        label = "flip",
    )
    val showFront = angle <= 90f

    // Brillo pulsante para rarezas altas, solo cuando ya está cara arriba.
    val glow = rarity.ordinal >= Rarity.DOUBLE_RARE.ordinal && faceUp
    val infinite = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha",
    )
    val accent = rarityColor(rarity)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .graphicsLayer {
                rotationY = angle
                cameraDistance = 14f * density
            }
            .drawBehind {
                if (glow) {
                    drawRoundRect(
                        color = accent.copy(alpha = glowAlpha),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f),
                    )
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (showFront) TcgColors.Parchment else TcgColors.Navy),
        contentAlignment = Alignment.Center,
    ) {
        if (showFront) {
            // Cara: el contenido se "des-voltea" para no salir en espejo.
            Column(
                modifier = Modifier
                    .graphicsLayer { rotationY = 180f }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.25f)),
                )
                Text(
                    text = card.name,
                    color = TcgColors.Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = rarityLabel(rarity),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
            }
        } else {
            // Dorso estilizado (sin logos de terceros).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(listOf(TcgColors.Red, TcgColors.RedDark)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("TCG", color = TcgColors.Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
    }
}

private fun rarityColor(r: Rarity): Color = when (r) {
    Rarity.COMMON -> Color(0xFF7A7A7A)
    Rarity.UNCOMMON -> Color(0xFF3AA6E8)
    Rarity.RARE -> Color(0xFF2E7D32)
    Rarity.RARE_HOLO -> Color(0xFF1565C0)
    Rarity.DOUBLE_RARE -> Color(0xFFB8842F)
    Rarity.ULTRA_RARE -> Color(0xFF8E24AA)
    Rarity.ILLUSTRATION_RARE -> Color(0xFFE65100)
    Rarity.SPECIAL_ILLUSTRATION_RARE -> Color(0xFFC2185B)
    Rarity.HYPER_RARE -> Color(0xFFD4AF37)
    Rarity.PROMO -> Color(0xFF455A64)
}

private fun rarityLabel(r: Rarity): String = when (r) {
    Rarity.COMMON -> "Común"
    Rarity.UNCOMMON -> "Poco común"
    Rarity.RARE -> "Rara"
    Rarity.RARE_HOLO -> "Rara Holo"
    Rarity.DOUBLE_RARE -> "Doble Rara (ex)"
    Rarity.ULTRA_RARE -> "Ultra Rara"
    Rarity.ILLUSTRATION_RARE -> "Ilustración Rara"
    Rarity.SPECIAL_ILLUSTRATION_RARE -> "Ilustración Especial"
    Rarity.HYPER_RARE -> "Hyper Rara"
    Rarity.PROMO -> "Promo"
}
