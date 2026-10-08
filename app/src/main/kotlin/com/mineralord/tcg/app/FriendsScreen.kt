package com.mineralord.tcg.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.data.netfirestore.FriendEntry
import com.mineralord.tcg.data.netfirestore.FriendRequest
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay

// Acentos de acción del hub (coherentes con el resto de la app).
private val AcceptGreen = Color(0xFF19D08B)
private val RejectRed = Color(0xFFF24139)
private val DarkPill = Color(0xFF415268)

/** Estado de última conexión que muestra cada tarjeta. */
private enum class LastSeen(val label: String) {
    RECENT("Hace 16 h"),
    WEEK_PLUS("Hace más de 7 d"),
    NOT_SHARED("No compartida"),
}

/** Un usuario del hub (amigo o solicitud). [uid] identifica al jugador en el backend real. */
private data class FriendUi(
    val name: String,
    val level: Int,
    val lastSeen: LastSeen,
    val badges: List<Color>,
    val sharesSelection: Boolean = true,
    val avatar: List<Color>,
    val uid: String = "",
    // Campos del PERFIL (valores de ejemplo por defecto; se poblarán desde el backend luego).
    val friendCode: String = "4897-4115-8907-2209",
    val statusMessage: String = "¡Quiero completar el índice de cartas!",
    val collectionCount: Int = 11_505,
    val tradeMessage: String = "Acepto cartas en cualquier idioma",
    val hashtag: String = "#LegendariosSingulares",
)

private enum class FriendsTab(val label: String) {
    AMIGOS("Amigos"),
    ENVIADAS("Solicitudes\nenviadas"),
    RECIBIDAS("Solicitudes\nrecibidas"),
}

// Paletas de avatar (degradados originales, sin personajes con derechos).
private val avatarPalettes = listOf(
    listOf(Color(0xFFFF8A8A), Color(0xFFB83B5E)),
    listOf(Color(0xFF8AD1FF), Color(0xFF3A6EA5)),
    listOf(Color(0xFFBFe08A), Color(0xFF4C9A4A)),
    listOf(Color(0xFFD7A8FF), Color(0xFF7A3FB8)),
    listOf(Color(0xFFFFD28A), Color(0xFFB8842F)),
)
private val badgePalette = listOf(
    Color(0xFFE8503A), Color(0xFF3AA6E8), Color(0xFF4CAF50),
    Color(0xFF9B59B6), Color(0xFFF2C500), Color(0xFF95A5A6),
)
private fun badges(n: Int, seed: Int) = List(n) { badgePalette[(seed + it) % badgePalette.size] }

// Avatar/insignias deterministas a partir del ID de amigo (placeholder visual estable, sin backend
// de cosméticos social todavía). El nombre y el ID son datos REALES del backend.
private fun uiFromReal(uid: String, username: String, friendCode: String): FriendUi {
    val seed = friendCode.hashCode().absoluteValue
    return FriendUi(
        name = username.ifBlank { "Entrenador" },
        level = 0,
        lastSeen = LastSeen.NOT_SHARED,
        badges = badges(3, seed),
        avatar = avatarPalettes[seed % avatarPalettes.size],
        uid = uid,
        friendCode = formatFriendCode(friendCode),
    )
}

private fun FriendEntry.toUi() = uiFromReal(uid, username, friendCode)
private fun FriendRequest.toUi() = uiFromReal(uid, username, friendCode)

/**
 * Hub de Amigos. Réplica de la UX del hub de referencia con nuestro design system claro:
 * cabecera "Amigos" + divisor arcoíris · sub-barra fija (contador n/99 + añadir) · tres pestañas
 * (Amigos / Solicitudes enviadas / Solicitudes recibidas) con tarjetas de usuario cuya zona de
 * acción cambia por pestaña · barra inferior con cerrar y "Borrar todas" en las de solicitudes.
 * Datos MOCK locales (sin red todavía): la lógica se conectará después.
 */
