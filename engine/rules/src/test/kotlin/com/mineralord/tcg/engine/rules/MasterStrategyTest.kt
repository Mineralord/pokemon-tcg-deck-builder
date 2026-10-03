package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage as DamageModel
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.PendingInteraction
import com.mineralord.tcg.engine.model.Phase
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Rarity
import com.mineralord.tcg.engine.model.SetInfo
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Stage
import com.mineralord.tcg.engine.model.TypeModifier
import com.mineralord.tcg.engine.model.withId
import com.mineralord.tcg.engine.model.Card
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests de COMPORTAMIENTO de la IA Master Ball ([SmartAgent] con [Difficulty.MASTERBALL]). Construyen
 * estados mínimos y comprueban que [SmartAgent.decide] toma la decisión experta esperada: Noqueo
 * barato, planificación a 2 turnos, promoción de un atacante listo y objetivo de mayor valor en premios.
 */
class MasterStrategyTest {
    private fun art() = ArtworkRefs(null, null, "s", "l")
    private fun set() = SetInfo("test", LocalizedText("Test", "Test"), "Test")

    private fun atk(name: String, cost: Int, dmg: Int) =
        Attack(LocalizedText(name, name), List(cost) { EnergyType.PSYCHIC }, cost, DamageModel.Fixed(dmg), null)

    private fun mon(
        id: String,
        hp: Int,
        attacks: List<Attack>,
        mechanic: PokemonMechanic = PokemonMechanic.Normal,
        weaknessTo: EnergyType? = null,
    ) = PokemonCard(
        CardId(id), LocalizedText(id, id), set(), Rarity.COMMON, "H", art(),
        Stage.Basic, mechanic, hp, listOf(EnergyType.PSYCHIC), null, emptyList(), attacks,
        weaknessTo?.let { listOf(TypeModifier(it, "×2")) } ?: emptyList(),
        emptyList(), emptyList(), emptyList(),
    )

    private fun energies(n: Int) = List(n) {
        BasicEnergy(CardId("e$it"), LocalizedText("E", "E"), set(), Rarity.COMMON, null, art(), EnergyType.PSYCHIC)
    }

    private fun pip(card: PokemonCard, energy: Int = 0, damage: Int = 0) =
        PokemonInPlay(card = card, damage = damage, attachedEnergy = energies(energy), turnsInPlay = 1)

    private fun masterAgent(): Pair<GameEngine, SmartAgent> {
        val engine = GameEngine(SeededRng(1))
        return engine to SmartAgent(engine, Difficulty.MASTERBALL)
    }

    /** Estado MAIN (turno 3) con la IA (OPPONENT) al ataque; manos/Banca vacías → solo ataques legales. */
    private fun attackState(aiActive: PokemonInPlay, playerActive: PokemonInPlay) = GameState(
        player = PlayerState(Side.PLAYER, active = playerActive),
        opponent = PlayerState(Side.OPPONENT, active = aiActive),
        turn = 3, activeSide = Side.OPPONENT, phase = Phase.MAIN,
        energyAttachedThisTurn = true,
    )

    @Test
    fun `elige el Noqueo mas barato cuando varios ataques son letales`() {
        val (_, agent) = masterAgent()
        val ai = pip(mon("AI", 140, listOf(atk("Jab", 1, 40), atk("Crush", 3, 200))), energy = 3)
        val def = pip(mon("DEF", 30, listOf(atk("x", 1, 10)))) // 30 PS: ambos lo Noquean
        val intent = agent.decide(attackState(ai, def), Side.OPPONENT)
        assertEquals(GameIntent.Attack("Jab"), intent, "Debe rematar con el ataque letal MÁS barato")
    }

    @Test
    fun `sin Noqueo inmediato, a igual dano elige el ataque mas barato (plan a 2 turnos)`() {
        val (_, agent) = masterAgent()
        // Dos ataques de 70 (no letales vs 120 PS): deja al rival rematable el próximo turno; ahorra energía.
        val ai = pip(mon("AI", 140, listOf(atk("Big", 3, 70), atk("Small", 1, 70))), energy = 3)
        val def = pip(mon("DEF", 120, listOf(atk("x", 1, 10))))
        val intent = agent.decide(attackState(ai, def), Side.OPPONENT)
        assertEquals(GameIntent.Attack("Small"), intent, "A igual daño debe elegir el ataque más barato")
    }

