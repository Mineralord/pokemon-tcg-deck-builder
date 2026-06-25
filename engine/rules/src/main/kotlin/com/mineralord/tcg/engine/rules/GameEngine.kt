package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.events.GameEvent
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.Phase
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Status

/**
 * Resultado de aplicar un intent: nuevo estado + eventos emitidos. Si [accepted]
 * es false, [state] es el estado SIN cambios y [rejection] explica por qué la
 * acción era ilegal (jamás se muta nada ante una acción inválida).
 */
data class EngineResult(
    val state: GameState,
    val events: List<GameEvent>,
    val accepted: Boolean = true,
    val rejection: String? = null,
) {
    companion object {
        fun reject(state: GameState, reason: String) =
            EngineResult(state, emptyList(), accepted = false, rejection = reason)
    }
}

/**
 * Motor de reglas — Kotlin puro, determinista, headless.
 *
 * Transforma `(estado, intent) -> (estado', eventos)` sin mutar nada. Toda la
 * aleatoriedad pasa por [Rng], de modo que con la misma semilla la partida es
 * reproducible. Emite [GameEvent] para alimentar log, animaciones e IA (§0).
 *
 * Cobertura de Fase 1: turn-loop, robar, desplegar Básicos, evolucionar, unir
 * energía, retirar, atacar (con Debilidad/Resistencia), KO + premios, victoria
 * por premios/sin-Activo/deck-out, y daño de Veneno/Quemadura entre turnos. Los
 * efectos autorados por carta llegan con `:engine:effects`.
 */
class GameEngine(private val rng: Rng) {

    fun apply(state: GameState, intent: GameIntent): EngineResult {
        if (state.isOver) return EngineResult.reject(state, "La partida ha terminado")
        return when (intent) {
            is GameIntent.PlayBasicToBench -> playBasicToBench(state, intent.card)
            is GameIntent.Evolve -> evolve(state, intent.evolution, intent.onto)
            is GameIntent.AttachEnergy -> attachEnergy(state, intent.energy, intent.to)
            is GameIntent.Retreat -> retreat(state, intent.benchTarget)
            is GameIntent.Attack -> attack(state, intent.attackName)
            GameIntent.EndTurn -> endTurn(state)
        }
    }

    // ---------------------------------------------------------------- despliegue

    private fun playBasicToBench(state: GameState, cardId: CardId): EngineResult {
        val me = state.activePlayer
        val card = me.hand.firstOrNull { it.id == cardId }
            ?: return EngineResult.reject(state, "La carta no está en la mano")
        if (card !is PokemonCard || !card.isBasic) {
            return EngineResult.reject(state, "Solo se puede poner un Pokémon Básico en la Banca")
        }
        if (me.bench.size >= BENCH_LIMIT) {
            return EngineResult.reject(state, "La Banca está llena")
        }
        val updated = me.copy(
            hand = me.hand - card,
            bench = me.bench + PokemonInPlay(card),
        )
        return EngineResult(
            withPlayer(state, updated),
            listOf(GameEvent.PokemonPlayed(state.activeSide, cardId, toBench = true)),
        )
    }

    private fun evolve(state: GameState, evoId: CardId, ontoId: CardId): EngineResult {
        val me = state.activePlayer
        val evo = me.hand.firstOrNull { it.id == evoId } as? PokemonCard
            ?: return EngineResult.reject(state, "La evolución no está en la mano")
        val target = me.allInPlay.firstOrNull { it.card.id == ontoId }
            ?: return EngineResult.reject(state, "El objetivo no está en juego")
        if (evo.evolvesFrom != target.card.name.en && evo.evolvesFrom != target.card.name.es) {
            return EngineResult.reject(state, "${evo.name.es} no evoluciona de ${target.card.name.es}")
        }
        if (target.turnsInPlay < 1) {
            return EngineResult.reject(state, "No se puede evolucionar el mismo turno en que entró")
        }
        val evolved = target.copy(
            card = evo,
            evolutionStack = target.evolutionStack + target.card,
            statuses = emptySet(),       // evolucionar cura condiciones especiales
        )
        val updated = replaceInPlay(me, ontoId, evolved).copy(hand = me.hand - evo)
        return EngineResult(
            withPlayer(state, updated),
            listOf(GameEvent.Evolved(state.activeSide, ontoId, evoId)),
        )
    }

