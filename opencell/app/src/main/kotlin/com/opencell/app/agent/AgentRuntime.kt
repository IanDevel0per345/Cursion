package com.opencell.app.agent

import com.opencell.app.data.API_KEY_SECRET
import com.opencell.app.data.AppSettings
import com.opencell.app.data.OpenCellDatabase
import com.opencell.app.data.SecretStore
import com.opencell.app.data.TaskEntity
import com.opencell.app.data.TaskEventEntity
import com.opencell.app.device.AndroidActionExecutor
import com.opencell.app.device.AndroidScreenObserver
import com.opencell.app.device.AppResolver
import com.opencell.core.engine.AgentState
import com.opencell.core.engine.ApprovalGate
import com.opencell.core.engine.Clock
import com.opencell.core.engine.Orchestrator
import com.opencell.core.engine.OrchestratorConfig
import com.opencell.core.engine.Outcome
import com.opencell.core.engine.StepStatus
import com.opencell.core.engine.TaskController
import com.opencell.core.engine.TaskEvent
import com.opencell.core.engine.TaskJournal
import com.opencell.core.policy.PolicyEngine
import com.opencell.core.policy.PolicySettings
import com.opencell.core.policy.Risk
import com.opencell.core.security.Redactor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class PanelState { Idle, Listening, Transcribing, Thinking, Planning, Executing, WaitingForApproval, Paused, Success, PartialSuccess, Error }

enum class StepUiStatus { PENDING, RUNNING, VERIFIED, UNVERIFIED, FAILED, BLOCKED, SKIPPED }

data class StepUi(val id: String, val description: String, val status: StepUiStatus, val detail: String = "")

data class PendingApproval(val taskId: String, val stepId: String, val reason: String, val risk: Risk)

data class AgentUiState(
    val visible: Boolean = false,
    val panel: PanelState = PanelState.Idle,
    val goal: String = "",
    val partialTranscript: String = "",
    val steps: List<StepUi> = emptyList(),
    val approval: PendingApproval? = null,
    val message: String? = null,
    val outcome: Outcome? = null,
    val agentState: AgentState = AgentState.IDLE,
    val taskActive: Boolean = false,
    val lastReportSpeech: String? = null,
)

