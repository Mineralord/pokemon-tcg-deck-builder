package com.mineralord.tcg.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Barra de scroll vertical con **pulgar arrastrable** (réplica del comportamiento
 * de Pokémon TCG Pocket): aparece al desplazar y se **desvanece suavemente** al
 * detenerse, pero mientras está visible se puede **tocar y arrastrar** para
 * desplazar el contenido. Uso: `Modifier.fadingScrollbar(listState)`.
 *
 * No usar en los visores de cartas de partida (por decisión de diseño).
 */
private data class ScrollMetrics(
    val visibleFraction: Float, // proporción visible (0f..1f) → alto del pulgar
    val progress: Float,        // progreso del scroll (0f..1f) → posición del pulgar
    val scrollable: Boolean,    // hay contenido fuera de pantalla
)

private val HandleSize = 34.dp   // diámetro fijo del botón (nunca se redimensiona)
private val EdgeMargin = 6.dp    // separación respecto al borde derecho

private fun Modifier.fadingScrollbarCore(
    state: ScrollableState,
    metrics: () -> ScrollMetrics,
    scrollToFraction: suspend (Float) -> Unit,
    color: Color,
    thickness: Dp,
): Modifier = composed {
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val target = if (state.isScrollInProgress || dragging) 1f else 0f
    val alpha by animateFloatAsState(
        targetValue = target,
        // Aparece al instante; al detenerse permanece ~0.9 s y luego se desvanece.
        animationSpec = if (target == 1f) tween(durationMillis = 100)
            else tween(durationMillis = 450, delayMillis = 900),
        label = "scrollbarAlpha",
    )

    this
        .drawWithContent {
            drawContent()
            val m = metrics()
            if (!m.scrollable || alpha <= 0.01f) return@drawWithContent
            val d = HandleSize.toPx()
            val r = d / 2f
            val trackH = size.height
            val cx = size.width - EdgeMargin.toPx() - r
            val cy = r + (trackH - d) * m.progress
            val a = alpha
            // Sombra suave + círculo blanco con borde.
            drawCircle(color = Color.Black.copy(alpha = 0.10f * a), radius = r, center = Offset(cx, cy + 1.5.dp.toPx()))
            drawCircle(color = Color.White.copy(alpha = a), radius = r, center = Offset(cx, cy))
            drawCircle(
                color = color.copy(alpha = 0.25f * a), radius = r,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
            )
            // Flechas ▲▼ (chevrons) centradas.
            val chevW = 4.5.dp.toPx()
            val chevH = 3.dp.toPx()
            val gap = 3.dp.toPx()
            val sw = 1.6.dp.toPx()
            val arrowColor = color.copy(alpha = (if (dragging) 1f else 0.85f) * a)
            // ▲ arriba
            drawLine(arrowColor, Offset(cx - chevW, cy - gap), Offset(cx, cy - gap - chevH), sw, StrokeCap.Round)
            drawLine(arrowColor, Offset(cx + chevW, cy - gap), Offset(cx, cy - gap - chevH), sw, StrokeCap.Round)
            // ▼ abajo
            drawLine(arrowColor, Offset(cx - chevW, cy + gap), Offset(cx, cy + gap + chevH), sw, StrokeCap.Round)
            drawLine(arrowColor, Offset(cx + chevW, cy + gap), Offset(cx, cy + gap + chevH), sw, StrokeCap.Round)
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val m = metrics()
                if (!m.scrollable) return@awaitEachGesture
                val d = HandleSize.toPx()
                val r = d / 2f
                val trackH = size.height.toFloat()
                val cx = size.width - EdgeMargin.toPx() - r
                val cy = r + (trackH - d) * m.progress
                // Zona táctil: caja del botón (algo ampliada) a la derecha.
                val pad = 6.dp.toPx()
                if (down.position.x < cx - r - pad || down.position.y < cy - r - pad || down.position.y > cy + r + pad) {
                    return@awaitEachGesture
                }
                dragging = true
                down.consume()
                val grab = down.position.y - cy   // desfase dedo↔centro
                val travel = (trackH - d).coerceAtLeast(1f)
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
                    val newCy = (change.position.y - grab).coerceIn(r, trackH - r)
                    val frac = ((newCy - r) / travel).coerceIn(0f, 1f)
                    change.consume()
                    scope.launch { scrollToFraction(frac) }
                }
                dragging = false
            }
        }
}

@Composable
fun Modifier.fadingScrollbar(
    state: LazyListState,
    color: Color = BarajasPalette.NavIcon,
    thickness: Dp = 6.dp,
): Modifier {
    fun snapshot(): ScrollMetrics {
        val info = state.layoutInfo
        val total = info.totalItemsCount
        val visible = info.visibleItemsInfo
        if (total == 0 || visible.isEmpty() || visible.size >= total) return ScrollMetrics(1f, 0f, false)
        val first = visible.first().index
        val last = visible.last().index
        val visibleCount = (last - first + 1).toFloat()
        val denom = (total - visibleCount).coerceAtLeast(1f)
        return ScrollMetrics((visibleCount / total).coerceIn(0f, 1f), (first / denom).coerceIn(0f, 1f), true)
    }
    return fadingScrollbarCore(state, ::snapshot, { frac ->
        val info = state.layoutInfo
        val visibleCount = info.visibleItemsInfo.size.coerceAtLeast(1)
        val target = (frac * (info.totalItemsCount - visibleCount)).roundToInt().coerceAtLeast(0)
        state.scrollToItem(target)
    }, color, thickness)
}

@Composable
fun Modifier.fadingScrollbar(
    state: LazyGridState,
    color: Color = BarajasPalette.NavIcon,
    thickness: Dp = 6.dp,
): Modifier {
    fun snapshot(): ScrollMetrics {
        val info = state.layoutInfo
        val total = info.totalItemsCount
        val visible = info.visibleItemsInfo
        if (total == 0 || visible.isEmpty() || visible.size >= total) return ScrollMetrics(1f, 0f, false)
        val first = visible.first().index
        val last = visible.last().index
        val visibleCount = (last - first + 1).toFloat()
        val denom = (total - visibleCount).coerceAtLeast(1f)
        return ScrollMetrics((visibleCount / total).coerceIn(0f, 1f), (first / denom).coerceIn(0f, 1f), true)
    }
    return fadingScrollbarCore(state, ::snapshot, { frac ->
        val info = state.layoutInfo
        val visibleCount = info.visibleItemsInfo.size.coerceAtLeast(1)
        val target = (frac * (info.totalItemsCount - visibleCount)).roundToInt().coerceAtLeast(0)
        state.scrollToItem(target)
    }, color, thickness)
}

@Composable
fun Modifier.fadingScrollbar(
    state: ScrollState,
    color: Color = BarajasPalette.NavIcon,
    thickness: Dp = 6.dp,
): Modifier {
    fun snapshot(): ScrollMetrics {
        val max = state.maxValue
        if (max == 0 || max == Int.MAX_VALUE) return ScrollMetrics(1f, 0f, false)
        return ScrollMetrics(0.25f, (state.value.toFloat() / max).coerceIn(0f, 1f), true)
    }
    return fadingScrollbarCore(state, ::snapshot, { frac ->
        state.scrollTo((frac * state.maxValue).roundToInt())
    }, color, thickness)
}
