package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.mineralord.tcg.core.animation.AnimationDefinition
import com.mineralord.tcg.core.animation.AnimationId
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * Prueba de INTEGRACIÓN de extremo a extremo del framework de animaciones.
 *
 * Recorre el pipeline COMPLETO sin saltarse pasos, entrando por la única API pública
 * (`AnimationDirector.submit`) con una request de dominio real:
 *
 *   Feature → AnimationDirector.submit(PokemonPlayed)
 *     → DefaultAnimationScheduler → DefaultAnimationQueue
 *     → StateReportingRunner → ComposeAnimationRunner
 *     → AnimationDefinitionRegistry → AnimationDefinition(Move)
 *     → AnimationPlayer → MoveExecutor
 *     → AnimationRenderState  (lo que AnimationStage/MoveNodeRenderer dibujarían)
 *
 * El tiempo se conduce con [BroadcastFrameClock]; [Dispatchers.Unconfined] hace que cada
 * `sendFrame` reanude el ejecutor de forma síncrona. No hay executor llamado a mano: la
 * receta de `PokemonPlayed` produce un `Move`, y el resto del sistema lo lleva hasta el
 * estado de render.
 */
class PipelineIntegrationTest {

    /** Ranuras de tablero que existirían en la UI real (rastreadas por `trackBounds`). */
    private val hand = SlotId("hand")
    private val active = SlotId("active")

    /**
     * Contribuidor de dominio: la receta de "un Pokémon entra en juego" = mover la carta
     * de la mano al activo. Es lo único que una feature añadiría sobre el framework.
     */
    private fun contributor() = com.mineralord.tcg.core.animationcompose.AnimationDefinitionContributor { builder ->
        builder.register(AnimationRequest.PokemonPlayed::class) { request ->
            AnimationDefinition(
                id = AnimationId("pokemon-played:${request.pokemonId}"),
                name = "PokemonPlayed",
                steps = listOf(
                    AnimationStep.Move(
                        fromSlotId = hand.value,
                        toSlotId = active.value,
                        duration = 100.milliseconds,
                    ),
                ),
            )
        }
    }

    @Test
    fun `una request de dominio recorre todo el pipeline hasta el estado de render y se limpia`() {
        // --- Composition root del test: cablea el framework REAL de punta a punta. ---
        val clock = BroadcastFrameClock()
        val scope = CoroutineScope(Dispatchers.Unconfined + clock)

        val coordinates = CoordinateRegistry().apply {
            place(hand, SlotBounds(Offset(0f, 0f), IntSize(60, 90)))
            place(active, SlotBounds(Offset(200f, 400f), IntSize(60, 90)))
        }
        val renderState = AnimationRenderState()

        val executors = AnimationStepExecutorRegistry().apply {
            register(AnimationStep.Move::class, MoveExecutor(coordinates, renderState))
        }
        val definitions = AnimationDefinitionRegistry.from(listOf(contributor()))
        val runner = ComposeAnimationRunner.create(definitions, executors, scope)
        val director = DefaultAnimationDirector.create(runner = runner, scope = scope)

        // Punto de partida: nada reproduciéndose, nada dibujándose.
        assertEquals(AnimationState.Idle, director.state.value)
        assertEquals(0, renderState.size)

        // --- Feature entra por la fachada con una request de dominio. ---
        director.submit(AnimationRequest.PokemonPlayed(playerId = "p1", pokemonId = "pikachu"))

        // Tras submit (Unconfined drena de inmediato): Scheduler recibió, Queue almacenó y
        // drenó, Runner resolvió la Definition, Player interpretó el árbol y el Executor ya
        // creó el nodo de vuelo (progress 0) en la capa Flight — lo que el Stage dibujaría.
        assertTrue(director.state.value.isPlaying, "el director debe estar reproduciendo")
        val startNode = renderState.nodesOf(RenderLayer.Flight).single() as MoveRenderNode
        assertEquals(0f, startNode.progress)
        assertEquals(Offset(0f, 0f), startNode.origin.topLeft)
        assertEquals(Offset(200f, 400f), startNode.destination.topLeft)
        // Fidelidad del renderer: en progress 0 dibujaría exactamente en el origen.
        assertEquals(Offset(0f, 0f), startNode.interpolatedBounds().topLeft)

        // --- Avanza el reloj: el RenderState se actualiza frame a frame. ---
        clock.sendFrame(0L) // establece la base temporal
        clock.sendFrame(50_000_000L) // 50 ms → mitad del recorrido
        val midNode = renderState.nodesOf(RenderLayer.Flight).single() as MoveRenderNode
        assertEquals(0.5f, midNode.progress, 0.0001f)
        assertEquals(Offset(100f, 200f), midNode.interpolatedBounds().topLeft) // punto medio real

        // --- Último frame: el nodo llega, se completa y DESAPARECE. ---
        clock.sendFrame(100_000_000L) // 100 ms → progress 1 → break → finally remove

        assertEquals(0, renderState.size, "el nodo debe desaparecer al finalizar")
        assertTrue(renderState.nodesOf(RenderLayer.Flight).isEmpty())
        // Y el estado del framework vuelve a Idle (StateReportingRunner cerró el ciclo).
        assertEquals(AnimationState.Idle, director.state.value)
    }
}
