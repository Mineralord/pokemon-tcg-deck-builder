package com.mineralord.tcg.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.HexagonShape
import com.mineralord.tcg.core.designsystem.TcgColors

/**
 * Pantalla de Inicio — réplica del menú principal de TCG Live (capturas de
 * referencia). El arte con copyright (hero, avatar, caja de baraja, logos) se
 * representa con placeholders estilizados; la estructura, colores y disposición
 * replican la pantalla original.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onCartadex: () -> Unit,
    onTienda: () -> Unit,
    onJugar: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TcgColors.Cream),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ---------- Panel rojo superior ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(listOf(TcgColors.Red, TcgColors.RedDark)),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                StatusBar()
                Spacer(Modifier.height(10.dp))
                Tabs()
                Spacer(Modifier.height(12.dp))
                HeroBanner()
            }
        }

        // ---------- Cluster central (avatar + JUGAR), solapando el borde ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-34).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarHex()
            CoinBadge(count = 10)
            Spacer(Modifier.height(14.dp))
            PlayButton(onClick = onJugar)
        }

        Spacer(Modifier.height(8.dp))

        // ---------- Navegación hexagonal inferior ----------
        HomeNav(onCartadex = onCartadex, onTienda = onTienda)
    }
}

@Composable
private fun StatusBar() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Counter(Color(0xFF4FC3F7), "350")
        Counter(Color(0xFFFFD54F), "440")
        Counter(Color(0xFFB0BEC5), "0")
    }
}

@Composable
private fun Counter(dotColor: Color, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x33000000))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(6.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun Tabs() {
    val tabs = listOf("COMPETITIVO", "INFORMAL", "APRENDER", "COMBINA Y COMBATE")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        tabs.forEachIndexed { i, t ->
            val active = i == 0
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(82.dp)) {
                Text(
                    t,
                    color = if (active) Color.White else Color(0xCCFFFFFF),
                    fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .height(3.dp)
                        .width(if (active) 64.dp else 0.dp)
                        .clip(RoundedCornerShape(50))
                        .background(TcgColors.Gold),
                )
            }
        }
    }
}

@Composable
private fun HeroBanner() {
    // Placeholder del arte hero (Mega) — degradado colorido evocador.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFF7043), Color(0xFFEC407A),
                        Color(0xFF7E57C2), Color(0xFF26C6DA), Color(0xFF66BB6A),
                    ),
                ),
            ),
    )
}

@Composable
private fun AvatarHex() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(HexagonShape())
            .background(Brush.verticalGradient(listOf(Color(0xFFE9EDF2), Color(0xFFB9C2CC))))
            .border(3.dp, TcgColors.Gold, HexagonShape()),
        contentAlignment = Alignment.Center,
    ) {
        // Placeholder del avatar (Pokémon).
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFD54F)))
    }
}

@Composable
private fun CoinBadge(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .offset(y = (-10).dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .border(2.dp, Color(0xFFD0D0D0), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 3.dp),
    ) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(TcgColors.Red))
        Spacer(Modifier.width(6.dp))
        Text("$count", color = TcgColors.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun PlayButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF4D27A), TcgColors.Gold, Color(0xFFC8902F))))
            .border(2.dp, Color(0xFF8C6420), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("JUGAR", color = Color(0xFF5A3D12), fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

@Composable
private fun HomeNav(onCartadex: () -> Unit, onTienda: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            HexNav("CARTADEX", Color(0xFF42A5F5), onCartadex)
            HexNav("BARAJAS", Color(0xFF8D6E63), {}, big = true)
            HexNav("PASE DE\nCOMBATE", Color(0xFFFFB300), {})
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            HexNav("PERFIL", Color(0xFF8E63C0), {})
            HexNav("", Color(0xFFBDBDBD), {})
            HexNav("TIENDA", Color(0xFF26A69A), onTienda)
        }
    }
}

@Composable
private fun HexNav(label: String, accent: Color, onClick: () -> Unit, big: Boolean = false) {
    val s = if (big) 84.dp else 64.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(s)
                .clip(HexagonShape())
                .background(Color.White)
                .border(2.dp, TcgColors.Red.copy(alpha = 0.6f), HexagonShape())
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(s * 0.42f).clip(RoundedCornerShape(6.dp)).background(accent))
        }
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = TcgColors.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
            )
        }
    }
}
