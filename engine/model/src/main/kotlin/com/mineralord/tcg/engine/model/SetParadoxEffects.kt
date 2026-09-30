package com.mineralord.tcg.engine.model

/**
 * Efectos de la expansión **Brecha Paradójica** (`sv4`, Paradox Rift) de la serie
 * Escarlata y Púrpura. AISLADO por expansión: no toca `Set151Effects.kt` ni ningún
 * otro set. Registrado por [EffectsDb.registry] vía [registerParadoxRift].
 *
 * COBERTURA — Fase 1 (daño puro): sólo ataques cuyo ÚNICO efecto es CALCULAR daño
 * (monedas por cara y bonos "+X" condicionales que encajan en el DSL existente). Los
 * ataques de daño FIJO ya funcionan sin autorar (el motor toma `Attack.baseDamage`);
 * aquí se cubren los de daño VARIABLE, que si no darían 0. Las mecánicas que aún no
 * existen en el motor (bono por Energía unida, "si noquearon el turno pasado", buscar
 * en descarte…) quedan para fases posteriores y NO se registran (degradan a daño 0).
 */
internal fun MutableMap<EffectId, Effect>.registerParadoxRift() {

    // ---- Daño por MONEDAS: "Lanza N monedas. Este ataque hace D por cada cara." ----
    // El daño total lo aporta CoinFlipDamage (la carta trae daño Variable → base 0).
    put(atkKey("sv4-1", "Triple Spin"), Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 10))))
    put(atkKey("sv4-9", "Double Spin"), Effect(ops = listOf(EffectOp.CoinFlipDamage(2, 40))))
    put(atkKey("sv4-51", "Fury Headbutt"), Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 10))))
    put(atkKey("sv4-53", "Triple Whip"), Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 70))))
    put(atkKey("sv4-64", "Flail Around"), Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 10))))
    put(atkKey("sv4-71", "Triple Strike"), Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 10))))
    put(atkKey("sv4-97", "Three-Step Strike"), Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 20))))
    put(atkKey("sv4-196", "Flail Around"), Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 10))))
    put(atkKey("sv4-200", "Three-Step Strike"), Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 20))))

    // ---- Daño condicional "X+" (pasa por Debilidad/Resistencia una sola vez) ----

    // Ice Shard (10+): +30 si el Activo rival es de tipo Lucha.
    val iceShard = Effect(attackDamage = listOf(
        DamageTerm(10), DamageTerm(30, DamageCondition.IfDefenderType(EnergyType.FIGHTING))))
    put(atkKey("sv4-37", "Ice Shard"), iceShard)
    put(atkKey("sv4-188", "Ice Shard"), iceShard)

    // Prize Count (80+): +80 si te quedan MÁS Premios que a tu rival.
    put(atkKey("sv4-40", "Prize Count"), Effect(attackDamage = listOf(
        DamageTerm(80), DamageTerm(80, DamageCondition.IfMorePrizesThanOpponent))))

    // Cross-Cut (30+): +60 si el Activo rival es un Pokémon de Evolución.
    val crossCut = Effect(attackDamage = listOf(
        DamageTerm(30), DamageTerm(60, DamageCondition.IfDefenderEvolved)))
    put(atkKey("sv4-118", "Cross-Cut"), crossCut)
    put(atkKey("sv4-205", "Cross-Cut"), crossCut)

    // Driving Buddy (70+): +70 si jugaste una carta de Apoyo (Partidario) de tu mano este turno.
    val drivingBuddy = Effect(attackDamage = listOf(
        DamageTerm(70), DamageTerm(70, DamageCondition.IfSupporterPlayedThisTurn)))
    put(atkKey("sv4-157", "Driving Buddy"), drivingBuddy)
    put(atkKey("sv4-215", "Driving Buddy"), drivingBuddy)

    // Plus Damage (10+): +10 por cada contador de daño en el Activo rival.
    val plusDamage = Effect(attackDamage = listOf(
        DamageTerm(10), DamageTerm(0, perDefenderCounter = 10)))
    put(atkKey("sv4-60", "Plus Damage"), plusDamage)
    put(atkKey("sv4-193", "Plus Damage"), plusDamage)

    // Punishing Kick (10+): +40 por cada contador de daño en el Activo rival.
    put(atkKey("sv4-122", "Punishing Kick"), Effect(attackDamage = listOf(
        DamageTerm(10), DamageTerm(0, perDefenderCounter = 40))))

    // Combat Beak (20+): +20 por cada Pokémon en la Banca del rival.
    put(atkKey("sv4-106", "Combat Beak"), Effect(
        attackDamage = listOf(DamageTerm(20)),
        ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.BENCH_COUNT, Target.OPP_BENCH, 20))),
    ))

    // =========================== Fase 2: daño + rider simple ===========================
    // Ataques cuyo daño ya funciona (fijo) y añaden un efecto sencillo del DSL existente.

    // ---- Condición Especial al Activo rival (daño + estado, sin moneda) ----
    put(atkKey("sv4-12", "Brain Shake"), Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.CONFUSED)))))
    put(atkKey("sv4-20", "Searing Flame"), Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.BURNED)))))
    put(atkKey("sv4-36", "Hypno Splash"), Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.ASLEEP)))))
    put(atkKey("sv4-41", "Water Pulse"), Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.ASLEEP)))))
    val venomousHit = Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.POISONED))))
    put(atkKey("sv4-116", "Venomous Hit"), venomousHit)
    put(atkKey("sv4-117", "Venomous Hit"), venomousHit)
    put(atkKey("sv4-204", "Venomous Hit"), venomousHit)
    // Condición Especial que se aplica el PROPIO atacante.
    put(atkKey("sv4-105", "Boiled Press"), Effect(ops = listOf(EffectOp.ApplyStatus(Target.SELF, listOf(Status.BURNED)))))
    put(atkKey("sv4-151", "Teetering Steps"), Effect(ops = listOf(EffectOp.ApplyStatus(Target.SELF, listOf(Status.CONFUSED)))))
    // Burning Turbulence: 90 de retroceso a sí mismo + Activo rival Quemado.
    val burningTurbulence = Effect(ops = listOf(
        EffectOp.Recoil(Amount.Fixed(90)),
        EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.BURNED)),
    ))
    put(atkKey("sv4-107", "Burning Turbulence"), burningTurbulence)
    put(atkKey("sv4-203", "Burning Turbulence"), burningTurbulence)

    // ---- Condición Especial por MONEDA ("si cara, … queda …") ----
    val coinParalyze = Effect(ops = listOf(EffectOp.CoinFlipStatus(Target.OPP_ACTIVE, listOf(Status.PARALYZED))))
    put(atkKey("sv4-31", "Bubble Beam"), coinParalyze)
    put(atkKey("sv4-67", "Volt Wave"), coinParalyze)
    put(atkKey("sv4-149", "Body Slam"), coinParalyze)
    put(atkKey("sv4-212", "Body Slam"), coinParalyze)
    val coinPoison = Effect(ops = listOf(EffectOp.CoinFlipStatus(Target.OPP_ACTIVE, listOf(Status.POISONED))))
    put(atkKey("sv4-91", "Toxic"), coinPoison)
    put(atkKey("sv4-92", "Toxic Sting"), coinPoison)
    put(atkKey("sv4-110", "Supersonic"), Effect(ops = listOf(EffectOp.CoinFlipStatus(Target.OPP_ACTIVE, listOf(Status.CONFUSED)))))

    // ---- Robar cartas ----
    val draw2 = Effect(ops = listOf(EffectOp.DrawCards(2)))
    put(atkKey("sv4-138", "Punch and Draw"), draw2)
    put(atkKey("sv4-146", "Collect"), draw2)
    put(atkKey("sv4-155", "Nom-Nom-Nom Incisors"), draw2)
    put(atkKey("sv4-233", "Nom-Nom-Nom Incisors"), draw2)
    val draw1 = Effect(ops = listOf(EffectOp.DrawCards(1)))
    put(atkKey("sv4-145", "Filch"), draw1)
    put(atkKey("sv4-211", "Filch"), draw1)

    // ---- Daño de RETROCESO ("también se hace N a sí mismo") ----
    put(atkKey("sv4-21", "Heat Tackle"), Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(30)))))
    put(atkKey("sv4-63", "Thunder"), Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(50)))))
    put(atkKey("sv4-103", "Rocky Tackle"), Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(30)))))
    val wildCharge = Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(20))))
    put(atkKey("sv4-62", "Wild Charge"), wildCharge)
    put(atkKey("sv4-195", "Wild Charge"), wildCharge)
    put(atkKey("sv4-77", "Reckless Charge"), Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(20)))))
    put(atkKey("sv4-131", "Reckless Charge"), Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(10)))))
    val doubleEdged = Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(30))))
    put(atkKey("sv4-135", "Double-Edged Slash"), doubleEdged)
    put(atkKey("sv4-230", "Double-Edged Slash"), doubleEdged)

    // ---- Descartar Energía del PROPIO atacante (coste/efecto del ataque) ----
    val discard1 = Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 1)))
    put(atkKey("sv4-95", "Power Blast"), discard1)
    put(atkKey("sv4-143", "Powered Ball"), discard1)
    put(atkKey("sv4-118", "Dark Edge"), discard1)
    put(atkKey("sv4-205", "Dark Edge"), discard1)
    val extremeCurrent = Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 1)))
    put(atkKey("sv4-68", "Extreme Current"), extremeCurrent)
    put(atkKey("sv4-222", "Extreme Current"), extremeCurrent)
    put(atkKey("sv4-247", "Extreme Current"), extremeCurrent)
    put(atkKey("sv4-57", "Wrathful Blade"), Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 2))))
    put(atkKey("sv4-73", "Luster Purge"), Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 3))))

    // ---- Daño "+X por cada Energía [tipo] unida a este Pokémon" ----
    val scorchingBazooka = Effect(
        attackDamage = listOf(DamageTerm(40)),
        ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.ENERGY_ATTACHED, Target.SELF, 40, EnergyType.FIRE))),
    )
    put(atkKey("sv4-27", "Scorching Bazooka"), scorchingBazooka)
    put(atkKey("sv4-218", "Scorching Bazooka"), scorchingBazooka)
    val hydroPump = Effect(
        attackDamage = listOf(DamageTerm(60)),
        ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.ENERGY_ATTACHED, Target.SELF, 20, EnergyType.WATER))),
    )
    put(atkKey("sv4-54", "Hydro Pump"), hydroPump)
    put(atkKey("sv4-192", "Hydro Pump"), hydroPump)

    // ---- "Lanza 1 moneda. Si cara, +20 de daño" (bono crudo por cara) ----
    val quickHit = Effect(attackDamage = listOf(DamageTerm(10)), ops = listOf(EffectOp.CoinFlipDamage(1, 20)))
    put(atkKey("sv4-8", "Quick Blow"), quickHit)
    put(atkKey("sv4-79", "Quick Attack"), quickHit)
    put(atkKey("sv4-101", "Stone Edge"), Effect(attackDamage = listOf(DamageTerm(20)), ops = listOf(EffectOp.CoinFlipDamage(1, 20))))

    // =========================== Fase 3: ops del DSL (curación, banca, control) ===========================

    // ---- Curación al PROPIO atacante ----
    put(atkKey("sv4-4", "Leech Seed"), Effect(ops = listOf(EffectOp.Heal(Target.SELF, Amount.Fixed(10)))))
    put(atkKey("sv4-16", "Absorb"), Effect(ops = listOf(EffectOp.Heal(Target.SELF, Amount.Fixed(20)))))
    val mushroomDrain = Effect(ops = listOf(EffectOp.Heal(Target.SELF, Amount.Fixed(30))))
    put(atkKey("sv4-17", "Mushroom Drain"), mushroomDrain)
    put(atkKey("sv4-185", "Mushroom Drain"), mushroomDrain)
    // Trop Kick: cura 30 y se recupera de TODAS las Condiciones Especiales.
    val tropKick = Effect(ops = listOf(
        EffectOp.Heal(Target.SELF, Amount.Fixed(30)),
        EffectOp.RemoveStatus(Target.SELF),
    ))
    put(atkKey("sv4-46", "Trop Kick"), tropKick)
    put(atkKey("sv4-220", "Trop Kick"), tropKick)
    // Buoyant Healing: cura 120 a 1 de tus Pokémon de Banca (elegido).
    put(atkKey("sv4-39", "Buoyant Healing"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige un Pokémon de tu Banca para curar 120", "Choose a Benched Pokémon to heal 120")),
        EffectOp.Heal(Target.CHOSEN, Amount.Fixed(120)),
    )))

    // ---- Descartar Energía del Activo RIVAL ----
    put(atkKey("sv4-85", "Crushing Blow"), Effect(ops = listOf(EffectOp.DiscardEnergy(Target.OPP_ACTIVE, 1))))

    // ---- Cambiarse por un Pokémon de la Banca propia ----
    // Swing and Skedaddle: descarta 1 Energía propia y se cambia por 1 de tu Banca.
    val swingSkedaddle = Effect(ops = listOf(
        EffectOp.DiscardEnergy(Target.SELF, 1),
        EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige un Pokémon de tu Banca para pasar al Activo", "Choose a Benched Pokémon to switch to the Active Spot")),
        EffectOp.SwapActiveWithChosen,
    ))
    put(atkKey("sv4-50", "Swing and Skedaddle"), swingSkedaddle)
    put(atkKey("sv4-221", "Swing and Skedaddle"), swingSkedaddle)
    put(atkKey("sv4-246", "Swing and Skedaddle"), swingSkedaddle)
    // Teleportation Burst: PUEDES cambiarte por 1 de tu Banca (opcional).
    put(atkKey("sv4-40", "Teleportation Burst"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Puedes cambiar este Pokémon por 1 de tu Banca", "You may switch this Pokémon with 1 of your Benched Pokémon"), optional = true),
        EffectOp.SwapActiveWithChosen,
    )))

    // ---- Daño adicional a 1 Pokémon de Banca elegido (no aplica Debilidad/Resistencia) ----
    put(atkKey("sv4-3", "Frost Bullet"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OPP_BENCH, 1, prompt("Elige un Pokémon de la Banca rival (20 de daño)", "Choose a Benched Pokémon (20 damage)"), optional = true),
        EffectOp.Damage(Target.CHOSEN, Amount.Fixed(20)),
    )))
    put(atkKey("sv4-65", "Electrobullet"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OPP_BENCH, 1, prompt("Elige un Pokémon de la Banca rival (30 de daño)", "Choose a Benched Pokémon (30 damage)"), optional = true),
        EffectOp.Damage(Target.CHOSEN, Amount.Fixed(30)),
    )))
    put(atkKey("sv4-66", "Raging Thunder"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige un Pokémon de TU Banca (40 de daño)", "Choose 1 of your Benched Pokémon (40 damage)"), optional = true),
        EffectOp.Damage(Target.CHOSEN, Amount.Fixed(40)),
    )))

    // ---- Daño a 1 Pokémon rival cualquiera (sin daño al Activo, o tras descartar) ----
    put(atkKey("sv4-59", "Crackling Shot"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OPP_ALL, 1, prompt("Elige 1 Pokémon del rival (30 de daño)", "Choose 1 of your opponent's Pokémon (30 damage)")),
        EffectOp.Damage(Target.CHOSEN, Amount.Fixed(30)),
    )))
    // Sonic Dive: descarta 2 Energía propia y hace 120 a 1 Pokémon rival elegido.
    val sonicDive = Effect(ops = listOf(
        EffectOp.DiscardEnergy(Target.SELF, 2),
        EffectOp.ChooseTarget(Target.OPP_ALL, 1, prompt("Elige 1 Pokémon del rival (120 de daño)", "Choose 1 of your opponent's Pokémon (120 damage)")),
        EffectOp.Damage(Target.CHOSEN, Amount.Fixed(120)),
    ))
    put(atkKey("sv4-38", "Sonic Dive"), sonicDive)
    put(atkKey("sv4-219", "Sonic Dive"), sonicDive)
    put(atkKey("sv4-245", "Sonic Dive"), sonicDive)
    put(atkKey("sv4-260", "Sonic Dive"), sonicDive)

    // ---- +Daño por Energía unida al Activo RIVAL ----
    put(atkKey("sv4-80", "Psychic"), Effect(
        attackDamage = listOf(DamageTerm(10)),
        ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.ENERGY_ATTACHED, Target.OPP_ACTIVE, 10))),
    ))
    // Energy Crush (50×): 50 por cada Energía unida a TODOS los Pokémon del rival.
    val energyCrush = Effect(ops = listOf(
        EffectOp.ExtraDamage(Amount.PerCount(Counter.ENERGY_ATTACHED, Target.OPP_ALL, 50)),
    ))
    put(atkKey("sv4-98", "Energy Crush"), energyCrush)

    // ---- Control: "no puede atacar / retirarse el próximo turno" ----
    put(atkKey("sv4-83", "Boundless Power"), Effect(ops = listOf(EffectOp.NoAttackNextTurn)))
    put(atkKey("sv4-89", "Laser Blade"), Effect(ops = listOf(EffectOp.NoAttackNextTurn)))
    put(atkKey("sv4-78", "Shadow Bind"), Effect(ops = listOf(EffectOp.DefenderCannotRetreatNextTurn)))

    // ---- "Si cruz, este ataque no hace nada" (una moneda gatea todo el ataque) ----
    put(atkKey("sv4-10", "Surprise Attack"), Effect(coinFlipOrNothing = true))
    put(atkKey("sv4-69", "Whimsy Tackle"), Effect(coinFlipOrNothing = true))

    // ---- Prima: 1 Premio más si este ataque noquea ----
    put(atkKey("sv4-70", "Amp You Very Much"), Effect(extraPrizeIfKo = true))

    // ---- Descartar la carta superior del mazo rival ----
    put(atkKey("sv4-100", "Knocking Hammer"), Effect(ops = listOf(EffectOp.DiscardTopDeck(1, own = false))))

    // =========================== Fase 4: condiciones de daño nuevas ===========================

    // "Si este Pokémon tiene Energía Metálica unida, +X" (condición IfSelfHasEnergyType).
    put(atkKey("sv4-84", "Alloy Aswing"), Effect(attackDamage = listOf(
        DamageTerm(20), DamageTerm(40, DamageCondition.IfSelfHasEnergyType(EnergyType.METAL)))))
    put(atkKey("sv4-85", "Alloyed Hammer"), Effect(attackDamage = listOf(
        DamageTerm(60), DamageTerm(120, DamageCondition.IfSelfHasEnergyType(EnergyType.METAL)))))

    // "Si este Pokémon está afectado por una Condición Especial, +160" (Unhinged Scissors).
    put(atkKey("sv4-105", "Unhinged Scissors"), Effect(attackDamage = listOf(
        DamageTerm(30), DamageTerm(160, DamageCondition.IfSelfAffectedBySpecialCondition))))

    // ---- Daño de SALPICADURA a toda una Banca (crudo, sin Debilidad) ----
    // Liquid Lashing (50): +30 a CADA Pokémon de la Banca rival.
    put(atkKey("sv4-42", "Liquid Lashing"), Effect(ops = listOf(
        EffectOp.Damage(Target.OPP_BENCH, Amount.Fixed(30)))))
    // Earthquake (130): +30 a CADA Pokémon de TU Banca (contragolpe).
    val earthquake = Effect(ops = listOf(EffectOp.Damage(Target.OWN_BENCH, Amount.Fixed(30))))
    put(atkKey("sv4-125", "Earthquake"), earthquake)
    put(atkKey("sv4-208", "Earthquake"), earthquake)

    // =========================== Fase 4c: búsquedas en el mazo (ops existentes) ===========================

    // Call for Family: busca 1 Básico y ponlo en tu Banca.
    val callForFamily = Effect(ops = listOf(
        EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON, isBasic = true), Zone.BENCH, 1)))
    put(atkKey("sv4-4", "Call for Family"), callForFamily)
    put(atkKey("sv4-20", "Call for Family"), callForFamily)
    put(atkKey("sv4-41", "Call for Family"), callForFamily)
    put(atkKey("sv4-87", "Call for Family"), callForFamily)

    // Drawup Power: busca 1 Energía y llévala a tu mano.
    put(atkKey("sv4-35", "Drawup Power"), Effect(ops = listOf(
        EffectOp.SearchDeck(CardFilter(supertype = Supertype.ENERGY), Zone.HAND, 1))))

    // Fiery Fighting Spirit: busca 1 Energía Fuego Básica y únela a este Pokémon.
    put(atkKey("sv4-26", "Fiery Fighting Spirit"), Effect(ops = listOf(
        EffectOp.SearchEnergyAttachSelf(EnergyType.FIRE, 1))))
}
