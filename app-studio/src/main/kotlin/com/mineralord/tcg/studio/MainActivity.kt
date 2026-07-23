package com.mineralord.tcg.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.studio.shell.StudioShell

/**
 * Punto de entrada del APK 2 (Pokémon TCG Studio).
 *
 * Monta el contenedor permanente (Boot + Shell, 11.1) bajo el `StudioTheme`. No conoce el interior
 * de ningún Lab (aún no existen): el Shell hospeda el estado vacío del Workspace. El núcleo
 * compartido con el juego llega a través de `studio:shell` (Regla de Oro / Arquitectura Dual).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudioTheme {
                StudioShell()
            }
        }
    }
}