    @Test
    fun `elige el ataque de mayor dano efectivo`() {
        val (_, agent) = masterAgent()
        val ai = pip(mon("AI", 140, listOf(atk("Weak", 2, 50), atk("Strong", 2, 90))), energy = 2)
        val def = pip(mon("DEF", 300, listOf(atk("x", 1, 10)))) // muy bulky: ninguno letal
        val intent = agent.decide(attackState(ai, def), Side.OPPONENT)
        assertEquals(GameIntent.Attack("Strong"), intent, "Debe pegar con el de mayor daño efectivo")
    }

    @Test
    fun `tras un KO promueve un atacante LISTO antes que un muro sin energia`() {
        val (_, agent) = masterAgent()
        val ready = pip(mon("READY", 60, listOf(atk("Hit", 1, 50))), energy = 1)   // puede atacar YA
        val wall = pip(mon("WALL", 200, listOf(atk("Hit", 1, 50))), energy = 0)    // enorme pero sin energía
        val state = GameState(
            player = PlayerState(Side.PLAYER, active = pip(mon("PA", 100, listOf(atk("x", 1, 30))), energy = 1)),
            opponent = PlayerState(Side.OPPONENT, active = null, bench = listOf(wall, ready)),
            turn = 5, activeSide = Side.PLAYER, phase = Phase.MAIN,
            pendingPromotion = setOf(Side.OPPONENT),
        )
        val intent = agent.decide(state, Side.OPPONENT)
        assertEquals(GameIntent.PromoteActive(ready.card.id), intent, "Debe promover al atacante listo")
    }

    @Test
    fun `al elegir objetivo, a igual PS prioriza el de MAYOR valor en premios`() {
        val (_, agent) = masterAgent()
        val single = pip(mon("SINGLE", 60, listOf(atk("x", 1, 10))))                       // 1 premio
        val exMon = pip(mon("EX", 60, listOf(atk("x", 1, 10)), PokemonMechanic.ExLower))   // 2 premios
        val state = GameState(
            player = PlayerState(Side.PLAYER, active = pip(mon("PA", 100, listOf(atk("x", 1, 10)))),
                bench = listOf(single, exMon)),
            opponent = PlayerState(Side.OPPONENT, active = pip(mon("OA", 100, listOf(atk("x", 1, 10))), energy = 1)),
            turn = 3, activeSide = Side.OPPONENT, phase = Phase.MAIN,
            interaction = PendingInteraction(
                decision = PendingDecision.ChooseTargets(
                    side = Side.OPPONENT,
                    prompt = LocalizedText("Elige", "Choose"),
                    candidates = listOf(single.card.id, exMon.card.id),
                    count = 1,
                ),
                remainingOps = emptyList(),
                side = Side.OPPONENT,
                sourceId = null,
            ),
        )
        val intent = agent.decide(state, Side.OPPONENT)
        assertEquals(
            GameIntent.ResolveDecision(listOf(exMon.card.id)), intent,
            "A igual PS debe apuntar al Pokémon que da más premios",
        )
    }

    private fun chooseTargetsState(
        candidates: List<CardId>,
        aiActive: PokemonInPlay,
        aiBench: List<PokemonInPlay> = emptyList(),
        playerBench: List<PokemonInPlay> = emptyList(),
    ) = GameState(
        player = PlayerState(Side.PLAYER, active = pip(mon("PA", 100, listOf(atk("x", 1, 10)))), bench = playerBench),
        opponent = PlayerState(Side.OPPONENT, active = aiActive, bench = aiBench),
        turn = 3, activeSide = Side.OPPONENT, phase = Phase.MAIN,
        interaction = PendingInteraction(
            decision = PendingDecision.ChooseTargets(
                Side.OPPONENT, LocalizedText("Elige", "Choose"), candidates, 1,
            ),
            remainingOps = emptyList(), side = Side.OPPONENT, sourceId = null,
        ),
    )

