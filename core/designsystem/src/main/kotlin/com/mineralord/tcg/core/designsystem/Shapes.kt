package com.mineralord.tcg.core.designsystem

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Hexágono (de lado plano arriba/abajo por defecto), como los botones de
 * navegación del menú de TCG Live (Cartadex, Perfil, Tienda…).
 */
class HexagonShape(private val flatTop: Boolean = true) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            if (flatTop) {
                moveTo(w * 0.25f, 0f)
                lineTo(w * 0.75f, 0f)
                lineTo(w, h * 0.5f)
                lineTo(w * 0.75f, h)
                lineTo(w * 0.25f, h)
                lineTo(0f, h * 0.5f)
            } else {
                moveTo(w * 0.5f, 0f)
                lineTo(w, h * 0.25f)
                lineTo(w, h * 0.75f)
                lineTo(w * 0.5f, h)
                lineTo(0f, h * 0.75f)
                lineTo(0f, h * 0.25f)
            }
            close()
        }
        return Outline.Generic(path)
    }
}
