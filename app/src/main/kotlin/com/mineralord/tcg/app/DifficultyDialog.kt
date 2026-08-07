package com.mineralord.tcg.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.engine.rules.Difficulty

/** Datos de presentación de cada dificultad (nombre, subtítulo y color de la Ball). */
private data class BallOption(
    val difficulty: Difficulty,
    val title: String,
    val subtitle: String,
    val top: Color,
)

private val Options = listOf(
    BallOption(Difficulty.POKEBALL, "POKÉ BALL", "Novata · juega a lo loco", Color(0xFFE23B2E)),
    BallOption(Difficulty.SUPERBALL, "SÚPER BALL", "Básica · ataca sin pensar", Color(0xFF3B7BE2)),
    BallOption(Difficulty.ULTRABALL, "ULTRA BALL", "Táctica · desarrolla y golpea fuerte", Color(0xFFF3C41A)),
    BallOption(Difficulty.MASTERBALL, "MASTER BALL", "Experta · busca el Noqueo letal", Color(0xFF7E3FF2)),
)

private val Scrim = Color(0xCC0A1020)
private val Panel = Color(0xFF16203A)
private val Muted = Color(0xFF8C96AC)

/**
 * Diálogo de selección de dificultad del rival IA (PvE), mostrado tras pulsar
 * JUGAR. Cada opción es una Poké Ball estilizada; al tocarla se confirma la
 * dificultad y comienza el emparejamiento.
 */
@Composable
fun DifficultyDialog(
    onPick: (Difficulty) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Scrim)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Panel)
                .clickable(enabled = false) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "ELIGE LA DIFICULTAD",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = 20.sp,
                letterSpacing = 0.5.sp,
            )
            Text(
                "Cuanto más avanzada la Ball, mejor juega el rival.",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Options.forEach { opt ->
                DifficultyRow(opt, onClick = { onPick(opt.difficulty) })
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun DifficultyRow(opt: BallOption, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(Color(0x18FFFFFF), Color(0x08FFFFFF))))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp)) {
            Canvas(Modifier.fillMaxSize()) { drawPokeball(opt.top) }
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(opt.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 0.5.sp)
            Text(opt.subtitle, color = Muted, fontSize = 12.sp)
        }
    }
}

private fun DrawScope.drawPokeball(top: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f
    drawCircle(Color.White, r, c)
    drawArc(
        color = top,
        startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(c.x - r, c.y - r),
        size = Size(r * 2, r * 2),
    )
    drawCircle(Color(0xFF1A1A1A), r, c, style = Stroke(width = r * 0.14f))
    drawRect(Color(0xFF1A1A1A), topLeft = Offset(c.x - r, c.y - r * 0.12f), size = Size(r * 2, r * 0.24f))
    drawCircle(Color(0xFF1A1A1A), r * 0.3f, c)
    drawCircle(Color.White, r * 0.18f, c)
}
