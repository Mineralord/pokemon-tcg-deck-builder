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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens
import com.mineralord.tcg.core.designsystem.tokens.StudioTypographyTokens

/**
 * **Event Player: el reproductor UNIVERSAL del Event Catalog dentro del Board Simulator.**
 *
 * No hay un reproductor por evento ni un Lab por evento. El Board Simulator carga cualquier evento del
 * catálogo, reproduce sus propuestas (variantes) sobre el MISMO tapete real, y ofrece transporte y
 * comparación. Este archivo aporta: el estado del reproductor ([EventPlayerState]), el render de la
 * cinemática **sobre el tablero real** (canales de cámara + overlay) y el panel de control embebible.
 */

private val Speeds = floatArrayOf(0.5f, 1f, 2f)

/** Un evento del catálogo con sus propuestas. */
class CatalogEvent(val id: String, val name: String, val variants: List<EventVariant>)

/** Catálogo de eventos reproducibles hoy (crecerá; hoy sólo V0.1). */
val PREP_EVENTS: List<CatalogEvent> = listOf(
    CatalogEvent("V0.1", "Entrada al combate", V01_VARIANTS),
)

/** Estado del reproductor universal, hospedado por el Board Simulator. */
@Stable
class EventPlayerState(val events: List<CatalogEvent>) {
    var active by mutableStateOf(false); private set
    var eventIndex by mutableIntStateOf(0); private set
    var variantIndex by mutableIntStateOf(0); private set
    var elapsed by mutableFloatStateOf(0f)
    var playing by mutableStateOf(false)
    var loop by mutableStateOf(false)
    var speedIndex by mutableIntStateOf(1); private set

    /** Variante marcada como Canon por evento (eventId → variantId). En memoria (persistencia: futura). */
    val canon = mutableStateMapOf<String, String>()

    val speed: Float get() = Speeds[speedIndex]
    val event: CatalogEvent get() = events[eventIndex]
    val variant: EventVariant get() = event.variants[variantIndex]
    fun frame(): Frame = variant.frameAt(elapsed)

    fun load(index: Int) { eventIndex = index; variantIndex = 0; active = true; elapsed = 0f; playing = true }
    fun close() { active = false; playing = false; elapsed = 0f }
    fun selectVariant(i: Int) { variantIndex = i; elapsed = 0f; playing = true }
    fun restart() { elapsed = 0f; playing = true }
    fun togglePlay() { if (!playing && elapsed >= variant.totalMs) elapsed = 0f; playing = !playing }
    fun cycleSpeed() { speedIndex = (speedIndex + 1) % Speeds.size }
    fun markCanon() { canon[event.id] = variant.id }
    fun canonVariantId(): String? = canon[event.id]
}

// ─────────────────────────────────────────────────────────────────────────────
// Render de la cinemática SOBRE EL TABLERO REAL
// ─────────────────────────────────────────────────────────────────────────────

/** Aplica al tablero real la "cámara" del fotograma (escala/desplazamiento/tilt/reveal/rack-focus). */
fun Modifier.cameraChannels(ch: SceneChannels?): Modifier = if (ch == null) this else this
    .graphicsLayer {
        scaleX = ch.arenaScale
        scaleY = ch.arenaScale
        translationY = ch.rootOffsetY.dp.toPx()
        rotationZ = ch.rootRotation
        alpha = ch.arenaAlpha
    }
    .blur(ch.arenaBlur.dp)

/** Capas overlay del fotograma (menú saliente, velo, luz, bloom, partículas), sobre el tablero. */
@Composable
fun EventOverlay(ch: SceneChannels) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (ch.bloom > 0.001f) BloomOverlay(ch.bloom)
        if (ch.particles > 0.001f) ParticleOverlay(ch.particles)
        if (ch.menuAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = ch.menuScale; scaleY = ch.menuScale; alpha = ch.menuAlpha }
                    .blur(ch.menuBlur.dp),
            ) { MenuPlaceholder() }
        }
        if (ch.scrimAlpha > 0.001f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = ch.scrimAlpha)))
        if (ch.lightIntensity > 0.001f && ch.lightBand > -0.3f && ch.lightBand < 1.3f) LightBand(ch.lightBand, ch.lightIntensity)
    }
}

