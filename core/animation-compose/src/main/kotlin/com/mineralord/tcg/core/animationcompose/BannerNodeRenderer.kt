package com.mineralord.tcg.core.animationcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Overshoot de asentado (easeOutBack); [emphasis] 0f→sobrio, 1f→dramático. Pura y testeable. */
fun bannerSettle(t: Float, emphasis: Float): Float {
    val c1 = 1.70158f * (0.35f + emphasis)
    val c3 = c1 + 1f
    val x = t.coerceIn(0f, 1f) - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}

/**
 * NodeRenderer del rótulo de anuncio ([BannerRenderNode]) — lower-third cinematográfico tipo
 * Valorant / Legends of Runeterra / Overwatch. Responsabilidad única: dibujar; el tiempo ya viene
 * en `progress`. Sustituye la placa RECTANGULAR por una **placa en paralelogramo biselado** con
 * **slash de acento** en el borde de ataque, hairline de bisel y **sheen** que cruza en el sostenido;
 * y la sans genérica por la display **Rajdhani** (HUD AAA), en versalitas.
 *
 * DIRECCIÓN: `visual.fromRight` (rival→mí) barre entrante desde la derecha con el slash a la derecha;
 * en caso contrario (mí→rival) barre desde la izquierda con el slash a la izquierda.
 */
@Composable
fun BannerNodeRenderer(node: BannerRenderNode) {
    val v = node.visual
    val p = node.progress

    // Línea temporal: barrido-in → sostenido → barrido-out.
    val inEnd = 0.16f
    val outStart = 0.82f
    val enter = (p / inEnd).coerceIn(0f, 1f)
    val exit = ((p - outStart) / (1f - outStart)).coerceIn(0f, 1f)
    val alpha = (enter * (1f - exit)).coerceIn(0f, 1f)
    val settle = bannerSettle(enter, v.emphasis)
    val dir = if (v.fromRight) 1f else -1f
    val slideFraction = dir * ((1f - settle) * 0.6f + exit * 0.18f)

    val hue = v.accentHue.coerceIn(0f, 360f)
    val hue2 = v.secondaryHue.coerceIn(0f, 360f)
    val accent = Color.hsv(hue, 0.85f, 1f)
    val deep = Color.hsv(hue, 0.9f, 0.28f)
    val glowSecondary = Color.hsv(hue2, 0.8f, 1f)
    val holdT = ((p - inEnd) / (outStart - inEnd)).coerceIn(0f, 1f)
    val fromRight = v.fromRight

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val padH = 46.dp
        val title = node.title.uppercase()
        val baseTitle = 40.sp
        // Auto-fit: mide el título a tamaño base y REDUCE la escala si excede el ancho disponible
        // (ancho de la placa menos padding y el hueco del slash). Garantiza que nunca se recorte
        // horizontalmente, manteniendo el mismo diseño visual (sólo cambia la escala tipográfica).
        val availPx = with(density) { (maxWidth - padH * 2 - 30.dp).toPx() }.coerceAtLeast(1f)
        val titleStyleBase = TextStyle(
            fontFamily = RajdhaniFamily,
            fontSize = baseTitle,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        val measuredW = measurer.measure(title, titleStyleBase).size.width.toFloat()
        val fit = if (measuredW > availPx) (availPx / measuredW).coerceIn(0.42f, 1f) else 1f
        val titleSize = baseTitle * fit
        val kickerSize = 14.sp * fit.coerceAtLeast(0.72f)

        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center) // SIEMPRE centrado exactamente en el medio del tapete
                .graphicsLayer {
                    this.alpha = alpha
                    translationX = slideFraction * size.width
                }
                .height(108.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val skew = h * 0.42f // pendiente horizontal del paralelogramo

                // Placa: paralelogramo con lados inclinados (top y bottom horizontales).
                val plate = Path().apply {
                    moveTo(skew, 0f); lineTo(w, 0f); lineTo(w - skew, h); lineTo(0f, h); close()
                }
                val plateBrush = if (fromRight) {
                    Brush.horizontalGradient(0f to Color.Transparent, 0.4f to deep.copy(alpha = 0.94f), 1f to deep)
                } else {
                    Brush.horizontalGradient(0f to deep, 0.6f to deep.copy(alpha = 0.94f), 1f to Color.Transparent)
                }
                drawPath(plate, brush = plateBrush)
                // Bisel: sombreado vertical (arriba luz, abajo sombra).
                drawPath(
                    plate,
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.10f),
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.22f),
                    ),
                )

                // Slash de acento en el borde de ATAQUE.
                val accW = 22.dp.toPx()
                val slash = Path().apply {
                    if (!fromRight) {
                        moveTo(skew, 0f); lineTo(skew + accW, 0f); lineTo(accW, h); lineTo(0f, h)
                    } else {
                        moveTo(w - accW, 0f); lineTo(w, 0f); lineTo(w - skew, h); lineTo(w - skew - accW, h)
                    }
                    close()
                }
                drawPath(slash, color = accent)
                // Halo del acento hacia el interior de la placa.
                clipPath(plate) {
                    drawRect(
                        brush = if (!fromRight) {
                            Brush.horizontalGradient(0f to accent.copy(alpha = 0.30f), 1f to Color.Transparent)
                        } else {
                            Brush.horizontalGradient(0f to Color.Transparent, 1f to accent.copy(alpha = 0.30f))
                        },
                    )
                    // Sheen: streak que cruza durante el sostenido (recortado a la placa).
                    if (v.sheen > 0f && holdT > 0f && holdT < 1f) {
                        val cx = holdT * w
                        val half = w * 0.12f
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.5f to Color.White.copy(alpha = 0.22f * v.sheen),
                                1f to Color.Transparent,
                                startX = cx - half,
                                endX = cx + half,
                            ),
                        )
                    }
                }

                // Hairline de bisel en el borde superior + rim del contorno.
                drawLine(accent.copy(alpha = 0.9f), Offset(skew, 1.5f), Offset(w, 1.5f), strokeWidth = 2f)
                drawPath(plate, color = accent.copy(alpha = 0.35f), style = Stroke(width = 1.5f))
            }

            // Textos display (Rajdhani), versalitas. Escala auto-ajustada; una sola línea, sin recorte.
            Column(
                Modifier.fillMaxSize().padding(horizontal = padH),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = if (fromRight) Alignment.End else Alignment.Start,
            ) {
                BasicText(
                    text = node.kicker.uppercase(),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    style = TextStyle(
                        fontFamily = RajdhaniFamily,
                        color = glowSecondary,
                        fontSize = kickerSize,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 5.sp,
                        textAlign = if (fromRight) TextAlign.End else TextAlign.Start,
                    ),
                )
                BasicText(
                    text = title,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    style = TextStyle(
                        fontFamily = RajdhaniFamily,
                        color = Color.White,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = if (fromRight) TextAlign.End else TextAlign.Start,
                    ),
                )
            }
        }
    }
}
