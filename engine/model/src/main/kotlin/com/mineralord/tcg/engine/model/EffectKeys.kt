package com.mineralord.tcg.engine.model

/**
 * Utilidades compartidas para registrar efectos por carta y definiciones de
 * [Effect] reutilizadas por VARIAS expansiones (p. ej. Solid Shell aparece en 151
 * y en sv8). Viven a nivel de paquete para que cada archivo de set las use sin
 * cualificar. La composición de claves es estable:
 *  - ataque:    "<cardId>#atk:<nombreEn>"
 *  - habilidad: "<cardId>#abi:<nombreEn>"
 *  - entrenador/energía/estadio/herramienta: el id de carta a secas.
 */
internal fun atkKey(cardId: String, attackEn: String): EffectId = EffectId("$cardId#atk:$attackEn")
internal fun abiKey(cardId: String, abilityEn: String): EffectId = EffectId("$cardId#abi:$abilityEn")
internal fun prompt(es: String, en: String) = LocalizedText(es, en)

// --- Definiciones reutilizables (mismo efecto en varios artes/printings/sets) ---

/** Solid Shell / Solid Body — pasivo: -30 al daño recibido por este Pokémon. */
internal val SOLID = Effect(passives = listOf(PassiveModifier(ModKind.REDUCE_DAMAGE, 30, Target.SELF)))

/** Tranquil Flower — 1/turno (solo activo): elige 1 de los tuyos y cúralo 60. */
internal val TRANQUIL = Effect(
    ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_ALL, 1, prompt("Elige un Pokémon para curar 60", "Choose a Pokémon to heal 60"), onlyDamaged = true),
        EffectOp.Heal(Target.CHOSEN, Amount.Fixed(60)),
    ),
    oncePerTurn = true,
    activeOnly = true,
)

/** Restart — 1/turno: roba hasta tener 3 cartas en mano. */
internal val RESTART = Effect(ops = listOf(EffectOp.DrawUntil(3)), oncePerTurn = true)

/** Calming Light — 1/turno (solo activo): el Activo rival queda Dormido. */
internal val CALMING = Effect(
    ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.ASLEEP))),
    oncePerTurn = true,
    activeOnly = true,
)

/** Cambio (Switch) — intercambia tu Activo con un Pokémon de tu Banca elegido.
 *  Compartido: sv1-194 (baraja Pikachu) y sv3pt5-206 (set 151). */
internal val switch = Effect(
    ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige un Pokémon de tu Banca para pasar al Activo", "Choose a Benched Pokémon to switch to the Active Spot")),
        EffectOp.SwapActiveWithChosen,
    ),
)
