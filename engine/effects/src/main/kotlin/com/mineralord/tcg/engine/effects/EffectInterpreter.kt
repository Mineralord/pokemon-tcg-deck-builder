package com.mineralord.tcg.engine.effects

import com.mineralord.tcg.engine.events.GameEvent
import com.mineralord.tcg.engine.model.Amount
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardFilter
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Counter
import com.mineralord.tcg.engine.model.Effect
import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.PendingInteraction
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Target
import com.mineralord.tcg.engine.model.Zone

/** Quién/qué origina el efecto (para resolver SELF, recoil, etc.). */
data class EffectSource(val actingSide: Side, val sourceId: CardId?)

/**
 * Resultado de interpretar un [Effect]: nuevo estado, eventos emitidos y
 * decisiones pendientes. Si el efecto encontró una op de elección, el [state]
 * resultante lleva una [GameState.interaction] no nula y [pending] contiene la
 * decisión a resolver. Las ops deterministas ya quedaron aplicadas en [state].
 */
data class EffectResult(
    val state: GameState,
    val events: List<GameEvent>,
    val pending: List<PendingDecision> = emptyList(),
)

/**
 * Intérprete del DSL de efectos — Kotlin puro y determinista, **reanudable**.
 *
 * Ejecuta las operaciones del catálogo cerrado ([EffectOp]) contra un
 * [GameState] inmutable. Las ops deterministas (Damage, Heal, ApplyStatus,
 * Draw…) se aplican al vuelo. Al toparse con una op de elección
 * ([EffectOp.ChooseTarget], [EffectOp.SearchDeck], [EffectOp.MoveEnergy]) el
 * intérprete **pausa**: guarda las ops restantes como continuación en
 * [PendingInteraction] dentro del estado y emite la [PendingDecision]. Cuando el
 * motor recibe la respuesta llama a [resolve], que aplica la elección, liga
 * [Target.CHOSEN] y reanuda la continuación (que puede volver a pausarse).
 */
class EffectInterpreter {

    /**
     * Ejecuta [effect] desde el inicio. [endsTurnOnResolve] = true cuando el
     * efecto proviene de un ataque, para que el motor cierre el turno al vaciar
     * la cadena de decisiones.
     */
    fun execute(
        effect: Effect,
        source: EffectSource,
        state: GameState,
        endsTurnOnResolve: Boolean = false,
    ): EffectResult = runFrom(effect.ops, source, state, endsTurnOnResolve, chosenIds = emptyList())

    /**
     * Resuelve la [GameState.interaction] en curso con las cartas [chosen]
     * elegidas por el jugador/IA, y reanuda la continuación. [shuffle] permite
     * barajar el mazo tras una búsqueda sin acoplar el intérprete a `:rules`.
     */
    fun resolve(
        state: GameState,
        chosen: List<CardId>,
        shuffle: (List<Card>) -> List<Card>,
    ): EffectResult {
        val interaction = state.interaction ?: return EffectResult(state, emptyList())
        val src = EffectSource(interaction.side, interaction.sourceId)
        val cleared = state.copy(interaction = null)

        // 1) Aplicar la decisión concreta.
        val applied: EffectResult = when (val d = interaction.decision) {
            is PendingDecision.SearchCards -> applySearch(d, chosen, cleared, shuffle)
            is PendingDecision.MoveEnergy -> applyMoveEnergy(d, chosen, cleared)
            is PendingDecision.ChooseTargets -> EffectResult(cleared, emptyList())
        }

        // 2) Reanudar la continuación; si fue ChooseTargets, ligamos CHOSEN.
        val chosenIds = if (interaction.decision is PendingDecision.ChooseTargets) chosen else emptyList()
        val resumed = runFrom(
            interaction.remainingOps, src, applied.state,
            interaction.endsTurnOnResolve, chosenIds,
        )
        return EffectResult(resumed.state, applied.events + resumed.events, resumed.pending)
    }

    // ---------------------------------------------------------------- pasada

    /**
     * Ejecuta [ops] en orden. Al primer op de elección, guarda las restantes
     * como continuación en `state.interaction` y detiene la pasada. [chosenIds]
     * liga [Target.CHOSEN] (vacío salvo al reanudar tras un ChooseTargets).
     */
    private fun runFrom(
        ops: List<EffectOp>,
        src: EffectSource,
        state: GameState,
        endsTurnOnResolve: Boolean,
        chosenIds: List<CardId>,
    ): EffectResult {
        var working = state
        val events = mutableListOf<GameEvent>()

        for ((i, op) in ops.withIndex()) {
            val decision = pendingFor(op, src, working)
            if (decision != null) {
                val remaining = ops.drop(i + 1)
                working = working.copy(
                    interaction = PendingInteraction(
                        decision = decision,
                        remainingOps = remaining,
                        side = src.actingSide,
                        sourceId = src.sourceId,
                        endsTurnOnResolve = endsTurnOnResolve,
                    ),
                )
                return EffectResult(working, events, listOf(decision))
            }
            val step = applyOp(op, src, working, chosenIds)
            working = step.state
            events += step.events
        }
        return EffectResult(working, events)
    }

