package com.mineralord.tcg.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.TcgColors

/** Un jugador del lobby. [isHost] marca al anfitrión (corona). [ready] su estado "Listo".
 *  [isLocal] indica si es ESTE dispositivo (su fila muestra el botón para marcarse listo). */
data class LobbyPlayer(
    val name: String,
    val role: String,
    val online: Boolean,
    val ready: Boolean,
    val isHost: Boolean,
    val isLocal: Boolean = false,
    val avatarColors: List<Color>? = null,
)

/** Un amigo invitable desde la lista. [status] = "En línea" / "Jugando" / etc. (vacío = oculto). */
data class LobbyFriend(
    val uid: String,
    val name: String,
    val status: String = "",
    val avatarColors: List<Color>? = null,
)

private val Online = Color(0xFF2FA86A)
private val Busy = TcgColors.Gold
private val Line = Color(0x14000000)
private val CardBg = TcgColors.Parchment

/**
 * Sala multijugador "Jugar con amigos" (lobby del anfitrión), reinterpretada al design system del
 * clon: cabecera roja (identidad TCG Live), cuerpo crema con tarjetas blancas y **scroll vertical
 * cómodo** (nada de alto fijo). Sin píldoras de moneda ni QR: compartir es un único botón que copia
 * el código de sala. El combate arranca con [onStart] (habilitado por [canStart]).
 */
@Composable
fun FriendLobbyScreen(
    roomCode: String,
    players: List<LobbyPlayer>,
    friends: List<LobbyFriend>,
    canStart: Boolean,
    localReady: Boolean,
    onToggleReady: (Boolean) -> Unit,
    onBack: () -> Unit,
    onShareCode: () -> Unit,
    onInviteFriend: (LobbyFriend) -> Unit,
    onCardLimit: () -> Unit,
    onChooseDeck: () -> Unit,
    onStart: () -> Unit,
    myDeckName: String,
    modifier: Modifier = Modifier,
    format: String = "Estándar",
    rules: String = "Oficiales",
    mode: String = "1 vs 1",
    turnTime: String = "25:00",
    maxPlayers: Int = 2,
) {
    Column(modifier.background(TcgColors.Cream)) {
        LobbyHeader(roomCode = roomCode, onBack = onBack, onShareCode = onShareCode)

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val host = players.firstOrNull { it.isHost } ?: players.firstOrNull()
            if (host != null) HostSpotlight(host)

            ParamsGrid(format = format, rules = rules, mode = mode, turnTime = turnTime)

            PlayersSection(
                players = players,
                maxPlayers = maxPlayers,
                localReady = localReady,
                onToggleReady = onToggleReady,
            )

            MyDeckRow(deckName = myDeckName, onClick = onChooseDeck)

            CardLimitRow(format = format, onClick = onCardLimit)

            InviteFriends(friends = friends, onInvite = onInviteFriend)

            Spacer(Modifier.height(2.dp))

            ShareButton(onClick = onShareCode)
            StartButton(enabled = canStart, onClick = onStart)

            Text(
                "El combate empezará cuando ambos jugadores estén listos.",
                color = TcgColors.Ink.copy(alpha = 0.55f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
            )
        }
    }
}

// ---- Cabecera roja ----------------------------------------------------------------------------

