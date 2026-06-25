package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.events.GameEvent
import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage as DamageModel
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
import com.mineralord.tcg.engine.model.Status
import com.mineralord.tcg.engine.model.TypeModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameEngineTest {

    private val engine = GameEngine(SeededRng(42))
    private fun art() = ArtworkRefs(null, null, "s", "l")
    private fun set() = SetInfo("test", LocalizedText("Test", "Test"), "Test")

    private fun mon(
        id: String, hp: Int, type: EnergyType,
        attack: Attack, weakness: EnergyType? = null,
    ) = PokemonCard(
        id = CardId(id), name = LocalizedText(id, id), set = set(),
        rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
        stage = Stage.Basic, mechanic = PokemonMechanic.Normal, hp = hp,
        types = listOf(type), evolvesFrom = null, abilities = emptyList(),
        attacks = listOf(attack),
        weaknesses = weakness?.let { listOf(TypeModifier(it, "×2")) } ?: emptyList(),
        resistances = emptyList(), retreatCost = emptyList(), rulesText = emptyList(),
    )

    private fun basicEnergy(id: String, type: EnergyType) = BasicEnergy(
        CardId(id), LocalizedText("Energía", "Energy"), set(),
        Rarity.COMMON, null, art(), type,
    )

    private fun withEnergy(card: PokemonCard, n: Int, type: EnergyType) =
        PokemonInPlay(card, attachedEnergy = (1..n).map { basicEnergy("e$it-${card.id.raw}", type) })

    @Test
    fun `atacar aplica debilidad, noquea, toma premios y promociona la banca`() {
        val bolt = Attack(LocalizedText("Rayo", "Bolt"), listOf(EnergyType.LIGHTNING), 1, DamageModel.Fixed(60), null)
        val pikachu = mon("pika", 60, EnergyType.LIGHTNING, bolt)
        // Defensor débil al rayo (×2): 60 -> 120, con 70 HP queda noqueado.
        val tackle = Attack(LocalizedText("Placaje", "Tackle"), emptyList(), 0, DamageModel.Fixed(10), null)
        val wooper = mon("wooper", 70, EnergyType.WATER, tackle, weakness = EnergyType.LIGHTNING)
        val benchMon = mon("magikarp", 30, EnergyType.WATER, tackle)

        val player = PlayerState(
            side = Side.PLAYER,
            active = withEnergy(pikachu, 1, EnergyType.LIGHTNING),
            deck = listOf(basicEnergy("draw1", EnergyType.LIGHTNING)),
            prizes = (1..6).map { basicEnergy("pz$it", EnergyType.LIGHTNING) },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT,
            active = PokemonInPlay(wooper),
            bench = listOf(PokemonInPlay(benchMon)),
            deck = listOf(basicEnergy("odraw", EnergyType.WATER)),
            prizes = (1..6).map { basicEnergy("opz$it", EnergyType.WATER) },
        )
        val state = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val result = engine.apply(state, GameIntent.Attack("Rayo"))
        assertTrue(result.accepted, result.rejection)

        val ev = result.events
        // Daño con debilidad.
        val dmg = ev.filterIsInstance<GameEvent.DamageDealt>().first()
        assertEquals(120, dmg.amount)
        assertTrue(dmg.weaknessApplied)
        // KO + premio.
        assertTrue(ev.any { it is GameEvent.KnockedOut })
        assertEquals(1, ev.filterIsInstance<GameEvent.PrizeTaken>().first().count)
        // El atacante bajó de 6 a 5 premios restantes.
        assertEquals(5, result.state.player.prizesRemaining)
        // El rival promocionó a su banca (magikarp pasa a Activo).
        assertEquals(CardId("magikarp"), result.state.opponent.active?.card?.id)
        assertTrue(result.state.opponent.bench.isEmpty())
        // Atacar terminó el turno: ahora juega el rival y robó.
        assertEquals(Side.OPPONENT, result.state.activeSide)
        assertTrue(ev.any { it is GameEvent.TurnStarted && it.side == Side.OPPONENT })
        assertFalse(result.state.isOver)
    }

    @Test
    fun `quedarse sin premios gana la partida`() {
        val bolt = Attack(LocalizedText("Rayo", "Bolt"), emptyList(), 0, DamageModel.Fixed(200), null)
        val attacker = mon("atk", 100, EnergyType.LIGHTNING, bolt)
        val victim = mon("vic", 60, EnergyType.WATER, bolt)
        val player = PlayerState(
            side = Side.PLAYER, active = PokemonInPlay(attacker),
            deck = listOf(basicEnergy("d", EnergyType.LIGHTNING)),
            prizes = listOf(basicEnergy("last", EnergyType.LIGHTNING)),  // 1 premio restante
            prizesRemaining = 1,
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = PokemonInPlay(victim),
            deck = emptyList(),
            prizes = (1..6).map { basicEnergy("opz$it", EnergyType.WATER) },
        )
        val state = GameState(player, opponent, 5, Side.PLAYER, Phase.MAIN)

        val result = engine.apply(state, GameIntent.Attack("Rayo"))
        assertTrue(result.accepted, result.rejection)
        assertEquals(Side.PLAYER, result.state.winner)
        assertTrue(result.state.isOver)
        assertTrue(result.events.any { it is GameEvent.GameWon && it.side == Side.PLAYER })
    }

    @Test
    fun `no se puede atacar sin energia suficiente`() {
        val costly = Attack(LocalizedText("Caro", "Costly"), listOf(EnergyType.FIRE, EnergyType.FIRE), 2, DamageModel.Fixed(90), null)
        val mon = mon("m", 100, EnergyType.FIRE, costly)
        val player = PlayerState(Side.PLAYER, active = PokemonInPlay(mon), deck = listOf(basicEnergy("d", EnergyType.FIRE)), prizes = (1..6).map { basicEnergy("p$it", EnergyType.FIRE) })
        val opp = PlayerState(Side.OPPONENT, active = PokemonInPlay(mon.copy(id = CardId("m2"))), deck = listOf(basicEnergy("d2", EnergyType.FIRE)), prizes = (1..6).map { basicEnergy("q$it", EnergyType.FIRE) })
        val state = GameState(player, opp, 2, Side.PLAYER, Phase.MAIN)

        val result = engine.apply(state, GameIntent.Attack("Caro"))
        assertFalse(result.accepted)
        assertEquals(state, result.state)   // estado intacto
    }

    @Test
    fun `ataque con efecto aplica estado al activo rival`() {
        // El efecto de Heat Wave (Ninetales ex) está registrado en EffectsDb.
        val heatWave = Attack(
            LocalizedText("Onda Ígnea", "Heat Wave"), emptyList(), 0, DamageModel.Fixed(10),
            EffectsDb.atkKey("sv3pt5-38", "Heat Wave"),
        )
        val ninetales = mon("sv3pt5-38", 100, EnergyType.FIRE, heatWave)
        val dummy = Attack(LocalizedText("x", "x"), emptyList(), 0, DamageModel.None, null)
        val target = mon("tgt", 120, EnergyType.WATER, dummy)
        val player = PlayerState(
            Side.PLAYER, active = PokemonInPlay(ninetales),
            deck = listOf(basicEnergy("d", EnergyType.FIRE)), prizes = (1..6).map { basicEnergy("p$it", EnergyType.FIRE) },
        )
        val opp = PlayerState(
            Side.OPPONENT, active = PokemonInPlay(target),
            deck = listOf(basicEnergy("od", EnergyType.WATER)), prizes = (1..6).map { basicEnergy("q$it", EnergyType.WATER) },
        )
        val state = GameState(player, opp, 2, Side.PLAYER, Phase.MAIN)

        val result = engine.apply(state, GameIntent.Attack("Heat Wave"))
        assertTrue(result.accepted, result.rejection)
        assertTrue(result.state.opponent.active?.statuses?.contains(Status.BURNED) == true)
    }

    @Test
    fun `ataque con dano extra suma por cada banca rival`() {
        val mindJack = Attack(
            LocalizedText("Asalto Mental", "Mind Jack"), emptyList(), 0, DamageModel.Fixed(0),
            EffectsDb.atkKey("sv3pt5-65", "Mind Jack"),
        )
        val alakazam = mon("sv3pt5-65", 100, EnergyType.PSYCHIC, mindJack)
        val dummy = Attack(LocalizedText("x", "x"), emptyList(), 0, DamageModel.None, null)
        val target = mon("tgt", 200, EnergyType.DARKNESS, dummy)
        val player = PlayerState(
            Side.PLAYER, active = PokemonInPlay(alakazam),
            deck = listOf(basicEnergy("d", EnergyType.PSYCHIC)), prizes = (1..6).map { basicEnergy("p$it", EnergyType.PSYCHIC) },
        )
        val opp = PlayerState(
            Side.OPPONENT, active = PokemonInPlay(target),
            bench = listOf(PokemonInPlay(mon("bA", 60, EnergyType.WATER, dummy)), PokemonInPlay(mon("bB", 60, EnergyType.WATER, dummy))),
            deck = listOf(basicEnergy("od", EnergyType.DARKNESS)), prizes = (1..6).map { basicEnergy("q$it", EnergyType.DARKNESS) },
        )
        val state = GameState(player, opp, 2, Side.PLAYER, Phase.MAIN)

        val result = engine.apply(state, GameIntent.Attack("Mind Jack"))
        assertTrue(result.accepted, result.rejection)
        // 0 base + 30×2 de banca rival = 60 sobre el Activo rival.
        assertEquals(60, result.state.opponent.active?.damage)
    }

    @Test
    fun `veneno inflige dano al final del turno`() {
        val poke = mon("p", 100, EnergyType.GRASS, Attack(LocalizedText("x", "x"), emptyList(), 0, DamageModel.None, null))
        val poisoned = PokemonInPlay(poke, statuses = setOf(com.mineralord.tcg.engine.model.Status.POISONED))
        val player = PlayerState(Side.PLAYER, active = poisoned, deck = listOf(basicEnergy("d", EnergyType.GRASS)), prizes = (1..6).map { basicEnergy("p$it", EnergyType.GRASS) })
        val opp = PlayerState(Side.OPPONENT, active = PokemonInPlay(poke.copy(id = CardId("o"))), deck = listOf(basicEnergy("od", EnergyType.GRASS)), prizes = (1..6).map { basicEnergy("q$it", EnergyType.GRASS) })
        val state = GameState(player, opp, 1, Side.PLAYER, Phase.MAIN)

        val result = engine.apply(state, GameIntent.EndTurn)
        assertTrue(result.accepted)
        assertEquals(GameEngine.POISON_DAMAGE, result.state.player.active?.damage)
    }
}
