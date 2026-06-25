package com.mineralord.tcg.engine.events

import com.mineralord.tcg.engine.model.CardId

/**
 * Renderiza un [GameEvent] a la línea localizada del "REGISTRO DEL COMBATE".
 *
 * Es el consumidor (a) del stream de eventos. Toma resolvedores externos para
 * nombres de carta y de jugador, de modo que el módulo de eventos sigue siendo
 * Kotlin puro y agnóstico de datos. El cliente decide idioma vía [spanish].
 */
class CombatLog(
    private val spanish: Boolean = true,
    private val cardName: (CardId) -> String,
    private val sideName: (com.mineralord.tcg.engine.model.Side) -> String,
) {
    fun render(e: GameEvent): String = if (spanish) renderEs(e) else renderEn(e)

    private fun renderEs(e: GameEvent): String = when (e) {
        is GameEvent.TurnStarted -> "Turno de ${sideName(e.side)}"
        is GameEvent.TurnEnded -> "${sideName(e.side)} ha terminado su turno."
        is GameEvent.CardsDrawn -> "${sideName(e.side)} ha robado ${cards(e.count)}."
        is GameEvent.DeckShuffled -> "${sideName(e.side)} ha barajado su mazo."
        is GameEvent.PokemonPlayed ->
            "${sideName(e.side)} ha jugado ${cardName(e.card)}" +
                if (e.toBench) " en la Banca." else " en el Puesto Activo."
        is GameEvent.Evolved -> "${sideName(e.side)} ha hecho que ${cardName(e.from)} evolucione a ${cardName(e.to)}."
        is GameEvent.EnergyAttached -> "${sideName(e.side)} ha unido ${cardName(e.energy)} a ${cardName(e.to)}."
        is GameEvent.ToolAttached -> "${sideName(e.side)} ha unido ${cardName(e.tool)} a ${cardName(e.to)}."
        is GameEvent.TrainerPlayed -> "${sideName(e.side)} ha jugado ${cardName(e.card)}."
        is GameEvent.StadiumPlayed -> "${sideName(e.side)} ha jugado el Estadio ${cardName(e.card)}."
        is GameEvent.Retreated -> "${sideName(e.side)} ha retirado ${cardName(e.from)} por ${cardName(e.to)}."
        is GameEvent.Attacked -> "${cardName(e.attacker)} de ${sideName(e.side)} ha usado ${e.attackName}."
        is GameEvent.DamageDealt -> buildString {
            append("${cardName(e.target)} ha recibido ${e.amount} puntos de daño")
            if (e.weaknessApplied) append(" (Debilidad)")
            if (e.resistanceApplied) append(" (Resistencia)")
            append(".")
        }
        is GameEvent.Healed -> "${cardName(e.target)} ha recuperado ${e.amount} PS."
        is GameEvent.StatusApplied -> "${cardName(e.target)} ahora está ${statusEs(e.status)}."
        is GameEvent.StatusRemoved -> "${cardName(e.target)} ya no está ${statusEs(e.status)}."
        is GameEvent.KnockedOut -> "${cardName(e.pokemon)} ha quedado Fuera de Combate."
        is GameEvent.PrizeTaken -> "${sideName(e.side)} ha tomado ${prizes(e.count)}."
        is GameEvent.CoinFlipped -> "Lanzamiento de moneda: ${if (e.heads) "cara" else "cruz"}."
        is GameEvent.GameWon -> "${sideName(e.side)} ha ganado la partida."
    }

    private fun renderEn(e: GameEvent): String = when (e) {
        is GameEvent.TurnStarted -> "${sideName(e.side)}'s turn"
        is GameEvent.TurnEnded -> "${sideName(e.side)} ended their turn."
        is GameEvent.CardsDrawn -> "${sideName(e.side)} drew ${e.count} card(s)."
        is GameEvent.DeckShuffled -> "${sideName(e.side)} shuffled their deck."
        is GameEvent.PokemonPlayed ->
            "${sideName(e.side)} played ${cardName(e.card)}" +
                if (e.toBench) " to the Bench." else " to the Active Spot."
        is GameEvent.Evolved -> "${sideName(e.side)} evolved ${cardName(e.from)} into ${cardName(e.to)}."
        is GameEvent.EnergyAttached -> "${sideName(e.side)} attached ${cardName(e.energy)} to ${cardName(e.to)}."
        is GameEvent.ToolAttached -> "${sideName(e.side)} attached ${cardName(e.tool)} to ${cardName(e.to)}."
        is GameEvent.TrainerPlayed -> "${sideName(e.side)} played ${cardName(e.card)}."
        is GameEvent.StadiumPlayed -> "${sideName(e.side)} played Stadium ${cardName(e.card)}."
        is GameEvent.Retreated -> "${sideName(e.side)} retreated ${cardName(e.from)} for ${cardName(e.to)}."
        is GameEvent.Attacked -> "${sideName(e.side)}'s ${cardName(e.attacker)} used ${e.attackName}."
        is GameEvent.DamageDealt -> buildString {
            append("${cardName(e.target)} took ${e.amount} damage")
            if (e.weaknessApplied) append(" (Weakness)")
            if (e.resistanceApplied) append(" (Resistance)")
            append(".")
        }
        is GameEvent.Healed -> "${cardName(e.target)} healed ${e.amount} HP."
        is GameEvent.StatusApplied -> "${cardName(e.target)} is now ${e.status.name.lowercase()}."
        is GameEvent.StatusRemoved -> "${cardName(e.target)} is no longer ${e.status.name.lowercase()}."
        is GameEvent.KnockedOut -> "${cardName(e.pokemon)} was Knocked Out."
        is GameEvent.PrizeTaken -> "${sideName(e.side)} took ${e.count} Prize card(s)."
        is GameEvent.CoinFlipped -> "Coin flip: ${if (e.heads) "heads" else "tails"}."
        is GameEvent.GameWon -> "${sideName(e.side)} won the game."
    }

    private fun cards(n: Int) = if (n == 1) "una carta" else "$n cartas"
    private fun prizes(n: Int) = if (n == 1) "1 carta de Premio" else "$n cartas de Premio"

    private fun statusEs(s: com.mineralord.tcg.engine.model.Status) = when (s) {
        com.mineralord.tcg.engine.model.Status.ASLEEP -> "Dormido"
        com.mineralord.tcg.engine.model.Status.CONFUSED -> "Confundido"
        com.mineralord.tcg.engine.model.Status.PARALYZED -> "Paralizado"
        com.mineralord.tcg.engine.model.Status.POISONED -> "Envenenado"
        com.mineralord.tcg.engine.model.Status.BURNED -> "Quemado"
    }
}
