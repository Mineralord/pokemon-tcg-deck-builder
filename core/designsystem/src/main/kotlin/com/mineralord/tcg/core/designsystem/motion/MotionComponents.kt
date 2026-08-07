package com.mineralord.tcg.core.designsystem.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp

/**
 * COMPONENTES base del framework Motion. Cada uno encapsula un patrón de animación completo
 * apoyándose SOLO en la infraestructura central ([MotionTransitions], [AnimationManager],
 * modificadores `motion*`). Son la única vía recomendada para animar en el proyecto: no
 * contienen valores mágicos ni transiciones inline.
 *
 * No tocan motor, reglas, estado ni ViewModels: son pura capa visual reutilizable.
 */

// --- Contenedor de visibilidad genérico -----------------------------------------------------

/** Aparición/desaparición estándar (fade + escala) de cualquier contenido según [visible]. */
@Composable
fun MotionContainer(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition? = null,
    exit: ExitTransition? = null,
    label: String = "MotionContainer",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter ?: MotionTransitions.enter(),
        exit = exit ?: MotionTransitions.exit(),
        label = label,
        content = content,
    )
}

// --- Superficie con color animado ------------------------------------------------------------

/** Superficie cuyo [color] de fondo se interpola suavemente al cambiar. Base de card/panel. */
@Composable
fun MotionSurface(
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    shape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateColorAsState(
        targetValue = color,
        animationSpec = AnimationSpecs.color(),
        label = "MotionSurfaceColor",
    )
    Box(modifier.clip(shape).background(animated), content = content)
}

// --- Card con feedback de pulsación ----------------------------------------------------------

/** Tarjeta animada: color interpolado + encogido al pulsar (si es interactiva vía [onClick]). */
@Composable
fun MotionCard(
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    shape: Shape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    MotionSurface(
        modifier = if (onClick != null) modifier.motionPress(onClick = onClick) else modifier,
        color = color,
        shape = shape,
        content = content,
    )
}

// --- Botón con feedback de pulsación ---------------------------------------------------------

/** Botón animado: encogido al pulsar + color interpolado. El contenido se dispone en fila. */
@Composable
fun MotionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color.Transparent,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    MotionSurface(
        modifier = modifier.motionPress(enabled = enabled, onClick = onClick),
        color = color,
        shape = shape,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// --- Panel deslizante -------------------------------------------------------------------------

/** Panel que entra/sale deslizando desde [edge] (con fade). Ideal para barras y hojas laterales. */
@Composable
fun MotionPanel(
    visible: Boolean,
    modifier: Modifier = Modifier,
    edge: MotionEdge = MotionEdge.Bottom,
    label: String = "MotionPanel",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = MotionTransitions.slideEnter(edge),
        exit = MotionTransitions.slideExit(edge),
        label = label,
        content = content,
    )
}

// --- Overlay (scrim) --------------------------------------------------------------------------

/** Capa a pantalla completa que se funde al entrar/salir; opcionalmente cierra al tocarla. */
@Composable
fun MotionOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black.copy(alpha = AnimationConstants.AlphaScrim),
    onScrimClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = MotionTransitions.overlayEnter(),
        exit = MotionTransitions.overlayExit(),
        label = "MotionOverlay",
    ) {
        val scrimModifier = if (onScrimClick != null) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onScrimClick,
            )
        } else {
            Modifier
        }
        Box(
            Modifier.fillMaxSize().background(scrimColor).then(scrimModifier),
            content = content,
        )
    }
}

// --- Dialog (scrim + contenido con pop propio) -----------------------------------------------

/** Diálogo animado: scrim que se funde + contenido centrado con su propia entrada/salida (pop). */
@Composable
fun MotionDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black.copy(alpha = AnimationConstants.AlphaScrim),
    dismissOnScrimClick: Boolean = true,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = MotionTransitions.overlayEnter(),
        exit = MotionTransitions.overlayExit(),
        label = "MotionDialog",
    ) {
        val scrimModifier = if (dismissOnScrimClick) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
        } else {
            Modifier
        }
        Box(
            Modifier.fillMaxSize().background(scrimColor).then(scrimModifier),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .animateEnterExit(
                        enter = MotionTransitions.dialogEnter(),
                        exit = MotionTransitions.dialogExit(),
                    )
                    .then(modifier),
            ) {
                content()
            }
        }
    }
}

// --- Screen (transición entre estados de pantalla) -------------------------------------------

/** Envoltorio de transición entre estados de pantalla ([targetState]) con crossfade coherente. */
@Composable
fun <S> MotionScreen(
    targetState: S,
    modifier: Modifier = Modifier,
    label: String = "MotionScreen",
    content: @Composable AnimatedContentScope.(S) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        // transitionSpec NO es @Composable: se usan specs estáticos (sin lectura de config).
        transitionSpec = {
            fadeIn(AnimationSpecs.standard()) togetherWith fadeOut(AnimationSpecs.exit())
        },
        label = label,
        content = content,
    )
}

// --- Envoltorios avanzados (elementos compartidos / lookahead) -------------------------------

/** Habilita transiciones de ELEMENTOS COMPARTIDOS en su árbol (p. ej. carta mano→tablero). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MotionSharedLayout(
    modifier: Modifier = Modifier,
    content: @Composable SharedTransitionScope.() -> Unit,
) {
    SharedTransitionLayout(modifier = modifier, content = content)
}

/** Habilita medición anticipada (Lookahead) para auto-animar cambios de layout de sus hijos. */
@Composable
fun MotionLookahead(
    content: @Composable LookaheadScope.() -> Unit,
) {
    LookaheadScope(content = content)
}
