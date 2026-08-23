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
     * Cristales de PARTICIPACIÓN al perder una partida PvE. El Canon exige que la economía
     * "no se convierta en una barrera para jugar" (Fase 2, §4.3.1): terminar una partida
     * SIEMPRE otorga algo. Cantidad menor que la victoria (balance ajustable, §8.1.3).
     */
    const val PVE_LOSS_CRISTALES = 10

    /** Monedas de participación al perder una partida PvE (menor que la victoria). */
    const val PVE_LOSS_MONEDAS = 5

    // ----- PvP CASUAL (no clasificatorio) -----
    // El Ranked competitivo premia por TEMPORADA (Fase 10, Cap. VII), NO por partida. El PvP
    // online actual es CASUAL/no clasificatorio (sin Rating ni temporada): fuera del alcance de
    // esa regla, otorga una recompensa de participación por partida (menor que el PvE, que es el
    // pilar de progresión en solitario). Cuando exista el Ranked, este premio casual convive con
    // las recompensas de temporada sin solaparse.

    /** Cristales al ganar una partida PvP casual. */
    const val PVP_WIN_CRISTALES = 15

    /** Monedas al ganar una partida PvP casual. */
    const val PVP_WIN_MONEDAS = 8

    /** Cristales de participación al perder una partida PvP casual. */
    const val PVP_LOSS_CRISTALES = 6

    /** Monedas de participación al perder una partida PvP casual. */
    const val PVP_LOSS_MONEDAS = 3

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

    // El precio de los cosméticos por rareza (§14) vive en `data:cosmetics`
    // (`CosmeticPricing`), junto al dominio cosmético, para mantener ese módulo
    // autocontenido y evitar dependencias circulares con la economía.
}
