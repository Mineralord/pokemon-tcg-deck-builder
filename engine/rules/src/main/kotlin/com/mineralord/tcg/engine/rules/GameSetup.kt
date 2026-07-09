package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.Phase
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import com.mineralord.tcg.engine.model.withId

/**
 * Preparación de una partida a partir de dos mazos. Versión simplificada (sin
 * mulligan ni elección de Activo) pensada para el arnés de self-play y los
 * tests: baraja con el [Rng], coloca un Básico en Activo y hasta dos en Banca,
 * reparte premios y una mano inicial, y deja el resto como mazo.
 *
 * La preparación completa (robar 7, mulligan, colocar Básicos manualmente) se
 * implementará en la fase de UI; aquí solo necesitamos un estado válido y
 * determinista para validar el motor.
 */
object GameSetup {

    fun start(
        playerDeck: List<Card>,
        opponentDeck: List<Card>,
        rng: Rng,
        prizes: Int = 6,
        handSize: Int = 5,
    ): GameState = GameState(
        player = buildSide(Side.PLAYER, playerDeck, rng, prizes, handSize),
        opponent = buildSide(Side.OPPONENT, opponentDeck, rng, prizes, handSize),
        turn = 1,
        activeSide = Side.PLAYER,
        phase = Phase.MAIN,
    )

    private fun buildSide(side: Side, deck: List<Card>, rng: Rng, prizes: Int, handSize: Int): PlayerState {
        val shuffled = rng.shuffle(deck)
        val basics = shuffled.filterIsInstance<PokemonCard>().filter { it.isBasic }
        require(basics.isNotEmpty()) { "El mazo de $side no tiene ningún Pokémon Básico" }

        val active = basics.first()
        val benchBasics = basics.drop(1).take(GameEngine.BENCH_LIMIT.coerceAtMost(2))
        val placed = (listOf(active) + benchBasics).toSet()

        val rest = shuffled.filterNot { it in placed }
        val prizeCards = rest.take(prizes)
        val afterPrizes = rest.drop(prizes)
        val hand = afterPrizes.take(handSize)
        val library = afterPrizes.drop(handSize)

        return PlayerState(
            side = side,
            // turnsInPlay = 1: simplificación del arnés (pueden actuar de inmediato).
            active = PokemonInPlay(active, turnsInPlay = 1),
            bench = benchBasics.map { PokemonInPlay(it, turnsInPlay = 1) },
            hand = hand,
            deck = library,
            prizes = prizeCards,
            prizesRemaining = prizes,
        )
    }

    // ----------------------------------------------------- preparación interactiva

    /** Mano inicial repartida (con al menos un Básico) y el resto del mazo. */
    data class DealtSide(val hand: List<Card>, val deck: List<Card>)

    /** Elección de un lado tras el reparto: Activo y Banca escogidos de la mano. */
    data class SideChoice(
        val dealt: DealtSide,
        val activeId: CardId,
        val benchIds: List<CardId> = emptyList(),
    )

    /**
     * Reparte una mano inicial válida barajando con [rng] y resolviendo el
     * **mulligan automáticamente**: si las primeras [handSize] cartas no incluyen
     * ningún Pokémon Básico, rebaraja y vuelve a robar (hasta [maxMulligans]).
     * Devuelve la mano y el resto del mazo, sin colocar nada aún.
     */
    fun deal(deck: List<Card>, rng: Rng, handSize: Int = 7, maxMulligans: Int = 100): DealtSide {
        require(deck.any { it is PokemonCard && it.isBasic }) {
            "El mazo no tiene ningún Pokémon Básico"
        }
        var shuffled = rng.shuffle(deck)
        var guard = 0
        while (shuffled.take(handSize).none { it is PokemonCard && it.isBasic } && guard++ < maxMulligans) {
            shuffled = rng.shuffle(deck)
        }
        return DealtSide(shuffled.take(handSize), shuffled.drop(handSize))
    }

    /**
     * Igual que [deal] pero DEVOLVIENDO cuántos **mulligans** hubo (rebarajas por no
     * tener ningún Básico en la mano inicial). El motor usa ese conteo para la
     * compensación oficial: el rival de quien hace mulligan roba 1 carta extra por cada uno.
     */
    fun dealCounting(deck: List<Card>, rng: Rng, handSize: Int = 7, maxMulligans: Int = 100): Pair<DealtSide, Int> {
        require(deck.any { it is PokemonCard && it.isBasic }) {
            "El mazo no tiene ningún Pokémon Básico"
        }
        var shuffled = rng.shuffle(deck)
        var mulligans = 0
        while (shuffled.take(handSize).none { it is PokemonCard && it.isBasic } && mulligans < maxMulligans) {
            shuffled = rng.shuffle(deck)
            mulligans++
        }
        return DealtSide(shuffled.take(handSize), shuffled.drop(handSize)) to mulligans
    }

