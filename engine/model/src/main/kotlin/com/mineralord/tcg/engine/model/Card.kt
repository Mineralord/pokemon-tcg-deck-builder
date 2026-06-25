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

    data object Normal : PokemonMechanic { override val prizesWhenKO = 1 }
    data object ExLower : PokemonMechanic { override val prizesWhenKO = 2 }   // sv "ex"
    data object ExUpper : PokemonMechanic { override val prizesWhenKO = 2 }   // "EX" era SM
    data object V : PokemonMechanic { override val prizesWhenKO = 2 }
    data object VMax : PokemonMechanic { override val prizesWhenKO = 3 }
    data object VStar : PokemonMechanic { override val prizesWhenKO = 2 }
    data object Gx : PokemonMechanic { override val prizesWhenKO = 2 }
    data object Radiant : PokemonMechanic { override val prizesWhenKO = 1 }
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
)

data class Ability(
    val name: LocalizedText,
    val text: LocalizedText,
    val effect: EffectId?,
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
) : Card {
    override val supertype get() = Supertype.POKEMON
    val isBasic: Boolean get() = stage is Stage.Basic
    val prizeValue: Int get() = mechanic.prizesWhenKO
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
