package com.mineralord.tcg.feature.game.combat

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.lerp
import com.mineralord.tcg.engine.model.EnergyType
import kotlin.math.PI
import kotlin.math.sin

/**
 * SISTEMA AMBIENTAL PROCEDURAL de la lente central del tapete de combate.
 *
 * Objetivo: que la zona central REACCIONE al tipo del Pokémon Activo y se sienta como un tablero
 * vivo, comunicando el tipo de un vistazo, SIN robar protagonismo a las cartas ni tocar lógica,
 * reglas, estado, ViewModels, cartas ni motor. Es 100% presentación: recibe un [EnergyType] y
 * dibuja capas translúcidas RECORTADAS a la lente (por el llamador), siempre por DEBAJO de las
 * cartas del tablero.
 *
 * Un ÚNICO renderer compuesto por capas —color, iluminación, movimiento, reflejos y energía—;
 * cada tipo NO trae una implementación propia, solo CONFIGURA esos parámetros vía [AmbientProfile].
 * Añadir un tipo nuevo = añadir un caso en [ambientProfileFor]. El movimiento nace de un único
 * reloj continuo `clock` (0→1 en bucle lineal) que el llamador provee; todas las oscilaciones usan
 * `sin` con múltiplos ENTEROS de fase para ser perfectamente continuas en el reinicio del bucle
 * (sin saltos), y las expansiones (anillos) se desvanecen antes de reiniciarse. Coste bajo:
 * un puñado de gradientes y círculos por frame, apto para gama media.
 */

/** Estilo del barrido de luz que cruza la lente (reflejo/iluminación en movimiento). */
enum class SweepKind { None, Diagonal, Horizontal, Vertical }

/**
 * Parámetros —y SOLO parámetros— que definen la identidad visual de un tipo. El renderer
 * ([DrawScope.drawLensAmbient]) los interpreta; ningún tipo dibuja por su cuenta.
 */
data class AmbientProfile(
    /** Color ambiental base (lavado tenue que tiñe la lente). En OSCURO, oscurece. */
    val base: Color,
    /** Color de ILUMINACIÓN: brillo radial que respira desde el centro. */
    val glow: Color,
    /** Color de ACENTO: reflejos, barridos, anillos y destellos de energía. */
    val accent: Color,
    /** Velocidad del latido de respiración (entero: 1 = lento y señorial … 4 = enérgico). */
    val pulseMult: Int,
    /** Fuerza global, deliberadamente contenida (0..1). 0 = sin ambiente. */
    val intensity: Float,
    /** Movimiento: barrido de luz y su orientación. */
    val sweep: SweepKind = SweepKind.None,
    /** Velocidad del barrido (entero, por continuidad de fase). */
    val sweepMult: Int = 1,
    /** Ondas circulares concéntricas (psíquico, agua). */
    val rings: Boolean = false,
    /** Nº de micro-destellos de energía (eléctrico, hada). 0 = ninguno. */
    val sparkles: Int = 0,
    /** Ondas de calor ascendentes muy sutiles (fuego). Nunca llamas. */
    val shimmer: Boolean = false,
    /** Neblina/oscurecimiento a la deriva (oscuro). */
    val mist: Boolean = false,
    /** Microvibración contundente (lucha). */
    val vibrate: Boolean = false,
)

private const val TWO_PI = (PI * 2.0).toFloat()

/** Perfil neutro sin ambiente: para tipo desconocido o antes de que haya Activo. */
private val NoAmbient = AmbientProfile(Color.Transparent, Color.Transparent, Color.Transparent, 1, 0f)

/**
 * Traduce el tipo del Activo a su [AmbientProfile]. Fuente ÚNICA de la identidad por tipo:
 * colores contenidos, movimiento elegante, nunca infantil ni invasivo. Añadir un tipo = un caso.
 */
