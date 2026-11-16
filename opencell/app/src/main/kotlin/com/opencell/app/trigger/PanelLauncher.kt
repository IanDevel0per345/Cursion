package com.opencell.app.trigger

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import com.opencell.app.MainActivity
import com.opencell.app.device.OpenCellAccessibilityService
import com.opencell.app.device.OverlayPanelHost
import com.opencell.app.ui.PanelActivity

class PanelLauncher(private val context: Context) {
    @Volatile var overlay: OverlayPanelHost? = null
    @Volatile var activityShield: ((Boolean) -> Unit)? = null

    fun show(dismissShade: Boolean = false) {
        val host = overlay
        if (host != null) {
            if (dismissShade && Build.VERSION.SDK_INT >= 31) {
                OpenCellAccessibilityService.connection.value?.service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            }
            host.show()
        } else {
            context.startActivity(Intent(context, PanelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
    }

    fun passThrough(enabled: Boolean) {
        overlay?.setTouchPassthrough(enabled)
        activityShield?.invoke(enabled)
    }

    fun openMainApp(from: Context = context) {
        from.startActivity(Intent(from, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
