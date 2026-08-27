package com.mineralord.tcg.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.data.profile.CurrencyKind

/**
 * Pantalla de Inicio — chasis de la nueva Home (estilo claro TCG Pocket),
 * replicada 1:1 de la captura de referencia (1080×2400).
 *
 * Todo el arte de marca (avatar, sobres, hero) se representa con placeholders
 * estilizados. Los iconos de monedas y de botones se dibujan como arte vectorial
 * (Canvas) idéntico al de la referencia. Es "solo el chasis": la lógica de cada
 * acción se cablea en fases posteriores.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    balances: Map<CurrencyKind, Int> = emptyMap(),
    avatarColors: List<Color>? = null,
    onCartadex: () -> Unit,
    onTienda: () -> Unit,
    onBarajas: () -> Unit,
    onPerfil: () -> Unit,
    onJugar: () -> Unit,
    onJugarOnline: () -> Unit,
    onAmigos: () -> Unit = {},
    onOpenPack: () -> Unit = {},
) {
    var info by remember { mutableStateOf<Currency?>(null) }

    // Escaparate de sobres: 3 huecos que se re-sortean en cada arranque en frío (nuevo proceso).
    // Hoy solo existe la expansión 151, así que los 3 la muestran; al añadir expansiones, variará.
    val showcase = remember { List(3) { HomeExpansions.ALL.random() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HomePalette.BgTop, HomePalette.BgBottom))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Separa los contadores del notch/recorte de cámara.
            Spacer(Modifier.height(18.dp))
            CurrencyBar(balances = balances, onCurrency = { info = it })
            Spacer(Modifier.height(16.dp))
            AccessRow(onPerfil = onPerfil)
            Spacer(Modifier.height(20.dp))
            PackCarousel(showcase = showcase, onOpen = onOpenPack)
            Spacer(Modifier.height(12.dp))
            ProgressStrip()
            Spacer(Modifier.height(18.dp))
            ActionCards(onMercado = onTienda, onTienda = onTienda)
        }

        // FAB Misiones (flotante, abajo-derecha). La barra de navegación es compartida
        // (vive en el AppShell, siempre presente en las pantallas principales).
        MisionesFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 20.dp),
            onClick = onCartadex,
        )
    }

    info?.let { CurrencyInfoDialog(currency = it, onDismiss = { info = null }) }
}

// ===========================================================================
//  PALETA LOCAL (muestreada de la captura de referencia)
// ===========================================================================

private object HomePalette {
    val BgTop = Color(0xFFEDF4FB)
    val BgBottom = Color(0xFFE8F1F9)
    val Surface = Color(0xFFFFFFFF)
    val Ink = BarajasPalette.Ink
    val Muted = BarajasPalette.Muted
    val Hairline = Color(0xFFDCE6EF)
    val PlusBtn = Color(0xFFE3EEF8)
    val PlusIcon = Color(0xFF8FA6BC)
    val NavActive = Color(0xFF2C4160)
    val NavInactive = Color(0xFFAEBACB)
    val RedDot = Color(0xFFFF3B6B)
    val Teal = Color(0xFF07D4C1)

    // Monedas.
    val CristalHi = Color(0xFF8FD3FF)
    val CristalMid = Color(0xFF7C7BF0)
    val CristalLo = Color(0xFF6A4FD0)
    val MonedaHi = Color(0xFFFFE07A)
    val MonedaLo = Color(0xFFF2A518)
    val MonedaRing = Color(0xFFB9791A)
    val FichaHi = Color(0xFFC79BF2)
    val FichaLo = Color(0xFF9B5DE0)
    val CreditoHi = Color(0xFF57E0CE)
    val CreditoLo = Color(0xFF2FB8A6)
}

// ===========================================================================
//  BARRA DE MONEDAS
// ===========================================================================

@Composable
private fun CurrencyBar(balances: Map<CurrencyKind, Int>, onCurrency: (Currency) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CurrencyPill(Currency.CRISTALES, balances[CurrencyKind.CRISTALES] ?: 0, Modifier.weight(1f), onCurrency)
        CurrencyPill(Currency.MONEDAS, balances[CurrencyKind.MONEDAS] ?: 0, Modifier.weight(1f), onCurrency)
        CurrencyPill(Currency.FICHAS, balances[CurrencyKind.FICHAS] ?: 0, Modifier.weight(1f), onCurrency)
        CurrencyPill(Currency.CREDITOS, balances[CurrencyKind.CREDITOS] ?: 0, Modifier.weight(1f), onCurrency)
    }
}

@Composable
private fun CurrencyPill(
    currency: Currency,
    value: Int,
    modifier: Modifier = Modifier,
    onCurrency: (Currency) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(HomePalette.Surface)
            .border(1.dp, HomePalette.Hairline, RoundedCornerShape(50))
            .clickable { onCurrency(currency) }
            .semantics { contentDescription = "${currency.displayName}: $value. ${currency.purpose}" }
            .padding(start = 4.dp, end = 3.dp),
    ) {
        CurrencyIcon(currency, Modifier.size(22.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            formatThousands(value),
            color = HomePalette.Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(3.dp))
        PlusButton()
    }
}

@Composable
private fun PlusButton() {
    Box(
        Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(HomePalette.PlusBtn),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(11.dp)) {
            val s = size.minDimension
            val w = s * 0.16f
            drawLine(HomePalette.PlusIcon, Offset(s / 2, s * 0.12f), Offset(s / 2, s * 0.88f), w, StrokeCap.Round)
            drawLine(HomePalette.PlusIcon, Offset(s * 0.12f, s / 2), Offset(s * 0.88f, s / 2), w, StrokeCap.Round)
        }
    }
}

/** Arte vectorial de cada moneda, idéntico a la referencia. */
@Composable
private fun CurrencyIcon(currency: Currency, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (currency) {
            Currency.CRISTALES -> drawCrystal()
            Currency.MONEDAS -> drawPokeCoin()
            Currency.FICHAS -> drawTokenChip()
            Currency.CREDITOS -> drawCreditCard()
        }
    }
}

