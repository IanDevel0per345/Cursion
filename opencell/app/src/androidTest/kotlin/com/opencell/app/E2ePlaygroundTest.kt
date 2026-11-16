package com.opencell.app

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencell.app.agent.PanelState
import com.opencell.app.agent.StepUiStatus
import com.opencell.app.device.OpenCellAccessibilityService
import com.opencell.core.engine.Outcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Requer: Playground instalado e o serviço de acessibilidade do OpenCell habilitado (ver scripts/e2e.sh).
 * Sem isso os testes são ignorados (Assume), nunca aprovados em falso.
 */
@RunWith(AndroidJUnit4::class)
class E2ePlaygroundTest {
    private lateinit var container: AppContainer

    @Before fun setUp() {
        val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
        container = (ctx.applicationContext as OpenCellApp).container
        assumeTrue("Serviço de acessibilidade não conectado", OpenCellAccessibilityService.isConnected)
        assumeTrue("Playground não instalado", container.apps.resolve("playground", null) != null)
        container.runtime.dismissResult()
    }

    private fun awaitIdle(timeoutMs: Long = 60_000, until: () -> Boolean = { !container.runtime.ui.value.taskActive && container.runtime.ui.value.outcome != null }) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) { if (until()) return; Thread.sleep(200) }
        throw AssertionError("Tempo esgotado. Estado: ${container.runtime.ui.value}")
    }

    private fun run(goal: String) { container.runtime.dismissResult(); container.runtime.submit(goal) }

    @Test fun opensAppTapsTabAndScrollsWithVerification() {
        run("abra o aplicativo de teste, toque em Lista, role para baixo")
        awaitIdle()
        val ui = container.runtime.ui.value
        assertEquals(ui.message, Outcome.SUCCESS, ui.outcome)
        assertTrue(ui.steps.all { it.status == StepUiStatus.VERIFIED })
        val snap = runBlocking { container.observer.snapshot() }
        assertNotNull(snap)
        assertEquals("com.opencell.playground", snap!!.packageName)
        assertTrue("A lista deveria ter rolado", snap.nodes.none { it.text == "Item 1" })
    }

    @Test fun fillsMessageFieldWithoutSending() {
        run("abra o aplicativo de teste, toque em Mensagens, toque em Cliente A, digite 'Olá! Como posso ajudar?' no campo")
        awaitIdle()
        val ui = container.runtime.ui.value
        assertEquals(ui.message, Outcome.SUCCESS, ui.outcome)
        val snap = runBlocking { container.observer.snapshot() }!!
        assertTrue(snap.nodes.any { it.editable && it.text?.contains("Como posso ajudar") == true })
        assertTrue(snap.nodes.any { it.text == "Mensagens enviadas: 0" })
    }

    @Test fun sendRequiresApprovalAndDeclineLeavesCounterUntouched() {
        run("abra o aplicativo de teste, toque em Mensagens, toque em Cliente A, toque em Enviar")
        awaitIdle(until = { container.runtime.ui.value.approval != null })
        assertEquals(PanelState.WaitingForApproval, container.runtime.ui.value.panel)
        container.runtime.approve(false)
        awaitIdle()
        assertEquals(Outcome.DECLINED, container.runtime.ui.value.outcome)
        val snap = runBlocking { container.observer.snapshot() }!!
        assertTrue(snap.nodes.any { it.text == "Mensagens enviadas: 0" })
    }

    @Test fun changedButtonLabelFailsInsteadOfBlindRetrying() {
        run("abra o aplicativo de teste, toque em Mensagens, toque em Cliente A, toque em Alterar texto do botão, toque em Enviar")
        awaitIdle(timeoutMs = 90_000)
        val ui = container.runtime.ui.value
        assertEquals(ui.message, Outcome.FAILED, ui.outcome)
        val snap = runBlocking { container.observer.snapshot() }!!
        assertTrue(snap.nodes.any { it.text == "Mensagens enviadas: 0" })
    }

    @Test fun stopCancelsRunningTask() {
        run("abra o aplicativo de teste, toque em Lista, " + List(15) { "role para baixo" }.joinToString(", "))
        awaitIdle(until = { container.runtime.ui.value.steps.any { it.status == StepUiStatus.RUNNING } })
        container.runtime.stop()
        awaitIdle()
        assertEquals(Outcome.CANCELLED, container.runtime.ui.value.outcome)
    }
}
