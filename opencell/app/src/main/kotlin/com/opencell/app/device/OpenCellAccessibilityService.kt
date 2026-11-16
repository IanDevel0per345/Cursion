package com.opencell.app.device

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.opencell.app.OpenCellApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OpenCellAccessibilityService : AccessibilityService() {
    private var overlay: OverlayPanelHost? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val bridge = AccessibilityBridge(this, packageName)
        state.value = Connection(this, bridge)
        overlay = OverlayPanelHost(this, (application as OpenCellApp).container)
        (application as OpenCellApp).container.panelLauncher.overlay = overlay
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        (application as OpenCellApp).container.runtime.stop("Serviço de acessibilidade interrompido pelo sistema.")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        val container = (application as OpenCellApp).container
        container.runtime.stop("Permissão de acessibilidade revogada; tarefa encerrada.")
        overlay?.hide(); overlay = null
        container.panelLauncher.overlay = null
        state.value = null
    }

    data class Connection(val service: OpenCellAccessibilityService, val bridge: AccessibilityBridge)

    companion object {
        private val state = MutableStateFlow<Connection?>(null)
        val connection: StateFlow<Connection?> = state.asStateFlow()
        fun bridge(): AccessibilityBridge? = state.value?.bridge
        val isConnected get() = state.value != null
    }
}
