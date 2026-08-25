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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.core.designsystem.TypeEmblem
import com.mineralord.tcg.engine.model.EnergyType

/** Tipos ofrecidos y su etiqueta ES (concordando con "Energía"), en el orden del popup. */
private val AUTO_TYPES: List<Pair<EnergyType, String>> = listOf(
    EnergyType.GRASS to "Planta",
    EnergyType.FIRE to "Fuego",
    EnergyType.WATER to "Agua",
    EnergyType.LIGHTNING to "Rayo",
    EnergyType.PSYCHIC to "Psíquica",
    EnergyType.FIGHTING to "Lucha",
    EnergyType.DARKNESS to "Oscura",
    EnergyType.METAL to "Metálica",
    EnergyType.DRAGON to "Dragón",
    EnergyType.COLORLESS to "Incolora",
)

private const val MAX_TYPES = 2

/**
 * Popup "Autocreación" (réplica de TCG Live): elige hasta dos tipos de energía y el
 * mazo se rellena hasta 60 con energías básicas de esos tipos. Rejilla 2-col de
 * emblema + nombre; pie Cancelar / Vale (Vale deshabilitado sin selección).
 */
@Composable
internal fun AutoBuildDialog(
    onConfirm: (Set<EnergyType>) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember { mutableStateOf<Set<EnergyType>>(emptySet()) }

    Box(
        modifier = Modifier.fillMaxSize().background(BarajasPalette.Scrim).clickable(onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(BarajasPalette.BgBottom)
                .clickable(enabled = false) {}       // consume toques del diálogo
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Autocreación", color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 19.sp)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth(0.5f).height(1.dp).background(BarajasPalette.HairlineBorder),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Selecciona hasta dos tipos.",
                color = BarajasPalette.DeckName,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))

            // Rejilla 2 columnas.
            AUTO_TYPES.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { (type, label) ->
                        val isSel = type in selected
                        TypePill(
                            type = type,
                            label = label,
                            selected = isSel,
                            modifier = Modifier.weight(1f),
                        ) {
                            selected = when {
                                isSel -> selected - type
                                selected.size < MAX_TYPES -> selected + type
                                else -> selected // ya hay dos: ignora
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DialogButton(
                    text = "Cancelar",
                    textColor = BarajasPalette.Muted,
                    borderColor = BarajasPalette.HairlineBorder,
                    modifier = Modifier.weight(1f),
                    onClick = onCancel,
                )
                DialogButton(
                    text = "Vale",
                    textColor = Color.White,
                    borderColor = Color.Transparent,
                    filled = true,
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    onClick = { onConfirm(selected) },
                )
            }
        }
    }
}

@Composable
private fun TypePill(
    type: EnergyType,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(BarajasPalette.Surface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) BarajasPalette.NavIcon else BarajasPalette.HairlineBorder,
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeEmblem(type = type, size = 26.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = if (selected) BarajasPalette.NavIcon else BarajasPalette.Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}
