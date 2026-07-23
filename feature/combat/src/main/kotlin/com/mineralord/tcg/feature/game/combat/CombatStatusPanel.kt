package com.mineralord.tcg.feature.game.combat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.mineralord.tcg.core.designsystem.motion.motionPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * Columna derecha 1:1 con TCG Live (Combate 1). Réplica fiel de las dos PESTAÑAS DE ESTADO
 * (rival arriba, jugador abajo, espejo vertical) y del BOTÓN DE REGISTRO de batalla.
 *
 * Medidas y colores tomados de referencias_live/combate1 (frame at_0300, 1080×2400):
 *   pestaña docked al borde (fx 0.898→1.0), esquinas derechas redondeadas y la esquina
 *   INTERIOR del contador achaflanada; cuerpo #292929; badge premios rojo #CE3B39 (rival)
 *   / azul #3977BE (jugador); barra de turno dorada #FDFE76; borde del log azul #3F7ABB.
 */

private val PanelBody = Color(0xFF292929)
private val PanelBodyTop = Color(0xFF34343A)
private val PrizeRed = Color(0xFFCE3B39)
private val PrizeBlue = Color(0xFF3977BE)
private val TurnGold = Color(0xFFFDFE76)
private val ChevronGrey = Color(0xFF6E6E70)
private val LogBlue = Color(0xFF3F7ABB)

/**
 * Pestaña de estado de un jugador. [isOpponent] = panel del RIVAL (badge abajo, chaflán
 * abajo-izq); si es del jugador va en ESPEJO (badge arriba, chaflán arriba-izq). [active]
 * enciende la barra de turno dorada del borde exterior. [prizeAccent] rojo/azul.
 *
 * ⚠️ Los chevrones ▲▼ se replican VISUALMENTE (gris apagado) pero su función no se
 * determinó con certeza en los frames: no llevan comportamiento cableado.
 */
@Composable
fun PlayerStatusPanel(
    prizes: Int,
    prizeAccent: Color,
    isOpponent: Boolean,
    active: Boolean,
    timerText: String,
    modifier: Modifier = Modifier,
) {
    val shape = remember(isOpponent) { StatusPanelShape(isOpponent) }
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PanelBodyTop, PanelBody))),
    ) {
        // Contenido apilado. El orden se invierte según el lado (el badge SIEMPRE hacia el
        // centro del tablero; la barra de turno SIEMPRE hacia el borde exterior de pantalla).
        Column(Modifier.fillMaxSize()) {
            if (isOpponent) {
                TurnBar(active)
                ChevronPair(Modifier.weight(1f))
                TimerText(timerText, Modifier.weight(1.05f))
                PrizeBadge(prizes, prizeAccent, Modifier.weight(2.1f))
            } else {
                PrizeBadge(prizes, prizeAccent, Modifier.weight(2.1f))
                TimerText(timerText, Modifier.weight(1.05f))
                ChevronPair(Modifier.weight(1f))
                TurnBar(active)
            }
        }
    }
}

/** Barra fina del borde exterior: dorada brillante cuando es el turno de ese jugador. */
@Composable
private fun TurnBar(active: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                if (active) Brush.horizontalGradient(listOf(Color(0xFFFFF6A0), TurnGold, Color(0xFFE7B322)))
                else Brush.horizontalGradient(listOf(Color(0xFF3A3A2A), Color(0xFF2E2E22))),
            ),
    )
}

/** Par de chevrones ▲▼ grises (réplica visual; función no confirmada → sin acción). */
@Composable
private fun ChevronPair(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("▲", color = ChevronGrey, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text("▼", color = ChevronGrey, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

/** Temporizador mm:ss, texto blanco (fuente pesada como en TCG Live). */
@Composable
private fun TimerText(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text, color = Color(0xFFF2F2F2), fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

/** Contador de premios: bloque coloreado (rojo rival / azul jugador) con el número grande. */
@Composable
private fun PrizeBadge(prizes: Int, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(lighten(accent), accent, darken(accent)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$prizes",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

/**
 * Botón de REGISTRO de batalla: cuadrado redondeado oscuro con borde azul e icono de
 * LISTA + LUPA (100% dibujado en Canvas). [onClick] abre el registro.
 */
@Composable
fun BattleLogButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF25272B))
            .border(2.dp, LogBlue, RoundedCornerShape(12.dp))
            .motionPress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(9.dp)) {
            val s = size.minDimension
            val ink = Color.White
            val stroke = s * 0.055f
            // Hoja/documento (rectángulo redondeado), desplazado arriba-izquierda.
            val docW = s * 0.62f
            val docH = s * 0.66f
            val ox = s * 0.06f
            val oy = s * 0.05f
            drawRoundRectStroke(ox, oy, docW, docH, s * 0.10f, ink, stroke)
            // Líneas de lista con su viñeta.
            val bx = ox + docW * 0.20f
            val lx0 = ox + docW * 0.38f
            val lx1 = ox + docW * 0.82f
            for (i in 0..2) {
                val ly = oy + docH * (0.30f + i * 0.22f)
                drawCircle(ink, radius = stroke * 0.9f, center = Offset(bx, ly))
                drawLine(ink, Offset(lx0, ly), Offset(lx1, ly), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            // Lupa abajo-derecha (círculo + mango).
            val mr = s * 0.16f
            val mc = Offset(s * 0.70f, s * 0.72f)
            drawCircle(ink, radius = mr, center = mc, style = Stroke(stroke))
            val h0 = Offset(mc.x + mr * 0.72f, mc.y + mr * 0.72f)
            val h1 = Offset(mc.x + mr * 1.5f, mc.y + mr * 1.5f)
            drawLine(ink, h0, h1, strokeWidth = stroke * 1.3f, cap = StrokeCap.Round)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectStroke(
    x: Float, y: Float, w: Float, h: Float, r: Float, color: Color, stroke: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        style = Stroke(stroke),
    )
}

// --- Forma de la pestaña: esquinas derechas redondeadas + chaflán en la esquina interior --
private class StatusPanelShape(private val isOpponent: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val r = w * 0.30f          // radio de esquinas derechas
        val c = w * 0.34f          // tamaño del chaflán interior
        val path = Path().apply {
            if (isOpponent) {
                // Chaflán en la esquina INFERIOR-izquierda (badge abajo).
                moveTo(0f, 0f)
                lineTo(w - r, 0f)
                quadraticBezierTo(w, 0f, w, r)
                lineTo(w, h - r)
                quadraticBezierTo(w, h, w - r, h)
                lineTo(c, h)
                lineTo(0f, h - c)
                close()
            } else {
                // Chaflán en la esquina SUPERIOR-izquierda (badge arriba).
                moveTo(0f, c)
                lineTo(c, 0f)
                lineTo(w - r, 0f)
                quadraticBezierTo(w, 0f, w, r)
                lineTo(w, h - r)
                quadraticBezierTo(w, h, w - r, h)
                lineTo(0f, h)
                close()
            }
        }
        return Outline.Generic(path)
    }
}

private fun lighten(c: Color): Color = androidx.compose.ui.graphics.lerp(c, Color.White, 0.18f)
private fun darken(c: Color): Color = androidx.compose.ui.graphics.lerp(c, Color.Black, 0.30f)
