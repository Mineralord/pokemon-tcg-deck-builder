package com.mineralord.tcg.feature.packs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Spark(val angle: Float, val maxDist: Float, val size: Float, val delay: Float)

/**
 * Estallido de chispas (estrellas de 4 puntas) que parten del centro al revelar
 * una carta. [trigger] reinicia la animación por cada carta; [intensity] y [color]
 * escalan con la rareza. Arte propio (sin assets de terceros).
 */
@Composable
fun SparkleBurst(
    trigger: Any,
    color: Color,
    intensity: Int,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 750, easing = LinearOutSlowInEasing))
    }
    val sparks = remember(trigger) {
        val rnd = Random(trigger.hashCode())
        List(intensity) {
            Spark(
                angle = rnd.nextFloat() * 6.2832f,
                maxDist = 0.30f + rnd.nextFloat() * 0.55f,   // fracción del lado menor
                size = 6f + rnd.nextFloat() * 12f,
                delay = rnd.nextFloat() * 0.25f,
            )
        }
    }

    Canvas(modifier = modifier) {
        val p = progress.value
        val c = Offset(size.width / 2f, size.height / 2f)
        val span = size.minDimension * 0.5f
        for (s in sparks) {
            val local = ((p - s.delay) / (1f - s.delay)).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val dist = s.maxDist * span * local
            val pos = Offset(c.x + cos(s.angle) * dist, c.y + sin(s.angle) * dist)
            val alpha = (1f - local)
            val scale = 0.4f + 0.6f * (1f - local)
            drawSparkle(pos, s.size * scale, color.copy(alpha = alpha))
        }
    }
}

/** Estrella de 4 puntas (rombo afilado en + ). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkle(center: Offset, r: Float, color: Color) {
    val inner = r * 0.28f
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + r, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + r)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - r, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
    drawPath(path, color)
}
