package com.mineralord.tcg.engine.effects

import com.mineralord.tcg.engine.events.GameEvent
import com.mineralord.tcg.engine.model.Amount
import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.Effect
import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.PendingDecision
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
import com.mineralord.tcg.engine.model.Target
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EffectInterpreterTest {

    private val interp = EffectInterpreter()
    private fun art() = ArtworkRefs(null, null, "s", "l")
    private fun set() = SetInfo("t", LocalizedText("T", "T"), "T")
    private fun atk() = Attack(LocalizedText("x", "x"), emptyList(), 0, Damage.None, null)

    private fun mon(id: String, hp: Int = 100) = PokemonCard(
        CardId(id), LocalizedText(id, id), set(), Rarity.COMMON, "H", art(),
        Stage.Basic, PokemonMechanic.Normal, hp, listOf(EnergyType.PSYCHIC),
        null, emptyList(), listOf(atk()), emptyList(), emptyList(), emptyList(), emptyList(),
    )

    private fun energy(id: String) = BasicEnergy(CardId(id), LocalizedText("E", "E"), set(), Rarity.COMMON, null, art(), EnergyType.PSYCHIC)
    private fun lenergy(id: String) = BasicEnergy(CardId(id), LocalizedText("E", "E"), set(), Rarity.COMMON, null, art(), EnergyType.LIGHTNING)
    private fun fenergy(id: String) = BasicEnergy(CardId(id), LocalizedText("E", "E"), set(), Rarity.COMMON, null, art(), EnergyType.FIRE)
    private fun lmon(id: String) = mon(id).copy(types = listOf(EnergyType.LIGHTNING))

    private fun state(): GameState {
        val player = PlayerState(
            side = Side.PLAYER,
            active = PokemonInPlay(mon("self"), damage = 50),
            bench = listOf(PokemonInPlay(mon("benchA"))),
            deck = (1..5).map { energy("d$it") },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT,
            active = PokemonInPlay(mon("oppActive")),
            bench = listOf(PokemonInPlay(mon("oppBenchA")), PokemonInPlay(mon("oppBenchB"))),
        )
        return GameState(player, opponent, 1, Side.PLAYER, Phase.MAIN)
    }

    private val src = EffectSource(Side.PLAYER, CardId("self"))

    @Test
    fun `dano a la banca rival se aplica a todos y emite eventos`() {
        val eff = Effect(ops = listOf(EffectOp.Damage(Target.OPP_BENCH, Amount.Fixed(30))))
        val r = interp.execute(eff, src, state())
        assertEquals(30, r.state.opponent.bench.first { it.card.id == CardId("oppBenchA") }.damage)
        assertEquals(30, r.state.opponent.bench.first { it.card.id == CardId("oppBenchB") }.damage)
        assertEquals(2, r.events.filterIsInstance<GameEvent.DamageDealt>().size)
    }

    @Test
    fun `curar al propio activo no baja de cero`() {
        val eff = Effect(ops = listOf(EffectOp.Heal(Target.OWN_ACTIVE, Amount.Fixed(80))))
        val r = interp.execute(eff, src, state())
        assertEquals(0, r.state.player.active?.damage)   // tenía 50, cura 80 -> 0
    }

    @Test
    fun `aplicar estado al activo rival`() {
        val eff = Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.POISONED, Status.CONFUSED))))
        val r = interp.execute(eff, src, state())
        assertEquals(setOf(Status.POISONED, Status.CONFUSED), r.state.opponent.active?.statuses)
    }

    @Test
    fun `dano por cada Pokemon en banca rival`() {
        // 2 en banca rival * 30 = 60 al activo rival.
        val eff = Effect(ops = listOf(EffectOp.Damage(Target.OPP_ACTIVE, Amount.PerCount(com.mineralord.tcg.engine.model.Counter.BENCH_COUNT, Target.OPP_BENCH, 30))))
        val r = interp.execute(eff, src, state())
        assertEquals(60, r.state.opponent.active?.damage)
    }

    @Test
    fun `robar mueve cartas del mazo a la mano`() {
        val eff = Effect(ops = listOf(EffectOp.DrawCards(3)))
        val r = interp.execute(eff, src, state())
        assertEquals(3, r.state.player.hand.size)
        assertEquals(2, r.state.player.deck.size)
        assertEquals(3, r.events.filterIsInstance<GameEvent.CardsDrawn>().first().count)
    }

    @Test
    fun `ChooseTarget no adivina, emite decision pendiente con candidatos`() {
        val eff = Effect(
            ops = listOf(
                EffectOp.ChooseTarget(Target.OPP_BENCH, howMany = 1, prompt = LocalizedText("Elige", "Choose")),
            ),
        )
        val r = interp.execute(eff, src, state())
        assertTrue(r.events.isEmpty())
        assertEquals(1, r.pending.size)
        val d = r.pending.first() as PendingDecision.ChooseTargets
        assertEquals(listOf(CardId("oppBenchA"), CardId("oppBenchB")), d.candidates)
        assertEquals(1, d.count)
    }

    @Test
    fun `execute pausa en la op de eleccion y guarda la continuacion`() {
        // Elige objetivo y luego cura: la pausa debe conservar el Heal pendiente.
        val eff = Effect(
            ops = listOf(
                EffectOp.ChooseTarget(Target.OWN_ALL, 1, LocalizedText("Elige", "Choose")),
                EffectOp.Heal(Target.CHOSEN, Amount.Fixed(30)),
            ),
        )
        val r = interp.execute(eff, src, state())
        val interaction = r.state.interaction
        assertTrue(interaction != null)
        assertEquals(1, interaction!!.remainingOps.size)
        assertTrue(interaction.remainingOps.first() is EffectOp.Heal)
    }

    @Test
    fun `resolve de ChooseTargets liga CHOSEN y ejecuta el Heal pendiente`() {
        val eff = Effect(
            ops = listOf(
                EffectOp.ChooseTarget(Target.OWN_ALL, 1, LocalizedText("Elige", "Choose")),
                EffectOp.Heal(Target.CHOSEN, Amount.Fixed(30)),
            ),
        )
        val paused = interp.execute(eff, src, state())
        // "self" tenía 50 de daño; curar 30 -> 20.
        val resolved = interp.resolve(paused.state, listOf(CardId("self"))) { it }
        assertNull(resolved.state.interaction)
        assertEquals(20, resolved.state.player.active?.damage)
    }

    @Test
    fun `resolve de SearchCards mueve la carta del mazo a la mano`() {
        val eff = Effect(
            ops = listOf(
                EffectOp.SearchDeck(
                    com.mineralord.tcg.engine.model.CardFilter(),
                    com.mineralord.tcg.engine.model.Zone.HAND, 1,
                ),
            ),
        )
        val paused = interp.execute(eff, src, state())
        assertTrue(paused.state.interaction!!.decision is PendingDecision.SearchCards)
        val resolved = interp.resolve(paused.state, listOf(CardId("d1"))) { it }
        assertNull(resolved.state.interaction)
        assertTrue(resolved.state.player.hand.any { it.id == CardId("d1") })
        assertFalse(resolved.state.player.deck.any { it.id == CardId("d1") })
    }

    @Test
    fun `Electric Generator revela top 5, pausa y une energias Rayo a la Banca`() {
        val player = PlayerState(
            side = Side.PLAYER,
            active = PokemonInPlay(mon("self")),
            bench = listOf(PokemonInPlay(lmon("benchL"))),
            // Top 5 = L1, p1, L2, x, p2  (L3deep queda por debajo del top 5).
            deck = listOf(lenergy("L1"), energy("p1"), lenergy("L2"), mon("x"), energy("p2"), lenergy("L3deep")),
        )
        val gs = GameState(player, PlayerState(Side.OPPONENT, PokemonInPlay(mon("oppA"))), 1, Side.PLAYER, Phase.MAIN)
        val eff = Effect(ops = listOf(EffectOp.RevealAttachEnergy(5, 2, EnergyType.LIGHTNING, EnergyType.LIGHTNING)))

        val paused = interp.execute(eff, EffectSource(Side.PLAYER, null), gs)
        val d = paused.state.interaction!!.decision as PendingDecision.AttachFromRevealed
        assertEquals(listOf(CardId("L1"), CardId("L2")), d.energyCandidates)
        assertEquals(listOf(CardId("benchL")), d.benchCandidates)

        // Une L1 y L2 al mismo Pokémon Rayo de la Banca (pares energía,destino).
        val resolved = interp.resolve(
            paused.state,
            listOf(CardId("L1"), CardId("benchL"), CardId("L2"), CardId("benchL")),
        ) { it }
        assertNull(resolved.state.interaction)
        val bench = resolved.state.player.bench.first { it.card.id == CardId("benchL") }
        assertEquals(2, bench.attachedEnergy.size)
        assertFalse(resolved.state.player.deck.any { it.id == CardId("L1") || it.id == CardId("L2") })
        assertTrue(resolved.state.player.deck.any { it.id == CardId("L3deep") })
        assertEquals(2, resolved.events.filterIsInstance<GameEvent.EnergyAttached>().size)
    }

    @Test
    fun `Boss's Orders sube al Activo rival el Pokemon elegido de su Banca`() {
        val eff = Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_BENCH, 1, LocalizedText("Elige", "Choose")),
            EffectOp.SwapOppActiveWithChosen,
        ))
        val paused = interp.execute(eff, EffectSource(Side.PLAYER, null), state())
        assertTrue(paused.state.interaction!!.decision is PendingDecision.ChooseTargets)
        // Sube "oppBenchB"; el Activo previo "oppActive" baja a la Banca.
        val resolved = interp.resolve(paused.state, listOf(CardId("oppBenchB"))) { it }
        assertNull(resolved.state.interaction)
        assertEquals(CardId("oppBenchB"), resolved.state.opponent.active?.card?.id)
        assertTrue(resolved.state.opponent.bench.any { it.card.id == CardId("oppActive") })
        assertFalse(resolved.state.opponent.bench.any { it.card.id == CardId("oppBenchB") })
    }

    @Test
    fun `Jet Wing marca al atacante para no atacar en su proximo turno`() {
        // Origen "self", turno 4 → su próximo turno propio es el 6 (turn+2).
        val gs = state().copy(turn = 4)
        val eff = Effect(ops = listOf(EffectOp.NoAttackNextTurn))
        val r = interp.execute(eff, src, gs)
        assertEquals(6, r.state.player.active?.cannotAttackOnTurn)
    }

    @Test
    fun `Concentrated Fire lanza una moneda por Energia Fuego y escala el dano`() {
        // Origen "self" con 2 Energía Fuego; flip=siempre cara → 2 caras × 80 = 160.
        val player = PlayerState(
            side = Side.PLAYER,
            active = PokemonInPlay(mon("self"), attachedEnergy = listOf(fenergy("f1"), fenergy("f2"))),
        )
        val gs = GameState(player, PlayerState(Side.OPPONENT, PokemonInPlay(mon("oppA"))), 2, Side.PLAYER, Phase.MAIN)
        val eff = Effect(ops = listOf(EffectOp.CoinsPerEnergyDamage(EnergyType.FIRE, 80)))
        val r = interp.execute(eff, src, gs, flip = { true })
        assertEquals(160, r.state.opponent.active?.damage)
        assertEquals(2, r.events.filterIsInstance<GameEvent.CoinFlipped>().size)
    }

    @Test
    fun `Flame Cloak une una Energia Fuego del descarte a si mismo (determinista)`() {
        val player = PlayerState(
            side = Side.PLAYER,
            active = PokemonInPlay(mon("self")),
            discard = listOf(fenergy("f1"), energy("p1"), fenergy("f2")),
        )
        val gs = GameState(player, PlayerState(Side.OPPONENT, PokemonInPlay(mon("oppA"))), 2, Side.PLAYER, Phase.MAIN)
        val eff = Effect(ops = listOf(EffectOp.AttachEnergyFromDiscard(1, EnergyType.FIRE, Target.SELF)))
        val r = interp.execute(eff, src, gs)
        assertNull(r.state.interaction)                                  // determinista, sin pausa
        assertEquals(1, r.state.player.active?.attachedEnergy?.size)
        assertEquals(CardId("f1"), r.state.player.active?.attachedEnergy?.first()?.id)
        assertFalse(r.state.player.discard.any { it.id == CardId("f1") })
        assertTrue(r.state.player.discard.any { it.id == CardId("f2") })  // solo tomó 1
    }

    @Test
    fun `Passionate Singing pausa y une hasta 2 Energia del descarte a tus Pokemon`() {
        val player = PlayerState(
            side = Side.PLAYER,
            active = PokemonInPlay(mon("self")),
            bench = listOf(PokemonInPlay(mon("benchA"))),
            discard = listOf(fenergy("f1"), energy("p1"), energy("p2")),
        )
        val gs = GameState(player, PlayerState(Side.OPPONENT, PokemonInPlay(mon("oppA"))), 2, Side.PLAYER, Phase.MAIN)
        val eff = Effect(ops = listOf(EffectOp.AttachEnergyFromDiscard(2, null, Target.OWN_ALL)))
        val paused = interp.execute(eff, src, gs)
        val d = paused.state.interaction!!.decision as PendingDecision.AttachFromRevealed
        assertTrue(d.fromDiscard)
        assertEquals(3, d.energyCandidates.size)                        // f1,p1,p2 (todas Básicas)
        assertTrue(d.benchCandidates.containsAll(listOf(CardId("self"), CardId("benchA"))))
        // Une f1 al activo y p1 a la Banca.
        val resolved = interp.resolve(
            paused.state,
            listOf(CardId("f1"), CardId("self"), CardId("p1"), CardId("benchA")),
        ) { it }
        assertNull(resolved.state.interaction)
        assertEquals(CardId("f1"), resolved.state.player.active?.attachedEnergy?.first()?.id)
        assertEquals(CardId("p1"), resolved.state.player.bench.first().attachedEnergy.first().id)
        assertFalse(resolved.state.player.discard.any { it.id == CardId("f1") || it.id == CardId("p1") })
    }

    @Test
    fun `Electric Generator sin Pokemon Rayo en Banca solo baraja (sin pausa)`() {
        val player = PlayerState(
            side = Side.PLAYER,
            active = PokemonInPlay(mon("self")),
            bench = listOf(PokemonInPlay(mon("benchPsy"))),   // no es Rayo
            deck = listOf(lenergy("L1"), energy("p1"), lenergy("L2")),
        )
        val gs = GameState(player, PlayerState(Side.OPPONENT, PokemonInPlay(mon("oppA"))), 1, Side.PLAYER, Phase.MAIN)
        val eff = Effect(ops = listOf(EffectOp.RevealAttachEnergy(5, 2, EnergyType.LIGHTNING, EnergyType.LIGHTNING)))
        val r = interp.execute(eff, EffectSource(Side.PLAYER, null), gs)
        assertNull(r.state.interaction)
        assertEquals(3, r.state.player.deck.size)   // nada se unió; el mazo sigue completo
        assertTrue(r.events.any { it is GameEvent.DeckShuffled })
    }
}
