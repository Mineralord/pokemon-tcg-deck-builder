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

    @Test
    fun `Searing Flame deja Quemado al Activo rival`() {
        val a = attack("Searing Flame", EffectsDb.atkKey("sv4-20", "Searing Flame"))
        val attacker = PokemonInPlay(mon("fire", 90, EnergyType.FIRE, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.WATER, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Searing Flame"))
        assertEquals(true, r.state.opponent.active?.statuses?.contains(com.mineralord.tcg.engine.model.Status.BURNED))
    }

    @Test
    fun `Heat Tackle hace 30 de retroceso al atacante`() {
        val a = attack("Heat Tackle", EffectsDb.atkKey("sv4-21", "Heat Tackle"))
        val attacker = PokemonInPlay(mon("charc", 120, EnergyType.FIRE, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.WATER, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Heat Tackle"))
        assertEquals(30, r.state.player.active?.damage)
    }

    @Test
    fun `Alloyed Hammer hace +120 si tiene Energia Metalica unida`() {
        val a = attack("Alloyed Hammer", EffectsDb.atkKey("sv4-85", "Alloyed Hammer"))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.PSYCHIC, a))
        val sinMetal = PokemonInPlay(mon("m1", 150, EnergyType.METAL, a))
        val conMetal = PokemonInPlay(mon("m2", 150, EnergyType.METAL, a), attachedEnergy = listOf(energy("me", EnergyType.METAL)))
        assertEquals(60, firstDamage(GameEngine(SeededRng(1)).apply(duel(sinMetal, foe), GameIntent.Attack("Alloyed Hammer"))))
        assertEquals(180, firstDamage(GameEngine(SeededRng(1)).apply(duel(conMetal, foe), GameIntent.Attack("Alloyed Hammer"))))
    }

    @Test
    fun `Unhinged Scissors hace +160 si el atacante tiene Condicion Especial`() {
        val a = attack("Unhinged Scissors", EffectsDb.atkKey("sv4-105", "Unhinged Scissors"))
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.PSYCHIC, a))
        val sano = PokemonInPlay(mon("s1", 150, EnergyType.WATER, a))
        val envenenado = PokemonInPlay(mon("s2", 150, EnergyType.WATER, a), statuses = setOf(com.mineralord.tcg.engine.model.Status.POISONED))
        assertEquals(30, firstDamage(GameEngine(SeededRng(1)).apply(duel(sano, foe), GameIntent.Attack("Unhinged Scissors"))))
        assertEquals(190, firstDamage(GameEngine(SeededRng(1)).apply(duel(envenenado, foe), GameIntent.Attack("Unhinged Scissors"))))
    }

    @Test
    fun `Leech Seed cura 10 al atacante`() {
        val a = attack("Leech Seed", EffectsDb.atkKey("sv4-4", "Leech Seed"))
        val attacker = PokemonInPlay(mon("seed", 90, EnergyType.GRASS, a), damage = 30)
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.WATER, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Leech Seed"))
        assertEquals(20, r.state.player.active?.damage)
    }

    @Test
    fun `Crushing Blow descarta 1 Energia del Activo rival`() {
        val a = attack("Crushing Blow", EffectsDb.atkKey("sv4-85", "Crushing Blow"))
        val attacker = PokemonInPlay(mon("crush", 120, EnergyType.METAL, a))
        val foe = PokemonInPlay(
            mon("foe", 200, EnergyType.WATER, a),
            attachedEnergy = listOf(energy("e1", EnergyType.WATER), energy("e2", EnergyType.WATER)),
        )
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Crushing Blow"))
        assertEquals(1, r.state.opponent.active?.attachedEnergy?.size)
    }

    @Test
    fun `Boundless Power impide atacar al propio Pokemon el proximo turno`() {
        val a = attack("Boundless Power", EffectsDb.atkKey("sv4-83", "Boundless Power"))
        val attacker = PokemonInPlay(mon("bound", 120, EnergyType.FIGHTING, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.PSYCHIC, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Boundless Power"))
        assertEquals(true, r.state.player.active?.cannotAttackOnTurn != null)
    }

    @Test
    fun `Scorching Bazooka hace +40 por cada Energia Fuego unida (base 40 + 2 = 120)`() {
        val a = attack("Scorching Bazooka", EffectsDb.atkKey("sv4-27", "Scorching Bazooka"))
        val fire2 = listOf(energy("f1", EnergyType.FIRE), energy("f2", EnergyType.FIRE))
        val attacker = PokemonInPlay(mon("bazo", 150, EnergyType.FIRE, a), attachedEnergy = fire2)
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Scorching Bazooka"))
        // El bono por Energía se aplica como daño adicional; comprobamos el TOTAL sobre el defensor.
        assertEquals(120, r.state.opponent.active?.damage)
    }
}
