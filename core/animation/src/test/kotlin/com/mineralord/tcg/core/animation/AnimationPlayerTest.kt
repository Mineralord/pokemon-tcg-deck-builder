package com.mineralord.tcg.core.animation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifica el RECORRIDO del árbol (no animaciones reales). Registra un ejecutor de
 * hoja que sólo anota su ejecución, y comprueba que Sequence/Parallel se interpretan
 * como se espera.
 */
class AnimationPlayerTest {

    /** Ejecutor de prueba: registra el texto de cada ShowText que ejecuta. */
    private class Recorder : AnimationStepExecutor<AnimationStep.ShowText> {
        val log = CopyOnWriteArrayList<String>()
        override suspend fun execute(step: AnimationStep.ShowText, context: AnimationExecutionContext) {
            log += step.text
        }
    }

    private fun leaf(text: String) = AnimationStep.ShowText(targetId = "t", text = text)

    @Test
    fun `sequence ejecuta las hojas en orden`() = runBlocking {
        val recorder = Recorder()
        val registry = AnimationStepExecutorRegistry().apply {
            register(AnimationStep.ShowText::class, recorder)
        }
        val player = AnimationPlayer(registry, CoroutineScope(Dispatchers.Unconfined))

        val def = AnimationDefinition(
            id = AnimationId("seq"),
            name = "seq",
            steps = listOf(AnimationStep.Sequence(listOf(leaf("a"), leaf("b"), leaf("c")))),
        )

        val result = player.play(def).await()

        assertEquals(AnimationResult.Completed, result)
        assertEquals(listOf("a", "b", "c"), recorder.log.toList())
    }

    @Test
    fun `parallel espera a todos los hijos`() = runBlocking {
        val recorder = Recorder()
        val registry = AnimationStepExecutorRegistry().apply {
            register(AnimationStep.ShowText::class, recorder)
        }
        val player = AnimationPlayer(registry, CoroutineScope(Dispatchers.Default))

        val def = AnimationDefinition(
            id = AnimationId("par"),
            name = "par",
            steps = listOf(AnimationStep.Parallel(listOf(leaf("x"), leaf("y"), leaf("z")))),
        )

        val result = player.play(def).await()

        assertEquals(AnimationResult.Completed, result)
        assertEquals(setOf("x", "y", "z"), recorder.log.toSet())
    }

    @Test
    fun `hoja sin ejecutor registrado hace no-op y completa`() = runBlocking {
        val player = AnimationPlayer(AnimationStepExecutorRegistry(), CoroutineScope(Dispatchers.Unconfined))
        val def = AnimationDefinition(
            id = AnimationId("noop"),
            name = "noop",
            steps = listOf(leaf("sin-ejecutor")),
        )

        assertEquals(AnimationResult.Completed, player.play(def).await())
    }

    @Test
    fun `arbol anidado sequence-parallel se recorre completo`() = runBlocking {
        val recorder = Recorder()
        val registry = AnimationStepExecutorRegistry().apply {
            register(AnimationStep.ShowText::class, recorder)
        }
        val player = AnimationPlayer(registry, CoroutineScope(Dispatchers.Default))

        val def = AnimationDefinition(
            id = AnimationId("nested"),
            name = "nested",
            steps = listOf(
                AnimationStep.Sequence(
                    listOf(
                        leaf("1"),
                        AnimationStep.Parallel(listOf(leaf("2a"), leaf("2b"))),
                        leaf("3"),
                    ),
                ),
            ),
        )

        player.play(def).await()

        val log = recorder.log.toList()
        assertEquals(4, log.size)
        assertEquals("1", log.first())
        assertEquals("3", log.last())
        assertTrue(log.subList(1, 3).toSet() == setOf("2a", "2b"))
    }
}
