package com.mineralord.tcg.core.animationcompose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Valida la parte PURA del renderer: la interpolación lineal de Rect que el
 * [MoveNodeRenderer] usará para su `graphicsLayer`. Se apoya en el `lerp(Rect,Rect,t)`
 * nativo de Compose (no interpolación casera).
 */
class MoveRenderNodeTest {

    private val origin = Rect(Offset(0f, 0f), androidx.compose.ui.geometry.Size(10f, 10f))
    private val destination = Rect(Offset(100f, 200f), androidx.compose.ui.geometry.Size(10f, 10f))

    private fun nodeAt(progress: Float) =
        MoveRenderNode(RenderNodeId("n"), origin, destination, progress)

    @Test
    fun `progress 0 devuelve el origen`() {
        assertEquals(Offset(0f, 0f), nodeAt(0f).interpolatedBounds().topLeft)
    }

    @Test
    fun `progress 1 devuelve el destino`() {
        assertEquals(Offset(100f, 200f), nodeAt(1f).interpolatedBounds().topLeft)
    }

    @Test
    fun `progress 0_5 devuelve el punto medio`() {
        assertEquals(Offset(50f, 100f), nodeAt(0.5f).interpolatedBounds().topLeft)
    }

    @Test
    fun `la capa de un nodo de movimiento es siempre Flight`() {
        assertEquals(RenderLayer.Flight, nodeAt(0f).layer)
    }
}
