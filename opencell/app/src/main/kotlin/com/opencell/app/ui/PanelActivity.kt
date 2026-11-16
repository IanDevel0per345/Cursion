package com.opencell.app.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.opencell.app.OpenCellApp

class PanelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as OpenCellApp).container
        container.runtime.setVisible(true)
        container.panelLauncher.activityShield = { on -> setPassthrough(on) }
        setContent {
            OpenCellTheme(container) {
                Box(
                    Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) { finish() },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    PanelHost(container, onDismiss = { finish() }, onRequestMic = { container.panelLauncher.openMainApp(this@PanelActivity) })
                }
            }
        }
    }

    private fun setPassthrough(on: Boolean) = runOnUiThread {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) else window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    override fun onDestroy() {
        val container = (application as OpenCellApp).container
        container.panelLauncher.activityShield = null
        container.runtime.setVisible(false)
        super.onDestroy()
    }
}
