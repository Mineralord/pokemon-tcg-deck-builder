package com.mineralord.tcg.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.engine.model.EnergyType

/** Color de acento por tipo de energía (tokens del design system + locales). */
fun typeColor(t: EnergyType): Color = when (t) {
    EnergyType.GRASS -> TcgColors.TypeGrass
    EnergyType.FIRE -> TcgColors.TypeFire
    EnergyType.WATER -> TcgColors.TypeWater
    EnergyType.LIGHTNING -> TcgColors.TypeLightning
    EnergyType.PSYCHIC -> TcgColors.TypePsychic
    EnergyType.FIGHTING -> TcgColors.TypeFighting
    EnergyType.DARKNESS -> TcgColors.TypeDarkness
    EnergyType.METAL -> TcgColors.TypeMetal
    EnergyType.FAIRY -> Color(0xFFEC7FB0)
    EnergyType.DRAGON -> Color(0xFFC9A227)
    EnergyType.COLORLESS -> Color(0xFFBDBDBD)
}

/** Abreviatura original (1-2 letras) por tipo; evita usar los símbolos de TPC. */
private fun typeGlyph(t: EnergyType): String = when (t) {
    EnergyType.GRASS -> "Pl"
    EnergyType.FIRE -> "Fu"
    EnergyType.WATER -> "Ag"
    EnergyType.LIGHTNING -> "Ra"
    EnergyType.PSYCHIC -> "Ps"
    EnergyType.FIGHTING -> "Lu"
    EnergyType.DARKNESS -> "Os"
    EnergyType.METAL -> "Me"
    EnergyType.FAIRY -> "Ha"
    EnergyType.DRAGON -> "Dr"
    EnergyType.COLORLESS -> "In"
}

/**
 * Emblema circular de tipo con **arte original** (disco con degradado del color
 * del tipo, anillo blanco y abreviatura). Sustituye a los símbolos de energía de
 * TCG Live sin copiarlos.
 */
@Composable
fun TypeEmblem(type: EnergyType, modifier: Modifier = Modifier, size: Dp = 36.dp, selected: Boolean = false) {
    val c = typeColor(type)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(lerp(c, Color.White, 0.25f), lerp(c, Color.Black, 0.25f))))
            .border(if (selected) 3.dp else 2.dp, if (selected) TcgColors.Gold else Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            typeGlyph(type),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.34f).sp,
        )
    }
}
