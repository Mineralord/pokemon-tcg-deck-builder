package com.mineralord.tcg.core.animationcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.pow

/** Rango de escala de la colocación: la carta llega PESADA (menos "crecimiento" que el robo). */
private const val STADIUM_SCALE_START = 0.9f
private const val STADIUM_SCALE_END = 1.0f

/** Inclinación inicial (grados); se endereza al asentar en el slot. */
private const val STADIUM_TILT_START_DEG = -5f

/** El destello de aterrizaje empieza a este progreso (justo antes de tocar el slot). */
private const val STADIUM_FLASH_START = 0.8f
private const val STADIUM_FLASH_PEAK = 0.95f
private const val STADIUM_FLASH_MAX_ALPHA = 0.55f

/**
 * Centro de la carta a lo largo de un arco (Bézier cuadrática) mano→slot. Función PURA (testeable).
 * Punto de control = punto medio elevado [arcHeightPx] hacia arriba (y negativo en pantalla). `t`
 * puede exceder 1 (overshoot del easing): la fórmula extrapola de forma continua (rebasar-y-asentar).
 */
fun StadiumPlaceRenderNode.center(): Offset {
    val p0 = origin.center
    val p1 = destination.center
    val control = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f - arcHeightPx)
    val t = progress
    val oneMinusT = 1f - t
    val x = oneMinusT.pow(2) * p0.x + 2 * oneMinusT * t * control.x + t.pow(2) * p1.x
    val y = oneMinusT.pow(2) * p0.y + 2 * oneMinusT * t * control.y + t.pow(2) * p1.y
    return Offset(x, y)
}

/** Escala interpolada 0.9→1.0. Progreso acotado a [0,1] para no deformar en overshoot. */
fun StadiumPlaceRenderNode.scale(): Float {
    val p = progress.coerceIn(0f, 1f)
    return STADIUM_SCALE_START + (STADIUM_SCALE_END - STADIUM_SCALE_START) * p
}

/** Inclinación interpolada -5°→0°. Acotada a [0,1]. */
fun StadiumPlaceRenderNode.tiltDeg(): Float {
    val p = progress.coerceIn(0f, 1f)
    return STADIUM_TILT_START_DEG * (1f - p)
}

/**
 * Alpha del destello radial de aterrizaje: 0 hasta [STADIUM_FLASH_START], sube al pico en
 * [STADIUM_FLASH_PEAK] y decae hacia el final del overshoot (~1.12). Comunica el "thud" del peso.
 */
fun StadiumPlaceRenderNode.landingFlashAlpha(): Float {
    val p = progress
    return when {
        p < STADIUM_FLASH_START -> 0f
        p < STADIUM_FLASH_PEAK ->
            (p - STADIUM_FLASH_START) / (STADIUM_FLASH_PEAK - STADIUM_FLASH_START) * STADIUM_FLASH_MAX_ALPHA
        else ->
            (((1.12f - p) / (1.12f - STADIUM_FLASH_PEAK)).coerceIn(0f, 1f)) * STADIUM_FLASH_MAX_ALPHA
    }
}

/**
 * NodeRenderer de la colocación de Estadio: transforma un [StadiumPlaceRenderNode] en Compose.
 * Responsabilidad única — dibujar; tiempo/overshoot ya vienen resueltos en `progress`.
 *
 * Capas del propio nodo: (1) la carta en vuelo (arco + escala + inclinación con [graphicsLayer], sin
 * relayout, pivote centro) y (2) un destello radial aditivo en el punto de aterrizaje que aparece al
 * tocar el slot y se desvanece — el "peso" del asentamiento. Sin assets externos (placeholder).
 */
@Composable
fun StadiumPlaceNodeRenderer(node: StadiumPlaceRenderNode) {
    val center = node.center()
    val scale = node.scale()
    val tilt = node.tiltDeg()
    val size = node.origin.size
    val flightOrigin = LocalFlightOrigin.current
    val density = LocalDensity.current
    val widthDp = with(density) { size.width.toDp() }
    val heightDp = with(density) { size.height.toDp() }

    // Destello radial de aterrizaje, anclado al centro del slot de destino.
    val flashAlpha = node.landingFlashAlpha()
    if (flashAlpha > 0f) {
        val dest = node.destination
        val radius = dest.maxDimension * 0.9f
        Canvas(Modifier.size(widthDp, heightDp).graphicsLayer {
            translationX = dest.center.x - size.width / 2f - flightOrigin.x
            translationY = dest.center.y - size.height / 2f - flightOrigin.y
        }) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = flashAlpha),
                        Color(0xFF8FD3FF).copy(alpha = flashAlpha * 0.6f),
                        Color.Transparent,
                    ),
                    center = c,
                    radius = radius,
                ),
                radius = radius,
                center = c,
            )
        }
    }

    Box(
        Modifier
            .graphicsLayer {
                translationX = center.x - size.width / 2f - flightOrigin.x
                translationY = center.y - size.height / 2f - flightOrigin.y
                scaleX = scale
                scaleY = scale
                rotationZ = tilt
            }
            .size(widthDp, heightDp)
            .flightCardPlaceholder(),
    )
}
