package com.mineralord.tcg.core.animationcompose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Posición interpolada del nodo en este instante. Función PURA (testeable sin Compose
 * runtime): usa `androidx.compose.ui.geometry.lerp(Rect, Rect, fraction)` de la stdlib
 * de Compose en vez de rodar la interpolación a mano. Trayectoria lineal.
 */
fun MoveRenderNode.interpolatedBounds(): Rect = lerp(origin, destination, progress)

/**
 * Primer NodeRenderer: transforma UN [MoveRenderNode] en su representación Compose.
 * Responsabilidad única — dibujar; no conoce tiempo, cola ni ejecutor.
 *
 * Técnica (justificada en la investigación): la posición se aplica con
 * [graphicsLayer] `translationX/Y` — ocurre en la fase de *draw/layer*, sin invalidar
 * layout cada frame (superior a `Modifier.offset` para movimiento continuo). El tamaño
 * es el del rectángulo de origen. Contenido = placeholder mínimo (sin assets aún): lo
 * que valida el pipeline es que el rect vuele, no su estética.
 *
 * Los rects vienen en píxeles (coordenadas de raíz); se convierten a dp con la densidad
 * para el `size`, mientras la traslación (px) se aplica directa en graphicsLayer.
 */
@Composable
fun MoveNodeRenderer(node: MoveRenderNode) {
    val bounds = node.interpolatedBounds()
    val flightOrigin = LocalFlightOrigin.current
    val density = LocalDensity.current
    val widthDp = with(density) { bounds.width.toDp() }
    val heightDp = with(density) { bounds.height.toDp() }

    Box(
        Modifier
            .graphicsLayer {
                // Coords de raíz menos el origen de la capa de vuelo (0 a pantalla completa).
                translationX = bounds.left - flightOrigin.x
                translationY = bounds.top - flightOrigin.y
            }
            .size(widthDp, heightDp)
            .flightCardPlaceholder(),
    )
}
