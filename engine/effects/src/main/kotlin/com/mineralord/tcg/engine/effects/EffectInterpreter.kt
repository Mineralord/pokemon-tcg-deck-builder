package com.mineralord.tcg.engine.effects

import com.mineralord.tcg.engine.events.GameEvent
import com.mineralord.tcg.engine.model.Amount
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Counter
import com.mineralord.tcg.engine.model.Effect
import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Target

/** Quién/qué origina el efecto (para resolver SELF, recoil, etc.). */
data class EffectSource(val actingSide: Side, val sourceId: CardId?)

/**
 * Resultado de interpretar un [Effect]: nuevo estado, eventos emitidos y
 * decisiones pendientes. Si [pending] no está vacío, el motor debe resolverlas
 * (vía IA o jugador) antes de continuar. Las ops deterministas ya quedaron
 * aplicadas en [state].
 */
data class EffectResult(
    val state: GameState,
    val events: List<GameEvent>,
    val pending: List<PendingDecision> = emptyList(),
)

/**
 * Intérprete del DSL de efectos — Kotlin puro y determinista.
 *
 * Ejecuta las operaciones del catálogo cerrado ([EffectOp]) contra un
 * [GameState] inmutable, devolviendo un estado nuevo + [GameEvent]. Las ops que
 * requieren elección ([EffectOp.ChooseTarget], [EffectOp.SearchDeck],
 * [EffectOp.MoveEnergy]) no se "adivinan": se emiten como [PendingDecision].
 *
 * Cobertura determinista de Fase 1: Damage, ExtraDamage, Recoil, Heal,
 * ApplyStatus, RemoveStatus, DrawCards, DrawUntil, DiscardEnergy. El cálculo de
 * Debilidad/Resistencia del ataque principal vive en `:engine:rules`; el daño de
 * efecto aquí es directo (como en banca), salvo que el motor indique lo contrario.
 */
class EffectInterpreter {

    fun execute(effect: Effect, source: EffectSource, state: GameState): EffectResult {
        var working = state
        val events = mutableListOf<GameEvent>()
        val pending = mutableListOf<PendingDecision>()

        for (op in effect.ops) {
            val step = applyOp(op, source, working)
            working = step.state
            events += step.events
            pending += step.pending
        }
        return EffectResult(working, events, pending)
    }

    // ---------------------------------------------------------------- ops

    private fun applyOp(op: EffectOp, src: EffectSource, state: GameState): EffectResult =
        when (op) {
            is EffectOp.Damage -> {
                val n = resolveAmount(op.amount, src, state)
                damageTargets(targets(op.target, src, state), n, src.actingSide, state)
            }
            is EffectOp.ExtraDamage -> {
                val n = resolveAmount(op.amount, src, state)
                damageTargets(targets(Target.OPP_ACTIVE, src, state), n, src.actingSide, state)
            }
            is EffectOp.Recoil -> {
                val n = resolveAmount(op.amount, src, state)
                damageTargets(targets(Target.SELF, src, state), n, src.actingSide, state)
            }
            is EffectOp.Heal -> {
                val n = resolveAmount(op.amount, src, state)
                healTargets(targets(op.target, src, state), n, src.actingSide, state)
            }
            is EffectOp.ApplyStatus -> applyStatus(op, src, state)
            is EffectOp.RemoveStatus -> removeStatus(op, src, state)
            is EffectOp.DrawCards -> draw(src.actingSide, op.count, state)
            is EffectOp.DrawUntil -> {
                val have = state.sideState(src.actingSide).hand.size
                draw(src.actingSide, (op.handSize - have).coerceAtLeast(0), state)
            }
            is EffectOp.DiscardEnergy -> discardEnergy(op, src, state)

            // Ops con elección: se delegan como decisión pendiente.
            is EffectOp.ChooseTarget -> EffectResult(
                state, emptyList(),
                listOf(
                    PendingDecision.ChooseTargets(
                        src.actingSide, op.prompt,
                        candidates = targets(op.from, src, state).map { it.card.id },
                        count = op.howMany,
                    ),
                ),
            )
            is EffectOp.SearchDeck -> EffectResult(
                state, emptyList(),
                listOf(
                    PendingDecision.SearchCards(
                        src.actingSide,
                        com.mineralord.tcg.engine.model.LocalizedText("Busca en tu mazo", "Search your deck"),
                        from = com.mineralord.tcg.engine.model.Zone.DECK,
                        filter = op.filter,
                        destination = op.to,
                        count = op.count,
                        candidates = matching(state.sideState(src.actingSide).deck, op.filter),
                    ),
                ),
            )
            is EffectOp.MoveEnergy -> EffectResult(
                state, emptyList(),
                listOf(
                    PendingDecision.MoveEnergy(
                        src.actingSide,
                        com.mineralord.tcg.engine.model.LocalizedText("Mueve energía", "Move Energy"),
                        fromCandidates = targets(op.from, src, state).map { it.card.id },
                        toCandidates = targets(op.to, src, state).map { it.card.id },
                        count = op.count,
                    ),
                ),
            )
        }