    /** Construye la decisión pendiente para una op de elección; null si es determinista. */
    private fun pendingFor(op: EffectOp, src: EffectSource, state: GameState): PendingDecision? =
        when (op) {
            is EffectOp.ChooseTarget -> PendingDecision.ChooseTargets(
                src.actingSide, op.prompt,
                candidates = targets(op.from, src, state, emptyList()).map { it.card.id },
                count = op.howMany,
            )
            is EffectOp.SearchDeck -> PendingDecision.SearchCards(
                src.actingSide,
                LocalizedText("Busca en tu mazo", "Search your deck"),
                from = Zone.DECK,
                filter = op.filter,
                destination = op.to,
                count = op.count,
                candidates = matching(state.sideState(src.actingSide).deck, op.filter),
            )
            is EffectOp.MoveEnergy -> PendingDecision.MoveEnergy(
                src.actingSide,
                LocalizedText("Mueve energía", "Move Energy"),
                fromCandidates = targets(op.from, src, state, emptyList()).map { it.card.id },
                toCandidates = targets(op.to, src, state, emptyList()).map { it.card.id },
                count = op.count,
            )
            else -> null
        }

    // ------------------------------------------------------- ops deterministas

    private fun applyOp(op: EffectOp, src: EffectSource, state: GameState, chosenIds: List<CardId>): EffectResult =
        when (op) {
            is EffectOp.Damage -> {
                val n = resolveAmount(op.amount, src, state)
                damageTargets(targets(op.target, src, state, chosenIds), n, src.actingSide, state)
            }
            is EffectOp.ExtraDamage -> {
                val n = resolveAmount(op.amount, src, state)
                damageTargets(targets(Target.OPP_ACTIVE, src, state, chosenIds), n, src.actingSide, state)
            }
            is EffectOp.Recoil -> {
                val n = resolveAmount(op.amount, src, state)
                damageTargets(targets(Target.SELF, src, state, chosenIds), n, src.actingSide, state)
            }
            is EffectOp.Heal -> {
                val n = resolveAmount(op.amount, src, state)
                healTargets(targets(op.target, src, state, chosenIds), n, src.actingSide, state)
            }
            is EffectOp.ApplyStatus -> applyStatus(op, src, state, chosenIds)
            is EffectOp.RemoveStatus -> removeStatus(op, src, state, chosenIds)
            is EffectOp.DrawCards -> draw(src.actingSide, op.count, state)
            is EffectOp.DrawUntil -> {
                val have = state.sideState(src.actingSide).hand.size
                draw(src.actingSide, (op.handSize - have).coerceAtLeast(0), state)
            }
            is EffectOp.DiscardEnergy -> discardEnergy(op, src, state, chosenIds)
            // Las ops de elección las captura runFrom/pendingFor: nunca llegan aquí.
            is EffectOp.ChooseTarget, is EffectOp.SearchDeck, is EffectOp.MoveEnergy ->
                EffectResult(state, emptyList())
        }

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

    private fun applyStatus(op: EffectOp.ApplyStatus, src: EffectSource, state: GameState, chosenIds: List<CardId>): EffectResult {
        var working = state
        val events = mutableListOf<GameEvent>()
        for (r in targets(op.target, src, state, chosenIds)) {
            working = updatePokemon(working, r.card.id) { it.copy(statuses = it.statuses + op.states) }
            op.states.forEach { events += GameEvent.StatusApplied(src.actingSide, r.card.id, it) }
        }
        return EffectResult(working, events)
    }

