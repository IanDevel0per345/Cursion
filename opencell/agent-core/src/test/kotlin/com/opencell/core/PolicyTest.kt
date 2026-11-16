package com.opencell.core

import com.opencell.core.model.Bounds
import com.opencell.core.model.UiNode
import com.opencell.core.model.UiSnapshot
import com.opencell.core.policy.AutonomyMode
import com.opencell.core.policy.PolicyDecision
import com.opencell.core.policy.PolicyEngine
import com.opencell.core.policy.PolicySettings
import com.opencell.core.policy.Risk
import com.opencell.core.protocol.AgentAction
import com.opencell.core.protocol.Selector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyTest {
    private val engine = PolicyEngine(setOf("com.opencell.app"))
    private val sel = Selector(text = "x")
    private fun node(text: String, password: Boolean = false) =
        UiNode("n", text, null, null, null, Bounds(0, 0, 10, 10), clickable = true, password = password)
    private fun snap(pkg: String = "com.test", protectedContent: Boolean = false) =
        UiSnapshot(1, 0, pkg, 100, 100, emptyList(), protectedContent = protectedContent)

    @Test fun sendRequiresApproval() {
        val d = engine.evaluate("s1", AgentAction.TapElement(sel), node("Enviar"), snap(), PolicySettings())
        assertTrue(d is PolicyDecision.RequireApproval && d.risk == Risk.EXTERNAL_EFFECT)
    }

    @Test fun deleteAndPayAreIrreversible() {
        listOf("Excluir conversa", "Pagar agora", "Comprar").forEach {
            val d = engine.evaluate("s", AgentAction.TapElement(sel), node(it), snap(), PolicySettings())
            assertTrue(it, d is PolicyDecision.RequireApproval && d.risk == Risk.IRREVERSIBLE)
        }
    }

    @Test fun approvedStepIsAllowed() {
        val d = engine.evaluate("s1", AgentAction.TapElement(sel), node("Enviar"), snap(), PolicySettings(approvedStepIds = setOf("s1")))
        assertEquals(PolicyDecision.Allow, d)
    }

    @Test fun harmlessTapAllowedInSupervised() {
        assertEquals(PolicyDecision.Allow, engine.evaluate("s", AgentAction.TapElement(sel), node("Mensagens"), snap(), PolicySettings()))
    }

    @Test fun wordBoundaryAvoidsFalsePositive() {
        assertEquals(Risk.NONE, engine.classify(AgentAction.TapElement(sel), node("Resend help")))
        assertEquals(Risk.NONE, engine.classify(AgentAction.TapElement(sel), node("Postagens")))
    }

    @Test fun passwordFieldTypingDenied() {
        val d = engine.evaluate("s", AgentAction.EnterText(sel, "abc"), node("", password = true), snap(), PolicySettings())
        assertTrue(d is PolicyDecision.Deny)
    }

    @Test fun observerModeDeniesInteraction() {
        val d = engine.evaluate("s", AgentAction.TapElement(sel), node("Ok"), snap(), PolicySettings(mode = AutonomyMode.OBSERVER))
        assertTrue(d is PolicyDecision.Deny)
    }

    @Test fun assistedModeAsksForEveryInteraction() {
        val d = engine.evaluate("s", AgentAction.TapElement(sel), node("Ok"), snap(), PolicySettings(mode = AutonomyMode.ASSISTED))
        assertTrue(d is PolicyDecision.RequireApproval)
    }

    @Test fun blockedAndNonAllowedPackagesDenied() {
        assertTrue(engine.evaluate("s", AgentAction.TapElement(sel), node("Ok"), snap("com.bank"), PolicySettings(blockedPackages = setOf("com.bank"))) is PolicyDecision.Deny)
        assertTrue(engine.evaluate("s", AgentAction.TapElement(sel), node("Ok"), snap("com.other"), PolicySettings(allowedPackages = setOf("com.test"))) is PolicyDecision.Deny)
    }

    @Test fun protectedScreenDenied() {
        assertTrue(engine.evaluate("s", AgentAction.TapElement(sel), node("Ok"), snap(protectedContent = true), PolicySettings()) is PolicyDecision.Deny)
    }
}