    // ---------------------------------------------------- aplicaciones concretas

    private fun damageTargets(refs: List<PokemonInPlay>, amount: Int, by: Side, state: GameState): EffectResult {
        if (amount <= 0 || refs.isEmpty()) return EffectResult(state, emptyList())
        var working = state
        val events = mutableListOf<GameEvent>()
        for (r in refs) {
            working = updatePokemon(working, r.card.id) { it.copy(damage = it.damage + amount) }
            events += GameEvent.DamageDealt(by, r.card.id, amount)
        }
        return EffectResult(working, events)
    }

    private fun healTargets(refs: List<PokemonInPlay>, amount: Int, by: Side, state: GameState): EffectResult {
        if (amount <= 0 || refs.isEmpty()) return EffectResult(state, emptyList())
        var working = state
        val events = mutableListOf<GameEvent>()
        for (r in refs) {
            working = updatePokemon(working, r.card.id) {
                it.copy(damage = (it.damage - amount).coerceAtLeast(0))
            }
            events += GameEvent.Healed(by, r.card.id, amount)
        }
        return EffectResult(working, events)
    }

    private fun applyStatus(op: EffectOp.ApplyStatus, src: EffectSource, state: GameState): EffectResult {
        var working = state
        val events = mutableListOf<GameEvent>()
        for (r in targets(op.target, src, state)) {
            working = updatePokemon(working, r.card.id) { it.copy(statuses = it.statuses + op.states) }
            op.states.forEach { events += GameEvent.StatusApplied(src.actingSide, r.card.id, it) }
        }
        return EffectResult(working, events)
    }

    private fun removeStatus(op: EffectOp.RemoveStatus, src: EffectSource, state: GameState): EffectResult {
        var working = state
        val events = mutableListOf<GameEvent>()
        for (r in targets(op.target, src, state)) {
            r.statuses.forEach { events += GameEvent.StatusRemoved(src.actingSide, r.card.id, it) }
            working = updatePokemon(working, r.card.id) { it.copy(statuses = emptySet()) }
        }
        return EffectResult(working, events)
    }

    private fun draw(side: Side, count: Int, state: GameState): EffectResult {
        if (count <= 0) return EffectResult(state, emptyList())
        val ps = state.sideState(side)
        val real = minOf(count, ps.deck.size)
        if (real == 0) return EffectResult(state, emptyList())
        val drawn = ps.deck.take(real)
        val updated = ps.copy(deck = ps.deck.drop(real), hand = ps.hand + drawn)
        return EffectResult(withPlayer(state, updated, side), listOf(GameEvent.CardsDrawn(side, real)))
    }

