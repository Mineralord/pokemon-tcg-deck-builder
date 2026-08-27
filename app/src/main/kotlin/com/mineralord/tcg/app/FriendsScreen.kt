package com.mineralord.tcg.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.BarajasPalette

private val Accent = Color(0xFFC0303A)
private val AccentDark = Color(0xFF7C1622)

/**
 * Hub de Amigos (placeholder). Cabecera roja coherente con Perfil + cuerpo neumórfico
 * claro (design system de barajas, ahora tema claro de toda la app). El hub completo
 * (agregar por código, lista con estados, solicitudes, jugar con amigos) llega a continuación.
 */
@Composable
fun FriendsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    Column(modifier.fillMaxSize().background(BarajasPalette.BgBottom)) {
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.verticalGradient(listOf(Accent, AccentDark))),
        ) {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color(0x33FFFFFF)).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.height(14.dp))
                Text("Amigos", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                Text("Juega, intercambia y comparte con tus amigos.", color = Color(0xCCFFFFFF), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(Accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Group, null, tint = Accent, modifier = Modifier.size(38.dp)) }
                Spacer(Modifier.height(16.dp))
                Text("Hub de amigos en construcción", color = BarajasPalette.Ink, fontWeight = FontWeight.Black,
                    fontSize = 17.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("Pronto podrás agregar amigos con tu código, ver quién está en línea y jugar juntos.",
                    color = BarajasPalette.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
