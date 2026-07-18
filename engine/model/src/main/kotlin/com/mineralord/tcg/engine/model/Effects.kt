package com.mineralord.tcg.engine.model

/**
 * DSL de efectos — catálogo CERRADO y type-safe (reescrito en Kotlin, inspirado
 * en el DSL JS ya validado de la web `js/efectos-dsl.js`).
 *
 * Un [Effect] es un programita declarativo que el motor interpreta de forma
 * **determinista**. Cuando una op requiere una decisión (p.ej. [ChooseTarget]),
 * el motor emite una decisión pendiente que IA/jugador resuelven — nunca queda
 * en "resolución manual" ambigua.
 */

/** A quién apunta una operación. */
enum class Target {
    SELF, OWN_ACTIVE, OPP_ACTIVE,
    OWN_BENCH, OPP_BENCH, OWN_ALL, OPP_ALL,
    CHOSEN,                       // resuelto por una ChooseTarget previa
    OWN_DECK, OWN_DISCARD, OWN_HAND,
}

/** Contadores escalables para daño/curación "por cada …". */
enum class Counter { BENCH_COUNT, ENERGY_ATTACHED, DAMAGE_COUNTERS, HEADS }

/** Cantidad fija o derivada de un conteo del estado de juego. */
sealed interface Amount {
    data class Fixed(val n: Int) : Amount
    /**
     * Conteo escalado. Si [of] es [Counter.ENERGY_ATTACHED] y [energyType] no es null,
     * solo cuenta las Energías Básicas de ese tipo unidas a [target] (Seaking — Cuerno
     * Aqua: +30 por cada Energía {W} unida a sí mismo). Para el resto de contadores
     * [energyType] se ignora.
     */
    data class PerCount(
        val of: Counter,
        val target: Target,
        val mult: Int,
        val energyType: EnergyType? = null,
    ) : Amount
}

/** Filtro para búsquedas en mazo/descarte. */
data class CardFilter(
    val supertype: Supertype? = null,
    val isBasic: Boolean? = null,
    val type: EnergyType? = null,
    val nameContains: String? = null,
    /**
     * Solo casa cartas de Entrenador de esta categoría (Jigglypuff — Liderazgo busca
     * 1 Partidario; Magneton — Imán de Chatarra recupera Objetos). Implica Entrenador.
     */
    val trainerKind: TrainerCategory? = null,
)

/** Categoría de Entrenador para filtrar (Partidario/Objeto/Estadio/Herramienta). */
enum class TrainerCategory { SUPPORTER, ITEM, STADIUM, TOOL }

/** Zona destino de una búsqueda/movimiento. */
enum class Zone { HAND, BENCH, ACTIVE, DECK, DISCARD, LOST }

/** Modificadores pasivos (habilidades/herramientas/energías especiales). */
enum class ModKind {
    REDUCE_DAMAGE, EXTRA_HP, RETREAT_COST, ATTACK_COST,
    IMMUNE_STATUS, PROVIDES_ENERGY, EXTRA_DAMAGE,
    BLOCK_ABILITY, NO_RETREAT, NO_WEAKNESS,
}

data class PassiveModifier(
    val mod: ModKind,
    val amount: Int = 0,
    val appliesTo: Target = Target.SELF,
    /**
     * Si no es null, el pasivo solo aplica cuando el Pokémon que porta la
     * habilidad tiene unida ≥1 carta de Energía de este tipo (Flotación
     * Glacial/Voltaica/Ígnea: "si este Pokémon tiene alguna Energía {X}
     * unida, no tiene ningún Coste de Retirada").
     */
    val requiresEnergyType: EnergyType? = null,
    /**
     * Si es true, el pasivo solo aplica cuando el Pokémon que porta la habilidad
     * tiene unida ≥1 carta de Energía ESPECIAL (Wigglytuff ex — Cuerpo Expansivo:
     * "si este Pokémon tiene alguna Energía Especial unida, obtiene 100 PS más").
     */
    val requiresSpecialEnergy: Boolean = false,
    /**
     * Solo para [ModKind.BLOCK_ABILITY]: alcance del bloqueo de Habilidades.
     * - [blockBothSides] true (por defecto): afecta a los Pokémon de AMBOS lados en juego
     *   (Klefki *Cierre Travieso*, Camino hacia la Cima). false = solo a los del lado RIVAL
     *   del origen del bloqueo.
     * - [blockOnlyRuleBox] true: solo afecta a Pokémon CON caja de regla (Camino hacia la
     *   Cima / Path to the Peak). false = a todos.
     * El propio Pokémon origen del bloqueo (si lo hay) queda siempre exento, y solo se apagan
     * las [AbilityKind.ABILITY] (Poké-Power/Poké-Body y Rasgos Antiguos son inmunes).
     */
    val blockBothSides: Boolean = true,
    val blockOnlyRuleBox: Boolean = false,
)

