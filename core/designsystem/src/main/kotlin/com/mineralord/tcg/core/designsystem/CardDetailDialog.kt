package com.mineralord.tcg.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.tilt.HoloAssets
import com.mineralord.tcg.core.designsystem.tilt.HoloBitmaps
import com.mineralord.tcg.core.designsystem.tilt.Tilt
import com.mineralord.tcg.core.designsystem.tilt.holoOverlay
import com.mineralord.tcg.core.designsystem.tilt.resolveFinish
import com.mineralord.tcg.core.designsystem.tilt.tiltParallax
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.launch

/**
 * Visor de carta a pantalla completa: arte HD grande con **pinch-to-zoom** y arrastre.
 * Si se conoce la rareza aplica el **holo** correspondiente (giroscopio); si además hay
 * máscara de foil real empaquetada para [cardNumber], la usa para máxima fidelidad.
 */
@Composable
fun CardDetailDialog(
    imageUrl: String,
    contentDescription: String?,
    onDismiss: () -> Unit,
    rarity: Rarity? = null,
    cardNumber: Int? = null,
    // Código de set de la app (p. ej. "sv3pt5") para resolver el foil REAL de cualquier set
    // vía el catálogo dinámico de malie. Null = set 151 (compatibilidad).
    setCode: String? = null,
    // Zona de acciones opcional al pie (p. ej. ataques + retirada del Activo). Null = solo visor.
    bottomBar: (@Composable () -> Unit)? = null,
    // Copias en la colección (para la Cartadex): si no es null, muestra un chip "n/tope" sobre la
    // carta. Null = no mostrar (p. ej. desde el combate). [copiesCap] es el tope (4 · 30 energías).
    copiesOwned: Int? = null,
    copiesCap: Int? = null,
    // Superposición opcional SOBRE la carta (p. ej. el badge PS+tipo de TCG Live). Se dibuja
    // encima del arte, dentro del mismo Box (usa align()).
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    // Descarga bajo demanda + caché (no infla el APK): front+máscara(+etch) reales de CUALQUIER
    // set vía el export dinámico de malie (fallback al manifiesto del 151 si la red falla).
    val bundled by produceState<HoloBitmaps?>(null, cardNumber, rarity, setCode) {
        value = HoloAssets.resolve(context, setCode ?: "sv3pt5", cardNumber, rarity)
    }
    // Con assets reales, el finish sale de los metadatos de foil; si no, de la rareza.
    val rarityFinish = remember(rarity) { resolveFinish(rarity) }
    val finish = bundled?.finish ?: rarityFinish

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        // Inclinación por DEDO (como Pokémon TCG Live/Pocket): arrastrar el dedo por la carta la
        // inclina hacia el puntero y el holo lo sigue; al soltar, vuelve elástica al reposo. Sin
        // giroscopio. Normalizada a [-1, 1] en cada eje; alimenta tiltParallax y holoOverlay.
        val scope = rememberCoroutineScope()
        val tiltX = remember { Animatable(0f) }
        val tiltY = remember { Animatable(0f) }
        val tilt = Tilt(tiltX.value, tiltY.value)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() },
                        onDoubleTap = { scale = 1f; offset = Offset.Zero },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val holo = bundled
            // Sin acciones = visor a pantalla completa; con acciones = carta más pequeña
            // pegada ARRIBA para dejar sitio a los botones justo debajo.
            val sizeMod = if (bottomBar == null) {
                Modifier.fillMaxWidth().padding(16.dp)
            } else {
                // Visor EN PARTIDA (con panel de stats/acciones debajo): carta más grande y pegada
                // arriba. Antes 0.72; ampliada para que la carta se vea prominente como en TCG Live.
                Modifier.fillMaxWidth(0.86f).padding(top = 10.dp)
            }
            val base = sizeMod
                .aspectRatio(0.72f)
                .tiltParallax(tilt)
                .holoOverlay(tilt, finish, holo?.mask, holo?.etch, holo?.foilCode ?: -1f)
            // Interacción táctil unificada (sustituye al giroscopio):
            //  - 1 dedo, sin zoom → INCLINA la carta hacia el dedo (parallax + holo).
            //  - 1 dedo, con zoom → desplaza (pan).
            //  - 2 dedos (solo en modo visor) → pinch-to-zoom.
            // Un toque simple SIN arrastre no se consume, así el toque de fondo cierra el visor.
            val allowZoom = bottomBar == null
            val artModifier = base
                .then(
                    if (allowZoom) Modifier.graphicsLayer(
                        scaleX = scale, scaleY = scale,
                        translationX = offset.x, translationY = offset.y,
                    ) else Modifier,
                )
                .pointerInput(allowZoom) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            when {
                                allowZoom && pressed.size >= 2 -> {
                                    scale = (scale * event.calculateZoom()).coerceIn(1f, 4f)
                                    if (scale > 1f) offset += event.calculatePan()
                                }
                                pressed.size == 1 -> {
                                    if (allowZoom && scale > 1f) {
                                        offset += event.calculatePan()
                                    } else {
                                        // Inclina hacia el punto tocado, normalizado a [-1, 1].
                                        val p = pressed.first().position
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        val h = size.height.toFloat().coerceAtLeast(1f)
                                        val tx = ((p.x / w) * 2f - 1f).coerceIn(-1f, 1f)
                                        val ty = ((p.y / h) * 2f - 1f).coerceIn(-1f, 1f)
                                        scope.launch { tiltX.snapTo(tx) }
                                        scope.launch { tiltY.snapTo(ty) }
                                    }
                                }
                            }
                            // Consume SOLO si hubo arrastre (para no anular el toque de cierre).
                            if (event.changes.any { it.positionChanged() }) {
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        // Al soltar: la carta vuelve elástica a su posición de reposo.
                        val restSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        scope.launch { tiltX.animateTo(0f, restSpring) }
                        scope.launch { tiltY.animateTo(0f, restSpring) }
                    }
                }

            @Composable
            fun ArtCard() {
                Box(artModifier) {
                    if (holo != null) {
                        // Front de malie (alineado con su máscara de foil).
                        Image(
                            bitmap = holo.front,
                            contentDescription = contentDescription,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = contentDescription,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Superposición del llamador (badge PS+tipo), anclada a la CARTA.
                    overlay?.invoke(this)
                }
            }

            if (bottomBar == null) {
                ArtCard()
            } else {
                // Carta arriba (casi al borde superior) + acciones JUSTO DEBAJO, sin solaparse.
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ArtCard()
                    Spacer(Modifier.height(8.dp))
                    // weight → la zona de acciones queda ACOTADA a la altura restante, para que su
                    // propio scroll interno funcione cuando hay 4-5 paneles.
                    Box(Modifier.fillMaxWidth().weight(1f, fill = false)) { bottomBar() }
                }
            }

            // Chip de copias en la colección (como en TCG Live): se dibuja ENCIMA de la carta, así
            // el jugador ve cuántas tiene al abrir el detalle. Solo cuando se conocen las copias.
            if (copiesOwned != null && copiesCap != null) {
                val complete = copiesOwned >= copiesCap
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (complete) Color(0xFFFFD54F) else Color(0xCC10161F))
                        .border(1.dp, if (complete) Color(0xFFFFE9A6) else Color(0x55FFFFFF), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                ) {
                    Text(
                        "$copiesOwned / $copiesCap en la colección",
                        color = if (complete) Color(0xFF1A1400) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
