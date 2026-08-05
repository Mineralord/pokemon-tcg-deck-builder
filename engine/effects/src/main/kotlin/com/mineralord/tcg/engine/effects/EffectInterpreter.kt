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
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.PendingInteraction
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.Supertype
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import com.mineralord.tcg.engine.model.TrainerCategory
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

        // Caso especial: moneda→búsqueda (Parasect). Aquí SÍ tenemos flip(): lanzamos las
        // monedas, contamos caras y ENCADENAMOS una SearchCards a la Banca con count=caras.
        (interaction.decision as? PendingDecision.CoinFlipThenSearch)?.let { d ->
            val results = (1..d.flips).map { flip() }
            val heads = results.count { it }
            val flipEvents = results.map { GameEvent.CoinFlipped(src.actingSide, it) }
            val cands = matching(cleared.sideState(src.actingSide).deck, d.filter)
            if (heads == 0 || cands.isEmpty()) {
                // Sin caras (o nada que buscar): reanuda la continuación sin buscar.
                val resumed = runFrom(
                    interaction.remainingOps, src, cleared,
                    interaction.endsTurnOnResolve, emptyList(), shuffle, flip,
                )
                return EffectResult(resumed.state, flipEvents + resumed.events, resumed.pending)
            }
            val search = PendingDecision.SearchCards(
                src.actingSide, d.searchPrompt,
                from = Zone.DECK, filter = d.filter, destination = Zone.BENCH,
                count = heads, candidates = cands,
            )
            val chained = cleared.copy(
                interaction = PendingInteraction(
                    decision = search,
                    remainingOps = interaction.remainingOps,
                    side = src.actingSide,
                    sourceId = interaction.sourceId,
                    endsTurnOnResolve = interaction.endsTurnOnResolve,
                ),
            )
            return EffectResult(chained, flipEvents, listOf(search))
        }

        // 1) Aplicar la decisión concreta.
        val applied: EffectResult = when (val d = interaction.decision) {
            is PendingDecision.SearchCards ->
                if (d.fromOpponentHand) applyOpponentHandToBottom(d, chosen, cleared)
                else applySearch(d, chosen, cleared, shuffle)
            is PendingDecision.MoveEnergy -> applyMoveEnergy(d, chosen, cleared)
            is PendingDecision.PlaceCounters -> applyPlaceCounters(d, chosen, cleared)
            is PendingDecision.AttachFromRevealed -> applyAttachFromRevealed(d, chosen, cleared, shuffle, flip)
            is PendingDecision.ChooseTargets -> EffectResult(cleared, emptyList())
            // Hackeo Genómico: la resuelve GameEngine (re-ejecuta el ataque); nunca llega aquí.
            is PendingDecision.ChooseAttack -> EffectResult(cleared, emptyList())
            is PendingDecision.ChooseEnergyType -> {
                // Porygon — Conversión 4: fija el TIPO de Debilidad-override en el Activo rival.
                val type = chosen.firstNotNullOfOrNull { PendingDecision.decodeType(it) }
                val foeSide = src.actingSide.other()
                val foeActive = cleared.sideState(foeSide).active
                if (type == null || foeActive == null) EffectResult(cleared, emptyList())
                else EffectResult(
                    updatePokemon(cleared, foeActive.card.id) { it.copy(weaknessOverrideType = type) },
                    emptyList(),
                )
            }
            // Ya manejada arriba con early-return; rama inalcanzable para exhaustividad.
            is PendingDecision.CoinFlipThenSearch -> EffectResult(cleared, emptyList())
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
            is EffectOp.ChooseTarget -> {
                val cands = targets(op.from, src, state, emptyList())
                    .filter { !op.onlyDamaged || it.damage > 0 }
                    .map { it.card.id }
                // Elección ADITIVA de un ataque (optional) sin candidatos → saltar la op:
                // el ataque ya hizo su daño base, solo se omite la parte dirigida.
                if (cands.isEmpty() && op.optional) null
                else PendingDecision.ChooseTargets(src.actingSide, op.prompt, cands, op.howMany)
            }
            is EffectOp.OverrideDefenderWeaknessType -> {
                // Porygon — Conversión 4: elige 1 tipo (los 9 del texto). Solo pausa si hay Activo rival.
                if (state.sideState(src.actingSide.other()).active == null) null
                else PendingDecision.ChooseEnergyType(
                    src.actingSide,
                    LocalizedText(
                        "Elige el nuevo tipo de Debilidad del Pokémon Defensor",
                        "Choose the Defending Pokémon's new Weakness type",
                    ),
                    candidates = listOf(
                        EnergyType.GRASS, EnergyType.FIRE, EnergyType.WATER, EnergyType.LIGHTNING,
                        EnergyType.PSYCHIC, EnergyType.FIGHTING, EnergyType.DARKNESS, EnergyType.METAL,
                        EnergyType.DRAGON,
                    ),
                )
            }
            is EffectOp.PlaceCounters -> {
                val cands = targets(op.target, src, state, emptyList()).map { it.card.id }
                // Sin Pokémon donde repartir: no pausa (el ataque no congela el turno).
                if (cands.isEmpty()) null
                else PendingDecision.PlaceCounters(
                    src.actingSide,
                    LocalizedText(
                        "Reparte ${op.count} contadores de daño entre los Pokémon del rival",
                        "Put ${op.count} damage counters on your opponent's Pokémon in any way you like",
                    ),
                    cands, op.count,
                )
            }
            is EffectOp.SearchDeck -> {
                // fromTop != null: solo se miran las N primeras cartas (Transferencia de Bill).
                val deck = state.sideState(src.actingSide).deck
                val pool = op.fromTop?.let { deck.take(it) } ?: deck
                PendingDecision.SearchCards(
                    src.actingSide,
                    if (op.fromTop != null)
                        LocalizedText("Mira las ${op.fromTop} primeras cartas de tu mazo", "Look at the top ${op.fromTop} cards of your deck")
                    else LocalizedText("Busca en tu mazo", "Search your deck"),
                    from = Zone.DECK,
                    filter = op.filter,
                    destination = op.to,
                    count = op.count,
                    candidates = matching(pool, op.filter),
                    distinctTypes = op.distinctTypes,
                )
            }
            is EffectOp.DiscardFromHandForDamage -> {
                val cands = matching(state.sideState(src.actingSide).hand, op.filter)
                if (cands.isEmpty()) null   // sin cartas que descartar: 0 descartadas = 0 daño
                else PendingDecision.SearchCards(
                    src.actingSide,
                    LocalizedText("Descarta cartas de tu mano", "Discard cards from your hand"),
                    from = Zone.HAND,
                    filter = op.filter,
                    destination = Zone.DISCARD,
                    count = op.maxCount,
                    candidates = cands,
                    damagePerDiscardToOppActive = op.perCard,
                )
            }
            is EffectOp.DiscardOwnToolsForDamage -> {
                // Candidatos = Herramientas enganchadas a tus Pokémon (Activo + Banca).
                val tools = state.sideState(src.actingSide).allInPlay.flatMap { it.attachedTools }
                if (tools.isEmpty()) null   // sin Herramientas: 0 descartadas = 0 bonus
                else PendingDecision.SearchCards(
                    src.actingSide,
                    LocalizedText(
                        "Descarta Herramientas de tus Pokémon (+${op.perCard} daño por cada una)",
                        "Discard Pokémon Tools from your Pokémon (+${op.perCard} damage each)",
                    ),
                    from = Zone.DISCARD,          // ignorado (fromAttachedTools = true)
                    filter = CardFilter(),
                    destination = Zone.DISCARD,
                    count = tools.size,
                    candidates = tools.map { it.id },
                    damagePerDiscardToOppActive = op.perCard,
                    fromAttachedTools = true,
                )
            }
            is EffectOp.CoinFlipSearchToBench -> PendingDecision.CoinFlipThenSearch(
                src.actingSide,
                LocalizedText("Lanza ${op.flips} monedas", "Flip ${op.flips} coins"),
                flips = op.flips,
                filter = op.filter,
                searchPrompt = LocalizedText(
                    "Busca Pokémon en tu mazo para tu Banca",
                    "Search your deck for Pokémon to put on your Bench",
                ),
            )
            is EffectOp.RecoverFromDiscard -> {
                val cands = matching(state.sideState(src.actingSide).discard, op.filter)
                if (cands.isEmpty()) null   // nada que recuperar: no pausa
                else PendingDecision.SearchCards(
                    src.actingSide,
                    LocalizedText("Elige cartas de tu pila de descartes", "Choose cards from your discard pile"),
                    from = Zone.DISCARD,
                    filter = op.filter,
                    destination = Zone.HAND,
                    count = op.count,
                    candidates = cands,
                )
            }
            is EffectOp.RecoverOppFromDiscard -> {
                // Haunter — Regreso Espiritual: candidatos = cartas del descarte RIVAL que casan el
                // filtro (Partidario). Sin ninguna, no pausa. Van a la MANO del rival (onOpponent).
                val foe = state.sideState(src.actingSide.other())
                val cands = matching(foe.discard, op.filter)
                if (cands.isEmpty()) null
                else PendingDecision.SearchCards(
                    src.actingSide,
                    LocalizedText(
                        "Puedes poner una carta de Partidario del descarte de tu rival en su mano",
                        "You may put a Supporter card from your opponent's discard pile into their hand",
                    ),
                    from = Zone.DISCARD,
                    filter = op.filter,
                    destination = Zone.HAND,
                    count = op.count,
                    candidates = cands,
                    onOpponent = true,
                )
            }
            is EffectOp.PutOppHandPokemonToBottomOfDeck -> {
                // Agarrador Mecánico: candidatos = Pokémon en la MANO del rival. Sin ninguno, no pausa.
                val foe = state.sideState(src.actingSide.other())
                val cands = foe.hand.filter { it.supertype == Supertype.POKEMON }.map { it.id }
                if (cands.isEmpty()) null
                else PendingDecision.SearchCards(
                    src.actingSide,
                    LocalizedText(
                        "Elige un Pokémon de la mano de tu rival para ponerlo en el fondo de su baraja",
                        "Choose a Pokémon from your opponent's hand to put on the bottom of their deck",
                    ),
                    from = Zone.HAND,
                    filter = CardFilter(supertype = Supertype.POKEMON),
                    destination = Zone.DECK,
                    count = 1,
                    candidates = cands,
                    fromOpponentHand = true,
                )
            }
            is EffectOp.TransformIntoBasicFromDeck -> {
                // Ditto — Inicio Transformador: candidatos = Pokémon Básicos del mazo que casan
                // el filtro (excepto Ditto). Es "puedes": sin candidatos no pausa (no hace nada).
                val cands = matching(state.sideState(src.actingSide).deck, op.filter)
                if (cands.isEmpty()) null
                else PendingDecision.SearchCards(
                    src.actingSide,
                    LocalizedText(
                        "Elige un Pokémon Básico en el que transformar a Ditto",
                        "Choose a Basic Pokémon to transform Ditto into",
                    ),
                    from = Zone.DECK,
                    filter = op.filter,
                    destination = Zone.ACTIVE,
                    count = 1,
                    candidates = cands,
                    replaceActiveWithSource = true,
                )
            }
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
                val energies = revealed
                    .filter { it is BasicEnergy && (op.energyType == null || it.type == op.energyType) }
                    .map { it.id }
                val pool = if (op.includeActive) ps.allInPlay else ps.bench
                val benched = pool.filter { op.benchType == null || op.benchType in it.card.types }.map { it.card.id }
                // Solo se pausa si HAY algo que unir; si no, es determinista (barajar).
                if (energies.isEmpty() || benched.isEmpty()) null
                else PendingDecision.AttachFromRevealed(
                    src.actingSide,
                    LocalizedText(
                        "Une hasta ${op.maxAttach} Energía a tus Pokémon",
                        "Attach up to ${op.maxAttach} Energy to your Pokémon",
                    ),
                    revealed = revealed.map { it.id },
                    energyCandidates = energies,
                    benchCandidates = benched,
                    maxAttach = op.maxAttach,
                )
            }
            is EffectOp.AttachEnergyFromDiscard -> {
                // Interactivo cuando el destino es "cualquiera de los tuyos" (OWN_ALL) o "tu
                // Banca" (OWN_BENCH, Scyther — Tajo Útil); SELF/OWN_ACTIVE deterministas en applyOp.
                if (op.target != Target.OWN_ALL && op.target != Target.OWN_BENCH) null
                else {
                    val ps = state.sideState(src.actingSide)
                    val energies = ps.discard
                        .filter { it is BasicEnergy && (op.energyType == null || it.type == op.energyType) }
                        .map { it.id }
                    val targets =
                        (if (op.target == Target.OWN_BENCH) ps.bench else ps.allInPlay).map { it.card.id }
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
                        coinFlip = op.coinFlip,
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
            is EffectOp.ApplyStatusIfEmptyHand ->
                if (state.sideState(src.actingSide).hand.isEmpty())
                    applyStatus(EffectOp.ApplyStatus(op.target, op.states), src, state, chosenIds)
                else EffectResult(state, emptyList())
            is EffectOp.RemoveStatus -> removeStatus(op, src, state, chosenIds)
            is EffectOp.KoIfStatus -> {
                // KO directo si el objetivo tiene el estado: daño crudo = sus PS impresos.
                val hit = targets(op.target, src, state, chosenIds)
                    .filter { op.status in it.statuses }
                if (hit.isEmpty()) EffectResult(state, emptyList())
                else {
                    var working = state
                    val events = mutableListOf<GameEvent>()
                    for (r in hit) {
                        val hp = (r.card as? PokemonCard)?.hp ?: continue
                        val res = damageTargets(listOf(r), hp, src.actingSide, working)
                        working = res.state; events += res.events
                    }
                    EffectResult(working, events)
                }
            }
            is EffectOp.RevealOpponentHand -> {
                // Zubat — Eco Revelador: el rival enseña su mano. Informativo (no cambia estado).
                val foeSide = src.actingSide.other()
                EffectResult(state, listOf(GameEvent.HandRevealed(foeSide, state.sideState(foeSide).hand.size)))
            }
            is EffectOp.DrawCards -> draw(src.actingSide, op.count, state)
            is EffectOp.DrawUntil -> {
                val have = state.sideState(src.actingSide).hand.size
                draw(src.actingSide, (op.handSize - have).coerceAtLeast(0), state)
            }
            is EffectOp.DiscardEnergy -> discardEnergy(op, src, state, chosenIds, flip)
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
            is EffectOp.CoinFlipSwapOppActiveWithChosen -> {
                // Meowth — Ven Aquí Ya: lanza la moneda (emite el evento para la animación);
                // solo con cara sube al Activo rival el elegido de SU Banca.
                val heads = flip()
                val ev = listOf(GameEvent.CoinFlipped(src.actingSide, heads))
                if (!heads) EffectResult(state, ev)
                else {
                    val foeSide = src.actingSide.other()
                    val foe = state.sideState(foeSide)
                    val chosenId = chosenIds.firstOrNull()
                    val benchMon = foe.bench.firstOrNull { it.card.id == chosenId }
                    val active = foe.active
                    if (benchMon == null || active == null) EffectResult(state, ev)
                    else {
                        val updated = foe.copy(
                            active = benchMon,
                            bench = foe.bench.map { if (it.card.id == chosenId) active else it },
                        )
                        EffectResult(withPlayer(state, updated, foeSide), ev)
                    }
                }
            }
            is EffectOp.GustDefenderChooseNewActive -> {
                // Remolino/Oprimir: el Activo rival pasa a su Banca y el RIVAL elige el nuevo
                // Activo (reusa `pendingPromotion`). Si ya quedó Noqueado por el daño de este
                // ataque, no se toca (lo procesa handleKnockouts).
                val foeSide = src.actingSide.other()
                val foe = state.sideState(foeSide)
                val active = foe.active
                if (active == null || active.isKnockedOut) EffectResult(state, emptyList())
                else {
                    // Deja el Puesto Activo → expira el override de Debilidad (Porygon).
                    val updated = foe.copy(active = null, bench = foe.bench + active.copy(weaknessOverrideType = null))
                    val moved = withPlayer(state, updated, foeSide)
                    EffectResult(moved.copy(pendingPromotion = moved.pendingPromotion + foeSide), emptyList())
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
            is EffectOp.CoinFlipDamage -> {
                // Lanza [flips] moneda(s); daño extra = damagePerHeads × caras al Activo rival.
                val events = mutableListOf<GameEvent>()
                var heads = 0
                repeat(op.flips) {
                    val h = flip()
                    if (h) heads++
                    events += GameEvent.CoinFlipped(src.actingSide, h)
                }
                val dmg = damageTargets(targets(Target.OPP_ACTIVE, src, state, chosenIds), heads * op.damagePerHeads, src.actingSide, state)
                EffectResult(dmg.state, events + dmg.events)
            }
            is EffectOp.CoinFlipDamageOrRecoil -> {
                // Mankey — Saña: cara → +daño al rival; cruz → daño de rebote a sí mismo.
                val h = flip()
                val evFlip = GameEvent.CoinFlipped(src.actingSide, h)
                val res = if (h)
                    damageTargets(targets(Target.OPP_ACTIVE, src, state, chosenIds), op.bonusToOpp, src.actingSide, state)
                else
                    damageTargets(targets(Target.SELF, src, state, chosenIds), op.recoilToSelf, src.actingSide, state)
                EffectResult(res.state, listOf(evFlip) + res.events)
            }
            is EffectOp.CoinFlipStatus -> {
                // Lanza 1 moneda; si sale cara, aplica los estados a [target].
                val h = flip()
                val evFlip = GameEvent.CoinFlipped(src.actingSide, h)
                if (!h) EffectResult(state, listOf(evFlip))
                else {
                    val applied = applyStatus(EffectOp.ApplyStatus(op.target, op.states), src, state, chosenIds)
                    EffectResult(applied.state, listOf(evFlip) + applied.events)
                }
            }
            is EffectOp.OpponentDiscardsHand -> {
                // El rival descarta [count] cartas desde el frente de su mano.
                val foeSide = src.actingSide.other()
                val foe = state.sideState(foeSide)
                val discarded = foe.hand.take(op.count)
                if (discarded.isEmpty()) EffectResult(state, emptyList())
                else {
                    val updated = foe.copy(hand = foe.hand.drop(discarded.size), discard = foe.discard + discarded)
                    EffectResult(withPlayer(state, updated, foeSide), listOf(GameEvent.CardsDiscarded(foeSide, discarded.size)))
                }
            }
            is EffectOp.PreventDamageNextTurn -> {
                // Si es por moneda, solo surte efecto con cara. Marca al objetivo para
                // evitar daño de ataques durante el próximo turno (turn + 1).
                val events = mutableListOf<GameEvent>()
                val ok = if (op.coinFlip) {
                    val h = flip()
                    events += GameEvent.CoinFlipped(src.actingSide, h)
                    h
                } else true
                if (!ok) EffectResult(state, events)
                else {
                    var working = state
                    for (r in targets(op.target, src, state, chosenIds)) {
                        working = updatePokemon(working, r.card.id) {
                            if (op.onlyFromBasic) it.copy(preventBasicDamageOnTurn = state.turn + 1)
                            else it.copy(preventDamageOnTurn = state.turn + 1)
                        }
                    }
                    EffectResult(working, events)
                }
            }
            is EffectOp.RequireCoinsToAttackNextTurn -> {
                // Marca al objetivo (Seadra: el Activo rival) para tener que lanzar N monedas
                // si intenta atacar el próximo turno (turn + 1). Lo comprueba GameEngine.attack.
                var working = state
                for (r in targets(op.target, src, state, chosenIds)) {
                    working = updatePokemon(working, r.card.id) {
                        it.copy(flipsToAttackOnTurn = state.turn + 1, flipsToAttackCount = op.coins)
                    }
                }
                EffectResult(working, emptyList())
            }
            is EffectOp.ForceOpponentCoinsTailsNextTurn -> {
                // Marca al RIVAL del que ataca: durante su próximo turno (turn + 1) sus monedas
                // se considerarán cruz. Lo aplica el flip gateado de GameEngine.
                EffectResult(
                    state.copy(
                        coinsAsTailsSide = src.actingSide.other(),
                        coinsAsTailsOnTurn = state.turn + 1,
                    ),
                    emptyList(),
                )
            }
            is EffectOp.ScheduleDelayedDamage -> {
                // Marca a los objetivos (Victreebel: el Activo rival) para recibir daño diferido
                // al final de su próximo turno (turn + 1). Lo aplica GameEngine.endTurn.
                var working = state
                for (r in targets(op.target, src, state, chosenIds)) {
                    working = updatePokemon(working, r.card.id) {
                        it.copy(delayedDamageOnTurn = state.turn + 1, delayedDamageAmount = op.amount)
                    }
                }
                EffectResult(working, emptyList())
            }
            is EffectOp.OverrideDefenderWeaknessType ->
                // Interactiva: se resuelve vía la decisión ChooseEnergyType. Solo llega aquí si
                // no había Activo rival al que aplicar (pendingFor devolvió null) → no-op.
                EffectResult(state, emptyList())
            is EffectOp.DeEvolveDefender -> {
                // Involuciona el Activo rival: la carta de fase más alta (la actual) vuelve a la
                // mano de su dueño; el Pokémon pasa a ser la carta inferior de la pila de evolución.
                val foeSide = src.actingSide.other()
                val foe = state.sideState(foeSide)
                val defender = foe.active
                val topCard = defender?.card
                val below = defender?.evolutionStack?.lastOrNull()
                if (defender == null || topCard == null || below == null) EffectResult(state, emptyList())
                else {
                    var working = updatePokemon(state, defender.card.id) {
                        it.copy(card = below, evolutionStack = it.evolutionStack.dropLast(1))
                    }
                    working = withPlayer(working, working.sideState(foeSide).copy(hand = foe.hand + topCard), foeSide)
                    EffectResult(working, listOf(GameEvent.DeEvolved(foeSide, topCard.id, below.id)))
                }
            }
            is EffectOp.ScheduleReflectDamageNextTurn -> {
                // Marca al Pokémon que usa el ataque (Activo propio) para reflejar el daño
                // recibido durante el próximo turno del rival (turn + 1).
                val self = state.sideState(src.actingSide).active
                if (self == null) EffectResult(state, emptyList())
                else EffectResult(
                    updatePokemon(state, self.card.id) { it.copy(reflectDamageOnTurn = state.turn + 1) },
                    emptyList(),
                )
            }
            is EffectOp.CoinFlipKnockOutTarget -> {
                val events = mutableListOf<GameEvent>()
                val ok = if (op.coinFlip) {
                    val h = flip()
                    events += GameEvent.CoinFlipped(src.actingSide, h)
                    h
                } else true
                if (!ok) EffectResult(state, events)
                else {
                    // KO = daño crudo igual a los PS impresos del objetivo (mismo patrón que KoIfStatus).
                    var working = state
                    for (r in targets(op.target, src, state, chosenIds)) {
                        val hp = (r.card as? PokemonCard)?.hp ?: continue
                        val res = damageTargets(listOf(r), hp, src.actingSide, working)
                        working = res.state; events += res.events
                    }
                    EffectResult(working, events)
                }
            }
            is EffectOp.DiscardStadium -> {
                // Descarta el Estadio en juego a la pila de su dueño (no-op si no hay).
                val stadium = state.stadium
                val owner = state.stadiumOwner
                if (stadium == null || owner == null) EffectResult(state, emptyList())
                else {
                    val ownerState = state.sideState(owner)
                    val working = withPlayer(
                        state.copy(stadium = null, stadiumOwner = null),
                        ownerState.copy(discard = ownerState.discard + stadium),
                        owner,
                    )
                    EffectResult(working, listOf(GameEvent.StadiumDiscarded(src.actingSide)))
                }
            }
            is EffectOp.GiovanniCharisma -> {
                // Paso 1: devuelve 1 Energía del Activo rival a la mano de su dueño.
                val foeSide = src.actingSide.other()
                val foe = state.sideState(foeSide)
                val foeActive = foe.active
                val bounced = foeActive?.attachedEnergy?.firstOrNull()
                if (foeActive == null || bounced == null) EffectResult(state, emptyList())
                else {
                    var working = updatePokemon(state, foeActive.card.id) {
                        it.copy(attachedEnergy = it.attachedEnergy - bounced)
                    }
                    working = withPlayer(working, working.sideState(foeSide).copy(hand = foe.hand + bounced), foeSide)
                    // Paso 2 ("si lo haces"): une 1 Energía de TU mano a tu Activo.
                    val me = working.sideState(src.actingSide)
                    val myActive = me.active
                    val fromHand = me.hand.firstOrNull { it is EnergyCard } as? EnergyCard
                    val events = mutableListOf<GameEvent>()
                    if (myActive != null && fromHand != null) {
                        working = updatePokemon(working, myActive.card.id) {
                            it.copy(attachedEnergy = it.attachedEnergy + fromHand)
                        }
                        working = withPlayer(working, working.sideState(src.actingSide).copy(hand = me.hand - fromHand), src.actingSide)
                        events += GameEvent.EnergyAttached(src.actingSide, fromHand.id, myActive.card.id)
                    }
                    EffectResult(working, events)
                }
            }
            is EffectOp.SearchEnergyAttachSelf -> {
                // Moneda opcional; luego toma hasta [count] Energía Básica del tipo del mazo,
                // la une a este Pokémon y baraja el mazo.
                val events = mutableListOf<GameEvent>()
                val ok = if (op.coinFlip) {
                    val h = flip(); events += GameEvent.CoinFlipped(src.actingSide, h); h
                } else true
                val selfMon = targets(Target.SELF, src, state, chosenIds).firstOrNull()
                if (!ok || selfMon == null) EffectResult(state, events)
                else {
                    val ps = state.sideState(src.actingSide)
                    val picked = ps.deck
                        .filter { it is BasicEnergy && it.type == op.energyType }
                        .take(op.count)
                        .filterIsInstance<EnergyCard>()
                    if (picked.isEmpty()) {
                        // Nada que unir: igualmente se barajó al mirar el mazo.
                        EffectResult(withPlayer(state, ps.copy(deck = shuffle(ps.deck)), src.actingSide),
                            events + GameEvent.DeckShuffled(src.actingSide))
                    } else {
                        var working = updatePokemon(state, selfMon.card.id) {
                            it.copy(attachedEnergy = it.attachedEnergy + picked)
                        }
                        working = withPlayer(working, working.sideState(src.actingSide).copy(deck = shuffle(ps.deck - picked.toSet())), src.actingSide)
                        picked.forEach { events += GameEvent.EnergyAttached(src.actingSide, it.id, selfMon.card.id) }
                        events += GameEvent.DeckShuffled(src.actingSide)
                        EffectResult(working, events)
                    }
                }
            }
            is EffectOp.DefenderCannotRetreatNextTurn -> {
                val defender = targets(Target.OPP_ACTIVE, src, state, chosenIds).firstOrNull()
                if (defender == null) EffectResult(state, emptyList())
                else EffectResult(
                    updatePokemon(state, defender.card.id) { it.copy(cannotRetreatOnTurn = state.turn + 1) },
                    emptyList(),
                )
            }
            is EffectOp.DefenderCannotAttackNextTurn -> {
                val defender = targets(Target.OPP_ACTIVE, src, state, chosenIds).firstOrNull()
                if (defender == null) EffectResult(state, emptyList())
                else EffectResult(
                    updatePokemon(state, defender.card.id) { it.copy(cannotAttackOnTurn = state.turn + 1) },
                    emptyList(),
                )
            }
            is EffectOp.BumpDefenderRetreatCostNextTurn -> {
                val defender = targets(Target.OPP_ACTIVE, src, state, chosenIds).firstOrNull()
                if (defender == null) EffectResult(state, emptyList())
                else EffectResult(
                    updatePokemon(state, defender.card.id) {
                        it.copy(retreatCostBumpOnTurn = state.turn + 1, retreatCostBumpAmount = op.amount)
                    },
                    emptyList(),
                )
            }
            is EffectOp.BumpDefenderAttackCostNextTurn -> {
                val defender = targets(Target.OPP_ACTIVE, src, state, chosenIds).firstOrNull()
                if (defender == null) EffectResult(state, emptyList())
                else EffectResult(
                    updatePokemon(state, defender.card.id) {
                        it.copy(attackCostBumpOnTurn = state.turn + 1, attackCostBumpAmount = op.amount)
                    },
                    emptyList(),
                )
            }
            is EffectOp.CoinUntilTailsDamage -> {
                // Lanza monedas hasta cruz; daño = damagePerHeads × caras al Activo rival.
                val events = mutableListOf<GameEvent>()
                var heads = 0
                var first = true
                var firstTails = false
                while (true) {
                    val h = flip()
                    events += GameEvent.CoinFlipped(src.actingSide, h)
                    if (h) heads++ else { if (first) firstTails = true; break }
                    first = false
                }
                val dmg = damageTargets(targets(Target.OPP_ACTIVE, src, state, chosenIds), heads * op.damagePerHeads, src.actingSide, state)
                var working = dmg.state
                val evs = events + dmg.events
                if (op.confuseIfFirstTails && firstTails) {
                    val applied = applyStatus(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(com.mineralord.tcg.engine.model.Status.CONFUSED)), src, working, chosenIds)
                    EffectResult(applied.state, evs + applied.events)
                } else EffectResult(working, evs)
            }
            is EffectOp.CoinUntilTailsDraw -> {
                // Lanza monedas hasta cruz; roba 1 por cada cara (Magikarp).
                val events = mutableListOf<GameEvent>()
                var heads = 0
                while (true) {
                    val h = flip()
                    events += GameEvent.CoinFlipped(src.actingSide, h)
                    if (h) heads++ else break
                }
                val drawn = draw(src.actingSide, heads, state)
                EffectResult(drawn.state, events + drawn.events)
            }
            is EffectOp.DiscardTopDeck -> {
                // Descarta las N primeras cartas de la baraja (propia o rival) al descarte.
                val side = if (op.own) src.actingSide else src.actingSide.other()
                val ps = state.sideState(side)
                val discarded = ps.deck.take(op.count)
                if (discarded.isEmpty()) EffectResult(state, emptyList())
                else {
                    val updated = ps.copy(deck = ps.deck.drop(discarded.size), discard = ps.discard + discarded)
                    EffectResult(withPlayer(state, updated, side), listOf(GameEvent.CardsDiscarded(side, discarded.size)))
                }
            }
            is EffectOp.ReduceDamageNextTurn -> {
                // Marca al Pokémon origen: los ataques le hacen [amount] menos el próximo turno.
                val id = src.sourceId
                if (id == null) EffectResult(state, emptyList())
                else EffectResult(
                    updatePokemon(state, id) {
                        it.copy(damageReductionOnTurn = state.turn + 1, damageReductionAmount = op.amount)
                    },
                    emptyList(),
                )
            }
            is EffectOp.SelfAttackBonusNextTurn -> {
                // Marca al Pokémon origen: sus ataques harán [amount] más el próximo turno propio
                // (turn actual + 2; el turno intermedio es del rival).
                val id = src.sourceId
                if (id == null) EffectResult(state, emptyList())
                else EffectResult(
                    updatePokemon(state, id) {
                        it.copy(attackBonusOnTurn = state.turn + 2, attackBonusAmount = op.amount)
                    },
                    emptyList(),
                )
            }
            is EffectOp.BounceOppActiveEnergyToHand -> {
                // Devuelve las primeras [count] Energías del Activo rival a la mano de su dueño.
                val foeSide = src.actingSide.other()
                val foe = state.sideState(foeSide)
                val foeActive = foe.active
                val bounced = foeActive?.attachedEnergy?.take(op.count).orEmpty()
                if (foeActive == null || bounced.isEmpty()) EffectResult(state, emptyList())
                else {
                    val ids = bounced.map { it.id }.toSet()
                    var working = updatePokemon(state, foeActive.card.id) {
                        it.copy(attachedEnergy = it.attachedEnergy.filter { e -> e.id !in ids })
                    }
                    val updatedFoe = working.sideState(foeSide).copy(
                        hand = working.sideState(foeSide).hand + bounced,
                    )
                    EffectResult(withPlayer(working, updatedFoe, foeSide), emptyList())
                }
            }
            is EffectOp.DamagePerOppHandTrainer -> {
                // Daño crudo al Activo rival = damagePerCard × Entrenadores en la mano rival.
                val foe = state.sideState(src.actingSide.other())
                val trainers = foe.hand.count { it.supertype == Supertype.TRAINER }
                damageTargets(targets(Target.OPP_ACTIVE, src, state, chosenIds), trainers * op.damagePerCard, src.actingSide, state)
            }
            is EffectOp.DiscardTopDeckDamagePerRetreat -> {
                // Descarta las N primeras cartas propias; daño crudo por cada Pokémon con
                // Coste de Retirada exacto descartado así.
                val ps = state.sideState(src.actingSide)
                val discarded = ps.deck.take(op.count)
                val hits = discarded.count { it is PokemonCard && it.retreatCost.size == op.retreatEquals }
                var working = state
                val events = mutableListOf<GameEvent>()
                if (discarded.isNotEmpty()) {
                    val updated = ps.copy(deck = ps.deck.drop(discarded.size), discard = ps.discard + discarded)
                    working = withPlayer(state, updated, src.actingSide)
                    events += GameEvent.CardsDiscarded(src.actingSide, discarded.size)
                }
                val dmg = damageTargets(targets(Target.OPP_ACTIVE, src, working, chosenIds), hits * op.damagePer, src.actingSide, working)
                EffectResult(dmg.state, events + dmg.events)
            }
            // Las ops de elección las captura runFrom/pendingFor: nunca llegan aquí
            // (RecoverFromDiscard solo cae aquí si el descarte no tenía nada que recuperar).
            is EffectOp.ChooseTarget, is EffectOp.SearchDeck, is EffectOp.MoveEnergy,
            is EffectOp.RecoverFromDiscard, is EffectOp.RecoverOppFromDiscard, is EffectOp.PlaceCounters,
            is EffectOp.DiscardFromHandForDamage, is EffectOp.CoinFlipSearchToBench,
            is EffectOp.DiscardOwnToolsForDamage, is EffectOp.PutOppHandPokemonToBottomOfDeck,
            is EffectOp.TransformIntoBasicFromDeck ->
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

    private fun discardEnergy(op: EffectOp.DiscardEnergy, src: EffectSource, state: GameState, chosenIds: List<CardId>, flip: () -> Boolean): EffectResult {
        val events = mutableListOf<GameEvent>()
        // Si es por moneda, solo con cara (Acid Spray/Destructive Flame).
        if (op.coinFlip) {
            val h = flip()
            events += GameEvent.CoinFlipped(src.actingSide, h)
            if (!h) return EffectResult(state, events)
        }
        // La energía descartada va al descarte del DUEÑO del Pokémon objetivo.
        val ownerSide = when (op.target) {
            Target.OPP_ACTIVE, Target.OPP_BENCH, Target.OPP_ALL -> src.actingSide.other()
            else -> src.actingSide
        }
        var working = state
        val discarded = mutableListOf<Card>()
        for (r in targets(op.target, src, state, chosenIds)) {
            val eligible = if (op.energyType == null) r.attachedEnergy
                else r.attachedEnergy.filter { it is BasicEnergy && it.type == op.energyType }
            val toDiscard = eligible.take(op.count)
            discarded += toDiscard
            working = updatePokemon(working, r.card.id) {
                it.copy(attachedEnergy = it.attachedEnergy - toDiscard.toSet())
            }
        }
        if (discarded.isEmpty()) return EffectResult(working, events)
        val updated = working.sideState(ownerSide).copy(
            discard = working.sideState(ownerSide).discard + discarded,
        )
        return EffectResult(withPlayer(working, updated, ownerSide), events)
    }

    // ------------------------------------------------------- resolución de elección

    /**
     * Agarrador Mecánico: la carta [chosen] elegida de la MANO del rival sale de su mano y va al
     * FONDO de su baraja. `d.side` es el jugador que decide → el rival es su `other()`. Emite
     * [GameEvent.HandRevealed] (la mano se enseñó) con el nº de cartas restante. Solo mueve 1.
     */
    private fun applyOpponentHandToBottom(
        d: PendingDecision.SearchCards,
        chosen: List<CardId>,
        state: GameState,
    ): EffectResult {
        val foeSide = d.side.other()
        val foe = state.sideState(foeSide)
        val pickedId = chosen.firstOrNull { it in d.candidates }
        val card = pickedId?.let { id -> foe.hand.firstOrNull { it.id == id } }
        if (card == null) return EffectResult(state, listOf(GameEvent.HandRevealed(foeSide, foe.hand.size)))
        val updated = foe.copy(hand = foe.hand - card, deck = foe.deck + card)
        return EffectResult(
            withPlayer(state, updated, foeSide),
            listOf(GameEvent.HandRevealed(foeSide, updated.hand.size)),
        )
    }

    /** Mueve las cartas [chosen] del mazo a la zona destino y baraja el resto. */
    private fun applySearch(
        d: PendingDecision.SearchCards,
        chosen: List<CardId>,
        state: GameState,
        shuffle: (List<Card>) -> List<Card>,
    ): EffectResult {
        // onOpponent (Haunter — Regreso Espiritual): las zonas [from]/[destination] son del RIVAL.
        val side = if (d.onOpponent) d.side.other() else d.side
        // Electrode — Cadena Bum Bum: descarte de Herramientas enganchadas (no de una zona).
        if (d.fromAttachedTools) return applyDiscardTools(d, chosen, state)
        val ps = state.sideState(side)
        val fromDiscard = d.from == Zone.DISCARD
        val fromHand = d.from == Zone.HAND
        // Solo el MAZO se baraja tras mirarlo; mano y descarte son zonas conocidas.
        val fromDeck = !fromDiscard && !fromHand
        val source = when (d.from) {
            Zone.DISCARD -> ps.discard
            Zone.HAND -> ps.hand
            else -> ps.deck
        }
        // Solo cartas legales (candidatas) y como mucho [count].
        var picked = chosen.filter { it in d.candidates }
            .mapNotNull { id -> source.firstOrNull { it.id == id } }
            .take(d.count)
        // "De diferentes tipos" (Eevee — Amigos Coloridos): un Pokémon por tipo primario.
        if (d.distinctTypes) {
            val seen = mutableSetOf<EnergyType>()
            picked = picked.filter { card ->
                val t = (card as? PokemonCard)?.types?.firstOrNull() ?: return@filter false
                seen.add(t)
            }
        }
        if (picked.isEmpty()) {
            // Mano/descarte: no se baraja nada. Mazo: se baraja al haberlo mirado.
            if (!fromDeck) return EffectResult(state, emptyList())
            val shuffled = ps.copy(deck = shuffle(ps.deck))
            return EffectResult(withPlayer(state, shuffled, side), listOf(GameEvent.DeckShuffled(side)))
        }
        // Ditto — Inicio Transformador: reemplaza el Activo por el Básico elegido. El Activo
        // actual (Ditto) y TODO lo unido (energías, Herramientas, pila de evolución) van al
        // descarte; el mazo se baraja tras retirar el elegido.
        if (d.replaceActiveWithSource) {
            val newBasic = picked.filterIsInstance<PokemonCard>().firstOrNull()
            val oldActive = ps.active
            if (newBasic == null || oldActive == null) {
                val shuffled = ps.copy(deck = shuffle(ps.deck))
                return EffectResult(withPlayer(state, shuffled, side), listOf(GameEvent.DeckShuffled(side)))
            }
            val discardedCards: List<Card> = listOf(oldActive.card) +
                oldActive.evolutionStack + oldActive.attachedEnergy + oldActive.attachedTools
            val updated = ps.copy(
                deck = shuffle(ps.deck - picked.toSet()),
                active = PokemonInPlay(newBasic),
                discard = ps.discard + discardedCards,
            )
            return EffectResult(
                withPlayer(state, updated, side),
                listOf(GameEvent.CardsDiscarded(side, discardedCards.size), GameEvent.DeckShuffled(side)),
            )
        }
        // Retira lo elegido de su zona origen (solo el mazo se baraja).
        val remainingDeck = if (fromDeck) shuffle(ps.deck - picked.toSet()) else ps.deck
        var updated = when (d.from) {
            Zone.DISCARD -> ps.copy(discard = ps.discard - picked.toSet(), deck = remainingDeck)
            Zone.HAND -> ps.copy(hand = ps.hand - picked.toSet(), deck = remainingDeck)
            else -> ps.copy(deck = remainingDeck)
        }
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
        // Solo el mazo se baraja tras mirarlo (mano/descarte no emiten el evento).
        if (fromDeck) events += GameEvent.DeckShuffled(side)
        var result = withPlayer(state, updated, side)
        // Cañones Gemelos: daño CRUDO al Activo rival por cada carta descartada.
        if (d.damagePerDiscardToOppActive > 0 && picked.isNotEmpty()) {
            val oppActive = result.sideState(side.other()).active
            if (oppActive != null) {
                val dmg = damageTargets(
                    listOf(oppActive), d.damagePerDiscardToOppActive * picked.size, side, result,
                )
                result = dmg.state
                events += dmg.events
            }
        }
        return EffectResult(result, events)
    }

    /**
     * Electrode — Cadena Bum Bum: descarta las Herramientas elegidas de tus Pokémon (a la
     * pila de descartes de su dueño) y hace [d.damagePerDiscardToOppActive] CRUDO al Activo
     * rival por cada una. Sin elección = 0 descartes = 0 daño extra (el base ya lo aplicó el motor).
     */
    private fun applyDiscardTools(
        d: PendingDecision.SearchCards,
        chosen: List<CardId>,
        state: GameState,
    ): EffectResult {
        val side = d.side
        val ps = state.sideState(side)
        val toDiscard = chosen.filter { it in d.candidates }.toSet()
        val discarded = ps.allInPlay.flatMap { it.attachedTools }.filter { it.id in toDiscard }
        if (discarded.isEmpty()) return EffectResult(state, emptyList())
        fun strip(p: PokemonInPlay) = p.copy(attachedTools = p.attachedTools.filterNot { it.id in toDiscard })
        val updated = ps.copy(
            active = ps.active?.let(::strip),
            bench = ps.bench.map(::strip),
            discard = ps.discard + discarded,
        )
        var result = withPlayer(state, updated, side)
        val events = mutableListOf<GameEvent>(GameEvent.CardsDiscarded(side, discarded.size))
        val oppActive = result.sideState(side.other()).active
        if (oppActive != null) {
            val dmg = damageTargets(
                listOf(oppActive), d.damagePerDiscardToOppActive * discarded.size, side, result,
            )
            result = dmg.state
            events += dmg.events
        }
        return EffectResult(result, events)
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
     * Reparte contadores de daño: [chosen] repite el id de cada Pokémon una vez por
     * contador. Solo cuentan los ids legales (candidatos) y como mucho [d.count] en total.
     * Cada contador = 10 de daño CRUDO (sin Debilidad/Resistencia). Los KO los resuelve
     * luego el motor (handleKnockouts), como con cualquier daño de efecto.
     */
    private fun applyPlaceCounters(
        d: PendingDecision.PlaceCounters,
        chosen: List<CardId>,
        state: GameState,
    ): EffectResult {
        val placements = chosen.filter { it in d.candidates }.take(d.count)
        if (placements.isEmpty()) return EffectResult(state, emptyList())
        var working = state
        val events = mutableListOf<GameEvent>()
        // Agrupa por objetivo: N apariciones = N*10 de daño a ese Pokémon.
        for ((id, hits) in placements.groupingBy { it }.eachCount()) {
            val mon = inPlay(working, id) ?: continue
            val dmg = hits * 10
            working = updatePokemon(working, id) { it.copy(damage = it.damage + dmg) }
            events += GameEvent.DamageDealt(d.side, mon.card.id, dmg)
        }
        return EffectResult(working, events)
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
        flip: () -> Boolean = { true },
    ): EffectResult {
        val side = d.side
        val ps = state.sideState(side)
        // Pegatinas de Energía: el enganche se juega a una moneda. Con cruz no se une nada
        // (la elección ya se hizo; solo emitimos el evento de la tirada para la animación).
        if (d.coinFlip) {
            val heads = flip()
            if (!heads) return EffectResult(state, listOf(GameEvent.CoinFlipped(side, false)))
        }
        // Origen de las energías: mazo (Generador Eléctrico) o descarte (Passionate Singing).
        val source = if (d.fromDiscard) ps.discard else ps.deck
        var working = state
        // Con moneda (Pegatinas de Energía) ya sabemos que salió cara: registra el evento.
        val events = if (d.coinFlip) mutableListOf<GameEvent>(GameEvent.CoinFlipped(side, true))
            else mutableListOf()
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
                Counter.ENERGY_ATTACHED -> refs.sumOf { p ->
                    if (amount.energyType == null) p.attachedEnergyCount
                    else p.attachedEnergy.count { it is BasicEnergy && it.type == amount.energyType }
                }
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
                (filter.type == null || (c is PokemonCard && filter.type in c.types) || (c is BasicEnergy && c.type == filter.type)) &&
                (filter.nameContains == null || c.name.es.contains(filter.nameContains!!, true) || c.name.en.contains(filter.nameContains!!, true)) &&
                (filter.nameExcludes == null || !(c.name.es.contains(filter.nameExcludes!!, true) || c.name.en.contains(filter.nameExcludes!!, true))) &&
                (filter.trainerKind == null || (c is TrainerCard && trainerCategoryOf(c.kind) == filter.trainerKind))
        }.map { it.id }

    private fun trainerCategoryOf(kind: TrainerKind): TrainerCategory = when (kind) {
        is TrainerKind.Supporter -> TrainerCategory.SUPPORTER
        is TrainerKind.Item -> TrainerCategory.ITEM
        is TrainerKind.Stadium -> TrainerCategory.STADIUM
        is TrainerKind.Tool -> TrainerCategory.TOOL
    }

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