fun ambientProfileFor(type: EnergyType?): AmbientProfile = when (type) {
    EnergyType.FIRE -> AmbientProfile(
        base = Color(0xFF3A1E12), glow = Color(0xFFFF6A2A), accent = Color(0xFFFF8A3D),
        pulseMult = 1, intensity = 0.95f, sweep = SweepKind.Diagonal, sweepMult = 1, shimmer = true,
    )
    EnergyType.WATER -> AmbientProfile(
        base = Color(0xFF0E2338), glow = Color(0xFF2E8BD6), accent = Color(0xFF57C9FF),
        pulseMult = 1, intensity = 0.9f, sweep = SweepKind.Horizontal, sweepMult = 1, rings = true,
    )
    EnergyType.GRASS -> AmbientProfile(
        base = Color(0xFF16301B), glow = Color(0xFF3FA35B), accent = Color(0xFF6FD08A),
        pulseMult = 1, intensity = 0.85f, sweep = SweepKind.Vertical, sweepMult = 1,
    )
    EnergyType.LIGHTNING -> AmbientProfile(
        base = Color(0xFF352C0E), glow = Color(0xFFFFD22E), accent = Color(0xFFFFE45C),
        pulseMult = 4, intensity = 0.85f, sparkles = 11,
    )
    EnergyType.PSYCHIC -> AmbientProfile(
        base = Color(0xFF2A1236), glow = Color(0xFFB74AD1), accent = Color(0xFFE96BE0),
        pulseMult = 2, intensity = 0.9f, rings = true,
    )
    EnergyType.FIGHTING -> AmbientProfile(
        base = Color(0xFF35180F), glow = Color(0xFFB5502C), accent = Color(0xFFE0724A),
        pulseMult = 2, intensity = 0.85f, vibrate = true,
    )
    EnergyType.DARKNESS -> AmbientProfile(
        base = Color(0xFF090B12), glow = Color(0xFF2A2340), accent = Color(0xFF6A5B8A),
        pulseMult = 1, intensity = 0.95f, mist = true,
    )
    EnergyType.METAL -> AmbientProfile(
        base = Color(0xFF1C222B), glow = Color(0xFF8FA6BC), accent = Color(0xFFCFE0F0),
        pulseMult = 1, intensity = 0.85f, sweep = SweepKind.Vertical, sweepMult = 2,
    )
    EnergyType.FAIRY -> AmbientProfile(
        base = Color(0xFF33162B), glow = Color(0xFFF48FD0), accent = Color(0xFFFFB6E6),
        pulseMult = 1, intensity = 0.75f, sparkles = 8,
    )
    EnergyType.DRAGON -> AmbientProfile(
        base = Color(0xFF2A2210), glow = Color(0xFFC79A34), accent = Color(0xFFF3C960),
        pulseMult = 1, intensity = 0.9f, sweep = SweepKind.Diagonal, sweepMult = 1,
    )
    EnergyType.COLORLESS -> AmbientProfile(
        base = Color(0xFF20242C), glow = Color(0xFFAAB4C2), accent = Color(0xFFE7ECF3),
        pulseMult = 1, intensity = 0.6f,
    )
    null -> NoAmbient
}

