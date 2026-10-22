package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.Amount
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.DamageCondition
import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.EffectsDb
import com.mineralord.tcg.engine.model.Status
import com.mineralord.tcg.engine.model.PokemonCard
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Auditoría de cobertura de EFECTOS de **Brecha Paradójica** (`sv4`). No prueba una carta concreta:
 * recorre TODAS las cartas reales del dataset y cruza cada ataque con el registro de efectos (el
 * mapper deja `Attack.effect = null` si no hay efecto registrado para ese id+nombre).
 *
 * Un ataque de daño **Variable** (p. ej. "10×", "80+") SIN efecto registrado haría **0 de daño** en
 * combate: es un bug. Este test exige que no haya ninguno (la lista de huecos sale en el mensaje).
 */
class ParadoxAttackCoverageTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val repo = CardRepository.load()
    private val sv4 = repo.all.filterIsInstance<PokemonCard>().filter { it.id.raw.startsWith("sv4-") }

    // DTOs CRUDOS de sv4 (conservan el string de daño "30+"/"10×" y el texto), para auditoría semántica.
    private val sv4Dtos: List<CardDto> = run {
        val body = javaClass.getResourceAsStream("/cards/scarlet-violet/paradox-rift.json")!!
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        json.decodeFromString(CardDbDto.serializer(), body).cartas
    }

    /** Número base impreso (prefijo entero de "30+", "10×", "80"); null si no hay número. */
    private fun printedBase(dmg: String?): Int? =
        dmg?.trim()?.let { Regex("^(\\d+)").find(it)?.groupValues?.get(1)?.toInt() }

    @Test
    fun `todos los ataques de dano VARIABLE de sv4 tienen efecto registrado`() {
        val gaps = sv4.flatMap { card ->
            card.attacks.filter { it.baseDamage == Damage.Variable && it.effect == null }
                .map { "${card.id.raw} · ${it.name.en}" }
        }
        assertTrue(
            gaps.isEmpty(),
            "Ataques de daño VARIABLE sin efecto (harían 0 de daño) — ${gaps.size}:\n" + gaps.joinToString("\n"),
        )
    }

    @Test
    fun `las unicas habilidades sin efecto son las NO soportadas por el DSL`() {
        val gaps = sv4.flatMap { card ->
            card.abilities.filter { it.effect == null }.map { "${card.id.raw} · ${it.name.en}" }
        }.toSet()
        // Habilidades que requieren mecánicas aún inexistentes (triggers reactivos "al subir al Activo"/
        // "al ser Noqueado con búsqueda", conteos por aliado con nombre, manual con "si lo haces…",
        // prevención/restricción muy específicas…). Allowlist: si una habilidad NUEVA queda sin cubrir
        // o se implementa una de estas, el test falla para revisar la cobertura.
        val unsupported = setOf(
            "sv4-28 · Thermal Reactor", "sv4-187 · Thermal Reactor",
            "sv4-36 · Lifeboat",
            "sv4-120 · Rob-'n'-Run",
            "sv4-126 · Stellar Veil",
        )
        assertEquals(
            unsupported, gaps,
            "Cambió el conjunto de habilidades sin implementar.\nNuevas sin cubrir: ${gaps - unsupported}\n" +
                "Ya cubiertas (quitar del allowlist): ${unsupported - gaps}",
        )
    }

    @Test
    fun `los unicos riders sin efecto son los NO soportados por el DSL`() {
        val gaps = sv4.flatMap { card ->
            card.attacks.filter { it.effect == null && it.text.en.isNotBlank() }
                .map { "${card.id.raw} · ${it.name.en}" }
        }.distinct().toSet()
        // Riders que requieren mecánicas aún inexistentes en el motor y por eso NO se registran:
        //  · conteos dinámicos (Burst of Braying, Pep Up)
        //  · coin-until-tails que mueve Energía al mazo (Daunting Eyes)
        //  · reveal + snipe condicional (Whirltide)
        //  · prevención/control muy específicos (Arm Thrust Needle, Echoing Madness, Data Displacement)
        //  · mirar/robar top (Mountain Scrounging)
        //  · coste alternativo por Herramienta, no es un efecto de daño (Baryon Beam)
        // Documentados aquí: si aparece un rider NUEVO sin implementar (o se implementa uno de estos),
        // el test falla para obligar a revisar la cobertura. Drawareness pasó a estar cubierto
        // (descarte mano→descarte + DrawUntil 5).
        val unsupported = setOf(
            "sv4-32 · Whirltide",
        )
        assertEquals(
            unsupported, gaps,
            "Cambió el conjunto de riders sin implementar.\nNuevos sin cubrir: ${gaps - unsupported}\n" +
                "Ya cubiertos (quitar del allowlist): ${unsupported - gaps}",
        )
    }

    @Test
    fun `AUDITORIA SEMANTICA - el dano base de los ataques X+ coincide con la carta`() {
        // Para cada ataque "X+" registrado con attackDamage, la SUMA de los términos incondicionales
        // (DamageCondition.Always) debe igualar el daño base IMPRESO en la carta. Cruza los números
        // reales (DTO) contra el registro: caza transcripciones erróneas (p. ej. poner 40 donde va 30).
        val mismatches = mutableListOf<String>()
        for (card in sv4Dtos.filter { it.id.startsWith("sv4-") }) {
            for (atk in card.ataques) {
                val eff = EffectsDb.registry[EffectsDb.atkKey(card.id, atk.name)] ?: continue
                if (eff.attackDamage.isEmpty()) continue
                val base = printedBase(atk.damage) ?: continue
                val alwaysSum = eff.attackDamage.filter { it.condition == DamageCondition.Always }.sumOf { it.amount }
                if (alwaysSum != base) {
                    mismatches += "${card.id} · ${atk.name}: impreso=$base, Always=$alwaysSum (dmg=\"${atk.damage}\")"
                }
            }
        }
        assertTrue(mismatches.isEmpty(), "Daño base registrado ≠ carta — ${mismatches.size}:\n" + mismatches.joinToString("\n"))
    }

    /** "Flip 3 coins" → 3 · "Flip a coin" → 1 · null si no es un lanzamiento simple de N monedas. */
    private fun flipsFromText(t: String): Int? {
        Regex("Flip (\\d+) coins", RegexOption.IGNORE_CASE).find(t)?.let { return it.groupValues[1].toInt() }
        // "Flip a coin until you get tails" NO es de N monedas fijas (lo cubre CoinUntilTailsDamage).
        if (t.contains("Flip a coin", true) && !t.contains("until you get tails", true)) return 1
        return null
    }

    @Test
    fun `AUDITORIA SEMANTICA - el numero de monedas de CoinFlipDamage coincide con la carta`() {
        val mismatches = mutableListOf<String>()
        for (card in sv4Dtos.filter { it.id.startsWith("sv4-") }) {
            for (atk in card.ataques) {
                val eff = EffectsDb.registry[EffectsDb.atkKey(card.id, atk.name)] ?: continue
                val coinOps = eff.ops.filterIsInstance<EffectOp.CoinFlipDamage>()
                if (coinOps.isEmpty()) continue
                val expected = flipsFromText(atk.text ?: "") ?: continue
                val got = coinOps.sumOf { it.flips }
                if (got != expected) {
                    mismatches += "${card.id} · ${atk.name}: texto=$expected monedas, registrado=$got"
                }
            }
        }
        assertTrue(mismatches.isEmpty(), "Nº de monedas registrado ≠ carta — ${mismatches.size}:\n" + mismatches.joinToString("\n"))
    }

    @Test
    fun `AUDITORIA SEMANTICA - el retroceso (Recoil) coincide con la carta`() {
        val mism = mutableListOf<String>()
        for (card in sv4Dtos.filter { it.id.startsWith("sv4-") }) {
            for (atk in card.ataques) {
                val eff = EffectsDb.registry[EffectsDb.atkKey(card.id, atk.name)] ?: continue
                val recoils = eff.ops.filterIsInstance<EffectOp.Recoil>().mapNotNull { (it.amount as? Amount.Fixed)?.n }
                if (recoils.isEmpty()) continue
                val m = Regex("does (\\d+) damage to itself", RegexOption.IGNORE_CASE).find(atk.text ?: "") ?: continue
                val expected = m.groupValues[1].toInt()
                val got = recoils.sum()
                if (got != expected) mism += "${card.id} · ${atk.name}: texto=$expected, registrado=$got"
            }
        }
        assertTrue(mism.isEmpty(), "Retroceso registrado ≠ carta — ${mism.size}:\n" + mism.joinToString("\n"))
    }

    @Test
    fun `AUDITORIA SEMANTICA - el multiplicador X por cada coincide con la carta`() {
        val mism = mutableListOf<String>()
        for (card in sv4Dtos.filter { it.id.startsWith("sv4-") }) {
            for (atk in card.ataques) {
                val eff = EffectsDb.registry[EffectsDb.atkKey(card.id, atk.name)] ?: continue
                val muls = eff.ops.filterIsInstance<EffectOp.ExtraDamage>().mapNotNull { (it.amount as? Amount.PerCount)?.mult }
                if (muls.isEmpty()) continue
                val m = Regex("(\\d+)\\s+(?:more\\s+)?damage for each", RegexOption.IGNORE_CASE).find(atk.text ?: "") ?: continue
                val expected = m.groupValues[1].toInt()
                if (expected !in muls) mism += "${card.id} · ${atk.name}: texto=$expected por cada, registrado=$muls"
            }
        }
        assertTrue(mism.isEmpty(), "Multiplicador ×N registrado ≠ carta — ${mism.size}:\n" + mism.joinToString("\n"))
    }

    @Test
    fun `AUDITORIA SEMANTICA - las condiciones especiales aplicadas aparecen en el texto`() {
        val kw = mapOf(
            Status.BURNED to "Burned", Status.ASLEEP to "Asleep", Status.POISONED to "Poisoned",
            Status.CONFUSED to "Confused", Status.PARALYZED to "Paralyzed",
        )
        val mism = mutableListOf<String>()
        for (card in sv4Dtos.filter { it.id.startsWith("sv4-") }) {
            for (atk in card.ataques) {
                val eff = EffectsDb.registry[EffectsDb.atkKey(card.id, atk.name)] ?: continue
                val statuses = eff.ops.flatMap {
                    when (it) {
                        is EffectOp.ApplyStatus -> it.states
                        is EffectOp.CoinFlipStatus -> it.states
                        else -> emptyList()
                    }
                }.distinct()
                if (statuses.isEmpty()) continue
                val text = atk.text ?: ""
                for (s in statuses) {
                    val word = kw[s] ?: continue
                    if (!text.contains(word, true)) {
                        mism += "${card.id} · ${atk.name}: aplica $s pero el texto no menciona '$word'"
                    }
                }
            }
        }
        assertTrue(mism.isEmpty(), "Condición especial registrada ausente en el texto — ${mism.size}:\n" + mism.joinToString("\n"))
    }

    /** Recorre (carta, ataque, efecto registrado, texto EN) de sv4 con efecto no nulo. */
    private fun forEachRegistered(block: (CardDto, AttackDto, com.mineralord.tcg.engine.model.Effect, String) -> Unit) {
        for (card in sv4Dtos.filter { it.id.startsWith("sv4-") }) {
            for (atk in card.ataques) {
                val eff = EffectsDb.registry[EffectsDb.atkKey(card.id, atk.name)] ?: continue
                block(card, atk, eff, atk.text ?: "")
            }
        }
    }

    @Test
    fun `AUDITORIA SEMANTICA - la curacion coincide con la carta`() {
        val mism = mutableListOf<String>()
        forEachRegistered { card, atk, eff, text ->
            val heals = eff.ops.filterIsInstance<EffectOp.Heal>().mapNotNull { (it.amount as? Amount.Fixed)?.n }
            if (heals.isEmpty()) return@forEachRegistered
            val m = Regex("[Hh]eal (\\d+) damage").find(text) ?: return@forEachRegistered
            val expected = m.groupValues[1].toInt()
            if (expected !in heals) mism += "${card.id} · ${atk.name}: texto cura $expected, registrado=$heals"
        }
        assertTrue(mism.isEmpty(), "Curación registrada ≠ carta — ${mism.size}:\n" + mism.joinToString("\n"))
    }

    @Test
    fun `AUDITORIA SEMANTICA - el robo de cartas coincide con la carta`() {
        val mism = mutableListOf<String>()
        forEachRegistered { card, atk, eff, text ->
            val draws = eff.ops.filterIsInstance<EffectOp.DrawCards>().map { it.count }
            if (draws.isEmpty()) return@forEachRegistered
            val expected = Regex("[Dd]raw (\\d+) card").find(text)?.groupValues?.get(1)?.toInt()
                ?: if (text.contains("draw a card", true) || text.contains("Draw a card")) 1 else null
                ?: return@forEachRegistered
            if (expected !in draws) mism += "${card.id} · ${atk.name}: texto roba $expected, registrado=$draws"
        }
        assertTrue(mism.isEmpty(), "Robo registrado ≠ carta — ${mism.size}:\n" + mism.joinToString("\n"))
    }

    @Test
    fun `AUDITORIA SEMANTICA - el descarte de Energia propia coincide con la carta`() {
        val mism = mutableListOf<String>()
        forEachRegistered { card, atk, eff, text ->
            // Solo descartes con conteo FIJO (no "all"); evita ambigüedad con "discard all Energy".
            val discards = eff.ops.filterIsInstance<EffectOp.DiscardEnergy>().map { it.count }
            if (discards.isEmpty() || text.contains("discard all", true)) return@forEachRegistered
            val expected = Regex("[Dd]iscard (\\d+) .{0,40}?[Ee]nergy").find(text)?.groupValues?.get(1)?.toInt()
                ?: if (Regex("[Dd]iscard an? .{0,40}?[Ee]nergy").containsMatchIn(text)) 1 else null
                ?: return@forEachRegistered
            if (expected !in discards) mism += "${card.id} · ${atk.name}: texto descarta $expected Energía, registrado=$discards"
        }
        assertTrue(mism.isEmpty(), "Descarte de Energía registrado ≠ carta — ${mism.size}:\n" + mism.joinToString("\n"))
    }
}
