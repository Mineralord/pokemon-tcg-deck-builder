package com.mineralord.tcg.feature.packs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Confetto(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val spin: Float,
    val delay: Float,
    val ribbon: Boolean,
)

/**
 * Lluvia de confeti/serpentinas que estalla desde el centro al revelar una carta rara
 * (estilo Pokémon TCG Pocket). Cada partícula parte del centro con impulso radial, sube y
 * luego cae por "gravedad", girando sobre sí misma y desvaneciéndose. Arte propio.
 *
 * [trigger] reinicia la animación; [intensity] escala el número de piezas.
 */
@Composable
fun ConfettiBurst(
    trigger: Any,
    intensity: Int,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1600))
    }
    val pieces = remember(trigger) {
        val rnd = Random(trigger.hashCode() * 31 + 7)
        List(intensity) {
            Confetto(
                angle = -1.5708f + (rnd.nextFloat() - 0.5f) * 3.4f,   // hacia arriba, abanicado
                speed = 0.55f + rnd.nextFloat() * 0.9f,
                size = 8f + rnd.nextFloat() * 12f,
                color = CONFETTI_PALETTE[rnd.nextInt(CONFETTI_PALETTE.size)],
                spin = (rnd.nextFloat() - 0.5f) * 1440f,
                delay = rnd.nextFloat() * 0.15f,
                ribbon = rnd.nextFloat() < 0.5f,
            )
        }
    }

    Canvas(modifier = modifier) {
        val p = progress.value
        val c = Offset(size.width / 2f, size.height * 0.46f)
        val span = size.minDimension
        for (piece in pieces) {
            val local = ((p - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (local <= 0f) continue
            // Balística: impulso radial + gravedad creciente (t^2).
            val reach = piece.speed * span * 0.6f
            val dx = cos(piece.angle) * reach * local
            val dy = sin(piece.angle) * reach * local + span * 0.9f * local * local
            val pos = Offset(c.x + dx, c.y + dy)
            val alpha = (1f - local * local).coerceIn(0f, 1f)
            rotate(degrees = piece.spin * local, pivot = pos) {
                if (piece.ribbon) {
                    drawRect(
                        color = piece.color.copy(alpha = alpha),
                        topLeft = Offset(pos.x - piece.size * 0.25f, pos.y - piece.size),
                        size = Size(piece.size * 0.5f, piece.size * 2f),
                    )
                } else {
                    drawRect(
                        color = piece.color.copy(alpha = alpha),
                        topLeft = Offset(pos.x - piece.size / 2f, pos.y - piece.size / 2f),
                        size = Size(piece.size, piece.size * 0.7f),
                    )
                }
            }
        }
    }
}

private val CONFETTI_PALETTE = listOf(
    Color(0xFFFFD54F), // dorado
    Color(0xFF4FC3F7), // celeste
    Color(0xFFFF7043), // naranja
    Color(0xFF81C784), // verde
    Color(0xFFEC407A), // rosa
    Color(0xFFBA68C8), // violeta
    Color(0xFFFFFFFF), // blanco
)
