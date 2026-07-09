package com.mineralord.tcg.feature.game.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.HexagonShape
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.feature.game.R
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.EnergyProvision
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.SpecialEnergy
import com.mineralord.tcg.engine.model.Status

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
 * Fondo del tapete (réplica de TCG Live): textura de PANAL hexagonal sobre un split
 * horizontal (granate rival arriba / azul jugador abajo), con un CARRIL CENTRAL más
 * claro (la "calle" donde van los activos y el mazo), banda/lente dorada en el medio
 * y un marco dorado redondeado alrededor de todo el campo.
 */
@Composable
fun MatBackground(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        // Base del tablero (híbrido): bitmap 1080x2400 derivado de la referencia real
        // de TCG Live (arena ovalada con rieles dorados curvos, zona rival granate,
        // carril central gris hexagonal y zona jugador azul marino). Encima se dibujan
        // las capas dinámicas (cartas/HUD) posicionadas por BoardGeometry.NBox.
        Image(
            painter = painterResource(R.drawable.board_mat),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Dibuja un panal de hexágonos (flat-top) cubriendo el área dada. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHoneycomb(
    w: Float,
    h: Float,
    s: Float,
    color: Color,
) {
    val hexH = (kotlin.math.sqrt(3f) * s)
    val colStep = s * 1.5f
    var col = 0
    var cx = 0f
    while (cx <= w + s) {
        val yOffset = if (col % 2 == 0) 0f else hexH / 2f
        var cy = yOffset
        while (cy <= h + hexH) {
            val path = Path().apply {
                moveTo(cx + s, cy)
                lineTo(cx + s / 2f, cy + hexH / 2f)
                lineTo(cx - s / 2f, cy + hexH / 2f)
                lineTo(cx - s, cy)
                lineTo(cx - s / 2f, cy - hexH / 2f)
                lineTo(cx + s / 2f, cy - hexH / 2f)
                close()
            }
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f))
            cy += hexH
        }
        cx += colStep
        col++
    }
}

/** Óvalo blanco de HP sobre la carta (color del tipo en el borde). */
@Composable
fun HpBadge(pip: PokemonInPlay, modifier: Modifier = Modifier) {
    val accent = typeColor(pip.card.types.firstOrNull())
    Row(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(BattleTheme.HpFill)
            .border(2.dp, accent, RoundedCornerShape(50))
            .padding(horizontal = 5.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
        Text(
            "${pip.remainingHp}",
            color = BattleTheme.HpInk,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
        )
    }
}

/**
 * Una carta de Pokémon en el campo (activo o banca) con su HUD: HP, energías,
 * estados y la barra de daño.
 */
@Composable
fun FieldPokemon(
    pip: PokemonInPlay?,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(width, height)
            .then(if (pip != null) Modifier.shadow(3.dp, RoundedCornerShape(7.dp)) else Modifier)
            .clip(RoundedCornerShape(7.dp))
            .background(BattleTheme.EmptySlot)
            .border(1.dp, BattleTheme.SlotLine, RoundedCornerShape(7.dp))
            .then(if (onClick != null && pip != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (pip == null) return@Box

        AsyncImage(
            model = pip.card.artwork.small(true),
            contentDescription = pip.card.name.es,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)),
        )

        // HP arriba a la derecha.
        HpBadge(pip, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp))

        // Energías individuales (un símbolo por energía unida) + estados, abajo.
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EnergyIcons(pip)
            pip.statuses.forEach { st -> StatusDot(st) }
        }

        // Contador de daño: número grande en círculo naranja sobre la carta
        // (estilo TCG Live), en vez de la antigua barra proporcional.
        if (pip.damage > 0) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(BattleTheme.DamageOrange)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${pip.damage}",
                    color = Color.White,
                    fontSize = if (pip.damage >= 100) 8.sp else 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

/**
 * Energías unidas como símbolos individuales por tipo (estilo TCG Live): un punto
 * coloreado por cada energía. Si hay muchas, muestra hasta 5 y un "+N".
 */
