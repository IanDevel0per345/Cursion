package com.opencell.app

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OpenCellApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.start()
        container.scope.launch(Dispatchers.IO) { pruneHistory() }
    }

    private suspend fun pruneHistory() {
        val days = container.settingsRepo.flow.first().retentionDays
        if (days <= 0) return
        val dao = container.db.tasks()
        dao.idsOlderThan(System.currentTimeMillis() - days * 86_400_000L).forEach { id ->
            dao.deleteEvents(id); dao.deleteTask(id)
        }
    }
}
