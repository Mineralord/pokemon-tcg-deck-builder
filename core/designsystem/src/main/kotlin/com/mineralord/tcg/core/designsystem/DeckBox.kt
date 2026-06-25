package com.mineralord.tcg.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.mineralord.tcg.engine.model.EnergyType

/**
 * "Caja" de baraja estilizada con arte propio (sin assets de terceros): un cubo
 * con cara frontal tintada por el tipo dominante, lomo lateral más oscuro, un
 * patrón hexagonal sutil y el [TypeEmblem] del tipo. Replica la silueta de las
 * cajas del gestor de TCG Live sin copiar su arte.
 */
@Composable
fun DeckBox(type: EnergyType, modifier: Modifier = Modifier) {
    val accent = typeColor(type)
    val dark = lerp(accent, Color.Black, 0.5f)
    val mid = lerp(accent, Color.Black, 0.22f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(mid, dark))),
    ) {
        // Cara frontal con degradado del acento + patrón hex.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp)
                .background(Brush.verticalGradient(listOf(lerp(accent, Color.White, 0.12f), mid))),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension * 0.10f
                val light = Color.White.copy(alpha = 0.06f)
                var row = 0
                var y = 0f
                while (y < size.height + r) {
                    val xOff = if (row % 2 == 0) 0f else r * 1.5f
                    var x = xOff
                    while (x < size.width + r) {
                        hexagon(this, Offset(x, y), r, light)
                        x += r * 3f
                    }
                    y += r * 1.7f
                    row++
                }
            }
        }
        // Lomo lateral (cubo 3D).
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(16.dp)
                .background(Brush.horizontalGradient(listOf(mid, dark))),
        )
        // Emblema del tipo, abajo-izquierda, sobresaliendo un poco.
        TypeEmblem(
            type = type,
            size = 30.dp,
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
        )
    }
}

private fun hexagon(scope: androidx.compose.ui.graphics.drawscope.DrawScope, c: Offset, r: Float, color: Color) {
    val path = androidx.compose.ui.graphics.Path()
    for (i in 0..5) {
        val a = Math.toRadians((60.0 * i - 30.0)).toFloat()
        val x = c.x + r * kotlin.math.cos(a)
        val y = c.y + r * kotlin.math.sin(a)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    scope.drawPath(path, color)
}
