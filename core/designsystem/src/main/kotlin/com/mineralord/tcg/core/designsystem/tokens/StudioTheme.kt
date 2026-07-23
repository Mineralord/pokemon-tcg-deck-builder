package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Punto de acceso al lenguaje visual del **Pokémon TCG Developer Studio** dentro de Compose.
 *
 * Expone el [StudioColorTokens.Scheme] y la [StudioSpacingTokens.Density] activos mediante
 * CompositionLocals, de modo que todos los estilos de la Fase 10 consuman el tema sin recibir
 * valores por parámetro ni hornear literales. Cambiar de tema (`studio-dark`/`studio-light`/
 * launcher) es cambiar el `Scheme` provisto; los estilos no se tocan (VL-EVO-3).
 *
 * No introduce tokens nuevos: es solo el cableado que entrega los tokens de la Fase 9 a los
 * componentes.
 */
object StudioTheme {
    val colors: StudioColorTokens.Scheme
        @Composable @ReadOnlyComposable get() = LocalStudioColors.current

    val density: StudioSpacingTokens.Density
        @Composable @ReadOnlyComposable get() = LocalStudioDensity.current
}

/** Esquema de color activo. Por defecto, el tema canónico del Studio (`studio-dark`). */
val LocalStudioColors = staticCompositionLocalOf<StudioColorTokens.Scheme> { StudioColorTokens.Dark }

/** Densidad activa. Por defecto, la del Studio (herramienta densa de escritorio). */
val LocalStudioDensity =
    staticCompositionLocalOf<StudioSpacingTokens.Density> { StudioSpacingTokens.Density.Default }

/**
 * Envuelve el árbol de UI con el tema del Studio. Todo componente descendiente resuelve sus tokens
 * desde aquí.
 */
@Composable
fun StudioTheme(
    colors: StudioColorTokens.Scheme = StudioColorTokens.Dark,
    density: StudioSpacingTokens.Density = StudioSpacingTokens.Density.Default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStudioColors provides colors,
        LocalStudioDensity provides density,
        content = content,
    )
}
