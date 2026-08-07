package com.mineralord.tcg.core.designsystem.tilt

/**
 * Inclinación del dispositivo normalizada a ~[-1, 1], relativa a la orientación base
 * (la que tenía el móvil en la primera lectura). Alimenta el parallax/holo del futuro
 * motor de acabados.
 *
 * @property x roll (giro lateral): negativo = borde izquierdo hacia ti.
 * @property y pitch (inclinación adelante/atrás): negativo = borde superior hacia ti.
 */
data class Tilt(val x: Float, val y: Float) {
    companion object {
        val Zero = Tilt(0f, 0f)
    }
}
