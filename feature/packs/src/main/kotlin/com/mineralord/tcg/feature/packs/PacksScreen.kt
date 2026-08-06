package com.mineralord.tcg.feature.packs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/** Paso del flujo de apertura, al estilo Pokémon TCG Pocket. */
private enum class PackStep { SERIES, EXPANSION, PACK }

/**
 * Pantalla de sobres al estilo Pokémon TCG Pocket: SERIE → EXPANSIÓN → abrir el sobre (deslizando
 * hacia arriba). Sin ruleta de sobres. Hoy solo la serie «Escarlata y Púrpura» con la expansión 151.
 */
@Composable
fun PacksScreen(
    modifier: Modifier = Modifier,
    viewModel: PacksViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Apertura a pantalla completa (revelado carta a carta). Tiene prioridad sobre el navegador.
    if (state.opening) {
        PackOpeningOverlay(
            cards = state.revealed,
            setLabel = state.setLabel,
            remainingToday = state.remainingToday,
            onDismiss = viewModel::dismissOpening,
        )
        return
    }

    var step by remember { mutableStateOf(PackStep.SERIES) }
    var series by remember { mutableStateOf<SeriesUi?>(null) }
    var expansion by remember { mutableStateOf<ExpansionUi?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF161B2E), Color(0xFF0A0E1A)))),
    ) {
        if (state.loading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val dir = if (forward) 1 else -1
                (slideInHorizontally(tween(320)) { w -> dir * w } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(320)) { w -> -dir * w } + fadeOut(tween(180)))
            },
            label = "packStep",
        ) { s ->
            when (s) {
                PackStep.SERIES -> SeriesSelectStage(
                    series = PACK_CATALOG,
                    onSelect = { series = it; step = PackStep.EXPANSION },
                )
                PackStep.EXPANSION -> {
                    val sel = series ?: PACK_CATALOG.first()
                    ExpansionSelectStage(
                        series = sel,
                        remainingToday = state.remainingToday,
                        maxPerDay = state.maxPerDay,
                        onBack = { step = PackStep.SERIES },
                        onSelect = { expansion = it; step = PackStep.PACK },
                    )
                }
                PackStep.PACK -> PackStage(
                    expansion = expansion ?: PACK_CATALOG.first().expansions.first(),
                    remainingToday = state.remainingToday,
                    maxPerDay = state.maxPerDay,
                    deniedMessage = state.deniedMessage,
                    onBack = { step = PackStep.EXPANSION },
                    onOpen = viewModel::openPack,
                )
            }
        }
    }
}

/**
 * Etapa del sobre: el envoltorio flota y se abre DESLIZÁNDOLO HACIA ARRIBA (gesto de TCG Pocket).
 * Al superar el umbral se dispara [onOpen] y arranca la animación de revelado a pantalla completa.
 */
@Composable
private fun PackStage(
    expansion: ExpansionUi,
    remainingToday: Int,
    maxPerDay: Int,
    deniedMessage: String?,
    onBack: () -> Unit,
    onOpen: () -> Unit,
) {
    val canOpen = remainingToday > 0
    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var firing by remember { mutableStateOf(false) }
    val openThreshold = -180f

    // Pista visual: flecha que sube y baja invitando a deslizar.
    val hintPulse = remember { Animatable(0f) }
    LaunchedEffect(canOpen) {
        if (canOpen) while (true) { hintPulse.animateTo(1f, tween(700)); hintPulse.animateTo(0f, tween(700)) }
    }

    Box(Modifier.fillMaxSize().padding(20.dp)) {
        // Botón "Volver" (toca para retroceder a la elección de expansión).
        Box(
            Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(50))
                .background(Color(0x22FFFFFF))
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) { Text("‹  Volver", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (canOpen) "Desliza el sobre hacia arriba" else "Sin sobres por hoy",
                color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text("Restantes hoy: $remainingToday/$maxPerDay", color = Color(0xB3FFFFFF), fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))

            // El sobre: se arrastra en Y (solo hacia arriba) y se abre al soltar pasado el umbral.
            BoosterPack(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .aspectRatio(0.62f)
                    .graphicsLayer {
                        translationY = dragY.value
                        val lift = (-dragY.value / -openThreshold).coerceIn(0f, 1f)
                        val s = 1f + lift * 0.06f
                        scaleX = s; scaleY = s
                    }
                    .pointerInput(canOpen) {
                        if (!canOpen) return@pointerInput
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, delta ->
                                change.consume()
                                scope.launch { dragY.snapTo((dragY.value + delta).coerceIn(openThreshold * 1.4f, 40f)) }
                            },
                            onDragEnd = {
                                if (dragY.value <= openThreshold && !firing) {
                                    firing = true
                                    scope.launch { dragY.animateTo(-1400f, tween(260)) }
                                    onOpen()
                                } else {
                                    scope.launch { dragY.animateTo(0f, tween(220)) }
                                }
                            },
                        )
                    },
            )

            Spacer(Modifier.height(16.dp))
            if (canOpen) {
                // Flecha-guía pulsante hacia arriba.
                Text(
                    "▲",
                    color = Color.White.copy(alpha = 0.4f + 0.5f * hintPulse.value),
                    fontSize = 22.sp,
                    modifier = Modifier.graphicsLayer { translationY = -hintPulse.value * 10f },
                )
            }
            deniedMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
