package com.mineralord.tcg.core.animationcompose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.zIndex

/**
 * Estado de render observable disponible para los futuros renderizadores de nodos
 * (fase 3). Los executors escriben en él; los renderers por capa lo leerán aquí.
 */
val LocalAnimationRenderState = staticCompositionLocalOf<AnimationRenderState> {
    error("No hay AnimationRenderState en composición. Envuelve la UI en un AnimationStage.")
}

/**
 * Desplazamiento en coordenadas de raíz de la capa de vuelo respecto al origen de la ventana. Las
 * ranuras se registran con `positionInRoot` (coords de ventana), pero la traslación de un nodo con
 * `graphicsLayer` es relativa a la posición donde se colocó la capa de vuelo. Restando este origen,
 * el nodo se dibuja en las coordenadas de raíz correctas **sea cual sea el punto de montaje** del
 * `AnimationStage` (pantalla completa en el juego, o dentro del Host del Studio).
 *
 * Por defecto [Offset.Zero]: si el Stage está en el origen de la ventana (uso a pantalla completa),
 * el comportamiento es idéntico al anterior — cambio retrocompatible.
 */
val LocalFlightOrigin = staticCompositionLocalOf { Offset.Zero }

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
        // Aura de habilidad por debajo de las cartas (glow pasivo/manual).
        RenderLayer.Underglow -> NodeLayer(RenderLayer.Underglow)
        // Primera capa REAL conectada: overlay de vuelo de cartas.
        RenderLayer.Flight -> FlightLayer()
        // Capas preparadas y vacías (se implementan en fases posteriores):
        RenderLayer.Particles -> Unit // Canvas de partículas (un lienzo, withFrameNanos)
        RenderLayer.Effects -> NodeLayer(RenderLayer.Effects) // FX a pantalla completa
        RenderLayer.Overlay -> NodeLayer(RenderLayer.Overlay)  // rótulos / indicadores
    }
}

/**
 * Renderiza una capa GENÉRICA por despacho de tipo de nodo (Open-Closed): lee los nodos vivos de
 * [layer] del [AnimationRenderState] y delega cada uno a su renderer. Comparte con [FlightLayer] la
 * captura del origen de la capa en coords de raíz ([LocalFlightOrigin]) para dibujar correcto sea
 * cual sea el punto de montaje del Stage. Un tipo de nodo sin renderer se ignora (aún no existe).
 */
@Composable
private fun NodeLayer(layer: RenderLayer) {
    val renderState = LocalAnimationRenderState.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        CompositionLocalProvider(LocalFlightOrigin provides origin) {
            renderState.nodesOf(layer).forEach { node ->
                when (node) {
                    is AbilityGlowRenderNode -> AbilityGlowNodeRenderer(node)
                    is BannerRenderNode -> BannerNodeRenderer(node)
                    else -> Unit // otros tipos de nodo: su renderer llegará en fases futuras
                }
            }
        }
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
    // Captura el origen de la capa de vuelo en coords de raíz para que los nodos se dibujen bien
    // aunque el Stage no esté en el origen de la ventana (p. ej. dentro del Host del Studio).
    var flightOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { flightOrigin = it.positionInRoot() },
    ) {
        CompositionLocalProvider(LocalFlightOrigin provides flightOrigin) {
            renderState.nodesOf(RenderLayer.Flight).forEach { node ->
                when (node) {
                    is MoveRenderNode -> MoveNodeRenderer(node)
                    is DrawCardRenderNode -> DrawCardNodeRenderer(node)
                    is EvolveRenderNode -> EvolveNodeRenderer(node)
                    else -> Unit // otros tipos de nodo: su renderer llegará en fases futuras
                }
            }
        }
    }
}
