package com.mineralord.tcg.app

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineralord.tcg.data.cloud.AuthState
import kotlinx.coroutines.launch

/** Desenvuelve la Activity desde el Context de Compose (para lanzar el selector de cuentas). */
private fun Any?.asActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * Pantalla de BIENVENIDA al arrancar. Detecta si ya hay una sesión (o una cuenta recordada) y, de
 * ser así, no aparece. Si NO hay sesión ni se eligió antes jugar como invitado, invita a entrar con
 * Google (recomendado, para guardar la colección en la nube) u ofrece continuar como invitado,
 * advirtiendo que su progreso no se guardará. La elección se recuerda para no preguntar cada vez.
 *
 * Tiene MENOR prioridad que el aviso de actualización: si hay versión nueva obligatoria, ese tapa
 * a este (se dibuja encima en AppShell).
 */
@Composable
fun WelcomeGate() {
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.asActivity() }
    val app = remember(ctx) { ctx.applicationContext as TcgApplication }
    val cloud = app.cloudSync
    val vm = viewModel<ProfileViewModel>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val authState by vm.authState.collectAsStateWithLifecycle()
    val syncStatus by vm.syncStatus.collectAsStateWithLifecycle()

    // null = aún decidiendo si mostrar; true/false = decisión tomada.
    var visible by remember { mutableStateOf<Boolean?>(null) }
    var signingIn by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> vm.onAuthorizationResult(result.data) }

    // Decide si mostrar la bienvenida: oculta si ya hay sesión, cuenta recordada o invitado elegido.
    LaunchedEffect(Unit) {
        if (authState is AuthState.SignedIn) { visible = false; return@LaunchedEffect }
        val remembered = runCatching { cloud.hasRememberedSession() }.getOrDefault(false)
        val guest = runCatching { cloud.isGuestChosen() }.getOrDefault(false)
        visible = !remembered && !guest
    }
    // Si en cualquier momento se confirma la sesión, desaparece.
    LaunchedEffect(authState) { if (authState is AuthState.SignedIn) visible = false }
    // Feedback de error mientras se intenta entrar. Si el usuario simplemente cerró el selector de
    // cuentas (cancelación), no es un error: se resetea sin mensaje. En otros casos mostramos el
    // motivo REAL para poder diagnosticar (p. ej. huella SHA no registrada en Firebase).
    LaunchedEffect(syncStatus) {
        val st = syncStatus
        if (signingIn && st is com.mineralord.tcg.data.cloud.SyncStatus.Error) {
            signingIn = false
            val cause = st.cause
            val cancelled = cause is androidx.credentials.exceptions.GetCredentialCancellationException ||
                (cause.message?.contains("cancel", ignoreCase = true) == true)
            errorMsg = if (cancelled) null
            else "No se pudo iniciar sesión.\n${cause::class.simpleName}: ${cause.message ?: "sin detalle"}"
        }
    }

    if (visible != true) return

    androidx.activity.compose.BackHandler(enabled = true) { /* no-op: elección obligatoria */ }

    fun signInWithGoogle() {
        val act = activity ?: run { errorMsg = "No se pudo abrir el inicio de sesión."; return }
        errorMsg = null
        signingIn = true
        vm.signIn(act) { authLauncher.launch(it) }
    }

    fun continueAsGuest() {
        scope.launch { runCatching { cloud.chooseGuest() } }
        visible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Panel)
                .border(1.dp, Hairline, RoundedCornerShape(20.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(AccentHi, Accent))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AccountCircle, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text(
                "Te damos la bienvenida",
                color = Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Inicia sesión con Google para guardar tu colección, monedas y progreso en la nube " +
                    "y recuperarlos en cualquier dispositivo.",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            // CTA primario recomendado: entrar con Google.
            PrimaryButton(
                label = if (signingIn) "Entrando…" else "Entrar con Google",
                enabled = !signingIn,
                onClick = { signInWithGoogle() },
            )

            // Opción secundaria: invitado, con advertencia.
            GuestButton(enabled = !signingIn, onClick = { continueAsGuest() })

            Text(
                "Como invitado, tu progreso se guarda solo en este dispositivo y puede perderse si " +
                    "desinstalas la app o cambias de teléfono.",
                color = Muted.copy(alpha = 0.8f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp),
            )

            errorMsg?.let {
                Text(
                    it,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(AccentHi, Accent)))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
private fun GuestButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudOff, null, tint = Muted, modifier = Modifier.size(18.dp))
            Text(
                "Continuar como invitado",
                color = Ink.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// Paleta OFICIAL clara de la app (misma que UpdateGate/splash).
private val BgTop = Color(0xFFEDF1F8)
private val BgBottom = Color(0xFFDBE3EF)
private val Panel = Color(0xFFFFFFFF)
private val Hairline = Color(0xFFDCE6EF)
private val Ink = Color(0xFF2B3346)
private val Muted = Color(0xFF8A93A6)
private val Accent = Color(0xFF35C4E8)
private val AccentHi = Color(0xFF4FD3F2)
private val ErrorRed = Color(0xFFE0564E)
