package com.mineralord.tcg.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import com.mineralord.tcg.core.designsystem.motion.AnimationCurves
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.feature.game.board.BattleTheme
import com.mineralord.tcg.feature.game.board.BoardGeometry
import com.mineralord.tcg.feature.game.board.CardBack
import com.mineralord.tcg.feature.game.board.DeckPile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val STAGGER_MS = 120L
private const val TRAVEL_MS = 380
private const val SETTLE_MS = 550L

/**
 * Reparto animado de la mano inicial (réplica de TCG Live): las cartas salen de la
 * pila del mazo (esquina inferior derecha) y viajan una a una hacia un abanico
 * centrado en la zona de la mano, revelándose (dorso → arte) al aterrizar. Al
 * terminar la última + un breve reposo, avisa con [onComplete] para pasar a la
 * preparación. Se dibuja sobre el tapete vacío (el mat ya está de fondo).
 *
 * Toda la geometría se ancla al mismo lienzo 1080:2400 que el tablero jugable
 * (letterbox por [BoardGeometry.Aspect]), reusando las cajas [BoardGeometry.MeDeck]
 * (origen) y [BoardGeometry.MeHand] (destino) para que el reparto caiga justo donde
 * luego vivirá la mano.
 */
@Composable
fun DealOverlay(cards: List<Card>, onComplete: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availW = maxWidth
        val availH = maxHeight
        val boardW = if (availW <= availH * BoardGeometry.Aspect) availW else availH * BoardGeometry.Aspect
        val boardH = if (availW <= availH * BoardGeometry.Aspect) availW / BoardGeometry.Aspect else availH
        val boardLeft = (availW - boardW) / 2f
        val boardTop = (availH - boardH) / 2f

        // Origen: centro de la pila del mazo del jugador.
        val deck = BoardGeometry.MeDeck
        val deckCx = boardLeft + boardW * deck.cx
        val deckCy = boardTop + boardH * deck.cy

        // Destino: abanico repartido dentro de la caja de la mano.
        val hand = BoardGeometry.MeHand
        val handLeft = boardLeft + boardW * hand.x
        val handW = boardW * hand.w
        val handTop = boardTop + boardH * hand.y

        val cardW = 76.dp
        val cardH = 106.dp
        val n = cards.size
        val center = (n - 1) / 2f
        // Reparte los bordes izquierdos entre [handLeft, handLeft+handW-cardW] con
        // inset de media carta, de modo que NINGUNA carta se salga de la zona.
        val spacing = if (n > 1) (handW - cardW) / (n - 1) else 0.dp

        // Un progreso 0→1 por carta, arrancados de forma escalonada.
        val progress = cards.map { remember { Animatable(0f) } }
        LaunchedEffect(Unit) {
            cards.indices.forEach { i ->
                launch {
                    delay(i * STAGGER_MS)
                    progress[i].animateTo(1f, tween(TRAVEL_MS, easing = AnimationCurves.Standard))
                }
            }
            delay(n * STAGGER_MS + TRAVEL_MS + SETTLE_MS)
            onComplete()
        }

        // Pila del mazo (origen) dibujada estática en su caja.
        Box(Modifier.offset(x = deckCx - 20.dp, y = deckCy - 27.dp)) {
            DeckPile(count = 60 - n, mine = true, label = "Mazo")
        }

        cards.forEachIndexed { i, card ->
            val delta = i - center
            val angle = delta * 4.5f
            val lift = (abs(delta) * abs(delta) * 1.6f).dp
            val p = progress[i].value

            // Interpolación mazo (centro) → posición final en el abanico.
            val fromX = deckCx - cardW / 2f
            val fromY = deckCy - cardH / 2f
            val toX = handLeft + spacing * i
            val toY = handTop + lift
            val x = fromX + (toX - fromX) * p
            val y = fromY + (toY - fromY) * p
            val scale = 0.5f + 0.5f * p

            Box(
                Modifier
                    .zIndex(i.toFloat())
                    .offset(x = x, y = y)
                    .size(cardW, cardH)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = angle * p
                    },
            ) {
                // Viaja de dorso; se revela (arte) al aterrizar.
                if (p < 0.6f) {
                    CardBack(width = cardW, height = cardH, mine = true)
                } else {
                    AsyncImage(
                        model = card.artwork.small(true),
                        contentDescription = card.name.es,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(7.dp))
                            .border(1.dp, BattleTheme.Gold.copy(alpha = 0.5f), RoundedCornerShape(7.dp)),
                    )
                }
            }
        }
    }
}