@Composable
private fun EnergyIcons(pip: PokemonInPlay) {
    val energies = pip.attachedEnergy
    val shown = energies.take(5)
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
        shown.forEach { e ->
            com.mineralord.tcg.core.designsystem.EnergySphere(type = energyTypeOf(e), size = 17.dp)
        }
        val extra = energies.size - shown.size
        if (extra > 0) {
            Text("+$extra", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** Tipo de energía representativo de una carta de energía (para colorear su símbolo). */
private fun energyTypeOf(e: EnergyCard): EnergyType? = when (e) {
    is BasicEnergy -> e.type
    is SpecialEnergy -> (e.provides as? EnergyProvision.Fixed)?.types?.firstOrNull()
}

@Composable
private fun StatusDot(status: Status) {
    val (letter, color) = when (status) {
        Status.ASLEEP -> "Z" to Color(0xFF7E57C2)
        Status.CONFUSED -> "?" to Color(0xFFFFB300)
        Status.PARALYZED -> "P" to Color(0xFFFFD54F)
        Status.POISONED -> "X" to Color(0xFF8E24AA)
        Status.BURNED -> "F" to Color(0xFFE8503A)
    }
    Box(
        Modifier.size(13.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
    }
}

/** Pila lateral (mazo / descarte / premios) como una pila de cartas con su contador. */
@Composable
fun CountPile(label: String, count: Int, accent: Color, modifier: Modifier = Modifier) {
    val cardW = 30.dp
    val cardH = 40.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(cardW + 6.dp, cardH + 6.dp), contentAlignment = Alignment.TopStart) {
            // Capas traseras para dar sensación de pila.
            repeat(2) { i ->
                val off = (4 - i * 2).dp
                Box(
                    Modifier
                        .offset(x = off, y = off)
                        .size(cardW, cardH)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent.copy(alpha = 0.55f))
                        .border(1.dp, BattleTheme.Gold.copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
                )
            }
            // Carta frontal con el contador.
            Box(
                Modifier
                    .size(cardW, cardH)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
                    .border(1.dp, BattleTheme.Gold, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$count", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
        Text(label, color = TcgColors.Parchment.copy(alpha = 0.7f), fontSize = 8.sp)
    }
}

/**
 * Racimo de premios (estilo TCG Live): 6 hexágonos; los restantes en dorado, los ya
 * tomados atenuados. [mine] tiñe el acento (azul jugador / granate rival).
 */
@Composable
fun PrizeCluster(remaining: Int, mine: Boolean, modifier: Modifier = Modifier) {
    val dim = (if (mine) BattleTheme.MineTop else BattleTheme.OppTop).copy(alpha = 0.30f)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(2) { col ->
                        val idx = row * 2 + col
                        val filled = idx < remaining
                        Box(
                            Modifier
                                .size(15.dp, 17.dp)
                                .clip(HexagonShape(flatTop = false))
                                .background(if (filled) BattleTheme.Gold else dim)
                                .border(1.dp, BattleTheme.Gold.copy(alpha = 0.7f), HexagonShape(flatTop = false)),
                        )
                    }
                }
            }
        }
        Text("Premios", color = TcgColors.Parchment.copy(alpha = 0.75f), fontSize = 8.sp)
    }
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
 * Pila de PREMIOS: dorsos solapados restantes ([remaining]) con pestaña numérica.
 * Llena `MePrizes`/`OppPrizes`.
 */
@Composable
fun PrizeStack(remaining: Int, mine: Boolean, modifier: Modifier = Modifier) {
    PileSlot(count = remaining, mine = mine, modifier = modifier, fan = true)
}

/**
 * MAZO o DESCARTE como slot de pila con pestaña numérica (estilo TCG Live).
 * El descarte muestra la cara real de su carta superior si [topCard] no es null.
 */
@Composable
fun DeckPile(count: Int, mine: Boolean, label: String, modifier: Modifier = Modifier, topCard: Card? = null, showTab: Boolean = true) {
    PileSlot(count = count, mine = mine, modifier = modifier, topCard = topCard, showTab = showTab)
}

/**
 * Slot de Estadio en el lente central (estilo TCG Live): si hay un Estadio en
 * juego muestra su arte; si no, un marco vacío tenue con la etiqueta "Estadio".
 */
@Composable
fun StadiumSlot(
    stadium: com.mineralord.tcg.engine.model.TrainerCard?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(58.dp, 40.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0x33000000))
                .border(1.dp, BattleTheme.Gold.copy(alpha = 0.5f), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (stadium != null) {
                AsyncImage(
                    model = stadium.artwork.small(true),
                    contentDescription = stadium.name.es,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(5.dp)),
                )
            } else {
                Text("Estadio", color = TcgColors.Parchment.copy(alpha = 0.4f), fontSize = 8.sp)
            }
        }
    }
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

/**
 * Acabar el turno como TEXTO en el rail derecho (estilo TCG Live real, §1.6),
 * con doble chevron y realce dorado cuando está disponible.
 */
@Composable
fun EndTurnHex(enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tint = if (enabled) BattleTheme.Gold else BattleTheme.Gold.copy(alpha = 0.4f)
    Column(
        modifier = modifier
            .width(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ACABAR", color = tint, fontWeight = FontWeight.Black, fontSize = 10.sp, textAlign = TextAlign.Center)
        Text("EL TURNO", color = tint, fontWeight = FontWeight.Black, fontSize = 10.sp, textAlign = TextAlign.Center)
        Text("»", color = tint, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}
