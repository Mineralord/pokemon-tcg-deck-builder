package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonInPlay

/** Resultado de un cálculo de daño, con banderas para el log/animación. */
data class DamageResult(
    val finalAmount: Int,
    val weaknessApplied: Boolean,
    val resistanceApplied: Boolean,
)

/**
 * Cálculo de daño con Debilidad y Resistencia. Puro y sin estado: dados el
 * daño base, los tipos del atacante y el defensor, devuelve el daño final.
 *
 * Reglas modeladas (TCG moderno):
 *  - Debilidad: si el defensor es débil a algún tipo del atacante, el daño
 *    se multiplica (formato "×2"/"x2") — soporta también "+N".
 *  - Resistencia: resta el valor indicado (formato "-30").
 *  - El daño nunca baja de 0.
 */
object Damage {

    fun calculate(
        baseDamage: Int,
        attackerTypes: List<EnergyType>,
        defender: PokemonInPlay,
    ): DamageResult {
        if (baseDamage <= 0) return DamageResult(0, false, false)

        var amount = baseDamage
        var weakness = false
        var resistance = false

        defender.card.weaknesses.firstOrNull { it.type in attackerTypes }?.let { w ->
            amount = applyModifier(amount, w.value, isWeakness = true)
            weakness = true
        }
        defender.card.resistances.firstOrNull { it.type in attackerTypes }?.let { r ->
            amount = applyModifier(amount, r.value, isWeakness = false)
            resistance = true
        }

        return DamageResult(amount.coerceAtLeast(0), weakness, resistance)
    }

    /** Interpreta valores tipo "×2", "x2", "+20", "-30". */
    private fun applyModifier(amount: Int, raw: String, isWeakness: Boolean): Int {
        val value = raw.trim()
        val number = value.filter { it.isDigit() }.toIntOrNull() ?: return amount
        return when {
            value.startsWith("×") || value.startsWith("x") || value.startsWith("X") -> amount * number
            value.startsWith("-") -> amount - number
            value.startsWith("+") -> amount + number
            // Por defecto: debilidad multiplica, resistencia resta.
            isWeakness -> amount * (number.takeIf { it > 0 } ?: 2)
            else -> amount - number
        }
    }
}