/**
 * Dibuja UNA capa ambiental dentro de la banda [top, bottom] (ancho [w]). El llamador ya ha
 * recortado a la forma de la lente. [clock] es el reloj continuo 0→1; [weight] pondera la mezcla
 * durante los cambios de tipo (crossfade). Todo se multiplica por [weight] · `intensity` para
 * mantener el ambiente contenido y garantizar que las cartas siempre dominen.
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
    // Respiración base (0..1) que modula iluminación y energía sin sobresaltos.
    val breath = 0.5f + 0.5f * sin(angle * profile.pulseMult)

    // Microvibración (lucha): desplazamiento minúsculo y contundente de toda la capa.
    val vib = if (profile.vibrate) sin(angle * 22f) * (rh * 0.006f) else 0f

    // --- 1. Color ambiental: lavado uniforme que tiñe (u oscurece) la lente ---------------
    drawRect(
        color = profile.base.copy(alpha = 0.16f * k),
        topLeft = Offset(0f, top),
        size = Size(w, rh),
    )

    // --- 2. Iluminación: brillo radial que respira desde el centro ------------------------
    val glowR = w * (0.42f + 0.06f * breath)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(profile.glow.copy(alpha = 0.22f * k * (0.6f + 0.4f * breath)), Color.Transparent),
            center = Offset(cx, cy + vib),
            radius = glowR,
        ),
        topLeft = Offset(0f, top),
        size = Size(w, rh),
    )

    // --- 3/4. Movimiento + reflejos: barrido de luz que cruza la lente --------------------
    if (profile.sweep != SweepKind.None) {
        drawSweep(profile, angle, k, top, rh, w, vib)
    }

    // --- 3b. Ondas circulares (psíquico / agua): distorsión espacial elegante -------------
    if (profile.rings) {
        for (i in 0..2) {
            val rp = ((clock * profile.pulseMult) + i * 0.34f) % 1f
            val r = (w * 0.5f) * rp
            val a = (1f - rp) * 0.35f * k
            if (a <= 0.001f || r <= 0f) continue
            drawCircle(
                color = profile.accent.copy(alpha = a),
                radius = r,
                center = Offset(cx, cy + vib),
                style = Stroke(width = w * 0.006f),
            )
        }
    }

    // --- 5. Energía: micro-destellos que titilan (eléctrico / hada) -----------------------
    if (profile.sparkles > 0) {
        for (i in 0 until profile.sparkles) {
            val fx = hash(i * 2 + 1)
            val fy = hash(i * 2 + 2)
            // Titileo por destello, desfasado con la semilla del índice; nunca un flash global.
            val tw = 0.5f + 0.5f * sin(angle * (profile.pulseMult + (i % 3)) + i)
            val a = tw * tw * 0.5f * k
            if (a <= 0.02f) continue
            val px = w * (0.12f + 0.76f * fx)
            val py = top + rh * (0.16f + 0.68f * fy)
            val dot = w * 0.006f * (0.6f + 0.8f * tw)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(profile.accent.copy(alpha = a), Color.Transparent),
                    center = Offset(px, py), radius = dot * 2.6f,
                ),
                radius = dot * 2.6f, center = Offset(px, py),
            )
        }
    }

    // --- Ondas de calor (fuego): bandas cálidas que ascienden, jamás llamas ---------------
    if (profile.shimmer) {
        for (i in 0..2) {
            val yBase = lerp(top + rh * 0.2f, bottom - rh * 0.2f, i / 2f)
            val drift = sin(angle + i * 1.7f) * (rh * 0.03f)
            val bandH = rh * 0.16f
            val yy = yBase + drift
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, profile.accent.copy(alpha = 0.06f * k * breath), Color.Transparent),
                    startY = yy - bandH / 2f, endY = yy + bandH / 2f,
                ),
                topLeft = Offset(0f, yy - bandH / 2f),
                size = Size(w, bandH),
            )
        }
    }

    // --- Neblina (oscuro): sombra suave a la deriva + oscurecimiento extra -----------------
    if (profile.mist) {
        val mx = cx + sin(angle) * (w * 0.12f)
        val my = cy + sin(angle * 2f) * (rh * 0.08f)
        drawRect(
            brush = Brush.radialGradient(
                listOf(Color.Black.copy(alpha = 0.28f * k), Color.Transparent),
                center = Offset(mx, my), radius = w * 0.5f,
            ),
            topLeft = Offset(0f, top),
            size = Size(w, rh),
        )
    }
}

/** Barrido de luz según [SweepKind]: oscila (nunca reinicia con salto) y se funde en los bordes. */
private fun DrawScope.drawSweep(
    profile: AmbientProfile,
    angle: Float,
    k: Float,
    top: Float,
    rh: Float,
    w: Float,
    vib: Float,
) {
    val a = 0.12f * k
    val stripe = profile.accent.copy(alpha = a)
    when (profile.sweep) {
        SweepKind.Horizontal, SweepKind.Diagonal -> {
            val bandW = w * 0.55f
            val cx = (0.5f + 0.42f * sin(angle * profile.sweepMult)) * w
            val startY = if (profile.sweep == SweepKind.Diagonal) top else top
            val endY = if (profile.sweep == SweepKind.Diagonal) top + rh else top
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, stripe, Color.Transparent),
                    start = Offset(cx - bandW / 2f, startY),
                    end = Offset(cx + bandW / 2f, endY + if (endY == startY) rh else 0f),
                ),
                topLeft = Offset(0f, top),
                size = Size(w, rh),
            )
        }
        SweepKind.Vertical -> {
            val bandH = rh * 0.5f
            val cy = top + (0.5f + 0.42f * sin(angle * profile.sweepMult)) * rh + vib
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, stripe, Color.Transparent),
                    startY = cy - bandH / 2f, endY = cy + bandH / 2f,
                ),
                topLeft = Offset(0f, top),
                size = Size(w, rh),
            )
        }
        SweepKind.None -> Unit
    }
}

/** Hash determinista 0..1 a partir de un entero (posiciones estables de los destellos). */
private fun hash(n: Int): Float {
    val s = sin(n * 12.9898f) * 43758.5453f
    return s - kotlin.math.floor(s)
}
