package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.events.GameEvent
import com.mineralord.tcg.engine.model.Ability
import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage as DamageModel
import com.mineralord.tcg.engine.model.Effect
import com.mineralord.tcg.engine.model.EffectId
import com.mineralord.tcg.engine.model.EffectRegistry
import com.mineralord.tcg.engine.model.EffectsDb
import com.mineralord.tcg.engine.model.ModKind
import com.mineralord.tcg.engine.model.PassiveModifier
import com.mineralord.tcg.engine.model.Target
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

    private fun fenergy(id: String) = BasicEnergy(
        CardId(id), LocalizedText("Energía", "Energy"), set(), Rarity.COMMON, null, art(), EnergyType.FIRE,
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
    fun `Tanque del Futuro da +20 de dano solo a un Pokemon Futuro`() {
        val booster = trainer("sv4-164", TrainerKind.Tool())
        fun hitter(id: String, subtypes: List<String>) = PokemonCard(
            id = CardId(id), name = LocalizedText(id, id), set = set(),
            rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
            stage = Stage.Basic, mechanic = PokemonMechanic.Normal, hp = 120,
            types = listOf(EnergyType.PSYCHIC), evolvesFrom = null, abilities = emptyList(),
            attacks = listOf(Attack(LocalizedText("Golpe", "Hit"), emptyList(), 0, DamageModel.Fixed(20), null)),
            weaknesses = emptyList(), resistances = emptyList(), retreatCost = emptyList(),
            rulesText = emptyList(), subtypes = subtypes,
        )
        // Pokémon Futuro con la Cápsula: 20 base + 20 de la Herramienta = 40.
        val future = baseState(active = PokemonInPlay(hitter("fut", listOf("Futuro")), attachedTools = listOf(booster)))
        assertEquals(40, engine.apply(future, GameIntent.Attack("Golpe")).state.opponent.active?.damage)
        // Pokémon normal con la misma Cápsula: la condición no se cumple → solo 20.
        val normal = baseState(active = PokemonInPlay(hitter("nor", emptyList()), attachedTools = listOf(booster)))
        assertEquals(20, engine.apply(normal, GameIntent.Attack("Golpe")).state.opponent.active?.damage)
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
    fun `Transferencia de Bill solo ofrece los Pokemon del top 8 y los pasa a la mano`() {
        val bills = trainer("sv3pt5-156", TrainerKind.Supporter())   // SearchDeck(POKEMON -> HAND, top 8)
        // Top 8: 2 Pokémon + 6 Energías; un 9º Pokémon (deep) queda FUERA del top 8.
        val top = listOf(mon("t1"), mon("t2")) + (1..6).map { energy("e$it") }
        val deep = mon("deep")
        val state = baseState(hand = listOf(bills), deck = top + deep)

        val played = engine.apply(state, GameIntent.PlayTrainer(CardId("sv3pt5-156")))
        assertTrue(played.accepted, played.rejection)
        assertTrue(played.state.awaitingDecision)
        val decision = played.state.interaction!!.decision as PendingDecision.SearchCards
        assertEquals(listOf(CardId("t1"), CardId("t2")), decision.candidates, "solo los Pokémon del top 8")

        val resolved = engine.apply(played.state, GameIntent.ResolveDecision(listOf(CardId("t1"), CardId("t2"))))
        assertTrue(resolved.accepted, resolved.rejection)
        assertNull(resolved.state.interaction)
        assertTrue(resolved.state.player.hand.any { it.id == CardId("t1") })
        assertTrue(resolved.state.player.hand.any { it.id == CardId("t2") })
        // Los elegidos salen del mazo; el 9º Pokémon sigue en el mazo (nunca fue candidato).
        assertFalse(resolved.state.player.deck.any { it.id == CardId("t1") || it.id == CardId("t2") })
        assertTrue(resolved.state.player.deck.any { it.id == CardId("deep") })
        assertTrue(resolved.state.supporterPlayedThisTurn)
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

    @Test
    fun `Pocion no se puede jugar si ningun Pokemon tiene dano`() {
        // Regla oficial: no puedes jugar un Entrenador que no haría nada. Sin Pokémon
        // dañados, Poción se rechaza y NO se gasta (sigue en la mano, no va al descarte).
        val pocion = trainer("sv1-188", TrainerKind.Item())
        val state = baseState(
            hand = listOf(pocion),
            active = PokemonInPlay(mon("ownActive")),               // sano (daño 0)
            bench = listOf(PokemonInPlay(mon("benchMon"))),         // sano (daño 0)
        )

        val r = engine.apply(state, GameIntent.PlayTrainer(CardId("sv1-188")))
        assertFalse(r.accepted)
        assertTrue(r.state.player.hand.any { it.id == CardId("sv1-188") })
        assertFalse(r.state.player.discard.any { it.id == CardId("sv1-188") })
    }

    @Test
    fun `Pocion solo ofrece como objetivo a los Pokemon danados`() {
        // Activo sano + banca dañada → la elección solo incluye al de la banca.
        val pocion = trainer("sv1-188", TrainerKind.Item())
        val state = baseState(
            hand = listOf(pocion),
            active = PokemonInPlay(mon("ownActive")),               // sano
            bench = listOf(PokemonInPlay(mon("benchMon"), damage = 30)),
        )

        val played = engine.apply(state, GameIntent.PlayTrainer(CardId("sv1-188")))
        assertTrue(played.accepted, played.rejection)
        val decision = played.state.interaction!!.decision as PendingDecision.ChooseTargets
        assertEquals(listOf(CardId("benchMon")), decision.candidates)
    }

    @Test
    fun `Melo no se puede jugar si no te noquearon el turno pasado`() {
        val mela = trainer("sv4-167", TrainerKind.Supporter())
        val state = baseState(hand = listOf(mela), deck = (1..8).map { energy("d$it") })
            .copy(koedLastOppTurn = emptySet())
        val r = engine.apply(state, GameIntent.PlayTrainer(CardId("sv4-167")))
        assertFalse(r.accepted)
    }

    @Test
    fun `Melo une Fuego del descarte y roba hasta 6 si te noquearon`() {
        val mela = trainer("sv4-167", TrainerKind.Supporter())
        val state = baseState(hand = listOf(mela), deck = (1..8).map { energy("d$it") })
            .copy(
                player = baseState().player.copy(
                    hand = listOf(mela),
                    deck = (1..8).map { energy("d$it") },
                    discard = listOf(fenergy("mf1")),
                ),
                koedLastOppTurn = setOf(Side.PLAYER),
            )

        val played = engine.apply(state, GameIntent.PlayTrainer(CardId("sv4-167")))
        assertTrue(played.accepted, played.rejection)
        val d = played.state.interaction!!.decision as PendingDecision.AttachFromRevealed
        assertTrue(d.fromDiscard)

        val resolved = engine.apply(
            played.state,
            GameIntent.ResolveDecision(listOf(CardId("mf1"), CardId("ownActive"))),
        )
        assertTrue(resolved.accepted, resolved.rejection)
        assertNull(resolved.state.interaction)
        assertEquals(CardId("mf1"), resolved.state.player.active?.attachedEnergy?.firstOrNull()?.id)
        assertEquals(6, resolved.state.player.hand.size)           // robó hasta tener 6
    }

    @Test
    fun `Melo sin Fuego en el descarte no engancha ni roba`() {
        // Condición cumplida (te noquearon) pero descarte sin Energía Fuego → "si lo
        // haces" no se cumple: no hay decisión, no roba (mano queda vacía tras descartar Melo).
        val mela = trainer("sv4-167", TrainerKind.Supporter())
        val state = baseState().let { s ->
            s.copy(
                player = s.player.copy(
                    hand = listOf(mela),
                    deck = (1..8).map { energy("d$it") },
                    discard = listOf(energy("psy1")),   // Psíquica, no Fuego
                ),
                koedLastOppTurn = setOf(Side.PLAYER),
            )
        }
        val r = engine.apply(state, GameIntent.PlayTrainer(CardId("sv4-167")))
        assertTrue(r.accepted, r.rejection)
        assertNull(r.state.interaction)
        assertEquals(0, r.state.player.hand.size)       // no robó
        assertEquals(8, r.state.player.deck.size)       // mazo intacto
    }

    /** Rng con moneda fija (para el enganche condicionado de Pegatinas de Energía). */
    private class FixedRng(private val heads: Boolean) : Rng {
        override fun flipCoin(): Boolean = heads
        override fun <T> shuffle(list: List<T>): List<T> = list
        override fun nextInt(untilExclusive: Int): Int = 0
    }

    private fun energyStickerState(): GameState = baseState().let { s ->
        s.copy(
            player = s.player.copy(
                hand = listOf(trainer("sv3pt5-159", TrainerKind.Item())),
                bench = listOf(PokemonInPlay(mon("benchMon"))),
                discard = listOf(energy("disc1")),
            ),
        )
    }

    @Test
    fun `Pegatinas de Energia con cara une 1 Basica del descarte a un Pokemon de Banca`() {
        val engine = GameEngine(FixedRng(true))
        val played = engine.apply(energyStickerState(), GameIntent.PlayTrainer(CardId("sv3pt5-159")))
        assertTrue(played.accepted, played.rejection)
        val d = played.state.interaction!!.decision as PendingDecision.AttachFromRevealed
        assertTrue(d.fromDiscard)
        assertEquals(listOf(CardId("benchMon")), d.benchCandidates, "solo la Banca es destino")

        val resolved = engine.apply(
            played.state, GameIntent.ResolveDecision(listOf(CardId("disc1"), CardId("benchMon"))),
        )
        assertTrue(resolved.accepted, resolved.rejection)
        assertTrue(resolved.events.any { it is GameEvent.CoinFlipped && it.heads })
        val benched = resolved.state.player.bench.first { it.card.id == CardId("benchMon") }
        assertEquals(CardId("disc1"), benched.attachedEnergy.firstOrNull()?.id, "se unió la Energía")
        assertFalse(resolved.state.player.discard.any { it.id == CardId("disc1") }, "salió del descarte")
    }

    @Test
    fun `Pegatinas de Energia con cruz no une nada`() {
        val engine = GameEngine(FixedRng(false))
        val played = engine.apply(energyStickerState(), GameIntent.PlayTrainer(CardId("sv3pt5-159")))
        val resolved = engine.apply(
            played.state, GameIntent.ResolveDecision(listOf(CardId("disc1"), CardId("benchMon"))),
        )
        assertTrue(resolved.accepted, resolved.rejection)
        assertTrue(resolved.events.any { it is GameEvent.CoinFlipped && !it.heads })
        val benched = resolved.state.player.bench.first { it.card.id == CardId("benchMon") }
        assertTrue(benched.attachedEnergy.isEmpty(), "sin cara no se une Energía")
        assertTrue(resolved.state.player.discard.any { it.id == CardId("disc1") }, "la Energía sigue en el descarte")
    }

    // ---------------------------------------------------------------- Tools

    private fun hitAttacker(dmg: Int) = PokemonInPlay(
        mon("attacker").copy(
            attacks = listOf(Attack(LocalizedText("Golpe", "Hit"), emptyList(), 0, DamageModel.Fixed(dmg), null)),
        ),
    )

    @Test
    fun `anclar una Herramienta a un Pokemon propio`() {
        val tool = trainer("tool-x", TrainerKind.Tool())
        val state = baseState(hand = listOf(tool))
        val r = engine.apply(state, GameIntent.AttachTool(CardId("tool-x"), CardId("ownActive")))
        assertTrue(r.accepted, r.rejection)
        assertTrue(r.state.player.active!!.attachedTools.any { it.id == CardId("tool-x") })
        assertFalse(r.state.player.hand.any { it.id == CardId("tool-x") })   // salió de la mano
    }

    @Test
    fun `un Pokemon no admite dos Herramientas`() {
        val t1 = trainer("tool-a", TrainerKind.Tool())
        val t2 = trainer("tool-b", TrainerKind.Tool())
        val state = baseState(hand = listOf(t1, t2))
        val first = engine.apply(state, GameIntent.AttachTool(CardId("tool-a"), CardId("ownActive")))
        assertTrue(first.accepted, first.rejection)
        val second = engine.apply(first.state, GameIntent.AttachTool(CardId("tool-b"), CardId("ownActive")))
        assertFalse(second.accepted)
    }

    @Test
    fun `una Herramienta con HP extra evita el KO`() {
        val registry = EffectRegistry(
            mapOf(EffectId("tool-hp") to Effect(passives = listOf(PassiveModifier(ModKind.EXTRA_HP, 30, Target.SELF)))),
        )
        val eng = GameEngine(SeededRng(1), registry)
        val toolCard = trainer("tool-hp", TrainerKind.Tool())
        val defender = PokemonInPlay(mon("oppActive", hp = 100), attachedTools = listOf(toolCard))
        val player = PlayerState(Side.PLAYER, active = hitAttacker(100), deck = listOf(energy("d")), prizes = (1..6).map { energy("p$it") })
        val opp = PlayerState(Side.OPPONENT, active = defender, deck = listOf(energy("od")), prizes = (1..6).map { energy("q$it") })
        val state = GameState(player, opp, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val r = eng.apply(state, GameIntent.Attack("Golpe"))
        assertTrue(r.accepted, r.rejection)
        // 100 de daño con HP efectivo 130 → NO noqueado (sigue en juego con 100 de daño).
        assertEquals(100, r.state.opponent.active?.damage)
        assertFalse(r.state.opponent.active == null)
    }

    @Test
    fun `una Herramienta reduce el dano recibido`() {
        val registry = EffectRegistry(
            mapOf(EffectId("tool-armor") to Effect(passives = listOf(PassiveModifier(ModKind.REDUCE_DAMAGE, 30, Target.SELF)))),
        )
        val eng = GameEngine(SeededRng(1), registry)
        val toolCard = trainer("tool-armor", TrainerKind.Tool())
        val defender = PokemonInPlay(mon("oppActive", hp = 100), attachedTools = listOf(toolCard))
        val player = PlayerState(Side.PLAYER, active = hitAttacker(50), deck = listOf(energy("d")), prizes = (1..6).map { energy("p$it") })
        val opp = PlayerState(Side.OPPONENT, active = defender, deck = listOf(energy("od")), prizes = (1..6).map { energy("q$it") })
        val state = GameState(player, opp, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val r = eng.apply(state, GameIntent.Attack("Golpe"))
        assertTrue(r.accepted, r.rejection)
        assertEquals(20, r.state.opponent.active?.damage)   // 50 - 30
    }

    @Test
    fun `legalIntents ofrece anclar una Herramienta a cada Pokemon propio sin una`() {
        val tool = trainer("tool-x", TrainerKind.Tool())
        val state = baseState(
            hand = listOf(tool),
            active = PokemonInPlay(mon("ownActive")),
            bench = listOf(PokemonInPlay(mon("benchMon"))),
        )
        val intents = engine.legalIntents(state).filterIsInstance<GameIntent.AttachTool>()
        assertEquals(
            setOf(CardId("ownActive"), CardId("benchMon")),
            intents.map { it.target }.toSet(),
        )
        assertTrue(intents.all { it.tool == CardId("tool-x") })
    }

    @Test
    fun `legalIntents no ofrece anclar a un Pokemon que ya tiene Herramienta`() {
        val tool = trainer("tool-x", TrainerKind.Tool())
        val occupied = PokemonInPlay(mon("ownActive"), attachedTools = listOf(trainer("tool-y", TrainerKind.Tool())))
        val state = baseState(hand = listOf(tool), active = occupied)
        val intents = engine.legalIntents(state).filterIsInstance<GameIntent.AttachTool>()
        assertTrue(intents.none { it.target == CardId("ownActive") }, intents.toString())
    }

    @Test
    fun `SmartAgent ancla la Herramienta al Activo`() {
        val tool = trainer("tool-x", TrainerKind.Tool())
        val state = baseState(hand = listOf(tool), active = PokemonInPlay(mon("ownActive")))
        val decided = SmartAgent(engine).decide(state, Side.PLAYER)
        assertEquals(GameIntent.AttachTool(CardId("tool-x"), CardId("ownActive")), decided)
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

    // --------------------------------------------------- Fase 46: mano del rival (Agarrador Mecánico)

    @Test
    fun `Agarrador Mecanico pone un Pokemon de la mano rival al fondo de su baraja`() {
        val grabber = trainer("sv3pt5-162", TrainerKind.Item())   // PutOppHandPokemonToBottomOfDeck
        val base = baseState(hand = listOf(grabber))
        val oppState = base.opponent.copy(
            hand = listOf(mon("oppHandMon"), energy("oe1")),
            deck = listOf(energy("od1"), energy("od2")),
        )
        val state = base.copy(opponent = oppState)

        val played = engine.apply(state, GameIntent.PlayTrainer(CardId("sv3pt5-162")))
        assertTrue(played.accepted, played.rejection)
        assertTrue(played.state.awaitingDecision)
        val d = played.state.interaction!!.decision as PendingDecision.SearchCards
        assertTrue(d.fromOpponentHand)
        assertEquals(listOf(CardId("oppHandMon")), d.candidates, "solo el Pokémon, no la Energía")

        val resolved = engine.apply(played.state, GameIntent.ResolveDecision(listOf(CardId("oppHandMon"))))
        assertTrue(resolved.accepted, resolved.rejection)
        assertNull(resolved.state.interaction)
        assertFalse(resolved.state.opponent.hand.any { it.id == CardId("oppHandMon") }, "salió de la mano rival")
        assertEquals(CardId("oppHandMon"), resolved.state.opponent.deck.last().id, "quedó al fondo de su baraja")
        assertEquals(1, resolved.state.opponent.hand.size)
    }

    @Test
    fun `Agarrador Mecanico sin Pokemon en la mano rival se juega sin abrir decision`() {
        val grabber = trainer("sv3pt5-162", TrainerKind.Item())
        val base = baseState(hand = listOf(grabber))
        val oppState = base.opponent.copy(hand = listOf(energy("oe1")), deck = listOf(energy("od1")))
        val state = base.copy(opponent = oppState)

        val played = engine.apply(state, GameIntent.PlayTrainer(CardId("sv3pt5-162")))
        assertTrue(played.accepted, played.rejection)
        assertFalse(played.state.awaitingDecision, "sin Pokémon en la mano rival no abre decisión")
        assertTrue(played.state.player.discard.any { it.id == CardId("sv3pt5-162") }, "el Objeto se jugó igualmente")
    }
}
