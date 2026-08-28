package com.mineralord.tcg.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Hexagon
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.core.designsystem.HexagonShape
import com.mineralord.tcg.core.designsystem.fadingScrollbar
import com.mineralord.tcg.data.cloud.AuthState
import com.mineralord.tcg.data.cloud.SyncStatus
import com.mineralord.tcg.data.cosmetics.Cosmetic
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Accent = Color(0xFFC0303A)          // acento cabecera (rojo TCG)
private val AccentDark = Color(0xFF7C1622)
private val ChipBlue = Color(0xFF3FA9F5)
private val ChipGold = Color(0xFFF3B33B)
private val ChipViolet = Color(0xFF9B6FE0)

/**
 * Pantalla de Perfil del jugador (scrolleable, réplica del prototipo con el design system
 * de barajas): cabecera con identidad + monedas, tarjeta de estadísticas, info de cuenta
 * (incluye estado de sesión/nube real), Personalización, Resumen rápido (datos reales de
 * la colección) y accesos. Lo aún no implementado abre "Esta función no está disponible".
 */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    vm: ProfileViewModel = viewModel(),
    onBack: () -> Unit = {},
    onOpenCollection: () -> Unit = {},
    equippedAvatar: Cosmetic? = null,
    equippedFrame: Cosmetic? = null,
    equippedBackground: Cosmetic? = null,
    equippedBadge: Cosmetic? = null,
) {
    val activity = LocalActivity()
    val authState by vm.authState.collectAsStateWithLifecycle()
    val syncStatus by vm.syncStatus.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> vm.onAuthorizationResult(result.data) }
    fun signIn() { activity?.let { act -> vm.signIn(act) { authLauncher.launch(it) } } }

    var soon by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    val signedIn = authState as? AuthState.SignedIn
    // Nombre de Usuario editable (fuente de verdad); si aún no se ha definido, se propone el de la
    // cuenta Google como valor inicial, y en último término "Entrenador".
    val playerName = stats.username.ifBlank { signedIn?.name ?: "Entrenador" }
    var editingName by remember { mutableStateOf(false) }

    // Código de amigo real (12 dígitos) + copiar al portapapeles con confirmación transitoria.
    val rawCode = stats.friendCode
    val idText = if (rawCode.length == 12)
        "${rawCode.substring(0, 4)} ${rawCode.substring(4, 8)} ${rawCode.substring(8, 12)}"
    else "— — — —"
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(copied) {
        if (copied) { kotlinx.coroutines.delay(1500); copied = false }
    }

    var showStats by remember { mutableStateOf(false) }

    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to PocketPalette.HeaderTop,
                0.28f to PocketPalette.Body,
                1f to PocketPalette.Body,
            ),
        ),
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).fadingScrollbar(scroll),
        ) {
            // ---- Cabecera: iniciar sesión (izq) + ID de amigo + lupa (der) ----
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SignInChip(signedIn = signedIn != null, onClick = { if (signedIn == null) signIn() else showStats = true })
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("ID de amigo", color = PocketPalette.Muted, fontSize = 11.sp)
                    Text(idText, color = PocketPalette.Ink, fontWeight = FontWeight.Black, fontSize = 15.sp,
                        modifier = Modifier.pressable {
                            if (rawCode.length == 12) {
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(rawCode)); copied = true
                            }
                        })
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(PocketPalette.Surface).pressable { soon = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Search, "Buscar amigo", tint = PocketPalette.Icon, modifier = Modifier.size(20.dp)) }
            }

            // ---- Anillo de nivel + avatar circular ----
            Spacer(Modifier.height(4.dp))
            LevelAvatar(
                avatarColors = equippedAvatar.brushColors(listOf(Color(0xFFB39DDB), Color(0xFF9575CD))),
                fraction = 0f,               // pendiente sistema de nivel real
                onEditAvatar = { soon = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(12.dp))
            Text("Nv. 1", color = PocketPalette.Ink, fontWeight = FontWeight.Black, fontSize = 22.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("0 / 100", color = PocketPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally))

            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(14.dp))
                NamePill(playerName, onEdit = { editingName = true })
                Spacer(Modifier.height(10.dp))
                StatusPill("¡Echemos una partida!", onClick = { soon = true })

                Spacer(Modifier.height(18.dp))
                StatsButton(onClick = { showStats = true }, modifier = Modifier.align(Alignment.CenterHorizontally))

                Spacer(Modifier.height(20.dp))
                // ---- Emblemas ----
                Row(
                    Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Emblemas", color = PocketPalette.Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    AlertBadge()
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    EmblemHex(listOf(Color(0xFF8E7BE0), Color(0xFF5B3FB0)), onClick = { soon = true })
                    EmblemHex(listOf(Color(0xFF3FD69C), Color(0xFF1E9B72)), onClick = { soon = true })
                    EmblemHex(listOf(Color(0xFFB8C0CC), Color(0xFF7E8794)), onClick = { soon = true })
                }

                Spacer(Modifier.height(22.dp))
                CenterLabel("Selección personal")
                Spacer(Modifier.height(12.dp))
                Box(Modifier.align(Alignment.CenterHorizontally)) { CardSlot(width = 150.dp, onClick = { soon = true }) }

                Spacer(Modifier.height(22.dp))
                CenterLabel("Cartas en la colección")
                Spacer(Modifier.height(12.dp))
                CountPill(stats.totalOwned, Modifier.align(Alignment.CenterHorizontally))

                Spacer(Modifier.height(22.dp))
                CenterLabel("Mensaje de intercambio")
                Spacer(Modifier.height(12.dp))
                TradeDropdown("Sin preferencia", onClick = { soon = true })
                Spacer(Modifier.height(10.dp))
                TradeDropdown("Acepto cartas en cualquier idioma", onClick = { soon = true })

                Spacer(Modifier.height(22.dp))
                CenterLabel("Lista de deseadas")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { CardSlot(modifier = Modifier.weight(1f), onClick = { soon = true }) }
                }

                Spacer(Modifier.height(22.dp))
                CenterLabel("Resumen rápido")
                Spacer(Modifier.height(12.dp))
                QuickSummary(stats)

                Spacer(Modifier.height(22.dp))
                CenterLabel("Logros")
                Spacer(Modifier.height(14.dp))
                TrophyGrid(onClick = { soon = true })
                Spacer(Modifier.height(90.dp))
            }
            Spacer(Modifier.navigationBarsPadding())
        }

        // ---- X flotante de cierre (fija abajo-centro) ----
        Box(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 22.dp)
                .size(56.dp).clip(CircleShape).background(PocketPalette.Surface).pressable(onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Close, "Cerrar", tint = PocketPalette.Icon, modifier = Modifier.size(26.dp)) }

        // Confirmación transitoria al copiar el código de amigo.
        androidx.compose.animation.AnimatedVisibility(
            visible = copied,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp).navigationBarsPadding(),
        ) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(BarajasPalette.Ink.copy(alpha = 0.9f))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.ContentCopy, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Código de amigo copiado", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showStats) StatsSheet(
        authState = authState,
        syncStatus = syncStatus,
        onSignIn = { signIn() },
        onRetry = { vm.retry() },
        onSoon = { soon = true },
        onDismiss = { showStats = false },
    )
    if (soon) NotAvailableDialog(onClose = { soon = false })
    if (editingName) EditNameDialog(
        current = stats.username,
        onDismiss = { editingName = false },
        onSave = { vm.setUsername(it); editingName = false },
    )
}

