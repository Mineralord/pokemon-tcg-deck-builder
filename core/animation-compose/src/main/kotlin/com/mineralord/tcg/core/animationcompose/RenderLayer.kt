package com.mineralord.tcg.core.animationcompose

/**
 * Modelo de capas de render del framework de animaciones.
 *
 * CONTRATO DE ORDEN: el orden de dibujo lo define [zOrder] (menor = más al fondo),
 * **nunca el `ordinal`** del enum. Reordenar las constantes de este enum NO debe
 * cambiar el resultado visual; sólo cambiar un [zOrder] lo hace. Esto evita `zIndex`
 * arbitrarios dispersos por la UI: el apilamiento es un dato de primera clase que vive
 * SÓLO aquí.
 *
 * HUECOS: los valores dejan espacio (paso de 100) para poder insertar capas nuevas
 * entre dos existentes sin renumerar nada. Añadir una capa = una constante con su
 * [zOrder] en el hueco correcto.
 *
 * El `AnimationStage` dibuja las capas en el orden de [orderedByZ], de fondo a frente.
 */
enum class RenderLayer(val zOrder: Int) {

    /** Fondo estático: tablero, zonas, banca. No se anima. */
    Board(0),

    /** Cartas en su posición de reposo dentro de sus celdas. */
    Cards(100),

    /**
     * Overlay único para cartas en tránsito entre zonas. Vive por encima de [Cards]
     * para escapar del *clipping* del contenedor padre durante el "vuelo".
     */
    Flight(200),

    /** Sistemas de partículas (chispas, energía, KO). Se dibuja sobre las cartas. */
    Particles(300),

    /** Efectos a pantalla completa (flashes, viñeta, sacudida de cámara). */
    Effects(400),

    /** Texto flotante de daño, indicadores y banners. Siempre al frente. */
    Overlay(500);

    companion object {
        /**
         * Capas ordenadas de fondo a frente por [zOrder] (no por `ordinal`). Es la
         * única fuente para iterar el pipeline de dibujo.
         */
        val orderedByZ: List<RenderLayer> = entries.sortedBy { it.zOrder }
    }
}
