package com.mineralord.tcg.engine.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regla oficial de coste de ataque por TIPOS (rulebook "CHECK the Energy attached"): los símbolos de
 * tipo específico exigen una Energía de ESE tipo; los Incoloros aceptan cualquiera. Vale para todos los
 * ataques presentes y futuros porque opera sobre [Attack.cost].
 */
class EnergyCostTest {

    private fun art() = ArtworkRefs(null, null, "s.png", "l.png")
    private fun set() = SetInfo("sv3pt5", LocalizedText("151", "151"), "Scarlet & Violet")
    private fun basic(type: EnergyType) = BasicEnergy(
        CardId("e-${type.name}-${count++}"), LocalizedText(type.name, type.name), set(),
        Rarity.COMMON, null, art(), type,
    )
    private fun special(provision: EnergyProvision) = SpecialEnergy(
        CardId("se-${count++}"), LocalizedText("Especial", "Special"), set(),
        Rarity.RARE, null, art(), provides = provision, stateModifiers = emptyList(), effect = EffectId("none"),
    )

    private var count = 0
    private val W = EnergyType.WATER
    private val L = EnergyType.LIGHTNING
    private val C = EnergyType.COLORLESS

    // --- El caso reportado: Pulso Dragón de Dragonite = 1 Agua + 1 Rayo ---

    @Test
    fun `dos Agua NO pagan un coste de Agua mas Rayo`() {
        assertFalse(canPayEnergyCost(listOf(basic(W), basic(W)), listOf(W, L)))
    }

    @Test
    fun `una Agua y una Rayo SI pagan Agua mas Rayo`() {
        assertTrue(canPayEnergyCost(listOf(basic(W), basic(L)), listOf(W, L)))
    }

    // --- Incoloro acepta cualquier tipo ---

    @Test
    fun `el simbolo Incoloro se paga con cualquier Energia`() {
        assertTrue(canPayEnergyCost(listOf(basic(W)), listOf(C)))
        assertTrue(canPayEnergyCost(listOf(basic(W), basic(L)), listOf(C, C)))
    }

    @Test
    fun `un tipo especifico mas Incoloro exige el tipo especifico`() {
        // Coste Rayo + Incoloro: dos Agua NO alcanzan (falta el Rayo).
        assertFalse(canPayEnergyCost(listOf(basic(W), basic(W)), listOf(L, C)))
        // Rayo + Agua: el Agua cubre el Incoloro. Sí alcanza.
        assertTrue(canPayEnergyCost(listOf(basic(L), basic(W)), listOf(L, C)))
    }

    // --- Cantidad total insuficiente ---

    @Test
    fun `sin suficientes Energias totales no se puede pagar`() {
        assertFalse(canPayEnergyCost(listOf(basic(W)), listOf(W, C)))
    }

    // --- Energías especiales ---

    @Test
    fun `Energia comodin ElegirUno cubre cualquier tipo`() {
        assertTrue(canPayEnergyCost(listOf(special(EnergyProvision.ChooseOne), basic(W)), listOf(W, L)))
    }

    @Test
    fun `Energia Fija cubre solo sus tipos`() {
        val doble = special(EnergyProvision.Fixed(listOf(W, L)))   // aporta Agua o Rayo (uno)
        assertTrue(canPayEnergyCost(listOf(doble, basic(W)), listOf(W, L)))    // doble=Rayo, basic=Agua
        assertFalse(canPayEnergyCost(listOf(doble), listOf(W, L)))            // una sola energía, dos símbolos
    }

    // --- Coste vacío (0 símbolos) ---

    @Test
    fun `un coste de cero se paga sin Energia`() {
        assertTrue(canPayEnergyCost(emptyList(), emptyList()))
    }

    // --- Recargo temporal +Incoloro ---

    @Test
    fun `el recargo Incoloro extra aumenta el coste`() {
        assertFalse(canPayEnergyCost(listOf(basic(W)), listOf(W), extraColorless = 1))
        assertTrue(canPayEnergyCost(listOf(basic(W), basic(L)), listOf(W), extraColorless = 1))
    }
}