/** Gema facetada violeta-azul (Cristales). */
private fun DrawScope.drawCrystal() {
    val w = size.width; val h = size.height
    val top = h * 0.10f; val belt = h * 0.42f; val bot = h * 0.92f
    val lx = w * 0.14f; val rx = w * 0.86f; val cx = w * 0.5f
    val body = Path().apply {
        moveTo(cx, top)
        lineTo(rx, belt)
        lineTo(cx, bot)
        lineTo(lx, belt)
        close()
    }
    drawPath(body, Brush.verticalGradient(listOf(HomePalette.CristalHi, HomePalette.CristalMid, HomePalette.CristalLo)))
    // Facetas de brillo.
    val faceL = Path().apply {
        moveTo(cx, top); lineTo(lx, belt); lineTo(cx, belt); close()
    }
    drawPath(faceL, color = Color.White.copy(alpha = 0.35f))
    val faceBot = Path().apply {
        moveTo(lx, belt); lineTo(cx, belt); lineTo(cx, bot); close()
    }
    drawPath(faceBot, color = Color.Black.copy(alpha = 0.10f))
    drawPath(body, color = HomePalette.CristalLo.copy(alpha = 0.6f), style = Stroke(width = w * 0.03f, join = StrokeJoin.Round))
}

/** Moneda dorada con anillo tipo Poké (Monedas). */
private fun DrawScope.drawPokeCoin() {
    val c = center; val r = size.minDimension * 0.44f
    drawCircle(Brush.verticalGradient(listOf(HomePalette.MonedaHi, HomePalette.MonedaLo)), r, c)
    drawCircle(HomePalette.MonedaRing, r, c, style = Stroke(size.minDimension * 0.08f))
    // Línea ecuatorial + botón central (motivo Poké estilizado).
    drawLine(HomePalette.MonedaRing, Offset(c.x - r, c.y), Offset(c.x + r, c.y), size.minDimension * 0.07f)
    drawCircle(HomePalette.MonedaHi, r * 0.34f, c)
    drawCircle(HomePalette.MonedaRing, r * 0.34f, c, style = Stroke(size.minDimension * 0.06f))
    // Brillo.
    drawCircle(Color.White.copy(alpha = 0.5f), r * 0.18f, Offset(c.x - r * 0.4f, c.y - r * 0.45f))
}