/** Operaciones del catálogo cerrado. */
sealed interface EffectOp {
    data class Damage(val target: Target, val amount: Amount) : EffectOp
    data class ExtraDamage(val amount: Amount) : EffectOp
    data class Recoil(val amount: Amount) : EffectOp
    data class Heal(val target: Target, val amount: Amount) : EffectOp
    data class ApplyStatus(val target: Target, val states: List<Status>) : EffectOp
    /**
     * Aplica [states] a [target] SOLO si el jugador que ataca no tiene cartas en la
     * mano (Beedrill — Aguijón Nadir sv3pt5-15: si tu mano está vacía, el Activo
     * rival pasa a Envenenado y Paralizado). Si hay cartas en mano, no hace nada.
     * El +120 de daño de ese ataque lo cubre la DamageCondition [IfEmptyHand].
     */
    data class ApplyStatusIfEmptyHand(val target: Target, val states: List<Status>) : EffectOp
    data class RemoveStatus(val target: Target) : EffectOp
    data class DrawCards(val count: Int) : EffectOp
    data class DrawUntil(val handSize: Int) : EffectOp
    /**
     * Descarta [count] Energías de [target] (al descarte de su DUEÑO). Si [energyType]
     * no es null, solo descarta Energía Básica de ese tipo (Vaporize: 1 {W} del rival).
     * Si [coinFlip] es true, solo ocurre con cara (Acid Spray/Destructive Flame). */
    data class DiscardEnergy(
        val target: Target,
        val count: Int,
        val energyType: EnergyType? = null,
        val coinFlip: Boolean = false,
    ) : EffectOp
    data class SearchDeck(
        val filter: CardFilter,
        val to: Zone,
        val count: Int,
        /**
         * Si es true, las cartas elegidas deben ser de tipos DISTINTOS entre sí
         * (Eevee — Amigos Coloridos sv3pt5-133: "hasta 3 Pokémon de diferentes tipos").
         * El intérprete recorta la selección a un Pokémon por tipo. */
        val distinctTypes: Boolean = false,
    ) : EffectOp
    /**
     * Recupera hasta [count] cartas del DESCARTE propio (que casen [filter]) y las
     * pone en la mano — Golduck Rescate Acuático, Wartortle Buceo Libre, Snorlax
     * Glotonería. Pausa con una decisión de elección; si el descarte no tiene
     * ninguna carta que case, no hace nada.
     */
    data class RecoverFromDiscard(val filter: CardFilter, val count: Int) : EffectOp
    /**
     * Descarta HASTA [maxCount] cartas de la MANO que casen [filter] (elección del
     * jugador) y hace [perCard] de daño al Activo rival por cada carta descartada
     * (Blastoise ex — Cañones Gemelos sv3pt5-9: descarta hasta 2 {W} Básicas, 140 c/u).
     * Reusa la decisión [PendingDecision.SearchCards] (from=HAND, destination=DISCARD)
     * llevando [perCard] en su campo `damagePerDiscardToOppActive`. El daño es CRUDO
     * (como el resto de daño por efecto: no pasa por Debilidad/Resistencia). Si la mano
     * no tiene cartas que casen, no pausa y el ataque hace 0 (0 descartadas = 0 daño).
     */
    data class DiscardFromHandForDamage(
        val filter: CardFilter,
        val maxCount: Int,
        val perCard: Int,
    ) : EffectOp
    /**
     * Lanza [flips] monedas y busca en tu mazo HASTA (nº de caras) Pokémon que casen
     * [filter] para ponerlos en tu Banca; luego baraja (Parasect — Filamentos Dispersos
     * sv3pt5-47: 2 monedas → Pokémon {G} = caras → Banca). Pausa con [PendingDecision.
     * CoinFlipThenSearch]: el jugador "tira" la moneda y, al resolver (donde el motor SÍ
     * tiene `flip()`), se cuentan las caras y se ENCADENA una `SearchCards` a la Banca con
     * `count = caras`. Si salen 0 caras (o no hay candidatos), no se busca nada.
     */
    data class CoinFlipSearchToBench(val flips: Int, val filter: CardFilter) : EffectOp
    /**
     * Elige [howMany] Pokémon de [from]. Si [onlyDamaged] es true, solo son elegibles
     * los que tienen daño (curaciones tipo Poción): así una carta que solo cura no puede
     * jugarse cuando no hay ningún Pokémon dañado (el motor rechaza si no quedan candidatos).
     *
     * Si [optional] es true, la elección es ADITIVA a un ataque (daño extra a banca, snipe,
     * switch-self): cuando no hay candidatos (banca vacía) la op se SALTA silenciosamente en
     * vez de dejar una decisión vacía. El ataque hace su daño base igual. NO usar en
     * Entrenadores/habilidades cuyo único fin es la elección (Poción, Órdenes de Jefe): esos
     * deben rechazarse sin candidatos.
     */
    data class ChooseTarget(
        val from: Target,
        val howMany: Int,
        val prompt: LocalizedText,
        val onlyDamaged: Boolean = false,
        val optional: Boolean = false,
    ) : EffectOp
    data class MoveEnergy(val from: Target, val to: Target, val count: Int) : EffectOp

