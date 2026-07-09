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
    /**
     * Elige [howMany] Pokémon de [from]. Si [onlyDamaged] es true, solo son elegibles
     * los que tienen daño (curaciones tipo Poción): así una carta que solo cura no puede
     * jugarse cuando no hay ningún Pokémon dañado (el motor rechaza si no quedan candidatos).
     */
    data class ChooseTarget(
        val from: Target,
        val howMany: Int,
        val prompt: LocalizedText,
        val onlyDamaged: Boolean = false,
    ) : EffectOp
    data class MoveEnergy(val from: Target, val to: Target, val count: Int) : EffectOp

    /** Baraja la mano del jugador dentro de su mazo (Youngster, Iono…). */
    data object ShuffleHandIntoDeck : EffectOp

    /** Intercambia el Activo propio con el Pokémon de Banca elegido (Switch/Cambio).
     *  Usa la carta ligada por una [ChooseTarget] previa sobre [Target.OWN_BENCH]. */
    data object SwapActiveWithChosen : EffectOp

    /** Sube al Activo RIVAL el Pokémon de su Banca elegido (Órdenes de Jefe / gust);
     *  su Activo anterior baja a la Banca. Usa la carta ligada por una [ChooseTarget]
     *  previa sobre [Target.OPP_BENCH]. */
    data object SwapOppActiveWithChosen : EffectOp

    /** Lanza una moneda y roba [ifHeads] cartas si sale cara, o [ifTails] si cruz (Picnicker). */
    data class CoinFlipDraw(val ifHeads: Int, val ifTails: Int) : EffectOp

    /**
     * Marca al Pokémon origen para que NO pueda atacar durante su PRÓXIMO turno
     * (Kilowattrel — Jet Wing, sv2-82). El intérprete fija
     * [PokemonInPlay.cannotAttackOnTurn] al número del siguiente turno propio
     * (turn actual + 2, porque el turno intermedio es del rival). */
    data object NoAttackNextTurn : EffectOp

    /**
     * Daño al Activo rival = [damagePerHeads] por cada CARA al lanzar una moneda
     * por cada Energía de tipo [energyType] unida al Pokémon origen (Torkoal —
     * Concentrated Fire, 80×). El daño base del ataque es Variable (=0), así que
     * todo el daño proviene de esta op. */
    data class CoinsPerEnergyDamage(val energyType: EnergyType, val damagePerHeads: Int) : EffectOp

    /**
     * Une hasta [count] Energía Básica (de tipo [energyType]; null = cualquiera)
     * desde tu DESCARTE a [target]. Si [target] apunta a un único Pokémon
     * (SELF/OWN_ACTIVE) es determinista (Volcarona — Flame Cloak). Si apunta a
     * varios (OWN_ALL) emite una decisión [PendingDecision.AttachFromRevealed]
     * con `fromDiscard = true` para que el jugador reparta arrastrando
     * (Skeledirge — Passionate Singing). */
    data class AttachEnergyFromDiscard(
        val count: Int,
        val energyType: EnergyType?,
        val target: Target,
        /**
         * "Si lo haces, roba hasta tener [thenDrawUpTo] cartas" (Mela). El robo
         * es ATÓMICO con el enganche: solo ocurre si se unió al menos 1 Energía.
         * null = no roba. */
        val thenDrawUpTo: Int? = null,
    ) : EffectOp

    /**
     * Mira las [lookAt] primeras cartas del mazo y une hasta [maxAttach] Energías
     * Básicas de tipo [energyType] que encuentres allí a tus Pokémon de Banca de
     * tipo [benchType] (null = cualquier Pokémon de Banca), como quieras; el resto
     * se baraja de vuelta (Generador Eléctrico, sv1-170). Genera una decisión
     * [PendingDecision.AttachFromRevealed] que el jugador resuelve arrastrando. */
    data class RevealAttachEnergy(
        val lookAt: Int,
        val maxAttach: Int,
        val energyType: EnergyType,
        val benchType: EnergyType?,
    ) : EffectOp
}

/** Condición para un término de daño de ataque, evaluada contra el estado. */
enum class DamageCondition {
    /** Siempre suma. */
    ALWAYS,
    /** Suma solo si el Activo rival (defensor) es un Pokémon de Evolución. */
    IF_DEFENDER_EVOLVED,
}

/**
 * Término de daño de un ataque, evaluado ANTES de Debilidad/Resistencia (a
 * diferencia de [EffectOp.ExtraDamage], que aplica daño crudo después). Modela
 * los ataques "X+" con bonus condicional (p.ej. Cross-Cut): el total pasa por
 * Debilidad/Resistencia una sola vez, exacto al reglamento.
 */
data class DamageTerm(val amount: Int, val condition: DamageCondition = DamageCondition.ALWAYS)

/**
 * Programa de efecto completo asociado a un ataque, habilidad o carta jugable.
 * `oncePerTurn`/`activeOnly` aplican a habilidades.
 */
data class Effect(
    val ops: List<EffectOp> = emptyList(),
    val passives: List<PassiveModifier> = emptyList(),
    val oncePerTurn: Boolean = false,
    val activeOnly: Boolean = false,
    /**
     * Daño de ataque autorado (para ataques "X+" con bonus condicional). Si NO
     * está vacío, el motor usa la suma de los términos aplicables como daño base
     * (sustituye al `baseDamage` de la carta, que es Variable) y le aplica
     * Debilidad/Resistencia. Vacío = el daño lo da `Attack.baseDamage`. */
    val attackDamage: List<DamageTerm> = emptyList(),
    /**
     * Si es true, la carta solo puede jugarse cuando alguno de TUS Pokémon fue
     * Noqueado durante el último turno del rival (Mela). El motor lo comprueba
     * contra [GameState.koedLastOppTurn] en `playTrainer`/`legalIntents`. */
    val requiresOwnKoLastTurn: Boolean = false,
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
