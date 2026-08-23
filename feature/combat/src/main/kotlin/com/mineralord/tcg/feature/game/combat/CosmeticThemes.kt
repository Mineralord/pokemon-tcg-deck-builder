package com.mineralord.tcg.feature.game.combat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.cos
import kotlin.math.sin

/**
 * TEMAS COSMÉTICOS del tablero (Fase 3 §8: personalización visual, neutralidad competitiva).
 *
 * Un [MatTheme] reviste el TAPETE (zonas del rival y del jugador + su enrejado + el fondo base)
 * SIN alterar el **lente gris central** —que es funcional y permanece idéntico— ni los rieles de
 * neón ni las proporciones (todo se dibuja por fracciones en [CombatMat]). Un [SleeveTheme] reviste
 * el DORSO de las cartas boca abajo. Ambos se seleccionan por el id del cosmético equipado; ids
 * desconocidos/null caen al tema por defecto (aspecto actual del juego).
 *
 * Los tipos son de PRESENTACIÓN (viven en feature:combat) y se mapean desde un String id, de modo
 * que el módulo no depende de `data:profile`: la capa de app le pasa el id equipado.
 */
data class MatTheme(
    val baseTop: Color,
    val baseBottom: Color,
    val foeTop: Color,
    val foeBottom: Color,
    val foeLattice: Color,
    val meTop: Color,
    val meBottom: Color,
    val meLattice: Color,
) {
    companion object {
        /** Aspecto original del juego (granate rival + navy jugador). */
        val Default = MatTheme(
            baseTop = Color(0xFF141A26), baseBottom = Color(0xFF090C12),
            foeTop = Color(0xFF5E1B24), foeBottom = Color(0xFF34121A), foeLattice = Color(0x12FFE0D0),
            meTop = Color(0xFF17264F), meBottom = Color(0xFF0B1224), meLattice = Color(0x1A9FC0FF),
        )
    }
}

/** Tema de tapete para el cosmético [id] equipado (o [MatTheme.Default] si null/desconocido). */
fun matThemeFor(id: String?): MatTheme = when (id) {
    // Arena (Común): desierto cálido, arena y ámbar; enrejado dorado tenue.
    "tapete-arena" -> MatTheme(
        baseTop = Color(0xFF211A12), baseBottom = Color(0xFF0F0B07),
        foeTop = Color(0xFF6E4A22), foeBottom = Color(0xFF3A2611), foeLattice = Color(0x14FFE7B0),
        meTop = Color(0xFF8A5E2A), meBottom = Color(0xFF33220F), meLattice = Color(0x1EFFD98A),
    )
    // Liga (Raro): índigo profundo estilo torneo, con retícula plateada.
    "tapete-liga" -> MatTheme(
        baseTop = Color(0xFF10162B), baseBottom = Color(0xFF070A16), foeLattice = Color(0x14CFE0FF),
        foeTop = Color(0xFF2A2F6B), foeBottom = Color(0xFF141636),
        meTop = Color(0xFF1C2E63), meBottom = Color(0xFF0A1024), meLattice = Color(0x1EBFD4FF),
    )
    // Campeón (Épico): carmesí-magenta intenso.
    "tapete-campeon" -> MatTheme(
        baseTop = Color(0xFF1E0E1A), baseBottom = Color(0xFF0C060B),
        foeTop = Color(0xFF7A1533), foeBottom = Color(0xFF3E0A1E), foeLattice = Color(0x16FFD0E0),
        meTop = Color(0xFF531A5E), meBottom = Color(0xFF230A2A), meLattice = Color(0x1EF0B8FF),
    )
    // Maestro (Legendario): oro y brasa, lujo cálido.
    "tapete-maestro" -> MatTheme(
        baseTop = Color(0xFF231803), baseBottom = Color(0xFF0F0A02),
        foeTop = Color(0xFF7E3B12), foeBottom = Color(0xFF3E1D08), foeLattice = Color(0x1AFFE3A0),
        meTop = Color(0xFF7A5A16), meBottom = Color(0xFF2C2008), meLattice = Color(0x24FFE79A),
    )
    else -> MatTheme.Default
}

// ============================ FUNDAS (DORSO DE CARTA) ============================

/** Estilo procedural del dorso; DEFAULT usa el arte oficial (`card_back_default`). */
enum class SleeveStyle { DEFAULT, SOLID, WAVE, PRISM, ECLIPSE }

/** Tema de funda: estilo + paleta + acento del emblema. */
data class SleeveTheme(
    val style: SleeveStyle,
    val colors: List<Color>,
    val accent: Color,
) {
    companion object {
        val Default = SleeveTheme(SleeveStyle.DEFAULT, emptyList(), Color.White)
    }
}