    /**
     * Reparte [count] contadores de daño (10 de daño cada uno) entre los Pokémon de
     * [target] "de la manera que desees" (Embestida Hueca de Gengar sobre la Banca rival,
     * Psicopoder de Mr. Mime sobre todos los Pokémon rivales). Pausa con
     * [PendingDecision.PlaceCounters] para que el jugador/IA reparta los contadores; si no
     * hay candidatos, la op se salta (no congela el turno). No aplica Debilidad/Resistencia.
     */
    data class PlaceCounters(val count: Int, val target: Target) : EffectOp

    /** Baraja la mano del jugador dentro de su mazo (Youngster, Iono…). */
    data object ShuffleHandIntoDeck : EffectOp

    /** Intercambia el Activo propio con el Pokémon de Banca elegido (Switch/Cambio).
     *  Usa la carta ligada por una [ChooseTarget] previa sobre [Target.OWN_BENCH]. */
    data object SwapActiveWithChosen : EffectOp

    /** Sube al Activo RIVAL el Pokémon de su Banca elegido (Órdenes de Jefe / gust);
     *  su Activo anterior baja a la Banca. Usa la carta ligada por una [ChooseTarget]
     *  previa sobre [Target.OPP_BENCH]. */
    data object SwapOppActiveWithChosen : EffectOp

    /**
     * "Mueve el Pokémon Activo de tu rival a la Banca. (Tu rival elige el nuevo Pokémon
     * Activo)" — Butterfree Remolino, Rhyhorn Oprimir. El Activo rival pasa a su Banca y se
     * marca `pendingPromotion` para ese lado: el RIVAL elegirá el nuevo Activo (reusa la
     * maquinaria de promoción tras KO). Si el Activo ya quedó Noqueado por el daño de este
     * ataque, no hace nada (lo procesa `handleKnockouts`). */
    data object GustDefenderChooseNewActive : EffectOp

    /** Lanza una moneda y roba [ifHeads] cartas si sale cara, o [ifTails] si cruz (Picnicker). */
    data class CoinFlipDraw(val ifHeads: Int, val ifTails: Int) : EffectOp

    /**
     * Marca al Pokémon origen para que NO pueda atacar durante su PRÓXIMO turno
     * (Kilowattrel — Jet Wing, sv2-82). El intérprete fija
     * [PokemonInPlay.cannotAttackOnTurn] al número del siguiente turno propio
     * (turn actual + 2, porque el turno intermedio es del rival). */
    data object NoAttackNextTurn : EffectOp

    /**
     * Daño al Activo rival = [damagePerHeads] por cada CARA al lanzar una moneda
     * por cada Energía de tipo [energyType] unida al Pokémon origen (Torkoal —
     * Concentrated Fire, 80×). El daño base del ataque es Variable (=0), así que
     * todo el daño proviene de esta op. */
    data class CoinsPerEnergyDamage(val energyType: EnergyType, val damagePerHeads: Int) : EffectOp

    /**
     * Une hasta [count] Energía Básica (de tipo [energyType]; null = cualquiera)
     * desde tu DESCARTE a [target]. Si [target] apunta a un único Pokémon
     * (SELF/OWN_ACTIVE) es determinista (Volcarona — Flame Cloak). Si apunta a
     * varios (OWN_ALL) emite una decisión [PendingDecision.AttachFromRevealed]
     * con `fromDiscard = true` para que el jugador reparta arrastrando
     * (Skeledirge — Passionate Singing). */
    data class AttachEnergyFromDiscard(
        val count: Int,
        val energyType: EnergyType?,
        val target: Target,
        /**
         * "Si lo haces, roba hasta tener [thenDrawUpTo] cartas" (Mela). El robo
         * es ATÓMICO con el enganche: solo ocurre si se unió al menos 1 Energía.
         * null = no roba. */
        val thenDrawUpTo: Int? = null,
    ) : EffectOp

