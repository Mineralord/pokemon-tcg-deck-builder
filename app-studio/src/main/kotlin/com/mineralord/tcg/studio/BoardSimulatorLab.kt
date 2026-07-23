package com.mineralord.tcg.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import com.mineralord.tcg.studio.assets.AnimationAsset
import com.mineralord.tcg.studio.assets.AssetRegistry
import com.mineralord.tcg.studio.assets.AssetStatus
import com.mineralord.tcg.studio.assets.AssetType
import com.mineralord.tcg.studio.shell.StudioWindowWidth
import kotlin.math.roundToInt

/**
 * **Board Simulator — Lab v1 (Game Sandbox): el corazón del Studio.**
 *
 * Un tapete REAL del Pokémon TCG Clone sin lógica de partida: el usuario coloca cartas donde quiera,
 * las superpone y las recoloca a mano (mesa física). Reproduce recursos visuales sobre un escenario
 * real usando EXACTAMENTE el mismo motor que usará el juego —`AnimationStage` + el `AnimationDirector`
 * canónico ([rememberCanonicalAnimationDirector])—; no hay caminos paralelos ni lógica duplicada.
 *
 * **Consumo del Asset Registry por categoría, nunca por id:** la acción Evolution sólo conoce la
 * categoría [EVOLUTION_CATEGORY]; al soltar una carta sobre otra, pregunta al [AssetRegistry] qué
 * variantes existen en esa categoría y ofrece las que devuelva (hoy EVO_001…EVO_005). El Sandbox no
 * cita ninguna variante concreta.
 *
 * Es la base pensada para crecer hasta el laboratorio central: futuras capacidades (partículas,
 * sonidos, cámaras, shaders…) se añaden como nuevas [SandboxAction] sobre este mismo escenario.
 * Adaptativo por contrato (panel lateral en ancho medio/expandido; inferior en compacto).
 */

private const val SANDBOX_PLAYER = "studio"
private val CardW: Dp = 64.dp
private val CardH: Dp = 92.dp
private val ActionPanelWidth: Dp = 236.dp

