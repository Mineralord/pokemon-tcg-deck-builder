package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf

/** Identificador tipado de un nodo de render vivo. */
@JvmInline
value class RenderNodeId(val value: String)

/**
 * Descriptor de "algo que dibujar ahora mismo" en una capa concreta.
 *
 * Es una interfaz **abierta** (no `sealed`) a propósito: con el registro distribuido,
 * distintos módulos/executors aportarán sus propios tipos de nodo (carta en vuelo,
 * ráfaga de partículas, flash…) sin tocar `core:animation-compose`. Cada nodo declara
 * en qué [RenderLayer] vive; el `AnimationStage` sólo enruta por capa (Open-Closed).
 *
 * Sólo describe QUÉ dibujar (datos de render), nunca CÓMO ni con qué Composable.
 */
interface RenderNode {
    val id: RenderNodeId
    val layer: RenderLayer
}

/**
 * Estado de render observable del framework: el canal entre el intérprete puro
 * (`AnimationPlayer` + executors) y el dibujo (`AnimationStage`).
 *
 * Nombre elegido sobre "Scene": no es un scene-graph con lógica de dominio, sino el
 * **estado de dibujo reactivo**. Los executors escriben nodos aquí; `AnimationStage`
 * los lee y los pinta. Sin comportamiento de animación propio.
 *
 * Seguridad ante recomposición: respaldado por `SnapshotStateMap`; las lecturas
 * ([nodesOf]) son observables y las escrituras son seguras bajo snapshots. [Stable]
 * para permitir a Compose saltar recomposiciones cuando la referencia no cambia.
 */
@Stable
class AnimationRenderState {

    private val nodes = mutableStateMapOf<RenderNodeId, RenderNode>()

    /** Nodos vivos de una capa, en orden de inserción. Lectura observable. */
    fun nodesOf(layer: RenderLayer): List<RenderNode> =
        nodes.values.filter { it.layer == layer }

    /** Número total de nodos vivos. */
    val size: Int get() = nodes.size

    /** Inserta o reemplaza (por [RenderNode.id]) un nodo de render. */
    fun put(node: RenderNode) {
        nodes[node.id] = node
    }

    /** Elimina un nodo (p. ej. al terminar su animación). */
    fun remove(id: RenderNodeId) {
        nodes.remove(id)
    }

    /** Elimina todos los nodos (p. ej. al abortar/limpiar la escena). */
    fun clear() {
        nodes.clear()
    }
}
