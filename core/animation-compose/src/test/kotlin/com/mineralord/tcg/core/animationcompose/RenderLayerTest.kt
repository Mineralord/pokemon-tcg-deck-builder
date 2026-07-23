package com.mineralord.tcg.core.animationcompose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RenderLayerTest {

    @Test
    fun `orderedByZ va de fondo a frente por zOrder`() {
        assertEquals(
            listOf(
                RenderLayer.Board,
                RenderLayer.Cards,
                RenderLayer.Flight,
                RenderLayer.Particles,
                RenderLayer.Effects,
                RenderLayer.Overlay,
            ),
            RenderLayer.orderedByZ,
        )
    }

    @Test
    fun `zOrder es estrictamente creciente en orderedByZ`() {
        val zs = RenderLayer.orderedByZ.map { it.zOrder }
        assertEquals(zs.sorted(), zs)
        assertEquals(zs.toSet().size, zs.size) // sin colisiones de zOrder
    }

    @Test
    fun `hay huecos entre capas (contrato no atado al ordinal)`() {
        // Cada salto es mayor que 1: imposible que el zOrder coincida con el ordinal
        // (0,1,2,3...). Prueba que el orden NO deriva del ordinal.
        val zs = RenderLayer.orderedByZ.map { it.zOrder }
        zs.zipWithNext { a, b -> assertTrue(b - a > 1, "Se espera hueco entre capas: $a→$b") }
    }

    @Test
    fun `zOrder es independiente del ordinal`() {
        // Salvo la capa de fondo (0), ningún zOrder coincide con su ordinal.
        RenderLayer.entries.forEach { layer ->
            if (layer != RenderLayer.Board) {
                assertTrue(
                    layer.zOrder != layer.ordinal,
                    "El zOrder de $layer no debe igualar su ordinal (${layer.ordinal})",
                )
            }
        }
    }

    @Test
    fun `estabilidad del contrato de zOrder`() {
        // Fija los valores del contrato: cambiarlos es una decisión consciente que
        // debe romper este test a propósito.
        assertEquals(0, RenderLayer.Board.zOrder)
        assertEquals(100, RenderLayer.Cards.zOrder)
        assertEquals(200, RenderLayer.Flight.zOrder)
        assertEquals(300, RenderLayer.Particles.zOrder)
        assertEquals(400, RenderLayer.Effects.zOrder)
        assertEquals(500, RenderLayer.Overlay.zOrder)
    }
}
