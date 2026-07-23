package com.mineralord.tcg.core.animationcompose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Valida la parte PURA del robo: arco (Bézier), escala y rotación derivados del progreso.
 */
class DrawCardRenderNodeTest {

    private val origin = Rect(Offset(0f, 0f), Size(10f, 10f))       // centro (5,5)
    private val destination = Rect(Offset(100f, 0f), Size(10f, 10f)) // centro (105,5)

    private fun nodeAt(progress: Float, arc: Float = 20f) =
        DrawCardRenderNode(RenderNodeId("n"), origin, destination, arcHeightPx = arc, progress = progress)

    @Test
    fun `en progress 0 el centro es el del origen`() {
        assertEquals(origin.center, nodeAt(0f).center())
    }

    @Test
    fun `en progress 1 el centro es el del destino`() {
        val c = nodeAt(1f).center()
        assertEquals(destination.center.x, c.x, 0.001f)
        assertEquals(destination.center.y, c.y, 0.001f)
    }

    @Test
    fun `el arco eleva el punto medio (y menor que la recta)`() {
        val mid = nodeAt(0.5f).center()
        val straightY = (origin.center.y + destination.center.y) / 2f // 5
        // Elevación = mitad de la altura del arco en el punto medio de una cuadrática.
        assertTrue(mid.y < straightY, "el punto medio debe estar elevado (y=${mid.y} < $straightY)")
        assertEquals(straightY - 20f / 2f, mid.y, 0.001f) // 5 - 10 = -5
    }

    @Test
    fun `sin arco la trayectoria es recta`() {
        val mid = nodeAt(0.5f, arc = 0f).center()
        assertEquals(5f, mid.y, 0.001f)
    }

    @Test
    fun `escala va de 0_82 a 1_0`() {
        assertEquals(0.82f, nodeAt(0f).scale(), 0.0001f)
        assertEquals(1.0f, nodeAt(1f).scale(), 0.0001f)
        assertEquals(0.91f, nodeAt(0.5f).scale(), 0.0001f)
    }

    @Test
    fun `rotacion se endereza a 0 al asentar`() {
        assertEquals(-6f, nodeAt(0f).rotationDeg(), 0.0001f)
        assertEquals(0f, nodeAt(1f).rotationDeg(), 0.0001f)
    }

    @Test
    fun `overshoot en escala y rotacion se acota (no deforma)`() {
        // progress 1.08 (overshoot del easing) NO debe pasar de escala 1 ni girar de más.
        assertEquals(1.0f, nodeAt(1.08f).scale(), 0.0001f)
        assertEquals(0f, nodeAt(1.08f).rotationDeg(), 0.0001f)
    }

    @Test
    fun `el easing del robo produce overshoot por encima de 1`() {
        // Cerca del final la curva rebasa 1 (rebote contenido) antes de asentar.
        val peak = (80..99).map { DrawCardEasing.transform(it / 100f) }.maxOrNull()!!
        assertTrue(peak > 1f, "el easing debe rebasar 1 (overshoot); pico=$peak")
        assertEquals(1f, DrawCardEasing.transform(1f), 0.0001f) // reposo exacto en destino
    }
}
