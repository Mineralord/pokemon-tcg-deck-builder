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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val Ink = Color(0xFF23262B)
private val SheetBg = Color(0xFFF3F1EC)

/**
 * Overlay del lanzamiento de moneda inicial sobre el tablero vacío (réplica de TCG
 * Live): elegir CARA/CRUZ → moneda girando → resultado (moneda LIVE + banner +
 * mensaje de quién empieza).
 */
@Composable
fun CoinFlipOverlay(
    state: CoinFlipUiState,
    onCall: (Boolean) -> Unit,
    onChooseFirst: (Boolean) -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        when (state.phase) {
            CoinPhase.CHOOSE_ORDER -> {
                // El jugador ganó el volado: elige quién toma el primer turno (regla oficial).
                Box(Modifier.align(Alignment.Center).size(130.dp)) {
                    Canvas(Modifier.fillMaxSize()) { drawLiveCoin() }
                }
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .background(SheetBg)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "HAS GANADO EL LANZAMIENTO. ¿QUIÉN EMPIEZA?",
                        color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp,
                        textAlign = TextAlign.Center, lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    CoinChoice("EMPIEZO YO") { onChooseFirst(true) }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22000000)))
                    Spacer(Modifier.height(6.dp))
                    CoinChoice("EMPIEZA EL RIVAL") { onChooseFirst(false) }
                    Spacer(Modifier.height(6.dp))
                }
            }

            CoinPhase.CHOOSING -> {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .background(SheetBg)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "ELIGE PARA EL LANZAMIENTO INICIAL DE MONEDA: CARA O CRUZ.",
                        color = Ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    CoinChoice("CARA") { onCall(true) }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22000000)))
                    Spacer(Modifier.height(6.dp))
                    CoinChoice("CRUZ") { onCall(false) }
                    Spacer(Modifier.height(6.dp))
                }
            }

            CoinPhase.SPINNING -> {
                val t = rememberInfiniteTransition(label = "coin")
                val a by t.animateFloat(
                    0f, 360f,
                    infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Restart),
                    label = "flip",
                )
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(120.dp)
                        .graphicsLayer { rotationX = a },
                ) { Canvas(Modifier.fillMaxSize()) { drawCoin(a) } }
                // Mientras giramos, si hay un mensaje (p. ej. "Esperando al rival…") se
                // muestra bajo la moneda.
                state.message?.let {
                    Toast(it, Modifier.align(Alignment.Center).offset(y = 96.dp))
                }
            }

            CoinPhase.RESULT -> {
                Box(Modifier.align(Alignment.Center).size(130.dp)) {
                    Canvas(Modifier.fillMaxSize()) { drawLiveCoin() }
                }
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .offset(y = 96.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SheetBg)
                        .padding(horizontal = 44.dp, vertical = 8.dp),
                ) {
                    Text(
                        if (state.result == true) "CARA" else "CRUZ",
                        color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp,
                    )
                }
                state.message?.let {
                    Toast(it, Modifier.align(Alignment.Center).offset(y = 168.dp))
                }
            }
        }
    }
}

@Composable
private fun CoinChoice(label: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp)
    }
}

/** Píldora blanca de aviso centrada (estilo TCG Live). */
@Composable
private fun Toast(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF2F3F1EC))
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Text(text, color = Ink, fontWeight = FontWeight.Medium, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

/** Moneda roja/negra girando (durante el volado). */
private fun DrawScope.drawCoin(angle: Float) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f
    // "Aplastado" al girar sobre el eje X para dar sensación 3D.
    val squash = abs(kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat())
    val rr = r * (0.25f + 0.75f * squash)
    drawCircleFace(c, r, rr)
}

private fun DrawScope.drawCircleFace(c: Offset, r: Float, ry: Float) {
    // Óvalo base (metal oscuro).
    drawOval(
        Color(0xFF2A2320),
        topLeft = Offset(c.x - r, c.y - ry),
        size = Size(r * 2, ry * 2),
    )
    // Disco rojo interior.
    drawOval(
        Brush.verticalGradient(listOf(Color(0xFFE23B2E), Color(0xFF9A1414))),
        topLeft = Offset(c.x - r * 0.82f, c.y - ry * 0.82f),
        size = Size(r * 1.64f, ry * 1.64f),
    )
    // Silueta central.
    drawOval(
        Color(0xFF1A1010),
        topLeft = Offset(c.x - r * 0.30f, c.y - ry * 0.55f),
        size = Size(r * 0.6f, ry * 1.1f),
    )
}

/** Moneda plateada "Pokémon TCG LIVE" del resultado. */
private fun DrawScope.drawLiveCoin() {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFEDEFF3), Color(0xFFB9BFC9))), r, c)
    drawCircle(Color(0xFF8B93A1), r, c, style = Stroke(width = r * 0.08f))
    drawCircle(Color(0xFF5B6270), r * 0.7f, c, style = Stroke(width = r * 0.03f))
    // Franja azul central (evoca el logo LIVE).
    drawRect(
        Color(0xFF2E5FA8),
        topLeft = Offset(c.x - r * 0.62f, c.y - r * 0.18f),
        size = Size(r * 1.24f, r * 0.36f),
    )
}
