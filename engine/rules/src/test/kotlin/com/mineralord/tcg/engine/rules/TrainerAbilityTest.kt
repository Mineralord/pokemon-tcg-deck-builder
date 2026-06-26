package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.Ability
import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage as DamageModel
import com.mineralord.tcg.engine.model.EffectId
import com.mineralord.tcg.engine.model.EffectsDb
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
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cubre los intents nuevos ([GameIntent.PlayTrainer], [GameIntent.UseAbility],
 * [GameIntent.ResolveDecision]) contra los efectos YA registrados en
 * [EffectsDb], incluida la resolución de decisiones encadenadas.
 */
class TrainerAbilityTest {

    private val engine = GameEngine(SeededRng(7))
    private fun art() = ArtworkRefs(null, null, "s", "l")
    private fun set() = SetInfo("test", LocalizedText("Test", "Test"), "Test")
    private fun dummyAtk() = Attack(LocalizedText("x", "x"), emptyList(), 0, DamageModel.None, null)

    private fun mon(id: String, hp: Int = 100, abilities: List<Ability> = emptyList()) = PokemonCard(
        id = CardId(id), name = LocalizedText(id, id), set = set(),
        rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
        stage = Stage.Basic, mechanic = PokemonMechanic.Normal, hp = hp,
        types = listOf(EnergyType.PSYCHIC), evolvesFrom = null, abilities = abilities,
        attacks = listOf(dummyAtk()),
        weaknesses = emptyList(), resistances = emptyList(), retreatCost = emptyList(), rulesText = emptyList(),
    )

    private fun energy(id: String) = BasicEnergy(
        CardId(id), LocalizedText("Energía", "Energy"), set(), Rarity.COMMON, null, art(), EnergyType.PSYCHIC,
    )

    private fun trainer(id: String, kind: TrainerKind) = TrainerCard(
        id = CardId(id), name = LocalizedText(id, id), set = set(),
        rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
        kind = kind, text = LocalizedText("t", "t"), effect = EffectId(id),
    )

