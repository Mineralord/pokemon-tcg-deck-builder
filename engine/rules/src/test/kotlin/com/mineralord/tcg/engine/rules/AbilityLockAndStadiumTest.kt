package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.Ability
import com.mineralord.tcg.engine.model.AbilityKind
import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Effect
import com.mineralord.tcg.engine.model.EffectId
import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.EffectRegistry
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.ModKind
import com.mineralord.tcg.engine.model.PassiveModifier
import com.mineralord.tcg.engine.model.Phase
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Rarity
import com.mineralord.tcg.engine.model.SetInfo
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Stage
import com.mineralord.tcg.engine.model.Target
import com.mineralord.tcg.engine.model.EffectsDb
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Bloqueo de Habilidades (fiel al TCG): solo se apagan las [AbilityKind.ABILITY]
 * modernas; Poké-Power/Poké-Body y Rasgos Antiguos son inmunes. Fuentes: Estadio
 * (Camino hacia la Cima) y habilidades en juego (Klefki). Además, colocación de
 * Estadios (reemplazo, mismo-nombre prohibido, descarte a su dueño).
 */
class AbilityLockAndStadiumTest {

    private class NoRng : Rng {
        override fun flipCoin() = true
        override fun <T> shuffle(list: List<T>) = list
        override fun nextInt(untilExclusive: Int) = 0
    }

    private fun art() = ArtworkRefs(null, null, "s", "l")
    private fun set() = SetInfo("t", LocalizedText("T", "T"), "T")

    // --- claves de efecto sintéticas ---
    private val kAct = EffectId("act")          // habilidad activable (roba 1)
    private val kLockStadium = EffectId("lockStadium")   // Estadio: bloquea solo caja de regla
    private val kLockAll = EffectId("lockAll")   // habilidad Klefki: bloquea ambos lados

    private fun registry() = EffectRegistry(
        mapOf(
            kAct to Effect(ops = listOf(EffectOp.DrawCards(1))),
            kLockStadium to Effect(passives = listOf(
                PassiveModifier(ModKind.BLOCK_ABILITY, blockBothSides = true, blockOnlyRuleBox = true))),
            kLockAll to Effect(passives = listOf(
                PassiveModifier(ModKind.BLOCK_ABILITY, blockBothSides = true))),
        ),
    )

    private fun poke(
        id: String, mechanic: PokemonMechanic = PokemonMechanic.Normal,
        abilities: List<Ability> = emptyList(),
    ) = PokemonCard(
        id = CardId(id), name = LocalizedText(id, id), set = set(),
        rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
        stage = Stage.Basic, mechanic = mechanic, hp = 100,
        types = listOf(EnergyType.COLORLESS), evolvesFrom = null,
        abilities = abilities, attacks = emptyList(),
        weaknesses = emptyList(), resistances = emptyList(),
        retreatCost = emptyList(), rulesText = emptyList(),
    )

    private fun abi(name: String, effect: EffectId, kind: AbilityKind = AbilityKind.ABILITY) =
        Ability(LocalizedText(name, name), LocalizedText("", ""), effect, kind)

    private fun stadium(id: String, name: String, effect: EffectId?) = TrainerCard(
        id = CardId(id), name = LocalizedText(name, name), set = set(),
        rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
        kind = TrainerKind.Stadium(), text = LocalizedText("", ""),
        effect = effect ?: EffectId(id),
    )

    private fun state(player: PlayerState, opponent: PlayerState, stadiumCard: TrainerCard? = null,
                      stadiumOwner: Side? = null) = GameState(
        player = player, opponent = opponent, turn = 2,
        activeSide = Side.PLAYER, phase = Phase.MAIN,
        stadium = stadiumCard, stadiumOwner = stadiumOwner,
    )

    private fun engine() = GameEngine(NoRng(), registry())

    @Test
    fun `un Estadio que bloquea solo caja de regla apaga la Habilidad de un ex pero no la de un Normal`() {
        val exMon = PokemonInPlay(poke("ex1", PokemonMechanic.ExLower, listOf(abi("Impulso", kAct))))
        val normalMon = PokemonInPlay(poke("n1", PokemonMechanic.Normal, listOf(abi("Impulso2", kAct))))
        val me = PlayerState(Side.PLAYER, active = exMon, bench = listOf(normalMon),
            deck = List(3) { poke("d$it") })
        val foe = PlayerState(Side.OPPONENT, active = PokemonInPlay(poke("f1")))
        val lock = stadium("st1", "Torre Bloqueo", kLockStadium)
        val st = state(me, foe, lock, Side.OPPONENT)

        val blocked = engine().apply(st, GameIntent.UseAbility(CardId("ex1"), "Impulso"))
        assertFalse(blocked.accepted)   // ex tiene caja de regla → bloqueada
        val ok = engine().apply(st, GameIntent.UseAbility(CardId("n1"), "Impulso2"))
        assertTrue(ok.accepted)         // Normal no tiene caja de regla → intacta
    }

