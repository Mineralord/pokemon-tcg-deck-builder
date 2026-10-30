package com.mineralord.tcg.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hilo divisor "arcoíris" ANIMADO (firma visual de TCG Live): el degradado RGB difuminado se desliza
 * continuamente hacia la derecha, en bucle perfecto (no se nota inicio ni fin) y con buen espacio por
 * color.
 *
 * El bucle sin costura se consigue con un degradado PERIÓDICO ([TileMode.Repeated]) cuyo periodo es
 * [periodWidth]: la lista de colores se cierra repitiendo el primero al final, y la animación desplaza
 * el patrón exactamente un periodo; al reiniciarse, el fotograma final coincide con el inicial.
 *
 * @param colors secuencia de colores del arcoíris (se repite en bucle).
 * @param height grosor del hilo.
 * @param periodWidth ancho de un ciclo completo del degradado (mayor = colores más espaciados/suaves).
 * @param periodMillis duración de un ciclo (menor = más rápido).
 */
@Composable
fun RainbowBar(
    colors: List<androidx.compose.ui.graphics.Color>,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
    periodWidth: Dp = 240.dp,
    periodMillis: Int = 4200,
) {
    // Cierra el ciclo: repetir el primer color al final hace que el patrón sea perfectamente periódico.
    val looped = remember(colors) { colors + colors.first() }
    // Calidad baja: barra quieta y sin desenfoque (ahorra un redibujado por frame en cada pantalla).
    val low = graphicsQuality() == GraphicsQuality.LOW
    val phase = if (low) 0f else {
        val transition = rememberInfiniteTransition(label = "rainbow")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "rainbowPhase",
        ).value
    }
    androidx.compose.foundation.layout.Spacer(
        modifier
            .height(height)
            // Leve desenfoque para el look "RGB difuminado" sin perder el hilo fino.
            .then(if (low) Modifier else Modifier.blur(1.5.dp))
            .drawBehind {
                val periodPx = periodWidth.toPx()
                val shift = phase * periodPx // desplazamiento hacia la derecha (un periodo completo).
                drawRect(
                    brush = Brush.linearGradient(
                        colors = looped,
                        start = Offset(-shift, 0f),
                        end = Offset(periodPx - shift, 0f),
                        tileMode = TileMode.Repeated,
                    ),
                )
            },
    )
}
