package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.mineralord.tcg.core.animation.AnimationExecutionContext
import com.mineralord.tcg.core.animation.AnimationId
import com.mineralord.tcg.core.animation.AnimationStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Prueba vertical del ejecutor real: valida crear → actualizar → eliminar el nodo de
 * vuelo conforme avanza el reloj de frames. El tiempo se conduce con
 * [BroadcastFrameClock] (sin depender de coroutines-test); [Dispatchers.Unconfined]
 * hace que cada `sendFrame` reanude el ejecutor de forma síncrona hasta su siguiente
 * `withFrameNanos`.
 */
class MoveExecutorTest {

    private fun coordsWith(vararg slots: Pair<String, Offset>) = CoordinateRegistry().apply {
        slots.forEach { (id, pos) -> place(SlotId(id), SlotBounds(pos, IntSize(10, 10))) }
    }

    private fun moveStep() = AnimationStep.Move("a", "b", 100.milliseconds)
    private val ctx = AnimationExecutionContext(AnimationId("test"))

    @Test
    fun `crea, actualiza y elimina el nodo de vuelo`() = runBlocking {
        val coords = coordsWith("a" to Offset(0f, 0f), "b" to Offset(100f, 100f))
        val state = AnimationRenderState()
        val clock = BroadcastFrameClock()

        val job = launch(Dispatchers.Unconfined + clock) {
            MoveExecutor(coords, state).execute(moveStep(), ctx)
        }

        // Frame 0 → progress 0: nodo creado en la capa Flight.
        clock.sendFrame(0L)
        val atStart = state.nodesOf(RenderLayer.Flight).single() as MoveRenderNode
        assertEquals(1, state.size)
        assertEquals(0f, atStart.progress)
        assertEquals(Offset(0f, 0f), atStart.origin.topLeft)
        assertEquals(Offset(100f, 100f), atStart.destination.topLeft)

        // Frame a 50 ms → progress 0.5 (trayectoria lineal).
        clock.sendFrame(50_000_000L)
        val mid = state.nodesOf(RenderLayer.Flight).single() as MoveRenderNode
        assertEquals(0.5f, mid.progress, 0.0001f)

        // Frame a 100 ms → progress 1 → rompe el bucle → finally elimina el nodo.
        clock.sendFrame(100_000_000L)
        assertEquals(0, state.size)
        assertTrue(state.nodesOf(RenderLayer.Flight).isEmpty())

        job.join()
    }

    @Test
    fun `ranura ausente hace no-op (no crea nodo)`() = runBlocking {
        // Sólo existe "a"; el destino "b" no está registrado.
        val coords = coordsWith("a" to Offset(0f, 0f))
        val state = AnimationRenderState()

        MoveExecutor(coords, state).execute(moveStep(), ctx)

        assertEquals(0, state.size)
    }

    @Test
    fun `cancelacion elimina el nodo (limpieza en finally)`() = runBlocking {
        val coords = coordsWith("a" to Offset(0f, 0f), "b" to Offset(100f, 100f))
        val state = AnimationRenderState()
        val clock = BroadcastFrameClock()

        val job = launch(Dispatchers.Unconfined + clock) {
            MoveExecutor(coords, state).execute(moveStep(), ctx)
        }
        clock.sendFrame(0L) // nodo vivo a medio vuelo
        assertEquals(1, state.size)

        job.cancel()
        job.join()

        assertEquals(0, state.size) // finally limpió pese a la cancelación
    }
}
