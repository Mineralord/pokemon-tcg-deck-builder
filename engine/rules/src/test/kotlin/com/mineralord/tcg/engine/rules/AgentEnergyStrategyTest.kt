package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage as DamageModel
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Rarity
import com.mineralord.tcg.engine.model.SetInfo
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Stage
import com.mineralord.tcg.engine.model.withId
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Comprueba que la IA Master Ball POTENCIA a su atacante: aunque tenga un ataque gratis, invierte
 * la energía en el Activo hasta poder usar su ataque FUERTE (antes un bug la mandaba a la Banca).
 */
class AgentEnergyStrategyTest {
    private fun art() = ArtworkRefs(null, null, "s", "l")
    private fun set() = SetInfo("test", LocalizedText("Test", "Test"), "Test")

    private fun atk(name: String, cost: Int, dmg: Int) =
        Attack(LocalizedText(name, name), List(cost) { EnergyType.PSYCHIC }, cost, DamageModel.Fixed(dmg), null)

    private fun attacker(id: String) = PokemonCard(
        CardId(id), LocalizedText(id, id), set(), Rarity.COMMON, "H", art(),
        Stage.Basic, PokemonMechanic.Normal, 140, listOf(EnergyType.PSYCHIC),
        null, emptyList(),
        // Ataque GRATIS flojo + ataque CARO fuerte: la IA debe cargar energía para el fuerte.
        listOf(atk("Tap", 0, 10), atk("Blast", 3, 120)),
        emptyList(), emptyList(), emptyList(), emptyList(),
    )

    private fun energy(id: String) = BasicEnergy(
        CardId(id), LocalizedText("Energía", "Energy"), set(), Rarity.COMMON, null, art(), EnergyType.PSYCHIC,
    )

    private fun deck(prefix: String): List<Card> {
        val cards = ArrayList<Card>()
        repeat(3) { cards += attacker("$prefix-mon$it") }
        repeat(30) { cards += energy("$prefix-e$it") }
        return cards.mapIndexed { i, c -> c.withId(c.id.withInstance(i)) }
    }

    @Test
    fun `la IA carga energia en el Activo para habilitar su ataque fuerte`() {
        val engine = GameEngine(SeededRng(3))
        val agent = SmartAgent(engine, Difficulty.MASTERBALL)
        var state = GameSetup.start(deck("P"), deck("O"), SeededRng(3))

        var steps = 0
        var maxOppActiveEnergy = 0
        while (!state.isOver && steps++ < 600 && state.turn < 12) {
            val pend = state.pendingPromotion.firstOrNull()
            if (pend != null) {
                val b = state.sideState(pend).bench.firstOrNull() ?: break
                val r = engine.apply(state, GameIntent.PromoteActive(b.card.id))
                state = if (r.accepted) r.state else break
                continue
            }
            val side = state.activeSide
            val r = engine.apply(state, agent.decide(state, side))
            if (r.accepted) {
                state = r.state
            } else {
                val rec = if (state.interaction?.side == side)
                    engine.apply(state, GameIntent.ResolveDecision(emptyList())) else null
                val endTurn = engine.apply(state, GameIntent.EndTurn)
                state = when {
                    rec?.accepted == true -> rec.state
                    endTurn.accepted -> endTurn.state
                    else -> break
                }
            }
            maxOppActiveEnergy = maxOf(maxOppActiveEnergy, state.opponent.active?.attachedEnergyCount ?: 0)
        }
        // El Activo de la IA debió acumular ≥3 energías (para su ataque "Blast"), no dejarlas sueltas.
        assertTrue(maxOppActiveEnergy >= 3, "El Activo de la IA solo llegó a $maxOppActiveEnergy energías (esperado ≥3)")
    }
}
