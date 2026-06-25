package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.Phase
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side

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
}
