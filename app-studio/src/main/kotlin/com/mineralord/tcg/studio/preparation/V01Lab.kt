package com.mineralord.tcg.studio.preparation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.core.designsystem.tokens.StudioTypographyTokens

/**
 * **Lab del Studio: V0.1 — Entrada al combate.** Reproduce las 5 propuestas ([V01_VARIANTS]) sobre un
 * escenario stylized, con conmutación instantánea y controles (Play/Pausa/Reinicio/Loop/velocidad).
 * El "Studio Player" es el bucle de frames que interpola los canales; el renderer los aplica.
 *
 * Audio: por ahora sólo **marcadores de cue** (no hay identidad ni assets de sonido todavía).
 */
private val ArenaBg = Color(0xFF05070C)
private val Speeds = floatArrayOf(0.5f, 1f, 2f)

@Composable
fun V01LabContent() {
    val scheme = StudioTheme.colors
    var variantIndex by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var playing by remember { mutableStateOf(true) }
    var loop by remember { mutableStateOf(false) }
    var speedIndex by remember { mutableIntStateOf(1) }

    val variant = V01_VARIANTS[variantIndex]
    val speed = Speeds[speedIndex]

    // ── Studio Player: avanza el reloj por frame, escalado por velocidad; respeta loop/fin. ──
    LaunchedEffect(playing, speed, variantIndex) {
        if (!playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dtMs = (now - last) / 1_000_000f * speed
            last = now
            var next = elapsed + dtMs
            if (next >= variant.totalMs) {
                if (loop) {
                    next = 0f
                } else {
                    elapsed = variant.totalMs; playing = false; break
                }
            }
            elapsed = next
        }
    }

    val frame = variant.frameAt(elapsed)

    fun selectVariant(i: Int) { variantIndex = i; elapsed = 0f; playing = true }
    fun restart() { elapsed = 0f; playing = true }
    fun togglePlay() { if (!playing && elapsed >= variant.totalMs) elapsed = 0f; playing = !playing }

    Box(modifier = Modifier.fillMaxSize().background(ArenaBg)) {
        // Escenario (aplica los canales del fotograma actual).
        V01Scene(frame.channels)

        // ── Consola de control (siempre encima; no afectada por la cinemática) ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Encabezado: evento + beat + cue + progreso.
            BasicText(
                text = "V0.1 · Entrada al combate — «${variant.name}»",
                style = text(StudioTypographyTokens.Role.Title, Color.White),
            )
            BasicText(
                text = "Beat: ${frame.beat}" + (frame.cue?.let { "   ·   🔊 $it" } ?: "") +
                    "   ·   ${(frame.progress * 100).toInt()}%   ·   ${"%.1f".format(variant.totalMs / 1000f)}s",
                style = text(StudioTypographyTokens.Role.Status, Color.White.copy(alpha = 0.8f)),
            )
            // Barra de progreso.
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.15f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(frame.progress).background(scheme.selectionEdge))
            }
            // Selector de propuestas (P1..P5).
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                V01_VARIANTS.forEachIndexed { i, v ->
                    Chip(scheme, "${v.id}", selected = i == variantIndex) { selectVariant(i) }
                }
            }
            // Transporte.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip(scheme, if (playing) "⏸ Pausa" else "▶ Play", selected = playing) { togglePlay() }
                Chip(scheme, "⟲ Reinicio", selected = false) { restart() }
                Chip(scheme, "Loop", selected = loop) { loop = !loop }
                Chip(scheme, "Velocidad ${speed}x", selected = false) { speedIndex = (speedIndex + 1) % Speeds.size }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Renderer: aplica los canales visuales del fotograma.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun V01Scene(ch: SceneChannels) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Arena (escenario del duelo): cámara (escala/desplazamiento/tilt) + reveal + rack-focus.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = ch.arenaScale
                    scaleY = ch.arenaScale
                    translationY = ch.rootOffsetY.dp.toPx()
                    rotationZ = ch.rootRotation
                    alpha = ch.arenaAlpha
                }
                .blur(ch.arenaBlur.dp),
        ) { ArenaBackdrop() }

        // Destello central (bloom).
        if (ch.bloom > 0.001f) BloomOverlay(ch.bloom)
        // Partículas de materialización.
        if (ch.particles > 0.001f) ParticleOverlay(ch.particles)

        // Menú saliente (encima de la arena; se desvanece para revelarla).
        if (ch.menuAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = ch.menuScale; scaleY = ch.menuScale; alpha = ch.menuAlpha }
                    .blur(ch.menuBlur.dp),
            ) { MenuPlaceholder() }
        }

        // Velo (fundido a negro).
        if (ch.scrimAlpha > 0.001f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = ch.scrimAlpha)))
        }
        // Barrido de luz.
        if (ch.lightIntensity > 0.001f && ch.lightBand > -0.3f && ch.lightBand < 1.3f) {
            LightBand(ch.lightBand, ch.lightIntensity)
        }
    }
}

