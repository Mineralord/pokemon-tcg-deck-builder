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
    put(atkKey("sv4-1", "Triple Spin"), TRIPLE_COIN_10)
    put(atkKey("sv4-9", "Double Spin"), Effect(ops = listOf(EffectOp.CoinFlipDamage(2, 40))))
    put(atkKey("sv4-51", "Fury Headbutt"), TRIPLE_COIN_10)
    put(atkKey("sv4-53", "Triple Whip"), Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 70))))
    put(atkKey("sv4-64", "Flail Around"), TRIPLE_COIN_10)
    put(atkKey("sv4-71", "Triple Strike"), TRIPLE_COIN_10)
    put(atkKey("sv4-97", "Three-Step Strike"), Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 20))))
    put(atkKey("sv4-196", "Flail Around"), TRIPLE_COIN_10)
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
    put(atkKey("sv4-31", "Bubble Beam"), COIN_PARALYZE)
    put(atkKey("sv4-67", "Volt Wave"), COIN_PARALYZE)
    put(atkKey("sv4-149", "Body Slam"), COIN_PARALYZE)
    put(atkKey("sv4-212", "Body Slam"), COIN_PARALYZE)
    val coinPoison = Effect(ops = listOf(EffectOp.CoinFlipStatus(Target.OPP_ACTIVE, listOf(Status.POISONED))))
    put(atkKey("sv4-91", "Toxic"), coinPoison)
    put(atkKey("sv4-92", "Toxic Sting"), coinPoison)
    put(atkKey("sv4-110", "Supersonic"), Effect(ops = listOf(EffectOp.CoinFlipStatus(Target.OPP_ACTIVE, listOf(Status.CONFUSED)))))

    // ---- Robar cartas (comportamiento compartido: helper draw(n)) ----
    val draw2 = draw(2)
    put(atkKey("sv4-138", "Punch and Draw"), draw2)
    put(atkKey("sv4-146", "Collect"), draw2)
    put(atkKey("sv4-155", "Nom-Nom-Nom Incisors"), draw2)
    put(atkKey("sv4-233", "Nom-Nom-Nom Incisors"), draw2)
    val draw1 = draw(1)
    put(atkKey("sv4-145", "Filch"), draw1)
    put(atkKey("sv4-211", "Filch"), draw1)

    // ---- Daño de RETROCESO ("también se hace N a sí mismo") ----
    put(atkKey("sv4-21", "Heat Tackle"), recoil(30))
    put(atkKey("sv4-63", "Thunder"), recoil(50))
    put(atkKey("sv4-103", "Rocky Tackle"), recoil(30))
    val wildCharge = recoil(20)
    put(atkKey("sv4-62", "Wild Charge"), wildCharge)
    put(atkKey("sv4-195", "Wild Charge"), wildCharge)
    put(atkKey("sv4-77", "Reckless Charge"), recoil(20))
    put(atkKey("sv4-131", "Reckless Charge"), recoil(10))
    val doubleEdged = recoil(30)
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
    put(atkKey("sv4-83", "Boundless Power"), NO_ATTACK_NEXT_TURN)
    put(atkKey("sv4-89", "Laser Blade"), NO_ATTACK_NEXT_TURN)
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

    // =========================== Fase 5: control de turno y daño variable por monedas ===========================

    // ---- "Durante tu próximo turno, este Pokémon no puede atacar" (NoAttackNextTurn) ----
    val noAttackNext = NO_ATTACK_NEXT_TURN
    put(atkKey("sv4-108", "Earthen Spike"), noAttackNext)
    put(atkKey("sv4-228", "Earthen Spike"), noAttackNext)
    put(atkKey("sv4-250", "Earthen Spike"), noAttackNext)
    put(atkKey("sv4-123", "Rampaging Hammer"), noAttackNext)
    put(atkKey("sv4-207", "Rampaging Hammer"), noAttackNext)
    put(atkKey("sv4-136", "Brave Blade"), noAttackNext)
    put(atkKey("sv4-154", "Tumble Over"), noAttackNext)
    put(atkKey("sv4-225", "Laser Blade"), noAttackNext)
    put(atkKey("sv4-249", "Laser Blade"), noAttackNext)
    put(atkKey("sv4-261", "Laser Blade"), noAttackNext)

    // ---- Control sobre el Activo rival (retirada / coste de ataque / monedas para atacar) ----
    // Corner: el Defensor no puede retirarse el próximo turno.
    put(atkKey("sv4-102", "Corner"), Effect(ops = listOf(EffectOp.DefenderCannotRetreatNextTurn)))
    // Binding Greed: los ataques del Defensor cuestan {C}{C} más el próximo turno.
    put(atkKey("sv4-18", "Binding Greed"), Effect(ops = listOf(EffectOp.BumpDefenderAttackCostNextTurn(2))))
    // Smokescreen Shot: el próximo turno, si el Defensor intenta atacar, lanza 1 moneda; cruz = no ataca.
    put(atkKey("sv4-34", "Smokescreen Shot"), Effect(ops = listOf(
        EffectOp.RequireCoinsToAttackNextTurn(Target.OPP_ACTIVE, 1))))

    // ---- Reducir el daño recibido el próximo turno (Hard Scissors: -20) ----
    put(atkKey("sv4-129", "Hard Scissors"), Effect(ops = listOf(EffectOp.ReduceDamageNextTurn(20))))

    // ---- "Lanza 1 moneda hasta que salga cruz. X por cada cara" (CoinUntilTailsDamage) ----
    val contCoinToss = Effect(ops = listOf(EffectOp.CoinUntilTailsDamage(20)))
    put(atkKey("sv4-88", "Continuous Coin Toss"), contCoinToss)
    put(atkKey("sv4-198", "Continuous Coin Toss"), contCoinToss)
    put(atkKey("sv4-153", "Damage Rush"), Effect(ops = listOf(EffectOp.CoinUntilTailsDamage(10))))

    // ---- Protección por moneda ("si cara, evita todo el daño el próximo turno") ----
    val protect = Effect(ops = listOf(EffectOp.PreventDamageNextTurn(Target.SELF, coinFlip = true)))
    put(atkKey("sv4-11", "Protect"), protect)
    put(atkKey("sv4-25", "Protect"), protect)
    put(atkKey("sv4-184", "Protect"), protect)
    put(atkKey("sv4-47", "Hide"), protect)
    put(atkKey("sv4-90", "Hard Headbutt"), protect) // base 20 fijo + protección por moneda

    // ---- Daño que ignora los efectos del Activo rival ----
    val ignoreDefEffects = Effect(ignoresDefenderEffects = true)
    put(atkKey("sv4-134", "Hard Bashing"), ignoreDefEffects)
    put(atkKey("sv4-210", "Hard Bashing"), ignoreDefEffects)
    put(atkKey("sv4-137", "Luster Burn"), ignoreDefEffects)

    // ---- "Si cruz, este ataque no hace nada" ----
    put(atkKey("sv4-119", "Surprise Attack"), Effect(coinFlipOrNothing = true))

    // ---- Prima: 1 Premio más si este ataque noquea ----
    val extraPrize = Effect(extraPrizeIfKo = true)
    put(atkKey("sv4-223", "Amp You Very Much"), extraPrize)
    put(atkKey("sv4-248", "Amp You Very Much"), extraPrize)

    // ---- Descartar la carta superior del mazo rival ----
    put(atkKey("sv4-107", "Stomp Off"), Effect(ops = listOf(EffectOp.DiscardTopDeck(1, own = false))))
    put(atkKey("sv4-203", "Stomp Off"), Effect(ops = listOf(EffectOp.DiscardTopDeck(1, own = false))))
    put(atkKey("sv4-227", "Knocking Hammer"), Effect(ops = listOf(EffectOp.DiscardTopDeck(1, own = false))))

    // ---- Descartar Energía propia como efecto del ataque (daño fijo) ----
    val gaiaPunk = Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 3, EnergyType.LIGHTNING)))
    put(atkKey("sv4-100", "Gaia Punk"), gaiaPunk)
    put(atkKey("sv4-227", "Gaia Punk"), gaiaPunk)

    // =========================== Fase 6: daño por conteo de Energía, snipe y curación dirigida ===========================

    // ---- +Daño por Energía unida (ExtraDamage con PerCount) ----
    // Energized Attack (40×): 40 por cada Energía unida a este Pokémon (base Variable → 0).
    val energizedAttack = Effect(ops = listOf(
        EffectOp.ExtraDamage(Amount.PerCount(Counter.ENERGY_ATTACHED, Target.SELF, 40))))
    put(atkKey("sv4-144", "Energized Attack"), energizedAttack)
    put(atkKey("sv4-214", "Energized Attack"), energizedAttack)
    // Photon Kinesis (10+): +30 por cada Energía Psíquica unida a TODOS tus Pokémon.
    put(atkKey("sv4-58", "Photon Kinesis"), Effect(
        attackDamage = listOf(DamageTerm(10)),
        ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.ENERGY_ATTACHED, Target.OWN_ALL, 30, EnergyType.PSYCHIC))),
    ))
    // Energy Crush (50×): 50 por cada Energía unida a TODOS los Pokémon del rival.
    put(atkKey("sv4-226", "Energy Crush"), Effect(ops = listOf(
        EffectOp.ExtraDamage(Amount.PerCount(Counter.ENERGY_ATTACHED, Target.OPP_ALL, 50)))))

    // ---- Snipe: daño a 1 Pokémon rival (opcionalmente solo dañados) ----
    // Blindside: 100 a 1 Pokémon rival que tenga algún contador de daño.
    put(atkKey("sv4-176", "Blindside"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OPP_ALL, 1, prompt("Elige 1 Pokémon dañado del rival (100 de daño)", "Choose 1 of your opponent's damaged Pokémon (100 damage)"), onlyDamaged = true),
        EffectOp.Damage(Target.CHOSEN, Amount.Fixed(100)),
    )))
    // Roaring Scream: 20 a 1 Pokémon rival por cada contador de daño en ESTE Pokémon.
    put(atkKey("sv4-86", "Roaring Scream"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OPP_ALL, 1, prompt("Elige 1 Pokémon del rival", "Choose 1 of your opponent's Pokémon")),
        EffectOp.Damage(Target.CHOSEN, Amount.PerCount(Counter.DAMAGE_COUNTERS, Target.SELF, 20)),
    )))
    // Frost Bullet (140): +20 a 1 Pokémon de la Banca rival.
    put(atkKey("sv4-217", "Frost Bullet"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OPP_BENCH, 1, prompt("Elige un Pokémon de la Banca rival (20 de daño)", "Choose a Benched Pokémon (20 damage)"), optional = true),
        EffectOp.Damage(Target.CHOSEN, Amount.Fixed(20)),
    )))

    // ---- Contadores de daño repartidos / fijados ----
    // Hollow Hands (110): reparte 5 contadores de daño en la Banca rival como quieras.
    val hollowHands = Effect(ops = listOf(EffectOp.PlaceCounters(5, Target.OPP_BENCH)))
    put(atkKey("sv4-76", "Hollow Hands"), hollowHands)
    put(atkKey("sv4-224", "Hollow Hands"), hollowHands)
    // Ominous Eyes: pon 3 contadores de daño (=30) en 1 Pokémon rival.
    put(atkKey("sv4-75", "Ominous Eyes"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OPP_ALL, 1, prompt("Elige 1 Pokémon del rival (3 contadores de daño)", "Choose 1 of your opponent's Pokémon (3 damage counters)")),
        EffectOp.Damage(Target.CHOSEN, Amount.Fixed(30)),
    )))

    // ---- Curación dirigida a 1 de tus Pokémon ----
    // Bind Wound: cura 30 a 1 de tus Pokémon (dañado).
    val bindWound = Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_ALL, 1, prompt("Elige 1 de tus Pokémon para curar 30", "Choose 1 of your Pokémon to heal 30"), onlyDamaged = true),
        EffectOp.Heal(Target.CHOSEN, Amount.Fixed(30)),
    ))
    put(atkKey("sv4-152", "Bind Wound"), bindWound)
    put(atkKey("sv4-213", "Bind Wound"), bindWound)
    // Buoyant Healing (sv4-189): cura 120 a 1 de tus Pokémon de Banca.
    put(atkKey("sv4-189", "Buoyant Healing"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige un Pokémon de tu Banca para curar 120", "Choose a Benched Pokémon to heal 120")),
        EffectOp.Heal(Target.CHOSEN, Amount.Fixed(120)),
    )))

    // ---- Cambios de Activo ----
    // Bounce (30): cámbiate por 1 de tu Banca.
    put(atkKey("sv4-122", "Bounce"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige un Pokémon de tu Banca para pasar al Activo", "Choose a Benched Pokémon to switch to the Active Spot"), optional = true),
        EffectOp.SwapActiveWithChosen,
    )))
    // Push Down (10): manda el Activo rival a la Banca (el rival elige el nuevo Activo).
    put(atkKey("sv4-148", "Push Down"), GUST_DEFENDER)

    // ---- Autonoqueo del Activo rival + retroceso (Frenzied Gouging) ----
    val frenziedGouging = Effect(ops = listOf(
        EffectOp.CoinFlipKnockOutTarget(Target.OPP_ACTIVE, coinFlip = false),
        EffectOp.Recoil(Amount.Fixed(200)),
    ))
    put(atkKey("sv4-124", "Frenzied Gouging"), frenziedGouging)
    put(atkKey("sv4-229", "Frenzied Gouging"), frenziedGouging)
    put(atkKey("sv4-251", "Frenzied Gouging"), frenziedGouging)
    put(atkKey("sv4-262", "Frenzied Gouging"), frenziedGouging)

    // ---- Descartar cartas de la mano para daño (X por carta) ----
    // Make It Rain (50×): descarta cualquier nº de Energía Básica de tu mano, 50 c/u.
    val makeItRain = Effect(ops = listOf(
        EffectOp.DiscardFromHandForDamage(CardFilter(supertype = Supertype.ENERGY), maxCount = 20, perCard = 50)))
    put(atkKey("sv4-139", "Make It Rain"), makeItRain)
    put(atkKey("sv4-231", "Make It Rain"), makeItRain)
    put(atkKey("sv4-252", "Make It Rain"), makeItRain)
    // Chuck (50×): descarta cualquier nº de Herramientas Pokémon de tu mano, 50 c/u.
    val chuck = Effect(ops = listOf(
        EffectOp.DiscardFromHandForDamage(CardFilter(trainerKind = TrainerCategory.TOOL), maxCount = 20, perCard = 50)))
    put(atkKey("sv4-117", "Chuck"), chuck)
    put(atkKey("sv4-204", "Chuck"), chuck)

    // ---- Unir Energía del descarte "de la forma que quieras" (decisión de reparto) ----
    // Hydro Lander (160): une hasta 3 Energía Lucha Básica del descarte a tu Banca.
    val hydroLander = Effect(ops = listOf(
        EffectOp.AttachEnergyFromDiscard(3, EnergyType.FIGHTING, Target.OWN_BENCH)))
    put(atkKey("sv4-38", "Hydro Lander"), hydroLander)
    put(atkKey("sv4-219", "Hydro Lander"), hydroLander)
    put(atkKey("sv4-245", "Hydro Lander"), hydroLander)
    put(atkKey("sv4-260", "Hydro Lander"), hydroLander)
    // Transfer Charge: une hasta 2 Energía Psíquica Básica del descarte a tus Pokémon.
    put(atkKey("sv4-58", "Transfer Charge"), Effect(ops = listOf(
        EffectOp.AttachEnergyFromDiscard(2, EnergyType.PSYCHIC, Target.OWN_ALL))))
    // Dual Turbo (20): une hasta 2 Energía Fuego Básica del descarte a tu Banca.
    put(atkKey("sv4-22", "Dual Turbo"), Effect(ops = listOf(
        EffectOp.AttachEnergyFromDiscard(2, EnergyType.FIRE, Target.OWN_BENCH))))
    // Supplemental Swallow-Up (Dondozo sv4-55): mira las 5 primeras cartas del mazo y une cualquier
    // Energía Básica que encuentres a "este Pokémon"; el resto se baraja de vuelta. Se reutiliza
    // RevealAttachEnergy (includeActive = true): el destino es tus Pokémon en juego (incluye el
    // Activo atacante). Pequeña holgura frente al literal "a este Pokémon" (permite también la Banca).
    put(atkKey("sv4-55", "Supplemental Swallow-Up"), Effect(ops = listOf(
        EffectOp.RevealAttachEnergy(lookAt = 5, maxAttach = 5, energyType = null, benchType = null, includeActive = true))))

    // ---- Búsquedas en el mazo ----
    // Charge Energy (ataque): busca hasta 2 Energía Básica → mano.
    put(atkKey("sv4-126", "Charge Energy"), Effect(ops = listOf(
        EffectOp.SearchDeck(CardFilter(supertype = Supertype.ENERGY), Zone.HAND, 2))))
    // Fast Carrier: busca hasta 3 Pokémon Básicos → Banca.
    val fastCarrier = Effect(ops = listOf(
        EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON, isBasic = true), Zone.BENCH, 3)))
    put(atkKey("sv4-156", "Fast Carrier"), fastCarrier)
    put(atkKey("sv4-234", "Fast Carrier"), fastCarrier)
    // Survival Strategy: busca hasta 2 cartas → mano y PUEDES cambiarte por 1 de tu Banca.
    put(atkKey("sv4-141", "Survival Strategy"), Effect(ops = listOf(
        EffectOp.SearchDeck(CardFilter(), Zone.HAND, 2),
        EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Puedes cambiar este Pokémon por 1 de tu Banca", "You may switch this Pokémon with 1 of your Benched Pokémon"), optional = true),
        EffectOp.SwapActiveWithChosen,
    )))

    // =========================== Fase 7: daño "X×" por conteos de estado y bonos "+X" condicionales ===========================

    // ---- Daño "X por cada …" vía contadores de estado nuevos (crudo, como el resto de X×) ----
    // Hand Fling (20×): 20 por cada carta en TU mano.
    put(atkKey("sv4-146", "Hand Fling"), Effect(ops = listOf(
        EffectOp.ExtraDamage(Amount.PerCount(Counter.OWN_HAND_SIZE, Target.SELF, 20)))))
    // Powerful Cross (20×): 20 por cada carta en la mano del RIVAL.
    put(atkKey("sv4-49", "Powerful Cross"), Effect(ops = listOf(
        EffectOp.ExtraDamage(Amount.PerCount(Counter.OPP_HAND_SIZE, Target.OPP_ACTIVE, 20)))))
    // Gravitational Tackle (20×): 20 por cada {C} del Coste de Retirada del Activo rival.
    val gravTackle = Effect(ops = listOf(
        EffectOp.ExtraDamage(Amount.PerCount(Counter.OPP_RETREAT_COST, Target.OPP_ACTIVE, 20))))
    put(atkKey("sv4-99", "Gravitational Tackle"), gravTackle)
    put(atkKey("sv4-201", "Gravitational Tackle"), gravTackle)
    // Satellite Beam (30×): 30 por cada Energía en el descarte del RIVAL.
    put(atkKey("sv4-12", "Satellite Beam"), Effect(ops = listOf(
        EffectOp.ExtraDamage(Amount.PerCount(Counter.OPP_DISCARD_ENERGY, Target.OPP_ACTIVE, 30)))))
    // Peerless Edge (70×): 70 por cada Premio que hayas tomado.
    val peerlessEdge = Effect(ops = listOf(
        EffectOp.ExtraDamage(Amount.PerCount(Counter.OWN_PRIZES_TAKEN, Target.SELF, 70))))
    put(atkKey("sv4-135", "Peerless Edge"), peerlessEdge)
    put(atkKey("sv4-230", "Peerless Edge"), peerlessEdge)

    // ---- Bonos "+X" condicionales nuevos (pasan por Debilidad/Resistencia una vez) ----
    // Crunch-Time Rush (90+): +150 si tu baraja tiene 3 cartas o menos.
    put(atkKey("sv4-138", "Crunch-Time Rush"), Effect(attackDamage = listOf(
        DamageTerm(90), DamageTerm(150, DamageCondition.IfDeckCountAtMost(3)))))
    // Glittering Eyes (70+): +70 si "Tulip" está en tu descarte.
    val glitteringEyes = Effect(attackDamage = listOf(
        DamageTerm(70), DamageTerm(70, DamageCondition.IfCardInOwnDiscardNamed("Tulip"))))
    put(atkKey("sv4-81", "Glittering Eyes"), glitteringEyes)
    put(atkKey("sv4-197", "Glittering Eyes"), glitteringEyes)
    // Megafire of Envy (50+): +90 si alguno de tus Pokémon fue Noqueado el último turno del rival.
    put(atkKey("sv4-29", "Megafire of Envy"), Effect(attackDamage = listOf(
        DamageTerm(50), DamageTerm(90, DamageCondition.IfOwnKoLastOppTurn))))
    // Enhanced Blade (20+): +60 si este Pokémon tiene una Herramienta unida.
    put(atkKey("sv4-113", "Enhanced Blade"), Effect(attackDamage = listOf(
        DamageTerm(20), DamageTerm(60, DamageCondition.IfSelfHasTool))))
    // Whip Expert (50+): +70 si uniste una Herramienta a este Pokémon desde tu mano este turno.
    val whipExpert = Effect(attackDamage = listOf(
        DamageTerm(50), DamageTerm(70, DamageCondition.IfAttachedToolThisTurn)))
    put(atkKey("sv4-97", "Whip Expert"), whipExpert)
    put(atkKey("sv4-200", "Whip Expert"), whipExpert)
    // Lively Tackle (Miltank sv4-147, 60+): +90 si este Pokémon fue curado este turno.
    put(atkKey("sv4-147", "Lively Tackle"), Effect(attackDamage = listOf(
        DamageTerm(60), DamageTerm(90, DamageCondition.IfSelfHealedThisTurn))))

    // Vengeful Shock (30+): +90 si tus Pokémon fueron Noqueados el último turno del rival; además
    // el Activo rival queda Paralizado (el estado se aplica SIEMPRE, el bono es condicional).
    val vengefulShock = Effect(
        attackDamage = listOf(DamageTerm(30), DamageTerm(90, DamageCondition.IfOwnKoLastOppTurn)),
        ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.PARALYZED))),
    )
    put(atkKey("sv4-68", "Vengeful Shock"), vengefulShock)
    put(atkKey("sv4-222", "Vengeful Shock"), vengefulShock)
    put(atkKey("sv4-247", "Vengeful Shock"), vengefulShock)

    // =========================== Fase 8: auto-restricción / auto-buff por NOMBRE de ataque ===========================

    // ---- "Durante tu próximo turno, este Pokémon no puede usar ESTE ataque" ----
    val lockSelfAttack = Effect(locksSelfAttackNextTurn = true)
    put(atkKey("sv4-28", "Heat Ray"), lockSelfAttack)
    put(atkKey("sv4-187", "Heat Ray"), lockSelfAttack)
    put(atkKey("sv4-98", "Bandit's Fist"), lockSelfAttack)
    put(atkKey("sv4-226", "Bandit's Fist"), lockSelfAttack)
    put(atkKey("sv4-132", "Slashing Strike"), lockSelfAttack)

    // ---- "Durante tu próximo turno, el ataque X de este Pokémon hace +N" ----
    // Spinning Needles (50): su propio ataque hace +100 el próximo turno.
    val spinningNeedles = Effect(buffsAttackNamed = "Spinning Needles", buffsAttackAmount = 100)
    put(atkKey("sv4-128", "Spinning Needles"), spinningNeedles)
    put(atkKey("sv4-209", "Spinning Needles"), spinningNeedles)
    // Swords Dance: el ataque "Slicing Blade" de este Pokémon hace +80 el próximo turno.
    put(atkKey("sv4-133", "Swords Dance"), Effect(buffsAttackNamed = "Slicing Blade", buffsAttackAmount = 80))

    // ---- "Si este Pokémon tiene 4+ contadores de daño, este ataque no hace nada" (Arrogant Impact) ----
    put(atkKey("sv4-109", "Arrogant Impact"), Effect(noDamageIfSelfCountersAtLeast = 4))

    // =========================== Fase 9: ops deterministas nuevas (sin decisiones) ===========================

    // Crushing Short (20): antes de hacer daño, descarta todas las Herramientas del Activo rival.
    put(atkKey("sv4-66", "Crushing Short"), Effect(ops = listOf(EffectOp.DiscardTargetTools(Target.OPP_ACTIVE))))
    // Rolling Fireball (90): devuelve 1 Energía unida a este Pokémon a tu mano.
    put(atkKey("sv4-24", "Rolling Fireball"), Effect(ops = listOf(EffectOp.BounceSelfEnergyToHand(1))))
    // Devolution: involuciona (un paso) todos los Pokémon de Evolución del rival.
    put(atkKey("sv4-177", "Devolution"), Effect(ops = listOf(EffectOp.DeEvolveAllOpponent)))
    // Scorching Heater: si este Pokémon es dañado el próximo turno, 6 contadores (60) al Atacante.
    val scorchingHeater = Effect(ops = listOf(EffectOp.ScheduleCountersOnAttackerNextTurn(60)))
    put(atkKey("sv4-19", "Scorching Heater"), scorchingHeater)
    put(atkKey("sv4-186", "Scorching Heater"), scorchingHeater)
    // Light Pulse (140): el próximo turno, evita todos los EFECTOS de los ataques rivales (no el daño).
    val lightPulse = Effect(ops = listOf(EffectOp.PreventAttackEffectsNextTurn))
    put(atkKey("sv4-140", "Light Pulse"), lightPulse)
    put(atkKey("sv4-232", "Light Pulse"), lightPulse)
    put(atkKey("sv4-253", "Light Pulse"), lightPulse)
    // Refrigerated Stream (80): si el Activo rival es de Evolución, no puede atacar el próximo turno.
    put(atkKey("sv4-56", "Refrigerated Stream"), Effect(ops = listOf(EffectOp.DefenderCannotAttackNextTurnIfEvolved)))
    // Crag Bash (100): el próximo turno recibe 100 de daño menos de ataques de Pokémon de Evolución.
    val cragBash = Effect(ops = listOf(EffectOp.ReduceDamageNextTurn(100, onlyFromEvolution = true)))
    put(atkKey("sv4-7", "Crag Bash"), cragBash)
    put(atkKey("sv4-183", "Crag Bash"), cragBash)
    // Leech Life (30): cura al atacante una cantidad igual al daño infligido.
    put(atkKey("sv4-111", "Leech Life"), Effect(healSelfEqualToDamageDealt = true))

    // =========================== Fase 10: unir Energía del descarte a 1 Pokémon elegido + snipe múltiple ===========================

    // Familia "Bringer": elige 1 de tus Pokémon y únele hasta 2 Energía Básica [tipo] de tu descarte.
    fun bringer(type: EnergyType) = Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_ALL, 1, prompt("Elige 1 de tus Pokémon para unirle Energía", "Choose 1 of your Pokémon to attach Energy to")),
        EffectOp.AttachEnergyFromDiscard(2, type, Target.CHOSEN),
    ))
    put(atkKey("sv4-18", "Leaf Bringer"), bringer(EnergyType.GRASS))
    put(atkKey("sv4-29", "Flare Bringer"), bringer(EnergyType.FIRE))
    put(atkKey("sv4-57", "Snow Bringer"), bringer(EnergyType.WATER))
    put(atkKey("sv4-109", "Sand Bringer"), bringer(EnergyType.FIGHTING))
    // Iron Roar (30): elige 1 de tu Banca y únele 1 Energía Metálica Básica de tu descarte.
    put(atkKey("sv4-136", "Iron Roar"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige un Pokémon de tu Banca para unirle Energía", "Choose a Benched Pokémon to attach Energy to")),
        EffectOp.AttachEnergyFromDiscard(1, EnergyType.METAL, Target.CHOSEN),
    )))

    // Homing Headbutt: 50 de daño a 3 Pokémon rivales que tengan contadores de daño.
    val homingHeadbutt = Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OPP_ALL, 3, prompt("Elige hasta 3 Pokémon dañados del rival (50 de daño)", "Choose up to 3 of your opponent's damaged Pokémon (50 damage)"), onlyDamaged = true, optional = true),
        EffectOp.Damage(Target.CHOSEN, Amount.Fixed(50)),
    ))
    put(atkKey("sv4-158", "Homing Headbutt"), homingHeadbutt)
    put(atkKey("sv4-216", "Homing Headbutt"), homingHeadbutt)

    // =========================== Fase 11: contadores, descarte de energía por daño y scoop ===========================

    // Dishonest Swap: mueve todos los contadores de daño de 1 de tu Banca al Activo rival.
    put(atkKey("sv4-115", "Dishonest Swap"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige 1 de tu Banca (mueve su daño al Activo rival)", "Choose 1 of your Benched Pokémon (move its damage to the opponent's Active)"), onlyDamaged = true, optional = true),
        EffectOp.MoveChosenDamageToOppActive,
    )))
    // Icicle Sole: pon contadores en 1 Pokémon rival hasta dejarlo con 30 PS.
    val icicleSole = Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OPP_ALL, 1, prompt("Elige 1 Pokémon del rival (dejarlo con 30 PS)", "Choose 1 of your opponent's Pokémon (leave it with 30 HP)")),
        EffectOp.DamageToLeaveHp(Target.CHOSEN, 30),
    ))
    put(atkKey("sv4-46", "Icicle Sole"), icicleSole)
    put(atkKey("sv4-220", "Icicle Sole"), icicleSole)
    // Magma Purge (60×): descarta hasta 4 Energías del atacante; 60 de daño por cada una.
    val magmaPurge = Effect(ops = listOf(EffectOp.DiscardSelfEnergyForDamage(4, 60)))
    put(atkKey("sv4-93", "Magma Purge"), magmaPurge)
    put(atkKey("sv4-199", "Magma Purge"), magmaPurge)
    // Shadowy Wind (130): devuelve este Pokémon y sus cartas unidas a tu mano.
    val shadowyWind = Effect(ops = listOf(EffectOp.ScoopSelfToHand))
    put(atkKey("sv4-156", "Shadowy Wind"), shadowyWind)
    put(atkKey("sv4-234", "Shadowy Wind"), shadowyWind)
    // Sneaky Snacking: lanza 1 moneda; si cara, el rival descarta 1 carta al azar de su mano.
    val sneakySnacking = Effect(ops = listOf(EffectOp.OpponentDiscardsHand(1, coinFlip = true)))
    put(atkKey("sv4-48", "Sneaky Snacking"), sneakySnacking)
    put(atkKey("sv4-191", "Sneaky Snacking"), sneakySnacking)

    // =========================== Fase 12: unir Energía desde la mano y mover Energía propia ===========================

    // Clinging Spore: une 1 Energía Hierba Básica de tu mano a 1 de tu Banca.
    put(atkKey("sv4-15", "Clinging Spore"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige un Pokémon de tu Banca para unirle Energía", "Choose a Benched Pokémon to attach Energy to")),
        EffectOp.AttachEnergyFromHand(EnergyType.GRASS, Target.CHOSEN, 1),
    )))
    // Swelling Power: une 1 Energía Lucha Básica de tu mano a 1 de tus Pokémon.
    val swellingPower = Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_ALL, 1, prompt("Elige 1 de tus Pokémon para unirle Energía", "Choose 1 of your Pokémon to attach Energy to")),
        EffectOp.AttachEnergyFromHand(EnergyType.FIGHTING, Target.CHOSEN, 1),
    ))
    put(atkKey("sv4-93", "Swelling Power"), swellingPower)
    put(atkKey("sv4-199", "Swelling Power"), swellingPower)
    // Energizer Wheel (70): mueve 2 Energía de este Pokémon a 1 de tu Banca.
    val energizerWheel = Effect(ops = listOf(EffectOp.MoveEnergy(Target.SELF, Target.OWN_BENCH, 2)))
    put(atkKey("sv4-121", "Energizer Wheel"), energizerWheel)
    put(atkKey("sv4-206", "Energizer Wheel"), energizerWheel)
    // Genome Spiral (120): mueve toda la Energía de este Pokémon a tu Banca.
    put(atkKey("sv4-74", "Genome Spiral"), Effect(ops = listOf(EffectOp.MoveEnergy(Target.SELF, Target.OWN_BENCH, 20))))

    // =========================== Fase 13: daño condicional por Estadio y descarte del Activo rival ===========================

    // Calamity Storm (100+): si hay un Estadio en juego, +120 y se descarta ese Estadio.
    val calamityStorm = Effect(
        attackDamage = listOf(DamageTerm(100), DamageTerm(120, DamageCondition.IfStadiumInPlay)),
        ops = listOf(EffectOp.DiscardStadium),
    )
    put(atkKey("sv4-124", "Calamity Storm"), calamityStorm)
    put(atkKey("sv4-229", "Calamity Storm"), calamityStorm)
    put(atkKey("sv4-251", "Calamity Storm"), calamityStorm)
    put(atkKey("sv4-262", "Calamity Storm"), calamityStorm)
    // Sudden Shout: descarta (noquea) el Activo rival; no hace nada si este Pokémon no evolucionó este turno.
    put(atkKey("sv4-150", "Sudden Shout"), Effect(
        onlyIfEvolvedThisTurn = true,
        ops = listOf(EffectOp.CoinFlipKnockOutTarget(Target.OPP_ACTIVE, coinFlip = false)),
    ))

    // =========================== Fase 14: Entrenadores (Partidarios / Objetos) ===========================
    // Clave = id de carta (sin #atk). "Juegas solo 1 Partidario/Estadio por turno" lo gestiona el motor.

    // Roco (Roark) (Partidario): roba 2 y recupera 1 Energía Básica de tu descarte a la mano.
    val roark = Effect(ops = listOf(
        EffectOp.DrawCards(2),
        EffectOp.RecoverFromDiscard(CardFilter(supertype = Supertype.ENERGY), 1),
    ))
    put(EffectId("sv4-173"), roark)
    put(EffectId("sv4-242"), roark)

    // Melo (Mela) (Partidario): solo si te noquearon el turno pasado. Une 1 Energía Fuego Básica de tu
    // descarte a 1 de tus Pokémon y, si lo haces, roba hasta tener 6 cartas.
    val mela = Effect(
        requiresOwnKoLastTurn = true,
        ops = listOf(EffectOp.AttachEnergyFromDiscard(1, EnergyType.FIRE, Target.OWN_ALL, thenDrawUpTo = 6)),
    )
    put(EffectId("sv4-167"), mela)
    put(EffectId("sv4-236"), mela)
    put(EffectId("sv4-254"), mela)

    // Capturador Contraataque (Counter Catcher) (Objeto): solo si te quedan más Premios que a tu
    // rival. Sube al Activo rival 1 Pokémon de su Banca (tú eliges).
    val counterCatcher = Effect(
        requiresMorePrizesRemaining = true,
        ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_BENCH, 1, prompt("Elige 1 Pokémon de la Banca rival para subirlo al Activo", "Choose 1 of your opponent's Benched Pokémon to switch to the Active Spot")),
            EffectOp.SwapOppActiveWithChosen,
        ),
    )
    put(EffectId("sv4-160"), counterCatcher)
    put(EffectId("sv4-264"), counterCatcher)

    // --- Herramientas con subtipo Pasado/Futuro y condiciones de Premio/caja de regla ---

    // Tanque de Energía Potenciadora del Futuro (Future Booster Energy Capsule): el Pokémon FUTURO
    // al que está unida no tiene Coste de Retirada y sus ataques hacen +20 al Activo rival.
    put(EffectId("sv4-164"), Effect(passives = listOf(
        PassiveModifier(ModKind.NO_RETREAT, appliesTo = Target.SELF, requiresHolderSubtype = "Futuro"),
        PassiveModifier(ModKind.EXTRA_DAMAGE, amount = 20, appliesTo = Target.SELF, requiresHolderSubtype = "Futuro"),
    )))

    // Chaleco Desafío (Defiance Vest): si te quedan más Premios que a tu rival, el Pokémon al que
    // está unida recibe 40 de daño menos de los ataques rivales (tras Debilidad/Resistencia).
    val defianceVest = Effect(passives = listOf(
        PassiveModifier(ModKind.REDUCE_DAMAGE, amount = 40, appliesTo = Target.SELF, requiresMorePrizesRemaining = true),
    ))
    put(EffectId("sv4-162"), defianceVest)

    // Tanque de Energía Potenciadora del Pasado (Ancient Booster Energy Capsule): el Pokémon PASADO
    // al que está unida obtiene +60 PS. (La inmunidad/recuperación de Condiciones Especiales queda
    // pendiente de cableado del motor; el +60 PS sí está activo.)
    put(EffectId("sv4-159"), Effect(passives = listOf(
        PassiveModifier(ModKind.EXTRA_HP, amount = 60, appliesTo = Target.SELF, requiresHolderSubtype = "Pasado"),
    )))

    // Capa Suntuosa (Luxurious Cape): si el Pokémon al que está unida NO tiene caja de regla, +100 PS.
    // (El "1 Premio más al ser Noqueado" queda pendiente; el +100 PS sí está activo.)
    val luxuriousCape = Effect(passives = listOf(
        PassiveModifier(ModKind.EXTRA_HP, amount = 100, appliesTo = Target.SELF, requiresNoRuleBox = true),
    ))
    put(EffectId("sv4-166"), luxuriousCape)
    put(EffectId("sv4-265"), luxuriousCape)

    // Escenario del Profesor Turo (Professor Turo's Scenario) (Partidario): devuelve 1 de tus
    // Pokémon a tu mano, descartando todas las cartas unidas a él.
    val turoScenario = Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_ALL, 1, prompt("Elige 1 de tus Pokémon para devolverlo a tu mano", "Choose 1 of your Pokémon to put into your hand")),
        EffectOp.BounceChosenToHandDiscardingAttached,
    ))
    put(EffectId("sv4-171"), turoScenario)
    put(EffectId("sv4-240"), turoScenario)
    put(EffectId("sv4-257"), turoScenario)
}
