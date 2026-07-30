package com.mineralord.tcg.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mineralord.tcg.core.designsystem.tokens.ButtonStyle
import com.mineralord.tcg.core.designsystem.tokens.ButtonVariant
import com.mineralord.tcg.core.designsystem.tokens.ListRowStyle
import com.mineralord.tcg.core.designsystem.tokens.PanelStyle
import com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.core.designsystem.tokens.StudioTypographyTokens
import com.mineralord.tcg.core.designsystem.tokens.VisualState
import com.mineralord.tcg.studio.assets.Asset
import com.mineralord.tcg.studio.assets.AssetStatus

/**
 * **Panel «Asset Tool» permanente (flujo herramienta-primero).**
 *
 * Sin herramienta activa (modo neutro): ofrece las herramientas disponibles y, al elegir una, sus
 * variantes tomadas del Asset Registry (por categoría); al pulsar «Activar», el asset queda activo.
 * Con herramienta activa: muestra Tipo/Categoría/Variante/Estado/Objetivo, permite cambiar de variante
 * (todas las evoluciones usarán la nueva) y «Cancelar herramienta» para volver al modo neutro.
 *
 * No conoce ninguna categoría concreta: todo sale de [ToolController] y del Registry.
 */
@Composable
fun AssetToolPanel(
    controller: ToolController,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    /** Si no es null, muestra un botón de PANTALLA COMPLETA (toggle) arriba del panel. */
    fullscreenLabel: String? = null,
    onToggleFullscreen: () -> Unit = {},
) {
    val scheme = StudioTheme.colors
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(PanelStyle.contentPadding),
        verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
    ) {
        BasicText(text = "Asset Tool", style = coloredTool(PanelStyle.titleStyle, scheme.contentEmphasis))
        if (fullscreenLabel != null) ToolButton(scheme, ButtonVariant.Primary, fullscreenLabel, onToggleFullscreen)

        val active = controller.active
        if (active == null) {
            NeutralSelector(scheme, controller)
        } else {
            ActiveToolInfo(scheme, controller, active)
        }

        Divider(scheme)
        ToolButton(scheme, ButtonVariant.Neutral, "Reset tablero", onReset)
    }
}

// ── Modo neutro: elegir herramienta → variante → Activar ─────────────────────
@Composable
private fun NeutralSelector(scheme: StudioColorTokens.Scheme, controller: ToolController) {
    var expandedTool by remember { mutableStateOf<AssetTool?>(null) }
    var selectedVariantId by remember { mutableStateOf<String?>(null) }

    BasicText(
        text = "Modo neutro · elige una herramienta",
        style = coloredTool(StudioTypographyTokens.Role.Status, scheme.contentMuted),
    )
    controller.tools.forEach { tool ->
        val open = expandedTool === tool
        Row(
            modifier = Modifier
                .fillMaxWidth().height(ListRowStyle.rowHeight)
                .clip(RoundedCornerShape(ListRowStyle.radius))
                .background(scheme.surfaceRaised)
                .clickable { expandedTool = if (open) null else tool; selectedVariantId = null }
                .padding(horizontal = ListRowStyle.paddingH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
        ) {
            BasicText(if (open) "▾" else "▸", style = coloredTool(ListRowStyle.textStyle, scheme.contentSecondary))
            BasicText("${tool.type.label} · ${tool.label}", style = coloredTool(ListRowStyle.textStyle, scheme.contentEmphasis))
        }
        if (open) {
            controller.variantsFor(tool).forEach { asset ->
                VariantRow(scheme, asset, selected = asset.id == selectedVariantId) { selectedVariantId = asset.id }
            }
            val chosen = controller.variantsFor(tool).firstOrNull { it.id == selectedVariantId }
            if (chosen != null) {
                ToolButton(scheme, ButtonVariant.Primary, "Activar ${chosen.id}") { controller.activate(tool, chosen) }
            }
        }
    }
}

// ── Herramienta activa: HUD + cambiar variante + cancelar ────────────────────
@Composable
private fun ActiveToolInfo(scheme: StudioColorTokens.Scheme, controller: ToolController, active: ActiveTool) {
    InfoRow(scheme, "Tipo", active.tool.type.label)
    InfoRow(scheme, "Categoría", active.tool.label)
    InfoRow(scheme, "Variante", "${active.asset.id} · ${active.asset.name}")
    Row(horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap), verticalAlignment = Alignment.CenterVertically) {
        BasicText("Estado", style = coloredTool(StudioTypographyTokens.Role.Label, scheme.contentSecondary))
        BasicText("● Activa", style = coloredTool(StudioTypographyTokens.Role.Body, scheme.selectionEdge))
    }
    InfoRow(scheme, "Objetivo", controller.lastTarget ?: "Esperando…")

    Divider(scheme)
    BasicText("Cambiar variante", style = coloredTool(StudioTypographyTokens.Role.Status, scheme.contentMuted))
    controller.variantsFor(active.tool).forEach { asset ->
        VariantRow(scheme, asset, selected = asset.id == active.asset.id) { controller.activate(active.tool, asset) }
    }
    ToolButton(scheme, ButtonVariant.Neutral, "Cancelar herramienta") { controller.cancel() }
}

@Composable
private fun VariantRow(scheme: StudioColorTokens.Scheme, asset: Asset, selected: Boolean, onClick: () -> Unit) {
    val state = if (selected) VisualState.Selected else VisualState.Rest
    val colors = ListRowStyle.colors(scheme, state)
    Row(
        modifier = Modifier
            .fillMaxWidth().height(ListRowStyle.rowHeight)
            .padding(start = ListRowStyle.paddingH)
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(colors.container)
            .border(1.dp, if (selected) scheme.selectionEdge else scheme.borderSubtle, RoundedCornerShape(ListRowStyle.radius))
            .clickable(onClick = onClick)
            .padding(horizontal = ListRowStyle.paddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
    ) {
        BasicText(asset.id, style = coloredTool(StudioTypographyTokens.Role.Status, scheme.contentMuted))
        BasicText(asset.name, style = coloredTool(ListRowStyle.textStyle, colors.content))
        Box(Modifier.weight(1f))
        if (asset.status == AssetStatus.Canon) BasicText("★", style = coloredTool(StudioTypographyTokens.Role.Status, scheme.selectionEdge))
    }
}

@Composable
private fun InfoRow(scheme: StudioColorTokens.Scheme, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap)) {
        BasicText(label, style = coloredTool(StudioTypographyTokens.Role.Label, scheme.contentSecondary))
        BasicText(value, style = coloredTool(StudioTypographyTokens.Role.Body, scheme.contentPrimary))
    }
}

@Composable
private fun ToolButton(scheme: StudioColorTokens.Scheme, variant: ButtonVariant, label: String, onClick: () -> Unit) {
    val style = ButtonStyle(variant)
    val colors = style.colors(scheme, VisualState.Rest)
    Box(
        modifier = Modifier
            .height(style.metrics.height)
            .clip(RoundedCornerShape(style.metrics.radius))
            .background(colors.container)
            .clickable(onClick = onClick)
            .padding(horizontal = style.metrics.paddingH),
        contentAlignment = Alignment.Center,
    ) { BasicText(label, style = coloredTool(style.textStyle, colors.content)) }
}

@Composable
private fun Divider(scheme: StudioColorTokens.Scheme) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.borderSubtle))
}

private fun coloredTool(role: TextStyle, color: Color): TextStyle = role.copy(color = color)
