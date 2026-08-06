package com.mineralord.tcg.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EffectsDbTest {

    private val registry = EffectsDb.registry

    @Test
    fun `el registro no esta vacio`() {
        assertTrue(registry.size > 0, "El EffectRegistry debería poblarse desde el kit")
    }

    @Test
    fun `claves de ataque y habilidad presentes`() {
        // Ataque con efecto (Venusaur ex — Dangerous Toxwhip).
        val toxwhip = registry[EffectsDb.atkKey("sv3pt5-3", "Dangerous Toxwhip")]
        assertNotNull(toxwhip)
        assertTrue(toxwhip.ops.any { it is EffectOp.ApplyStatus })

        // Habilidad reutilizada en varios artes (Solid Shell → pasivo -30).
        val solid = registry[EffectsDb.abiKey("sv3pt5-9", "Solid Shell")]
        assertNotNull(solid)
        assertTrue(solid.passives.any { it.mod == ModKind.REDUCE_DAMAGE && it.amount == 30 })
    }

    @Test
    fun `daño extra por conteo se mapea con PerCount`() {
        val mindJack = registry[EffectsDb.atkKey("sv3pt5-65", "Mind Jack")]
        assertNotNull(mindJack)
        val extra = mindJack.ops.filterIsInstance<EffectOp.ExtraDamage>().first()
        val amt = extra.amount
        assertTrue(amt is Amount.PerCount && amt.of == Counter.BENCH_COUNT && amt.mult == 30)
    }

    @Test
    fun `entrenador con clave a nivel de carta`() {
        val nemona = registry[EffectId("sv1-180")]
        assertNotNull(nemona)
        assertTrue(nemona.ops.any { it is EffectOp.DrawCards })
    }

    @Test
    fun `entrenadores de la baraja Pikachu ex registrados`() {
        // Switch (intercambio), Youngster (barajar mano + robar 5), Picnicker (moneda→robar).
        assertTrue(registry.has(EffectId("sv1-194")))
        assertTrue(registry.has(EffectId("sv1-198")))
        assertTrue(registry.has(EffectId("svp-114")))
        val youngster = registry[EffectId("sv1-198")]
        assertNotNull(youngster)
        assertTrue(youngster.ops.any { it is EffectOp.ShuffleHandIntoDeck })
    }

    @Test
    fun `Cambio registrado en sus dos printings`() {
        // Switch/Cambio es la misma carta: sv1-194 (SV base) y sv3pt5-206 (set 151).
        assertTrue(registry.has(EffectId("sv1-194")))
        assertTrue(registry.has(EffectId("sv3pt5-206")))
    }

    @Test
    fun `Generador Electrico registrado`() {
        // Electric Generator (sv1-170): mira top 5 + une hasta 2 Energía Rayo a la Banca.
        assertTrue(registry.has(EffectId("sv1-170")))
    }

    @Test
    fun `Kilowattrel Jet Wing registrado con NoAttackNextTurn`() {
        val jetWing = registry[EffectsDb.atkKey("sv2-82", "Jet Wing")]
        assertNotNull(jetWing)
        assertTrue(jetWing.ops.any { it is EffectOp.NoAttackNextTurn })
    }

    @Test
    fun `ataques de la baraja Armarouge ex registrados`() {
        // Armor Cannon / Fire Blast: descartan energía propia.
        assertTrue(registry[EffectsDb.atkKey("svp-105", "Armor Cannon")]!!.ops.any { it is EffectOp.DiscardEnergy })
        assertTrue(registry[EffectsDb.atkKey("sv1-34", "Fire Blast")]!!.ops.any { it is EffectOp.DiscardEnergy })
        // Take Down / Blazing Shout: recoil a sí mismo.
        assertTrue(registry[EffectsDb.atkKey("sv3-40", "Take Down")]!!.ops.any { it is EffectOp.Recoil })
        assertTrue(registry[EffectsDb.atkKey("sv1-38", "Blazing Shout")]!!.ops.any { it is EffectOp.Recoil })
        // Flame Cloak / Passionate Singing: unir energía del descarte.
        assertTrue(registry[EffectsDb.atkKey("sv3-41", "Flame Cloak")]!!.ops.any { it is EffectOp.AttachEnergyFromDiscard })
        assertTrue(registry[EffectsDb.atkKey("sv1-38", "Passionate Singing")]!!.ops.any { it is EffectOp.AttachEnergyFromDiscard })
        // Concentrated Fire: daño escalable por moneda.
        assertTrue(registry[EffectsDb.atkKey("sv1-35", "Concentrated Fire")]!!.ops.any { it is EffectOp.CoinsPerEnergyDamage })
        // Mela: Partidario condicional (requiere KO en el turno rival).
        val mela = registry[EffectId("sv4-167")]
        assertNotNull(mela)
        assertTrue(mela.requiresOwnKoLastTurn)
        assertTrue(mela.ops.any { it is EffectOp.AttachEnergyFromDiscard })
    }

    @Test
    fun `cartas de la baraja Darkrai ex registradas`() {
        // Cross-Cut (Seviper/Yveltal): daño autorado con término condicional.
        val sev = registry[EffectsDb.atkKey("sv2-137", "Cross-Cut")]!!
        assertEquals(listOf(50, 50), sev.attackDamage.map { it.amount })
        assertTrue(sev.attackDamage.any { it.condition == DamageCondition.IfDefenderEvolved })
        val yv = registry[EffectsDb.atkKey("sv4-118", "Cross-Cut")]!!
        assertEquals(listOf(30, 60), yv.attackDamage.map { it.amount })
        // Dark Edge / Touring: ops existentes.
        assertTrue(registry[EffectsDb.atkKey("sv4-118", "Dark Edge")]!!.ops.any { it is EffectOp.DiscardEnergy })
        assertTrue(registry[EffectsDb.atkKey("sv1-164", "Touring")]!!.ops.any { it is EffectOp.DrawCards })
        // Órdenes de Jefe: gust del Activo rival.
        assertTrue(registry[EffectId("sv2-172")]!!.ops.any { it is EffectOp.SwapOppActiveWithChosen })
    }

    @Test
    fun `cartas aun no modeladas NO estan registradas`() {
        // Butterfree — Adiós, Vuelo (sv3pt5-12, "Bye-Bye Flight") YA está modelada: baraja un
        // Pokémon de la Banca rival y este Pokémon en sus mazos (EffectOp.ByeByeFlightBounce).
        assertTrue(registry[EffectsDb.atkKey("sv3pt5-12", "Bye-Bye Flight")]!!.ops.any { it is EffectOp.ByeByeFlightBounce })
        // Camino de Bicis (Estadio sv3pt5-157) YA está modelado: acción descartar-Energía→robar.
        assertTrue(registry[EffectId("sv3pt5-157")]!!.stadiumDiscardEnergyDraw)
        // Ámbar Viejo Antiguo — Amber Protection (sv3pt5-154#abi) YA está modelada: la Habilidad del
        // Fósil marca al portador inmune a los EFECTOS de las Habilidades rivales (último efecto único
        // del set → 198/198). Lo aplica GameEngine.useAbility filtrando los ops dirigidos al Activo rival.
        assertTrue(registry[EffectsDb.abiKey("sv3pt5-154", "Amber Protection")]!!.immuneToOpponentAbilityEffects)
    }

    /**
     * Naturaleza de una Habilidad tal y como la clasifica el motor:
     *  - AUTO: se dispara sola (al evolucionar / al ser dañada o KO / al cogerla de Premios).
     *  - MANUAL: tiene ops que se ejecutan bajo demanda (GameEngine.useAbility / botón "USAR HABILIDAD").
     *  - PASIVA: sin ops (solo `passives`/flags); nunca se "usa".
     * Es EXACTAMENTE el criterio de GameEngine.legalIntents y del guard de useAbility.
     */
    private enum class AbilityNature { AUTO, MANUAL, PASSIVE }

    private fun natureOf(e: Effect): AbilityNature = when {
        e.triggerOnEvolve || e.triggerOnActiveDamaged || e.triggerOnActiveKO || e.luckyBonusOnPrized -> AbilityNature.AUTO
        e.ops.isNotEmpty() -> AbilityNature.MANUAL
        else -> AbilityNature.PASSIVE
    }

    @Test
    fun `cada Habilidad del set 151 tiene la naturaleza correcta (manual vs pasiva vs auto)`() {
        // Tabla exhaustiva: TODAS las impresiones de Habilidad del set 151 y su naturaleza esperada.
        // Si una Habilidad nueva se registra o cambia de naturaleza, este test debe actualizarse a la vez.
        val manual = listOf(
            "sv3pt5-3" to "Tranquil Flower", "sv3pt5-182" to "Tranquil Flower", "sv3pt5-198" to "Tranquil Flower",
            "sv3pt5-85" to "Zooming Draw",
            "sv3pt5-121" to "Mysterious Comet",
            "sv3pt5-151" to "Restart", "sv3pt5-193" to "Restart", "sv3pt5-205" to "Restart",
            "sv3pt5-53" to "Rocket Call",
            "sv3pt5-41" to "Revealing Echo",
            "sv3pt5-143" to "Voraciousness",
            "sv3pt5-132" to "Transformative Start",
        )
        val auto = listOf(
            "sv3pt5-107" to "Counterattack",
            "sv3pt5-110" to "Let's Have a Blast",
            "sv3pt5-93" to "Spirit Return",
            "sv3pt5-130" to "Untamed One",
            "sv3pt5-97" to "Here for Hypnosis",
            "sv3pt5-44" to "Semi-Blooming Energy",
            "sv3pt5-45" to "Fully Blooming Energy",
            "sv3pt5-113" to "Lucky Bonus",
        )
        val passive = listOf(
            "sv3pt5-9" to "Solid Shell", "sv3pt5-184" to "Solid Shell", "sv3pt5-200" to "Solid Shell",
            "sv3pt5-144" to "Ice Float",
            "sv3pt5-145" to "Voltaic Float", "sv3pt5-192" to "Voltaic Float", "sv3pt5-202" to "Voltaic Float",
            "sv3pt5-146" to "Flare Float",
            "sv3pt5-149" to "Jet Cruise",
            "sv3pt5-139" to "Primordial Tentacles",
            "sv3pt5-40" to "Expanding Body", "sv3pt5-187" to "Expanding Body",
            "sv3pt5-152" to "Domed Armor",
            "sv3pt5-153" to "Helical Swell",
            "sv3pt5-27" to "Sand Screen",
            "sv3pt5-154" to "Amber Protection",
            "sv3pt5-122" to "Mimic Barrier", "sv3pt5-179" to "Mimic Barrier",
            "sv3pt5-34" to "Enthusiastic King", "sv3pt5-174" to "Enthusiastic King",
            "sv3pt5-68" to "Guts",
            "sv3pt5-21" to "Evolutionary Advantage",
            "sv3pt5-14" to "Cocoon Cover",
            "sv3pt5-141" to "Ancient Way",
            "sv3pt5-104" to "Cheering Bone",
            "sv3pt5-26" to "Electrical Grounding",
        )

        val expected = manual.associateWith { AbilityNature.MANUAL } +
            auto.associateWith { AbilityNature.AUTO } +
            passive.associateWith { AbilityNature.PASSIVE }

        // 1) Cada Habilidad registrada tiene la naturaleza esperada.
        for ((card, name) in expected.keys) {
            val eff = registry[EffectsDb.abiKey(card, name)]
            assertNotNull(eff, "Falta registrar la Habilidad $name ($card)")
            assertEquals(expected[card to name], natureOf(eff),
                "Naturaleza incorrecta para $name ($card)")
        }

        // 2) La tabla cubre las 46 impresiones de Habilidad del set (ninguna sin clasificar).
        assertEquals(46, expected.size, "La tabla de naturalezas debe cubrir las 46 Habilidades del set 151")
    }
}
