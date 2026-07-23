package com.mineralord.tcg.core.animationcompose

import kotlin.test.Test
import kotlin.test.assertEquals

class AnimationRenderStateTest {

    /** Nodo de prueba (posible porque RenderNode es interfaz abierta). */
    private data class TestNode(
        override val id: RenderNodeId,
        override val layer: RenderLayer,
    ) : RenderNode

    private fun node(id: String, layer: RenderLayer) = TestNode(RenderNodeId(id), layer)

    @Test
    fun `put y nodesOf devuelven el nodo en su capa`() {
        val state = AnimationRenderState()
        state.put(node("n1", RenderLayer.Flight))

        assertEquals(listOf(RenderNodeId("n1")), state.nodesOf(RenderLayer.Flight).map { it.id })
        assertEquals(emptyList(), state.nodesOf(RenderLayer.Particles))
        assertEquals(1, state.size)
    }

    @Test
    fun `put por mismo id reemplaza`() {
        val state = AnimationRenderState()
        state.put(node("n1", RenderLayer.Flight))
        state.put(node("n1", RenderLayer.Particles)) // mismo id, otra capa

        assertEquals(1, state.size)
        assertEquals(emptyList(), state.nodesOf(RenderLayer.Flight))
        assertEquals(listOf(RenderNodeId("n1")), state.nodesOf(RenderLayer.Particles).map { it.id })
    }

    @Test
    fun `remove elimina el nodo`() {
        val state = AnimationRenderState()
        state.put(node("n1", RenderLayer.Effects))
        state.remove(RenderNodeId("n1"))

        assertEquals(0, state.size)
        assertEquals(emptyList(), state.nodesOf(RenderLayer.Effects))
    }

    @Test
    fun `nodos se agrupan por capa`() {
        val state = AnimationRenderState()
        state.put(node("a", RenderLayer.Flight))
        state.put(node("b", RenderLayer.Flight))
        state.put(node("c", RenderLayer.Particles))

        assertEquals(2, state.nodesOf(RenderLayer.Flight).size)
        assertEquals(1, state.nodesOf(RenderLayer.Particles).size)
        assertEquals(3, state.size)
    }

    @Test
    fun `clear vacia el estado`() {
        val state = AnimationRenderState()
        repeat(4) { state.put(node("n$it", RenderLayer.Overlay)) }
        state.clear()

        assertEquals(0, state.size)
    }
}
