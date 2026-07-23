package com.mineralord.tcg.core.animationcompose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.pow

/** Rango de escala del robo (la carta "sale del mazo" creciendo). */
private const val DRAW_SCALE_START = 0.82f
private const val DRAW_SCALE_END = 1.0f

/** Rotación inicial del robo en grados; se endereza al asentar en el fan. */
private const val DRAW_ROTATION_START_DEG = -6f

/**
 * Posición del CENTRO de la carta a lo largo de un arco (Bézier cuadrática) entre el
 * centro del mazo (origin) y el de la mano (destination). Función PURA (testeable).
 *
 * Punto de control = punto medio elevado [arcHeightPx] hacia ARRIBA (y negativo en
 * coordenadas de pantalla) → la carta "despega" del mazo y cae al fan en vez de ir en
 * línea recta. `t` puede exceder 1 (overshoot del easing): la fórmula extrapola de forma
 * continua, produciendo el leve rebasar-y-asentar deseado.
 */
fun DrawCardRenderNode.center(): Offset {
    val p0 = origin.center
    val p1 = destination.center
    val control = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f - arcHeightPx)
    val t = progress
    val oneMinusT = 1f - t
    // B(t) = (1-t)²·P0 + 2(1-t)t·C + t²·P1
    val x = oneMinusT.pow(2) * p0.x + 2 * oneMinusT * t * control.x + t.pow(2) * p1.x
    val y = oneMinusT.pow(2) * p0.y + 2 * oneMinusT * t * control.y + t.pow(2) * p1.y
    return Offset(x, y)
}

/** Escala interpolada 0.82→1.0. El progreso se acota a [0,1] para no deformar en overshoot. */
fun DrawCardRenderNode.scale(): Float {
    val p = progress.coerceIn(0f, 1f)
    return DRAW_SCALE_START + (DRAW_SCALE_END - DRAW_SCALE_START) * p
}

/** Rotación interpolada -6°→0°. Acotada a [0,1] por la misma razón que la escala. */
fun DrawCardRenderNode.rotationDeg(): Float {
    val p = progress.coerceIn(0f, 1f)
    return DRAW_ROTATION_START_DEG * (1f - p)
}

/**
 * NodeRenderer del robo: transforma un [DrawCardRenderNode] en Compose. Responsabilidad
 * única — dibujar; el tiempo/overshoot ya vienen resueltos en `progress`.
 *
 * Técnica (de implementation-research): posición en arco vía Bézier, y escala+rotación
 * aplicadas con [graphicsLayer] (fase draw, sin relayout; `transformOrigin` por defecto =
 * centro, así escala y giro pivotan sobre el centro de la carta). El tamaño es el del
 * rect de origen; el contenido es un placeholder mínimo (sin assets/holo aún: eso son
 * animaciones posteriores del backlog).
 */
@Composable
fun DrawCardNodeRenderer(node: DrawCardRenderNode) {
    val center = node.center()
    val scale = node.scale()
    val rotation = node.rotationDeg()
    val size = node.origin.size
    val flightOrigin = LocalFlightOrigin.current
    val density = LocalDensity.current
    val widthDp = with(density) { size.width.toDp() }
    val heightDp = with(density) { size.height.toDp() }

    Box(
        Modifier
            .graphicsLayer {
                // Traslación del top-left = centro interpolado menos media carta, menos el origen
                // de la capa de vuelo (0 a pantalla completa; el offset del Host en el Studio).
                translationX = center.x - size.width / 2f - flightOrigin.x
                translationY = center.y - size.height / 2f - flightOrigin.y
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .size(widthDp, heightDp)
            .flightCardPlaceholder(),
    )
}
