package com.mineralord.tcg.core.combatscene

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mineralord.tcg.core.animationcompose.AnimationRenderState
import com.mineralord.tcg.core.animationcompose.AnimationStage
import com.mineralord.tcg.core.animationcompose.CoordinateRegistry
import com.mineralord.tcg.core.animationcompose.LocalCoordinateRegistry
import com.mineralord.tcg.core.animationcompose.activeSlotId
import com.mineralord.tcg.core.animationcompose.rememberCanonicalAnimationDirector
import com.mineralord.tcg.core.animationcompose.trackBounds
import com.mineralord.tcg.core.designsystem.tokens.ButtonStyle
import com.mineralord.tcg.core.designsystem.tokens.ButtonVariant
import com.mineralord.tcg.core.designsystem.tokens.ListRowStyle
import com.mineralord.tcg.core.designsystem.tokens.PanelStyle
import com.mineralord.tcg.core.designsystem.tokens.StudioColorTokens
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.core.designsystem.tokens.StudioTypographyTokens
import com.mineralord.tcg.core.designsystem.tokens.VisualState

/**
 * **La ÚNICA Combat Scene del proyecto: escena de combate visual y reutilizable.**
 *
 * Renderiza TODO el aspecto del combate (tapete, zonas, cartas, HUD, botones, overlays) y hospeda el
 * `AnimationStage` con el motor canónico compartido ([rememberCanonicalAnimationDirector]) —el MISMO
 * pipeline que usará el juego—. **No conoce reglas, ni partidas, ni el Sandbox:** se conduce por el
 * [CombatSceneController]. El juego la usará con su Game Controller; el Studio con su Sandbox
 * Controller. Una sola implementación visual del combate para ambos productos.
 *
 * Debe invocarse dentro de un `StudioTheme { … }` (apariencia del núcleo compartido). Adaptativa:
 * panel de acciones lateral en ancho ≥600dp, inferior en compacto (contrato responsive del Studio).
 */
private const val SCENE_PLAYER = "scene"
private val ActionPanelWidth: Dp = 236.dp
private val CompactWidthThreshold: Dp = 600.dp

@Composable
fun CombatScene(controller: CombatSceneController, modifier: Modifier = Modifier) {
    val scheme = StudioTheme.colors
    val state = controller.state

    // La escena hospeda el motor real y se lo entrega al controlador (que no dibuja pero sí reproduce).
    val coordinates = remember { CoordinateRegistry() }
    val renderState = remember { AnimationRenderState() }
    val director = rememberCanonicalAnimationDirector(coordinates, renderState)
    LaunchedEffect(controller, director) {
        controller.attachEngine(object : CombatSceneEngine {
            override fun submit(request: com.mineralord.tcg.core.animation.AnimationRequest) = director.submit(request)
        })
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < CompactWidthThreshold
            if (compact) {
                Column(modifier = Modifier.fillMaxSize()) {
                    BoardArea(scheme, state, coordinates, renderState, controller, Modifier.fillMaxWidth().weight(1f))
                    ActionPanel(scheme, state, controller, horizontal = true)
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    BoardArea(scheme, state, coordinates, renderState, controller, Modifier.fillMaxHeight().weight(1f))
                    ActionPanel(scheme, state, controller, horizontal = false, modifier = Modifier.width(ActionPanelWidth))
                }
            }
        }
        state.overlay?.let { overlay ->
            OverlayModal(scheme, overlay, controller)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tapete (zonas) + cartas, dentro del motor real.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BoardArea(
    scheme: StudioColorTokens.Scheme,
    state: CombatSceneState,
    coordinates: CoordinateRegistry,
    renderState: AnimationRenderState,
    controller: CombatSceneController,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(PanelStyle.padding)
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(scheme.surfaceCanvas)
            .border(1.dp, scheme.borderSubtle, RoundedCornerShape(ListRowStyle.radius)),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val boardW = maxWidth
            val boardH = maxHeight
            val density = LocalDensity.current
            val boardWpx = with(density) { boardW.toPx() }
            val boardHpx = with(density) { boardH.toPx() }

            AnimationStage(
                registry = coordinates,
                renderState = renderState,
                board = { Zones(scheme, boardW, boardH) },
                cards = {
                    state.cards.forEach { card ->
                        CardView(scheme, card, state, controller, boardW, boardH, boardWpx, boardHpx)
                    }
                },
            )
        }
    }
}

