package com.mineralord.tcg.engine.model

/**
 * Estado de juego — todo inmutable. El motor (`:engine:rules`) toma un
 * [GameState] y un intent, y devuelve un nuevo [GameState] + lista de eventos.
 * Nada aquí muta in-place: cada transición produce una copia (`copy`), lo que
 * hace el motor determinista, depurable y trivialmente serializable para
 * guardar/reproducir partidas.
 */

/** Identifica a uno de los dos lados de la mesa. */
enum class Side { PLAYER, OPPONENT }

/** Una carta de Pokémon puesta en juego, con su estado dinámico. */
data class PokemonInPlay(
    val card: PokemonCard,
    val damage: Int = 0,                      // daño acumulado (no HP restante)
    val attachedEnergy: List<EnergyCard> = emptyList(),
    val attachedTools: List<TrainerCard> = emptyList(),
    val statuses: Set<Status> = emptySet(),
    /** Pila de evolución debajo de esta carta (de más antigua a más reciente). */
    val evolutionStack: List<PokemonCard> = emptyList(),
    val turnsInPlay: Int = 0,
    /**
     * Si no es null, número de turno en el que este Pokémon NO puede atacar
     * (Jet Wing y similares: "durante tu próximo turno, este Pokémon no puede
     * atacar"). Se compara contra [GameState.turn] en el motor.
     */
    val cannotAttackOnTurn: Int? = null,
    /**
     * Si no es null, número de turno durante el cual, si este Pokémon intenta atacar, su
     * dueño primero lanza [flipsToAttackCount] monedas; si sale cruz en ALGUNA, el ataque
     * "no se lleva a cabo" (Seadra — Tinta Cegadora sv3pt5-117: "durante el próximo turno de
     * tu rival, si el Defensor intenta usar un ataque, tu rival lanza 2 monedas…"). Se fija a
     * [GameState.turn] + 1; lo comprueba [GameEngine.attack]. Auto-expira.
     */
    val flipsToAttackOnTurn: Int? = null,
    val flipsToAttackCount: Int = 0,
    /**
     * Si no es null, número de turno durante el cual se EVITA todo el daño de
     * ataques a este Pokémon (Refugio/Postura Defensiva/Vuelo: "durante el próximo
     * turno de tu rival, se evita todo el daño…"). Se fija a [GameState.turn] + 1 y
     * se compara contra [GameState.turn] al recibir daño. Auto-expira.
     */
    val preventDamageOnTurn: Int? = null,
    /**
     * Si no es null, número de turno durante el cual se EVITA el daño a este Pokémon SOLO
     * de ataques de Pokémon BÁSICOS (Nidoqueen — Prensa Real: "durante el próximo turno de
     * tu rival, se evita todo el daño infligido a este Pokémon por ataques de Pokémon
     * Básicos"). Se fija a [GameState.turn] + 1; se comprueba en [GameEngine.attack] mirando
     * si el atacante es Básico. Auto-expira. Es independiente de [preventDamageOnTurn].
     */
    val preventBasicDamageOnTurn: Int? = null,
    /**
     * Si no es null, número de turno durante el cual este Pokémon "refleja" el daño: si
     * resulta dañado por un ataque rival (incluso si queda Fuera de Combate), el Atacante
     * recibe daño crudo igual al infligido a este Pokémon (Mewtwo — Barrera Reflectante).
     * Se fija a [GameState.turn] + 1; lo dispara [GameEngine] en applyDefenderRetaliation.
     * Auto-expira.
     */
    val reflectDamageOnTurn: Int? = null,
    /**
     * Si no es null, número de turno durante el cual este Pokémon NO puede retirarse
     * (Amarrar/Rumble: "durante el próximo turno de tu rival, el Defensor no puede
     * retirarse"). Se fija a [GameState.turn] + 1 y se compara en el retiro.
     */
    val cannotRetreatOnTurn: Int? = null,
    /**
     * Si no es null, número de turno durante el cual los ataques hacen [damageReductionAmount]
     * puntos MENOS de daño a este Pokémon (Endurecimiento/Presión Caparazón/Carga Protectora:
     * "durante el próximo turno de tu rival, los ataques hacen X menos a este Pokémon, después
     * de aplicar Debilidad y Resistencia"). Se fija a [GameState.turn] + 1; se resta en
     * [GameEngine.attack] tras Debilidad/Resistencia (como la reducción de Herramientas). Auto-expira.
     */
    val damageReductionOnTurn: Int? = null,
    val damageReductionAmount: Int = 0,
    /**
     * Si no es null, número de turno durante el cual los ataques de ESTE Pokémon hacen
     * [attackBonusAmount] puntos MÁS de daño al Activo rival (Golem ex — Giro Dinámico,
     * Hitmonchan — Puño Exaltado: "durante tu próximo turno, los ataques de este Pokémon
     * hacen X más, antes de aplicar Debilidad y Resistencia"). Se fija a [GameState.turn] + 2
     * (el próximo turno PROPIO), se suma al daño base en [GameEngine.attack] ANTES de
     * Debilidad/Resistencia. Auto-expira.
     */
    val attackBonusOnTurn: Int? = null,
    val attackBonusAmount: Int = 0,
    /**
     * Si no es null, número de turno durante el cual el Coste de Retirada de este Pokémon
     * es [retreatCostBumpAmount] MÁS (Grimer — Presión Pegajosa, Muk — Prisión Viscosa:
     * "durante el próximo turno de tu rival, el Coste de Retirada del Defensor es de {C} más").
     * Se fija a [GameState.turn] + 1; se suma en `effectiveRetreatCost`. Auto-expira.
     */
    val retreatCostBumpOnTurn: Int? = null,
    val retreatCostBumpAmount: Int = 0,
    /**
     * Si no es null, número de turno durante el cual los ataques de este Pokémon cuestan
     * [attackCostBumpAmount] Energía {C} MÁS (Muk — Prisión Viscosa). Se fija a
     * [GameState.turn] + 1; se suma al coste convertido en `GameEngine.attack`/`legalIntents`.
     * Auto-expira.
     */
    val attackCostBumpOnTurn: Int? = null,
    val attackCostBumpAmount: Int = 0,
    /**
     * Si no es null, número de turno AL FINAL del cual se ponen [delayedDamageAmount] puntos
     * de daño en este Pokémon (Victreebel — Ácido de Acción Lenta: "al final del próximo turno
     * de tu rival, pon 12 contadores de daño en el Pokémon Defensor"). Se fija a
     * [GameState.turn] + 1 (el próximo turno del dueño de este Pokémon); lo aplica
     * [GameEngine.endTurn] cuando ese jugador termina su turno. Auto-expira.
     */
    val delayedDamageOnTurn: Int? = null,
    val delayedDamageAmount: Int = 0,
    /**
     * Si no es null, el TIPO de la Debilidad de este Pokémon queda sustituido por este tipo
     * (la CANTIDAD no cambia) hasta que deje el Puesto Activo (Porygon — Conversión 4 sv3pt5-137).
     * Lo fija el intérprete al resolver la elección de tipo y lo LEE [GameEngine.attack] al calcular
     * el daño (vía [Damage.calculate]). Se REINICIA (null) cuando el Pokémon pasa de Activo a Banca. */
    val weaknessOverrideType: EnergyType? = null,
) {
    val remainingHp: Int get() = (card.hp - damage).coerceAtLeast(0)
    val isKnockedOut: Boolean get() = damage >= card.hp
    val attachedEnergyCount: Int get() = attachedEnergy.size
}

