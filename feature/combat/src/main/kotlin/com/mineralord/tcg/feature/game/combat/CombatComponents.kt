package com.mineralord.tcg.feature.game.combat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.HoloCardImage
import com.mineralord.tcg.engine.model.Status

/**
 * Componentes atómicos y reutilizables del rebuild (capa de PRESENTACIÓN pura,
 * sin lógica de juego). Cada uno tiene responsabilidad única y se anima fácilmente
 * en el futuro (todos aceptan Modifier).
 */

/** Carta genérica: arte recortado a aspecto de carta, o dorso si [faceDown]. */
@Composable
fun CombatCard(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
    selected: Boolean = false,
    dimmed: Boolean = false,
    contentDescription: String? = null,
    // Si se pasa la carta, se pinta con HOLO real (foil de malie, brillo ambiental sin
    // giroscopio). Null = arte plano por [imageUrl] (dorsos, fantasmas de arrastre, etc.).
    card: com.mineralord.tcg.engine.model.Card? = null,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(CombatTheme.CardCorner))
            .background(CombatTheme.Surface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) CombatTheme.Gold else CombatTheme.Border,
                RoundedCornerShape(CombatTheme.CardCorner),
            ),
    ) {
        if (faceDown || imageUrl == null) {
            CardBack(Modifier.fillMaxSize())
        } else if (card != null) {
            HoloCardImage(
                // OJO: `card.set.code` guarda el NOMBRE del set; el código que malie entiende
                // sale del prefijo del id impreso ("svp-106" → "svp").
                imageUrl = imageUrl,
                setCode = card.id.printed.raw.let { if (it.startsWith("energy")) "energy" else it.substringBeforeLast('-') },
                cardNumber = card.id.printed.raw.substringAfterLast('-').toIntOrNull(),
                rarity = card.rarity,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (dimmed) 0.45f else 1f },
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (dimmed) 0.45f else 1f },
            )
        }
    }
}

/** Dorso de carta: reverso clásico ya empaquetado en el proyecto (`card_back_default`).
 *  Se usa en TODAS las cartas boca abajo (dorsos, mazo, premios, mano rival, fantasmas).
 *  Para cambiar el diseño, sustituye ese drawable en feature/game/src/main/res/drawable. */
@Composable
fun CardBack(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(com.mineralord.tcg.feature.combat.R.drawable.card_back_default),
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = modifier,
    )
}

/** Barra de PS fina bajo un Pokémon (verde→ámbar→rojo según fracción restante). */
@Composable
fun HpBar(remaining: Int, max: Int, modifier: Modifier = Modifier) {
    val frac = if (max <= 0) 0f else (remaining.toFloat() / max).coerceIn(0f, 1f)
    val color = when {
        frac > 0.5f -> CombatTheme.Good
        frac > 0.2f -> CombatTheme.Gold
        else -> CombatTheme.Foe
    }
    Box(
        modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0x33000000)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(frac)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
    }
}

/** Marcadores de estado especial como puntos de color sobre la carta. */
@Composable
fun StatusRow(statuses: Set<Status>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        statuses.forEach { s ->
            val c = when (s) {
                Status.POISONED -> Color(0xFF9B59B6)
                Status.BURNED -> Color(0xFFE0555F)
                Status.ASLEEP -> Color(0xFF4DA3FF)
                Status.PARALYZED -> Color(0xFFF0C24B)
                Status.CONFUSED -> Color(0xFFE08A3C)
            }
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(c)
                    .border(1.dp, Color(0x66000000), CircleShape),
            )
        }
    }
}

/** Insignia con el nº de energías adjuntas. */
@Composable
fun EnergyBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(CombatTheme.Gold),
        contentAlignment = Alignment.Center,
    ) {
        Text("$count", color = Color(0xFF20232B), fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

/** Hueco vacío (Activo/Banca sin Pokémon); se ilumina como objetivo de soltado. */
@Composable
fun EmptySlot(modifier: Modifier = Modifier, highlighted: Boolean = false) {
    Box(
        modifier
            .clip(RoundedCornerShape(CombatTheme.CardCorner))
            .background(if (highlighted) Color(0x33F0C24B) else Color(0x14FFFFFF))
            .border(
                if (highlighted) 2.dp else 1.dp,
                if (highlighted) CombatTheme.Gold else CombatTheme.Border,
                RoundedCornerShape(CombatTheme.CardCorner),
            ),
    )
}

/** Píldora de conteo de una zona (mazo/descarte/premios/mano). Clickable opcional. */
@Composable
fun ZonePill(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CombatTheme.Surface.copy(alpha = 0.9f))
            .border(1.dp, CombatTheme.Border, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = CombatTheme.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text("$count", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}
