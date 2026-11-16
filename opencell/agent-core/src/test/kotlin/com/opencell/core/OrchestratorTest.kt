package com.opencell.core

import com.opencell.core.engine.AgentState
import com.opencell.core.engine.ApprovalGate
import com.opencell.core.engine.Orchestrator
import com.opencell.core.engine.OrchestratorConfig
import com.opencell.core.engine.Outcome
import com.opencell.core.engine.PlanResult
import com.opencell.core.engine.Planner
import com.opencell.core.engine.ReplanContext
import com.opencell.core.engine.StepStatus
import com.opencell.core.engine.TaskController
import com.opencell.core.engine.TaskEvent
import com.opencell.core.engine.TaskJournal
import com.opencell.core.model.UiSnapshot
import com.opencell.core.policy.PolicyEngine
import com.opencell.core.policy.PolicySettings
import com.opencell.core.policy.Risk
import com.opencell.core.protocol.AgentAction
import com.opencell.core.protocol.Expectation
import com.opencell.core.protocol.Plan
import com.opencell.core.protocol.PlanStep
import com.opencell.core.protocol.Role
import com.opencell.core.protocol.Selector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrchestratorTest {
    private class Events : TaskJournal {
        val all = mutableListOf<TaskEvent>()
        override fun onEvent(event: TaskEvent) { all += event }
        val states get() = all.filterIsInstance<TaskEvent.StateChanged>().map { it.state }
    }

    private class Gate(val answer: Boolean) : ApprovalGate {
        val asked = mutableListOf<String>()
        override suspend fun request(taskId: String, stepId: String, reason: String, risk: Risk): Boolean { asked += reason; return answer }
    }

    private class FixedPlanner(val plan: Plan, val replan: Plan? = null) : Planner {
        var replanCalls = 0
        override suspend fun plan(goal: String, snapshot: UiSnapshot?) = PlanResult.Ok(plan)
        override suspend fun replan(context: ReplanContext): PlanResult {
            replanCalls++
            return replan?.let { PlanResult.Ok(it) } ?: PlanResult.Failed("sem replanejamento")
        }
    }

    private val cfg = OrchestratorConfig(settleDelayMs = 1, maxRetriesPerAction = 1, policy = PolicySettings())
    private fun orch(device: FakeDevice, planner: Planner, gate: ApprovalGate, journal: Events, config: OrchestratorConfig = cfg) =
        Orchestrator(device, device, planner, gate, journal, PolicyEngine(emptySet()), { config })

    private fun step(id: String, action: AgentAction, expect: Expectation? = null) = PlanStep(id, id, action, expect)
    private fun tap(text: String) = AgentAction.TapElement(Selector(text = text))
    private val input = Selector(role = Role.INPUT)

    private val fillPlan = Plan(goal = "g", steps = listOf(
        step("s1", tap("Mensagens"), Expectation.TextVisible("Cliente A")),
        step("s2", tap("Cliente A"), Expectation.ElementExists(input)),
        step("s3", AgentAction.EnterText(input, "Olá! Como posso ajudar?"), Expectation.FieldContains(input, "Como posso ajudar")),
        step("s4", AgentAction.CompleteTask("Mensagem preenchida")),
    ))

    @Test fun multiStepTaskCompletesAndVerifiesWithoutSending() = runTest {
        val d = FakeDevice(); val j = Events(); val gate = Gate(true)
        val report = orch(d, FixedPlanner(fillPlan), gate, j).run("preencher")
        assertEquals(report.summary, Outcome.SUCCESS, report.outcome)
        assertEquals("chat", d.screen)
        assertEquals("Olá! Como posso ajudar?", d.field)
        assertEquals(0, d.sent)
        assertTrue(report.steps.all { it.status == StepStatus.VERIFIED })
        assertEquals(AgentState.COMPLETED, j.states.last())
        assertTrue(gate.asked.isEmpty())
    }

    @Test fun sendRequiresApprovalAndDeclineStopsWithoutSending() = runTest {
        val d = FakeDevice().apply { screen = "chat" }; val j = Events(); val gate = Gate(false)
        val plan = Plan(goal = "g", steps = listOf(step("s1", tap("Enviar"), Expectation.TextVisible("Enviado")), step("s2", AgentAction.CompleteTask("ok"))))
        val report = orch(d, FixedPlanner(plan), gate, j).run("enviar")
        assertEquals(Outcome.DECLINED, report.outcome)
        assertEquals(0, d.sent)
        assertEquals(1, gate.asked.size)
        assertTrue(AgentState.WAITING_FOR_APPROVAL in j.states)
    }

    @Test fun sendWithApprovalExecutesAndVerifies() = runTest {
        val d = FakeDevice().apply { screen = "chat" }; val j = Events(); val gate = Gate(true)
        val plan = Plan(goal = "g", steps = listOf(step("s1", tap("Enviar"), Expectation.TextVisible("Enviado")), step("s2", AgentAction.CompleteTask("ok"))))
        val report = orch(d, FixedPlanner(plan), gate, j).run("enviar")
        assertEquals(Outcome.SUCCESS, report.outcome)
        assertEquals(1, d.sent)
    }

    @Test fun externalActionIsNeverBlindlyRetriedWhenUnverified() = runTest {
        val d = FakeDevice().apply { screen = "chat"; swallowTaps = true }; val j = Events()
        val plan = Plan(goal = "g", steps = listOf(step("s1", tap("Enviar"), Expectation.TextVisible("Enviado")), step("s2", AgentAction.CompleteTask("ok"))))
        val report = orch(d, FixedPlanner(plan), Gate(true), j).run("enviar")
        assertEquals(Outcome.PARTIAL, report.outcome)
        assertEquals(1, d.executed.size)
        assertEquals(StepStatus.UNVERIFIED, report.steps.first().status)
    }

    @Test fun changedButtonTextTriggersReplanAndRecovers() = runTest {
        val d = FakeDevice(sendLabel = "Submit").apply { screen = "chat" }; val j = Events()
        val original = Plan(goal = "g", steps = listOf(step("s1", tap("Mensagens")), step("s2", AgentAction.CompleteTask("ok"))))
        val replan = Plan(goal = "g", steps = listOf(
            step("r1", AgentAction.EnterText(input, "Oi"), Expectation.FieldContains(input, "Oi")),
            step("r2", AgentAction.CompleteTask("ok")),
        ))
        val planner = FixedPlanner(original, replan)
        val report = orch(d, planner, Gate(true), j).run("tarefa")
        assertEquals(report.summary, Outcome.SUCCESS, report.outcome)
        assertEquals(1, planner.replanCalls)
        assertTrue(AgentState.RECOVERING in j.states)
        assertEquals("Oi", d.field)
    }

    @Test fun missingElementWithoutReplanFailsInsteadOfClickingForever() = runTest {
        val d = FakeDevice(); val j = Events()
        val plan = Plan(goal = "g", steps = listOf(step("s1", tap("Inexistente")), step("s2", AgentAction.CompleteTask("ok"))))
        val report = orch(d, FixedPlanner(plan), Gate(true), j).run("x")
        assertEquals(Outcome.FAILED, report.outcome)
        assertTrue(d.executed.isEmpty())
    }

    @Test fun passwordFieldIsBlocked() = runTest {
        val d = FakeDevice().apply { screen = "login" }; val j = Events()
        val plan = Plan(goal = "g", steps = listOf(step("s1", AgentAction.EnterText(Selector(role = Role.INPUT), "123")), step("s2", AgentAction.CompleteTask("ok"))))
        val report = orch(d, FixedPlanner(plan), Gate(true), j).run("login")
        assertEquals(Outcome.BLOCKED, report.outcome)
        assertTrue(d.executed.isEmpty())
    }

    @Test fun missingAccessibilityStopsWithExplanation() = runTest {
        val d = FakeDevice().apply { snapshotsAvailable = false }; val j = Events()
        val report = orch(d, FixedPlanner(fillPlan), Gate(true), j).run("x")
        assertEquals(Outcome.FAILED, report.outcome)
        assertTrue(report.summary.contains("acessibilidade"))
        assertTrue(AgentState.WAITING_FOR_PERMISSION in j.states)
    }

    @Test fun unverifiedTapIsReportedAsUnverifiedNotSuccess() = runTest {
        val d = FakeDevice().apply { swallowTaps = true }; val j = Events()
        val plan = Plan(goal = "g", steps = listOf(step("s1", tap("Mensagens")), step("s2", AgentAction.CompleteTask("ok"))))
        val report = orch(d, FixedPlanner(plan), Gate(true), j).run("x")
        assertEquals(Outcome.PARTIAL, report.outcome)
        assertTrue(report.summary.contains("não verificado"))
    }

    @Test fun controllerCancelStopsBeforeNextAction() = runTest {
        val d = FakeDevice(); val j = Events(); val ctl = TaskController()
        d.onExecute = { ctl.cancel() }
        val report = orch(d, FixedPlanner(fillPlan), Gate(true), j).run("x", ctl)
        assertEquals(Outcome.CANCELLED, report.outcome)
        assertEquals(1, d.executed.size)
        assertEquals(AgentState.CANCELLED, j.states.last())
    }

    @Test fun coroutineCancellationDuringRunIsReportedAndRethrown() = runTest {
        val d = FakeDevice(); val j = Events()
        d.onExecute = { }
        val gate = object : ApprovalGate {
            override suspend fun request(taskId: String, stepId: String, reason: String, risk: Risk): Boolean { awaitCancellation() }
        }
        val plan = Plan(goal = "g", steps = listOf(step("s1", tap("Mensagens")), step("s2", AgentAction.RequestUserConfirmation("ok?"))))
        val job = launch { orch(d, FixedPlanner(plan), gate, j).run("x") }
        advanceUntilIdle()
        job.cancel(CancellationException("stop"))
        job.join()
        assertTrue(job.isCancelled)
        val finished = j.all.filterIsInstance<TaskEvent.Finished>().single()
        assertEquals(Outcome.CANCELLED, finished.report.outcome)
    }

    @Test fun pauseHoldsExecutionUntilResume() = runTest {
        val d = FakeDevice(); val j = Events(); val ctl = TaskController()
        ctl.pause()
        val deferred = async { orch(d, FixedPlanner(fillPlan), Gate(true), j).run("x", ctl) }
        advanceUntilIdle()
        assertTrue(AgentState.PAUSED in j.states)
        assertTrue(d.executed.isEmpty())
        assertFalse(deferred.isCompleted)
        ctl.resume()
        assertEquals(Outcome.SUCCESS, deferred.await().outcome)
    }

    @Test fun repeatedIdenticalActionIsDetectedAsLoop() = runTest {
        val d = FakeDevice().apply { swallowTaps = true }; val j = Events()
        val plan = Plan(goal = "g", steps = listOf(step("s1", tap("Mensagens"), Expectation.TextVisible("Nunca aparece")), step("s2", AgentAction.CompleteTask("ok"))))
        val report = orch(d, FixedPlanner(plan), Gate(true), j, cfg.copy(maxRetriesPerAction = 10, maxRepeatSameAction = 2)).run("x")
        assertEquals(Outcome.FAILED, report.outcome)
        assertTrue(d.executed.size <= 2)
    }

    @Test fun userCorrectionTriggersReplan() = runTest {
        val d = FakeDevice(); val j = Events(); val ctl = TaskController()
        val corrected = Plan(goal = "g", steps = listOf(step("c1", tap("Mensagens"), Expectation.TextVisible("Cliente B")), step("c2", AgentAction.CompleteTask("ok"))))
        val planner = FixedPlanner(fillPlan, corrected)
        ctl.correct("não, vá para mensagens")
        val report = orch(d, planner, Gate(true), j).run("x", ctl)
        assertEquals(1, planner.replanCalls)
        assertEquals(Outcome.SUCCESS, report.outcome)
        assertEquals("list", d.screen)
    }

    @Test fun openApplicationWorksEvenWhenNoTargetWindowIsObservable() = runTest {
        val d = FakeDevice().apply { snapshotsAvailable = false }; val j = Events()
        d.onExecute = { d.snapshotsAvailable = true }
        val plan = Plan(goal = "g", steps = listOf(
            step("s1", AgentAction.OpenApplication("App", "com.test.app"), Expectation.PackageIs("com.test.app")),
            step("s2", AgentAction.CompleteTask("ok")),
        ))
        val report = orch(d, FixedPlanner(plan), Gate(true), j).run("abrir")
        assertEquals(report.summary, Outcome.SUCCESS, report.outcome)
        assertEquals(StepStatus.VERIFIED, report.steps.first().status)
    }
}
