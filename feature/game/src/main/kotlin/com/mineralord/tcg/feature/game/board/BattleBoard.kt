package com.mineralord.tcg.feature.game.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.HexagonShape
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.engine.model.BasicEnergy
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
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val mid = h * 0.5f
        val band = h * 0.03f

        // Mitades base.
        drawRect(
            brush = Brush.verticalGradient(listOf(BattleTheme.OppTop, BattleTheme.OppBottom), endY = mid),
            size = androidx.compose.ui.geometry.Size(w, mid),
        )
        drawRect(
            brush = Brush.verticalGradient(listOf(BattleTheme.MineTop, BattleTheme.MineBottom), startY = mid),
            topLeft = Offset(0f, mid),
            size = androidx.compose.ui.geometry.Size(w, h - mid),
        )

        // Carril central más claro (la "calle" de activos/mazo).
        val laneL = w * 0.30f
        val laneR = w * 0.70f
        drawRect(
            color = Color(0x22FFFFFF),
            topLeft = Offset(laneL, 0f),
            size = androidx.compose.ui.geometry.Size(laneR - laneL, h),
        )

        // Panal hexagonal (líneas tenues) en todo el tapete.
        drawHoneycomb(w, h, s = w * 0.045f, color = Color(0x14FFFFFF))

        // Banda/lente dorada central.
        drawRect(
            color = BattleTheme.CenterBand.copy(alpha = 0.65f),
            topLeft = Offset(0f, mid - band),
            size = androidx.compose.ui.geometry.Size(w, band * 2f),
        )
        drawLine(BattleTheme.Gold, Offset(0f, mid - band), Offset(w, mid - band), strokeWidth = 3f)
        drawLine(BattleTheme.Gold, Offset(0f, mid + band), Offset(w, mid + band), strokeWidth = 3f)
        drawOval(
            color = BattleTheme.Gold.copy(alpha = 0.20f),
            topLeft = Offset(w * 0.32f, mid - band * 1.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.36f, band * 2.2f),
        )

        // Marco dorado redondeado del campo.
        val inset = w * 0.012f
        drawRoundRect(
            color = BattleTheme.Gold,
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(w - inset * 2f, h - inset * 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f, w * 0.05f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.013f),
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
            Box(
                Modifier.size(9.dp).clip(CircleShape)
                    .background(typeColor(energyTypeOf(e)))
                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            )
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

/** Badge de premios (estilo TCG Live, rojo). */
@Composable
fun PrizeBadge(count: Int, timer: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(7.dp)).background(TcgColors.Red),
            contentAlignment = Alignment.Center,
        ) {
            Text("$count", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(timer, color = TcgColors.Parchment.copy(alpha = 0.85f), fontSize = 9.sp)
    }
}

/** Botón hexagonal "ACABAR EL TURNO". */
@Composable
fun EndTurnHex(enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (enabled) BattleTheme.Gold else BattleTheme.Gold.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .size(86.dp, 58.dp)
            .clip(HexagonShape(flatTop = false))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "ACABAR\nEL TURNO",
            color = Color(0xFF3A2A08),
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
        )
    }
}
