package com.mineralord.tcg.core.combatscene

/**
 * **Geometría normalizada del tapete compartido (0..1, origen arriba-izquierda).**
 *
 * Fuente ÚNICA de la maquetación de zonas de la Combat Scene. Es deliberadamente simple en esta v1
 * (un tapete a dos lados: rival arriba, jugador abajo); cuando la pantalla de combate del juego migre
 * a esta escena, su `BoardGeometry` afinada 1:1 (cajas NBox medidas contra TCG Live) se consolidará
 * AQUÍ y pasará a ser el único origen para ambos productos. No hay reglas: sólo posiciones.
 */
object CombatGeometry {

    /** Caja normalizada [x,y,w,h] en 0..1 relativa al área de tapete. */
    data class NBox(val x: Float, val y: Float, val w: Float, val h: Float)

    /** Relación ancho/alto de una carta en juego (106×148). Preserva la forma al dibujar. */
    const val CardAspect = 106f / 148f

    /** Zonas visuales del tapete. La [Zone.tracked] Activa del jugador es el ancla de animaciones. */
    enum class Zone(val label: String, val tracked: Boolean = false) {
        OppPrizes("Premios"),
        OppActive("Activo rival"),
        OppBench("Banca rival"),
        OppDeckDiscard("Mazo · Descarte"),
        MePrizes("Premios"),
        MeActive("Activo", tracked = true),
        MeBench("Banca"),
        MeDeckDiscard("Mazo · Descarte"),
        MeHand("Mano"),
    }

    /** Maquetación de cada zona. Espejo vertical entre rival (arriba) y jugador (abajo). */
    val layout: Map<Zone, NBox> = mapOf(
        Zone.OppPrizes to NBox(0.010f, 0.060f, 0.130f, 0.150f),
        Zone.OppActive to NBox(0.390f, 0.070f, 0.220f, 0.150f),
        Zone.OppBench to NBox(0.150f, 0.240f, 0.700f, 0.120f),
        Zone.OppDeckDiscard to NBox(0.860f, 0.060f, 0.130f, 0.150f),

        Zone.MePrizes to NBox(0.010f, 0.560f, 0.130f, 0.150f),
        Zone.MeActive to NBox(0.390f, 0.470f, 0.220f, 0.150f),
        Zone.MeBench to NBox(0.150f, 0.330f, 0.700f, 0.120f),
        Zone.MeDeckDiscard to NBox(0.860f, 0.560f, 0.130f, 0.150f),
        Zone.MeHand to NBox(0.020f, 0.760f, 0.960f, 0.210f),
    )
}
