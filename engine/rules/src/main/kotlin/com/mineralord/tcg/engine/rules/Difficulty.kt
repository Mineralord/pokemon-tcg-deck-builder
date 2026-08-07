package com.mineralord.tcg.engine.rules

/**
 * Dificultad del rival IA en el combate PvE. Escala la **calidad de juego** de
 * [SmartAgent] (nunca da ventajas de estadísticas ni trampas): de una IA torpe
 * que malgasta sus ataques a una que optimiza objetivo y busca el Noqueo letal.
 *
 * Nombradas como las Poké Balls por gama ascendente de captura/dificultad.
 */
enum class Difficulty {
    /** Poké Ball — Novata: no desarrolla, elige el ataque más flojo, promueve mal. */
    POKEBALL,

    /** Súper Ball — Básica: ataca con lo primero pagable; sin Herramientas ni Entrenadores. */
    SUPERBALL,

    /** Ultra Ball — Táctica: desarrolla banca, ancla Herramientas, ataca con lo más fuerte. */
    ULTRABALL,

    /** Master Ball — Experta: todo lo de Ultra + prioriza el Noqueo letal y el mejor objetivo. */
    MASTERBALL,
}
