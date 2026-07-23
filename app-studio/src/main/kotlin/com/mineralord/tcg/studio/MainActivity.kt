package com.mineralord.tcg.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.studio.shell.StudioShell

/**
 * Punto de entrada del APK 2 (Pokémon TCG Studio).
 *
 * Monta el contenedor permanente (Boot + Shell + Navegación v1) bajo el `StudioTheme` y le entrega
 * el catálogo de Labs registrados ([studioLabs]). El Shell solo los hospeda y conmuta entre ellos; no
 * conoce su interior. El núcleo compartido con el juego llega a través de `studio:shell` (Regla de
 * Oro / Arquitectura Dual).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudioTheme {
                StudioShell(labs = studioLabs())
            }
        }
    }
}
