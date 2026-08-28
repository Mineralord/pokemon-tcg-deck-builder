package com.mineralord.tcg.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.BarajasPalette

/**
 * Menú lateral (popup del último icono de la barra). Réplica del menú de TCG Pocket:
 * panel anclado a la derecha (~82% de ancho) que ENTRA deslizándose desde el borde derecho
 * hacia la izquierda, con el resto de la pantalla oscurecida (el desenfoque se aplica en el
 * contenedor base). Funciona sobre cualquier página principal.
 *
 * Estructura:
 *   - Fila "ID de usuario" + código + botón copiar.
 *   - Ficha del jugador: avatar (imagen, NO vectorial) + nombre + chevron + aviso rojo.
 *   - Píldora "Pase normal" (vacía, sin arte por indicación de diseño).
 *   - Accesos: Tienda·, Inventario  |  Noticias, Regalos·, Pistas para Entrenadores, Otros.
 *
 * Los puntos rojos (·) son avisos: submenú próximamente, recompensas por reclamar o acción pendiente.
 */
private object MenuPalette {
    val Panel = Color(0xFFF7FAFC)
    val Pill = Color(0xFFEDF2F7)
    val Ink = BarajasPalette.Ink
    val Icon = Color(0xFF6B7A90)
    val Muted = Color(0xFF9AA7B8)
    val Divider = Color(0xFFE4EBF2)
    val RedDot = Color(0xFFFF3B6B)
    val Scrim = Color(0x40222B38)
}

@Composable
fun MenuOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    userId: String,
    playerName: String,
    avatarColors: List<Color>?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        // Scrim: oscurece y captura el toque para cerrar.
        AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MenuPalette.Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }
        // Panel deslizante desde la derecha.
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(280)) { it },
            exit = slideOutHorizontally(tween(220)) { it },
        ) {
            MenuPanel(userId, playerName, avatarColors)
        }
    }
}

@Composable
private fun MenuPanel(userId: String, playerName: String, avatarColors: List<Color>?) {
    Column(
        Modifier
            .fillMaxWidth(0.76f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
            .background(MenuPalette.Panel)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        // ---- ID de usuario ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ID de usuario", color = MenuPalette.Muted, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MenuPalette.Pill)
                    .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    userId.ifBlank { "----------" },
                    color = MenuPalette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { CopyIcon() }
            }
        }
        Spacer(Modifier.height(14.dp))

        // ---- Ficha del jugador ----
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(MenuPalette.Pill)
                    .clickable { }
                    .padding(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar (imagen / placeholder, NO vectorial).
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                avatarColors?.takeIf { it.isNotEmpty() }
                                    ?: listOf(Color(0xFFB39DDB), Color(0xFF9575CD)),
                            ),
                        ),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    playerName.ifBlank { "Entrenador" },
                    color = MenuPalette.Ink, fontWeight = FontWeight.Black, fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { ChevronIcon() }
            }
            // Aviso rojo (esquina superior derecha).
            AlertBadge(Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp))
        }
        Spacer(Modifier.height(10.dp))

        // ---- Pase normal (píldora vacía por indicación de diseño) ----
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MenuPalette.Pill)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Pase normal", color = MenuPalette.Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(14.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        // ---- Grupo 1: Tienda / Inventario ----
        MenuRow("Tienda", dot = true) { ShopIcon() }
        MenuRow("Inventario") { BackpackIcon() }
        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        // ---- Grupo 2: Noticias / Regalos / Pistas / Otros ----
        MenuRow("Noticias") { MailIcon() }
        MenuRow("Regalos", dot = true) { GiftIcon() }
        MenuRow("Pistas para Entrenadores") { HelpIcon() }
        MenuRow("Otros") { GearIcon() }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MenuPalette.Divider))
}

@Composable
private fun MenuRow(label: String, dot: Boolean = false, icon: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            icon()
            if (dot) AlertDot(Modifier.align(Alignment.TopEnd))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, color = MenuPalette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

// ===========================================================================
//  AVISOS
// ===========================================================================

@Composable
private fun AlertDot(modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).clip(CircleShape).background(MenuPalette.RedDot))
}

