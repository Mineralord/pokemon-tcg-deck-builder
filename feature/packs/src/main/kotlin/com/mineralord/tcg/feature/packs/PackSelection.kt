package com.mineralord.tcg.feature.packs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

// ========================== ELEGIR EXPANSIÓN (estilo TCG Pocket) ==========================
// Réplica de "Select Expansion": fondo claro, panel blanco por expansión con LOGO + progreso de
// colección "X/total" (icono diamante) + chevron y las MINIATURAS del sobre asomando abajo; la SERIE
// como PESTAÑA inferior; y botón de cerrar (✕). Ref.: video PTCGP (at_0018).

@Composable
fun ExpansionSelectStage(
    series: SeriesUi,
    ownedInSet: Int,
    totalInSet: Int,
    onSelect: (ExpansionUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE9F1FB), Color(0xFFCFE0F2))))
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Elige una expansión", color = Color(0xFF1B2A3A), fontWeight = FontWeight.Black, fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))
        series.expansions.forEach { e ->
            PocketExpansionCard(e, ownedInSet, totalInSet, onClick = { onSelect(e) })
            Spacer(Modifier.height(18.dp))
        }
        Spacer(Modifier.weight(1f))
        // Pestaña de SERIE (única) — como "A/B Series" de TCG Pocket.
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFFFFFFF))
                .border(1.dp, Color(0x22000000), RoundedCornerShape(50))
                .padding(horizontal = 22.dp, vertical = 8.dp),
        ) { Text(series.subtitle, color = Color(0xFF1B2A3A), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Spacer(Modifier.height(18.dp))
    }
}

/** Panel de expansión estilo TCG Pocket: logo + progreso "X/total" + chevron, y el sobre asomando abajo. */
@Composable
private fun PocketExpansionCard(expansion: ExpansionUi, owned: Int, total: Int, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(0.94f)) {
        // Panel blanco (queda por debajo del sobre que asoma).
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFFFFFF))
                .border(1.dp, Color(0x14000000), RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (expansion.logoRes != null) {
                    Image(
                        painter = painterResource(expansion.logoRes),
                        contentDescription = expansion.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.weight(1f).height(44.dp),
                    )
                } else {
                    Text(expansion.name, color = Color(0xFF1B2A3A), fontWeight = FontWeight.Black, fontSize = 22.sp,
                        modifier = Modifier.weight(1f))
                }
                Text("›", color = Color(0xFF8A98A6), fontWeight = FontWeight.Black, fontSize = 26.sp)
            }
            Spacer(Modifier.height(10.dp))
            // Progreso de colección: "◆ owned/total" en una píldora clara.
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFEFF3F8)).padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("◆", color = Color(0xFF5AA9E6), fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text("$owned/$total", color = Color(0xFF33475B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(64.dp)) // hueco para que el sobre asome dentro del panel
        }
        // Sobre asomando desde el borde inferior del panel (como en TCG Pocket).
        AsyncImage(
            model = expansion.packArtUrl,
            contentDescription = expansion.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.30f)
                .aspectRatio(0.62f),
        )
    }
}