    /**
     * Roba [n] cartas del tope del mazo de [side] a su mano (compensación por el
     * mulligan del rival). No hace nada si [n] <= 0 o el mazo se queda sin cartas.
     */
    fun drawExtra(state: GameState, side: Side, n: Int): GameState {
        if (n <= 0) return state
        val ps = state.sideState(side)
        val real = minOf(n, ps.deck.size)
        if (real == 0) return state
        val drawn = ps.deck.take(real)
        val updated = ps.copy(deck = ps.deck.drop(real), hand = ps.hand + drawn)
        return if (side == Side.PLAYER) state.copy(player = updated) else state.copy(opponent = updated)
    }

    /** Pokémon Básicos de una mano repartida (candidatos a Activo/Banca). */
    fun basicsIn(dealt: DealtSide): List<PokemonCard> =
        dealt.hand.filterIsInstance<PokemonCard>().filter { it.isBasic }

    /**
     * Elección automática para la IA/rival: el Básico con más PS como Activo y el
     * resto de Básicos (hasta [GameEngine.BENCH_LIMIT]) en Banca.
     */
    fun autoChoose(dealt: DealtSide): SideChoice {
        val basics = basicsIn(dealt)
        require(basics.isNotEmpty()) { "La mano no tiene ningún Pokémon Básico" }
        val active = basics.maxByOrNull { it.hp } ?: basics.first()
        val bench = basics.filter { it.id != active.id }.take(GameEngine.BENCH_LIMIT).map { it.id }
        return SideChoice(dealt, active.id, bench)
    }

    /**
     * Estado **provisional** para la preparación EN EL TABLERO: coloca lo que el
     * jugador lleva elegido (Activo y/o Banca, ambos pueden estar vacíos mientras
     * decide) y al rival ya colocado (la UI lo pinta boca abajo). Aún **NO** se
     * reparten premios: eso ocurre al confirmar (LISTO) en [finish]. Las cartas
     * colocadas salen de la mano, igual que en [finish].
     */
    fun provisional(
        player: DealtSide,
        playerActiveId: CardId?,
        playerBenchIds: List<CardId>,
        opponent: SideChoice,
    ): GameState = GameState(
        player = buildProvisionalPlayer(player, playerActiveId, playerBenchIds),
        // El rival se coloca ya (para dibujar sus dorsos), pero sin premios todavía.
        opponent = buildChosenSide(Side.OPPONENT, opponent, prizes = 0),
        turn = 1,
        activeSide = Side.PLAYER,
        phase = Phase.SETUP,
    )

    private fun buildProvisionalPlayer(
        dealt: DealtSide,
        activeId: CardId?,
        benchIds: List<CardId>,
    ): PlayerState {
        val activeCard = activeId?.let { id -> dealt.hand.firstOrNull { it.id == id } as? PokemonCard }
            ?.takeIf { it.isBasic }
        val benchCards = benchIds
            .mapNotNull { id -> dealt.hand.firstOrNull { it.id == id } as? PokemonCard }
            .filter { it.isBasic }
            .take(GameEngine.BENCH_LIMIT)
        val placed = (listOfNotNull(activeCard) + benchCards).map { it.id }.toSet()
        val handRest = dealt.hand.filterNot { it.id in placed }
        return PlayerState(
            side = Side.PLAYER,
            active = activeCard?.let { PokemonInPlay(it, turnsInPlay = 1) },
            bench = benchCards.map { PokemonInPlay(it, turnsInPlay = 1) },
            hand = handRest,
            // Aún sin repartir premios: el mazo conserva todas las cartas no colocadas.
            deck = dealt.deck,
            prizes = emptyList(),
            prizesRemaining = 0,
        )
    }

    /**
     * Finaliza la preparación una vez que ambos lados eligieron Activo y Banca:
     * retira esas cartas de la mano, reparte [prizes] premios del resto del mazo y
     * arranca la partida en [Phase.MAIN] con [firstSide] en turno.
     */
    fun finish(
        player: SideChoice,
        opponent: SideChoice,
        firstSide: Side = Side.PLAYER,
        prizes: Int = 6,
    ): GameState = GameState(
        player = buildChosenSide(Side.PLAYER, player, prizes),
        opponent = buildChosenSide(Side.OPPONENT, opponent, prizes),
        turn = 1,
        activeSide = firstSide,
        phase = Phase.MAIN,
    )

    // ----------------------------------------------------- depuración / verificación

