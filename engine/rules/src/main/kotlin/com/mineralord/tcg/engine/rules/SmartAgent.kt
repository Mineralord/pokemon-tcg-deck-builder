package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import com.mineralord.tcg.engine.model.canPayEnergyCost

/**
 * IA de juego real: resuelve decisiones pendientes y usa [GameEngine.legalIntents] para jugar
 * de forma razonable. Determinista y con orden de prioridad fijo, de modo que nunca cuelga un
 * turno.
 *
 * El comportamiento se **gradúa** según [difficulty] (ver [Difficulty]):
 * - **Pokéball / Súperball (fáciles):** apenas desarrollan (Banca solo si está vacía), unen
 *   energía al Activo a ciegas y atacan de inmediato con un ataque flojo/cualquiera.
 * - **Ultra / Master (difíciles):** DESARROLLAN el tablero (llenan la Banca), evolucionan,
 *   juegan Entrenadores, anclan Herramientas, unen la energía al **mejor atacante** y **atacan
 *   al final del turno** (tras desarrollar, no antes). Master además se **retira** para
 *   reposicionar a un atacante listo, busca el **Noqueo letal** y arrastra al mejor objetivo.
 *
 * Clave de dificultad: como atacar TERMINA el turno, el orden de prioridad coloca el ataque
 * casi al final para las gamas altas → primero construyen su tablero y luego rematan.
 */
/**
 * Parámetros de la búsqueda minimax de Master. [MAX_PLIES] = TURNOS de anticipación (3 = nuestro turno
 * → rival → nuestro turno); [TURN_ACT_DEPTH] = acciones encadenadas que se exploran dentro de un turno;
 * [SEARCH_BUDGET] = tope global de nodos simulados por decisión (acota la latencia; iterative deepening
 * + alfa-beta + tabla de transposición permiten aprovecharlo a mayor profundidad).
 */
private const val MAX_PLIES = 3
private const val TURN_ACT_DEPTH = 6
private const val SEARCH_BUDGET = 14000

// Banderas de la tabla de transposición (valor EXACTO, o cota inferior/superior por corte alfa-beta).
private const val FLAG_EXACT = 0
private const val FLAG_LOWER = 1
private const val FLAG_UPPER = 2

