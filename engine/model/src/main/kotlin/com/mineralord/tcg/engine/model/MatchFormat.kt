package com.mineralord.tcg.engine.model

/**
 * Formato de una partida (PvP/PvE). En el Pokémon TCG real el formato NO cambia el motor de reglas:
 * ambos usan el MISMO ruleset vivo y solo difieren en el **pool de cartas legales** (Standard = sets
 * recientes con rotación; Expanded = Black & White en adelante). Modelamos esa verdad: el formato es
 * un **filtro de pool**, no un motor distinto.
 *
 * - [Expanded]: todas las expansiones del catálogo son legales.
 * - [Standard]: solo las cartas de una [series] (era) concreta. Es el "Estándar por selección de
 *   ERA/SERIE" que pidió el diseño: eliges una era y juegas con su pool (y, el día que existan, con
 *   su [RuleProfile]).
 */
sealed interface MatchFormat {
    data object Expanded : MatchFormat
    data class Standard(val series: String) : MatchFormat
}

/** Serializa un formato a un String estable (para la sala de Firestore / persistencia). */
fun encodeMatchFormat(format: MatchFormat): String = when (format) {
    MatchFormat.Expanded -> "EXPANDED"
    is MatchFormat.Standard -> "STANDARD:${format.series}"
}

/** Inverso de [encodeMatchFormat]. Vacío/desconocido → Expandido (el más permisivo). */
fun decodeMatchFormat(s: String?): MatchFormat = when {
    s.isNullOrBlank() || s == "EXPANDED" -> MatchFormat.Expanded
    s.startsWith("STANDARD:") -> MatchFormat.Standard(s.removePrefix("STANDARD:"))
    else -> MatchFormat.Expanded
}

/**
 * Legalidad de carta por formato. Puro y testeable: decide con [SetInfo.series], que toda carta
 * conoce. No toca el motor de combate (el motor juega las cartas que haya; la legalidad se valida al
 * construir la baraja y al entrar a la partida).
 */
object FormatLegality {

    fun isLegal(set: SetInfo, format: MatchFormat): Boolean = when (format) {
        MatchFormat.Expanded -> true
        is MatchFormat.Standard -> set.series.equals(format.series, ignoreCase = true)
    }

    fun isLegal(card: Card, format: MatchFormat): Boolean =
        // La Energía Básica es legal en TODOS los formatos (no pertenece al pool rotable de ninguna
        // serie); el resto se decide por la serie de su expansión.
        card is BasicEnergy || isLegal(card.set, format)

    /** Cartas de [cards] que NO son legales en [format] (para marcar/rechazar la baraja). */
    fun illegalCards(cards: List<Card>, format: MatchFormat): List<CardId> =
        cards.filterNot { isLegal(it, format) }.map { it.id }

    /** ¿Toda la baraja es legal en [format]? */
    fun isDeckLegal(cards: List<Card>, format: MatchFormat): Boolean =
        cards.all { isLegal(it, format) }
}

/**
 * Cómo se calcula la Debilidad según la era (seam para las series clásicas futuras):
 * - [TIMES_TWO]: ×2 (moderno, y también Black & White en adelante).
 * - [ADDITIVE]: +X fijo (era Diamond & Pearl / HGSS).
 */
enum class WeaknessMode { TIMES_TWO, ADDITIVE }

/**
 * Perfil de reglas de una ERA. El motor es ÚNICO y se parametriza con este perfil pequeño en los
 * puntos donde las eras divergen; así añadir una era clásica es registrar un perfil nuevo y
 * enchufarlo, NO duplicar el motor (evita la explosión combinatoria de "un rulebook por era").
 *
 * Hoy solo existe [RuleProfiles.Modern]; todos los campos reflejan la era moderna (Scarlet & Violet
 * y Megaevolución). Los flags quedan documentados para cuando lleguen las series pre-BW.
 */
data class RuleProfile(
    val id: String,
    /** Cálculo de Debilidad. Moderno = ×2. */
    val weakness: WeaknessMode = WeaknessMode.TIMES_TWO,
    /** Resistencia fija restada (moderno = −30). */
    val resistance: Int = 30,
    /**
     * Si las "Poké-Power/Poké-Body" (habilidades pre-BW) se DESACTIVAN cuando el Pokémon tiene una
     * Condición Especial. Moderno (Abilities) = false: no se desactivan. */
    val abilitiesDisabledBySpecialCondition: Boolean = false,
)

/**
 * Registro de perfiles por era. [forSeries] devuelve el perfil activo para una serie; hoy todas las
 * series del catálogo son modernas, así que siempre es [Modern] (comportamiento idéntico al actual).
 * El día que se añada una serie pre-BW se registra aquí su perfil y el motor lo consultará.
 */
object RuleProfiles {
    val Modern = RuleProfile(id = "modern")

    fun forSeries(series: String): RuleProfile = Modern

    /**
     * Perfil activo de una partida. Expanded siempre usa el ruleset moderno (es BW en adelante);
     * Standard usa el perfil de la era seleccionada.
     */
    fun forFormat(format: MatchFormat): RuleProfile = when (format) {
        MatchFormat.Expanded -> Modern
        is MatchFormat.Standard -> forSeries(format.series)
    }
}
