package com.mineralord.tcg.studio.assets

import com.mineralord.tcg.core.animation.AnimationRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifica el contrato de la fuente única de verdad: consulta por tipo/categoría, invariantes de
 * integridad (ids únicos, ≤1 Canon por categoría) y que la historia se preserva vía `Deprecated`.
 */
class AssetRegistryTest {

    private fun anim(
        id: String,
        category: String,
        status: AssetStatus,
    ): AnimationAsset = AnimationAsset(
        id = id,
        name = id,
        category = category,
        status = status,
        request = AnimationRequest.CardDrawn("studio", "preview"),
    )

    @Test
    fun `agrupa por tipo y categoria en orden de insercion`() {
        val registry = AssetRegistry(
            listOf(
                anim("A1", "Robar carta", AssetStatus.Canon),
                anim("B1", "Evolución", AssetStatus.Experimental),
                anim("B2", "Evolución", AssetStatus.Candidate),
            ),
        )
        assertEquals(listOf("Robar carta", "Evolución"), registry.categories(AssetType.Animation))
        assertEquals(listOf("B1", "B2"), registry.byCategory(AssetType.Animation, "Evolución").map { it.id })
        assertEquals(3, registry.byType(AssetType.Animation).size)
    }

    @Test
    fun `permite como maximo una variante Canon por categoria`() {
        assertFailsWith<IllegalArgumentException> {
            AssetRegistry(
                listOf(
                    anim("A1", "Evolución", AssetStatus.Canon),
                    anim("A2", "Evolución", AssetStatus.Canon),
                ),
            )
        }
    }

    @Test
    fun `rechaza ids duplicados`() {
        assertFailsWith<IllegalArgumentException> {
            AssetRegistry(listOf(anim("DUP", "c", AssetStatus.Experimental), anim("DUP", "c", AssetStatus.Experimental)))
        }
    }

    @Test
    fun `Deprecated conserva la historia sin ser Canon`() {
        val registry = AssetRegistry(
            listOf(
                anim("OLD", "Evolución", AssetStatus.Deprecated),
                anim("NEW", "Evolución", AssetStatus.Canon),
            ),
        )
        // La antigua sigue existiendo (no se borra); la Canon es la nueva.
        assertTrue(registry.byId("OLD") != null)
        assertEquals("NEW", registry.canonOf(AssetType.Animation, "Evolución")?.id)
    }

    @Test
    fun `canonOf devuelve null si ninguna variante es Canon`() {
        val registry = AssetRegistry(listOf(anim("E1", "Evolución", AssetStatus.Experimental)))
        assertNull(registry.canonOf(AssetType.Animation, "Evolución"))
    }
}
