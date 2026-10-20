package com.mineralord.tcg.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.FileProvider
import com.mineralord.tcg.data.netfirestore.AppVersionInfo
import com.mineralord.tcg.data.netfirestore.FirestoreAppVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** `versionCode` instalado de ESTA app (fuente de verdad local, sin depender de BuildConfig). */
private fun installedVersionCode(context: Context): Int = runCatching {
    val pm = context.packageManager
    val info: PackageInfo = pm.getPackageInfo(context.packageName, 0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt()
    else @Suppress("DEPRECATION") info.versionCode
}.getOrDefault(Int.MAX_VALUE) // Ante la duda, NO molestar (se asume al día).

/**
 * Normaliza el enlace a su forma de DESCARGA DIRECTA. Un enlace de Google Drive para compartir
 * (`.../file/d/<ID>/view`) sirve una página HTML, no el archivo; lo convertimos al endpoint de
 * descarga directa de Drive (válido para archivos < 100 MB, como nuestra APK). Cualquier otro
 * enlace (p. ej. Firebase Storage) se usa tal cual.
 */
private fun directDownloadUrl(raw: String): String {
    val id = Regex("/file/d/([^/]+)").find(raw)?.groupValues?.get(1)
        ?: Regex("[?&]id=([^&]+)").find(raw)?.groupValues?.get(1)
    return if (id != null) {
        "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=t"
    } else {
        raw
    }
}

/** Estado del flujo del aviso de actualización. */
private sealed interface GateState {
    object Prompt : GateState
    data class Downloading(val progress: Float) : GateState
    data class Error(val reason: String) : GateState
}

/**
 * Descarga la APK a la carpeta privada de la app, informando del progreso (0..1; -1 si el servidor
 * no da el tamaño). Sigue redirecciones de Drive. Devuelve el archivo descargado o lanza excepción.
 */
private fun updatesDir(context: Context): File =
    File(context.cacheDir, "updates")

/**
 * Borra cualquier APK de actualización que haya quedado en caché. Se llama AL ARRANCAR: así, en
 * cuanto la versión nueva se instala y la app se reabre, el archivo descargado se elimina y NO queda
 * almacenado en el dispositivo (vive solo entre la descarga y el siguiente arranque).
 */
private fun cleanupUpdates(context: Context) {
    runCatching { updatesDir(context).deleteRecursively() }
}

private suspend fun downloadApk(
    context: Context,
    url: String,
    onProgress: (Float) -> Unit,
): File = withContext(Dispatchers.IO) {
    val dir = updatesDir(context).apply { mkdirs() }
    val out = File(dir, "update.apk")
    if (out.exists()) out.delete()

    var current = URL(directDownloadUrl(url))
    var conn = (current.openConnection() as HttpURLConnection)
    // Sigue hasta 5 redirecciones (Drive encadena varias, a veces entre dominios).
    var redirects = 0
    while (true) {
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 20_000
        conn.readTimeout = 30_000
        conn.connect()
        val code = conn.responseCode
        if (code in 300..399 && redirects < 5) {
            val loc = conn.getHeaderField("Location") ?: break
            conn.disconnect()
            current = URL(URL(current, loc).toString())
            conn = (current.openConnection() as HttpURLConnection)
            redirects++
            continue
        }
        break
    }
    if (conn.responseCode !in 200..299) {
        throw IllegalStateException("HTTP ${conn.responseCode}")
    }
    val contentType = conn.contentType.orEmpty()
    if (contentType.startsWith("text/html")) {
        // Drive devolvió una página (archivo demasiado grande o sin acceso público): no es la APK.
        throw IllegalStateException("El enlace no entrega la APK directamente.")
    }
    val total = conn.contentLengthLong
    conn.inputStream.use { input ->
        out.outputStream().use { output ->
            val buffer = ByteArray(16 * 1024)
            var downloaded = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                downloaded += read
                onProgress(if (total > 0) downloaded.toFloat() / total else -1f)
            }
        }
    }
    conn.disconnect()
    out
}

