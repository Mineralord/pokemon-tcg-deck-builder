package com.mineralord.tcg.data.cosmetics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CosmeticRepositoryTest {

    private val repo = CosmeticRepository.load()

    @Test
    fun `carga el catalogo core desde recursos`() {
        assertTrue(repo.size >= 25, "esperaba el catálogo core completo, fue ${repo.size}")
    }

    @Test
    fun `los ids historicos siguen presentes y estables`() {
        // Ids que ya persisten en saves/nube de jugadores existentes: NO deben cambiar.
        listOf("avatar-brasa", "tapete-arena", "funda-eclipse", "marco-aureo", "caja-real").forEach {
            assertNotNull(repo[it], "falta el id estable $it")
        }
    }

    @Test
    fun `colores hex se parsean a ARGB Long`() {
        val brasa = repo["avatar-brasa"]!!
        assertEquals(0xFFFF7043L, brasa.colors.first())
    }

    @Test
    fun `precio se deriva de la rareza y el prestigio`() {
        assertEquals(CosmeticPricing.priceFor(CosmeticRarity.COMUN), repo["avatar-brasa"]!!.priceMonedas)
        // Cosmos es mítico + prestigio → 6000 * 3.
        assertEquals(6_000 * 3, repo["avatar-cosmos"]!!.priceMonedas)
    }

    @Test
    fun `shopByCategory filtra por categoria y ordena por rareza`() {
        val avatares = repo.shopByCategory(CosmeticCategory.AVATAR)
        assertEquals(CosmeticRarity.COMUN, avatares.first().rarity)
        assertTrue(avatares.all { it.category == CosmeticCategory.AVATAR })
    }

    @Test
    fun `los cosmeticos RESTRICTED se catalogan pero nunca aparecen en la tienda`() {
        // Existen en el catálogo (trazabilidad/preservación §8/§32)…
        val restricted = repo["ref.cardback.kards.legacy"]
        assertNotNull(restricted)
        assertEquals(CosmeticStatus.RESTRICTED, restricted.status)
        assertTrue(!restricted.isDistributable)
        // …pero NUNCA se ofrecen en la tienda (filtrados por status + licencia).
        assertTrue(repo.shopByCategory(CosmeticCategory.FUNDA).none { it.status == CosmeticStatus.RESTRICTED })
    }
}
