package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage as DamageModel
import com.mineralord.tcg.engine.model.EffectId
import com.mineralord.tcg.engine.model.EffectsDb
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.Phase
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Rarity
import com.mineralord.tcg.engine.model.SetInfo
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Stage
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegalMovesAndAgentTest {

    private val engine = GameEngine(SeededRng(99))
    private fun art() = ArtworkRefs(null, null, "s", "l")
    private fun set() = SetInfo("test", LocalizedText("Test", "Test"), "Test")

    private fun atk(name: String, cost: Int = 0, dmg: Int = 0, effect: EffectId? = null) =
        Attack(LocalizedText(name, name), List(cost) { EnergyType.PSYCHIC }, cost, DamageModel.Fixed(dmg), effect)

    private fun mon(id: String, hp: Int = 100, attacks: List<Attack> = listOf(atk("x"))) = PokemonCard(
        CardId(id), LocalizedText(id, id), set(), Rarity.COMMON, "H", art(),
        Stage.Basic, PokemonMechanic.Normal, hp, listOf(EnergyType.PSYCHIC),
        null, emptyList(), attacks, emptyList(), emptyList(), emptyList(), emptyList(),
    )

    private fun energy(id: String) = BasicEnergy(
        CardId(id), LocalizedText("Energía", "Energy"), set(), Rarity.COMMON, null, art(), EnergyType.PSYCHIC,
    )

    private fun trainer(id: String, kind: TrainerKind) = TrainerCard(
        CardId(id), LocalizedText(id, id), set(), Rarity.COMMON, "H", art(),
        kind, LocalizedText("t", "t"), EffectId(id),
    )

    @Test
    fun `legalIntents enumera ataque, banca, energia, entrenador y fin`() {
        val attacker = PokemonInPlay(mon("act", attacks = listOf(atk("Golpe", cost = 1, dmg = 20))),
            attachedEnergy = listOf(energy("att-e")))
        val player = PlayerState(
            Side.PLAYER, active = attacker,
            hand = listOf(mon("basicoEnMano"), energy("manoE"), trainer("sv1-180", TrainerKind.Supporter())),
            deck = listOf(energy("d1")), prizes = (1..6).map { energy("p$it") },
        )
        val opp = PlayerState(Side.OPPONENT, active = PokemonInPlay(mon("opp")), deck = listOf(energy("od")))
        val state = GameState(player, opp, 2, Side.PLAYER, Phase.MAIN)

        val legal = engine.legalIntents(state)
        assertTrue(legal.any { it is GameIntent.Attack })
        assertTrue(legal.any { it is GameIntent.PlayBasicToBench })
        assertTrue(legal.any { it is GameIntent.AttachEnergy })
        assertTrue(legal.any { it is GameIntent.PlayTrainer && it.card == CardId("sv1-180") })
        assertTrue(legal.any { it is GameIntent.EndTurn })
    }

    @Test
    fun `con energia ya unida no se ofrece AttachEnergy`() {
        val player = PlayerState(
            Side.PLAYER, active = PokemonInPlay(mon("act")),
            hand = listOf(energy("manoE")), deck = listOf(energy("d1")), prizes = (1..6).map { energy("p$it") },
        )
        val opp = PlayerState(Side.OPPONENT, active = PokemonInPlay(mon("opp")), deck = listOf(energy("od")))
        val state = GameState(player, opp, 2, Side.PLAYER, Phase.MAIN, energyAttachedThisTurn = true)
        assertFalse(engine.legalIntents(state).any { it is GameIntent.AttachEnergy })
    }

    @Test
    fun `con decision pendiente legalIntents es vacio`() {
        val player = PlayerState(Side.PLAYER, active = PokemonInPlay(mon("act")), deck = listOf(energy("d1")))
        val opp = PlayerState(Side.OPPONENT, active = PokemonInPlay(mon("opp")), deck = listOf(energy("od")))
        // Forzamos un estado "esperando decisión" jugando un ataque con elección (abajo lo probamos bien);
        // aquí basta con un interaction simulado vía el flujo real.
        val zap = PokemonInPlay(
            mon("sv3pt5-145", attacks = listOf(atk("Multishot Lightning", effect = EffectsDb.atkKey("sv3pt5-145", "Multishot Lightning")))),
        )
        val p2 = player.copy(active = zap, prizes = (1..6).map { energy("p$it") })
        val o2 = opp.copy(active = PokemonInPlay(mon("tgt", hp = 200)), bench = listOf(PokemonInPlay(mon("b1"))))
        val res = engine.apply(GameState(p2, o2, 2, Side.PLAYER, Phase.MAIN), GameIntent.Attack("Multishot Lightning"))
        assertTrue(res.state.awaitingDecision)
        assertTrue(engine.legalIntents(res.state).isEmpty())
    }

    @Test
    fun `solo una energia por turno`() {
        val player = PlayerState(
            Side.PLAYER, active = PokemonInPlay(mon("act")),
            hand = listOf(energy("e1"), energy("e2")), deck = listOf(energy("d1")), prizes = (1..6).map { energy("p$it") },
        )
        val opp = PlayerState(Side.OPPONENT, active = PokemonInPlay(mon("opp")), deck = listOf(energy("od")), prizes = (1..6).map { energy("q$it") })
        val state = GameState(player, opp, 2, Side.PLAYER, Phase.MAIN)

        val first = engine.apply(state, GameIntent.AttachEnergy(CardId("e1"), CardId("act")))
        assertTrue(first.accepted, first.rejection)
        assertTrue(first.state.energyAttachedThisTurn)
        val second = engine.apply(first.state, GameIntent.AttachEnergy(CardId("e2"), CardId("act")))
        assertFalse(second.accepted)
    }

    @Test
    fun `SmartAgent resuelve la decision de un ataque con eleccion`() {
        val zap = PokemonInPlay(
            mon("sv3pt5-145", attacks = listOf(atk("Multishot Lightning", effect = EffectsDb.atkKey("sv3pt5-145", "Multishot Lightning")))),
        )
        val player = PlayerState(Side.PLAYER, active = zap, deck = listOf(energy("d1")), prizes = (1..6).map { energy("p$it") })
        val opp = PlayerState(
            Side.OPPONENT, active = PokemonInPlay(mon("tgt", hp = 200)),
            bench = listOf(PokemonInPlay(mon("b1", hp = 200))),
            deck = listOf(energy("od")), prizes = (1..6).map { energy("q$it") },
        )
        val state = GameState(player, opp, 2, Side.PLAYER, Phase.MAIN)

        // 1) Ataque deja la decisión pendiente (no termina el turno).
        val attacked = engine.apply(state, GameIntent.Attack("Multishot Lightning"))
        assertTrue(attacked.state.awaitingDecision)
        assertEquals(Side.PLAYER, attacked.state.activeSide)

        // 2) La IA resuelve eligiendo el banco; se aplica el daño y termina el turno.
        val agent = SmartAgent(engine)
        val decision = agent.decide(attacked.state, Side.PLAYER)
        assertTrue(decision is GameIntent.ResolveDecision)
        val resolved = engine.apply(attacked.state, decision)
        assertTrue(resolved.accepted, resolved.rejection)
        assertEquals(90, resolved.state.opponent.bench.first { it.card.id == CardId("b1") }.damage)
        assertEquals(Side.OPPONENT, resolved.state.activeSide)   // atacar terminó el turno al resolver
    }
}