@Composable
private fun LobbyHeader(roomCode: String, onBack: () -> Unit, onShareCode: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(TcgColors.Red, TcgColors.RedDark)))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.weight(1f))
            // Píldora con el código de sala (toque = compartir/copiar).
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(onClick = onShareCode)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Código de sala", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(roomCode, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Jugar con amigos", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Text("Lobby del anfitrión", color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ---- Tarjeta del anfitrión --------------------------------------------------------------------

@Composable
private fun HostSpotlight(host: LobbyPlayer) {
    Surface {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LobbyAvatar(host.name, host.avatarColors, 54.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(host.name, color = TcgColors.Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(host.role, color = TcgColors.Ink.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            StatusTag(online = host.online)
        }
    }
}

// ---- Parámetros de la sala (2x2, cómodo) ------------------------------------------------------

@Composable
private fun ParamsGrid(format: String, rules: String, mode: String, turnTime: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ParamChip("Formato", format, "📋", Modifier.weight(1f))
            ParamChip("Reglas", rules, "⚖️", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ParamChip("Modo", mode, "⚔️", Modifier.weight(1f))
            ParamChip("Tiempo por turno", turnTime, "⏱️", Modifier.weight(1f))
        }
    }
}

@Composable
private fun ParamChip(label: String, value: String, glyph: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(glyph, fontSize = 15.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, color = TcgColors.Ink.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(value, color = TcgColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// ---- Jugadores --------------------------------------------------------------------------------

@Composable
private fun PlayersSection(
    players: List<LobbyPlayer>,
    maxPlayers: Int,
    localReady: Boolean,
    onToggleReady: (Boolean) -> Unit,
) {
    Surface {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Jugadores", color = TcgColors.Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text("${players.size}/$maxPlayers", color = TcgColors.Ink.copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            players.forEach { p ->
                PlayerRow(p, onToggleReady = { onToggleReady(!localReady) })
                Spacer(Modifier.height(10.dp))
            }
            // Huecos vacíos hasta completar la sala.
            repeat((maxPlayers - players.size).coerceAtLeast(0)) {
                EmptyPlayerSlot()
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun PlayerRow(p: LobbyPlayer, onToggleReady: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            LobbyAvatar(p.name, p.avatarColors, 44.dp)
            if (p.isHost) {
                Box(Modifier.align(Alignment.TopStart).padding(top = 0.dp)) {
                    Text("👑", fontSize = 15.sp)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (p.isLocal) "${p.name} (tú)" else p.name,
                color = TcgColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            )
            Text(p.role, color = TcgColors.Ink.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        // El jugador local puede pulsar su etiqueta para marcarse/desmarcarse "Listo".
        if (p.isLocal) ReadyTag(ready = p.ready, onClick = onToggleReady) else ReadyTag(ready = p.ready)
    }
}

@Composable
private fun EmptyPlayerSlot() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(TcgColors.Ink.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = TcgColors.Ink.copy(alpha = 0.35f), fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Esperando jugador…", color = TcgColors.Ink.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Comparte el código o invita a un amigo", color = TcgColors.Ink.copy(alpha = 0.45f), fontSize = 12.sp)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Busy.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text("Esperando", color = TcgColors.GoldDark, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ---- Límite de cartas -------------------------------------------------------------------------

@Composable
private fun CardLimitRow(format: String, onClick: () -> Unit) {
    Surface {
        Row(
            Modifier.clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(TcgColors.Red.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Text("🃏", fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Límite de cartas", color = TcgColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Solo cartas del formato $format. Expansiones válidas permitidas.", color = TcgColors.Ink.copy(alpha = 0.5f), fontSize = 12.sp, lineHeight = 15.sp)
            }
            Text("›", color = TcgColors.Ink.copy(alpha = 0.4f), fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ---- Tu baraja (cada jugador elige la suya) ---------------------------------------------------

@Composable
private fun MyDeckRow(deckName: String, onClick: () -> Unit) {
    Surface {
        Row(
            Modifier.clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(TcgColors.Gold.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Text("🃏", fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Tu baraja", color = TcgColors.Ink.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(
                    deckName.ifBlank { "Elige una baraja" },
                    color = TcgColors.Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                )
            }
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(TcgColors.Red.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("Cambiar", color = TcgColors.Red, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// ---- Invitar amigos ---------------------------------------------------------------------------

@Composable
private fun InviteFriends(friends: List<LobbyFriend>, onInvite: (LobbyFriend) -> Unit) {
    Surface {
        Column(Modifier.padding(14.dp)) {
            Text("Invitar amigos", color = TcgColors.Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text("Invita desde tu lista de amigos", color = TcgColors.Ink.copy(alpha = 0.5f), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            if (friends.isEmpty()) {
                Text(
                    "Aún no tienes amigos en línea para invitar.",
                    color = TcgColors.Ink.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                )
            } else {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    friends.forEach { f ->
                        Column(
                            Modifier.width(60.dp).clickable { onInvite(f) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            LobbyAvatar(f.name, f.avatarColors, 48.dp)
                            Spacer(Modifier.height(5.dp))
                            Text(f.name, color = TcgColors.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                            if (f.status.isNotBlank()) {
                                Text(
                                    f.status,
                                    color = if (f.status.equals("En línea", true)) Online else TcgColors.Ink.copy(alpha = 0.45f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Acciones inferiores ----------------------------------------------------------------------

@Composable
private fun ShareButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔗", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text("Compartir código", color = TcgColors.Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

@Composable
private fun StartButton(enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) Brush.verticalGradient(listOf(TcgColors.Gold, TcgColors.GoldDark))
    else Brush.verticalGradient(listOf(TcgColors.Ink.copy(alpha = 0.18f), TcgColors.Ink.copy(alpha = 0.18f)))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "⚔️  INICIAR COMBATE",
            color = if (enabled) TcgColors.Ink else TcgColors.Ink.copy(alpha = 0.4f),
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            letterSpacing = 0.5.sp,
        )
    }
}

// ---- Piezas compartidas -----------------------------------------------------------------------

/** Tarjeta blanca redondeada, contenedor estándar del cuerpo. */
@Composable
private fun Surface(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg),
    ) { content() }
}

/** Avatar circular con degradado (de los colores del cosmético o por defecto) y la inicial. */
@Composable
private fun LobbyAvatar(name: String, colors: List<Color>?, size: androidx.compose.ui.unit.Dp) {
    val grad = colors?.takeIf { it.isNotEmpty() }?.let { if (it.size == 1) listOf(it[0], it[0]) else it }
        ?: listOf(TcgColors.NavyLight, TcgColors.Navy)
    Box(
        Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(grad)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

@Composable
private fun StatusTag(online: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (online) Online else TcgColors.Ink.copy(alpha = 0.3f)))
        Spacer(Modifier.width(6.dp))
        Text(if (online) "En línea" else "Ausente", color = if (online) Online else TcgColors.Ink.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReadyTag(ready: Boolean, onClick: (() -> Unit)? = null) {
    val c = if (ready) Online else Busy
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = if (onClick != null) 0.22f else 0.16f))
            .then(clickMod)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            when {
                ready -> "Listo ✓"
                onClick != null -> "Marcar listo"
                else -> "Esperando"
            },
            color = if (ready) Online else TcgColors.GoldDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
        )
    }
}
