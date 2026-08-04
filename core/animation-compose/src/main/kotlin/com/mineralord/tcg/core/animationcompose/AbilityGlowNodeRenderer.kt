package com.mineralord.tcg.core.animationcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Envolvente de aparición/desaparición del aura (fade-in corto, fade-out algo mayor). Pura. */
fun glowEnvelope(p: Float): Float {
    val fin = (p / 0.10f).coerceIn(0f, 1f)
    val fout = ((1f - p) / 0.15f).coerceIn(0f, 1f)
    return min(fin, fout)
}

/**
 * NodeRenderer del aura de habilidad ([AbilityGlowRenderNode]). Dibuja bajo la carta:
 * - **Bloom radial** POR DEBAJO (pool desplazado hacia abajo) que respira.
 * - **Rim-glow** de los BORDES (varias pasadas de trazo redondeado para suavizar el halo).
 * - **Shimmer** que recorre el borde SÓLO en modo manual (dorado, call-to-action).
 *
 * El carácter (rojo estable = pasiva; dorado con shimmer = manual) llega como DATOS en [GlowVisual].
 * Coordenadas en raíz menos [LocalFlightOrigin], como los demás renderers.
 */
@Composable
fun AbilityGlowNodeRenderer(node: AbilityGlowRenderNode) {
    val v = node.visual
    val p = node.progress
    val origin = LocalFlightOrigin.current
    val b = node.bounds

    val accent = Color.hsv(v.hue.coerceIn(0f, 360f), v.saturation.coerceIn(0f, 1f), 1f)
    val env = glowEnvelope(p)
    val breath = 0.55f + 0.45f * sin(2f * PI.toFloat() * v.breathCycles * p)
    val intensity = env * (0.6f + 0.4f * breath)

    Canvas(Modifier.fillMaxSize()) {
        val left = b.left - origin.x
        val top = b.top - origin.y
        val w = b.width
        val h = b.height
        val cx = left + w / 2f
        val cy = top + h / 2f

        // Bloom radial por debajo (centro desplazado hacia abajo para leer como "underglow").
        val bloomCenter = Offset(cx, cy + h * 0.14f)
        val bloomR = (v.bloomRadius * w * (0.9f + 0.12f * breath)).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.55f * intensity), accent.copy(alpha = 0f)),
                center = bloomCenter,
                radius = bloomR,
            ),
            radius = bloomR,
            center = bloomCenter,
        )

        // Rim-glow de los bordes: varias pasadas concéntricas para un halo suave.
        val rimW = (v.edgeWidth * w).coerceAtLeast(2f)
        val corner = CornerRadius(w * 0.10f, w * 0.10f)
        for (i in 0..2) {
            val f = 1f - i * 0.30f
            drawRoundRect(
                color = accent.copy(alpha = 0.5f * intensity * f),
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = corner,
                style = Stroke(width = rimW * (1f + i * 1.6f)),
            )
        }

        // Shimmer manual: reflejo que recorre el borde (arriba en un sentido, abajo en el otro).
        if (v.shimmer > 0f) {
            val t = (p * 3f) % 1f
            drawCircle(
                color = Color.White.copy(alpha = 0.5f * v.shimmer * env),
                radius = w * 0.10f,
                center = Offset(left + t * w, top),
            )
            drawCircle(
                color = accent.copy(alpha = 0.4f * v.shimmer * env),
                radius = w * 0.08f,
                center = Offset(left + (1f - t) * w, top + h),
            )
        }
    }
}
