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

/*
 * ===================== CATÁLOGO DE COMPORTAMIENTOS DE ATAQUE COMPARTIDOS =====================
 *
 * POLÍTICA (aplica a TODOS los sets, presentes y futuros): un [Effect] codifica SOLO el
 * COMPORTAMIENTO de un ataque (sus `ops`, riders y daño condicional), NUNCA su coste de Energía
 * ni su daño base (esos viven en los datos de la carta: `Attack.cost` / `Attack.baseDamage`).
 *
 * Por tanto, dos impresiones del MISMO ataque (mismo nombre y mismo rider) en sets distintos
 * comparten EXACTAMENTE el mismo [Effect] aunque difieran en coste o daño. Antes de autorar un
 * ataque en un archivo de set, busca aquí: si su comportamiento ya existe, REUTILIZA el `val`
 * (o el helper) en vez de reconstruir el [Effect]. Así no hay código duplicado entre sets.
 *
 * Los helpers [recoil]/[draw]/… centralizan families paramétricas (mismo rider, distinto número).
 */

/** "Lanza 1 moneda. Si cara, el Activo rival queda Paralizado." (Bubble Beam, Body Slam, Volt Wave…). */
internal val COIN_PARALYZE = Effect(ops = listOf(EffectOp.CoinFlipStatus(Target.OPP_ACTIVE, listOf(Status.PARALYZED))))

/** "Lanza 3 monedas. Este ataque hace 10 por cada cara." (Triple Strike, Triple Spin, Fury Headbutt…). */
internal val TRIPLE_COIN_10 = Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 10)))

/** "Mueve el Activo rival a la Banca (el rival elige el nuevo Activo)." (Push Down, Oprimir…). */
internal val GUST_DEFENDER = Effect(ops = listOf(EffectOp.GustDefenderChooseNewActive))

/** "Durante tu próximo turno, este Pokémon no puede atacar." (Boundless Power, Laser Blade…). */
internal val NO_ATTACK_NEXT_TURN = Effect(ops = listOf(EffectOp.NoAttackNextTurn))

/** Daño de RETROCESO fijo al propio atacante (Thunder=50, Reckless Charge=10/20, Heat Tackle=30…). */
internal fun recoil(amount: Int) = Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(amount))))

/** "Roba [n] cartas." (Collect, Punch and Draw, Nom-Nom-Nom Incisors…). */
internal fun draw(n: Int) = Effect(ops = listOf(EffectOp.DrawCards(n)))

/** "El Activo rival queda afectado por [status]" sin moneda (Searing Flame=Quemado, etc.). */
internal fun statusOppActive(vararg status: Status) =
    Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, status.toList())))
