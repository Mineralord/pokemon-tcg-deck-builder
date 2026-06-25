package com.mineralord.tcg.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.data.cloud.AuthState
import com.mineralord.tcg.data.cloud.SyncStatus

/**
 * Pantalla de Perfil: inicio de sesión con Google y estado del guardado en la
 * nube (Google Drive). Se monta dentro del SubScreen (fondo navy).
 */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier, vm: ProfileViewModel = viewModel()) {
    val activity = LocalActivity()
    val authState by vm.authState.collectAsStateWithLifecycle()
    val syncStatus by vm.syncStatus.collectAsStateWithLifecycle()
    val lastSynced by vm.lastSynced.collectAsStateWithLifecycle(initialValue = 0L)

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> vm.onAuthorizationResult(result.data) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TcgColors.Navy)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        when (val state = authState) {
            is AuthState.SignedOut -> SignedOutContent(
                onSignIn = { activity?.let { act -> vm.signIn(act) { authLauncher.launch(it) } } },
            )
            is AuthState.SignedIn -> SignedInContent(
                state = state,
                syncStatus = syncStatus,
                lastSynced = lastSynced,
                onRetry = vm::retry,
                onSignOut = vm::signOut,
            )
        }
    }
}

@Composable
private fun SignedOutContent(onSignIn: () -> Unit) {
    AvatarCircle(url = null)
    Spacer(Modifier.height(20.dp))
    Text(
        "Guarda tu progreso en la nube",
        color = TcgColors.Parchment,
        fontWeight = FontWeight.Black,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Inicia sesión con tu cuenta de Google para respaldar tu colección y barajas, y restaurarlas en cualquier dispositivo.",
        color = TcgColors.Parchment.copy(alpha = 0.7f),
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp,
    )
    Spacer(Modifier.height(28.dp))
    PrimaryButton(text = "Iniciar sesión con Google", onClick = onSignIn)
}

@Composable
private fun SignedInContent(
    state: AuthState.SignedIn,
    syncStatus: SyncStatus,
    lastSynced: Long,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    AvatarCircle(url = state.avatarUrl)
    Spacer(Modifier.height(16.dp))
    Text(
        state.name ?: "Cuenta de Google",
        color = TcgColors.Parchment,
        fontWeight = FontWeight.Black,
        fontSize = 18.sp,
    )
    state.email?.let {
        Spacer(Modifier.height(2.dp))
        Text(it, color = TcgColors.Parchment.copy(alpha = 0.7f), fontSize = 13.sp)
    }

    Spacer(Modifier.height(24.dp))
    SyncStatusRow(syncStatus = syncStatus, lastSynced = lastSynced, onRetry = onRetry)

    Spacer(Modifier.height(28.dp))
    SecondaryButton(text = "Cerrar sesión", onClick = onSignOut)
}

@Composable
private fun SyncStatusRow(syncStatus: SyncStatus, lastSynced: Long, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TcgColors.NavyLight)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (syncStatus) {
            is SyncStatus.Syncing -> {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TcgColors.Gold, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Sincronizando…", color = TcgColors.Parchment, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            is SyncStatus.Error -> {
                StatusDot(Color(0xFFE57373))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Error de sincronización", color = TcgColors.Parchment, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Toca para reintentar", color = TcgColors.Parchment.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                Text(
                    "Reintentar",
                    color = TcgColors.Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onRetry),
                )
            }
            else -> {
                StatusDot(Color(0xFF81C784))
                Spacer(Modifier.width(12.dp))
                Text(
                    if (lastSynced > 0L) "Sincronizado ${relativeTime(lastSynced)}" else "Sincronizado",
                    color = TcgColors.Parchment,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(Modifier.size(12.dp).clip(CircleShape).background(color))
}

@Composable
private fun AvatarCircle(url: String?) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(TcgColors.NavyLight)
            .border(3.dp, TcgColors.Gold, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = "Avatar",
                modifier = Modifier.size(96.dp).clip(CircleShape),
            )
        } else {
            Box(Modifier.size(44.dp).clip(CircleShape).background(TcgColors.Gold.copy(alpha = 0.4f)))
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(TcgColors.Gold)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = TcgColors.Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, TcgColors.Parchment.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = TcgColors.Parchment, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

private fun relativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000L -> "hace un momento"
        diff < 3_600_000L -> "hace ${diff / 60_000L} min"
        diff < 86_400_000L -> "hace ${diff / 3_600_000L} h"
        else -> "hace ${diff / 86_400_000L} d"
    }
}

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
