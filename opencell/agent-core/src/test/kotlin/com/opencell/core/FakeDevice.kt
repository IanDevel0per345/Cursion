package com.opencell.core

import com.opencell.core.engine.ActionExecutor
import com.opencell.core.engine.ExecResult
import com.opencell.core.engine.ScreenObserver
import com.opencell.core.model.Bounds
import com.opencell.core.model.UiNode
import com.opencell.core.model.UiSnapshot
import com.opencell.core.protocol.AgentAction

class FakeDevice(var sendLabel: String = "Enviar") : ScreenObserver, ActionExecutor {
    var screen = "home"
    var field = ""
    var sent = 0
    var snapshotsAvailable = true
    var swallowTaps = false
    var onExecute: ((AgentAction) -> Unit)? = null
    val executed = mutableListOf<AgentAction>()
    private var counter = 0L
    private var y = 0

    private fun node(id: String, text: String?, clickable: Boolean = false, editable: Boolean = false, password: Boolean = false): UiNode {
        y += 100
        return UiNode(id, text, null, null, if (editable) "android.widget.EditText" else null, Bounds(0, y, 1000, y + 80),
            clickable = clickable, editable = editable, password = password)
    }

    private fun nodes(): List<UiNode> {
        y = 0
        return when (screen) {
            "home" -> listOf(node("tab-msg", "Mensagens", true), node("tab-profile", "Perfil", true), node("label", "Início"))
            "list" -> listOf(node("c1", "Cliente A", true), node("c2", "Cliente B", true), node("hdr", "Conversas"))
            "chat" -> listOf(node("hdr", "Chat Cliente A"), node("input", field, editable = true), node("send", sendLabel, true)) +
                if (sent > 0) listOf(node("ok", "Enviado")) else emptyList()
            "login" -> listOf(node("pwd", "", editable = true, password = true), node("go", "Entrar", true))
            else -> emptyList()
        }
    }

    override suspend fun snapshot(): UiSnapshot? =
        if (!snapshotsAvailable) null else UiSnapshot(++counter, counter * 100, "com.test.app", 1080, 2400, nodes())

    override suspend fun execute(action: AgentAction, target: UiNode?, snapshot: UiSnapshot): ExecResult {
        executed += action
        onExecute?.invoke(action)
        if (swallowTaps) return ExecResult.Dispatched
        when (action) {
            is AgentAction.TapElement -> when (target?.id) {
                "tab-msg" -> screen = "list"
                "c1" -> screen = "chat"
                "send" -> sent++
            }
            is AgentAction.EnterText -> if (target?.id == "input") field = action.text
            else -> Unit
        }
        return ExecResult.Dispatched
    }
}
