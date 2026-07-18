package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.events.GameEvent
import com.mineralord.tcg.engine.model.Ability
import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage as DamageModel
import com.mineralord.tcg.engine.model.EffectId
import com.mineralord.tcg.engine.model.EffectsDb
import com.mineralord.tcg.engine.model.EnergyProvision
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.Phase
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Rarity
import com.mineralord.tcg.engine.model.SetInfo
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.SpecialEnergy
import com.mineralord.tcg.engine.model.Stage
import com.mineralord.tcg.engine.model.Status
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import com.mineralord.tcg.engine.model.TypeModifier
import com.mineralord.tcg.engine.model.Zone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cobertura de la Fase 1 de efectos del set 151: ops nuevas (daño/estado por
 * moneda, descarte de mano rival) y el daño condicional "X+" (tipo del defensor,
 * defensor/atacante con daño). Se construyen cartas sintéticas cuyo `Attack.effect`
 * apunta a las claves reales registradas en [EffectsDb].
 */
class Set151EffectsTest {

    /** Rng con moneda fija (para tests deterministas de monedas). */
    private class FixedRng(private val heads: Boolean) : Rng {
        override fun flipCoin(): Boolean = heads
        override fun <T> shuffle(list: List<T>): List<T> = list
        override fun nextInt(untilExclusive: Int): Int = 0
    }

    private fun art() = ArtworkRefs(null, null, "s", "l")
    private fun set() = SetInfo("test", LocalizedText("Test", "Test"), "Test")

    private fun mon(
        id: String, hp: Int, type: EnergyType, attack: Attack,
        weakness: EnergyType? = null, stage: Stage = Stage.Basic,
        mechanic: PokemonMechanic = PokemonMechanic.Normal,
    ) = PokemonCard(
        id = CardId(id), name = LocalizedText(id, id), set = set(),
        rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
        stage = stage, mechanic = mechanic, hp = hp,
        types = listOf(type), evolvesFrom = null, abilities = emptyList(),
        attacks = listOf(attack),
        weaknesses = weakness?.let { listOf(TypeModifier(it, "×2")) } ?: emptyList(),
        resistances = emptyList(), retreatCost = emptyList(), rulesText = emptyList(),
    )

    private fun energy(id: String, type: EnergyType) = BasicEnergy(
        CardId(id), LocalizedText("Energía", "Energy"), set(), Rarity.COMMON, null, art(), type,
    )

    private fun specialEnergy(id: String, type: EnergyType = EnergyType.PSYCHIC) = SpecialEnergy(
        CardId(id), LocalizedText("Especial", "Special"), set(), Rarity.COMMON, null, art(),
        provides = EnergyProvision.Fixed(listOf(type)), stateModifiers = emptyList(), effect = EffectId("none"),
    )

    private fun attack(name: String, dmg: Int, effect: EffectId, cost: Int = 0) =
        Attack(LocalizedText(name, name), emptyList(), cost, DamageModel.Fixed(dmg), effect)

    /** Estado mínimo: mi Activo ataca al Activo rival (turno 3, sin premios en juego relevante). */
    private fun duel(mine: PokemonInPlay, foe: PokemonInPlay, foeHand: List<com.mineralord.tcg.engine.model.Card> = emptyList()): GameState {
        val player = PlayerState(
            side = Side.PLAYER, active = mine,
            deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) },
            prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = foe, hand = foeHand,
            deck = (1..5).map { energy("od$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("opz$it", EnergyType.WATER) },
        )
        return GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
    }

    @Test
    fun `daño condicional por tipo del defensor (Leaf Munch)`() {
        val a = attack("Leaf Munch", 0, EffectsDb.atkKey("sv3pt5-10", "Leaf Munch"))
        val attacker = PokemonInPlay(mon("caterpie", 60, EnergyType.GRASS, a))
        val grassFoe = PokemonInPlay(mon("bulba", 200, EnergyType.GRASS, a))
        val fireFoe = PokemonInPlay(mon("char", 200, EnergyType.FIRE, a))

        val vsGrass = GameEngine(SeededRng(1)).apply(duel(attacker, grassFoe), GameIntent.Attack("Leaf Munch"))
        assertEquals(40, vsGrass.events.filterIsInstance<GameEvent.DamageDealt>().first().amount)

        val vsFire = GameEngine(SeededRng(1)).apply(duel(attacker, fireFoe), GameIntent.Attack("Leaf Munch"))
        assertEquals(10, vsFire.events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    @Test
    fun `daño condicional si el defensor ya tiene daño (Spike Rend)`() {
        val a = attack("Spike Rend", 0, EffectsDb.atkKey("sv3pt5-28", "Spike Rend"))
        val attacker = PokemonInPlay(mon("sandslash", 120, EnergyType.FIGHTING, a))
        val healthy = PokemonInPlay(mon("t1", 300, EnergyType.WATER, a))
        val hurt = PokemonInPlay(mon("t2", 300, EnergyType.WATER, a), damage = 10)

        assertEquals(80, GameEngine(SeededRng(1)).apply(duel(attacker, healthy), GameIntent.Attack("Spike Rend"))
            .events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
        assertEquals(180, GameEngine(SeededRng(1)).apply(duel(attacker, hurt), GameIntent.Attack("Spike Rend"))
            .events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    @Test
    fun `daño condicional si el atacante tiene daño (Brave Wing)`() {
        val a = attack("Brave Wing", 0, EffectsDb.atkKey("sv3pt5-6", "Brave Wing"))
        val fresh = PokemonInPlay(mon("charizard", 330, EnergyType.FIRE, a))
        val hurt = PokemonInPlay(mon("charizard", 330, EnergyType.FIRE, a), damage = 20)
        val foe = PokemonInPlay(mon("target", 400, EnergyType.WATER, a))

        assertEquals(60, GameEngine(SeededRng(1)).apply(duel(fresh, foe), GameIntent.Attack("Brave Wing"))
            .events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
        assertEquals(160, GameEngine(SeededRng(1)).apply(duel(hurt, foe), GameIntent.Attack("Brave Wing"))
            .events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    @Test
    fun `daño extra por moneda (Tumbling Attack) - cara suma, cruz no`() {
        val a = attack("Tumbling Attack", 10, EffectsDb.atkKey("sv3pt5-100", "Tumbling Attack"))
        val attacker = PokemonInPlay(mon("voltorb", 60, EnergyType.LIGHTNING, a))
        val foe = PokemonInPlay(mon("target", 200, EnergyType.WATER, a))

        val heads = GameEngine(FixedRng(true)).apply(duel(attacker, foe), GameIntent.Attack("Tumbling Attack"))
        // 10 base + 20 por cara = 30 (dos eventos de daño: base y extra).
        assertEquals(30, heads.events.filterIsInstance<GameEvent.DamageDealt>().sumOf { it.amount })

        val tails = GameEngine(FixedRng(false)).apply(duel(attacker, foe), GameIntent.Attack("Tumbling Attack"))
        assertEquals(10, tails.events.filterIsInstance<GameEvent.DamageDealt>().sumOf { it.amount })
    }

    @Test
    fun `estado por moneda (Bubble) - cara paraliza`() {
        val a = attack("Bubble", 10, EffectsDb.atkKey("sv3pt5-60", "Bubble"))
        val attacker = PokemonInPlay(mon("squirtle", 70, EnergyType.WATER, a))
        val foe = PokemonInPlay(mon("target", 200, EnergyType.GRASS, a))

        val heads = GameEngine(FixedRng(true)).apply(duel(attacker, foe), GameIntent.Attack("Bubble"))
        assertTrue(heads.state.opponent.active!!.statuses.contains(Status.PARALYZED))

        val tails = GameEngine(FixedRng(false)).apply(duel(attacker, foe), GameIntent.Attack("Bubble"))
        assertTrue(tails.state.opponent.active!!.statuses.isEmpty())
    }

    @Test
    fun `el rival descarta cartas de su mano (Menacing Fangs)`() {
        val a = attack("Menacing Fangs", 30, EffectsDb.atkKey("sv3pt5-24", "Menacing Fangs"))
        val attacker = PokemonInPlay(mon("arbok", 180, EnergyType.DARKNESS, a))
        val foe = PokemonInPlay(mon("target", 200, EnergyType.FIGHTING, a))
        val foeHand = listOf(energy("h1", EnergyType.WATER), energy("h2", EnergyType.WATER), energy("h3", EnergyType.WATER))

        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe, foeHand), GameIntent.Attack("Menacing Fangs"))
        // El rival descartó 2 (tras el ataque, al empezar SU turno, roba 1: por eso no
        // comprobamos el tamaño de la mano, sino el descarte y el evento).
        assertEquals(2, res.state.opponent.discard.size)
        assertTrue(res.events.any { it is GameEvent.CardsDiscarded && it.count == 2 })
    }

    @Test
    fun `FASE 27 - Filamentos Dispersos con 2 caras busca hasta 2 Grass a la Banca`() {
        val a = attack("Spread Filaments", 0, EffectsDb.atkKey("sv3pt5-47", "Spread Filaments"))
        val attacker = PokemonInPlay(mon("parasect", 120, EnergyType.GRASS, a))
        val foe = PokemonInPlay(mon("target", 200, EnergyType.FIRE, a))
        val grassPoke = mon("oddish", 60, EnergyType.GRASS, a)   // Pokémon {G} en el mazo
        val base = duel(attacker, foe)
        val st = base.copy(player = base.player.copy(deck = listOf<com.mineralord.tcg.engine.model.Card>(grassPoke) + base.player.deck))

        // 2 monedas → 2 caras (FixedRng(true)).
        val flipped = GameEngine(FixedRng(true)).apply(st, GameIntent.Attack("Spread Filaments"))
        assertTrue(flipped.state.interaction!!.decision is PendingDecision.CoinFlipThenSearch)

        val resolved = GameEngine(FixedRng(true)).apply(flipped.state, GameIntent.ResolveDecision(emptyList()))
        val search = resolved.state.interaction!!.decision as PendingDecision.SearchCards
        assertEquals(2, search.count)                                   // caras = 2
        assertTrue(search.candidates.any { it.raw == "oddish" })        // el {G} es candidato

        val done = GameEngine(FixedRng(true)).apply(resolved.state, GameIntent.ResolveDecision(listOf(CardId("oddish"))))
        assertTrue(done.state.player.bench.any { it.card.id.raw == "oddish" })
    }

    @Test
    fun `FASE 27 - Filamentos Dispersos con 0 caras no busca nada`() {
        val a = attack("Spread Filaments", 0, EffectsDb.atkKey("sv3pt5-47", "Spread Filaments"))
        val attacker = PokemonInPlay(mon("parasect", 120, EnergyType.GRASS, a))
        val foe = PokemonInPlay(mon("target", 200, EnergyType.FIRE, a))
        val grassPoke = mon("oddish", 60, EnergyType.GRASS, a)
        val base = duel(attacker, foe)
        val st = base.copy(player = base.player.copy(deck = listOf<com.mineralord.tcg.engine.model.Card>(grassPoke) + base.player.deck))

        val flipped = GameEngine(FixedRng(false)).apply(st, GameIntent.Attack("Spread Filaments"))
        val resolved = GameEngine(FixedRng(false)).apply(flipped.state, GameIntent.ResolveDecision(emptyList()))
        // 0 caras: no encadena búsqueda; la cadena se agota y el turno se cierra.
        assertNull(resolved.state.interaction)
        assertTrue(resolved.state.player.bench.none { it.card.id.raw == "oddish" })
    }

    @Test
    fun `FASE 28 - Gyarados Indomable descarta las 5 primeras cartas al evolucionar`() {
        val dummy = attack("x", 0, EffectId("none"))
        val magikarp = PokemonInPlay(mon("Magikarp", 30, EnergyType.WATER, dummy)).copy(turnsInPlay = 1)
        val gyarados = mon("gyarados", 130, EnergyType.WATER, dummy).copy(
            stage = Stage.Stage1, evolvesFrom = "Magikarp",
            abilities = listOf(Ability(
                LocalizedText("Indomable", "Untamed One"), LocalizedText("", ""),
                EffectsDb.abiKey("sv3pt5-130", "Untamed One"))),
        )
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.LIGHTNING, dummy))
        val base = duel(magikarp, foe)
        val st = base.copy(player = base.player.copy(hand = listOf(gyarados)))

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Evolve(CardId("gyarados"), CardId("Magikarp")))
        assertEquals(0, res.state.player.deck.size)       // el mazo tenía 5 → se descartan todas
        assertEquals(5, res.state.player.discard.size)
        assertEquals(CardId("gyarados"), res.state.player.active?.card?.id)
    }

    @Test
    fun `FASE 28 - Hypno Toma Hipnosis duerme al Activo rival al evolucionar`() {
        val dummy = attack("x", 0, EffectId("none"))
        val drowzee = PokemonInPlay(mon("Drowzee", 70, EnergyType.PSYCHIC, dummy)).copy(turnsInPlay = 1)
        val hypno = mon("hypno", 110, EnergyType.PSYCHIC, dummy).copy(
            stage = Stage.Stage1, evolvesFrom = "Drowzee",
            abilities = listOf(Ability(
                LocalizedText("Toma Hipnosis", "Here for Hypnosis"), LocalizedText("", ""),
                EffectsDb.abiKey("sv3pt5-97", "Here for Hypnosis"))),
        )
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.DARKNESS, dummy))
        val base = duel(drowzee, foe)
        val st = base.copy(player = base.player.copy(hand = listOf(hypno)))

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Evolve(CardId("hypno"), CardId("Drowzee")))
        assertTrue(res.state.opponent.active?.statuses?.contains(Status.ASLEEP) == true)
    }