    /**
     * Mira las [lookAt] primeras cartas del mazo y une hasta [maxAttach] Energías
     * Básicas de tipo [energyType] (null = CUALQUIER Energía Básica) que encuentres
     * allí a tus Pokémon de tipo [benchType] (null = cualquier Pokémon), como quieras;
     * el resto se baraja de vuelta (Generador Eléctrico, sv1-170). Con [includeActive]
     * = true el destino incluye también tu Activo, no solo la Banca (Gloom/Vileplume
     * 151 — "a tus Pokémon"). Genera una decisión [PendingDecision.AttachFromRevealed]
     * que el jugador resuelve arrastrando. */
    data class RevealAttachEnergy(
        val lookAt: Int,
        val maxAttach: Int,
        val energyType: EnergyType?,
        val benchType: EnergyType?,
        val includeActive: Boolean = false,
    ) : EffectOp

    /**
     * Lanza [flips] moneda(s) y hace [damagePerHeads] de daño extra al Activo rival
     * por cada CARA (después de Debilidad/Resistencia, como daño crudo). Modela
     * "Lanza 1 moneda. Si sale cara, este ataque hace X puntos de daño más"
     * ([flips]=1) y ataques "X× por cara" con varias monedas. Emite un
     * [GameEvent.CoinFlipped] por tirada para la animación. */
    data class CoinFlipDamage(val flips: Int, val damagePerHeads: Int) : EffectOp

    /**
     * Lanza 1 moneda: si sale CARA, este ataque hace [bonusToOpp] de daño extra al
     * Activo rival (daño crudo, tras el base); si sale CRUZ, este Pokémon se hace
     * [recoilToSelf] de daño a sí mismo. Modela Mankey — Saña (sv3pt5-56). Emite un
     * [GameEvent.CoinFlipped] para la animación. */
    data class CoinFlipDamageOrRecoil(val bonusToOpp: Int, val recoilToSelf: Int) : EffectOp

    /**
     * Lanza 1 moneda y, si sale cara, aplica [states] a [target] ("Lanza 1 moneda.
     * Si sale cara, el Pokémon Activo de tu rival queda Paralizado/Envenenado/…").
     * Emite el [GameEvent.CoinFlipped] para la animación. */
    data class CoinFlipStatus(val target: Target, val states: List<Status>) : EffectOp

    /**
     * "Lanza 1 moneda hasta que salga cruz. Este ataque hace [damagePerHeads] puntos de
     * daño por cada cara" — daño crudo al Activo rival (después de Debilidad/Resistencia).
     * Cañón Roca (Graveler, 40), Rodabola (Exeggcute, 30), Pánico Tentacular (Tentacruel, 90).
     * Si [confuseIfFirstTails] es true y la PRIMERA tirada sale cruz, el Activo rival queda
     * Confundido (Pánico Tentacular). Emite un [GameEvent.CoinFlipped] por tirada. */
    data class CoinUntilTailsDamage(
        val damagePerHeads: Int,
        val confuseIfFirstTails: Boolean = false,
    ) : EffectOp

    /**
     * "Lanza 1 moneda hasta que salga cruz. Por cada cara, roba 1 carta" (Magikarp —
     * Salpicadura Salpicona). Emite un [GameEvent.CoinFlipped] por tirada. */
    data object CoinUntilTailsDraw : EffectOp

    /**
     * "Durante el próximo turno de tu rival, los ataques hacen [amount] puntos de daño MENOS
     * a este Pokémon (después de aplicar Debilidad y Resistencia)" (Geodude — Endurecimiento 30,
     * Shellder — Presión Caparazón 30, Cloyster — Carga Protectora 80). Fija
     * [PokemonInPlay.damageReductionOnTurn] = turno actual + 1 y [damageReductionAmount] = [amount]. */
    data class ReduceDamageNextTurn(val amount: Int) : EffectOp

    /**
     * "Durante tu PRÓXIMO turno, los ataques de este Pokémon hacen [amount] puntos de daño
     * más al Activo rival (antes de aplicar Debilidad y Resistencia)" — Golem ex Giro Dinámico,
     * Hitmonchan Puño Exaltado. Fija `attackBonusOnTurn = turn+2` / `attackBonusAmount = amount`
     * al Pokémon origen; el motor lo suma al daño base en el próximo turno propio. Auto-expira. */
    data class SelfAttackBonusNextTurn(val amount: Int) : EffectOp

    /**
     * "Al final del próximo turno de tu rival, pon [amount] puntos de daño en el objetivo"
     * (Victreebel — Ácido de Acción Lenta sv3pt5-71: 12 contadores = 120 daño al Defensor).
     * Fija [PokemonInPlay.delayedDamageOnTurn] = turno actual + 1 y [delayedDamageAmount] =
     * [amount] en el/los [target]; lo aplica [GameEngine.endTurn] cuando ese jugador termina
     * su turno (y [GameEngine.handleKnockouts] resuelve el posible KO). Auto-expira. */
    data class ScheduleDelayedDamage(val target: Target, val amount: Int) : EffectOp

