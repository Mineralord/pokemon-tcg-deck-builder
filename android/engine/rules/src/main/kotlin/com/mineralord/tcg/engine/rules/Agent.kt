package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.Side

/**
 * Política de decisión de un jugador automático. El motor le pide una acción
 * dado el estado y el lado al que le toca; debe devolver un [GameIntent] legal
 * (o el motor lo rechazará y el orquestador forzará fin de turno).
 *
 * Esta interfaz sirve tanto al **arnés de self-play** (validación en CI) como
 * al **rival IA** del juego real.
 */
interface Agent {
    fun decide(state: GameState, side: Side): GameIntent
}

/**
 * Agente codicioso de referencia: ataca si puede; si no, intenta unir energía
 * al Activo; si no, termina el turno. Determinista (sin aleatoriedad propia),
 * suficiente para garantizar progreso y terminación de la partida.
 */
class GreedyAgent : Agent {
    override fun decide(state: GameState, side: Side): GameIntent {
        val me = state.sideState(side)
        val active = me.active ?: return GameIntent.EndTurn

        // 1) ¿Hay un ataque pagable? Atacar (además, termina el turno).
        val affordable = active.card.attacks.firstOrNull { active.attachedEnergyCount >= it.convertedCost }
        if (affordable != null) return GameIntent.Attack(affordable.name.es)

        // 2) ¿Energía en mano? Unirla al Activo para habilitar un ataque futuro.
        val energy = me.hand.firstOrNull { it is EnergyCard }
        if (energy != null) return GameIntent.AttachEnergy(energy.id, active.card.id)

        // 3) Nada más que hacer.
        return GameIntent.EndTurn
    }
}
