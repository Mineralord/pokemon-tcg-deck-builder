package com.mineralord.tcg.data.netplay

import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.withId
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
    val cannotAttackOnTurn: Int? = null,
    val flipsToAttackOnTurn: Int? = null,
    val flipsToAttackCount: Int = 0,
    val preventDamageOnTurn: Int? = null,
    val preventBasicDamageOnTurn: Int? = null,
    val reflectDamageOnTurn: Int? = null,
    val cannotRetreatOnTurn: Int? = null,
    val damageReductionOnTurn: Int? = null,
    val damageReductionAmount: Int = 0,
    val attackBonusOnTurn: Int? = null,
    val attackBonusAmount: Int = 0,
    val retreatCostBumpOnTurn: Int? = null,
    val retreatCostBumpAmount: Int = 0,
    val attackCostBumpOnTurn: Int? = null,
    val attackCostBumpAmount: Int = 0,
    val delayedDamageOnTurn: Int? = null,
    val delayedDamageAmount: Int = 0,
    val weaknessOverrideType: String? = null,
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
    // Conteos de las zonas OCULTAS. En un snapshot censurado (para el rival) las
    // listas de ids van vacías y solo viaja el conteo (la UI las pinta como dorsos).
    // Por defecto = tamaño de la lista, para no alterar el caso local/IA.
    val handCount: Int = 0,
    val deckCount: Int = 0,
    val prizeCount: Int = 0,
)

/**
 * Decisión visible para el receptor (solo lo necesario para pintar el panel).
 * La "continuación" del efecto (remainingOps) NO viaja: la conserva el host
 * autoritativo, único que ejecuta el motor.
 */
@Serializable
enum class DecisionKindDto { CHOOSE_TARGETS, SEARCH_CARDS, MOVE_ENERGY, ATTACH_FROM_REVEALED, COIN_FLIP, PLACE_COUNTERS, CHOOSE_ENERGY_TYPE }

@Serializable
data class PendingDecisionDto(
    val kind: DecisionKindDto,
    val side: String,
    val promptEs: String,
    val promptEn: String,
    val candidates: List<String>,
    val count: Int,
    // Solo para COIN_FLIP: cartas a robar según cara/cruz.
    val ifHeads: Int = 0,
    val ifTails: Int = 0,
)

@Serializable
data class GameStateDto(
    val player: PlayerStateDto,
    val opponent: PlayerStateDto,
    val turn: Int,
    val activeSide: String,
    val phase: String,
    val stadium: String? = null,
    val stadiumOwner: String? = null,
    val winner: String? = null,
    val decision: PendingDecisionDto? = null,
    val supporterPlayedThisTurn: Boolean = false,
    val energyAttachedThisTurn: Boolean = false,
    val abilitiesUsedThisTurn: List<String> = emptyList(),
    /** Lados que deben promover un nuevo Activo tras un KO (en el marco del receptor). */
    val pendingPromotion: List<String> = emptyList(),
    /** Psyduck — Cavilar: lado cuyas monedas cuentan como cruz (en el marco del receptor) y turno. */
    val coinsAsTailsSide: String? = null,
    val coinsAsTailsOnTurn: Int? = null,
)

// ----------------------------------------------------------------- a DTO

/** Snapshot COMPLETO sin censura (uso local/IA: ambos lados en el mismo dispositivo). */
fun GameState.toDto(): GameStateDto = GameStateDto(
    player = player.toDto(),
    opponent = opponent.toDto(),
    turn = turn,
    activeSide = activeSide.name,
    phase = phase.name,
    stadium = stadium?.id?.raw,
    stadiumOwner = stadiumOwner?.name,
    winner = winner?.name,
    decision = interaction?.decision?.toDto(),
    supporterPlayedThisTurn = supporterPlayedThisTurn,
    energyAttachedThisTurn = energyAttachedThisTurn,
    abilitiesUsedThisTurn = abilitiesUsedThisTurn.map { it.raw },
    pendingPromotion = pendingPromotion.map { it.name },
    coinsAsTailsSide = coinsAsTailsSide?.name,
    coinsAsTailsOnTurn = coinsAsTailsOnTurn,
)