/** Tema de funda para el cosmético [id] equipado (o [SleeveTheme.Default]). */
fun sleeveFor(id: String?): SleeveTheme = when (id) {
    "funda-lisa" -> SleeveTheme(SleeveStyle.SOLID, listOf(Color(0xFFCFD8DC), Color(0xFF78909C)), Color(0xFFECEFF1))
    "funda-ola" -> SleeveTheme(SleeveStyle.WAVE, listOf(Color(0xFF4FC3F7), Color(0xFF0277BD)), Color(0xFFB3E5FC))
    "funda-prisma" -> SleeveTheme(SleeveStyle.PRISM, listOf(Color(0xFF00BCD4), Color(0xFF7C4DFF), Color(0xFFEC407A)), Color(0xFFFFFFFF))
    "funda-eclipse" -> SleeveTheme(SleeveStyle.ECLIPSE, listOf(Color(0xFF1A1A1A), Color(0xFF000000)), Color(0xFFFFC107))
    else -> SleeveTheme.Default
}

/** Funda equipada, provista una vez en la raíz del tablero para que todos los dorsos la lean. */
val LocalCardSleeve: ProvidableCompositionLocal<SleeveTheme> = compositionLocalOf { SleeveTheme.Default }

/** Provee el tema de funda a todo el subárbol (los dorsos lo consumen sin recablear). */
@Composable
fun ProvideCardSleeve(theme: SleeveTheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCardSleeve provides theme, content = content)
}

/**
 * Dorso de carta PROCEDURAL AAA (arte propio, sin assets), respetando las proporciones del slot.
 * Fondo por paleta + emblema circular central de dos tonos + motivo según el estilo. Bordes
 * redondeados y anillo interior para una lectura premium.
 */
@Composable
fun SleeveArt(theme: SleeveTheme, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val r = minOf(w, h) * 0.09f
        val round = androidx.compose.ui.geometry.CornerRadius(r, r)
        val outline = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, round))
        }
        clipPath(outline) {
            // Fondo por paleta.
            drawRect(Brush.verticalGradient(theme.colors.ifEmpty { listOf(Color(0xFF2A3550), Color(0xFF141A2A)) }))
            when (theme.style) {
                SleeveStyle.WAVE -> drawWaves(w, h, theme.accent)
                SleeveStyle.PRISM -> drawPrism(w, h, theme.colors)
                SleeveStyle.ECLIPSE -> drawEclipse(w, h, theme.accent)
                else -> Unit
            }
            drawSleeveEmblem(w, h, theme.accent)
        }
        // Marco interior + borde exterior premium.
        drawRoundRect(
            color = Color(0x33FFFFFF), topLeft = Offset(w * 0.045f, h * 0.045f),
            size = Size(w * 0.91f, h * 0.91f), cornerRadius = round, style = Stroke(w * 0.012f),
        )
        drawRoundRect(color = Color(0x66000000), cornerRadius = round, style = Stroke(w * 0.02f))
    }
}

/** Emblema circular central (evoca un dorso de TCG): anillo + disco de dos tonos. */
private fun DrawScope.drawSleeveEmblem(w: Float, h: Float, accent: Color) {
    val c = Offset(w / 2f, h / 2f)
    val rad = minOf(w, h) * 0.26f
    drawCircle(Color(0x22000000), radius = rad * 1.15f, center = c)
    drawCircle(
        brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.25f)), center = c, radius = rad),
        radius = rad, center = c,
    )
    drawCircle(Color.White.copy(alpha = 0.85f), radius = rad, center = c, style = Stroke(w * 0.018f))
    drawCircle(Color(0x55FFFFFF), radius = rad * 0.5f, center = c, style = Stroke(w * 0.012f))
}

/** Motivo de olas horizontales (funda Agua). */
private fun DrawScope.drawWaves(w: Float, h: Float, accent: Color) {
    val rows = 6
    for (i in 0..rows) {
        val y = h * i / rows
        val path = Path().apply {
            moveTo(0f, y)
            var x = 0f
            val step = w / 6f
            var up = true
            while (x <= w) {
                quadraticBezierTo(x + step / 2f, y + (if (up) -h * 0.03f else h * 0.03f), x + step, y)
                x += step; up = !up
            }
        }
        drawPath(path, accent.copy(alpha = 0.18f), style = Stroke(w * 0.01f, cap = StrokeCap.Round))
    }
}

/** Motivo de facetas diagonales tipo prisma (funda Épica). */
private fun DrawScope.drawPrism(w: Float, h: Float, colors: List<Color>) {
    val bands = 5
    for (i in 0 until bands) {
        val t = i / bands.toFloat()
        val col = colors[i % colors.size].copy(alpha = 0.22f)
        val path = Path().apply {
            moveTo(w * (t - 0.2f), 0f)
            lineTo(w * (t + 0.1f), 0f)
            lineTo(w * (t + 0.5f), h)
            lineTo(w * (t + 0.2f), h)
            close()
        }
        drawPath(path, col)
    }
}