    @Test
    fun `Klefki (bloqueo total) apaga una Habilidad rival pero NO un Poke-Body ni su propia fuente`() {
        val klefki = PokemonInPlay(poke("klefki", abilities = listOf(abi("Cierre", kLockAll))))
        val abilityMon = PokemonInPlay(poke("a1", abilities = listOf(abi("Impulso", kAct))))
        val bodyMon = PokemonInPlay(poke("b1", abilities = listOf(abi("Cuerpo", kAct, AbilityKind.POKE_BODY))))
        val me = PlayerState(Side.PLAYER, active = abilityMon, bench = listOf(bodyMon),
            deck = List(3) { poke("d$it") })
        val foe = PlayerState(Side.OPPONENT, active = klefki)
        val st = state(me, foe)

        assertFalse(engine().apply(st, GameIntent.UseAbility(CardId("a1"), "Impulso")).accepted)  // Habilidad bloqueada
        assertTrue(engine().apply(st, GameIntent.UseAbility(CardId("b1"), "Cuerpo")).accepted)    // Poké-Body inmune
    }

    @Test
    fun `jugar un Estadio lo pone en campo, sale de la mano y no va al descarte`() {
        val lock = stadium("st1", "Torre Bloqueo", kLockStadium)
        val me = PlayerState(Side.PLAYER, active = PokemonInPlay(poke("p1")), hand = listOf(lock))
        val foe = PlayerState(Side.OPPONENT, active = PokemonInPlay(poke("f1")))
        val res = engine().apply(state(me, foe), GameIntent.PlayTrainer(CardId("st1")))

        assertTrue(res.accepted)
        assertEquals(CardId("st1"), res.state.stadium?.id)
        assertEquals(Side.PLAYER, res.state.stadiumOwner)
        assertTrue(res.state.player.hand.isEmpty())
        assertTrue(res.state.player.discard.none { it.id == CardId("st1") })
    }

    @Test
    fun `Oleaje Helicoidal del Activo rival impide jugar Estadios`() {
        // End-to-end con el EffectsDb REAL: verifica registro (sv3pt5-153) + hook del motor.
        val realEngine = GameEngine(NoRng())
        val fossilAbi = abi("Helical Swell", EffectsDb.abiKey("sv3pt5-153", "Helical Swell"))
        val lock = stadium("st1", "Torre X", null)
        val me = PlayerState(Side.PLAYER, active = PokemonInPlay(poke("p1")), hand = listOf(lock))

        // Rival con el Fósil Hélix en el Activo → no se puede jugar el Estadio.
        val foeBlock = PlayerState(Side.OPPONENT, active = PokemonInPlay(poke("fossil", abilities = listOf(fossilAbi))))
        assertFalse(realEngine.apply(state(me, foeBlock), GameIntent.PlayTrainer(CardId("st1"))).accepted)

        // Rival sin el fósil → el Estadio se juega con normalidad.
        val foePlain = PlayerState(Side.OPPONENT, active = PokemonInPlay(poke("f1")))
        assertTrue(realEngine.apply(state(me, foePlain), GameIntent.PlayTrainer(CardId("st1"))).accepted)
    }

    @Test
    fun `un Estadio nuevo descarta el anterior a su dueno y el del mismo nombre se rechaza`() {
        val old = stadium("old", "Torre Vieja", null)
        val newSt = stadium("new", "Torre Nueva", null)
        val sameName = stadium("same", "Torre Vieja", null)
        val me = PlayerState(Side.PLAYER, active = PokemonInPlay(poke("p1")), hand = listOf(newSt, sameName))
        val foe = PlayerState(Side.OPPONENT, active = PokemonInPlay(poke("f1")))
        // El Estadio en campo es del rival (OPPONENT).
        val st = state(me, foe, old, Side.OPPONENT)

        val replaced = engine().apply(st, GameIntent.PlayTrainer(CardId("new")))
        assertTrue(replaced.accepted)
        assertEquals(CardId("new"), replaced.state.stadium?.id)
        // El anterior fue al descarte de SU dueño (el rival).
        assertTrue(replaced.state.opponent.discard.any { it.id == CardId("old") })

        val rejected = engine().apply(st, GameIntent.PlayTrainer(CardId("same")))
        assertFalse(rejected.accepted)   // mismo nombre que el que ya está en juego
    }
}
