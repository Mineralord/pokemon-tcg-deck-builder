package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.Rarity

/**
 * Política de FABRICACIÓN por rareza: las **DOS rarezas más altas de cada expansión** (las "chase")
 * **no se pueden fabricar con fichas**. Se deben conseguir abriendo sobres.
 *
 * Es automática y a prueba de futuras expansiones: calcula, del catálogo real, las dos rarezas tope
 * presentes en cada set y prohíbe fabricar las cartas que las tengan. Para no bloquear sets sin
 * rareza secreta (p. ej. energías básicas, cuyo tope es Común), solo cuentan las rarezas que
 * alcanzan al menos [RARE_FLOOR] (Ultra Rara), que es donde empiezan las cartas "chase".
 */
object CraftPolicy {

    /** A partir de esta rareza (inclusive) una expansión tiene cartas "chase" bloqueables. */
    private val RARE_FLOOR = Rarity.ULTRA_RARE.ordinal

    /** Nº de rarezas tope (las más altas) que quedan bloqueadas por expansión. */
    private const val TOP_N = 2

    /** ¿Cuenta esta rareza como nivel "chase" rankeable? Excluye PROMO (es una categoría, no un nivel
     *  de rareza, pese a tener el ordinal más alto en el enum) y todo lo que no llega a [RARE_FLOOR]. */
    private fun isRankable(ordinal: Int): Boolean =
        ordinal >= RARE_FLOOR && ordinal != Rarity.PROMO.ordinal

    /**
     * Ordinales de rareza bloqueados por expansión: las [TOP_N] rarezas más altas PRESENTES en el set,
     * contando solo rarezas "rankeables" ([isRankable]). Calculado una vez del catálogo.
     */
    private val blockedRaritiesBySet: Map<String, Set<Int>> by lazy {
        runCatching {
            CardRepository.load().all
                .groupBy { it.set.code }
                .mapValues { (_, cards) ->
                    cards.map { it.rarity.ordinal }
                        .filter { isRankable(it) }
                        .distinct()
                        .sortedDescending()
                        .take(TOP_N)
                        .toSet()
                }
        }.getOrDefault(emptyMap())
    }

    /** ¿Está [card] entre las dos rarezas más altas (chase) de su expansión? → no fabricable. */
    fun isTopRarityOfSet(card: Card): Boolean =
        card.rarity.ordinal in (blockedRaritiesBySet[card.set.code] ?: emptySet())

    /** ¿Se puede FABRICAR [card] con fichas? False para las dos rarezas más altas de cada expansión. */
    fun isCraftable(card: Card): Boolean = !isTopRarityOfSet(card)
}
