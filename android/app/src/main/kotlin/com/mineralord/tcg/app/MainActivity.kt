package com.mineralord.tcg.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.mineralord.tcg.core.designsystem.TcgTheme
import com.mineralord.tcg.feature.packs.PacksScreen

/**
 * Punto de entrada de la app. Fase 1: arranca directamente en el slice vertical
 * de apertura de sobres (gacha) para validar de extremo a extremo el toolchain
 * Android + Compose + los módulos puros (motor/datos/gacha).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TcgTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    PacksScreen(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}
