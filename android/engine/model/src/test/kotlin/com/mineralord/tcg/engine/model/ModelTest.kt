package com.mineralord.tcg.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelTest {

    private fun art() = ArtworkRefs(null, null, "s.png", "l.png")
    private fun set() = SetInfo("sv3pt5", LocalizedText("151", "151"), "Scarlet & Violet")

    private fun pikachuEx() = PokemonCard(
        id = CardId("svp-106"),
        name = LocalizedText("Pikachu ex", "Pikachu ex"),
        set = set(),
        rarity = Rarity.DOUBLE_RARE,
        regulationMark = "H",
        artwork = art(),
        stage = Stage.Basic,
        mechanic = PokemonMechanic.ExLower,
        hp = 200,
        types = listOf(EnergyType.LIGHTNING),
        evolvesFrom = null,
        abilities = emptyList(),
        attacks = listOf(
            Attack(
                name = LocalizedText("Rayo", "Thunderbolt"),
                cost = listOf(EnergyType.LIGHTNING, EnergyType.LIGHTNING, EnergyType.COLORLESS),
                convertedCost = 3,
                baseDamage = Damage.Fixed(120),
                effect = null,
            ),
        ),
        weaknesses = listOf(TypeModifier(EnergyType.FIGHTING, "×2")),
        resistances = emptyList(),
        retreatCost = listOf(EnergyType.COLORLESS),
        rulesText = emptyList(),
    )

    @Test
    fun `ex entrega 2 premios`() {
        assertEquals(2, pikachuEx().prizeValue)
        assertEquals(3, PokemonMechanic.VMax.prizesWhenKO)
        assertEquals(1, PokemonMechanic.Normal.prizesWhenKO)
    }

    @Test
    fun `HP restante y KO se calculan desde el dano acumulado`() {
        val inPlay = PokemonInPlay(card = pikachuEx(), damage = 120)
        assertEquals(80, inPlay.remainingHp)
        assertFalse(inPlay.isKnockedOut)

        val dead = inPlay.copy(damage = 200)
        assertEquals(0, dead.remainingHp)
        assertTrue(dead.isKnockedOut)
    }

    @Test
    fun `un Basico se identifica como tal`() {
        assertTrue(pikachuEx().isBasic)
        assertEquals(Supertype.POKEMON, pikachuEx().supertype)
    }

    @Test
    fun `el estado de juego resuelve el lado activo`() {
        val p = PlayerState(side = Side.PLAYER, active = PokemonInPlay(pikachuEx()))
        val o = PlayerState(side = Side.OPPONENT, active = PokemonInPlay(pikachuEx()))
        val gs = GameState(
            player = p, opponent = o, turn = 1,
            activeSide = Side.PLAYER, phase = Phase.MAIN,
        )
        assertEquals(Side.PLAYER, gs.activePlayer.side)
        assertFalse(gs.isOver)
    }
}
