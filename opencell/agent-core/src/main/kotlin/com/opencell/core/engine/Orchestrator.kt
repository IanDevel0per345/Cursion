package com.opencell.core.engine

import com.opencell.core.model.UiNode
import com.opencell.core.model.UiSnapshot
import com.opencell.core.perception.ElementResolver
import com.opencell.core.perception.Resolution
import com.opencell.core.perception.Verifier
import com.opencell.core.policy.AutonomyMode
import com.opencell.core.policy.PolicyDecision
import com.opencell.core.policy.PolicyEngine
import com.opencell.core.policy.Risk
import com.opencell.core.protocol.AgentAction
import com.opencell.core.protocol.Expectation
import com.opencell.core.protocol.Plan
import com.opencell.core.protocol.PlanStep
import com.opencell.core.protocol.Selector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

class Orchestrator(
    private val observer: ScreenObserver,
    private val executor: ActionExecutor,
    private val planner: Planner,
    private val approvals: ApprovalGate,
    private val journal: TaskJournal,
    private val policy: PolicyEngine,
    private val config: () -> OrchestratorConfig,
    private val clock: Clock = SystemClock,
) {
    private class Stop(val outcome: Outcome, val summary: String, val state: AgentState) : Exception(summary, null, false, false)

    private inner class Run(val goal: String, val controller: TaskController) {
        val taskId = "task-" + UUID.randomUUID().toString().take(8)
        val machine = AgentStateMachine()
        val results = mutableListOf<StepResult>()
        val startedAt = clock.nowMs()
        val repeats = HashMap<String, Int>()

        fun emit(e: TaskEvent) = journal.onEvent(e)

        fun move(to: AgentState) {
            if (machine.state == to) return
            machine.transition(to)
            emit(TaskEvent.StateChanged(taskId, to))
        }

        fun record(step: PlanStep, status: StepStatus, detail: String) {
            val r = StepResult(step.id, step.description, status, detail)
            results += r
            emit(TaskEvent.StepFinished(taskId, r))
        }

        fun report(outcome: Outcome, summary: String) =
            TaskReport(taskId, goal, outcome, summary, results.toList(), startedAt, clock.nowMs())

        suspend fun checkpoint() {
            if (controller.cancelled) throw Stop(Outcome.CANCELLED, "Tarefa cancelada pelo usuário.", AgentState.CANCELLED)
            if (controller.paused) {
                move(AgentState.PAUSED)
                controller.awaitResumed()
                if (controller.cancelled) throw Stop(Outcome.CANCELLED, "Tarefa cancelada pelo usuário.", AgentState.CANCELLED)
                machine.resume()
                emit(TaskEvent.StateChanged(taskId, machine.state))
            }
        }
    }

    suspend fun run(goal: String, controller: TaskController = TaskController()): TaskReport {
        val run = Run(goal, controller)
        return try {
            execute(run)
        } catch (stop: Stop) {
            finish(run, stop)
        } catch (e: CancellationException) {
            withContext(NonCancellable) { finish(run, Stop(Outcome.CANCELLED, "Tarefa interrompida.", AgentState.CANCELLED)) }
            throw e
        }
    }

    private fun finish(run: Run, stop: Stop): TaskReport {
        executor.cancelPending()
        if (!run.machine.state.isTerminal) {
            val target = if (run.machine.canTransition(stop.state)) stop.state else AgentState.FAILED
            if (run.machine.canTransition(target)) run.move(target)
        }
        return run.report(stop.outcome, stop.summary).also { run.emit(TaskEvent.Finished(run.taskId, it)) }
    }

    private suspend fun execute(run: Run): TaskReport {
        run.move(AgentState.UNDERSTANDING)
        run.move(AgentState.PLANNING)
        val first = observer.snapshot()
        val plan = when (val p = planner.plan(run.goal, first)) {
            is PlanResult.Ok -> p.plan
            is PlanResult.Failed -> throw Stop(Outcome.FAILED, "Não consegui montar um plano: ${p.reason}", AgentState.FAILED)
        }
        run.emit(TaskEvent.PlanReady(run.taskId, plan))
        val steps = plan.steps.toMutableList()
        var index = 0
        var executed = 0
        var replans = 0

        while (index < steps.size) {
            run.move(AgentState.OBSERVING)
            run.checkpoint()
            val cfg = config()
            if (executed >= cfg.maxSteps) throw Stop(Outcome.FAILED, "Limite de ${cfg.maxSteps} passos atingido.", AgentState.FAILED)
            var step = steps[index]

            run.controller.takeCorrection()?.let { hint ->
                val replanned = replan(run, step, "Correção do usuário", hint, null, steps, index)
                    ?: throw Stop(Outcome.FAILED, "Não consegui aplicar a correção: \"$hint\".", AgentState.FAILED)
                steps.subList(index, steps.size).clear(); steps.addAll(replanned.steps); step = steps[index]
            }

            run.emit(TaskEvent.StepStarted(run.taskId, step))
            executed++
            when (val outcome = runStep(run, step, cfg)) {
                StepOutcome.Done -> index++
                is StepOutcome.Finish -> {
                    run.record(step, StepStatus.VERIFIED, outcome.summary)
                    return finishPlan(run, outcome.summary)
                }
                is StepOutcome.Failed -> {
                    run.move(AgentState.RECOVERING)
                    val canReplan = replans < cfg.maxReplans
                    val replanned = if (canReplan) {
                        replans++
                        run.move(AgentState.PLANNING)
                        replan(run, step, outcome.reason, null, outcome.snapshot, steps, index)
                    } else null
                    if (replanned == null) {
                        run.record(step, StepStatus.FAILED, outcome.reason)
                        throw Stop(Outcome.FAILED, "Falha no passo \"${step.description}\": ${outcome.reason}", AgentState.FAILED)
                    }
                    steps.subList(index, steps.size).clear(); steps.addAll(replanned.steps)
                }
            }
        }
        return finishPlan(run, "Plano concluído.")
    }

    private fun finishPlan(run: Run, summary: String): TaskReport {
        val unverified = run.results.count { it.status == StepStatus.UNVERIFIED }
        val outcome = if (unverified == 0) Outcome.SUCCESS else Outcome.PARTIAL
        val text = if (unverified == 0) summary else "$summary Ação executada, resultado ainda não verificado em $unverified passo(s)."
        if (run.machine.canTransition(AgentState.COMPLETED)) run.move(AgentState.COMPLETED)
        return run.report(outcome, text).also { run.emit(TaskEvent.Finished(run.taskId, it)) }
    }

    private suspend fun replan(
        run: Run, failed: PlanStep, reason: String, hint: String?, snapshot: UiSnapshot?,
        steps: List<PlanStep>, index: Int,
    ): Plan? {
        val ctx = ReplanContext(
            run.goal, snapshot ?: observer.snapshot(), failed, reason,
            run.results.filter { it.status != StepStatus.FAILED }.map { it.description }, hint,
        )
        return (planner.replan(ctx) as? PlanResult.Ok)?.plan?.takeIf { it.steps.isNotEmpty() }
    }

    private sealed interface StepOutcome {
        data object Done : StepOutcome
        data class Finish(val summary: String) : StepOutcome
        data class Failed(val reason: String, val snapshot: UiSnapshot?) : StepOutcome
    }

    private suspend fun runStep(run: Run, step: PlanStep, cfg: OrchestratorConfig): StepOutcome {
        when (val action = step.action) {
            is AgentAction.CompleteTask -> return StepOutcome.Finish(action.summary)
            is AgentAction.ReportLimitation -> {
                run.record(step, StepStatus.BLOCKED, action.message)
                throw Stop(Outcome.PARTIAL, action.message, AgentState.COMPLETED)
            }
            is AgentAction.RequestUserConfirmation -> {
                if (cfg.policy.mode == AutonomyMode.PERMISSIVE) {
                    run.record(step, StepStatus.VERIFIED, "Confirmação dispensada (modo 100% permissivo)")
                    return StepOutcome.Done
                }
                askApproval(run, step, action.message, Risk.NONE)
                run.record(step, StepStatus.VERIFIED, "Confirmado pelo usuário")
                return StepOutcome.Done
            }
            is AgentAction.WaitForUiChange -> return waitForChange(run, step, action.timeoutMs)
            else -> Unit
        }

        var attempt = 0
        var lastReason = ""
        var lastSnapshot: UiSnapshot? = null
        while (attempt <= cfg.maxRetriesPerAction) {
            run.checkpoint()
            run.move(AgentState.OBSERVING)
            val before = observer.snapshot() ?: if (step.action is AgentAction.OpenApplication) {
                UiSnapshot(0, clock.nowMs(), "", 0, 0, emptyList())
            } else null
            if (before == null) {
                run.move(AgentState.WAITING_FOR_PERMISSION)
                throw Stop(Outcome.FAILED, "Não consigo observar a tela: serviço de acessibilidade indisponível.", AgentState.FAILED)
            }
            lastSnapshot = before

            val selector = step.action.selector()
            var target: UiNode? = null
            if (selector != null) {
                when (val res = ElementResolver.resolve(selector, before)) {
                    is Resolution.Found -> target = res.node
                    Resolution.NotFound -> { lastReason = "Elemento não encontrado na tela."; attempt = recover(run, cfg, attempt); continue }
                    is Resolution.Ambiguous -> {
                        lastReason = "${res.candidates.size} elementos correspondem ao seletor; indique qual."
                        attempt = recover(run, cfg, attempt); continue
                    }
                }
            }

            val loopKey = "${step.id}:${Verifier.signature(before)}"
            val seen = run.repeats.merge(loopKey, 1, Int::plus)!!
            if (seen > cfg.maxRepeatSameAction) return StepOutcome.Failed("Loop detectado: mesma ação repetida sem efeito.", before)

            val risk = policy.classify(step.action, target)
            when (val d = policy.evaluate(step.id, step.action, target, before, cfg.policy)) {
                PolicyDecision.Allow -> Unit
                is PolicyDecision.Deny -> {
                    run.record(step, StepStatus.BLOCKED, d.reason)
                    throw Stop(Outcome.BLOCKED, d.reason, AgentState.FAILED)
                }
                is PolicyDecision.RequireApproval -> askApproval(run, step, d.reason, d.risk)
            }

            run.move(AgentState.EXECUTING)
            when (val r = executor.execute(step.action, target, before)) {
                is ExecResult.Failed -> {
                    lastReason = r.reason
                    if (risk != Risk.NONE) return StepOutcome.Failed(r.reason, before)
                    attempt = recover(run, cfg, attempt); continue
                }
                ExecResult.Dispatched -> Unit
            }

            run.move(AgentState.VERIFYING)
            val explicit = step.expect
            val expectation = explicit ?: defaultExpectation(step.action)
            var verdict = verifyAfter(before, expectation, cfg.settleDelayMs)
            if (verdict?.ok == false) verdict = verifyAfter(before, expectation, cfg.settleDelayMs * 3)
            when {
                verdict == null -> {
                    run.record(step, StepStatus.UNVERIFIED, "Ação executada, resultado ainda não verificado (tela indisponível).")
                    return StepOutcome.Done
                }
                verdict.ok -> {
                    run.record(step, StepStatus.VERIFIED, verdict.detail)
                    return StepOutcome.Done
                }
                explicit == null -> {
                    run.record(step, StepStatus.UNVERIFIED, "Ação executada, resultado ainda não verificado.")
                    return StepOutcome.Done
                }
                risk != Risk.NONE -> {
                    run.record(step, StepStatus.UNVERIFIED, "Ação executada, resultado ainda não verificado. ${verdict.detail}")
                    throw Stop(Outcome.PARTIAL, "Ação com efeito externo executada, mas não confirmada. Verifique manualmente antes de repetir.", AgentState.COMPLETED)
                }
                else -> {
                    lastReason = verdict.detail
                    attempt = recover(run, cfg, attempt)
                }
            }
        }
        return StepOutcome.Failed(lastReason.ifBlank { "Tentativas esgotadas." }, lastSnapshot)
    }

    private suspend fun recover(run: Run, cfg: OrchestratorConfig, attempt: Int): Int {
        run.move(AgentState.RECOVERING)
        delay(cfg.settleDelayMs)
        return attempt + 1
    }

    private suspend fun verifyAfter(before: UiSnapshot, expectation: Expectation?, settleMs: Long) =
        expectation?.let {
            delay(settleMs)
            observer.snapshot()?.let { after -> Verifier.check(it, before, after) }
        } ?: observer.snapshot()?.let { Verifier.check(Expectation.ScreenChanged, before, it) }

    private suspend fun askApproval(run: Run, step: PlanStep, reason: String, risk: Risk) {
        run.move(AgentState.WAITING_FOR_APPROVAL)
        run.emit(TaskEvent.ApprovalRequested(run.taskId, step.id, reason))
        val ok = approvals.request(run.taskId, step.id, reason, risk)
        run.checkpoint()
        if (!ok) {
            run.record(step, StepStatus.SKIPPED, "Recusado pelo usuário")
            throw Stop(Outcome.DECLINED, "Você recusou: $reason", AgentState.CANCELLED)
        }
    }

    private suspend fun waitForChange(run: Run, step: PlanStep, timeoutMs: Long): StepOutcome {
        val base = observer.snapshot()?.let(Verifier::signature)
        var waited = 0L
        while (waited < timeoutMs) {
            delay(200); waited += 200
            val now = observer.snapshot()?.let(Verifier::signature)
            if (now != null && now != base) {
                run.record(step, StepStatus.VERIFIED, "A tela mudou.")
                return StepOutcome.Done
            }
        }
        run.record(step, StepStatus.UNVERIFIED, "Nenhuma mudança observada em ${timeoutMs}ms.")
        return StepOutcome.Done
    }

    private fun defaultExpectation(action: AgentAction): Expectation? = when (action) {
        is AgentAction.EnterText -> Expectation.FieldContains(action.selector, action.text)
        is AgentAction.OpenApplication -> action.packageName?.let { Expectation.PackageIs(it) }
        is AgentAction.ScrollContainer, is AgentAction.Swipe -> null
        else -> null
    }

    private fun AgentAction.selector(): Selector? = when (this) {
        is AgentAction.TapElement -> selector
        is AgentAction.LongPress -> selector
        is AgentAction.EnterText -> selector
        is AgentAction.ScrollContainer -> selector
        else -> null
    }
}
