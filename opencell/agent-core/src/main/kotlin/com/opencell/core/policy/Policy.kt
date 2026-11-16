package com.opencell.core.policy

import com.opencell.core.model.UiNode
import com.opencell.core.model.UiSnapshot
import com.opencell.core.protocol.AgentAction

enum class AutonomyMode { OBSERVER, ASSISTED, SUPERVISED, BOUNDED_ROUTINE }

enum class Risk { NONE, EXTERNAL_EFFECT, IRREVERSIBLE, SENSITIVE_INPUT }

sealed interface PolicyDecision {
    data object Allow : PolicyDecision
    data class RequireApproval(val reason: String, val risk: Risk) : PolicyDecision
    data class Deny(val reason: String) : PolicyDecision
}

data class PolicySettings(
    val mode: AutonomyMode = AutonomyMode.SUPERVISED,
    val blockedPackages: Set<String> = emptySet(),
    val allowedPackages: Set<String> = emptySet(),
    val confirmExternalActions: Boolean = true,
    val confirmIrreversible: Boolean = true,
    val approvedStepIds: Set<String> = emptySet(),
)

class PolicyEngine(private val ownPackages: Set<String>) {

    fun evaluate(
        stepId: String,
        action: AgentAction,
        target: UiNode?,
        snapshot: UiSnapshot?,
        settings: PolicySettings,
    ): PolicyDecision {
        if (action is AgentAction.RequestUserConfirmation || action is AgentAction.ReportLimitation || action is AgentAction.CompleteTask) {
            return PolicyDecision.Allow
        }
        if (settings.mode == AutonomyMode.OBSERVER && action.isInteractive()) {
            return PolicyDecision.Deny("Modo Observador não executa toques ou digitação.")
        }
        val pkg = snapshot?.packageName
        if (pkg != null && pkg !in ownPackages) {
            if (pkg in settings.blockedPackages) return PolicyDecision.Deny("O aplicativo $pkg está bloqueado nas configurações.")
            if (settings.allowedPackages.isNotEmpty() && pkg !in settings.allowedPackages) {
                return PolicyDecision.Deny("O aplicativo $pkg não está na lista de aplicativos permitidos.")
            }
        }
        if (snapshot?.protectedContent == true && action.isInteractive()) {
            return PolicyDecision.Deny("A tela atual é protegida; é necessário que você assuma o controle.")
        }
        if (action is AgentAction.EnterText && target?.password == true) {
            return PolicyDecision.Deny("OpenCell não digita em campos de senha.")
        }
        val risk = classify(action, target)
        val approved = stepId in settings.approvedStepIds
        return when {
            risk == Risk.NONE && settings.mode != AutonomyMode.ASSISTED -> PolicyDecision.Allow
            risk == Risk.NONE && !action.isInteractive() -> PolicyDecision.Allow
            approved -> PolicyDecision.Allow
            risk == Risk.IRREVERSIBLE && settings.confirmIrreversible ->
                PolicyDecision.RequireApproval("Ação irreversível: ${describe(action, target)}", risk)
            risk == Risk.EXTERNAL_EFFECT && settings.confirmExternalActions ->
                PolicyDecision.RequireApproval("Ação com efeito externo: ${describe(action, target)}", risk)
            risk == Risk.SENSITIVE_INPUT ->
                PolicyDecision.RequireApproval("Entrada em campo sensível: ${describe(action, target)}", risk)
            settings.mode == AutonomyMode.ASSISTED && action.isInteractive() ->
                PolicyDecision.RequireApproval("Modo Assistido: ${describe(action, target)}", Risk.NONE)
            else -> PolicyDecision.Allow
        }
    }

    fun classify(action: AgentAction, target: UiNode?): Risk {
        val label = when (action) {
            is AgentAction.TapElement, is AgentAction.LongPress -> target?.label.orEmpty()
            else -> ""
        }
        if (label.isEmpty()) return Risk.NONE
        val norm = label.lowercase()
        return when {
            IRREVERSIBLE.any { it in norm } -> Risk.IRREVERSIBLE
            EXTERNAL.any { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(norm) } -> Risk.EXTERNAL_EFFECT
            else -> Risk.NONE
        }
    }

    private fun describe(action: AgentAction, target: UiNode?) =
        target?.label?.takeIf { it.isNotBlank() }?.let { "tocar em \"$it\"" } ?: action::class.simpleName.orEmpty()

    private fun AgentAction.isInteractive() = when (this) {
        is AgentAction.TapElement, is AgentAction.TapPoint, is AgentAction.LongPress, is AgentAction.Swipe,
        is AgentAction.ScrollContainer, is AgentAction.EnterText -> true
        else -> false
    }

    private companion object {
        val IRREVERSIBLE = listOf(
            "excluir", "apagar", "deletar", "delete", "remove", "remover", "pagar", "pay", "comprar", "buy",
            "purchase", "transferir", "transfer", "confirmar compra", "finalizar pedido", "place order", "sair da conta", "logout",
        )
        val EXTERNAL = listOf(
            "enviar", "send", "publicar", "post", "publish", "compartilhar", "share", "responder", "reply", "tweetar",
        )
    }
}
