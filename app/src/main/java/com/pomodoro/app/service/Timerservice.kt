package com.pomodoro.app.service

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import androidx.core.app.NotificationCompat
import com.pomodoro.app.R
import com.pomodoro.app.data.models.AmbientSound
import com.pomodoro.app.ui.MainActivity
import com.pomodoro.app.util.BeeAlertManager
import java.util.*

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID      = "pomodoro_timer_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_STOP  = "action_stop"
    }

    private val binder = TimerBinder()
    private var timer: Timer? = null
    private var timeLeft: Int = 0
    private var isRunning = false
    private var ambientPlayer: MediaPlayer? = null

    private var tickCallback: ((Int) -> Unit)? = null
    private var completeCallback: (() -> Unit)? = null

    // ── Savoir si l'app est visible (mis à jour par TimerFragment) ────────────
    var isAppInForeground: Boolean = false
    var currentIsFocusSession: Boolean = true

    // ── Vibrator tenu vivant dans le Service (foreground process) ─────────────
    private var alarmVibrator: Vibrator? = null

    private val handler = Handler(Looper.getMainLooper())

    inner class TimerBinder : Binder() {
        fun getService() = this@TimerService
    }

    override fun onBind(intent: Intent) = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        BeeAlertManager.createCompletionChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> resumeTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP  -> stopTimer()
        }
        return START_STICKY
    }

    fun setCallbacks(onTick: (Int) -> Unit, onComplete: () -> Unit) {
        tickCallback = onTick
        completeCallback = onComplete
    }

    fun startTimer(seconds: Int, ambientSound: AmbientSound) {
        timeLeft = seconds
        isRunning = true
        startAmbientSound(ambientSound)
        startForeground(NOTIFICATION_ID, buildNotification(timeLeft, true))
        scheduleTimer()
    }

    fun pauseTimer() {
        isRunning = false
        timer?.cancel()
        timer = null
        ambientPlayer?.pause()
        updateNotification(timeLeft, false)
    }

    fun resumeTimer() {
        if (!isRunning && timeLeft > 0) {
            isRunning = true
            ambientPlayer?.start()
            scheduleTimer()
            updateNotification(timeLeft, true)
        }
    }

    fun stopTimer() {
        isRunning = false
        timer?.cancel()
        timer = null
        stopAmbientSound()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun getTimeLeft() = timeLeft
    fun isTimerRunning() = isRunning

    // ── Vibration alarme en boucle infinie ────────────────────────────────────

    fun startAlarmVibration() {
        alarmVibrator?.cancel()

        alarmVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 700, 300, 700, 300, 700, 300)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alarmVibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            alarmVibrator?.vibrate(pattern, 0)
        }
    }

    fun stopAlarmVibration() {
        alarmVibrator?.cancel()
        alarmVibrator = null
    }

    // ── Timer interne ─────────────────────────────────────────────────────────

    private fun scheduleTimer() {
        timer?.cancel()
        timer = Timer()

        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (timeLeft > 0) {
                    timeLeft--
                    handler.post {
                        tickCallback?.invoke(timeLeft)
                        updateNotification(timeLeft, true)
                    }
                } else {
                    isRunning = false
                    cancel()
                    handler.post {
                        stopAmbientSound()
                        onSessionComplete()
                    }
                }
            }
        }, 1000L, 1000L)
    }

    private fun onSessionComplete() {
        playCompletionSound()
        startAlarmVibration()

        if (isAppInForeground) {
            // App visible → TimerFragment affiche le dialog
            completeCallback?.invoke()
        } else {
            // App en background → notification + vibration continue
            BeeAlertManager.showCompletionNotification(this, currentIsFocusSession)
            completeCallback?.invoke()
        }
    }

    // ── Audio ─────────────────────────────────────────────────────────────────

    private fun startAmbientSound(sound: AmbientSound) {
        stopAmbientSound()
        if (sound == AmbientSound.NONE) return

        val rawResId = when (sound) {
            AmbientSound.FOREST    -> R.raw.ambient_forest
            AmbientSound.CAFE      -> R.raw.ambient_cafe
            AmbientSound.RAIN      -> R.raw.ambient_rain
            AmbientSound.OCEAN     -> R.raw.ambient_ocean
            AmbientSound.FIREPLACE -> R.raw.ambient_fireplace
            AmbientSound.NONE      -> return
        }

        try {
            ambientPlayer = MediaPlayer.create(this, rawResId)?.apply {
                isLooping = true
                setVolume(0.3f, 0.3f)
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAmbientSound() {
        ambientPlayer?.apply { if (isPlaying) stop(); release() }
        ambientPlayer = null
    }

    private fun playCompletionSound() {
        try {
            MediaPlayer.create(this, R.raw.timer_complete)?.apply {
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "BeeFlow Session", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows timer progress"; setSound(null, null) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(seconds: Int, isRunning: Boolean): Notification {
        val timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60)

        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val actionIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).apply {
                action = if (isRunning) ACTION_PAUSE else ACTION_START
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pomodoro Timer")
            .setContentText("Time remaining: $timeStr")
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(contentIntent)
            .addAction(
                if (isRunning) R.drawable.ic_pause else R.drawable.ic_play,
                if (isRunning) "Pause" else "Resume",
                actionIntent
            )
            .setOngoing(isRunning)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(seconds: Int, isRunning: Boolean) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(seconds, isRunning))
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        stopAmbientSound()
        alarmVibrator?.cancel()
        alarmVibrator = null
    }
}
