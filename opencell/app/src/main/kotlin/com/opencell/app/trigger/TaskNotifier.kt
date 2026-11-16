package com.opencell.app.trigger

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.opencell.app.OpenCellApp
import com.opencell.app.R
import com.opencell.app.agent.AgentRuntime
import com.opencell.app.data.AppSettings
import com.opencell.app.voice.Speaker
import com.opencell.core.security.Redactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class TaskNotifier(
    private val context: Context,
    private val runtime: AgentRuntime,
    private val scope: CoroutineScope,
    private val speaker: Speaker,
    private val settings: () -> AppSettings,
) {
    private val nm = context.getSystemService(NotificationManager::class.java)

    fun start() {
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notif_channel_tasks), NotificationManager.IMPORTANCE_LOW))
        scope.launch {
            runtime.ui.distinctUntilChangedBy { it.taskActive to it.message }.collect { s ->
                if (s.taskActive) {
                    notify(ONGOING, build("OpenCell executando", Redactor.redact(s.goal), ongoing = true))
                } else {
                    nm.cancel(ONGOING)
                    val msg = s.message
                    if (msg != null) {
                        notify(DONE, build("OpenCell", "Tarefa encerrada", ongoing = false))
                        if (settings().speakResponses) speaker.speak(msg, settings().language)
                    }
                }
            }
        }
    }

    private fun build(title: String, text: String, ongoing: Boolean): Notification {
        val stop = PendingIntent.getBroadcast(context, 1, Intent(context, ControlReceiver::class.java).setAction(ControlReceiver.STOP), PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getBroadcast(context, 2, Intent(context, ControlReceiver::class.java).setAction(ControlReceiver.SHOW), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
            .setOngoing(ongoing).setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .apply {
                if (ongoing) addAction(0, "Parar OpenCell", stop)
                addAction(0, "Abrir painel", open)
            }.build()
    }

    private fun notify(id: Int, n: Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        nm.notify(id, n)
    }

    private companion object {
        const val CHANNEL = "tasks"
        const val ONGOING = 1
        const val DONE = 2
    }
}

class ControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as OpenCellApp).container
        when (intent.action) {
            STOP -> container.runtime.stop("Tarefa interrompida pelo botão Parar OpenCell.")
            SHOW -> container.panelLauncher.show(dismissShade = true)
        }
    }

    companion object {
        const val STOP = "com.opencell.app.action.STOP"
        const val SHOW = "com.opencell.app.action.SHOW"
    }
}
