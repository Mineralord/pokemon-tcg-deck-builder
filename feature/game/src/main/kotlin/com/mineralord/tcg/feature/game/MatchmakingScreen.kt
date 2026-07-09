package com.mineralord.tcg.feature.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sqrt

private val Navy = Color(0xFF16203A)
private val NavyDark = Color(0xFF0D1626)
private val HexLine = Color(0x14FFFFFF)
private val MmRed = Color(0xFFC41E1E)
private val MmRedDark = Color(0xFF6E0E0E)
private val Yellow = Color(0xFFF3C41A)
private val Ink = Color(0xFF3A2A08)
private val Muted = Color(0xFF8C96AC)

/**
 * Pantalla de matchmaking (réplica del arranque de TCG Live tras pulsar JUGAR):
 * cabecera INFORMAL/ESTÁNDAR, caja del mazo, "BUSCANDO UN EMPAREJAMIENTO" con
 * Pokéball giratoria, botón CANCELAR y consejo. Tras [matchDelayMs] llama a
 * [onMatched] (emparejamiento encontrado); [onCancel] vuelve al inicio.
 */
@Composable
fun MatchmakingScreen(
    deckName: String,
    onCancel: () -> Unit,
    onMatched: () -> Unit,
    modifier: Modifier = Modifier,
    matchDelayMs: Long = 3500L,
) {
    LaunchedEffect(Unit) {
        delay(matchDelayMs)
        onMatched()
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, NavyDark))),
    ) {
        HexBackground(Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.12f))

            Text("INFORMAL", color = Color.White, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 30.sp)
            Spacer(Modifier.height(8.dp))
            ModeChip("ESTÁNDAR")

            Spacer(Modifier.weight(0.22f))

            DeckBox(Modifier.size(150.dp, 190.dp))
            Spacer(Modifier.height(14.dp))
            Text(deckName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)

            Spacer(Modifier.weight(0.28f))

            Text(
                "BUSCANDO UN EMPAREJAMIENTO",
                color = Muted,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            SearchingBar()

            Spacer(Modifier.weight(0.18f))

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Yellow)
                    .clickable(onClick = onCancel)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("CANCELAR", color = Ink, fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = 1.sp)
            }

            Spacer(Modifier.weight(0.12f))

            Text(
                "Solo puedes jugar una carta de Partidario en cada turno, pero puedes jugar todas las cartas de Objeto que quieras.",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Chip rojo con extremos en punta (estilo banner) para el modo de juego. */
@Composable
private fun ModeChip(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(MmRed, MmRedDark)))
            .padding(horizontal = 32.dp, vertical = 7.dp),
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
    }
}

/** Caja del mazo (aprox. 3D) con portada roja, patrón hexagonal y emblema de fuego. */
@Composable
private fun DeckBox(modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val faceW = w * 0.78f
            val depth = w * 0.16f
            // Cara lateral (3D) a la derecha.
            val side = Path().apply {
                moveTo(faceW, h * 0.03f)
                lineTo(faceW + depth, h * 0.10f)
                lineTo(faceW + depth, h * 0.93f)
                lineTo(faceW, h * 0.97f)
                close()
            }
            drawPath(side, MmRedDark)
            // Cara frontal.
            val r = w * 0.04f
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFD23232), MmRed, Color(0xFF9A1414))),
                topLeft = Offset(0f, h * 0.03f),
                size = androidx.compose.ui.geometry.Size(faceW, h * 0.94f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            )
            // Marco claro.
            drawRoundRect(
                color = Color(0x66FFFFFF),
                topLeft = Offset(faceW * 0.05f, h * 0.06f),
                size = androidx.compose.ui.geometry.Size(faceW * 0.9f, h * 0.88f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                style = Stroke(width = w * 0.012f),
            )
            // Patrón hexagonal tenue en la portada.
            val hs = faceW * 0.12f
            var oy = h * 0.10f
            var row = 0
            while (oy < h * 0.9f) {
                var ox = if (row % 2 == 0) faceW * 0.12f else faceW * 0.30f
                while (ox < faceW * 0.9f) {
                    drawHex(Offset(ox, oy), hs * 0.5f, Color(0x1AFFFFFF))
                    ox += hs
                }
                oy += hs * 0.9f
                row++
            }
        }
        // Emblema de fuego (arriba-derecha).
        FireEmblem(Modifier.align(Alignment.TopEnd).padding(6.dp).size(46.dp))
        // Moneda de fuego (abajo-izquierda).
        FireEmblem(Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 18.dp).size(38.dp))
    }
}

