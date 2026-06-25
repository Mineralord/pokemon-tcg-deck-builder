package com.mineralord.tcg.engine.model

/**
 * DSL de efectos — catálogo CERRADO y type-safe (reescrito en Kotlin, inspirado
 * en el DSL JS ya validado de la web `js/efectos-dsl.js`).
 *
 * Un [Effect] es un programita declarativo que el motor interpreta de forma
 * **determinista**. Cuando una op requiere una decisión (p.ej. [ChooseTarget]),
 * el motor emite una decisión pendiente que IA/jugador resuelven — nunca queda
 * en "resolución manual" ambigua.
 */

/** A quién apunta una operación. */
enum class Target {
    SELF, OWN_ACTIVE, OPP_ACTIVE,
    OWN_BENCH, OPP_BENCH, OWN_ALL, OPP_ALL,
    CHOSEN,                       // resuelto por una ChooseTarget previa
    OWN_DECK, OWN_DISCARD, OWN_HAND,
}

/** Contadores escalables para daño/curación "por cada …". */
enum class Counter { BENCH_COUNT, ENERGY_ATTACHED, DAMAGE_COUNTERS, HEADS }

/** Cantidad fija o derivada de un conteo del estado de juego. */
sealed interface Amount {
    data class Fixed(val n: Int) : Amount
    data class PerCount(val of: Counter, val target: Target, val mult: Int) : Amount
}

/** Filtro para búsquedas en mazo/descarte. */
data class CardFilter(
    val supertype: Supertype? = null,
    val isBasic: Boolean? = null,
    val type: EnergyType? = null,
    val nameContains: String? = null,
)

/** Zona destino de una búsqueda/movimiento. */
enum class Zone { HAND, BENCH, ACTIVE, DECK, DISCARD, LOST }

/** Modificadores pasivos (habilidades/herramientas/energías especiales). */
enum class ModKind {
    REDUCE_DAMAGE, EXTRA_HP, RETREAT_COST, ATTACK_COST,
    IMMUNE_STATUS, PROVIDES_ENERGY, EXTRA_DAMAGE,
    BLOCK_ABILITY, NO_RETREAT, NO_WEAKNESS,
}

data class PassiveModifier(
    val mod: ModKind,
    val amount: Int = 0,
    val appliesTo: Target = Target.SELF,
)

/** Operaciones del catálogo cerrado. */
sealed interface EffectOp {
    data class Damage(val target: Target, val amount: Amount) : EffectOp
    data class ExtraDamage(val amount: Amount) : EffectOp
    data class Recoil(val amount: Amount) : EffectOp
    data class Heal(val target: Target, val amount: Amount) : EffectOp
    data class ApplyStatus(val target: Target, val states: List<Status>) : EffectOp
    data class RemoveStatus(val target: Target) : EffectOp
    data class DrawCards(val count: Int) : EffectOp
    data class DrawUntil(val handSize: Int) : EffectOp
    data class DiscardEnergy(val target: Target, val count: Int) : EffectOp
    data class SearchDeck(val filter: CardFilter, val to: Zone, val count: Int) : EffectOp
    data class ChooseTarget(val from: Target, val howMany: Int, val prompt: LocalizedText) : EffectOp
    data class MoveEnergy(val from: Target, val to: Target, val count: Int) : EffectOp
}

/**
 * Programa de efecto completo asociado a un ataque, habilidad o carta jugable.
 * `oncePerTurn`/`activeOnly` aplican a habilidades.
 */
data class Effect(
    val ops: List<EffectOp> = emptyList(),
    val passives: List<PassiveModifier> = emptyList(),
    val oncePerTurn: Boolean = false,
    val activeOnly: Boolean = false,
)

/**
 * Registro central id → efecto. Se irá poblando carta por carta (Fase 7 del
 * roadmap). Vacío de inicio: el motor cae a comportamiento por defecto cuando
 * un [EffectId] no está registrado.
 */
class EffectRegistry(private val byId: Map<EffectId, Effect> = emptyMap()) {
    operator fun get(id: EffectId?): Effect? = id?.let { byId[it] }
    fun has(id: EffectId): Boolean = byId.containsKey(id)
    val size: Int get() = byId.size

    companion object {
        val EMPTY = EffectRegistry()
    }
}
