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
}