/** Token hexagonal violeta con estrella (Fichas). */
private fun DrawScope.drawTokenChip() {
    val w = size.width; val h = size.height; val cx = w / 2; val cy = h / 2
    val r = size.minDimension * 0.46f
    val hexa = Path()
    for (i in 0 until 6) {
        val a = Math.toRadians((60.0 * i - 90.0))
        val x = cx + r * kotlin.math.cos(a).toFloat()
        val y = cy + r * kotlin.math.sin(a).toFloat()
        if (i == 0) hexa.moveTo(x, y) else hexa.lineTo(x, y)
    }
    hexa.close()
    drawPath(hexa, Brush.verticalGradient(listOf(HomePalette.FichaHi, HomePalette.FichaLo)))
    drawPath(hexa, Color.White.copy(alpha = 0.55f), style = Stroke(width = w * 0.06f, join = StrokeJoin.Round))
    // Estrella central.
    val star = Path()
    val sr = r * 0.55f; val ir = sr * 0.45f
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) sr else ir
        val a = Math.toRadians((36.0 * i - 90.0))
        val x = cx + rad * kotlin.math.cos(a).toFloat()
        val y = cy + rad * kotlin.math.sin(a).toFloat()
        if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
    }
    star.close()
    drawPath(star, Color.White)
}

/** Tarjeta de crédito teal (Créditos). */
private fun DrawScope.drawCreditCard() {
    val w = size.width; val h = size.height
    val cardW = w * 0.82f; val cardH = h * 0.58f
    val left = (w - cardW) / 2; val top = (h - cardH) / 2
    rotate(degrees = -8f, pivot = center) {
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(HomePalette.CreditoHi, HomePalette.CreditoLo)),
            topLeft = Offset(left, top),
            size = Size(cardW, cardH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.10f),
        )
        // Banda magnética.
        drawRect(
            Color(0xFF1B8577),
            topLeft = Offset(left, top + cardH * 0.24f),
            size = Size(cardW, cardH * 0.20f),
        )
        // Chip.
        drawRoundRect(
            Color(0xFFFFE9A8),
            topLeft = Offset(left + cardW * 0.12f, top + cardH * 0.60f),
            size = Size(cardW * 0.22f, cardH * 0.22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.02f),
        )
    }
}

// ===========================================================================
//  FILA DE ACCESO (tiles + avatar + botones)
// ===========================================================================

@Composable
private fun AccessRow(onPerfil: () -> Unit) {
    // Avatar CENTRADO en la pantalla; a su derecha, correo y regalos (outline).
    // El costado izquierdo queda libre a propósito (uso futuro).
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        AvatarBadge(avatarColors = null, level = 38, onClick = onPerfil)

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(dot = true) { MailIcon() }
            Spacer(Modifier.width(8.dp))
            CircleButton(dot = true) { GiftIcon() }
        }
    }
}

@Composable
private fun AvatarBadge(avatarColors: List<Color>?, level: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            // Anillo bicolor rojo/teal.
            Box(
                Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(HomePalette.RedDot, HomePalette.Teal, HomePalette.RedDot)))
                    .padding(3.dp)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                val bg = avatarColors?.takeIf { it.isNotEmpty() }?.let { Brush.verticalGradient(it) }
                    ?: Brush.verticalGradient(listOf(Color(0xFFEAF0F6), Color(0xFFC6D2DE)))
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(bg)
                        .border(2.dp, Color.White, CircleShape),
                )
            }
            Box(Modifier.size(12.dp).clip(CircleShape).background(HomePalette.RedDot).border(2.dp, Color.White, CircleShape))
        }
        Spacer(Modifier.height(2.dp))
        Text("Nv. $level", color = HomePalette.Muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun CircleButton(dot: Boolean, content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(HomePalette.Surface)
                .border(1.dp, HomePalette.Hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) { content() }
        if (dot) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(HomePalette.RedDot).border(1.5.dp, Color.White, CircleShape))
        }
    }
}

