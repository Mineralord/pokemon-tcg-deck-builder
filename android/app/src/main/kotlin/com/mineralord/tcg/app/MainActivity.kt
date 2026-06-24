package com.mineralord.tcg.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mineralord.tcg.core.designsystem.TcgTheme
import com.mineralord.tcg.feature.packs.PacksScreen

/**
 * Punto de entrada. Navegación inferior simple entre Sobres y Colección — primer
 * esqueleto del shell de la app de cara a replicar el menú de TCG Live.
 */
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
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    label = { Text("Sobres") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text("Colección") },
                )
            }
        },
    ) { padding ->
        when (tab) {
            0 -> PacksScreen(modifier = Modifier.padding(padding))
            else -> CollectionScreen(modifier = Modifier.padding(padding))
        }
    }
}
