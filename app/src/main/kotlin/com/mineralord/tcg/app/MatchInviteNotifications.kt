package com.mineralord.tcg.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Notificaciones de **invitación a combate**. Crea un canal con el sonido "shiny" de Pokémon (si el
 * recurso `res/raw/shiny` existe; si no, el sonido por defecto) y publica un aviso que, al tocarse,
 * abre la app ENTRANDO DIRECTAMENTE a la sala del anfitrión (vía extras de intent en [MainActivity]).
 *
 * Nota: esto entrega el aviso mientras el proceso de la app sigue vivo (primer plano o segundo plano
 * reciente), que es cuando el listener de Firestore está activo. Para entrega con la app CERRADA del
 * todo haría falta Firebase Cloud Messaging (push real); queda pendiente.
 */
object MatchInviteNotifications {
    const val CHANNEL_ID = "match_invites"
    const val EXTRA_ROOM_CODE = "extra_room_code"
    const val EXTRA_FROM_UID = "extra_from_uid"
    const val EXTRA_FROM_NAME = "extra_from_name"

    /** Id de `res/raw/shiny` (0 si no se ha añadido el archivo). Resuelto por nombre para no depender
     *  en compilación de un asset que puede no estar presente. */
    private fun shinySound(ctx: Context): Uri? {
        val id = ctx.resources.getIdentifier("shiny", "raw", ctx.packageName)
        return if (id != 0) Uri.parse("android.resource://${ctx.packageName}/$id") else null
    }

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Invitaciones a combate",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisos cuando un amigo te invita a combatir"
            enableVibration(true)
            shinySound(ctx)?.let { uri ->
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(uri, attrs)
            }
        }
        mgr.createNotificationChannel(channel)
    }

    /** Publica la notificación de invitación de [fromName] a la sala [roomCode]. */
    fun show(ctx: Context, fromUid: String, fromName: String, roomCode: String) {
        ensureChannel(ctx)
        val tapIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROOM_CODE, roomCode)
            putExtra(EXTRA_FROM_UID, fromUid)
            putExtra(EXTRA_FROM_NAME, fromName)
        }
        val pending = PendingIntent.getActivity(
            ctx,
            fromUid.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("¡$fromName te reta a combatir!")
            .setContentText("Toca para entrar a la sala")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(pending)
        shinySound(ctx)?.let { builder.setSound(it) } // pre-O: el sonido va en la notificación
        runCatching {
            NotificationManagerCompat.from(ctx).notify(fromUid.hashCode(), builder.build())
        }
    }

    fun cancel(ctx: Context, fromUid: String) {
        NotificationManagerCompat.from(ctx).cancel(fromUid.hashCode())
    }
}
