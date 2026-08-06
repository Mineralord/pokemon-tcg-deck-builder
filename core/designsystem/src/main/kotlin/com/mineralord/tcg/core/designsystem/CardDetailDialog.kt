package com.mineralord.tcg.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.tilt.HoloAssets
import com.mineralord.tcg.core.designsystem.tilt.HoloBitmaps
import com.mineralord.tcg.core.designsystem.tilt.holoOverlay
import com.mineralord.tcg.core.designsystem.tilt.rememberTilt
import com.mineralord.tcg.core.designsystem.tilt.resolveFinish
import com.mineralord.tcg.core.designsystem.tilt.tiltParallax
import com.mineralord.tcg.engine.model.Rarity

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
        // Parallax por giroscopio (solo con zoom neutro, para no chocar con el pan).
        val tilt by rememberTilt(enabled = scale <= 1f)

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
                Modifier.fillMaxWidth(0.72f).padding(top = 12.dp)
            }
            val base = sizeMod
                .aspectRatio(0.72f)
                .tiltParallax(tilt)
                .holoOverlay(tilt, finish, holo?.mask, holo?.etch, holo?.foilCode ?: -1f)
            // El pinch-to-zoom solo en modo visor (sin acciones), para no tapar los botones.
            val artModifier = if (bottomBar == null) {
                base
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            offset = if (scale > 1f) offset + pan else Offset.Zero
                        }
                    }
            } else {
                base
            }

            @Composable
            fun ArtCard() {
                if (holo != null) {
                    // Front de malie (alineado con su máscara de foil).
                    Image(
                        bitmap = holo.front,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = artModifier,
                    )
                } else {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = artModifier,
                    )
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
