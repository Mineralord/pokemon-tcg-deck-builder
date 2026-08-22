package com.mineralord.tcg.engine.model

/**
 * Decisión que el motor NO puede resolver solo: requiere que el jugador (o la
 * IA) elija. El intérprete pausa el efecto, emite una [PendingDecision] (dentro
 * de [PendingInteraction]) y, al recibir la respuesta, continúa. Así se garantiza
 * que **toda** resolución es explícita y automatizable — nunca queda una acción
 * "a mano" sin definir.
 *
 * `candidates` ya viene filtrada/calculada por el intérprete, de modo que la IA
 * solo tiene que escoger entre opciones legales.
 *
 * Vive en `:engine:model` (y no en `:engine:effects`) para que [GameState] pueda
 * contener la interacción en curso sin invertir la dirección de dependencias.
 */
sealed interface PendingDecision {
    val side: Side
    val prompt: LocalizedText

    /** Elegir N Pokémon objetivo entre [candidates]. */
    data class ChooseTargets(
        override val side: Side,
        override val prompt: LocalizedText,
        val candidates: List<CardId>,
        val count: Int,
    ) : PendingDecision

    /** Buscar en una zona cartas que cumplan [filter] y llevarlas a [destination]. */
    data class SearchCards(
        override val side: Side,
        override val prompt: LocalizedText,
        val from: Zone,
        val filter: CardFilter,
        val destination: Zone,
        val count: Int,
        val candidates: List<CardId>,
        /**
         * Si es true, las cartas elegidas deben ser de tipos DISTINTOS entre sí
         * (Eevee — Amigos Coloridos). El intérprete (host autoritativo) recorta la
         * selección a un Pokémon por tipo al resolver. */
        val distinctTypes: Boolean = false,
        /**
         * >0 solo para la mecánica "descarta de la mano y haz daño por carta"
         * (Blastoise ex — Cañones Gemelos): al resolver (from=HAND, destination=DISCARD),
         * el Activo rival recibe este daño CRUDO por cada carta descartada. */
        val damagePerDiscardToOppActive: Int = 0,
        /**
         * true solo para "descarta Herramientas de TUS Pokémon y haz daño por cada una"
         * (Electrode — Cadena Bum Bum sv3pt5-101). Los [candidates] son las Herramientas
         * enganchadas a los Pokémon propios (Activo + Banca), no cartas de una zona; al
         * resolver, cada Herramienta elegida se retira de su portador → descarte del dueño
         * y el Activo rival recibe [damagePerDiscardToOppActive] CRUDO por cada una. Los
         * campos [from]/[destination] se ignoran cuando esto es true. */
        val fromAttachedTools: Boolean = false,
        /**
         * true solo para las cartas que operan sobre la MANO del RIVAL (Agarrador Mecánico
         * sv3pt5-162: "tu rival enseña su mano y pones 1 Pokémon en el fondo de su baraja").
         * Los [candidates] son cartas de la mano del rival (no del lado que decide); al resolver,
         * la carta elegida sale de la mano del rival y va al FONDO de su baraja ([destination]
         * se interpreta como esa zona rival). [side] sigue siendo el jugador que decide. */
        val fromOpponentHand: Boolean = false,
        /**
         * true solo para Invitación de Erika (sv3pt5-160): el Básico elegido de la mano del rival
         * se pone en SU Banca y acto seguido se cambia al Puesto Activo (el Activo anterior baja a
         * la Banca). Implica [fromOpponentHand] = true. */
        val switchOppActive: Boolean = false,
        /**
         * true = la búsqueda opera sobre las zonas del RIVAL ([from] y [destination] se leen en el
         * lado contrario a [side]): Haunter — Regreso Espiritual (descarte rival → mano rival). El
         * host es autoritativo; para el invitado (netplay) es render-only. */
        val onOpponent: Boolean = false,
        /**
         * true solo para Ditto — Inicio Transformador (sv3pt5-132): al resolver (from=DECK,
         * destination=ACTIVE, count=1), el Activo actual de [side] y TODAS sus cartas unidas
         * (energías, Herramientas y la pila de evolución) van al descarte, y el Pokémon Básico
         * elegido pasa a ser el nuevo Activo; después se baraja el mazo. */
        val replaceActiveWithSource: Boolean = false,
    ) : PendingDecision

    /**
     * Lanzar una moneda de forma INTERACTIVA (el jugador la "tira") y, según el
     * resultado, robar [ifHeads] cartas (cara) o [ifTails] (cruz) — La Dominguera/
     * Picnicker y similares. No hay candidatos: se resuelve con `chosen` vacío; el
     * lanzamiento real (autoritativo) lo hace el motor al resolver, emitiendo
     * [GameEvent.CoinFlipped] para la animación, y luego roba. Fiel a TCG Live:
     * primero se tira la moneda, DESPUÉS ocurre el efecto.
     */
    data class CoinFlip(
        override val side: Side,
        override val prompt: LocalizedText,
        val ifHeads: Int,
        val ifTails: Int,
    ) : PendingDecision

    /**
     * "Tira" [flips] monedas de forma interactiva y busca en el mazo HASTA (nº de caras)
     * Pokémon que casen [filter] → Banca (Parasect — Filamentos Dispersos). No hay
     * candidatos aquí: se resuelve con `chosen` vacío; el motor lanza las monedas
     * (autoritativo, emite [GameEvent.CoinFlipped]) y ENCADENA una [SearchCards] a la
     * Banca con `count = caras` (si hubo ≥1 cara y hay candidatos). Fiel a TCG Live:
     * primero la moneda, después la búsqueda. Netplay: viaja como COIN_FLIP (render-only).
     */
    data class CoinFlipThenSearch(
        override val side: Side,
        override val prompt: LocalizedText,
        val flips: Int,
        val filter: CardFilter,
        val searchPrompt: LocalizedText,
    ) : PendingDecision