@Composable
private fun AlertBadge(modifier: Modifier = Modifier) {
    Box(
        modifier.size(24.dp).clip(CircleShape).background(MenuPalette.RedDot),
        contentAlignment = Alignment.Center,
    ) {
        Text("!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

// ===========================================================================
//  ARTE VECTORIAL — iconos monocromos (outline gris azulado)
// ===========================================================================

private fun DrawScope.iconStroke(f: Float = 0.08f) =
    Stroke(width = size.width * f, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
private fun CopyIcon() {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width; val h = size.height; val c = MenuPalette.Icon
        drawRoundRect(c, topLeft = Offset(w * 0.30f, h * 0.10f), size = Size(w * 0.55f, h * 0.55f),
            cornerRadius = CornerRadius(w * 0.10f), style = iconStroke(0.10f))
        drawRoundRect(c, topLeft = Offset(w * 0.12f, h * 0.32f), size = Size(w * 0.55f, h * 0.55f),
            cornerRadius = CornerRadius(w * 0.10f), style = iconStroke(0.10f))
    }
}

@Composable
private fun ChevronIcon() {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width; val h = size.height
        val p = Path().apply {
            moveTo(w * 0.38f, h * 0.22f); lineTo(w * 0.64f, h * 0.5f); lineTo(w * 0.38f, h * 0.78f)
        }
        drawPath(p, MenuPalette.Muted, style = iconStroke(0.11f))
    }
}

@Composable
private fun ShopIcon() {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height; val c = MenuPalette.Icon
        // Cuerpo de la bolsa (trapecio invertido redondeado).
        val bag = Path().apply {
            moveTo(w * 0.22f, h * 0.34f)
            lineTo(w * 0.78f, h * 0.34f)
            lineTo(w * 0.86f, h * 0.86f)
            lineTo(w * 0.14f, h * 0.86f)
            close()
        }
        drawPath(bag, c, style = iconStroke(0.075f))
        // Asa.
        drawArc(c, 180f, 180f, false, topLeft = Offset(w * 0.34f, h * 0.14f),
            size = Size(w * 0.32f, h * 0.40f), style = iconStroke(0.075f))
    }
}

@Composable
private fun BackpackIcon() {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height; val c = MenuPalette.Icon
        // Cuerpo.
        drawRoundRect(c, topLeft = Offset(w * 0.22f, h * 0.24f), size = Size(w * 0.56f, h * 0.64f),
            cornerRadius = CornerRadius(w * 0.16f), style = iconStroke(0.07f))
        // Asa superior.
        drawArc(c, 180f, 180f, false, topLeft = Offset(w * 0.36f, h * 0.10f),
            size = Size(w * 0.28f, h * 0.24f), style = iconStroke(0.07f))
        // Bolsillo frontal.
        val pocket = Path().apply {
            moveTo(w * 0.32f, h * 0.56f)
            lineTo(w * 0.32f, h * 0.88f)
            lineTo(w * 0.68f, h * 0.88f)
            lineTo(w * 0.68f, h * 0.56f)
            arcTo(
                androidx.compose.ui.geometry.Rect(Offset(w * 0.32f, h * 0.44f), Size(w * 0.36f, h * 0.24f)),
                0f, -180f, false,
            )
            close()
        }
        drawPath(pocket, c, style = iconStroke(0.07f))
    }
}

@Composable
private fun MailIcon() {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height; val c = MenuPalette.Icon
        drawRoundRect(c, topLeft = Offset(w * 0.14f, h * 0.26f), size = Size(w * 0.72f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.08f), style = iconStroke(0.075f))
        val flap = Path().apply {
            moveTo(w * 0.18f, h * 0.32f); lineTo(w * 0.5f, h * 0.56f); lineTo(w * 0.82f, h * 0.32f)
        }
        drawPath(flap, c, style = iconStroke(0.075f))
    }
}

@Composable
private fun GiftIcon() {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height; val c = MenuPalette.Icon
        // Cuerpo.
        drawRoundRect(c, topLeft = Offset(w * 0.20f, h * 0.42f), size = Size(w * 0.60f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.06f), style = iconStroke(0.07f))
        // Tapa.
        drawRoundRect(c, topLeft = Offset(w * 0.15f, h * 0.30f), size = Size(w * 0.70f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.05f), style = iconStroke(0.07f))
        // Cinta vertical.
        drawLine(c, Offset(w * 0.5f, h * 0.30f), Offset(w * 0.5f, h * 0.86f), w * 0.07f)
        // Lazo.
        drawArc(c, 0f, -180f, false, topLeft = Offset(w * 0.28f, h * 0.16f),
            size = Size(w * 0.22f, h * 0.20f), style = iconStroke(0.07f))
        drawArc(c, 180f, -180f, false, topLeft = Offset(w * 0.50f, h * 0.16f),
            size = Size(w * 0.22f, h * 0.20f), style = iconStroke(0.07f))
    }
}

@Composable
private fun HelpIcon() {
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(24.dp)) {
            drawCircle(MenuPalette.Icon, size.width * 0.44f, style = iconStroke(0.07f))
        }
        Text("?", color = MenuPalette.Icon, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

@Composable
private fun GearIcon() {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height; val c = MenuPalette.Icon
        val cx = w * 0.5f; val cy = h * 0.5f
        // Dientes.
        for (i in 0 until 8) {
            rotate(45f * i, pivot = Offset(cx, cy)) {
                drawRoundRect(
                    c,
                    topLeft = Offset(cx - w * 0.055f, h * 0.06f),
                    size = Size(w * 0.11f, h * 0.16f),
                    cornerRadius = CornerRadius(w * 0.03f),
                )
            }
        }
        // Anillo.
        drawCircle(c, w * 0.28f, Offset(cx, cy), style = iconStroke(0.075f))
        // Agujero central (recorta con el color del panel).
        drawCircle(MenuPalette.Panel, w * 0.12f, Offset(cx, cy))
    }
}
