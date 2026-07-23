package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animation.AnimationState
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.AnimationStepExecutorRegistry
import com.mineralord.tcg.core.animation.DefaultAnimationDirector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integración E2E de la ANIMACIÓN #001: `submit(CardDrawn)` recorre todo el pipeline v1.0
 * (Director→Scheduler→Queue→Runner→Registry→Definition→Player→DrawCardExecutor→RenderState)
 * usando la receta real [DrawCardAnimations]. Verifica creación, avance con arco/overshoot
 * y limpieza. Reloj conducido con [BroadcastFrameClock].
 */
class DrawCardPipelineTest {

    private val playerId = "p1"

    @Test
    fun `robar una carta recorre el pipeline y limpia el nodo`() {
        val clock = BroadcastFrameClock()
        val scope = CoroutineScope(Dispatchers.Unconfined + clock)

        val coordinates = CoordinateRegistry().apply {
            place(deckSlotId(playerId), SlotBounds(Offset(0f, 1000f), IntSize(60, 90)))
            place(handSlotId(playerId), SlotBounds(Offset(300f, 1800f), IntSize(60, 90)))
        }
        val renderState = AnimationRenderState()
        val executors = AnimationStepExecutorRegistry().apply {
            register(AnimationStep.DrawCard::class, DrawCardExecutor(coordinates, renderState))
        }
        val definitions = AnimationDefinitionRegistry.from(listOf(DrawCardAnimations))
        val runner = ComposeAnimationRunner.create(definitions, executors, scope)
        val director = DefaultAnimationDirector.create(runner = runner, scope = scope)

        assertEquals(AnimationState.Idle, director.state.value)
        assertEquals(0, renderState.size)

        // Feature entra por la fachada con la request de dominio.
        director.submit(AnimationRequest.CardDrawn(playerId = playerId, cardId = "pikachu"))

        // Nodo de robo creado en la capa Flight (progress 0), listo para que el Stage lo dibuje.
        assertTrue(director.state.value.isPlaying)
        val start = renderState.nodesOf(RenderLayer.Flight).single() as DrawCardRenderNode
        assertEquals(0f, start.progress)
        assertEquals(deckCenter(coordinates), start.center()) // arranca en el mazo

        // A mitad de recorrido (160 ms de 320): el nodo está en vuelo, describiendo un arco.
        clock.sendFrame(0L)
        clock.sendFrame(160_000_000L)
        val mid = renderState.nodesOf(RenderLayer.Flight).single() as DrawCardRenderNode
        assertTrue(mid.progress > 0f && mid.progress < 1.2f)
        // El centro en vuelo está ELEVADO respecto a la recta mazo→mano (trayectoria física).
        assertTrue(mid.center().y < straightLineY(coordinates, mid.progress) + 0.01f)

        // Fin (320 ms): progress lineal llega a 1 → break → finally elimina el nodo.
        clock.sendFrame(320_000_000L)
        assertEquals(0, renderState.size, "el nodo debe desaparecer al terminar el robo")
        assertEquals(AnimationState.Idle, director.state.value)
    }

    private fun deckCenter(c: CoordinateRegistry): Offset =
        c.bounds(deckSlotId(playerId))!!.toRect().center

    /** Y de la recta origen→destino en un `t` dado (para comprobar que el arco eleva). */
    private fun straightLineY(c: CoordinateRegistry, t: Float): Float {
        val a = c.bounds(deckSlotId(playerId))!!.toRect().center
        val b = c.bounds(handSlotId(playerId))!!.toRect().center
        return a.y + (b.y - a.y) * t
    }
}
