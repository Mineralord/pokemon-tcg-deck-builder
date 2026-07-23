package com.mineralord.tcg.core.animation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultAnimationQueueTest {

    /**
     * `AnimationRequest` es sellado; se usan subtipos reales cuyo `priority` conocido
     * mapea al nivel deseado, codificando un id en un campo para poder afirmar orden.
     * (No hay request con prioridad Low en el modelo actual, así que se cubren
     * Critical/High/Normal, suficiente para validar orden entre niveles y estabilidad.)
     */
    private fun req(id: String, priority: AnimationPriority): AnimationRequest = when (priority) {
        AnimationPriority.Critical -> AnimationRequest.PokemonKnockedOut(pokemonId = id)
        AnimationPriority.High -> AnimationRequest.AttackStarted(attackerId = id, attackName = id)
        AnimationPriority.Normal -> AnimationRequest.CardDrawn(playerId = "p", cardId = id)
        AnimationPriority.Low -> error("Ningún AnimationRequest mapea a Low en el modelo actual")
    }

    private val supportedPriorities =
        listOf(AnimationPriority.Critical, AnimationPriority.High, AnimationPriority.Normal)

    private fun idOf(request: AnimationRequest): String = when (request) {
        is AnimationRequest.PokemonKnockedOut -> request.pokemonId
        is AnimationRequest.AttackStarted -> request.attackerId
        is AnimationRequest.CardDrawn -> request.cardId
        else -> request.toString()
    }

    private fun drainIds(queue: AnimationQueue): List<String> = buildList {
        while (true) add(idOf(queue.dequeue() ?: break))
    }

    @Test
    fun `FIFO dentro de la misma prioridad`() {
        val q = DefaultAnimationQueue()
        q.enqueue(req("A", AnimationPriority.High))
        q.enqueue(req("B", AnimationPriority.High))
        q.enqueue(req("C", AnimationPriority.High))

        assertEquals(listOf("A", "B", "C"), drainIds(q))
    }

    @Test
    fun `mayor prioridad sale primero`() {
        val q = DefaultAnimationQueue()
        q.enqueue(req("normal", AnimationPriority.Normal))
        q.enqueue(req("crit", AnimationPriority.Critical))
        q.enqueue(req("high", AnimationPriority.High))

        assertEquals(listOf("crit", "high", "normal"), drainIds(q))
    }

    @Test
    fun `estabilidad entre niveles mezclados`() {
        val q = DefaultAnimationQueue()
        q.enqueue(req("H1", AnimationPriority.High))
        q.enqueue(req("N1", AnimationPriority.Normal))
        q.enqueue(req("H2", AnimationPriority.High))
        q.enqueue(req("N2", AnimationPriority.Normal))
        q.enqueue(req("H3", AnimationPriority.High))

        assertEquals(listOf("H1", "H2", "H3", "N1", "N2"), drainIds(q))
    }

    @Test
    fun `peek no extrae`() {
        val q = DefaultAnimationQueue()
        q.enqueue(req("A", AnimationPriority.Normal))
        q.enqueue(req("B", AnimationPriority.Critical))

        assertEquals("B", idOf(q.peek()!!))
        assertEquals("B", idOf(q.peek()!!)) // idempotente
        assertEquals(2, q.size)
    }

    @Test
    fun `vaciado deja la cola vacia`() {
        val q = DefaultAnimationQueue()
        repeat(5) { q.enqueue(req("x$it", AnimationPriority.Normal)) }
        assertEquals(5, q.size)

        q.clear()

        assertTrue(q.isEmpty)
        assertEquals(0, q.size)
        assertNull(q.dequeue())
        assertNull(q.peek())
    }

    @Test
    fun `thread-safe bajo carga concurrente no pierde solicitudes`() = runBlocking {
        val q = DefaultAnimationQueue()
        val producers = 8
        val perProducer = 1000

        coroutineScope {
            repeat(producers) { p ->
                launch(Dispatchers.Default) {
                    repeat(perProducer) { i ->
                        q.enqueue(req("p$p-$i", supportedPriorities[i % supportedPriorities.size]))
                    }
                }
            }
        }

        assertEquals(producers * perProducer, q.size)

        val collected = coroutineScope {
            (1..producers).map {
                async(Dispatchers.Default) {
                    buildList { while (true) add(idOf(q.dequeue() ?: break)) }
                }
            }.awaitAll().flatten()
        }

        assertEquals(producers * perProducer, collected.size)
        assertEquals(producers * perProducer, collected.toSet().size) // sin duplicados
        assertTrue(q.isEmpty)
    }

    @Test
    fun `FIFO por productor se mantiene bajo concurrencia`() = runBlocking {
        val q = DefaultAnimationQueue()
        val producers = 6
        val perProducer = 500

        coroutineScope {
            repeat(producers) { p ->
                launch(Dispatchers.Default) {
                    repeat(perProducer) { i -> q.enqueue(req("p$p-$i", AnimationPriority.High)) }
                }
            }
        }

        val ids = drainIds(q)
        repeat(producers) { p ->
            val own = ids.filter { it.startsWith("p$p-") }.map { it.substringAfter('-').toInt() }
            assertEquals((0 until perProducer).toList(), own)
        }
    }
}
