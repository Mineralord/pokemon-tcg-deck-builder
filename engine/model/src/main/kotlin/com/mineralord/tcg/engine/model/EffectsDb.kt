package com.mineralord.tcg.engine.model

/**
 * Base de efectos AUTORADA por carta — traducción Kotlin del kit
 * `docs/efectos-porting/efectos-db.js` (49 cartas semilla, ya validadas por la
 * web retirada). Cada entrada del JS se mapea al catálogo CERRADO de
 * [EffectOp]/[Effect] de `Effects.kt`. El resultado puebla el [EffectRegistry]
 * central que el motor consulta por [EffectId].
 *
 * Claves: una carta tiene varios ataques/habilidades, así que la clave no puede
 * ser solo el id — se compone con el nombre (en inglés) del ataque/habilidad:
 *  - ataque:    "<cardId>#atk:<nombreEn>"   (ver [atkKey])
 *  - habilidad: "<cardId>#abi:<nombreEn>"   (ver [abiKey])
 *  - entrenador/energía/estadio/herramienta: el id de carta a secas.
 *
 * Cobertura: se registran las entradas cuyo efecto cabe en el catálogo actual.
 * Las cartas que usan ops aún no modeladas (cambiarActivo, barajarManoEnMazo,
 * buscarDescarte, mirarTopN, daño/heal condicional, filtros premiosMin/tera)
 * quedan SIN registrar a propósito: caen a daño base. Ver `// TODO`s al final.
 */
object EffectsDb {

    fun atkKey(cardId: String, attackEn: String): EffectId = EffectId("$cardId#atk:$attackEn")
    fun abiKey(cardId: String, abilityEn: String): EffectId = EffectId("$cardId#abi:$abilityEn")

    private fun prompt(es: String, en: String) = LocalizedText(es, en)

    // --- Definiciones reutilizables (mismo efecto en varios artes/printings) ---

    /** Solid Shell / Solid Body — pasivo: -30 al daño recibido por este Pokémon. */
    private val SOLID = Effect(passives = listOf(PassiveModifier(ModKind.REDUCE_DAMAGE, 30, Target.SELF)))

    /** Tranquil Flower — 1/turno (solo activo): elige 1 de los tuyos y cúralo 60. */
    private val TRANQUIL = Effect(
        ops = listOf(
            EffectOp.ChooseTarget(Target.OWN_ALL, 1, prompt("Elige un Pokémon para curar 60", "Choose a Pokémon to heal 60")),
            EffectOp.Heal(Target.CHOSEN, Amount.Fixed(60)),
        ),
        oncePerTurn = true,
        activeOnly = true,
    )

    /** Restart — 1/turno: roba hasta tener 3 cartas en mano. */
    private val RESTART = Effect(ops = listOf(EffectOp.DrawUntil(3)), oncePerTurn = true)

