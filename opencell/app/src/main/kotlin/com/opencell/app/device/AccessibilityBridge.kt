package com.opencell.app.device

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.opencell.core.model.Bounds
import com.opencell.core.model.UiNode
import com.opencell.core.model.UiSnapshot
import com.opencell.core.perception.ScreenInsets
import com.opencell.core.perception.ScreenState
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume

class AccessibilityBridge(private val service: AccessibilityService, private val ownPackage: String) {
    private val snapshotIds = AtomicLong()
    @Volatile private var pendingResume: (() -> Unit)? = null

    fun targetRoot(): AccessibilityNodeInfo? {
        val windows = runCatching { service.windows }.getOrNull().orEmpty()
        windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }.forEach { w ->
            val root = w.root ?: return@forEach
            if (root.packageName?.toString() != ownPackage) return root
        }
        return service.rootInActiveWindow?.takeIf { it.packageName?.toString() != ownPackage }
    }

    fun screenState(nowMs: Long): ScreenState {
        val m = metrics()
        return ScreenState(m.widthPixels, m.heightPixels, ScreenInsets(), nowMs)
    }

    private fun metrics(): DisplayMetrics {
        val wm = service.getSystemService(WindowManager::class.java)
        return DisplayMetrics().also { dm ->
            if (Build.VERSION.SDK_INT >= 30) {
                val b = wm.currentWindowMetrics.bounds
                dm.widthPixels = b.width(); dm.heightPixels = b.height()
            } else {
                @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm)
            }
        }
    }

    fun snapshot(nowMs: Long): UiSnapshot? {
        val root = targetRoot() ?: return null
        val nodes = ArrayList<UiNode>()
        walk(root, "0", 0, nodes)
        val windows = runCatching { service.windows }.getOrNull().orEmpty()
        val m = metrics()
        val dialog = nodes.any { it.className?.contains("Dialog", ignoreCase = true) == true }
        return UiSnapshot(
            id = snapshotIds.incrementAndGet(),
            timestampMs = nowMs,
            packageName = root.packageName?.toString().orEmpty(),
            screenWidth = m.widthPixels,
            screenHeight = m.heightPixels,
            nodes = nodes,
            keyboardVisible = windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD },
            dialogVisible = dialog,
        )
    }

    private fun walk(node: AccessibilityNodeInfo, path: String, depth: Int, out: MutableList<UiNode>) {
        if (out.size >= MAX_NODES) return
        val rect = Rect().also(node::getBoundsInScreen)
        if (node.isVisibleToUser) {
            out += UiNode(
                id = path,
                text = node.text?.toString(),
                description = node.contentDescription?.toString(),
                viewId = node.viewIdResourceName,
                className = node.className?.toString(),
                bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
                clickable = node.isClickable,
                longClickable = node.isLongClickable,
                editable = node.isEditable,
                scrollable = node.isScrollable,
                enabled = node.isEnabled,
                focused = node.isFocused,
                password = node.isPassword,
                depth = depth,
            )
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            walk(child, "$path.$i", depth + 1, out)
            recycleCompat(child)
        }
    }

    fun nodeFor(target: UiNode): AccessibilityNodeInfo? {
        var cur = targetRoot() ?: return null
        val indexes = target.id.split('.').drop(1).map { it.toIntOrNull() ?: return null }
        for (i in indexes) cur = cur.getChild(i) ?: return null
        val label = (cur.text?.toString() ?: cur.contentDescription?.toString()).orEmpty()
        return if (label == target.label || target.label.isBlank()) cur else null
    }

    fun focusedInput(): AccessibilityNodeInfo? = targetRoot()?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

    fun clickableAncestor(node: AccessibilityNodeInfo, longClick: Boolean): AccessibilityNodeInfo? {
        var cur: AccessibilityNodeInfo? = node
        var hops = 0
        while (cur != null && hops < 5) {
            if (if (longClick) cur.isLongClickable else cur.isClickable) return cur
            cur = cur.parent
            hops++
        }
        return null
    }

    suspend fun tap(x: Int, y: Int, durationMs: Long = 60) = gesture(Path().apply { moveTo(x.toFloat(), y.toFloat()) }, durationMs)

    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = 350) =
        gesture(Path().apply { moveTo(x1.toFloat(), y1.toFloat()); lineTo(x2.toFloat(), y2.toFloat()) }, durationMs)

    private suspend fun gesture(path: Path, durationMs: Long): Boolean {
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, durationMs)).build()
        return suspendCancellableCoroutine { cont ->
            pendingResume = { if (cont.isActive) cont.resume(false) }
            val accepted = service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(g: GestureDescription?) { pendingResume = null; if (cont.isActive) cont.resume(true) }
                override fun onCancelled(g: GestureDescription?) { pendingResume = null; if (cont.isActive) cont.resume(false) }
            }, null)
            if (!accepted) { pendingResume = null; if (cont.isActive) cont.resume(false) }
            cont.invokeOnCancellation { pendingResume = null }
        }
    }

    fun cancelPendingGesture() { pendingResume?.invoke(); pendingResume = null }

    fun back() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

    @Suppress("DEPRECATION")
    private fun recycleCompat(n: AccessibilityNodeInfo) { if (Build.VERSION.SDK_INT < 33) n.recycle() }

    private companion object { const val MAX_NODES = 600 }
}