    /**
     * Repartir [count] contadores de daño (10 c/u) entre [candidates] "como quieras"
     * (Embestida Hueca / Psicopoder). Resolución: la lista `chosen` repite el [CardId]
     * de cada Pokémon UNA vez por contador que recibe (p.ej. 2 en A y 1 en B → [A, A, B]).
     * El motor aplica 10 de daño por cada aparición (máx. [count] contadores en total).
     */
    data class PlaceCounters(
        override val side: Side,
        override val prompt: LocalizedText,
        val candidates: List<CardId>,
        val count: Int,
    ) : PendingDecision

    /** Mover N energías de un Pokémon a otro. */
    data class MoveEnergy(
        override val side: Side,
        override val prompt: LocalizedText,
        val fromCandidates: List<CardId>,
        val toCandidates: List<CardId>,
        val count: Int,
    ) : PendingDecision

    /**
     * Se han revelado las [revealed] primeras cartas del mazo; el jugador puede unir
     * hasta [maxAttach] de las [energyCandidates] (Energías elegibles reveladas) a los
     * [benchCandidates] (Pokémon de Banca elegibles), como quiera. El resto se baraja
     * de vuelta al resolver (Generador Eléctrico).
     *
     * Resolución: la lista `chosen` codifica PARES intercalados
     * `[energía, destino, energía, destino, …]` (la UI los arma al arrastrar).
     */
    /**
     * Elegir 1 tipo de Energía entre [candidates] (Porygon — Conversión 4). Se resuelve por el
     * canal estándar `chosen: List<CardId>` con UN [CardId] centinela codificado por
     * [encodeType]/[decodeType] (no es una carta real). El intérprete decodifica el tipo elegido y
     * fija la Debilidad-override del Activo rival.
     */
    data class ChooseEnergyType(
        override val side: Side,
        override val prompt: LocalizedText,
        val candidates: List<EnergyType>,
    ) : PendingDecision {
        val candidateIds: List<CardId> get() = candidates.map { encodeType(it) }
    }

    /**
     * Elegir 1 de los ataques del Activo RIVAL para usarlo como propio (Mew ex — Hackeo Genómico).
     * Cada ataque se identifica por su ÍNDICE, codificado como CardId centinela; [attackNames] son
     * los nombres (ES) en el mismo orden para que la UI los muestre. [fromPokemon] es el Activo rival
     * cuyos ataques se ofrecen. Lo resuelve el MOTOR (no el intérprete): re-ejecuta el ataque elegido.
     */
    data class ChooseAttack(
        override val side: Side,
        override val prompt: LocalizedText,
        val fromPokemon: CardId,
        val attackNames: List<String>,
    ) : PendingDecision {
        val candidateIds: List<CardId> get() = attackNames.indices.map { encodeAttackIndex(it) }
    }

    companion object {
        /** Centinela CardId para transportar un [EnergyType] por el canal `chosen`. */
        fun encodeType(type: EnergyType): CardId = CardId("energytype:${type.name}")
        fun decodeType(id: CardId): EnergyType? =
            id.raw.removePrefix("energytype:").let { name -> EnergyType.entries.firstOrNull { it.name == name } }

        /** Centinela CardId para transportar el ÍNDICE del ataque elegido (Hackeo Genómico). */
        fun encodeAttackIndex(i: Int): CardId = CardId("attackidx:$i")
        fun decodeAttackIndex(id: CardId): Int? =
            if (id.raw.startsWith("attackidx:")) id.raw.removePrefix("attackidx:").toIntOrNull() else null
    }

    data class AttachFromRevealed(
        override val side: Side,
        override val prompt: LocalizedText,
        val revealed: List<CardId>,
        val energyCandidates: List<CardId>,
        val benchCandidates: List<CardId>,
        val maxAttach: Int,
        /**
         * false = las Energías salen del MAZO y el resto se baraja de vuelta
         * (Generador Eléctrico). true = salen del DESCARTE, sin barajar
         * (Passionate Singing). En ambos casos [benchCandidates] son los Pokémon
         * propios elegibles (Banca y/o Activo). */
        val fromDiscard: Boolean = false,
        /**
         * "Si lo haces, roba hasta tener N cartas" (Melo): tras resolver, si se
         * unió al menos 1 Energía, roba hasta [thenDrawUpTo]. null = no roba. */
        val thenDrawUpTo: Int? = null,
        /**
         * Si es true, el enganche se juega a una moneda: al resolver, el motor lanza 1
         * moneda y solo une la Energía elegida si sale cara (Pegatinas de Energía
         * sv3pt5-159). La elección (energía + Pokémon) se hace antes de la tirada; el
         * resultado es idéntico. */
        val coinFlip: Boolean = false,
    ) : PendingDecision
}

/**
 * Efecto en pausa esperando una elección. Guarda la [decision] visible para
 * UI/IA, las [remainingOps] que faltan por ejecutar (la "continuación") y de
 * quién es el efecto ([side]/[sourceId], para resolver SELF/recoil y bindings).
 *
 * [endsTurnOnResolve] = true cuando la interacción proviene de un ataque: el
 * turno se cerrará automáticamente al vaciar la cadena de decisiones.
 */
data class PendingInteraction(
    val decision: PendingDecision,
    val remainingOps: List<EffectOp>,
    val side: Side,
    val sourceId: CardId?,
    val endsTurnOnResolve: Boolean = false,
)