    /** Calming Light — 1/turno (solo activo): el Activo rival queda Dormido. */
    private val CALMING = Effect(
        ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.ASLEEP))),
        oncePerTurn = true,
        activeOnly = true,
    )

    val registry: EffectRegistry = EffectRegistry(
        buildMap {

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
            // Milotic ex — Hypno Splash: rival Dormido.
            put(atkKey("sv8-42", "Hypno Splash"),
                Effect(ops = listOf(EffectOp.ApplyStatus(Target.OPP_ACTIVE, listOf(Status.ASLEEP)))))
            // Scovillain ex — Spicy Rage: +70 por cada contador de daño en sí mismo.
            put(atkKey("sv8-37", "Spicy Rage"),
                Effect(ops = listOf(EffectOp.ExtraDamage(Amount.PerCount(Counter.DAMAGE_COUNTERS, Target.SELF, 70)))))
            // Black Kyurem ex — Black Frost: 30 de retroceso a sí mismo.
            put(atkKey("sv8-48", "Black Frost"),
                Effect(ops = listOf(EffectOp.Recoil(Amount.Fixed(30)))))
            // Zapdos ex — Multishot Lightning: 90 a 1 Pokémon de la Banca rival.
            put(atkKey("sv3pt5-145", "Multishot Lightning"),
                Effect(ops = listOf(
                    EffectOp.ChooseTarget(Target.OPP_BENCH, 1, prompt("Elige un Pokémon de la Banca rival (90 de daño)", "Choose a Benched Pokémon (90 damage)")),
                    EffectOp.Damage(Target.CHOSEN, Amount.Fixed(90)),
                )))
            // Hydreigon ex — Obsidian: 130 a 2 Pokémon de la Banca rival.
            put(atkKey("sv8-119", "Obsidian"),
                Effect(ops = listOf(
                    EffectOp.ChooseTarget(Target.OPP_BENCH, 2, prompt("Elige 2 Pokémon de la Banca rival (130 de daño)", "Choose 2 Benched Pokémon (130 damage)")),
                    EffectOp.Damage(Target.CHOSEN, Amount.Fixed(130)),
                )))

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
            put(abiKey("sv8-54", "Solid Body"), SOLID)
            put(abiKey("sv8-201", "Solid Body"), SOLID)
            // Mew ex — Restart (varios artes).
            put(abiKey("sv3pt5-151", "Restart"), RESTART)
            put(abiKey("sv3pt5-193", "Restart"), RESTART)
            put(abiKey("sv3pt5-205", "Restart"), RESTART)
            // Shiinotic — Calming Light (varios artes).
            put(abiKey("sv8-9", "Calming Light"), CALMING)
            put(abiKey("sv8-194", "Calming Light"), CALMING)

            // ============================ ENTRENADORES (listos; corren cuando exista su intent) ============================

            // Nemona — roba 3.
            put(EffectId("sv1-180"), Effect(ops = listOf(EffectOp.DrawCards(3))))
            // Ayuda de Daisy — roba 2.
            put(EffectId("sv3pt5-158"), Effect(ops = listOf(EffectOp.DrawCards(2))))
            // Nido Ball — busca 1 Básico a la Banca.
            put(EffectId("sv1-181"), Effect(ops = listOf(
                EffectOp.SearchDeck(CardFilter(isBasic = true), Zone.BENCH, 1))))
            // Super Ball — busca 1 Pokémon a la mano.
            put(EffectId("sv2-183"), Effect(ops = listOf(
                EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON), Zone.HAND, 1))))
            // Bola Ocaso — busca 1 Pokémon a la mano.
            put(EffectId("sv8-175"), Effect(ops = listOf(
                EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON), Zone.HAND, 1))))
            // Poción — cura 30 a 1 de los tuyos.
            put(EffectId("sv1-188"), Effect(ops = listOf(
                EffectOp.ChooseTarget(Target.OWN_ALL, 1, prompt("Elige un Pokémon para curarle 30", "Choose a Pokémon to heal 30")),
                EffectOp.Heal(Target.CHOSEN, Amount.Fixed(30)))))
            // Cinio (Jacq) — busca hasta 2 Pokémon de Evolución a la mano.
            put(EffectId("sv1-175"), Effect(ops = listOf(
                EffectOp.SearchDeck(CardFilter(supertype = Supertype.POKEMON, isBasic = false), Zone.HAND, 2))))

            // ============================ HERRAMIENTAS (pasivos; INERTES hasta fase de pasivos) ============================
            // El motor aún no aplica PassiveModifier; se registran para completar el mapeo.
            // Los filtros de etapa/básico del kit no se modelan todavía en PassiveModifier.

            // Gafas Protectoras — Básico portador sin Debilidad.
            put(EffectId("sv3pt5-164"), Effect(passives = listOf(PassiveModifier(ModKind.NO_WEAKNESS, 0, Target.SELF))))
            // Gran Globo Aerostático — portador sin coste de retirada.
            put(EffectId("sv3pt5-155"), Effect(passives = listOf(PassiveModifier(ModKind.RETREAT_COST, 0, Target.SELF))))
            // Banda Rígida — portador recibe 30 menos de daño.
            put(EffectId("sv3pt5-165"), Effect(passives = listOf(PassiveModifier(ModKind.REDUCE_DAMAGE, 30, Target.SELF))))

            // TODO(fase: extender DSL) — sin registrar porque usan ops no modeladas:
            //  - cambiarActivo:        sv3pt5-206 (Cambio), me1-114 (Órdenes de Jefes)
            //  - barajarManoEnMazo:    sv1-198 (Joven), sv8-173 (Drasna)
            //  - buscarDescarte:       sv8-251 (Camilla Nocturna)
            //  - mirarTopN:            sv3pt5-156 (Transferencia de Bill)
            //  - daño condicional:     sv3pt5-6 Brave Wing (tieneDanio)
            //  - heal con filtro tipo: sv8-167 (Clemont), sv8-172 (Elixir de Dragón)
            //  - filtros no modelados: sv8-170 (Cyrano, premiosMin), sv8-189 (Tera Orbe, tera)
            //  - estadios "a todos":   sv8-180 (Estadio Animado), sv8-177 (Montaña Gravedad)
        },
    )
}
