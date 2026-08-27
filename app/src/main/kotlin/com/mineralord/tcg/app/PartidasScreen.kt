package com.mineralord.tcg.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla "Partidas" (4ª pestaña) — réplica de la referencia. Hero de batalla
 * arriba (arte con copyright → placeholder azul) y, sobre la sección clara, dos
 * accesos grandes: Versus (hub PvP) e Individual (PvE vs IA), más los botones
 * pequeños Guía / Barajas y el tile Misiones. La barra inferior es compartida.
 */
@Composable
fun PartidasScreen(
    modifier: Modifier = Modifier,
    onVersus: () -> Unit,
    onIndividual: () -> Unit,
    onGuia: () -> Unit = {},
    onBarajas: () -> Unit = {},
    onMisiones: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PartidasPalette.BgTop, PartidasPalette.BgBottom))),
    ) {
        Hero()
        // El espacio flexible va ARRIBA: empuja los botones al fondo (fiel a la referencia,
        // Guía/Barajas/Misiones justo sobre la barra inferior y Versus/Individual encima).
        Spacer(Modifier.weight(1f))
        // Dos accesos grandes.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BigButton("Versus", Modifier.weight(1f), onClick = onVersus) { VersusIcon() }
            BigButton("Individual", Modifier.weight(1f), alert = true, onClick = onIndividual) { IndividualIcon() }
        }
        Spacer(Modifier.height(18.dp))
        // Botones pequeños (Guía · Barajas) a la izquierda; Misiones a la derecha.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SmallButton("Guía", onClick = onGuia) { GuiaIcon() }
            Spacer(Modifier.size(14.dp))
            SmallButton("Barajas", onClick = onBarajas) { BarajasIcon() }
            Spacer(Modifier.weight(1f))
            SmallButton("Misiones", alert = true, onClick = onMisiones) { MisionesTileIcon() }
        }
        Spacer(Modifier.height(14.dp))
    }
}

private object PartidasPalette {
    val BgTop = Color(0xFFEFF5FA)
    val BgBottom = Color(0xFFE2ECF3)
    val HeroTop = Color(0xFF2E5BBF)
    val HeroBottom = Color(0xFF5B8CE0)
    val BandTop = Color(0xFF3D74E0)
    val BandBottom = Color(0xFF2B57C8)
    val Surface = Color(0xFFFFFFFF)
    val Hairline = Color(0xFFDCE6EF)
    val Ink = Color(0xFF24272A)
    val Muted = Color(0xFF7C8794)
    val RedDot = Color(0xFFFF3B6B)
    val IconGrad = listOf(Color(0xFF3FC6F5), Color(0xFF2E7BE0))
}

// ===========================================================================
//  HERO
// ===========================================================================

@Composable
private fun Hero() {
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.42f)
            .background(Brush.verticalGradient(listOf(PartidasPalette.HeroTop, PartidasPalette.HeroBottom))),
    ) {
        // Placeholder del arte de batalla (copyright): destellos tenues.
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.02f
            listOf(0.2f to 0.7f, 0.5f to 0.55f, 0.75f to 0.75f, 0.85f to 0.4f, 0.3f to 0.85f).forEach { (fx, fy) ->
                drawCircle(Color.White.copy(alpha = 0.5f), r, Offset(size.width * fx, size.height * fy))
            }
        }
        // Banda inclinada con el título.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 46.dp)
                .rotate(-7f)
                .background(Brush.verticalGradient(listOf(PartidasPalette.BandTop, PartidasPalette.BandBottom)))
                .padding(vertical = 10.dp),
        ) {
            Text(
                "Partidas",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = 40.sp,
                modifier = Modifier.padding(start = 20.dp),
            )
        }
    }
}

// ===========================================================================
//  BOTONES
// ===========================================================================

@Composable
private fun BigButton(
    label: String,
    modifier: Modifier = Modifier,
    alert: Boolean = false,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.TopEnd) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PartidasPalette.Surface)
                .border(1.dp, PartidasPalette.Hairline, RoundedCornerShape(24.dp))
                .clickable(onClick = onClick)
                .semantics { contentDescription = label }
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { icon() }
            Text(label, color = PartidasPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        if (alert) AlertDot(Modifier.padding(4.dp))
    }
}

@Composable
private fun SmallButton(
    label: String,
    alert: Boolean = false,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PartidasPalette.Surface)
                    .border(1.dp, PartidasPalette.Hairline, RoundedCornerShape(18.dp))
                    .clickable(onClick = onClick)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) { icon() }
            if (alert) AlertDot(Modifier.padding(2.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = PartidasPalette.Muted, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun AlertDot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(PartidasPalette.RedDot)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
    }
}

// ===========================================================================
//  ICONOS VECTORIALES
// ===========================================================================