/** Diálogo para editar el Nombre de Usuario. Limita a 16 caracteres; el vacío no se guarda. */
@Composable
private fun EditNameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(20.dp)).background(BarajasPalette.BgBottom).padding(22.dp),
        ) {
            Text("Nombre de Usuario", color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Se mostrará en tu perfil, el menú y tus partidas.",
                color = BarajasPalette.Muted, fontSize = 12.sp,
            )
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 16) text = it },
                singleLine = true,
                supportingText = { Text("${text.length}/16") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "Cancelar", color = BarajasPalette.Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.pressable(onDismiss).padding(horizontal = 14.dp, vertical = 8.dp),
                )
                Spacer(Modifier.width(8.dp))
                val enabled = text.isNotBlank()
                Text(
                    "Guardar",
                    color = if (enabled) Color.White else Color(0x66FFFFFF),
                    fontWeight = FontWeight.Black, fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (enabled) BarajasPalette.Ink else BarajasPalette.Ink.copy(alpha = 0.4f))
                        .then(if (enabled) Modifier.pressable { onSave(text.trim()) } else Modifier)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cabecera.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    name: String,
    idText: String,
    stats: ProfileStats,
    avatar: Cosmetic?, frame: Cosmetic?, background: Cosmetic?, badge: Cosmetic?,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onEditAvatar: () -> Unit,
    onChangeTitle: () -> Unit,
    onEditName: () -> Unit,
    onCopyId: () -> Unit,
) {
    val bg = background.brushColors(listOf(Accent, AccentDark))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.verticalGradient(bg)),
    ) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Fila superior: atrás · monedas · ajustes.
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, onBack)
                Spacer(Modifier.width(10.dp))
                CurrencyChip(Icons.Outlined.Diamond, stats.cristales, ChipBlue)
                Spacer(Modifier.width(8.dp))
                CurrencyChip(Icons.Outlined.Paid, stats.monedas, ChipGold)
                Spacer(Modifier.width(8.dp))
                CurrencyChip(Icons.Outlined.Toll, stats.fichas, ChipViolet)
                Spacer(Modifier.weight(1f))
                CircleIconButton(Icons.Outlined.Settings, onSettings, bg = Color.White, tint = Accent)
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar hexagonal + marco + lápiz.
                Box(contentAlignment = Alignment.BottomEnd) {
                    val avatarColors = avatar.brushColors(listOf(Color(0xFFE9EDF2), Color(0xFFB9C2CC)))
                    val frameColors = frame.brushColors(listOf(ChipGold, Color(0xFFB07A18)))
                    Box(
                        Modifier.size(96.dp).clip(HexagonShape())
                            .background(Brush.verticalGradient(avatarColors))
                            .border(4.dp, Brush.verticalGradient(frameColors), HexagonShape()),
                    )
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(Color.White)
                            .pressable(onEditAvatar),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Edit, "Editar avatar", tint = Accent, modifier = Modifier.size(16.dp)) }
                    if (badge != null) {
                        Box(
                            Modifier.align(Alignment.TopStart).size(26.dp).clip(CircleShape)
                                .background(Brush.verticalGradient(badge.brushColors(listOf(ChipGold, Color(0xFFB07A18)))))
                                .border(2.dp, Color(0x66FFFFFF), CircleShape),
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.pressable(onEditName),
                    ) {
                        Text(name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, maxLines = 1)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Edit, "Editar nombre", tint = Color(0xCCFFFFFF), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.pressable(onCopyId),
                    ) {
                        Text("ID: #$idText", color = Color(0xCCFFFFFF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.ContentCopy, "Copiar ID", tint = Color(0xCCFFFFFF), modifier = Modifier.size(13.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WorkspacePremium, null, tint = ChipGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Entrenador", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Perfil nuevo", color = Color(0xAAFFFFFF), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).border(1.5.dp, Color(0x88FFFFFF), RoundedCornerShape(50))
                            .pressable(onChangeTitle).padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cambiar título", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, onClick: () -> Unit, bg: Color = Color(0x33FFFFFF), tint: Color = Color.White) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(bg).pressable(onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun CurrencyChip(icon: ImageVector, value: Int, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color(0x33000000)).padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text("$value", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjeta de estadísticas.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsCard(onSoon: () -> Unit) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Nivel + XP.
            Column(Modifier.weight(1.2f)) {
                Text("NIVEL", color = BarajasPalette.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bolt, null, tint = ChipBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("1", color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 24.sp)
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(BarajasPalette.Hollow)) {
                    Box(Modifier.fillMaxWidth(0.05f).height(6.dp).clip(RoundedCornerShape(50)).background(ChipBlue))
                }
                Spacer(Modifier.height(4.dp))
                Text("0 / 100 XP", color = BarajasPalette.Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            StatCol(Icons.Outlined.EmojiEvents, ChipGold, "VICTORIAS", "0")
            StatCol(Icons.Outlined.Style, ChipBlue, "CARTAS", "0")
            StatCol(Icons.Outlined.MilitaryTech, Color(0xFF35C48A), "LOGROS", "0")
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StatCol(icon: ImageVector, tint: Color, label: String, value: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = BarajasPalette.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Info de cuenta.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountInfoCard(
    authState: AuthState,
    syncStatus: SyncStatus,
    onOnlineClick: () -> Unit,
    onSoon: () -> Unit,
) {
    val signedIn = authState is AuthState.SignedIn
    val (dotColor, title, subtitle) = when {
        !signedIn -> Triple(Color(0xFFB0B6C2), "Sin conexión", "Toca para iniciar sesión")
        syncStatus is SyncStatus.Syncing -> Triple(ChipGold, "Sincronizando…", "Guardando en la nube")
        syncStatus is SyncStatus.Error -> Triple(Color(0xFFE57373), "Error de sincronización", "Toca para reintentar")
        else -> Triple(Color(0xFF35C48A), "EN LÍNEA", "Conectado")
    }
    val now = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale("es"))) }
    val region = remember { Locale.getDefault().displayCountry.ifBlank { "—" } }
    val idioma = remember { Locale.getDefault().displayLanguage.replaceFirstChar { it.uppercase() } }

    Card {
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            // Estado de conexión (fila destacada).
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).pressable(onOnlineClick).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(dotColor))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = BarajasPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(subtitle, color = BarajasPalette.Muted, fontSize = 11.sp)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = BarajasPalette.Muted, modifier = Modifier.size(20.dp))
            }
            Divider()
            InfoRow(Icons.Outlined.CalendarMonth, "Miembro desde", "—")
            Divider()
            InfoRow(Icons.Outlined.Place, "Región", region)
            Divider()
            InfoRow(Icons.Outlined.Language, "Idioma", idioma)
            Divider()
            InfoRow(Icons.Outlined.Schedule, "Hora local", now)
            Divider()
            InfoRow(Icons.Outlined.Person, "Sobre mí", "Añade una descripción", onClick = onSoon, chevron = true)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null, chevron: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.pressable(onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = BarajasPalette.NavIcon, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = BarajasPalette.DeckName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(value, color = BarajasPalette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (chevron) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.ChevronRight, null, tint = BarajasPalette.Muted, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Personalización.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PersonalizationGrid(onSoon: () -> Unit) {
    val items = listOf(
        PersoItem(Icons.Outlined.Person, "Avatar", "Elige tu avatar", ChipViolet),
        PersoItem(Icons.Outlined.Hexagon, "Marco", "Cambia tu marco", ChipBlue),
        PersoItem(Icons.Outlined.Image, "Fondo", "Cambia tu fondo", Color(0xFF35C48A)),
        PersoItem(Icons.Outlined.WorkspacePremium, "Título", "Tu distintivo", ChipGold),
        PersoItem(Icons.Outlined.Stars, "Insignias", "Edita tus insignias", Color(0xFFEC5F94)),
        PersoItem(Icons.Outlined.Lock, "Privacidad", "Info pública", BarajasPalette.NavIcon),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item -> PersonalizationCard(item, Modifier.weight(1f), onSoon) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class PersoItem(val icon: ImageVector, val title: String, val subtitle: String, val accent: Color)

@Composable
private fun PersonalizationCard(item: PersoItem, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(BarajasPalette.Surface)
            .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(18.dp))
            .pressable(onClick)
            .padding(14.dp),
    ) {
        Icon(Icons.Outlined.ChevronRight, null, tint = BarajasPalette.Muted,
            modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
        Column {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(item.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(item.icon, null, tint = item.accent, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.height(10.dp))
            Text(item.title, color = BarajasPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(item.subtitle, color = BarajasPalette.Muted, fontSize = 11.sp, maxLines = 1)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Resumen rápido (datos reales).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickSummary(stats: ProfileStats) {
    val cells = listOf(
        Triple(Icons.Outlined.Style, "${stats.totalOwned}", "Cartas en colección"),
        Triple(Icons.Outlined.Category, "${stats.distinctOwned}", "Cartas diferentes"),
        Triple(Icons.Outlined.Inventory2, "${stats.expansions}", "Expansiones"),
    )
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCell(Modifier.weight(1f), cells[0].first, cells[0].second, cells[0].third, ChipBlue)
                SummaryCell(Modifier.weight(1f), cells[1].first, cells[1].second, cells[1].third, ChipViolet)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCell(Modifier.weight(1f), cells[2].first, cells[2].second, cells[2].third, ChipGold)
                // Índice con anillo.
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(BarajasPalette.BgBottom).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProgressRing(stats.indexPct / 100f, Modifier.size(44.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("${"%.1f".format(stats.indexPct)}%", color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Índice", color = BarajasPalette.Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCell(modifier: Modifier, icon: ImageVector, value: String, label: String, accent: Color) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(BarajasPalette.BgBottom).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(label, color = BarajasPalette.Muted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ProgressRing(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 6.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(BarajasPalette.HairlineBorder, -90f, 360f, false,
            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
        drawArc(Accent, -90f, 360f * fraction.coerceIn(0f, 1f), false,
            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Accesos.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccessTiles(onOpenCollection: () -> Unit, onSoon: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccessTile(Icons.Outlined.Style, "Colección", "Explora tus cartas",
            listOf(Color(0xFF2E9E6B), Color(0xFF1E7A50)), onOpenCollection)
        AccessTile(Icons.Outlined.Museum, "Museo", "Muestra tus favoritas",
            listOf(Color(0xFF7E4FD0), Color(0xFF5B2FA0)), onSoon)
        AccessTile(Icons.Outlined.MenuBook, "Legado", "Tu historia en el juego",
            listOf(Color(0xFFD08A3A), Color(0xFFA85F1E)), onSoon)
    }
}

@Composable
private fun AccessTile(icon: ImageVector, title: String, subtitle: String, gradient: List<Color>, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(gradient)).pressable(onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(subtitle, color = Color(0xDDFFFFFF), fontSize = 12.sp)
        }
        Icon(icon, null, tint = Color(0x55FFFFFF), modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ChevronRight, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Comunes.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Card(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BarajasPalette.Surface)
            .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(20.dp)),
    ) { content() }
}

@Composable
private fun SectionHeader(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(HexagonShape()).background(Accent))
        Spacer(Modifier.width(8.dp))
        Text(text, color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(BarajasPalette.HairlineBorder.copy(alpha = 0.6f)))
}

/** Clic con feedback de pulsación: la vista se comprime ligeramente al tocar (coherencia táctil). */
@Composable
private fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(), label = "pressScale")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

@Composable
private fun NotAvailableDialog(onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.clip(RoundedCornerShape(20.dp)).background(BarajasPalette.Surface).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Lock, null, tint = Accent, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Esta función no está disponible", color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("Estará lista más adelante.", color = BarajasPalette.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Accent).pressable(onClose).padding(horizontal = 28.dp, vertical = 10.dp),
            ) { Text("Vale", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
        }
    }
}

/** Convierte la paleta ARGB (Long) de un cosmético en colores Compose (fallback si vacía). */
private fun Cosmetic?.brushColors(fallback: List<Color>): List<Color> =
    this?.colors?.takeIf { it.isNotEmpty() }?.map { Color(it) } ?: fallback

/** Desenvuelve la Activity desde el Context de Compose. */
@Composable
private fun LocalActivity(): Activity? {
    var ctx: Context? = androidx.compose.ui.platform.LocalContext.current
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// ═════════════════════════════════════════════════════════════════════════════
//  DESIGN SYSTEM "TCG POCKET" — Perfil de Jugador (réplica UI/UX)
//  Neumórfico claro y frío: fondo continuo, elementos flotantes con sombra suave,
//  acento cian-teal para el anillo de nivel y las barras de progreso.
// ═════════════════════════════════════════════════════════════════════════════

private object PocketPalette {
    val HeaderTop = Color(0xFFDCE0F0)
    val Body = Color(0xFFECEFF6)
    val Surface = Color(0xFFF6F8FC)
    val Inset = Color(0xFFE3E8F1)
    val Ink = BarajasPalette.Ink
    val Muted = Color(0xFF8A97A8)
    val Icon = Color(0xFF6B7A90)
    val Teal = Color(0xFF3ACFDB)
    val TealDeep = Color(0xFF23A7C4)
    val Gold = Color(0xFFE8C15A)
    val GoldDeep = Color(0xFFB98D2E)
    val Alert1 = Color(0xFFFF4D6D)
    val Alert2 = Color(0xFFFF8AA6)
}

/** Botón/estado de inicio de sesión (cabecera izquierda). */
@Composable
private fun SignInChip(signedIn: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(PocketPalette.Surface).pressable(onClick)
            .padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (signedIn) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF35C48A)))
            Spacer(Modifier.width(8.dp))
            Text("En línea", color = PocketPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        } else {
            Icon(Icons.Outlined.Login, null, tint = PocketPalette.TealDeep, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Iniciar sesión", color = PocketPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

/** Avatar circular con anillo de progreso de nivel (cian) y lápiz de edición. */
@Composable
private fun LevelAvatar(
    avatarColors: List<Color>,
    fraction: Float,
    onEditAvatar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(168.dp), contentAlignment = Alignment.Center) {
        // Anillo (pista + progreso), con hueco superior.
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val start = 130f
            val total = 280f
            drawArc(
                PocketPalette.Inset, start, total, false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                Brush.sweepGradient(listOf(PocketPalette.Teal, PocketPalette.TealDeep, PocketPalette.Teal)),
                start, total * fraction.coerceIn(0f, 1f), false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        // Avatar circular (recorte del cosmético equipado; futuro: todos circulares).
        Box(
            Modifier.size(128.dp).clip(CircleShape).background(Brush.linearGradient(avatarColors)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Person, null, tint = Color(0x55FFFFFF), modifier = Modifier.size(64.dp)) }
        // Lápiz de edición (FAB abajo-derecha, sobre el anillo).
        Box(
            Modifier.align(Alignment.BottomEnd).size(38.dp).clip(CircleShape)
                .background(PocketPalette.Surface).pressable(onEditAvatar),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Edit, "Editar avatar", tint = PocketPalette.Icon, modifier = Modifier.size(18.dp)) }
    }
}

/** Píldora del Nombre de Usuario (inset) con lápiz de edición. */
@Composable
private fun NamePill(name: String, onEdit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(PocketPalette.Inset)
            .pressable(onEdit).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(20.dp))
        Text(name, color = PocketPalette.Ink, fontWeight = FontWeight.Black, fontSize = 20.sp,
            maxLines = 1, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Icon(Icons.Outlined.Edit, "Editar nombre", tint = PocketPalette.Icon, modifier = Modifier.size(18.dp))
    }
}

/** Mensaje de estado con borde de gradiente arcoíris sutil. */
@Composable
private fun StatusPill(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color(0xFFFFB3C6), Color(0xFFFFD6A5), Color(0xFFCDE8B5),
                        Color(0xFFA5D8E8), Color(0xFFC9B3E8), Color(0xFFFFB3C6),
                    ),
                ),
            )
            .padding(2.dp)
            .clip(RoundedCornerShape(50))
            .background(PocketPalette.Surface)
            .pressable(onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = PocketPalette.Icon, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** Botón circular "Estadísticas" (abre la hoja inferior). */
@Composable
private fun StatsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(58.dp).clip(CircleShape).background(PocketPalette.Surface).pressable(onClick),
            contentAlignment = Alignment.Center,
        ) { StatsGlyph() }
        Spacer(Modifier.height(6.dp))
        Text("Estadísticas", color = PocketPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Glifo de Estadísticas: dos cartas/burbujas cian solapadas. */
@Composable
private fun StatsGlyph() {
    Canvas(Modifier.size(30.dp)) {
        val w = size.width; val h = size.height
        val c = PocketPalette.Teal
        val st = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        drawRoundRect(c, topLeft = Offset(w * 0.10f, h * 0.20f), size = Size(w * 0.50f, h * 0.44f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f), style = st)
        val tail = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.55f, h * 0.30f); lineTo(w * 0.90f, h * 0.30f)
            lineTo(w * 0.90f, h * 0.74f); lineTo(w * 0.72f, h * 0.74f)
            lineTo(w * 0.66f, h * 0.86f); lineTo(w * 0.62f, h * 0.74f)
            lineTo(w * 0.55f, h * 0.74f); close()
        }
        drawPath(tail, c, style = st)
    }
}

/** Badge de aviso "!" (gradiente rosa-rojo). */
@Composable
private fun AlertBadge() {
    Box(
        Modifier.size(22.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(PocketPalette.Alert2, PocketPalette.Alert1))),
        contentAlignment = Alignment.Center,
    ) { Text("!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
}

/** Emblema hexagonal: bisel dorado + fondo de gema facetada (placeholder de arte). */
@Composable
private fun EmblemHex(gem: List<Color>, onClick: () -> Unit) {
    Box(
        Modifier.size(88.dp).pressable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Bisel dorado.
        Box(
            Modifier.size(88.dp).clip(HexagonShape())
                .background(Brush.verticalGradient(listOf(PocketPalette.Gold, PocketPalette.GoldDeep))),
        )
        // Gema interior facetada (dos triángulos de tono para simular facetas).
        Box(Modifier.size(72.dp).clip(HexagonShape()).background(Brush.linearGradient(gem))) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val hi = Color(0x33FFFFFF)
                drawPath(
                    androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.5f, 0f); lineTo(w, h * 0.28f); lineTo(w * 0.5f, h * 0.5f); close()
                    }, hi,
                )
                drawPath(
                    androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h * 0.28f); lineTo(w * 0.5f, h * 0.5f); lineTo(0f, h * 0.72f); close()
                    }, Color(0x22000000),
                )
            }
        }
    }
}

