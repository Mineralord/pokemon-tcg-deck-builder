package com.mineralord.tcg.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mineralord.tcg.engine.model.MatchFormat

/**
 * Selector del "Límite de cartas" (formato de la partida). Expandido = todas las series/expansiones;
 * Estándar = una sola serie (era) del catálogo. Es puro UI: recibe el formato actual, la lista de
 * series disponibles y notifica la elección — no conoce persistencia ni red.
 */
@Composable
fun FormatPickerDialog(
    current: MatchFormat,
    series: List<String>,
    onPick: (MatchFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState())) {
                Text("Límite de cartas", color = Color(0xFF2B3346), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Expandido: juega con TODAS las series. Estándar: solo cartas de una misma serie.",
                    color = Color(0xFF8A93A6), fontSize = 12.sp, lineHeight = 16.sp,
                )
                Spacer(Modifier.height(14.dp))

                FormatOption(
                    title = "Expandido",
                    subtitle = "Todas las expansiones y series",
                    selected = current is MatchFormat.Expanded,
                ) { onPick(MatchFormat.Expanded) }

                series.forEach { s ->
                    Spacer(Modifier.height(8.dp))
                    FormatOption(
                        title = "Estándar · $s",
                        subtitle = "Solo cartas de $s",
                        selected = current is MatchFormat.Standard && current.series == s,
                    ) { onPick(MatchFormat.Standard(s)) }
                }
            }
        }
    }
}

@Composable
private fun FormatOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val accent = Color(0xFF35C4E8)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.12f) else Color(0xFFF2F4F8))
            .border(1.5.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color(0xFF2B3346), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = Color(0xFF8A93A6), fontSize = 12.sp)
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Text("✓", color = accent, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}