@Composable
fun BoardSimulatorLabContent(registry: AssetRegistry) {
    val scheme = StudioTheme.colors

    // Escenario manual (hoisted). Cartas reales fijas: Bulbasaur → Ivysaur → Venusaur.
    val controller = remember {
        BoardController().apply {
            cards += SandboxCard("bulbasaur", "Bulbasaur", Offset(40f, 380f))
            cards += SandboxCard("ivysaur", "Ivysaur", Offset(150f, 380f))
            cards += SandboxCard("venusaur", "Venusaur", Offset(260f, 380f))
            cards.forEach { zOrder += it.id }
        }
    }

    // Anfitrión del motor real: mismas coordenadas/estado que consume el AnimationStage del tapete.
    val coordinates = remember { CoordinateRegistry() }
    val renderState = remember { AnimationRenderState() }
    val director = rememberCanonicalAnimationDirector(coordinates, renderState)

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = StudioWindowWidth.fromWidth(maxWidth) == StudioWindowWidth.Compact
            if (compact) {
                Column(modifier = Modifier.fillMaxSize()) {
                    BoardArea(scheme, controller, coordinates, renderState, Modifier.fillMaxWidth().weight(1f))
                    ActionPanel(scheme, controller, horizontal = true)
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    BoardArea(scheme, controller, coordinates, renderState, Modifier.fillMaxHeight().weight(1f))
                    ActionPanel(scheme, controller, horizontal = false, modifier = Modifier.width(ActionPanelWidth))
                }
            }
        }

        // Selector de variantes: se abre al soltar una carta sobre otra con Evolution armada.
        controller.pendingEvolution?.let { pending ->
            VariantPicker(
                scheme = scheme,
                registry = registry,
                pending = pending,
                controller = controller,
                onPlay = { asset -> director.submit(asset.request); controller.pendingEvolution = null },
                onDismiss = { controller.pendingEvolution = null },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Escenario (tapete) + capa de cartas arrastrables, dentro del motor real.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BoardArea(
    scheme: StudioColorTokens.Scheme,
    controller: BoardController,
    coordinates: CoordinateRegistry,
    renderState: AnimationRenderState,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val cardSizePx = with(density) { Size(CardW.toPx(), CardH.toPx()) }

    Box(
        modifier = modifier
            .padding(PanelStyle.padding)
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(scheme.surfaceCanvas)
            .border(1.dp, scheme.borderSubtle, RoundedCornerShape(ListRowStyle.radius)),
    ) {
        AnimationStage(
            registry = coordinates,
            renderState = renderState,
            board = { BoardMat(scheme) },
            cards = {
                controller.cards.forEach { card ->
                    DraggableCard(scheme, card, controller, cardSizePx)
                }
            },
        )
    }
}

/** Tapete estático (zonas visuales). Sólo la zona Activa se rastrea como slot del motor. */
@Composable
private fun BoardMat(scheme: StudioColorTokens.Scheme) {
    Column(
        modifier = Modifier.fillMaxSize().padding(PanelStyle.padding),
        verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
    ) {
        Row(modifier = Modifier.fillMaxWidth().weight(1.2f), horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap)) {
            Zone(scheme, BoardZone.Prizes, Modifier.weight(1f).fillMaxHeight())
            Zone(scheme, BoardZone.Active, Modifier.weight(1.4f).fillMaxHeight(), tracked = true)
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap)) {
                Zone(scheme, BoardZone.Deck, Modifier.fillMaxWidth().weight(1f))
                Zone(scheme, BoardZone.Discard, Modifier.fillMaxWidth().weight(1f))
            }
        }
        Zone(scheme, BoardZone.Bench, Modifier.fillMaxWidth().weight(1f))
        Zone(scheme, BoardZone.Hand, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun Zone(
    scheme: StudioColorTokens.Scheme,
    zone: BoardZone,
    modifier: Modifier = Modifier,
    tracked: Boolean = false,
) {
    val registry = LocalCoordinateRegistry.current
    val base = modifier
        .clip(RoundedCornerShape(ListRowStyle.radius))
        .background(scheme.surfacePanel)
        .border(1.dp, if (tracked) scheme.selectionEdge else scheme.borderSubtle, RoundedCornerShape(ListRowStyle.radius))
    val tracking = if (tracked) base.trackBounds(activeSlotId(SANDBOX_PLAYER), registry) else base
    Box(modifier = tracking, contentAlignment = Alignment.TopStart) {
        BasicText(
            text = zone.label,
            style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
            modifier = Modifier.padding(PanelStyle.contentGap),
        )
    }
}

/** Carta física arrastrable libremente. Al soltar con Evolution armada y solaparse con otra, evoluciona. */
@Composable
private fun DraggableCard(
    scheme: StudioColorTokens.Scheme,
    card: SandboxCard,
    controller: BoardController,
    cardSizePx: Size,
) {
    Box(
        modifier = Modifier
            .zIndex(controller.zIndexOf(card.id))
            .offset { IntOffset(card.offset.x.roundToInt(), card.offset.y.roundToInt()) }
            .size(CardW, CardH)
            .clip(RoundedCornerShape(ListRowStyle.radius))
            .background(scheme.surfaceRaised)
            .border(1.dp, scheme.borderDefault, RoundedCornerShape(ListRowStyle.radius))
            .pointerInput(card.id) {
                detectDragGestures(
                    onDragStart = { controller.bringToFront(card.id) },
                    onDrag = { change, delta -> change.consume(); card.offset += delta },
                    onDragEnd = { onCardDropped(card, controller, cardSizePx) },
                )
            }
            .padding(PanelStyle.contentGap),
        contentAlignment = Alignment.BottomStart,
    ) {
        BasicText(text = card.name, style = coloredText(StudioTypographyTokens.Role.Label, scheme.contentEmphasis))
    }
}

/** Hit-test al soltar: si la acción Evolution está armada y la carta se solapa con otra, abre el selector. */
private fun onCardDropped(dropped: SandboxCard, controller: BoardController, sizePx: Size) {
    if (controller.armedActionId != "evolution") return
    val droppedRect = Rect(dropped.offset, sizePx)
    val target = controller.cards.firstOrNull { other ->
        other.id != dropped.id && droppedRect.overlaps(Rect(other.offset, sizePx))
    } ?: return
    controller.pendingEvolution = PendingEvolution(baseId = target.id, evolvingId = dropped.id)
}

// ─────────────────────────────────────────────────────────────────────────────
// Panel de acciones (genérico, adaptativo). Sólo Evolution está implementada.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionPanel(
    scheme: StudioColorTokens.Scheme,
    controller: BoardController,
    horizontal: Boolean,
    modifier: Modifier = Modifier,
) {
    val header: @Composable () -> Unit = {
        BasicText(
            text = if (horizontal) "Acciones — arma «Evolution» y arrastra una carta sobre otra"
            else "Acciones",
            style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
        )
    }
    if (horizontal) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(scheme.surfacePanel)
                .padding(PanelStyle.contentPadding),
            verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
        ) {
            header()
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
            ) {
                SandboxActions.forEach { action ->
                    ActionChip(scheme, action, controller.armedActionId == action.id) {
                        controller.armedActionId = if (controller.armedActionId == action.id) null else action.id
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .background(scheme.surfacePanel)
                .verticalScroll(rememberScrollState())
                .padding(PanelStyle.contentPadding),
            verticalArrangement = Arrangement.spacedBy(PanelStyle.contentGap),
        ) {
            BasicText(text = "Board Simulator", style = coloredText(PanelStyle.titleStyle, scheme.contentEmphasis))
            header()
            SandboxActions.forEach { action ->
                ActionChip(scheme, action, controller.armedActionId == action.id, fill = true) {
                    controller.armedActionId = if (controller.armedActionId == action.id) null else action.id
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    scheme: StudioColorTokens.Scheme,
    action: SandboxAction,
    armed: Boolean,
    fill: Boolean = false,
    onClick: () -> Unit,
) {
    val state = if (armed) VisualState.Selected else VisualState.Rest
    val colors = ListRowStyle.colors(scheme, state)
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
        if (!action.implemented) {
            BasicText(text = "· próximamente", style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Selector de variantes (consulta al Asset Registry por categoría).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VariantPicker(
    scheme: StudioColorTokens.Scheme,
    registry: AssetRegistry,
    pending: PendingEvolution,
    controller: BoardController,
    onPlay: (AnimationAsset) -> Unit,
    onDismiss: () -> Unit,
) {
    // El Sandbox sólo conoce la categoría; el Registry provee las variantes concretas.
    val variants = registry.byCategory(AssetType.Animation, EVOLUTION_CATEGORY).filterIsInstance<AnimationAsset>()
    var selectedId by remember { mutableStateOf(variants.firstOrNull()?.id.orEmpty()) }
    val base = controller.cardById(pending.baseId)?.name ?: pending.baseId
    val evolving = controller.cardById(pending.evolvingId)?.name ?: pending.evolvingId

    // Scrim modal.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
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
            BasicText(text = "Evolución — $evolving sobre $base", style = coloredText(PanelStyle.titleStyle, scheme.contentEmphasis))
            BasicText(
                text = "Categoría «$EVOLUTION_CATEGORY» · ${variants.size} variantes del Asset Registry",
                style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted),
            )
            variants.forEach { asset ->
                VariantRow(scheme, asset, asset.id == selectedId) { selectedId = asset.id }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PanelStyle.contentGap)) {
                val selected = variants.firstOrNull { it.id == selectedId }
                ActionButton(scheme, ButtonVariant.Primary, "Play") { selected?.let(onPlay) }
                ActionButton(scheme, ButtonVariant.Neutral, "Cancelar", onDismiss)
            }
        }
    }
}

@Composable
private fun VariantRow(
    scheme: StudioColorTokens.Scheme,
    asset: AnimationAsset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val state = if (selected) VisualState.Selected else VisualState.Rest
    val colors = ListRowStyle.colors(scheme, state)
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
        BasicText(text = asset.id, style = coloredText(StudioTypographyTokens.Role.Status, scheme.contentMuted))
        BasicText(text = asset.name, style = coloredText(ListRowStyle.textStyle, colors.content))
        Box(modifier = Modifier.weight(1f))
        if (asset.status == AssetStatus.Canon) {
            BasicText(text = "★", style = coloredText(StudioTypographyTokens.Role.Status, scheme.selectionEdge))
        }
    }
}

@Composable
private fun ActionButton(
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
