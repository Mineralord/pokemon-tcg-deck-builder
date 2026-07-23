package com.mineralord.tcg.core.animationcompose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex

/**
 * Estado de render observable disponible para los futuros renderizadores de nodos
 * (fase 3). Los executors escriben en él; los renderers por capa lo leerán aquí.
 */
val LocalAnimationRenderState = staticCompositionLocalOf<AnimationRenderState> {
    error("No hay AnimationRenderState en composición. Envuelve la UI en un AnimationStage.")
}

/**
 * Raíz de render del framework de animaciones (ESQUELETO de la Fase 1).
 *
 * Responsabilidades de esta fase:
 * - Dibujar las capas en el orden de [RenderLayer.orderedByZ] (de fondo a frente),
 *   fijando el apilamiento con `zIndex(layer.zOrder)` — derivado del CONTRATO
 *   [RenderLayer], no de números arbitrarios.
 * - Proveer el [CoordinateRegistry] a los hijos vía [LocalCoordinateRegistry], para
 *   que las ranuras se rastreen con [trackBounds].
 * - **Preparar** las capas de overlay/vuelo, partículas y efectos como ranuras de
 *   render vacías, listas para llenarse en fases futuras.
 *
 * Lo que NO hace todavía: no reproduce animaciones, no observa un `AnimationScene`
 * (aún no existe), no dibuja partículas ni efectos. Los slots [board] y [cards]
 * permiten montar la UI real de la partida desde ahora; el resto son placeholders.
 *
 * @param registry registro de coordenadas; por defecto uno recordado por composición.
 * @param board contenido de la capa de fondo (tablero/zonas).
 * @param cards contenido de la capa de cartas en reposo.
 */
@Composable
fun AnimationStage(
    modifier: Modifier = Modifier,
    registry: CoordinateRegistry = remember { CoordinateRegistry() },
    renderState: AnimationRenderState = remember { AnimationRenderState() },
    board: @Composable () -> Unit = {},
    cards: @Composable () -> Unit = {},
) {
    CompositionLocalProvider(
        LocalCoordinateRegistry provides registry,
        LocalAnimationRenderState provides renderState,
    ) {
        Box(modifier.fillMaxSize()) {
            RenderLayer.orderedByZ.forEach { layer ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(layer.zOrder.toFloat()),
                ) {
                    RenderLayerContent(layer, board, cards)
                }
            }
        }
    }
}

/**
 * Enruta cada [RenderLayer] a su contenido. Separado del layout para que añadir el
 * render real de una capa (partículas, efectos…) en fases futuras sea un cambio
 * localizado (Open-Closed).
 */
@Composable
private fun RenderLayerContent(
    layer: RenderLayer,
    board: @Composable () -> Unit,
    cards: @Composable () -> Unit,
) {
    when (layer) {
        RenderLayer.Board -> board()
        RenderLayer.Cards -> cards()
        // Primera capa REAL conectada: overlay de vuelo de cartas.
        RenderLayer.Flight -> FlightLayer()
        // Capas preparadas y vacías (se implementan en fases posteriores):
        RenderLayer.Particles -> Unit // Canvas de partículas (un lienzo, withFrameNanos)
        RenderLayer.Effects -> Unit   // FX a pantalla completa
        RenderLayer.Overlay -> Unit   // texto flotante / indicadores
    }
}

/**
 * Renderiza ÚNICAMENTE la capa [RenderLayer.Flight]: lee los nodos vivos del
 * [AnimationRenderState] y delega cada [MoveRenderNode] a su [MoveNodeRenderer]. Lectura
 * observable ⇒ se recompone al crear/actualizar/eliminar nodos (el frame loop del
 * ejecutor "empuja", esta capa "reacciona"). Otros tipos de nodo se ignoran hasta que
 * su renderer exista (Open-Closed).
 */
@Composable
private fun FlightLayer() {
    val renderState = LocalAnimationRenderState.current
    renderState.nodesOf(RenderLayer.Flight).forEach { node ->
        when (node) {
            is MoveRenderNode -> MoveNodeRenderer(node)
            is DrawCardRenderNode -> DrawCardNodeRenderer(node)
            else -> Unit // otros tipos de nodo: su renderer llegará en fases futuras
        }
    }
}
