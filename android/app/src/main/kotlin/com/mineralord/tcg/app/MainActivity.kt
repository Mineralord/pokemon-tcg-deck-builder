package com.mineralord.tcg.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.core.designsystem.TcgTheme
import com.mineralord.tcg.feature.packs.PacksScreen

private enum class Screen { HOME, COLLECTION, PACKS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TcgTheme {
                AppShell()
            }
        }
    }
}

@Composable
private fun AppShell() {
    var screen by remember { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    when (screen) {
        Screen.HOME -> HomeScreen(
            modifier = Modifier.fillMaxSize(),
            onCartadex = { screen = Screen.COLLECTION },
            onTienda = { screen = Screen.PACKS },
            onJugar = { /* combate: próximamente */ },
        )
        Screen.COLLECTION -> SubScreen(title = "Colección de cartas", onHome = { screen = Screen.HOME }) {
            CollectionScreen(modifier = Modifier.fillMaxSize())
        }
        Screen.PACKS -> SubScreen(title = "Tienda · Sobres", onHome = { screen = Screen.HOME }) {
            PacksScreen(modifier = Modifier.fillMaxSize())
        }
    }
}

/** Envoltorio de pantalla secundaria con barra superior y botón de Inicio. */
@Composable
private fun SubScreen(title: String, onHome: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(TcgColors.Navy)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TcgColors.Navy)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹ Inicio",
                color = TcgColors.Parchment,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.clickable(onClick = onHome),
            )
            Text(
                title,
                color = TcgColors.Parchment,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        content()
    }
}
