package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side

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
    private val retreats get() = difficulty >= Difficulty.MASTERBALL

    override fun decide(state: GameState, side: Side): GameIntent {
        // 0) Nuestro Activo fue Noqueado: promover un Pokémon de la Banca.
        if (side in state.pendingPromotion) {
            val bench = state.sideState(side).bench
            val best = when {
                bench.isEmpty() -> null
                smartPromotion -> bench.maxByOrNull { pip -> promotionScore(pip) }
                else -> bench.firstOrNull()
            }
            if (best != null) return GameIntent.PromoteActive(best.card.id)
        }

        // 1) Hay una decisión pendiente nuestra: resolverla con una elección legal.
        state.interaction?.let { inter ->
            if (inter.side == side) return resolveDecision(inter.decision, state, side)
        }

        val legal = engine.legalIntents(state)
        val me = state.sideState(side)
        val active = me.active

        // 2) DESARROLLO: bajar Básicos a la Banca. Las gamas altas la LLENAN (más atacantes y
        //    respaldo); las bajas solo bajan uno cuando la Banca está vacía.
        if (developsBench || me.bench.isEmpty()) {
            legal.firstOrNull { it is GameIntent.PlayBasicToBench }?.let { return it }
        }

        // 3) Evolucionar (gamas altas): sube el techo de daño/HP; hazlo antes de atacar.
        if (evolves) {
            legal.firstOrNull { it is GameIntent.Evolve }?.let { return it }
        }

        // 4) Jugar un Entrenador (robo/búsqueda/gust) ANTES de atacar: cava cartas y prepara el
        //    Noqueo. (Atacar cierra el turno, así que los Entrenadores van primero.)
        if (usesTrainers) {
            legal.firstOrNull { it is GameIntent.PlayTrainer }?.let { return it }
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

        // 7) RETIRADA (Master): si el Activo no puede atacar este turno pero un Pokémon de la
        //    Banca sí, retírate promoviendo a ESE atacante (reposicionamiento agresivo).
        if (retreats && active != null && affordableAttacks(active).isEmpty()) {
            val retreatTargets = legal.filterIsInstance<GameIntent.Retreat>()
            val best = retreatTargets
                .mapNotNull { r -> benchPip(me, r.benchTarget)?.let { r to bestAffordableDamage(it) } }
                .filter { it.second > 0 }
                .maxByOrNull { it.second }
            if (best != null) return best.first
        }

        // 8) ATACAR (cierra el turno). La elección depende de la dificultad.
        val affordable = active?.let { affordableAttacks(it) }.orEmpty()
        if (affordable.isNotEmpty()) {
            val oppActiveHp = state.sideState(side.other()).active?.remainingHp
            val chosen = when (difficulty) {
                // Novata: el ataque más flojo (menor coste; a igualdad, menor daño).
                Difficulty.POKEBALL -> affordable.minWithOrNull(
                    compareBy({ it.convertedCost }, { it.fixedDamage() }),
                )
                // Básica: lo primero pagable, sin pensar.
                Difficulty.SUPERBALL -> affordable.first()
                // Táctica: el ataque de más DAÑO pagable.
                Difficulty.ULTRABALL -> affordable.maxByOrNull { it.fixedDamage() }
                    ?: affordable.maxByOrNull { it.convertedCost }
                // Experta: si hay un ataque que Noquea al Activo rival, ese; si no, el de más daño.
                Difficulty.MASTERBALL -> {
                    val lethal = oppActiveHp?.let { hp ->
                        affordable.filter { it.fixedDamage() >= hp }.minByOrNull { it.convertedCost }
                    }
                    lethal ?: affordable.maxByOrNull { it.fixedDamage() }
                        ?: affordable.maxByOrNull { it.convertedCost }
                }
            }
            if (chosen != null) return GameIntent.Attack(chosen.name.es)
        }

        // 9) Nada productivo: terminar el turno.
        return GameIntent.EndTurn
    }

    /** Escoge una respuesta legal mínima para cada tipo de decisión. */
    private fun resolveDecision(decision: PendingDecision, state: GameState, side: Side): GameIntent = when (decision) {
        is PendingDecision.ChooseTargets -> {
            // Experta: prioriza objetivos con menos HP restante (rematar). Resto: los primeros.
            val ordered = if (difficulty >= Difficulty.MASTERBALL) {
                decision.candidates.sortedBy { id -> remainingHpOf(state, id) }
            } else {
                decision.candidates
            }
            GameIntent.ResolveDecision(ordered.take(decision.count))
        }
        // Hackeo Genómico: copia el ataque de MÁS daño fijo del Activo rival (o el primero).
        is PendingDecision.ChooseAttack -> {
            val attacks = state.sideState(side.other()).active?.card?.attacks.orEmpty()
            val bestIdx = attacks.indices.maxByOrNull { attacks[it].fixedDamage() } ?: 0
            GameIntent.ResolveDecision(listOf(PendingDecision.encodeAttackIndex(bestIdx)))
        }
        // Reparte contadores: uno por objetivo empezando por el de menos HP (round-robin
        // sobre los candidatos ordenados) hasta agotar los [count] contadores.
        is PendingDecision.PlaceCounters -> {
            val ordered = if (difficulty >= Difficulty.MASTERBALL) {
                decision.candidates.sortedBy { id -> remainingHpOf(state, id) }
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

    /** Elige a qué Pokémon unir la energía para MAXIMIZAR el daño de un ataque pagable tras
     *  recibirla; a igualdad, prefiere el Activo y a quien esté más cerca de completar el coste. */
    private fun bestEnergyAttach(
        state: GameState,
        side: Side,
        attaches: List<GameIntent.AttachEnergy>,
    ): GameIntent.AttachEnergy {
        val me = state.sideState(side)
        val activeId = me.active?.card?.id
        return attaches.maxWithOrNull(
            compareBy(
                { att -> pipOf(me, att.to)?.let { damageAfterOneEnergy(it) } ?: 0 },
                { att -> if (att.to == activeId) 1 else 0 },
                { att -> pipOf(me, att.to)?.attachedEnergyCount ?: 0 },
            ),
        ) ?: attaches.first()
    }

    /** Mejor daño pagable de un Pokémon SI recibiera una energía más. */
    private fun damageAfterOneEnergy(pip: PokemonInPlay): Int {
        val energy = pip.attachedEnergyCount + 1
        return pip.card.attacks.filter { energy >= it.convertedCost }
            .maxOfOrNull { it.fixedDamage() } ?: 0
    }

    /** Puntuación para promover tras KO: prioriza un atacante ya listo; luego, más HP. */
    private fun promotionScore(pip: PokemonInPlay): Int =
        bestAffordableDamage(pip) * 1000 + pip.remainingHp

    private fun affordableAttacks(pip: PokemonInPlay): List<Attack> =
        pip.card.attacks.filter { pip.attachedEnergyCount >= it.convertedCost }

    private fun bestAffordableDamage(pip: PokemonInPlay): Int =
        affordableAttacks(pip).maxOfOrNull { it.fixedDamage() } ?: 0

    private fun pipOf(side: com.mineralord.tcg.engine.model.PlayerState, id: CardId): PokemonInPlay? =
        side.allInPlay.firstOrNull { it.card.id == id }

    private fun benchPip(side: com.mineralord.tcg.engine.model.PlayerState, id: CardId): PokemonInPlay? =
        side.bench.firstOrNull { it.card.id == id }

    /** HP restante de un Pokémon en juego (cualquier lado) por su id; alto si no se encuentra. */
    private fun remainingHpOf(state: GameState, id: CardId): Int =
        (state.player.allInPlay + state.opponent.allInPlay)
            .firstOrNull { it.card.id == id }?.remainingHp ?: Int.MAX_VALUE

    private fun Side.other(): Side = if (this == Side.PLAYER) Side.OPPONENT else Side.PLAYER

    /** Daño fijo del ataque (0 para ataques variables o puramente de efecto). */
    private fun Attack.fixedDamage(): Int = (baseDamage as? Damage.Fixed)?.value ?: 0
}
