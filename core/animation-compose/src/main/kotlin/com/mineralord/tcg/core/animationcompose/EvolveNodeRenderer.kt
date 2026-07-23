package com.mineralord.tcg.core.animationcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Curva de "ida y vuelta" (0→1→0), pico en la mitad. Pura y testeable. */
fun evolveArc(progress: Float): Float = sin((progress.coerceIn(0f, 1f) * PI).toFloat())

/**
 * NodeRenderer de la evolución: transforma un [EvolveRenderNode] en Compose. Responsabilidad
 * única — dibujar; el tiempo ya viene resuelto en `progress`. Combina:
 * - **Carta** (placeholder de vuelo compartido) con `graphicsLayer`: ascenso (rise), bombeo
 *   (pulse), giro (spin) y volteo horizontal (flip).
 * - **Anillo** expansivo y **destello** (Canvas), con el matiz (hue) de la variante.
 *
 * Coordenadas en raíz menos [LocalFlightOrigin] (0 a pantalla completa; offset del Host en el
 * Studio), igual que los demás renderers ⇒ dibuja correcto sea cual sea el punto de montaje.
 */
@Composable
fun EvolveNodeRenderer(node: EvolveRenderNode) {
    val v = node.visual
    val p = node.progress
    val arc = evolveArc(p)
    val flightOrigin = LocalFlightOrigin.current
    val b = node.bounds

    val centerX = b.center.x - flightOrigin.x
    val centerY = b.center.y - flightOrigin.y - v.rise * b.height * arc
    val scale = 1f + v.pulse * arc
    val flipX = if (v.flip > 0f) cos(v.flip * 2f * PI.toFloat() * p) else 1f
    val spinDeg = v.spin * 360f * p
    val hue = v.hue.coerceIn(0f, 360f)
    val accent = Color.hsv(hue, 0.7f, 1f)

    val density = LocalDensity.current
    val widthDp = with(density) { b.width.toDp() }
    val heightDp = with(density) { b.height.toDp() }

    Box(Modifier.fillMaxSize()) {
        // Anillo + destello (detrás de la carta).
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(centerX, centerY)
            if (v.ring > 0f && p > 0f) {
                val radius = v.ring * b.width * p
                drawCircle(
                    color = accent.copy(alpha = (1f - p) * 0.6f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = (b.width * 0.06f).coerceAtLeast(2f)),
                )
            }
            if (v.flash > 0f) {
                drawCircle(
                    color = Color.White.copy(alpha = v.flash * arc),
                    radius = b.width * (0.7f + p * 0.9f),
                    center = center,
                )
            }
        }
        // Carta transformada.
        Box(
            Modifier
                .graphicsLayer {
                    translationX = centerX - b.width / 2f
                    translationY = centerY - b.height / 2f
                    scaleX = scale * flipX
                    scaleY = scale
                    rotationZ = spinDeg
                }
                .size(widthDp, heightDp)
                .flightCardPlaceholder(),
        )
    }
}