/** Estallido/starburst (contorno gradiente) centrado en [c] con radio [r]. */
private fun DrawScope.drawBurst(c: Offset, r: Float, brush: Brush, stroke: Float, points: Int = 10) {
    val path = Path()
    for (i in 0 until points * 2) {
        val rad = if (i % 2 == 0) r else r * 0.62f
        val a = Math.toRadians((360.0 / (points * 2) * i - 90.0))
        val x = c.x + rad * kotlin.math.cos(a).toFloat()
        val y = c.y + rad * kotlin.math.sin(a).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, Color.White)
    drawPath(path, brush, style = Stroke(width = stroke, join = StrokeJoin.Round))
}

/**
 * Busto de persona (cabeza + hombros) en contorno gradiente, relleno blanco para
 * que "tape" el estallido de detrás. [s] es el radio de referencia de la cabeza.
 */
private fun DrawScope.drawPerson(c: Offset, s: Float, brush: Brush, stroke: Float) {
    val st = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val headR = s
    val headC = Offset(c.x, c.y - s * 1.15f)
    // Hombros: cúpula ancha (medio óvalo) con base recta.
    val shoulders = Path().apply {
        val left = c.x - s * 1.5f; val right = c.x + s * 1.5f
        val topY = c.y - s * 0.1f; val baseY = c.y + s * 1.35f
        moveTo(left, baseY)
        lineTo(left, c.y + s * 0.6f)
        cubicTo(left, topY, right, topY, right, c.y + s * 0.6f)
        lineTo(right, baseY)
        close()
    }
    drawPath(shoulders, Color.White)
    drawPath(shoulders, brush, style = st)
    drawCircle(Color.White, headR, headC)
    drawCircle(brush, headR, headC, style = st)
}

@Composable
private fun VersusIcon() {
    Canvas(Modifier.size(46.dp)) {
        val brush = Brush.linearGradient(PartidasPalette.IconGrad)
        val stroke = size.minDimension * 0.05f
        val s = size.minDimension * 0.15f
        // Estallido detrás, arriba-derecha.
        drawBurst(Offset(size.width * 0.66f, size.height * 0.34f), size.minDimension * 0.24f, brush, stroke)
        // Dos personas (multijugador): la de atrás (der.) y delante (izq.).
        drawPerson(Offset(size.width * 0.60f, size.height * 0.52f), s, brush, stroke)
        drawPerson(Offset(size.width * 0.38f, size.height * 0.64f), s, brush, stroke)
    }
}

@Composable
private fun IndividualIcon() {
    Canvas(Modifier.size(46.dp)) {
        val brush = Brush.linearGradient(PartidasPalette.IconGrad)
        val stroke = size.minDimension * 0.05f
        val s = size.minDimension * 0.17f
        drawBurst(Offset(size.width * 0.64f, size.height * 0.34f), size.minDimension * 0.26f, brush, stroke)
        drawPerson(Offset(size.width * 0.46f, size.height * 0.60f), s, brush, stroke)
    }
}

@Composable
private fun GuiaIcon() {
    Canvas(Modifier.size(26.dp)) {
        val c = center; val r = size.minDimension * 0.42f
        val col = PartidasPalette.Ink
        val st = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(col, r, c, style = st)
        // Signo "?".
        val q = Path().apply {
            moveTo(c.x - r * 0.28f, c.y - r * 0.18f)
            cubicTo(c.x - r * 0.28f, c.y - r * 0.62f, c.x + r * 0.34f, c.y - r * 0.62f, c.x + r * 0.30f, c.y - r * 0.14f)
            cubicTo(c.x + r * 0.27f, c.y + r * 0.14f, c.x, c.y + r * 0.06f, c.x, c.y + r * 0.34f)
        }
        drawPath(q, col, style = st)
        drawCircle(col, size.minDimension * 0.045f, Offset(c.x, c.y + r * 0.62f))
    }
}

@Composable
private fun BarajasIcon() {
    Canvas(Modifier.size(26.dp)) {
        val w = size.width; val h = size.height
        val col = PartidasPalette.Ink
        val st = Stroke(width = w * 0.08f, join = StrokeJoin.Round)
        // Caja/mazo.
        drawRoundRect(
            col,
            topLeft = Offset(w * 0.24f, h * 0.16f),
            size = Size(w * 0.52f, h * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
            style = st,
        )
        // Punto tipo Poké.
        drawCircle(col, w * 0.09f, Offset(w * 0.5f, h * 0.42f), style = Stroke(width = w * 0.06f))
        drawLine(col, Offset(w * 0.28f, h * 0.42f), Offset(w * 0.72f, h * 0.42f), w * 0.055f)
    }
}

@Composable
private fun MisionesTileIcon() {
    Canvas(Modifier.size(26.dp)) {
        val w = size.width; val h = size.height
        val col = PartidasPalette.Ink
        val st = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            col,
            topLeft = Offset(w * 0.2f, h * 0.16f),
            size = Size(w * 0.6f, h * 0.72f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
            style = st,
        )
        val chk = Path().apply {
            moveTo(w * 0.36f, h * 0.52f); lineTo(w * 0.47f, h * 0.64f); lineTo(w * 0.66f, h * 0.4f)
        }
        drawPath(chk, col, style = st)
    }
}