    /**
     * Involuciona el Activo rival si es un Pokémon evolucionado (Aerodactyl — Rayo Involutivo
     * sv3pt5-142: "pon la carta de Evolución de fase más alta que tenga sobre él en la mano de
     * tu rival"). La carta superior de la pila de evolución vuelve a la mano de su dueño y el
     * Pokémon pasa a ser la carta inmediatamente inferior (conserva daño, Energía, Herramientas
     * y estados). Si no está evolucionado, no hace nada. El posible KO (si el daño ya supera los
     * PS de la fase inferior) lo resuelve el motor tras el ataque. */
    data object DeEvolveDefender : EffectOp

    /**
     * Devuelve [count] Energía(s) unida(s) al Activo RIVAL a la mano de su dueño
     * (Omanyte — Retorno Tentacular). Toma las primeras disponibles (cualquier copia
     * básica es equivalente). Si no hay Energía unida, no hace nada. */
    data class BounceOppActiveEnergyToHand(val count: Int) : EffectOp

    /**
     * Daño crudo al Activo rival = [damagePerCard] por cada carta de Entrenador (Partidario/
     * Objeto/Herramienta/Estadio) en la MANO del rival (Gengar — Poltergeist). El rival
     * "enseña" su mano; contamos los Entrenadores. Daño después de Debilidad/Resistencia
     * (crudo), como CoinFlipDamage. */
    data class DamagePerOppHandTrainer(val damagePerCard: Int) : EffectOp

    /**
     * Descarta las [count] primeras cartas de TU baraja y hace [damagePer] de daño crudo al
     * Activo rival por cada Pokémon con un Coste de Retirada EXACTO de [retreatEquals] que
     * hayas descartado así (Onix — Alud Descomunal: 5 cartas, 80 por Pokémon con retiro 4).
     * Las cartas descartadas van a tu descarte; emite [GameEvent.CardsDiscarded]. */
    data class DiscardTopDeckDamagePerRetreat(
        val count: Int,
        val retreatEquals: Int,
        val damagePer: Int,
    ) : EffectOp

    /**
     * Descarta las [count] primeras cartas del mazo (mill). Si [own] es true, de TU baraja
     * (Dragonite — Pulso Drágon, 2 propias); si es false, de la baraja del rival
     * (Machop/Machoke/Kingler = 1, Machamp = 2). Cada carta va al descarte de su dueño.
     * Emite [GameEvent.CardsDiscarded] con el lado afectado. */
    data class DiscardTopDeck(val count: Int, val own: Boolean) : EffectOp

    /** El rival descarta [count] cartas de su mano (al azar, desde el frente). */
    data class OpponentDiscardsHand(val count: Int) : EffectOp

    /**
     * Marca a [target] para evitar TODO el daño de ataques durante el próximo turno
     * del rival (Refugio/Postura Defensiva/Vuelo). Si [coinFlip] es true, solo surte
     * efecto si sale cara (emite el [GameEvent.CoinFlipped]). Si [onlyFromBasic] es
     * true, solo evita el daño de ataques de Pokémon BÁSICOS (Nidoqueen — Prensa Real:
     * fija [PokemonInPlay.preventBasicDamageOnTurn] en vez de [preventDamageOnTurn]).
     * Fija el turno actual + 1. */
    data class PreventDamageNextTurn(
        val target: Target,
        val coinFlip: Boolean = false,
        val onlyFromBasic: Boolean = false,
    ) : EffectOp

    /**
     * Descarta el Estadio que haya en juego (a la pila de descartes de SU dueño), si lo
     * hay (Charmander — Destrucción Abrasadora). No-op si no hay Estadio. */
    data object DiscardStadium : EffectOp

    /**
     * Noquea a [target] (daño crudo = sus PS impresos). Si [coinFlip] es true, solo con cara
     * (Weezing — A Pasarlo Bomba: cara → noquea al Atacante). No-op si no hay objetivo. */
    data class CoinFlipKnockOutTarget(val target: Target, val coinFlip: Boolean = true) : EffectOp

    /**
     * Programa un "reflejo de daño" en el Pokémon que usa este ataque (SELF): durante el
     * próximo turno del rival, si este Pokémon resulta dañado por un ataque (incluso si queda
     * Fuera de Combate), el Atacante recibe daño crudo igual al infligido a este Pokémon
     * (Mewtwo — Barrera Reflectante sv3pt5-150). Fija [PokemonInPlay.reflectDamageOnTurn] al
     * turno actual + 1; el disparo lo resuelve GameEngine (applyDefenderRetaliation). */
    data object ScheduleReflectDamageNextTurn : EffectOp

