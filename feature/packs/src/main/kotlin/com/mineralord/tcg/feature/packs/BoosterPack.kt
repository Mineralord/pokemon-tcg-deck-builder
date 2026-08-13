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
import androidx.compose.ui.graphics.drawscope.clipRect
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
import kotlin.random.Random

/** URL del arte real del sobre del Set 151 (sobre de Mew). */
const val PACK_IMAGE_151 =
    "https://images.wikidexcdn.net/mwuploads/wikidex/thumb/2/21/latest/20230616180309/Sobre_Mew_151.png/512px-Sobre_Mew_151.png"

/**
 * Sobre **troceable** con rasgado **LOCAL**: la ruptura se propaga desde la izquierda según un
 * TEAR FRONT en x = ancho·[tear]. La tira superior se divide en dos:
 *  - la parte YA RASGADA (x < front) se despega (sube/gira/atenúa);
 *  - la parte AÚN CERRADA (x ≥ front) permanece en su sitio, pegada al cuerpo.
 * Así NO se abre todo el borde de golpe: la zona no rasgada sigue cerrada. Mismo arte ([artUrl])
 * para cualquier expansión; la costura de luz del frente se dibuja aparte ([TornTopOverlay]).
 */
@Composable
fun TearablePack(
    artUrl: String,
    tear: Float,
    modifier: Modifier = Modifier,
    seamFraction: Float = 0.16f,
) {
    Box(modifier) {
        // Cuerpo del sobre (región POR DEBAJO de la costura): estático.
        AsyncImage(
            model = artUrl,
            contentDescription = "Sobre",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val ys = seamYs(size.height * seamFraction, size.height * SEAM_AMPLITUDE, SEAM_SEGMENTS, SEAM_SEED)
                    clipPath(seamRegionPath(size.width, size.height, ys, below = true)) { this@drawWithContent.drawContent() }
                },
        )
        // Tira AÚN CERRADA (por encima de la costura, a la DERECHA del frente): sigue pegada.
        AsyncImage(
            model = artUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val ys = seamYs(size.height * seamFraction, size.height * SEAM_AMPLITUDE, SEAM_SEGMENTS, SEAM_SEED)
                    clipRect(left = size.width * tear) {
                        clipPath(seamRegionPath(size.width, size.height, ys, below = false)) { this@drawWithContent.drawContent() }
                    }
                },
        )
        // Tira YA RASGADA (por encima de la costura, a la IZQUIERDA del frente): se despega.
        AsyncImage(
            model = artUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)   // pivota desde el inicio del rasgado
                    translationY = -tear * size.height * 0.5f
                    rotationZ = tear * -8f
                    alpha = 1f - tear * 0.55f
                }
                .drawWithContent {
                    val ys = seamYs(size.height * seamFraction, size.height * SEAM_AMPLITUDE, SEAM_SEGMENTS, SEAM_SEED)
                    clipRect(right = (size.width * tear).coerceAtLeast(0.01f)) {
                        clipPath(seamRegionPath(size.width, size.height, ys, below = false)) { this@drawWithContent.drawContent() }
                    }
                },
        )
    }
}

/**
 * Parámetros de la costura de rasgado — compartidos por el sobre (máscara) y la costura de luz para
 * que el borde IRREGULAR coincida exactamente. Semilla fija = mismo perfil de papel roto siempre.
 */
internal const val SEAM_SEGMENTS = 30
internal const val SEAM_AMPLITUDE = 0.022f
internal const val SEAM_SEED = 0x50BE

/**
 * Perfil IRREGULAR de la costura: `y` para cada vértice a lo ancho. No es un zigzag matemático:
 * combina alternancia con jitter aleatorio (semilla fija) para parecer papel rasgado a mano.
 */
internal fun seamYs(seamY: Float, amp: Float, segments: Int, seed: Int): FloatArray {
    val rnd = Random(seed)
    return FloatArray(segments + 1) { i ->
        if (i == 0 || i == segments) {
            seamY + (rnd.nextFloat() - 0.5f) * amp
        } else {
            val alt = if (i % 2 == 0) -1f else 1f
            seamY + alt * amp * (0.45f + 0.55f * rnd.nextFloat()) + (rnd.nextFloat() - 0.5f) * amp * 0.9f
        }
    }
}

/**
 * Región cerrada delimitada por la costura irregular [ys]: [below] = todo lo que queda por DEBAJO del
 * borde (cuerpo del sobre); `false` = todo lo que queda por ENCIMA (tira despegable). Ambas comparten
 * los MISMOS vértices, así que encajan exactamente como papel roto.
 */
internal fun seamRegionPath(width: Float, height: Float, ys: FloatArray, below: Boolean): Path {
    val segments = ys.size - 1
    val step = width / segments
    val p = Path()
    p.moveTo(0f, ys[0])
    for (i in 1..segments) p.lineTo(i * step, ys[i])
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
