package com.mineralord.tcg.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mineralord.tcg.data.gacha.PackRegen
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.data.profile.TrustedClock
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Notificaciones de sobres: avisa **cuando llega cada sobre** (regeneración). La programación se basa
 * en el TIEMPO CONFIABLE (no en el reloj del teléfono), así que adelantar la hora no dispara avisos
 * falsos: el Worker revalida con [TrustedClock] al ejecutarse y, si aún no ha llegado el sobre,
 * reprograma para el tiempo real restante.
 */
object PackNotifications {
    private const val CHANNEL_ID = "daily_packs"
    private const val WORK_NAME = "pack_ready"
    private const val NOTIF_ID = 4201

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ch = NotificationChannel(
            CHANNEL_ID, "Sobres diarios", NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Aviso cuando un sobre de mejora está listo" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    /** Programa el chequeo del PRÓXIMO sobre dentro de [delayMs] (tiempo real restante). */
    fun scheduleNext(context: Context, delayMs: Long) {
        val req = OneTimeWorkRequestBuilder<PackReadyWorker>()
            .setInitialDelay(delayMs.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, req)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** (Re)programa el aviso del siguiente sobre según el monedero y el reloj CONFIABLE actuales. */
    suspend fun refreshSchedule(context: Context, repo: ProfileRepository, clock: TrustedClock, regen: PackRegen) {
        val wallet = repo.wallet.first() ?: return
        val now = clock.nowMs()
        val next = regen.nextPackAt(regen.credited(wallet, now), now)
        if (next == null) cancel(context) else scheduleNext(context, next - now)
    }

    fun notifyPackReady(context: Context, available: Int, max: Int) {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (available >= max) "¡Tienes el máximo de $max sobres! Ábrelos antes de perder regeneración."
        else "Tienes $available sobre(s) de mejora listo(s) para abrir."
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("¡Sobre de mejora listo!")
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_ID, notif) }
    }
}

/**
 * Worker que se ejecuta al llegar (según el reloj CONFIABLE) el próximo sobre: acredita la
 * regeneración, persiste el monedero, avisa si aumentó el saldo y **reprograma** el siguiente. Si
 * se ejecutó antes de tiempo (p. ej. por sesgo del reloj), reprograma sin avisar.
 */
class PackReadyWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val repo = ProfileRepository(ctx)
        val clock = TrustedClock(ctx)
        val regen = PackRegen()
        val wallet = repo.wallet.first() ?: return Result.success()
        val now = clock.nowMs()
        val credited = regen.credited(wallet, now)
        if (credited.balance > wallet.balance) {
            repo.saveWallet(credited)
            PackNotifications.notifyPackReady(ctx, credited.balance, regen.maxPacks)
        }
        // Reprograma el siguiente sobre (o nada si está en el tope).
        val next = regen.nextPackAt(credited, now)
        if (next != null) PackNotifications.scheduleNext(ctx, next - now)
        return Result.success()
    }
}
