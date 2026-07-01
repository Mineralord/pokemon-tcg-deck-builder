package com.mineralord.tcg.data.netplay

import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.model.PendingInteraction
import com.mineralord.tcg.engine.model.Phase
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Status
import com.mineralord.tcg.engine.model.TrainerCard
import kotlinx.serialization.Serializable

/**
 * DTOs serializables del estado de juego para viajar por la red.
 *
 * Diseño (§Fase 0 del plan): el snapshot transporta **solo ids de carta**; el
 * receptor rehidrata los objetos [com.mineralord.tcg.engine.model.Card] con su
 * [CardRepository] local (ambos dispositivos tienen la misma `cartas-db.json`).
 * Así el payload es pequeño y estable. El estado dinámico (daño, energías,
 * estados, premios, fase…) sí viaja completo.
 */
@Serializable
data class PokemonInPlayDto(
    val card: String,
    val damage: Int = 0,
    val attachedEnergy: List<String> = emptyList(),
    val attachedTools: List<String> = emptyList(),
    val statuses: List<String> = emptyList(),
    val evolutionStack: List<String> = emptyList(),
    val turnsInPlay: Int = 0,
)

@Serializable
data class PlayerStateDto(
    val side: String,
    val active: PokemonInPlayDto?,
    val bench: List<PokemonInPlayDto> = emptyList(),
    val hand: List<String> = emptyList(),
    val deck: List<String> = emptyList(),
    val discard: List<String> = emptyList(),
    val lostZone: List<String> = emptyList(),
    val prizes: List<String> = emptyList(),
    val prizesRemaining: Int = 0,
)

/**
 * Decisión visible para el receptor (solo lo necesario para pintar el panel).
 * La "continuación" del efecto (remainingOps) NO viaja: la conserva el host
 * autoritativo, único que ejecuta el motor.
 */
@Serializable
enum class DecisionKindDto { CHOOSE_TARGETS, SEARCH_CARDS, MOVE_ENERGY }

@Serializable
data class PendingDecisionDto(
    val kind: DecisionKindDto,
    val side: String,
    val promptEs: String,
    val promptEn: String,
    val candidates: List<String>,
    val count: Int,
)

@Serializable
data class GameStateDto(
    val player: PlayerStateDto,
    val opponent: PlayerStateDto,
    val turn: Int,
    val activeSide: String,
    val phase: String,
    val stadium: String? = null,
    val winner: String? = null,
    val decision: PendingDecisionDto? = null,
    val supporterPlayedThisTurn: Boolean = false,
    val energyAttachedThisTurn: Boolean = false,
    val abilitiesUsedThisTurn: List<String> = emptyList(),
)

// ----------------------------------------------------------------- a DTO

fun GameState.toDto(): GameStateDto = GameStateDto(
    player = player.toDto(),
    opponent = opponent.toDto(),
    turn = turn,
    activeSide = activeSide.name,
    phase = phase.name,
    stadium = stadium?.id?.raw,
    winner = winner?.name,
    decision = interaction?.decision?.toDto(),
    supporterPlayedThisTurn = supporterPlayedThisTurn,
    energyAttachedThisTurn = energyAttachedThisTurn,
    abilitiesUsedThisTurn = abilitiesUsedThisTurn.map { it.raw },
)

private fun PlayerState.toDto() = PlayerStateDto(
    side = side.name,
    active = active?.toDto(),
    bench = bench.map { it.toDto() },
    hand = hand.map { it.id.raw },
    deck = deck.map { it.id.raw },
    discard = discard.map { it.id.raw },
    lostZone = lostZone.map { it.id.raw },
    prizes = prizes.map { it.id.raw },
    prizesRemaining = prizesRemaining,
)

private fun PokemonInPlay.toDto() = PokemonInPlayDto(
    card = card.id.raw,
    damage = damage,
    attachedEnergy = attachedEnergy.map { it.id.raw },
    attachedTools = attachedTools.map { it.id.raw },
    statuses = statuses.map { it.name },
    evolutionStack = evolutionStack.map { it.id.raw },
    turnsInPlay = turnsInPlay,
)