/** Etiqueta de sección centrada (patrón del design system). */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.CenterLabel(text: String) {
    Text(
        text, color = PocketPalette.Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
}

/** Slot de carta vacío con "+" (Selección personal / Lista de deseadas). */
@Composable
private fun CardSlot(modifier: Modifier = Modifier, width: androidx.compose.ui.unit.Dp? = null, onClick: () -> Unit) {
    val base = if (width != null) modifier.width(width) else modifier
    Box(
        base.aspectRatio(0.72f).clip(RoundedCornerShape(12.dp)).background(PocketPalette.Inset)
            .border(1.dp, PocketPalette.Muted.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .pressable(onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Add, "Añadir", tint = PocketPalette.Muted, modifier = Modifier.size(30.dp)) }
}

/** Píldora de recuento de colección. */
@Composable
private fun CountPill(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(PocketPalette.Inset)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Style, null, tint = PocketPalette.Icon, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Text("$count", color = PocketPalette.Ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

/** Desplegable de preferencia de intercambio. */
@Composable
private fun TradeDropdown(text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(PocketPalette.Surface)
            .pressable(onClick).padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = PocketPalette.Icon, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Icon(Icons.Outlined.KeyboardArrowDown, null, tint = PocketPalette.Muted, modifier = Modifier.size(22.dp))
    }
}

/** Rejilla de Logros: pedestales con trofeo placeholder + barra de progreso teal. */
@Composable
private fun TrophyGrid(onClick: () -> Unit) {
    // Progreso simulado por celda (placeholder hasta el sistema real de logros).
    val progress = listOf(0.4f, 1f, 0.2f, 0.7f, 1f, 0.5f, 0.3f, 1f, 0.6f, 0.9f, 0.2f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        progress.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { p -> TrophyCell(p, Modifier.weight(1f), onClick) }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TrophyCell(fraction: Float, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.pressable(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomCenter) {
            // Base/pedestal.
            Box(Modifier.padding(top = 26.dp).size(width = 46.dp, height = 16.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFF4F7FC), Color(0xFFDBE2EC)))))
            // Trofeo (placeholder: copa dorada simple).
            TrophyGlyph()
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.8f).height(6.dp).clip(RoundedCornerShape(50)).background(PocketPalette.Inset)) {
            Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(PocketPalette.Teal, PocketPalette.TealDeep))))
        }
    }
}

