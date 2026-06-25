package com.mineralord.tcg.engine.rules

import kotlin.random.Random

/**
 * Fuente de aleatoriedad del motor. Abstraída para que las partidas sean
 * **reproducibles**: con la misma semilla, la misma secuencia de barajados y
 * lanzamientos de moneda. Imprescindible para tests deterministas y para el
 * arnés de self-play headless (IA vs IA) en CI.
 */
interface Rng {
    /** true = cara. */
    fun flipCoin(): Boolean
    fun <T> shuffle(list: List<T>): List<T>
    fun nextInt(untilExclusive: Int): Int
}

/** Implementación sembrada sobre [kotlin.random.Random]. */
class SeededRng(seed: Long) : Rng {
    private val random = Random(seed)
    override fun flipCoin(): Boolean = random.nextBoolean()
    override fun <T> shuffle(list: List<T>): List<T> = list.shuffled(random)
    override fun nextInt(untilExclusive: Int): Int = random.nextInt(untilExclusive)
}
