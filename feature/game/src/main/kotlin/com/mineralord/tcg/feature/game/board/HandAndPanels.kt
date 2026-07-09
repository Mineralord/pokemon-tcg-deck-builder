package com.mineralord.tcg.feature.game.board

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.SpecialEnergy

/** Color de fondo para una carta de energía en la mano (null si no es energía). */
private fun energyCardColor(card: Card): Color? = when (card) {
    is BasicEnergy -> typeColor(card.type)
    is SpecialEnergy -> Color(0xFF9E9E9E)
    else -> null
}

/** Acción mostrada en el panel de carta (ataque, habilidad, jugar, retirada). */
data class SheetAction(
    val label: String,
    val sublabel: String?,
    val accent: Color,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

/**
 * Banner de cambio de turno (estilo TCG Live real): barra horizontal plateada a
 * TODO el ancho, centrada, con texto en negrita ("TU TURNO" / "EL TURNO DEL RIVAL").
 * Aparece con un breve fade+scale y se desvanece, en vez del antiguo deslizamiento
 * lateral.
 */
@Composable
fun TurnBanner(text: String, visible: Boolean, mine: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(tween(260), initialScale = 0.92f),
        exit = fadeOut(tween(220)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        if (mine) {
                            listOf(Color(0xFFF4F1EA), Color(0xFFD9D2C4), Color(0xFFF4F1EA))
                        } else {
                            listOf(Color(0xFF8E2330), Color(0xFF5A121C), Color(0xFF8E2330))
                        },
                    ),
                )
                .border(
                    width = 2.dp,
                    color = BattleTheme.Gold,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                color = if (mine) TcgColors.Ink else Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
            )
        }
    }
}

/** Mano en abanico, con cartas ligeramente rotadas y solapadas. El tamaño de carta
 *  se pasa desde el tablero (proporcional a su alto) para respetar la escala 1:1. */
@Composable
fun HandFan(
    cards: List<Card>,
    enabled: Boolean,
    onSelect: (Card) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: CardId? = null,
    cardW: androidx.compose.ui.unit.Dp = 76.dp,
    cardH: androidx.compose.ui.unit.Dp = 106.dp,
    // Arrastre de cartas (energía a un Pokémon, o evolución sobre su pre-evolución).
    // Posiciones en coords de RAÍZ. `canDrag` decide qué cartas son arrastrables.
    canDrag: (Card) -> Boolean = { false },
    onCardDragStart: ((Card, Offset) -> Unit)? = null,
    onCardDrag: ((Offset) -> Unit)? = null,
    onCardDragEnd: (() -> Unit)? = null,
    onCardDragCancel: (() -> Unit)? = null,
) {
    // El gesto de arrastre vive en un `pointerInput(card.id)` que captura sus lambdas
    // UNA sola vez (la clave no cambia). Sin esto, `onCardDragEnd` quedaba congelado en
    // la PRIMERA composición —cuando aún no se arrastra nada— y perdía los valores que
    // el tablero calcula por composición al arrastrar (objetivo de Objeto dirigido/
    // Herramienta, estado vigente). Resultado: la Poción y demás Objetos dirigidos no se
    // jugaban al soltarlos. `rememberUpdatedState` mantiene vivas las ÚLTIMAS lambdas.
    val latestOnDragStart by rememberUpdatedState(onCardDragStart)
    val latestOnDrag by rememberUpdatedState(onCardDrag)
    val latestOnDragEnd by rememberUpdatedState(onCardDragEnd)
    val latestOnDragCancel by rememberUpdatedState(onCardDragCancel)
    val center = (cards.size - 1) / 2f
    val maxAbs = center.coerceAtLeast(1f)
    // Solape moderado: cartas separadas y legibles como en TCG Live (ref_0040).
    val overlap = cardW * 0.24f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(top = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(-overlap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        cards.forEachIndexed { i, card ->
            val delta = i - center
            val norm = delta / maxAbs                      // -1 (izq) .. 1 (der)
            val isSel = selectedId != null && card.id == selectedId
            // Abanico SUTIL como TCG Live: rotación pequeña con pivote en la BASE, y un
            // arco muy suave (colina) donde la carta central sube apenas y los extremos
            // quedan en la línea base (nada se hunde bajo el borde inferior).
            val angle = if (isSel) 0f else norm * 5f
            val hill = cardH * (0.05f * (1f - norm * norm))
            val lift = if (isSel) cardH * 0.16f else hill
            val cardScale = if (isSel) 1.14f else 1f
            val energyBg = energyCardColor(card)
            // Cartas arrastrables (energías / evoluciones) según `canDrag`.
            val draggable = enabled && canDrag(card) && onCardDragStart != null
            var cardCoords by remember(card.id) { mutableStateOf<LayoutCoordinates?>(null) }
            Box(
                modifier = Modifier
                    .zIndex(if (isSel) 2f else 0f)
                    // Rotación/escala como capa de DIBUJO con pivote en la BASE de la carta
                    // → abanico limpio (bases convergen, puntas se despliegan) sin sumar
                    // alto de layout ni comprimir/deformar la carta.
                    .graphicsLayer {
                        rotationZ = angle
                        scaleX = cardScale; scaleY = cardScale
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .offset(y = -lift)
                    .size(cardW, cardH)
                    .clip(RoundedCornerShape(7.dp))
                    .border(
                        if (isSel) 2.dp else 1.dp,
                        if (isSel) BattleTheme.Gold else BattleTheme.Gold.copy(alpha = 0.5f),
                        RoundedCornerShape(7.dp),
                    )
                    .onGloballyPositioned { cardCoords = it }
                    // Tocar una carta SIEMPRE la inspecciona (también en el turno del
                    // rival), para poder leer las cartas de la mano en cualquier momento.
                    // El arrastre (jugar) sí queda restringido por `enabled`/`draggable`.
                    .then(
                        if (draggable) Modifier.pointerInput(card.id) {
                            // Solo el arrastre VERTICAL (hacia arriba, hacia el tablero)
                            // levanta la carta; el gesto horizontal NO se consume y lo recibe
                            // el horizontalScroll de la fila, permitiendo DESLIZAR la mano
                            // para ojearla sin arrastrar cartas.
                            detectVerticalDragGestures(
                                onDragStart = { local ->
                                    cardCoords?.let { latestOnDragStart?.invoke(card, it.localToRoot(local)) }
                                },
                                onVerticalDrag = { change, _ ->
                                    cardCoords?.let { latestOnDrag?.invoke(it.localToRoot(change.position)) }
                                },
                                onDragEnd = { latestOnDragEnd?.invoke() },
                                onDragCancel = { latestOnDragCancel?.invoke() },
                            )
                        } else Modifier,
                    )
                    .clickable { onSelect(card) },
            ) {
                if (energyBg != null) {
                    // Energía: tarjeta de color con el símbolo centrado (estilo TCG Live).
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(energyBg, energyBg.copy(alpha = 0.55f)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = card.artwork.small(true),
                            contentDescription = card.name.es,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                        )
                    }
                } else {
                    AsyncImage(
                        model = card.artwork.small(true),
                        contentDescription = card.name.es,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)),
                    )
                }
            }
        }
    }
}