/**
 * Snapshot en el MARCO del [viewer]: el viewer se coloca como [Side.PLAYER] (abajo)
 * y el rival como [Side.OPPONENT] (arriba), con las zonas OCULTAS del rival
 * (mano/mazo/premios) censuradas a solo su conteo. Así el receptor reutiliza
 * `GameScreen` sin espejar y sin recibir información oculta del contrario.
 * Lo usa el HOST autoritativo para enviar a cada jugador su vista.
 */
fun GameState.toDtoFor(viewer: Side): GameStateDto {
    val other = if (viewer == Side.PLAYER) Side.OPPONENT else Side.PLAYER
    fun frame(s: Side): Side = if (s == viewer) Side.PLAYER else Side.OPPONENT
    return GameStateDto(
        player = sideState(viewer).toDto(frameSide = Side.PLAYER, censorHidden = false),
        opponent = sideState(other).toDto(frameSide = Side.OPPONENT, censorHidden = true),
        turn = turn,
        activeSide = frame(activeSide).name,
        phase = phase.name,
        stadium = stadium?.id?.raw,
        stadiumOwner = stadiumOwner?.let { frame(it).name },
        winner = winner?.let { frame(it).name },
        decision = interaction?.decision?.toDto(::frame),
        supporterPlayedThisTurn = supporterPlayedThisTurn,
        energyAttachedThisTurn = energyAttachedThisTurn,
        abilitiesUsedThisTurn = abilitiesUsedThisTurn.map { it.raw },
        pendingPromotion = pendingPromotion.map { frame(it).name },
        coinsAsTailsSide = coinsAsTailsSide?.let { frame(it).name },
        coinsAsTailsOnTurn = coinsAsTailsOnTurn,
    )
}

/**
 * @param frameSide nombre de lado a escribir en el DTO (para reencuadre de perspectiva).
 * @param censorHidden si true, mano/mazo/premios viajan SOLO como conteo (rival).
 *   El tablero (activo/banca), el descarte y la zona perdida son públicos: no se censuran.
 */
private fun PlayerState.toDto(
    frameSide: Side = side,
    censorHidden: Boolean = false,
) = PlayerStateDto(
    side = frameSide.name,
    active = active?.toDto(),
    bench = bench.map { it.toDto() },
    hand = if (censorHidden) emptyList() else hand.map { it.id.raw },
    deck = if (censorHidden) emptyList() else deck.map { it.id.raw },
    discard = discard.map { it.id.raw },
    lostZone = lostZone.map { it.id.raw },
    prizes = if (censorHidden) emptyList() else prizes.map { it.id.raw },
    prizesRemaining = prizesRemaining,
    handCount = hand.size,
    deckCount = deck.size,
    prizeCount = prizes.size,
)

private fun PokemonInPlay.toDto() = PokemonInPlayDto(
    card = card.id.raw,
    damage = damage,
    attachedEnergy = attachedEnergy.map { it.id.raw },
    attachedTools = attachedTools.map { it.id.raw },
    statuses = statuses.map { it.name },
    evolutionStack = evolutionStack.map { it.id.raw },
    turnsInPlay = turnsInPlay,
    cannotAttackOnTurn = cannotAttackOnTurn,
    flipsToAttackOnTurn = flipsToAttackOnTurn,
    flipsToAttackCount = flipsToAttackCount,
    preventDamageOnTurn = preventDamageOnTurn,
    preventBasicDamageOnTurn = preventBasicDamageOnTurn,
    reflectDamageOnTurn = reflectDamageOnTurn,
    cannotRetreatOnTurn = cannotRetreatOnTurn,
    damageReductionOnTurn = damageReductionOnTurn,
    damageReductionAmount = damageReductionAmount,
    attackBonusOnTurn = attackBonusOnTurn,
    attackBonusAmount = attackBonusAmount,
    retreatCostBumpOnTurn = retreatCostBumpOnTurn,
    retreatCostBumpAmount = retreatCostBumpAmount,
    attackCostBumpOnTurn = attackCostBumpOnTurn,
    attackCostBumpAmount = attackCostBumpAmount,
    delayedDamageOnTurn = delayedDamageOnTurn,
    delayedDamageAmount = delayedDamageAmount,
    weaknessOverrideType = weaknessOverrideType?.name,
)