/** Icono de correo: sobre lineal. */
@Composable
private fun MailIcon() {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width; val h = size.height
        val st = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val body = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    w * 0.10f, h * 0.24f, w * 0.90f, h * 0.76f,
                    androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
                ),
            )
        }
        drawPath(body, HomePalette.NavActive, style = st)
        val flap = Path().apply {
            moveTo(w * 0.12f, h * 0.28f)
            lineTo(w * 0.5f, h * 0.55f)
            lineTo(w * 0.88f, h * 0.28f)
        }
        drawPath(flap, HomePalette.NavActive, style = st)
    }
}

/** Icono de regalos: caja con lazo, solo contorno (sin relleno de color). */
@Composable
private fun GiftIcon() {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width; val h = size.height
        val c = HomePalette.NavActive
        val st = Stroke(width = w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Caja.
        drawRoundRect(
            c,
            topLeft = Offset(w * 0.18f, h * 0.42f),
            size = Size(w * 0.64f, h * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
            style = st,
        )
        // Tapa.
        drawRoundRect(
            c,
            topLeft = Offset(w * 0.12f, h * 0.28f),
            size = Size(w * 0.76f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f),
            style = st,
        )
        // Cinta vertical + lazo.
        drawLine(c, Offset(w * 0.5f, h * 0.30f), Offset(w * 0.5f, h * 0.82f), w * 0.07f)
        drawLine(c, Offset(w * 0.5f, h * 0.28f), Offset(w * 0.38f, h * 0.18f), st.width, StrokeCap.Round)
        drawLine(c, Offset(w * 0.5f, h * 0.28f), Offset(w * 0.62f, h * 0.18f), st.width, StrokeCap.Round)
    }
}

// ===========================================================================
//  CARRUSEL DE SOBRES
// ===========================================================================

/** Una expansión mostrable en el escaparate de la Home (por ahora solo la 151). */
private data class HomeExpansion(val artUrl: String)

private object HomeExpansions {
    val ALL: List<HomeExpansion> = listOf(
        HomeExpansion(com.mineralord.tcg.feature.packs.PACK_IMAGE_151),
        // Al agregar futuras expansiones, añádelas aquí y el escaparate las rotará.
    )
}

@Composable
private fun PackCarousel(showcase: List<HomeExpansion>, onOpen: () -> Unit) {
    // Proporciones calcadas de la referencia: panel verde ~150dp con los sobres
    // (~196dp de alto) sobresaliendo por arriba y por abajo.
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(210.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Panel verde de fondo.
        Box(
            Modifier
                .fillMaxWidth()
                .height(154.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFA6E9C8), Color(0xFF8FE0C7)))),
        )
        // Fila de sobres reales, sobresaliendo del panel.
        Row(
            Modifier
                .fillMaxWidth()
                .height(196.dp)
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            showcase.forEach { exp ->
                BoosterArt(exp, Modifier.weight(1f), onOpen)
            }
        }
        // Píldora ">" con icono de sobre (arriba-derecha).
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable(onClick = onOpen)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(16.dp)) {
                val w = size.width; val h = size.height
                drawRoundRect(
                    Color(0xFF6BC6F5),
                    topLeft = Offset(w * 0.2f, h * 0.1f),
                    size = Size(w * 0.6f, h * 0.8f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f),
                )
            }
            Spacer(Modifier.width(4.dp))
            Canvas(Modifier.size(9.dp)) {
                val s = size.minDimension
                val p = Path().apply {
                    moveTo(s * 0.35f, s * 0.15f); lineTo(s * 0.7f, s * 0.5f); lineTo(s * 0.35f, s * 0.85f)
                }
                drawPath(p, HomePalette.Muted, style = Stroke(width = s * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
private fun BoosterArt(exp: HomeExpansion, modifier: Modifier, onOpen: () -> Unit) {
    Box(
        modifier
            .fillMaxHeight()
            .clickable(onClick = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        coil.compose.AsyncImage(
            model = exp.artUrl,
            contentDescription = "Sobre de la expansión 151",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
    }
}

// ===========================================================================
//  BANDA DE PROGRESO ("Al máximo" + contador)
// ===========================================================================

@Composable
private fun ProgressStrip() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(HomePalette.Surface)
                .border(1.dp, HomePalette.Hairline, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFF2C94C)))
            Spacer(Modifier.width(6.dp))
            repeat(3) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(HomePalette.Muted.copy(alpha = 0.4f)))
                Spacer(Modifier.width(4.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(HomePalette.Surface)
                .border(1.dp, HomePalette.Hairline, RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFFFFE08A)))
            Spacer(Modifier.width(4.dp))
            Text("40", color = HomePalette.Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(6.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .offset(x = (-70).dp, y = (-16).dp)
                .clip(RoundedCornerShape(50))
                .background(HomePalette.Teal)
                .padding(horizontal = 10.dp, vertical = 2.dp),
        ) { Text("Al máximo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
    }
}

// ===========================================================================
//  CARDS DE ACCIÓN (Mercado / Tienda)
// ===========================================================================

@Composable
private fun ActionCards(onMercado: () -> Unit, onTienda: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Mercado.
        ActionCard(
            title = "Mercado",
            bg = Brush.verticalGradient(listOf(Color(0xFFFFE7A6), Color(0xFFFFF2CF))),
            modifier = Modifier.weight(1f),
            onClick = onMercado,
        ) { RecycleIcon() }

        // Tienda (con burbuja de regalos + punto rojo).
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopEnd) {
            ActionCard(
                title = "Tienda",
                bg = Brush.verticalGradient(listOf(Color(0xFFF3ECFB), Color(0xFFEAF2FB))),
                modifier = Modifier.fillMaxWidth(),
                onClick = onTienda,
            ) { BagIcon() }
            Row(
                Modifier
                    .offset(y = (-6).dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, HomePalette.Hairline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Regalos diarios\ndisponibles",
                    color = HomePalette.Muted,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(4.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(HomePalette.RedDot))
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    bg: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, HomePalette.Hairline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { icon() }
        Text(title, color = HomePalette.Ink.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** Icono de reciclaje (Mercado): dos flechas circulares sobre una carta. */
@Composable
private fun RecycleIcon() {
    Canvas(Modifier.size(56.dp)) {
        val w = size.width; val h = size.height
        val gold = Color(0xFFF0A81C)
        // Carta.
        drawRoundRect(
            gold,
            topLeft = Offset(w * 0.32f, h * 0.3f),
            size = Size(w * 0.36f, h * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
            style = Stroke(width = w * 0.06f),
        )
        // Flechas circulares.
        val st = Stroke(width = w * 0.06f, cap = StrokeCap.Round)
        drawArc(gold, 200f, 130f, false, topLeft = Offset(w * 0.16f, h * 0.16f), size = Size(w * 0.68f, h * 0.68f), style = st)
        drawArc(gold, 20f, 130f, false, topLeft = Offset(w * 0.16f, h * 0.16f), size = Size(w * 0.68f, h * 0.68f), style = st)
    }
}

/** Icono de bolsa (Tienda). */
@Composable
private fun BagIcon() {
    Canvas(Modifier.size(52.dp)) {
        val w = size.width; val h = size.height
        val c = Color(0xFFB9C3D8)
        val st = Stroke(width = w * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val body = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    w * 0.22f, h * 0.38f, w * 0.78f, h * 0.86f,
                    androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
                ),
            )
        }
        drawPath(body, c, style = st)
        // Asa.
        drawArc(c, 180f, 180f, false, topLeft = Offset(w * 0.34f, h * 0.24f), size = Size(w * 0.32f, h * 0.32f), style = st)
    }
}

// ===========================================================================
//  FAB MISIONES
// ===========================================================================

@Composable
private fun MisionesFab(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2AE6D2), HomePalette.Teal)))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(26.dp)) {
                    val w = size.width; val h = size.height
                    val st = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    // Portapapeles.
                    drawRoundRect(
                        Color.White,
                        topLeft = Offset(w * 0.2f, h * 0.16f),
                        size = Size(w * 0.6f, h * 0.72f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                        style = st,
                    )
                    // Check.
                    val chk = Path().apply {
                        moveTo(w * 0.36f, h * 0.52f); lineTo(w * 0.47f, h * 0.64f); lineTo(w * 0.66f, h * 0.4f)
                    }
                    drawPath(chk, Color.White, style = st)
                }
            }
            Box(Modifier.size(14.dp).clip(CircleShape).background(HomePalette.RedDot).border(2.dp, Color.White, CircleShape))
        }
        Spacer(Modifier.height(2.dp))
        Text("Misiones", color = HomePalette.Muted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

// ===========================================================================
//  BARRA DE NAVEGACIÓN INFERIOR
// ===========================================================================

/**
 * Barra de navegación inferior COMPARTIDA por las pantallas principales
 * (Inicio · Cartas · Amigos · Logros · Menú). Vive en el [AppShell] y está
 * siempre presente. [current] es el índice de la página activa (0..3); "Menú"
 * no es una página sino un disparador de popup ([onMenu]).
 */
@Composable
fun MainBottomNav(
    current: Int,
    onSelect: (Int) -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Color(0xFFF6F8FB))
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem("Inicio", active = current == 0, onClick = { onSelect(0) }) { NavHomeIcon(it) }
        NavItem("Cartas", active = current == 1, onClick = { onSelect(1) }) { NavCartasIcon(it) }
        NavItem("Amigos", active = current == 2, onClick = { onSelect(2) }, dot = true) { NavAmigosIcon(it) }
        NavItem("Partidas", active = current == 3, onClick = { onSelect(3) }, dot = true) { NavPartidasIcon(it) }
        NavItem("Menú", active = false, onClick = onMenu) { NavMenuIcon(it) }
    }
}

