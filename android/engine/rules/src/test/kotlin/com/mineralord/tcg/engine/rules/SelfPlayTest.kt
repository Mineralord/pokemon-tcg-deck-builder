package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Rarity
import com.mineralord.tcg.engine.model.SetInfo
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Stage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelfPlayTest {

    private fun art() = ArtworkRefs(null, null, "s", "l")
    private fun set() = SetInfo("t", LocalizedText("T", "T"), "T")

    private fun attacker(id: String) = PokemonCard(
        id = CardId(id), name = LocalizedText(id, id), set = set(),
        rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
        stage = Stage.Basic, mechanic = PokemonMechanic.Normal, hp = 60,
        types = listOf(EnergyType.LIGHTNING), evolvesFrom = null, abilities = emptyList(),
        attacks = listOf(
            Attack(LocalizedText("Golpe", "Hit"), listOf(EnergyType.LIGHTNING), 1, Damage.Fixed(30), null),
        ),
        weaknesses = emptyList(), resistances = emptyList(), retreatCost = emptyList(), rulesText = emptyList(),
    )

    private fun energy(id: String) =
        BasicEnergy(CardId(id), LocalizedText("Energía", "Energy"), set(), Rarity.COMMON, null, art(), EnergyType.LIGHTNING)

    /** Mazo: 8 atacantes Básicos + 12 energías. */
    private fun deck(prefix: String): List<Card> =
        (1..8).map { attacker("$prefix-p$it") } + (1..12).map { energy("$prefix-e$it") }

    private fun engine(seed: Long) = GameEngine(SeededRng(seed))

    @Test
    fun `cien partidas IA vs IA terminan solas con ganador`() {
        var clean = 0
        repeat(100) { i ->
            val seed = 1000L + i
            val state = GameSetup.start(deck("A"), deck("B"), SeededRng(seed))
            val harness = SelfPlay(
                engine(seed),
                mapOf(Side.PLAYER to GreedyAgent(), Side.OPPONENT to GreedyAgent()),
            )
            val result = harness.play(state)
            assertTrue(result.finishedCleanly, "La partida con semilla $seed no terminó sola (turnos=${result.turns})")
            assertTrue(result.winner != null, "La partida con semilla $seed terminó sin ganador")
            assertTrue(result.totalEvents > 0)
            clean++
        }
        assertEquals(100, clean)
    }

    @Test
    fun `el motor es determinista- misma semilla, mismo desenlace`() {
        fun run(seed: Long): SelfPlayResult {
            val state = GameSetup.start(deck("A"), deck("B"), SeededRng(seed))
            return SelfPlay(
                engine(seed),
                mapOf(Side.PLAYER to GreedyAgent(), Side.OPPONENT to GreedyAgent()),
            ).play(state)
        }
        val a = run(777L)
        val b = run(777L)
        assertEquals(a.winner, b.winner)
        assertEquals(a.turns, b.turns)
        assertEquals(a.totalEvents, b.totalEvents)
    }
}
