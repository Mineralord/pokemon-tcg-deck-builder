package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.AnimationDefinition
import com.mineralord.tcg.core.animation.AnimationHandle
import com.mineralord.tcg.core.animation.AnimationId
import com.mineralord.tcg.core.animation.AnimationRequest
import com.mineralord.tcg.core.animation.AnimationResult
import com.mineralord.tcg.core.animation.AnimationStepExecutorRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeAnimationRunnerTest {

    private fun runner(vararg contributors: AnimationDefinitionContributor): ComposeAnimationRunner =
        ComposeAnimationRunner.create(
            definitions = AnimationDefinitionRegistry.from(contributors.toList()),
            executors = AnimationStepExecutorRegistry(), // vacío en esta fase
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

    @Test
    fun `resuelve la definicion y la reproduce hasta completarse`() = runBlocking {
        val runner = runner(
            AnimationDefinitionContributor { b ->
                b.register(AnimationRequest.CardDrawn::class) {
                    AnimationDefinition(id = AnimationId("draw"), name = "draw") // sin pasos → completa
                }
            },
        )

        val handle = runner.start(AnimationRequest.CardDrawn("p", "c"))
        assertEquals(AnimationResult.Completed, handle.await())
    }

    @Test
    fun `request sin receta devuelve un handle ya finalizado`() = runBlocking {
        val handle = runner().start(AnimationRequest.PokemonKnockedOut("pk"))

        assertEquals(AnimationHandle.RunningState.Finished, handle.state)
        assertEquals(AnimationResult.Completed, handle.await())
    }
}
