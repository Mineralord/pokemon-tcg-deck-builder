package com.mineralord.tcg.core.designsystem.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Configuración GLOBAL del framework de animación, provista una sola vez en la raíz de la app
 * con [AnimationTheme]. Centraliza los interruptores que afectan a TODAS las animaciones
 * (accesibilidad "reducir movimiento", factor de velocidad global) sin que ningún componente
 * los codifique por su cuenta.
 */
@Immutable
data class AnimationConfig(
    /** Interruptor maestro: en false, las animaciones se resuelven de forma instantánea. */
    val enabled: Boolean = true,
    /** Multiplicador global de duración (1 = normal, <1 más rápido, >1 más lento). */
    val speedFactor: Float = AnimationConstants.SpeedNormal,
)

/** Acceso al [AnimationConfig] vigente. `static` porque cambia muy rara vez. */
val LocalAnimationConfig = staticCompositionLocalOf { AnimationConfig() }

/**
 * Provee la configuración global de animación al árbol de Compose. Envolver el contenido raíz
 * de la app con esto es lo único necesario para que el framework quede activo en todo el árbol.
 */
@Composable
fun AnimationTheme(
    config: AnimationConfig = AnimationConfig(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAnimationConfig provides config, content = content)
}

/** Atajo de lectura del config vigente. */
val animationConfig: AnimationConfig
    @Composable @ReadOnlyComposable get() = LocalAnimationConfig.current
