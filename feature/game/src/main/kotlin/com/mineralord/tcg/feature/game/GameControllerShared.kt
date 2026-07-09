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
internal fun Side.other(): Side = if (this == Side.PLAYER) Side.OPPONENT else Side.PLAYER

/**
 * Mapea un [GameEvent] del motor al "cue" visual de la UI, normalizando el lado que
 * muestra el FX. Único para ambos modos: así el daño/embate/KO/premio/curación/moneda
 * se animan igual en PvE y PvP.
 */
internal fun GameEvent.toFxCue(): FxCue? = when (this) {
    is GameEvent.Attacked -> FxCue.Attack(side)
    // El daño lo recibe el rival del atacante.
    is GameEvent.DamageDealt -> FxCue.Damage(side.other(), amount, weaknessApplied, resistanceApplied)
    is GameEvent.Healed -> FxCue.Heal(side, amount)
    is GameEvent.KnockedOut -> FxCue.Knockout(side)
    is GameEvent.PrizeTaken -> FxCue.Prize(side, count)
    is GameEvent.CoinFlipped -> FxCue.Coin(side, heads)
    else -> null
}

/**
 * Reemite una lista de eventos del motor como [FxCue] para la UI, espaciando los
 * golpes para que las animaciones no se solapen (feel de TCG Live). [emitCue] entrega
 * cada señal al flujo de FX del controlador correspondiente.
 */
internal suspend fun playFx(events: List<GameEvent>, emitCue: suspend (FxCue) -> Unit) {
    for (e in events) {
        val cue = e.toFxCue() ?: continue
        emitCue(cue)
        when (cue) {
            is FxCue.Attack -> delay(220)
            is FxCue.Damage -> delay(360)
            is FxCue.Knockout -> delay(420)
            is FxCue.Prize -> delay(200)
            is FxCue.Coin -> delay(700)
            else -> {}
        }
    }
}

/**
 * Resuelve la carta (por id de INSTANCIA) buscándola en cualquier zona del [state]
 * actual, para poder dibujar su arte en los paneles de decisión (búsqueda en mazo,
 * elegir objetivo de Banca, etc.). Si no aparece en juego (id impreso genérico), cae
 * al repositorio por `id.printed`.
 */
internal fun lookupCard(state: GameState?, repo: CardRepository, id: CardId): Card? {
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
internal fun cardNameOf(repo: CardRepository, id: CardId): String =
    repo[id.printed]?.name?.es ?: id.printed.raw

/**
 * Si [card] es un Objeto que apunta a UN Pokémon PROPIO (su efecto empieza por un
 * `ChooseTarget` de 1 sobre Pokémon propios, p. ej. Poción → cura 30), devuelve esa op
 * de elección; si no, `null`. La UI la usa para permitir ARRASTRAR la carta sobre el
 * Pokémon objetivo y aplicar el efecto en un gesto (en vez de jugarla al panel central
 * y elegir aparte). El campo `onlyDamaged` indica que solo son válidos los Pokémon con
 * daño (así Poción no se puede soltar sobre uno intacto).
 */
internal fun itemTargetChoose(card: TrainerCard): EffectOp.ChooseTarget? {
    val effect = EffectsDb.registry[card.effect] ?: return null
    val first = effect.ops.firstOrNull() as? EffectOp.ChooseTarget ?: return null
    if (first.howMany != 1) return null
    return when (first.from) {
        Target.OWN_ALL, Target.OWN_ACTIVE, Target.OWN_BENCH -> first
        else -> null
    }
}

/**
 * Si [card] es una HERRAMIENTA (Pokémon Tool), devuelve a qué Pokémon puede anclarse
 * ([ToolTarget]); si no, `null`. Como los Objetos dirigidos, las Herramientas se
 * arrastran sobre un Pokémon (no se juegan en el panel central); la UI la usa para
 * resaltar los Pokémon válidos y anclarla al soltar.
 */
internal fun toolAttachScope(card: TrainerCard): ToolTarget? =
    (card.kind as? TrainerKind.Tool)?.attachTo

/** ¿Esta carta se juega apuntando a un Pokémon (Objeto dirigido o Herramienta)? */
internal fun cardTargetsPokemon(card: TrainerCard): Boolean =
    itemTargetChoose(card) != null || toolAttachScope(card) != null
