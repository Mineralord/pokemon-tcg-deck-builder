package com.mineralord.tcg.engine.model

/**
 * Efectos de la expansión **Llamaradas Fantasmales** (`me2`, Phantasmal Flames) de la serie
 * Megaevolución. AISLADO por expansión (no toca otros sets). Registrado por [EffectsDb.registry] vía
 * [registerLlamaradasFantasmales].
 *
 * COBERTURA — Fase 1 (daño variable mapeable al DSL existente): ataques por MONEDAS, bonos "X+"
 * condicionales que ya tienen su [DamageCondition], daño "×N por Energía" unida y descarte de Energía
 * propia por daño. Las condiciones aún inexistentes en el motor (contar Pokémon en juego, "si subió de
 * la Banca este turno", "si el rival tiene ≤N Premios", etc.) quedan para fases posteriores y NO se
 * registran (el ataque degradaría a 0; lo vigila el test de cobertura de me2).
 */
internal fun MutableMap<EffectId, Effect>.registerLlamaradasFantasmales() {

    // ---- Daño por MONEDAS: "Lanza N monedas. X por cada cara" (daño base Variable → 0) ----
    val flail10 = Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 10)))     // Flail Around
    put(atkKey("PFL-LA-9", "Flail Around"), flail10)
    put(atkKey("PFL-LA-96", "Flail Around"), flail10)
    put(atkKey("PFL-LA-106", "Fury Swipes"), Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 20))))
    val slam70 = Effect(ops = listOf(EffectOp.CoinFlipDamage(2, 70)))      // Slam
    put(atkKey("PFL-LA-22", "Slam"), slam70)
    put(atkKey("PFL-LA-97", "Slam"), slam70)
    put(atkKey("PFL-LA-51", "Double Headbutt"), Effect(ops = listOf(EffectOp.CoinFlipDamage(2, 10))))
    // "Lanza 1 moneda hasta que salga cruz. 20 por cada cara."
    put(atkKey("PFL-LA-76", "Ball Roll"), Effect(ops = listOf(EffectOp.CoinUntilTailsDamage(20))))

    // ---- "Lanza 1 moneda. Si cara, +X" (base fijo en attackDamage + bono crudo por cara) ----
    fun coinBonus(base: Int, bonus: Int) =
        Effect(attackDamage = listOf(DamageTerm(base)), ops = listOf(EffectOp.CoinFlipDamage(1, bonus)))
    val ambush = coinBonus(10, 20)        // Ambush / Trip Over (base 10, +20)
    put(atkKey("PFL-LA-57", "Ambush"), ambush)
    put(atkKey("PFL-LA-102", "Trip Over"), ambush)
    val playRough = coinBonus(20, 20)     // Play Rough (base 20, +20)
    put(atkKey("PFL-LA-30", "Play Rough"), playRough)
    put(atkKey("PFL-LA-99", "Play Rough"), playRough)
    put(atkKey("PFL-LA-24", "Rising Lunge"), coinBonus(30, 30))
    put(atkKey("PFL-LA-31", "Electric Run"), coinBonus(70, 70))

    // ---- Bonos "X+" condicionales (pasan por Debilidad/Resistencia una vez) ----
    // Fighting Wings (20+): +90 si el Activo rival es un Pokémon ex.
    put(atkKey("PFL-LA-14", "Fighting Wings"), Effect(attackDamage = listOf(
        DamageTerm(20), DamageTerm(90, DamageCondition.IfDefenderExOrV))))
    // Finishing Blow (90+): +90 si el Activo rival ya tiene contadores de daño.
    put(atkKey("PFL-LA-38", "Finishing Blow"), Effect(attackDamage = listOf(
        DamageTerm(90), DamageTerm(90, DamageCondition.IfDefenderHasDamage))))
    // Hungry Jaws (120+): +150 si este Pokémon tiene algún contador de daño.
    val hungryJaws = Effect(attackDamage = listOf(
        DamageTerm(120), DamageTerm(150, DamageCondition.IfSelfHasDamage)))
    put(atkKey("PFL-LA-61", "Hungry Jaws"), hungryJaws)
    put(atkKey("PFL-LA-113", "Hungry Jaws"), hungryJaws)
    put(atkKey("PFL-LA-127", "Hungry Jaws"), hungryJaws)
    // Vengeful Fang (60+): +160 si alguno de tus Pokémon fue Noqueado el último turno del rival.
    put(atkKey("PFL-LA-66", "Vengeful Fang"), Effect(attackDamage = listOf(
        DamageTerm(60), DamageTerm(160, DamageCondition.IfOwnKoLastOppTurn))))
    // Lively Flower (60+): +120 si este Pokémon fue curado este turno.
    put(atkKey("PFL-LA-3", "Lively Flower"), Effect(attackDamage = listOf(
        DamageTerm(60), DamageTerm(120, DamageCondition.IfSelfHealedThisTurn))))
    // Wreck (120+): +120 si hay un Estadio en juego; además lo descarta.
    put(atkKey("PFL-LA-25", "Wreck"), Effect(
        attackDamage = listOf(DamageTerm(120), DamageTerm(120, DamageCondition.IfStadiumInPlay)),
        ops = listOf(EffectOp.DiscardStadium)))

    // ---- Daño "+X por cada Energía Fuego unida a este Pokémon" ----
    put(atkKey("PFL-LA-15", "Blaze Ball"), Effect(
        attackDamage = listOf(DamageTerm(10)),
        ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.ENERGY_ATTACHED, Target.SELF, 20, EnergyType.FIRE)))))
    put(atkKey("PFL-LA-16", "Blaze Ball"), Effect(
        attackDamage = listOf(DamageTerm(40)),
        ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.ENERGY_ATTACHED, Target.SELF, 40, EnergyType.FIRE)))))

    // ---- Descartar hasta N Energía de este Pokémon; X por cada una ----
    // Garland Ray (120×): descarta hasta 2 Energía de este Pokémon, 120 por cada una.
    put(atkKey("PFL-LA-41", "Garland Ray"), Effect(ops = listOf(EffectOp.DiscardSelfEnergyForDamage(2, 120))))
}
