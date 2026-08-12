package com.mineralord.tcg.feature.packs

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import com.mineralord.tcg.core.designsystem.motion.AnimationSpecs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
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
 * Sobre **troceable**: el arte se parte en dos por una costura horizontal superior. La tira de
 * arriba (~16 %) se despega (sube + gira + se atenúa) según [tear] (0..1), dejando ver el cuerpo del
 * sobre por debajo. Sirve para CUALQUIER expansión: solo cambia [artUrl]. Réplica del rasgado de
 * Pokémon TCG Pocket. La costura de luz dentada se dibuja aparte ([TornTopOverlay]).
 */
@Composable
fun TearablePack(
    artUrl: String,
    tear: Float,
    modifier: Modifier = Modifier,
    seamFraction: Float = 0.16f,
) {
    Box(modifier) {
        // Cuerpo del sobre (región POR DEBAJO de la costura dentada): estático.
        AsyncImage(
            model = artUrl,
            contentDescription = "Sobre",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val amp = size.height * SEAM_AMPLITUDE
                    val path = seamPath(size.width, size.height, size.height * seamFraction, amp, SEAM_TEETH, below = true)
                    clipPath(path) { this@drawWithContent.drawContent() }
                },
        )
        // Tira SUPERIOR (región POR ENCIMA de la costura dentada): se despega al rasgar.
        // La máscara en zigzag es COMPLEMENTARIA de la del cuerpo, así encajan como papel roto.
        AsyncImage(
            model = artUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    translationY = -tear * size.height * 0.55f
                    translationX = tear * size.width * 0.10f
                    rotationZ = tear * -10f
                    alpha = 1f - tear * 0.65f
                }
                .drawWithContent {
                    val amp = size.height * SEAM_AMPLITUDE
                    val path = seamPath(size.width, size.height, size.height * seamFraction, amp, SEAM_TEETH, below = false)
                    clipPath(path) { this@drawWithContent.drawContent() }
                },
        )
    }
}

/** Nº de dientes y amplitud (fracción de la altura) de la costura de rasgado — compartidos con la costura de luz. */
internal const val SEAM_TEETH = 22
internal const val SEAM_AMPLITUDE = 0.02f

/**
 * Región cerrada delimitada por la costura DENTADA en [seamY]: [below] = todo lo que queda por
 * DEBAJO del zigzag (cuerpo del sobre); `false` = todo lo que queda por ENCIMA (tira despegable).
 * Ambas regiones comparten los MISMOS vértices, por lo que encajan exactamente al ensamblarse.
 */
internal fun seamPath(width: Float, height: Float, seamY: Float, amp: Float, teeth: Int, below: Boolean): Path {
    val step = width / teeth
    val p = Path()
    p.moveTo(0f, seamY - amp)
    for (i in 1..teeth) {
        val x = i * step
        val y = if (i % 2 == 0) seamY - amp else seamY + amp
        p.lineTo(x, y)
    }
    if (below) {
        p.lineTo(width, height); p.lineTo(0f, height)
    } else {
        p.lineTo(width, 0f); p.lineTo(0f, 0f)
    }
    p.close()
    return p
}

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
        animationSpec = AnimationSpecs.loop(2200),
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
