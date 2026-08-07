package com.mineralord.tcg.feature.game.combat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.EnergySphere
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonInPlay

/**
 * Badge de PS + tipo del Pokémon, como en Pokémon TCG Live: píldora blanca con el nº de PS y el icono
 * de tipo en un disco oscuro, superpuesta en la esquina SUPERIOR-DERECHA de la carta en el visor.
 * Ref.: video "VISTA DE ESTADIO Y HERRAMIENTA" (Relicanth "100" con la esfera de tipo a la derecha).
 */
@Composable
fun HpTypeBadge(remaining: Int, type: EnergyType?, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xF2FFFFFF))
            .border(1.5.dp, Color(0x33000000), RoundedCornerShape(50))
            .padding(start = 12.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("$remaining", color = Color(0xFF15202B), fontWeight = FontWeight.Black, fontSize = 22.sp)
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(Color(0xFF1B2430)),
            contentAlignment = Alignment.Center,
        ) { EnergySphere(type = type, size = 22.dp) }
    }
}

/**
 * Panel de estadísticas del Pokémon al estilo TCG Live: hoja clara con el nombre + PS, y filas de
 * Debilidad/Resistencia, Retirada, Energía unida y Herramientas unidas (con miniatura de la Tool).
 * Se muestra bajo la carta en el visor de detalle, para CUALQUIER Pokémon en juego (propio o rival).
 */
@Composable
fun PokemonDetailSheet(pip: PokemonInPlay, modifier: Modifier = Modifier) {
    val card = pip.card
    val type = card.types.firstOrNull()
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFFF4F1EA))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Cabecera: nombre + PS actuales/máx con el icono de tipo.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(card.name.es, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Black, fontSize = 18.sp,
                modifier = Modifier.weight(1f))
            Text("${pip.remainingHp}/${card.hp}", color = Color(0xFF1A1A1A), fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            EnergySphere(type = type, size = 20.dp)
        }
        Divider()

        // Debilidad / Resistencia (en una fila, como el binder de TCG Live).
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatLabel("Debilidad", Modifier.width(96.dp))
            card.weaknesses.forEach { w -> EnergySphere(type = w.type, size = 18.dp); Spacer(Modifier.width(3.dp)); ValueText(w.value) }
            Spacer(Modifier.weight(1f))
            StatLabel("Resistencia", Modifier.width(96.dp))
            card.resistances.forEach { r -> EnergySphere(type = r.type, size = 18.dp); Spacer(Modifier.width(3.dp)); ValueText(r.value) }
        }
        // Coste de retirada.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatLabel("Retirada", Modifier.width(96.dp))
            if (card.retreatCost.isEmpty()) ValueText("—")
            else card.retreatCost.forEach { EnergySphere(type = it, size = 18.dp); Spacer(Modifier.width(2.dp)) }
        }
        Divider()

        // Energía unida (agrupada por tipo con su recuento).
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatLabel("Energía unida", Modifier.width(130.dp))
            ValueText("x${pip.attachedEnergy.size}")
            Spacer(Modifier.weight(1f))
            energyByType(pip).forEach { (t, n) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EnergySphere(type = t, size = 18.dp)
                    if (n > 1) Text("×$n", color = Color(0xFF444444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
        // Herramientas unidas (con miniatura de la carta, como en TCG Live).
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatLabel("Herramientas unidas", Modifier.width(130.dp))
            ValueText("x${pip.attachedTools.size}")
            Spacer(Modifier.weight(1f))
            pip.attachedTools.forEach { tool ->
                AsyncImage(
                    model = tool.artwork.small(true),
                    contentDescription = tool.name.es,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(26.dp)
                        .aspectRatio(1.4f)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0x33000000), RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun StatLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Color(0xFF5A5A5A), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = modifier)
}

@Composable
private fun ValueText(text: String) {
    Text(text, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1A000000)))
}

/** Recuento de Energía Básica unida agrupado por tipo (las Especiales se omiten del desglose). */
private fun energyByType(pip: PokemonInPlay): List<Pair<EnergyType, Int>> =
    pip.attachedEnergy.filterIsInstance<BasicEnergy>()
        .groupingBy { it.type }.eachCount()
        .toList().sortedByDescending { it.second }