    @Test
    fun `elegir objetivo PROPIO (cambio-curacion) escoge el MEJOR atacante, no el peor`() {
        val (_, agent) = masterAgent()
        val weak = pip(mon("W", 40, listOf(atk("t", 1, 10))), energy = 0)    // sin energía: 0 daño
        val strong = pip(mon("S", 120, listOf(atk("t", 1, 80))), energy = 1) // atacante real
        val state = chooseTargetsState(
            candidates = listOf(weak.card.id, strong.card.id),
            aiActive = pip(mon("OA", 100, listOf(atk("x", 1, 10)))),
            aiBench = listOf(weak, strong),
        )
        val intent = agent.decide(state, Side.OPPONENT)
        assertEquals(
            GameIntent.ResolveDecision(listOf(strong.card.id)), intent,
            "Un objetivo PROPIO debe ser nuestro mejor atacante, no el de menos PS",
        )
    }

    @Test
    fun `gust (objetivo RIVAL) arrastra el linchpin NOQUEABLE de mas premios`() {
        val (_, agent) = masterAgent()
        val tank = pip(mon("TANK", 200, listOf(atk("x", 1, 10))))                       // no Noqueable
        val exMon = pip(mon("EX", 50, listOf(atk("x", 1, 10)), PokemonMechanic.ExLower)) // Noqueable, 2 premios
        val state = chooseTargetsState(
            candidates = listOf(tank.card.id, exMon.card.id),
            aiActive = pip(mon("OA", 140, listOf(atk("Hit", 1, 60))), energy = 1),       // pega 60
            playerBench = listOf(tank, exMon),
        )
        val intent = agent.decide(state, Side.OPPONENT)
        assertEquals(
            GameIntent.ResolveDecision(listOf(exMon.card.id)), intent,
            "El gust debe arrastrar al rival que podemos Noquear y da más premios",
        )
    }

    @Test
    fun `la busqueda TERMINA el turno (sin bucle de retirada con coste 0)`() {
        // Atacante con COSTE DE RETIRADA 0: antes la búsqueda ciclaba retirándose sin fin. La partida
        // Master vs Master debe avanzar y terminar dentro de un número acotado de pasos.
        fun freeRetreatMon(id: String) = PokemonCard(
            CardId(id), LocalizedText(id, id), set(), Rarity.COMMON, "H", art(),
            Stage.Basic, PokemonMechanic.Normal, 70, listOf(EnergyType.PSYCHIC),
            null, emptyList(), listOf(atk("Tap", 0, 20), atk("Blast", 2, 90)),
            emptyList(), emptyList(), emptyList(), emptyList(), // retiro 0
        )
        fun deck(p: String): List<Card> {
            val c = ArrayList<Card>()
            repeat(4) { c += freeRetreatMon("$p-m$it") }
            repeat(30) { c += energies(1).first().copy(id = CardId("$p-e$it")) }
            return c.mapIndexed { i, x -> x.withId(x.id.withInstance(i)) }
        }
        val engine = GameEngine(SeededRng(7))
        val agent = SmartAgent(engine, Difficulty.MASTERBALL)
        var state = GameSetup.start(deck("P"), deck("O"), SeededRng(7))
        var steps = 0
        while (!state.isOver && steps++ < 4000) {
            val pend = state.pendingPromotion.firstOrNull()
            if (pend != null) {
                val b = state.sideState(pend).bench.firstOrNull() ?: break
                val r = engine.apply(state, GameIntent.PromoteActive(b.card.id))
                state = if (r.accepted) r.state else break
                continue
            }
            val r = engine.apply(state, agent.decide(state, state.activeSide))
            state = if (r.accepted) r.state else engine.apply(state, GameIntent.EndTurn).state
        }
        assertTrue(state.isOver, "La partida Master vs Master debe terminar (no quedar en bucle); pasos=$steps")
    }
}
