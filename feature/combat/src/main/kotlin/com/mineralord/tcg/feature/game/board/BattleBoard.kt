package com.mineralord.tcg.feature.game.board

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.feature.combat.R
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.EnergyType

/** Paleta específica del tapete de combate (estilo TCG Live). */
object BattleTheme {
    val OppTop = Color(0xFF6E1C28)
    val OppBottom = Color(0xFF3A0E16)
    val MineTop = Color(0xFF0C2647)
    val MineBottom = Color(0xFF04152B)
    val Gold = TcgColors.Gold
    val CenterBand = Color(0xFF1A1410)
    val SlotLine = Color(0x66E4AB5E)
    val EmptySlot = Color(0x22FFFFFF)
    val HpFill = Color(0xFFFFFFFF)
    val HpInk = Color(0xFF14233D)
    /** Naranja del contador de daño (estilo TCG Live). */
    val DamageOrange = Color(0xFFF39C12)
}

/** Color de acento por tipo de energía. */
fun typeColor(type: EnergyType?): Color = when (type) {
    EnergyType.LIGHTNING -> TcgColors.TypeLightning
    EnergyType.FIRE -> TcgColors.TypeFire
    EnergyType.WATER -> TcgColors.TypeWater
    EnergyType.GRASS -> TcgColors.TypeGrass
    EnergyType.PSYCHIC -> TcgColors.TypePsychic
    EnergyType.FIGHTING -> TcgColors.TypeFighting
    EnergyType.DARKNESS -> TcgColors.TypeDarkness
    EnergyType.METAL -> TcgColors.TypeMetal
    EnergyType.FAIRY -> Color(0xFFE56FB0)
    EnergyType.DRAGON -> Color(0xFFB8923A)
    EnergyType.COLORLESS, null -> Color(0xFFBFC4CC)
}


/**
 * Reverso de carta boca abajo por DEFECTO (`card_back_default`), usado en premios, mazo,
 * descarte y mano rival — y en general en cualquier carta boca abajo del clon. Es el
 * mismo dorso para ambos lados por ahora (más adelante habrá un editor de dorsos, por eso
 * [mine] se conserva en la firma aunque de momento no cambie el arte).
 */
@Composable
fun CardBack(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    mine: Boolean,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.card_back_default),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.size(width, height).clip(RoundedCornerShape(4.dp)),
    )
}

/**
 * Pestaña numérica (estilo TCG Live): etiqueta redondeada anclada al borde SUPERIOR del
 * slot con el número de cartas de la pila. Fondo oscuro, borde del color del lado
 * (azul jugador / granate rival) y número blanco. Es la "pestaña" que llevan todos los
 * slots de pila del tablero auténtico (premios/mazo/descarte).
 */
