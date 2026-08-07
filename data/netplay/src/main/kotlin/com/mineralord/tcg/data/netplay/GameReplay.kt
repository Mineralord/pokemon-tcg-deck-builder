package com.mineralord.tcg.data.netplay

import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.withId
import com.mineralord.tcg.engine.rules.GameEngine
import com.mineralord.tcg.engine.rules.GameSetup
import com.mineralord.tcg.engine.rules.SeededRng
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Registro serializable de una partida ONLINE (event sourcing, §A del plan). El HOST
 * comparte UN solo [SeededRng] entre el reparto (dealCounting) y el motor (GameEngine),
 * por lo que capturando **la semilla** + la preparación + los intents aceptados EN ORDEN,
 * la partida se reconstruye por completo de forma **determinista** (incluidos los volados
 * en juego). Es la lección de PTCG Sim: transmitir la decisión aleatoria (la semilla),
 * no su resultado.
 *
 * Habilita: replay, export/import (estilo `?key=` de PTCG Sim) y diagnóstico de desyncs.
 * La perspectiva es la del HOST (host = [Side.PLAYER], guest = [Side.OPPONENT]).
 */
@Serializable
data class GameReplay(
    val seed: Long,
    val hostDeck: List<String>, // ids IMPRESOS del mazo del host
    val guestDeck: List<String>, // ids IMPRESOS del mazo del guest
    val firstSideIsHost: Boolean,
    val hostActiveId: String,
    val hostBenchIds: List<String>,
    val guestActiveId: String,
    val guestBenchIds: List<String>,
    val intents: List<GameIntentDto> = emptyList(),
) {
    fun encode(): String = ReplayJson.encodeToString(serializer(), this)

    companion object {
        fun decode(raw: String): GameReplay = ReplayJson.decodeFromString(serializer(), raw)
    }
}

private val ReplayJson: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** Rehidrata un mazo de ids IMPRESOS a cartas con id de INSTANCIA único (base disjunta),
 *  igual que `OnlineGameController.buildDeck`, para que las instancias coincidan. */
private fun buildDeck(printedIds: List<String>, base: Int, repo: CardRepository) =
    printedIds.mapNotNull { repo[CardId(it)] }
        .mapIndexed { i, c -> c.withId(c.id.withInstance(base + i)) }

/**
 * Reproduce la preparación con [seed] y devuelve el estado inicial (fase MAIN, antes del
 * primer intent) junto al [SeededRng] YA avanzado por los repartos —listo para sembrar el
 * motor y reproducir los volados en juego con la misma secuencia que el host.
 */
private fun GameReplay.rebuildStart(repo: CardRepository): Pair<GameState, SeededRng> {
    val rng = SeededRng(seed)
    val hostCards = buildDeck(hostDeck, base = 0, repo = repo)
    val guestCards = buildDeck(guestDeck, base = 100_000, repo = repo)
    val (hd, hm) = GameSetup.dealCounting(hostCards, rng)
    val (gd, gm) = GameSetup.dealCounting(guestCards, rng)
    var gs = GameSetup.finish(
        player = GameSetup.SideChoice(hd, CardId(hostActiveId), hostBenchIds.map { CardId(it) }),
        opponent = GameSetup.SideChoice(gd, CardId(guestActiveId), guestBenchIds.map { CardId(it) }),
        firstSide = if (firstSideIsHost) Side.PLAYER else Side.OPPONENT,
    )
    // Compensación por mulligan (idéntica a maybeFinishHost).
    gs = GameSetup.drawExtra(gs, Side.OPPONENT, hm)
    gs = GameSetup.drawExtra(gs, Side.PLAYER, gm)
    return gs to rng
}

/** Estado inicial reconstruido (tras la preparación, antes del primer intent). */
fun GameReplay.reconstructInitial(repo: CardRepository): GameState =
    rebuildStart(repo).first

/**
 * Reconstrucción COMPLETA: pliega los [intents] aceptados sobre un motor sembrado con el
 * MISMO rng que ya avanzó con el reparto. Los intents ilegales (no deberían existir en un
 * log autoritativo) se ignoran, igual que hacía el host.
 */
fun GameReplay.reconstructFinal(repo: CardRepository): GameState {
    var (gs, rng) = rebuildStart(repo)
    val engine = GameEngine(rng)
    for (dto in intents) {
        val res = engine.apply(gs, dto.toIntent())
        if (res.accepted) gs = res.state
    }
    return gs
}
