package com.pomodoro.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.*
import androidx.core.app.NotificationCompat
import com.pomodoro.app.R
import com.pomodoro.app.ui.MainActivity

object BeeAlertManager {

    const val CHANNEL_COMPLETE_ID      = "pomodoro_complete_channel"
    const val NOTIFICATION_COMPLETE_ID = 2

    private val BUZZ_PATTERN = longArrayOf(0, 500, 300, 500, 300, 800, 500)

    // ── Canal de notification ────────────────────────────────────────────────

    fun createCompletionChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Session Complete \uD83D\uDC1D",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifie quand une session se termine"
                enableVibration(true)
                vibrationPattern = BUZZ_PATTERN
                setSound(null, audioAttr)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    // ── Notification background ──────────────────────────────────────────────

    fun showCompletionNotification(context: Context, isFocusSession: Boolean) {
        val title = if (isFocusSession) "Session focus terminée ! \uD83D\uDC1D" else "Pause terminée ! \uD83D\uDC1D"

        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText("Let\u2019s bezzzzz \uD83D\uDC1D")
            .setStyle(
                androidx.core.app.NotificationCompat.BigTextStyle()
                    .bigText("Let\u2019s bezzzzz \uD83D\uDC1D\nAppuie pour continuer !")
            )
            .setColor(0xFFE7992C.toInt())
            .setColorized(true)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setVibrate(BUZZ_PATTERN)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_COMPLETE_ID, notification)
    }
}