    /** Estado base con Activo propio y rival, mazo/premios mínimos válidos. */
    private fun baseState(
        hand: List<Card> = emptyList(),
        deck: List<Card> = (1..5).map { energy("d$it") },
        active: PokemonInPlay = PokemonInPlay(mon("ownActive")),
        bench: List<PokemonInPlay> = emptyList(),
    ): GameState {
        val player = PlayerState(
            Side.PLAYER, active = active, bench = bench, hand = hand, deck = deck,
            prizes = (1..6).map { energy("p$it") },
        )
        val opp = PlayerState(
            Side.OPPONENT, active = PokemonInPlay(mon("oppActive")),
            deck = listOf(energy("od")), prizes = (1..6).map { energy("q$it") },
        )
        return GameState(player, opp, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
    }

    // ----------------------------------------------------------------- Trainers

    @Test
    fun `Nemona roba 3 y va al descarte`() {
        val nemona = trainer("sv1-180", TrainerKind.Supporter())   // DrawCards(3)
        val state = baseState(hand = listOf(nemona), deck = (1..5).map { energy("d$it") })

        val r = engine.apply(state, GameIntent.PlayTrainer(CardId("sv1-180")))
        assertTrue(r.accepted, r.rejection)
        assertEquals(3, r.state.player.hand.size)            // 0 (tras descartar Nemona) + 3 robadas
        assertEquals(2, r.state.player.deck.size)            // 5 - 3
        assertTrue(r.state.player.discard.any { it.id == CardId("sv1-180") })
        assertTrue(r.state.supporterPlayedThisTurn)
    }

    @Test
    fun `solo un Apoyo por turno`() {
        val nemona = trainer("sv1-180", TrainerKind.Supporter())
        val daisy = trainer("sv3pt5-158", TrainerKind.Supporter())  // DrawCards(2)
        val state = baseState(hand = listOf(nemona, daisy))

        val first = engine.apply(state, GameIntent.PlayTrainer(CardId("sv1-180")))
        assertTrue(first.accepted, first.rejection)
        val second = first.state.let { engine.apply(it, GameIntent.PlayTrainer(CardId("sv3pt5-158"))) }
        assertFalse(second.accepted)
    }

    @Test
    fun `Super Ball deja una busqueda y ResolveDecision la completa`() {
        val superBall = trainer("sv2-183", TrainerKind.Item())     // SearchDeck(POKEMON -> HAND, 1)
        val deckPoke = mon("deckMon")
        val state = baseState(
            hand = listOf(superBall),
            deck = listOf(deckPoke, energy("e1"), energy("e2")),
        )

        val played = engine.apply(state, GameIntent.PlayTrainer(CardId("sv2-183")))
        assertTrue(played.accepted, played.rejection)
        assertTrue(played.state.awaitingDecision)
        val decision = played.state.interaction!!.decision as PendingDecision.SearchCards
        assertEquals(listOf(CardId("deckMon")), decision.candidates)

        val resolved = engine.apply(played.state, GameIntent.ResolveDecision(listOf(CardId("deckMon"))))
        assertTrue(resolved.accepted, resolved.rejection)
        assertNull(resolved.state.interaction)
        assertTrue(resolved.state.player.hand.any { it.id == CardId("deckMon") })
        assertFalse(resolved.state.player.deck.any { it.id == CardId("deckMon") })
    }

    @Test
    fun `mientras hay decision pendiente solo se acepta ResolveDecision`() {
        val superBall = trainer("sv2-183", TrainerKind.Item())
        val state = baseState(hand = listOf(superBall), deck = listOf(mon("deckMon")))
        val played = engine.apply(state, GameIntent.PlayTrainer(CardId("sv2-183")))
        assertTrue(played.state.awaitingDecision)

        val blocked = engine.apply(played.state, GameIntent.EndTurn)
        assertFalse(blocked.accepted)
    }

    @Test
    fun `Pocion cura 30 al objetivo elegido`() {
        val pocion = trainer("sv1-188", TrainerKind.Item())        // ChooseTarget(OWN_ALL,1) + Heal(CHOSEN,30)
        val hurt = PokemonInPlay(mon("ownActive"), damage = 50)
        val state = baseState(hand = listOf(pocion), active = hurt)

        val played = engine.apply(state, GameIntent.PlayTrainer(CardId("sv1-188")))
        assertTrue(played.accepted, played.rejection)
        assertTrue(played.state.interaction!!.decision is PendingDecision.ChooseTargets)

        val resolved = engine.apply(played.state, GameIntent.ResolveDecision(listOf(CardId("ownActive"))))
        assertTrue(resolved.accepted, resolved.rejection)
        assertNull(resolved.state.interaction)
        assertEquals(20, resolved.state.player.active?.damage)     // 50 - 30
    }

    // ---------------------------------------------------------------- Abilities

    @Test
    fun `Zooming Draw se autoinflige 10 y roba 1, una vez por turno`() {
        val abi = Ability(
            LocalizedText("Zumbido", "Zooming Draw"), LocalizedText("t", "t"),
            EffectsDb.abiKey("sv3pt5-85", "Zooming Draw"),
        )
        val dodrio = PokemonInPlay(mon("sv3pt5-85", abilities = listOf(abi)))
        val state = baseState(active = dodrio, deck = (1..3).map { energy("d$it") })

        val first = engine.apply(state, GameIntent.UseAbility(CardId("sv3pt5-85"), "Zooming Draw"))
        assertTrue(first.accepted, first.rejection)
        assertEquals(10, first.state.player.active?.damage)
        assertEquals(1, first.state.player.hand.size)
        assertTrue(CardId("sv3pt5-85") in first.state.abilitiesUsedThisTurn)

        val second = engine.apply(first.state, GameIntent.UseAbility(CardId("sv3pt5-85"), "Zooming Draw"))
        assertFalse(second.accepted)
    }

    @Test
    fun `una habilidad activeOnly no se puede usar desde la banca`() {
        val abi = Ability(
            LocalizedText("Luz", "Calming Light"), LocalizedText("t", "t"),
            EffectsDb.abiKey("sv8-9", "Calming Light"),
        )
        val shiinotic = PokemonInPlay(mon("sv8-9", abilities = listOf(abi)))
        val state = baseState(active = PokemonInPlay(mon("ownActive")), bench = listOf(shiinotic))

        val r = engine.apply(state, GameIntent.UseAbility(CardId("sv8-9"), "Calming Light"))
        assertFalse(r.accepted)
    }

    @Test
    fun `fin de turno reinicia los limites por turno`() {
        val nemona = trainer("sv1-180", TrainerKind.Supporter())
        val state = baseState(hand = listOf(nemona))
        val played = engine.apply(state, GameIntent.PlayTrainer(CardId("sv1-180")))
        assertTrue(played.state.supporterPlayedThisTurn)

        val ended = engine.apply(played.state, GameIntent.EndTurn)
        assertTrue(ended.accepted, ended.rejection)
        assertFalse(ended.state.supporterPlayedThisTurn)
        assertTrue(ended.state.abilitiesUsedThisTurn.isEmpty())
    }
}
