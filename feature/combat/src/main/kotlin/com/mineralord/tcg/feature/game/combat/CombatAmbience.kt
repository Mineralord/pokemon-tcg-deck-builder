package com.mineralord.tcg.feature.game.combat

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.mineralord.tcg.engine.model.EnergyType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * SISTEMA AMBIENTAL PROCEDURAL AAA de la lente central del tapete de combate.
 *
 * Objetivo: que la zona central REACCIONE al tipo del Pokémon Activo con una animación preciosa que
 * comunique el elemento de un vistazo, SIN robar protagonismo a las cartas ni tocar lógica/reglas/
 * estado/motor. 100% presentación y 100% dibujada en Compose (Canvas), sin assets de terceros.
 *
 * Lenguaje visual (benchmark AAA — Hearthstone / LoR / "magic circle"):
 *  1. NÚCLEO: un corazón de energía con rampa de color núcleo-blanco → saturado → transparente, que
 *     respira. Es el centro premium que lee "arena de poder".
 *  2. ANILLOS: un sistema de anillos concéntricos con ticks/segmentos que ROTAN a velocidades
 *     distintas (parallax), estilo sello arcano, modulado por el elemento.
 *  3. MOTIF: una capa de energía específica del elemento (brasas, ondas, rayos, pétalos, glifos,
 *     ondas de choque, bruma, destello metálico, chispas de hada, remolino de dragón…).
 *
 * Continuidad: un único reloj `clock` (0→1 en bucle lineal) provisto por el llamador. Las rotaciones
 * usan el ángulo crudo (continuo) y las expansiones (anillos/ondas) se desvanecen antes de reiniciar.
 * Coste contenido: un puñado de gradientes/paths por frame; apto para gama media.
 *
 * Registro Animation Gallery (NUEVA): ID estable `lens.ambient.procedural.v2` · Autor: Claude Code.
 */

/** Familia visual del elemento. Añadir un tipo = añadir un caso en [ambientProfileFor] + [drawMotif]. */
enum class LensMotif { NONE, FIRE, WATER, LIGHTNING, GRASS, PSYCHIC, FIGHTING, DARK, METAL, FAIRY, DRAGON, NEUTRAL }

/**
 * Parámetros —y SOLO parámetros— que definen la identidad visual de un tipo. El renderer los
 * interpreta; ningún tipo dibuja por su cuenta. Añadir un tipo = un caso en [ambientProfileFor].
 */
data class AmbientProfile(
    /** Lavado tenue que tiñe (u oscurece) la lente. */
    val base: Color,
    /** Color del NÚCLEO de energía (el corazón que respira desde el centro). */
    val glow: Color,
    /** Color de ACENTO: anillos, ticks, partículas y destellos del motif. */
    val accent: Color,
    /** Motif de energía del elemento. */
    val motif: LensMotif,
    /** Fuerza global contenida (0..1). 0 = sin ambiente. */
    val intensity: Float,
    /** Velocidad del latido de respiración (entero: 1 lento/señorial … 4 enérgico). */
    val pulseMult: Int = 1,
)

private const val TWO_PI = (PI * 2.0).toFloat()

/** Relleno gris por defecto del lente (sin Activo / tipo desconocido). */
private val LensGrayTop = Color(0xFF3A414E)
private val LensGrayBot = Color(0xFF2C323D)

/**
 * Colores del RELLENO del lente (arriba, abajo) teñidos por el tipo del Activo, para que el lente
 * sea totalmente dinámico (no gris). Sin tipo → gris por defecto. El tinte parte del [base] del
 * perfil (oscuro, rico) con la parte superior desplazada hacia el [glow] para dar profundidad; se
 * mantiene oscuro para que las cartas sigan dominando.
 */
fun lensFillColors(profile: AmbientProfile): Pair<Color, Color> {
    if (profile.base.alpha <= 0f) return LensGrayTop to LensGrayBot
    val top = lerp(profile.base, profile.glow, 0.26f)
    val bot = lerp(profile.base, Color.Black, 0.22f)
    return top to bot
}

