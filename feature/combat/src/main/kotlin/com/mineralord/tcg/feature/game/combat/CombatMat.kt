package com.mineralord.tcg.feature.game.combat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.mineralord.tcg.core.designsystem.motion.AnimationCurves
import com.mineralord.tcg.core.designsystem.motion.AnimationManager
import com.mineralord.tcg.core.designsystem.motion.AnimationSpecs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.Side
import kotlinx.coroutines.launch

/**
 * TAPETE ORIGINAL dibujado en Compose (Canvas), evocando la DISPOSICIÓN de TCG Live
 * con su perspectiva 3D: zona del RIVAL más pequeña (arriba), gran PANEL CENTRAL curvo
 * (lente) donde se enfrentan los dos Activos, y zona del JUGADOR (abajo). Arte PROPIO,
 * responsive (todo por fracciones); sin bitmap horneado ni assets de terceros.
 *
 * La lente central usa las fracciones MEDIDAS de las referencias:
 *   arco superior (rival)   ∪  bordes y≈0.2594, centro y≈0.2894
 *   arco inferior (jugador) ∩  bordes y≈0.5635, centro y≈0.5365
 * Los Activos (BoardGeometry.OppActive 0.268..0.408 y MeActive 0.415..0.555) quedan
 * DENTRO de esta lente; las bancas quedan fuera (rival arriba, jugador abajo).
 *
 * Sin bucles de animación: los rieles de neón y la lente son ESTÁTICOS. El único
 * movimiento es REACTIVO: al tocar la lente nace una onda de agua (ripple) plateada
 * bajo el dedo que se expande y se desvanece (~650 ms). Toque premium, cero ruido en reposo.
 */
