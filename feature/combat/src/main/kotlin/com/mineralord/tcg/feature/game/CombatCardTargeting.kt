package com.mineralord.tcg.feature.game

import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.EffectsDb
import com.mineralord.tcg.engine.model.Target
import com.mineralord.tcg.engine.model.ToolTarget
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind

/**
 * Helpers de la UI de combate para arrastrar cartas dirigidas sobre un Pokémon (Objetos como Poción,
 * Herramientas). Viven en `feature:combat` junto a `CombatScreen`, que los usa para resaltar los
 * objetivos válidos y aplicar el efecto en un gesto. Son presentación pura (no ejecutan reglas).
 */

/**
 * Si [card] es un Objeto que apunta a UN Pokémon PROPIO (su efecto empieza por un `ChooseTarget` de 1
 * sobre Pokémon propios, p. ej. Poción → cura 30), devuelve esa op de elección; si no, `null`. El
 * campo `onlyDamaged` indica que solo son válidos los Pokémon con daño.
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
 * Si [card] es una HERRAMIENTA (Pokémon Tool), devuelve a qué Pokémon puede anclarse ([ToolTarget]);
 * si no, `null`. Como los Objetos dirigidos, se arrastra sobre un Pokémon (no al panel central).
 */
internal fun toolAttachScope(card: TrainerCard): ToolTarget? =
    (card.kind as? TrainerKind.Tool)?.attachTo

/** ¿Esta carta se juega apuntando a un Pokémon (Objeto dirigido o Herramienta)? */
internal fun cardTargetsPokemon(card: TrainerCard): Boolean =
    itemTargetChoose(card) != null || toolAttachScope(card) != null
