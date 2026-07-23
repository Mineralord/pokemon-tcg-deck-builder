package com.mineralord.tcg.feature.game.combat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import com.mineralord.tcg.core.designsystem.motion.AnimationCurves
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.mineralord.tcg.engine.model.EnergyType
import kotlin.math.cos
import kotlin.math.sin

/**
 * FX PREMIUM de ataque en el LENTE central: al lanzar un ataque, un estallido de ~1 s
 * (color y "motivo" según el TIPO del Pokémon atacante) reventa sobre el Pokémon golpeado.
 * Arte 100% dibujado en Compose (Canvas), sin assets de terceros. Efímero: se auto-descarta.
 */

/** Motivo visual del estallido, derivado del tipo de energía del atacante. */
enum class AtkMotif { EMBERS, BOLTS, RIPPLE, LEAVES, SHARDS, ORBS }

fun motifFor(type: EnergyType?): AtkMotif = when (type) {
    EnergyType.FIRE -> AtkMotif.EMBERS
    EnergyType.LIGHTNING -> AtkMotif.BOLTS
    EnergyType.WATER -> AtkMotif.RIPPLE
    EnergyType.GRASS -> AtkMotif.LEAVES
    EnergyType.METAL, EnergyType.FIGHTING -> AtkMotif.SHARDS
    else -> AtkMotif.ORBS
}

/** Una instancia de estallido pendiente de reproducir. [id] único dispara la animación. */
data class LensFxSpec(
    val id: Long,
    val color: Color,
    val motif: AtkMotif,
    /** true = ataca el jugador (impacto arriba, sobre el activo rival); false = al revés. */
    val fromPlayer: Boolean,
)

/**
 * Overlay a pantalla de tablero que reproduce UN estallido y llama [onDone] al terminar.
 * Centrado sobre el Pokémon GOLPEADO (activo del defensor).
 */
@Composable
fun LensAttackFx(spec: LensFxSpec, onDone: () -> Unit) {
    val p = remember(spec.id) { Animatable(0f) }
    LaunchedEffect(spec.id) {
        p.snapTo(0f)
        p.animateTo(1f, tween(1000, easing = AnimationCurves.Linear))
        onDone()
    }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Impacto sobre el activo del DEFENSOR (centro de su caja).
        val cx = w * 0.5f
        val cy = if (spec.fromPlayer) h * 0.338f else h * 0.485f
        val base = w * 0.24f
        drawImpact(spec, p.value, Offset(cx, cy), base)
    }
}