/** Dibuja todas las zonas del tapete desde [CombatGeometry]. La Activa del jugador se rastrea como slot. */
@Composable
private fun Zones(scheme: StudioColorTokens.Scheme, boardW: Dp, boardH: Dp) {
    val registry = LocalCoordinateRegistry.current
    CombatGeometry.Zone.entries.forEach { zone ->
        val box = CombatGeometry.layout[zone] ?: return@forEach
        val base = Modifier
            .offset(x = boardW * box.x, y = boardH * box.y)
            .size(width = boardW * box.w, height = boardH * box.h)
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(scheme.surfacePanel)
            .border(1.dp, if (zone.tracked) scheme.selectionEdge else scheme.borderSubtle, RoundedCornerShape(ListRowStyle.radius))
        val m = if (zone.tracked) base.trackBounds(activeSlotId(SCENE_PLAYER), registry) else base
        Box(modifier = m, contentAlignment = Alignment.TopStart) {
            BasicText(
                text = zone.label,
                style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
                modifier = Modifier.padding(PanelStyle.contentGap),
            )
        }
    }
}

/** Carta arrastrable libre. La escena captura el gesto y reporta deltas normalizados al controlador. */
@Composable
private fun CardView(
    scheme: StudioColorTokens.Scheme,
    card: SceneCard,
    state: CombatSceneState,
    controller: CombatSceneController,
    boardW: Dp,
    boardH: Dp,
    boardWpx: Float,
    boardHpx: Float,
) {
    val cardW = boardW * card.widthFrac
    val cardH = cardW / CombatGeometry.CardAspect
    Box(
        modifier = Modifier
            .zIndex(state.zIndexOf(card.id))
            .offset(x = boardW * card.position.x, y = boardH * card.position.y)
            .size(cardW, cardH)
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(scheme.surfaceRaised)
            .border(1.dp, scheme.borderDefault, RoundedCornerShape(ListRowStyle.radius))
            .pointerInput(card.id) {
                detectDragGestures(
                    onDragStart = { controller.onEvent(CombatSceneEvent.CardDragStart(card.id)) },
                    onDrag = { change, delta ->
                        change.consume()
                        controller.onEvent(
                            CombatSceneEvent.CardDrag(card.id, Offset(delta.x / boardWpx, delta.y / boardHpx)),
                        )
                    },
                    onDragEnd = { controller.onEvent(CombatSceneEvent.CardDrop(card.id)) },
                )
            }
            .padding(PanelStyle.contentGap),
        contentAlignment = Alignment.BottomStart,
    ) {
        BasicText(text = card.label, style = coloredText(StudioTypographyTokens.Role.Label, scheme.contentEmphasis))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HUD / Panel de acciones (adaptativo). Renderizado por la escena desde el estado.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionPanel(
    scheme: StudioColorTokens.Scheme,
    state: CombatSceneState,
    controller: CombatSceneController,
    horizontal: Boolean,
    modifier: Modifier = Modifier,
) {
    if (horizontal) {
        Column(
            modifier = modifier.fillMaxWidth().background(scheme.surfacePanel).padding(PanelStyle.contentPadding),
            verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
        ) {
            BasicText(text = state.hudTitle, style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
            ) {
                state.actions.forEach { action ->
                    ActionChip(scheme, action, state.armedActionId == action.id) { controller.onEvent(CombatSceneEvent.ActionClick(action.id)) }
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxHeight().background(scheme.surfacePanel).verticalScroll(rememberScrollState()).padding(PanelStyle.contentPadding),
            verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
        ) {
            BasicText(text = state.hudTitle, style = coloredText(PanelStyle.titleStyle, scheme.contentEmphasis))
            state.actions.forEach { action ->
                ActionChip(scheme, action, state.armedActionId == action.id, fill = true) { controller.onEvent(CombatSceneEvent.ActionClick(action.id)) }
            }
        }
    }
}

@Composable
private fun ActionChip(
    scheme: StudioColorTokens.Scheme,
    action: SceneAction,
    armed: Boolean,
    fill: Boolean = false,
    onClick: () -> Unit,
) {
    val vstate = if (armed) VisualState.Selected else VisualState.Rest
    val colors = ListRowStyle.colors(scheme, vstate)
    val base = Modifier
        .then(if (fill) Modifier.fillMaxWidth() else Modifier)
        .height(ListRowStyle.rowHeight)
        .clip(RoundedCornerShape(ListRowStyle.radius))
        .background(colors.container)
        .border(1.dp, if (armed) scheme.selectionEdge else scheme.borderSubtle, RoundedCornerShape(ListRowStyle.radius))
        .clickable(onClick = onClick)
        .padding(horizontal = ListRowStyle.paddingH)
    Row(modifier = base, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap)) {
        BasicText(text = action.label, style = coloredText(ListRowStyle.textStyle, colors.content))
        if (!action.enabled) {
            BasicText(text = "· próximamente", style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overlay modal (p. ej. selector de variantes). Escena dibuja; controlador decide.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OverlayModal(
    scheme: StudioColorTokens.Scheme,
    overlay: SceneOverlay,
    controller: CombatSceneController,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { controller.onEvent(CombatSceneEvent.OverlayDismiss) },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(PanelStyle.padding)
                .width(360.dp)
                .clip(RoundedCornerShape(ListRowStyle.radius))
                .background(PanelStyle.material.surface(scheme))
                .border(1.dp, scheme.borderDefault, RoundedCornerShape(ListRowStyle.radius))
                .clickable(enabled = false) {}
                .padding(PanelStyle.contentPadding),
            verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
        ) {
            BasicText(text = overlay.title, style = coloredText(PanelStyle.titleStyle, scheme.contentEmphasis))
            BasicText(text = overlay.subtitle, style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted))
            overlay.options.forEach { option ->
                OptionRow(scheme, option, option.id == overlay.selectedId) { controller.onEvent(CombatSceneEvent.OverlayOption(option.id)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap)) {
                SceneButton(scheme, ButtonVariant.Primary, overlay.confirmLabel) { controller.onEvent(CombatSceneEvent.OverlayConfirm) }
                SceneButton(scheme, ButtonVariant.Neutral, "Cancelar") { controller.onEvent(CombatSceneEvent.OverlayDismiss) }
            }
        }
    }
}

@Composable
private fun OptionRow(
    scheme: StudioColorTokens.Scheme,
    option: SceneOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val vstate = if (selected) VisualState.Selected else VisualState.Rest
    val colors = ListRowStyle.colors(scheme, vstate)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ListRowStyle.rowHeight)
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(colors.container)
            .border(1.dp, if (selected) scheme.selectionEdge else scheme.borderSubtle, RoundedCornerShape(ListRowStyle.radius))
            .clickable(onClick = onClick)
            .padding(horizontal = ListRowStyle.paddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
    ) {
        BasicText(text = option.id, style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted))
        BasicText(text = option.label, style = coloredText(ListRowStyle.textStyle, colors.content))
        Box(modifier = Modifier.weight(1f))
        if (option.highlighted) {
            BasicText(text = "★", style = coloredText(StudioTypographyTokens.Role.Status, scheme.selectionEdge))
        }
    }
}

@Composable
private fun SceneButton(
    scheme: StudioColorTokens.Scheme,
    variant: ButtonVariant,
    label: String,
    onClick: () -> Unit,
) {
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
    ) {
        BasicText(text = label, style = coloredText(style.textStyle, colors.content).copy(textAlign = TextAlign.Center))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
private fun coloredText(role: TextStyle, color: Color): TextStyle = role.copy(color = color)
