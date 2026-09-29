package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.events.GameEvent
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
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Cobertura de la Fase 1 (daño puro) de **Brecha Paradójica** (`sv4`): daño por monedas
 * y bonos condicionales "X+". Cartas sintéticas cuyo `Attack.effect` apunta a las claves
 * reales registradas en [EffectsDb] por `registerParadoxRift()`.
 */
class SetParadoxEffectsTest {

    private class FixedRng(private val heads: Boolean) : Rng {
        override fun flipCoin(): Boolean = heads
        override fun <T> shuffle(list: List<T>): List<T> = list
        override fun nextInt(untilExclusive: Int): Int = 0
    }

    private fun art() = ArtworkRefs(null, null, "s", "l")
    private fun set() = SetInfo("test", LocalizedText("Test", "Test"), "Test")

    private fun mon(id: String, hp: Int, type: EnergyType, attack: Attack, stage: Stage = Stage.Basic) = PokemonCard(
        id = CardId(id), name = LocalizedText(id, id), set = set(),
        rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
        stage = stage, mechanic = PokemonMechanic.Normal, hp = hp,
        types = listOf(type), evolvesFrom = null, abilities = emptyList(),
        attacks = listOf(attack), weaknesses = emptyList(),
        resistances = emptyList(), retreatCost = emptyList(), rulesText = emptyList(),
    )

    private fun energy(id: String, type: EnergyType) = BasicEnergy(
        CardId(id), LocalizedText("Energía", "Energy"), set(), Rarity.COMMON, null, art(), type,
    )

    private fun attack(name: String, effect: EffectId) =
        Attack(LocalizedText(name, name), emptyList(), 0, DamageModel.Variable, effect)

    private fun duel(mine: PokemonInPlay, foe: PokemonInPlay): GameState {
        val player = PlayerState(
            side = Side.PLAYER, active = mine,
            deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) },
            prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = foe,
            deck = (1..5).map { energy("od$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("opz$it", EnergyType.WATER) },
        )
        return GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
    }

    private fun firstDamage(r: EngineResult): Int =
        r.events.filterIsInstance<GameEvent.DamageDealt>().firstOrNull()?.amount ?: 0

    @Test
    fun `Cross-Cut hace +60 contra un Pokemon de Evolucion`() {
        val a = attack("Cross-Cut", EffectsDb.atkKey("sv4-118", "Cross-Cut"))
        val attacker = PokemonInPlay(mon("cut", 120, EnergyType.FIGHTING, a))
        val basic = PokemonInPlay(mon("basic", 200, EnergyType.WATER, a, stage = Stage.Basic))
        val evo = PokemonInPlay(mon("evo", 200, EnergyType.WATER, a, stage = Stage.Stage2))

        val vsBasic = GameEngine(SeededRng(1)).apply(duel(attacker, basic), GameIntent.Attack("Cross-Cut"))
        assertEquals(30, firstDamage(vsBasic))
        val vsEvo = GameEngine(SeededRng(1)).apply(duel(attacker, evo), GameIntent.Attack("Cross-Cut"))
        assertEquals(90, firstDamage(vsEvo))
    }

    @Test
    fun `Ice Shard hace +30 contra tipo Lucha`() {
        val a = attack("Ice Shard", EffectsDb.atkKey("sv4-37", "Ice Shard"))
        val attacker = PokemonInPlay(mon("ice", 90, EnergyType.WATER, a))
        val fight = PokemonInPlay(mon("fight", 200, EnergyType.FIGHTING, a))
        val water = PokemonInPlay(mon("wat", 200, EnergyType.WATER, a))

        assertEquals(40, firstDamage(GameEngine(SeededRng(1)).apply(duel(attacker, fight), GameIntent.Attack("Ice Shard"))))
        assertEquals(10, firstDamage(GameEngine(SeededRng(1)).apply(duel(attacker, water), GameIntent.Attack("Ice Shard"))))
    }

    @Test
    fun `Triple Spin hace 10 por cada cara (3 caras = 30)`() {
        val a = attack("Triple Spin", EffectsDb.atkKey("sv4-1", "Triple Spin"))
        val attacker = PokemonInPlay(mon("surskit", 60, EnergyType.GRASS, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIRE, a))

        val allHeads = GameEngine(FixedRng(true)).apply(duel(attacker, foe), GameIntent.Attack("Triple Spin"))
        assertEquals(30, firstDamage(allHeads))
    }
}
