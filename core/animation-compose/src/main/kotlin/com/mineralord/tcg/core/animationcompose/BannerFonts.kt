package com.mineralord.tcg.core.animationcompose

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Tipografía display de los rótulos AAA. **Rajdhani** (Indian Type Foundry, licencia OFL): una
 * grotesca angular/condensada de estilo HUD que usan los lower-thirds de juegos AAA (sci-fi/deporte).
 * Sustituye a la sans genérica del sistema, que se leía plana en los banners.
 *
 * Se empaqueta en ESTE módulo (`res/font/`) porque el renderer vive aquí y `core:animation-compose`
 * no depende del Design System. Si el juego adopta esta fuente, basta referenciar esta familia.
 */
val RajdhaniFamily = FontFamily(
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)
