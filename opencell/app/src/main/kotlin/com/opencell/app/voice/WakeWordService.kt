package com.opencell.app.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.opencell.app.OpenCellApp
import com.opencell.app.R
import com.opencell.app.trigger.ControlReceiver
import kotlinx.coroutines.launch
import java.text.Normalizer

/** EXPERIMENTAL: reconhecimento contínuo via SpeechRecognizer do sistema; sujeito a bipes, consumo de bateria e restrições do fabricante. */
class WakeWordService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var voice: VoiceInput
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.notif_channel_wake), NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 3, Intent(this, WakeWordService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat).setContentTitle("OpenCell está ouvindo a palavra de ativação")
            .setContentText("Microfone ativo (experimental). Toque em Desativar para parar.")
            .setOngoing(true).addAction(0, "Desativar", stop).build()
        if (intent?.action == ACTION_STOP) {
            val container = (application as OpenCellApp).container
            container.scope.launch { container.settingsRepo.update { it.copy(wakeWordEnabled = false) } }
            stopSelf(); return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT >= 29) startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(1001, notification)
        voice = VoiceInput(this)
        running = true
        listen()
        return START_STICKY
    }

    private fun listen() {
        if (!running) return
        val container = (application as OpenCellApp).container
        voice.start(
            container.settings.value.language,
            onPartial = { if (matches(it)) trigger() },
            onFinal = { if (matches(it)) trigger() else restart() },
            onError = { restart() },
        )
    }

    private fun restart() { if (running) handler.postDelayed({ listen() }, 600) }

    private fun trigger() {
        voice.stop()
        (application as OpenCellApp).container.panelLauncher.show()
        restart()
    }

    private fun matches(text: String): Boolean {
        val n = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return WAKE.any { it.containsMatchIn(n) }
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        if (::voice.isInitialized) voice.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.opencell.app.action.STOP_WAKE"
        private const val CHANNEL = "wake"
        private val WAKE = listOf(Regex("\\bok(ay)?\\s*open\\s*(cell|sell|sel)\\b"), Regex("\\bok(ay)?\\s*opencell\\b"), Regex("\\bok\\s*open\\s*cel\\b"))
    }
}