/** Lanza el instalador del sistema para la APK ya descargada (el usuario confirma "Instalar"). */
private fun launchInstaller(context: Context, apk: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.updateprovider", apk)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

/** ¿Puede la app lanzar instalaciones de orígenes desconocidos? (Android 8+ lo pide por app.) */
private fun canInstall(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.packageManager.canRequestPackageInstalls()
    else true

/**
 * Aviso OBLIGATORIO de nueva versión: cubre TODA la app si hay una versión más reciente publicada
 * en `config/appVersion`. El botón descarga la APK DENTRO de la app (barra de progreso) y lanza el
 * instalador del sistema; el usuario solo confirma "Instalar" (sin navegador ni buscar el archivo).
 *
 * No se puede cerrar (sin "Más tarde", sin botón atrás): todos acaban en la última versión. Si no
 * hay red o el documento no existe, no aparece nada: la ausencia de aviso nunca bloquea el juego.
 */
@Composable
fun UpdateGate() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = remember { installedVersionCode(ctx) }
    var pending by remember { mutableStateOf<AppVersionInfo?>(null) }
    var state by remember { mutableStateOf<GateState>(GateState.Prompt) }

    LaunchedEffect(Unit) {
        // Limpia cualquier APK que haya quedado de una actualización previa (no debe persistir).
        cleanupUpdates(ctx)
        val repo = FirestoreAppVersionRepository()
        val latest = repo.fetchLatest() ?: return@LaunchedEffect
        if (latest.latestVersionCode > current) pending = latest
    }

    val info = pending ?: return
    // Bloqueo total: captura el botón atrás para que no se pueda esquivar el aviso.
    androidx.activity.compose.BackHandler(enabled = true) { /* no-op: aviso obligatorio */ }

    fun startDownload() {
        if (!canInstall(ctx)) {
            // Lleva a Ajustes a permitir instalar desde esta app; al volver, el usuario reintenta.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching {
                    ctx.startActivity(
                        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:${ctx.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
            state = GateState.Error("Activa \"Permitir de esta fuente\" y pulsa Reintentar.")
            return
        }
        state = GateState.Downloading(-1f)
        scope.launch {
            runCatching {
                val apk = downloadApk(ctx, info.downloadUrl) { p ->
                    state = GateState.Downloading(p)
                }
                launchInstaller(ctx, apk)
            }.onFailure {
                state = GateState.Error(it.message ?: "No se pudo descargar la actualización.")
            }
            // Tras lanzar el instalador volvemos al prompt: si el usuario cancela la instalación,
            // el aviso sigue ahí para reintentar.
            if (state is GateState.Downloading) state = GateState.Prompt
        }
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Emblema con el acento cian de la app (mismo del botón Buscar / interruptores).
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(AccentHi, Accent))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.SystemUpdateAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            Text(
                "Nueva versión disponible",
                color = Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            val subtitle = buildString {
                append("Hay una versión más reciente del juego")
                if (info.versionName.isNotBlank()) append(" (${info.versionName})")
                append(". Para seguir jugando, instala la actualización.")
            }
            Text(
                subtitle,
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (info.message.isNotBlank()) {
                Text(
                    info.message,
                    color = Muted.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            when (val s = state) {
                is GateState.Downloading -> {
                    Text(
                        if (s.progress >= 0f) "Descargando… ${(s.progress * 100).toInt()}%" else "Descargando…",
                        color = Ink.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                    if (s.progress >= 0f) {
                        LinearProgressIndicator(
                            progress = { s.progress },
                            color = Accent,
                            trackColor = TrackBg,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(6.dp),
                        )
                    } else {
                        LinearProgressIndicator(
                            color = Accent,
                            trackColor = TrackBg,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(6.dp),
                        )
                    }
                }
                is GateState.Error -> {
                    Text(
                        s.reason,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                    UpdateButton("Reintentar") { startDownload() }
                }
                GateState.Prompt -> {
                    UpdateButton("Actualizar") { startDownload() }
                }
            }
        }
    }
}

/** CTA primario con el gradiente cian de la app (mismo lenguaje que el botón "Buscar"/"Aplicar"). */
@Composable
private fun UpdateButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(AccentHi, Accent)))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            letterSpacing = 0.3.sp,
        )
    }
}

// Paleta OFICIAL clara de la app (fondo blanco-lila; ver HomeScreen/CollectionScreen).
private val BgTop = Color(0xFFEDF1F8)
private val BgBottom = Color(0xFFDBE3EF)
private val Panel = Color(0xFFFFFFFF)
private val Hairline = Color(0xFFDCE6EF)
private val Ink = Color(0xFF2B3346)
private val Muted = Color(0xFF8A93A6)
private val Accent = Color(0xFF35C4E8)      // cian TCG Live
private val AccentHi = Color(0xFF4FD3F2)
private val TrackBg = Color(0xFFE3EAF3)
private val ErrorRed = Color(0xFFE0564E)
