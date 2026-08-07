package com.mineralord.tcg.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.tilt.HoloAssets
import com.mineralord.tcg.core.designsystem.tilt.HoloBitmaps
import com.mineralord.tcg.core.designsystem.tilt.holoAmbient
import com.mineralord.tcg.core.designsystem.tilt.resolveFinish
import com.mineralord.tcg.engine.model.Rarity

/**
 * Carta con **holo real** reutilizable para colección, mano y tablero. Resuelve el foil de
 * malie por `set + número` ([HoloAssets.resolve], válido para cualquier set presente/futuro) y
 * aplica el brillo **sin giroscopio** ([holoAmbient], barrido por tiempo) para no pagar el
 * parallax 3D cuando hay muchas cartas. El visor a pantalla completa ([CardDetailDialog])
 * sigue usando el giroscopio; este es el camino "ligero" para todo lo demás.
 *
 * Mientras se descarga el foil (o si el set no está en malie) pinta el arte plano por
 * [imageUrl]; en cuanto llega, cambia al front real alineado con su máscara.
 *
 * @param enabled si es `false`, nunca aplica holo (pinta arte plano) — útil para dorsos o para
 *   limitar cuántas cartas animan a la vez por rendimiento.
 * @param intensity multiplicador de brillo (baja en miniaturas para no saturar).
 */
@Composable
fun HoloCardImage(
    imageUrl: String?,
    setCode: String?,
    cardNumber: Int?,
    rarity: Rarity?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    intensity: Float = 1f,
) {
    val context = LocalContext.current
    val holo by produceState<HoloBitmaps?>(null, setCode, cardNumber, rarity, enabled) {
        value = if (enabled) HoloAssets.resolve(context, setCode ?: "sv3pt5", cardNumber, rarity) else null
    }
    val finish = remember(rarity, holo) { holo?.finish ?: resolveFinish(rarity) }
    val holoMod = if (enabled) {
        modifier.holoAmbient(finish, holo?.mask, holo?.etch, holo?.foilCode ?: -1f, intensity)
    } else modifier

    val front = holo?.front
    if (front != null) {
        Image(
            bitmap = front,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = holoMod,
        )
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = holoMod,
        )
    }
}