/** Motivo de eclipse: anillo dorado con corona radial (funda Mítica de prestigio). */
private fun DrawScope.drawEclipse(w: Float, h: Float, accent: Color) {
    val c = Offset(w / 2f, h / 2f)
    val rad = minOf(w, h) * 0.4f
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.Transparent, accent.copy(alpha = 0.35f)), center = c, radius = rad),
        radius = rad, center = c,
    )
    val n = 24
    for (i in 0 until n) {
        val a = i / n.toFloat() * 6.2832f
        val inner = rad * 0.85f
        val outer = rad * (1.02f + (i % 2) * 0.06f)
        drawLine(
            accent.copy(alpha = 0.30f),
            Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
            Offset(c.x + cos(a) * outer, c.y + sin(a) * outer),
            strokeWidth = w * 0.006f,
        )
    }
}

// ============================ PREVIEWS PARA LA TIENDA (WYSIWYG) ============================

/**
 * Mini-representación fiel del TAPETE para la tienda: bandas rival/lente/jugador con el tema real,
 * de modo que lo que se ve en la ficha es lo que se verá en la partida. Proporciones aproximadas
 * a las de [CombatMat] (lente central gris).
 */
@Composable
fun MatThemePreview(id: String?, modifier: Modifier = Modifier) {
    val theme = matThemeFor(id)
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val r = minOf(w, h) * 0.08f
        val round = androidx.compose.ui.geometry.CornerRadius(r, r)
        val outline = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, round)) }
        clipPath(outline) {
            drawRect(Brush.verticalGradient(listOf(theme.baseTop, theme.baseBottom)))
            val foeH = h * 0.34f
            val meY = h * 0.62f
            drawRect(Brush.verticalGradient(listOf(theme.foeTop, theme.foeBottom), endY = foeH), size = Size(w, foeH))
            drawRect(
                Brush.verticalGradient(listOf(theme.meTop, theme.meBottom), startY = meY, endY = h),
                topLeft = Offset(0f, meY), size = Size(w, h - meY),
            )
            // Lente central gris (idéntico al del juego), como banda curva simplificada.
            val lens = Path().apply {
                moveTo(0f, foeH)
                quadraticBezierTo(w / 2f, foeH - h * 0.05f, w, foeH)
                lineTo(w, meY); quadraticBezierTo(w / 2f, meY + h * 0.05f, 0f, meY); close()
            }
            drawPath(lens, Brush.verticalGradient(listOf(Color(0xFF3A414E), Color(0xFF2C323D)), startY = foeH, endY = meY))
            // Rieles dorados sobre los arcos (como en la partida).
            drawArcRail(w, foeH, h * -0.05f)
            drawArcRail(w, meY, h * 0.05f)
        }
        drawRoundRect(color = Color(0x55000000), cornerRadius = round, style = Stroke(w * 0.02f))
    }
}

private fun DrawScope.drawArcRail(w: Float, edgeY: Float, bulge: Float) {
    val path = Path().apply { moveTo(0f, edgeY); quadraticBezierTo(w / 2f, edgeY + bulge, w, edgeY) }
    drawPath(path, Color(0xFFF6C43C).copy(alpha = 0.85f), style = Stroke(w * 0.012f, cap = StrokeCap.Round))
}

/** Previsualización de la FUNDA para la tienda (el dorso real). */
@Composable
fun SleevePreview(id: String?, modifier: Modifier = Modifier) {
    SleeveArt(sleeveFor(id), modifier)
}

// ============================ EFECTOS DE VICTORIA / DERROTA ============================

/** Motivo de la celebración/derrota (partículas procedurales). */
enum class CelebrationMotif { CONFETTI, FIREWORKS, PETALS, EMBERS, ASH }

/** Tema de un efecto de fin de partida (color + motivo), mapeado por id de cosmético. */
data class CelebrationTheme(val colors: List<Color>, val motif: CelebrationMotif)

/** Efecto de VICTORIA equipado (o el confeti dorado por defecto). */
fun victoryEffectFor(id: String?): CelebrationTheme = when (id) {
    "victoria-fuegos" -> CelebrationTheme(listOf(Color(0xFFFFD54F), Color(0xFFFF4081), Color(0xFF40C4FF)), CelebrationMotif.FIREWORKS)
    "victoria-petalos" -> CelebrationTheme(listOf(Color(0xFFFF80AB), Color(0xFFF48FB1), Color(0xFFFFFFFF)), CelebrationMotif.PETALS)
    "victoria-confeti", null -> CelebrationTheme(listOf(Color(0xFFFFD54F), Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFFEC407A)), CelebrationMotif.CONFETTI)
    else -> CelebrationTheme(listOf(Color(0xFFFFD54F), Color(0xFF66BB6A), Color(0xFF42A5F5)), CelebrationMotif.CONFETTI)
}

