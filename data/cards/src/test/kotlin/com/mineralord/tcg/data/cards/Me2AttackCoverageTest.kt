package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.Amount
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.DamageCondition
import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.EffectsDb
import com.mineralord.tcg.engine.model.PokemonCard
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cobertura + auditoría SEMÁNTICA de efectos de **Llamaradas Fantasmales** (`me2`). Misma metodología
 * que [ParadoxAttackCoverageTest]: ningún ataque de daño variable debe quedar en 0 por falta de efecto
 * (salvo los documentados como aún no soportados), y los valores registrados (daño base `X+`, nº de
 * monedas, multiplicador `×N`) deben coincidir con el texto real de la carta.
 */
class Me2AttackCoverageTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val repo = CardRepository.load()
    private val me2 = repo.all.filterIsInstance<PokemonCard>().filter { it.id.raw.startsWith("PFL-LA-") }

    private val me2Dtos: List<CardDto> = run {
        val body = javaClass.getResourceAsStream("/cards/mega-evolution/llamaradas-fantasmales.json")!!
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        json.decodeFromString(CardDbDto.serializer(), body).cartas
    }

    private fun printedBase(dmg: String?): Int? =
        dmg?.trim()?.let { Regex("^(\\d+)").find(it)?.groupValues?.get(1)?.toInt() }

    private fun forEachRegistered(block: (CardDto, AttackDto, com.mineralord.tcg.engine.model.Effect, String) -> Unit) {
        for (card in me2Dtos.filter { it.id.startsWith("PFL-LA-") }) {
            for (atk in card.ataques) {
                val eff = EffectsDb.registry[EffectsDb.atkKey(card.id, atk.name)] ?: continue
                block(card, atk, eff, atk.text ?: "")
            }
        }
    }

    @Test
    fun `los unicos ataques de dano variable sin efecto son los NO soportados por el DSL`() {
        val gaps = me2.flatMap { card ->
            card.attacks.filter { it.baseDamage == Damage.Variable && it.effect == null }
                .map { "${card.id.raw} · ${it.name.en}" }
        }.toSet()
        // Requieren mecánicas aún inexistentes (contar Pokémon en juego / Tools / por nombre con daño,
        // "si subió de la Banca este turno", "si el rival tiene ≤N Premios", "≥4 Energía de tipo en
        // juego", daño igual al recibido, descarte de Energía de CUALQUIER Pokémon propio).
        val unsupported = setOf(
            "PFL-LA-4 · Juggernaut Horn", "PFL-LA-108 · Juggernaut Horn",
            "PFL-LA-13 · Inferno X", "PFL-LA-109 · Inferno X", "PFL-LA-125 · Inferno X", "PFL-LA-130 · Inferno X",
            "PFL-LA-26 · Crystal Fall",
            "PFL-LA-44 · Sweet Circle",
            "PFL-LA-45 · Limit Break", "PFL-LA-100 · Limit Break",
            "PFL-LA-48 · Raging Charge",
            "PFL-LA-59 · Cocky Claw",
            "PFL-LA-72 · Tool Drop",
            "PFL-LA-77 · Round", "PFL-LA-105 · Round",
            "PFL-LA-84 · Gale Thrust", "PFL-LA-115 · Gale Thrust", "PFL-LA-128 · Gale Thrust",
        )
        assertEquals(
            unsupported, gaps,
            "Cambió el conjunto de ataques variables sin cubrir.\nNuevos sin cubrir: ${gaps - unsupported}\n" +
                "Ya cubiertos (quitar del allowlist): ${unsupported - gaps}",
        )
    }

    @Test
    fun `AUDITORIA SEMANTICA - el dano base de los ataques X+ coincide con la carta`() {
        val mism = mutableListOf<String>()
        forEachRegistered { card, atk, eff, _ ->
            if (eff.attackDamage.isEmpty()) return@forEachRegistered
            val base = printedBase(atk.damage) ?: return@forEachRegistered
            val alwaysSum = eff.attackDamage.filter { it.condition == DamageCondition.Always }.sumOf { it.amount }
            if (alwaysSum != base) mism += "${card.id} · ${atk.name}: impreso=$base, Always=$alwaysSum (dmg=\"${atk.damage}\")"
        }
        assertTrue(mism.isEmpty(), "Daño base registrado ≠ carta — ${mism.size}:\n" + mism.joinToString("\n"))
    }

    @Test
    fun `AUDITORIA SEMANTICA - el numero de monedas coincide con la carta`() {
        val mism = mutableListOf<String>()
        forEachRegistered { card, atk, eff, text ->
            val coins = eff.ops.filterIsInstance<EffectOp.CoinFlipDamage>()
            if (coins.isEmpty()) return@forEachRegistered
            val expected = Regex("Flip (\\d+) coins", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toInt()
                ?: if (text.contains("Flip a coin", true) && !text.contains("until you get tails", true)) 1 else null
                ?: return@forEachRegistered
            val got = coins.sumOf { it.flips }
            if (got != expected) mism += "${card.id} · ${atk.name}: texto=$expected monedas, registrado=$got"
        }
        assertTrue(mism.isEmpty(), "Nº de monedas registrado ≠ carta — ${mism.size}:\n" + mism.joinToString("\n"))
    }

    @Test
    fun `AUDITORIA SEMANTICA - el multiplicador X por cada coincide con la carta`() {
        val mism = mutableListOf<String>()
        forEachRegistered { card, atk, eff, text ->
            val muls = eff.ops.filterIsInstance<EffectOp.ExtraDamage>().mapNotNull { (it.amount as? Amount.PerCount)?.mult }
            if (muls.isEmpty()) return@forEachRegistered
            val m = Regex("(\\d+)\\s+(?:more\\s+)?damage for each", RegexOption.IGNORE_CASE).find(text) ?: return@forEachRegistered
            val expected = m.groupValues[1].toInt()
            if (expected !in muls) mism += "${card.id} · ${atk.name}: texto=$expected por cada, registrado=$muls"
        }
        assertTrue(mism.isEmpty(), "Multiplicador ×N registrado ≠ carta — ${mism.size}:\n" + mism.joinToString("\n"))
    }
}
