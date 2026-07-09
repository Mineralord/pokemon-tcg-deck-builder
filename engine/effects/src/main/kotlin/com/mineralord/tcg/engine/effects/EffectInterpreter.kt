package com.mineralord.tcg.engine.effects

import com.mineralord.tcg.engine.events.GameEvent
import com.mineralord.tcg.engine.model.Amount
import com.mineralord.tcg.engine.model.BasicEnergy
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
        shuffle: (List<Card>) -> List<Card> = { it },
        flip: () -> Boolean = { true },
    ): EffectResult = runFrom(effect.ops, source, state, endsTurnOnResolve, emptyList(), shuffle, flip)

    /**
     * Resuelve la [GameState.interaction] en curso con las cartas [chosen]
     * elegidas por el jugador/IA, y reanuda la continuación. [shuffle] permite
     * barajar el mazo tras una búsqueda sin acoplar el intérprete a `:rules`.
     */
    fun resolve(
        state: GameState,
        chosen: List<CardId>,
        flip: () -> Boolean = { true },
        shuffle: (List<Card>) -> List<Card>,
    ): EffectResult {
        val interaction = state.interaction ?: return EffectResult(state, emptyList())
        val src = EffectSource(interaction.side, interaction.sourceId)
        val cleared = state.copy(interaction = null)

        // 1) Aplicar la decisión concreta.
        val applied: EffectResult = when (val d = interaction.decision) {
            is PendingDecision.SearchCards -> applySearch(d, chosen, cleared, shuffle)
            is PendingDecision.MoveEnergy -> applyMoveEnergy(d, chosen, cleared)
            is PendingDecision.AttachFromRevealed -> applyAttachFromRevealed(d, chosen, cleared, shuffle)
            is PendingDecision.ChooseTargets -> EffectResult(cleared, emptyList())
            is PendingDecision.CoinFlip -> {
                // El jugador ya "tiró" la moneda: ahora el motor la lanza (autoritativo),
                // emite el evento para la animación y roba según el resultado.
                val heads = flip()
                val drawn = draw(src.actingSide, if (heads) d.ifHeads else d.ifTails, cleared)
                EffectResult(drawn.state, listOf(GameEvent.CoinFlipped(src.actingSide, heads)) + drawn.events)
            }
        }

        // 2) Reanudar la continuación; si fue ChooseTargets, ligamos CHOSEN.
        val chosenIds = if (interaction.decision is PendingDecision.ChooseTargets) chosen else emptyList()
        val resumed = runFrom(
            interaction.remainingOps, src, applied.state,
            interaction.endsTurnOnResolve, chosenIds, shuffle, flip,
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
        shuffle: (List<Card>) -> List<Card>,
        flip: () -> Boolean,
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
            val step = applyOp(op, src, working, chosenIds, shuffle, flip)
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
                candidates = targets(op.from, src, state, emptyList())
                    .filter { !op.onlyDamaged || it.damage > 0 }
                    .map { it.card.id },
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
            is EffectOp.CoinFlipDraw -> PendingDecision.CoinFlip(
                src.actingSide,
                LocalizedText("Lanza la moneda", "Flip a coin"),
                ifHeads = op.ifHeads,
                ifTails = op.ifTails,
            )
            is EffectOp.MoveEnergy -> PendingDecision.MoveEnergy(
                src.actingSide,
                LocalizedText("Mueve energía", "Move Energy"),
                fromCandidates = targets(op.from, src, state, emptyList()).map { it.card.id },
                toCandidates = targets(op.to, src, state, emptyList()).map { it.card.id },
                count = op.count,
            )
            is EffectOp.RevealAttachEnergy -> {
                val ps = state.sideState(src.actingSide)
                val revealed = ps.deck.take(op.lookAt)
                val energies = revealed.filter { it is BasicEnergy && it.type == op.energyType }.map { it.id }
                val benched = ps.bench.filter { op.benchType == null || op.benchType in it.card.types }.map { it.card.id }
                // Solo se pausa si HAY algo que unir; si no, es determinista (barajar).
                if (energies.isEmpty() || benched.isEmpty()) null
                else PendingDecision.AttachFromRevealed(
                    src.actingSide,
                    LocalizedText(
                        "Une hasta ${op.maxAttach} Energía a tus Pokémon de Banca",
                        "Attach up to ${op.maxAttach} Energy to your Benched Pokémon",
                    ),
                    revealed = revealed.map { it.id },
                    energyCandidates = energies,
                    benchCandidates = benched,
                    maxAttach = op.maxAttach,
                )
            }
            is EffectOp.AttachEnergyFromDiscard -> {
                // Interactivo solo cuando el destino es "cualquiera de los tuyos" (OWN_ALL);
                // SELF/OWN_ACTIVE se resuelven deterministas en applyOp.
                if (op.target != Target.OWN_ALL) null
                else {
                    val ps = state.sideState(src.actingSide)
                    val energies = ps.discard
                        .filter { it is BasicEnergy && (op.energyType == null || it.type == op.energyType) }
                        .map { it.id }
                    val targets = ps.allInPlay.map { it.card.id }
                    if (energies.isEmpty() || targets.isEmpty()) null
                    else PendingDecision.AttachFromRevealed(
                        src.actingSide,
                        LocalizedText(
                            "Une hasta ${op.count} Energía del descarte a tus Pokémon",
                            "Attach up to ${op.count} Energy from your discard to your Pokémon",
                        ),
                        revealed = energies,
                        energyCandidates = energies,
                        benchCandidates = targets,
                        maxAttach = op.count,
                        fromDiscard = true,
                        thenDrawUpTo = op.thenDrawUpTo,
                    )
                }
            }
            else -> null
        }

    // ------------------------------------------------------- ops deterministas

    private fun applyOp(
        op: EffectOp,
        src: EffectSource,
        state: GameState,
        chosenIds: List<CardId>,
        shuffle: (List<Card>) -> List<Card>,
        flip: () -> Boolean,
    ): EffectResult =
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
            is EffectOp.ShuffleHandIntoDeck -> {
                val ps = state.sideState(src.actingSide)
                if (ps.hand.isEmpty()) EffectResult(state, emptyList())
                else {
                    val updated = ps.copy(hand = emptyList(), deck = shuffle(ps.deck + ps.hand))
                    EffectResult(withPlayer(state, updated, src.actingSide), listOf(GameEvent.DeckShuffled(src.actingSide)))
                }
            }
            is EffectOp.SwapActiveWithChosen -> {
                val ps = state.sideState(src.actingSide)
                val chosenId = chosenIds.firstOrNull()
                val benchMon = ps.bench.firstOrNull { it.card.id == chosenId }
                val active = ps.active
                if (benchMon == null || active == null) EffectResult(state, emptyList())
                else {
                    val updated = ps.copy(
                        active = benchMon,
                        bench = ps.bench.map { if (it.card.id == chosenId) active else it },
                    )
                    EffectResult(withPlayer(state, updated, src.actingSide), emptyList())
                }
            }
            is EffectOp.SwapOppActiveWithChosen -> {
                // Órdenes de Jefe (gust): sube al Activo rival el elegido de SU Banca.
                val foeSide = src.actingSide.other()
                val foe = state.sideState(foeSide)
                val chosenId = chosenIds.firstOrNull()
                val benchMon = foe.bench.firstOrNull { it.card.id == chosenId }
                val active = foe.active
                if (benchMon == null || active == null) EffectResult(state, emptyList())
                else {
                    val updated = foe.copy(
                        active = benchMon,
                        bench = foe.bench.map { if (it.card.id == chosenId) active else it },
                    )
                    EffectResult(withPlayer(state, updated, foeSide), emptyList())
                }
            }
            is EffectOp.CoinFlipDraw -> {
                // Lanza la moneda EMITIENDO el evento (para que la UI anime el giro) y
                // luego roba según el resultado, continuando el efecto (Dominguera/Picnicker).
                val heads = flip()
                val drawn = draw(src.actingSide, if (heads) op.ifHeads else op.ifTails, state)
                EffectResult(
                    drawn.state,
                    listOf(GameEvent.CoinFlipped(src.actingSide, heads)) + drawn.events,
                )
            }
            is EffectOp.NoAttackNextTurn -> {
                // Marca al Pokémon origen: no podrá atacar en su próximo turno propio
                // (turn actual + 2; el turno intermedio es del rival).
                val id = src.sourceId
                if (id == null) EffectResult(state, emptyList())
                else EffectResult(
                    updatePokemon(state, id) { it.copy(cannotAttackOnTurn = state.turn + 2) },
                    emptyList(),
                )
            }
            is EffectOp.RevealAttachEnergy -> {
                // Solo llega aquí si pendingFor devolvió null (nada que unir): se miró el
                // top del mazo y se baraja de vuelta.
                val ps = state.sideState(src.actingSide)
                EffectResult(
                    withPlayer(state, ps.copy(deck = shuffle(ps.deck)), src.actingSide),
                    listOf(GameEvent.DeckShuffled(src.actingSide)),
                )
            }
            is EffectOp.CoinsPerEnergyDamage -> {
                // Lanza una moneda por cada Energía [energyType] del origen; el daño al
                // Activo rival es damagePerHeads × caras. Emite un CoinFlipped por tirada.
                val self = targets(Target.SELF, src, state, chosenIds).firstOrNull()
                if (self == null) EffectResult(state, emptyList())
                else {
                    val flips = self.attachedEnergy.count { (it as? BasicEnergy)?.type == op.energyType }
                    val events = mutableListOf<GameEvent>()
                    var heads = 0
                    repeat(flips) {
                        val h = flip()
                        if (h) heads++
                        events += GameEvent.CoinFlipped(src.actingSide, h)
                    }
                    val dmg = damageTargets(targets(Target.OPP_ACTIVE, src, state, chosenIds), heads * op.damagePerHeads, src.actingSide, state)
                    EffectResult(dmg.state, events + dmg.events)
                }
            }
            is EffectOp.AttachEnergyFromDiscard -> {
                // Ruta determinista (SELF/OWN_ACTIVE): une las primeras [count] Energías
                // Básicas de [energyType] del descarte al Pokémon objetivo. La ruta OWN_ALL
                // se captura en pendingFor como decisión de arrastre.
                val ps = state.sideState(src.actingSide)
                val targetMon = targets(op.target, src, state, chosenIds).firstOrNull()
                val picked = ps.discard
                    .filter { it is BasicEnergy && (op.energyType == null || it.type == op.energyType) }
                    .take(op.count)
                    .filterIsInstance<EnergyCard>()
                if (targetMon == null || picked.isEmpty()) EffectResult(state, emptyList())
                else {
                    var working = updatePokemon(state, targetMon.card.id) {
                        it.copy(attachedEnergy = it.attachedEnergy + picked)
                    }
                    working = withPlayer(
                        working,
                        working.sideState(src.actingSide).copy(discard = ps.discard - picked.toSet()),
                        src.actingSide,
                    )
                    val events: MutableList<GameEvent> =
                        picked.mapTo(mutableListOf()) { GameEvent.EnergyAttached(src.actingSide, it.id, targetMon.card.id) }
                    // "Si lo haces, roba hasta N" — atómico: solo porque se unió Energía.
                    val drawUpTo = op.thenDrawUpTo
                    if (drawUpTo != null) {
                        val have = working.sideState(src.actingSide).hand.size
                        val drawn = draw(src.actingSide, (drawUpTo - have).coerceAtLeast(0), working)
                        working = drawn.state
                        events += drawn.events
                    }
                    EffectResult(working, events)
                }
            }
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

    /**
     * Une las energías reveladas elegidas a los Pokémon de Banca indicados. [chosen]
     * viene como PARES intercalados `[energía, destino, …]`. Las energías unidas salen
     * del mazo; el resto (incluido lo mirado y no unido) se baraja de vuelta.
     */
    private fun applyAttachFromRevealed(
        d: PendingDecision.AttachFromRevealed,
        chosen: List<CardId>,
        state: GameState,
        shuffle: (List<Card>) -> List<Card>,
    ): EffectResult {
        val side = d.side
        val ps = state.sideState(side)
        // Origen de las energías: mazo (Generador Eléctrico) o descarte (Passionate Singing).
        val source = if (d.fromDiscard) ps.discard else ps.deck
        var working = state
        val events = mutableListOf<GameEvent>()
        val attached = mutableListOf<Card>()
        // Recorre pares (energía, destino) respetando el tope y sin reusar energías.
        var i = 0
        while (i + 1 < chosen.size && attached.size < d.maxAttach) {
            val energyId = chosen[i]
            val benchId = chosen[i + 1]
            i += 2
            if (energyId !in d.energyCandidates || benchId !in d.benchCandidates) continue
            if (attached.any { it.id == energyId }) continue
            val energy = source.firstOrNull { it.id == energyId } as? EnergyCard ?: continue
            working = updatePokemon(working, benchId) {
                it.copy(attachedEnergy = it.attachedEnergy + energy)
            }
            attached += energy
            events += GameEvent.EnergyAttached(side, energyId, benchId)
        }
        if (d.fromDiscard) {
            // Las energías unidas salen del descarte; el resto se queda (no se baraja).
            val newDiscard = ps.discard - attached.toSet()
            working = withPlayer(working, working.sideState(side).copy(discard = newDiscard), side)
            // "Si lo haces, roba hasta N" (Mela): atómico, solo si se unió Energía.
            val drawUpTo = d.thenDrawUpTo
            if (drawUpTo != null && attached.isNotEmpty()) {
                val have = working.sideState(side).hand.size
                val drawn = draw(side, (drawUpTo - have).coerceAtLeast(0), working)
                working = drawn.state
                events += drawn.events
            }
        } else {
            // El mazo mirado se baraja de vuelta, ya sin las energías unidas.
            val newDeck = shuffle(ps.deck - attached.toSet())
            working = withPlayer(working, working.sideState(side).copy(deck = newDeck), side)
            events += GameEvent.DeckShuffled(side)
        }
        return EffectResult(working, events)
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