    /**
     * Estado de DEPURACIÓN para verificar la maquetación del tablero: arranca ya en
     * [Phase.MAIN] con la **Banca LLENA (5)** en ambos lados, sin pasar por moneda /
     * reparto / preparación. No depende de que la mano tenga suficientes Básicos:
     * toma los Básicos del mazo y, si hay menos de 6 (1 Activo + 5 Banca), los
     * **repite** para forzar siempre 5 en Banca. Pensado solo para inspección visual
     * (panal 3+2), no para jugar de verdad.
     */
    fun debugFullBench(
        playerDeck: List<Card>,
        opponentDeck: List<Card>,
        rng: Rng,
        prizes: Int = 6,
    ): GameState = GameState(
        player = buildDebugFullBench(Side.PLAYER, playerDeck, rng, prizes),
        opponent = buildDebugFullBench(Side.OPPONENT, opponentDeck, rng, prizes),
        turn = 1,
        activeSide = Side.PLAYER,
        phase = Phase.MAIN,
    )

    private fun buildDebugFullBench(side: Side, deck: List<Card>, rng: Rng, prizes: Int): PlayerState {
        val shuffled = rng.shuffle(deck)
        var basics = shuffled.filterIsInstance<PokemonCard>().filter { it.isBasic }
        require(basics.isNotEmpty()) { "El mazo de $side no tiene ningún Pokémon Básico" }
        // Garantiza 6 (Activo + 5 Banca) repitiendo la lista si el mazo no tiene tantos.
        while (basics.size < GameEngine.BENCH_LIMIT + 1) basics = basics + basics
        val active0 = basics[0]
        val bench0 = basics.subList(1, GameEngine.BENCH_LIMIT + 1) // 5 cartas

        val placed = (listOf(active0) + bench0).toSet()
        val rest = shuffled.filterNot { it in placed }
        // Re-id: el padding (basics+basics) puede reusar la MISMA instancia; cada
        // Pokémon colocado debe tener id único para ser independiente en juego.
        val active = active0.withId(active0.id.withInstance(9000)) as PokemonCard
        val bench = bench0.mapIndexed { i, c -> c.withId(c.id.withInstance(9001 + i)) as PokemonCard }
        val prizeCards = rest.take(prizes)
        val pool = rest.drop(prizes)
        // Mano de depuración: al frente unas ENERGÍAS y, si existen en el mazo, unas
        // EVOLUCIONES de los Pokémon ya en juego, para probar el arrastre de energía →
        // Pokémon y de evolución → su pre-evolución.
        val placedNames = (listOf(active) + bench).flatMap { listOf(it.name.es, it.name.en) }.toSet()
        val energies = pool.filterIsInstance<EnergyCard>().take(2)
        val evolutions = pool.filterIsInstance<PokemonCard>()
            .filter { !it.isBasic && it.evolvesFrom in placedNames }
            .distinctBy { it.evolvesFrom }
            .take(2)
        val trainers = pool.filterIsInstance<TrainerCard>()
            .filter { it.kind is TrainerKind.Supporter || it.kind is TrainerKind.Item }
            .take(1)
        val seeded = (energies + evolutions + trainers)
        val others = pool.filterNot { it in seeded }
        val hand = (seeded + others).take(5)
        val library = pool.filterNot { it in hand }

        return PlayerState(
            side = side,
            active = PokemonInPlay(active, turnsInPlay = 1),
            bench = bench.map { PokemonInPlay(it, turnsInPlay = 1) },
            hand = hand,
            deck = library,
            prizes = prizeCards,
            prizesRemaining = prizes,
        )
    }

    private fun buildChosenSide(side: Side, choice: SideChoice, prizes: Int): PlayerState {
        val hand = choice.dealt.hand
        val activeCard = hand.firstOrNull { it.id == choice.activeId } as? PokemonCard
            ?: error("El Activo elegido no está en la mano de $side")
        require(activeCard.isBasic) { "El Activo elegido no es un Pokémon Básico" }
        val benchCards = choice.benchIds
            .mapNotNull { id -> hand.firstOrNull { it.id == id } as? PokemonCard }
            .filter { it.isBasic }
            .take(GameEngine.BENCH_LIMIT)

        val placed = (listOf(activeCard) + benchCards).map { it.id }.toSet()
        val handRest = hand.filterNot { it.id in placed }

        // Los premios salen del tope del mazo restante; el resto queda como mazo.
        val prizeCards = choice.dealt.deck.take(prizes)
        val library = choice.dealt.deck.drop(prizes)

        return PlayerState(
            side = side,
            active = PokemonInPlay(activeCard, turnsInPlay = 1),
            bench = benchCards.map { PokemonInPlay(it, turnsInPlay = 1) },
            hand = handRest,
            deck = library,
            prizes = prizeCards,
            prizesRemaining = prizes,
        )
    }
}
