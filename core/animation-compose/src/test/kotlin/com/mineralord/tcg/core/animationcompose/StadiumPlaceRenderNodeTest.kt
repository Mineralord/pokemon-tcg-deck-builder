package com.mineralord.tcg.core.animationcompose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Valida la parte PURA de la colocación de Estadio: arco (Bézier), escala/inclinación de asentamiento
 * y el destello de aterrizaje, todos derivados del progreso.
 */
class StadiumPlaceRenderNodeTest {

    private val origin = Rect(Offset(0f, 0f), Size(10f, 10f))        // centro (5,5)
    private val destination = Rect(Offset(100f, 0f), Size(10f, 10f)) // centro (105,5)

    private fun nodeAt(progress: Float, arc: Float = 20f) =
        StadiumPlaceRenderNode(RenderNodeId("n"), origin, destination, arcHeightPx = arc, progress = progress)

    @Test
    fun `en progress 0 y 1 el centro es origen y destino`() {
        assertEquals(origin.center, nodeAt(0f).center())
        val c = nodeAt(1f).center()
        assertEquals(destination.center.x, c.x, 0.001f)
        assertEquals(destination.center.y, c.y, 0.001f)
    }

    @Test
    fun `el arco eleva el punto medio`() {
        val mid = nodeAt(0.5f).center()
        assertTrue(mid.y < (origin.center.y + destination.center.y) / 2f)
    }

    @Test
    fun `escala llega pesada de 0_9 a 1_0 y se acota en overshoot`() {
        assertEquals(0.9f, nodeAt(0f).scale(), 0.0001f)
        assertEquals(1.0f, nodeAt(1f).scale(), 0.0001f)
        assertEquals(1.0f, nodeAt(1.12f).scale(), 0.0001f)   // overshoot no deforma
    }

    @Test
    fun `la inclinacion se endereza a 0 al asentar`() {
        assertEquals(-5f, nodeAt(0f).tiltDeg(), 0.0001f)
        assertEquals(0f, nodeAt(1f).tiltDeg(), 0.0001f)
    }

    @Test
    fun `el destello de aterrizaje aparece al tocar y no antes`() {
        assertEquals(0f, nodeAt(0.5f).landingFlashAlpha(), 0.0001f)   // aún viajando: sin flash
        assertTrue(nodeAt(0.95f).landingFlashAlpha() > 0f)            // al aterrizar: destella
        assertTrue(nodeAt(1.12f).landingFlashAlpha() <= 0.0001f)      // fin del overshoot: apagado
    }

    @Test
    fun `el easing de la colocacion rebasa 1 (peso) y reposa exacto`() {
        val peak = (80..99).map { StadiumPlaceEasing.transform(it / 100f) }.maxOrNull()!!
        assertTrue(peak > 1f, "el easing debe rebasar 1 (overshoot de peso); pico=$peak")
        assertEquals(1f, StadiumPlaceEasing.transform(1f), 0.0001f)
    }
}
