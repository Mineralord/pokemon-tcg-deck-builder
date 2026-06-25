package com.mineralord.tcg.feature.packs

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.mineralord.tcg.core.designsystem.TcgColors

/** URL del arte real del sobre del Set 151 (sobre de Mew). */
const val PACK_IMAGE_151 =
    "https://images.wikidexcdn.net/mwuploads/wikidex/thumb/2/21/latest/20230616180309/Sobre_Mew_151.png/512px-Sobre_Mew_151.png"

/**
 * Arte del sobre con leve flotación/balanceo idle. Carga la imagen real del Set
 * 151 por URL (Coil); si falla, cae a un envoltorio degradado original con el
 * logo "TCG".
 */
@Composable
fun BoosterPack(modifier: Modifier = Modifier, floating: Boolean = true) {
    val infinite = rememberInfiniteTransition(label = "packFloat")
    val t by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "packBob",
    )
    val painter = rememberAsyncImagePainter(PACK_IMAGE_151)
    val isError = painter.state is AsyncImagePainter.State.Error

    Box(
        modifier = modifier.graphicsLayer {
            if (floating) {
                translationY = t * 14f
                rotationZ = t * 2.2f
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        if (isError) {
            // Fallback: envoltorio original.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(TcgColors.Red, TcgColors.RedDark, TcgColors.Navy))),
                contentAlignment = Alignment.Center,
            ) {
                Text("TCG", color = TcgColors.Gold, fontWeight = FontWeight.Black, fontSize = 40.sp)
            }
        } else {
            AsyncImage(
                model = PACK_IMAGE_151,
                contentDescription = "Sobre del Set 151",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
