package com.mineralord.tcg.feature.decks

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.BarajasPalette

/**
 * Botón-píldora de los diálogos. Dos variantes fieles a TCG Live:
 * - [filled] = false: fondo blanco, borde y texto de color (neutro / destructivo).
 * - [filled] = true: relleno con degradado cian y texto blanco (acción de confirmar).
 */
@Composable
internal fun DialogButton(
    text: String,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val base = if (filled) {
        Modifier.background(Brush.horizontalGradient(BarajasPalette.SaveGradient))
    } else {
        Modifier
            .background(BarajasPalette.Surface)
            .border(1.5.dp, borderColor, RoundedCornerShape(50))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .alpha(if (enabled) 1f else 0.45f)
            .then(base)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (filled) Color.White else textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

/**
 * Diálogo genérico de confirmación de dos botones (Cancelar + acción). Fija el
 * chasis fiel a TCG Live: tarjeta blanca centrada sobre velo tenue, título, hilo
 * arcoíris, cuerpo y dos botones.
 */
@Composable
internal fun ConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    confirmFilled: Boolean,
    confirmColor: Color,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BarajasPalette.Scrim)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onCancel,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(24.dp))
                .background(BarajasPalette.Surface)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    onClick = {},
                )
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 19.sp)
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(2.dp)
                    .background(Brush.horizontalGradient(BarajasPalette.DividerGradient)),
            )
            Spacer(Modifier.height(18.dp))
            Text(body, color = BarajasPalette.DeckName, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DialogButton(
                    text = "Cancelar",
                    textColor = BarajasPalette.Muted,
                    borderColor = BarajasPalette.HairlineBorder,
                    modifier = Modifier.weight(1f),
                    onClick = onCancel,
                )
                DialogButton(
                    text = confirmText,
                    textColor = confirmColor,
                    borderColor = confirmColor,
                    filled = confirmFilled,
                    modifier = Modifier.weight(1f),
                    onClick = onConfirm,
                )
            }
        }
    }
}

/** Máximo de caracteres del nombre de una baraja (TCG Live). */
private const val MAX_DECK_NAME = 22

/**
 * #5 — "Nombre de la baraja": campo de texto (máx. 22), ayuda y aviso de privacidad.
 * "Vale" cian-relleno, deshabilitado si el nombre está vacío.
 */
@Composable
internal fun RenameDeckDialog(current: String, onCancel: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BarajasPalette.Scrim)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onCancel,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(24.dp))
                .background(BarajasPalette.Surface)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                )
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Nombre de la baraja", color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 19.sp)
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.fillMaxWidth(0.5f).height(2.dp)
                    .background(Brush.horizontalGradient(BarajasPalette.DividerGradient)),
            )
            Spacer(Modifier.height(18.dp))
            // Campo de texto (píldora clara, texto centrado).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(BarajasPalette.Hollow)
                    .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= MAX_DECK_NAME) text = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = BarajasPalette.Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(BarajasPalette.NavIcon),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Máximo $MAX_DECK_NAME caracteres.", color = BarajasPalette.DeckName, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("⚠ No utilices información personal.", color = BarajasPalette.DeleteRed,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DialogButton(
                    text = "Cancelar",
                    textColor = BarajasPalette.Muted,
                    borderColor = BarajasPalette.HairlineBorder,
                    modifier = Modifier.weight(1f),
                    onClick = onCancel,
                )
                DialogButton(
                    text = "Vale",
                    textColor = BarajasPalette.DeleteRed,
                    borderColor = BarajasPalette.DeleteRed,
                    filled = true,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    onClick = { onConfirm(text.trim()) },
                )
            }
        }
    }
}

/**
 * #6 — Menú "…" de opciones de una baraja (cabecera del editor). Tarjeta blanca
 * anclada arriba-derecha, etiquetas alineadas a la derecha con icono. Cierra al
 * tocar fuera.
 */
@Composable
internal fun DeckOptionsMenu(
    onDismiss: () -> Unit,
    onEliminar: () -> Unit,
    onVerCartas: () -> Unit,
    onMostrarCodigo: () -> Unit,
    onCopiar: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 70.dp, end = 16.dp)
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(BarajasPalette.Surface)
                .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(18.dp)),
        ) {
            DeckOptionRow("Eliminar esta baraja", "🗑", onEliminar)
            OptionDivider()
            DeckOptionRow("Ver todas las cartas de la baraja", "🃏", onVerCartas)
            OptionDivider()
            DeckOptionRow("Mostrar código", "▦", onMostrarCodigo)
            OptionDivider()
            DeckOptionRow("Copiar esta baraja", "❐", onCopiar)
        }
    }
}

@Composable
private fun OptionDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(BarajasPalette.HairlineBorder))
}

@Composable
private fun DeckOptionRow(text: String, icon: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text, color = BarajasPalette.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(icon, fontSize = 13.sp, color = BarajasPalette.Muted) }
    }
}

/** Diálogo informativo genérico (título + cuerpo + Cerrar). Reutilizable. */
@Composable
internal fun InfoDialog(title: String, body: String, onClose: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(BarajasPalette.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(24.dp))
                .background(BarajasPalette.Surface)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 18.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(body, color = BarajasPalette.DeckName, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            DialogButton(
                text = "Cerrar",
                textColor = BarajasPalette.Muted,
                borderColor = BarajasPalette.HairlineBorder,
                modifier = Modifier.fillMaxWidth(0.6f),
                onClick = onClose,
            )
        }
    }
}

/** #3 — "Salir sin guardar": botón "Vale" cian-relleno (confirmar salida). */
@Composable
internal fun ExitWithoutSaveDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    ConfirmDialog(
        title = "Salir sin guardar",
        body = "No has guardado esta baraja. Se perderán todos los cambios y la baraja " +
            "volverá a su estado previo. ¿Te parece bien?",
        confirmText = "Vale",
        confirmFilled = true,
        confirmColor = BarajasPalette.DeleteRed,
        onCancel = onCancel,
        onConfirm = onConfirm,
    )
}
