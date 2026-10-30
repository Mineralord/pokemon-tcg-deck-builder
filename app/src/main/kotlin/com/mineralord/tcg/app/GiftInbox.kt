package com.mineralord.tcg.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.data.netfirestore.Gift

/**
 * Bandeja de regalos como hoja inferior (bottom sheet): entra deslizando de abajo hacia arriba sobre
 * un velo que atenúa el fondo (el blur lo aplica [AppShell] al contenido base). Usa el design system
 * claro oficial ([BarajasPalette]). Lista los regalos sin reclamar y los reclama uno a uno; los
 * regalos solo se conceden server-side (Admin SDK): aquí solo se leen y reclaman.
 */
@Composable
fun GiftInbox(visible: Boolean, gifts: List<Gift>, onClaim: (Gift) -> Unit, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        // Velo: atenúa y captura el toque para cerrar.
        AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(BarajasPalette.Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }
        // Hoja inferior deslizante.
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(300)) { it },
            exit = slideOutVertically(tween(240)) { it },
        ) {
            GiftSheet(gifts = gifts, onClaim = onClaim, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun GiftSheet(gifts: List<Gift>, onClaim: (Gift) -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Brush.verticalGradient(listOf(BarajasPalette.BgTop, BarajasPalette.BgBottom)))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Asa superior.
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(44.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(BarajasPalette.HairlineBorder),
        )
        Spacer(Modifier.height(16.dp))
        Text("Regalos", color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            if (gifts.isEmpty()) "No tienes regalos por reclamar."
            else "Toca «Reclamar» para añadir las recompensas a tu cuenta.",
            color = BarajasPalette.Muted, fontSize = 13.sp,
        )
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            gifts.forEach { gift -> GiftRow(gift, onClaim = { onClaim(gift) }) }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BarajasPalette.Hollow)
                .border(1.dp, BarajasPalette.HollowBorder, RoundedCornerShape(14.dp))
                .clickable(onClick = onDismiss)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Cerrar", color = BarajasPalette.NavIcon, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
    }
}

@Composable
private fun GiftRow(gift: Gift, onClaim: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BarajasPalette.Surface)
            .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                gift.title.ifBlank { "Regalo" },
                color = BarajasPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            )
            val rewards = giftRewardParts(gift).joinToString(" · ")
            if (rewards.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(rewards, color = BarajasPalette.NavIcon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (gift.message.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(gift.message, color = BarajasPalette.Muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(BarajasPalette.SaveGradient))
                .clickable(onClick = onClaim)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) { Text("Reclamar", color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 13.sp) }
    }
}