@Composable
private fun NavItem(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    dot: Boolean = false,
    icon: @Composable (Color) -> Unit,
) {
    val tint = if (active) HomePalette.NavActive else HomePalette.NavInactive
    // Sin etiquetas de texto: solo el icono (con su punto rojo de aviso si aplica).
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) { icon(tint) }
        if (dot) Box(Modifier.size(8.dp).clip(CircleShape).background(HomePalette.RedDot))
    }
}

@Composable
private fun NavHomeIcon(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height
        val roof = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.9f, h * 0.5f)
            lineTo(w * 0.1f, h * 0.5f)
            close()
        }
        drawPath(roof, tint)
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.22f, h * 0.48f),
            size = Size(w * 0.56f, h * 0.38f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f),
        )
    }
}

@Composable
private fun NavCartasIcon(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height
        val st = Stroke(width = w * 0.08f, join = StrokeJoin.Round)
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.28f, h * 0.16f),
            size = Size(w * 0.44f, h * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
            style = st,
        )
        drawLine(tint, Offset(w * 0.5f, h * 0.3f), Offset(w * 0.5f, h * 0.7f), w * 0.06f, StrokeCap.Round)
    }
}

@Composable
private fun NavAmigosIcon(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height
        drawCircle(tint, w * 0.11f, Offset(w * 0.34f, h * 0.36f))
        drawCircle(tint, w * 0.11f, Offset(w * 0.66f, h * 0.36f))
        val st = Stroke(width = w * 0.07f, cap = StrokeCap.Round)
        drawArc(tint, 180f, 180f, false, topLeft = Offset(w * 0.18f, h * 0.5f), size = Size(w * 0.32f, h * 0.34f), style = st)
        drawArc(tint, 180f, 180f, false, topLeft = Offset(w * 0.5f, h * 0.5f), size = Size(w * 0.32f, h * 0.34f), style = st)
    }
}

