package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.PokemonCard
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class AiDeckFactoryTest {

    private val repo = CardRepository.load()

    @Test
    fun `genera mazos legales de 60 cartas con al menos un basico`() {
        repeat(30) { seed ->
            val deck = AiDeckFactory.buildRandomDeck(repo.all, rng = Random(seed.toLong()))
            val total = deck.sumOf { it.count }
            assertTrue(total == 60, "El mazo IA (seed=$seed) tiene $total cartas, no 60")

            // Regla de 4 (la energía básica no cuenta).
            deck.forEach { e ->
                val card = repo[e.cardId]
                if (card != null && card !is com.mineralord.tcg.engine.model.BasicEnergy) {
                    assertTrue(e.count <= 4, "${e.cardId.raw} aparece ${e.count} veces (>4)")
                }
            }

            // Al menos un Pokémon Básico para poder abrir mano.
            val hasBasic = deck.any { e ->
                (repo[e.cardId] as? PokemonCard)?.isBasic == true
            }
            assertTrue(hasBasic, "El mazo IA (seed=$seed) no tiene ningún Básico")
        }
    }

    @Test
    fun `distintas semillas producen mazos distintos (variedad)`() {
        fun signature(seed: Long): Set<String> =
            AiDeckFactory.buildRandomDeck(repo.all, rng = Random(seed))
                .map { it.cardId.raw }.toSet()

        val signatures = (0L until 12L).map { signature(it) }.toSet()
        // Con un pool amplio, 12 semillas deben dar varias barajas diferentes.
        assertTrue(signatures.size >= 4, "Poca variedad: solo ${signatures.size} mazos distintos de 12")
    }
}
