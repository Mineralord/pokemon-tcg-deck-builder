package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.Side

/**
 * IA de juego real: a diferencia de [GreedyAgent], **resuelve decisiones
 * pendientes** y usa [GameEngine.legalIntents] para jugar de forma razonable
 * (desarrolla la Banca, une energía y ataca). Determinista y con orden de
 * prioridad fijo, de modo que nunca cuelga un turno.
 *
 * Recibe el [engine] para consultar las jugadas legales (el motor es sin estado:
 * solo `rng`/`effects`/`interpreter`).
 */
class SmartAgent(private val engine: GameEngine) : Agent {

    override fun decide(state: GameState, side: Side): GameIntent {
        // 1) Hay una decisión pendiente nuestra: resolverla con una elección legal.
        state.interaction?.let { inter ->
            if (inter.side == side) return resolveDecision(inter.decision)
        }

        val legal = engine.legalIntents(state)
        val me = state.sideState(side)

        // 2) Asegurar respaldo: si la Banca está vacía, bajar un Básico.
        if (me.bench.isEmpty()) {
            legal.firstOrNull { it is GameIntent.PlayBasicToBench }?.let { return it }
        }

        // 3) Unir energía al Activo (preferente) si aún no se hizo este turno.
        val active = me.active
        if (active != null) {
            legal.firstOrNull {
                it is GameIntent.AttachEnergy && it.to == active.card.id
            }?.let { return it }
        }

        // 4) Atacar con el ataque pagable de mayor coste convertido (más fuerte).
        val bestAttack = active?.card?.attacks
            ?.filter { active.attachedEnergyCount >= it.convertedCost }
            ?.maxByOrNull { it.convertedCost }
        if (bestAttack != null) return GameIntent.Attack(bestAttack.name.es)

        // 5) Jugar un Entrenador útil (robo/búsqueda) si hay.
        legal.firstOrNull { it is GameIntent.PlayTrainer }?.let { return it }

        // 6) Evolucionar si se puede.
        legal.firstOrNull { it is GameIntent.Evolve }?.let { return it }

        // 7) Nada productivo: terminar el turno.
        return GameIntent.EndTurn
    }

    /** Escoge una respuesta legal mínima para cada tipo de decisión. */
    private fun resolveDecision(decision: PendingDecision): GameIntent = when (decision) {
        is PendingDecision.ChooseTargets ->
            GameIntent.ResolveDecision(decision.candidates.take(decision.count))
        is PendingDecision.SearchCards ->
            GameIntent.ResolveDecision(decision.candidates.take(decision.count))
        is PendingDecision.MoveEnergy ->
            GameIntent.ResolveDecision(decision.fromCandidates.take(1) + decision.toCandidates.take(1))
    }
}
