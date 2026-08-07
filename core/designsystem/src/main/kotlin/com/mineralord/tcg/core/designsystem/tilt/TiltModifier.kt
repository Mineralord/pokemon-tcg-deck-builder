package com.mineralord.tcg.core.designsystem.tilt

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Aplica un parallax 3D ligero según [tilt] (giro del dispositivo). Es el gancho de
 * verificación visual y el futuro punto donde el shader de acabado leerá el mismo [Tilt].
 * Recomendado usarlo SOLO en la carta activa (decisión de rendimiento: un tablero tiene
 * muchas cartas).
 *
 * @param maxDegrees inclinación máxima en cada eje al llegar al tope del recorrido.
 */
fun Modifier.tiltParallax(tilt: Tilt, maxDegrees: Float = 10f): Modifier = graphicsLayer {
    rotationX = -tilt.y * maxDegrees
    rotationY = tilt.x * maxDegrees
    cameraDistance = 12f * density
}
