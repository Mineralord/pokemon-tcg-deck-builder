package com.mineralord.tcg.feature.packs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Catálogo de apertura de sobres al estilo Pokémon TCG Pocket: primero se elige una SERIE, luego una
 * EXPANSIÓN de esa serie y por último se abre su sobre. Por ahora la app solo trae la serie
 * «Escarlata y Púrpura» con la expansión «151»; el catálogo es una lista para crecer sin tocar la UI.
 */
data class ExpansionUi(
    val code: String,          // prefijo de set ("sv3pt5")
    val name: String,          // nombre corto ("151")
    val logoRes: Int?,         // logo empaquetado (null = solo texto)
    val packArtUrl: String,    // arte real del sobre
    val accent: Color,         // color de acento de la expansión
)

data class SeriesUi(
    val id: String,
    val title: String,         // "Escarlata y Púrpura"
    val subtitle: String,      // "Serie SV"
    val accent: Color,
    val expansions: List<ExpansionUi>,
)

/** Catálogo raíz (extensible). Hoy: 1 serie · 1 expansión. */
val PACK_CATALOG: List<SeriesUi> = listOf(
    SeriesUi(
        id = "sv",
        title = "Escarlata y Púrpura",
        subtitle = "Serie SV",
        accent = Color(0xFF7E57C2),
        expansions = listOf(
            ExpansionUi(
                code = "sv3pt5",
                name = "151",
                logoRes = R.drawable.set151_logo,
                packArtUrl = PACK_IMAGE_151,
                accent = Color(0xFF7E57C2),
            ),
        ),
    ),
)

// ============================ ETAPA 1 · ELEGIR SERIE ============================

@Composable
fun SeriesSelectStage(
    series: List<SeriesUi>,
    onSelect: (SeriesUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StageHeader("ELIGE UNA SERIE", "Selecciona una serie de cartas")
        Spacer(Modifier.height(20.dp))
        series.forEach { s ->
            SeriesCard(s, onClick = { onSelect(s) })
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SeriesCard(series: SeriesUi, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(series.accent.copy(alpha = 0.95f), series.accent.copy(alpha = 0.55f), Color(0xFF0E1220)),
                ),
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text(series.subtitle.uppercase(), color = Color(0xCCFFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(series.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text("${series.expansions.size} expansión(es)", color = Color(0xCCFFFFFF), fontSize = 12.sp)
        }
        Chevron(Modifier.align(Alignment.CenterEnd))
    }
}

// ========================== ETAPA 2 · ELEGIR EXPANSIÓN ==========================

@Composable
fun ExpansionSelectStage(
    series: SeriesUi,
    remainingToday: Int,
    maxPerDay: Int,
    onBack: () -> Unit,
    onSelect: (ExpansionUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BackRow(series.title, onBack)
        Spacer(Modifier.height(6.dp))
        StageHeader("ELIGE UNA EXPANSIÓN", "Sobres restantes hoy: $remainingToday/$maxPerDay")
        Spacer(Modifier.height(20.dp))
        series.expansions.forEach { e ->
            ExpansionCard(e, onClick = { onSelect(e) })
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ExpansionCard(expansion: ExpansionUi, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(expansion.accent.copy(alpha = 0.85f), Color(0xFF0E1220)),
                ),
            )
            .clickable(onClick = onClick),
    ) {
        // Arte del sobre a la derecha (grande, sangrando).
        AsyncImage(
            model = expansion.packArtUrl,
            contentDescription = expansion.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .fillMaxWidth(0.34f)
                .aspectRatio(0.62f),
        )
        Column(
            Modifier.align(Alignment.CenterStart).padding(20.dp).fillMaxWidth(0.6f),
        ) {
            if (expansion.logoRes != null) {
                Image(
                    painter = painterResource(expansion.logoRes),
                    contentDescription = expansion.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(expansion.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("Toca para abrir un sobre", color = Color(0xE6FFFFFF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ================================ COMPARTIDOS ================================

@Composable
private fun StageHeader(title: String, subtitle: String) {
    Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(4.dp))
    Text(subtitle, color = Color(0xB3FFFFFF), fontSize = 13.sp, textAlign = TextAlign.Center)
}

@Composable
private fun BackRow(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Text("‹", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp) }
        Spacer(Modifier.width(10.dp))
        Text(title, color = Color(0xCCFFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Chevron(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF)),
        contentAlignment = Alignment.Center,
    ) { Text("›", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp) }
}
