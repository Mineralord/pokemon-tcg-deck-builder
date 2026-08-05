package com.mineralord.tcg.engine.effects

import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.Zone

/**
 * Punto de extensión de REGLAS que el intérprete consulta ANTES de ejecutar un movimiento de cartas
 * entre zonas. Desacopla al [EffectInterpreter] (que no conoce el registro de efectos ni las
 * habilidades pasivas) del motor de reglas (`:engine:rules`), que sí puede resolver qué habilidades
 * en juego restringen un movimiento.
 *
 * Es GENÉRICO por diseño: el intérprete nunca sabe de cartas concretas. El host (GameEngine)
 * implementa la regla y decide; una habilidad como Sand Screen (Sandshrew — "las cartas de
 * Entrenador del descarte del rival no pueden volver a su mazo por efectos de sus Objetos/Partidarios")
 * es solo UNA implementación de [allowedZoneMove], no un caso especial cableado en el intérprete.
 *
 * Se inyecta por parámetro (igual que `shuffle`/`flip`); por defecto [NONE] no restringe nada, de modo
 * que el intérprete sigue siendo puro y testeable de forma aislada.
 */
fun interface RuleHook {
    /**
     * Devuelve el subconjunto de [cards] que SÍ puede moverse de la zona [from] a la zona [to] para
     * [side], según las reglas activas en [state]. Las cartas excluidas del resultado se quedan en su
     * zona de origen (el efecto continúa con el resto). Por defecto se permite todo.
     */
    fun allowedZoneMove(state: GameState, side: Side, cards: List<Card>, from: Zone, to: Zone): List<Card>

    companion object {
        /** Regla nula: no restringe ningún movimiento (comportamiento base del intérprete). */
        val NONE = RuleHook { _, _, cards, _, _ -> cards }
    }
}
