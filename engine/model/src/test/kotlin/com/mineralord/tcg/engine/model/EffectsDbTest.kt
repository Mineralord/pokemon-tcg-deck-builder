package com.mineralord.tcg.engine.model

import kotlin.test.Test
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
    fun `cartas con ops no modeladas NO estan registradas`() {
        // Cambio (cambiarActivo) y Joven (barajarManoEnMazo) caen a daño base.
        assertFalse(registry.has(EffectId("sv3pt5-206")))
        assertFalse(registry.has(EffectId("sv1-198")))
    }
}