    /**
     * El Activo rival (Defensor) no podrá retirarse durante su próximo turno
     * (Amarrar/Rumble). Fija [PokemonInPlay.cannotRetreatOnTurn] = turno actual + 1. */
    data object DefenderCannotRetreatNextTurn : EffectOp

    /**
     * "Durante el próximo turno de tu rival, el Coste de Retirada del Pokémon Defensor es de
     * [amount] {C} más" — Grimer Presión Pegajosa, Muk Prisión Viscosa. Fija
     * `retreatCostBumpOnTurn = turn+1` al Activo rival. Auto-expira. */
    data class BumpDefenderRetreatCostNextTurn(val amount: Int) : EffectOp

    /**
     * "Durante el próximo turno de tu rival, los ataques usados por el Pokémon Defensor cuestan
     * [amount] {C} más" — Muk Prisión Viscosa. Fija `attackCostBumpOnTurn = turn+1` al Activo
     * rival. Auto-expira. */
    data class BumpDefenderAttackCostNextTurn(val amount: Int) : EffectOp

    /** "Durante el próximo turno de tu rival, el Pokémon Defensor no puede atacar"
     * (Traba-Lengua). Fija [PokemonInPlay.cannotAttackOnTurn] del Activo rival = turno
     * actual + 1 (su próximo turno). */
    data object DefenderCannotAttackNextTurn : EffectOp

    /**
     * Carisma de Giovanni: devuelve 1 Energía del Activo RIVAL a la mano de su dueño
     * y, si lo haces, une 1 Energía de TU mano a tu Activo. Op dedicada (dos pasos
     * acoplados por "si lo haces"). Determinista: toma la primera Energía disponible
     * en cada caso (cualquier copia básica es equivalente). */
    data object GiovanniCharisma : EffectOp

    /**
     * Busca en tu mazo hasta [count] Energía Básica de tipo [energyType] y únela a
     * este Pokémon; luego baraja (Charge/Salt Water). Como cualquier copia de la
     * energía básica es equivalente, se toma de forma determinista (no abre elección).
     * Si [coinFlip] es true, solo ocurre con cara. */
    data class SearchEnergyAttachSelf(
        val energyType: EnergyType,
        val count: Int,
        val coinFlip: Boolean = false,
    ) : EffectOp

    /**
     * Si [target] tiene el estado [status], queda Fuera de Combate (Jynx ex — Beso
     * de Infarto: si el Activo rival está Dormido, queda KO). Se modela infligiendo
     * daño igual a sus PS impresos (daño CRUDO, sin Debilidad/Resistencia); el motor
     * resuelve el KO y los premios. Si no tiene el estado, no hace nada. */
    data class KoIfStatus(val target: Target, val status: Status) : EffectOp
}

/** Condición para un término de daño de ataque, evaluada contra el estado. */
sealed interface DamageCondition {
    /** Siempre suma. */
    data object Always : DamageCondition
    /** Suma solo si el Activo rival (defensor) es un Pokémon de Evolución. */
    data object IfDefenderEvolved : DamageCondition
    /** Suma solo si el Activo rival ya tiene algún contador de daño. */
    data object IfDefenderHasDamage : DamageCondition
    /** Suma solo si el Pokémon atacante ya tiene algún contador de daño. */
    data object IfSelfHasDamage : DamageCondition
    /** Suma solo si el atacante no tiene cartas en la mano. */
    data object IfEmptyHand : DamageCondition
    /** Suma solo si el Activo rival es del tipo [type]. */
    data class IfDefenderType(val type: EnergyType) : DamageCondition
    /** Suma solo si el Activo rival es un Pokémon ex o un Pokémon V (Full Fighting). */
    data object IfDefenderExOrV : DamageCondition
    /** Suma solo si tienes la MISMA cantidad de cartas en la mano que tu rival
     * (Ninetales ex — Llamas Reflejadas). */
    data object IfSameHandSizeAsOpponent : DamageCondition
    /** Suma solo si te quedan MÁS cartas de Premio que a tu rival (Pinsir — Lanzamiento Audaz). */
    data object IfMorePrizesThanOpponent : DamageCondition
    /** Suma solo si tienes en TU Banca un Pokémon cuyo nombre (ES o EN) contiene [name]
     * (Electabuzz — Combo Eléctrico [Magmar], Magmar — Combo Flamígero [Electabuzz]). */
    data class IfSelfBenchHasName(val name: String) : DamageCondition
    /** Suma solo si jugaste una carta de Apoyo (Partidario) de tu mano este turno
     * (Wigglytuff ex — Placaje Amigo). */
    data object IfSupporterPlayedThisTurn : DamageCondition
    /** Suma solo si jugaste este turno una carta de Entrenador cuyo nombre (ES o EN)
     * contiene [name] (Rhydon — Taladro Carismático ["Giovanni"], Tangela — Enredo
     * Sutil ["Erika"]). */
    data class IfPlayedTrainerThisTurn(val name: String) : DamageCondition
}

