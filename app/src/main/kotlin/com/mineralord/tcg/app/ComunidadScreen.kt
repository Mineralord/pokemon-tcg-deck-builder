package com.mineralord.tcg.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.BarajasPalette

/**
 * Pantalla principal "Comunidad" (antes "Amigos"). Réplica del hub social de TCG Pocket:
 *
 *   - Header: banda verde inclinada con el título "Comunidad".
 *   - Hero: diorama placeholder (suelo cuadriculado en perspectiva); el render 3D real es raster.
 *   - Acciones (jerarquía en 3 niveles, misma posición/proporción que el original):
 *       · "Galerías públicas"  → tarjeta elevada y centrada (nivel 1).
 *       · "Compartir" + "Intercambio" → par simétrico 50/50 (nivel 2).
 *       · "Amigos" → chip pequeño abajo-izquierda, pegado a la barra, icono oscuro (nivel 3).
 *
 * El arte de los tres primeros iconos es line-art con degradado verde lima→esmeralda; el de
 * "Amigos" es monocromo oscuro (coherente con la barra inferior), separándolo semánticamente.
 */
private object ComunidadPalette {
    val BandTop = Color(0xFF34C759)
    val BandBottom = Color(0xFF12A150)
    val Grid = Color(0xFF3FB865)
    val GridBg = Color(0xFF2FA84F)
    val Surface = Color(0xFFF4F7FA)
    val Ink = BarajasPalette.Ink
    val Label = Color(0xFF3A4A5C)
    val GreenHi = Color(0xFF8DE86B)
    val GreenLo = Color(0xFF22C55E)
    val AmigosInk = Color(0xFF3C4A5E)
}

@Composable
fun ComunidadScreen(
    modifier: Modifier = Modifier,
    onGalerias: () -> Unit = {},
    onCompartir: () -> Unit = {},
    onIntercambio: () -> Unit = {},
    onAmigos: () -> Unit = {},
) {
    Column(modifier.fillMaxSize().background(ComunidadPalette.Surface)) {
        // ---- Hero: banda inclinada + suelo cuadriculado en perspectiva ----
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    Brush.verticalGradient(
                        listOf(ComunidadPalette.BandTop, ComunidadPalette.GridBg, ComunidadPalette.Surface),
                    ),
                ),
        ) {
            PerspectiveGrid(Modifier.fillMaxSize())
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, top = 8.dp)) {
                Text(
                    "Comunidad",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 40.sp,
                )
            }
        }

        // ---- Zona de acciones ----
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 12.dp),
        ) {
            // Nivel 1 — Galerías públicas (centrada, elevada).
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ActionCard(
                    label = "Galerías públicas",
                    width = 0.62f,
                    onClick = onGalerias,
                ) { GaleriasIcon() }
            }
            Spacer(Modifier.height(12.dp))

            // Nivel 2 — Compartir + Intercambio (par 50/50).
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionCard("Compartir", Modifier.weight(1f), onClick = onCompartir) { CompartirIcon() }
                ActionCard("Intercambio", Modifier.weight(1f), onClick = onIntercambio) { IntercambioIcon() }
            }
            Spacer(Modifier.height(12.dp))

            // Nivel 3 — Amigos (chip pequeño abajo-izquierda, pegado a la barra).
            Box(Modifier.fillMaxWidth()) {
                AmigosChip(onClick = onAmigos)
            }
        }
    }
}

// ===========================================================================
//  COMPONENTES DE ACCIÓN
// ===========================================================================

