package com.mineralord.tcg.engine.model

/**
 * Efectos de las cartas de las barajas preconstruidas "Academia de Combate 2024"
 * (Pikachu ex / Armarouge ex / Darkrai ex) — printings de sv1/sv2/sv3/sv4/sv8/svp/me1.
 * Semilla original validada por la web retirada. AISLADO del set 151.
 * Registrado por [EffectsDb.registry] vía [registerAcademiaDecks].
 */
internal fun MutableMap<EffectId, Effect>.registerAcademiaDecks() {
    // Milotic ex — Hypno Splash: rival Dormido.
    put(atkKey("sv8-42", "Hypno Splash"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.ASLEEP)))))

    // Scovillain ex — Spicy Rage: +70 por cada contador de daño en sí mismo.
    put(atkKey("sv8-37", "Spicy Rage"),
        Effect(ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.DAMAGE_COUNTERS, Target.SELF, 70)))))

    // Black Kyurem ex — Black Frost: 30 de retroceso a sí mismo.
    put(atkKey("sv8-48", "Black Frost"),
        Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(30)))))

    // Hydreigon ex — Obsidian: 130 a 2 Pokémon de la Banca rival.
    put(atkKey("sv8-119", "Obsidian"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_BENCH, 2, prompt("Elige 2 Pokémon de la Banca rival (130 de daño)", "Choose 2 Benched Pokémon (130 damage)"), optional = true),
            EffectOp.Damage(Target.CHOSEN, Amount.Fixed(130)),
        )))


    // ---- Baraja "Pikachu ex Academia de Combate 2024" ----
    // Pikachu ex — Thunderbolt (120): DAÑO PURO, sin efecto (la carta real trae
    // texto vacío). El coste de energía es un REQUISITO para atacar, NO se
    // descarta al usar el ataque. (Antes, por error, se registraba
    // DiscardEnergy(SELF, MAX) y "gastaba" las 3 energías: retirado para ser fiel.)
    // Rotom — Linear Attack: 20 a 1 Pokémon rival elegido (sin debilidad/resistencia en banca).
    put(atkKey("sv1-69", "Linear Attack"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_ALL, 1, prompt("Elige un Pokémon rival (20 de daño)", "Choose an opposing Pokémon (20 damage)")),
            EffectOp.Damage(Target.CHOSEN, Amount.Fixed(20)),
        )))

    // Wattrel — Collect: roba 1 carta.
    put(atkKey("sv1-77", "Collect"),
        Effect(ops = listOf(EffectOp.DrawCards(1))))

    // Kilowattrel — Jet Wing (150): en tu próximo turno, este Pokémon no puede atacar.
    put(atkKey("sv2-82", "Jet Wing"),
        Effect(ops = listOf(EffectOp.NoAttackNextTurn)))


    // ---- Baraja "Armarouge ex Academia de Combate 2024" ----
    // Armarouge ex — Armor Cannon (200): descarta 1 Energía Fuego de sí mismo.
    put(atkKey("svp-105", "Armor Cannon"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 1))))

    // Houndoom — Fire Blast (150): descarta 1 energía de sí mismo.
    put(atkKey("sv1-34", "Fire Blast"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 1))))

    // Larvesta — Take Down (40): 10 de daño a sí mismo.
    put(atkKey("sv3-40", "Take Down"),
        Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(10)))))

    // Skeledirge — Blazing Shout (190): 30 de daño a sí mismo.
    put(atkKey("sv1-38", "Blazing Shout"),
        Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(30)))))

    // Volcarona — Flame Cloak (30): une 1 Energía Fuego Básica del descarte a sí mismo.
    put(atkKey("sv3-41", "Flame Cloak"),
        Effect(ops = listOf(EffectOp.AttachEnergyFromDiscard(1, EnergyType.FIRE, Target.SELF))))

    // Skeledirge — Passionate Singing (50): une hasta 2 Energía Básica del descarte a tus Pokémon.
    put(atkKey("sv1-38", "Passionate Singing"),
        Effect(ops = listOf(EffectOp.AttachEnergyFromDiscard(2, null, Target.OWN_ALL))))

    // Torkoal — Concentrated Fire (80×): moneda por cada Energía Fuego; 80 por cara.
    put(atkKey("sv1-35", "Concentrated Fire"),
        Effect(ops = listOf(EffectOp.CoinsPerEnergyDamage(EnergyType.FIRE, 80))))

    // Mela (Partidario) — SOLO si te noquearon el turno pasado: une 1 Energía Fuego
    // Básica del descarte a 1 de tus Pokémon y roba hasta tener 6 cartas.
    put(EffectId("sv4-167"), Effect(
        ops = listOf(
            EffectOp.AttachEnergyFromDiscard(1, EnergyType.FIRE, Target.OWN_ALL, thenDrawUpTo = 6),
        ),
        requiresOwnKoLastTurn = true,
    ))


    // ---- Baraja "Darkrai ex Academia de Combate 2024" ----
    // Seviper — Cross-Cut (50+): +50 si el Activo rival es Pokémon de Evolución.
    put(atkKey("sv2-137", "Cross-Cut"), Effect(attackDamage = listOf(
        DamageTerm(50), DamageTerm(50, DamageCondition.IfDefenderEvolved))))

    // Yveltal — Cross-Cut (30+): +60 si el Activo rival es Pokémon de Evolución.
    put(atkKey("sv4-118", "Cross-Cut"), Effect(attackDamage = listOf(
        DamageTerm(30), DamageTerm(60, DamageCondition.IfDefenderEvolved))))

    // Yveltal — Dark Edge (120): descarta 1 energía de sí mismo.
    put(atkKey("sv4-118", "Dark Edge"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 1))))

    // Cyclizar — Touring: roba 2 cartas.
    put(atkKey("sv1-164", "Touring"),
        Effect(ops = listOf(EffectOp.DrawCards(2))))

    // Órdenes de Jefe (Ghetsis) — sube al Activo rival 1 Pokémon de su Banca.
    put(EffectId("sv2-172"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OPP_BENCH, 1, prompt(
            "Elige un Pokémon de la Banca rival para subirlo a su Activo",
            "Choose a Benched Pokémon of your opponent to switch to the Active Spot")),
        EffectOp.SwapOppActiveWithChosen)))

    put(abiKey("sv8-54", "Solid Body"), SOLID)

    put(abiKey("sv8-201", "Solid Body"), SOLID)

    // Shiinotic — Calming Light (varios artes).
    put(abiKey("sv8-9", "Calming Light"), CALMING)

    put(abiKey("sv8-194", "Calming Light"), CALMING)


    // ============================ ENTRENADORES (listos; corren cuando exista su intent) ============================

    // Nemona — roba 3.
    put(EffectId("sv1-180"), Effect(ops = listOf(EffectOp.DrawCards(3))))

    // Nido Ball — busca 1 Básico a la Banca.
    put(EffectId("sv1-181"), Effect(ops = listOf(
        EffectOp.SearchDeck(CardFilter(isBasic = true), Zone.BENCH, 1))))

    // Super Ball — busca 1 Pokémon a la mano.
    put(EffectId("sv2-183"), Effect(ops = listOf(
        EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON), Zone.HAND, 1))))

    // Bola Ocaso — busca 1 Pokémon a la mano.
    put(EffectId("sv8-175"), Effect(ops = listOf(
        EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON), Zone.HAND, 1))))

    // Poción — cura 30 a 1 de los tuyos (solo si tiene daño; si no, no se juega).
    put(EffectId("sv1-188"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OWN_ALL, 1, prompt("Elige un Pokémon para curarle 30", "Choose a Pokémon to heal 30"), onlyDamaged = true),
        EffectOp.Heal(Target.CHOSEN, Amount.Fixed(30)))))

    // Cinio (Jacq) — busca hasta 2 Pokémon de Evolución a la mano.
    put(EffectId("sv1-175"), Effect(ops = listOf(
        EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON, isBasic = false), Zone.HAND, 2))))

    // Cambio (Switch) — sv1-194 (SV base, baraja Pikachu). Mismo efecto `switch`
    // compartido con sv3pt5-206 (set 151); definido en EffectKeys.kt.
    put(EffectId("sv1-194"), switch)

    // Joven (Youngster) — baraja tu mano en el mazo y roba 5.
    put(EffectId("sv1-198"), Effect(ops = listOf(
        EffectOp.ShuffleHandIntoDeck, EffectOp.DrawCards(5))))

    // Excursionista (Picnicker) — lanza una moneda: cara robas 4, cruz robas 2.
    put(EffectId("svp-114"), Effect(ops = listOf(
        EffectOp.CoinFlipDraw(4, 2))))

    // Generador Eléctrico — mira el top 5, une hasta 2 Energía Rayo Básica que
    // encuentres a tus Pokémon Rayo de Banca; baraja el resto.
    put(EffectId("sv1-170"), Effect(ops = listOf(
        EffectOp.RevealAttachEnergy(
            lookAt = 5, maxAttach = 2,
            energyType = EnergyType.LIGHTNING, benchType = EnergyType.LIGHTNING))))


    // TODO(fase: extender DSL) — sin registrar porque usan ops no modeladas:
    //  - cambiarActivo:        sv3pt5-206 (Cambio), me1-114 (Órdenes de Jefes)
    //  - barajarManoEnMazo:    sv1-198 (Joven), sv8-173 (Drasna)
    //  - buscarDescarte:       sv8-251 (Camilla Nocturna)
    //  - mirarTopN:            sv3pt5-156 (Transferencia de Bill)
    //  - daño condicional:     sv3pt5-6 Brave Wing (tieneDanio)
    //  - heal con filtro tipo: sv8-167 (Clemont), sv8-172 (Elixir de Dragón)
    //  - filtros no modelados: sv8-170 (Cyrano, premiosMin), sv8-189 (Tera Orbe, tera)
    //  - estadios "a todos":   sv8-180 (Estadio Animado), sv8-177 (Montaña Gravedad)
}
