package com.mineralord.tcg.core.animationcompose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mineralord.tcg.core.animation.GlowVisual
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
 * Dibujo NÚCLEO del aura (ÚNICA fuente de verdad, compartida por el aura por evento
 * [AbilityGlowNodeRenderer] y el indicador persistente [PersistentAbilityGlow]):
 * - **Bloom radial** por debajo (pool desplazado hacia abajo) que respira.
 * - **Rim-glow** de los bordes (varias pasadas de trazo redondeado para suavizar el halo).
 * - **Shimmer** que recorre el borde SÓLO cuando `visual.shimmer > 0` (manual, call-to-action).
 *
 * El carácter (rojo estable = pasiva; dorado con shimmer = manual) llega como DATOS en [GlowVisual].
 * Timing (intensidad/respiración/fase de shimmer) lo controla el llamador.
 */
fun DrawScope.drawAbilityGlow(
    v: GlowVisual,
    left: Float,
    top: Float,
    w: Float,
    h: Float,
    intensity: Float,
    breath: Float,
    env: Float,
    shimmerPhase: Float,
) {
    val accent = Color.hsv(v.hue.coerceIn(0f, 360f), v.saturation.coerceIn(0f, 1f), 1f)

    // Bloom radial por debajo (centro desplazado hacia abajo para leer como "underglow").
    val bloomCenter = Offset(left + w / 2f, top + h / 2f + h * 0.14f)
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
        drawCircle(
            color = Color.White.copy(alpha = 0.5f * v.shimmer * env),
            radius = w * 0.10f,
            center = Offset(left + shimmerPhase * w, top),
        )
        drawCircle(
            color = accent.copy(alpha = 0.4f * v.shimmer * env),
            radius = w * 0.08f,
            center = Offset(left + (1f - shimmerPhase) * w, top + h),
        )
    }
}

/**
 * NodeRenderer del aura de habilidad por EVENTO ([AbilityGlowRenderNode]). Reproduce el aura una vez
 * a lo largo del progreso 0→1 (con envolvente de aparición/desaparición). Coordenadas en raíz menos
 * [LocalFlightOrigin], como los demás renderers.
 */
@Composable
fun AbilityGlowNodeRenderer(node: AbilityGlowRenderNode) {
    val v = node.visual
    val p = node.progress
    val origin = LocalFlightOrigin.current
    val b = node.bounds

    val env = glowEnvelope(p)
    val breath = 0.55f + 0.45f * sin(2f * PI.toFloat() * v.breathCycles * p)
    val intensity = env * (0.6f + 0.4f * breath)
    val shimmerPhase = (p * 3f) % 1f

    Canvas(Modifier.fillMaxSize()) {
        drawAbilityGlow(v, b.left - origin.x, b.top - origin.y, b.width, b.height, intensity, breath, env, shimmerPhase)
    }
}

/**
 * Indicador PERSISTENTE del aura de habilidad para el tablero: dibuja el MISMO aura del Studio
 * ([drawAbilityGlow] + [AbilityGlowVisuals]) pero en bucle continuo (respiración infinita, sin
 * fade), ajustado a su propia caja. Úsalo como marcador de estado (pasiva activa / manual
 * disponible) sobre la carta. `manual=false` → rojo pasiva; `manual=true` → dorado con shimmer.
 */
@Composable
fun PersistentAbilityGlow(manual: Boolean, modifier: Modifier = Modifier) {
    val v = AbilityGlowVisuals.forManual(manual)
    val infinite = rememberInfiniteTransition(label = "persistentGlow")
    val phase by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "persistentGlowPhase",
    )
    Canvas(modifier.fillMaxSize()) {
        val breath = 0.55f + 0.45f * sin(2f * PI.toFloat() * v.breathCycles * phase)
        val intensity = 0.6f + 0.4f * breath   // persistente: sin envolvente (env = 1)
        drawAbilityGlow(v, 0f, 0f, size.width, size.height, intensity, breath, 1f, (phase * 3f) % 1f)
    }
}