private fun PendingDecision.toDto(frame: (Side) -> Side = { it }): PendingDecisionDto = when (this) {
    is PendingDecision.ChooseTargets -> PendingDecisionDto(
        DecisionKindDto.CHOOSE_TARGETS, frame(side).name, prompt.es, prompt.en, candidates.map { it.raw }, count,
    )
    is PendingDecision.SearchCards -> PendingDecisionDto(
        DecisionKindDto.SEARCH_CARDS, frame(side).name, prompt.es, prompt.en, candidates.map { it.raw }, count,
    )
    is PendingDecision.MoveEnergy -> PendingDecisionDto(
        DecisionKindDto.MOVE_ENERGY, frame(side).name, prompt.es, prompt.en,
        (fromCandidates + toCandidates).map { it.raw }, count,
    )
    // Render-only: el receptor pinta candidatos; el host conserva la continuación.
    is PendingDecision.AttachFromRevealed -> PendingDecisionDto(
        DecisionKindDto.ATTACH_FROM_REVEALED, frame(side).name, prompt.es, prompt.en,
        (energyCandidates + benchCandidates).map { it.raw }, maxAttach,
    )
    is PendingDecision.CoinFlip -> PendingDecisionDto(
        DecisionKindDto.COIN_FLIP, frame(side).name, prompt.es, prompt.en,
        emptyList(), 0, ifHeads = ifHeads, ifTails = ifTails,
    )
    // Render-only: el invitado ve un lanzamiento de moneda; el host lanza y encadena la
    // búsqueda a Banca (autoritativo). La SearchCards resultante viaja luego normalmente.
    is PendingDecision.CoinFlipThenSearch -> PendingDecisionDto(
        DecisionKindDto.COIN_FLIP, frame(side).name, prompt.es, prompt.en, emptyList(), 0,
    )
    // count = nº de contadores a repartir; candidates = Pokémon elegibles.
    is PendingDecision.PlaceCounters -> PendingDecisionDto(
        DecisionKindDto.PLACE_COUNTERS, frame(side).name, prompt.es, prompt.en, candidates.map { it.raw }, count,
    )
    // candidates = tipos elegibles codificados como CardId centinela ("energytype:XXX").
    is PendingDecision.ChooseEnergyType -> PendingDecisionDto(
        DecisionKindDto.CHOOSE_ENERGY_TYPE, frame(side).name, prompt.es, prompt.en, candidateIds.map { it.raw }, 1,
    )
    // Render-only: los ataques del Activo rival a copiar (Hackeo Genómico) viajan como candidatos
    // centinela; el host conserva la decisión real y re-ejecuta el ataque al resolver.
    is PendingDecision.ChooseAttack -> PendingDecisionDto(
        DecisionKindDto.CHOOSE_TARGETS, frame(side).name, prompt.es, prompt.en, candidateIds.map { it.raw }, 1,
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
        stadiumOwner = stadiumOwner?.let { Side.valueOf(it) },
        winner = winner?.let { Side.valueOf(it) },
        interaction = decisionModel?.let {
            PendingInteraction(decision = it, remainingOps = emptyList(), side = it.side, sourceId = null)
        },
        supporterPlayedThisTurn = supporterPlayedThisTurn,
        energyAttachedThisTurn = energyAttachedThisTurn,
        abilitiesUsedThisTurn = abilitiesUsedThisTurn.map { CardId(it) }.toSet(),
        pendingPromotion = pendingPromotion.map { Side.valueOf(it) }.toSet(),
        coinsAsTailsSide = coinsAsTailsSide?.let { Side.valueOf(it) },
        coinsAsTailsOnTurn = coinsAsTailsOnTurn,
    )
}

private fun PlayerStateDto.toModel(repo: CardRepository): PlayerState {
    // Zonas censuradas (rival): ids vacíos pero conteo>0 → se rellenan con una carta
    // placeholder (nunca se dibuja su arte: la UI las pinta como dorsos por tamaño).
    val filler = repo.all.firstOrNull()
    fun zone(ids: List<String>, count: Int): List<Card> =
        if (ids.isEmpty() && count > 0 && filler != null) List(count) { filler } else ids.cards(repo)
    return PlayerState(
        side = Side.valueOf(side),
        active = active?.toModel(repo),
        bench = bench.map { it.toModel(repo) },
        hand = zone(hand, handCount),
        deck = zone(deck, deckCount),
        discard = discard.cards(repo),
        lostZone = lostZone.cards(repo),
        prizes = zone(prizes, prizeCount),
        prizesRemaining = prizesRemaining,
    )
}