@Composable
fun FriendsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    vm: FriendsViewModel = viewModel(),
) {
    var tab by remember { mutableStateOf(FriendsTab.AMIGOS) }
    // Perfil abierto (al tocar una tarjeta de amigo). Null = hub con sus pestañas.
    var profile by remember { mutableStateOf<FriendUi?>(null) }
    // Panel "Añadir amigo" (solo por ID de amigo; sin QR).
    var addOpen by remember { mutableStateOf(false) }

    // Datos REALES en vivo (Firestore) + identidad propia.
    val friends by vm.friends.collectAsStateWithLifecycle()
    val incoming by vm.incoming.collectAsStateWithLifecycle()
    val outgoing by vm.outgoing.collectAsStateWithLifecycle()
    val myName by vm.myName.collectAsStateWithLifecycle()
    val myCode by vm.myCode.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    val list = when (tab) {
        FriendsTab.AMIGOS -> friends.map { it.toUi() }
        FriendsTab.ENVIADAS -> outgoing.map { it.toUi() }
        FriendsTab.RECIBIDAS -> incoming.map { it.toUi() }
    }
    val emptyText = when (tab) {
        FriendsTab.AMIGOS -> "Aún no tienes amigos.\nToca + para añadir por ID de amigo."
        FriendsTab.ENVIADAS -> "No tienes solicitudes enviadas."
        FriendsTab.RECIBIDAS -> "No tienes solicitudes recibidas."
    }

    profile?.let { p ->
        FriendProfileScreen(user = p, modifier = modifier, onBack = { profile = null })
        return
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BarajasPalette.BgTop, BarajasPalette.BgBottom))),
        ) {
            Header(showFavorites = tab == FriendsTab.AMIGOS)
            RainbowDivider()
            CounterBar(friendCount = friends.size, onAdd = { addOpen = true })

            Box(Modifier.fillMaxSize().weight(1f)) {
                if (list.isEmpty()) {
                    Text(
                        emptyText,
                        color = BarajasPalette.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(list) { user ->
                            FriendCard(
                                user, tab,
                                onOpen = { profile = user },
                                onAccept = { vm.accept(user.uid) },
                                onReject = { vm.reject(user.uid) },
                                onCancel = { vm.cancel(user.uid) },
                                onRemove = { vm.remove(user.uid) },
                            )
                        }
                    }
                }
            }

            BottomTabs(current = tab, onSelect = { tab = it })
            BottomActions(
                showClearAll = tab != FriendsTab.AMIGOS && list.isNotEmpty(),
                onClose = onBack,
                onClearAll = { if (tab == FriendsTab.ENVIADAS) vm.clearAllOutgoing() else vm.clearAllIncoming() },
            )
        }

        if (addOpen) {
            AddFriendSheet(
                myName = myName.ifBlank { "Entrenador" },
                rawCode = myCode,
                message = message,
                onSend = { vm.send(it) },
                onClose = { vm.clearMessage(); addOpen = false },
            )
        }
    }
}

// ───────────────────────────── cabecera / barras ─────────────────────────────