/** Perfil neutro sin ambiente: para tipo desconocido o antes de que haya Activo. */
private val NoAmbient = AmbientProfile(Color.Transparent, Color.Transparent, Color.Transparent, LensMotif.NONE, 0f)

/**
 * Traduce el tipo del Activo a su [AmbientProfile]. Fuente ÚNICA de la identidad por tipo:
 * colores contenidos y elegantes, nunca infantiles ni invasivos. Añadir un tipo = un caso.
 */
fun ambientProfileFor(type: EnergyType?): AmbientProfile = when (type) {
    EnergyType.FIRE -> AmbientProfile(
        base = Color(0xFF3A1A0E), glow = Color(0xFFFF7A2E), accent = Color(0xFFFFC24D),
        motif = LensMotif.FIRE, intensity = 0.95f, pulseMult = 2,
    )
    EnergyType.WATER -> AmbientProfile(
        base = Color(0xFF0C2338), glow = Color(0xFF3AA0E0), accent = Color(0xFF7FE0FF),
        motif = LensMotif.WATER, intensity = 0.9f, pulseMult = 1,
    )
    EnergyType.GRASS -> AmbientProfile(
        base = Color(0xFF123019), glow = Color(0xFF49B566), accent = Color(0xFF9BF0A8),
        motif = LensMotif.GRASS, intensity = 0.85f, pulseMult = 1,
    )
    EnergyType.LIGHTNING -> AmbientProfile(
        base = Color(0xFF342C0C), glow = Color(0xFFFFDE3A), accent = Color(0xFFFFF59A),
        motif = LensMotif.LIGHTNING, intensity = 0.9f, pulseMult = 4,
    )
    EnergyType.PSYCHIC -> AmbientProfile(
        base = Color(0xFF261038), glow = Color(0xFFC257E0), accent = Color(0xFFF59BF0),
        motif = LensMotif.PSYCHIC, intensity = 0.9f, pulseMult = 2,
    )
    EnergyType.FIGHTING -> AmbientProfile(
        base = Color(0xFF34160C), glow = Color(0xFFD06636), accent = Color(0xFFF2A878),
        motif = LensMotif.FIGHTING, intensity = 0.88f, pulseMult = 3,
    )
    EnergyType.DARKNESS -> AmbientProfile(
        base = Color(0xFF070910), glow = Color(0xFF3A3056), accent = Color(0xFF8B79B8),
        motif = LensMotif.DARK, intensity = 0.95f, pulseMult = 1,
    )
    EnergyType.METAL -> AmbientProfile(
        base = Color(0xFF191F28), glow = Color(0xFF9DB4CC), accent = Color(0xFFE6F2FF),
        motif = LensMotif.METAL, intensity = 0.85f, pulseMult = 1,
    )
    EnergyType.FAIRY -> AmbientProfile(
        base = Color(0xFF331629), glow = Color(0xFFF79BD4), accent = Color(0xFFFFD0EC),
        motif = LensMotif.FAIRY, intensity = 0.8f, pulseMult = 2,
    )
    EnergyType.DRAGON -> AmbientProfile(
        base = Color(0xFF2A2008), glow = Color(0xFFD6A52E), accent = Color(0xFFFFE07A),
        motif = LensMotif.DRAGON, intensity = 0.92f, pulseMult = 2,
    )
    EnergyType.COLORLESS -> AmbientProfile(
        base = Color(0xFF1E232C), glow = Color(0xFFB7C2D1), accent = Color(0xFFEDF2F9),
        motif = LensMotif.NEUTRAL, intensity = 0.6f, pulseMult = 1,
    )
    null -> NoAmbient
}

/**
 * Dibuja el ambiente dentro de la banda [top, bottom] (ancho [w]); el llamador ya recorta a la forma
 * de la lente. [clock] = reloj continuo 0→1; [weight] pondera la mezcla en los cambios de tipo
 * (crossfade). Todo se multiplica por [weight]·`intensity` para mantenerse contenido.
 */
