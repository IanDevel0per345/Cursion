package com.opencell.app

import android.content.Context
import com.opencell.app.agent.AgentRuntime
import com.opencell.app.data.AppSettings
import com.opencell.app.data.KeystoreSecretStore
import com.opencell.app.data.OpenCellDatabase
import com.opencell.app.data.SecretStore
import com.opencell.app.data.SettingsRepository
import com.opencell.app.device.AndroidActionExecutor
import com.opencell.app.device.AndroidScreenObserver
import com.opencell.app.device.AppResolver
import com.opencell.app.trigger.PanelLauncher
import com.opencell.app.trigger.TaskNotifier
import com.opencell.app.voice.Speaker
import com.opencell.core.engine.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AppContainer(val context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settingsRepo = SettingsRepository(context)
    val settings: StateFlow<AppSettings> = settingsRepo.flow.stateIn(scope, SharingStarted.Eagerly, AppSettings())
    val secrets: SecretStore = KeystoreSecretStore(context)
    val db: OpenCellDatabase = OpenCellDatabase.create(context)
    val apps = AppResolver(context)
    val observer = AndroidScreenObserver(SystemClock)
    val panelLauncher = PanelLauncher(context)
    val executor = AndroidActionExecutor(context, apps, SystemClock) { panelLauncher.passThrough(it) }
    val speaker = Speaker(context)
    val runtime = AgentRuntime(scope, { settings.value }, secrets, db, observer, executor, apps, SystemClock, context.packageName)
    val notifier = TaskNotifier(context, runtime, scope, speaker) { settings.value }
}
