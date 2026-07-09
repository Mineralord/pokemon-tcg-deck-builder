package com.mineralord.tcg.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

/**
 * Esfera de energía oficial (PNG) por tipo. HADA (Fairy) no tiene esfera en la era
 * SV → devuelve null para que el llamador use un fallback dibujado.
 */
@DrawableRes
fun energySphereRes(type: EnergyType?): Int? = when (type) {
    EnergyType.GRASS -> R.drawable.energy_grass
    EnergyType.FIRE -> R.drawable.energy_fire
    EnergyType.WATER -> R.drawable.energy_water
    EnergyType.LIGHTNING -> R.drawable.energy_lightning
    EnergyType.PSYCHIC -> R.drawable.energy_psychic
    EnergyType.FIGHTING -> R.drawable.energy_fighting
    EnergyType.DARKNESS -> R.drawable.energy_darkness
    EnergyType.METAL -> R.drawable.energy_metal
    EnergyType.DRAGON -> R.drawable.energy_dragon
    EnergyType.COLORLESS -> R.drawable.energy_colorless
    EnergyType.FAIRY, null -> null
}

/** Abreviatura original (1-2 letras) por tipo; fallback cuando no hay esfera. */
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
 * Esfera de energía reutilizable: muestra el orbe oficial (PNG) del [type]. Si el
 * tipo no tiene esfera (HADA/null), cae a un disco coloreado con anillo blanco.
 * Punto único usado por el tablero, costes de ataque y chips de tipo.
 */
@Composable
fun EnergySphere(type: EnergyType?, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    val res = energySphereRes(type)
    if (res != null) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size),
        )
    } else {
        val c = type?.let(::typeColor) ?: Color(0xFFBDBDBD)
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(lerp(c, Color.White, 0.25f), lerp(c, Color.Black, 0.25f))))
                .border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            type?.let {
                Text(typeGlyph(it), color = Color.White, fontWeight = FontWeight.Black, fontSize = (size.value * 0.34f).sp)
            }
        }
    }
}

/**
 * Emblema de tipo: ahora usa la [EnergySphere] oficial. Conserva el anillo dorado
 * cuando [selected] (p.ej. chips de filtro). Sustituye al antiguo disco con glifo.
 */
@Composable
fun TypeEmblem(type: EnergyType, modifier: Modifier = Modifier, size: Dp = 36.dp, selected: Boolean = false) {
    Box(
        modifier = modifier
            .size(size)
            .then(if (selected) Modifier.border(3.dp, TcgColors.Gold, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        EnergySphere(type = type, size = if (selected) size - 6.dp else size)
    }
}