class SmartAgent(
    private val engine: GameEngine,
    private val difficulty: Difficulty = Difficulty.ULTRABALL,
) : Agent {

    // Rasgos de comportamiento derivados de la dificultad.
    private val developsBench get() = difficulty >= Difficulty.ULTRABALL
    private val usesTools get() = difficulty >= Difficulty.ULTRABALL
    private val usesTrainers get() = difficulty >= Difficulty.ULTRABALL
    private val evolves get() = difficulty >= Difficulty.ULTRABALL
    private val smartEnergy get() = difficulty >= Difficulty.ULTRABALL
    private val smartPromotion get() = difficulty >= Difficulty.ULTRABALL
    private val usesAbilities get() = difficulty >= Difficulty.ULTRABALL
    private val retreats get() = difficulty >= Difficulty.MASTERBALL
    /** Master: razona 1 turno vista (amenaza entrante del rival) y secuencia Entrenadores por valor. */
    private val looksAhead get() = difficulty >= Difficulty.MASTERBALL
    /** Master: planifica el turno con una BÚSQUEDA ligera (simula secuencias de acciones y las evalúa). */
    private val usesSearch get() = difficulty >= Difficulty.MASTERBALL

    /**
     * Motor AISLADO para simular líneas de juego durante la búsqueda. Tiene su propio [Rng] para que
     * los lanzamientos de moneda simulados NO alteren la aleatoriedad de la partida real. Usa el mismo
     * catálogo de efectos por defecto que el motor de PvE, de modo que la simulación es fiel.
     */
    private val simEngine = GameEngine(SeededRng(0xA11CE5))

    /** Entrada de la tabla de transposición: valor minimax de un estado a cierta profundidad y su tipo. */
    private class TtEntry(val depth: Int, val value: Double, val flag: Int)

    /** Tabla de transposición (se limpia en cada [searchBestIntent]; clave = [ttKey]). */
    private val tt = HashMap<String, TtEntry>()

    override fun decide(state: GameState, side: Side): GameIntent {
        // 0) Nuestro Activo fue Noqueado: promover un Pokémon de la Banca.
        if (side in state.pendingPromotion) {
            decidePromotion(state, side)?.let { return it }
        }

        // 1) Hay una decisión pendiente nuestra: resolverla con una elección legal.
        state.interaction?.let { inter ->
            if (inter.side == side) return resolveDecision(inter.decision, state, side)
        }

        // 1.5) BÚSQUEDA (Master): planifica el turno simulando secuencias de acciones con el motor y
        //   elige la primera acción de la MEJOR línea (ver [searchBestIntent]). Si no aplica, cae al
        //   juicio heurístico de abajo (que además ordena/poda los candidatos de la búsqueda).
        if (usesSearch && state.activeSide == side) {
            searchBestIntent(state, side)?.let { return it }
        }

        val legal = engine.legalIntents(state)
        val me = state.sideState(side)
        val active = me.active

        // 2) DESARROLLO: bajar Básicos a la Banca. Las gamas altas la LLENAN (más atacantes y
        //    respaldo); las bajas solo bajan uno cuando la Banca está vacía.
        if (developsBench || me.bench.isEmpty()) {
            legal.firstOrNull { it is GameIntent.PlayBasicToBench }?.let { return it }
        }

        // 3) Evolucionar (gamas altas): sube el techo de daño/HP; hazlo antes de atacar. Master lo hace
        //    POR IMPACTO: evoluciona primero su ATACANTE (el Activo, o el de Banca con energía ya unida),
        //    y a igualdad la línea que más PS/daño aporta; Ultra conserva el orden simple.
        if (evolves) {
            val evos = legal.filterIsInstance<GameIntent.Evolve>()
            if (evos.isNotEmpty()) return if (looksAhead) bestEvolve(state, side, evos) else evos.first()
        }

        // 3.5) HABILIDADES (motores de robo/aceleración de energía): úsalas ANTES de energía y
        //   ataque para montar el turno como un jugador experto. Solo "1 vez por turno" (el motor
        //   las retira tras usarse) → garantiza progreso y nunca cuelga el turno.
        if (usesAbilities) {
            legal.firstOrNull { it is GameIntent.UseAbility && isSafeAbility(state, side, it) }
                ?.let { return it }
        }

        // 4) Jugar un Entrenador ANTES de atacar. Master lo hace por VALOR y secuenciación experta:
        //    robo/búsqueda/aceleración primero (ganar info y recursos), gust SOLO si prepara un KO,
        //    curar si hay daño; Ultra conserva el orden simple. (Atacar cierra el turno → van antes.)
        if (usesTrainers) {
            val trainers = legal.filterIsInstance<GameIntent.PlayTrainer>()
            if (trainers.isNotEmpty()) {
                val pick = if (looksAhead) bestTrainer(state, side, trainers) else trainers.first()
                if (pick != null) return pick
            }
        }

        // 5) Unir energía (una por turno). Las gamas altas la ponen en el MEJOR atacante
        //    (quien saque el mayor daño tras recibirla); las bajas, al Activo a ciegas.
        val attaches = legal.filterIsInstance<GameIntent.AttachEnergy>()
        if (attaches.isNotEmpty()) {
            val pick = if (smartEnergy) bestEnergyAttach(state, side, attaches)
            else attaches.firstOrNull { it.to == active?.card?.id } ?: attaches.first()
            return pick
        }

        // 6) Anclar Herramienta a un Pokémon propio (gratis y siempre útil: HP/reducción).
        if (usesTools) {
            val myIds = me.allInPlay.map { it.card.id }.toSet()
            (legal.firstOrNull { it is GameIntent.AttachTool && it.target == active?.card?.id }
                ?: legal.firstOrNull { it is GameIntent.AttachTool && it.target in myIds })
                ?.let { return it }
        }

        // 7) RETIRADA (Master): reposicionamiento ofensivo o defensivo por PREMIO (ver [chooseRetreat]).
        //    Solo se retira si APORTA (un atacante de Banca que remata, o salvar a un Activo condenado
        //    de 2+ premios) — nunca una retirada ociosa, que con coste 0 podría ciclar sin fin.
        chooseRetreat(state, side, legal)?.let { return it }

        // 8) ATACAR (cierra el turno). La elección depende de la dificultad.
        val affordable = active?.let { affordableAttacks(it) }.orEmpty()
        if (affordable.isNotEmpty() && active != null) {
            val defender = state.sideState(side.other()).active
            val chosen = when (difficulty) {
                // Novata: el ataque más flojo (menor coste; a igualdad, menor daño).
                Difficulty.POKEBALL -> affordable.minWithOrNull(
                    compareBy({ it.convertedCost }, { it.estimatedBaseDamage() }),
                )
                // Básica: lo primero pagable, sin pensar.
                Difficulty.SUPERBALL -> affordable.first()
                // Táctica: el ataque de más DAÑO EFECTIVO (cuenta Debilidad y ataques variables).
                Difficulty.ULTRABALL -> affordable.maxByOrNull { effectiveDamage(it, active, defender) }
                    ?: affordable.maxByOrNull { it.convertedCost }
                // Experta: si hay un ataque que Noquea al Activo rival, el MÁS BARATO que lo logra
                //   (ahorra energía para el sucesor). Si no hay Noqueo YA, planifica a 2 TURNOS:
                //   máximo daño efectivo y, a igualdad, el ataque que deja al rival REMATABLE el
                //   próximo turno (HP restante ≤ lo que repetimos) y luego el más barato.
                Difficulty.MASTERBALL -> masterAttackChoice(active, defender, affordable)
            }
            if (chosen != null) return GameIntent.Attack(chosen.name.es)
        }

        // 9) Nada productivo: terminar el turno.
        return GameIntent.EndTurn
    }

    /** Escoge una respuesta legal mínima para cada tipo de decisión. */
    private fun resolveDecision(decision: PendingDecision, state: GameState, side: Side): GameIntent = when (decision) {
        is PendingDecision.ChooseTargets -> {
            // Experta: distingue si los candidatos son PROPIOS o RIVALES (antes elegía siempre el de
            // menos HP, lo que para un cambio/curación PROPIA escogía el PEOR Pokémon). Rivales →
            // objetivo de gust/daño óptimo; propios → nuestro mejor atacante.
            val ordered = if (difficulty >= Difficulty.MASTERBALL) {
                smartTargetOrder(state, side, decision.candidates)
            } else {
                decision.candidates
            }
            GameIntent.ResolveDecision(ordered.take(decision.count))
        }
        // Hackeo Genómico: copia el ataque de MÁS daño fijo del Activo rival (o el primero).
        is PendingDecision.ChooseAttack -> {
            val attacks = state.sideState(side.other()).active?.card?.attacks.orEmpty()
            val bestIdx = attacks.indices.maxByOrNull { attacks[it].estimatedBaseDamage() } ?: 0
            GameIntent.ResolveDecision(listOf(PendingDecision.encodeAttackIndex(bestIdx)))
        }
        // Reparte contadores: uno por objetivo empezando por el de menos HP (round-robin
        // sobre los candidatos ordenados) hasta agotar los [count] contadores.
        is PendingDecision.PlaceCounters -> {
            val ordered = if (difficulty >= Difficulty.MASTERBALL) {
                decision.candidates.sortedWith(
                    compareBy({ id -> remainingHpOf(state, id) }, { id -> -prizeValueOf(state, id) }),
                )
            } else {
                decision.candidates
            }
            val hits = if (ordered.isEmpty()) emptyList()
                else (0 until decision.count).map { ordered[it % ordered.size] }
            GameIntent.ResolveDecision(hits)
        }
        // Conversión 4: elige un tipo (la IA toma el primero disponible).
        is PendingDecision.ChooseEnergyType ->
            GameIntent.ResolveDecision(decision.candidateIds.take(1))
        // Moneda: sin elección; resolver dispara el lanzamiento autoritativo.
        is PendingDecision.CoinFlip -> GameIntent.ResolveDecision(emptyList())
        is PendingDecision.CoinFlipThenSearch -> GameIntent.ResolveDecision(emptyList())
        is PendingDecision.SearchCards ->
            GameIntent.ResolveDecision(decision.candidates.take(decision.count))
        is PendingDecision.MoveEnergy ->
            GameIntent.ResolveDecision(decision.fromCandidates.take(1) + decision.toCandidates.take(1))
        is PendingDecision.AttachFromRevealed -> {
            // Empareja cada energía con un Pokémon de Banca (round-robin), hasta el tope.
            val energies = decision.energyCandidates.take(decision.maxAttach)
            val bench = decision.benchCandidates
            val pairs = energies.flatMapIndexed { i, e -> listOf(e, bench[i % bench.size]) }
            GameIntent.ResolveDecision(pairs)
        }
    }

    /**
     * Elige a qué Pokémon unir la energía, con la lógica de un jugador competente:
     *  1) Si una unión permite al **Activo NOQUEAR** al defensor este turno, complétala (remate).
     *  2) Si el Activo YA puede atacar, **desarrolla un segundo atacante** en la Banca (prepara al
     *     sucesor) en vez de amontonar energía en un solo Pokémon — así el tablero tiene amenazas
     *     de respaldo y no colapsa cuando cae el Activo.
     *  3) Si no, invierte donde MÁS aumente el daño efectivo alcanzable; a igualdad, en el Activo.
     */
    private fun bestEnergyAttach(
        state: GameState,
        side: Side,
        attaches: List<GameIntent.AttachEnergy>,
    ): GameIntent.AttachEnergy {
        val me = state.sideState(side)
        val defender = state.sideState(side.other()).active
        val activeId = me.active?.card?.id

        // 1) Remate: unión que da al Activo un ataque pagable que Noquea al defensor.
        if (defender != null && activeId != null) {
            attaches.firstOrNull { att ->
                att.to == activeId &&
                    pipOf(me, att.to)?.let { damageAfterOneEnergy(it, defender) >= defender.remainingHp } == true
            }?.let { return it }
        }

        // 2) CONCENTRA en el Activo hacia su MEJOR ataque. Un error de novato es repartir energía:
        //   un jugador experto CARGA primero a su atacante hasta tener online su ataque más fuerte
        //   (aunque una unión suelta aún no desbloquee daño nuevo — los ataques caros necesitan
        //   varias energías), y SOLO ENTONCES invierte en la Banca. Esto evita que la energía se
        //   "estanque" y que la IA nunca llegue a usar ataques de coste 2+.
        //
        //   LOOKAHEAD (Master): si el Activo está CONDENADO (el rival lo Noquea el próximo turno) y
        //   cargarlo no lo deja Noquear YA, NO tires energía en él — desarróllala en la Banca para
        //   tener listo al sucesor. (Evita "alimentar" un atacante que caerá sin devolver el golpe.)
        val activePip = activeId?.let { pipOf(me, it) }
        val activeDoomed = looksAhead && activePip != null &&
            incomingThreat(state, side) >= activePip.remainingHp &&
            !(defender != null && damageAfterOneEnergy(activePip, defender) >= defender.remainingHp)
        if (activePip != null && !activeDoomed && !canUseStrongestAttack(activePip, defender)) {
            attaches.firstOrNull { it.to == activeId }?.let { return it }
        }

        // 3) Activo ya cargado: DESARROLLA el mejor atacante de Banca (prepara al sucesor). Máxima
        //   ganancia marginal de daño y, a igualdad, el que llegue a mayor daño absoluto.
        return attaches.maxWithOrNull(
            compareBy(
                { att -> pipOf(me, att.to)?.let { marginalEnergyGain(it, defender) } ?: 0 },
                { att -> pipOf(me, att.to)?.let { damageAfterOneEnergy(it, defender) } ?: 0 },
            ),
        ) ?: attaches.first()
    }

    /**
     * ¿El Pokémon ya puede pagar su ataque de MAYOR daño efectivo (vs [defender])? Si es así, cargarle
     * más energía rinde poco y conviene desarrollar la Banca; si no, hay que seguir cargándolo.
     */
    private fun canUseStrongestAttack(pip: PokemonInPlay, defender: PokemonInPlay?): Boolean {
        val strongest = pip.card.attacks.maxByOrNull { effectiveDamage(it, pip, defender) } ?: return true
        return pip.attachedEnergyCount >= strongest.convertedCost
    }

    /** Promoción tras KO con criterio experto (reutilizable por [decide] y por la búsqueda). */
    private fun decidePromotion(state: GameState, side: Side): GameIntent? {
        val bench = state.sideState(side).bench
        if (bench.isEmpty()) return null
        val defender = state.sideState(side.other()).active
        val best = if (smartPromotion) {
            val threat = if (looksAhead) incomingThreat(state, side) else 0
            bench.maxWithOrNull(
                compareBy(
                    { pip -> currentAffordableDamage(pip, defender) },
                    { pip -> if (pip.remainingHp > threat) 1 else 0 },
                    { pip -> -pip.card.prizeValue },
                    { pip -> pip.remainingHp },
                ),
            )
        } else {
            bench.firstOrNull()
        }
        return best?.let { GameIntent.PromoteActive(it.card.id) }
    }

    /**
     * Retirada ÚTIL o null. Ofensiva: reposiciona a un atacante de Banca que NOQUEA cuando el Activo
     * no puede rematar (o no pega nada). Defensiva: salva a un Activo CONDENADO de 2+ premios llevando
     * un refugio de menos premios. Nunca devuelve una retirada ociosa — clave para que una retirada de
     * coste 0 no entre en bucle. Solo en turno > 1 (en el 1 no se ataca, retirarse no aporta).
     */
    private fun chooseRetreat(state: GameState, side: Side, legal: List<GameIntent>): GameIntent.Retreat? {
        if (!retreats || state.turn <= 1) return null
        val me = state.sideState(side)
        val active = me.active ?: return null
        val defender = state.sideState(side.other()).active
        val activeDmg = currentAffordableDamage(active, defender)
        val activeLethal = defender != null && activeDmg >= defender.remainingHp
        if (activeLethal) return null
        // Ofensiva.
        val best = legal.filterIsInstance<GameIntent.Retreat>()
            .mapNotNull { r -> benchPip(me, r.benchTarget)?.let { r to currentAffordableDamage(it, defender) } }
            .maxByOrNull { it.second }
        if (best != null) {
            val benchDmg = best.second
            val benchLethal = defender != null && benchDmg >= defender.remainingHp
            if (benchLethal || (activeDmg == 0 && benchDmg > 0)) return best.first
        }
        // Defensiva.
        if (looksAhead) {
            val threat = incomingThreat(state, side)
            if (threat >= active.remainingHp && active.card.prizeValue >= 2) {
                val shelter = legal.filterIsInstance<GameIntent.Retreat>()
                    .mapNotNull { r -> benchPip(me, r.benchTarget)?.let { r to it } }
                    .filter { it.second.card.prizeValue < active.card.prizeValue }
                    .maxWithOrNull(
                        compareBy(
                            { if (it.second.remainingHp > threat) 1 else 0 },
                            { -it.second.card.prizeValue },
                            { currentAffordableDamage(it.second, defender) },
                        ),
                    )
                if (shelter != null) return shelter.first
            }
        }
        return null
    }

    /** Elección de ataque experta (Noqueo letal barato; si no, máximo daño con plan a 2 turnos). */
    private fun masterAttackChoice(active: PokemonInPlay, defender: PokemonInPlay?, affordable: List<Attack>): Attack? {
        val lethal = defender?.let { d ->
            affordable.filter { effectiveDamage(it, active, d) >= d.remainingHp }.minByOrNull { it.convertedCost }
        }
        if (lethal != null) return lethal
        val repeatable = affordable.maxOfOrNull { effectiveDamage(it, active, defender) } ?: 0
        return affordable.maxWithOrNull(
            compareBy(
                { effectiveDamage(it, active, defender) },
                { if (defender != null && defender.remainingHp - effectiveDamage(it, active, defender) <= repeatable) 1 else 0 },
                { -it.convertedCost },
            ),
        ) ?: affordable.maxByOrNull { it.convertedCost }
    }

    // ============================================================
    //  BÚSQUEDA MINIMAX N-PLY (Master): iterative deepening + alfa-beta + tabla de transposición
    // ============================================================
    //
    //  Cada "ply" es un TURNO COMPLETO (varias acciones del mismo jugador). [playTurn] es un nodo
    //  genérico: si el jugador a mover es el nuestro, MAXIMIZA [evaluate] (óptica nuestra); si es el
    //  rival, la MINIMIZA (él maximiza la suya). Al acabar un turno baja al siguiente ply (jugador
    //  contrario) hasta agotar [MAX_PLIES] turnos y evaluar el horizonte.
    //
    //  - ITERATIVE DEEPENING: busca a 1, 2, … MAX_PLIES turnos; cada iteración ordena primero la mejor
    //    jugada de la anterior → más cortes alfa-beta y resultado útil aunque se agote el presupuesto.
    //  - ALFA-BETA: poda ramas que no pueden cambiar el resultado (sólida con nodos MAX/MIN reales).
    //  - TABLA DE TRANSPOSICIÓN: cachea el valor de cada estado (mismo tablero al que se llega por
    //    distinto orden de acciones — frecuente al reordenar energía/Banca/evolución), reusándolo entre
    //    iteraciones de ID y entre ramas. Clave = firma EXACTA del estado (ids de instancia, daño,
    //    energía, mano ordenada…), así que los aciertos son correctos; se limpia en cada decisión.

    private fun searchBestIntent(state: GameState, side: Side): GameIntent? {
        val rootCandidates = candidateActions(state, side)
        if (rootCandidates.isEmpty()) return null
        tt.clear()
        val budget = intArrayOf(SEARCH_BUDGET)
        var best = rootCandidates.first()
        // Iterative deepening: profundidad creciente en TURNOS de anticipación.
        for (plies in 1..MAX_PLIES) {
            if (budget[0] <= 0) break
            var alpha = Double.NEGATIVE_INFINITY
            var localBest = best
            var localBestScore = Double.NEGATIVE_INFINITY
            // La mejor jugada de la iteración previa, primero (mejora los cortes alfa-beta).
            val ordered = listOf(best) + rootCandidates.filter { it != best }
            for (intent in ordered) {
                if (budget[0] <= 0) break
                budget[0]--
                val r = simEngine.apply(state, intent)
                if (!r.accepted) continue
                val v = playTurn(r.state, side, side, plies, TURN_ACT_DEPTH - 1, alpha, Double.POSITIVE_INFINITY, budget)
                if (v > localBestScore) { localBestScore = v; localBest = intent } // ">" → empates: primero
                if (v > alpha) alpha = v
            }
            if (localBestScore > Double.NEGATIVE_INFINITY) best = localBest
        }
        return best
    }

    /**
     * Nodo genérico del minimax. [mover] es quien juega este turno; MAX si es [rootSide], MIN si no.
     * Encadena las acciones del turno (recursión con el mismo [mover]); cuando el turno termina baja al
     * siguiente ply con el jugador contrario, hasta agotar [turnPliesLeft]. Alfa-beta + transposición.
     */
    private fun playTurn(
        state: GameState,
        rootSide: Side,
        mover: Side,
        turnPliesLeft: Int,
        actDepth: Int,
        alpha0: Double,
        beta0: Double,
        budget: IntArray,
    ): Double {
        val st = settle(state)
        if (st.isOver) return evaluate(st, rootSide)
        if (st.activeSide != mover) {
            // El turno de [mover] terminó → siguiente ply (jugador contrario), o horizonte.
            if (turnPliesLeft <= 1) return evaluate(st, rootSide)
            return playTurn(st, rootSide, st.activeSide, turnPliesLeft - 1, TURN_ACT_DEPTH, alpha0, beta0, budget)
        }
        if (actDepth <= 0 || budget[0] <= 0) return evaluate(st, rootSide)

        val key = ttKey(st)
        val depth = turnPliesLeft * (TURN_ACT_DEPTH + 2) + actDepth
        var alpha = alpha0
        var beta = beta0
        tt[key]?.let { e ->
            if (e.depth >= depth) {
                when (e.flag) {
                    FLAG_EXACT -> return e.value
                    FLAG_LOWER -> if (e.value > alpha) alpha = e.value
                    FLAG_UPPER -> if (e.value < beta) beta = e.value
                }
                if (alpha >= beta) return e.value
            }
        }

        val isMax = mover == rootSide
        var best = if (isMax) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        for (a in candidateActions(st, mover)) {
            if (budget[0] <= 0) break
            budget[0]--
            val r = simEngine.apply(st, a)
            if (!r.accepted) continue
            val v = playTurn(r.state, rootSide, mover, turnPliesLeft, actDepth - 1, alpha, beta, budget)
            if (isMax) {
                if (v > best) best = v
                if (best > alpha) alpha = best
            } else {
                if (v < best) best = v
                if (best < beta) beta = best
            }
            if (alpha >= beta) break // poda alfa-beta
        }
        if (best == Double.NEGATIVE_INFINITY || best == Double.POSITIVE_INFINITY) best = evaluate(st, rootSide)

        val flag = when {
            best <= alpha0 -> FLAG_UPPER
            best >= beta0 -> FLAG_LOWER
            else -> FLAG_EXACT
        }
        val existing = tt[key]
        if (existing == null || existing.depth <= depth) tt[key] = TtEntry(depth, best, flag)
        return best
    }

    /** Avanza el estado resolviendo TODAS las pendientes (promociones/decisiones de cualquier lado). */
    private fun settle(state: GameState): GameState {
        var st = state
        var g = 0
        while (g++ < 12 && !st.isOver) {
            val promoSide = when {
                Side.PLAYER in st.pendingPromotion -> Side.PLAYER
                Side.OPPONENT in st.pendingPromotion -> Side.OPPONENT
                else -> null
            }
            if (promoSide != null) {
                val p = decidePromotion(st, promoSide) ?: break
                val r = simEngine.apply(st, p); if (!r.accepted) break; st = r.state; continue
            }
            val inter = st.interaction
            if (inter != null) {
                val r = simEngine.apply(st, resolveDecision(inter.decision, st, inter.side))
                if (!r.accepted) break; st = r.state; continue
            }
            break
        }
        return st
    }

    /**
     * Firma EXACTA del estado para la tabla de transposición. Incluye turno activo, flags de turno,
     * Estadio, y por cada lado: premios, Activo y Banca (ordenada, por id de instancia) con su daño /
     * energía / estados, mano ordenada y tamaño de mazo. Dos estados con la misma firma son idénticos
     * para la búsqueda (p.ej. llegar al mismo tablero uniendo energía antes o después de bajar un Básico).
     */
    private fun ttKey(s: GameState): String {
        val sb = StringBuilder(256)
        sb.append(s.activeSide.ordinal)
            .append(if (s.energyAttachedThisTurn) '1' else '0')
            .append(if (s.supporterPlayedThisTurn) '1' else '0')
            .append(if (s.stadiumUsedThisTurn) '1' else '0')
            .append('@').append(s.stadium?.id?.raw ?: "-").append('|')
        appendSideKey(sb, s.player); sb.append("//"); appendSideKey(sb, s.opponent)
        return sb.toString()
    }

    private fun appendSideKey(sb: StringBuilder, p: com.mineralord.tcg.engine.model.PlayerState) {
        sb.append('p').append(p.prizesRemaining).append('d').append(p.deck.size).append(':')
        appendPipKey(sb, p.active); sb.append('[')
        p.bench.sortedBy { it.card.id.raw }.forEach { appendPipKey(sb, it); sb.append(';') }
        sb.append("]h")
        p.hand.map { it.id.raw }.sorted().joinTo(sb, ",")
    }

    private fun appendPipKey(sb: StringBuilder, pip: PokemonInPlay?) {
        if (pip == null) { sb.append('-'); return }
        sb.append(pip.card.id.raw).append('#').append(pip.damage)
            .append('e').append(pip.attachedEnergyCount)
            .append('s').append(pip.statuses.size)
            .append('v').append(pip.evolutionStack.size)
    }

    /**
     * Función de EVALUACIÓN del estado desde la óptica de [side]. Prioriza el intercambio de premios,
     * luego el daño (bajar su HP, conservar el nuestro) y el desarrollo (energía montada), y penaliza
     * dejar a nuestro Activo en rango de Noqueo del rival (minimax ligero de 1 ply).
     */
    private fun evaluate(state: GameState, side: Side): Double {
        state.winner?.let { return if (it == side) 1e7 else -1e7 }
        val me = state.sideState(side)
        val opp = state.sideState(side.other())
        var s = 0.0
        s += (opp.prizesRemaining - me.prizesRemaining) * 1000.0           // premios: lo que más pesa
        s += me.allInPlay.sumOf { it.remainingHp.toDouble() }              // conservar nuestro HP
        s -= opp.allInPlay.sumOf { it.remainingHp.toDouble() }             // bajar su HP
        s += me.allInPlay.sumOf { it.attachedEnergyCount.toDouble() } * 2.0 // atacantes montados
        val myActive = me.active
        if (myActive != null) {
            val threat = opp.active?.let { oa ->
                affordableAttacks(oa).maxOfOrNull { effectiveDamage(it, oa, myActive) } ?: 0
            } ?: 0
            if (threat >= myActive.remainingHp) s -= myActive.card.prizeValue * 900.0 // no regalar premio
            s += currentAffordableDamage(myActive, opp.active) * 0.5                   // Activo que pega ya
        }
        return s
    }

    /**
     * Candidatos PODADOS y ORDENADOS (mejor primero según heurística) que la búsqueda expande. Incluye
     * desarrollo, evolución, habilidades, Entrenadores con valor, energía al mejor destino, Herramienta,
     * retirada, ataques (el mejor primero) y SIEMPRE "terminar el turno" (parar la línea aquí).
     */
    private fun candidateActions(state: GameState, side: Side): List<GameIntent> {
        val legal = engine.legalIntents(state)
        val me = state.sideState(side)
        val active = me.active
        val activeId = active?.card?.id
        val defender = state.sideState(side.other()).active
        val out = LinkedHashSet<GameIntent>()

        legal.filterIsInstance<GameIntent.PlayBasicToBench>().take(3).forEach { out += it }
        val evos = legal.filterIsInstance<GameIntent.Evolve>()
        if (evos.isNotEmpty()) { out += bestEvolve(state, side, evos); evos.forEach { out += it } }
        legal.filterIsInstance<GameIntent.UseAbility>().filter { isSafeAbility(state, side, it) }.take(2).forEach { out += it }
        legal.filterIsInstance<GameIntent.PlayTrainer>()
            .mapNotNull { intent ->
                (me.hand.firstOrNull { it.id == intent.card } as? TrainerCard)?.let { intent to trainerScore(state, side, it) }
            }
            .filter { it.second > 0 }.sortedByDescending { it.second }.take(3).forEach { out += it.first }
        legal.filterIsInstance<GameIntent.AttachEnergy>().distinctBy { it.to }
            .sortedByDescending { attachPriority(me, it, defender, activeId) }.take(3).forEach { out += it }
        (legal.filterIsInstance<GameIntent.AttachTool>().firstOrNull { it.target == activeId }
            ?: legal.filterIsInstance<GameIntent.AttachTool>().firstOrNull())?.let { out += it }
        chooseRetreat(state, side, legal)?.let { out += it }
        orderedAttacks(legal, active, defender).forEach { out += it }
        out += GameIntent.EndTurn
        return out.toList()
    }

    /** Prioridad de una unión de energía: ganancia marginal, llegar a dañar, y cargar al Activo. */
    private fun attachPriority(
        me: com.mineralord.tcg.engine.model.PlayerState,
        att: GameIntent.AttachEnergy,
        defender: PokemonInPlay?,
        activeId: CardId?,
    ): Int {
        val pip = pipOf(me, att.to) ?: return -1
        var p = marginalEnergyGain(pip, defender) * 10 + damageAfterOneEnergy(pip, defender)
        if (att.to == activeId) { p += 1; if (!canUseStrongestAttack(pip, defender)) p += 5 }
        return p
    }

    /** Ataques como candidatos de búsqueda, con el mejor (juicio Master) primero. */
    private fun orderedAttacks(
        legal: List<GameIntent>,
        active: PokemonInPlay?,
        defender: PokemonInPlay?,
    ): List<GameIntent> {
        val attackIntents = legal.filterIsInstance<GameIntent.Attack>()
        if (attackIntents.isEmpty() || active == null) return attackIntents
        val best = masterAttackChoice(active, defender, affordableAttacks(active))
        val bestIntent = best?.let { b -> attackIntents.firstOrNull { it.attackName == b.name.es && it.attacker == null } }
        return listOfNotNull(bestIntent) + attackIntents.filter { it != bestIntent }
    }

    /** Daño EFECTIVO que un Pokémon puede hacer AHORA con la energía que ya tiene (vs [defender]). */
    private fun currentAffordableDamage(pip: PokemonInPlay, defender: PokemonInPlay?): Int =
        pip.card.attacks.filter { pip.attachedEnergyCount >= it.convertedCost }
            .maxOfOrNull { effectiveDamage(it, pip, defender) } ?: 0

    /** Mejor daño EFECTIVO pagable de un Pokémon SI recibiera una energía más (vs [defender]). */
    private fun damageAfterOneEnergy(pip: PokemonInPlay, defender: PokemonInPlay?): Int {
        val energy = pip.attachedEnergyCount + 1
        return pip.card.attacks.filter { energy >= it.convertedCost }
            .maxOfOrNull { effectiveDamage(it, pip, defender) } ?: 0
    }

    /** Cuánto AUMENTA el daño afordable de un Pokémon al unirle una energía más (0 si no mejora). */
    private fun marginalEnergyGain(pip: PokemonInPlay, defender: PokemonInPlay?): Int =
        (damageAfterOneEnergy(pip, defender) - currentAffordableDamage(pip, defender)).coerceAtLeast(0)

    /** ¿Es seguro que la IA active esta Habilidad? Solo las de "1 vez por turno" (sin bucles). */
    private fun isSafeAbility(state: GameState, side: Side, use: GameIntent.UseAbility): Boolean {
        val pip = state.sideState(side).allInPlay.firstOrNull { it.card.id == use.pokemon } ?: return false
        val ability = pip.card.abilities.firstOrNull { it.name.es == use.abilityName } ?: return false
        return engine.abilityIsOncePerTurn(ability)
    }

    private fun affordableAttacks(pip: PokemonInPlay): List<Attack> =
        pip.card.attacks.filter { canPayEnergyCost(pip.attachedEnergy, it.cost) }

    /**
     * Evolución de mayor IMPACTO: primero el ATACANTE Activo (sube el techo de quien va a golpear),
     * luego el de Banca con más energía unida (un atacante real en preparación) y, a igualdad, la
     * línea evolutiva con más PS. Evita "gastar" la evolución en un Pokémon de respaldo irrelevante.
     */
    private fun bestEvolve(
        state: GameState,
        side: Side,
        evos: List<GameIntent.Evolve>,
    ): GameIntent.Evolve {
        val me = state.sideState(side)
        val activeId = me.active?.card?.id
        return evos.maxWithOrNull(
            compareBy(
                { if (it.onto == activeId) 1 else 0 },
                { pipOf(me, it.onto)?.attachedEnergyCount ?: 0 },
                { (me.hand.firstOrNull { c -> c.id == it.evolution } as? PokemonCard)?.hp ?: 0 },
            ),
        ) ?: evos.first()
    }

    // ============================================================
    //  LOOKAHEAD DE 1 TURNO (Master)
    // ============================================================

    /**
     * Daño EFECTIVO máximo que el Activo RIVAL puede hacer a NUESTRO Activo el próximo turno con la
     * energía que YA tiene unida (lookahead de 1 ply). Prudente: ignora que el rival pudiera acelerar
     * energía o retirarse. Sirve para saber si nuestro Activo está "condenado" y decidir en consecuencia.
     */
    private fun incomingThreat(state: GameState, side: Side): Int {
        val myActive = state.sideState(side).active ?: return 0
        val oppActive = state.sideState(side.other()).active ?: return 0
        return affordableAttacks(oppActive).maxOfOrNull { effectiveDamage(it, oppActive, myActive) } ?: 0
    }

    // ============================================================
    //  USO INTELIGENTE DE ENTRENADORES (Master)
    // ============================================================

    /**
     * Elige el Entrenador de MAYOR valor a jugar ahora (robo/búsqueda/aceleración > gust útil >
     * curación), descartando los que no aportan nada este turno (p.ej. un gust sin KO que preparar).
     * Devuelve null si ninguno vale la pena → la IA pasa a unir energía / atacar.
     */
    private fun bestTrainer(
        state: GameState,
        side: Side,
        trainers: List<GameIntent.PlayTrainer>,
    ): GameIntent.PlayTrainer? {
        val me = state.sideState(side)
        val best = trainers
            .mapNotNull { intent ->
                (me.hand.firstOrNull { it.id == intent.card } as? TrainerCard)
                    ?.let { intent to trainerScore(state, side, it) }
            }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
        return best?.first
    }

    /**
     * Puntúa un Entrenador por su EFECTO (clasificado por sus ops, sin acoplarse a ids concretos),
     * con la secuenciación que recomiendan las guías expertas: robar/buscar/acelerar primero (ganar
     * información y recursos), gust SOLO cuando prepara un Noqueo ventajoso, curar si hay daño propio.
     */
    private fun trainerScore(state: GameState, side: Side, card: TrainerCard): Int {
        val me = state.sideState(side)
        // ESTADIO: juega el nuestro solo si NO hay ninguno en campo o el actual es del RIVAL (se lo
        // quitamos). Nunca reemplazamos nuestro propio Estadio (sería tirar una carta). Los efectos de
        // Estadio son continuos y no siempre aparecen como ops, así que se valoran aquí por separado.
        if (card.kind is TrainerKind.Stadium) {
            return if (state.stadium == null || state.stadiumOwner != side) 35 else 0
        }
        var score = 0
        for (op in engine.effectOps(card.effect)) {
            score += when (op) {
                is EffectOp.DrawCards, is EffectOp.DrawUntil, is EffectOp.CoinFlipDraw,
                EffectOp.ShuffleHandIntoDeck -> 50            // robo / renovar mano: ganar información pronto
                is EffectOp.SearchDeck -> 45                  // búsqueda: montar el tablero
                is EffectOp.AttachEnergyFromDiscard -> 60     // aceleración de energía: gana la carrera
                EffectOp.SwapOppActiveWithChosen,
                EffectOp.CoinFlipSwapOppActiveWithChosen,
                EffectOp.GustDefenderChooseNewActive -> gustValue(state, side) // gust: solo si prepara KO
                is EffectOp.Heal -> if (me.allInPlay.any { it.damage > 0 }) 30 else 0
                else -> 5                                      // otros efectos útiles: ligera preferencia
            }
        }
        return score
    }

    /**
     * Valor de un GUST (Órdenes de Jefe / Switch rival). Alto SOLO si arrastra al Puesto Activo un
     * Pokémon de Banca rival que NUESTRO Activo puede NOQUEAR este turno y que mejora el intercambio
     * de premios (más premios que el Activo actual, o cuando el Activo rival no es Noqueable ahora).
     * 0 en caso contrario: no se malgasta la carta. (Timing de Boss's Orders de las guías expertas.)
     */
    private fun gustValue(state: GameState, side: Side): Int {
        val me = state.sideState(side)
        val opp = state.sideState(side.other())
        val active = me.active ?: return 0
        fun canKo(target: PokemonInPlay): Boolean =
            affordableAttacks(active).any { effectiveDamage(it, active, target) >= target.remainingHp }
        val koableBench = opp.bench.filter { canKo(it) }
        if (koableBench.isEmpty()) return 0
        val activeKoable = opp.active?.let { canKo(it) } ?: false
        val bestTargetPrize = koableBench.maxOf { it.card.prizeValue }
        val oppActivePrize = opp.active?.card?.prizeValue ?: 0
        return if (!activeKoable || bestTargetPrize > oppActivePrize) 40 + bestTargetPrize * 10 else 0
    }

    /**
     * Daño EFECTIVO estimado de un ataque contra [defender], aplicando Debilidad/Resistencia
     * reales (reutiliza el cálculo del motor). Sin defensor conocido, devuelve el daño base.
     */
    private fun effectiveDamage(attack: Attack, attacker: PokemonInPlay, defender: PokemonInPlay?): Int {
        val base = attack.estimatedBaseDamage()
        if (base <= 0 || defender == null) return base
        return com.mineralord.tcg.engine.rules.Damage
            .calculate(base, attacker.card.types, defender).finalAmount
    }

    private fun pipOf(side: com.mineralord.tcg.engine.model.PlayerState, id: CardId): PokemonInPlay? =
        side.allInPlay.firstOrNull { it.card.id == id }

    private fun benchPip(side: com.mineralord.tcg.engine.model.PlayerState, id: CardId): PokemonInPlay? =
        side.bench.firstOrNull { it.card.id == id }

    /** HP restante de un Pokémon en juego (cualquier lado) por su id; alto si no se encuentra. */
    private fun remainingHpOf(state: GameState, id: CardId): Int =
        (state.player.allInPlay + state.opponent.allInPlay)
            .firstOrNull { it.card.id == id }?.remainingHp ?: Int.MAX_VALUE

    /** Premios que entrega al ser Noqueado el Pokémon con este id (1 normal, 2 ex/V, 3 VMAX); 0 si no está. */
    private fun prizeValueOf(state: GameState, id: CardId): Int =
        (state.player.allInPlay + state.opponent.allInPlay)
            .firstOrNull { it.card.id == id }?.card?.prizeValue ?: 0

    /** Pokémon en juego (cualquier lado) por id, o null. */
    private fun pipAnySide(state: GameState, id: CardId): PokemonInPlay? =
        (state.player.allInPlay + state.opponent.allInPlay).firstOrNull { it.card.id == id }

    /**
     * Ordena los candidatos de una elección de objetivo con criterio experto, distinguiendo dueño:
     *  - Candidatos RIVALES (gust / daño dirigido): primero los que NUESTRO Activo puede NOQUEAR ya
     *    (arrastrar el linchpin rematable), luego mayor valor en premios y, por último, menos PS.
     *  - Candidatos PROPIOS (cambio / curación): nuestro MEJOR atacante (más daño afordable; luego más PS).
     */
    private fun smartTargetOrder(state: GameState, side: Side, candidates: List<CardId>): List<CardId> {
        val me = state.sideState(side)
        val mineIds = me.allInPlay.map { it.card.id }.toSet()
        val defender = state.sideState(side.other()).active
        val targetsAreMine = candidates.all { it in mineIds }
        if (targetsAreMine) {
            return candidates.sortedWith(
                compareByDescending<CardId> { id -> pipOf(me, id)?.let { currentAffordableDamage(it, defender) } ?: -1 }
                    .thenByDescending { id -> remainingHpOf(state, id) },
            )
        }
        val active = me.active
        fun koable(id: CardId): Boolean {
            val pip = pipAnySide(state, id) ?: return false
            return active != null && affordableAttacks(active).any { effectiveDamage(it, active, pip) >= pip.remainingHp }
        }
        return candidates.sortedWith(
            compareByDescending<CardId> { if (koable(it)) 1 else 0 }
                .thenByDescending { id -> prizeValueOf(state, id) }
                .thenBy { id -> remainingHpOf(state, id) },
        )
    }

    private fun Side.other(): Side = if (this == Side.PLAYER) Side.OPPONENT else Side.PLAYER

    /**
     * Daño base ESTIMADO del ataque para valoración de la IA:
     *  - [Damage.Fixed]: su valor impreso.
     *  - [Damage.Variable] ("por cada…"): estimación prudente por coste (~20/energía), para que
     *    la IA NO ignore a evolucionados y atacantes de daño variable (causa de que antes solo
     *    usara ex/básicos de daño fijo alto).
     *  - [Damage.None] (puro efecto): 0.
     */
    private fun Attack.estimatedBaseDamage(): Int = when (val d = baseDamage) {
        is Damage.Fixed -> d.value
        Damage.Variable -> (convertedCost * 20).coerceAtLeast(20)
        Damage.None -> 0
    }
}
