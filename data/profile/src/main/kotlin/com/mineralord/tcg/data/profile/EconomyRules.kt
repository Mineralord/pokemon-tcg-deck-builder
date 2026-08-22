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

    // ----- Consumos (Fase 2 §7.4 / Fase 3 §4.6: compra de sobres con Cristales) -----

    /** Precio en Cristales de un sobre comprado (uniforme para todas las expansiones). */
    const val PACK_PRICE_CRISTALES = 100

    /** Máximo de sobres COMPRADOS por día (además de los gratuitos del monedero). */
    const val MAX_PACKS_BOUGHT_PER_DAY = 10
}