    private fun discardEnergy(op: EffectOp.DiscardEnergy, src: EffectSource, state: GameState): EffectResult {
        var working = state
        val ps = state.sideState(src.actingSide)
        val discarded = mutableListOf<com.mineralord.tcg.engine.model.Card>()
        for (r in targets(op.target, src, state)) {
            val toDiscard = r.attachedEnergy.take(op.count)
            discarded += toDiscard
            working = updatePokemon(working, r.card.id) {
                it.copy(attachedEnergy = it.attachedEnergy - toDiscard.toSet())
            }
        }
        if (discarded.isEmpty()) return EffectResult(working, emptyList())
        val updated = working.sideState(src.actingSide).copy(
            discard = working.sideState(src.actingSide).discard + discarded,
        )
        return EffectResult(withPlayer(working, updated, src.actingSide), emptyList())
    }

    // ---------------------------------------------------------------- helpers

    private fun targets(target: Target, src: EffectSource, state: GameState): List<PokemonInPlay> {
        val me = state.sideState(src.actingSide)
        val foe = state.sideState(src.actingSide.other())
        return when (target) {
            Target.SELF -> listOfNotNull(me.allInPlay.firstOrNull { it.card.id == src.sourceId })
            Target.OWN_ACTIVE -> listOfNotNull(me.active)
            Target.OPP_ACTIVE -> listOfNotNull(foe.active)
            Target.OWN_BENCH -> me.bench
            Target.OPP_BENCH -> foe.bench
            Target.OWN_ALL -> me.allInPlay
            Target.OPP_ALL -> foe.allInPlay
            Target.CHOSEN -> emptyList()  // resuelto vía PendingDecision
            else -> emptyList()           // zonas: no son objetivos Pokémon
        }
    }

    private fun resolveAmount(amount: Amount, src: EffectSource, state: GameState): Int = when (amount) {
        is Amount.Fixed -> amount.n
        is Amount.PerCount -> {
            val refs = targets(amount.target, src, state)
            val count = when (amount.of) {
                Counter.BENCH_COUNT -> state.sideState(
                    if (amount.target == Target.OPP_BENCH || amount.target == Target.OPP_ALL) src.actingSide.other() else src.actingSide,
                ).bench.size
                Counter.ENERGY_ATTACHED -> refs.sumOf { it.attachedEnergyCount }
                Counter.DAMAGE_COUNTERS -> refs.sumOf { it.damage / 10 }
                Counter.HEADS -> 0  // requiere lanzamientos: lo cubrirá rules con Rng
            }
            count * amount.mult
        }
    }

    private fun matching(cards: List<com.mineralord.tcg.engine.model.Card>, filter: com.mineralord.tcg.engine.model.CardFilter): List<CardId> =
        cards.filter { c ->
            (filter.supertype == null || c.supertype == filter.supertype) &&
                (filter.isBasic == null || (c is com.mineralord.tcg.engine.model.PokemonCard && c.isBasic == filter.isBasic)) &&
                (filter.type == null || (c is com.mineralord.tcg.engine.model.PokemonCard && filter.type in c.types)) &&
                (filter.nameContains == null || c.name.es.contains(filter.nameContains!!, true) || c.name.en.contains(filter.nameContains!!, true))
        }.map { it.id }

    private fun Side.other(): Side = if (this == Side.PLAYER) Side.OPPONENT else Side.PLAYER

    private fun withPlayer(state: GameState, ps: PlayerState, side: Side): GameState =
        if (side == Side.PLAYER) state.copy(player = ps) else state.copy(opponent = ps)

    /** Aplica una transformación al Pokémon (activo o de banca) con ese id, en ambos lados. */
    private fun updatePokemon(
        state: GameState,
        id: CardId,
        transform: (PokemonInPlay) -> PokemonInPlay,
    ): GameState {
        fun update(ps: PlayerState): PlayerState = ps.copy(
            active = ps.active?.let { if (it.card.id == id) transform(it) else it },
            bench = ps.bench.map { if (it.card.id == id) transform(it) else it },
        )
        return state.copy(player = update(state.player), opponent = update(state.opponent))
    }
}
