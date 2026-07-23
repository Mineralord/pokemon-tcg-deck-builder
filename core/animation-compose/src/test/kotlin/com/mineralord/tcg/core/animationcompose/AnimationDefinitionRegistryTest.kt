package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.AnimationDefinition
import com.mineralord.tcg.core.animation.AnimationId
import com.mineralord.tcg.core.animation.AnimationRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnimationDefinitionRegistryTest {

    private fun def(name: String) = AnimationDefinition(id = AnimationId(name), name = name)

    @Test
    fun `resuelve la definicion registrada usando datos de la request`() {
        val registry = AnimationDefinitionRegistry.from(
            listOf(
                AnimationDefinitionContributor { b ->
                    b.register(AnimationRequest.CardDrawn::class) { req -> def("draw-${req.cardId}") }
                },
            ),
        )

        val resolved = registry.resolve(AnimationRequest.CardDrawn(playerId = "p", cardId = "pikachu"))
        assertEquals("draw-pikachu", resolved?.name)
    }

    @Test
    fun `request sin receta resuelve a null`() {
        val registry = AnimationDefinitionRegistry.from(emptyList())
        assertNull(registry.resolve(AnimationRequest.CardDrawn("p", "x")))
    }

    @Test
    fun `fusion distribuida de varios contribuidores`() {
        val battle = AnimationDefinitionContributor { b ->
            b.register(AnimationRequest.AttackStarted::class) { def("attack") }
        }
        val cards = AnimationDefinitionContributor { b ->
            b.register(AnimationRequest.CardDrawn::class) { def("draw") }
        }

        val registry = AnimationDefinitionRegistry.from(listOf(battle, cards))

        assertEquals(2, registry.size)
        assertTrue(registry.contains(AnimationRequest.AttackStarted::class))
        assertTrue(registry.contains(AnimationRequest.CardDrawn::class))
    }

    @Test
    fun `colision de dos contribuidores falla ruidosamente`() {
        val a = AnimationDefinitionContributor { b ->
            b.register(AnimationRequest.CardDrawn::class) { def("a") }
        }
        val b = AnimationDefinitionContributor { builder ->
            builder.register(AnimationRequest.CardDrawn::class) { def("b") }
        }

        val error = assertFailsWith<IllegalArgumentException> {
            AnimationDefinitionRegistry.from(listOf(a, b))
        }
        assertTrue(error.message!!.contains("duplicada"))
    }
}