@Composable
fun CombatMat(
    modifier: Modifier = Modifier,
    litSide: Side? = null,
    activeType: EnergyType? = null,
    theme: MatTheme = MatTheme.Default,
) {
    // Ondas activas nacidas de toques sobre la lente (cada una con su propio progreso 0→1).
    val ripples = remember { mutableStateListOf<LensRipple>() }
    val scope = rememberCoroutineScope()

    // ---- Ambiente reactivo de la lente (procedural, por capas, según el tipo Activo) ----
    val motionOn = AnimationManager.enabled()
    // Reloj continuo 0→1 (bucle lineal): única fuente de movimiento del ambiente.
    val clock = if (motionOn) {
        rememberInfiniteTransition(label = "ambientClock").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = AnimationSpecs.loopLinear(AMBIENT_CLOCK_MS),
            label = "ambientClockPhase",
        ).value
    } else {
        0.25f // Iluminación estática elegante cuando el movimiento está desactivado.
    }
    // Crossfade entre el ambiente anterior y el nuevo al cambiar el tipo del Activo.
    var curType by remember { mutableStateOf(activeType) }
    var oldType by remember { mutableStateOf<EnergyType?>(null) }
    val blend = remember { Animatable(1f) }
    LaunchedEffect(activeType) {
        if (activeType != curType) {
            oldType = curType
            curType = activeType
            if (motionOn) {
                blend.snapTo(0f)
                blend.animateTo(1f, tween(AMBIENT_CROSSFADE_MS, easing = AnimationCurves.EmphasizedDecelerate))
            } else {
                blend.snapTo(1f)
            }
        }
    }
    val curProfile = ambientProfileFor(curType)
    val oldProfile = ambientProfileFor(oldType)
    val blendV = blend.value

    Canvas(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    // Solo dentro de la banda vertical de la lente (borde sup→inf).
                    val topEdge = 0.2594f * size.height
                    val botEdge = 0.5635f * size.height
                    if (pos.y in topEdge..botEdge) {
                        val ripple = LensRipple(origin = pos, progress = Animatable(0f))
                        ripples.add(ripple)
                        scope.launch {
                            ripple.progress.animateTo(1f, tween(650, easing = AnimationCurves.Standard))
                            ripples.remove(ripple)
                        }
                    }
                }
            },
    ) {
        val w = size.width
        val h = size.height

        // Base general (tema).
        drawRect(Brush.verticalGradient(listOf(theme.baseTop, theme.baseBottom)))

        // Arcos que delimitan la lente central (control = 2*centro - borde).
        val topEdge = 0.2594f * h
        val topCtrl = (2f * 0.2894f - 0.2594f) * h
        val botEdge = 0.5635f * h
        val botCtrl = (2f * 0.5365f - 0.5635f) * h

        // Zona del RIVAL (granate) — pequeña, arriba, hasta el arco superior.
        val foeTop = 0f
        val foeBottom = topCtrl
        drawRect(
            Brush.verticalGradient(listOf(theme.foeTop, theme.foeBottom), startY = foeTop, endY = foeBottom),
            topLeft = Offset(0f, foeTop),
            size = Size(w, foeBottom - foeTop),
        )
        // Textura de rombos acolchados, tenue y cálida (como el original, más sutil que el jugador).
        clipRect(0f, foeTop, w, foeBottom) {
            drawDiamondLattice(w, foeTop, foeBottom, w * 0.11f, theme.foeLattice, 1f)
        }

        // Zona del JUGADOR (azul marino) — desde el arco inferior hasta antes de la mano.
        val meTop = botCtrl
        val meBottom = 0.845f * h
        drawRect(
            Brush.verticalGradient(listOf(theme.meTop, theme.meBottom), startY = meTop, endY = meBottom),
            topLeft = Offset(0f, meTop),
            size = Size(w, meBottom - meTop),
        )
        // Enrejado de rombos (argyle) claro sobre la zona del jugador — la textura más visible del tapete.
        clipRect(0f, meTop, w, meBottom) {
            drawDiamondLattice(w, meTop, meBottom, w * 0.11f, theme.meLattice, 1.4f)
        }

        // Lente central (gris plateado claro, casi liso) — panel curvo grande.
        val lens = Path().apply {
            moveTo(0f, topEdge)
            quadraticBezierTo(w / 2f, topCtrl, w, topEdge)
            lineTo(w, botEdge)
            quadraticBezierTo(w / 2f, botCtrl, 0f, botEdge)
            close()
        }
        drawPath(
            lens,
            Brush.verticalGradient(listOf(Color(0xFF3A414E), Color(0xFF2C323D)), startY = topEdge, endY = botEdge),
        )
        // Veta de rombos MUY tenue recortada a la lente (casi imperceptible, no un panal marcado)
        // + las ondas de agua reactivas al toque, recortadas a la forma de la lente.
        clipPath(lens) {
            drawDiamondLattice(w, topEdge - h * 0.03f, botEdge + h * 0.03f, w * 0.11f, Color(0x0AFFFFFF), 1f)
            // Ambiente procedural del tipo Activo (bajo las ondas de toque, sobre la veta).
            // Durante un cambio de tipo se mezclan el ambiente saliente y el entrante.
            drawLensAmbient(oldProfile, clock, 1f - blendV, topEdge, botEdge, w)
            drawLensAmbient(curProfile, clock, blendV, topEdge, botEdge, w)
            ripples.forEach { drawLensRipple(it.origin, it.progress.value, w) }
        }

        // Rieles: tubos de neón GRUESOS sobre los dos arcos de la lente. El lado ACTIVO se
        // enciende (brillante, ESTÁTICO); el otro queda apagado (oro oscuro tenue).
        drawNeonRail(w, h, topEdge, topCtrl, lit = litSide == Side.OPPONENT)
        drawNeonRail(w, h, botEdge, botCtrl, lit = litSide == Side.PLAYER)
    }
}

/** Periodo del reloj continuo del ambiente (bucle lineal, movimiento base lento y elegante). */
private const val AMBIENT_CLOCK_MS = 5200

/** Duración de la mezcla entre el ambiente saliente y el entrante al cambiar de tipo Activo. */
private const val AMBIENT_CROSSFADE_MS = 900

/** Una onda de agua nacida de un toque: su origen y su progreso animado 0→1. */
private data class LensRipple(val origin: Offset, val progress: Animatable<Float, *>)

/**
 * Onda de agua PREMIUM: anillos plateados concéntricos que nacen bajo el dedo, se expanden
 * y se desvanecen. Un bloom suave al inicio da el "impacto" del toque. Recortada a la lente
 * por el llamador. [p] = progreso 0→1, [w] = ancho para escalar el radio máximo.
 */
