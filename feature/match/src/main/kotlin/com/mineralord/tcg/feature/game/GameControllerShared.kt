package com.mineralord.tcg.feature.game

import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.engine.events.GameEvent
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.EffectsDb
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Target
import com.mineralord.tcg.engine.model.ToolTarget
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import com.mineralord.tcg.feature.game.anim.FxCue
import kotlinx.coroutines.delay

/**
 * Helpers COMPARTIDOS por los controladores de combate (PvE [GameViewModel] y PvP
 * [OnlineGameController]). Aquí vive la lógica que debe ser IDÉNTICA en ambos modos
 * (mapeo de eventos a animaciones, búsqueda de cartas por id), para no duplicarla y
 * evitar que los modos diverjan sin querer. La orquestación específica de cada modo
 * (IA vs. red) queda en su propio controlador.
 *
 * Fase 1 del plan de unificación: extraer los helpers puros/sin estado. La Fase 2
 * moverá el pipeline de intents a un `GameCore` por composición.
 */

/** El otro lado del tablero. */
fun Side.other(): Side = if (this == Side.PLAYER) Side.OPPONENT else Side.PLAYER

/**
 * Mapea un [GameEvent] del motor al "cue" visual de la UI, normalizando el lado que
 * muestra el FX. Único para ambos modos: así el daño/embate/KO/premio/curación/moneda
 * se animan igual en PvE y PvP.
 */
fun GameEvent.toFxCue(): FxCue? = when (this) {
    is GameEvent.Attacked -> FxCue.Attack(side, attackName)
    is GameEvent.AbilityUsed -> FxCue.AbilityUse(side, pokemon, manual)
    // El daño lo recibe el rival del atacante.
    is GameEvent.DamageDealt -> FxCue.Damage(side.other(), amount, weaknessApplied, resistanceApplied)
    is GameEvent.Healed -> FxCue.Heal(side, amount)
    is GameEvent.KnockedOut -> FxCue.Knockout(side)
    is GameEvent.PrizeTaken -> FxCue.Prize(side, count)
    is GameEvent.CoinFlipped -> FxCue.Coin(side, heads)
    is GameEvent.StadiumPlayed -> FxCue.StadiumPlaced(side, card)
    else -> null
}

/**
 * Reemite una lista de eventos del motor como [FxCue] para la UI, espaciando los
 * golpes para que las animaciones no se solapen (feel de TCG Live). [emitCue] entrega
 * cada señal al flujo de FX del controlador correspondiente.
 */
suspend fun playFx(events: List<GameEvent>, emitCue: suspend (FxCue) -> Unit) =
    playCues(events.mapNotNull { it.toFxCue() }, emitCue)

/**
 * Reproduce una lista YA MAPEADA de [FxCue] con el espaciado cinematográfico compartido. Es la vía
 * que usa el invitado PvP (que recibe los cues del host por red) para animar EXACTAMENTE igual que el
 * host y que PvE, sin volver a pasar por los eventos del motor.
 */
suspend fun playCues(cues: List<FxCue>, emitCue: suspend (FxCue) -> Unit) {
    for (cue in cues) {
        emitCue(cue)
        delay(cue.playbackDelayMs())
    }
}

/** Espaciado tras cada cue (feel de TCG Live). Único para PvE, host PvP e invitado PvP. */
private fun FxCue.playbackDelayMs(): Long = when (this) {
    // El rótulo actúa como ANUNCIO cinematográfico: se lee ANTES de que impacte la acción.
    is FxCue.Attack -> 1150
    is FxCue.AbilityUse -> 1150
    is FxCue.Damage -> 360
    is FxCue.Knockout -> 420
    is FxCue.Prize -> 200
    is FxCue.Coin -> 700
    is FxCue.StadiumPlaced -> 420   // deja asentar la carta antes del siguiente golpe
    else -> 0
}

/**
 * Resuelve la carta (por id de INSTANCIA) buscándola en cualquier zona del [state]
 * actual, para poder dibujar su arte en los paneles de decisión (búsqueda en mazo,
 * elegir objetivo de Banca, etc.). Si no aparece en juego (id impreso genérico), cae
 * al repositorio por `id.printed`.
 */
fun lookupCard(state: GameState?, repo: CardRepository, id: CardId): Card? {
    val s = state ?: return repo[id.printed]
    val pool = listOf(s.player, s.opponent).flatMap { ps ->
        ps.hand + ps.deck + ps.discard + ps.prizes + ps.lostZone +
            ps.allInPlay.map { it.card } +
            ps.allInPlay.flatMap { it.attachedEnergy } +
            ps.allInPlay.flatMap { it.evolutionStack }
    }
    return pool.firstOrNull { it.id == id } ?: repo[id.printed]
}

/** Nombre legible (español) de una carta por su id. */
fun cardNameOf(repo: CardRepository, id: CardId): String =
    repo[id.printed]?.name?.es ?: id.printed.raw

// Los helpers de targeting de UI (itemTargetChoose/toolAttachScope/cardTargetsPokemon) se movieron
// a `feature:combat` (CombatCardTargeting.kt), junto a la pantalla que los usa.
