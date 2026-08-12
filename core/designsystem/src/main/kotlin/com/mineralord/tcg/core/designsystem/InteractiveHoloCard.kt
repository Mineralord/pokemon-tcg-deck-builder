package com.mineralord.tcg.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.tilt.HoloAssets
import com.mineralord.tcg.core.designsystem.tilt.HoloBitmaps
import com.mineralord.tcg.core.designsystem.tilt.Tilt
import com.mineralord.tcg.core.designsystem.tilt.holoOverlay
import com.mineralord.tcg.core.designsystem.tilt.resolveFinish
import com.mineralord.tcg.core.designsystem.tilt.tiltParallax
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.launch

/**
 * Carta **HD interactiva** reutilizable: front real de malie (o arte por URL como respaldo) con
 * **inclinación por dedo** (parallax + holo que sigue al puntero) y vuelta elástica al soltar.
 * Es el mismo motor del visor a pantalla completa ([CardDetailDialog]), pero embebible en cualquier
 * layout (revelado de sobre, colección, etc.) SIN diálogo ni zoom.
 *
 * Un toque simple (sin arrastre) NO se consume: así el contenedor puede seguir reaccionando al tap
 * (p. ej. avanzar a la siguiente carta). Solo el arrastre inclina la carta y se consume.
 *
 * El tamaño lo decide el [modifier] del llamador (no impone aspect ratio).
 */
@Composable
fun InteractiveHoloCard(
    imageUrl: String?,
    setCode: String?,
    cardNumber: Int?,
    rarity: Rarity?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val holo by produceState<HoloBitmaps?>(null, cardNumber, rarity, setCode) {
        value = HoloAssets.resolve(context, setCode ?: "sv3pt5", cardNumber, rarity)
    }
    val finish = remember(rarity, holo) { holo?.finish ?: resolveFinish(rarity) }

    val scope = rememberCoroutineScope()
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }
    val tilt = Tilt(tiltX.value, tiltY.value)

    Box(
        modifier = modifier
            .tiltParallax(tilt)
            .holoOverlay(tilt, finish, holo?.mask, holo?.etch, holo?.foilCode ?: -1f)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isNotEmpty()) {
                            val p = pressed.first().position
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val tx = ((p.x / w) * 2f - 1f).coerceIn(-1f, 1f)
                            val ty = ((p.y / h) * 2f - 1f).coerceIn(-1f, 1f)
                            scope.launch { tiltX.snapTo(tx) }
                            scope.launch { tiltY.snapTo(ty) }
                        }
                        // Consume SOLO si hubo arrastre (para no anular el toque del contenedor).
                        if (event.changes.any { it.positionChanged() }) {
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    val restSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    scope.launch { tiltX.animateTo(0f, restSpring) }
                    scope.launch { tiltY.animateTo(0f, restSpring) }
                }
            },
    ) {
        val front = holo?.front
        if (front != null) {
            Image(
                bitmap = front,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
