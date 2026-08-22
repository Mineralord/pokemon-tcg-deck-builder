package com.mineralord.tcg.data.profile

/**
 * Los cuatro recursos económicos oficiales del proyecto.
 *
 * Canon: Fase 2 — Economía Principal, Cap. 6 (Sistema de Monedas). Cada recurso
 * cumple una función única (especialización económica) y no debe invadir la de
 * otro. Este enum es la identidad de dominio; el color/estilo es un detalle de UI.
 */
enum class CurrencyKind {
    /** Obtención de elementos cosméticos y recompensas de prestigio. */
    MONEDAS,
    /** Adquisición de sobres de cartas. */
    CRISTALES,
    /** Fabricación de cartas mediante el sistema de crafting. */
    FICHAS,
    /** Funcionamiento del mercado y las actividades comerciales. */
    CREDITOS,
}