/** Tapete stylized del combate (representación del escenario que V0.1 revela). */
@Composable
private fun ArenaBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val pad = w * 0.06f
        val matW = w - pad * 2; val matTop = h * 0.05f; val matH = h * 0.90f
        // Mat.
        drawRoundRect(Color(0xFF12203A), topLeft = Offset(pad, matTop), size = Size(matW, matH), cornerRadius = CornerRadius(28f))
        // Carril central.
        drawRect(Color(0x22FFFFFF), topLeft = Offset(pad, h * 0.47f), size = Size(matW, h * 0.06f))
        // Bancas.
        val benchW = matW - w * 0.16f
        drawRoundRect(Color(0x18FFFFFF), topLeft = Offset(pad + w * 0.08f, h * 0.16f), size = Size(benchW, h * 0.09f), cornerRadius = CornerRadius(12f))
        drawRoundRect(Color(0x18FFFFFF), topLeft = Offset(pad + w * 0.08f, h * 0.75f), size = Size(benchW, h * 0.09f), cornerRadius = CornerRadius(12f))
        // Activos.
        val aw = w * 0.16f; val ah = h * 0.14f
        drawRoundRect(Color(0x30FFFFFF), topLeft = Offset(w / 2 - aw / 2, h * 0.30f - ah / 2), size = Size(aw, ah), cornerRadius = CornerRadius(14f))
        drawRoundRect(Color(0x48FFFFFF), topLeft = Offset(w / 2 - aw / 2, h * 0.68f - ah / 2), size = Size(aw, ah), cornerRadius = CornerRadius(14f))
        // Premios (columnas).
        for (i in 0 until 3) {
            drawRoundRect(Color(0x22FFFFFF), topLeft = Offset(pad + w * 0.01f, h * 0.10f + i * h * 0.045f), size = Size(w * 0.05f, h * 0.035f), cornerRadius = CornerRadius(6f))
            drawRoundRect(Color(0x22FFFFFF), topLeft = Offset(pad + matW - w * 0.06f, h * 0.82f - i * h * 0.045f), size = Size(w * 0.05f, h * 0.035f), cornerRadius = CornerRadius(6f))
        }
        // Mano.
        drawRoundRect(Color(0x22FFFFFF), topLeft = Offset(pad + w * 0.05f, h * 0.86f), size = Size(matW - w * 0.10f, h * 0.08f), cornerRadius = CornerRadius(12f))
    }
}

/** Placeholder del menú de origen (solo para dar sentido a "el menú cede"). */
@Composable
private fun MenuPlaceholder() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E18)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BasicText("POKÉMON TCG", style = text(StudioTypographyTokens.Role.Title, Color.White.copy(alpha = 0.9f)))
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF2B5CB8)).padding(horizontal = 40.dp, vertical = 12.dp),
            ) { BasicText("JUGAR", style = text(StudioTypographyTokens.Role.Label, Color.White)) }
            BasicText("Menú principal", style = text(StudioTypographyTokens.Role.Status, Color.White.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun BloomOverlay(intensity: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height * 0.5f)
        val r = size.minDimension * 0.65f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = intensity.coerceIn(0f, 1f) * 0.5f), Color.Transparent),
                center = c,
                radius = r,
            ),
        )
    }
}

@Composable
private fun ParticleOverlay(intensity: Float) {
    val pts = remember {
        val r = java.util.Random(7)
        List(48) { Offset(r.nextFloat(), r.nextFloat()) }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rad = size.minDimension * 0.004f
        pts.forEach { p ->
            drawCircle(Color.White.copy(alpha = intensity.coerceIn(0f, 1f) * 0.7f), radius = rad, center = Offset(p.x * size.width, p.y * size.height))
        }
    }
}

@Composable
private fun LightBand(pos: Float, intensity: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val x = pos * size.width
        val half = size.width * 0.18f
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = intensity.coerceIn(0f, 1f)), Color.Transparent),
                startX = x - half,
                endX = x + half,
            ),
            topLeft = Offset(x - half, 0f),
            size = Size(half * 2f, size.height),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun Chip(scheme: StudioColorTokens.Scheme, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) scheme.selectionEdge.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f))
            .border(1.dp, if (selected) scheme.selectionEdge else Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        BasicText(label, style = text(StudioTypographyTokens.Role.Label, Color.White))
    }
}

private fun text(role: TextStyle, color: Color): TextStyle = role.copy(color = color)
