package com.mineralord.tcg.data.profile

/**
 * Parámetros oficiales de la economía (Fase 2 — Economía Principal).
 *
 * Centraliza fuentes (recompensas) y consumos (precios/límites) de los 4 recursos
 * para que ninguna pantalla los invente por su cuenta. Los valores pueden ajustarse
 * (balance) sin tocar la lógica; su cambio afecta a todo el juego de forma coherente.
 */
object EconomyRules {

    // ----- Fuentes (Fase 2 §6.8: recursos desde PvE/PvP/Logros/Temporadas) -----

    /** Cristales otorgados al ganar una partida PvE. */
    const val PVE_WIN_CRISTALES = 25

    /** Monedas otorgadas al ganar una partida PvE (prestigio/cosméticos). */
    const val PVE_WIN_MONEDAS = 10

    /**
     * Concesión inicial única de Cristales (estado inicial de la cuenta, análogo a los
     * mazos de inicio; NO es una recompensa por inicio de sesión). Permite que un jugador
     * nuevo pueda comprar algún sobre antes de acumular Cristales jugando.
     */
    const val STARTER_CRISTALES = 300

    /**
     * Concesión inicial única de Monedas (estado inicial de la cuenta, no login-reward).
     * Permite que un jugador nuevo pueda personalizarse en la Tienda de Cosméticos antes
     * de acumular Monedas jugando (Fase 3 §3.3).
     */
    const val STARTER_MONEDAS = 1500

    // ----- Consumos (Fase 2 §7.4 / Fase 3 §4.6: compra de sobres con Cristales) -----

    /** Precio en Cristales de un sobre comprado (uniforme para todas las expansiones). */
    const val PACK_PRICE_CRISTALES = 100

    /** Máximo de sobres COMPRADOS por día (además de los gratuitos del monedero). */
    const val MAX_PACKS_BOUGHT_PER_DAY = 10

    // ----- Consumos cosméticos (Fase 3 §14: precio por rareza en Monedas) -----

    /**
     * Precio en Monedas de un cosmético según su rareza (§14.3: el precio refleja la
     * rareza/complejidad). Estable y transparente (§14.6). Los cosméticos de prestigio
     * (§14.5) cuestan [PRESTIGE_MULTIPLIER]× el precio de su rareza: objetivos de muy
     * largo plazo, símbolo de dedicación, nunca ventaja competitiva.
     */
    fun cosmeticPrice(rarity: CosmeticRarity, prestige: Boolean = false): Int {
        val base = when (rarity) {
            CosmeticRarity.COMUN -> 200
            CosmeticRarity.POCO_COMUN -> 400
            CosmeticRarity.RARO -> 800
            CosmeticRarity.EPICO -> 1_500
            CosmeticRarity.LEGENDARIO -> 3_000
            CosmeticRarity.MITICO -> 6_000
        }
        return if (prestige) base * PRESTIGE_MULTIPLIER else base
    }

    private const val PRESTIGE_MULTIPLIER = 3
}