/**
 * Término de daño de un ataque, evaluado ANTES de Debilidad/Resistencia (a
 * diferencia de [EffectOp.ExtraDamage], que aplica daño crudo después). Modela
 * los ataques "X+" con bonus condicional (p.ej. Cross-Cut): el total pasa por
 * Debilidad/Resistencia una sola vez, exacto al reglamento.
 */
data class DamageTerm(
    val amount: Int,
    val condition: DamageCondition = DamageCondition.Always,
    /**
     * Daño extra = [perDefenderCounter] por cada contador de daño en el Activo rival,
     * medido ANTES de aplicar el daño base de este ataque (Rattata — Roer la Herida = 10,
     * Raticate — Segundo Mordisco = 30). Se evalúa aquí, en el cálculo de daño de attack(),
     * NO con ExtraDamage (que se aplicaría después del daño base y contaría de más). */
    val perDefenderCounter: Int = 0,
)

/**
 * Programa de efecto completo asociado a un ataque, habilidad o carta jugable.
 * `oncePerTurn`/`activeOnly` aplican a habilidades.
 */
data class Effect(
    val ops: List<EffectOp> = emptyList(),
    val passives: List<PassiveModifier> = emptyList(),
    val oncePerTurn: Boolean = false,
    val activeOnly: Boolean = false,
    /**
     * Daño de ataque autorado (para ataques "X+" con bonus condicional). Si NO
     * está vacío, el motor usa la suma de los términos aplicables como daño base
     * (sustituye al `baseDamage` de la carta, que es Variable) y le aplica
     * Debilidad/Resistencia. Vacío = el daño lo da `Attack.baseDamage`. */
    val attackDamage: List<DamageTerm> = emptyList(),
    /**
     * Si es true, la carta solo puede jugarse cuando alguno de TUS Pokémon fue
     * Noqueado durante el último turno del rival (Mela). El motor lo comprueba
     * contra [GameState.koedLastOppTurn] en `playTrainer`/`legalIntents`. */
    val requiresOwnKoLastTurn: Boolean = false,
    /**
     * Si es true, el daño de este ataque NO se ve afectado por la Debilidad del
     * Pokémon Defensor (Staryu — Meteoros sv3pt5-120). */
    val ignoresWeakness: Boolean = false,
    /**
     * Si es true, el daño de este ataque NO se ve afectado por la Resistencia del
     * Pokémon Defensor (Golem ex — Explosión Roca sv3pt5-76, Staryu — Meteoros). */
    val ignoresResistance: Boolean = false,
    /**
     * Si es true, el daño de este ataque ignora TODOS los efectos en el Activo rival
     * (prevención/reducción de daño, Herramientas). Staryu — Meteoros sv3pt5-120. */
    val ignoresDefenderEffects: Boolean = false,
    /**
     * Si es true, esta habilidad NO se activa manualmente (no aparece en `legalIntents`
     * ni se acepta vía `UseAbility`): el motor la dispara AUTOMÁTICAMENTE al jugar este
     * Pokémon desde la mano para EVOLUCIONAR a uno de los tuyos (Gyarados — Indomable
     * sv3pt5-130: descarta las 5 primeras cartas de tu baraja; Hypno — Toma Hipnosis
     * sv3pt5-97: deja Dormido al Activo rival). Solo ops NO interactivas por ahora. */
    val triggerOnEvolve: Boolean = false,
    /**
     * Si es true, esta habilidad NO se activa manualmente: el motor la dispara
     * AUTOMÁTICAMENTE cuando este Pokémon está en el Puesto Activo y resulta DAÑADO por un
     * ataque rival (incluso si queda Fuera de Combate) — Hitmonchan *Contragolpe* sv3pt5-107:
     * pon 3 contadores en el Atacante. Las ops se ejecutan con `actingSide` = el lado del
     * DEFENSOR, así que `Target.OPP_ACTIVE` apunta al Pokémon ATACANTE. */
    val triggerOnActiveDamaged: Boolean = false,
    /**
     * Como [triggerOnActiveDamaged] pero solo si este Pokémon queda Fuera de Combate por el
     * daño del ataque (Weezing *A Pasarlo Bomba* sv3pt5-110: moneda → cara noquea al Atacante).
     * Mismo convenio de objetivos (OPP_ACTIVE = Atacante). */
    val triggerOnActiveKO: Boolean = false,
    /**
     * Habilidad "evitar KO con moneda" (Machamp — Agallas sv3pt5-68): si este Pokémon fuese a
     * quedar Fuera de Combate por el daño de un ATAQUE, lanza 1 moneda; con cara NO queda KO y
     * sus PS restantes pasan a 10. La resuelve [GameEngine.handleKnockouts] (solo `byAttack`). */
    val survivesKoWithCoin: Boolean = false,
    /**
     * Habilidad "absorber Energía de un aliado Noqueado" (Raichu — Toma de Tierra sv3pt5-26):
     * cuando uno de tus Pokémon queda Fuera de Combate por el daño de un ataque RIVAL, mueve 1
     * Energía Básica de este tipo del Pokémon Noqueado a este Pokémon (que está en la Banca). La
     * resuelve [GameEngine.handleKnockouts] antes de mandar la Energía al descarte. null = no aplica. */
    val pullsEnergyFromKoAllyType: EnergyType? = null,
    /**
     * Habilidad-escudo pasiva (Mr. Mime — Barrera Mímica sv3pt5-122/-179): si este Pokémon está
     * en el Puesto Activo y él y el Activo rival tienen la MISMA cantidad de Energías unidas, se
     * evita todo el daño que le inflijan los ataques rivales. La comprueba [GameEngine.attack]
     * al calcular la prevención de daño (se salta si el ataque ignora los efectos del Defensor). */
    val preventsDamageIfEnergyParity: Boolean = false,
    /**
     * "Si este Pokémon NO está Confundido, este ataque no hace nada" (Primeape — Golpe Rabioso
     * sv3pt5-57). Si es true y el atacante no está Confundido, `GameEngine.attack` anula el daño
     * Y el efecto (el daño no es un efecto, pero "no hace nada" cubre ambos). */
    val noEffectUnlessSelfConfused: Boolean = false,
    /**
     * "Si este Pokémon ha evolucionado durante este turno, este ataque no hace nada" (Slowbro —
     * Placaje Relajado sv3pt5-80). Se detecta con `attacker.turnsInPlay == 0` (evolucionar/colocar
     * resetea el contador; un Fase 1 en el Activo con 0 turnos evolucionó este turno). */
    val noEffectIfEvolvedThisTurn: Boolean = false,
    /**
     * "Si un Pokémon rival queda Fuera de Combate por el daño de ESTE ataque, coge 1 Premio más"
     * (Clefable — Más Luna sv3pt5-36). `GameEngine.attack` pasa 1 premio extra a handleKnockouts
     * del Defensor cuando este ataque lo noquea. */
    val extraPrizeIfKo: Boolean = false,
    /**
     * Habilidad pasiva: si tienes en juego (Activo o Banca) un Pokémon cuyo nombre (ES o EN)
     * contiene [freeAttackIfAllyNamed], los ataques de este Pokémon no cuestan Energía
     * (Nidoking — Rey Entusiasta sv3pt5-34: "si tienes a Nidoqueen en juego, ignora todas las
     * Energías en el coste de los ataques usados por este Pokémon"). La comprueba
     * [GameEngine.effectiveAttackCost] (respeta el bloqueo de Habilidades). null = no aplica. */
    val freeAttackIfAllyNamed: String? = null,
    /**
     * Habilidad pasiva de un Pokémon en Banca: los ataques de tus Pokémon cuyo nombre (ES o EN)
     * contiene [boostAlliedAttackerNamed] hacen [boostAlliedAttackerAmount] puntos MÁS al Activo
     * rival, ANTES de aplicar Debilidad y Resistencia (Cubone — Ovación Ósea sv3pt5-104: "mientras
     * este Pokémon esté en tu Banca, los ataques usados por tus Marowak hacen 30 más"). La suma
     * [GameEngine.attack] recorriendo la Banca propia (respeta el bloqueo). null = no aplica. */
    val boostAlliedAttackerNamed: String? = null,
    val boostAlliedAttackerAmount: Int = 0,
    /**
     * Herramienta que cura a su portador AL FINAL de TU turno si está en el Puesto Activo
     * (Restos sv3pt5-163: "al final de tu turno, si el Pokémon al que está unida esta carta
     * está en el Puesto Activo, cúrale 20 puntos de daño"). 0 = no aplica. Lo procesa
     * [GameEngine.endTurn] sobre el Activo del jugador que termina su turno. */
    val healSelfEndOfTurnIfActive: Int = 0,
)

/**
 * Registro central id → efecto. Se irá poblando carta por carta (Fase 7 del
 * roadmap). Vacío de inicio: el motor cae a comportamiento por defecto cuando
 * un [EffectId] no está registrado.
 */
class EffectRegistry(private val byId: Map<EffectId, Effect> = emptyMap()) {
    operator fun get(id: EffectId?): Effect? = id?.let { byId[it] }
    fun has(id: EffectId): Boolean = byId.containsKey(id)
    val size: Int get() = byId.size

    companion object {
        val EMPTY = EffectRegistry()
    }
}