@Composable
private fun TrophyGlyph() {
    Canvas(Modifier.size(38.dp)) {
        val w = size.width; val h = size.height
        val gold = Brush.verticalGradient(listOf(Color(0xFFF6D879), Color(0xFFCC9B33)))
        // Copa.
        drawRoundRect(gold, topLeft = Offset(w * 0.28f, h * 0.14f), size = Size(w * 0.44f, h * 0.40f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f))
        // Asas.
        val st = Stroke(width = w * 0.06f)
        drawArc(Color(0xFFCC9B33), 90f, 180f, false, topLeft = Offset(w * 0.14f, h * 0.16f),
            size = Size(w * 0.20f, h * 0.28f), style = st)
        drawArc(Color(0xFFCC9B33), -90f, 180f, false, topLeft = Offset(w * 0.66f, h * 0.16f),
            size = Size(w * 0.20f, h * 0.28f), style = st)
        // Pie.
        drawRoundRect(Color(0xFFB98D2E), topLeft = Offset(w * 0.44f, h * 0.54f), size = Size(w * 0.12f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f))
    }
}

/** Hoja inferior de Estadísticas: datos de cuenta (Miembro desde, Región, Idioma, Hora local, Sobre mí). */
@Composable
private fun StatsSheet(
    authState: AuthState,
    syncStatus: SyncStatus,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    onSoon: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color(0x40222B38))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(PocketPalette.Body)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Box(Modifier.size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(50))
                    .background(PocketPalette.Muted.copy(alpha = 0.4f)).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(14.dp))
                Text("Estadísticas", color = PocketPalette.Ink, fontWeight = FontWeight.Black, fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(14.dp))
                AccountInfoCard(
                    authState = authState,
                    syncStatus = syncStatus,
                    onOnlineClick = { if (authState !is AuthState.SignedIn) onSignIn() else if (syncStatus is SyncStatus.Error) onRetry() else onSoon() },
                    onSoon = onSoon,
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
