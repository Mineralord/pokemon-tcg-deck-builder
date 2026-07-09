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
         * "Si lo haces, roba hasta tener N cartas" (Mela): tras resolver, si se
         * unió al menos 1 Energía, roba hasta [thenDrawUpTo]. null = no roba. */
        val thenDrawUpTo: Int? = null,
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