/** Placeholder del menú de origen (da sentido a "el menú cede"). */
@Composable
private fun MenuPlaceholder() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E18)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BasicText("POKÉMON TCG", style = text(StudioTypographyTokens.Role.Title, Color.White.copy(alpha = 0.9f)))
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF2B5CB8)).padding(horizontal = 40.dp, vertical = 12.dp)) {
                BasicText("JUGAR", style = text(StudioTypographyTokens.Role.Label, Color.White))
            }
            BasicText("Menú principal", style = text(StudioTypographyTokens.Role.Status, Color.White.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun BloomOverlay(intensity: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height * 0.5f)
        drawRect(brush = Brush.radialGradient(listOf(Color.White.copy(alpha = intensity.coerceIn(0f, 1f) * 0.5f), Color.Transparent), center = c, radius = size.minDimension * 0.65f))
    }
}

@Composable
private fun ParticleOverlay(intensity: Float) {
    val pts = remember { val r = java.util.Random(7); List(48) { Offset(r.nextFloat(), r.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rad = size.minDimension * 0.004f
        pts.forEach { p -> drawCircle(Color.White.copy(alpha = intensity.coerceIn(0f, 1f) * 0.7f), radius = rad, center = Offset(p.x * size.width, p.y * size.height)) }
    }
}

@Composable
private fun LightBand(pos: Float, intensity: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val x = pos * size.width
        val half = size.width * 0.18f
        drawRect(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = intensity.coerceIn(0f, 1f)), Color.Transparent), startX = x - half, endX = x + half),
            topLeft = Offset(x - half, 0f),
            size = Size(half * 2f, size.height),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panel de control (embebido en la consola del Board Simulator)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EventPlayerPanel(state: EventPlayerState, scheme: StudioColorTokens.Scheme, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText("🎬 Event Player", style = text(StudioTypographyTokens.Role.Title, scheme.contentEmphasis))

        if (!state.active) {
            BasicText("Selecciona un evento del catálogo:", style = text(StudioTypographyTokens.Role.Status, scheme.contentMuted))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.events.forEachIndexed { i, ev ->
                    Chip(scheme, "${ev.id} · ${ev.name}", selected = false) { state.load(i) }
                }
            }
        } else {
            val frame = state.frame()
            val canonId = state.canonVariantId()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText("${state.event.id} · «${state.variant.name}»", style = text(StudioTypographyTokens.Role.Label, scheme.contentEmphasis))
                if (canonId == state.variant.id) BasicText("★ CANON", style = text(StudioTypographyTokens.Role.Status, scheme.selectionEdge))
            }
            BasicText(
                text = "Beat: ${frame.beat}" + (frame.cue?.let { "   ·   🔊 $it" } ?: "") + "   ·   ${(frame.progress * 100).toInt()}%",
                style = text(StudioTypographyTokens.Role.Status, scheme.contentMuted),
            )
            Box(Modifier.fillMaxWidth().height(3.dp).background(scheme.borderSubtle)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(frame.progress).background(scheme.selectionEdge))
            }
            // Propuestas (comparación instantánea).
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.event.variants.forEachIndexed { i, v ->
                    val star = if (canonId == v.id) "★" else ""
                    Chip(scheme, "$star${v.id}", selected = i == state.variantIndex) { state.selectVariant(i) }
                }
            }
            // Transporte.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip(scheme, if (state.playing) "⏸" else "▶", selected = state.playing) { state.togglePlay() }
                Chip(scheme, "⟲", selected = false) { state.restart() }
                Chip(scheme, "Loop", selected = state.loop) { state.loop = !state.loop }
                Chip(scheme, "${state.speed}x", selected = false) { state.cycleSpeed() }
            }
            // Acciones.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(scheme, "★ Marcar Canon", selected = false) { state.markCanon() }
                Chip(scheme, "✕ Cerrar", selected = false) { state.close() }
            }
            BasicText("Editar parámetros y Guardar: próxima iteración.", style = text(StudioTypographyTokens.Role.Status, scheme.contentMuted))
        }
    }
}

@Composable
private fun Chip(scheme: StudioColorTokens.Scheme, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) scheme.selectionEdge.copy(alpha = 0.35f) else scheme.surfaceRaised)
            .border(1.dp, if (selected) scheme.selectionEdge else scheme.borderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) { BasicText(label, style = text(StudioTypographyTokens.Role.Label, scheme.contentPrimary)) }
}

private fun text(role: TextStyle, color: Color): TextStyle = role.copy(color = color)
