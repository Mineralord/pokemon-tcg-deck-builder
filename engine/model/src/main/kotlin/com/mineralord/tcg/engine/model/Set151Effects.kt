package com.mineralord.tcg.engine.model

/**
 * Efectos de la expansión **151** (`sv3pt5`) de la serie Escarlata y Púrpura.
 * AISLADO por expansión: al añadir futuros sets NO se toca este archivo.
 * Registrado por [EffectsDb.registry] vía [registerSet151]. Las claves y las
 * definiciones compartidas viven en `EffectKeys.kt`.
 */
internal fun MutableMap<EffectId, Effect>.registerSet151() {

    // ============================ ATAQUES (ejecutables al atacar) ============================

    // Venusaur ex — Dangerous Toxwhip: rival Confundido + Envenenado.
    put(atkKey("sv3pt5-3", "Dangerous Toxwhip"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.CONFUSED, Status.POISONED)))))

    // Charizard ex — Explosive Vortex: descarta 3 energías de sí mismo.
    // (Brave Wing usa daño condicional `tieneDanio`, no modelado → sin registrar.)
    put(atkKey("sv3pt5-6", "Explosive Vortex"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 3))))

    // Ninetales ex — Heat Wave: rival Quemado.
    put(atkKey("sv3pt5-38", "Heat Wave"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.BURNED)))))

    // Alakazam ex — Mind Jack: +30 por cada Pokémon en la Banca rival.
    put(atkKey("sv3pt5-65", "Mind Jack"),
        Effect(ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.BENCH_COUNT, Target.OPP_BENCH, 30)))))

    // Kangaskhan ex — Triple Draw: roba 3.
    put(atkKey("sv3pt5-115", "Triple Draw"),
        Effect(ops = listOf(EffectOp.DrawCards(3))))

    // Jynx ex — Icy Wind: rival Dormido.
    put(atkKey("sv3pt5-124", "Icy Wind"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.ASLEEP)))))

    // Zapdos ex — Multishot Lightning: 90 a 1 Pokémon de la Banca rival.
    put(atkKey("sv3pt5-145", "Multishot Lightning"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_BENCH, 1, prompt("Elige un Pokémon de la Banca rival (90 de daño)", "Choose a Benched Pokémon (90 damage)"), optional = true),
            EffectOp.Damage(Target.CHOSEN, Amount.Fixed(90)),
        )))


    // ======================= SET 151 (sv3pt5) — Fase 1 de cobertura =======================
    // Lote de ataques que encajan en el catálogo de ops (estados, robo, descarte de
    // energía propia, retroceso, daño por moneda, descarte de mano rival y daño
    // condicional "X+"). Textos fieles a la carta en español.

    // -- Estados al Activo rival (incondicionales) --
    put(atkKey("sv3pt5-29", "Poison Horn"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.POISONED)))))

    put(atkKey("sv3pt5-34", "Venomous Impact"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.POISONED)))))

    put(atkKey("sv3pt5-73", "Poisonous Whip"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.POISONED)))))

    put(atkKey("sv3pt5-46", "Spore Ball"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.ASLEEP)))))

    put(atkKey("sv3pt5-78", "Singe"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.BURNED)))))

    put(atkKey("sv3pt5-109", "Suspicious Gas"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.CONFUSED)))))

    // Perplexing Powder: registramos el Confundido (el bloqueo de Objetos aún no se modela).
    put(atkKey("sv3pt5-49", "Perplexing Powder"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.CONFUSED)))))

    // Rant and Rave: el propio atacante queda Confundido.
    put(atkKey("sv3pt5-57", "Rant and Rave"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.SELF, listOf(Status.CONFUSED)))))

    // Artes alternativos de cartas del set base ya cubiertas.
    put(atkKey("sv3pt5-182", "Dangerous Toxwhip"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.CONFUSED, Status.POISONED)))))

    put(atkKey("sv3pt5-186", "Heat Wave"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.BURNED)))))

    put(atkKey("sv3pt5-191", "Icy Wind"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.ASLEEP)))))


    // -- Estado por moneda: "Lanza 1 moneda. Si sale cara, …" --
    put(atkKey("sv3pt5-37", "Super Singe"),
        Effect(ops = listOf(EffectOp.CoinFlipStatus(Target.OPP_ACTIVE, listOf(Status.BURNED)))))

    put(atkKey("sv3pt5-60", "Bubble"),
        Effect(ops = listOf(EffectOp.CoinFlipStatus(Target.OPP_ACTIVE, listOf(Status.PARALYZED)))))

    put(atkKey("sv3pt5-62", "Bubble Beam"),
        Effect(ops = listOf(EffectOp.CoinFlipStatus(Target.OPP_ACTIVE, listOf(Status.PARALYZED)))))


    // -- Daño extra por moneda: "Si sale cara, este ataque hace X puntos de daño más" --
    put(atkKey("sv3pt5-61", "Frog Hop"),
        Effect(ops = listOf(EffectOp.CoinFlipDamage(flips = 1, damagePerHeads = 60))))

    put(atkKey("sv3pt5-62", "Heroic Punch"),
        Effect(ops = listOf(EffectOp.CoinFlipDamage(flips = 1, damagePerHeads = 150))))

    put(atkKey("sv3pt5-100", "Tumbling Attack"),
        Effect(ops = listOf(EffectOp.CoinFlipDamage(flips = 1, damagePerHeads = 20))))


    // -- Robar cartas --
    put(atkKey("sv3pt5-83", "Package Deal"),
        Effect(ops = listOf(EffectOp.DrawCards(2))))

    put(atkKey("sv3pt5-190", "Triple Draw"),
        Effect(ops = listOf(EffectOp.DrawCards(3))))


    // -- Descartar Energía del propio Pokémon --
    put(atkKey("sv3pt5-5", "Fire Blast"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 1))))

    put(atkKey("sv3pt5-59", "Dynamite Fang"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 2))))

    put(atkKey("sv3pt5-150", "Psyslash"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 2))))

    put(atkKey("sv3pt5-183", "Explosive Vortex"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 3))))

    // Blazing Flight: descarta 2 Energía {R} y pega 120 a 1 Pokémon de la Banca rival.
    put(atkKey("sv3pt5-146", "Blazing Flight"),
        Effect(ops = listOf(
            EffectOp.DiscardEnergy(Target.SELF, 2),
            EffectOp.ChooseTarget(Target.OPP_BENCH, 1, prompt("Elige un Pokémon de la Banca rival (120 de daño)", "Choose a Benched Pokémon (120 damage)"), optional = true),
            EffectOp.Damage(Target.CHOSEN, Amount.Fixed(120)),
        )))


    // -- Retroceso (auto-daño) --
    put(atkKey("sv3pt5-26", "Thunder"),
        Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(50)))))

    put(atkKey("sv3pt5-81", "Big Explosion"),
        Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(60)))))

    put(atkKey("sv3pt5-84", "Reckless Charge"),
        Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(10)))))

    put(atkKey("sv3pt5-143", "Thudding Press"),
        Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(30)))))


    // -- El rival descarta cartas de su mano --
    put(atkKey("sv3pt5-24", "Menacing Fangs"),
        Effect(ops = listOf(EffectOp.OpponentDiscardsHand(2))))


    // -- Daño condicional "X+" (pasa por Debilidad/Resistencia una sola vez) --
    // Leaf Munch (10+): +30 si el Activo rival es tipo Planta.
    put(atkKey("sv3pt5-10", "Leaf Munch"), Effect(attackDamage = listOf(
        DamageTerm(10), DamageTerm(30, DamageCondition.IfDefenderType(EnergyType.GRASS)))))

    // Spike Rend (80+): +100 si el Activo rival ya tiene daño.
    put(atkKey("sv3pt5-28", "Spike Rend"), Effect(attackDamage = listOf(
        DamageTerm(80), DamageTerm(100, DamageCondition.IfDefenderHasDamage))))

    // Charizard ex — Brave Wing (60+): +100 si este Pokémon ya tiene daño.
    put(atkKey("sv3pt5-6", "Brave Wing"), Effect(attackDamage = listOf(
        DamageTerm(60), DamageTerm(100, DamageCondition.IfSelfHasDamage))))


    // ======================= SET 151 (sv3pt5) — Fase 6 de cobertura =======================
    // Entrenadores del set que encajan sin mecánicas nuevas de una sola carta.

    // Carisma de Giovanni (Partidario; 3 printings): devuelve 1 Energía del Activo rival a
    // su mano y, si lo haces, une 1 Energía de tu mano a tu Activo.
    put(EffectId("sv3pt5-161"), Effect(ops = listOf(EffectOp.GiovanniCharisma)))

    put(EffectId("sv3pt5-197"), Effect(ops = listOf(EffectOp.GiovanniCharisma)))

    put(EffectId("sv3pt5-204"), Effect(ops = listOf(EffectOp.GiovanniCharisma)))

    // Ayuda de Dalia (arte alternativo del ya registrado sv3pt5-158): roba 2.
    put(EffectId("sv3pt5-195"), Effect(ops = listOf(EffectOp.DrawCards(2))))

    // Transferencia de Bill (Bill's Transfer, Partidario; 2 printings): mira las 8 primeras
    // cartas del mazo, enseña cualquier cantidad de Pokémon y ponlos en tu mano; baraja el resto.
    // SearchDeck con fromTop = 8 (candidatos = Pokémon entre las 8 de arriba; count = 8 = "cualquier
    // cantidad"; el resto del mazo se baraja como en toda búsqueda).
    run {
        val billsTransfer = Effect(ops = listOf(
            EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON), Zone.HAND, 8, fromTop = 8)))
        put(EffectId("sv3pt5-156"), billsTransfer)
        put(EffectId("sv3pt5-194"), billsTransfer)
    }

    // Pegatinas de Energía (Energy Sticker, Objeto): lanza 1 moneda; si cara, une 1 Energía
    // Básica de tu descarte a uno de tus Pokémon en Banca. AttachEnergyFromDiscard con
    // coinFlip=true (la moneda se resuelve al confirmar la elección de energía+Pokémon).
    put(EffectId("sv3pt5-159"), Effect(ops = listOf(
        EffectOp.AttachEnergyFromDiscard(count = 1, energyType = null, target = Target.OWN_BENCH, coinFlip = true))))


    // ======================= SET 151 (sv3pt5) — Fase 5 de cobertura =======================
    // Daño a la Banca rival (dirigido y "a cada uno") + daño condicional contra ex/V.

    // -- Daño a UN Pokémon de la Banca rival (elegido) --
    // Bone Throw (30): +30 a 1 Pokémon de la Banca rival.
    put(atkKey("sv3pt5-105", "Bone Throw"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_BENCH, 1, prompt("Elige un Pokémon de la Banca rival (30 de daño)", "Choose a Benched Pokémon (30 damage)"), optional = true),
            EffectOp.Damage(Target.CHOSEN, Amount.Fixed(30)),
        )))


    // -- Daño a CADA Pokémon de la Banca rival (sin Debilidad/Resistencia) --
    put(atkKey("sv3pt5-110", "Spinning Fumes"),
        Effect(ops = listOf(EffectOp.Damage(Target.OPP_BENCH, Amount.Fixed(10)))))

    put(atkKey("sv3pt5-144", "Blizzard"),
        Effect(ops = listOf(EffectOp.Damage(Target.OPP_BENCH, Amount.Fixed(10)))))


    // -- Daño condicional "+90 si el Activo rival es Pokémon ex o V" (Full Fighting) --
    put(atkKey("sv3pt5-134", "Fighting Whirlpool"), Effect(attackDamage = listOf(
        DamageTerm(90), DamageTerm(90, DamageCondition.IfDefenderExOrV))))

    put(atkKey("sv3pt5-135", "Fighting Lightning"), Effect(attackDamage = listOf(
        DamageTerm(90), DamageTerm(90, DamageCondition.IfDefenderExOrV))))

    put(atkKey("sv3pt5-136", "Fighting Blaze"), Effect(attackDamage = listOf(
        DamageTerm(90), DamageTerm(90, DamageCondition.IfDefenderExOrV))))


    // ======================= SET 151 (sv3pt5) — Fase 4 de cobertura =======================
    // Buscar Energía y unirla a sí mismo, descarte de energía por tipo, y curación propia.

    // -- Buscar Energía Básica en el mazo y unirla a este Pokémon --
    put(atkKey("sv3pt5-25", "Charge"),
        Effect(ops = listOf(EffectOp.SearchEnergyAttachSelf(EnergyType.LIGHTNING, 1))))

    put(atkKey("sv3pt5-98", "Salt Water"),
        Effect(ops = listOf(EffectOp.SearchEnergyAttachSelf(EnergyType.WATER, 2, coinFlip = true))))


    // -- Descarte de Energía del rival POR TIPO --
    put(atkKey("sv3pt5-58", "Vaporize"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.OPP_ACTIVE, 1, energyType = EnergyType.WATER))))


    // -- Curación a sí mismo --
    put(atkKey("sv3pt5-1", "Leech Seed"),
        Effect(ops = listOf(EffectOp.Heal(Target.SELF, Amount.Fixed(20)))))

    put(atkKey("sv3pt5-134", "Spiral Drain"),
        Effect(ops = listOf(EffectOp.Heal(Target.SELF, Amount.Fixed(30)))))

    put(atkKey("sv3pt5-141", "Draining Blade"),
        Effect(ops = listOf(EffectOp.Heal(Target.SELF, Amount.Fixed(30)))))

    // Sea Bathing: cura 30 y se recupera de todas las Condiciones Especiales.
    put(atkKey("sv3pt5-79", "Sea Bathing"),
        Effect(ops = listOf(
            EffectOp.Heal(Target.SELF, Amount.Fixed(30)),
            EffectOp.RemoveStatus(Target.SELF),
        )))


    // ======================= SET 151 (sv3pt5) — Fase 3 de cobertura =======================
    // Búsquedas en mazo (a Banca / a la mano) y descarte de Energía del Activo rival.

    // -- Buscar Básicos y ponerlos en la Banca --
    put(atkKey("sv3pt5-16", "Call for Family"),
        Effect(ops = listOf(EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON, isBasic = true), Zone.BENCH, 2))))

    put(atkKey("sv3pt5-128", "Gather the Crew"),
        Effect(ops = listOf(EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON, isBasic = true), Zone.BENCH, 1))))


    // -- Buscar cartas y ponerlas en la mano --
    put(atkKey("sv3pt5-22", "Beak Catch"),
        Effect(ops = listOf(EffectOp.SearchDeck(CardFilter(), Zone.HAND, 3))))

    put(atkKey("sv3pt5-30", "Fetch Family"),
        Effect(ops = listOf(EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON), Zone.HAND, 3))))

    put(atkKey("sv3pt5-131", "Hop on My Back"),
        Effect(ops = listOf(EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON), Zone.HAND, 2))))


    // -- Descarte de Energía del Activo rival --
    // Hyper Beam (200): descarta 1 Energía (cualquiera) del Activo rival.
    put(atkKey("sv3pt5-130", "Hyper Beam"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.OPP_ACTIVE, 1))))

    // Acid Spray (30) / Destructive Flame (30): moneda → si cara, descarta 1 Energía del Activo rival.
    put(atkKey("sv3pt5-23", "Acid Spray"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.OPP_ACTIVE, 1, coinFlip = true))))

    put(atkKey("sv3pt5-136", "Destructive Flame"),
        Effect(ops = listOf(EffectOp.DiscardEnergy(Target.OPP_ACTIVE, 1, coinFlip = true))))


    // ======================= SET 151 (sv3pt5) — Fase 2 de cobertura =======================
    // Estado temporal en el defensor: prevención de daño por moneda y "no puede retirarse".

    // -- Prevención de daño por moneda: "Si sale cara, durante el próximo turno del rival,
    //    se evita todo el daño a este Pokémon por ataques." (self) --
    put(atkKey("sv3pt5-7", "Withdraw"),
        Effect(ops = listOf(EffectOp.PreventDamageNextTurn(Target.SELF, coinFlip = true))))

    put(atkKey("sv3pt5-11", "Defensive Posture"),
        Effect(ops = listOf(EffectOp.PreventDamageNextTurn(Target.SELF, coinFlip = true))))

    // Swim Freely (10): además "y efectos de ataques" (los efectos aún no se bloquean; el daño sí).
    put(atkKey("sv3pt5-119", "Swim Freely"),
        Effect(ops = listOf(EffectOp.PreventDamageNextTurn(Target.SELF, coinFlip = true))))


    // -- "Durante el próximo turno de tu rival, el Pokémon Defensor no puede retirarse." --
    put(atkKey("sv3pt5-24", "Bind Down"),
        Effect(ops = listOf(EffectOp.DefenderCannotRetreatNextTurn)))

    put(atkKey("sv3pt5-28", "Rumble"),
        Effect(ops = listOf(EffectOp.DefenderCannotRetreatNextTurn)))


    // ============================ HABILIDADES (listas; corren cuando exista su intent) ============================

    // Venusaur ex — Tranquil Flower (incl. artes alternativos).
    put(abiKey("sv3pt5-3", "Tranquil Flower"), TRANQUIL)

    put(abiKey("sv3pt5-182", "Tranquil Flower"), TRANQUIL)

    put(abiKey("sv3pt5-198", "Tranquil Flower"), TRANQUIL)

    // Dodrio — Zooming Draw: 1/turno, 1 contador de daño a sí mismo y roba 1.
    put(abiKey("sv3pt5-85", "Zooming Draw"),
        Effect(ops = listOf(EffectOp.Damage(Target.SELF, Amount.Fixed(10)), EffectOp.DrawCards(1)), oncePerTurn = true))

    // Starmie — Mysterious Comet: 1/turno, 20 a 1 Pokémon rival elegido.
    put(abiKey("sv3pt5-121", "Mysterious Comet"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_ALL, 1, prompt("Elige un Pokémon rival (20 de daño)", "Choose an opposing Pokémon (20 damage)")),
            EffectOp.Damage(Target.CHOSEN, Amount.Fixed(20)),
        ), oncePerTurn = true))

    // Solid Shell / Solid Body — pasivo -30 (varios artes).
    put(abiKey("sv3pt5-9", "Solid Shell"), SOLID)

    put(abiKey("sv3pt5-184", "Solid Shell"), SOLID)

    put(abiKey("sv3pt5-200", "Solid Shell"), SOLID)

    // Mew ex — Restart (varios artes).
    put(abiKey("sv3pt5-151", "Restart"), RESTART)

    put(abiKey("sv3pt5-193", "Restart"), RESTART)

    put(abiKey("sv3pt5-205", "Restart"), RESTART)


    // ------------------- SET 151 (sv3pt5) — Fase 7: HABILIDADES -------------------
    // Persian — Llamar al Rocket: 1/turno, busca 1 Carisma de Giovanni a la mano.
    put(abiKey("sv3pt5-53", "Rocket Call"),
        Effect(ops = listOf(EffectOp.SearchDeck(CardFilter(nameContains = "Giovanni"), Zone.HAND, 1)), oncePerTurn = true))

    // Flotación (pasivo condicional): coste de retirada 0 si tiene Energía de su tipo unida.
    put(abiKey("sv3pt5-144", "Ice Float"),
        Effect(passives = listOf(PassiveModifier(ModKind.RETREAT_COST, 0, Target.SELF, requiresEnergyType = EnergyType.WATER))))

    put(abiKey("sv3pt5-145", "Voltaic Float"),
        Effect(passives = listOf(PassiveModifier(ModKind.RETREAT_COST, 0, Target.SELF, requiresEnergyType = EnergyType.LIGHTNING))))

    put(abiKey("sv3pt5-146", "Flare Float"),
        Effect(passives = listOf(PassiveModifier(ModKind.RETREAT_COST, 0, Target.SELF, requiresEnergyType = EnergyType.FIRE))))

    // Dragonite — Travesía Propulsión: tus Pokémon en juego no tienen coste de retirada.
    put(abiKey("sv3pt5-149", "Jet Cruise"),
        Effect(passives = listOf(PassiveModifier(ModKind.RETREAT_COST, 0, Target.OWN_ALL))))

    // Omastar — Tentáculos Primordiales: mientras esté Activo, el Activo rival no puede retirarse.
    put(abiKey("sv3pt5-139", "Primordial Tentacles"),
        Effect(passives = listOf(PassiveModifier(ModKind.NO_RETREAT, 0, Target.OPP_ACTIVE))))

    // ------------------- SET 151 (sv3pt5) — Fase 30: pasivos propios condicionales (HP / retirada) -------------------
    // El motor aplica estos pasivos de habilidad en effectiveMaxHp / effectiveRetreatCost.

    // Wigglytuff ex — Cuerpo Expansivo (Expanding Body): +100 PS si tiene alguna Energía
    // Especial unida. (sv3pt5-40 y su reimpresión -187.)
    put(abiKey("sv3pt5-40", "Expanding Body"),
        Effect(passives = listOf(PassiveModifier(ModKind.EXTRA_HP, 100, Target.SELF, requiresSpecialEnergy = true))))
    put(abiKey("sv3pt5-187", "Expanding Body"),
        Effect(passives = listOf(PassiveModifier(ModKind.EXTRA_HP, 100, Target.SELF, requiresSpecialEnergy = true))))

    // Zapdos ex — Flotación Voltaica (Voltaic Float): coste de retirada 0 si tiene Energía {L}
    // unida. (sv3pt5-192 y su reimpresión -202.)
    put(abiKey("sv3pt5-192", "Voltaic Float"),
        Effect(passives = listOf(PassiveModifier(ModKind.RETREAT_COST, 0, Target.SELF, requiresEnergyType = EnergyType.LIGHTNING))))
    put(abiKey("sv3pt5-202", "Voltaic Float"),
        Effect(passives = listOf(PassiveModifier(ModKind.RETREAT_COST, 0, Target.SELF, requiresEnergyType = EnergyType.LIGHTNING))))

    // ------------------- SET 151 (sv3pt5) — Fase 31: descartar Estadio + prevención condicional -------------------

    // Charmander — Destrucción Abrasadora (Blazing Destruction): descarta 1 Estadio en juego
    // (sin daño base). (sv3pt5-4 y su reimpresión -168.)
    put(atkKey("sv3pt5-4", "Blazing Destruction"), Effect(ops = listOf(EffectOp.DiscardStadium)))
    put(atkKey("sv3pt5-168", "Blazing Destruction"), Effect(ops = listOf(EffectOp.DiscardStadium)))

    // Nidoqueen — Prensa Real (Queen Press): 90; durante el próximo turno del rival se evita
    // todo el daño a este Pokémon por ataques de Pokémon Básicos.
    put(atkKey("sv3pt5-31", "Queen Press"),
        Effect(ops = listOf(EffectOp.PreventDamageNextTurn(Target.SELF, onlyFromBasic = true))))

    // ------------------- SET 151 (sv3pt5) — Fase 32: "contragolpe" (habilidad al ser dañado/KO) -------------------
    // Se disparan AUTOMÁTICAMENTE en GameEngine.attack (applyDefenderRetaliation), NO vía UseAbility.
    // OJO: las ops corren con actingSide = DEFENSOR → Target.OPP_ACTIVE apunta al ATACANTE.

    // Hitmonchan — Contragolpe (Counterattack): si es Activo y resulta dañado por un ataque
    // rival (incluso si queda KO), pon 3 contadores (=30 daño crudo) en el Atacante.
    put(abiKey("sv3pt5-107", "Counterattack"), Effect(
        triggerOnActiveDamaged = true,
        ops = listOf(EffectOp.Damage(Target.OPP_ACTIVE, Amount.Fixed(30)))))

    // Weezing — A Pasarlo Bomba (Let's Have a Blast): si es Activo y queda KO por un ataque
    // rival, lanza 1 moneda; con cara, el Atacante queda Fuera de Combate.
    put(abiKey("sv3pt5-110", "Let's Have a Blast"), Effect(
        triggerOnActiveKO = true,
        ops = listOf(EffectOp.CoinFlipKnockOutTarget(Target.OPP_ACTIVE))))

    // Mewtwo — Barrera Reflectante (Reflective Barrier): ATAQUE (base 20) que programa un
    // reflejo: durante el próximo turno del rival, si este Pokémon es dañado por un ataque
    // (incluso si queda KO), el Atacante recibe daño crudo = el daño infligido a Mewtwo.
    put(atkKey("sv3pt5-150", "Reflective Barrier"), Effect(
        ops = listOf(EffectOp.ScheduleReflectDamageNextTurn)))

    // Machamp — Agallas (Guts): si fuese a quedar KO por el daño de un ATAQUE, moneda; con
    // cara no queda KO y sus PS restantes pasan a 10. Lo resuelve GameEngine.handleKnockouts.
    put(abiKey("sv3pt5-68", "Guts"), Effect(survivesKoWithCoin = true))

    // Raichu — Toma de Tierra (Electrical Grounding): cuando uno de tus Pokémon queda KO por
    // el ataque del rival, mueve 1 Energía {L} del Noqueado a este Raichu (en la Banca).
    put(abiKey("sv3pt5-26", "Electrical Grounding"), Effect(
        pullsEnergyFromKoAllyType = EnergyType.LIGHTNING))

    // Mr. Mime — Barrera Mímica (Mimic Barrier) 122 y reprint 179: escudo pasivo — evita todo el
    // daño rival si él y el Activo rival tienen igual nº de Energías. Lo comprueba GameEngine.attack.
    put(abiKey("sv3pt5-122", "Mimic Barrier"), Effect(preventsDamageIfEnergyParity = true))
    put(abiKey("sv3pt5-179", "Mimic Barrier"), Effect(preventsDamageIfEnergyParity = true))

    // ------------------- SET 151 (sv3pt5) — Fase 34: "el ataque no hace nada si…" + premio extra -----
    // Primeape — Golpe Rabioso (Raging Smash) 150: no hace nada si NO está Confundido.
    put(atkKey("sv3pt5-57", "Raging Smash"), Effect(noEffectUnlessSelfConfused = true))
    // Slowbro — Placaje Relajado (Laid-Back Tackle) 160: no hace nada si evolucionó este turno.
    put(atkKey("sv3pt5-80", "Laid-Back Tackle"), Effect(noEffectIfEvolvedThisTurn = true))
    // Clefable — Más Luna (More Moon) 50: coge 1 Premio más si este ataque noquea a un rival.
    put(atkKey("sv3pt5-36", "More Moon"), Effect(extraPrizeIfKo = true))

    // ------------------- SET 151 (sv3pt5) — Fase 35: efectos AL FINAL del turno -----
    // Victreebel — Ácido de Acción Lenta (Slow-Acting Acid) 71: 120 base (daño puro) +
    // al final del próximo turno del rival, pon 12 contadores (=120 daño) en el Defensor.
    put(atkKey("sv3pt5-71", "Slow-Acting Acid"),
        Effect(ops = listOf(EffectOp.ScheduleDelayedDamage(Target.OPP_ACTIVE, 120))))
    // Restos (Leftovers) 163 — Herramienta: al final de tu turno, si está en el Activo, cura 20.
    put(EffectId("sv3pt5-163"), Effect(healSelfEndOfTurnIfActive = 20))

    // ------------------- SET 151 (sv3pt5) — Fase 36: pasivos condicionados por aliado en juego -----
    // Nidoking — Rey Entusiasta (Enthusiastic King) 34: ataques gratis si tienes Nidoqueen en juego.
    put(abiKey("sv3pt5-34", "Enthusiastic King"), Effect(freeAttackIfAllyNamed = "Nidoqueen"))
    // Cubone — Ovación Ósea (Cheering Bone) 104: tus Marowak +30 mientras Cubone esté en tu Banca.
    put(abiKey("sv3pt5-104", "Cheering Bone"),
        Effect(boostAlliedAttackerNamed = "Marowak", boostAlliedAttackerAmount = 30))

    // ------------------- SET 151 (sv3pt5) — Fase 37: des-evolución del Activo rival -----
    // Aerodactyl — Rayo Involutivo (Devolution Ray) 142: 100 base (daño puro) + si el Activo
    // rival está evolucionado, lo involuciona (la carta de fase más alta vuelve a su mano).
    put(atkKey("sv3pt5-142", "Devolution Ray"),
        Effect(ops = listOf(EffectOp.DeEvolveDefender)))

    // ------------------- SET 151 (sv3pt5) — Fase 38: override de Debilidad -----
    // Porygon — Conversión 4 (Conversion 4) 137: elige un tipo; la Debilidad del Defensor pasa a
    // ser de ese tipo (misma cantidad) hasta que deje el Activo. Sin daño base.
    put(atkKey("sv3pt5-137", "Conversion 4"),
        Effect(ops = listOf(EffectOp.OverrideDefenderWeaknessType)))
    // Kabutops — Modo Ancestral (Ancient Way) 141: habilidad pasiva → la Debilidad del Activo
    // rival se aplica como ×4 cuando el lado de Kabutops ataca.
    put(abiKey("sv3pt5-141", "Ancient Way"),
        Effect(overridesDefenderWeaknessMultiplier = 4))


    // ------------------- SET 151 (sv3pt5) — Fase 8: BÚSQUEDAS / ROBO -------------------
    // (Las búsquedas a mano/Banca y el descarte de Energía del rival — Call for Family,
    //  Beak Catch, Fetch Family, Gather the Crew, Hop on My Back, Hyper Beam, Acid Spray,
    //  Destructive Flame — ya están en el bloque "Fase 3" de arriba.)
    // Clefairy — Invitación de Avistamiento Lunar: busca hasta 3 Clefairy a la Banca.
    put(atkKey("sv3pt5-35", "Moon-Viewing Invitation"),
        Effect(ops = listOf(EffectOp.SearchDeck(CardFilter(nameContains = "Clefairy"), Zone.BENCH, 3))))

    // Ponyta — Coleccionar: roba 1 carta.
    put(atkKey("sv3pt5-77", "Collect"),
        Effect(ops = listOf(EffectOp.DrawCards(1))))

    // -- Daño por N monedas fijas (base "X×" = Variable, todo el daño lo pone la op) --
    // Jigglypuff — Pisotonazo: 2 monedas, 20 por cara.
    put(atkKey("sv3pt5-39", "Stompy Stomp"),
        Effect(ops = listOf(EffectOp.CoinFlipDamage(2, 20))))

    // Goldeen — Triple Impacto: 3 monedas, 10 por cara.
    put(atkKey("sv3pt5-118", "Triple Strike"),
        Effect(ops = listOf(EffectOp.CoinFlipDamage(3, 10))))

    // Cubone — Doble Redoble: 2 monedas, 10 por cara.
    put(atkKey("sv3pt5-104", "Hit Twice"),
        Effect(ops = listOf(EffectOp.CoinFlipDamage(2, 10))))

    // Kabuto — Arañazo Doble: 2 monedas, 70 por cara.
    put(atkKey("sv3pt5-140", "Double Scratch"),
        Effect(ops = listOf(EffectOp.CoinFlipDamage(2, 70))))

    // -- Curación al propio Activo --
    // Ivysaur — Drenadoras (30): cura 20 a este Pokémon.
    put(atkKey("sv3pt5-2", "Leech Seed"),
        Effect(ops = listOf(EffectOp.Heal(Target.SELF, Amount.Fixed(20)))))

    // Slowpoke — Baño de Mar: cura 30 a este Pokémon y se recupera de todas las Condiciones.
    put(atkKey("sv3pt5-79", "Sea Bathing"),
        Effect(ops = listOf(EffectOp.Heal(Target.SELF, Amount.Fixed(30)), EffectOp.RemoveStatus(Target.SELF))))


    // ------------------- SET 151 (sv3pt5) — Fase 9: RECUPERAR DEL DESCARTE -------------------
    // Golduck — Rescate Acuático: hasta 4 Pokémon del descarte a la mano.
    put(atkKey("sv3pt5-55", "Aquatic Rescue"),
        Effect(ops = listOf(EffectOp.RecoverFromDiscard(CardFilter(supertype = Supertype.POKEMON), 4))))

    // Wartortle — Buceo Libre: hasta 3 Energía {W} Básica del descarte a la mano.
    put(atkKey("sv3pt5-8", "Free Diving"),
        Effect(ops = listOf(EffectOp.RecoverFromDiscard(CardFilter(supertype = Supertype.ENERGY, type = EnergyType.WATER), 3))))

    // Snorlax — Glotonería (habilidad, 1/turno): hasta 2 Restos del descarte a la mano.
    put(abiKey("sv3pt5-143", "Voraciousness"),
        Effect(ops = listOf(EffectOp.RecoverFromDiscard(CardFilter(nameContains = "Restos"), 2)), oncePerTurn = true))


    // ------------------- SET 151 (sv3pt5) — Fase 20: ignorar Debilidad/Resistencia + curar-al-atacar -------------------

    // Staryu — Meteoros (Swift): 30 de daño que ignora Debilidad, Resistencia y TODO efecto del Activo rival.
    put(atkKey("sv3pt5-120", "Swift"),
        Effect(ignoresWeakness = true, ignoresResistance = true, ignoresDefenderEffects = true))
    // Golem ex — Explosión Roca (Rock Blaster): 180; el daño NO se ve afectado por Resistencia.
    put(atkKey("sv3pt5-76", "Rock Blaster"), Effect(ignoresResistance = true))
    // Kabutops — Cuchilla Drenadora (Draining Blade): 100 + cúrale 30 de daño a sí mismo (reuso de Heal).
    put(atkKey("sv3pt5-141", "Draining Blade"), Effect(ops = listOf(EffectOp.Heal(Target.SELF, Amount.Fixed(30)))))

    // ------------------- SET 151 (sv3pt5) — Fase 27: moneda → busca Pokémon a la Banca -------------------

    // Parasect — Filamentos Dispersos (Spread Filaments): lanza 2 monedas; busca hasta
    // (nº de caras) Pokémon {G} en tu mazo y ponlos en tu Banca; luego baraja.
    put(atkKey("sv3pt5-47", "Spread Filaments"), Effect(ops = listOf(
        EffectOp.CoinFlipSearchToBench(
            flips = 2, filter = CardFilter(supertype = Supertype.POKEMON, type = EnergyType.GRASS)))))

    // ------------------- SET 151 (sv3pt5) — Fase 28: habilidades "al evolucionar desde la mano" (auto) -------------------
    // Se disparan AUTOMÁTICAMENTE en GameEngine.evolve (triggerOnEvolve), NO vía UseAbility.
    // Solo casos NO interactivos por ahora (los que requieren decisión —Gloom/Vileplume/
    // Haunter— quedan diferidos hasta montar la decisión al evolucionar).

    // Gyarados — Indomable (Untamed One): al evolucionar desde la mano, descarta las 5
    // primeras cartas de tu baraja (obligatorio).
    put(abiKey("sv3pt5-130", "Untamed One"), Effect(
        triggerOnEvolve = true,
        ops = listOf(EffectOp.DiscardTopDeck(5, own = true))))

    // Hypno — Toma Hipnosis (Here for Hypnosis): al evolucionar desde la mano, puedes dejar
    // Dormido al Activo rival (lo aplicamos siempre: es un "puedes" sin coste ni desventaja).
    put(abiKey("sv3pt5-97", "Here for Hypnosis"), Effect(
        triggerOnEvolve = true,
        ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.ASLEEP)))))

    // ------------------- SET 151 (sv3pt5) — Fase 29: habilidades INTERACTIVAS al evolucionar -------------------
    // Se disparan en GameEngine.evolve (triggerOnEvolve) y PAUSAN con una decisión de arrastre
    // (AttachFromRevealed) que UI/IA/netplay resuelven. Reusan RevealAttachEnergy generalizado:
    // energyType=null (cualquier Básica), includeActive=true ("a tus Pokémon" = Banca + Activo).

    // Gloom — Floración Parcial de Energía (Semi-Blooming Energy): al evolucionar desde la mano,
    // puedes mirar las 3 primeras cartas del mazo y unir cualquier Energía Básica de ellas a
    // tus Pokémon como quieras; el resto vuelve al mazo y se baraja.
    put(abiKey("sv3pt5-44", "Semi-Blooming Energy"), Effect(
        triggerOnEvolve = true,
        ops = listOf(EffectOp.RevealAttachEnergy(
            lookAt = 3, maxAttach = 3, energyType = null, benchType = null, includeActive = true))))

    // Vileplume — Floración Total de Energía (Fully Blooming Energy): idéntico pero mirando 8.
    put(abiKey("sv3pt5-45", "Fully Blooming Energy"), Effect(
        triggerOnEvolve = true,
        ops = listOf(EffectOp.RevealAttachEnergy(
            lookAt = 8, maxAttach = 8, energyType = null, benchType = null, includeActive = true))))

    // ------------------- SET 151 (sv3pt5) — Fase 26: descarte de la mano que escala el daño -------------------

    // Blastoise ex — Cañones Gemelos (Twin Cannons): descarta hasta 2 Energías {W} Básicas de tu
    // mano; 140 de daño por cada carta descartada (base 0 → todo el daño lo pone el efecto, CRUDO).
    put(atkKey("sv3pt5-9", "Twin Cannons"), Effect(ops = listOf(
        EffectOp.DiscardFromHandForDamage(
            CardFilter(supertype = Supertype.ENERGY, type = EnergyType.WATER), maxCount = 2, perCard = 140))))

    // ------------------- SET 151 (sv3pt5) — Fase 42: descarte de Herramientas propias que escala el daño -------------------

    // Electrode — Cadena Bum Bum (Bang Boom Chain): 20 base (pasa por Debilidad/Resistencia) y,
    // antes de infligir daño, puedes descartar cualquier cantidad de Herramientas de TUS Pokémon;
    // +40 CRUDO por cada una. Base por attackDamage; el bonus lo aplica el efecto tras la elección.
    put(atkKey("sv3pt5-101", "Bang Boom Chain"), Effect(
        attackDamage = listOf(DamageTerm(20)),
        ops = listOf(EffectOp.DiscardOwnToolsForDamage(perCard = 40))))

    // ------------------- SET 151 (sv3pt5) — Fase 25: "si tu mano está vacía" (daño + estado) -------------------

    // Beedrill — Aguijón Nadir (Nadir Needle): 30 base; si tu mano está vacía, +120 Y
    // el Activo rival pasa a estar Envenenado y Paralizado.
    put(atkKey("sv3pt5-15", "Nadir Needle"), Effect(
        attackDamage = listOf(DamageTerm(30), DamageTerm(120, DamageCondition.IfEmptyHand)),
        ops = listOf(EffectOp.ApplyStatusIfEmptyHand(
            Target.OPP_ACTIVE, listOf(Status.POISONED, Status.PARALYZED)))))

    // ------------------- SET 151 (sv3pt5) — Fase 24: Gust "el rival elige el nuevo Activo" -------------------

    // Butterfree — Remolino (Whirlwind): 60; mueve el Activo rival a su Banca (el rival elige el nuevo).
    put(atkKey("sv3pt5-12", "Whirlwind"), Effect(ops = listOf(EffectOp.GustDefenderChooseNewActive)))
    // Rhyhorn — Oprimir (Push Down): 20; mueve el Activo rival a su Banca (el rival elige el nuevo).
    put(atkKey("sv3pt5-111", "Push Down"), Effect(ops = listOf(EffectOp.GustDefenderChooseNewActive)))

    // ------------------- SET 151 (sv3pt5) — Fase 23: recargo de Coste de Retirada / de ataque al Defensor -------------------

    // Grimer — Presión Pegajosa (Gummy Press): 10; el próximo turno el Coste de Retirada del Defensor +1.
    put(atkKey("sv3pt5-88", "Gummy Press"), Effect(ops = listOf(EffectOp.BumpDefenderRetreatCostNextTurn(1))))
    // Muk — Prisión Viscosa (Sticky Jail): 30; el próximo turno los ataques del Defensor cuestan +1 Y su Retirada +1.
    put(atkKey("sv3pt5-89", "Sticky Jail"), Effect(ops = listOf(
        EffectOp.BumpDefenderAttackCostNextTurn(1), EffectOp.BumpDefenderRetreatCostNextTurn(1))))

    // ------------------- SET 151 (sv3pt5) — Fase 22: bonus de daño "tu próximo turno +X" -------------------

    // Golem ex — Giro Dinámico (Dynamic Roll): 50; tu próximo turno, los ataques de este Pokémon +120.
    put(atkKey("sv3pt5-76", "Dynamic Roll"), Effect(ops = listOf(EffectOp.SelfAttackBonusNextTurn(120))))
    // Hitmonchan — Puño Exaltado (Excited Punch): 60; tu próximo turno, Puño Exaltado hace +60 (único ataque de daño).
    put(atkKey("sv3pt5-107", "Excited Punch"), Effect(ops = listOf(EffectOp.SelfAttackBonusNextTurn(60))))

    // ------------------- SET 151 (sv3pt5) — Fase 21: moneda cara/cruz + búsqueda de tipos distintos -------------------

    // Mankey — Saña (Thrash): 20 base; cara → +20 al rival, cruz → 20 de rebote a sí mismo.
    put(atkKey("sv3pt5-56", "Thrash"), Effect(ops = listOf(EffectOp.CoinFlipDamageOrRecoil(20, 20))))
    // Eevee — Amigos Coloridos (Colorful Friends): busca hasta 3 Pokémon de tipos DISTINTOS → mano.
    put(atkKey("sv3pt5-133", "Colorful Friends"), Effect(ops = listOf(
        EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON), Zone.HAND, 3, distinctTypes = true))))

    // ------------------- SET 151 (sv3pt5) — Fase 19: daño por energía de tipo + KO por estado -------------------

    // Seaking — Cuerno Aqua (Aqua Horn): 60 base + 30 por cada Energía {W} unida a sí mismo.
    put(atkKey("sv3pt5-119", "Aqua Horn"), Effect(ops = listOf(
        EffectOp.ExtraDamage(Amount.PerCount(Counter.ENERGY_ATTACHED, Target.SELF, 30, energyType = EnergyType.WATER)))))
    // Jynx ex — Beso de Infarto (Heart-Stopping Kiss): si el Activo rival está Dormido, queda KO (2 printings).
    put(atkKey("sv3pt5-124", "Heart-Stopping Kiss"), Effect(ops = listOf(
        EffectOp.KoIfStatus(Target.OPP_ACTIVE, Status.ASLEEP))))
    put(atkKey("sv3pt5-191", "Heart-Stopping Kiss"), Effect(ops = listOf(
        EffectOp.KoIfStatus(Target.OPP_ACTIVE, Status.ASLEEP))))

    // ------------------- SET 151 (sv3pt5) — Fase 18: buscar Partidario/Objeto, energía a Banca, snipe -------------------

    // Jigglypuff — Liderazgo (Lead): busca 1 Partidario en tu mazo y ponlo en tu mano.
    put(atkKey("sv3pt5-39", "Lead"), Effect(ops = listOf(
        EffectOp.SearchDeck(CardFilter(trainerKind = TrainerCategory.SUPPORTER), Zone.HAND, 1))))

    // Magneton — Imán de Chatarra (Junk Magnet): hasta 2 Objetos del descarte a tu mano.
    put(atkKey("sv3pt5-82", "Junk Magnet"), Effect(ops = listOf(
        EffectOp.RecoverFromDiscard(CardFilter(trainerKind = TrainerCategory.ITEM), 2))))

    // Scyther — Tajo Útil (Helpful Slash): 20 base + une 1 Energía {G} Básica del descarte a un Pokémon de tu Banca.
    put(atkKey("sv3pt5-123", "Helpful Slash"), Effect(ops = listOf(
        EffectOp.AttachEnergyFromDiscard(1, EnergyType.GRASS, Target.OWN_BENCH))))

    // Jolteon — Ataque Lineal (Linear Attack): 30 a un Pokémon rival cualquiera (snipe, sin Debilidad/Resistencia).
    put(atkKey("sv3pt5-135", "Linear Attack"), Effect(ops = listOf(
        EffectOp.ChooseTarget(Target.OPP_ALL, 1, prompt("Elige un Pokémon rival (30 de daño)", "Choose an opposing Pokémon (30 damage)"), optional = true),
        EffectOp.Damage(Target.CHOSEN, Amount.Fixed(30)))))

    // Jolteon — Rayo Luchador (Fighting Lightning): 90 + 90 si el Activo rival es ex o V.
    put(atkKey("sv3pt5-135", "Fighting Lightning"), Effect(attackDamage = listOf(
        DamageTerm(90), DamageTerm(90, DamageCondition.IfDefenderExOrV))))


    // ------------------- SET 151 (sv3pt5) — Fase 17: "+X si jugaste tal carta este turno" -------------------

    // Wigglytuff ex — Placaje Amigo (Friend Tackle): 90 + 90 si jugaste un Partidario este turno (2 printings).
    put(atkKey("sv3pt5-40", "Friend Tackle"), Effect(attackDamage = listOf(
        DamageTerm(90), DamageTerm(90, DamageCondition.IfSupporterPlayedThisTurn))))

    put(atkKey("sv3pt5-187", "Friend Tackle"), Effect(attackDamage = listOf(
        DamageTerm(90), DamageTerm(90, DamageCondition.IfSupporterPlayedThisTurn))))

    // Rhydon — Taladro Carismático (Charismatic Drill): 40 + 140 si jugaste Carisma de Giovanni este turno.
    put(atkKey("sv3pt5-112", "Charismatic Drill"), Effect(attackDamage = listOf(
        DamageTerm(40), DamageTerm(140, DamageCondition.IfPlayedTrainerThisTurn("Giovanni")))))

    // Tangela — Enredo Sutil (Tactful Tangling): 10 + 60 si jugaste Invitación de Erika este turno (2 printings).
    put(atkKey("sv3pt5-114", "Tactful Tangling"), Effect(attackDamage = listOf(
        DamageTerm(10), DamageTerm(60, DamageCondition.IfPlayedTrainerThisTurn("Erika")))))

    put(atkKey("sv3pt5-178", "Tactful Tangling"), Effect(attackDamage = listOf(
        DamageTerm(10), DamageTerm(60, DamageCondition.IfPlayedTrainerThisTurn("Erika")))))


    // ------------------- SET 151 (sv3pt5) — Fase 16: reducción PARCIAL de daño + prevención por moneda -------------------

    // -- "Durante el próximo turno del rival, los ataques hacen X menos a este Pokémon" (op nueva) --
    // Geodude — Endurecimiento (Stiffen): sin daño, reduce 30 el próximo turno.
    put(atkKey("sv3pt5-74", "Stiffen"), Effect(ops = listOf(EffectOp.ReduceDamageNextTurn(30))))

    // Shellder — Presión Caparazón (Shell Press): 30 base + reduce 30.
    put(atkKey("sv3pt5-90", "Shell Press"), Effect(ops = listOf(EffectOp.ReduceDamageNextTurn(30))))

    // Cloyster — Carga Protectora (Protect Charge): 80 base + reduce 80.
    put(atkKey("sv3pt5-91", "Protect Charge"), Effect(ops = listOf(EffectOp.ReduceDamageNextTurn(80))))


    // -- Squirtle — Refugio (Withdraw), printing 170: moneda, cara = evita todo el daño el próximo turno --
    put(atkKey("sv3pt5-170", "Withdraw"), Effect(ops = listOf(EffectOp.PreventDamageNextTurn(Target.SELF, coinFlip = true))))


    // ------------------- SET 151 (sv3pt5) — Fase 15: condicionales +X, reusos y ops nuevas -------------------

    // -- "+140 si tienes la misma cantidad de cartas en la mano que el rival" (Ninetales ex — Llamas Reflejadas) --
    put(atkKey("sv3pt5-38", "Mirrored Flames"), Effect(attackDamage = listOf(
        DamageTerm(80), DamageTerm(140, DamageCondition.IfSameHandSizeAsOpponent))))

    put(atkKey("sv3pt5-186", "Mirrored Flames"), Effect(attackDamage = listOf(
        DamageTerm(80), DamageTerm(140, DamageCondition.IfSameHandSizeAsOpponent))))


    // -- "+90 si te quedan más cartas de Premio que al rival" (Pinsir — Lanzamiento Audaz) --
    put(atkKey("sv3pt5-127", "Reckless Throw"), Effect(attackDamage = listOf(
        DamageTerm(90), DamageTerm(90, DamageCondition.IfMorePrizesThanOpponent))))


    // -- Combos "+X si tal Pokémon está en tu Banca" (Electabuzz/Magmar) --
    put(atkKey("sv3pt5-125", "Electro Combo"), Effect(attackDamage = listOf(
        DamageTerm(10), DamageTerm(40, DamageCondition.IfSelfBenchHasName("Magmar")))))

    put(atkKey("sv3pt5-126", "Flare Combo"), Effect(attackDamage = listOf(
        DamageTerm(80), DamageTerm(80, DamageCondition.IfSelfBenchHasName("Electabuzz")))))


    // -- Charizard ex — Ala Osada (Brave Wing): 60 + 100 si este Pokémon tiene daño (2 printings) --
    put(atkKey("sv3pt5-183", "Brave Wing"), Effect(attackDamage = listOf(
        DamageTerm(60), DamageTerm(100, DamageCondition.IfSelfHasDamage))))

    put(atkKey("sv3pt5-199", "Brave Wing"), Effect(attackDamage = listOf(
        DamageTerm(60), DamageTerm(100, DamageCondition.IfSelfHasDamage))))

    // Charizard ex — Vórtice Explosivo (Explosive Vortex): descarta 3 Energías de sí mismo.
    // (183 ya está registrado arriba; falta la otra printing.)
    put(atkKey("sv3pt5-199", "Explosive Vortex"), Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 3))))


    // -- Alakazam ex — Levantamiento (Mind Jack): 90 + 30 por cada Pokémon en Banca rival (2 printings) --
    put(atkKey("sv3pt5-188", "Mind Jack"), Effect(ops = listOf(
        EffectOp.ExtraDamage(Amount.PerCount(Counter.BENCH_COUNT, Target.OPP_BENCH, 30)))))

    put(atkKey("sv3pt5-201", "Mind Jack"), Effect(ops = listOf(
        EffectOp.ExtraDamage(Amount.PerCount(Counter.BENCH_COUNT, Target.OPP_BENCH, 30)))))


    // -- Poliwhirl — Salto de la Rana (Frog Hop): 30 + 60 si sale cara --
    put(atkKey("sv3pt5-176", "Frog Hop"), Effect(ops = listOf(EffectOp.CoinFlipDamage(1, 60))))


    // -- Kangaskhan ex — Puñetazo Incesante (Incessant Punching), printing 190: 4 monedas, 100 por cara --
    put(atkKey("sv3pt5-190", "Incessant Punching"), Effect(ops = listOf(EffectOp.CoinFlipDamage(4, 100))))


    // -- Arbok ex — Colmillos Amenazantes (Menacing Fangs): el rival descarta 2 de su mano --
    put(atkKey("sv3pt5-185", "Menacing Fangs"), Effect(ops = listOf(EffectOp.OpponentDiscardsHand(2))))


    // -- Pikachu — Carga (Charge): busca 1 Energía {L} Básica y únela a sí mismo --
    put(atkKey("sv3pt5-173", "Charge"), Effect(ops = listOf(EffectOp.SearchEnergyAttachSelf(EnergyType.LIGHTNING, 1))))


    // -- Charmeleon — Llamarada (Fire Blast): descarta 1 Energía de sí mismo --
    put(atkKey("sv3pt5-169", "Fire Blast"), Effect(ops = listOf(EffectOp.DiscardEnergy(Target.SELF, 1))))


    // -- Mr. Mime — Psicopoder (Psypower), printing 179: reparte 3 contadores entre los Pokémon rivales --
    put(atkKey("sv3pt5-179", "Psypower"), Effect(ops = listOf(EffectOp.PlaceCounters(3, Target.OPP_ALL))))


    // -- Wartortle — Buceo Libre (Free Diving), printing 171: hasta 3 Energía {W} Básica del descarte a la mano --
    put(atkKey("sv3pt5-171", "Free Diving"), Effect(ops = listOf(
        EffectOp.RecoverFromDiscard(CardFilter(supertype = Supertype.ENERGY, type = EnergyType.WATER), 3))))


    // -- Omanyte — Retorno Tentacular (Tentacular Return): devuelve 1 Energía del Activo rival a su mano (2 printings) --
    put(atkKey("sv3pt5-138", "Tentacular Return"), Effect(ops = listOf(EffectOp.BounceOppActiveEnergyToHand(1))))

    put(atkKey("sv3pt5-180", "Tentacular Return"), Effect(ops = listOf(EffectOp.BounceOppActiveEnergyToHand(1))))


    // -- Gengar — Poltergeist: 50 de daño por cada Entrenador en la mano del rival (op nueva) --
    put(atkKey("sv3pt5-94", "Poltergeist"), Effect(ops = listOf(EffectOp.DamagePerOppHandTrainer(50))))


    // -- Onix — Alud Descomunal (Thumpalanche): descarta 5 propias, 80 por Pokémon con retiro exacto 4 (op nueva) --
    put(atkKey("sv3pt5-95", "Thumpalanche"), Effect(ops = listOf(EffectOp.DiscardTopDeckDamagePerRetreat(5, 4, 80))))


    // ------------------- SET 151 (sv3pt5) — Fase 14: moneda-hasta-cruz + mill + daño-antes-del-base -------------------

    // -- "Lanza 1 moneda hasta que salga cruz. Este ataque hace X por cada cara" --
    // Graveler — Cañón Roca (Rock Cannon): 40 por cara (base Variable).
    put(atkKey("sv3pt5-75", "Rock Cannon"),
        Effect(ops = listOf(EffectOp.CoinUntilTailsDamage(40))))

    // Exeggcute — Rodabola (Ball Roll): 30 por cara.
    put(atkKey("sv3pt5-102", "Ball Roll"),
        Effect(ops = listOf(EffectOp.CoinUntilTailsDamage(30))))

    // Tentacruel — Pánico Tentacular (Tentacular Panic): 90 por cara; si la 1ª sale cruz, Confundido.
    put(atkKey("sv3pt5-73", "Tentacular Panic"),
        Effect(ops = listOf(EffectOp.CoinUntilTailsDamage(90, confuseIfFirstTails = true))))


    // -- "Lanza 1 moneda hasta que salga cruz. Por cada cara, roba 1 carta" --
    // Magikarp — Salpicadura Salpicona (Splashy Splash).
    put(atkKey("sv3pt5-129", "Splashy Splash"),
        Effect(ops = listOf(EffectOp.CoinUntilTailsDraw)))


    // -- Mill: descartar las N primeras cartas de una baraja --
    // Machop — Aplastamiento Montaña (Mountain Mashing): 1 de la baraja rival (sin daño base).
    put(atkKey("sv3pt5-66", "Mountain Mashing"),
        Effect(ops = listOf(EffectOp.DiscardTopDeck(1, own = false))))

    // Machoke — Empuje Montaña (Mountain Ramming): 50 base + 1 de la baraja rival (dos printings).
    put(atkKey("sv3pt5-67", "Mountain Ramming"),
        Effect(ops = listOf(EffectOp.DiscardTopDeck(1, own = false))))

    put(atkKey("sv3pt5-177", "Mountain Ramming"),
        Effect(ops = listOf(EffectOp.DiscardTopDeck(1, own = false))))

    // Kingler — Machada (Hammer Arm): 90 base + 1 de la baraja rival.
    put(atkKey("sv3pt5-99", "Hammer Arm"),
        Effect(ops = listOf(EffectOp.DiscardTopDeck(1, own = false))))

    // Machamp — Tajo Montaña (Mountain Chopping): 100 base + 2 de la baraja rival.
    put(atkKey("sv3pt5-68", "Mountain Chopping"),
        Effect(ops = listOf(EffectOp.DiscardTopDeck(2, own = false))))

    // Dragonite — Pulso Drágon (Dragon Pulse): 180 base + descarta 2 de TU baraja.
    put(atkKey("sv3pt5-149", "Dragon Pulse"),
        Effect(ops = listOf(EffectOp.DiscardTopDeck(2, own = true))))


    // -- "+X de daño por cada contador de daño en el ACTIVO RIVAL", medido ANTES del daño base --
    // (DamageTerm.perDefenderCounter evaluado en GameEngine.attack; NO snowballea como ExtraDamage.)
    // Rattata — Roer la Herida (Gnaw the Wound): 20 + 10 por contador del Activo rival.
    put(atkKey("sv3pt5-19", "Gnaw the Wound"),
        Effect(attackDamage = listOf(DamageTerm(20, perDefenderCounter = 10))))

    // Raticate — Segundo Mordisco (Second Bite): 30 + 30 por contador del Activo rival.
    put(atkKey("sv3pt5-20", "Second Bite"),
        Effect(attackDamage = listOf(DamageTerm(30, perDefenderCounter = 30))))


    // ------------------- SET 151 (sv3pt5) — Fase 13: DAÑO ESCALADO + varios (ops existentes) -------------------

    // -- "+X de daño por cada contador de daño en ESTE Pokémon" (PerCount · SELF; sin bola de nieve) --
    // Dodrio — Pico Balístico (Ballistic Beak): 10 base + 30 por contador propio.
    put(atkKey("sv3pt5-85", "Ballistic Beak"),
        Effect(ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.DAMAGE_COUNTERS, Target.SELF, 30)))))

    // Tauros — Furia (Rage): 30 base + 10 por contador propio.
    put(atkKey("sv3pt5-128", "Rage"),
        Effect(ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.DAMAGE_COUNTERS, Target.SELF, 10)))))


    // -- "+30 por cada Energía unida al Activo rival" --
    // Exeggutor — Psíquico (Psychic): 30 base + 30 por Energía del Activo rival.
    put(atkKey("sv3pt5-103", "Psychic"),
        Effect(ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.ENERGY_ATTACHED, Target.OPP_ACTIVE, 30)))))


    // -- Monedas fijas, X por cara --
    // Kangaskhan ex — Puñetazo Incesante (Incessant Punching): 4 monedas, 100 por cara (base Variable).
    put(atkKey("sv3pt5-115", "Incessant Punching"),
        Effect(ops = listOf(EffectOp.CoinFlipDamage(4, 100))))


    // -- Unir Energía del descarte a sí mismo --
    // Arcanine — Torrente Tórrido (Torrid Torrent): 30 base + une hasta 2 Energía {R} del descarte a sí mismo.
    put(atkKey("sv3pt5-59", "Torrid Torrent"),
        Effect(ops = listOf(EffectOp.AttachEnergyFromDiscard(2, EnergyType.FIRE, Target.SELF))))


    // -- Gust (subir al Activo rival un Pokémon de su Banca que YO elijo) --
    // Clefable — Señuelo (Follow Me): cambia 1 de la Banca rival por su Activo (sin daño base).
    put(atkKey("sv3pt5-36", "Follow Me"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_BENCH, 1, prompt(
                "Elige un Pokémon de la Banca rival para subirlo a su Activo",
                "Choose a Benched Pokémon of your opponent to switch to the Active Spot"), optional = true),
            EffectOp.SwapOppActiveWithChosen)))


    // ------------------- SET 151 (sv3pt5) — Fase 39: GUST CON MONEDA -------------------
    // Op nueva CoinFlipSwapOppActiveWithChosen: elige (ChooseTarget OPP_BENCH) y luego, SOLO con
    // cara, sube ese Pokémon al Activo rival. Sin daño base.

    // Meowth — Ven Aquí Ya (Come Here Right Meow): lanza 1 moneda; si cara, cambia 1 de la Banca
    // rival por su Activo. Con cruz (o banca vacía) no pasa nada.
    put(atkKey("sv3pt5-52", "Come Here Right Meow"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_BENCH, 1, prompt(
                "Elige un Pokémon de la Banca rival para subirlo a su Activo si sale cara",
                "Choose a Benched Pokémon of your opponent to switch to the Active Spot if heads"), optional = true),
            EffectOp.CoinFlipSwapOppActiveWithChosen)))


    // ------------------- SET 151 (sv3pt5) — Fase 12: REPARTIR CONTADORES DE DAÑO -------------------
    // Op nueva PlaceCounters(count, target): pausa con PendingDecision.PlaceCounters para
    // repartir [count] contadores de 10 de daño "como quieras". Sin candidatos → se salta.

    // Mr. Mime — Psicopoder (Psypower): 3 contadores entre los Pokémon del rival (Activo + Banca).
    put(atkKey("sv3pt5-122", "Psypower"),
        Effect(ops = listOf(EffectOp.PlaceCounters(3, Target.OPP_ALL))))

    // Gengar — Embestida Hueca (Hollow Dive): 110 de daño base al Activo (de la carta) Y ADEMÁS
    // pon 3 contadores en la Banca rival "como quieras". El 110 lo aplica el motor desde
    // Attack.damage; aquí solo el efecto de contadores → NO hay doble conteo. (Verificado contra
    // la carta impresa 094/165: el ataque hace las DOS cosas.)
    put(atkKey("sv3pt5-94", "Hollow Dive"),
        Effect(ops = listOf(EffectOp.PlaceCounters(3, Target.OPP_BENCH))))


    // ------------------- SET 151 (sv3pt5) — Fase 11: SNIPE DIRIGIDO + SWITCH-SELF -------------------
    // Todos usan ChooseTarget con optional=true: si la banca/objetivo está vacío, la op
    // se salta y el ataque hace su daño base igual (arreglo del "bug de altitud").

    // -- Snipe dirigido a la Banca/Pokémon rival --
    // Golbat — Técnica de Buceo (Skill Dive): 40 a UN Pokémon del rival (Activo o Banca).
    put(atkKey("sv3pt5-42", "Skill Dive"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_ALL, 1, prompt("Elige un Pokémon del rival (40 de daño)", "Choose one of your opponent's Pokémon (40 damage)"), optional = true),
            EffectOp.Damage(Target.CHOSEN, Amount.Fixed(40)),
        )))

    // Dewgong — Doble Salpicadura (Dual Splash): 50 a 2 Pokémon del rival.
    put(atkKey("sv3pt5-87", "Dual Splash"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_ALL, 2, prompt("Elige 2 Pokémon del rival (50 de daño cada uno)", "Choose 2 of your opponent's Pokémon (50 damage each)"), optional = true),
            EffectOp.Damage(Target.CHOSEN, Amount.Fixed(50)),
        )))

    // Omastar — Isoaqua (Aqua Split): además de su daño base (90), 30 a 2 Pokémon de la Banca rival.
    put(atkKey("sv3pt5-139", "Aqua Split"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OPP_BENCH, 2, prompt("Elige 2 Pokémon de la Banca rival (30 de daño cada uno)", "Choose 2 Benched Pokémon (30 damage each)"), optional = true),
            EffectOp.Damage(Target.CHOSEN, Amount.Fixed(30)),
        )))


    // -- Switch-self: tras atacar, este Pokémon vuelve a la Banca (elige quién sube) --
    // Kadabra — Ataque Teleportador (Teleportation Attack): 30 base + cambia este Pokémon por uno de tu Banca.
    put(atkKey("sv3pt5-64", "Teleportation Attack"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige un Pokémon de tu Banca para pasar al Activo", "Choose a Benched Pokémon to switch to the Active Spot"), optional = true),
            EffectOp.SwapActiveWithChosen,
        )))

    // Rapidash — Giro Mach (Mach Turn): cambia este Pokémon por uno de tu Banca.
    put(atkKey("sv3pt5-78", "Mach Turn"),
        Effect(ops = listOf(
            EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige un Pokémon de tu Banca para pasar al Activo", "Choose a Benched Pokémon to switch to the Active Spot"), optional = true),
            EffectOp.SwapActiveWithChosen,
        )))

    // Hitmonlee — Patada Torbellino (Twister Kick): 10 a cada Pokémon del rival + switch-self.
    put(atkKey("sv3pt5-106", "Twister Kick"),
        Effect(ops = listOf(
            EffectOp.Damage(Target.OPP_ALL, Amount.Fixed(10)),
            EffectOp.ChooseTarget(Target.OWN_BENCH, 1, prompt("Elige un Pokémon de tu Banca para pasar al Activo", "Choose a Benched Pokémon to switch to the Active Spot"), optional = true),
            EffectOp.SwapActiveWithChosen,
        )))


    // -- DIFERIDO (necesitan op nueva PlaceCounters con reparto de contadores): --
    // Gengar sv3pt5-94 Embestida Hueca (3 contadores a la Banca rival a repartir),
    // Mr. Mime sv3pt5-122 Psicopoder (3 contadores a los Pokémon rivales a repartir).

    // ------------------- SET 151 (sv3pt5) — Fase 10: ESTADOS / RESTRICCIONES / CURA -------------------
    // Slowbro — Gran Bostezo: ambos Activos pasan a estar Dormidos.
    put(atkKey("sv3pt5-80", "Big Yawn"),
        Effect(ops = listOf(
            EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.ASLEEP)),
            EffectOp.ApplyStatus(Target.SELF, listOf(Status.ASLEEP)),
        )))

    // Nidoking — Impacto Envenenado (190, arte alt 174): Activo rival Envenenado.
    put(atkKey("sv3pt5-174", "Venomous Impact"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.POISONED)))))

    // Venusaur ex — Toxilatigazo Peligroso (150, arte alt 198): Activo rival Confundido + Envenenado.
    put(atkKey("sv3pt5-198", "Dangerous Toxwhip"),
        Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.CONFUSED, Status.POISONED)))))

    // Marowak — Poder Ilimitado (120): durante tu próximo turno, este Pokémon no puede atacar.
    put(atkKey("sv3pt5-105", "Boundless Power"),
        Effect(ops = listOf(EffectOp.NoAttackNextTurn)))

    // Dragonair — Cuchillada Acuática (90, y arte alt 181): este Pokémon no puede atacar el próximo turno.
    put(atkKey("sv3pt5-148", "Aqua Slash"),
        Effect(ops = listOf(EffectOp.NoAttackNextTurn)))

    put(atkKey("sv3pt5-181", "Aqua Slash"),
        Effect(ops = listOf(EffectOp.NoAttackNextTurn)))

    // Bellsprout — Amarrar (20, y arte alt 185): el Defensor no puede retirarse el próximo turno.
    put(atkKey("sv3pt5-69", "Bind Down"),
        Effect(ops = listOf(EffectOp.DefenderCannotRetreatNextTurn)))

    put(atkKey("sv3pt5-185", "Bind Down"),
        Effect(ops = listOf(EffectOp.DefenderCannotRetreatNextTurn)))

    // Lickitung — Traba-Lengua (70): el Defensor no puede atacar el próximo turno.
    put(atkKey("sv3pt5-108", "Tongue-Tied"),
        Effect(ops = listOf(EffectOp.DefenderCannotAttackNextTurn)))

    // Bulbasaur/Ivysaur — Drenadoras (artes alt 166/167): cura 20 a este Pokémon.
    put(atkKey("sv3pt5-166", "Leech Seed"),
        Effect(ops = listOf(EffectOp.Heal(Target.SELF, Amount.Fixed(20)))))

    put(atkKey("sv3pt5-167", "Leech Seed"),
        Effect(ops = listOf(EffectOp.Heal(Target.SELF, Amount.Fixed(20)))))

    // Caterpie — Mascahojas (10+, arte alt 172): +30 si el Activo rival es de tipo {G}.
    put(atkKey("sv3pt5-172", "Leaf Munch"), Effect(attackDamage = listOf(
        DamageTerm(10), DamageTerm(30, DamageCondition.IfDefenderType(EnergyType.GRASS)))))

    // Ayuda de Daisy — roba 2.
    put(EffectId("sv3pt5-158"), Effect(ops = listOf(EffectOp.DrawCards(2))))

    put(EffectId("sv3pt5-206"), switch)


    // ============================ HERRAMIENTAS (pasivos; INERTES hasta fase de pasivos) ============================
    // El motor aún no aplica PassiveModifier; se registran para completar el mapeo.
    // Los filtros de etapa/básico del kit no se modelan todavía en PassiveModifier.

    // Gafas Protectoras — Básico portador sin Debilidad.
    put(EffectId("sv3pt5-164"), Effect(passives = listOf(PassiveModifier(ModKind.NO_WEAKNESS, 0, Target.SELF))))

    // Gran Globo Aerostático — portador sin coste de retirada.
    put(EffectId("sv3pt5-155"), Effect(passives = listOf(PassiveModifier(ModKind.RETREAT_COST, 0, Target.SELF))))

    // Banda Rígida — portador recibe 30 menos de daño.
    put(EffectId("sv3pt5-165"), Effect(passives = listOf(PassiveModifier(ModKind.REDUCE_DAMAGE, 30, Target.SELF))))
}
