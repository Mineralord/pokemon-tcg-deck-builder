package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.Side

/** Desenlace de una partida automática. */
data class SelfPlayResult(
    val winner: Side?,
    val turns: Int,
    val totalEvents: Int,
    /** true si la partida alcanzó GAME_OVER por sí sola (no por tope de turnos). */
    val finishedCleanly: Boolean,
)

/**
 * Arnés de self-play: hace jugar a dos [Agent] una partida completa con el
 * [GameEngine]. Es la **prueba empírica del "100% automatizado"**: si miles de
 * partidas (con semillas distintas) terminan solas, no existen estados colgados
 * ni resoluciones manuales pendientes.
 *
 * Guardas de progreso:
 *  - Acción ilegal de un agente -> se fuerza fin de turno (nunca se bloquea).
 *  - Tope de acciones por turno -> evita bucles si un agente insiste en algo.
 *  - Tope de turnos -> red de seguridad global.
 */
class SelfPlay(
    private val engine: GameEngine,
    private val agents: Map<Side, Agent>,
    private val maxTurns: Int = 400,
    private val maxActionsPerTurn: Int = 30,
) {
    fun play(initial: GameState): SelfPlayResult {
        var state = initial
        var totalEvents = 0
        var actionsThisTurn = 0

        while (!state.isOver && state.turn <= maxTurns) {
            val side = state.activeSide
            val intent = agents.getValue(side).decide(state, side)
            val result = engine.apply(state, intent)
            totalEvents += result.events.size

            if (!result.accepted) {
                // Acción ilegal: garantizamos progreso terminando el turno.
                val forced = engine.apply(state, GameIntent.EndTurn)
                totalEvents += forced.events.size
                state = forced.state
                actionsThisTurn = 0
                continue
            }

            val turnBefore = state.turn
            state = result.state
            if (state.turn != turnBefore) {
                actionsThisTurn = 0
            } else if (++actionsThisTurn >= maxActionsPerTurn) {
                val forced = engine.apply(state, GameIntent.EndTurn)
                totalEvents += forced.events.size
                state = forced.state
                actionsThisTurn = 0
            }
        }

        return SelfPlayResult(
            winner = state.winner,
            turns = state.turn,
            totalEvents = totalEvents,
            finishedCleanly = state.isOver,
        )
    }
}