/** Efecto de DERROTA equipado (o las brasas grises por defecto). */
fun defeatEffectFor(id: String?): CelebrationTheme = when (id) {
    "derrota-cenizas" -> CelebrationTheme(listOf(Color(0xFF9E9E9E), Color(0xFF616161)), CelebrationMotif.ASH)
    else -> CelebrationTheme(listOf(Color(0xFF8D6E63), Color(0xFFEF6C00)), CelebrationMotif.EMBERS)
}

private data class FxParticle(
    val x: Float, val size: Float, val speed: Float, val phase: Float, val drift: Float, val colorIdx: Int,
)

/**
 * Efecto de fin de partida PROCEDURAL a pantalla completa (arte propio, sin assets). Confeti/
 * fuegos/pétalos que caen en victoria; brasas/cenizas que ascienden o descienden en derrota.
 * Un único reloj continuo alimenta todas las partículas (barato). Se dibuja DETRÁS del panel.
 */
@Composable
fun CosmeticCelebrationFx(theme: CelebrationTheme, modifier: Modifier = Modifier) {
    val rng = remember(theme) { java.util.Random(theme.motif.ordinal * 1009L) }
    val particles = remember(theme) {
        List(46) {
            FxParticle(
                x = rng.nextFloat(),
                size = 0.6f + rng.nextFloat(),
                speed = 0.5f + rng.nextFloat(),
                phase = rng.nextFloat(),
                drift = (rng.nextFloat() - 0.5f),
                colorIdx = if (theme.colors.isEmpty()) 0 else rng.nextInt(theme.colors.size),
            )
        }
    }
    val clock by rememberInfiniteTransition(label = "fx").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "fxClock",
    )
    val rising = theme.motif == CelebrationMotif.EMBERS || theme.motif == CelebrationMotif.ASH
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val t = (clock * p.speed + p.phase) % 1f
            val y = if (rising) h * (1f - t) else h * t
            val x = w * (p.x + p.drift * 0.15f * kotlin.math.sin((t + p.phase) * 6.2832f))
            val col = theme.colors.getOrElse(p.colorIdx) { Color.White }
            val r = w * 0.010f * p.size
            val alpha = (1f - kotlin.math.abs(t - 0.5f) * 1.4f).coerceIn(0f, 1f)
            when (theme.motif) {
                CelebrationMotif.CONFETTI, CelebrationMotif.ASH -> drawRect(
                    col.copy(alpha = alpha),
                    topLeft = Offset(x, y), size = Size(r * 1.6f, r * 2.6f),
                )
                CelebrationMotif.PETALS -> drawCircle(col.copy(alpha = alpha), radius = r * 1.4f, center = Offset(x, y))
                CelebrationMotif.EMBERS -> {
                    drawCircle(col.copy(alpha = alpha), radius = r, center = Offset(x, y))
                    drawCircle(Color.White.copy(alpha = alpha * 0.5f), radius = r * 0.4f, center = Offset(x, y))
                }
                CelebrationMotif.FIREWORKS -> {
                    // Estallidos radiales que laten con el reloj.
                    val burst = (t * 1.6f).coerceAtMost(1f)
                    val n = 9
                    for (i in 0 until n) {
                        val a = i / n.toFloat() * 6.2832f
                        val rr = w * 0.10f * burst * p.size
                        drawCircle(
                            col.copy(alpha = (1f - burst) * alpha),
                            radius = r * 0.7f,
                            center = Offset(x + kotlin.math.cos(a) * rr, y + kotlin.math.sin(a) * rr),
                        )
                    }
                }
            }
        }
    }
}

/** Previsualización de un efecto de VICTORIA/DERROTA para la tienda. */
@Composable
fun CelebrationPreview(id: String?, victory: Boolean, modifier: Modifier = Modifier) {
    val theme = if (victory) victoryEffectFor(id) else defeatEffectFor(id)
    Canvas(modifier.fillMaxSize()) {
        // Muestra estática: unas cuantas partículas del color/motivo (sin animar).
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF11161F), Color(0xFF0A0E14))))
        val cols = theme.colors
        for (i in 0 until 18) {
            val col = cols[i % cols.size]
            val x = w * ((i * 53 % 100) / 100f)
            val y = h * ((i * 31 % 100) / 100f)
            drawCircle(col.copy(alpha = 0.9f), radius = w * 0.02f, center = Offset(x, y))
        }
    }
}
