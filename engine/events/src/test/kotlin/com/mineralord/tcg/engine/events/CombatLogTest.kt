package com.mineralord.tcg.engine.events

import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Status
import kotlin.test.Test
import kotlin.test.assertEquals

class CombatLogTest {

    private val names = mapOf(
        CardId("a") to "Oricorio",
        CardId("b") to "Mega-Kangaskhan ex",
        CardId("c") to "Slowpoke",
        CardId("d") to "Slowking",
        CardId("e") to "Energía Lucha",
    )
    private val log = CombatLog(
        spanish = true,
        cardName = { names[it] ?: it.raw },
        sideName = { if (it == Side.PLAYER) "Kukkii_Bakemono" else "Dakshinamurti" },
    )

    @Test
    fun `dano con debilidad se renderiza como en el Registro del Combate`() {
        val line = log.render(
            GameEvent.DamageDealt(
                side = Side.OPPONENT,
                target = CardId("b"),
                amount = 110,
                weaknessApplied = true,
            ),
        )
        assertEquals("Mega-Kangaskhan ex ha recibido 110 puntos de daño (Debilidad).", line)
    }

    @Test
    fun `evolucion replica la frase observada en el video`() {
        val line = log.render(GameEvent.Evolved(Side.PLAYER, CardId("c"), CardId("d")))
        assertEquals(
            "Kukkii_Bakemono ha hecho que Slowpoke evolucione a Slowking.",
            line,
        )
    }

    @Test
    fun `robar una carta usa singular`() {
        assertEquals(
            "Dakshinamurti ha robado una carta.",
            log.render(GameEvent.CardsDrawn(Side.OPPONENT, 1)),
        )
        assertEquals(
            "Dakshinamurti ha robado 3 cartas.",
            log.render(GameEvent.CardsDrawn(Side.OPPONENT, 3)),
        )
    }

    @Test
    fun `estado envenenado se localiza`() {
        assertEquals(
            "Slowking ahora está Envenenado.",
            log.render(GameEvent.StatusApplied(Side.PLAYER, CardId("d"), Status.POISONED)),
        )
    }
}
