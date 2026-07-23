package com.mineralord.tcg.engine.model

/** Supertipo de carta. */
enum class Supertype { POKEMON, TRAINER, ENERGY }

/**
 * Carta — interfaz sellada raíz. Toda carta comparte identidad, set, rareza,
 * marca de regulación y arte. Las tres ramas (Pokémon / Entrenador / Energía)
 * añaden su estructura específica.
 */
sealed interface Card {
    val id: CardId
    val name: LocalizedText
    val set: SetInfo
    val rarity: Rarity
    val regulationMark: String?
    val artwork: ArtworkRefs
    val supertype: Supertype
}

/**
 * Devuelve una copia de la carta con otro [CardId]. Se usa para dar a cada copia
 * física de una carta impresa un id de INSTANCIA único al construir la partida, de
 * modo que las copias sean independientes en juego (ver [CardId.withInstance]).
 */
fun Card.withId(newId: CardId): Card = when (this) {
    is PokemonCard -> copy(id = newId)
    is TrainerCard -> copy(id = newId)
    is BasicEnergy -> copy(id = newId)
    is SpecialEnergy -> copy(id = newId)
}

// ============================================================
//  POKÉMON
// ============================================================

/** Etapa evolutiva. */
sealed interface Stage {
    data object Basic : Stage
    data object Stage1 : Stage
    data object Stage2 : Stage
    data object BabyRestored : Stage
}

/**
 * "Rule box" del Pokémon: determina cuántos premios entrega al ser noqueado y
 * qué texto de regla especial aplica. Modelar esto como tipo (no como flags)
 * hace imposible estados inválidos.
 */
sealed interface PokemonMechanic {
    val prizesWhenKO: Int

    /**
     * ¿Tiene "Rule Box" (caja de regla) impresa? Todos los Pokémon con mecánica especial
     * (ex/EX/V/VMAX/VSTAR/GX/Radiant/Tera) la tienen; los [Normal] no. Lo consultan efectos
     * que solo afectan a Pokémon con caja de regla — p.ej. Camino hacia la Cima (Path to the
     * Peak): "los Pokémon que TIENEN una caja de regla no tienen Habilidades".
     */
    val hasRuleBox: Boolean get() = true

    data object Normal : PokemonMechanic {
        override val prizesWhenKO = 1
        override val hasRuleBox = false
    }
    data object ExLower : PokemonMechanic { override val prizesWhenKO = 2 }   // sv "ex"
    data object ExUpper : PokemonMechanic { override val prizesWhenKO = 2 }   // "EX" era SM
    data object V : PokemonMechanic { override val prizesWhenKO = 2 }
    data object VMax : PokemonMechanic { override val prizesWhenKO = 3 }
    data object VStar : PokemonMechanic { override val prizesWhenKO = 2 }
    data object Gx : PokemonMechanic { override val prizesWhenKO = 2 }
    data object Radiant : PokemonMechanic { override val prizesWhenKO = 1 }   // Radiante SÍ tiene caja de regla
    data class Tera(val onBenchProtected: Boolean = true) : PokemonMechanic {
        override val prizesWhenKO = 2
    }
}

/** Daño base de un ataque. */
sealed interface Damage {
    data class Fixed(val value: Int) : Damage
    data object Variable : Damage     // depende del efecto (p.ej. "por cada…")
    data object None : Damage         // ataque puramente de efecto
}

data class Attack(
    val name: LocalizedText,
    val cost: List<EnergyType>,
    val convertedCost: Int,
    val baseDamage: Damage,
    val effect: EffectId?,
    /** Texto de regla IMPRESO del ataque (lo que aparece bajo el nombre en la carta). Vacío
     *  si el ataque solo hace daño sin efecto — en ese caso la UI no muestra descripción. */
    val text: LocalizedText = LocalizedText("", ""),
)

/**
 * Clase de "poder" impreso sobre un Pokémon que NO es un ataque. El TCG ha usado
 * tres nombres según la época, y las reglas los tratan de forma distinta:
 *
 * - [ABILITY]  — "Habilidad" (era Blanco y Negro en adelante, 2011→hoy). Es lo que
 *   apagan las cartas que dicen "los Pokémon no tienen Habilidades" (Camino hacia la
 *   Cima, Klefki…).
 * - [POKE_POWER] — "Poké-Power" (pre-2011). Poder que normalmente se ACTIVA en tu turno.
 * - [POKE_BODY]  — "Poké-Body" (pre-2011). Efecto PASIVO permanente.
 *
 * Regla clave (Web Rulebook 2026 + reglas de época): un bloqueo moderno de Habilidades
 * NO afecta a Poké-Powers ni Poké-Bodies (son mecánicas legalmente distintas). Por eso
 * solo [ABILITY] es [suppressibleByAbilityLock].
 */
enum class AbilityKind {
    ABILITY, POKE_POWER, POKE_BODY;