@Composable
private fun Header(showFavorites: Boolean) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Amigos", color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Spacer(Modifier.weight(1f))
        if (showFavorites) {
            Row(
                Modifier.shadow(3.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                    .background(BarajasPalette.Surface).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("★", color = Color(0xFFE7B10A), fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("Favoritos", color = BarajasPalette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RainbowDivider() {
    Box(
        Modifier.fillMaxWidth().height(3.dp)
            .background(Brush.horizontalGradient(BarajasPalette.DividerGradient)),
    )
}

@Composable
private fun CounterBar(friendCount: Int, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
            .background(BarajasPalette.Surface).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Amigos", color = BarajasPalette.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Text("$friendCount/99", color = BarajasPalette.Muted, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.weight(1f))
        // Añadir amigo (persona con +): abre el panel de añadir por ID de amigo.
        Box(
            Modifier.size(34.dp).clip(CircleShape).border(1.5.dp, BarajasPalette.Muted, CircleShape)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) { Text("+", color = BarajasPalette.Ink, fontSize = 18.sp, fontWeight = FontWeight.Black) }
    }
}

/** Formatea un código de amigo de 12 dígitos como "XXXX-XXXX-XXXX"; si no, un placeholder. */
private fun formatFriendCode(raw: String): String =
    if (raw.length == 12) "${raw.substring(0, 4)}-${raw.substring(4, 8)}-${raw.substring(8, 12)}"
    else "————-————-————"

/**
 * Panel "Añadir amigo" (solo por ID de amigo; SIN QR). Muestra tu avatar, tu nombre y tu ID (copiable)
 * y un campo para buscar/añadir a otro jugador por su ID de amigo. Datos MOCK: "enviar solicitud"
 * valida el formato y confirma visualmente (la lógica de red se conectará después).
 */
@Composable
private fun AddFriendSheet(
    myName: String,
    rawCode: String,
    message: String?,
    onSend: (String) -> Unit,
    onClose: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1500); copied = false } }
    var query by remember { mutableStateOf("") }

    val codeText = formatFriendCode(rawCode)
    val noRipple = remember { MutableInteractionSource() }

    // Scrim que cierra al tocar fuera.
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000))
            .clickable(interactionSource = noRipple, indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Tarjeta inferior; consume toques para no cerrar al interactuar dentro.
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BarajasPalette.Surface)
                .clickable(interactionSource = noRipple, indication = null, onClick = {})
                .padding(20.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Avatar circular con la inicial.
            Box(
                Modifier.size(64.dp).clip(CircleShape)
                    .background(Brush.verticalGradient(avatarPalettes[0])),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    myName.take(1).uppercase(),
                    color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(myName, color = BarajasPalette.Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))

            // Tu ID de amigo (copiable).
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(BarajasPalette.BgTop).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ID de amigo", color = BarajasPalette.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                Text(codeText, color = BarajasPalette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(BarajasPalette.Surface)
                        .clickable(enabled = rawCode.length == 12) {
                            clipboard.setText(AnnotatedString(rawCode)); copied = true
                        },
                    contentAlignment = Alignment.Center,
                ) { Text("⧉", color = BarajasPalette.Ink, fontSize = 15.sp) }
            }
            if (copied) {
                Spacer(Modifier.height(6.dp))
                Text("¡Copiado!", color = AcceptGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(18.dp))
            Text("Añadir amigo", color = BarajasPalette.Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))

            // Campo: buscar por ID de amigo.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, BarajasPalette.Muted.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⌕", color = BarajasPalette.Muted, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Buscar ID de amigo", color = BarajasPalette.Muted, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { s -> query = s.filter { it.isDigit() || it == '-' || it == ' ' }.take(17) },
                        singleLine = true,
                        textStyle = TextStyle(color = BarajasPalette.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = Brush.verticalGradient(listOf(BarajasPalette.Ink, BarajasPalette.Ink)),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Botón enviar solicitud (valida 12 dígitos; mock por ahora).
            val digits = query.filter { it.isDigit() }
            val enabled = digits.length == 12
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (enabled) AcceptGreen else BarajasPalette.Muted.copy(alpha = 0.35f))
                    .clickable(enabled = enabled) { onSend(digits); query = "" }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Enviar solicitud", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            // Mensaje REAL del backend (éxito / "No se encontró ese ID" / "Ya es tu amigo"…).
            message?.let {
                val ok = it.contains("enviada", ignoreCase = true)
                Spacer(Modifier.height(8.dp))
                Text(it, color = if (ok) AcceptGreen else RejectRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))
            // Cerrar.
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(BarajasPalette.BgTop)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = BarajasPalette.Ink, fontSize = 18.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ───────────────────────────── tarjeta de usuario ─────────────────────────────

@Composable
private fun FriendCard(
    user: FriendUi,
    tab: FriendsTab,
    onOpen: (() -> Unit)? = null,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {},
    onCancel: () -> Unit = {},
    onRemove: () -> Unit = {},
) {
    Box(
        // Todas las tarjetas llevan los mismos elementos → misma altura natural (sin inflar).
        Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp))
            .background(BarajasPalette.Surface)
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(14.dp),
    ) {
        // Lupa (ver perfil) arriba a la derecha.
        Text(
            "⌕", color = BarajasPalette.Muted, fontSize = 16.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Avatar(user)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (user.level > 0) "Nv. ${user.level}" else "ID ${user.friendCode}",
                    color = BarajasPalette.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                )
                Text(
                    user.name, color = BarajasPalette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Black,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Última conexión: ${user.lastSeen.label}",
                    color = BarajasPalette.Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                )
                if (user.badges.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        user.badges.forEach { c ->
                            Box(Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(c.copy(alpha = 0.85f)))
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            CardActions(tab, onAccept = onAccept, onReject = onReject, onCancel = onCancel, onRemove = onRemove)
        }
    }
}

