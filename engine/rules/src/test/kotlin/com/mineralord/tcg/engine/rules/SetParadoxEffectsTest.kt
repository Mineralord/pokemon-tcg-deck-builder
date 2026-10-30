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
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.ModKind
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
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    /** RNG con una secuencia fija de caras/cruces (para ataques "lanza hasta que salga cruz"). */
    private class SeqRng(private val flips: List<Boolean>) : Rng {
        private var i = 0
        override fun flipCoin(): Boolean = flips[i++]
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

    private fun attackFixed(name: String, dmg: Int, effect: EffectId) =
        Attack(LocalizedText(name, name), emptyList(), 0, DamageModel.Fixed(dmg), effect)

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
    fun `Whip Expert hace +70 si uniste una Herramienta a este Pokemon este turno`() {
        val a = attack("Whip Expert", EffectsDb.atkKey("sv4-97", "Whip Expert"))
        val attacker = PokemonInPlay(mon("mienshao", 120, EnergyType.FIGHTING, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.PSYCHIC, a))

        // Sin Herramienta unida este turno → solo el daño base (50).
        val base = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Whip Expert"))
        assertEquals(50, firstDamage(base))

        // Con el atacante marcado como "recibió Herramienta este turno" → 50 + 70 = 120.
        val withTool = duel(attacker, foe).copy(toolsAttachedThisTurn = setOf(CardId("mienshao")))
        assertEquals(120, firstDamage(GameEngine(SeededRng(1)).apply(withTool, GameIntent.Attack("Whip Expert"))))
    }

    @Test
    fun `Lively Tackle hace +90 si este Pokemon fue curado este turno`() {
        val a = attack("Lively Tackle", EffectsDb.atkKey("sv4-147", "Lively Tackle"))
        val attacker = PokemonInPlay(mon("miltank", 120, EnergyType.COLORLESS, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.FIGHTING, a))

        // Sin curación este turno → solo el daño base (60).
        val base = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Lively Tackle"))
        assertEquals(60, firstDamage(base))

        // Con el atacante marcado como "curado este turno" → 60 + 90 = 150.
        val healed = duel(attacker, foe).copy(healedThisTurn = setOf(CardId("miltank")))
        assertEquals(150, firstDamage(GameEngine(SeededRng(1)).apply(healed, GameIntent.Attack("Lively Tackle"))))
    }

    @Test
    fun `Supplemental Swallow-Up revela el top 5 y une Energia Basica a este Pokemon`() {
        val a = attack("Supplemental Swallow-Up", EffectsDb.atkKey("sv4-55", "Supplemental Swallow-Up"))
        val dondozo = PokemonInPlay(mon("dondozo", 150, EnergyType.WATER, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIRE, a))

        // El mazo propio son 5 Energías Básicas (d1..d5): revela las 5 y pausa con la decisión de arrastre.
        val paused = GameEngine(SeededRng(1)).apply(duel(dondozo, foe), GameIntent.Attack("Supplemental Swallow-Up"))
        val d = paused.state.interaction!!.decision as PendingDecision.AttachFromRevealed
        assertEquals(5, d.revealed.size)
        assertEquals(5, d.energyCandidates.size)
        assertTrue(d.benchCandidates.contains(CardId("dondozo")))  // el Activo atacante es destino válido

        // Une la 1ª Energía revelada a Dondozo; el resto del mazo se baraja de vuelta.
        val res = GameEngine(SeededRng(1)).apply(
            paused.state, GameIntent.ResolveDecision(listOf(CardId("d1"), CardId("dondozo"))))
        assertTrue(res.state.player.active!!.attachedEnergy.any { it.id == CardId("d1") })
        assertEquals(4, res.state.player.deck.size)
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
    fun `Energized Attack hace 40 por cada Energia unida (2 = 80)`() {
        val a = attack("Energized Attack", EffectsDb.atkKey("sv4-144", "Energized Attack"))
        val e2 = listOf(energy("x1", EnergyType.LIGHTNING), energy("x2", EnergyType.LIGHTNING))
        val attacker = PokemonInPlay(mon("tool", 120, EnergyType.LIGHTNING, a), attachedEnergy = e2)
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.WATER, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Energized Attack"))
        assertEquals(80, r.state.opponent.active?.damage)
    }

    @Test
    fun `Earthen Spike impide atacar al propio Pokemon el proximo turno`() {
        val a = attack("Earthen Spike", EffectsDb.atkKey("sv4-108", "Earthen Spike"))
        val attacker = PokemonInPlay(mon("great", 180, EnergyType.FIGHTING, a))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.LIGHTNING, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Earthen Spike"))
        assertEquals(true, r.state.player.active?.cannotAttackOnTurn != null)
    }

    @Test
    fun `Frenzied Gouging noquea al Activo rival y se hace 200 de retroceso`() {
        val a = attack("Frenzied Gouging", EffectsDb.atkKey("sv4-124", "Frenzied Gouging"))
        val attacker = PokemonInPlay(mon("roar", 220, EnergyType.DRAGON, a))
        val foe = PokemonInPlay(mon("foe", 70, EnergyType.PSYCHIC, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Frenzied Gouging"))
        assertEquals(200, r.state.player.active?.damage)
    }

    @Test
    fun `Arrogant Impact no hace nada si el atacante tiene 4+ contadores de dano`() {
        val a = attack("Arrogant Impact", EffectsDb.atkKey("sv4-109", "Arrogant Impact"))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.LIGHTNING, a))
        val sano = PokemonInPlay(mon("s1", 220, EnergyType.FIGHTING, a), damage = 30)      // 3 contadores
        val herido = PokemonInPlay(mon("s2", 220, EnergyType.FIGHTING, a), damage = 40)    // 4 contadores
        // Con 3 contadores: hace su daño fijo (220). Con 4+: no hace nada (0).
        assertEquals(0, GameEngine(SeededRng(1)).apply(duel(herido, foe), GameIntent.Attack("Arrogant Impact")).state.opponent.active?.damage)
    }

    @Test
    fun `Vengeful Shock hace 30 y deja Paralizado al Activo rival (sin KO previo)`() {
        val a = attack("Vengeful Shock", EffectsDb.atkKey("sv4-68", "Vengeful Shock"))
        val attacker = PokemonInPlay(mon("volt", 120, EnergyType.LIGHTNING, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.PSYCHIC, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Vengeful Shock"))
        assertEquals(30, r.state.opponent.active?.damage)
        assertEquals(true, r.state.opponent.active?.statuses?.contains(com.mineralord.tcg.engine.model.Status.PARALYZED))
    }

    @Test
    fun `Magma Purge descarta Energia propia y hace 60 por cada una (2 = 120)`() {
        val a = attack("Magma Purge", EffectsDb.atkKey("sv4-93", "Magma Purge"))
        val e2 = listOf(energy("f1", EnergyType.FIRE), energy("f2", EnergyType.FIRE))
        val attacker = PokemonInPlay(mon("purge", 150, EnergyType.FIRE, a), attachedEnergy = e2)
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.GRASS, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Magma Purge"))
        assertEquals(120, r.state.opponent.active?.damage)
        assertEquals(0, r.state.player.active?.attachedEnergy?.size)   // descartó las 2
    }

    @Test
    fun `Leech Life cura al atacante lo mismo que el dano infligido`() {
        val a = attackFixed("Leech Life", 30, EffectsDb.atkKey("sv4-111", "Leech Life"))
        val attacker = PokemonInPlay(mon("leech", 90, EnergyType.GRASS, a), damage = 50)
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.WATER, a))
        val r = GameEngine(SeededRng(1)).apply(duel(attacker, foe), GameIntent.Attack("Leech Life"))
        assertEquals(30, r.state.opponent.active?.damage)   // daño infligido
        assertEquals(20, r.state.player.active?.damage)     // 50 - 30 curados
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

    // ─────────────────────────── Fase 15: Habilidades ───────────────────────────

    @Test
    fun `Exoskeleton reduce 30 el dano recibido`() {
        val hit = attackFixed("Hit", 100, EffectId("none"))
        val attacker = PokemonInPlay(mon("atk", 200, EnergyType.FIRE, hit))
        val withAbi = PokemonInPlay(mon("sv4-128", 200, EnergyType.GRASS, attack("x", EffectId("none"))).copy(
            abilities = listOf(Ability(
                LocalizedText("Exoesqueleto", "Exoskeleton"), LocalizedText("", ""),
                EffectsDb.abiKey("sv4-128", "Exoskeleton")))))
        val plain = PokemonInPlay(mon("plain", 200, EnergyType.GRASS, attack("x", EffectId("none"))))

        assertEquals(70, GameEngine(SeededRng(1)).apply(duel(attacker, withAbi), GameIntent.Attack("Hit")).state.opponent.active?.damage)
        assertEquals(100, GameEngine(SeededRng(1)).apply(duel(attacker, plain), GameIntent.Attack("Hit")).state.opponent.active?.damage)
    }

    @Test
    fun `Startling Pumpkin descarta 2 cartas de la mano rival al ser Noqueado`() {
        val ko = attackFixed("KO", 100, EffectId("none"))
        val attacker = PokemonInPlay(mon("atk", 200, EnergyType.PSYCHIC, ko))
        val pumpkin = PokemonInPlay(mon("sv4-78", 60, EnergyType.PSYCHIC, attack("x", EffectId("none"))).copy(
            abilities = listOf(Ability(
                LocalizedText("Calabaza Sobresaltante", "Startling Pumpkin"), LocalizedText("", ""),
                EffectsDb.abiKey("sv4-78", "Startling Pumpkin")))))
        val oppBench = PokemonInPlay(mon("oppally", 100, EnergyType.DARKNESS, attack("x", EffectId("none"))))
        val base = duel(attacker, pumpkin)
        val st = base.copy(
            player = base.player.copy(hand = listOf(
                energy("h1", EnergyType.PSYCHIC), energy("h2", EnergyType.PSYCHIC), energy("h3", EnergyType.PSYCHIC))),
            opponent = base.opponent.copy(bench = listOf(oppBench)),
        )

        // Al noquear a la calabaza, SU Habilidad hace que el atacante (jugador) descarte 2 de su mano.
        // Comprobamos el descarte (el Premio del KO sumaría +1 a la mano, así que medimos el descarte).
        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("KO"))
        assertEquals(2, res.state.player.discard.size)
    }

    @Test
    fun `Humming Heal cura 20 a cada uno de tus Pokemon`() {
        val abi = Ability(
            LocalizedText("Cura Tarareada", "Humming Heal"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-140", "Humming Heal"))
        val active = PokemonInPlay(
            mon("sv4-140", 150, EnergyType.PSYCHIC, attack("x", EffectId("none"))).copy(abilities = listOf(abi)),
            damage = 50,
        )
        val benched = PokemonInPlay(mon("ally", 120, EnergyType.PSYCHIC, attack("x", EffectId("none"))), damage = 30)
        val foe = PokemonInPlay(mon("foe", 120, EnergyType.DARKNESS, attack("x", EffectId("none"))))
        val base = duel(active, foe)
        val st = base.copy(player = base.player.copy(bench = listOf(benched)))

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.UseAbility(CardId("sv4-140"), "Cura Tarareada"))
        assertTrue(res.accepted, res.rejection)
        assertEquals(30, res.state.player.active?.damage)     // 50 - 20
        assertEquals(10, res.state.player.bench.first().damage) // 30 - 20
    }

    @Test
    fun `Suction Cup Draw roba 3 al evolucionar desde la mano`() {
        val dummy = attack("x", EffectId("none"))
        val basic = PokemonInPlay(mon("Remoraid", 60, EnergyType.WATER, dummy)).copy(turnsInPlay = 1)
        val evo = mon("sv4-34", 110, EnergyType.WATER, dummy).copy(
            stage = Stage.Stage1, evolvesFrom = "Remoraid",
            abilities = listOf(Ability(
                LocalizedText("Robo por Ventosas", "Suction Cup Draw"), LocalizedText("", ""),
                EffectsDb.abiKey("sv4-34", "Suction Cup Draw"))))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIRE, dummy))
        val base = duel(basic, foe)
        val st = base.copy(player = base.player.copy(hand = listOf(evo)))

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Evolve(CardId("sv4-34"), CardId("Remoraid")))
        assertTrue(res.accepted, res.rejection)
        assertEquals(2, res.state.player.deck.size)   // 5 - 3 robadas
        assertEquals(3, res.state.player.hand.size)   // el evo salió de la mano; +3 robadas
    }

    @Test
    fun `Stance evita dano y efectos al evolucionar desde la mano`() {
        val dummy = attack("x", EffectId("none"))
        val basic = PokemonInPlay(mon("Charcadet", 70, EnergyType.PSYCHIC, dummy)).copy(turnsInPlay = 1)
        val evo = mon("sv4-81", 120, EnergyType.PSYCHIC, dummy).copy(
            stage = Stage.Stage1, evolvesFrom = "Charcadet",
            abilities = listOf(Ability(
                LocalizedText("Postura", "Stance"), LocalizedText("", ""),
                EffectsDb.abiKey("sv4-81", "Stance"))))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.DARKNESS, dummy))
        val base = duel(basic, foe)
        val st = base.copy(player = base.player.copy(hand = listOf(evo)))

        val res = GameEngine(SeededRng(1)).apply(st, GameIntent.Evolve(CardId("sv4-81"), CardId("Charcadet")))
        assertTrue(res.accepted, res.rejection)
        assertTrue(res.state.player.active?.preventDamageOnTurn != null)
        assertTrue(res.state.player.active?.preventEffectsOnTurn != null)
    }

    @Test
    fun `Swarming Rage cuenta solo los contadores de dano en tus Durant`() {
        val a = attack("Swarming Rage", EffectsDb.atkKey("sv4-129", "Swarming Rage"))
        val durant = PokemonInPlay(mon("Durant", 70, EnergyType.METAL, a), damage = 30)      // 3 contadores
        val other = PokemonInPlay(mon("Snorlax", 150, EnergyType.COLORLESS, a), damage = 50)  // 5 contadores (no cuentan)
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.FIRE, a))
        val base = duel(durant, foe)
        val st = base.copy(player = base.player.copy(bench = listOf(other)))

        // Solo los 3 contadores de Durant → 3 × 20 = 60 (los 5 de Snorlax quedan excluidos por el nombre).
        val r = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Swarming Rage"))
        assertEquals(60, r.state.opponent.active?.damage)
    }

    @Test
    fun `Daunting Eyes devuelve al mazo rival 1 Energia por cada cara`() {
        val a = attack("Daunting Eyes", EffectsDb.atkKey("sv4-2", "Daunting Eyes"))
        val attacker = PokemonInPlay(mon("sv4-2", 120, EnergyType.PSYCHIC, a))
        val foe = PokemonInPlay(
            mon("foe", 200, EnergyType.WATER, a),
            attachedEnergy = listOf(energy("e1", EnergyType.WATER), energy("e2", EnergyType.WATER)),
        )
        // 2 caras y luego cruz → mueve 2 Energías del Activo rival a su mazo.
        val r = GameEngine(SeqRng(listOf(true, true, false))).apply(duel(attacker, foe), GameIntent.Attack("Daunting Eyes"))
        assertEquals(0, r.state.opponent.active?.attachedEnergy?.size)
        // Las 2 Energías vuelven a la zona de mazo del rival (mazo+mano = 5 iniciales + 2; el robo de
        // inicio de su turno solo reubica 1 del mazo a la mano, por eso contamos ambos).
        assertEquals(7, r.state.opponent.deck.size + r.state.opponent.hand.size)
    }

    @Test
    fun `Pep Up baraja la mano y roba 1 por cada Pokemon en Banca de ambos lados`() {
        val a = attack("Pep Up", EffectsDb.atkKey("sv4-67", "Pep Up"))
        val attacker = PokemonInPlay(mon("sv4-67", 120, EnergyType.LIGHTNING, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.WATER, a))
        val myBench = listOf(
            PokemonInPlay(mon("b1", 60, EnergyType.LIGHTNING, a)),
            PokemonInPlay(mon("b2", 60, EnergyType.LIGHTNING, a)),
        )
        val oppBench = listOf(PokemonInPlay(mon("ob1", 60, EnergyType.WATER, a)))
        val base = duel(attacker, foe)
        val st = base.copy(
            player = base.player.copy(
                hand = listOf(energy("h1", EnergyType.LIGHTNING), energy("h2", EnergyType.LIGHTNING)),
                bench = myBench,
            ),
            opponent = base.opponent.copy(bench = oppBench),
        )
        // Baraja la mano (2) en el mazo; luego roba 2 (mi Banca) + 1 (Banca rival) = 3.
        val r = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Pep Up"))
        assertEquals(3, r.state.player.hand.size)
    }

    @Test
    fun `Data Displacement mueve 1 Energia del Activo rival a su Banca si sale cara`() {
        val a = attack("Data Displacement", EffectsDb.atkKey("sv4-142", "Data Displacement"))
        val attacker = PokemonInPlay(mon("sv4-142", 120, EnergyType.PSYCHIC, a))
        val oppActive = PokemonInPlay(mon("foe", 200, EnergyType.WATER, a), attachedEnergy = listOf(energy("e1", EnergyType.WATER)))
        val oppBench = PokemonInPlay(mon("ob1", 90, EnergyType.WATER, a))
        val base = duel(attacker, oppActive)
        val st = base.copy(opponent = base.opponent.copy(bench = listOf(oppBench)))

        // 1) El ataque pausa pidiendo el Pokémon de Banca rival destino.
        val paused = GameEngine(FixedRng(true)).apply(st, GameIntent.Attack("Data Displacement"))
        // 2) Se elige "ob1" y, con cara, la Energía del Activo rival pasa a ese Pokémon.
        val r = GameEngine(FixedRng(true)).apply(paused.state, GameIntent.ResolveDecision(listOf(CardId("ob1"))))
        assertEquals(0, r.state.opponent.active?.attachedEnergy?.size)
        assertEquals(1, r.state.opponent.bench.first().attachedEnergy.size)
    }

    @Test
    fun `Burst of Braying une hasta los Premios que el rival ha tomado`() {
        val a = attack("Burst of Braying", EffectsDb.atkKey("sv4-63", "Burst of Braying"))
        val attacker = PokemonInPlay(mon("sv4-63", 120, EnergyType.LIGHTNING, a))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.WATER, a))
        val base = duel(attacker, foe)
        // El rival ha tomado 2 Premios (le quedan 4); mi descarte tiene 2 Energía Rayo.
        val st = base.copy(
            player = base.player.copy(discard = listOf(energy("l1", EnergyType.LIGHTNING), energy("l2", EnergyType.LIGHTNING))),
            opponent = base.opponent.copy(prizes = base.opponent.prizes.take(4)),
        )
        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Burst of Braying"))
        val d = paused.state.interaction!!.decision as PendingDecision.AttachFromRevealed
        assertEquals(2, d.maxAttach)   // conteo dinámico = Premios tomados por el rival
    }

    @Test
    fun `Crimson Armor reduce 80 solo con los PS completos`() {
        val hit = attackFixed("Hit", 100, EffectId("none"))
        val attacker = PokemonInPlay(mon("atk", 200, EnergyType.FIRE, hit))
        fun armor(dmg: Int) = PokemonInPlay(
            mon("sv4-27", 200, EnergyType.GRASS, attack("x", EffectId("none"))).copy(
                abilities = listOf(Ability(
                    LocalizedText("Armadura Carmesí", "Crimson Armor"), LocalizedText("", ""),
                    EffectsDb.abiKey("sv4-27", "Crimson Armor")))),
            damage = dmg,
        )
        // PS completos → 100 − 80 = 20.
        assertEquals(20, GameEngine(SeededRng(1)).apply(duel(attacker, armor(0)), GameIntent.Attack("Hit")).state.opponent.active?.damage)
        // Con daño previo (no completo) → sin reducción: 10 + 100 = 110.
        assertEquals(110, GameEngine(SeededRng(1)).apply(duel(attacker, armor(10)), GameIntent.Attack("Hit")).state.opponent.active?.damage)
    }

    @Test
    fun `In a Hungry Hurry permite retirarse gratis solo sin Energia unida`() {
        val dummy = attack("x", EffectId("none"))
        fun hungry(energy: List<BasicEnergy>) = PokemonInPlay(
            mon("sv4-121", 120, EnergyType.COLORLESS, dummy)
                .copy(retreatCost = listOf(EnergyType.COLORLESS, EnergyType.COLORLESS),
                    abilities = listOf(Ability(
                        LocalizedText("Con Prisas", "In a Hungry Hurry"), LocalizedText("", ""),
                        EffectsDb.abiKey("sv4-121", "In a Hungry Hurry")))),
            attachedEnergy = energy,
        )
        val bench = PokemonInPlay(mon("b1", 60, EnergyType.COLORLESS, dummy))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.WATER, dummy))
        fun stateWith(energy: List<BasicEnergy>) = duel(hungry(energy), foe).let {
            it.copy(player = it.player.copy(bench = listOf(bench)))
        }
        // Sin Energía → retirada gratis: acepta y cambia el Activo.
        val ok = GameEngine(SeededRng(1)).apply(stateWith(emptyList()), GameIntent.Retreat(CardId("b1")))
        assertTrue(ok.accepted, ok.rejection)
        assertEquals(CardId("b1"), ok.state.player.active?.card?.id)
        // Con 1 Energía (coste 2, no la paga) → rechazada.
        val no = GameEngine(SeededRng(1)).apply(stateWith(listOf(energy("e1", EnergyType.COLORLESS))), GameIntent.Retreat(CardId("b1")))
        assertFalse(no.accepted)
    }

    @Test
    fun `Mysterious Shield evita el dano de atacantes ex y V`() {
        val hit = attackFixed("Hit", 100, EffectId("none"))
        val shield = PokemonInPlay(mon("sv4-134", 200, EnergyType.PSYCHIC, attack("x", EffectId("none"))).copy(
            abilities = listOf(Ability(
                LocalizedText("Escudo Misterioso", "Mysterious Shield"), LocalizedText("", ""),
                EffectsDb.abiKey("sv4-134", "Mysterious Shield")))))
        val exAttacker = PokemonInPlay(mon("ex", 200, EnergyType.FIRE, hit).copy(mechanic = PokemonMechanic.ExLower))
        val normalAttacker = PokemonInPlay(mon("norm", 200, EnergyType.FIRE, hit))
        // Atacante ex → 0 (escudo); atacante normal → 100.
        assertEquals(0, GameEngine(SeededRng(1)).apply(duel(exAttacker, shield), GameIntent.Attack("Hit")).state.opponent.active?.damage)
        assertEquals(100, GameEngine(SeededRng(1)).apply(duel(normalAttacker, shield), GameIntent.Attack("Hit")).state.opponent.active?.damage)
    }

    @Test
    fun `Evanescent hace que el rival coja 1 Premio menos al ser Noqueado con cara`() {
        val ko = attackFixed("KO", 100, EffectId("none"))
        val attacker = PokemonInPlay(mon("atk", 200, EnergyType.PSYCHIC, ko))
        val evan = PokemonInPlay(mon("sv4-3", 60, EnergyType.PSYCHIC, attack("x", EffectId("none"))).copy(
            abilities = listOf(Ability(
                LocalizedText("Evanescente", "Evanescent"), LocalizedText("", ""),
                EffectsDb.abiKey("sv4-3", "Evanescent")))))
        val oppBench = PokemonInPlay(mon("ob1", 100, EnergyType.DARKNESS, attack("x", EffectId("none"))))
        val st = duel(attacker, evan).let { it.copy(opponent = it.opponent.copy(bench = listOf(oppBench))) }
        // Cara → el atacante coge 1 Premio menos (1-1=0) → mantiene sus 6; cruz → coge 1 → 5.
        assertEquals(6, GameEngine(FixedRng(true)).apply(st, GameIntent.Attack("KO")).state.player.prizes.size)
        assertEquals(5, GameEngine(FixedRng(false)).apply(st, GameIntent.Attack("KO")).state.player.prizes.size)
    }

    @Test
    fun `Coin Bonus roba 1 y 1 mas si esta Activo`() {
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Bonificación por Moneda", "Coin Bonus"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-139", "Coin Bonus"))
        val foe = PokemonInPlay(mon("foe", 120, EnergyType.WATER, dummy))
        // Activo con la habilidad → roba 2 (1 base + 1 por estar Activo).
        val active = PokemonInPlay(mon("sv4-139", 120, EnergyType.LIGHTNING, dummy).copy(abilities = listOf(abi)))
        val rA = GameEngine(SeededRng(1)).apply(duel(active, foe), GameIntent.UseAbility(CardId("sv4-139"), "Bonificación por Moneda"))
        assertTrue(rA.accepted, rA.rejection)
        assertEquals(2, rA.state.player.hand.size)
        // En Banca → solo roba 1.
        val onBench = PokemonInPlay(mon("sv4-139", 120, EnergyType.LIGHTNING, dummy).copy(abilities = listOf(abi)))
        val other = PokemonInPlay(mon("act", 120, EnergyType.LIGHTNING, dummy))
        val st = duel(other, foe).let { it.copy(player = it.player.copy(bench = listOf(onBench))) }
        val rB = GameEngine(SeededRng(1)).apply(st, GameIntent.UseAbility(CardId("sv4-139"), "Bonificación por Moneda"))
        assertTrue(rB.accepted, rB.rejection)
        assertEquals(1, rB.state.player.hand.size)
    }

    @Test
    fun `Frigid Room impide atacar al rival con 40 PS o menos restantes`() {
        val hit = attackFixed("Hit", 30, EffectId("none"))
        val frigid = PokemonInPlay(mon("sv4-45", 120, EnergyType.WATER, attack("x", EffectId("none"))).copy(
            abilities = listOf(Ability(
                LocalizedText("Sala Gélida", "Frigid Room"), LocalizedText("", ""),
                EffectsDb.abiKey("sv4-45", "Frigid Room")))))
        // 60 PS con 30 de daño → 30 PS restantes (≤40) → NO puede atacar.
        val weak = PokemonInPlay(mon("weak", 60, EnergyType.FIRE, hit), damage = 30)
        assertFalse(GameEngine(SeededRng(1)).apply(duel(weak, frigid), GameIntent.Attack("Hit")).accepted)
        // 120 PS sin daño → 120 restantes (>40) → SÍ puede atacar.
        val strong = PokemonInPlay(mon("strong", 120, EnergyType.FIRE, hit))
        val r = GameEngine(SeededRng(1)).apply(duel(strong, frigid), GameIntent.Attack("Hit"))
        assertTrue(r.accepted, r.rejection)
    }

    @Test
    fun `Clairvoyant Sense une Energia de la mano a la Banca y roba 2`() {
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Sentido Clarividente", "Clairvoyant Sense"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-72", "Clairvoyant Sense"))
        val active = PokemonInPlay(mon("sv4-72", 120, EnergyType.PSYCHIC, dummy).copy(abilities = listOf(abi)))
        val bench = PokemonInPlay(mon("b1", 60, EnergyType.PSYCHIC, dummy))
        val foe = PokemonInPlay(mon("foe", 120, EnergyType.WATER, dummy))
        val st = duel(active, foe).let {
            it.copy(player = it.player.copy(hand = listOf(energy("pe", EnergyType.PSYCHIC)), bench = listOf(bench)))
        }
        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.UseAbility(CardId("sv4-72"), "Sentido Clarividente"))
        val r = GameEngine(SeededRng(1)).apply(paused.state, GameIntent.ResolveDecision(listOf(CardId("b1"))))
        assertEquals(1, r.state.player.bench.first().attachedEnergy.size)   // Energía unida a b1
        assertEquals(2, r.state.player.hand.size)                          // 1 - 1 unida + 2 robadas
    }

    @Test
    fun `Energizing Rock Salt une Energia del descarte y cura 30 a ese Pokemon`() {
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Sal Mineral Energizante", "Energizing Rock Salt"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-104", "Energizing Rock Salt"))
        val active = PokemonInPlay(mon("sv4-104", 150, EnergyType.FIGHTING, dummy).copy(abilities = listOf(abi)), damage = 50)
        val foe = PokemonInPlay(mon("foe", 120, EnergyType.WATER, dummy))
        val st = duel(active, foe).let {
            it.copy(player = it.player.copy(discard = listOf(energy("fe", EnergyType.FIGHTING))))
        }
        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.UseAbility(CardId("sv4-104"), "Sal Mineral Energizante"))
        val r = GameEngine(SeededRng(1)).apply(paused.state, GameIntent.ResolveDecision(listOf(CardId("sv4-104"))))
        assertEquals(1, r.state.player.active?.attachedEnergy?.size)   // 1 Energía unida al Activo
        assertEquals(20, r.state.player.active?.damage)               // 50 - 30 curados
    }

    @Test
    fun `Tachyon Bits solo es usable al pasar de la Banca al Activo y pone 2 contadores`() {
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Bits de Taquión", "Tachyon Bits"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-89", "Tachyon Bits"))
        val front = PokemonInPlay(mon("front", 100, EnergyType.PSYCHIC, dummy))
        val tachyon = PokemonInPlay(mon("sv4-89", 120, EnergyType.PSYCHIC, dummy).copy(abilities = listOf(abi)))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.WATER, dummy))
        val st = duel(front, foe).let { it.copy(player = it.player.copy(bench = listOf(tachyon))) }
        // En Banca → la habilidad aún NO es usable.
        assertFalse(GameEngine(SeededRng(1)).apply(st, GameIntent.UseAbility(CardId("sv4-89"), "Bits de Taquión")).accepted)
        // Retira → Tachyon pasa al Activo (marcado este turno).
        val retreated = GameEngine(SeededRng(1)).apply(st, GameIntent.Retreat(CardId("sv4-89")))
        assertTrue(retreated.accepted, retreated.rejection)
        assertEquals(CardId("sv4-89"), retreated.state.player.active?.card?.id)
        // Ahora usa la habilidad → elige al Activo rival → 2 contadores (20 de daño).
        val paused = GameEngine(SeededRng(1)).apply(retreated.state, GameIntent.UseAbility(CardId("sv4-89"), "Bits de Taquión"))
        val r = GameEngine(SeededRng(1)).apply(paused.state, GameIntent.ResolveDecision(listOf(CardId("foe"))))
        assertEquals(20, r.state.opponent.active?.damage)
    }

    @Test
    fun `Solidarity pone 3 contadores en el Atacante por cada Tandemaus y Maushold en juego`() {
        val dummy = attack("x", EffectId("none"))
        val hit = attackFixed("Hit", 50, EffectId("none"))
        val maushold = PokemonInPlay(mon("sv4-155", 200, EnergyType.PSYCHIC, dummy).copy(
            name = LocalizedText("Maushold", "Maushold"),
            abilities = listOf(Ability(
                LocalizedText("Solidaridad", "Solidarity"), LocalizedText("", ""),
                EffectsDb.abiKey("sv4-155", "Solidarity")))))
        val tandemaus = PokemonInPlay(mon("tande", 60, EnergyType.PSYCHIC, dummy).copy(
            name = LocalizedText("Tandemaus", "Tandemaus")))
        val attacker = PokemonInPlay(mon("atk", 200, EnergyType.FIRE, hit))
        // El rival (defensor) tiene Maushold (Activo) + Tandemaus (Banca) = 2 → 2 × 30 = 60 al Atacante.
        val st = duel(attacker, maushold).let { it.copy(opponent = it.opponent.copy(bench = listOf(tandemaus))) }
        val r = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Hit"))
        assertEquals(60, r.state.player.active?.damage)
    }

    @Test
    fun `Monkey Trio ignora los Incoloros del coste solo con el trio completo en juego`() {
        // Simisage con un ataque de coste {G}{C}{C} y solo 1 Energía {G} unida.
        val atk = Attack(
            LocalizedText("Púa", "Arm Thrust"),
            listOf(EnergyType.GRASS, EnergyType.COLORLESS, EnergyType.COLORLESS),
            3, DamageModel.Fixed(100), EffectId("none"),
        )
        fun simisage() = PokemonInPlay(
            mon("sv4-5", 100, EnergyType.GRASS, atk, Stage.Stage1).copy(
                name = LocalizedText("Simisage", "Simisage"),
                abilities = listOf(Ability(
                    LocalizedText("Trío de Monos", "Monkey Trio"), LocalizedText("", ""),
                    EffectsDb.abiKey("sv4-5", "Monkey Trio"))),
            ),
            attachedEnergy = listOf(energy("g", EnergyType.GRASS)),
        )
        val simisear = PokemonInPlay(mon("sv4-21", 100, EnergyType.FIRE, atk, Stage.Stage1).copy(
            name = LocalizedText("Simisear", "Simisear")))
        val simipour = PokemonInPlay(mon("sv4-42", 100, EnergyType.WATER, atk, Stage.Stage1).copy(
            name = LocalizedText("Simipour", "Simipour")))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.PSYCHIC, atk))
        val engine = GameEngine(SeededRng(1))

        // Sin el trío completo (solo Simisage): no puede pagar {G}{C}{C} con 1 {G} → ataque NO legal.
        val solo = duel(simisage(), foe)
        assertFalse(engine.legalIntents(solo).any { it is GameIntent.Attack && it.attackName == "Púa" })

        // Con Simisear y Simipour en la Banca: se ignoran los {C} → basta 1 {G} → ataque legal.
        val trio = duel(simisage(), foe).let {
            it.copy(player = it.player.copy(bench = listOf(simisear, simipour)))
        }
        assertTrue(engine.legalIntents(trio).any { it is GameIntent.Attack && it.attackName == "Púa" })
    }

    @Test
    fun `Tangled Feet evita el dano con cara solo si esta Confundido`() {
        val hit = attackFixed("Hit", 50, EffectId("none"))
        val abi = Ability(LocalizedText("Pies Enredados", "Tangled Feet"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-151", "Tangled Feet"))
        fun spinda(confused: Boolean) = PokemonInPlay(
            mon("sv4-151", 120, EnergyType.COLORLESS, hit).copy(abilities = listOf(abi)),
            statuses = if (confused) setOf(Status.CONFUSED) else emptySet())
        val attacker = PokemonInPlay(mon("atk", 200, EnergyType.FIRE, hit))
        // Confundido + cara → daño evitado.
        val heads = GameEngine(FixedRng(true)).apply(duel(attacker, spinda(true)), GameIntent.Attack("Hit"))
        assertEquals(0, heads.state.opponent.active?.damage)
        // Confundido + cruz → daño normal.
        val tails = GameEngine(FixedRng(false)).apply(duel(attacker, spinda(true)), GameIntent.Attack("Hit"))
        assertEquals(50, tails.state.opponent.active?.damage)
        // No Confundido → el escudo no aplica aunque la moneda fuese cara.
        val sano = GameEngine(FixedRng(true)).apply(duel(attacker, spinda(false)), GameIntent.Attack("Hit"))
        assertEquals(50, sano.state.opponent.active?.damage)
    }

    @Test
    fun `Toxic Powder envenena ambos Activos solo con el Tanque del Pasado unido`() {
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Polvo Tóxico", "Toxic Powder"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-123", "Toxic Powder"))
        val capsule = TrainerCard(
            id = CardId("sv4-159"),
            name = LocalizedText("Tanque de Energía Potenciadora del Pasado", "Ancient Booster Energy Capsule"),
            set = set(), rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
            kind = TrainerKind.Tool(), text = LocalizedText("t", "t"), effect = EffectId("none"))
        val foe = PokemonInPlay(mon("foe", 120, EnergyType.WATER, dummy))
        // Sin la Herramienta → la habilidad se rechaza.
        val sinTool = PokemonInPlay(mon("sv4-123", 120, EnergyType.DARKNESS, dummy).copy(abilities = listOf(abi)))
        assertFalse(GameEngine(SeededRng(1)).apply(
            duel(sinTool, foe), GameIntent.UseAbility(CardId("sv4-123"), "Polvo Tóxico")).accepted)
        // Con el Tanque del Pasado unido → ambos Activos quedan Envenenados.
        val conTool = PokemonInPlay(
            mon("sv4-123", 120, EnergyType.DARKNESS, dummy).copy(abilities = listOf(abi)),
            attachedTools = listOf(capsule))
        val r = GameEngine(SeededRng(1)).apply(
            duel(conTool, foe), GameIntent.UseAbility(CardId("sv4-123"), "Polvo Tóxico"))
        assertTrue(r.accepted, r.rejection)
        assertTrue(Status.POISONED in (r.state.player.active?.statuses ?: emptySet()))
        assertTrue(Status.POISONED in (r.state.opponent.active?.statuses ?: emptySet()))
    }

    @Test
    fun `Magnetic Absorption une Energia Lucha del descarte solo con 4 Premios rivales o menos`() {
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Absorción Magnética", "Magnetic Absorption"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-108", "Magnetic Absorption"))
        val active = PokemonInPlay(mon("sv4-108", 150, EnergyType.FIGHTING, dummy).copy(abilities = listOf(abi)))
        val foe = PokemonInPlay(mon("foe", 120, EnergyType.WATER, dummy))
        fun conPremios(n: Int) = duel(active, foe).let {
            it.copy(
                player = it.player.copy(discard = listOf(energy("fe", EnergyType.FIGHTING))),
                opponent = it.opponent.copy(prizes = (1..n).map { k -> energy("op$k", EnergyType.WATER) }))
        }
        // Rival con 6 Premios → rechazada.
        assertFalse(GameEngine(SeededRng(1)).apply(
            conPremios(6), GameIntent.UseAbility(CardId("sv4-108"), "Absorción Magnética")).accepted)
        // Rival con 4 Premios → une 1 Energía Lucha del descarte al portador.
        val r = GameEngine(SeededRng(1)).apply(
            conPremios(4), GameIntent.UseAbility(CardId("sv4-108"), "Absorción Magnética"))
        assertTrue(r.accepted, r.rejection)
        assertEquals(1, r.state.player.active?.attachedEnergy?.size)
        assertEquals(0, r.state.player.discard.size)
    }

    @Test
    fun `Buggy Turbo une hasta 4 Energias del descarte con cara`() {
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Turbo Defectuoso", "Buggy Turbo"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-144", "Buggy Turbo"))
        val active = PokemonInPlay(mon("sv4-144", 120, EnergyType.COLORLESS, dummy).copy(abilities = listOf(abi)))
        val foe = PokemonInPlay(mon("foe", 120, EnergyType.WATER, dummy))
        val st = duel(active, foe).let {
            it.copy(player = it.player.copy(discard = (1..5).map { k -> energy("de$k", EnergyType.FIRE) }))
        }
        val r = GameEngine(FixedRng(true)).apply(st, GameIntent.UseAbility(CardId("sv4-144"), "Turbo Defectuoso"))
        assertTrue(r.accepted, r.rejection)
        assertEquals(4, r.state.player.active?.attachedEnergy?.size)   // hasta 4 unidas
        assertEquals(1, r.state.player.discard.size)                  // 5 - 4
    }

    @Test
    fun `Buggy Turbo descarta 1 Energia propia con cruz`() {
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Turbo Defectuoso", "Buggy Turbo"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-144", "Buggy Turbo"))
        val active = PokemonInPlay(
            mon("sv4-144", 120, EnergyType.COLORLESS, dummy).copy(abilities = listOf(abi)),
            attachedEnergy = listOf(energy("at", EnergyType.FIRE)))
        val foe = PokemonInPlay(mon("foe", 120, EnergyType.WATER, dummy))
        val r = GameEngine(FixedRng(false)).apply(duel(active, foe), GameIntent.UseAbility(CardId("sv4-144"), "Turbo Defectuoso"))
        assertTrue(r.accepted, r.rejection)
        assertEquals(0, r.state.player.active?.attachedEnergy?.size)
        assertEquals(1, r.state.player.discard.size)
    }

    @Test
    fun `Incendiary Song descarta 1 Fuego y suma 60 a los ataques del turno`() {
        val hit = attackFixed("Hit", 40, EffectId("none"))
        val abi = Ability(LocalizedText("Canción Incendiaria", "Incendiary Song"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-137", "Incendiary Song"))
        val active = PokemonInPlay(mon("sv4-137", 150, EnergyType.FIRE, hit).copy(abilities = listOf(abi)))
        val foe = PokemonInPlay(mon("foe", 300, EnergyType.PSYCHIC, hit))
        // Sin Energía Fuego en mano → rechazada.
        assertFalse(GameEngine(SeededRng(1)).apply(
            duel(active, foe), GameIntent.UseAbility(CardId("sv4-137"), "Canción Incendiaria")).accepted)
        // Con Energía Fuego en mano → se descarta y el ataque hace 40 + 60 = 100.
        val st = duel(active, foe).let { it.copy(player = it.player.copy(hand = listOf(energy("fe", EnergyType.FIRE)))) }
        val used = GameEngine(SeededRng(1)).apply(st, GameIntent.UseAbility(CardId("sv4-137"), "Canción Incendiaria"))
        assertTrue(used.accepted, used.rejection)
        assertEquals(0, used.state.player.hand.size)
        assertEquals(1, used.state.player.discard.size)
        val r = GameEngine(SeededRng(1)).apply(used.state, GameIntent.Attack("Hit"))
        assertEquals(100, r.state.opponent.active?.damage)
    }

    @Test
    fun `Suddenly Select busca hasta 3 Herramientas a la mano al evolucionar`() {
        val dummy = attack("x", EffectId("none"))
        val basic = PokemonInPlay(mon("Wugmo", 70, EnergyType.WATER, dummy)).copy(turnsInPlay = 1)
        val evo = mon("sv4-53", 90, EnergyType.WATER, dummy).copy(
            stage = Stage.Stage1, evolvesFrom = "Wugmo",
            abilities = listOf(Ability(
                LocalizedText("Selección Repentina", "Suddenly Select"), LocalizedText("", ""),
                EffectsDb.abiKey("sv4-53", "Suddenly Select"))))
        fun tool(id: String) = TrainerCard(
            id = CardId(id), name = LocalizedText("Herramienta $id", "Tool $id"), set = set(),
            rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
            kind = TrainerKind.Tool(), text = LocalizedText("t", "t"), effect = EffectId("none"))
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.FIRE, dummy))
        val base = duel(basic, foe)
        val st = base.copy(player = base.player.copy(
            hand = listOf(evo),
            deck = listOf(tool("t1"), tool("t2"), tool("t3"), energy("d1", EnergyType.WATER))))
        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.Evolve(CardId("sv4-53"), CardId("Wugmo")))
        assertTrue(paused.accepted, paused.rejection)
        // Elige las 3 Herramientas → van a la mano; el mazo se baraja sin Herramientas.
        val r = GameEngine(SeededRng(1)).apply(
            paused.state, GameIntent.ResolveDecision(listOf(CardId("t1"), CardId("t2"), CardId("t3"))))
        assertEquals(3, r.state.player.hand.count { it is TrainerCard })
        assertFalse(r.state.player.deck.any { it is TrainerCard })
    }

    @Test
    fun `Baryon Beam se puede pagar por 3 Incoloros con el Tanque del Futuro unido`() {
        val atk = Attack(
            LocalizedText("Rayo Barión", "Baryon Beam"),
            List(5) { EnergyType.COLORLESS }, 5, DamageModel.Fixed(150),
            EffectsDb.atkKey("sv4-158", "Baryon Beam"))
        val capsule = TrainerCard(
            id = CardId("sv4-164"),
            name = LocalizedText("Tanque de Energía Potenciadora del Futuro", "Future Booster Energy Capsule"),
            set = set(), rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
            kind = TrainerKind.Tool(), text = LocalizedText("t", "t"), effect = EffectId("none"))
        fun jugulis(tool: Boolean) = PokemonInPlay(
            mon("sv4-158", 220, EnergyType.DARKNESS, atk).copy(name = LocalizedText("Iron Jugulis", "Iron Jugulis")),
            attachedEnergy = (1..3).map { energy("c$it", EnergyType.PSYCHIC) },
            attachedTools = if (tool) listOf(capsule) else emptyList())
        val foe = PokemonInPlay(mon("foe", 200, EnergyType.PSYCHIC, atk))
        val engine = GameEngine(SeededRng(1))
        // Sin el Tanque → 3 Energías no cubren {C}×5 → ataque NO legal.
        assertFalse(engine.legalIntents(duel(jugulis(false), foe))
            .any { it is GameIntent.Attack && it.attackName == "Rayo Barión" })
        // Con el Tanque del Futuro → coste alternativo {C}×3 → ataque legal.
        assertTrue(engine.legalIntents(duel(jugulis(true), foe))
            .any { it is GameIntent.Attack && it.attackName == "Rayo Barión" })
    }

    @Test
    fun `Hyper Blower cambia el Activo rival y descarta a este Pokemon desde la Banca`() {
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Soplador Híper", "Hyper Blower"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-56", "Hyper Blower"))
        val active = PokemonInPlay(mon("myActive", 120, EnergyType.WATER, dummy))
        val bundle = PokemonInPlay(
            mon("sv4-56", 90, EnergyType.WATER, dummy).copy(abilities = listOf(abi)),
            attachedEnergy = listOf(energy("be", EnergyType.WATER)))
        val oppActive = PokemonInPlay(mon("oppA", 120, EnergyType.FIRE, dummy))
        val oppBench = PokemonInPlay(mon("oppB", 100, EnergyType.FIRE, dummy))
        val st = duel(active, oppActive).let {
            it.copy(
                player = it.player.copy(bench = listOf(bundle)),
                opponent = it.opponent.copy(bench = listOf(oppBench)))
        }
        // En el Puesto Activo la habilidad NO es usable (benchOnly). En Banca, sí.
        val r = GameEngine(SeededRng(1)).apply(st, GameIntent.UseAbility(CardId("sv4-56"), "Soplador Híper"))
        assertTrue(r.accepted, r.rejection)
        assertFalse(r.state.player.bench.any { it.card.id == CardId("sv4-56") })   // ya no está en Banca
        assertEquals(2, r.state.player.discard.size)                               // Iron Bundle + 1 Energía
        assertTrue(Side.OPPONENT in r.state.pendingPromotion)                      // el rival elige nuevo Activo
    }

    @Test
    fun `Buddy Pulse pone 2 contadores al cargar el rival solo si hay Plusle en juego`() {
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Pulso Amistoso", "Buddy Pulse"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-61", "Buddy Pulse"))
        val minun = PokemonInPlay(mon("sv4-61", 70, EnergyType.LIGHTNING, dummy).copy(
            name = LocalizedText("Minun", "Minun"), abilities = listOf(abi)))
        val plusle = PokemonInPlay(mon("plusle", 70, EnergyType.LIGHTNING, dummy).copy(
            name = LocalizedText("Plusle", "Plusle")))
        // El jugador (en turno) une 1 Energía de su mano a su Activo.
        fun stateWithOppBench(bench: List<PokemonInPlay>): GameState {
            val mine = PokemonInPlay(mon("mine", 120, EnergyType.WATER, dummy))
            return duel(mine, minun).let {
                it.copy(
                    player = it.player.copy(hand = listOf(energy("we", EnergyType.WATER))),
                    opponent = it.opponent.copy(bench = bench))
            }
        }
        // Con Plusle en la Banca rival → 2 contadores (20) en el Pokémon recién cargado.
        val conPlusle = GameEngine(SeededRng(1)).apply(
            stateWithOppBench(listOf(plusle)), GameIntent.AttachEnergy(CardId("we"), CardId("mine")))
        assertTrue(conPlusle.accepted, conPlusle.rejection)
        assertEquals(20, conPlusle.state.player.active?.damage)
        assertEquals(1, conPlusle.state.player.active?.attachedEnergy?.size)
        // Sin Plusle → sin contadores.
        val sinPlusle = GameEngine(SeededRng(1)).apply(
            stateWithOppBench(emptyList()), GameIntent.AttachEnergy(CardId("we"), CardId("mine")))
        assertTrue(sinPlusle.accepted, sinPlusle.rejection)
        assertEquals(0, sinPlusle.state.player.active?.damage)
    }

    @Test
    fun `Far-Flying Meteor puede cambiar el Minior cargado por el Activo`() {
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Meteoro Lejano", "Far-Flying Meteor"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-99", "Far-Flying Meteor"))
        val oldActive = PokemonInPlay(mon("act", 120, EnergyType.WATER, dummy))
        val minior = PokemonInPlay(mon("sv4-99", 70, EnergyType.PSYCHIC, dummy).copy(
            name = LocalizedText("Minior", "Minior"), abilities = listOf(abi)))
        val foe = PokemonInPlay(mon("foe", 120, EnergyType.FIRE, dummy))
        val st = duel(oldActive, foe).let {
            it.copy(player = it.player.copy(hand = listOf(energy("pe", EnergyType.PSYCHIC)), bench = listOf(minior)))
        }
        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.AttachEnergy(CardId("pe"), CardId("sv4-99")))
        assertTrue(paused.accepted, paused.rejection)
        assertTrue(paused.state.interaction?.decision is PendingDecision.ChooseTargets)
        // Acepta el cambio → Minior al Activo (con su Energía), el anterior a la Banca.
        val swapped = GameEngine(SeededRng(1)).apply(paused.state, GameIntent.ResolveDecision(listOf(CardId("sv4-99"))))
        assertEquals(CardId("sv4-99"), swapped.state.player.active?.card?.id)
        assertTrue(swapped.state.player.bench.any { it.card.id == CardId("act") })
        assertEquals(1, swapped.state.player.active?.attachedEnergy?.size)
        // Declinar (resolver vacío) → el Activo sigue siendo el anterior.
        val declined = GameEngine(SeededRng(1)).apply(paused.state, GameIntent.ResolveDecision(emptyList()))
        assertEquals(CardId("act"), declined.state.player.active?.card?.id)
    }

    @Test
    fun `Gold Coffin busca 1 carta fuera de turno al ser Noqueado por un ataque`() {
        val hit = attackFixed("Hit", 200, EffectId("none"))
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Ataúd Dorado", "Gold Coffin"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-76", "Gold Coffin"))
        val attacker = PokemonInPlay(mon("atk", 200, EnergyType.DARKNESS, hit))
        val cofagrigus = PokemonInPlay(mon("sv4-76", 150, EnergyType.PSYCHIC, dummy).copy(
            name = LocalizedText("Cofagrigus", "Cofagrigus"), abilities = listOf(abi)))
        val oppBench = PokemonInPlay(mon("ob", 100, EnergyType.PSYCHIC, dummy))
        val st = duel(attacker, cofagrigus).let {
            it.copy(opponent = it.opponent.copy(
                bench = listOf(oppBench),
                deck = listOf(energy("tgt", EnergyType.PSYCHIC)) + (1..4).map { k -> energy("x$k", EnergyType.PSYCHIC) }))
        }
        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Hit"))
        assertTrue(paused.accepted, paused.rejection)
        val inter = paused.state.interaction
        assertTrue(inter?.outOfTurn == true)                 // la resuelve el dueño del KO, fuera de turno
        assertEquals(Side.OPPONENT, inter?.side)
        // Cofagrigus aún en juego mientras se decide (la decisión precede al descarte).
        assertEquals(CardId("sv4-76"), paused.state.opponent.active?.card?.id)
        // El rival resuelve: elige 1 carta de su mazo → a su mano; luego se aplican los KO.
        val r = GameEngine(SeededRng(1)).apply(paused.state, GameIntent.ResolveDecision(listOf(CardId("tgt"))))
        assertTrue(r.state.opponent.hand.any { it.id == CardId("tgt") })
        assertTrue(r.state.opponent.discard.any { it.id == CardId("sv4-76") })   // ya Noqueado/descartado
    }

    @Test
    fun `Fillet Memento mueve hasta 2 Energia Agua a la Banca fuera de turno al ser Noqueado`() {
        val hit = attackFixed("Hit", 200, EffectId("none"))
        val dummy = attack("x", EffectId("none"))
        val abi = Ability(LocalizedText("Reliquia de Filete", "Fillet Memento"), LocalizedText("", ""),
            EffectsDb.abiKey("sv4-54", "Fillet Memento"))
        val attacker = PokemonInPlay(mon("atk", 200, EnergyType.LIGHTNING, hit))
        val veluza = PokemonInPlay(
            mon("sv4-54", 120, EnergyType.WATER, dummy).copy(
                name = LocalizedText("Veluza", "Veluza"), abilities = listOf(abi)),
            attachedEnergy = listOf(
                energy("w1", EnergyType.WATER), energy("w2", EnergyType.WATER), energy("f1", EnergyType.FIRE)))
        val benchMon = PokemonInPlay(mon("ob", 100, EnergyType.WATER, dummy))
        val st = duel(attacker, veluza).let {
            it.copy(opponent = it.opponent.copy(
                bench = listOf(benchMon),
                deck = (1..4).map { k -> energy("d$k", EnergyType.WATER) }))
        }
        val paused = GameEngine(SeededRng(1)).apply(st, GameIntent.Attack("Hit"))
        assertTrue(paused.accepted, paused.rejection)
        assertTrue(paused.state.interaction?.outOfTurn == true)
        assertEquals(Side.OPPONENT, paused.state.interaction?.side)
        // El dueño mueve 2 {W} de Veluza (no la {F}) a su Pokémon de Banca; luego Veluza se descarta.
        val r = GameEngine(SeededRng(1)).apply(
            paused.state, GameIntent.ResolveDecision(listOf(CardId("sv4-54"), CardId("ob"))))
        val bench = r.state.opponent.bench.firstOrNull { it.card.id == CardId("ob") }
        assertEquals(2, bench?.attachedEnergy?.size)
        assertTrue(bench!!.attachedEnergy.all { it is BasicEnergy && it.type == EnergyType.WATER })
        assertTrue(r.state.opponent.discard.any { it.id == CardId("sv4-54") })
    }

    @Test
    fun `Slime Mold Colony registra el pasivo que bloquea el reciclaje de Entrenadores rival`() {
        val eff = EffectsDb.registry[EffectsDb.abiKey("sv4-17", "Slime Mold Colony")]
        assertTrue(eff != null)
        assertTrue(eff!!.passives.any { it.mod == ModKind.BLOCK_OPPONENT_TRAINER_RECYCLE })
    }

    // ---------------------------------------------------------------- Partidarios (0.2.19)

    private fun supporter(id: String) = TrainerCard(
        id = CardId(id), name = LocalizedText(id, id), set = set(), rarity = Rarity.COMMON,
        regulationMark = "H", artwork = art(), kind = TrainerKind.Supporter(),
        text = LocalizedText("t", "t"), effect = EffectId(id),
    )

    private fun withHand(st: GameState, vararg cards: com.mineralord.tcg.engine.model.Card) =
        st.copy(player = st.player.copy(hand = cards.toList()))

    @Test
    fun `Norman roba 4 contra un Pokemon ex y 2 si no`() {
        val dummy = attackFixed("Golpe", 10, EffectId("none"))
        val me = PokemonInPlay(mon("me", 100, EnergyType.FIRE, dummy))
        val plain = PokemonInPlay(mon("foe", 100, EnergyType.WATER, dummy))
        val ex = PokemonInPlay(mon("foeEx", 200, EnergyType.WATER, dummy).copy(mechanic = PokemonMechanic.ExLower))
        val n = supporter("sv4-168")
        val vsPlain = GameEngine(FixedRng(true)).apply(withHand(duel(me, plain), n), GameIntent.PlayTrainer(n.id))
        assertEquals(2, vsPlain.state.player.hand.size)
        val vsEx = GameEngine(FixedRng(true)).apply(withHand(duel(me, ex), n), GameIntent.PlayTrainer(n.id))
        assertEquals(4, vsEx.state.player.hand.size)
    }

    @Test
    fun `Shauntal con cara sube un Pokemon de la Banca rival y con cruz cambia el tuyo`() {
        val dummy = attackFixed("Golpe", 10, EffectId("none"))
        val me = PokemonInPlay(mon("me", 100, EnergyType.FIRE, dummy))
        val myBench = PokemonInPlay(mon("myBench", 100, EnergyType.FIRE, dummy))
        val foe = PokemonInPlay(mon("foe", 100, EnergyType.WATER, dummy))
        val foeBench = PokemonInPlay(mon("foeBench", 100, EnergyType.WATER, dummy))
        val sh = supporter("sv4-174")
        val base = duel(me, foe).let { it.copy(
            player = it.player.copy(bench = listOf(myBench), hand = listOf(sh)),
            opponent = it.opponent.copy(bench = listOf(foeBench)),
        ) }

        val heads = GameEngine(FixedRng(true)).let { e ->
            val r = e.apply(base, GameIntent.PlayTrainer(sh.id))
            e.apply(r.state, GameIntent.ResolveDecision(listOf(foeBench.card.id)))
        }
        assertEquals(CardId("foeBench"), heads.state.opponent.active?.card?.id)

        val tails = GameEngine(FixedRng(false)).let { e ->
            val r = e.apply(base, GameIntent.PlayTrainer(sh.id))
            e.apply(r.state, GameIntent.ResolveDecision(listOf(myBench.card.id)))
        }
        assertEquals(CardId("myBench"), tails.state.player.active?.card?.id)
    }

    @Test
    fun `Vitalidad de Sada une 1 Energia a cada Pokemon Pasado y roba 3`() {
        val dummy = attackFixed("Golpe", 10, EffectId("none"))
        val past = PokemonInPlay(mon("past", 100, EnergyType.FIGHTING, dummy).copy(subtypes = listOf("Pasado")))
        val past2 = PokemonInPlay(mon("past2", 100, EnergyType.FIGHTING, dummy).copy(subtypes = listOf("Pasado")))
        val foe = PokemonInPlay(mon("foe", 100, EnergyType.WATER, dummy))
        val sada = supporter("sv4-170")
        val e1 = energy("e1", EnergyType.FIGHTING)
        val e2 = energy("e2", EnergyType.FIGHTING)
        val e3 = energy("e3", EnergyType.FIGHTING)
        val base = duel(past, foe).let { it.copy(player = it.player.copy(
            bench = listOf(past2), hand = listOf(sada), discard = listOf(e1, e2, e3),
        )) }
        val e = GameEngine(FixedRng(true))
        val r = e.apply(base, GameIntent.PlayTrainer(sada.id))
        // Intenta 2 Energías al mismo Pokémon + 1 al otro: solo cuenta 1 por destino.
        val done = e.apply(r.state, GameIntent.ResolveDecision(listOf(e1.id, past.card.id, e2.id, past.card.id, e3.id, past2.card.id)))
        assertEquals(1, done.state.player.active!!.attachedEnergy.size)
        assertEquals(1, done.state.player.bench.single().attachedEnergy.size)
        assertEquals(3, done.state.player.hand.size)
    }
}
