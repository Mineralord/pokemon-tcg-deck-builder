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
        // Ámbar Viejo Antiguo — Amber Protection (sv3pt5-154#abi) sigue pendiente de modelar
        // (inmunidad a efectos de Habilidades rivales; el fósil se juega, la Habilidad queda inerte).
        assertFalse(registry.has(EffectsDb.abiKey("sv3pt5-154", "Amber Protection")))
    }
}
