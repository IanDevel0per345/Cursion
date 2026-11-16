package com.opencell.app.device

import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.opencell.core.engine.ActionExecutor
import com.opencell.core.engine.Clock
import com.opencell.core.engine.ExecResult
import com.opencell.core.engine.ScreenObserver
import com.opencell.core.model.UiNode
import com.opencell.core.model.UiSnapshot
import com.opencell.core.perception.PointResult
import com.opencell.core.perception.ScreenGeometryMapper
import com.opencell.core.protocol.AgentAction
import com.opencell.core.protocol.Direction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidScreenObserver(private val clock: Clock) : ScreenObserver {
    @Volatile var lastPackage: String? = null
        private set

    override suspend fun snapshot(): UiSnapshot? = withContext(Dispatchers.Main) {
        OpenCellAccessibilityService.bridge()?.snapshot(clock.nowMs())?.also { lastPackage = it.packageName }
    }
}

class AndroidActionExecutor(
    private val context: Context,
    private val apps: AppResolver,
    private val clock: Clock,
    private val gestureShield: (Boolean) -> Unit,
) : ActionExecutor {

    override fun cancelPending() { OpenCellAccessibilityService.bridge()?.cancelPendingGesture() }

    override suspend fun execute(action: AgentAction, target: UiNode?, snapshot: UiSnapshot): ExecResult = withContext(Dispatchers.Main) {
        val bridge = OpenCellAccessibilityService.bridge() ?: return@withContext ExecResult.Failed("Serviço de acessibilidade não está ativo.")
        when (action) {
            is AgentAction.OpenApplication -> open(action)
            is AgentAction.TapElement -> click(bridge, target, snapshot, longClick = false)
            is AgentAction.LongPress -> click(bridge, target, snapshot, longClick = true)
            is AgentAction.EnterText -> setText(bridge, target, action.text)
            is AgentAction.ScrollContainer -> scroll(bridge, action, target, snapshot)
            is AgentAction.Swipe -> swipe(bridge, action.direction, snapshot)
            is AgentAction.PressEnter -> pressEnter(bridge, target)
            AgentAction.PressBack -> if (bridge.back()) ExecResult.Dispatched else ExecResult.Failed("O sistema recusou a ação Voltar.")
            is AgentAction.TapPoint -> tapPoint(bridge, action, snapshot)
            else -> ExecResult.Failed("Ação ${action::class.simpleName} não é executada no dispositivo.")
        }
    }

    private suspend fun shielded(block: suspend () -> Boolean): Boolean {
        gestureShield(true)
        return try { block() } finally { gestureShield(false) }
    }

    private fun open(action: AgentAction.OpenApplication): ExecResult {
        val app = apps.resolve(action.appName, action.packageName) ?: return ExecResult.Failed("Aplicativo \"${action.appName ?: action.packageName}\" não encontrado.")
        val intent = apps.launchIntent(app) ?: return ExecResult.Failed("${app.label} não possui tela inicial lançável.")
        return runCatching { context.startActivity(intent); ExecResult.Dispatched }
            .getOrElse { ExecResult.Failed("Não foi possível abrir ${app.label}: ${it.message}") }
    }

    private fun pressEnter(bridge: AccessibilityBridge, target: UiNode?): ExecResult {
        val node = target?.let { bridge.nodeFor(it) } ?: bridge.focusedInput()
            ?: return ExecResult.Failed("Nenhum campo de texto focado para pressionar Enter.")
        if (android.os.Build.VERSION.SDK_INT >= 30 && node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)) return ExecResult.Dispatched
        return ExecResult.Failed("Enter não suportado neste campo/versão do Android (exige Android 11+). Toque na sugestão ou no botão de busca.")
    }

    private suspend fun click(bridge: AccessibilityBridge, target: UiNode?, snapshot: UiSnapshot, longClick: Boolean): ExecResult {
        target ?: return ExecResult.Failed("Sem alvo para o toque.")
        val node = bridge.nodeFor(target) ?: return ExecResult.Failed("A tela mudou: o elemento \"${target.label}\" não está mais no mesmo lugar.")
        val clickable = bridge.clickableAncestor(node, longClick)
        if (clickable != null) {
            val action = if (longClick) AccessibilityNodeInfo.ACTION_LONG_CLICK else AccessibilityNodeInfo.ACTION_CLICK
            if (clickable.performAction(action)) return ExecResult.Dispatched
        }
        val ok = shielded { if (longClick) bridge.tap(target.bounds.centerX, target.bounds.centerY, 800) else bridge.tap(target.bounds.centerX, target.bounds.centerY) }
        return if (ok) ExecResult.Dispatched else ExecResult.Failed("O toque foi cancelado ou recusado pelo sistema.")
    }

    private fun setText(bridge: AccessibilityBridge, target: UiNode?, text: String): ExecResult {
        target ?: return ExecResult.Failed("Sem campo de texto alvo.")
        if (!target.editable) return ExecResult.Failed("O elemento \"${target.label}\" não é editável.")
        val node = bridge.nodeFor(target) ?: return ExecResult.Failed("O campo de texto saiu da tela.")
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        return if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) ExecResult.Dispatched
        else ExecResult.Failed("O aplicativo recusou a inserção de texto (campo sem suporte a ACTION_SET_TEXT).")
    }

    private suspend fun scroll(bridge: AccessibilityBridge, action: AgentAction.ScrollContainer, target: UiNode?, snapshot: UiSnapshot): ExecResult {
        val container = target ?: snapshot.nodes.filter { it.scrollable && it.visible }.maxByOrNull { it.bounds.width * it.bounds.height }
        if (container != null && (action.direction == Direction.DOWN || action.direction == Direction.UP)) {
            val node = bridge.nodeFor(container)
            val act = if (action.direction == Direction.DOWN) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            if (node != null && node.performAction(act)) return ExecResult.Dispatched
        }
        return swipe(bridge, action.direction, snapshot)
    }

    private suspend fun swipe(bridge: AccessibilityBridge, dir: Direction, snapshot: UiSnapshot): ExecResult {
        val w = snapshot.screenWidth; val h = snapshot.screenHeight
        val cx = w / 2; val cy = h / 2
        val (x1, y1, x2, y2) = when (dir) {
            Direction.DOWN -> listOf(cx, (h * 0.70).toInt(), cx, (h * 0.30).toInt())
            Direction.UP -> listOf(cx, (h * 0.30).toInt(), cx, (h * 0.70).toInt())
            Direction.LEFT -> listOf((w * 0.80).toInt(), cy, (w * 0.20).toInt(), cy)
            Direction.RIGHT -> listOf((w * 0.20).toInt(), cy, (w * 0.80).toInt(), cy)
        }
        return if (shielded { bridge.swipe(x1, y1, x2, y2) }) ExecResult.Dispatched else ExecResult.Failed("O gesto de deslizar foi cancelado.")
    }

    private suspend fun tapPoint(bridge: AccessibilityBridge, action: AgentAction.TapPoint, snapshot: UiSnapshot): ExecResult {
        if (action.snapshotId != snapshot.id) return ExecResult.Failed("Coordenadas pertencem a outro snapshot (${action.snapshotId}).")
        return when (val p = ScreenGeometryMapper.toPixels(action.x, action.y, snapshot, bridge.screenState(clock.nowMs()))) {
            is PointResult.Rejected -> ExecResult.Failed(p.reason)
            is PointResult.Ok -> if (shielded { bridge.tap(p.x, p.y) }) ExecResult.Dispatched else ExecResult.Failed("O toque foi cancelado.")
        }
    }
}