fun DrawScope.drawLensAmbient(
    profile: AmbientProfile,
    clock: Float,
    weight: Float,
    top: Float,
    bottom: Float,
    w: Float,
) {
    val k = profile.intensity * weight
    if (k <= 0.001f) return

    val angle = clock * TWO_PI
    val rh = bottom - top
    val cx = w * 0.5f
    val cy = top + rh * 0.5f
    val c = Offset(cx, cy)
    // Radio de referencia del "sello": contenido a la banda del lente (no invade todo el ancho).
    val R = rh * 0.62f
    // Respiración base (0..1) suave que modula núcleo y energía sin sobresaltos.
    val breath = 0.5f + 0.5f * sin(angle * profile.pulseMult)

    // Microvibración contundente (lucha): desplazamiento minúsculo de todo el sello.
    val vib = if (profile.motif == LensMotif.FIGHTING) sin(angle * 20f) * (rh * 0.006f) else 0f
    val cc = Offset(cx, cy + vib)

    // --- 1. Lavado ambiental: tiñe/oscurece la lente de forma uniforme y tenue --------------
    drawRect(
        color = profile.base.copy(alpha = 0.14f * k),
        topLeft = Offset(0f, top),
        size = Size(w, rh),
    )

    // --- 2. NÚCLEO de energía: rampa núcleo-blanco → saturado → transparente, respirando -----
    val coreR = R * (0.95f + 0.12f * breath)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f * k * (0.6f + 0.4f * breath)),
                profile.glow.copy(alpha = 0.34f * k),
                profile.glow.copy(alpha = 0f),
            ),
            center = cc, radius = coreR,
        ),
        radius = coreR, center = cc,
    )

    // --- 3. ANILLOS concéntricos con ticks que ROTAN (parallax), estilo sello arcano ---------
    drawSigilRings(profile, angle, k, cc, R, breath)

    // --- 4. MOTIF de energía específico del elemento -----------------------------------------
    drawMotif(profile, angle, clock, k, cc, R, breath, top, rh, w)
}

/** Dos anillos con segmentos/ticks que giran a distinta velocidad y sentido (parallax arcano). */
private fun DrawScope.drawSigilRings(
    profile: AmbientProfile,
    angle: Float,
    k: Float,
    c: Offset,
    R: Float,
    breath: Float,
) {
    if (profile.motif == LensMotif.NONE) return
    val accent = profile.accent
    // Anillo exterior: ticks largos girando en sentido horario.
    ringTicks(c, R * 1.02f, ticks = 48, len = R * 0.06f, width = R * 0.012f,
        color = accent.copy(alpha = 0.22f * k), rot = angle * 0.6f)
    // Anillo medio continuo, respira su grosor.
    drawCircle(
        color = accent.copy(alpha = 0.16f * k * (0.6f + 0.4f * breath)),
        radius = R * 0.78f, center = c, style = Stroke(width = R * 0.016f),
    )
    // Anillo interior: ticks cortos girando en sentido contrario (parallax).
    ringTicks(c, R * 0.6f, ticks = 24, len = R * 0.05f, width = R * 0.014f,
        color = accent.copy(alpha = 0.3f * k), rot = -angle * 1.1f)
}

/** Dibuja [ticks] marcas radiales equiespaciadas sobre un círculo de radio [r], rotado [rot] rad. */
private fun DrawScope.ringTicks(
    c: Offset, r: Float, ticks: Int, len: Float, width: Float, color: Color, rot: Float,
) {
    if (color.alpha <= 0.001f) return
    for (i in 0 until ticks) {
        val a = rot + (i / ticks.toFloat()) * TWO_PI
        val ca = cos(a); val sa = sin(a)
        drawLine(
            color = color,
            start = Offset(c.x + ca * r, c.y + sa * r),
            end = Offset(c.x + ca * (r + len), c.y + sa * (r + len)),
            strokeWidth = width, cap = StrokeCap.Round,
        )
    }
}

