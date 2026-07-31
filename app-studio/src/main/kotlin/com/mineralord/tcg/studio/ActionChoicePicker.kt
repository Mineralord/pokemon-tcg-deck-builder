package com.mineralord.tcg.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.animationcompose.RajdhaniFamily
import com.mineralord.tcg.core.designsystem.tokens.ButtonStyle
import com.mineralord.tcg.core.designsystem.tokens.ButtonVariant
import com.mineralord.tcg.core.designsystem.tokens.PanelStyle
import com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.core.designsystem.tokens.StudioTypographyTokens
import com.mineralord.tcg.core.designsystem.tokens.VisualState
import com.mineralord.tcg.core.designsystem.typeColor
import com.mineralord.tcg.engine.model.EnergyType

/**
 * **Opción de acción/ataque** que se presenta en el selector [ActionChoicePicker]. Es un DATO puro de
 * presentación (no lógica de juego): título, coste de Energía (pips por tipo), daño y texto de regla.
 * Sirve para Mew ex «Hackeo Genómico» (elegir un ataque del Activo rival) y cualquier decisión similar
 * de "elige una acción" (copiar ataque, elegir Partidario, etc.).
 */
data class ActionOption(
    val id: String,
    val title: String,
    val cost: List<EnergyType>,
    val damage: Int?,
    val text: String,
)

/** Acento dorado del estado "elegible/seleccionado" (mismo lenguaje que el aura manual AAA). */
private val Gold = Color(0xFFF0B23A)

/**
 * **Selector de acción AAA (estilo «Discover»), FUNCIONAL.** Presenta [options] como tarjetas
 * seleccionables: al tocar una, esa se **eleva** y se enciende con un halo dorado y el resto se
 * **atenúa**; el botón confirma la elección. Es el patrón de UI que usará «Hackeo Genómico» y otras
 * decisiones de "elige un ataque/acción".
 *
 * Autocontenido e interactivo (mantiene su propia selección + confirmación) para poder MOSTRARLO y
 * PROBARLO en la Galería. Compuesto SOLO con tokens existentes del Design System (cadena congelada).
 */
@Composable
fun ActionChoicePicker(
    kicker: String,
    prompt: String,
    options: List<ActionOption>,
    modifier: Modifier = Modifier,
) {
    val scheme = StudioTheme.colors
    var selectedId by remember { mutableStateOf<String?>(null) }
    var confirmed by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PanelStyle.material.surface(scheme))
            .padding(PanelStyle.contentPadding),
        verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
    ) {
        BasicText(
            text = kicker.uppercase(),
            style = TextStyle(fontFamily = RajdhaniFamily, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp),
        )
        BasicText(text = prompt, style = coloredText(PanelStyle.titleStyle, scheme.contentEmphasis))

        // Fila «Discover» de opciones (scroll horizontal → responsive en anchos compactos).
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            options.forEach { opt ->
                OptionCard(
                    scheme = scheme,
                    option = opt,
                    selected = selectedId == opt.id,
                    dimmed = selectedId != null && selectedId != opt.id,
                    onClick = { selectedId = opt.id; confirmed = null },
                )
            }
        }

        // Confirmación (token Primary). Deshabilitado hasta elegir.
        val chosen = options.firstOrNull { it.id == selectedId }
        ConfirmButton(
            scheme = scheme,
            label = if (chosen != null) "Usar «${chosen.title}»" else "Elige una acción",
            enabled = chosen != null,
            onClick = { confirmed = chosen?.title },
        )
        if (confirmed != null) {
            BasicText(
                text = "✓ Acción elegida: $confirmed",
                style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentPrimary),
            )
        }
    }
}

@Composable
private fun OptionCard(
    scheme: StudioColorTokens.Scheme,
    option: ActionOption,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, tween(180), label = "scale")
    val alpha by animateFloatAsState(if (dimmed) 0.5f else 1f, tween(180), label = "alpha")
    val border by animateColorAsState(if (selected) Gold else scheme.borderSubtle, tween(180), label = "border")

    Column(
        modifier = Modifier
            .width(168.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) scheme.surfaceRaised else scheme.surfacePanel)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Título (display Rajdhani) + daño grande a la derecha.
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = option.title,
                style = TextStyle(fontFamily = RajdhaniFamily, color = scheme.contentEmphasis, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                modifier = Modifier.weight(1f),
            )
            if (option.damage != null) {
                BasicText(
                    text = option.damage.toString(),
                    style = TextStyle(fontFamily = RajdhaniFamily, color = if (selected) Gold else scheme.contentPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
        // Pips de coste por tipo de Energía.
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (option.cost.isEmpty()) {
                BasicText(text = "Sin coste", style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted))
            } else {
                option.cost.forEach { t ->
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(typeColor(t)).border(1.dp, Color.Black.copy(alpha = 0.25f), CircleShape))
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        BasicText(text = option.text, style = coloredText(StudioTypographyTokens.Role.Body, scheme.contentSecondary))
    }
}

@Composable
private fun ConfirmButton(scheme: StudioColorTokens.Scheme, label: String, enabled: Boolean, onClick: () -> Unit) {
    val style = ButtonStyle(ButtonVariant.Primary)
    val colors = style.colors(scheme, if (enabled) VisualState.Rest else VisualState.Disabled)
    Box(
        modifier = Modifier
            .height(style.metrics.height)
            .clip(RoundedCornerShape(style.metrics.radius))
            .background(colors.container)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = style.metrics.paddingH),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = label, style = coloredText(style.textStyle, colors.content))
    }
}

private fun coloredText(role: TextStyle, color: Color): TextStyle = role.copy(color = color)
