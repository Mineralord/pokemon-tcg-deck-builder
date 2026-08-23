package com.mineralord.tcg.data.profile

/**
 * Parámetros oficiales del Sistema de Fabricación / Reciclaje (Canon Fase 5 — Crafting).
 *
 * La rareza oficial es el ÚNICO parámetro económico (§3.1). Las cartas se agrupan en 9
 * **escalones** económicos (§3.2). Cada escalón tiene un coste de fabricación (§3.4) y un
 * valor de reciclaje de duplicados (§3.7 = 4 % del coste, redondeo inteligente, mínimo 1).
 *
 * Este objeto es rareza-agnóstico (usa el índice de escalón 1..9) para no acoplar `data:profile`
 * con el enum de rareza de `engine:model`; la capa superior mapea Rareza → escalón.
 */
object CraftingRules {

    /** Escalón económico mínimo/máximo del Canon (I..IX). */
    const val MIN_TIER = 1
    const val MAX_TIER = 9

    /** Fichas obtenidas al reciclar UN duplicado del escalón dado (§3.7). Mínimo 1 (§3.7). */
    fun recycleFichas(tier: Int): Int = when (tier) {
        1 -> 1    // Common
        2 -> 1    // Uncommon
        3 -> 2    // Rare
        4 -> 4    // Rare Holo
        5 -> 4    // Carta Premium Base (ex/V/GX/Radiant/ACE SPEC…)
        6 -> 6    // Ultra Rare / Full Art (VMAX/VSTAR)
        7 -> 8    // Illustration Rare (IR/AR)
        8 -> 14   // Special Illustration Rare (SIR/SAR/Alt)
        9 -> 28   // Hyper Rare / Gold / Rainbow
        else -> 1
    }

    /** Coste en Fichas para FABRICAR una carta del escalón dado (§3.4). Reservado (Fase 5 futura). */
    fun craftCost(tier: Int): Int = when (tier) {
        1 -> 20
        2 -> 30
        3 -> 45
        4 -> 70
        5 -> 100
        6 -> 150
        7 -> 225
        8 -> 340
        9 -> 700
        else -> 20
    }
}
