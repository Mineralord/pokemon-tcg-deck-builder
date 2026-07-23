package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animation.AnimationState
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.DefaultAnimationDirector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integración de la categoría EVOLUCIÓN por el pipeline REAL compartido (mismos contribuidores y
 * ejecutores que usará el juego): `CanonicalAnimationContributors` + `canonicalStepExecutors`.
 *
 * No hay wiring específico del test: se entra por `AnimationDirector.submit(Evolved)` y se verifica
 * que el ejecutor real crea, avanza y limpia el [EvolveRenderNode] en la ranura del Activo.
 */
class EvolvePipelineTest {

    private val player = "studio"

    private fun director(coordinates: CoordinateRegistry, renderState: AnimationRenderState, scope: CoroutineScope) =
        DefaultAnimationDirector.create(
            runner = ComposeAnimationRunner.create(
                AnimationDefinitionRegistry.from(CanonicalAnimationContributors),
                canonicalStepExecutors(coordinates, renderState),
                scope,
            ),
            scope = scope,
        )

    @Test
    fun `Evolved recorre el pipeline real hasta el nodo de evolucion y se limpia`() {
        val clock = BroadcastFrameClock()
        val scope = CoroutineScope(Dispatchers.Unconfined + clock)

        val coordinates = CoordinateRegistry().apply {
            place(activeSlotId(player), SlotBounds(Offset(100f, 200f), IntSize(60, 90)))
        }
        val renderState = AnimationRenderState()
        val director = director(coordinates, renderState, scope)

        assertEquals(AnimationState.Idle, director.state.value)
        assertEquals(0, renderState.size)

        director.submit(AnimationRequest.Evolved(player, "pikachu", EvolveVariants.EVO_003))

        assertTrue(director.state.value.isPlaying, "el director debe estar reproduciendo")
        val node = renderState.nodesOf(RenderLayer.Flight).single() as EvolveRenderNode
        assertEquals(0f, node.progress)
        assertEquals(45f, node.visual.hue) // huella de EVO_003 (Radiant Ascension, oro)

        clock.sendFrame(0L)
        clock.sendFrame(950_000_000L) // duración de EVO_003 → progress 1 → finally remove

        assertEquals(0, renderState.size, "el nodo debe desaparecer al finalizar")
        assertEquals(AnimationState.Idle, director.state.value)
    }

    @Test
    fun `las cinco variantes resuelven a una definicion con un unico paso Evolve distinto`() {
        val registry = AnimationDefinitionRegistry.from(CanonicalAnimationContributors)
        val visuals = EvolveVariants.all.map { id ->
            val def = registry.resolve(AnimationRequest.Evolved(player, "mon", id))
            assertNotNull(def, "la variante $id debe resolver a una definición")
            val step = def.steps.single() as AnimationStep.Evolve
            step.visual
        }
        // Las cinco variantes son visualmente distintas (sin duplicados).
        assertEquals(5, visuals.toSet().size, "las cinco variantes deben ser distintas entre sí")
    }
}