@Composable
private fun NavPartidasIcon(tint: Color) {
    // Icono de batalla (Partidas): dos cartas superpuestas sobre un estallido.
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height
        // Estallido detrás.
        val burst = Path()
        val cx = w * 0.56f; val cy = h * 0.44f; val r = w * 0.34f
        for (i in 0 until 16) {
            val rad = if (i % 2 == 0) r else r * 0.55f
            val a = Math.toRadians((360.0 / 16 * i - 90.0))
            val x = cx + rad * kotlin.math.cos(a).toFloat()
            val y = cy + rad * kotlin.math.sin(a).toFloat()
            if (i == 0) burst.moveTo(x, y) else burst.lineTo(x, y)
        }
        burst.close()
        drawPath(burst, tint)
        // Dos cartas (rellenas del fondo con borde tinta) superpuestas.
        val st = Stroke(width = w * 0.06f, join = StrokeJoin.Round)
        fun card(ox: Float, oy: Float) {
            val tl = Offset(ox, oy); val sz = Size(w * 0.26f, h * 0.34f)
            drawRoundRect(Color(0xFFF6F8FB), topLeft = tl, size = sz, cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
            drawRoundRect(tint, topLeft = tl, size = sz, cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f), style = st)
        }
        card(w * 0.12f, h * 0.40f)
        card(w * 0.46f, h * 0.20f)
    }
}