class AgentRuntime(
    private val scope: CoroutineScope,
    private val settings: () -> AppSettings,
    private val secrets: SecretStore,
    private val db: OpenCellDatabase,
    private val observer: AndroidScreenObserver,
    private val executor: AndroidActionExecutor,
    private val apps: AppResolver,
    private val clock: Clock,
    private val ownPackage: String,
) {
    private val _ui = MutableStateFlow(AgentUiState())
    val ui: StateFlow<AgentUiState> = _ui.asStateFlow()

    private var job: Job? = null
    private var controller: TaskController? = null
    private var approval: CompletableDeferred<Boolean>? = null
    private var currentGoal = ""
    private val knownTasks = HashSet<String>()

    private val tokens = TokenCounter()

    val active get() = _ui.value.taskActive

    fun setVisible(visible: Boolean) = _ui.update {
        if (visible) it.copy(visible = true) else if (it.taskActive) it.copy(visible = false) else AgentUiState()
    }

    fun setListening(partial: String?) = _ui.update {
        if (it.taskActive) it
        else if (partial == null) it.copy(panel = PanelState.Idle, partialTranscript = "")
        else it.copy(panel = if (partial.isEmpty()) PanelState.Listening else PanelState.Transcribing, partialTranscript = partial)
    }

    fun submit(goal: String) {
        val text = goal.trim()
        if (text.isEmpty()) return
        if (active) { controller?.correct(text); return }
        start(text)
    }

    private fun start(goal: String) {
        currentGoal = goal
        val s = settings()
        val ctl = TaskController().also { controller = it }
        _ui.value = AgentUiState(visible = true, panel = PanelState.Thinking, goal = goal, taskActive = true)
        tokens.reset()
        val orchestrator = Orchestrator(
            observer = observer,
            executor = executor,
            planner = PlannerFactory.create(s, secrets.get(API_KEY_SECRET), apps, tokens),
            approvals = ApprovalBridge(),
            journal = TaskJournal { event -> onEvent(event) },
            policy = PolicyEngine(setOf(ownPackage)),
            config = { configFrom(settings()) },
            clock = clock,
        )
        job = scope.launch {
            try {
                withTimeout(s.taskTimeoutSec * 1000L) { orchestrator.run(goal, ctl) }
            } catch (e: TimeoutCancellationException) {
                _ui.update { it.copy(panel = PanelState.Error, taskActive = false, message = "Tempo limite de ${s.taskTimeoutSec}s excedido; tarefa interrompida.") }
            } catch (_: kotlinx.coroutines.CancellationException) {
            } catch (e: Exception) {
                _ui.update { it.copy(panel = PanelState.Error, taskActive = false, message = "Erro inesperado: ${Redactor.redact(e.message.orEmpty())}") }
            }
        }
    }

    fun approve(granted: Boolean) { approval?.complete(granted) }

    fun pause() { controller?.pause() }
    fun resume() { controller?.resume() }

    fun stop(reason: String? = null) {
        if (!active) return
        controller?.cancel()
        approval?.complete(false)
        job?.cancel()
        reason?.let { r -> _ui.update { it.copy(message = r) } }
    }

    fun dismissResult() = _ui.update { if (it.taskActive) it else AgentUiState(visible = it.visible) }

    private fun configFrom(s: AppSettings) = OrchestratorConfig(
        maxSteps = s.maxSteps,
        maxRetriesPerAction = s.maxRetriesPerAction,
        policy = PolicySettings(
            mode = s.autonomy,
            blockedPackages = s.blockedPackages,
            allowedPackages = s.allowedPackages,
            confirmExternalActions = s.confirmExternal,
            confirmIrreversible = s.confirmIrreversible,
        ),
    )

    private inner class ApprovalBridge : ApprovalGate {
        override suspend fun request(taskId: String, stepId: String, reason: String, risk: Risk): Boolean {
            val d = CompletableDeferred<Boolean>().also { approval = it }
            _ui.update { it.copy(panel = PanelState.WaitingForApproval, approval = PendingApproval(taskId, stepId, reason, risk)) }
            return try { d.await() } finally {
                approval = null
                _ui.update { it.copy(approval = null) }
            }
        }
    }

    private fun onEvent(event: TaskEvent) {
        persist(event)
        _ui.update { s ->
            when (event) {
                is TaskEvent.StateChanged -> s.copy(agentState = event.state, panel = panelFor(event.state, s.panel))
                is TaskEvent.PlanReady -> s.copy(steps = event.plan.steps.filter { it.action !is com.opencell.core.protocol.AgentAction.CompleteTask }
                    .map { StepUi(it.id, it.description, StepUiStatus.PENDING) })
                is TaskEvent.StepStarted -> s.copy(steps = s.steps.map { if (it.id == event.step.id) it.copy(status = StepUiStatus.RUNNING) else it })
                is TaskEvent.StepFinished -> s.copy(steps = s.steps.map {
                    if (it.id == event.result.stepId) it.copy(status = event.result.status.toUi(), detail = event.result.detail) else it
                })
                is TaskEvent.ApprovalRequested -> s
                is TaskEvent.Finished -> {
                    val r = event.report
                    s.copy(
                        taskActive = false, approval = null, outcome = r.outcome, message = r.summary, lastReportSpeech = r.summary,
                        panel = when (r.outcome) {
                            Outcome.SUCCESS -> PanelState.Success
                            Outcome.PARTIAL -> PanelState.PartialSuccess
                            Outcome.CANCELLED, Outcome.DECLINED -> PanelState.PartialSuccess
                            Outcome.FAILED, Outcome.BLOCKED -> PanelState.Error
                        },
                    )
                }
            }
        }
    }

    private fun panelFor(state: AgentState, current: PanelState) = when (state) {
        AgentState.UNDERSTANDING -> PanelState.Thinking
        AgentState.PLANNING -> PanelState.Planning
        AgentState.WAITING_FOR_APPROVAL -> PanelState.WaitingForApproval
        AgentState.PAUSED -> PanelState.Paused
        AgentState.OBSERVING, AgentState.EXECUTING, AgentState.VERIFYING, AgentState.RECOVERING -> PanelState.Executing
        else -> current
    }

    private fun StepStatus.toUi() = when (this) {
        StepStatus.VERIFIED -> StepUiStatus.VERIFIED
        StepStatus.UNVERIFIED -> StepUiStatus.UNVERIFIED
        StepStatus.FAILED -> StepUiStatus.FAILED
        StepStatus.BLOCKED -> StepUiStatus.BLOCKED
        StepStatus.SKIPPED -> StepUiStatus.SKIPPED
    }

    private fun persist(event: TaskEvent) {
        val goal = currentGoal
        val settingsSnapshot = settings()
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val dao = db.tasks()
            if (knownTasks.add(event.taskId)) {
                dao.upsert(TaskEntity(event.taskId, Redactor.redact(goal), clock.nowMs(), null, null, null, null, "${settingsSnapshot.providerKind}/${settingsSnapshot.model}"))
            }
            val (kind, title, detail) = when (event) {
                is TaskEvent.StateChanged -> Triple("state", event.state.name, "")
                is TaskEvent.PlanReady -> Triple("plan", "Plano com ${event.plan.steps.size} passos", event.plan.steps.joinToString("\n") { "${it.id}: ${it.description}" })
                is TaskEvent.StepStarted -> Triple("step_start", event.step.description, "")
                is TaskEvent.StepFinished -> Triple("step_${event.result.status.name.lowercase()}", event.result.description, event.result.detail)
                is TaskEvent.ApprovalRequested -> Triple("approval", "Aprovação solicitada", event.reason)
                is TaskEvent.Finished -> Triple("finished", event.report.outcome.name, event.report.summary)
            }
            if (event !is TaskEvent.StateChanged) {
                dao.insertEvent(TaskEventEntity(taskId = event.taskId, atMs = clock.nowMs(), kind = kind, title = Redactor.redact(title), detail = Redactor.redact(detail)))
            }
            if (event is TaskEvent.Finished) {
                val base = dao.task(event.taskId) ?: return@launch
                dao.upsert(base.copy(finishedAtMs = event.report.finishedAtMs, outcome = event.report.outcome.name, summary = Redactor.redact(event.report.summary),
                    appPackage = observer.lastPackage, inputTokens = tokens.input, outputTokens = tokens.output))
            }
        }
    }
}