private fun PokemonInPlayDto.toModel(repo: CardRepository): PokemonInPlay {
    val pokemon = repo.byInstance(card) as? PokemonCard
        ?: error("Carta de Pokémon desconocida al rehidratar: $card")
    return PokemonInPlay(
        card = pokemon,
        damage = damage,
        attachedEnergy = attachedEnergy.mapNotNull { repo.byInstance(it) as? EnergyCard },
        attachedTools = attachedTools.mapNotNull { repo.byInstance(it) as? TrainerCard },
        statuses = statuses.map { Status.valueOf(it) }.toSet(),
        evolutionStack = evolutionStack.mapNotNull { repo.byInstance(it) as? PokemonCard },
        turnsInPlay = turnsInPlay,
        cannotAttackOnTurn = cannotAttackOnTurn,
        flipsToAttackOnTurn = flipsToAttackOnTurn,
        flipsToAttackCount = flipsToAttackCount,
        preventDamageOnTurn = preventDamageOnTurn,
        preventBasicDamageOnTurn = preventBasicDamageOnTurn,
        cannotRetreatOnTurn = cannotRetreatOnTurn,
        damageReductionOnTurn = damageReductionOnTurn,
        damageReductionAmount = damageReductionAmount,
        attackBonusOnTurn = attackBonusOnTurn,
        attackBonusAmount = attackBonusAmount,
        retreatCostBumpOnTurn = retreatCostBumpOnTurn,
        retreatCostBumpAmount = retreatCostBumpAmount,
        attackCostBumpOnTurn = attackCostBumpOnTurn,
        attackCostBumpAmount = attackCostBumpAmount,
        delayedDamageOnTurn = delayedDamageOnTurn,
        delayedDamageAmount = delayedDamageAmount,
        weaknessOverrideType = weaknessOverrideType?.let { EnergyType.valueOf(it) },
    )
}

/**
 * Resuelve una carta por su id de INSTANCIA (p. ej. `sv1-170#3`): busca la carta
 * IMPRESA en el repo (indexado por id impreso) y le reaplica el id de instancia,
 * preservando la identidad única a través del viaje por la red. Sin esto, un
 * `repo[CardId(instancia)]` devuelve null y la rehidratación reventaba (crash).
 */
private fun CardRepository.byInstance(raw: String): Card? {
    val cid = CardId(raw)
    return this[cid.printed]?.withId(cid)
}

/**
 * Rehidrata la decisión para el receptor. La mayoría son RENDER-ONLY como
 * [PendingDecision.ChooseTargets] (el panel solo usa prompt+candidatos+count),
 * salvo COIN_FLIP, que necesita su propio tipo para mostrar el lanzamiento
 * interactivo de la moneda en el invitado.
 */
private fun PendingDecisionDto.toModel(): PendingDecision = when (kind) {
    DecisionKindDto.COIN_FLIP -> PendingDecision.CoinFlip(
        side = Side.valueOf(side),
        prompt = LocalizedText(es = promptEs, en = promptEn),
        ifHeads = ifHeads,
        ifTails = ifTails,
    )
    DecisionKindDto.PLACE_COUNTERS -> PendingDecision.PlaceCounters(
        side = Side.valueOf(side),
        prompt = LocalizedText(es = promptEs, en = promptEn),
        candidates = candidates.map { CardId(it) },
        count = count,
    )
    DecisionKindDto.CHOOSE_ENERGY_TYPE -> PendingDecision.ChooseEnergyType(
        side = Side.valueOf(side),
        prompt = LocalizedText(es = promptEs, en = promptEn),
        candidates = candidates.mapNotNull { PendingDecision.decodeType(CardId(it)) },
    )
    else -> PendingDecision.ChooseTargets(
        side = Side.valueOf(side),
        prompt = LocalizedText(es = promptEs, en = promptEn),
        candidates = candidates.map { CardId(it) },
        count = count,
    )
}

private fun List<String>.cards(repo: CardRepository) = mapNotNull { repo.byInstance(it) }