@Composable
private fun NavMenuIcon(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height
        val st = w * 0.08f
        drawLine(tint, Offset(w * 0.2f, h * 0.35f), Offset(w * 0.8f, h * 0.35f), st, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), st, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.2f, h * 0.65f), Offset(w * 0.8f, h * 0.65f), st, StrokeCap.Round)
    }
}

// ===========================================================================
//  UTILIDADES / MODELO ECONÓMICO (canon)
// ===========================================================================

private fun formatThousands(v: Int): String {
    val s = v.toString()
    val sb = StringBuilder()
    var c = 0
    for (i in s.indices.reversed()) {
        sb.append(s[i]); c++
        if (c % 3 == 0 && i != 0) sb.append('.')
    }
    return sb.reverse().toString()
}

/** Ficha informativa de un recurso (Fase 2, Cap. 6). */
@Composable
private fun CurrencyInfoDialog(currency: Currency, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(HomePalette.Surface)
                .border(1.dp, HomePalette.Hairline, RoundedCornerShape(18.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CurrencyIcon(currency, Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text(currency.displayName, color = HomePalette.Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Spacer(Modifier.height(14.dp))
            InfoRow("Para qué sirve", currency.purpose)
            Spacer(Modifier.height(10.dp))
            InfoRow("Cómo se obtiene", currency.howObtained)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(50))
                    .background(HomePalette.Teal)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) { Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = HomePalette.Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(3.dp))
        Text(value, color = HomePalette.Ink, fontSize = 14.sp, lineHeight = 18.sp)
    }
}

/**
 * Los cuatro recursos económicos oficiales (Fase 2 — Economía Principal, Cap. 6).
 * Orden en la barra superior: Cristales, Monedas, Fichas, Créditos.
 */
enum class Currency(
    val kind: CurrencyKind,
    val displayName: String,
    val purpose: String,
    val howObtained: String,
    val dot: Color,
) {
    CRISTALES(
        CurrencyKind.CRISTALES, "Cristales",
        "Adquisición de sobres de cartas.",
        "Ganando partidas PvE y PvP, en eventos y al completar logros.",
        HomePalette.CristalMid,
    ),
    MONEDAS(
        CurrencyKind.MONEDAS, "Monedas",
        "Obtención de elementos cosméticos y recompensas de prestigio.",
        "Jugando partidas, con logros y recompensas de temporada.",
        HomePalette.MonedaLo,
    ),
    FICHAS(
        CurrencyKind.FICHAS, "Fichas",
        "Fabricación de cartas mediante el sistema de crafting.",
        "Reciclando cartas repetidas y con logros y eventos.",
        HomePalette.FichaLo,
    ),
    CREDITOS(
        CurrencyKind.CREDITOS, "Créditos",
        "Funcionamiento del mercado y las actividades comerciales.",
        "Con la actividad del mercado, logros y recompensas de temporada.",
        HomePalette.CreditoLo,
    ),
}