/**
 * Panel de detalle de carta (sube desde abajo): imagen grande + acciones
 * (ataques/habilidad/jugar/retirada) + debilidad/resistencia/retirada y
 * botones confirmar (verde) / cancelar (X). Réplica del panel de TCG Live.
 */
@Composable
fun CardDetailSheet(
    imageUrl: String,
    title: String,
    info: String?,
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(tween(260)) { it } + fadeIn(tween(260)),
        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color(0xF21A1A1A))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(96.dp, 134.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(title, color = TcgColors.Parchment, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    info?.let { Text(it, color = TcgColors.Parchment.copy(alpha = 0.7f), fontSize = 11.sp) }
                    actions.forEach { a -> SheetActionRow(a) }
                    if (actions.isEmpty()) {
                        Text(
                            "Sin acciones disponibles ahora.",
                            color = TcgColors.Parchment.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            // Barra de cierre (X).
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = TcgColors.Parchment, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Visor de carta en HD a pantalla completa: SOLO la carta grande sobre un velo
 * oscuro, centrada; se toca en cualquier parte para cerrar. Sin texto ni acciones
 * —pensado para LEER la carta como en TCG Live—. El arte se muestra con su aspecto
 * exacto de carta (no se deforma) y ocupa casi todo el ancho.
 */
@Composable
fun CardHdViewer(imageUrl: String, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(BoardGeometry.CardAspect)
                    .clip(RoundedCornerShape(18.dp)),
            )
        }
    }
}

@Composable
private fun SheetActionRow(a: SheetAction) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (a.enabled) a.accent else a.accent.copy(alpha = 0.3f))
            .clickable(enabled = a.enabled, onClick = a.onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(a.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            a.sublabel?.let { Text(it, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp) }
        }
    }
}

/** Overlay "REGISTRO DEL COMBATE" a pantalla completa. */
@Composable
fun BattleLogOverlay(log: List<String>, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF2000000))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "REGISTRO DEL COMBATE",
                color = TcgColors.Gold,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BattleTheme.Gold.copy(alpha = 0.4f)),
            ) {}
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                log.asReversed().forEach { line ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF3EFE7))
                            .padding(10.dp),
                    ) {
                        Text(line, color = TcgColors.Ink, fontSize = 12.sp)
                    }
                }
            }
            Text(
                "Toca para cerrar",
                color = TcgColors.Parchment.copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
        }
    }
}

/** Overlay de fin de partida (victoria/derrota). */
@Composable
fun GameOverOverlay(won: Boolean, onExit: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedVisibility(visible = true, enter = scaleIn(tween(400)) + fadeIn(tween(400))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Emblema de estrella (dorada en victoria, roja en derrota).
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(if (won) TcgColors.Gold.copy(alpha = 0.18f) else TcgColors.Red.copy(alpha = 0.18f))
                            .border(3.dp, if (won) TcgColors.Gold else TcgColors.Red, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("★", color = if (won) TcgColors.Gold else TcgColors.Red, fontSize = 52.sp)
                    }
                    Text(
                        if (won) "¡VICTORIA!" else "DERROTA",
                        color = if (won) TcgColors.Gold else TcgColors.Red,
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        if (won) "¡Has ganado el combate!" else "Has perdido el combate.",
                        color = TcgColors.Parchment.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                    )
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(TcgColors.Gold)
                    .clickable(onClick = onExit)
                    .padding(horizontal = 28.dp, vertical = 12.dp),
            ) {
                Text("Volver al inicio", color = Color(0xFF3A2A08), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