    @Test
    fun `FASE 29 - Gloom Floracion Parcial revela 3 y une Energia al evolucionar`() {
        val dummy = attack("x", 0, EffectId("none"))
        val oddish = PokemonInPlay(mon("Oddish", 60, EnergyType.GRASS, dummy)).copy(turnsInPlay = 1)
        val gloom = mon("gloom", 90, EnergyType.GRASS, dummy).copy(
            stage = Stage.Stage1, evolvesFrom = "Oddish",
            abilities = listOf(Ability(
                LocalizedText("Floración Parcial de Energía", "Semi-Blooming Energy"), LocalizedText("", ""),
                EffectsDb.abiKey("sv3pt5-44", "Semi-Blooming Energy"))),
        )
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIRE, dummy))
        val base = duel(oddish, foe)
        val st = base.copy(player = base.player.copy(hand = listOf(gloom)))

        // Evolucionar dispara la habilidad interactiva → el motor PAUSA con la decisión de arrastre.
        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.Evolve(CardId("gloom"), CardId("Oddish")))
        val d = paused.state.interaction!!.decision as PendingDecision.AttachFromRevealed
        assertEquals(3, d.revealed.size)                        // miró el top 3 del mazo
        assertEquals(3, d.energyCandidates.size)                // las 3 son Energía Básica (cualquier tipo)
        assertTrue(d.benchCandidates.contains(CardId("gloom"))) // "a tus Pokémon": el Activo es destino válido

        // Une la 1ª Energía revelada al Activo; el resto del mazo se baraja de vuelta.
        val res = GameEngine(SeededRng(1)).apply(
            paused.state, GameIntent.ResolveDecision(listOf(CardId("d1"), CardId("gloom"))))
        assertTrue(res.state.player.active!!.attachedEnergy.any { it.id == CardId("d1") })
        assertEquals(4, res.state.player.deck.size)             // 5 − 1 unida = 4
    }

    @Test
    fun `FASE 29 - Vileplume Floracion Total revela hasta 8 al evolucionar`() {
        val dummy = attack("x", 0, EffectId("none"))
        val gloom = PokemonInPlay(mon("Gloom", 90, EnergyType.GRASS, dummy)).copy(turnsInPlay = 1)
        val vileplume = mon("vileplume", 140, EnergyType.GRASS, dummy).copy(
            stage = Stage.Stage2, evolvesFrom = "Gloom",
            abilities = listOf(Ability(
                LocalizedText("Floración Total de Energía", "Fully Blooming Energy"), LocalizedText("", ""),
                EffectsDb.abiKey("sv3pt5-45", "Fully Blooming Energy"))),
        )
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIRE, dummy))
        val base = duel(gloom, foe)
        val st = base.copy(player = base.player.copy(hand = listOf(vileplume)))

        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.Evolve(CardId("vileplume"), CardId("Gloom")))
        val d = paused.state.interaction!!.decision as PendingDecision.AttachFromRevealed
        assertEquals(5, d.revealed.size)                        // take(8) sobre un mazo de 5 → 5
        assertEquals(5, d.energyCandidates.size)
    }

    @Test
    fun `FASE 30 - Wigglytuff ex Cuerpo Expansivo suma 100 PS con Energia Especial`() {
        val hit = attack("Hit", 100, EffectId("none"))
        val dummy = attack("x", 0, EffectId("none"))
        val attacker = PokemonInPlay(mon("attacker", 200, EnergyType.PSYCHIC, hit))
        val wigglyCard = mon("wiggly", 100, EnergyType.PSYCHIC, dummy, mechanic = PokemonMechanic.ExLower).copy(
            abilities = listOf(Ability(
                LocalizedText("Cuerpo Expansivo", "Expanding Body"), LocalizedText("", ""),
                EffectsDb.abiKey("sv3pt5-40", "Expanding Body"))),
        )

        // Con Energía Especial → HP efectivo 200: el golpe de 100 NO noquea.
        val withSpecial = PokemonInPlay(wigglyCard).copy(attachedEnergy = listOf(specialEnergy("se1")))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, withSpecial), GameIntent.Attack("Hit"))
        assertTrue(res.state.opponent.active != null)
        assertEquals(100, res.state.opponent.active!!.damage)

        // Sin Energía Especial → HP efectivo 100: el mismo golpe lo noquea.
        val noSpecial = PokemonInPlay(wigglyCard)
        val res2 = GameEngine(SeededRng(1)).apply(duel(attacker, noSpecial), GameIntent.Attack("Hit"))
        assertNull(res2.state.opponent.active)
    }

    @Test
    fun `FASE 30 - Zapdos ex Flotacion Voltaica anula el coste de retirada con Energia L`() {
        val dummy = attack("x", 0, EffectId("none"))
        val zapdosCard = mon("zapdos", 200, EnergyType.LIGHTNING, dummy, mechanic = PokemonMechanic.ExLower).copy(
            retreatCost = listOf(EnergyType.COLORLESS, EnergyType.COLORLESS),   // impreso: 2
            abilities = listOf(Ability(
                LocalizedText("Flotación Voltaica", "Voltaic Float"), LocalizedText("", ""),
                EffectsDb.abiKey("sv3pt5-192", "Voltaic Float"))),
        )
        // Solo 1 Energía {L}: con retirada impresa 2 no podría, pero Voltaic Float la vuelve 0.
        val zapdos = PokemonInPlay(zapdosCard).copy(attachedEnergy = listOf(energy("l1", EnergyType.LIGHTNING)))
        val bench = PokemonInPlay(mon("benchy", 60, EnergyType.LIGHTNING, dummy))
        val foe = PokemonInPlay(mon("foe", 100, EnergyType.FIRE, dummy))
        val st = duel(zapdos, foe).let { it.copy(player = it.player.copy(bench = listOf(bench))) }

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Retreat(CardId("benchy")))
        assertEquals(CardId("benchy"), res.state.player.active?.card?.id)   // retirada exitosa
        assertTrue(res.state.player.discard.isEmpty())                      // coste 0: no descarta Energía
    }

    @Test
    fun `FASE 31 - Charmander Destruccion Abrasadora descarta el Estadio en juego`() {
        val atk = attack("Blazing Destruction", 0, EffectsDb.atkKey("sv3pt5-4", "Blazing Destruction"))
        val attacker = PokemonInPlay(mon("charmander", 70, EnergyType.FIRE, atk))
        val foe = PokemonInPlay(mon("foe", 100, EnergyType.WATER, atk))
        val stadium = TrainerCard(
            CardId("cyc"), LocalizedText("Camino de Bicis", "Cycling Road"), set(),
            Rarity.COMMON, "G", art(), TrainerKind.Stadium(), LocalizedText("", ""), EffectId("none"),
        )
        val base = duel(attacker, foe).copy(stadium = stadium, stadiumOwner = Side.OPPONENT)

        val res = GameEngine(SeededRng(1)).apply(base, GameIntent.Attack("Blazing Destruction"))
        assertNull(res.state.stadium)                                          // el Estadio se descartó
        assertTrue(res.state.opponent.discard.any { it.id == CardId("cyc") })  // a la pila de su dueño
    }

    @Test
    fun `FASE 31 - Nidoqueen Prensa Real evita danio solo de atacantes Basicos`() {
        val press = attack("Queen Press", 90, EffectsDb.atkKey("sv3pt5-31", "Queen Press"))
        val hit = attack("hit", 50, EffectId("none"))
        val nido = PokemonInPlay(mon("nidoqueen", 200, EnergyType.DARKNESS, press, stage = Stage.Stage2))

        // Atacante rival BÁSICO → tras Prensa Real, su golpe hace 0 el próximo turno.
        val basicFoe = PokemonInPlay(mon("basicFoe", 200, EnergyType.PSYCHIC, hit))
        val afterPress = GameEngine(SeededRng(1)).apply(duel(nido, basicFoe), GameIntent.Attack("Queen Press"))
        val res = GameEngine(SeededRng(1)).apply(afterPress.state, GameIntent.Attack("hit"))
        assertEquals(0, res.state.player.active!!.damage)

        // Contraste: un atacante EVOLUCIONADO sí hace daño pese a la prevención.
        val evoFoe = PokemonInPlay(mon("evoFoe", 200, EnergyType.PSYCHIC, hit, stage = Stage.Stage1))
        val afterPress2 = GameEngine(SeededRng(1)).apply(duel(nido, evoFoe), GameIntent.Attack("Queen Press"))
        val res2 = GameEngine(SeededRng(1)).apply(afterPress2.state, GameIntent.Attack("hit"))
        assertTrue(res2.state.player.active!!.damage > 0)
    }

    @Test
    fun `FASE 32 - Hitmonchan Contragolpe pone 30 en el atacante al ser danado`() {
        val hit = attack("hit", 50, EffectId("none"))
        val hitmonchan = mon("hitmonchan", 120, EnergyType.FIGHTING, hit).copy(
            abilities = listOf(Ability(
                LocalizedText("Contragolpe", "Counterattack"), LocalizedText("", ""),
                EffectsDb.abiKey("sv3pt5-107", "Counterattack"))),
        )
        // El ATACANTE (jugador) golpea a Hitmonchan (Activo rival) → recibe 30 de contragolpe.
        val attacker = PokemonInPlay(mon("attacker", 200, EnergyType.PSYCHIC, hit))
        val base = duel(attacker, PokemonInPlay(hitmonchan))
        val res = GameEngine(SeededRng(1)).apply(base, GameIntent.Attack("hit"))
        assertEquals(30, res.state.player.active!!.damage)          // atacante recibió el contragolpe
        assertEquals(50, res.state.opponent.active!!.damage)        // Hitmonchan recibió el golpe normal
    }

    @Test
    fun `FASE 32 - Weezing A Pasarlo Bomba noquea al atacante con cara al ser KO`() {
        val big = attack("big", 200, EffectId("none"))      // noquea a Weezing (70 PS)
        val weezing = mon("weezing", 70, EnergyType.DARKNESS, big).copy(
            stage = Stage.Stage1,
            abilities = listOf(Ability(
                LocalizedText("A Pasarlo Bomba", "Let's Have a Blast"), LocalizedText("", ""),
                EffectsDb.abiKey("sv3pt5-110", "Let's Have a Blast"))),
        )
        val attacker = PokemonInPlay(mon("attacker", 120, EnergyType.PSYCHIC, big))
        val base = duel(attacker, PokemonInPlay(weezing))

        // Cara (FixedRng(true)) → el atacante también queda Fuera de Combate.
        val res = GameEngine(FixedRng(true)).apply(base, GameIntent.Attack("big"))
        assertNull(res.state.opponent.active)                       // Weezing KO por el ataque
        assertNull(res.state.player.active)                         // atacante KO por el contragolpe

        // Cruz → el atacante sobrevive.
        val res2 = GameEngine(FixedRng(false)).apply(base, GameIntent.Attack("big"))
        assertNull(res2.state.opponent.active)                      // Weezing KO igual
        assertTrue(res2.state.player.active != null)                // atacante intacto
    }

    @Test
    fun `FASE 32 - Mewtwo Barrera Reflectante devuelve el dano recibido al atacante`() {
        // Mewtwo (Activo rival) programó Barrera Reflectante el turno anterior:
        // reflectDamageOnTurn = 3 (= turno actual del atacante en duel()).
        val hit = attack("hit", 50, EffectId("none"))
        val mewtwo = PokemonInPlay(mon("mewtwo", 130, EnergyType.PSYCHIC, hit))
            .copy(reflectDamageOnTurn = 3)
        val attacker = PokemonInPlay(mon("attacker", 200, EnergyType.DARKNESS, hit))
        val base = duel(attacker, mewtwo)
        val res = GameEngine(SeededRng(1)).apply(base, GameIntent.Attack("hit"))
        assertEquals(50, res.state.opponent.active!!.damage)   // Mewtwo recibió el golpe
        assertEquals(50, res.state.player.active!!.damage)     // atacante recibió el reflejo
    }

    @Test
    fun `FASE 32 - Barrera Reflectante fuera de su turno no refleja`() {
        val hit = attack("hit", 50, EffectId("none"))
        // reflectDamageOnTurn = 99 → no coincide con el turno 3 → no refleja.
        val mewtwo = PokemonInPlay(mon("mewtwo", 130, EnergyType.PSYCHIC, hit))
            .copy(reflectDamageOnTurn = 99)
        val attacker = PokemonInPlay(mon("attacker", 200, EnergyType.DARKNESS, hit))
        val base = duel(attacker, mewtwo)
        val res = GameEngine(SeededRng(1)).apply(base, GameIntent.Attack("hit"))
        assertEquals(0, res.state.player.active!!.damage)      // sin reflejo
    }

    @Test
    fun `FASE 33 - Machamp Agallas sobrevive al KO con cara a 10 PS y muere con cruz`() {
        val big = attack("big", 200, EffectId("none"))   // noquearía a Machamp (90 PS)
        val guts = abi("Guts", EffectsDb.abiKey("sv3pt5-68", "Guts"))
        val machamp = mon("machamp", 90, EnergyType.FIGHTING, big).copy(abilities = listOf(guts))
        val attacker = PokemonInPlay(mon("attacker", 120, EnergyType.PSYCHIC, big))
        val base = duel(attacker, PokemonInPlay(machamp))

        // Cara → no queda KO, PS restantes = 10 (daño = 90 - 10 = 80).
        val res = GameEngine(FixedRng(true)).apply(base, GameIntent.Attack("big"))
        assertEquals(80, res.state.opponent.active!!.damage)

        // Cruz → queda Fuera de Combate.
        val res2 = GameEngine(FixedRng(false)).apply(base, GameIntent.Attack("big"))
        assertNull(res2.state.opponent.active)
    }

    @Test
    fun `FASE 33 - Raichu Toma de Tierra mueve 1 L del Noqueado a Raichu en la Banca`() {
        val big = attack("big", 200, EffectId("none"))   // noquea al Activo rival
        val grounding = abi("Electrical Grounding", EffectsDb.abiKey("sv3pt5-26", "Electrical Grounding"))
        val victim = PokemonInPlay(
            mon("victim", 90, EnergyType.LIGHTNING, big),
            attachedEnergy = listOf(energy("L1", EnergyType.LIGHTNING)),
        )
        val raichu = PokemonInPlay(mon("raichu", 120, EnergyType.LIGHTNING, big).copy(abilities = listOf(grounding)))
        val attacker = PokemonInPlay(mon("attacker", 120, EnergyType.PSYCHIC, big))

        val player = PlayerState(
            side = Side.PLAYER, active = attacker,
            deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) },
            prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = victim, bench = listOf(raichu),
            deck = (1..5).map { energy("od$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("opz$it", EnergyType.WATER) },
        )
        val base = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(base, GameIntent.Attack("big"))
        assertNull(res.state.opponent.active)                       // víctima KO
        val raichuAfter = res.state.opponent.bench.first { it.card.id == CardId("raichu") }
        assertEquals(1, raichuAfter.attachedEnergy.size)            // el {L} se movió a Raichu
    }

    @Test
    fun `DEUDA - Mr Mime Barrera Mimica evita el dano con igual numero de Energias`() {
        val hit = attack("hit", 60, EffectId("none"))
        val barrier = abi("Mimic Barrier", EffectsDb.abiKey("sv3pt5-122", "Mimic Barrier"))
        val mime = mon("mrmime", 70, EnergyType.PSYCHIC, hit).copy(abilities = listOf(barrier))
        // Atacante con 1 Energía.
        val attacker = PokemonInPlay(
            mon("attacker", 120, EnergyType.DARKNESS, hit),
            attachedEnergy = listOf(energy("a1", EnergyType.DARKNESS)),
        )

        // Paridad (1 vs 1) → daño evitado.
        val mimeEqual = PokemonInPlay(mime, attachedEnergy = listOf(energy("m1", EnergyType.PSYCHIC)))
        val eq = GameEngine(SeededRng(1)).apply(duel(attacker, mimeEqual), GameIntent.Attack("hit"))
        assertEquals(0, eq.state.opponent.active!!.damage)

        // Sin paridad (1 vs 0) → daño normal.
        val mimeUnequal = PokemonInPlay(mime)
        val ne = GameEngine(SeededRng(1)).apply(duel(attacker, mimeUnequal), GameIntent.Attack("hit"))
        assertEquals(60, ne.state.opponent.active!!.damage)
    }

    @Test
    fun `FASE 34 - Primeape Golpe Rabioso no hace nada si no esta Confundido`() {
        val a = attack("Raging Smash", 150, EffectsDb.atkKey("sv3pt5-57", "Raging Smash"))
        val foe = PokemonInPlay(mon("target", 300, EnergyType.PSYCHIC, a))

        // Sin Confusión → 0 daño.
        val notConf = PokemonInPlay(mon("primeape", 110, EnergyType.FIGHTING, a))
        val r1 = GameEngine(SeededRng(1)).apply(duel(notConf, foe), GameIntent.Attack("Raging Smash"))
        assertEquals(0, r1.state.opponent.active!!.damage)

        // Confundido → 150 daño.
        val conf = PokemonInPlay(
            mon("primeape", 110, EnergyType.FIGHTING, a),
            statuses = setOf(com.mineralord.tcg.engine.model.Status.CONFUSED),
        )
        val r2 = GameEngine(SeededRng(1)).apply(duel(conf, foe), GameIntent.Attack("Raging Smash"))
        assertEquals(150, r2.state.opponent.active!!.damage)
    }

    @Test
    fun `FASE 34 - Slowbro Placaje Relajado no hace nada si evoluciono este turno`() {
        val a = attack("Laid-Back Tackle", 160, EffectsDb.atkKey("sv3pt5-80", "Laid-Back Tackle"))
        val foe = PokemonInPlay(mon("target", 300, EnergyType.LIGHTNING, a))
        val slowbroCard = mon("slowbro", 150, EnergyType.PSYCHIC, a, stage = Stage.Stage1)

        // turnsInPlay = 0 (evolucionó este turno) → 0 daño.
        val fresh = PokemonInPlay(slowbroCard, turnsInPlay = 0)
        val r1 = GameEngine(SeededRng(1)).apply(duel(fresh, foe), GameIntent.Attack("Laid-Back Tackle"))
        assertEquals(0, r1.state.opponent.active!!.damage)

        // turnsInPlay = 1 → 160 daño.
        val settled = PokemonInPlay(slowbroCard, turnsInPlay = 1)
        val r2 = GameEngine(SeededRng(1)).apply(duel(settled, foe), GameIntent.Attack("Laid-Back Tackle"))
        assertEquals(160, r2.state.opponent.active!!.damage)
    }

    @Test
    fun `FASE 34 - Clefable Mas Luna coge 1 Premio extra al noquear`() {
        val a = attack("More Moon", 50, EffectsDb.atkKey("sv3pt5-36", "More Moon"))
        val clefable = PokemonInPlay(mon("clefable", 110, EnergyType.PSYCHIC, a))
        val frail = PokemonInPlay(mon("target", 50, EnergyType.DARKNESS, a))   // 50 PS → KO con 50
        val r = GameEngine(SeededRng(1)).apply(duel(clefable, frail), GameIntent.Attack("More Moon"))
        assertNull(r.state.opponent.active)                 // noqueado
        assertEquals(4, r.state.player.prizes.size)         // cogió 2 Premios (1 + 1 extra), 6 - 2 = 4
    }

    @Test
    fun `FASE 26 - Canones Gemelos descarta 2 W de la mano y hace 140 por carta`() {
        val a = attack("Twin Cannons", 0, EffectsDb.atkKey("sv3pt5-9", "Twin Cannons"))
        val attacker = PokemonInPlay(mon("blastoise", 330, EnergyType.WATER, a))
        val foe = PokemonInPlay(mon("target", 600, EnergyType.LIGHTNING, a))
        val base = duel(attacker, foe)
        val hand = listOf(
            energy("w1", EnergyType.WATER), energy("w2", EnergyType.WATER), energy("f1", EnergyType.FIRE),
        )
        val st = base.copy(player = base.player.copy(hand = hand))

        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Twin Cannons"))
        val d = paused.state.interaction!!.decision as PendingDecision.SearchCards
        // Solo las 2 {W} básicas son candidatas (el Fuego no).
        assertEquals(setOf("w1", "w2"), d.candidates.map { it.raw }.toSet())

        // Descarta las 2 {W} → 280 de daño y ambas al descarte.
        val res = GameEngine(SeededRng(1)).apply(
            paused.state, GameIntent.ResolveDecision(listOf(CardId("w1"), CardId("w2"))))
        assertEquals(280, res.events.filterIsInstance<GameEvent.DamageDealt>().sumOf { it.amount })
        assertEquals(280, res.state.opponent.active!!.damage)
        assertTrue(res.state.player.discard.map { it.id.raw }.containsAll(listOf("w1", "w2")))
    }

    @Test
    fun `FASE 26 - Canones Gemelos con 1 W descartada hace 140`() {
        val a = attack("Twin Cannons", 0, EffectsDb.atkKey("sv3pt5-9", "Twin Cannons"))
        val attacker = PokemonInPlay(mon("blastoise", 330, EnergyType.WATER, a))
        val foe = PokemonInPlay(mon("target", 600, EnergyType.LIGHTNING, a))
        val base = duel(attacker, foe)
        val st = base.copy(player = base.player.copy(hand = listOf(energy("w1", EnergyType.WATER))))

        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Twin Cannons"))
        val res = GameEngine(SeededRng(1)).apply(
            paused.state, GameIntent.ResolveDecision(listOf(CardId("w1"))))
        assertEquals(140, res.state.opponent.active!!.damage)
    }

    @Test
    fun `FASE 25 - Aguijon Nadir con mano vacia suma 120 y aplica Veneno+Paralisis`() {
        val a = attack("Nadir Needle", 0, EffectsDb.atkKey("sv3pt5-15", "Nadir Needle"))
        val attacker = PokemonInPlay(mon("beedrill", 120, EnergyType.GRASS, a))
        val foe = PokemonInPlay(mon("target", 400, EnergyType.FIRE, a))

        // Mano del jugador vacía (por defecto en duel): 30 + 120 = 150 y estado.
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Nadir Needle"))
        assertEquals(150, res.events.filterIsInstance<GameEvent.DamageDealt>().sumOf { it.amount })
        val statuses = res.state.opponent.active!!.statuses
        assertTrue(statuses.contains(Status.POISONED) && statuses.contains(Status.PARALYZED))
    }

    @Test
    fun `FASE 25 - Aguijon Nadir con cartas en mano solo hace 30 sin estado`() {
        val a = attack("Nadir Needle", 0, EffectsDb.atkKey("sv3pt5-15", "Nadir Needle"))
        val attacker = PokemonInPlay(mon("beedrill", 120, EnergyType.GRASS, a))
        val foe = PokemonInPlay(mon("target", 400, EnergyType.FIRE, a))

        val base = duel(attacker, foe)
        val withHand = base.copy(player = base.player.copy(hand = listOf(energy("mano1", EnergyType.GRASS))))
        val res = GameEngine(SeededRng(1)).apply(withHand, GameIntent.Attack("Nadir Needle"))
        assertEquals(30, res.events.filterIsInstance<GameEvent.DamageDealt>().sumOf { it.amount })
        assertTrue(res.state.opponent.active!!.statuses.isEmpty())
    }

    @Test
    fun `retroceso auto-daño (Thunder)`() {
        val a = attack("Thunder", 100, EffectsDb.atkKey("sv3pt5-26", "Thunder"))
        val attacker = PokemonInPlay(mon("raichu", 120, EnergyType.LIGHTNING, a))
        val foe = PokemonInPlay(mon("target", 400, EnergyType.WATER, a))

        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Thunder"))
        // El atacante se hizo 50 de daño a sí mismo.
        assertEquals(50, res.state.player.active!!.damage)
    }

    @Test
    fun `descarta energía propia (Explosive Vortex descarta 3)`() {
        val a = attack("Explosive Vortex", 180, EffectsDb.atkKey("sv3pt5-183", "Explosive Vortex"), cost = 3)
        val attacker = PokemonInPlay(
            mon("charizard", 330, EnergyType.FIRE, a),
            attachedEnergy = (1..4).map { energy("fe$it", EnergyType.FIRE) },
        )
        val foe = PokemonInPlay(mon("target", 400, EnergyType.WATER, a))

        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Explosive Vortex"))
        assertEquals(1, res.state.player.active!!.attachedEnergy.size)
    }

    // ------------------------------------------------------------------ Fase 2

    @Test
    fun `prevención de daño - defensor marcado no recibe daño`() {
        val a = attack("Golpe", 50, EffectId("none"))
        val attacker = PokemonInPlay(mon("atk", 200, EnergyType.LIGHTNING, a))
        val foeCard = mon("def", 200, EnergyType.WATER, a)
        // Defensor con la marca activa para el turno actual (3): se evita el daño.
        val shielded = PokemonInPlay(foeCard, preventDamageOnTurn = 3)
        val normal = PokemonInPlay(foeCard)

        val prevented = GameEngine(SeededRng(1)).apply(duel(attacker, shielded), GameIntent.Attack("Golpe"))
        assertEquals(0, prevented.state.opponent.active!!.damage)

        val hit = GameEngine(SeededRng(1)).apply(duel(attacker, normal), GameIntent.Attack("Golpe"))
        assertEquals(50, hit.state.opponent.active!!.damage)
    }

    @Test
    fun `Withdraw marca prevención en sí mismo con cara, no con cruz`() {
        val a = attack("Withdraw", 0, EffectsDb.atkKey("sv3pt5-7", "Withdraw"))
        val squirtle = PokemonInPlay(mon("squirtle", 70, EnergyType.WATER, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.GRASS, a))

        val heads = GameEngine(FixedRng(true)).apply(duel(squirtle, foe), GameIntent.Attack("Withdraw"))
        // turno 3 → marca el próximo turno del rival (4).
        assertEquals(4, heads.state.player.active!!.preventDamageOnTurn)

        val tails = GameEngine(FixedRng(false)).apply(duel(squirtle, foe), GameIntent.Attack("Withdraw"))
        assertNull(tails.state.player.active!!.preventDamageOnTurn)
    }

    @Test
    fun `Bind Down marca al defensor y le bloquea la retirada`() {
        val a = attack("Bind Down", 70, EffectsDb.atkKey("sv3pt5-24", "Bind Down"))
        val arbok = PokemonInPlay(mon("arbok", 180, EnergyType.DARKNESS, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIGHTING, a))

        val res = GameEngine(SeededRng(1)).apply(duel(arbok, foe), GameIntent.Attack("Bind Down"))
        assertEquals(4, res.state.opponent.active!!.cannotRetreatOnTurn)

        // En SU turno (4), el defensor marcado no puede retirarse aunque tenga energía y banca.
        val bound = PokemonInPlay(mon("foe", 200, EnergyType.FIGHTING, a), cannotRetreatOnTurn = 4)
        val benchMon = mon("bench", 60, EnergyType.FIGHTING, a)
        val oppTurn = GameState(
            player = PlayerState(Side.PLAYER, PokemonInPlay(mon("p", 100, EnergyType.LIGHTNING, a))),
            opponent = PlayerState(
                Side.OPPONENT,
                active = bound.copy(attachedEnergy = listOf(energy("e", EnergyType.FIGHTING))),
                bench = listOf(PokemonInPlay(benchMon)),
            ),
            turn = 4, activeSide = Side.OPPONENT, phase = Phase.MAIN,
        )
        val retreat = GameEngine(SeededRng(1)).apply(oppTurn, GameIntent.Retreat(CardId("bench")))
        assertFalse(retreat.accepted)
    }

    // ------------------------------------------------------------------ Fase 3

    @Test
    fun `Acid Spray descarta energía del rival con cara y al descarte del rival`() {
        val a = attack("Acid Spray", 30, EffectsDb.atkKey("sv3pt5-23", "Acid Spray"))
        val attacker = PokemonInPlay(mon("ekans", 70, EnergyType.DARKNESS, a))
        val foe = PokemonInPlay(
            mon("foe", 200, EnergyType.WATER, a),
            attachedEnergy = (1..2).map { energy("we$it", EnergyType.WATER) },
        )

        val heads = GameEngine(FixedRng(true)).apply(duel(attacker, foe), GameIntent.Attack("Acid Spray"))
        assertEquals(1, heads.state.opponent.active!!.attachedEnergy.size)
        assertEquals(1, heads.state.opponent.discard.size)  // fue al descarte del DUEÑO (rival)

        val tails = GameEngine(FixedRng(false)).apply(duel(attacker, foe), GameIntent.Attack("Acid Spray"))
        assertEquals(2, tails.state.opponent.active!!.attachedEnergy.size)
    }

    @Test
    fun `Hyper Beam descarta energía del rival sin moneda`() {
        val a = attack("Hyper Beam", 200, EffectsDb.atkKey("sv3pt5-130", "Hyper Beam"))
        val attacker = PokemonInPlay(mon("porygon", 300, EnergyType.COLORLESS, a))
        val foe = PokemonInPlay(
            mon("foe", 400, EnergyType.WATER, a),
            attachedEnergy = (1..3).map { energy("e$it", EnergyType.WATER) },
        )
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Hyper Beam"))
        assertEquals(2, res.state.opponent.active!!.attachedEnergy.size)
        assertEquals(1, res.state.opponent.discard.size)
    }

    @Test
    fun `Beak Catch abre una búsqueda a la mano de 3 cartas`() {
        val a = attack("Beak Catch", 0, EffectsDb.atkKey("sv3pt5-22", "Beak Catch"))
        val attacker = PokemonInPlay(mon("fearow", 110, EnergyType.COLORLESS, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIGHTING, a))

        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Beak Catch"))
        val search = res.pending.filterIsInstance<PendingDecision.SearchCards>().firstOrNull()
        assertTrue(search != null, "debía abrir una búsqueda")
        assertEquals(3, search.count)
        assertEquals(Zone.HAND, search.destination)
    }

    // ------------------------------------------------------------------ Fase 4

    @Test
    fun `Charge busca una Energía Rayo del mazo y la une a sí mismo`() {
        val a = attack("Charge", 0, EffectsDb.atkKey("sv3pt5-25", "Charge"))
        val pikachu = PokemonInPlay(mon("pikachu", 70, EnergyType.LIGHTNING, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIGHTING, a))
        // Mazo con una Energía Rayo Básica que debe encontrarse.
        val player = PlayerState(
            side = Side.PLAYER, active = pikachu,
            deck = listOf(energy("lightning-1", EnergyType.LIGHTNING)) + (1..4).map { energy("w$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = foe,
            deck = (1..3).map { energy("od$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("opz$it", EnergyType.WATER) },
        )
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Charge"))
        assertEquals(1, res.state.player.active!!.attachedEnergy.size)
        assertEquals(EnergyType.LIGHTNING, (res.state.player.active!!.attachedEnergy.first() as com.mineralord.tcg.engine.model.BasicEnergy).type)
        // La energía salió del mazo.
        assertFalse(res.state.player.deck.any { it.id.raw == "lightning-1" })
    }

    @Test
    fun `Vaporize descarta solo Energía Agua del rival`() {
        val a = attack("Vaporize", 10, EffectsDb.atkKey("sv3pt5-58", "Vaporize"))
        val attacker = PokemonInPlay(mon("vaporeon", 120, EnergyType.WATER, a))
        // Rival con 1 Fuego + 1 Agua: solo se descarta la de Agua.
        val foe = PokemonInPlay(
            mon("foe", 200, EnergyType.FIRE, a),
            attachedEnergy = listOf(energy("fire", EnergyType.FIRE), energy("water", EnergyType.WATER)),
        )
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Vaporize"))
        val left = res.state.opponent.active!!.attachedEnergy
        assertEquals(1, left.size)
        assertEquals("fire", left.first().id.raw)  // quedó la de Fuego
    }

    @Test
    fun `Spiral Drain cura 30 al propio Pokémon`() {
        val a = attack("Spiral Drain", 30, EffectsDb.atkKey("sv3pt5-134", "Spiral Drain"))
        val attacker = PokemonInPlay(mon("clodsire", 130, EnergyType.WATER, a), damage = 50)
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.LIGHTNING, a))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Spiral Drain"))
        assertEquals(20, res.state.player.active!!.damage)  // 50 - 30
    }

    // ------------------------------------------------------------------ Fase 5

    @Test
    fun `Fighting Whirlpool pega +90 solo contra ex o V`() {
        val a = attack("Fighting Whirlpool", 0, EffectsDb.atkKey("sv3pt5-134", "Fighting Whirlpool"))
        val attacker = PokemonInPlay(mon("great-tusk", 200, EnergyType.FIGHTING, a))
        val normal = PokemonInPlay(mon("n", 400, EnergyType.WATER, a))
        val exFoe = PokemonInPlay(mon("ex", 400, EnergyType.WATER, a, mechanic = PokemonMechanic.ExLower))

        assertEquals(90, GameEngine(SeededRng(1)).apply(duel(attacker, normal), GameIntent.Attack("Fighting Whirlpool"))
            .events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
        assertEquals(180, GameEngine(SeededRng(1)).apply(duel(attacker, exFoe), GameIntent.Attack("Fighting Whirlpool"))
            .events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    @Test
    fun `Spinning Fumes pega 10 a cada Pokémon de la Banca rival`() {
        val a = attack("Spinning Fumes", 50, EffectsDb.atkKey("sv3pt5-110", "Spinning Fumes"))
        val attacker = PokemonInPlay(mon("weezing", 140, EnergyType.PSYCHIC, a))
        val foeActive = PokemonInPlay(mon("act", 300, EnergyType.FIGHTING, a))
        val b1 = PokemonInPlay(mon("b1", 100, EnergyType.WATER, a))
        val b2 = PokemonInPlay(mon("b2", 100, EnergyType.GRASS, a))
        val player = PlayerState(Side.PLAYER, attacker, deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foeActive, bench = listOf(b1, b2), deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Spinning Fumes"))
        assertEquals(50, res.state.opponent.active!!.damage)
        assertTrue(res.state.opponent.bench.all { it.damage == 10 })
    }

    @Test
    fun `Bone Throw abre elección de un Pokémon de la Banca rival`() {
        val a = attack("Bone Throw", 30, EffectsDb.atkKey("sv3pt5-105", "Bone Throw"))
        val attacker = PokemonInPlay(mon("marowak", 120, EnergyType.FIGHTING, a))
        val foeActive = PokemonInPlay(mon("act", 300, EnergyType.LIGHTNING, a))
        val b1 = PokemonInPlay(mon("b1", 100, EnergyType.WATER, a))
        val player = PlayerState(Side.PLAYER, attacker, deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foeActive, bench = listOf(b1), deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Bone Throw"))
        val choose = res.pending.filterIsInstance<PendingDecision.ChooseTargets>().firstOrNull()
        assertTrue(choose != null, "debía abrir elección de banca")
        assertTrue(choose.candidates.contains(CardId("b1")))
    }

    // ------------------------------------------------------------------ Fase 6

    @Test
    fun `Carisma de Giovanni devuelve energía del rival y une una de tu mano`() {
        val a = attack("x", 0, EffectId("none"))
        val giovanni = TrainerCard(
            CardId("sv3pt5-161"), LocalizedText("Carisma de Giovanni", "Giovanni's Charisma"), set(),
            Rarity.RARE, "H", art(), TrainerKind.Supporter(), LocalizedText("", ""),
            EffectId("sv3pt5-161"),
        )
        val myActive = PokemonInPlay(mon("mine", 200, EnergyType.FIRE, a))
        val handEnergy = energy("hand-fire", EnergyType.FIRE)
        val player = PlayerState(
            side = Side.PLAYER, active = myActive, hand = listOf(giovanni, handEnergy),
            deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) },
            prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) },
        )
        val foeActive = PokemonInPlay(
            mon("foe", 200, EnergyType.WATER, a),
            attachedEnergy = listOf(energy("foe-e1", EnergyType.WATER), energy("foe-e2", EnergyType.WATER)),
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = foeActive,
            deck = (1..5).map { energy("od$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("opz$it", EnergyType.WATER) },
        )
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.PlayTrainer(CardId("sv3pt5-161")))
        assertTrue(res.accepted, res.rejection)
        // El rival perdió 1 energía del Activo (la recuperó en su mano).
        assertEquals(1, res.state.opponent.active!!.attachedEnergy.size)
        assertEquals(1, res.state.opponent.hand.count { it.id.raw.startsWith("foe-e") })
        // Mi Activo ganó la energía que tenía en mano.
        assertEquals(1, res.state.player.active!!.attachedEnergy.size)
        assertFalse(res.state.player.hand.any { it.id.raw == "hand-fire" })
    }

    // ---------------------- Fase 7: HABILIDADES (retirada) ----------------------

    /** Pokémon con habilidad(es) y coste de retirada configurables. */
    private fun monAbi(
        id: String, hp: Int, type: EnergyType,
        retreat: List<EnergyType> = emptyList(),
        abilities: List<Ability> = emptyList(),
    ) = PokemonCard(
        id = CardId(id), name = LocalizedText(id, id), set = set(),
        rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
        stage = Stage.Basic, mechanic = PokemonMechanic.Normal, hp = hp,
        types = listOf(type), evolvesFrom = null, abilities = abilities,
        attacks = emptyList(), weaknesses = emptyList(), resistances = emptyList(),
        retreatCost = retreat, rulesText = emptyList(),
    )

    private fun abi(nameEn: String, key: EffectId) =
        Ability(LocalizedText(nameEn, nameEn), LocalizedText("", ""), key)

    private fun retreatState(mine: PokemonInPlay, benchMine: List<PokemonInPlay>, foe: PokemonInPlay): GameState {
        val player = PlayerState(
            side = Side.PLAYER, active = mine, bench = benchMine,
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

    @Test
    fun `Flotacion Glacial - sin coste de retirada si tiene Energia Agua`() {
        val iceFloat = abi("Ice Float", EffectsDb.abiKey("sv3pt5-144", "Ice Float"))
        // Activo con coste de retirada {C}{C} pero SIN energía suficiente: 1 Energía Agua.
        val withWater = PokemonInPlay(
            monAbi("articuno", 120, EnergyType.WATER, retreat = listOf(EnergyType.COLORLESS, EnergyType.COLORLESS), abilities = listOf(iceFloat)),
            attachedEnergy = listOf(energy("w1", EnergyType.WATER)),
        )
        val bench = PokemonInPlay(monAbi("bench", 60, EnergyType.WATER))
        val foe = PokemonInPlay(monAbi("foe", 200, EnergyType.FIRE))

        // Con Energía Agua la retirada es gratis (aunque el coste impreso sea 2 y solo tenga 1 energía).
        val res = GameEngine(SeededRng(1)).apply(retreatState(withWater, listOf(bench), foe), GameIntent.Retreat(CardId("bench")))
        assertTrue(res.accepted, res.rejection)
        assertEquals("bench", res.state.player.active!!.card.id.raw)
        // No se descartó energía (coste 0): la Energía Agua sigue en el ahora-en-banca Articuno.
        assertTrue(res.state.player.bench.first { it.card.id.raw == "articuno" }.attachedEnergy.isNotEmpty())
    }

    @Test
    fun `Flotacion Glacial - sin Energia Agua el coste impreso se aplica`() {
        val iceFloat = abi("Ice Float", EffectsDb.abiKey("sv3pt5-144", "Ice Float"))
        val noWater = PokemonInPlay(
            monAbi("articuno", 120, EnergyType.WATER, retreat = listOf(EnergyType.COLORLESS, EnergyType.COLORLESS), abilities = listOf(iceFloat)),
            attachedEnergy = listOf(energy("f1", EnergyType.FIRE)),   // solo 1 energía, no Agua
        )
        val bench = PokemonInPlay(monAbi("bench", 60, EnergyType.WATER))
        val foe = PokemonInPlay(monAbi("foe", 200, EnergyType.FIRE))

        val res = GameEngine(SeededRng(1)).apply(retreatState(noWater, listOf(bench), foe), GameIntent.Retreat(CardId("bench")))
        assertFalse(res.accepted)   // coste 2, solo 1 energía → rechazado
    }

    @Test
    fun `Travesia Propulsion - un Pokemon en banca anula el coste de todos`() {
        val jetCruise = abi("Jet Cruise", EffectsDb.abiKey("sv3pt5-149", "Jet Cruise"))
        val active = PokemonInPlay(
            monAbi("active", 120, EnergyType.DRAGON, retreat = listOf(EnergyType.COLORLESS, EnergyType.COLORLESS)),
        )
        val dragoniteBench = PokemonInPlay(monAbi("dragonite", 160, EnergyType.DRAGON, abilities = listOf(jetCruise)))
        val foe = PokemonInPlay(monAbi("foe", 200, EnergyType.FIRE))

        val res = GameEngine(SeededRng(1)).apply(retreatState(active, listOf(dragoniteBench), foe), GameIntent.Retreat(CardId("dragonite")))
        assertTrue(res.accepted, res.rejection)   // coste 2 anulado por Jet Cruise, 0 energía basta
    }

    @Test
    fun `Tentaculos Primordiales - el Activo rival bloquea mi retirada`() {
        val tentacles = abi("Primordial Tentacles", EffectsDb.abiKey("sv3pt5-139", "Primordial Tentacles"))
        val active = PokemonInPlay(
            monAbi("active", 120, EnergyType.WATER),   // sin coste de retirada
        )
        val bench = PokemonInPlay(monAbi("bench", 60, EnergyType.WATER))
        val omastar = PokemonInPlay(monAbi("omastar", 130, EnergyType.WATER, abilities = listOf(tentacles)))

        val res = GameEngine(SeededRng(1)).apply(retreatState(active, listOf(bench), omastar), GameIntent.Retreat(CardId("bench")))
        assertFalse(res.accepted)   // Omastar Activo rival impide retirarse
    }

    @Test
    fun `Rescate Acuatico - recupera Pokemon del descarte a la mano`() {
        val a = attack("Aquatic Rescue", 0, EffectsDb.atkKey("sv3pt5-55", "Aquatic Rescue"))
        val golduck = PokemonInPlay(mon("sv3pt5-55", 120, EnergyType.WATER, a))
        val dead1 = mon("dead1", 60, EnergyType.WATER, a)
        val dead2 = mon("dead2", 60, EnergyType.FIRE, a)
        val player = PlayerState(
            side = Side.PLAYER, active = golduck,
            deck = (1..5).map { energy("d$it", EnergyType.WATER) },
            discard = listOf(dead1, dead2),
            prizes = (1..6).map { energy("pz$it", EnergyType.WATER) },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = PokemonInPlay(mon("foe", 200, EnergyType.LIGHTNING, a)),
            deck = (1..5).map { energy("od$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("opz$it", EnergyType.WATER) },
        )
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val eng = GameEngine(SeededRng(1))
        val paused = eng.apply(st, GameIntent.Attack("Aquatic Rescue"))
        assertTrue(paused.state.awaitingDecision)
        val done = eng.apply(paused.state, GameIntent.ResolveDecision(listOf(CardId("dead1"))))
        assertTrue(done.state.player.hand.any { it.id.raw == "dead1" })
        assertFalse(done.state.player.discard.any { it.id.raw == "dead1" })
        assertTrue(done.state.player.discard.any { it.id.raw == "dead2" })   // el no elegido se queda
    }

    @Test
    fun `Buceo Libre - solo Energia Agua Basica del descarte es candidata`() {
        val a = attack("Free Diving", 0, EffectsDb.atkKey("sv3pt5-8", "Free Diving"))
        val wartortle = PokemonInPlay(mon("sv3pt5-8", 90, EnergyType.WATER, a))
        val player = PlayerState(
            side = Side.PLAYER, active = wartortle,
            deck = (1..5).map { energy("d$it", EnergyType.WATER) },
            discard = listOf(energy("w1", EnergyType.WATER), energy("f1", EnergyType.FIRE), energy("w2", EnergyType.WATER)),
            prizes = (1..6).map { energy("pz$it", EnergyType.WATER) },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = PokemonInPlay(mon("foe", 200, EnergyType.LIGHTNING, a)),
            deck = (1..5).map { energy("od$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("opz$it", EnergyType.WATER) },
        )
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Free Diving"))
        val d = paused.state.interaction!!.decision as PendingDecision.SearchCards
        assertEquals(setOf("w1", "w2"), d.candidates.map { it.raw }.toSet())   // el Fuego no es candidato
    }

    @Test
    fun `Persian Llamar al Rocket - habilidad registrada 1 por turno busca Giovanni`() {
        val rocketCall = abi("Rocket Call", EffectsDb.abiKey("sv3pt5-53", "Rocket Call"))
        val persian = PokemonInPlay(monAbi("sv3pt5-53", 90, EnergyType.DARKNESS, abilities = listOf(rocketCall)))
        val giovanni = TrainerCard(
            CardId("sv3pt5-161"), LocalizedText("Carisma de Giovanni", "Giovanni's Charisma"), set(),
            Rarity.RARE, "H", art(), TrainerKind.Supporter(), LocalizedText("", ""),
            EffectId("sv3pt5-161"),
        )
        val player = PlayerState(
            side = Side.PLAYER, active = persian,
            deck = listOf(giovanni) + (1..4).map { energy("d$it", EnergyType.LIGHTNING) },
            prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = PokemonInPlay(monAbi("foe", 200, EnergyType.WATER)),
            deck = (1..5).map { energy("od$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("opz$it", EnergyType.WATER) },
        )
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.UseAbility(CardId("sv3pt5-53"), "Rocket Call"))
        assertTrue(res.accepted, res.rejection)
        // Pausa pidiendo elegir la carta buscada (Carisma de Giovanni está en el mazo).
        assertTrue(res.pending.any { it is PendingDecision.SearchCards })
    }

    // ---------------------- Fase 8: búsquedas / robo / descarte de energía ----------------------

    @Test
    fun `Bomba Acida - cara descarta 1 Energia del Activo rival, cruz no`() {
        val a = attack("Acid Spray", 30, EffectsDb.atkKey("sv3pt5-23", "Acid Spray"))
        val attacker = PokemonInPlay(mon("ekans", 70, EnergyType.DARKNESS, a))
        fun foe() = PokemonInPlay(
            mon("target", 200, EnergyType.GRASS, a),
            attachedEnergy = listOf(energy("fe1", EnergyType.WATER), energy("fe2", EnergyType.WATER)),
        )
        val heads = GameEngine(FixedRng(true)).apply(duel(attacker, foe()), GameIntent.Attack("Acid Spray"))
        assertEquals(1, heads.state.opponent.active!!.attachedEnergy.size)
        val tails = GameEngine(FixedRng(false)).apply(duel(attacker, foe()), GameIntent.Attack("Acid Spray"))
        assertEquals(2, tails.state.opponent.active!!.attachedEnergy.size)
    }

    @Test
    fun `Hiperrayo - descarta 1 Energia del Activo rival`() {
        val a = attack("Hyper Beam", 200, EffectsDb.atkKey("sv3pt5-130", "Hyper Beam"))
        val attacker = PokemonInPlay(mon("gyarados", 180, EnergyType.WATER, a))
        val foe = PokemonInPlay(
            mon("target", 400, EnergyType.GRASS, a),
            attachedEnergy = listOf(energy("fe1", EnergyType.WATER), energy("fe2", EnergyType.WATER)),
        )
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Hyper Beam"))
        assertEquals(1, res.state.opponent.active!!.attachedEnergy.size)
    }

    @Test
    fun `Pisotonazo - 2 monedas, 20 por cara`() {
        val a = attack("Stompy Stomp", 0, EffectsDb.atkKey("sv3pt5-39", "Stompy Stomp"))
        val attacker = PokemonInPlay(mon("jigglypuff", 70, EnergyType.COLORLESS, a))
        val foe = PokemonInPlay(mon("target", 300, EnergyType.FIGHTING, a))
        val heads = GameEngine(FixedRng(true)).apply(duel(attacker, foe), GameIntent.Attack("Stompy Stomp"))
        assertEquals(40, heads.events.filterIsInstance<GameEvent.DamageDealt>().sumOf { it.amount })
        val tails = GameEngine(FixedRng(false)).apply(duel(attacker, foe), GameIntent.Attack("Stompy Stomp"))
        assertEquals(0, tails.events.filterIsInstance<GameEvent.DamageDealt>().sumOf { it.amount })
    }

    @Test
    fun `Drenadoras - cura 20 al propio Pokemon`() {
        val a = attack("Leech Seed", 30, EffectsDb.atkKey("sv3pt5-2", "Leech Seed"))
        val attacker = PokemonInPlay(mon("ivysaur", 100, EnergyType.GRASS, a), damage = 30)
        val foe = PokemonInPlay(mon("target", 300, EnergyType.WATER, a))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Leech Seed"))
        assertEquals(10, res.state.player.active!!.damage)   // 30 - 20 curados
    }

    @Test
    fun `Bano de Mar - cura 30 y quita Condiciones Especiales`() {
        val a = attack("Sea Bathing", 0, EffectsDb.atkKey("sv3pt5-79", "Sea Bathing"))
        val attacker = PokemonInPlay(mon("slowpoke", 90, EnergyType.PSYCHIC, a), damage = 30, statuses = setOf(Status.POISONED))
        val foe = PokemonInPlay(mon("target", 200, EnergyType.PSYCHIC, a))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Sea Bathing"))
        assertEquals(0, res.state.player.active!!.damage)
        assertTrue(res.state.player.active!!.statuses.isEmpty())
    }

    @Test
    fun `Gran Bostezo - ambos Activos quedan Dormidos`() {
        val a = attack("Big Yawn", 0, EffectsDb.atkKey("sv3pt5-80", "Big Yawn"))
        val attacker = PokemonInPlay(mon("slowbro", 150, EnergyType.PSYCHIC, a))
        val foe = PokemonInPlay(mon("target", 200, EnergyType.PSYCHIC, a))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Big Yawn"))
        assertTrue(res.state.opponent.active!!.statuses.contains(Status.ASLEEP))
        assertTrue(res.state.player.active!!.statuses.contains(Status.ASLEEP))
    }

    @Test
    fun `Impacto Envenenado - Activo rival Envenenado`() {
        val a = attack("Venomous Impact", 190, EffectsDb.atkKey("sv3pt5-174", "Venomous Impact"))
        val attacker = PokemonInPlay(mon("nidoking", 180, EnergyType.PSYCHIC, a))
        val foe = PokemonInPlay(mon("target", 400, EnergyType.GRASS, a))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Venomous Impact"))
        assertTrue(res.state.opponent.active!!.statuses.contains(Status.POISONED))
    }

    @Test
    fun `Traba-Lengua - el Defensor no puede atacar su proximo turno`() {
        val a = attack("Tongue-Tied", 70, EffectsDb.atkKey("sv3pt5-108", "Tongue-Tied"))
        val attacker = PokemonInPlay(mon("lickitung", 110, EnergyType.COLORLESS, a))
        val foe = PokemonInPlay(mon("target", 300, EnergyType.FIGHTING, a))
        val st = duel(attacker, foe)
        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Tongue-Tied"))
        assertEquals(st.turn + 1, res.state.opponent.active!!.cannotAttackOnTurn)
    }

    @Test
    fun `Captura Pico - pausa buscando hasta 3 cartas del mazo`() {
        val a = attack("Beak Catch", 0, EffectsDb.atkKey("sv3pt5-22", "Beak Catch"))
        val attacker = PokemonInPlay(mon("fearow", 110, EnergyType.COLORLESS, a))
        val foe = PokemonInPlay(mon("target", 200, EnergyType.LIGHTNING, a))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Beak Catch"))
        assertTrue(res.accepted, res.rejection)
        assertTrue(res.pending.any { it is PendingDecision.SearchCards })
    }

    // ------------------------------------------------------------------ Fase 11

    @Test
    fun `snipe con banca rival vacía hace el daño base y NO deja decisión pendiente`() {
        // Bone Throw (30 base, +30 a 1 de la Banca rival). Sin banca rival, la elección
        // (optional=true) se salta y el ataque hace su daño base al Activo rival.
        val a = attack("Bone Throw", 30, EffectsDb.atkKey("sv3pt5-105", "Bone Throw"))
        val attacker = PokemonInPlay(mon("marowak", 120, EnergyType.FIGHTING, a))
        val foe = PokemonInPlay(mon("act", 300, EnergyType.LIGHTNING, a))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Bone Throw"))
        assertTrue(res.accepted, res.rejection)
        assertTrue(res.pending.none { it is PendingDecision.ChooseTargets }, "no debía dejar elección vacía")
        assertEquals(30, res.state.opponent.active!!.damage)
    }

    @Test
    fun `Skill Dive abre elección de cualquier Pokémon del rival`() {
        val a = attack("Skill Dive", 0, EffectsDb.atkKey("sv3pt5-42", "Skill Dive"))
        val attacker = PokemonInPlay(mon("golbat", 80, EnergyType.DARKNESS, a))
        val foeActive = PokemonInPlay(mon("act", 300, EnergyType.LIGHTNING, a))
        val b1 = PokemonInPlay(mon("b1", 100, EnergyType.WATER, a))
        val player = PlayerState(Side.PLAYER, attacker, deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foeActive, bench = listOf(b1), deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Skill Dive"))
        val choose = res.pending.filterIsInstance<PendingDecision.ChooseTargets>().firstOrNull()
        assertTrue(choose != null, "debía abrir elección")
        assertTrue(choose.candidates.contains(CardId("act")))
        assertTrue(choose.candidates.contains(CardId("b1")))
    }

    @Test
    fun `Giro Mach con Banca propia abre elección para retirarse`() {
        val a = attack("Mach Turn", 90, EffectsDb.atkKey("sv3pt5-78", "Mach Turn"))
        val attacker = PokemonInPlay(mon("rapidash", 120, EnergyType.FIRE, a))
        val myBench = PokemonInPlay(mon("bench", 100, EnergyType.FIRE, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, a))
        val player = PlayerState(Side.PLAYER, attacker, bench = listOf(myBench), deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foe, deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Mach Turn"))
        val choose = res.pending.filterIsInstance<PendingDecision.ChooseTargets>().firstOrNull()
        assertTrue(choose != null, "debía abrir elección de Banca propia")
        assertTrue(choose.candidates.contains(CardId("bench")))
        assertEquals(90, res.state.opponent.active!!.damage)
    }

    @Test
    fun `Giro Mach sin Banca propia hace daño base y no deja decisión pendiente`() {
        val a = attack("Mach Turn", 90, EffectsDb.atkKey("sv3pt5-78", "Mach Turn"))
        val attacker = PokemonInPlay(mon("rapidash", 120, EnergyType.FIRE, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, a))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Mach Turn"))
        assertTrue(res.accepted, res.rejection)
        assertTrue(res.pending.none { it is PendingDecision.ChooseTargets })
        assertEquals(90, res.state.opponent.active!!.damage)
        assertEquals(CardId("rapidash"), res.state.player.active!!.card.id)
    }

    // ------------------------------------------------------------------ Fase 12

    @Test
    fun `Psicopoder reparte 3 contadores entre los Pokémon del rival`() {
        val a = attack("Psypower", 0, EffectsDb.atkKey("sv3pt5-122", "Psypower"))
        val attacker = PokemonInPlay(mon("mrmime", 70, EnergyType.PSYCHIC, a))
        val foeActive = PokemonInPlay(mon("act", 300, EnergyType.LIGHTNING, a))
        val b1 = PokemonInPlay(mon("b1", 100, EnergyType.WATER, a))
        val player = PlayerState(Side.PLAYER, attacker, deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foeActive, bench = listOf(b1), deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val eng = GameEngine(SeededRng(1))

        val paused = eng.apply(st, GameIntent.Attack("Psypower"))
        val choose = paused.pending.filterIsInstance<PendingDecision.PlaceCounters>().firstOrNull()
        assertTrue(choose != null, "debía abrir el reparto de contadores")
        assertEquals(3, choose.count)
        // 2 contadores al Activo (20) + 1 a la Banca (10).
        val done = eng.apply(paused.state, GameIntent.ResolveDecision(listOf(CardId("act"), CardId("act"), CardId("b1"))))
        assertEquals(20, done.state.opponent.active!!.damage)
        assertEquals(10, done.state.opponent.bench.first { it.card.id == CardId("b1") }.damage)
    }

    @Test
    fun `Psicopoder sin banca rival puede apilar los 3 contadores en el Activo`() {
        val a = attack("Psypower", 0, EffectsDb.atkKey("sv3pt5-122", "Psypower"))
        val attacker = PokemonInPlay(mon("mrmime", 70, EnergyType.PSYCHIC, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, a))
        val eng = GameEngine(SeededRng(1))
        val paused = eng.apply(duel(attacker, foe), GameIntent.Attack("Psypower"))
        val choose = paused.pending.filterIsInstance<PendingDecision.PlaceCounters>().firstOrNull()
        assertTrue(choose != null)
        assertTrue(choose.candidates.contains(CardId("foe")))
        val done = eng.apply(paused.state, GameIntent.ResolveDecision(listOf(CardId("foe"), CardId("foe"), CardId("foe"))))
        assertEquals(30, done.state.opponent.active!!.damage)
    }

    @Test
    fun `Embestida Hueca hace 110 al Activo y reparte 3 contadores en la Banca rival`() {
        val a = attack("Hollow Dive", 110, EffectsDb.atkKey("sv3pt5-94", "Hollow Dive"))
        val attacker = PokemonInPlay(mon("gengar", 130, EnergyType.PSYCHIC, a))
        val foeActive = PokemonInPlay(mon("act", 300, EnergyType.WATER, a))
        val b1 = PokemonInPlay(mon("b1", 100, EnergyType.WATER, a))
        val b2 = PokemonInPlay(mon("b2", 100, EnergyType.WATER, a))
        val player = PlayerState(Side.PLAYER, attacker, deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foeActive, bench = listOf(b1, b2), deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val eng = GameEngine(SeededRng(1))

        val paused = eng.apply(st, GameIntent.Attack("Hollow Dive"))
        // El daño base (110) ya se aplicó al Activo; la elección es SOLO la Banca.
        assertEquals(110, paused.state.opponent.active!!.damage)
        val choose = paused.pending.filterIsInstance<PendingDecision.PlaceCounters>().firstOrNull()
        assertTrue(choose != null, "debía abrir el reparto de contadores")
        assertFalse(choose.candidates.contains(CardId("act")), "los contadores son SOLO a la Banca")
        val done = eng.apply(paused.state, GameIntent.ResolveDecision(listOf(CardId("b1"), CardId("b1"), CardId("b2"))))
        assertEquals(20, done.state.opponent.bench.first { it.card.id == CardId("b1") }.damage)
        assertEquals(10, done.state.opponent.bench.first { it.card.id == CardId("b2") }.damage)
    }

    // ------------------------------------------------------------------ Fase 13

    @Test
    fun `Furia suma 10 por cada contador de daño propio`() {
        val a = attack("Rage", 30, EffectsDb.atkKey("sv3pt5-128", "Rage"))
        // Tauros con 4 contadores propios (40 de daño) → +40.
        val attacker = PokemonInPlay(mon("tauros", 100, EnergyType.COLORLESS, a), damage = 40)
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, a))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Rage"))
        assertTrue(res.accepted, res.rejection)
        assertEquals(30 + 40, res.state.opponent.active!!.damage) // 30 base + 40 extra
    }

    @Test
    fun `Puñetazo Incesante hace 100 por cada una de 4 caras`() {
        val a = attack("Incessant Punching", 0, EffectsDb.atkKey("sv3pt5-115", "Incessant Punching"))
        val attacker = PokemonInPlay(mon("kangaskhan", 180, EnergyType.COLORLESS, a))
        val foe = PokemonInPlay(mon("foe", 500, EnergyType.WATER, a))
        val res = GameEngine(FixedRng(true)).apply(duel(attacker, foe), GameIntent.Attack("Incessant Punching"))
        assertTrue(res.accepted, res.rejection)
        assertEquals(400, res.state.opponent.active!!.damage)
    }

    @Test
    fun `Torrente Tórrido une 2 Energía Fuego del descarte a sí mismo`() {
        val a = attack("Torrid Torrent", 30, EffectsDb.atkKey("sv3pt5-59", "Torrid Torrent"))
        val attacker = PokemonInPlay(mon("arcanine", 160, EnergyType.FIRE, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, a))
        val player = PlayerState(
            side = Side.PLAYER, active = attacker,
            deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) },
            discard = listOf(energy("f1", EnergyType.FIRE), energy("f2", EnergyType.FIRE)),
            prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) },
        )
        val opponent = PlayerState(Side.OPPONENT, foe, deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Torrid Torrent"))
        assertTrue(res.accepted, res.rejection)
        assertEquals(2, res.state.player.active!!.attachedEnergyCount)
        assertTrue(res.state.player.discard.none { it is BasicEnergy && it.type == EnergyType.FIRE })
    }

    @Test
    fun `Señuelo sube al Activo rival un Pokémon de su Banca que yo elijo`() {
        val a = attack("Follow Me", 0, EffectsDb.atkKey("sv3pt5-36", "Follow Me"))
        val attacker = PokemonInPlay(mon("clefable", 140, EnergyType.PSYCHIC, a))
        val foeActive = PokemonInPlay(mon("act", 300, EnergyType.WATER, a))
        val b1 = PokemonInPlay(mon("b1", 100, EnergyType.LIGHTNING, a))
        val player = PlayerState(Side.PLAYER, attacker, deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foeActive, bench = listOf(b1), deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val eng = GameEngine(SeededRng(1))
        val paused = eng.apply(st, GameIntent.Attack("Follow Me"))
        val done = eng.apply(paused.state, GameIntent.ResolveDecision(listOf(CardId("b1"))))
        assertEquals(CardId("b1"), done.state.opponent.active!!.card.id)
        assertTrue(done.state.opponent.bench.any { it.card.id == CardId("act") })
    }

    // ---------------- FASE 14: moneda-hasta-cruz + mill + daño-antes-del-base ----------------

    /** Rng con secuencia fija de monedas (para moneda-hasta-cruz determinista). */
    private class SeqRng(private val flips: List<Boolean>) : Rng {
        private var i = 0
        override fun flipCoin(): Boolean = flips[i++]
        override fun <T> shuffle(list: List<T>): List<T> = list
        override fun nextInt(untilExclusive: Int): Int = 0
    }

    @Test
    fun `Cañón Roca hace 40 por cara hasta que sale cruz`() {
        val a = attack("Rock Cannon", 0, EffectsDb.atkKey("sv3pt5-75", "Rock Cannon"))
        val attacker = PokemonInPlay(mon("graveler", 120, EnergyType.FIGHTING, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, a))
        // cara, cara, cruz → 2 caras → 80.
        val res = GameEngine(SeqRng(listOf(true, true, false))).apply(duel(attacker, foe), GameIntent.Attack("Rock Cannon"))
        assertEquals(80, res.events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    @Test
    fun `Pánico Tentacular Confunde si la primera moneda sale cruz`() {
        val a = attack("Tentacular Panic", 0, EffectsDb.atkKey("sv3pt5-73", "Tentacular Panic"))
        val attacker = PokemonInPlay(mon("tentacruel", 120, EnergyType.WATER, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.GRASS, a))
        // primera cruz → 0 daño + Confundido.
        val res = GameEngine(SeqRng(listOf(false))).apply(duel(attacker, foe), GameIntent.Attack("Tentacular Panic"))
        assertTrue(res.events.filterIsInstance<GameEvent.DamageDealt>().isEmpty())
        assertTrue(res.state.opponent.active!!.statuses.contains(Status.CONFUSED))
    }

    @Test
    fun `Salpicadura Salpicona roba 1 por cara hasta cruz`() {
        val a = attack("Splashy Splash", 0, EffectsDb.atkKey("sv3pt5-129", "Splashy Splash"))
        val attacker = PokemonInPlay(mon("magikarp", 30, EnergyType.WATER, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.LIGHTNING, a))
        // cara, cara, cruz → roba 2.
        val res = GameEngine(SeqRng(listOf(true, true, false))).apply(duel(attacker, foe), GameIntent.Attack("Splashy Splash"))
        assertEquals(2, res.state.player.hand.size)
    }

    @Test
    fun `Aplastamiento Montaña descarta 1 carta de la baraja rival`() {
        val a = attack("Mountain Mashing", 0, EffectsDb.atkKey("sv3pt5-66", "Mountain Mashing"))
        val attacker = PokemonInPlay(mon("machop", 70, EnergyType.FIGHTING, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.PSYCHIC, a))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Mountain Mashing"))
        assertEquals(1, res.state.opponent.discard.size)
    }

    @Test
    fun `Pulso Drágon descarta 2 cartas de tu propia baraja y hace 180`() {
        val a = attack("Dragon Pulse", 180, EffectsDb.atkKey("sv3pt5-149", "Dragon Pulse"))
        val attacker = PokemonInPlay(mon("dragonite", 220, EnergyType.LIGHTNING, a))
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.WATER, a))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Dragon Pulse"))
        assertEquals(180, res.events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
        assertEquals(2, res.state.player.discard.size)
    }

    @Test
    fun `Roer la Herida suma 10 por contador del Activo rival medido antes del daño base`() {
        val a = attack("Gnaw the Wound", 0, EffectsDb.atkKey("sv3pt5-19", "Gnaw the Wound"))
        val attacker = PokemonInPlay(mon("rattata", 60, EnergyType.COLORLESS, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.PSYCHIC, a), damage = 30) // 3 contadores
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Gnaw the Wound"))
        assertEquals(50, res.events.filterIsInstance<GameEvent.DamageDealt>().first().amount) // 20 + 10*3
    }

    @Test
    fun `Segundo Mordisco suma 30 por contador del Activo rival`() {
        val a = attack("Second Bite", 0, EffectsDb.atkKey("sv3pt5-20", "Second Bite"))
        val attacker = PokemonInPlay(mon("raticate", 120, EnergyType.COLORLESS, a))
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.PSYCHIC, a), damage = 20) // 2 contadores
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Second Bite"))
        assertEquals(90, res.events.filterIsInstance<GameEvent.DamageDealt>().first().amount) // 30 + 30*2
    }

    // ---------------- FASE 15: condicionales +X, reusos y ops nuevas ----------------

    private fun trainer(id: String, name: String) = TrainerCard(
        CardId(id), LocalizedText(name, name), set(), Rarity.COMMON, "H", art(),
        TrainerKind.Item(), LocalizedText("", ""), EffectId("none"),
    )

    @Test
    fun `Llamas Reflejadas suma 140 si igualas la mano del rival`() {
        val a = attack("Mirrored Flames", 0, EffectsDb.atkKey("sv3pt5-38", "Mirrored Flames"))
        val attacker = PokemonInPlay(mon("ninetales", 210, EnergyType.FIRE, a))
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.WATER, a))
        // Manos iguales (ambas vacías) → 80 + 140.
        val eq = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Mirrored Flames"))
        assertEquals(220, eq.events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
        // Rival con 1 carta → distinto → solo 80.
        val neq = GameEngine(SeededRng(1)).apply(duel(attacker, foe, foeHand = listOf(energy("h1", EnergyType.WATER))), GameIntent.Attack("Mirrored Flames"))
        assertEquals(80, neq.events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    @Test
    fun `Lanzamiento Audaz suma 90 si te quedan más Premios`() {
        val a = attack("Reckless Throw", 0, EffectsDb.atkKey("sv3pt5-127", "Reckless Throw"))
        val attacker = PokemonInPlay(mon("pinsir", 120, EnergyType.GRASS, a))
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.FIRE, a))
        val player = PlayerState(Side.PLAYER, attacker, deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foe, deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..4).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Reckless Throw"))
        assertEquals(180, res.events.filterIsInstance<GameEvent.DamageDealt>().first().amount) // 90 + 90
    }

    @Test
    fun `Combo Eléctrico suma 40 si Magmar está en tu Banca`() {
        val a = attack("Electro Combo", 0, EffectsDb.atkKey("sv3pt5-125", "Electro Combo"))
        val attacker = PokemonInPlay(mon("electabuzz", 90, EnergyType.LIGHTNING, a))
        val magmar = PokemonInPlay(mon("Magmar", 90, EnergyType.FIRE, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, a))
        val withMagmar = PlayerState(Side.PLAYER, attacker, bench = listOf(magmar), deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opp = PlayerState(Side.OPPONENT, foe, deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(withMagmar, opp, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        assertEquals(50, GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Electro Combo")).events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
        // Sin Magmar → solo 10.
        val stNo = st.copy(player = withMagmar.copy(bench = emptyList()))
        assertEquals(10, GameEngine(SeededRng(1)).apply(stNo, GameIntent.Attack("Electro Combo")).events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    @Test
    fun `Ala Osada de Charizard suma 100 si tiene daño`() {
        val a = attack("Brave Wing", 0, EffectsDb.atkKey("sv3pt5-183", "Brave Wing"))
        val fresh = PokemonInPlay(mon("charizard", 330, EnergyType.FIRE, a))
        val hurt = PokemonInPlay(mon("charizard", 330, EnergyType.FIRE, a), damage = 30)
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.WATER, a))
        assertEquals(60, GameEngine(SeededRng(1)).apply(duel(fresh, foe), GameIntent.Attack("Brave Wing")).events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
        assertEquals(160, GameEngine(SeededRng(1)).apply(duel(hurt, foe), GameIntent.Attack("Brave Wing")).events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    @Test
    fun `Poltergeist hace 50 por cada Entrenador en la mano rival`() {
        val a = attack("Poltergeist", 0, EffectsDb.atkKey("sv3pt5-94", "Poltergeist"))
        val attacker = PokemonInPlay(mon("gengar", 130, EnergyType.PSYCHIC, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.FIGHTING, a))
        val hand = listOf(trainer("t1", "Poción"), trainer("t2", "Switch"), energy("e1", EnergyType.WATER))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe, foeHand = hand), GameIntent.Attack("Poltergeist"))
        assertEquals(100, res.events.filterIsInstance<GameEvent.DamageDealt>().first().amount) // 2 Entrenadores × 50
    }

    @Test
    fun `Alud Descomunal descarta 5 y hace 80 por Pokémon con retiro 4`() {
        val a = attack("Thumpalanche", 0, EffectsDb.atkKey("sv3pt5-95", "Thumpalanche"))
        val attacker = PokemonInPlay(mon("onix", 130, EnergyType.FIGHTING, a))
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.GRASS, a))
        val heavy = List(4) { EnergyType.COLORLESS }
        val deck = listOf<com.mineralord.tcg.engine.model.Card>(
            monAbi("h1", 100, EnergyType.WATER, retreat = heavy),
            monAbi("h2", 100, EnergyType.WATER, retreat = heavy),
            energy("d1", EnergyType.LIGHTNING), energy("d2", EnergyType.LIGHTNING), energy("d3", EnergyType.LIGHTNING),
        )
        val player = PlayerState(Side.PLAYER, attacker, deck = deck, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opp = PlayerState(Side.OPPONENT, foe, deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opp, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Thumpalanche"))
        assertEquals(160, res.events.filterIsInstance<GameEvent.DamageDealt>().first().amount) // 2 × 80
        assertEquals(5, res.state.player.discard.size)
    }

    @Test
    fun `Retorno Tentacular devuelve 1 Energía del Activo rival a su mano`() {
        val a = attack("Tentacular Return", 50, EffectsDb.atkKey("sv3pt5-138", "Tentacular Return"))
        val attacker = PokemonInPlay(mon("omanyte", 70, EnergyType.WATER, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.LIGHTNING, a), attachedEnergy = listOf(energy("fe1", EnergyType.WATER)))
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Tentacular Return"))
        assertEquals(0, res.state.opponent.active!!.attachedEnergyCount)
        assertTrue(res.state.opponent.hand.any { it.id == CardId("fe1") })
    }

    @Test
    fun `Levantamiento suma 30 por cada Pokémon en Banca rival`() {
        val a = attack("Mind Jack", 90, EffectsDb.atkKey("sv3pt5-188", "Mind Jack"))
        val attacker = PokemonInPlay(mon("alakazam", 190, EnergyType.PSYCHIC, a))
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.DARKNESS, a))
        val b1 = PokemonInPlay(mon("b1", 100, EnergyType.WATER, a))
        val b2 = PokemonInPlay(mon("b2", 100, EnergyType.WATER, a))
        val player = PlayerState(Side.PLAYER, attacker, deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opp = PlayerState(Side.OPPONENT, foe, bench = listOf(b1, b2), deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opp, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Mind Jack"))
        // base 90 + ExtraDamage crudo 30*2 (dos eventos DamageDealt) → 150 al Activo rival.
        assertEquals(150, res.events.filterIsInstance<GameEvent.DamageDealt>().sumOf { it.amount })
    }

    // ---------------- FASE 16: reducción parcial de daño ----------------

    @Test
    fun `un Pokémon con reducción activa recibe menos daño este turno`() {
        val a = attack("Hit", 50, EffectId("none"))
        val attacker = PokemonInPlay(mon("hitter", 200, EnergyType.FIGHTING, a))
        // Defensor con reducción de 30 vigente en el turno actual (3).
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, a), damageReductionOnTurn = 3, damageReductionAmount = 30)
        val res = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Hit"))
        assertEquals(20, res.events.filterIsInstance<GameEvent.DamageDealt>().first().amount) // 50 - 30
    }

    @Test
    fun `Endurecimiento marca a Geodude con reducción de 30 el próximo turno`() {
        val a = attack("Stiffen", 0, EffectsDb.atkKey("sv3pt5-74", "Stiffen"))
        val geodude = PokemonInPlay(mon("geodude", 90, EnergyType.FIGHTING, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, a))
        val res = GameEngine(SeededRng(1)).apply(duel(geodude, foe), GameIntent.Attack("Stiffen"))
        val self = res.state.player.active!!
        assertEquals(4, self.damageReductionOnTurn) // turno 3 + 1
        assertEquals(30, self.damageReductionAmount)
    }

    // ---------------- FASE 17: "+X si jugaste tal carta este turno" ----------------

    @Test
    fun `Placaje Amigo suma 90 si jugaste un Partidario este turno`() {
        val a = attack("Friend Tackle", 0, EffectsDb.atkKey("sv3pt5-40", "Friend Tackle"))
        val attacker = PokemonInPlay(mon("wigglytuff", 160, EnergyType.COLORLESS, a))
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.FIGHTING, a))
        val base = duel(attacker, foe)
        assertEquals(90, GameEngine(SeededRng(1)).apply(base, GameIntent.Attack("Friend Tackle")).events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
        val withSup = base.copy(supporterPlayedThisTurn = true)
        assertEquals(180, GameEngine(SeededRng(1)).apply(withSup, GameIntent.Attack("Friend Tackle")).events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    @Test
    fun `Taladro Carismático suma 140 si jugaste Carisma de Giovanni este turno`() {
        val a = attack("Charismatic Drill", 0, EffectsDb.atkKey("sv3pt5-112", "Charismatic Drill"))
        val attacker = PokemonInPlay(mon("rhydon", 140, EnergyType.FIGHTING, a))
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.LIGHTNING, a))
        val base = duel(attacker, foe)
        assertEquals(40, GameEngine(SeededRng(1)).apply(base, GameIntent.Attack("Charismatic Drill")).events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
        val played = base.copy(trainerNamesPlayedThisTurn = setOf("Carisma de Giovanni", "Giovanni's Charisma"))
        assertEquals(180, GameEngine(SeededRng(1)).apply(played, GameIntent.Attack("Charismatic Drill")).events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    // ---------------- FASE 18: buscar Partidario/Objeto, energía a Banca, snipe ----------------

    private fun supporter(id: String, name: String) = TrainerCard(
        CardId(id), LocalizedText(name, name), set(), Rarity.COMMON, "H", art(),
        TrainerKind.Supporter(), LocalizedText("", ""), EffectId("none"),
    )

    @Test
    fun `Liderazgo busca solo Partidarios del mazo a la mano`() {
        val a = attack("Lead", 0, EffectsDb.atkKey("sv3pt5-39", "Lead"))
        val attacker = PokemonInPlay(mon("jigglypuff", 60, EnergyType.COLORLESS, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIGHTING, a))
        val deck = listOf(
            supporter("sup1", "Erika's Invitation"), trainer("item1", "Poké Ball"),
            energy("e1", EnergyType.LIGHTNING),
        )
        val player = PlayerState(Side.PLAYER, attacker, deck = deck, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foe, deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Lead"))
        val search = res.pending.filterIsInstance<PendingDecision.SearchCards>().firstOrNull()
        assertTrue(search != null, "debía abrir una búsqueda")
        assertEquals(Zone.HAND, search.destination)
        assertEquals(listOf(CardId("sup1")), search.candidates)
    }

    @Test
    fun `Imán de Chatarra recupera hasta 2 Objetos del descarte`() {
        val a = attack("Junk Magnet", 0, EffectsDb.atkKey("sv3pt5-82", "Junk Magnet"))
        val attacker = PokemonInPlay(mon("magneton", 130, EnergyType.LIGHTNING, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIGHTING, a))
        val discard = listOf(
            trainer("it1", "Poké Ball"), trainer("it2", "Potion"),
            supporter("sup1", "Erika's Invitation"), energy("de1", EnergyType.WATER),
        )
        val player = PlayerState(Side.PLAYER, attacker, discard = discard, deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foe, deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Junk Magnet"))
        val search = res.pending.filterIsInstance<PendingDecision.SearchCards>().firstOrNull()
        assertTrue(search != null, "debía abrir elección del descarte")
        assertEquals(2, search.count)
        assertEquals(listOf(CardId("it1"), CardId("it2")), search.candidates)
    }

    @Test
    fun `Tajo Útil hace 20 y ofrece unir Energía Planta del descarte a la Banca`() {
        val a = attack("Helpful Slash", 20, EffectsDb.atkKey("sv3pt5-123", "Helpful Slash"))
        val attacker = PokemonInPlay(mon("scyther", 110, EnergyType.GRASS, a))
        val myBench = PokemonInPlay(mon("bench", 100, EnergyType.GRASS, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIRE, a))
        val player = PlayerState(Side.PLAYER, attacker, bench = listOf(myBench), discard = listOf(energy("g1", EnergyType.GRASS)), deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foe, deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Helpful Slash"))
        assertEquals(20, res.state.opponent.active!!.damage)
        val attach = res.pending.filterIsInstance<PendingDecision.AttachFromRevealed>().firstOrNull()
        assertTrue(attach != null, "debía ofrecer unir Energía")
        assertEquals(listOf(CardId("bench")), attach.benchCandidates)
        assertEquals(listOf(CardId("g1")), attach.energyCandidates)
    }

    @Test
    fun `Ataque Lineal abre elección de cualquier Pokémon del rival`() {
        val a = attack("Linear Attack", 0, EffectsDb.atkKey("sv3pt5-135", "Linear Attack"))
        val attacker = PokemonInPlay(mon("jolteon", 110, EnergyType.LIGHTNING, a))
        val foeActive = PokemonInPlay(mon("act", 300, EnergyType.WATER, a))
        val b1 = PokemonInPlay(mon("b1", 100, EnergyType.WATER, a))
        val player = PlayerState(Side.PLAYER, attacker, deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) })
        val opponent = PlayerState(Side.OPPONENT, foeActive, bench = listOf(b1), deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) })
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Linear Attack"))
        val choose = res.pending.filterIsInstance<PendingDecision.ChooseTargets>().firstOrNull()
        assertTrue(choose != null, "debía abrir elección de objetivo")
        assertTrue(choose.candidates.contains(CardId("act")))
        assertTrue(choose.candidates.contains(CardId("b1")))
    }

    @Test
    fun `Rayo Luchador suma 90 si el Activo rival es ex`() {
        val a = attack("Fighting Lightning", 0, EffectsDb.atkKey("sv3pt5-135", "Fighting Lightning"))
        val attacker = PokemonInPlay(mon("jolteon", 110, EnergyType.LIGHTNING, a))
        val normalFoe = PokemonInPlay(mon("foe", 400, EnergyType.WATER, a))
        val exFoe = PokemonInPlay(mon("foeEx", 400, EnergyType.WATER, a, mechanic = PokemonMechanic.ExLower))
        assertEquals(90, GameEngine(SeededRng(1)).apply(duel(attacker, normalFoe), GameIntent.Attack("Fighting Lightning"))
            .events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
        assertEquals(180, GameEngine(SeededRng(1)).apply(duel(attacker, exFoe), GameIntent.Attack("Fighting Lightning"))
            .events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    // ---------------- FASE 19: daño por energía de tipo + KO por estado ----------------

    @Test
    fun `Cuerno Aqua suma 30 por cada Energía Agua unida a sí mismo`() {
        val a = attack("Aqua Horn", 60, EffectsDb.atkKey("sv3pt5-119", "Aqua Horn"))
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.LIGHTNING, a))
        // Sin energía: solo 60 base.
        val bare = PokemonInPlay(mon("seaking", 120, EnergyType.WATER, a))
        assertEquals(60, GameEngine(SeededRng(1)).apply(duel(bare, foe), GameIntent.Attack("Aqua Horn"))
            .events.filterIsInstance<GameEvent.DamageDealt>().sumOf { it.amount })
        // 2 Energía {W} (+ 1 {F} que NO cuenta) → 60 + 60 = 120.
        val loaded = PokemonInPlay(mon("seaking", 120, EnergyType.WATER, a), attachedEnergy = listOf(
            energy("w1", EnergyType.WATER), energy("w2", EnergyType.WATER), energy("f1", EnergyType.FIRE)))
        assertEquals(120, GameEngine(SeededRng(1)).apply(duel(loaded, foe), GameIntent.Attack("Aqua Horn"))
            .events.filterIsInstance<GameEvent.DamageDealt>().sumOf { it.amount })
    }

    @Test
    fun `Beso de Infarto noquea al Activo rival si está Dormido`() {
        val a = attack("Heart-Stopping Kiss", 0, EffectsDb.atkKey("sv3pt5-124", "Heart-Stopping Kiss"))
        val attacker = PokemonInPlay(mon("jynx", 210, EnergyType.WATER, a))
        val benchFoe = PokemonInPlay(mon("b1", 100, EnergyType.FIGHTING, a))
        // Rival Dormido → KO.
        val asleep = PokemonInPlay(mon("foe", 200, EnergyType.FIGHTING, a), statuses = setOf(Status.ASLEEP))
        val stKo = GameState(
            PlayerState(Side.PLAYER, attacker, deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) }, prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) }),
            PlayerState(Side.OPPONENT, asleep, bench = listOf(benchFoe), deck = (1..5).map { energy("od$it", EnergyType.WATER) }, prizes = (1..6).map { energy("opz$it", EnergyType.WATER) }),
            turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val ko = GameEngine(SeededRng(1)).apply(stKo, GameIntent.Attack("Heart-Stopping Kiss"))
        assertTrue(ko.events.any { it is GameEvent.KnockedOut }, "debía noquear al rival Dormido")

        // Rival despierto → no pasa nada (sin KO).
        val awake = PokemonInPlay(mon("foe", 200, EnergyType.FIGHTING, a))
        val noKo = GameEngine(SeededRng(1)).apply(duel(attacker, awake), GameIntent.Attack("Heart-Stopping Kiss"))
        assertTrue(noKo.events.none { it is GameEvent.KnockedOut }, "no debía noquear a un rival despierto")
    }

    // ---------------- FASE 20: ignorar Debilidad/Resistencia + curar-al-atacar ----------------

    @Test
    fun `Meteoros ignora Debilidad y los efectos del Activo rival`() {
        val a = attack("Swift", 30, EffectsDb.atkKey("sv3pt5-120", "Swift"))
        val attacker = PokemonInPlay(mon("staryu", 60, EnergyType.WATER, a))
        // Defensor débil a {W} (×2) Y con prevención de daño este turno: Meteoros ignora AMBOS.
        val foe = PokemonInPlay(
            mon("foe", 200, EnergyType.FIGHTING, a, weakness = EnergyType.WATER),
            preventDamageOnTurn = 3,
        )
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Swift"))
        assertEquals(30, r.events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    @Test
    fun `Explosión Roca ignora la Resistencia del Activo rival`() {
        val a = attack("Rock Blaster", 180, EffectsDb.atkKey("sv3pt5-76", "Rock Blaster"))
        val attacker = PokemonInPlay(mon("golem", 340, EnergyType.FIGHTING, a))
        // Defensor resistente a {F} (-30): normalmente 150; Explosión Roca hace los 180 completos.
        val foe = PokemonInPlay(
            mon("foe", 400, EnergyType.LIGHTNING, a).copy(
                resistances = listOf(TypeModifier(EnergyType.FIGHTING, "-30"))),
        )
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Rock Blaster"))
        assertEquals(180, r.events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    @Test
    fun `Cuchilla Drenadora cura 30 al atacante`() {
        val a = attack("Draining Blade", 100, EffectsDb.atkKey("sv3pt5-141", "Draining Blade"))
        val attacker = PokemonInPlay(mon("kabutops", 150, EnergyType.FIGHTING, a), damage = 50)
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.LIGHTNING, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Draining Blade"))
        assertEquals(20, r.state.player.active!!.damage, "50 - 30 curados = 20")
    }

    // ---------------- FASE 21: moneda cara/cruz + búsqueda de tipos distintos ----------------

    @Test
    fun `Saña cara suma 20 al rival, cruz hace 20 de rebote a sí mismo`() {
        val a = attack("Thrash", 20, EffectsDb.atkKey("sv3pt5-56", "Thrash"))
        val mankey = PokemonInPlay(mon("mankey", 100, EnergyType.FIGHTING, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.LIGHTNING, a))

        val heads = GameEngine(FixedRng(true)).apply(duel(mankey, foe), GameIntent.Attack("Thrash"))
        assertEquals(40, heads.state.opponent.active!!.damage, "20 base + 20 por cara")
        assertEquals(0, heads.state.player.active!!.damage, "sin rebote con cara")

        val tails = GameEngine(FixedRng(false)).apply(duel(mankey, foe), GameIntent.Attack("Thrash"))
        assertEquals(20, tails.state.opponent.active!!.damage, "solo el daño base con cruz")
        assertEquals(20, tails.state.player.active!!.damage, "20 de rebote a sí mismo con cruz")
    }

    @Test
    fun `Amigos Coloridos solo se queda con Pokémon de tipos distintos`() {
        val a = attack("Colorful Friends", 0, EffectsDb.atkKey("sv3pt5-133", "Colorful Friends"))
        val eevee = PokemonInPlay(mon("eevee", 70, EnergyType.COLORLESS, a))
        val g1 = mon("g1", 60, EnergyType.GRASS, a)
        val f1 = mon("f1", 60, EnergyType.FIRE, a)
        val f2 = mon("f2", 60, EnergyType.FIRE, a) // mismo tipo que f1 → debe descartarse
        val player = PlayerState(
            side = Side.PLAYER, active = eevee,
            deck = listOf(g1, f1, f2) + (1..3).map { energy("d$it", EnergyType.LIGHTNING) },
            prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = PokemonInPlay(mon("foe", 200, EnergyType.WATER, a)),
            deck = (1..5).map { energy("od$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("opz$it", EnergyType.WATER) },
        )
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Colorful Friends"))
        val done = GameEngine(SeededRng(1)).apply(
            paused.state, GameIntent.ResolveDecision(listOf(CardId("g1"), CardId("f1"), CardId("f2"))))
        val handIds = done.state.player.hand.map { it.id.raw }.toSet()
        assertEquals(setOf("g1", "f1"), handIds, "un Pokémon por tipo: se queda GRASS+FIRE y descarta el 2º FIRE")
    }

    // ---------------- FASE 22: bonus de daño "tu próximo turno +X" ----------------

    @Test
    fun `Giro Dinámico marca el bonus de daño para el próximo turno propio`() {
        val a = attack("Dynamic Roll", 50, EffectsDb.atkKey("sv3pt5-76", "Dynamic Roll"))
        val golem = PokemonInPlay(mon("golem", 340, EnergyType.FIGHTING, a))
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.LIGHTNING, a))
        val r = GameEngine(SeededRng(1)).apply(duel(golem, foe), GameIntent.Attack("Dynamic Roll"))
        val after = r.state.player.active!!
        assertEquals(5, after.attackBonusOnTurn, "turno 3 + 2 = próximo turno propio")
        assertEquals(120, after.attackBonusAmount)
        assertEquals(50, r.state.opponent.active!!.damage, "el daño base de Giro Dinámico igual se aplica")
    }

    @Test
    fun `un Pokémon con bonus activo suma el daño antes de Debilidad-Resistencia`() {
        val a = attack("Plain Hit", 50, EffectId("sv3pt5-none"))
        // Bonus activo ESTE turno (3): 50 base + 120 = 170.
        val golem = PokemonInPlay(mon("golem", 340, EnergyType.FIGHTING, a), attackBonusOnTurn = 3, attackBonusAmount = 120)
        val foe = PokemonInPlay(mon("foe", 400, EnergyType.LIGHTNING, a))
        val r = GameEngine(SeededRng(1)).apply(duel(golem, foe), GameIntent.Attack("Plain Hit"))
        assertEquals(170, r.state.opponent.active!!.damage)

        // Bonus para OTRO turno (5) → no aplica: solo 50.
        val golem2 = PokemonInPlay(mon("golem", 340, EnergyType.FIGHTING, a), attackBonusOnTurn = 5, attackBonusAmount = 120)
        val r2 = GameEngine(SeededRng(1)).apply(duel(golem2, foe), GameIntent.Attack("Plain Hit"))
        assertEquals(50, r2.state.opponent.active!!.damage)
    }

    // ---------------- FASE 23: recargo de Coste de Retirada / de ataque al Defensor ----------------

    @Test
    fun `Presión Pegajosa recarga el Coste de Retirada del Defensor el próximo turno`() {
        val a = attack("Gummy Press", 10, EffectsDb.atkKey("sv3pt5-88", "Gummy Press"))
        val grimer = PokemonInPlay(mon("grimer", 80, EnergyType.DARKNESS, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIGHTING, a))
        val r = GameEngine(SeededRng(1)).apply(duel(grimer, foe), GameIntent.Attack("Gummy Press"))
        val def = r.state.opponent.active!!
        assertEquals(4, def.retreatCostBumpOnTurn, "turno 3 + 1 = próximo turno del rival")
        assertEquals(1, def.retreatCostBumpAmount)
    }

    @Test
    fun `Prisión Viscosa recarga ataque Y retirada del Defensor`() {
        val a = attack("Sticky Jail", 30, EffectsDb.atkKey("sv3pt5-89", "Sticky Jail"))
        val muk = PokemonInPlay(mon("muk", 150, EnergyType.DARKNESS, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIGHTING, a))
        val r = GameEngine(SeededRng(1)).apply(duel(muk, foe), GameIntent.Attack("Sticky Jail"))
        val def = r.state.opponent.active!!
        assertEquals(4, def.attackCostBumpOnTurn)
        assertEquals(1, def.attackCostBumpAmount)
        assertEquals(4, def.retreatCostBumpOnTurn)
    }

    @Test
    fun `el recargo de retirada exige más energía para retirarse`() {
        // Coste impreso {C}=1 + recargo 1 (este turno 3) = 2. Con 1 energía no se puede retirar.
        val base = monAbi("act", 120, EnergyType.WATER, retreat = listOf(EnergyType.COLORLESS))
        val active1 = PokemonInPlay(base, attachedEnergy = listOf(energy("e1", EnergyType.WATER)))
            .copy(retreatCostBumpOnTurn = 3, retreatCostBumpAmount = 1)
        val bench = PokemonInPlay(monAbi("bench", 60, EnergyType.WATER))
        val foe = PokemonInPlay(monAbi("foe", 200, EnergyType.FIRE))
        val rejected = GameEngine(SeededRng(1)).apply(retreatState(active1, listOf(bench), foe), GameIntent.Retreat(CardId("bench")))
        assertFalse(rejected.accepted, "1 energía < coste efectivo 2")

        val active2 = PokemonInPlay(base, attachedEnergy = listOf(energy("e1", EnergyType.WATER), energy("e2", EnergyType.WATER)))
            .copy(retreatCostBumpOnTurn = 3, retreatCostBumpAmount = 1)
        val accepted = GameEngine(SeededRng(1)).apply(retreatState(active2, listOf(bench), foe), GameIntent.Retreat(CardId("bench")))
        assertTrue(accepted.accepted, accepted.rejection)
    }

    @Test
    fun `el recargo de ataque exige más energía para atacar`() {
        val a = attack("Hit", 20, EffectId("sv3pt5-none"), cost = 1)
        // Coste 1 + recargo 1 (turno 3) = 2. Con 1 energía no puede atacar.
        val weak = PokemonInPlay(mon("a", 100, EnergyType.FIGHTING, a), attachedEnergy = listOf(energy("e1", EnergyType.FIGHTING)))
            .copy(attackCostBumpOnTurn = 3, attackCostBumpAmount = 1)
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.LIGHTNING, a))
        val rejected = GameEngine(SeededRng(1)).apply(duel(weak, foe), GameIntent.Attack("Hit"))
        assertFalse(rejected.accepted, "1 energía < coste de ataque efectivo 2")

        val ok = PokemonInPlay(mon("a", 100, EnergyType.FIGHTING, a), attachedEnergy = listOf(energy("e1", EnergyType.FIGHTING), energy("e2", EnergyType.FIGHTING)))
            .copy(attackCostBumpOnTurn = 3, attackCostBumpAmount = 1)
        val accepted = GameEngine(SeededRng(1)).apply(duel(ok, foe), GameIntent.Attack("Hit"))
        assertTrue(accepted.accepted, accepted.rejection)
    }

    // ---------------- FASE 24: Gust "el rival elige el nuevo Activo" ----------------

    @Test
    fun `Remolino mueve el Activo rival a su Banca y el rival promueve`() {
        val a = attack("Whirlwind", 60, EffectsDb.atkKey("sv3pt5-12", "Whirlwind"))
        val butterfree = PokemonInPlay(mon("butterfree", 120, EnergyType.GRASS, a))
        val foeActive = PokemonInPlay(mon("foe", 200, EnergyType.FIRE, a))
        val foeBench = PokemonInPlay(mon("bench", 90, EnergyType.WATER, a))
        val player = PlayerState(
            side = Side.PLAYER, active = butterfree,
            deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) },
            prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = foeActive, bench = listOf(foeBench),
            deck = (1..5).map { energy("od$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("opz$it", EnergyType.WATER) },
        )
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val r = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Whirlwind"))

        assertTrue(r.state.awaitingPromotion, "el rival debe promover")
        assertTrue(Side.OPPONENT in r.state.pendingPromotion)
        assertNull(r.state.opponent.active, "el Activo rival se fue a la Banca")
        val moved = r.state.opponent.bench.first { it.card.id.raw == "foe" }
        assertEquals(60, moved.damage, "el daño de Remolino viaja con el Pokémon a la Banca")

        // El rival elige su nuevo Activo (cualquiera de su Banca, incl. el recién movido).
        val done = GameEngine(SeededRng(1)).apply(r.state, GameIntent.PromoteActive(CardId("bench")))
        assertTrue(done.accepted, done.rejection)
        assertEquals("bench", done.state.opponent.active!!.card.id.raw)
        assertTrue(done.state.pendingPromotion.isEmpty())
    }

    // ---------------- FASE 37: des-evolución del Activo rival ----------------

    @Test
    fun `Rayo Involutivo involuciona al Activo rival y devuelve la carta de fase alta a su mano`() {
        val a = attack("Devolution Ray", 100, EffectsDb.atkKey("sv3pt5-142", "Devolution Ray"), cost = 2)
        val aerodactyl = PokemonInPlay(
            mon("aerodactyl", 120, EnergyType.COLORLESS, a),
            attachedEnergy = listOf(energy("e1", EnergyType.COLORLESS), energy("e2", EnergyType.COLORLESS)),
        )
        val basic = mon("charmander", 260, EnergyType.FIRE, a) // PS altos: 100 no noquea tras involucionar
        val evolved = mon("charmeleon", 260, EnergyType.FIRE, a).copy(stage = Stage.Stage1, evolvesFrom = "charmander")
        val defender = PokemonInPlay(evolved, evolutionStack = listOf(basic))
        val r = GameEngine(SeededRng(1)).apply(duel(aerodactyl, defender), GameIntent.Attack("Devolution Ray"))

        assertTrue(r.accepted, r.rejection)
        assertEquals("charmander", r.state.opponent.active!!.card.id.raw, "el Activo rival volvió a su fase Básica")
        assertEquals(100, r.state.opponent.active!!.damage, "el daño permanece tras involucionar")
        assertTrue(r.state.opponent.hand.any { it.id.raw == "charmeleon" }, "la carta de fase más alta vuelve a la mano")
        assertTrue(r.state.opponent.active!!.evolutionStack.isEmpty())
        assertTrue(r.events.any { it is GameEvent.DeEvolved })
    }

    @Test
    fun `Rayo Involutivo puede noquear si los PS de la fase inferior ya no aguantan el dano`() {
        val a = attack("Devolution Ray", 100, EffectsDb.atkKey("sv3pt5-142", "Devolution Ray"), cost = 2)
        val aerodactyl = PokemonInPlay(
            mon("aerodactyl", 120, EnergyType.COLORLESS, a),
            attachedEnergy = listOf(energy("e1", EnergyType.COLORLESS), energy("e2", EnergyType.COLORLESS)),
        )
        val basic = mon("weedle", 60, EnergyType.GRASS, a) // 60 PS < 100 daño → KO tras involucionar
        val evolved = mon("kakuna", 260, EnergyType.GRASS, a).copy(stage = Stage.Stage1, evolvesFrom = "weedle")
        val defender = PokemonInPlay(evolved, evolutionStack = listOf(basic))
        val r = GameEngine(SeededRng(1)).apply(duel(aerodactyl, defender), GameIntent.Attack("Devolution Ray"))

        assertTrue(r.events.any { it is GameEvent.DeEvolved })
        assertTrue(r.events.any { it is GameEvent.KnockedOut }, "la fase Básica (60 PS) no aguanta 100 de daño")
        assertTrue(r.state.opponent.hand.any { it.id.raw == "kakuna" }, "la fase alta vuelve a la mano")
        assertTrue(r.state.opponent.discard.any { it.id.raw == "weedle" }, "la fase Básica noqueada va al descarte")
    }

    // ---------------- FASE 36: pasivos condicionados por aliado en juego ----------------

    @Test
    fun `Rey Entusiasta permite atacar sin Energia si Nidoqueen esta en juego`() {
        val a = attack("Venomous Impact", 30, EffectId("sv3pt5-none"), cost = 3)
        val nidoking = PokemonInPlay(
            mon("nidoking", 170, EnergyType.PSYCHIC, a).copy(
                abilities = listOf(Ability(
                    LocalizedText("Rey Entusiasta", "Enthusiastic King"), LocalizedText("", ""),
                    EffectsDb.abiKey("sv3pt5-34", "Enthusiastic King"))),
            ),
        ) // 0 Energía unida
        val nidoqueen = PokemonInPlay(mon("Nidoqueen", 150, EnergyType.PSYCHIC, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.DARKNESS, a))

        // Con Nidoqueen en Banca → ataca gratis pese a coste 3 y 0 Energía.
        val withQueen = duel(nidoking, foe).let { it.copy(player = it.player.copy(bench = listOf(nidoqueen))) }
        val ok = GameEngine(SeededRng(1)).apply(withQueen, GameIntent.Attack("Venomous Impact"))
        assertTrue(ok.accepted, ok.rejection)

        // Sin Nidoqueen → coste 3 con 0 Energía = rechazado.
        val rejected = GameEngine(SeededRng(1)).apply(duel(nidoking, foe), GameIntent.Attack("Venomous Impact"))
        assertFalse(rejected.accepted, "sin Nidoqueen el ataque exige Energía")
    }

    @Test
    fun `Ovacion Osea suma 30 al ataque de Marowak si Cubone esta en Banca`() {
        val cubAttack = attack("Bonemerang", 60, EffectId("sv3pt5-none"))
        val marowak = PokemonInPlay(mon("marowak", 120, EnergyType.FIGHTING, cubAttack))
        val cubone = PokemonInPlay(
            mon("cubone", 70, EnergyType.FIGHTING, cubAttack).copy(
                abilities = listOf(Ability(
                    LocalizedText("Ovación Ósea", "Cheering Bone"), LocalizedText("", ""),
                    EffectsDb.abiKey("sv3pt5-104", "Cheering Bone"))),
            ),
        )
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, cubAttack))

        // Con Cubone en Banca → 60 + 30 = 90.
        val withCubone = duel(marowak, foe).let { it.copy(player = it.player.copy(bench = listOf(cubone))) }
        val boosted = GameEngine(SeededRng(1)).apply(withCubone, GameIntent.Attack("Bonemerang"))
        assertEquals(90, boosted.events.filterIsInstance<GameEvent.DamageDealt>().first().amount)

        // Sin Cubone → 60 pelado.
        val plain = GameEngine(SeededRng(1)).apply(duel(marowak, foe), GameIntent.Attack("Bonemerang"))
        assertEquals(60, plain.events.filterIsInstance<GameEvent.DamageDealt>().first().amount)
    }

    // ---------------- FASE 35: efectos al final del turno ----------------

    @Test
    fun `Acido de Accion Lenta pone 120 de dano al final del turno del rival`() {
        val a = attack("Slow-Acting Acid", 120, EffectsDb.atkKey("sv3pt5-71", "Slow-Acting Acid"), cost = 2)
        val victreebel = PokemonInPlay(
            mon("victreebel", 150, EnergyType.GRASS, a),
            attachedEnergy = listOf(energy("e1", EnergyType.GRASS), energy("e2", EnergyType.GRASS)),
        )
        val foe = PokemonInPlay(mon("foe", 250, EnergyType.WATER, a)) // aguanta 120 + 120 sin KO
        val st = duel(victreebel, foe)
        val afterAtk = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Slow-Acting Acid"))
        // Daño base inmediato = 120; aún no cae el diferido.
        assertEquals(120, afterAtk.state.opponent.active!!.damage)
        assertEquals(Side.OPPONENT, afterAtk.state.activeSide, "el ataque cerró el turno del jugador")

        // El rival termina su turno → cae el daño diferido (12 contadores = 120).
        val afterEnd = GameEngine(SeededRng(1)).apply(afterAtk.state, GameIntent.EndTurn)
        val foeAfter = afterEnd.state.opponent.active ?: afterEnd.state.opponent.bench.firstOrNull { it.card.id.raw == "foe" }
        assertEquals(240, foeAfter!!.damage, "120 base + 120 diferido")
        assertNull(foeAfter.delayedDamageOnTurn, "el daño diferido se consumió")
    }

    @Test
    fun `Restos cura 20 al Activo al final de tu turno`() {
        val a = attack("Tackle", 10, EffectId("sv3pt5-none"))
        val leftovers = TrainerCard(
            CardId("sv3pt5-163"), LocalizedText("Restos", "Leftovers"), set(), Rarity.COMMON, "H", art(),
            TrainerKind.Tool(), LocalizedText("", ""), EffectId("sv3pt5-163"),
        )
        val mine = PokemonInPlay(mon("mine", 100, EnergyType.WATER, a), damage = 50, attachedTools = listOf(leftovers))
        val foe = PokemonInPlay(mon("foe", 100, EnergyType.FIRE, a))
        val st = duel(mine, foe)
        val r = GameEngine(SeededRng(1)).apply(st, GameIntent.EndTurn)
        val healed = r.state.player.active!!
        assertEquals(30, healed.damage, "Restos cura 20 (50 → 30) al final de tu turno")
        assertTrue(r.events.any { it is GameEvent.Healed })
    }

    @Test
    fun `Oprimir que noquea NO mueve a Banca - el KO va al descarte`() {
        val a = attack("Push Down", 20, EffectsDb.atkKey("sv3pt5-111", "Push Down"))
        val rhyhorn = PokemonInPlay(mon("rhyhorn", 110, EnergyType.FIGHTING, a))
        val fragileFoe = PokemonInPlay(mon("foe", 20, EnergyType.LIGHTNING, a)) // 20 PS: 20 daño = KO
        val foeBench = PokemonInPlay(mon("bench", 90, EnergyType.WATER, a))
        val player = PlayerState(
            side = Side.PLAYER, active = rhyhorn,
            deck = (1..5).map { energy("d$it", EnergyType.LIGHTNING) },
            prizes = (1..6).map { energy("pz$it", EnergyType.LIGHTNING) },
        )
        val opponent = PlayerState(
            side = Side.OPPONENT, active = fragileFoe, bench = listOf(foeBench),
            deck = (1..5).map { energy("od$it", EnergyType.WATER) },
            prizes = (1..6).map { energy("opz$it", EnergyType.WATER) },
        )
        val st = GameState(player, opponent, turn = 3, activeSide = Side.PLAYER, phase = Phase.MAIN)
        val r = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Push Down"))

        assertTrue(r.events.any { it is GameEvent.KnockedOut }, "el Activo rival quedó Noqueado")
        assertTrue(r.state.opponent.bench.none { it.card.id.raw == "foe" }, "el KO no debe estar en la Banca")
        assertTrue(r.state.opponent.discard.any { it.id.raw == "foe" }, "el KO va al descarte")
    }
}
