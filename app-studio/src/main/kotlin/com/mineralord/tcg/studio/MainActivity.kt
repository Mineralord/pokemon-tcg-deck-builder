package com.mineralord.tcg.studio

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.studio.shell.StudioShell

/**
 * Punto de entrada del APK 2 (Pokémon TCG Studio).
 *
 * **Infraestructura visual permanente (Studio Window & Adaptive Layout):** el Studio es una
 * herramienta profesional totalmente inmersiva. La Activity fija, de una vez para todo el Studio,
 * el comportamiento de la ventana:
 *
 *  - **Edge-to-edge** ([enableEdgeToEdge]): la superficie de Compose ocupa toda la pantalla.
 *  - **Barras del sistema ocultas** (Status Bar + Navigation Bar) vía [WindowInsetsControllerCompat],
 *    con reaparición transitoria por swipe (no rompe la inmersión).
 *  - **Display cutout / notch**: se dibuja hasta los bordes ([LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS]),
 *    pero el **Shell** respeta los Safe Areas por insets (`WindowInsets.safeDrawing`), de modo que el
 *    contenido útil nunca queda bajo el notch ni bajo las esquinas redondeadas.
 *
 * Los Labs NO tocan nada de esto: reciben del Shell un área de trabajo ya adaptada e inset-safe.
 * El núcleo compartido con el juego llega a través de `studio:shell` (Regla de Oro / Arquitectura Dual).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge: dibuja bajo (detrás de) las barras del sistema. Debe ir antes de setContent.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Permite dibujar en la zona del display cutout (notch/agujero) en todas las orientaciones.
        // El Shell reintroduce los Safe Areas por insets, así que "dibujar bajo el notch" solo aplica
        // al color de fondo a sangre, nunca al contenido útil.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
            }
        }

        hideSystemBars()

        setContent {
            StudioTheme {
                // Sesión del Board Simulator HOISTADA por encima del Shell: así la MISMA CombatScreen y
                // su controlador sobreviven al conmutar entre modos (no se reinician ni se duplican).
                val session = rememberStudioSession()
                // Contenido a PANTALLA COMPLETA que PUENTEA el Shell (sin Toolbar/Rail/StatusBar): lo usan
                // las experiencias inmersivas del Studio — Presentation Mode (Event Lab) y el Modo Partida
                // (Match Builder: partida real vs IA). Mientras haya contenido fullscreen, el Shell se oculta.
                var presentation by remember { mutableStateOf(false) }
                var fullscreen by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
                val fs = fullscreen
                Crossfade(targetState = presentation || fs != null, animationSpec = tween(320), label = "studioMode") { immersive ->
                    when {
                        presentation -> PresentationWorkspace(session = session, onExitPresentation = { presentation = false })
                        immersive && fs != null -> fs()
                        else -> StudioShell(
                            labs = studioLabs(
                                session = session,
                                onEnterPresentation = { presentation = true },
                                onEnterFullscreen = { content -> fullscreen = content },
                                onExitFullscreen = { fullscreen = null },
                            ),
                        )
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Re-afirma la inmersión si el usuario reveló las barras (swipe) y la ventana recupera foco.
        if (hasFocus) hideSystemBars()
    }

    /** Oculta Status Bar + Navigation Bar; reaparecen de forma transitoria con un swipe desde el borde. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
