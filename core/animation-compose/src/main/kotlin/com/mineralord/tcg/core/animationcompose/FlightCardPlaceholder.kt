package com.mineralord.tcg.core.animationcompose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Aspecto VISIBLE mínimo de la carta en vuelo (dorso genérico), compartido por todos los
 * NodeRenderers de la capa de vuelo. Hasta que existan los assets/holo reales (animaciones
 * posteriores del backlog), la carta se dibuja como un rectángulo redondeado con relleno y borde
 * para que el vuelo sea OBSERVABLE. Un único helper ⇒ sin duplicar el placeholder entre renderers.
 *
 * `core:animation-compose` no depende del Design System, así que el color es un valor local del
 * placeholder (no un token). El contenido real de la carta lo aportará una animación posterior.
 */
internal fun Modifier.flightCardPlaceholder(): Modifier =
    this
        .background(FlightCardFill, FlightCardShape)
        .border(1.dp, FlightCardBorder, FlightCardShape)

private val FlightCardShape = RoundedCornerShape(6.dp)
private val FlightCardFill = Color(0xFFECEFF5)
private val FlightCardBorder = Color(0x33101014)