/** Símbolo de energía de fuego: anillo blanco, disco rojo y llama negra. */
@Composable
private fun FireEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        drawCircle(Color.White, r, c)
        drawCircle(Color(0xFFE23B2E), r * 0.86f, c)
        // Llama (path teardrop con muescas).
        val flame = Path().apply {
            moveTo(c.x, c.y + r * 0.55f)
            cubicTo(c.x - r * 0.6f, c.y + r * 0.2f, c.x - r * 0.35f, c.y - r * 0.35f, c.x, c.y - r * 0.6f)
            cubicTo(c.x + r * 0.1f, c.y - r * 0.25f, c.x + r * 0.35f, c.y - r * 0.2f, c.x + r * 0.55f, c.y + r * 0.15f)
            cubicTo(c.x + r * 0.5f, c.y + r * 0.5f, c.x + r * 0.25f, c.y + r * 0.6f, c.x, c.y + r * 0.55f)
            close()
        }
        drawPath(flame, Color(0xFF1A1010))
    }
}

/** Línea divisoria con una Pokéball giratoria centrada. */
@Composable
private fun SearchingBar() {
    val transition = rememberInfiniteTransition(label = "pokeball")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Color(0x40FFFFFF)))
        Box(Modifier.size(34.dp).rotate(angle)) {
            Canvas(Modifier.fillMaxSize()) { drawPokeball() }
        }
        Box(Modifier.weight(1f).height(1.dp).background(Color(0x40FFFFFF)))
    }
}

private fun DrawScope.drawPokeball() {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f
    drawCircle(Color.White, r, c)
    // Mitad superior roja.
    drawArc(
        color = Color(0xFFE23B2E),
        startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(c.x - r, c.y - r),
        size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
    )
    // Banda + borde.
    drawCircle(Color(0xFF1A1A1A), r, c, style = Stroke(width = r * 0.14f))
    drawRect(Color(0xFF1A1A1A), topLeft = Offset(c.x - r, c.y - r * 0.12f), size = androidx.compose.ui.geometry.Size(r * 2, r * 0.24f))
    // Botón central.
    drawCircle(Color(0xFF1A1A1A), r * 0.3f, c)
    drawCircle(Color.White, r * 0.18f, c)
}

/** Fondo de panal hexagonal tenue (estilo TCG Live). */
@Composable
private fun HexBackground(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.width * 0.06f
        val hexH = sqrt(3f) * s
        var col = 0
        var cx = 0f
        while (cx <= size.width + s) {
            val yOff = if (col % 2 == 0) 0f else hexH / 2f
            var cy = yOff
            while (cy <= size.height + hexH) {
                drawHex(Offset(cx, cy), s, HexLine)
                cy += hexH
            }
            cx += s * 1.5f
            col++
        }
    }
}

/** Dibuja un hexágono (flat-top) con borde del color dado. */
private fun DrawScope.drawHex(center: Offset, s: Float, color: Color) {
    val hexH = sqrt(3f) * s
    val p = Path().apply {
        moveTo(center.x + s, center.y)
        lineTo(center.x + s / 2f, center.y + hexH / 2f)
        lineTo(center.x - s / 2f, center.y + hexH / 2f)
        lineTo(center.x - s, center.y)
        lineTo(center.x - s / 2f, center.y - hexH / 2f)
        lineTo(center.x + s / 2f, center.y - hexH / 2f)
        close()
    }
    drawPath(p, color, style = Stroke(width = 1.2f))
}