@Composable
fun NumericTab(count: Int, mine: Boolean, modifier: Modifier = Modifier) {
    val accent = if (mine) Color(0xFF2E7BD6) else TcgColors.Red
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF10151C))
            .border(1.5.dp, accent, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 0.dp),
    ) {
        Text("$count", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

/**
 * Slot de PILA (premios/mazo/descarte) alineado 1:1 a los slots horneados del tablero
 * auténtico. La carta LLENA el slot usando el aspecto de la CAJA (el lado del rival va
 * ESCORZADO por la perspectiva 3D, por eso su caja es más ancha y la carta se ve achatada
 * aunque sea vertical). Lleva una PESTAÑA NUMÉRICA arriba (tapa la pestaña horneada del
 * mat con el conteo real). El descarte muestra la cara real de la carta superior
 * ([topCard]); mazo y premios van boca abajo. Con [fan] los premios se apilan solapados
 * (base abajo, asoman hacia arriba). Slot VACÍO (count==0) => no dibuja nada (se ve el mat).
 */
@Composable
fun PileSlot(
    count: Int,
    mine: Boolean,
    modifier: Modifier = Modifier,
    topCard: Card? = null,
    fan: Boolean = false,
    showTab: Boolean = true,
) {
    if (count <= 0) {
        // Sin cartas: el slot horneado del mat ya muestra la huella vacía; no dibujamos nada.
        Box(modifier.fillMaxSize())
        return
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val cardW = maxWidth
        val cardH = maxHeight
        Box(Modifier.fillMaxSize()) {
            if (fan) {
                // PREMIOS: 6 dorsos VERTICALES de tamaño real (ancho del slot → alto por
                // CardAspect, NUNCA deformados), solapados con base abajo y asomando hacia
                // arriba. El paso se reparte para LLENAR POR COMPLETO el alto del slot con las
                // 6 posiciones FIJAS: al retirar un premio, las cartas restantes NO se mueven
                // (sólo desaparece la retirada), porque `step` no depende del conteo actual.
                val total = 6
                val n = count.coerceIn(1, total)
                val pcardH = cardW / BoardGeometry.CardAspect
                val step = ((cardH - pcardH) / (total - 1)).coerceAtLeast(pcardH * 0.08f)
                // De atrás (arriba) hacia delante (abajo): el frontal queda encima.
                for (i in (n - 1) downTo 0) {
                    Box(Modifier.align(Alignment.BottomCenter).offset(y = -step * i)) {
                        CardBack(width = cardW, height = pcardH, mine = mine)
                    }
                }
            } else {
                // MAZO/DESCARTE (RETRATO) como PILA con CANTO 3D. La carta se dibuja con su
                // ASPECTO EXACTO (CardAspect) — NUNCA se deforma: se ajusta DENTRO del slot por
                // su lado más restrictivo. El canto de papel apilado asoma como una banda gris
                // clara detrás, abajo-izquierda (más grueso cuantas más cartas). La carta
                // superior (dorso en el mazo, cara real en el descarte) queda arriba-derecha.
                val edge = cardW * (0.04f + 0.06f * (count.coerceIn(0, 60) / 60f))
                // Alto/ancho de la carta preservando forma, cabiendo en el slot menos el canto.
                val availW = cardW - edge
                val availH = cardH - edge
                val topW = minOf(availW, availH * BoardGeometry.CardAspect)
                val topH = topW / BoardGeometry.CardAspect
                Box(
                    Modifier.align(Alignment.Center).size(topW + edge, topH + edge),
                ) {
                    // Cuerpo/canto de la pila (mismo tamaño de carta, desplazado detrás).
                    Box(
                        Modifier.align(Alignment.BottomStart).size(topW, topH)
                            .clip(RoundedCornerShape(6.dp)).background(
                                Brush.linearGradient(listOf(Color(0xFFDCDFE4), Color(0xFF8B939C))),
                            ),
                    )
                    // Carta superior con forma EXACTA.
                    Box(
                        Modifier.align(Alignment.TopEnd).size(topW, topH).clip(RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (topCard != null) {
                            AsyncImage(
                                model = topCard.artwork.small(true),
                                contentDescription = topCard.name.es,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            CardBack(width = topW, height = topH, mine = mine)
                        }
                    }
                }
            }
            // Pestaña numérica. En los PREMIOS (fan) va CENTRADA sobre el borde superior del
            // slot (como en TCG Live: bajo el hexágono de chat). En mazo/descarte va arriba-
            // izquierda, tapando la pestaña horneada del mat.
            if (showTab) {
                val tabMod = if (fan) {
                    // PESTAÑA del slot de premios. En TCG Live la pestaña apunta hacia el centro
                    // del tablero: el JUGADOR (abajo) la lleva en el borde SUPERIOR del slot; el
                    // RIVAL (arriba) la lleva en el borde INFERIOR, colgando bajo la última carta.
                    if (mine) {
                        Modifier.align(Alignment.TopCenter).offset(x = cardW * 0.30f, y = cardH * 0.04f)
                    } else {
                        Modifier.align(Alignment.BottomCenter).offset(x = cardW * 0.19f, y = cardH * 0.12f)
                    }
                } else {
                    Modifier.align(Alignment.TopStart).offset(x = cardW * 0.04f, y = -cardH * 0.14f)
                }
                NumericTab(count, mine, tabMod)
            }
        }
    }
}


/**
 * MAZO o DESCARTE como slot de pila con pestaña numérica (estilo TCG Live).
 * El descarte muestra la cara real de su carta superior si [topCard] no es null.
 */
@Composable
fun DeckPile(count: Int, mine: Boolean, label: String, modifier: Modifier = Modifier, topCard: Card? = null, showTab: Boolean = true) {
    PileSlot(count = count, mine = mine, modifier = modifier, topCard = topCard, showTab = showTab)
}


/** Badge de premios del rail (rojo rival / azul jugador, estilo TCG Live). */
@Composable
fun PrizeBadge(count: Int, timer: String, mine: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(7.dp))
                .background(if (mine) Color(0xFF1E63B0) else TcgColors.Red),
            contentAlignment = Alignment.Center,
        ) {
            Text("$count", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(timer, color = TcgColors.Parchment.copy(alpha = 0.85f), fontSize = 9.sp)
    }
}

