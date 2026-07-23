package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * Acceso al [CoordinateRegistry] activo desde cualquier hijo del `AnimationStage`, sin
 * pasarlo por parámetros. `static` porque cambia con muy poca frecuencia.
 */
val LocalCoordinateRegistry = staticCompositionLocalOf<CoordinateRegistry> {
    error("No hay CoordinateRegistry en composición. Envuelve la UI en un AnimationStage.")
}

/**
 * Marca este Composable como una ranura rastreada: publica su geometría en el
 * [registry] bajo [id] cada vez que su posición/tamaño cambian.
 *
 * NOTA DE IMPLEMENTACIÓN (seam deliberado): hoy usa [onGloballyPositioned] porque es la
 * única API disponible en Compose 1.7 (nuestro BOM 2024.12.01). Cuando se suba el BOM a
 * 2025.04.01+ (Compose 1.8), este es el ÚNICO punto a cambiar por `onLayoutRectChanged`,
 * que ofrece *debounce/throttle* y mucho menor coste (evita recalcular coordenadas
 * globales en cada layout). El [CoordinateRegistry] y los call-sites NO cambian.
 *
 * La ELIMINACIÓN del registro al salir de composición se gestiona en el call-site con
 * `DisposableEffect { onDispose { registry.remove(id) } }`; este modifier sólo publica
 * geometría (responsabilidad única).
 */
fun Modifier.trackBounds(id: SlotId, registry: CoordinateRegistry): Modifier =
    this.onGloballyPositioned { coordinates ->
        registry.place(
            id = id,
            bounds = SlotBounds(
                positionInRoot = coordinates.positionInRoot(),
                size = coordinates.size,
            ),
        )
    }
