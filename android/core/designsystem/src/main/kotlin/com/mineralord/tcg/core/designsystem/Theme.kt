package com.mineralord.tcg.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = TcgColors.Red,
    onPrimary = TcgColors.Parchment,
    secondary = TcgColors.Gold,
    onSecondary = TcgColors.Ink,
    background = TcgColors.Cream,
    onBackground = TcgColors.Ink,
    surface = TcgColors.Parchment,
    onSurface = TcgColors.Ink,
)

private val DarkScheme = darkColorScheme(
    primary = TcgColors.Red,
    onPrimary = TcgColors.Parchment,
    secondary = TcgColors.Gold,
    onSecondary = TcgColors.Ink,
    background = TcgColors.Navy,
    onBackground = TcgColors.Parchment,
    surface = TcgColors.NavyLight,
    onSurface = TcgColors.Parchment,
)

/** Tema raíz de la app. Usa el esquema claro por defecto (como TCG Live). */
@Composable
fun TcgTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography(),
        content = content,
    )
}