    private fun attachEnergy(state: GameState, energyId: CardId, toId: CardId): EngineResult {
        val me = state.activePlayer
        val energy = me.hand.firstOrNull { it.id == energyId } as? EnergyCard
            ?: return EngineResult.reject(state, "La energía no está en la mano")
        val target = me.allInPlay.firstOrNull { it.card.id == toId }
            ?: return EngineResult.reject(state, "El objetivo no está en juego")
        val updated = replaceInPlay(
            me,
            toId,
            target.copy(attachedEnergy = target.attachedEnergy + energy),
        ).copy(hand = me.hand - (energy as Card))
        return EngineResult(
            withPlayer(state, updated),
            listOf(GameEvent.EnergyAttached(state.activeSide, energyId, toId)),
        )
    }

    private fun retreat(state: GameState, benchTargetId: CardId): EngineResult {
        val me = state.activePlayer
        val active = me.active
            ?: return EngineResult.reject(state, "No hay Pokémon Activo")
        val benchMon = me.bench.firstOrNull { it.card.id == benchTargetId }
            ?: return EngineResult.reject(state, "El objetivo no está en la Banca")
        if (active.attachedEnergyCount < active.card.retreatCost.size) {
            return EngineResult.reject(state, "Energía insuficiente para retirarse")
        }
        // Descarta tantas energías como cueste retirarse.
        val toDiscard = active.attachedEnergy.take(active.card.retreatCost.size)
        val newActive = benchMon.copy(statuses = emptySet())   // retirarse cura condiciones
        val updated = me.copy(
            active = newActive,
            bench = me.bench - benchMon + active.copy(
                attachedEnergy = active.attachedEnergy - toDiscard.toSet(),
            ),
            discard = me.discard + toDiscard,
        )
        return EngineResult(
            withPlayer(state, updated),
            listOf(GameEvent.Retreated(state.activeSide, active.card.id, benchTargetId)),
        )
    }

    // ------------------------------------------------------------------- ataque

    private fun attack(state: GameState, attackName: String): EngineResult {
        val me = state.activePlayer
        val foeSide = state.activeSide.other()
        val foe = state.sideState(foeSide)
        val attacker = me.active
            ?: return EngineResult.reject(state, "No hay Pokémon Activo para atacar")
        val defender = foe.active
            ?: return EngineResult.reject(state, "El rival no tiene Pokémon Activo")
        val atk = attacker.card.attacks.firstOrNull { it.name.es == attackName || it.name.en == attackName }
            ?: return EngineResult.reject(state, "Ataque desconocido: $attackName")
        if (attacker.attachedEnergyCount < atk.convertedCost) {
            return EngineResult.reject(state, "Energía insuficiente para ${atk.name.es}")
        }

        val events = mutableListOf<GameEvent>()
        events += GameEvent.Attacked(state.activeSide, attacker.card.id, atk.name.es)

        val base = (atk.baseDamage as? com.mineralord.tcg.engine.model.Damage.Fixed)?.value ?: 0
        val dmg = Damage.calculate(base, attacker.card.types, defender)
        var newFoe = foe
        if (dmg.finalAmount > 0) {
            val damaged = defender.copy(damage = defender.damage + dmg.finalAmount)
            newFoe = foe.copy(active = damaged)
            events += GameEvent.DamageDealt(
                state.activeSide, defender.card.id, dmg.finalAmount,
                dmg.weaknessApplied, dmg.resistanceApplied,
            )
        }

        var working = withPlayer(state, newFoe, foeSide)
        working = handleKnockouts(working, foeSide, events)

        if (working.isOver) return EngineResult(working, events)

        // Atacar termina el turno.
        val ended = endTurn(working)
        return EngineResult(ended.state, events + ended.events)
    }

    // --------------------------------------------------------------- KO / fin

