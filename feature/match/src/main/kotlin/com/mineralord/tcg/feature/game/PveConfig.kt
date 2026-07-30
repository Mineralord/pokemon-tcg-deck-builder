package com.mineralord.tcg.feature.game

import com.mineralord.tcg.engine.rules.Difficulty

/**
 * Configuración de la próxima partida PvE elegida en el menú (Inicio → JUGAR).
 * Es un puente simple entre la UI de selección de dificultad y el
 * [GameViewModel], que se construye sin argumentos (patrón AndroidViewModel) y
 * lee esta selección en su `init`.
 */
object PveConfig {
    /** Dificultad del rival IA para la siguiente partida. Por defecto, Ultra Ball. */
    @Volatile
    var difficulty: Difficulty = Difficulty.ULTRABALL
}
