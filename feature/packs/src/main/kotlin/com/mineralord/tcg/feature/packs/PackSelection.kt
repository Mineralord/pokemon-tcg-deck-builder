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
import androidx.compose.foundation.layout.fillMaxHeight
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
    val logoUrl: String?,      // logo oficial de la serie (wikidex); null = solo texto
    val accent: Color,
    val expansions: List<ExpansionUi>,
)

/** Logo oficial de la serie «Escarlata y Púrpura» (JCC) — Wikidex. */
const val SV_SERIES_LOGO =
    "https://images.wikidexcdn.net/mwuploads/wikidex/thumb/c/c6/latest/20230209170758/Logo_Escarlata_y_P%C3%BArpura_%28TCG%29.png/500px-Logo_Escarlata_y_P%C3%BArpura_%28TCG%29.png"

/** Arte real del sobre de «Brecha Paradójica» (sv4) — booster sellado (TCGplayer CDN). */
const val PACK_IMAGE_PARADOX =
    "https://tcgplayer-cdn.tcgplayer.com/product/512822_in_1000x1000.jpg"

/** Catálogo raíz (extensible). Hoy: 1 serie · 2 expansiones (151 · Brecha Paradójica). */
val PACK_CATALOG: List<SeriesUi> = listOf(
    SeriesUi(
        id = "sv",
        title = "Escarlata y Púrpura",
        subtitle = "Serie SV",
        logoUrl = SV_SERIES_LOGO,
        accent = Color(0xFF7E57C2),
        expansions = listOf(
            ExpansionUi(
                code = "sv3pt5",
                name = "151",
                logoRes = R.drawable.set151_logo,
                packArtUrl = PACK_IMAGE_151,
                accent = Color(0xFF7E57C2),
            ),
            ExpansionUi(
                code = "sv4",
                name = "Brecha Paradójica",
                logoRes = R.drawable.set_sv4_logo,
                packArtUrl = PACK_IMAGE_PARADOX,
                accent = Color(0xFF4DB6AC),
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
    /** Progreso (poseídas, total) por código de expansión. Cada tarjeta muestra el suyo. */
    progressFor: (String) -> Pair<Int, Int>,
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
        // Logo oficial de la serie (Escarlata y Púrpura) sobre el título.
        if (series.logoUrl != null) {
            AsyncImage(
                model = series.logoUrl,
                contentDescription = series.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.72f).height(70.dp),
            )
            Spacer(Modifier.height(10.dp))
        }
        Text("Elige una expansión", color = Color(0xFF1B2A3A), fontWeight = FontWeight.Black, fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))
        series.expansions.forEach { e ->
            val (owned, total) = progressFor(e.code)
            PocketExpansionCard(e, owned, total, onClick = { onSelect(e) })
            Spacer(Modifier.height(18.dp))
        }
        Spacer(Modifier.weight(1f))
        // Pestañas de SERIE (aquí irán las series que vayamos agregando). La activa: el nombre completo.
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFFFFFFF))
                .border(1.dp, Color(0x22000000), RoundedCornerShape(50))
                .padding(horizontal = 22.dp, vertical = 8.dp),
        ) { Text(series.title, color = Color(0xFF1B2A3A), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Spacer(Modifier.height(18.dp))
    }
}

/** Panel de expansión estilo TCG Pocket: progreso "X/total" + chevron y el LOGO de la expansión. */
@Composable
private fun PocketExpansionCard(expansion: ExpansionUi, owned: Int, total: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(0.94f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFFFFF))
            .border(1.dp, Color(0x14000000), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Progreso de colección: "◆ owned/total" en una píldora clara.
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFEFF3F8)).padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("◆", color = Color(0xFF5AA9E6), fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text("$owned/$total", color = Color(0xFF33475B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("›", color = Color(0xFF8A98A6), fontWeight = FontWeight.Black, fontSize = 26.sp)
        }
        Spacer(Modifier.height(12.dp))
        // LOGO de la expansión (151) como protagonista de la tarjeta (en vez del sobre).
        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            if (expansion.logoRes != null) {
                Image(
                    painter = painterResource(expansion.logoRes),
                    contentDescription = expansion.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.72f).fillMaxHeight(),
                )
            } else {
                Text(expansion.name, color = Color(0xFF1B2A3A), fontWeight = FontWeight.Black, fontSize = 40.sp)
            }
        }
    }
}

