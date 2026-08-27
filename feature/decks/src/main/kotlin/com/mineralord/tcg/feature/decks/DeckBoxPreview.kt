package com.mineralord.tcg.feature.decks

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.TypeEmblem
import com.mineralord.tcg.core.designsystem.typeColor
import com.mineralord.tcg.engine.model.EnergyType

/**
 * Vista de la "caja" 3D de una baraja con sus cartas DESTACADAS (réplica de TCG Pocket):
 * la [cover] (portada) ocupa la cara frontal; hasta 2 [sides] asoman **RECTAS** a la
 * derecha y **POR DELANTE** de la caja. Reutilizada por el gestor (lista) y el editor.
 */
@Composable
internal fun DeckBoxPreview(
    type: EnergyType,
    cover: String?,
    sides: List<String>,
    modifier: Modifier = Modifier,
) {
    val accent = typeColor(type)
    val accentLight = lerp(accent, Color.White, 0.28f)
    val accentDark = lerp(accent, Color.Black, 0.30f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Cuerpo de la caja.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.74f)
                .aspectRatio(0.84f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(accentLight, accent))),
        ) {
            // Lomo derecho (efecto 3D).
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.12f)
                    .background(Brush.horizontalGradient(listOf(accent, accentDark))),
            )
            // Portada en la cara frontal.
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp, start = 12.dp, end = 20.dp)
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(4.dp)),
                )
            }
            // Emblema del tipo, abajo-izquierda de la caja.
            TypeEmblem(
                type = type,
                size = 26.dp,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            )
        }
        // Cartas laterales (RECTAS) asomando a la derecha, POR DELANTE de la caja.
        sides.take(2).forEachIndexed { i, img ->
            AsyncImage(
                model = img,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = (i * 18).dp, end = (i * 7).dp)
                    .fillMaxWidth(0.26f)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.5.dp, Color.White, RoundedCornerShape(4.dp)),
            )
        }
    }
}