/** Tarjeta grande (Galerías / Compartir / Intercambio): icono verde arriba + label debajo. */
@Composable
private fun ActionCard(
    label: String,
    modifier: Modifier = Modifier,
    width: Float? = null,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val base = (if (width != null) modifier.fillMaxWidth(width) else modifier)
        .clip(RoundedCornerShape(18.dp))
        .background(Color.White)
        .clickable(onClick = onClick)
        .padding(vertical = 14.dp)
    Column(
        base.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.height(8.dp))
        Text(label, color = ComunidadPalette.Label, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/** Chip compacto "Amigos": icono oscuro de dos bustos + label. */
@Composable
private fun AmigosChip(onClick: () -> Unit) {
    Column(
        Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) { AmigosBustsIcon() }
        Spacer(Modifier.height(6.dp))
        Text("Amigos", color = ComunidadPalette.Label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

// ===========================================================================
//  ARTE VECTORIAL — line-art verde (degradado lima → esmeralda)
// ===========================================================================

private fun DrawScope.greenBrush() = Brush.verticalGradient(
    listOf(ComunidadPalette.GreenHi, ComunidadPalette.GreenLo),
    startY = 0f,
    endY = size.height,
)

/** Galerías públicas: carta enmarcada + postes con cordón (barrera de museo) + destello. */
@Composable
private fun GaleriasIcon() {
    Canvas(Modifier.size(44.dp)) {
        val w = size.width; val h = size.height
        val brush = greenBrush()
        val st = Stroke(width = w * 0.065f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Carta / pantalla enmarcada.
        drawRoundRect(
            brush, topLeft = Offset(w * 0.34f, h * 0.06f), size = Size(w * 0.34f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.06f), style = st,
        )
        drawRoundRect(
            brush, topLeft = Offset(w * 0.41f, h * 0.14f), size = Size(w * 0.20f, h * 0.30f),
            cornerRadius = CornerRadius(w * 0.03f), style = Stroke(width = w * 0.05f),
        )
        // Postes con cordón (barrera).
        drawLine(brush, Offset(w * 0.24f, h * 0.70f), Offset(w * 0.24f, h * 0.90f), w * 0.06f, StrokeCap.Round)
        drawLine(brush, Offset(w * 0.78f, h * 0.70f), Offset(w * 0.78f, h * 0.90f), w * 0.06f, StrokeCap.Round)
        drawCircle(brush, w * 0.045f, Offset(w * 0.24f, h * 0.68f))
        drawCircle(brush, w * 0.045f, Offset(w * 0.78f, h * 0.68f))
        val cord = Path().apply {
            moveTo(w * 0.24f, h * 0.72f)
            quadraticBezierTo(w * 0.51f, h * 0.92f, w * 0.78f, h * 0.72f)
        }
        drawPath(cord, brush, style = st)
        // Destello (sparkle 4 puntas).
        sparkle(w * 0.12f, h * 0.34f, w * 0.09f, brush)
    }
}

/** Compartir: carta emergiendo de una caja abierta en perspectiva. */
@Composable
private fun CompartirIcon() {
    Canvas(Modifier.size(44.dp)) {
        val w = size.width; val h = size.height
        val brush = greenBrush()
        val st = Stroke(width = w * 0.065f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Carta saliendo (grande, con marco interior).
        drawRoundRect(
            brush, topLeft = Offset(w * 0.33f, h * 0.06f), size = Size(w * 0.34f, h * 0.42f),
            cornerRadius = CornerRadius(w * 0.06f), style = st,
        )
        drawRoundRect(
            brush, topLeft = Offset(w * 0.40f, h * 0.14f), size = Size(w * 0.20f, h * 0.26f),
            cornerRadius = CornerRadius(w * 0.03f), style = Stroke(width = w * 0.045f),
        )
        // Caja abierta: cuerpo (rect redondeado) + solapas divergentes.
        drawRoundRect(
            brush, topLeft = Offset(w * 0.22f, h * 0.60f), size = Size(w * 0.56f, h * 0.30f),
            cornerRadius = CornerRadius(w * 0.05f), style = st,
        )
        // Boca de la caja (V hacia el centro) y solapas abiertas hacia fuera.
        val mouth = Path().apply {
            moveTo(w * 0.10f, h * 0.50f)   // solapa izquierda arriba
            lineTo(w * 0.22f, h * 0.60f)   // esquina izquierda de la boca
            lineTo(w * 0.50f, h * 0.68f)   // fondo de la boca
            lineTo(w * 0.78f, h * 0.60f)   // esquina derecha de la boca
            lineTo(w * 0.90f, h * 0.50f)   // solapa derecha arriba
        }
        drawPath(mouth, brush, style = st)
    }
}

/** Intercambio: dos cartas en diagonal + dos flechas curvas de rotación (swap). */
@Composable
private fun IntercambioIcon() {
    Canvas(Modifier.size(44.dp)) {
        val w = size.width; val h = size.height
        val brush = greenBrush()
        val st = Stroke(width = w * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Carta superior derecha.
        drawRoundRect(
            brush, topLeft = Offset(w * 0.52f, h * 0.12f), size = Size(w * 0.28f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.05f), style = st,
        )
        // Carta inferior izquierda.
        drawRoundRect(
            brush, topLeft = Offset(w * 0.20f, h * 0.54f), size = Size(w * 0.28f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.05f), style = st,
        )
        // Flecha izquierda (sube).
        val up = Path().apply {
            moveTo(w * 0.16f, h * 0.60f)
            quadraticBezierTo(w * 0.06f, h * 0.36f, w * 0.30f, h * 0.24f)
        }
        drawPath(up, brush, style = st)
        arrowHead(w * 0.30f, h * 0.24f, w * 0.10f, brush, up = true)
        // Flecha derecha (baja).
        val down = Path().apply {
            moveTo(w * 0.84f, h * 0.44f)
            quadraticBezierTo(w * 0.94f, h * 0.68f, w * 0.70f, h * 0.80f)
        }
        drawPath(down, brush, style = st)
        arrowHead(w * 0.70f, h * 0.80f, w * 0.10f, brush, up = false)
    }
}

/** Amigos: dos bustos monocromos oscuros (no verde). */
@Composable
private fun AmigosBustsIcon() {
    Canvas(Modifier.size(28.dp)) {
        val w = size.width; val h = size.height
        val ink = ComunidadPalette.AmigosInk
        val st = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Busto trasero (der., más arriba).
        drawCircle(ink, w * 0.13f, Offset(w * 0.64f, h * 0.34f), style = st)
        drawArc(ink, 180f, 180f, false, topLeft = Offset(w * 0.46f, h * 0.48f), size = Size(w * 0.36f, h * 0.40f), style = st)
        // Busto delantero (izq., más abajo, tapa parcialmente).
        drawCircle(ink, w * 0.15f, Offset(w * 0.38f, h * 0.44f))
        val body = Path().apply {
            arcTo(Rect(Offset(w * 0.16f, h * 0.60f), Size(w * 0.44f, h * 0.50f)), 180f, 180f, false)
        }
        drawPath(body, ink)
    }
}

// ---- Primitivas compartidas ----

private fun DrawScope.sparkle(cx: Float, cy: Float, r: Float, brush: Brush) {
    val p = Path().apply {
        moveTo(cx, cy - r)
        quadraticBezierTo(cx, cy, cx + r, cy)
        quadraticBezierTo(cx, cy, cx, cy + r)
        quadraticBezierTo(cx, cy, cx - r, cy)
        quadraticBezierTo(cx, cy, cx, cy - r)
        close()
    }
    drawPath(p, brush)
}

private fun DrawScope.arrowHead(x: Float, y: Float, s: Float, brush: Brush, up: Boolean) {
    val dy = if (up) 1f else -1f
    val head = Path().apply {
        moveTo(x, y)
        lineTo(x - s, y + s * dy * 0.2f)
        moveTo(x, y)
        lineTo(x + s * 0.2f, y + s * dy)
    }
    drawPath(head, brush, style = Stroke(width = size.width * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Suelo cuadriculado en perspectiva (líneas que convergen hacia un horizonte alto). */
@Composable
private fun PerspectiveGrid(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val horizon = h * 0.18f
        val line = ComunidadPalette.Grid.copy(alpha = 0.55f)
        val sw = w * 0.004f
        // Líneas longitudinales (convergen al punto de fuga central).
        val vp = Offset(w * 0.5f, horizon)
        for (i in -6..6) {
            val x = w * 0.5f + i * (w * 0.12f)
            drawLine(line, vp, Offset(x, h), sw)
        }
        // Líneas transversales (más juntas cerca del horizonte).
        var t = 0.02f
        var step = 0.02f
        while (t < 1f) {
            val y = horizon + (h - horizon) * t
            drawLine(line, Offset(0f, y), Offset(w, y), sw)
            step *= 1.28f
            t += step
        }
    }
}
