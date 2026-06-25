package com.mineralord.tcg.feature.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage

private val SlotBg = Color(0xFF26303D)

/**
 * Celda de carta reutilizable (editor y detalle). Muestra el arte ES o, si no
 * existe versión española, nombre + **punto rojo** (patrón del proyecto). Badge
 * inferior con un número (copias en mazo / poseídas) y atenuado si [dim].
 */
@Composable
fun DeckCardCell(
    name: String,
    imageEs: String?,
    badge: String?,
    modifier: Modifier = Modifier,
    dim: Boolean = false,
    badgeColor: Color = Color(0xCC000000),
    onClick: (() -> Unit)? = null,
) {
    val hasSpanish = imageEs != null
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .alpha(if (dim) 0.4f else 1f)
            .clip(RoundedCornerShape(6.dp))
            .background(SlotBg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (hasSpanish) {
            AsyncImage(
                model = imageEs,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(6.dp)),
            )
        } else {
            Text(
                name,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier.align(Alignment.Center).padding(3.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .border(1.dp, Color.White, CircleShape),
            )
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