    private fun removeStatus(op: EffectOp.RemoveStatus, src: EffectSource, state: GameState, chosenIds: List<CardId>): EffectResult {
        var working = state
        val events = mutableListOf<GameEvent>()
        for (r in targets(op.target, src, state, chosenIds)) {
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

    private fun discardEnergy(op: EffectOp.DiscardEnergy, src: EffectSource, state: GameState, chosenIds: List<CardId>): EffectResult {
        var working = state
        val discarded = mutableListOf<Card>()
        for (r in targets(op.target, src, state, chosenIds)) {
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

    // ------------------------------------------------------- resolución de elección

    /** Mueve las cartas [chosen] del mazo a la zona destino y baraja el resto. */
    private fun applySearch(
        d: PendingDecision.SearchCards,
        chosen: List<CardId>,
        state: GameState,
        shuffle: (List<Card>) -> List<Card>,
    ): EffectResult {
        val side = d.side
        val ps = state.sideState(side)
        // Solo cartas legales (candidatas) y como mucho [count].
        val picked = chosen.filter { it in d.candidates }
            .mapNotNull { id -> ps.deck.firstOrNull { it.id == id } }
            .take(d.count)
        if (picked.isEmpty()) {
            // Aun sin elección válida, el mazo se baraja al haberlo mirado.
            val shuffled = ps.copy(deck = shuffle(ps.deck))
            return EffectResult(withPlayer(state, shuffled, side), listOf(GameEvent.DeckShuffled(side)))
        }
        val remainingDeck = shuffle(ps.deck - picked.toSet())
        var updated = ps.copy(deck = remainingDeck)
        val events = mutableListOf<GameEvent>()
        when (d.destination) {
            Zone.HAND -> updated = updated.copy(hand = updated.hand + picked)
            Zone.BENCH -> updated = updated.copy(
                bench = updated.bench + picked.filterIsInstance<PokemonCard>().map { PokemonInPlay(it) },
            )
            Zone.ACTIVE -> {
                val mon = picked.filterIsInstance<PokemonCard>().firstOrNull()
                if (mon != null && updated.active == null) updated = updated.copy(active = PokemonInPlay(mon))
                else updated = updated.copy(hand = updated.hand + picked)
            }
            Zone.DISCARD -> updated = updated.copy(discard = updated.discard + picked)
            Zone.LOST -> updated = updated.copy(lostZone = updated.lostZone + picked)
            Zone.DECK -> updated = updated.copy(deck = shuffle(remainingDeck + picked))
        }
        events += GameEvent.DeckShuffled(side)
        return EffectResult(withPlayer(state, updated, side), events)
    }

    /**
     * Mueve [count] energías del primer Pokémon de [chosen] al segundo. Convención:
     * `chosen[0]` = origen, `chosen[1]` = destino.
     */
    private fun applyMoveEnergy(
        d: PendingDecision.MoveEnergy,
        chosen: List<CardId>,
        state: GameState,
    ): EffectResult {
        val fromId = chosen.firstOrNull { it in d.fromCandidates } ?: return EffectResult(state, emptyList())
        val toId = chosen.firstOrNull { it in d.toCandidates && it != fromId } ?: return EffectResult(state, emptyList())
        val fromMon = inPlay(state, fromId) ?: return EffectResult(state, emptyList())
        val moved = fromMon.attachedEnergy.take(d.count)
        if (moved.isEmpty()) return EffectResult(state, emptyList())
        var working = updatePokemon(state, fromId) { it.copy(attachedEnergy = it.attachedEnergy - moved.toSet()) }
        working = updatePokemon(working, toId) { it.copy(attachedEnergy = it.attachedEnergy + moved) }
        return EffectResult(working, emptyList())
    }

    // ---------------------------------------------------------------- helpers

    private fun targets(target: Target, src: EffectSource, state: GameState, chosenIds: List<CardId>): List<PokemonInPlay> {
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
            Target.CHOSEN -> (me.allInPlay + foe.allInPlay).filter { it.card.id in chosenIds }
            else -> emptyList()           // zonas: no son objetivos Pokémon
        }
    }

    private fun resolveAmount(amount: Amount, src: EffectSource, state: GameState): Int = when (amount) {
        is Amount.Fixed -> amount.n
        is Amount.PerCount -> {
            val refs = targets(amount.target, src, state, emptyList())
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

    private fun matching(cards: List<Card>, filter: CardFilter): List<CardId> =
        cards.filter { c ->
            (filter.supertype == null || c.supertype == filter.supertype) &&
                (filter.isBasic == null || (c is PokemonCard && c.isBasic == filter.isBasic)) &&
                (filter.type == null || (c is PokemonCard && filter.type in c.types)) &&
                (filter.nameContains == null || c.name.es.contains(filter.nameContains!!, true) || c.name.en.contains(filter.nameContains!!, true))
        }.map { it.id }

    private fun Side.other(): Side = if (this == Side.PLAYER) Side.OPPONENT else Side.PLAYER

    private fun withPlayer(state: GameState, ps: PlayerState, side: Side): GameState =
        if (side == Side.PLAYER) state.copy(player = ps) else state.copy(opponent = ps)

    private fun inPlay(state: GameState, id: CardId): PokemonInPlay? =
        (state.player.allInPlay + state.opponent.allInPlay).firstOrNull { it.card.id == id }

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
