package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.Phase
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Rarity
import com.mineralord.tcg.engine.model.SetInfo
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Stage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Cubre la preparación interactiva: [GameSetup.deal], [GameSetup.autoChoose], [GameSetup.finish]. */
class GameSetupTest {

    private fun art() = ArtworkRefs(null, null, "s", "l")
    private fun set() = SetInfo("t", LocalizedText("T", "T"), "T")

    private fun basic(id: String, hp: Int) = PokemonCard(
        id = CardId(id), name = LocalizedText(id, id), set = set(),
        rarity = Rarity.COMMON, regulationMark = "H", artwork = art(),
        stage = Stage.Basic, mechanic = PokemonMechanic.Normal, hp = hp,
        types = listOf(EnergyType.LIGHTNING), evolvesFrom = null, abilities = emptyList(),
        attacks = listOf(Attack(LocalizedText("Golpe", "Hit"), listOf(EnergyType.LIGHTNING), 1, Damage.Fixed(30), null)),
        weaknesses = emptyList(), resistances = emptyList(), retreatCost = emptyList(), rulesText = emptyList(),
    )

    private fun energy(id: String): Card =
        BasicEnergy(CardId(id), LocalizedText("Energía", "Energy"), set(), Rarity.COMMON, null, art(), EnergyType.LIGHTNING)

    /** Mazo: 6 Básicos (HP variado) + 20 energías. */
    private fun deck(prefix: String): List<Card> =
        (1..6).map { basic("$prefix-p$it", hp = 40 + it * 10) } + (1..20).map { energy("$prefix-e$it") }

    @Test
    fun `deal reparte 7 cartas con al menos un Basico y resto de mazo`() {
        val dealt = GameSetup.deal(deck("A"), SeededRng(42), handSize = 7)
        assertEquals(7, dealt.hand.size)
        assertTrue(GameSetup.basicsIn(dealt).isNotEmpty(), "La mano repartida debe tener al menos un Básico")
        assertEquals(26 - 7, dealt.deck.size)
    }

    @Test
    fun `autoChoose elige el Basico con mas PS como Activo y el resto en Banca`() {
        val dealt = GameSetup.deal(deck("A"), SeededRng(7))
        val choice = GameSetup.autoChoose(dealt)
        val basics = GameSetup.basicsIn(dealt)
        val topHp = basics.maxByOrNull { it.hp }!!
        assertEquals(topHp.id, choice.activeId)
        assertTrue(choice.activeId !in choice.benchIds)
        assertTrue(choice.benchIds.size <= GameEngine.BENCH_LIMIT)
    }

    @Test
    fun `finish coloca Activo y Banca elegidos, reparte 6 premios y arranca en MAIN`() {
        val rng = SeededRng(99)
        val pDealt = GameSetup.deal(deck("A"), rng)
        val oDealt = GameSetup.deal(deck("B"), rng)
        val pBasics = GameSetup.basicsIn(pDealt)
        val active = pBasics.first()
        val bench = pBasics.drop(1).take(2).map { it.id }

        val gs = GameSetup.finish(
            player = GameSetup.SideChoice(pDealt, active.id, bench),
            opponent = GameSetup.autoChoose(oDealt),
        )

        assertEquals(Phase.MAIN, gs.phase)
        assertEquals(Side.PLAYER, gs.activeSide)
        assertNotNull(gs.player.active)
        assertEquals(active.id, gs.player.active!!.card.id)
        assertEquals(bench.size, gs.player.bench.size)
        assertEquals(6, gs.player.prizes.size)
        assertEquals(6, gs.player.prizesRemaining)
        // El Activo y la Banca ya no están en la mano.
        assertNull(gs.player.hand.firstOrNull { it.id == active.id })
        // El motor acepta el estado resultante: hay jugadas legales.
        val engine = GameEngine(rng)
        assertTrue(engine.legalIntents(gs).isNotEmpty())
    }
}
