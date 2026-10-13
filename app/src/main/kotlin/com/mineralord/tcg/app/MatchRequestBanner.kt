package com.mineralord.tcg.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.TcgColors

/**
 * Banner superior de **solicitud de combate entrante**: aparece en el menú y en cualquier parte de
 * la app (salvo en combate) cuando un amigo te invita a combatir. Acepta con ✓ (entras a la sala) o
 * rechaza con ✗. Se desliza desde arriba con una transición suave.
 */
@Composable
fun MatchRequestBanner(
    visible: Boolean,
    fromName: String,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(TcgColors.Red, TcgColors.RedDark)))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⚔️", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("¡$fromName te reta a combatir!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text("Toca ✓ para entrar a la sala", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            CircleAction("✕", Color.White.copy(alpha = 0.18f), Color.White, onReject)
            Spacer(Modifier.width(10.dp))
            CircleAction("✓", TcgColors.Gold, TcgColors.Ink, onAccept)
        }
    }
}

@Composable
private fun CircleAction(glyph: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = fg, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}
