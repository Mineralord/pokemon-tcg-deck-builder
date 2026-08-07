package com.mineralord.tcg.core.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * VUELO REAL entre zonas. Compose no ofrece de forma nativa/estable un elemento que viaje con
 * TRAYECTORIA FÍSICA entre dos ubicaciones arbitrarias de la pantalla (posiciones absolutas del
 * tablero, distintos padres). Aquí se implementa como abstracción reutilizable: un HOST recibe
 * "vuelos" (rect origen → rect destino) y una CAPA los reproduce por encima de todo, animando
 * posición, tamaño, un ARCO de lanzamiento y una leve rotación con resorte (inercia).
 *
 * Es puramente visual: el llamador decide QUÉ vuela (diffing de estado) y CÓMO se dibuja la
 * carta (composable libre), sin acoplar el framework a assets del juego.
 */
class FlightHost {
    internal val flights = mutableStateListOf<Flight>()

    /**
     * Lanza un vuelo desde [from] hasta [to] (rects en coords de RAÍZ, px). [arc] es la altura
     * del arco como fracción de la distancia (0 = línea recta). [content] dibuja la carta que
     * viaja (llena su caja).
     */
    fun launch(
        from: Rect,
        to: Rect,
        arc: Float = 0.16f,
        content: @Composable () -> Unit,
    ) {
        flights.add(Flight(id = nextId++, from = from, to = to, arc = arc, content = content))
    }

    private var nextId: Long = 0L
}

/** Un vuelo en curso. */
internal class Flight(
    val id: Long,
    val from: Rect,
    val to: Rect,
    val arc: Float,
    val content: @Composable () -> Unit,
)

/** Crea/recuerda un [FlightHost] estable para la composición actual. */
@Composable
fun rememberFlightHost(): FlightHost = remember { FlightHost() }

/**
 * Capa que reproduce todos los vuelos activos de [host]. Debe colocarse en el nivel superior
 * del contenedor (por encima del tablero) y ocupar el mismo sistema de coordenadas de raíz que
 * los rects proporcionados. Cada vuelo se auto-descarta al aterrizar.
 */
@Composable
fun FlightOverlay(
    host: FlightHost,
    stiffness: Float = Spring.StiffnessLow,
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
) {
    val density = LocalDensity.current
    host.flights.forEach { flight ->
        key(flight.id) {
            val progress = remember { Animatable(0f) }
            LaunchedEffect(flight.id) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
                )
                host.flights.remove(flight)
            }
            val p = progress.value
            // Interpolación de caja (posición + tamaño).
            val x = lerp(flight.from.left, flight.to.left, p)
            val y0 = lerp(flight.from.top, flight.to.top, p)
            val w = lerp(flight.from.width, flight.to.width, p)
            val h = lerp(flight.from.height, flight.to.height, p)
            // Arco de lanzamiento: parábola con pico en el centro del trayecto.
            val dx = flight.to.center.x - flight.from.center.x
            val dy = flight.to.center.y - flight.from.center.y
            val dist = sqrt(dx * dx + dy * dy)
            val peak = 4f * p * (1f - p)
            val y = y0 - peak * flight.arc * dist
            // Leve rotación en el sentido del desplazamiento (sensación de "toss").
            val rot = sign(dx) * 8f * peak
            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                    .graphicsLayer { rotationZ = rot },
            ) {
                flight.content()
            }
        }
    }
}