@Composable
private fun Avatar(user: FriendUi) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(Brush.verticalGradient(user.avatar)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            user.name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black,
        )
    }
}

/** Zona de acción a la derecha de la tarjeta — cambia según la pestaña (acciones REALES). */
@Composable
private fun CardActions(
    tab: FriendsTab,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
) {
    when (tab) {
        FriendsTab.AMIGOS -> {
            Row(
                Modifier.clip(RoundedCornerShape(50))
                    .border(1.5.dp, RejectRed.copy(alpha = 0.7f), RoundedCornerShape(50))
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("✕", color = RejectRed, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text("Eliminar", color = RejectRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        FriendsTab.ENVIADAS -> {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(DarkPill)
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) { Text("Cancelar solicitud", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
        FriendsTab.RECIBIDAS -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconCircle("✕", RejectRed, onReject)
                IconCircle("✓", AcceptGreen, onAccept)
            }
        }
    }
}

@Composable
private fun IconCircle(glyph: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).border(2.dp, color, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black) }
}

// ───────────────────────────── barra inferior ─────────────────────────────

@Composable
private fun BottomTabs(current: FriendsTab, onSelect: (FriendsTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(BarajasPalette.Surface).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FriendsTab.entries.forEach { t ->
            val active = t == current
            Column(
                Modifier.weight(1f).clickable { onSelect(t) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    t.label,
                    color = if (active) BarajasPalette.Ink else BarajasPalette.Muted,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 14.sp,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.width(26.dp).height(3.dp).clip(RoundedCornerShape(50))
                        .background(if (active) BarajasPalette.NavIcon else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun BottomActions(showClearAll: Boolean, onClose: () -> Unit, onClearAll: () -> Unit = {}) {
    Box(
        Modifier.fillMaxWidth().background(BarajasPalette.Surface)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(52.dp).shadow(3.dp, CircleShape).clip(CircleShape)
                .background(BarajasPalette.Surface).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) { Text("✕", color = BarajasPalette.Muted, fontSize = 20.sp, fontWeight = FontWeight.Black) }

        if (showClearAll) {
            Row(
                Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(50))
                    .clickable(onClick = onClearAll).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("✕", color = RejectRed, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("Borrar todas", color = RejectRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═════════════════════════════ PERFIL DE AMIGO ═════════════════════════════

/**
 * Vista de PERFIL de un amigo (se abre al tocar su tarjeta en la pestaña Amigos). Réplica de la
 * UX de referencia con nuestro design system: ID+código, avatar, nivel/nombre, lema, Estadísticas,
 * estado de amistad, Emblemas, Selección personal, Cartas en la colección, Mensaje de intercambio,
 * Lista de deseadas y Logros. Placeholders ABSTRACTOS originales (sin arte de terceros). Mock local.
 */
@Composable
private fun FriendProfileScreen(user: FriendUi, modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(
        modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(BarajasPalette.BgTop, BarajasPalette.BgBottom))),
    ) {
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.statusBarsPadding().height(8.dp))
            // Cabecera: ID de amigo + código + guardar.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("ID de amigo", color = BarajasPalette.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(user.friendCode, color = BarajasPalette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(BarajasPalette.Surface),
                    contentAlignment = Alignment.Center,
                ) { Text("🔖", fontSize = 15.sp) }
            }

            Spacer(Modifier.height(10.dp))
            // Avatar grande con aro.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(116.dp).clip(CircleShape)
                        .border(4.dp, Brush.sweepGradient(BarajasPalette.DividerGradient), CircleShape)
                        .padding(5.dp).clip(CircleShape).background(Brush.verticalGradient(user.avatar)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        user.name.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Nv. ${user.level}", color = BarajasPalette.Ink, fontSize = 20.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(user.name, color = BarajasPalette.Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

            Spacer(Modifier.height(12.dp))
            // Píldora de lema/estado.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(BarajasPalette.Surface)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) { Text(user.statusMessage, color = BarajasPalette.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }

            Spacer(Modifier.height(16.dp))
            // Estadísticas + estado de amistad.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(BarajasPalette.Surface),
                        contentAlignment = Alignment.Center,
                    ) { Text("📊", fontSize = 20.sp) }
                    Spacer(Modifier.height(4.dp))
                    Text("Estadísticas", color = BarajasPalette.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(BarajasPalette.Surface)
                        .border(1.5.dp, AcceptGreen, RoundedCornerShape(50))
                        .padding(horizontal = 22.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("✓", color = AcceptGreen, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text("Amigo", color = AcceptGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }

            SectionTitle("Emblemas")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                listOf(0, 2, 4).forEach { i -> Hexagon(badgePalette[i % badgePalette.size], Modifier.size(70.dp)) }
            }

            SectionTitle("Selección personal")
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PlaceholderCard(user.avatar, Modifier.width(150.dp).height(210.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                repeat(3) { PlaceholderCard(avatarPalettes[it], Modifier.width(60.dp).height(84.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            Text(user.hashtag, color = BarajasPalette.NavIcon, fontSize = 12.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            PillButton("🖼  Ver galería")

            SectionTitle("Cartas en la colección")
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(BarajasPalette.Surface)
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("🃏", fontSize = 14.sp)
                    Text("%,d".format(user.collectionCount), color = BarajasPalette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }

            SectionTitle("Mensaje de intercambio")
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(BarajasPalette.Surface)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) { Text(user.tradeMessage, color = BarajasPalette.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }

            SectionTitle("Lista de deseadas")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                repeat(3) { PlaceholderCard(avatarPalettes[(it + 1) % avatarPalettes.size], Modifier.width(78.dp).height(108.dp)) }
            }
            Spacer(Modifier.height(10.dp))
            PillButton("♡  Ver lista de deseadas")

            SectionTitle("Logros")
            val trophyColors = badgePalette + badgePalette.reversed()
            trophyColors.chunked(4).forEach { rowColors ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    rowColors.forEach { c -> Trophy(c) }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Barra inferior: cerrar.
        Box(
            Modifier.fillMaxWidth().background(BarajasPalette.Surface).padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(52.dp).shadow(3.dp, CircleShape).clip(CircleShape)
                    .background(BarajasPalette.Surface).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = BarajasPalette.Muted, fontSize = 20.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(18.dp))
    Text(text, color = BarajasPalette.Ink, fontSize = 14.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun PillButton(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(BarajasPalette.Surface)
                .border(1.5.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(50))
                .padding(horizontal = 24.dp, vertical = 11.dp),
        ) { Text(text, color = BarajasPalette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

/** Tarjeta-placeholder abstracta (degradado + brillo diagonal), sin arte de terceros. */
@Composable
private fun PlaceholderCard(colors: List<Color>, modifier: Modifier) {
    Box(
        modifier.shadow(6.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(colors))
            .border(2.dp, Color(0x55FFFFFF), RoundedCornerShape(10.dp)),
    ) {
        Box(
            Modifier.fillMaxWidth().height(1.dp),
        )
        Box(
            Modifier.padding(10.dp).clip(RoundedCornerShape(6.dp))
                .background(Color(0x33FFFFFF)).fillMaxWidth().height(1.dp),
        )
    }
}

/** Emblema hexagonal original relleno de degradado. */
@Composable
private fun Hexagon(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.25f)
            lineTo(w, h * 0.75f)
            lineTo(w * 0.5f, h)
            lineTo(0f, h * 0.75f)
            lineTo(0f, h * 0.25f)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f))))
        drawPath(path, Color(0x66FFFFFF), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f))
    }
}

/** Trofeo-placeholder: medalla circular sobre pedestal, abstracta. */
@Composable
private fun Trophy(color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(42.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.55f))))
                .border(2.dp, Color(0x55FFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("★", color = Color(0xCCFFFFFF), fontSize = 16.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.width(22.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(BarajasPalette.HollowBorder))
    }
}