/** Estado completo de un jugador. */
data class PlayerState(
    val side: Side,
    val active: PokemonInPlay?,
    val bench: List<PokemonInPlay> = emptyList(),
    val hand: List<Card> = emptyList(),
    val deck: List<Card> = emptyList(),
    val discard: List<Card> = emptyList(),
    val lostZone: List<Card> = emptyList(),
    val prizes: List<Card> = emptyList(),
    val prizesRemaining: Int = prizes.size,
) {
    /** Todos los Pokémon en juego (activo + banca), sin nulls. */
    val allInPlay: List<PokemonInPlay>
        get() = listOfNotNull(active) + bench
}

/** Fases del turno (modeladas como tipo para una máquina de estados explícita). */
enum class Phase { SETUP, DRAW, MAIN, ATTACK, BETWEEN_TURNS, GAME_OVER }

/** Estado raíz de una partida. */
data class GameState(
    val player: PlayerState,
    val opponent: PlayerState,
    val turn: Int,
    val activeSide: Side,
    val phase: Phase,
    val stadium: TrainerCard? = null,         // estadio en campo (único, compartido)
    val stadiumOwner: Side? = null,           // quién lo jugó (para descartarlo a SU pila al reemplazarlo)
    val winner: Side? = null,
    /**
     * Efecto en pausa esperando una elección (buscar/objetivo/mover energía).
     * Mientras no sea null, el motor solo acepta resolver la decisión.
     */
    val interaction: PendingInteraction? = null,
    /** Un Apoyo por turno: se pone a true al jugar uno; se resetea en fin de turno. */
    val supporterPlayedThisTurn: Boolean = false,
    /** Una energía por turno: se pone a true al unir una; se resetea en fin de turno. */
    val energyAttachedThisTurn: Boolean = false,
    /**
     * Nombres (ES y EN) de las cartas de Entrenador jugadas ESTE turno por el jugador
     * activo. Habilita ataques "si jugaste [tal carta] este turno, +X daño" (Rhydon —
     * Taladro Carismático [Carisma de Giovanni], Tangela — Enredo Sutil [Invitación de
     * Erika]). Se resetea en fin de turno. */
    val trainerNamesPlayedThisTurn: Set<String> = emptySet(),
    /** Habilidades 1/turno ya usadas este turno (por id de Pokémon). */
    val abilitiesUsedThisTurn: Set<CardId> = emptySet(),
    /**
     * Lados que tuvieron algún Pokémon Noqueado DURANTE el último turno del rival
     * (no en el propio, p.ej. por recoil). Habilita cartas condicionales como
     * Mela ("solo si te noquearon el turno pasado"). Se limpia para un lado
     * cuando termina SU turno (así refleja siempre el turno rival más reciente).
     */
    val koedLastOppTurn: Set<Side> = emptySet(),
    /**
     * Lados cuyo Pokémon Activo fue Noqueado y que DEBEN promover uno de su Banca al
     * puesto Activo antes de que el juego continúe. Fiel a TCG Live: el jugador ELIGE
     * su nuevo Activo (arrastrando la carta de la Banca al centro), no se auto-sube el
     * primero. Mientras no esté vacío, el motor solo acepta [GameIntent.PromoteActive]
     * de un lado que esté aquí; el resto de acciones se rechazan.
     */
    val pendingPromotion: Set<Side> = emptySet(),
    /**
     * Psyduck — Cavilar/Overthink: durante el próximo turno del rival, cada moneda que lance el
     * jugador [coinsAsTailsSide] mientras sea SU turno ([coinsAsTailsOnTurn]) se considera CRUZ.
     * Lo aplica el lanzamiento gateado de GameEngine; no persiste más allá de ese turno.
     */
    val coinsAsTailsSide: Side? = null,
    val coinsAsTailsOnTurn: Int? = null,
) {
    fun sideState(side: Side): PlayerState = if (side == Side.PLAYER) player else opponent
    val activePlayer: PlayerState get() = sideState(activeSide)
    val isOver: Boolean get() = phase == Phase.GAME_OVER || winner != null

    /** Hay una decisión pendiente que el jugador/IA debe resolver antes de seguir. */
    val awaitingDecision: Boolean get() = interaction != null

    /** Hay al menos un lado que debe elegir su nuevo Pokémon Activo tras un KO. */
    val awaitingPromotion: Boolean get() = pendingPromotion.isNotEmpty()
}