    /** true solo para [ABILITY]: lo demás no lo apaga un "los Pokémon no tienen Habilidades". */
    val suppressibleByAbilityLock: Boolean get() = this == ABILITY
}

data class Ability(
    val name: LocalizedText,
    val text: LocalizedText,
    val effect: EffectId?,
    /** Época/clase del poder. Por defecto [AbilityKind.ABILITY] (cartas modernas). */
    val kind: AbilityKind = AbilityKind.ABILITY,
)

/**
 * Rasgo Antiguo (Ancient Trait) — poder especial impreso bajo el nombre del Pokémon
 * en la era XY (p.ej. Θ Barrera, Ω Recuperación, α Crecimiento…). NO es un ataque ni
 * una Habilidad, así que las cartas que bloquean ataques o Habilidades NO lo afectan
 * (Web Rulebook 2026, glosario "Ancient Trait"). Se modela como campo aparte de
 * [PokemonCard.abilities] precisamente para que sea inmune a esos bloqueos por diseño.
 */
data class AncientTrait(
    val name: LocalizedText,
    val text: LocalizedText,
    val effect: EffectId? = null,
)

data class PokemonCard(
    override val id: CardId,
    override val name: LocalizedText,
    override val set: SetInfo,
    override val rarity: Rarity,
    override val regulationMark: String?,
    override val artwork: ArtworkRefs,
    val stage: Stage,
    val mechanic: PokemonMechanic,
    val hp: Int,
    val types: List<EnergyType>,
    val evolvesFrom: String?,
    val abilities: List<Ability>,
    val attacks: List<Attack>,
    val weaknesses: List<TypeModifier>,
    val resistances: List<TypeModifier>,
    val retreatCost: List<EnergyType>,
    val rulesText: List<LocalizedText>,
    /** Rasgo Antiguo (era XY). null en la inmensa mayoría de cartas. Inmune a bloqueos. */
    val ancientTrait: AncientTrait? = null,
) : Card {
    override val supertype get() = Supertype.POKEMON
    val isBasic: Boolean get() = stage is Stage.Basic
    val prizeValue: Int get() = mechanic.prizesWhenKO

    /**
     * Habilidades EFECTIVAS teniendo en cuenta un posible bloqueo de Habilidades en
     * juego ("los Pokémon no tienen Habilidades"). Si [abilityLockActive] es true, se
     * ocultan las [AbilityKind.ABILITY] pero se conservan Poké-Powers/Poké-Bodies, que
     * ese bloqueo no afecta. Los Rasgos Antiguos van en [ancientTrait], nunca aquí, así
     * que también quedan inmunes por construcción.
     */
    fun effectiveAbilities(abilityLockActive: Boolean): List<Ability> =
        if (!abilityLockActive) abilities
        else abilities.filterNot { it.kind.suppressibleByAbilityLock }
}

// ============================================================
//  ENTRENADORES
// ============================================================

/** Dónde puede anexarse una Herramienta. */
enum class ToolTarget { ANY, OWN_POKEMON }

/** Categoría de Entrenador. */
sealed interface TrainerKind {
    val isAceSpec: Boolean

    data class Supporter(override val isAceSpec: Boolean = false) : TrainerKind  // 1 por turno
    data class Item(override val isAceSpec: Boolean = false) : TrainerKind
    data class Stadium(override val isAceSpec: Boolean = false) : TrainerKind     // único en campo
    data class Tool(
        val attachTo: ToolTarget = ToolTarget.OWN_POKEMON,
        override val isAceSpec: Boolean = false,
    ) : TrainerKind
}

data class TrainerCard(
    override val id: CardId,
    override val name: LocalizedText,
    override val set: SetInfo,
    override val rarity: Rarity,
    override val regulationMark: String?,
    override val artwork: ArtworkRefs,
    val kind: TrainerKind,
    val text: LocalizedText,
    val effect: EffectId,
) : Card {
    override val supertype get() = Supertype.TRAINER
}

// ============================================================
//  ENERGÍAS
// ============================================================

/** Qué energía aporta una carta de energía especial. */
sealed interface EnergyProvision {
    data class Fixed(val types: List<EnergyType>) : EnergyProvision
    data object ChooseOne : EnergyProvision               // el jugador elige el tipo
    data class Conditional(val effect: EffectId) : EnergyProvision
}

sealed interface EnergyCard : Card {
    override val supertype get() = Supertype.ENERGY
}

data class BasicEnergy(
    override val id: CardId,
    override val name: LocalizedText,
    override val set: SetInfo,
    override val rarity: Rarity,
    override val regulationMark: String?,
    override val artwork: ArtworkRefs,
    val type: EnergyType,
) : EnergyCard

data class SpecialEnergy(
    override val id: CardId,
    override val name: LocalizedText,
    override val set: SetInfo,
    override val rarity: Rarity,
    override val regulationMark: String?,
    override val artwork: ArtworkRefs,
    val provides: EnergyProvision,
    val stateModifiers: List<PassiveModifier>,
    val effect: EffectId,
) : EnergyCard