private fun PendingDecision.toDto(): PendingDecisionDto = when (this) {
    is PendingDecision.ChooseTargets -> PendingDecisionDto(
        DecisionKindDto.CHOOSE_TARGETS, side.name, prompt.es, prompt.en, candidates.map { it.raw }, count,
    )
    is PendingDecision.SearchCards -> PendingDecisionDto(
        DecisionKindDto.SEARCH_CARDS, side.name, prompt.es, prompt.en, candidates.map { it.raw }, count,
    )
    is PendingDecision.MoveEnergy -> PendingDecisionDto(
        DecisionKindDto.MOVE_ENERGY, side.name, prompt.es, prompt.en,
        (fromCandidates + toCandidates).map { it.raw }, count,
    )
}

// ----------------------------------------------------------------- de DTO

/**
 * Rehidrata un [GameState] para **renderizado** en el receptor usando [repo].
 *
 * Nota: la decisión pendiente se reconstruye sin su continuación (remainingOps
 * vacío) — suficiente para pintar el panel. El receptor nunca ejecuta el motor;
 * envía sus elecciones como [com.mineralord.tcg.engine.rules.GameIntent.ResolveDecision]
 * al host autoritativo.
 */
fun GameStateDto.toGameState(repo: CardRepository): GameState {
    val decisionModel = decision?.toModel()
    return GameState(
        player = player.toModel(repo),
        opponent = opponent.toModel(repo),
        turn = turn,
        activeSide = Side.valueOf(activeSide),
        phase = Phase.valueOf(phase),
        stadium = stadium?.let { repo[CardId(it)] as? TrainerCard },
        winner = winner?.let { Side.valueOf(it) },
        interaction = decisionModel?.let {
            PendingInteraction(decision = it, remainingOps = emptyList(), side = it.side, sourceId = null)
        },
        supporterPlayedThisTurn = supporterPlayedThisTurn,
        energyAttachedThisTurn = energyAttachedThisTurn,
        abilitiesUsedThisTurn = abilitiesUsedThisTurn.map { CardId(it) }.toSet(),
    )
}

private fun PlayerStateDto.toModel(repo: CardRepository) = PlayerState(
    side = Side.valueOf(side),
    active = active?.toModel(repo),
    bench = bench.map { it.toModel(repo) },
    hand = hand.cards(repo),
    deck = deck.cards(repo),
    discard = discard.cards(repo),
    lostZone = lostZone.cards(repo),
    prizes = prizes.cards(repo),
    prizesRemaining = prizesRemaining,
)

private fun PokemonInPlayDto.toModel(repo: CardRepository): PokemonInPlay {
    val pokemon = repo[CardId(card)] as? PokemonCard
        ?: error("Carta de Pokémon desconocida al rehidratar: $card")
    return PokemonInPlay(
        card = pokemon,
        damage = damage,
        attachedEnergy = attachedEnergy.mapNotNull { repo[CardId(it)] as? EnergyCard },
        attachedTools = attachedTools.mapNotNull { repo[CardId(it)] as? TrainerCard },
        statuses = statuses.map { Status.valueOf(it) }.toSet(),
        evolutionStack = evolutionStack.mapNotNull { repo[CardId(it)] as? PokemonCard },
        turnsInPlay = turnsInPlay,
    )
}

/** Rehidrata como [PendingDecision.ChooseTargets] (render-only: el panel solo usa prompt+candidatos+count). */
private fun PendingDecisionDto.toModel(): PendingDecision = PendingDecision.ChooseTargets(
    side = Side.valueOf(side),
    prompt = LocalizedText(es = promptEs, en = promptEn),
    candidates = candidates.map { CardId(it) },
    count = count,
)

private fun List<String>.cards(repo: CardRepository) = mapNotNull { repo[CardId(it)] }