private fun DrawScope.drawImpact(spec: LensFxSpec, p: Float, c: Offset, base: Float) {
    val col = spec.color
    val fade = (1f - p).coerceIn(0f, 1f)
    // ---- Fogonazo central (bloom): blanco→color, crece y se apaga rápido. ----
    val bloomR = base * (0.35f + 1.1f * p)
    val bloomA = (1f - p * 1.6f).coerceIn(0f, 1f)
    if (bloomA > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f * bloomA),
                    col.copy(alpha = 0.7f * bloomA),
                    col.copy(alpha = 0f),
                ),
                center = c, radius = bloomR,
            ),
            radius = bloomR, center = c,
        )
    }
    when (spec.motif) {
        AtkMotif.RIPPLE -> {
            // Agua: 3 ondas concéntricas desfasadas.
            for (k in 0..2) {
                val pp = (p + k * 0.28f) % 1f
                val r = base * (0.2f + 1.7f * pp)
                drawCircle(col.copy(alpha = (1f - pp) * 0.8f), radius = r, center = c, style = Stroke(base * 0.05f))
            }
        }
        AtkMotif.BOLTS -> {
            // Rayo: destello estroboscópico + rayos dentados radiales.
            val strobe = if ((p * 8f).toInt() % 2 == 0) 1f else 0.5f
            drawCircle(col.copy(alpha = 0.6f * fade), radius = base * (0.5f + p), center = c, style = Stroke(base * 0.03f))
            val n = 7
            for (i in 0 until n) {
                val a = (i / n.toFloat()) * 6.2832f + p * 0.6f
                jaggedBolt(c, a, base * (0.5f + 1.3f * p), col.copy(alpha = fade * strobe), base)
            }
        }
        AtkMotif.EMBERS -> {
            // Fuego: brasas que suben y se dispersan, cálidas.
            val n = 16
            for (i in 0 until n) {
                val a = (i / n.toFloat()) * 6.2832f
                val spd = 0.6f + 0.8f * ((i * 37) % 10) / 10f
                val dist = base * (0.2f + 1.4f * p * spd)
                val up = -base * 0.9f * p            // sesgo hacia arriba (flotan)
                val pos = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist * 0.6f + up)
                val r = base * 0.08f * (1f - p) * (0.6f + spd * 0.5f)
                drawCircle(col.copy(alpha = fade), radius = r.coerceAtLeast(0f), center = pos)
                drawCircle(Color.White.copy(alpha = fade * 0.5f), radius = (r * 0.4f).coerceAtLeast(0f), center = pos)
            }
        }
        AtkMotif.LEAVES -> {
            // Planta: motas que salen girando en espiral.
            val n = 14
            for (i in 0 until n) {
                val a = (i / n.toFloat()) * 6.2832f + p * 2.2f
                val dist = base * (0.15f + 1.3f * p)
                val pos = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist)
                drawLeaf(pos, a + p * 3f, base * 0.12f * (1f - p), col.copy(alpha = fade))
            }
        }
        AtkMotif.SHARDS -> {
            // Metal/Lucha: esquirlas angulares que salen disparadas.
            val n = 12
            for (i in 0 until n) {
                val a = (i / n.toFloat()) * 6.2832f
                val dist = base * (0.1f + 1.5f * p)
                val pos = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist)
                drawShard(pos, a, base * 0.16f * (1f - p * 0.7f), col.copy(alpha = fade))
            }
            drawCircle(col.copy(alpha = 0.5f * fade), radius = base * (0.4f + p), center = c, style = Stroke(base * 0.04f))
        }
        AtkMotif.ORBS -> {
            // Psíquico/Oscuro/Hada/genérico: orbes suaves que se expanden.
            val n = 12
            for (i in 0 until n) {
                val a = (i / n.toFloat()) * 6.2832f + p
                val dist = base * (0.15f + 1.2f * p)
                val pos = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist)
                val r = base * 0.13f * (1f - p)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = fade * 0.8f), col.copy(alpha = fade), col.copy(alpha = 0f)),
                        center = pos, radius = r.coerceAtLeast(0.1f),
                    ),
                    radius = r.coerceAtLeast(0f), center = pos,
                )
            }
            drawCircle(col.copy(alpha = 0.6f * fade), radius = base * (0.4f + 1.2f * p), center = c, style = Stroke(base * 0.03f))
        }
    }
}

/** Rayo dentado desde el centro hacia [angle], longitud [len]. */
private fun DrawScope.jaggedBolt(c: Offset, angle: Float, len: Float, color: Color, base: Float) {
    val path = Path().apply { moveTo(c.x, c.y) }
    val steps = 4
    for (s in 1..steps) {
        val t = s / steps.toFloat()
        val perp = angle + 1.5708f
        val jitter = (if (s % 2 == 0) 1f else -1f) * base * 0.09f * (1f - t)
        val x = c.x + cos(angle) * len * t + cos(perp) * jitter
        val y = c.y + sin(angle) * len * t + sin(perp) * jitter
        path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(base * 0.03f, cap = StrokeCap.Round))
}

/** Hoja/mota alargada. */
private fun DrawScope.drawLeaf(center: Offset, rot: Float, r: Float, color: Color) {
    if (r <= 0f) return
    rotate(Math.toDegrees(rot.toDouble()).toFloat(), pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - r)
            quadraticBezierTo(center.x + r * 0.7f, center.y, center.x, center.y + r)
            quadraticBezierTo(center.x - r * 0.7f, center.y, center.x, center.y - r)
            close()
        }
        drawPath(path, color)
    }
}

/** Esquirla triangular apuntando radialmente. */
private fun DrawScope.drawShard(center: Offset, angle: Float, r: Float, color: Color) {
    if (r <= 0f) return
    val perp = angle + 1.5708f
    val tip = Offset(center.x + cos(angle) * r, center.y + sin(angle) * r)
    val b1 = Offset(center.x + cos(perp) * r * 0.35f, center.y + sin(perp) * r * 0.35f)
    val b2 = Offset(center.x - cos(perp) * r * 0.35f, center.y - sin(perp) * r * 0.35f)
    val path = Path().apply {
        moveTo(tip.x, tip.y); lineTo(b1.x, b1.y); lineTo(b2.x, b2.y); close()
    }
    drawPath(path, color)
}