    private fun handleKnockouts(
        state: GameState,
        koSide: Side,
        events: MutableList<GameEvent>,
    ): GameState {
        val target = state.sideState(koSide)
        val active = target.active ?: return state
        if (!active.isKnockedOut) return state

        val attackerSide = koSide.other()
        events += GameEvent.KnockedOut(koSide, active.card.id)

        // El atacante toma premios.
        val prizesToTake = active.card.prizeValue
        val attacker = state.sideState(attackerSide)
        val taken = attacker.prizes.take(prizesToTake)
        val attackerAfter = attacker.copy(
            prizes = attacker.prizes - taken.toSet(),
            prizesRemaining = (attacker.prizesRemaining - prizesToTake).coerceAtLeast(0),
            hand = attacker.hand + taken,
        )
        if (prizesToTake > 0) events += GameEvent.PrizeTaken(attackerSide, prizesToTake)

        // El noqueado va al descarte (con lo que llevaba encima).
        val koalition = active.evolutionStack + active.card
        val targetAfter = target.copy(
            active = null,
            discard = target.discard + koalition + active.attachedEnergy + active.attachedTools,
        )

        var next = state
            .let { withPlayer(it, attackerAfter, attackerSide) }
            .let { withPlayer(it, targetAfter, koSide) }

        // Victoria por premios.
        if (attackerAfter.prizesRemaining <= 0) {
            events += GameEvent.GameWon(attackerSide)
            return next.copy(winner = attackerSide, phase = Phase.GAME_OVER)
        }
        // Promoción forzada: si no hay activo pero hay banca, sube el primero.
        if (targetAfter.active == null) {
            if (targetAfter.bench.isEmpty()) {
                events += GameEvent.GameWon(attackerSide)
                return next.copy(winner = attackerSide, phase = Phase.GAME_OVER)
            }
            val promoted = targetAfter.bench.first()
            next = withPlayer(
                next,
                targetAfter.copy(active = promoted, bench = targetAfter.bench.drop(1)),
                koSide,
            )
        }
        return next
    }

    // ------------------------------------------------------------- fin de turno

    private fun endTurn(state: GameState): EngineResult {
        val events = mutableListOf<GameEvent>()
        events += GameEvent.TurnEnded(state.activeSide)

        // Daño entre turnos (Veneno/Quemadura) al Activo del jugador que termina.
        var working = applyBetweenTurns(state, state.activeSide, events)
        working = handleKnockouts(working, state.activeSide, events)
        if (working.isOver) return EngineResult(working, events)

        // Cambio de turno.
        val nextSide = working.activeSide.other()
        val nextTurn = working.turn + 1
        working = working.copy(activeSide = nextSide, turn = nextTurn, phase = Phase.DRAW)
        events += GameEvent.TurnStarted(nextSide, nextTurn)

        // El jugador entrante roba; sin cartas = deck-out (pierde).
        val drawer = working.sideState(nextSide)
        if (drawer.deck.isEmpty()) {
            events += GameEvent.GameWon(nextSide.other())
            return EngineResult(
                working.copy(winner = nextSide.other(), phase = Phase.GAME_OVER),
                events,
            )
        }
        val drawn = drawer.deck.first()
        working = withPlayer(
            working,
            drawer.copy(deck = drawer.deck.drop(1), hand = drawer.hand + drawn),
            nextSide,
        )
        events += GameEvent.CardsDrawn(nextSide, 1)

        // Incrementa el contador de turnos-en-juego del nuevo Activo (permite evolucionar).
        val refreshed = working.sideState(nextSide)
        val tickedInPlay = refreshed.copy(
            active = refreshed.active?.let { it.copy(turnsInPlay = it.turnsInPlay + 1) },
            bench = refreshed.bench.map { it.copy(turnsInPlay = it.turnsInPlay + 1) },
        )
        working = withPlayer(working, tickedInPlay, nextSide).copy(phase = Phase.MAIN)

        return EngineResult(working, events)
    }

    private fun applyBetweenTurns(
        state: GameState,
        side: Side,
        events: MutableList<GameEvent>,
    ): GameState {
        val ps = state.sideState(side)
        val active = ps.active ?: return state
        var extra = 0
        if (Status.POISONED in active.statuses) extra += POISON_DAMAGE
        if (Status.BURNED in active.statuses) extra += BURN_DAMAGE
        if (extra == 0) return state
        val damaged = active.copy(damage = active.damage + extra)
        events += GameEvent.DamageDealt(side, active.card.id, extra)
        return withPlayer(state, ps.copy(active = damaged), side)
    }

    // ------------------------------------------------------------------ helpers

    private fun Side.other(): Side = if (this == Side.PLAYER) Side.OPPONENT else Side.PLAYER

    private fun withPlayer(state: GameState, ps: PlayerState, side: Side = ps.side): GameState =
        if (side == Side.PLAYER) state.copy(player = ps) else state.copy(opponent = ps)

    private fun replaceInPlay(ps: PlayerState, id: CardId, replacement: PokemonInPlay): PlayerState =
        when {
            ps.active?.card?.id == id -> ps.copy(active = replacement)
            else -> ps.copy(bench = ps.bench.map { if (it.card.id == id) replacement else it })
        }

    companion object {
        const val BENCH_LIMIT = 5
        const val POISON_DAMAGE = 10
        const val BURN_DAMAGE = 20
    }
}