/** Capa de energía por elemento. Cada motif es contenido, continuo y elegante. */
private fun DrawScope.drawMotif(
    profile: AmbientProfile,
    angle: Float,
    clock: Float,
    k: Float,
    c: Offset,
    R: Float,
    breath: Float,
    top: Float,
    rh: Float,
    w: Float,
) {
    val accent = profile.accent
    val glow = profile.glow
    when (profile.motif) {
        LensMotif.FIRE -> {
            // Brasas que ascienden girando + lengüetas cálidas (jamás llamas literales).
            val n = 16
            for (i in 0 until n) {
                val seed = hash(i + 1)
                val pp = (clock * (0.7f + seed * 0.6f) + seed) % 1f
                val a = (i / n.toFloat()) * TWO_PI + angle * 0.2f
                val dist = R * (0.1f + 0.9f * pp)
                val up = -R * 0.5f * pp
                val pos = Offset(c.x + cos(a) * dist * 0.7f, c.y + sin(a) * dist * 0.45f + up)
                val rr = R * 0.05f * (1f - pp) * (0.6f + seed)
                if (rr > 0f) {
                    drawCircle(accent.copy(alpha = (1f - pp) * 0.55f * k), rr, pos)
                    drawCircle(Color.White.copy(alpha = (1f - pp) * 0.3f * k), rr * 0.4f, pos)
                }
            }
        }
        LensMotif.WATER -> {
            // Ondas concéntricas que se expanden y desvanecen + brillo cáustico.
            for (i in 0..2) {
                val pp = ((clock) + i * 0.34f) % 1f
                val r = R * (0.2f + 1.0f * pp)
                drawCircle(accent.copy(alpha = (1f - pp) * 0.5f * k), r, c, style = Stroke(R * 0.02f))
            }
            drawCircle(glow.copy(alpha = 0.12f * k * breath), R * 0.3f, c)
        }
        LensMotif.LIGHTNING -> {
            // Destello estroboscópico + rayos dentados radiales que titilan.
            val strobe = if ((clock * 10f).toInt() % 3 == 0) 1f else 0.35f
            val n = 6
            for (i in 0 until n) {
                val a = (i / n.toFloat()) * TWO_PI + angle * 0.15f
                jaggedBolt(c, a, R * (0.5f + 0.7f * breath), accent.copy(alpha = 0.55f * k * strobe), R)
            }
            drawCircle(Color.White.copy(alpha = 0.14f * k * strobe), R * 0.22f, c)
        }
        LensMotif.GRASS -> {
            // Pétalos/hojas que salen en espiral, girando suave.
            val n = 12
            for (i in 0 until n) {
                val pp = ((clock) + hash(i) ) % 1f
                val a = (i / n.toFloat()) * TWO_PI + clock * TWO_PI * 0.5f
                val dist = R * (0.15f + 0.85f * pp)
                val pos = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist * 0.75f)
                drawLeaf(pos, a + pp * 3f, R * 0.08f * (1f - pp), accent.copy(alpha = (1f - pp) * 0.55f * k))
            }
        }
        LensMotif.PSYCHIC -> {
            // Glifos orbitando + anillo que se deforma (respiración espacial).
            val n = 8
            for (i in 0 until n) {
                val a = (i / n.toFloat()) * TWO_PI + angle * 0.8f
                val wobble = 1f + 0.12f * sin(angle * 3f + i)
                val pos = Offset(c.x + cos(a) * R * 0.85f * wobble, c.y + sin(a) * R * 0.7f * wobble)
                val rr = R * 0.03f * (0.6f + 0.6f * (0.5f + 0.5f * sin(angle * 2f + i)))
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.6f * k), accent.copy(alpha = 0.5f * k), accent.copy(alpha = 0f)),
                        center = pos, radius = rr * 2.4f,
                    ),
                    radius = rr * 2.4f, center = pos,
                )
            }
        }
        LensMotif.FIGHTING -> {
            // Ondas de choque pulsantes (impacto) + motas de polvo.
            for (i in 0..1) {
                val pp = ((clock * 2f) + i * 0.5f) % 1f
                val r = R * (0.2f + 1.0f * pp)
                drawCircle(accent.copy(alpha = (1f - pp) * 0.5f * k), r, c, style = Stroke(R * 0.03f * (1f - pp)))
            }
        }
        LensMotif.DARK -> {
            // Bruma que deriva + oscurecimiento, rim violáceo tenue.
            val mx = c.x + sin(angle) * (R * 0.5f)
            val my = c.y + sin(angle * 2f) * (R * 0.3f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = 0.3f * k), Color.Transparent),
                    center = Offset(mx, my), radius = R * 1.1f,
                ),
                radius = R * 1.1f, center = Offset(mx, my),
            )
            drawCircle(accent.copy(alpha = 0.16f * k * breath), R * 0.9f, c, style = Stroke(R * 0.02f))
        }
        LensMotif.METAL -> {
            // Destello especular que recorre un arco (pulido) + segmentos tipo engranaje.
            val sweepA = angle
            val sa = Offset(c.x + cos(sweepA) * R * 0.9f, c.y + sin(sweepA) * R * 0.9f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.5f * k), accent.copy(alpha = 0f)),
                    center = sa, radius = R * 0.4f,
                ),
                radius = R * 0.4f, center = sa,
            )
            ringTicks(c, R * 0.85f, ticks = 16, len = R * 0.09f, width = R * 0.02f,
                color = accent.copy(alpha = 0.22f * k), rot = angle * 0.3f)
        }
        LensMotif.FAIRY -> {
            // Chispas/bokeh que suben y titilan.
            val n = 12
            for (i in 0 until n) {
                val fx = hash(i * 2 + 1); val fy = hash(i * 2 + 2)
                val tw = 0.5f + 0.5f * sin(angle * (2 + i % 3) + i)
                val a = tw * tw * 0.6f * k
                if (a <= 0.02f) continue
                val px = c.x + (fx - 0.5f) * R * 1.8f
                val py = c.y + (fy - 0.5f) * R * 1.4f - R * 0.2f * ((clock + fy) % 1f)
                val dot = R * 0.02f * (0.6f + 0.8f * tw)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = a), accent.copy(alpha = a), accent.copy(alpha = 0f)),
                        center = Offset(px, py), radius = dot * 3f,
                    ),
                    radius = dot * 3f, center = Offset(px, py),
                )
            }
        }
        LensMotif.DRAGON -> {
            // Doble remolino de brasas doradas en sentidos opuestos (majestuoso).
            val n = 10
            for (dir in intArrayOf(1, -1)) {
                for (i in 0 until n) {
                    val pp = ((clock) + hash(i + dir + 3)) % 1f
                    val a = dir * (clock * TWO_PI * 1.2f) + (i / n.toFloat()) * TWO_PI
                    val dist = R * (0.2f + 0.8f * pp)
                    val pos = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist * 0.7f)
                    val rr = R * 0.035f * (1f - pp)
                    if (rr > 0f) drawCircle(accent.copy(alpha = (1f - pp) * 0.5f * k), rr, pos)
                }
            }
        }
        LensMotif.NEUTRAL -> {
            // Respiración sobria: solo un halo suave adicional, sin partículas.
            drawCircle(glow.copy(alpha = 0.1f * k * breath), R * 0.5f, c)
        }
        LensMotif.NONE -> Unit
    }
}

/** Rayo dentado desde el centro hacia [angle], longitud [len]. */
private fun DrawScope.jaggedBolt(c: Offset, angle: Float, len: Float, color: Color, base: Float) {
    if (color.alpha <= 0.001f) return
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
    drawPath(path, color, style = Stroke(base * 0.025f, cap = StrokeCap.Round))
}

/** Hoja/mota alargada. */
private fun DrawScope.drawLeaf(center: Offset, rot: Float, r: Float, color: Color) {
    if (r <= 0f || color.alpha <= 0.001f) return
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

/** Hash determinista 0..1 a partir de un entero (posiciones/semillas estables). */
private fun hash(n: Int): Float {
    val s = sin(n * 12.9898f) * 43758.5453f
    return s - kotlin.math.floor(s)
}