private fun DrawScope.drawLensRipple(origin: Offset, p: Float, w: Float) {
    val silver = Color(0xFFCFE4FF)
    val maxR = w * 0.42f
    // Bloom central: destello suave que se apaga rápido (da cuerpo al toque).
    val bloomA = (1f - p * 2.2f).coerceIn(0f, 1f)
    if (bloomA > 0f) {
        val bloomR = w * 0.10f * (0.6f + p)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.55f * bloomA), silver.copy(alpha = 0f)),
                center = origin, radius = bloomR,
            ),
            radius = bloomR, center = origin,
        )
    }
    // Tres anillos concéntricos desfasados: cada uno crece y se afina al desvanecerse.
    for (k in 0..2) {
        val pp = (p + k * 0.16f)
        if (pp > 1f) continue
        val r = maxR * pp
        val a = (1f - pp) * 0.75f
        if (a <= 0f) continue
        drawCircle(
            color = silver.copy(alpha = a),
            radius = r,
            center = origin,
            style = Stroke(width = (w * 0.010f) * (1f - pp * 0.6f)),
        )
    }
}

/**
 * Riel-tubo de neón siguiendo un arco cuadrático (borde→centro→borde). Grosor proporcional
 * al alto (como el TurnArc de la pantalla clásica). ESTÁTICO (sin pulso ni destello viajero).
 * Si [lit] está encendido: halo difuso + cuerpo oro brillante + highlight glossy; si no,
 * tubo oro oscuro apagado.
 */
private fun DrawScope.drawNeonRail(
    w: Float,
    h: Float,
    edgeY: Float,
    ctrlY: Float,
    lit: Boolean,
) {
    val path = Path().apply {
        moveTo(0f, edgeY)
        quadraticBezierTo(w / 2f, ctrlY, w, edgeY)
    }
    if (lit) {
        val gold = Color(0xFFF6C43C)
        // Halo exterior difuso (varias pasadas anchas y tenues).
        drawPath(path, gold.copy(alpha = 0.16f), style = Stroke(width = h * 0.030f, cap = StrokeCap.Round))
        drawPath(path, gold.copy(alpha = 0.30f), style = Stroke(width = h * 0.020f, cap = StrokeCap.Round))
        // Cuerpo del tubo dorado.
        drawPath(path, gold.copy(alpha = 0.95f), style = Stroke(width = h * 0.011f, cap = StrokeCap.Round))
        // Línea de brillo superior (highlight glossy).
        val hi = Path().apply {
            moveTo(0f, edgeY - h * 0.002f)
            quadraticBezierTo(w / 2f, ctrlY - h * 0.002f, w, edgeY - h * 0.002f)
        }
        drawPath(hi, Color(0xFFFFF0B4).copy(alpha = 0.85f), style = Stroke(width = h * 0.0035f, cap = StrokeCap.Round))
    } else {
        // Tubo apagado: oro oscuro, mismo grosor de cuerpo para conservar la forma física.
        val dim = Color(0xFF6E5A24)
        drawPath(path, dim.copy(alpha = 0.55f), style = Stroke(width = h * 0.011f, cap = StrokeCap.Round))
        drawPath(path, Color(0xFF3A2F14).copy(alpha = 0.6f), style = Stroke(width = h * 0.004f, cap = StrokeCap.Round))
    }
}

/**
 * Enrejado de rombos (argyle/acolchado): dos familias de líneas diagonales cruzadas que
 * forman una retícula de diamantes, recortado por el llamador a la banda [top, bottom].
 * [cell] = paso entre líneas del mismo sentido.
 */
private fun DrawScope.drawDiamondLattice(
    w: Float,
    top: Float,
    bottom: Float,
    cell: Float,
    color: Color,
    stroke: Float,
) {
    val hSpan = bottom - top
    // Diagonales "/" : x + y = k  → parametrizadas por el intercepto en el eje.
    var k = top
    while (k <= w + hSpan + cell) {
        drawLine(color, Offset(k - top, top), Offset(k - bottom, bottom), strokeWidth = stroke)
        k += cell
    }
    // Diagonales "\" : x - y = k.
    k = -hSpan
    while (k <= w + cell) {
        drawLine(color, Offset(k + top, top), Offset(k + bottom, bottom), strokeWidth = stroke)
        k += cell
    }
}
