package com.mineralord.tcg.feature.game

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.data.cards.StarterDecks
import com.mineralord.tcg.data.cards.toDeck
import com.mineralord.tcg.data.netplay.MatchFactoryProvider
import com.mineralord.tcg.data.profile.ProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private sealed interface LobbyPhase {
    data object Choosing : LobbyPhase
    data class Hosting(val code: String) : LobbyPhase
    data object Joining : LobbyPhase
    data object Connecting : LobbyPhase
    data object Playing : LobbyPhase
}

/**
 * Pantalla de partida ONLINE: primero un lobby (crear con código / unirse por
 * código) y, una vez conectados, el tablero de combate usando [OnlineGameController]
 * (host-autoritativo). El modo vs IA (PvE) es independiente y no se toca.
 */
@Composable
fun OnlineGameScreen(onExit: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val factoryProvider = app as MatchFactoryProvider
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf<LobbyPhase>(LobbyPhase.Choosing) }
    var controller by remember { mutableStateOf<OnlineGameController?>(null) }
    var codeInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { controller?.close() } }

    // Nombre + cartas de la baraja activa. El nombre solo es informativo (log/UI):
    // deja ver de un vistazo con qué baraja entra cada jugador y detectar duplicados.
    suspend fun loadDeck(): Pair<String, List<String>> {
        val profile = ProfileRepository(app).profile.first()
        val deck = profile.decks.firstOrNull { it.id == profile.activeDeckId }
            ?: profile.decks.firstOrNull()
            ?: StarterDecks.ALL.first().toDeck()
        return deck.name to deck.expandedCardIds().map { it.raw }
    }

    val ctrl = controller
    if (ctrl != null) {
        val ui by ctrl.ui.collectAsStateWithLifecycle()
        val started = ui.coinFlip != null || ui.dealing != null || ui.setup != null || ui.state != null
        val hosting = phase as? LobbyPhase.Hosting
        if (hosting != null && !started) {
            CodeWaitingCard(code = hosting.code, onCancel = onExit, modifier = modifier)
        } else {
            GameScreen(onExit = onExit, modifier = modifier, vm = ctrl)
        }
        return
    }

    Column(
        modifier = modifier.fillMaxSize().background(TcgColors.Navy).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Jugar online", color = TcgColors.Parchment, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Juega contra otra persona por Internet con tu baraja activa.",
            color = TcgColors.Parchment.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        when (phase) {
            is LobbyPhase.Choosing -> {
                LobbyButton("Crear partida", primary = true) {
                    error = null
                    phase = LobbyPhase.Connecting
                    scope.launch {
                        runCatching {
                            val (deckName, printed) = loadDeck()
                            val (code, transport) = factoryProvider.matchFactory.host("Anfitrión")
                            controller = OnlineGameController(app, transport, "Anfitrión", printed, deckName)
                            phase = LobbyPhase.Hosting(code)
                        }.onFailure { error = it.message ?: "No se pudo crear la partida"; phase = LobbyPhase.Choosing }
                    }
                }
                Spacer(Modifier.height(12.dp))
                LobbyButton("Unirse con código", primary = false) { error = null; phase = LobbyPhase.Joining }
            }
            is LobbyPhase.Joining -> {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { if (it.length <= 6) codeInput = it.uppercase() },
                    label = { Text("Código de la partida") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                LobbyButton("Entrar", primary = true) {
                    error = null
                    phase = LobbyPhase.Connecting
                    scope.launch {
                        runCatching {
                            val (deckName, printed) = loadDeck()
                            val transport = factoryProvider.matchFactory.join(codeInput.trim(), "Invitado")
                            controller = OnlineGameController(app, transport, "Invitado", printed, deckName)
                            phase = LobbyPhase.Playing
                        }.onFailure { error = it.message ?: "No se pudo unir"; phase = LobbyPhase.Joining }
                    }
                }
            }
            is LobbyPhase.Connecting -> Text("Conectando…", color = TcgColors.Parchment, fontSize = 15.sp)
            else -> {}
        }

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = Color(0xFFE57373), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CodeWaitingCard(code: String, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(TcgColors.Navy).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Comparte este código", color = TcgColors.Parchment.copy(alpha = 0.8f), fontSize = 15.sp)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(TcgColors.NavyLight)
                .border(2.dp, TcgColors.Gold, RoundedCornerShape(14.dp))
                .padding(horizontal = 32.dp, vertical = 20.dp),
        ) {
            Text(code, color = TcgColors.Gold, fontWeight = FontWeight.Black, fontSize = 44.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text("Esperando a que tu rival se una…", color = TcgColors.Parchment.copy(alpha = 0.7f), fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))
        LobbyButton("Cancelar", primary = false, onClick = onCancel)
    }
}

@Composable
private fun LobbyButton(text: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (primary) TcgColors.Gold else Color.Transparent)
            .then(if (primary) Modifier else Modifier.border(2.dp, TcgColors.Parchment.copy(alpha = 0.4f), RoundedCornerShape(10.dp)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (primary) TcgColors.Ink else TcgColors.Parchment,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
        )
    }
}
